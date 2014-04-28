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

import java.util.List;

import org.opendatakit.common.android.data.KeyValueHelper;
import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.common.android.data.KeyValueStoreHelper.AspectHelper;
import org.opendatakit.tables.R;
import org.opendatakit.tables.utils.TableFileUtils;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
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
public class ListViewManager extends ListActivity {

  public static final String TAG = ListViewManager.class.getName();

  /**
   * Menu ID for adding a new list view.
   */
  public static final int ADD_NEW_LIST_VIEW = 0;

  /**
   * Menu ID for deleting an entry.
   */
  public static final int MENU_DELETE_ENTRY = 1;
  /**
   * Menu ID for opening the edit entry activity.
   */
  public static final int MENU_EDIT_ENTRY = 2;

  /**
   * This will be the names of all the possible list views.
   */
  private List<String> listViewNames;

  /**
   * This will be the adapter that handles displaying the actual rows as well
   * as presenting the menu to edit the entry.
   */
  private ListViewAdapter adapter;

  private String appName;
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
    appName = getIntent().getStringExtra(Controller.INTENT_KEY_APP_NAME);
    if ( appName == null ) {
      appName = TableFileUtils.getDefaultAppName();
    }
    this.tableId = getIntent().getStringExtra(Controller.INTENT_KEY_TABLE_ID);
    this.tp = TableProperties.getTablePropertiesForTable(this, appName, tableId);
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
    // Check if there are group-by columns. If there are, then we're using
    // the collection view? This needs to be sorted out.
    // TODO: launch if something is a collection view correctly.
    // For example, right now there is an issue where you might be
    // selecting a collection list view but you're not viewing the
    // table with a group-by column, or vice versa, and this could create
    // an issue.
    Controller.launchTableActivityWithFilename(ListViewManager.this, tp,
        filenameOfSelectedView, TableViewType.List);
  }

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.list_view_manager);
    setTitle(getString(R.string.list_view_manager));
    // Set the app icon as an action to go home.
    ActionBar actionBar = getActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    registerForContextMenu(getListView());
  }

  @Override
  public void onResume() {
    super.onResume();
    init();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    MenuItem addItem = menu.add(0,
        ADD_NEW_LIST_VIEW, 0, getString(R.string.add_list_view));
    addItem.setIcon(R.drawable.content_new);
    addItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    return true;
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    switch (item.getItemId()) {
    case ADD_NEW_LIST_VIEW:
      // If this is the case we need to launch the edit activity.
      // The default name will just be some constant that changes when you
      // add new information.
      List<String> existingListViewNames = kvsh.getAspectsForPartition();
      int suffix = existingListViewNames.size();
      String potentialName = getString(R.string.nth_list_view_title, suffix);
      while (existingListViewNames.contains(potentialName)) {
        suffix++;
        potentialName = getString(R.string.nth_list_view_title, suffix);
      }
      Intent newListViewIntent =
          new Intent(this, EditSavedListViewEntryActivity.class);
      newListViewIntent.putExtra(
          Controller.INTENT_KEY_APP_NAME, tp.getAppName());
      newListViewIntent.putExtra(
          Controller.INTENT_KEY_TABLE_ID, tableId);
      newListViewIntent.putExtra(
          EditSavedListViewEntryActivity.INTENT_KEY_LISTVIEW_NAME,
          potentialName);
      startActivity(newListViewIntent);
      return true;
    case android.R.id.home:
      Intent i = new Intent(this, TableManager.class);
      i.putExtra(Controller.INTENT_KEY_APP_NAME, tp.getAppName());
      startActivity(i);
      return true;
    }
    return false;
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
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
      builder.setTitle(getString(R.string.confirm_delete_view_title));
      builder.setMessage(getString(R.string.are_you_sure_delete_view, entryName));
      // For the OK action we want to actually delete this list view.
      builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
          // We need to delete the entry. First delete it in the key value
          // store.
          AspectHelper aspectHelper = kvsh.getAspectHelper(entryName);
          aspectHelper.deleteAllEntriesInThisAspect();
          if (entryName.equals(defaultListViewName)) {
            KeyValueStoreHelper generalViewHelper =
                tp.getKeyValueStoreHelper(ListDisplayActivity.KVS_PARTITION);
            generalViewHelper.removeKey(
                ListDisplayActivity.KEY_LIST_VIEW_NAME);
          }
          defaultListViewName = null;
          // Now remove it from the list view.
          listViewNames.remove(position);
          adapter.notifyDataSetChanged();
        }
      });

      builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {

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
      Intent editListViewIntent = new Intent(ListViewManager.this,
      EditSavedListViewEntryActivity.class);
      editListViewIntent.putExtra(
          Controller.INTENT_KEY_APP_NAME, tp.getAppName());
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
    menu.add(0,MENU_DELETE_ENTRY, 0, getString(R.string.delete_list_view));
    menu.add(0, MENU_EDIT_ENTRY, 0, getString(R.string.edit_list_view));
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
      super(ListViewManager.this,
          R.layout.touchlistview_row2,
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
        row = getLayoutInflater().inflate(R.layout.row_for_edit_view_entry,
            parent, false);
      }
      final int currentPosition = position;
      final String listViewName = listViewNames.get(currentPosition);
      // Set the label of this row.
      TextView label =
          (TextView) row.findViewById(R.id.row_label);
      label.setText(listViewName);
      // We can ignore the "ext" TextView, as there's not at this point any
      // other information we wish to be displaying.
      TextView extraString =
          (TextView) row.findViewById(R.id.row_ext);
      AspectHelper aspectHelper = kvsh.getAspectHelper(listViewName);
      String filename =
          aspectHelper.getString(ListDisplayActivity.KEY_FILENAME);
      extraString.setText(filename);
      // The radio button showing whether or not this is the default list view.
      final RadioButton radioButton = (RadioButton)
          row.findViewById(R.id.radio_button);
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
            	getString(R.string.set_as_default_list_view, listViewName),
                Toast.LENGTH_SHORT).show();
          }
        }

      });
      // And now prepare the listener for the settings icon.
      final ImageView editView = (ImageView)
          row.findViewById(R.id.row_options);
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
