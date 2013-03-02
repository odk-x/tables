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
import org.opendatakit.tables.data.KeyValueStoreHelper.AspectHelper;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableViewType;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
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
   * Menu ID for deleting an entry.
   */
  public static final int MENU_DELETE_ENTRY = 1;
  /**
   * Text for the entry deletion.
   */
  public static final String MENU_TEXT_DELETE_ENTRY = "Delete this List View";
  /**
   * Menu ID for opening the edit entry activity.
   */
  public static final int MENU_EDIT_ENTRY = 2;
  /**
   * Text for the entry editing.
   */
  public static final String MENU_TEXT_EDIT_ENTRY = "Edit this List View";

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
  
  /**
   * This is the name of the list view that is currently set to the default.
   */
  private String defaultListViewName;
  
  /**
   * This is the aspect helper for the general list view partition. This stands
   * opposed to the partition where the named views themselves reside.
   */
  private KeyValueStoreHelper listViewKvsh;
  
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
    this.listViewKvsh = 
        tp.getKeyValueStoreHelper(ListDisplayActivity.KVS_PARTITION);
    this.defaultListViewName = 
        listViewKvsh.getString(ListDisplayActivity.KEY_LIST_VIEW_NAME);
    this.listViewNames = kvsh.getAspectsForPartition();
    Log.d(TAG, "listViewNames: " + listViewNames);
    // Set the adapter. It adds the list view itself.
    this.adapter = new ListViewAdapter();
    setListAdapter(adapter);
  }
  
  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
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
        kvsh.getAspectHelper((String) 
            getListView().getItemAtPosition(position));
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
  
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(org.opendatakit.tables.R.layout.col_manager);
    setTitle(ACTIVITY_TITLE);
    // Set the app icon as an action to go home.
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    registerForContextMenu(getListView());
//    getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
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
      // If this is the case we need to launch the edit activity. 
      // The default name will just be some constant that changes when you
      // add new information.
      String baseName = "List View ";
      List<String> existingListViewNames = kvsh.getAspectsForPartition();
      int suffix = existingListViewNames.size();
      String potentialName = baseName + suffix;
      while (existingListViewNames.contains(potentialName)) {
        suffix++;
        potentialName = baseName + suffix;
      }
      Intent newListViewIntent = 
          new Intent(this, EditSavedListViewEntryActivity.class);
      newListViewIntent.putExtra(
          EditSavedListViewEntryActivity.INTENT_KEY_TABLE_ID, tableId);
      newListViewIntent.putExtra(
          EditSavedListViewEntryActivity.INTENT_KEY_LISTVIEW_NAME, 
          potentialName);
      startActivity(newListViewIntent);
      return true;
    case android.R.id.home:
      startActivity(new Intent(this, TableManager.class));
      return true;
    }
    return false;    
  }
  
  @Override
  public boolean onContextItemSelected(android.view.MenuItem item) {
    // We need this so we can get the position of the thing that was clicked.
    AdapterContextMenuInfo menuInfo = 
        (AdapterContextMenuInfo) item.getMenuInfo();
    final int position = menuInfo.position;
    final String entryName = 
        (String) getListView().getItemAtPosition(position);
    switch(item.getItemId()) {
    case MENU_DELETE_ENTRY:
      // Make an alert dialog that will give them the option to delete it or
      // cancel.
      AlertDialog confirmDeleteAlert;
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle("Delete " + entryName + "?");
      // For the OK action we want to actually delete this list view.
      builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        
        @Override
        public void onClick(DialogInterface dialog, int which) {
          // We need to delete the entry. First delete it in the key value 
          // store.
          AspectHelper aspectHelper = kvsh.getAspectHelper(entryName);
          aspectHelper.deleteAllEntriesInThisAspect();
          // Now remove it from the list view.
          listViewNames.remove(position);
          adapter.notifyDataSetChanged();
          // TODO:
          // There is a possibility that this was also the file set to be the
          // default for the table. Therefore a reference to it might remain
          // in the KVS. Not yet known what to do in this case. Also note that
          // if the default just stores the file name, it's possible that 
          // several views might share a filename and therefore we can't just 
          // delete based on filename. Maybe should move to a name being stored
          // as the entry rather than the filename for the default.
          
        }
      });
      
      builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        
        @Override
        public void onClick(DialogInterface dialog, int which) {
          // Canceled. Do nothing.
        }
      });
      confirmDeleteAlert = builder.create();
      confirmDeleteAlert.show();
      return true;
    case MENU_EDIT_ENTRY:
      menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
      Intent editListViewIntent = new Intent(ListOfListViewsActivity.this,
      EditSavedListViewEntryActivity.class);
      editListViewIntent.putExtra(
          EditSavedListViewEntryActivity.INTENT_KEY_TABLE_ID, tableId);
      editListViewIntent.putExtra(
          EditSavedListViewEntryActivity.INTENT_KEY_LISTVIEW_NAME, entryName);
      startActivity(editListViewIntent);
      return true;
    default:
      Log.e(TAG, "android MenuItem id not recognized: " + item.getItemId());
      return false;
    }
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, 
      ContextMenuInfo menuInfo) {
    menu.add(0,MENU_DELETE_ENTRY, 0, MENU_TEXT_DELETE_ENTRY);
    menu.add(0, MENU_EDIT_ENTRY, 0, MENU_TEXT_EDIT_ENTRY);
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
            org.opendatakit.tables.R.layout.row_for_edit_view_entry,
            parent, false);
      }
      final int currentPosition = position;
      final String listViewName = listViewNames.get(currentPosition);
      // Set the label of this row.
      TextView label = 
          (TextView) row.findViewById(org.opendatakit.tables.R.id.row_label);
      label.setText(listViewName);
      // We can ignore the "ext" TextView, as there's not at this point any
      // other information we wish to be displaying.
      TextView extraString = 
          (TextView) row.findViewById(org.opendatakit.tables.R.id.row_ext);
      AspectHelper aspectHelper = kvsh.getAspectHelper(listViewName);
      String filename = 
          aspectHelper.getString(ListDisplayActivity.KEY_FILENAME);
      extraString.setText(filename);
      // The radio button showing whether or not this is the default list view.
      final RadioButton radioButton = (RadioButton)
          row.findViewById(org.opendatakit.tables.R.id.radio_button);
      if (isDefault(listViewName)) {
        radioButton.setChecked(true);
      } else {
        radioButton.setChecked(false);
      }
      radioButton.setVisibility(View.VISIBLE);
      // Set the click listener to set as default.
      radioButton.setOnClickListener(new OnClickListener() {

        @Override
        public void onClick(View v) {
          if (isDefault(listViewName)) {
            // Already set to default, do nothing.
          } else {
            setToDefault(listViewName);
            radioButton.setChecked(true);
            Toast.makeText(getContext(), 
                listViewName + " has been set to default." , 
                Toast.LENGTH_SHORT).show();
          }
        }
        
      });
      // And now prepare the listener for the settings icon.
      final ImageView editView = (ImageView) 
          row.findViewById(org.opendatakit.tables.R.id.row_options);
      final View holderView = row;
      editView.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          // Open the context menu of the view, because that's where we're 
          // doing the logistics.
          holderView.showContextMenu();
        }
      });
      // And now we're set, so just kick it on back.
      return row;
    }
    
    /**
     * Return true if the passed in name is defined as the default list view
     * for this table.
     * <p>
     * Checks that the defaultListViewName might be null, and returns false in
     * this case as if it is null it hasn't been defined.
     * @param nameOfListView
     * @return
     */
    private boolean isDefault(String nameOfListView) {
      if (defaultListViewName == null) {
        return false;
      }
      if (defaultListViewName.equals(nameOfListView)) {
        return true;
      } else {
        return false;
      }
    }
    
    /**
     * Set the list view with name nameOfListView to be the default for this
     * table. Updates the KVS as well as the global field in the activity.
     * @param nameOfListView
     */
    private void setToDefault(String nameOfListView) {
      listViewKvsh.setString(ListDisplayActivity.KEY_LIST_VIEW_NAME, 
          nameOfListView);
      defaultListViewName = nameOfListView;
      adapter.notifyDataSetChanged();
    }
    
    
  }

}
