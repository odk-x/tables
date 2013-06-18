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
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.KeyValueStoreHelper.AspectHelper;
import org.opendatakit.tables.data.KeyValueStoreManager;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.views.webkits.CustomTableView;

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

  /**
   * The filename the list view should be opened with. If not present, default
   * behavior might be to open the default file.
   */
  public static final String INTENT_KEY_FILENAME = "filename";

  /**************************
   * Strings necessary for the key value store.
   **************************/
  /**
   * The general partition in which table-wide ListDisplayActivity information
   * is stored. An example might be the current list view for a table.
   */
  public static final String KVS_PARTITION = "ListDisplayActivity";
  /**
   * The partition under which actual individual view information is stored.
   * For instance if a user added a list view named "Doctor", the partition
   * would be KVS_PARTITION_VIEWS, and all the keys relating to this view would
   * fall within this partition and a particular aspect. (Perhaps the name
   * "Doctor"?)
   */
  public static final String KVS_PARTITION_VIEWS = KVS_PARTITION + ".views";


  /**
   * This is the default aspect for the list view. This should be all that is
   * used until we allow multiple list views for a single file.
   */
  public static final String KVS_ASPECT_DEFAULT = "default";

  /**
   * This key holds the filename associated with the view.
   */
  public static final String KEY_FILENAME = "filename";

  /**
   * This key holds the name of the list view. In the default aspect the idea
   * is that this will then give the value of the aspect for which the default
   * list view is set.
   * <p>
   * E.g. partition=KVS_PARTITION, aspect=KVS_ASPECT_DEFAULT,
   * key="KEY_LIST_VIEW_NAME", value="My Custom List View" would mean that
   * "My Custom List View" was an aspect under the KVS_PARTITION_VIEWS
   * partition that had the information regarding a custom list view.
   */
  public static final String KEY_LIST_VIEW_NAME = "nameOfListView";

    private DataManager dm;
    private Controller c;
    private Query query;
    private UserTable table;
    private CustomTableView view;
    private DbHelper dbh;
    private KeyValueStoreHelper kvsh;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("");
        dbh = DbHelper.getDbHelper(this);
        c = new Controller(this, this, getIntent().getExtras());
        kvsh = c.getTableProperties().getKeyValueStoreHelper(KVS_PARTITION);
        dm = new DataManager(DbHelper.getDbHelper(this));
        // TODO: why do we get all table properties here? this is an expensive
        // call. I don't think we should do it.
        query = new Query(dm.getAllTableProperties(KeyValueStore.Type.ACTIVE),
            c.getTableProperties());
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

    @Override
    public void init() {
      // I hate having to do these two refreshes here, but with the code the
      // way it is it seems the only way.
      c.refreshDbTable();
        query.clear();
        query.loadFromUserQuery(c.getSearchText());
        table = c.getIsOverview() ?
                c.getDbTable().getUserOverviewTable(query) :
                c.getDbTable().getUserTable(query);
        String nameOfView =
            kvsh.getString(ListDisplayActivity.KEY_LIST_VIEW_NAME);
        // The nameOfView can be null in some cases, like if the default list
        // view has been deleted. If this ever occurs, we should just say no
        // filename specified and make them choose one.
        String filename =
            getIntent().getExtras().getString(INTENT_KEY_FILENAME);
        if (nameOfView != null) {
          if (filename == null) {
            KeyValueStoreHelper namedListViewsPartitionKvsh =
                c.getTableProperties().getKeyValueStoreHelper(
                    ListDisplayActivity.KVS_PARTITION_VIEWS);
            AspectHelper aspectHelper = kvsh.getAspectHelper(nameOfView);
            AspectHelper viewAspectHelper =
                namedListViewsPartitionKvsh.getAspectHelper(nameOfView);
            filename =
                viewAspectHelper.getString(ListDisplayActivity.KEY_FILENAME);
            KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
            KeyValueStore kvs =
                kvsm.getStoreForTable(c.getTableProperties().getTableId(),
                c.getTableProperties().getBackingStoreType());
          }
        }
        view = CustomTableView.get(this, c.getTableProperties(), table,
                filename, c);
        // change the info bar text IF necessary
        c.setListViewInfoBarText();
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
        } else {
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
