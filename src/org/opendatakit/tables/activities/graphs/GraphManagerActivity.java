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
package org.opendatakit.tables.activities.graphs;

import java.util.List;

import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.activities.ListDisplayActivity;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.KeyValueStoreHelper.AspectHelper;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableViewType;
import org.opendatakit.tables.utils.TableFileUtils;

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
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

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
public class GraphManagerActivity extends SherlockListActivity {

  public static final String TAG = GraphManagerActivity.class.getName();

  public static final String INTENT_KEY_GRAPHVIEW_NAME = "graphViewName";

  /**
   * Menu ID for changing to a different currentViewType.
   */
  public static final int MENU_ITEM_ID_VIEW_TYPE_SUBMENU = 1;

  /**
   * Menu ID for adding a new list view.
   */
  public static final int ADD_NEW_GRAPH_VIEW = 0;

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
  private List<String> graphViewNames;

  /**
   * This will be the adapter that handles displaying the actual rows as well as
   * presenting the menu to edit the entry.
   */
  private GraphViewAdapter adapter;

  /**
   * The appName we are running under.
   */
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
   * This is the name of the graph view that is currently set to the default.
   */
  private String defaultGraphViewName;

  /**
   * This is the aspect helper for the general list view partition. This stands
   * opposed to the partition where the named views themselves reside.
   */
  private KeyValueStoreHelper graphViewKvsh;

  /**
   * A sql where clause that may have come from an intent. Needs to be forwarded
   * to the display activity.
   *
   * @see DbTable#rawSqlQuery
   */
  private String mSqlWhereClause;
  /**
   * A String array of sql selection args that may have come from an intent.
   * Needs to be forwarded to the display activity.
   *
   * @see DbTable#rawSqlQuery
   */
  private String[] mSqlSelectionArgs;

