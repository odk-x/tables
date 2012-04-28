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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendatakit.tables.Library.graphs.EGraphicalView;
import org.opendatakit.tables.Library.graphs.GValuePercentilePoint;
import org.opendatakit.tables.Library.graphs.GraphClickListener;
import org.opendatakit.tables.Library.graphs.GraphFactory;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

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
