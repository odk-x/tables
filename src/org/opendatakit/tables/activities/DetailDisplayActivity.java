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

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.utils.CollectUtil;
import org.opendatakit.tables.utils.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.views.webkits.CustomDetailView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;


public class DetailDisplayActivity extends SherlockActivity
        implements DisplayActivity {
  
  private static final String TAG = "DetaiDisplayActivity";
    
    public static final String INTENT_KEY_ROW_ID = "rowId";
    public static final String INTENT_KEY_ROW_KEYS = "rowKeys";
    public static final String INTENT_KEY_ROW_VALUES = "rowValues";
    
    private String rowId;
    private Controller c;
    private String[] keys;
    private String[] values;
    private Map<String, String> data;
    private CustomDetailView view;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("");
        rowId = getIntent().getStringExtra(INTENT_KEY_ROW_ID);
        c = new Controller(this, this, getIntent().getExtras());
        keys = getIntent().getStringArrayExtra(INTENT_KEY_ROW_KEYS);
        values = getIntent().getStringArrayExtra(INTENT_KEY_ROW_VALUES);
        init();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
        Intent data) {
      if (c.handleActivityReturn(requestCode, resultCode, data)) {
        // If we're here, we also need to update the displayed data, which
        // may have been changed. 
        Query query = new Query(new TableProperties[] {c.getTableProperties()},
            c.getTableProperties());
        query.loadFromUserQuery("");
        UserTable table = c.getIsOverview() ?
            c.getDbTable().getUserOverviewTable(query) :
              c.getDbTable().getUserTable(query);
        // now we need to get the row num.
        int rowNum = table.getRowNumFromId(rowId);
        int instanceId = Integer.valueOf(data.getData().getLastPathSegment());
        Map<String, String> formValues = c.getOdkCollectFormValues(instanceId);
        this.data = c.getMapForInsertion(formValues);
      }
      super.onActivityResult(requestCode, resultCode, data);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        displayView();
    }
    
    @Override
    public void init() {
        // change the info bar text IF necessary
      if (!c.getInfoBarText().endsWith(" (Detailed)")) {
          c.setInfoBarText(c.getInfoBarText() + " (Detailed)");
      }
        data = new HashMap<String, String>();
        for (int i = 0; i < keys.length; i++) {
            data.put(keys[i], values[i]);
        }
        view = new CustomDetailView(this, c.getTableProperties());
        displayView();
    }
    
    private void displayView() {
        view.display(rowId, data);
        c.setDisplayView(view);
        setContentView(c.getContainerView());
    }
    
    @Override
    public void onSearch() {
        Controller.launchTableActivity(this, c.getTableProperties(),
                c.getSearchText(), c.getIsOverview());
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        c.buildOptionsMenu(menu, false);
        // We want to be able to edit a row from the detail view. Rather than
        // add a new button, we're just going to hook onto the add row
        // button, change the png.
        MenuItem addRow = menu.getItem(Controller.MENU_ITEM_ID_ADD_ROW_BUTTON);
        addRow.setIcon(R.drawable.content_edit);
        addRow.setEnabled(true);
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
      // If they've selected the edit button, we need to handle it here.
      // Otherwise, we let controller handle it.
      if (item.getItemId() == Controller.MENU_ITEM_ID_ADD_ROW_BUTTON) {
        CollectFormParameters params = 
            CollectUtil.CollectFormParameters
              .constructCollectFormParameters(c.getTableProperties());
        // now we have to do a bit of work to get the UserTable.
        // Since a query doesn't matter, we don't need to do anything
        // difficult to  get all the TableProperties, which is an expensive
        // method. However, it must be noted that this is a potentially
        // risky way to do it.
        Query query = new Query(new TableProperties[] {c.getTableProperties()},
            c.getTableProperties());
        query.loadFromUserQuery("");
        UserTable table = c.getIsOverview() ?
            c.getDbTable().getUserOverviewTable(query) :
              c.getDbTable().getUserTable(query);
        // now we need to get the row num.
        int rowNum = table.getRowNumFromId(rowId);
        // handle the case that it wasn't found, and do nothing
        if (rowNum == -1) {
          Toast.makeText(this.getApplicationContext(), "Row ID not found, " +
          		"please edit via Spreadsheet View", Toast.LENGTH_SHORT).show();
          return true;
        }
        c.editRow(table, rowNum, params);
//        Log.d(TAG, "clicked add row button");
        return true;
      }
        return c.handleMenuItemSelection(item);
    }
}