  /*
   * Get the fields up and running.
   */
  private void init() {
    this.appName = getIntent().getStringExtra(Controller.INTENT_KEY_APP_NAME);
    if (appName == null) {
      this.appName = TableFileUtils.getDefaultAppName();
    }
    this.tableId = getIntent().getStringExtra(Controller.INTENT_KEY_TABLE_ID);
    this.mSqlWhereClause = getIntent().getStringExtra(Controller.INTENT_KEY_SQL_WHERE);
    this.mSqlSelectionArgs = getIntent().getStringArrayExtra(
        Controller.INTENT_KEY_SQL_SELECTION_ARGS);
    this.tp = TableProperties.getTablePropertiesForTable(this, appName, tableId);
    this.kvsh = tp.getKeyValueStoreHelper(GraphDisplayActivity.KVS_PARTITION_VIEWS);
    this.graphViewKvsh = tp.getKeyValueStoreHelper(GraphDisplayActivity.KVS_PARTITION);
    this.defaultGraphViewName = graphViewKvsh.getString(GraphDisplayActivity.KEY_GRAPH_VIEW_NAME);
    this.graphViewNames = kvsh.getAspectsForPartition();
    Log.d(TAG, "graphViewNames: " + graphViewNames);
    // Set the adapter. It adds the list view itself.
    this.adapter = new GraphViewAdapter();
    setListAdapter(adapter);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    // Check if there are group-by columns. If there are, then we're using
    // the collection view? This needs to be sorted out.
    // TODO: launch if something is a collection view correctly.
    // For example, right now there is an issue where you might be
    // selecting a collection list view but you're not viewing the
    // table with a group-by column, or vice versa, and this could create
    // an issue.
    String graphName = (String) getListView().getItemAtPosition(position);
    Intent newGraphViewIntent = new Intent(this, GraphDisplayActivity.class);
    newGraphViewIntent.putExtra(Controller.INTENT_KEY_APP_NAME, appName);
    newGraphViewIntent.putExtra(Controller.INTENT_KEY_TABLE_ID, tp.getTableId());
    newGraphViewIntent
        .putExtra(Controller.INTENT_KEY_CURRENT_VIEW_TYPE, TableViewType.Graph.name());
    newGraphViewIntent.putExtra(GraphDisplayActivity.POTENTIAL_GRAPH_VIEW_NAME,
        getPotentialGraphName());
    newGraphViewIntent.putExtra(GraphDisplayActivity.KEY_GRAPH_VIEW_NAME, graphName);
    // Now put the sql ones if they exist.
    if (this.mSqlWhereClause != null) {
      newGraphViewIntent.putExtra(Controller.INTENT_KEY_SQL_WHERE, mSqlWhereClause);
      newGraphViewIntent.putExtra(Controller.INTENT_KEY_SQL_SELECTION_ARGS, mSqlSelectionArgs);
    }
    // TODO: shouldn't this be start-activity-for-result?
    startActivity(newGraphViewIntent);
  }

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(org.opendatakit.tables.R.layout.graph_view_manager);
    setTitle(getString(R.string.graph_manager));
    // Set the app icon as an action to go home.
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    registerForContextMenu(getListView());
  }

  @Override
  public void onResume() {
    super.onResume();
    init();
  }

  @Override
  public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
    super.onCreateOptionsMenu(menu);

    final TableViewType[] viewTypes = tp.getPossibleViewTypes();
    // -build a checkable submenu to select the view type
    SubMenu viewTypeSubMenu = menu.addSubMenu(Menu.NONE, MENU_ITEM_ID_VIEW_TYPE_SUBMENU, Menu.NONE,
        getString(R.string.view_type));
    MenuItem viewType = viewTypeSubMenu.getItem();
    viewType.setIcon(R.drawable.view);
    viewType.setEnabled(true);
    viewType.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    MenuItem item;
    // This will be the name of the default list view, which if exists
    // means we should display the list view as an option.
    KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(ListDisplayActivity.KVS_PARTITION);
    String nameOfView = kvsh.getString(ListDisplayActivity.KEY_LIST_VIEW_NAME);
    for (int i = 0; i < viewTypes.length; i++) {
      item = viewTypeSubMenu.add(MENU_ITEM_ID_VIEW_TYPE_SUBMENU, viewTypes[i].getId(), i,
          viewTypes[i].name());
      // fake it: mark the Graph viewType as selected
      if (TableViewType.Graph == viewTypes[i]) {
        item.setChecked(true);
      }
      // disable list view if no file is specified
      if (viewTypes[i] == TableViewType.List && nameOfView == null) {
        item.setEnabled(false);
      }
    }

    viewTypeSubMenu.setGroupCheckable(MENU_ITEM_ID_VIEW_TYPE_SUBMENU, true, true);

    MenuItem addItem = menu.add(Menu.NONE, ADD_NEW_GRAPH_VIEW, Menu.NONE,
        getString(R.string.add_new_graph)).setEnabled(true);
    addItem.setIcon(R.drawable.content_new);
    addItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

    return true;
  }

  @Override
  public boolean onMenuItemSelected(int featureId, com.actionbarsherlock.view.MenuItem item) {

    if (item.getGroupId() == MENU_ITEM_ID_VIEW_TYPE_SUBMENU) {
      Controller.launchTableActivity(this, tp, TableViewType.getViewTypeFromId(item.getItemId()));
      return true;
    }
    switch (item.getItemId()) {
    case MENU_ITEM_ID_VIEW_TYPE_SUBMENU:
      // let system handle displaying the sub-menu
      return true;
    case ADD_NEW_GRAPH_VIEW:
      createNewGraph();
      return true;
    }
    return false;
  }

  @Override
  public boolean onContextItemSelected(android.view.MenuItem item) {
    // We need this so we can get the position of the thing that was clicked.
    AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
    final int position = menuInfo.position;
    final String entryName = (String) getListView().getItemAtPosition(position);
    switch (item.getItemId()) {
    case MENU_DELETE_ENTRY:
      // Make an alert dialog that will give them the option to delete it or
      // cancel.
      AlertDialog confirmDeleteAlert;
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle(getString(R.string.confirm_delete_graph));
      builder.setMessage(getString(R.string.are_you_sure_delete_graph, entryName));
      // For the OK action we want to actually delete this list view.
      builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
          // We need to delete the entry. First delete it in the key value
          // store.
          AspectHelper aspectHelper = kvsh.getAspectHelper(entryName);
          aspectHelper.deleteAllEntriesInThisAspect();
          if (entryName.equals(defaultGraphViewName)) {
            KeyValueStoreHelper generalViewHelper = tp
                .getKeyValueStoreHelper(GraphDisplayActivity.KVS_PARTITION);
            generalViewHelper.removeKey(GraphDisplayActivity.KEY_GRAPH_VIEW_NAME);
          }
          defaultGraphViewName = null;
          // Now remove it from the list view.
          graphViewNames.remove(position);
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
      Intent editGraphViewIntent = new Intent(GraphManagerActivity.this, GraphDisplayActivity.class);
      editGraphViewIntent.putExtra(Controller.INTENT_KEY_APP_NAME, appName);
      editGraphViewIntent.putExtra(Controller.INTENT_KEY_TABLE_ID, tableId);
      editGraphViewIntent.putExtra(GraphDisplayActivity.KEY_GRAPH_VIEW_NAME, entryName);
      editGraphViewIntent.putExtra(GraphDisplayActivity.POTENTIAL_GRAPH_VIEW_NAME,
          getPotentialGraphName());
      startActivity(editGraphViewIntent);
      return true;
    default:
      Log.e(TAG, "android MenuItem id not recognized: " + item.getItemId());
      return false;
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    menu.add(0, MENU_DELETE_ENTRY, 0, getString(R.string.delete_graph));
    menu.add(0, MENU_EDIT_ENTRY, 0, getString(R.string.edit_graph));
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
  class GraphViewAdapter extends ArrayAdapter<String> {

    /**
     * Set this adapter to use the @listViewNames as its backing object.
     */
    GraphViewAdapter() {
      super(GraphManagerActivity.this, org.opendatakit.tables.R.layout.touchlistview_row2,
          graphViewNames);
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
        row = getLayoutInflater().inflate(org.opendatakit.tables.R.layout.row_for_edit_view_entry,
            parent, false);
      }
      final int currentPosition = position;
      final String listViewName = graphViewNames.get(currentPosition);
      // Set the label of this row.
      TextView label = (TextView) row.findViewById(org.opendatakit.tables.R.id.row_label);
      label.setText(listViewName);
      // We can ignore the "ext" TextView, as there's not at this point any
      // other information we wish to be displaying.
      TextView extraString = (TextView) row.findViewById(org.opendatakit.tables.R.id.row_ext);
      AspectHelper aspectHelper = kvsh.getAspectHelper(listViewName);
      String filename = aspectHelper.getString(GraphDisplayActivity.GRAPH_TYPE);
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
               getString(R.string.set_as_default_graph_view, listViewName),
                Toast.LENGTH_SHORT).show();
          }
        }

      });

      // And now prepare the listener for the settings icon.
      final ImageView editView = (ImageView) row
          .findViewById(org.opendatakit.tables.R.id.row_options);
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
  }

  private void createNewGraph() {
    Intent newGraphViewIntent = new Intent(this, GraphDisplayActivity.class);
    newGraphViewIntent.putExtra(Controller.INTENT_KEY_APP_NAME, tp.getAppName());
    newGraphViewIntent.putExtra(Controller.INTENT_KEY_TABLE_ID, tp.getTableId());
    newGraphViewIntent.putExtra(GraphDisplayActivity.POTENTIAL_GRAPH_VIEW_NAME,
        getPotentialGraphName());
    startActivity(newGraphViewIntent);
  }

  private String getPotentialGraphName() {
    List<String> existingListViewNames = kvsh.getAspectsForPartition();
    int suffix = existingListViewNames.size();
    String potentialName = getString(R.string.generated_graph_name, suffix);
    while (existingListViewNames.contains(potentialName)) {
      suffix++;
      potentialName = getString(R.string.generated_graph_name, suffix);
    }
    return potentialName;
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
    if (defaultGraphViewName == null) {
      return false;
    }
    if (defaultGraphViewName.equals(nameOfListView)) {
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
    graphViewKvsh.setString(GraphDisplayActivity.KEY_GRAPH_VIEW_NAME,
        nameOfListView);
    defaultGraphViewName = nameOfListView;
    adapter.notifyDataSetChanged();
  }

  public static String getDefaultGraphName(TableProperties tp) {
    KeyValueStoreHelper graphViewKvsh = tp.getKeyValueStoreHelper(GraphDisplayActivity.KVS_PARTITION);
    return graphViewKvsh.getString(GraphDisplayActivity.KEY_GRAPH_VIEW_NAME);
  }
}
