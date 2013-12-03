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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.opendatakit.hope.R;


import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class CalendarWeekView extends CalendarView {

	protected CalendarWeekView(Context c, List<GEventPoint> data, Date date,
			String title, String subtitle) {
		super(c, title, subtitle);
		List<GEventPoint> bl = getBounds(date);
		List<DayView> dvs = new ArrayList<DayView>();
		int earlyMin = (23 * 60) + 59;
		int lateMin = 0;
		LinearLayout.LayoutParams lp = new LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1);
		lp.setMargins(1, 0, 0, 0);
		for(GEventPoint bounds : bl) {
			DayView dv = new DayView(c, data, bounds, false);
			dv.setLayoutParams(lp);
			dvs.add(dv);
			earlyMin = Math.min(earlyMin, dv.getEarlyMin());
			lateMin = Math.max(lateMin, dv.getLateMin());
		}
		TimeList tl = new TimeList(c, earlyMin, lateMin);
		tl.setLayoutParams(new LayoutParams(tl.getW(),
				LayoutParams.WRAP_CONTENT, 0));
		LinearLayout lv = new LinearLayout(c);
		lv.setBackgroundColor(Color.DKGRAY);
		lv.addView(tl);
		for(DayView dv : dvs) {
			dv.setEarlyMin(earlyMin);
			dv.setLateMin(lateMin);
			lv.addView(dv);
		}
		ScrollView sv = new ScrollView(c);
		sv.addView(lv);
		addView(getTopRow(c, tl.getW()));
		addView(sv);
	}

	private View getTopRow(Context c, int timeWidth) {
		LinearLayout lv = new LinearLayout(c);
		lv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT));
		String[] dows = {
				c.getString(R.string.sunday_abbrev),
				c.getString(R.string.monday_abbrev),
				c.getString(R.string.tuesday_abbrev),
				c.getString(R.string.wednesday_abbrev),
				c.getString(R.string.thursday_abbrev),
				c.getString(R.string.friday_abbrev),
				c.getString(R.string.saturday_abbrev)};
		View gap = new View(c);
		gap.setLayoutParams(new LayoutParams(timeWidth, 0, 0));
		lv.addView(gap);
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT, 1);
		for(String dow : dows) {
			TextView tv = new TextView(c);
			tv.setGravity(Gravity.CENTER);
			tv.setText(dow);
			tv.setLayoutParams(lp);
			lv.addView(tv);
		}
		lv.setBackgroundColor(Color.BLACK);
		return lv;
	}

	private List<GEventPoint> getBounds(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int sub = cal.get(Calendar.DAY_OF_WEEK) * 24 * 60 * 60 * 1000;
		cal.setTimeInMillis(date.getTime() - sub);
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
				cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
		Date start = cal.getTime();
		Date end = new Date(cal.getTimeInMillis() + 1000 * ((23 * 60 * 60) +
				(59 * 60) + 59));
		List<GEventPoint> bl = new ArrayList<GEventPoint>();
		for(int i=0; i<7; i++) {
			bl.add(new GEventPoint(start, end, "", ""));
			start.setTime(start.getTime() + (1000 * 24 * 60 * 60));
			end.setTime(start.getTime() + (1000 * 24 * 60 * 60));
		}
		return bl;
	}

}
