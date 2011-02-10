package yoonsung.odk.spreadsheet.Activity.graphs;

import java.util.ArrayList;
import java.util.List;

import yoonsung.odk.spreadsheet.Library.graphs.GValueYPoint;
import yoonsung.odk.spreadsheet.Library.graphs.GraphFactory;
import android.os.Bundle;
import android.util.Log;


public class LineActivity extends GraphActivity {
	
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
	        List<GValueYPoint> list = createPlotData(x, y);
	    	
	        // Draw Line Graph
	        GraphFactory f = new GraphFactory(this);
	    	setContentView(f.getValueYLineGraph(list, "Fridge Temperatures", "Day", "Temperature (C)"));
        } 
    }
    
    private List<GValueYPoint> createPlotData(ArrayList<String> x, ArrayList<String> y) {
		// Result
    	List<GValueYPoint> list = new ArrayList<GValueYPoint>();
    	
    	// Map x and y and make a plot list
		for (int i = 0; i < x.size(); i++) {
			try {
			list.add(new GValueYPoint(x.get(i), Double.parseDouble(y.get(i))));
			} catch (Exception e) {
				Log.e("LineGraph", "Number Parse Error. Skiping 1 entry.");
			}
		}
		    	
		return list;
    }

}
