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
package org.opendatakit.tables.Activity;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.activities.ListDisplayActivity;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableViewType;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;

/**
 * This activity presents all the possible list views that might be displayed
 * for the given table.
 * <p>
 * It's general structure is modeled on the ColumnManager class, so that we keep
 * a standard feel throughout the app.
 * 
 * @author sudar.sam@gmail.com
 * 
 */
public class ListOfListViewsActivity extends SherlockListActivity {
  
  public static final String TAG = ListOfListViewsActivity.class.getName();
  
  public static final String INTENT_KEY_TABLE_ID = "tableId";
  private static final String ACTIVITY_TITLE = "List View Manager";
  
  /**
   * Menu ID for adding a new list view.
   */
  public static final int ADD_NEW_LIST_VIEW = 0;
  /**
   * The char sequence for the add new list view item.
   */
  public static final String ADD_NEW_LIST_VIEW_TEXT = "Add New List View";

  /**
   * This will be the names of all the possible list views.
   */
  private List<String> listViewNames;
  
  /**
   * This will be the adapter that handles displaying the actual rows as well
   * as presenting the menu to edit the entry.
   */
  private ListViewAdapter adapter;
  
  /**
   * The id of the table for which we are displaying the list views.
   */
  private String tableId;
  
  /**
   * The TableProperties of the table for which we are displaying the possible
   * list views
   */
  private TableProperties tp;
  
  /**
   * The KVS helper through which we will be getting the list view information.
   */
  private KeyValueStoreHelper kvsh;
  
