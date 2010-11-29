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
	    	setContentView(f.getDayCalendar(list, "2010-10-21", 2010, 10, 23));
        } 
    }
    
    private List<GEventPoint> createPlotData(ArrayList<String> x, ArrayList<String> y) {
    	List<GEventPoint> list = new ArrayList<GEventPoint>();
    	for(int i=0; i<x.size(); i++) {
    		String str = x.get(i);
    		if(str != null) {
    			String[] splt = str.split("/");
    			Date start = getDateFromStr(splt[0]);
    			Date end = getDateFromStr(splt[1]);
    			list.add(new GEventPoint(start, end, y.get(i), ""));
    		}
    	}
		return list;
    }
    
    private Date getDateFromStr(String str) {
    	Calendar cal = Calendar.getInstance();
    	String[] spl = str.split(":");
    	cal.set(new Integer(spl[0]), new Integer(spl[1]), new Integer(spl[2]),
    			new Integer(spl[3]), new Integer(spl[4]), new Integer(spl[5]));
    	Log.d("ca", "time:" + cal.getTime().toString());
    	return cal.getTime();
    }

}
