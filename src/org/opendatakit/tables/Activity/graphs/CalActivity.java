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
package org.opendatakit.tables.Activity.graphs;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.opendatakit.tables.Database.DataUtils;
import org.opendatakit.tables.Library.graphs.CalendarView;
import org.opendatakit.tables.Library.graphs.GEventPoint;
import org.opendatakit.tables.Library.graphs.GraphFactory;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;


public class CalActivity extends GraphActivity {
	
	private static final String[] monthArr = {"Jan", "Feb", "Mar", "Apr",
		"May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
	private Calendar cal;
	private List<String> xVals;
	private List<String> yVals;
	private OnClickListener navButtonListener;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cal = Calendar.getInstance();
        // Get x and y values
        xVals = getIntent().getExtras().getStringArrayList("x");
        yVals = getIntent().getExtras().getStringArrayList("y");
        
        // Check if data is valid to graph
        if((xVals == null) || (yVals == null) ||
                (xVals.size() != yVals.size())) {
            return;
        }
        navButtonListener = new NavListener();
        prepView();
    }
    
    private void prepView() {
        // Convert
        List<GEventPoint> list = createPlotData(xVals, yVals);
        Log.d("ca", list.toString());
        // Draw
        GraphFactory f = new GraphFactory(this);
        setContentView(f.getDayCalendar(list, cal, "Calendar"));
        prepNavButtons();
    }
    
    private List<GEventPoint> createPlotData(List<String> x, List<String> y) {
        DataUtils du = DataUtils.getInstance();
    	List<GEventPoint> list = new ArrayList<GEventPoint>();
    	for(int i=0; i<x.size(); i++) {
    		String str = x.get(i);
    		if(str != null) {
    			Date[] dates;
                try {
                    dates = du.parseDateRangeFromDB(str);
                } catch(ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return null;
                }
    			list.add(new GEventPoint(dates[0], dates[1], y.get(i), ""));
    		}
    	}
		return list;
    }
    
    private Date getDateFromStr(String str) {
    	Calendar cal = Calendar.getInstance();
    	String[] dateSpl = str.split(" ");
    	int year = new Integer(dateSpl[2].substring(0, dateSpl[2].length() - 1));
    	int mon = 0;
    	for(int i=0; i<monthArr.length; i++) {
    		if(monthArr[i].equals(dateSpl[0])) {mon = i;}
    	}
    	int date = new Integer(dateSpl[1]);
    	String[] timeSpl = dateSpl[3].split(":");
    	cal.set(year, mon, date, new Integer(timeSpl[0]),
    			new Integer(timeSpl[1]), 0);
    	cal.set(Calendar.MILLISECOND, 0);
    	Log.d("ca", "time:" + cal.getTime().toString());
    	return cal.getTime();
    }
    
    private void prepNavButtons() {
        Button prevButton = (Button) findViewById(CalendarView.PREV_BUTTON_ID);
        prevButton.setOnClickListener(navButtonListener);
        Button nextButton = (Button) findViewById(CalendarView.NEXT_BUTTON_ID);
        nextButton.setOnClickListener(navButtonListener);
    }
    
    private class NavListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            int inc;
            int vid = v.getId();
            if(vid == CalendarView.PREV_BUTTON_ID) {
                inc = -1;
            } else if(vid == CalendarView.NEXT_BUTTON_ID) {
                inc = 1;
            } else {
                throw new IllegalArgumentException();
            }
            cal.add(Calendar.DAY_OF_MONTH, inc);
            prepView();
        }
    }

}
