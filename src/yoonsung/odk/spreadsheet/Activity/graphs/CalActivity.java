package yoonsung.odk.spreadsheet.Activity.graphs;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import yoonsung.odk.spreadsheet.Library.graphs.GEventPoint;
import yoonsung.odk.spreadsheet.Library.graphs.GraphFactory;
import android.os.Bundle;
import android.util.Log;


public class CalActivity extends GraphActivity {
	
	private static final String[] monthArr = {"Jan", "Feb", "Mar", "Apr",
		"May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get x and y values
        ArrayList<String> x = getIntent().getExtras().getStringArrayList("x");
        ArrayList<String> y = getIntent().getExtras().getStringArrayList("y");
        
        // Check if data is valid to graph
        if ( (x != null && y != null) 
        		&& (x.size() == y.size()) ) {
                
	        // Convert
	        List<GEventPoint> list = createPlotData(x, y);
	        Log.d("ca", list.toString());
	    	
	        // Dras
	        GraphFactory f = new GraphFactory(this);
	    	setContentView(f.getDayCalendar(list, "2010-02-17", 2010, 2, 17));
        } 
    }
    
    private List<GEventPoint> createPlotData(ArrayList<String> x, ArrayList<String> y) {
    	List<GEventPoint> list = new ArrayList<GEventPoint>();
    	for(int i=0; i<x.size(); i++) {
    		String str = x.get(i);
    		if(str != null) {
    			String[] splt = str.split(" - ");
    			Date start = getDateFromStr(splt[0]);
    			Date end = getDateFromStr(splt[1]);
    			list.add(new GEventPoint(start, end, y.get(i), ""));
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

}
