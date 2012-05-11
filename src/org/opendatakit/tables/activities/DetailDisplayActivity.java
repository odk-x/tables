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
package org.opendatakit.tables.activities;

import java.util.HashMap;
import java.util.Map;

import org.opendatakit.tables.view.CustomDetailView;

import android.app.Activity;
import android.os.Bundle;


public class DetailDisplayActivity extends Activity
        implements DisplayActivity {
    
    public static final String INTENT_KEY_ROW_ID = "rowId";
    public static final String INTENT_KEY_ROW_KEYS = "rowKeys";
    public static final String INTENT_KEY_ROW_VALUES = "rowValues";
    
    private String rowId;
    private Controller c;
    private String[] keys;
    private String[] values;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rowId = getIntent().getStringExtra(INTENT_KEY_ROW_ID);
        c = new Controller(this, this, getIntent().getExtras());
        keys = getIntent().getStringArrayExtra(INTENT_KEY_ROW_KEYS);
        values = getIntent().getStringArrayExtra(INTENT_KEY_ROW_VALUES);
        init();
    }
    
    @Override
    public void init() {
        Map<String, String> data = new HashMap<String, String>();
        for (int i = 0; i < keys.length; i++) {
            data.put(keys[i], values[i]);
        }
        CustomDetailView cdv = new CustomDetailView(this,
                c.getTableProperties());
        cdv.display(rowId, data);
        c.setDisplayView(cdv);
        setContentView(c.getContainerView());
    }
    
    @Override
    public void onSearch() {
        Controller.launchTableActivity(this, c.getTableProperties(),
                c.getSearchText(), c.getIsOverview());
    }
    
    @Override
    public void onAddRow() {
        // TODO Auto-generated method stub
        
    }
}
