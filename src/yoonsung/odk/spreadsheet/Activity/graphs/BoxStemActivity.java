package yoonsung.odk.spreadsheet.Activity.graphs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import yoonsung.odk.spreadsheet.Library.graphs.EGraphicalView;
import yoonsung.odk.spreadsheet.Library.graphs.GValuePercentilePoint;
import yoonsung.odk.spreadsheet.Library.graphs.GraphClickListener;
import yoonsung.odk.spreadsheet.Library.graphs.GraphFactory;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class BoxStemActivity extends GraphActivity {
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        List<GValuePercentilePoint> list = new ArrayList<GValuePercentilePoint>();
        
        ArrayList<String> x = getIntent().getExtras().getStringArrayList("x");
        double[] min = getIntent().getExtras().getDoubleArray("min");
        double[] low = getIntent().getExtras().getDoubleArray("low");
        double[] mid = getIntent().getExtras().getDoubleArray("mid");
        double[] high = getIntent().getExtras().getDoubleArray("high");
        double[] max = getIntent().getExtras().getDoubleArray("max");
        String xname = getIntent().getExtras().getString("xname");
        String yname = getIntent().getExtras().getString("yname");
                    
        Set<String> seenX = new HashSet<String>();
        int index = 0;
        for (int i = 0; i < x.size(); i++) {
            String xVal = x.get(i);
            if (seenX.contains(xVal)) {
                continue;
            }
            seenX.add(xVal);
        	list.add(new GValuePercentilePoint(xVal, min[index], low[index], mid[index], high[index], max[index]));
        	index++;
        }
    	
        GraphFactory f = new GraphFactory(this);
    	EGraphicalView v = f.getBoxStemGraph(list, "", xname, yname);
    	//v.addListener(new ClickListener(this));
    	setContentView(v);
    }
    
    private class ClickListener implements GraphClickListener {
    	private Activity a;
    	protected ClickListener(Activity a) {
    		this.a = a;
    	}
		@Override
		public void graphClicked(double x) {
			if(x < 0) {
				return;
			}
			Intent i = new Intent();
			i.putExtra("x", x);
			i.setClass(a, LineActivity.class);
			startActivity(i);
		}
    }

}
