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
package org.opendatakit.tables.views.calendars;

import java.util.Calendar;
import java.util.Date;
import java.util.List;


import android.content.Context;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class CalendarDayView extends CalendarView {

	protected CalendarDayView(Context c, List<GEventPoint> data, Date date,
			String title, String subtitle) {
		super(c, title, subtitle);
		ScrollView sv = new ScrollView(c);
		LinearLayout lv = new LinearLayout(c);
		DayView dv = new DayView(c, data, getBounds(date), true);
		TimeList tl = new TimeList(c, dv.getEarlyMin(), dv.getLateMin());
		tl.setLayoutParams(new LayoutParams(tl.getW(),
				LayoutParams.WRAP_CONTENT, 0));
		dv.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT, 1));
		lv.addView(tl);
		lv.addView(dv);
		sv.addView(lv);
		addView(sv);
	}

	private GEventPoint getBounds(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH);
		int day = cal.get(Calendar.DAY_OF_MONTH);
		Calendar start = Calendar.getInstance();
		start.set(year, month, day, 0, 0, 0);
		Calendar end = Calendar.getInstance();
		end.set(year, month, day, 23, 59, 59);
		return new GEventPoint(start.getTime(), end.getTime(), "", "");
	}

}
