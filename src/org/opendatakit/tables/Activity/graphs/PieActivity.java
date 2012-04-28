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

import org.achartengine.GraphicalView;
import org.achartengine.model.CategorySeries;
import org.opendatakit.tables.Library.graphs.GraphFactory;

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
