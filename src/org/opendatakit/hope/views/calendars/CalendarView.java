/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.hope.views.calendars;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public abstract class CalendarView extends LinearLayout {
	
    public static final int PREV_BUTTON_ID = 10;
    public static final int NEXT_BUTTON_ID = 11;
	private static final int PIXELS_PER_MINUTE = 1;
	
	private static final int FOREGROUND_COLOR = Color.BLACK;
	private static final int BACKGROUND_COLOR = Color.rgb(80, 80, 255);
	
	protected CalendarView(Context c, String title, String subtitle) {
		super(c);
		setOrientation(LinearLayout.VERTICAL);
		addView(getNavView(c, title, subtitle));
	}
	
	private View getNavView(Context c, String title, String subtitle) {
	    // building the buttons
	    Button prev = new Button(c);
	    prev.setId(PREV_BUTTON_ID);
	    prev.setText("<");
	    Button next = new Button(c);
	    next.setId(NEXT_BUTTON_ID);
	    next.setText(">");
	    // building the center
		LinearLayout center = new LinearLayout(c);
		center.setOrientation(LinearLayout.VERTICAL);
		center.setBackgroundColor(Color.BLACK);
		TextView titleView = new TextView(c);
		titleView.setText(title);
		titleView.setBackgroundColor(Color.LTGRAY);
		titleView.setTextColor(Color.BLACK);
		titleView.setGravity(Gravity.CENTER_HORIZONTAL);
		center.addView(titleView);
        TextView subtitleView = new TextView(c);
        subtitleView.setText(subtitle);
        subtitleView.setBackgroundColor(Color.LTGRAY);
        subtitleView.setTextColor(Color.BLACK);
        subtitleView.setGravity(Gravity.CENTER_HORIZONTAL);
        center.addView(subtitleView);
        // building the whole
        LinearLayout v = new LinearLayout(c);
        v.setBackgroundColor(Color.LTGRAY);
        v.setGravity(Gravity.CENTER_HORIZONTAL);
        v.addView(prev);
        v.addView(center);
        v.addView(next);
		return v;
	}
	
	protected String getTimeStr(int mins) {
		String text = (mins / 60) + ":";
		if((mins % 60) < 10) {
			text += "0" + (mins % 60);
		} else {
			text += (mins % 60);
		}
		return text;
	}
	
	protected class DayView extends View {
		
		private TreeSet<ItemView> itemTree;
		private int colCount;
		private Map<Integer, Set<ItemView>> timeToItems;
		private int earlyMinute;
		private int lateMinute;
		private boolean showDetails;
		
		protected DayView(Context c, List<GEventPoint> data,
				GEventPoint bounds, boolean showDetails) {
			super(c);
			setBackgroundColor(Color.WHITE);
			colCount = 1;
			timeToItems = new HashMap<Integer, Set<ItemView>>();
			earlyMinute = (60 * 23) + 59;
			lateMinute = 0;
			this.showDetails = showDetails;
			TreeSet<ItemView> tree = new TreeSet<ItemView>();
			for(GEventPoint pt : data) {
				if(bounds.overlap(pt)) {
					ItemView next = new ItemView(c, pt);
					Set<Integer> takenPos = new HashSet<Integer>();
					for(ItemView other : tree) {
						if(other.getData().overlap(next.getData())) {
							takenPos.add(other.getPosition());
						}
					}
					int pos = 1;
					while(takenPos.contains(pos)) {
						pos++;
					}
					next.setPosition(pos);
					colCount = Math.max(colCount, pos);
					tree.add(next);
					int startMin = (60 * pt.getStartHour()) +
					pt.getStartMinute();
					earlyMinute = Math.min(earlyMinute, startMin);
					lateMinute = Math.max(lateMinute, ((60 * pt.getEndHour()) +
							pt.getEndMinute()));
					if(!timeToItems.containsKey(startMin)) {
						timeToItems.put(startMin, new HashSet<ItemView>());
					}
					timeToItems.get(startMin).add(next);
				}
			}
			earlyMinute -= (earlyMinute % 30);
			lateMinute += (30 - (lateMinute % 30));
			itemTree = tree;
			setMinimumHeight(PIXELS_PER_MINUTE * (lateMinute - earlyMinute));
		}
		
		@Override
		public void onDraw(Canvas canvas) {
			Paint paint = new Paint();
			int w = getWidth() - 8;
			int l = 8;
			int itemWidth = w / colCount;
			paint.setTextAlign(Align.LEFT);
			for(ItemView v : itemTree) {
				paint.setColor(FOREGROUND_COLOR);
				GEventPoint pt = v.getData();
				int startX = l + ((v.getPosition() - 1) * itemWidth);
				int endX = startX + itemWidth - 8;
				int startY = PIXELS_PER_MINUTE * ((60 * pt.getStartHour()) +
					pt.getStartMinute() - earlyMinute);
				int endY = PIXELS_PER_MINUTE * ((60 * pt.getEndHour()) +
						pt.getEndMinute() - earlyMinute);
				RectF outerRect = new RectF(startX, startY, endX, endY);
				canvas.drawRoundRect(outerRect, 3, 3, paint);
				paint.setColor(BACKGROUND_COLOR);
				RectF innerRect = new RectF(startX + 1, startY + 1, endX - 1,
						endY - 1);
				canvas.drawRoundRect(innerRect, 3, 3, paint);
				paint.setColor(FOREGROUND_COLOR);
				if(showDetails) {
					drawText(canvas, paint, startX, startY, itemWidth,
							(endY - startY), pt);
				}
			}
		}
		
		private void drawText(Canvas canvas, Paint paint, int startX,
				int startY, int width, int height, GEventPoint pt) {
			width -= 20;
			startX += 5;
			startY += 5;
			TextPaint tp = new TextPaint(paint);
			if(height > 20) {
				StaticLayout layout = new StaticLayout(pt.getName(), tp, width,
						Layout.Alignment.ALIGN_NORMAL, 1, 0, true);
				canvas.translate(startX, startY);
				layout.draw(canvas);
				canvas.translate(-startX, -startY);
				startY += 20;
				canvas.drawLine(startX - 5, startY, startX + width + 6, startY,
						paint);
				startY += 5;
			}
			if(height > 45) {
				int startMins = (60 * pt.getStartHour()) + pt.getStartMinute();
				int endMins = (60 * pt.getEndHour()) + pt.getEndMinute();
				String timeStr = getTimeStr(startMins) + " - " +
						getTimeStr(endMins);
				StaticLayout layout = new StaticLayout(timeStr, tp, width,
						Layout.Alignment.ALIGN_NORMAL, 1, 0, true);
				canvas.translate(startX, startY);
				layout.draw(canvas);
				canvas.translate(-startX, -startY);
			}
		}
		
		protected int getEarlyMin() {
			return earlyMinute;
		}
		
		protected void setEarlyMin(int min) {
			earlyMinute = min;
			setMinimumHeight(PIXELS_PER_MINUTE * (lateMinute - earlyMinute));
		}
		
		protected int getLateMin() {
			return lateMinute;
		}
		
		protected void setLateMin(int min) {
			lateMinute = min;
			setMinimumHeight(PIXELS_PER_MINUTE * (lateMinute - earlyMinute));
		}
		
	}
	
	protected class ItemView extends View implements Comparable<ItemView> {
		
		private GEventPoint data;
		private int position;
		
		protected ItemView(Context c, GEventPoint data) {
			super(c);
			this.data = data;
			position = 1;
		}
		
		protected GEventPoint getData() {
			return data;
		}
		
		protected int getPosition() {
			return position;
		}
		
		protected void setPosition(int position) {
			this.position = position;
		}

		@Override
		public int compareTo(ItemView other) {
			return getData().compareTo(other.getData());
		}
		
	}
	
	protected class TimeList extends View {
		
		private int earlyMinute;
		private int lateMinute;
		
		protected TimeList(Context c, int earlyMinute, int lateMinute) {
			super(c);
			this.earlyMinute = earlyMinute;
			this.lateMinute = lateMinute;
			setMinimumHeight(PIXELS_PER_MINUTE * (lateMinute - earlyMinute));
			setMinimumWidth(40);
		}
		
		public void onDraw(Canvas canvas) {
			Paint paint = new Paint();
			paint.setTextAlign(Align.RIGHT);
			paint.setColor(Color.DKGRAY);
			canvas.drawRect(0, 0, getW(), (PIXELS_PER_MINUTE *
					(lateMinute - earlyMinute)), paint);
			paint.setColor(Color.LTGRAY);
			for(int i=earlyMinute; i<lateMinute; i+=30) {
				int y = (i - earlyMinute + 30) * PIXELS_PER_MINUTE;
				canvas.drawLine(0, y, getW(), y, paint);
				y -= 15 * PIXELS_PER_MINUTE;
				canvas.drawText(getTimeStr(i), getW() - 5, y, paint);
			}
		}
		
		protected int getW() {
			return 40;
		}
		
	}
	
}
