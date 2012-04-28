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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class GraphActivity extends Activity {
	
	public static final int GRAPH_SETTING = 0;
	
	private String tableID;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Current Table
        this.tableID = getIntent().getStringExtra("tableID");
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, GRAPH_SETTING, 0, "Settings");
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        
    	switch(item.getItemId()) {
        	case GRAPH_SETTING:
        		Intent i = new Intent(this, GraphSetting.class);
        		i.putExtra("tableID", tableID);
        		Log.d("hi", "starting gs activity:"+tableID);
        		startActivity(i);
        		return true;
    	}
    	
    	return super.onMenuItemSelected(featureId, item);
    }
    
}
