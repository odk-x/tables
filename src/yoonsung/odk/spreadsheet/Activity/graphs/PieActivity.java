package yoonsung.odk.spreadsheet.Activity.graphs;

import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;

import yoonsung.odk.spreadsheet.Library.graphs.GraphFactory;
import android.os.Bundle;

public class PieActivity extends GraphActivity {
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String xName = getIntent().getExtras().getString("xName");
        String[] xVals = getIntent().getExtras().getStringArray("xVals");
        int[] yVals = getIntent().getExtras().getIntArray("yVals");
        CategorySeries data = new CategorySeries(xName);
        for(int i=0; i<xVals.length; i++) {
        	data.add(xVals[i], yVals[i]);
        }
        GraphFactory f = new GraphFactory(this);
        GraphicalView v = f.getPieChart(data, xName, "", "");
        setContentView(v);
    }
	
	
}