  /*
   * Get the fields up and running. 
   */
  private void init() {
    this.tableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
    DbHelper dbh = DbHelper.getDbHelper(this);
    this.tp = TableProperties.getTablePropertiesForTable(dbh, tableId, 
        KeyValueStore.Type.ACTIVE);
    this.kvsh = 
        tp.getKeyValueStoreHelper(ListDisplayActivity.KVS_PARTITION_VIEWS);
    this.listViewNames = kvsh.getAspectsForPartition();
    Log.d(TAG, "listViewNames: " + listViewNames);
    // Set the adapter. It adds the list view itself.
    this.adapter = new ListViewAdapter();
    setListAdapter(adapter);
    // Now we need to get the list view that actually supports this object. It
    // is NOT THE SAME LISTVIEW AS THE "LIST VIEW" ON A TABLE. This is an
    // unfortunate namespace issue.
    ListView androidListView = getListView();
    // Set the click to open the given list view.
    androidListView.setOnItemClickListener(
        new AdapterView.OnItemClickListener() {
          /**
           * Click this, open the list view displaying the info you're after.
           */
          @Override
          public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, 
              long arg3) {
            // Since at the moment we are counting on the Controller class to
            // do the changing, we don't use the intent directly. If someone 
            // clicks on this view, that means they want to display the list
            // view using this activity. Further, it means that they want to
            // see the list view. To get this to work, we need to set the view
            // type to list view, and change the default list view to be this
            // one.
            tp.setCurrentViewType(TableViewType.List);
            // This will help us access keys for the general partition. (We 
            // need this to set this view as the default list view for the 
            // table.)
            KeyValueStoreHelper kvshListViewPartition = 
                tp.getKeyValueStoreHelper(ListDisplayActivity.KVS_PARTITION);
            // We need this to get the filename of the current list view.
            KeyValueHelper aspectHelper = 
                kvsh.getAspectHelper(listViewNames.get(arg2));
            String filenameOfSelectedView = 
                aspectHelper.getString(ListDisplayActivity.KEY_FILENAME);
            // Now we have the filename of the selected view. Add it to the kvs
            // in the appropriate place so controller will fetch it when it's
            // time to open that activity.
            kvshListViewPartition.setString(ListDisplayActivity.KEY_FILENAME, 
                filenameOfSelectedView);
            // Check if there are prime columns. If there are, then we're using
            // the collection view? This needs to be sorted out.
            // TODO: launch if something is a collection view correctly.
            // For example, right now there is an issue where you might be 
            // selecting a collection list view but you're not viewing the 
            // table with a prime column, or vice versa, and this could create
            // an issue.
            ArrayList<String> primeColumns = tp.getPrimeColumns();
            boolean isOverview;
            if (primeColumns == null || primeColumns.size() == 0) {
              isOverview = false;
            } else {
              isOverview = true;
            }
            Controller.launchTableActivity(ListOfListViewsActivity.this, tp, 
                isOverview);
          }
    });
  }
  
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(org.opendatakit.tables.R.layout.col_manager);
    setTitle(ACTIVITY_TITLE);
    // Set the app icon as an action to go home.
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
  }
  
  @Override
  public void onResume() {
    super.onResume();
    init();
  }
  
  @Override
  public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
    super.onCreateOptionsMenu(menu);
    com.actionbarsherlock.view.MenuItem addItem = menu.add(0, 
        ADD_NEW_LIST_VIEW, 0, ADD_NEW_LIST_VIEW_TEXT);
    addItem.setIcon(org.opendatakit.tables.R.drawable.content_new);
    addItem.setShowAsAction(
        com.actionbarsherlock.view.MenuItem.SHOW_AS_ACTION_ALWAYS);
    return true;
  }
  
  @Override 
  public boolean onMenuItemSelected(int featureId, 
      com.actionbarsherlock.view.MenuItem item) {
    switch (item.getItemId()) {
    case ADD_NEW_LIST_VIEW: 
      Toast.makeText(ListOfListViewsActivity.this, 
          "would have added new list view", Toast.LENGTH_SHORT).show();
      return true;
    case android.R.id.home:
      startActivity(new Intent(this, TableManager.class));
      return true;
    }
    return false;    
  }

  /**
   * This is the data structure that actually bears the list to be displayed to
   * the user, and handles the View creation of each element in the normal
   * Android Adapter way.
   * <p>
   * The general idea is that this class gives the icon necessary for viewing
   * the adapter and adding settings.
   * 
   * @author sudar.sam@gmail.com
   * 
   */
  class ListViewAdapter extends ArrayAdapter<String> {
    
    /**
     * Set this adapter to use the @listViewNames as its backing object.
     */
    ListViewAdapter() {
      super(ListOfListViewsActivity.this, 
          org.opendatakit.tables.R.layout.touchlistview_row2,
          listViewNames);
    }

    /**
     * The method responsible for getting the view that will represent an
     * element in the list. Since we're just displaying Strings, we must make
     * sure that the settings button generates the correct options for selecting
     * a new list view.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      // Per the Android programming contract, it's possible that we've
      // been handed an old view we need to convert to a new one. (This
      // is what the convertView param is.) So, try that.
      View row = convertView;
      // It's possible that we weren't handed one and have to construct
      // it up from scratch.
      if (row == null) {
        row = getLayoutInflater().inflate(
            org.opendatakit.tables.R.layout.touchlistview_row2,
            parent, false);
      }
      final int currentPosition = position;
      String listViewName = listViewNames.get(currentPosition);
      // Set the label of this row.
      TextView label = 
          (TextView) row.findViewById(org.opendatakit.tables.R.id.row_label);
      label.setText(listViewName);
      // We can ignore the "ext" TextView, as there's not at this point any
      // other information we wish to be displaying.
      TextView extraString = 
          (TextView) row.findViewById(org.opendatakit.tables.R.id.row_ext);
      extraString.setVisibility(View.GONE);
      // We also want to hide the drag and drop icon, as that doesn't do 
      // anything here. Eventually we might want to make our own layout to 
      // avoid having to GONE this stuff.
      View dragIcon = row.findViewById(org.opendatakit.tables.R.id.icon);
      dragIcon.setVisibility(View.GONE);
      // Instead we need to establish the behavior of what will happen when
      // we click the settings icon. Eventually we want to be able to edit the
      // name and change the file. For now we're not going to do that, as more
      // thought has to be given to actually writing up that activity.
      final ImageView editView = (ImageView) 
          row.findViewById(org.opendatakit.tables.R.id.row_options);
      // We'll set the click listener to just toast for now.
      editView.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          // We'll want to be able to edit the preferences.
          Intent editListViewIntent = new Intent(ListOfListViewsActivity.this,
              EditSavedListViewEntryActivity.class);
          editListViewIntent.putExtra(
              EditSavedListViewEntryActivity.INTENT_KEY_TABLE_ID, tableId);
          startActivity(editListViewIntent);
        }
      });
      // And now we're set, so just kick it on back.
      return row;
    }
  }

}
