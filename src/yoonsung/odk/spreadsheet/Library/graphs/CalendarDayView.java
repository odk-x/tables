package yoonsung.odk.spreadsheet.Library.graphs;

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
		dv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
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
