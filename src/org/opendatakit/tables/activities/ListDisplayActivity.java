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

import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreManager;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.view.custom.CustomTableView;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * This class is responsible for the list view of a table.
 * 
 * SS: not sure who the original author is. Putting my tag on it because I'm
 * adding some things.
 * @author sudar.sam@gmail.com
 * @author unknown
 *
 */
public class ListDisplayActivity extends SherlockActivity 
    implements DisplayActivity {
  
  private static final String TAG = "ListDisplayActivity";
  /**************************
   * Strings necessary for the key value store.
   **************************/
  public static final String KVS_PARTITION = "ListDisplayActivity";
  
  /**
   * This is the default aspect for the list view. This should be all that is 
   * used until we allow multiple list views for a single file.
   */
  public static final String KVS_ASPECT_DEFAULT = "default";
  
  public static final String KEY_FILENAME = "filename";

    private static final int RCODE_ODKCOLLECT_ADD_ROW =
        Controller.FIRST_FREE_RCODE;
    
    private DataManager dm;
    private Controller c;
    private Query query;
    private UserTable table;
    private CustomTableView view;
    private DbHelper dbh;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("");
        dbh = DbHelper.getDbHelper(this);
        c = new Controller(this, this, getIntent().getExtras());
        dm = new DataManager(DbHelper.getDbHelper(this));
        // TODO: why do we get all table properties here? this is an expensive
        // call. I don't think we should do it.
        query = new Query(dm.getAllTableProperties(KeyValueStore.Type.ACTIVE), 
            c.getTableProperties());
        init();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        displayView();
    }
    
    @Override
    public void init() {
        query.clear();
        query.loadFromUserQuery(c.getSearchText());
        table = c.getIsOverview() ?
                c.getDbTable().getUserOverviewTable(query) :
                c.getDbTable().getUserTable(query);
        String filename = c.getTableProperties().getStringEntry(
            ListDisplayActivity.KVS_PARTITION,
            ListDisplayActivity.KVS_ASPECT_DEFAULT,
            ListDisplayActivity.KEY_FILENAME);
        KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
        KeyValueStore kvs = 
            kvsm.getStoreForTable(c.getTableProperties().getTableId(), 
            c.getTableProperties().getBackingStoreType());
//        view = CustomTableView.get(this, c.getTableProperties(), table,
//                c.getTableViewSettings().getCustomListFilename());
        view = CustomTableView.get(this, c.getTableProperties(), table,
            filename);
            
        displayView();
    }
    
    private void displayView() {
        view.display();
        c.setDisplayView(view);
        setContentView(c.getContainerView());
    }
    
    @Override
    public void onBackPressed() {
        c.onBackPressed();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (c.handleActivityReturn(requestCode, resultCode, data)) {
            return;
        }
        switch (requestCode) {
        case RCODE_ODKCOLLECT_ADD_ROW:
            c.addRowFromOdkCollectForm(
                    Integer.valueOf(data.getData().getLastPathSegment()));
            init();
            break;
        default:
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        c.buildOptionsMenu(menu);
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        return c.handleMenuItemSelection(item);
    }
    
    @Override
    public void onSearch() {
        c.recordSearch();
        init();
    }
}
