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
package org.opendatakit.hope.activities.graphs;

import java.util.List;

import org.opendatakit.hope.R;
import org.opendatakit.hope.activities.Controller;
import org.opendatakit.hope.activities.ListDisplayActivity;
import org.opendatakit.hope.activities.TablePropertiesManager;
import org.opendatakit.hope.data.DbHelper;
import org.opendatakit.hope.data.KeyValueHelper;
import org.opendatakit.hope.data.KeyValueStore;
import org.opendatakit.hope.data.KeyValueStoreHelper;
import org.opendatakit.hope.data.TableProperties;
import org.opendatakit.hope.data.TableViewType;
import org.opendatakit.hope.data.KeyValueStoreHelper.AspectHelper;

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
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

/**
 * This activity presents all the possible graph views that might be displayed
 * for the given table.
 * <p>
 * It's general structure is modeled on the ColumnManager class, so that we keep
 * a standard feel throughout the app.
 *
 * @author Nathan Brandes?
 *
 */
public class GraphDisplayActivity extends SherlockListActivity {

	public static final String TAG = GraphDisplayActivity.class.getName();

	public static final String INTENT_KEY_TABLE_ID = "tableId";
	public static final String INTENT_KEY_GRAPHVIEW_NAME = "graphViewName";


	/**
	 * Menu ID for adding a new list view.
	 */
	public static final int MENU_ITEM_ID_SEARCH_BUTTON = 1;

	public static final int ADD_NEW_GRAPH_VIEW = 0;

	/**
	 * Menu ID for deleting an entry.
	 */
	public static final int MENU_DELETE_ENTRY = 1;
	/**
	 * Menu ID for opening the edit entry activity.
	 */
	public static final int MENU_EDIT_ENTRY = 2;
	
	public static final int MENU_ITEM_ID_SETTINGS_SUBMENU = 3;
	public static final int MENU_ITEM_ID_OPEN_TABLE_PROPERTIES = 4;
	
	/**
	 * This will be the names of all the possible list views.
	 */
	private List<String> graphViewNames;

	/**
	 * This will be the adapter that handles displaying the actual rows as well
	 * as presenting the menu to edit the entry.
	 */
	private GraphViewAdapter adapter;

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
	 * A sql where clause that may have come from an intent. Needs to be
	 * forwarded to the display activity.
	 * @see DbTable#rawSqlQuery
	 */
	private String mSqlWhereClause;
	/**
	 * A String array of sql selection args that may have come from an intent.
	 * Needs to be forwarded to the display activity.
	 * @see DbTable#rawSqlQuery
	 */
	private String[] mSqlSelectionArgs;

	/*
	 * Get the fields up and running.
	 */
	private void init() {
		this.tableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
		this.mSqlWhereClause =
		    getIntent().getStringExtra(Controller.INTENT_KEY_SQL_WHERE);
		this.mSqlSelectionArgs = getIntent().getStringArrayExtra(
		    Controller.INTENT_KEY_SQL_SELECTION_ARGS);
		DbHelper dbh = DbHelper.getDbHelper(this);
		this.tp = TableProperties.getTablePropertiesForTable(dbh, tableId,
				KeyValueStore.Type.ACTIVE);
		this.kvsh =
				tp.getKeyValueStoreHelper(BarGraphDisplayActivity.KVS_PARTITION_VIEWS);
		this.graphViewKvsh =
				tp.getKeyValueStoreHelper(BarGraphDisplayActivity.KVS_PARTITION);
		this.defaultGraphViewName =
				graphViewKvsh.getString(BarGraphDisplayActivity.KEY_GRAPH_VIEW_NAME);
		this.graphViewNames = kvsh.getAspectsForPartition();
		Log.d(TAG, "graphViewNames: " + graphViewNames);
		// Set the adapter. It adds the list view itself.
		this.adapter = new GraphViewAdapter();
		setListAdapter(adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// Since at the moment we are counting on the Controller class to
		// do the changing, we don't use the intent directly. If someone
		// clicks on this view, that means they want to display the list
		// view using this activity. Further, it means that they want to
		// see the list view. To get this to work, we need to set the view
		// type to list view
		tp.setCurrentViewType(TableViewType.Graph);
		// This will help us access keys for the general partition. (We
		// need this to set this view as the default list view for the
		// table.)
		KeyValueStoreHelper kvshGraphViewPartition =
				tp.getKeyValueStoreHelper(BarGraphDisplayActivity.KVS_PARTITION);
		// We need this to get the filename of the current list view.
		KeyValueHelper aspectHelper =
				kvsh.getAspectHelper((String)
						getListView().getItemAtPosition(position));
		String filenameOfSelectedView =
				aspectHelper.getString(BarGraphDisplayActivity.GRAPH_TYPE);
		// Check if there are prime columns. If there are, then we're using
		// the collection view? This needs to be sorted out.
		// TODO: launch if something is a collection view correctly.
		// For example, right now there is an issue where you might be
		// selecting a collection list view but you're not viewing the
		// table with a prime column, or vice versa, and this could create
		// an issue.
		String graphName = (String)getListView().getItemAtPosition(position);
		Intent newGraphViewIntent = new Intent(this, BarGraphDisplayActivity.class);
		newGraphViewIntent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
		newGraphViewIntent.putExtra(BarGraphDisplayActivity.POTENTIAL_GRAPH_VIEW_NAME, getPotentialGraphName());
		newGraphViewIntent.putExtra(BarGraphDisplayActivity.KEY_GRAPH_VIEW_NAME, graphName);
		// Now put the sql ones if they exist.
		if (this.mSqlWhereClause != null) {
		  newGraphViewIntent.putExtra(Controller.INTENT_KEY_SQL_WHERE,
		      mSqlWhereClause);
		  newGraphViewIntent.putExtra(Controller.INTENT_KEY_SQL_SELECTION_ARGS,
		      mSqlSelectionArgs);
		}
		startActivity(newGraphViewIntent);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(org.opendatakit.hope.R.layout.graph_view_manager);
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
// TODO: HOPESTUDY_UI
//		final TableViewType[] viewTypes = tp.getPossibleViewTypes();
//		// 	  -build a checkable submenu to select the view type
//		SubMenu viewTypeSubMenu =
//				menu.addSubMenu(Menu.NONE, MENU_ITEM_ID_SEARCH_BUTTON,
//						Menu.NONE, getString(R.string.view_type));
//		MenuItem viewType = viewTypeSubMenu.getItem();
//		viewType.setIcon(R.drawable.view);
//		viewType.setEnabled(true);
//		viewType.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
//		MenuItem item;
//		// This will be the name of the default list view, which if exists
//		// means we should display the list view as an option.
//		KeyValueStoreHelper kvsh =
//				tp.getKeyValueStoreHelper(ListDisplayActivity.KVS_PARTITION);
//		String nameOfView = kvsh.getString(
//				ListDisplayActivity.KEY_LIST_VIEW_NAME);
//		for(int i = 0; i < viewTypes.length; i++) {
//			item = viewTypeSubMenu.add(MENU_ITEM_ID_SEARCH_BUTTON,
//					viewTypes[i].getId(), i,
//					viewTypes[i].name());
//			// mark the current viewType as selected
//			if (tp.getCurrentViewType() == viewTypes[i]) {
//				item.setChecked(true);
//			}
//			// disable list view if no file is specified
//			if (viewTypes[i] == TableViewType.List &&
//					nameOfView == null) {
//				item.setEnabled(false);
//			}
//		}
//
//		viewTypeSubMenu.setGroupCheckable(MENU_ITEM_ID_SEARCH_BUTTON,
//				true, true);
//
//
		MenuItem addItem = menu.add(Menu.NONE, ADD_NEW_GRAPH_VIEW,
				Menu.NONE,
				getString(R.string.add_new_graph)).setEnabled(true);
		addItem.setIcon(R.drawable.content_new);
		addItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		
      // Settings submenu
      SubMenu settings =
          menu.addSubMenu(Menu.NONE, MENU_ITEM_ID_SETTINGS_SUBMENU,
              Menu.NONE, getString(R.string.settings));
      MenuItem settingsItem = settings.getItem();
      settingsItem.setIcon(R.drawable.settings_icon2);
      settingsItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
      settings.add(Menu.NONE, MENU_ITEM_ID_OPEN_TABLE_PROPERTIES, Menu.NONE,
          getString(R.string.table_props)).setEnabled(true);

		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId,
			com.actionbarsherlock.view.MenuItem item) {

		if(item.getGroupId() == MENU_ITEM_ID_SEARCH_BUTTON) {
			tp.setCurrentViewType(
					TableViewType.getViewTypeFromId(item.getItemId()));
			Controller.launchTableActivity(this, tp, "", true, null, null);
			return true;
		}
		switch (item.getItemId()) {
		case MENU_ITEM_ID_SEARCH_BUTTON:

			return true;
		case ADD_NEW_GRAPH_VIEW:
			createNewGraph();
			return true;
      case MENU_ITEM_ID_OPEN_TABLE_PROPERTIES:
         {
         Intent intent = new Intent(this, TablePropertiesManager.class);
         intent.putExtra(TablePropertiesManager.INTENT_KEY_TABLE_ID,
                 tp.getTableId());
         startActivity(intent);
         }
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
						KeyValueStoreHelper generalViewHelper =
								tp.getKeyValueStoreHelper(BarGraphDisplayActivity.KVS_PARTITION);
						generalViewHelper.removeKey(
								BarGraphDisplayActivity.KEY_GRAPH_VIEW_NAME);
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
			Intent editGraphViewIntent = new Intent(GraphDisplayActivity.this,
					BarGraphDisplayActivity.class);
			editGraphViewIntent.putExtra(
					GraphDisplayActivity.INTENT_KEY_TABLE_ID, tableId);
			editGraphViewIntent.putExtra(
					BarGraphDisplayActivity.KEY_GRAPH_VIEW_NAME, entryName);
			editGraphViewIntent.putExtra(BarGraphDisplayActivity.POTENTIAL_GRAPH_VIEW_NAME, getPotentialGraphName());
			startActivity(editGraphViewIntent);
			return true;
		default:
			Log.e(TAG, "android MenuItem id not recognized: " + item.getItemId());
			return false;
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.add(0,MENU_DELETE_ENTRY, 0, getString(R.string.delete_graph));
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
			super(GraphDisplayActivity.this,
					org.opendatakit.hope.R.layout.touchlistview_row2,
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
	        row = getLayoutInflater().inflate(
	            org.opendatakit.hope.R.layout.row_for_edit_view_entry,
	            parent, false);
	      }
	      final int currentPosition = position;
	      final String listViewName = graphViewNames.get(currentPosition);
	      // Set the label of this row.
	      TextView label =
	          (TextView) row.findViewById(org.opendatakit.hope.R.id.row_label);
	      label.setText(listViewName);
	      // We can ignore the "ext" TextView, as there's not at this point any
	      // other information we wish to be displaying.
	      TextView extraString =
	          (TextView) row.findViewById(org.opendatakit.hope.R.id.row_ext);
	      AspectHelper aspectHelper = kvsh.getAspectHelper(listViewName);
	      String filename =
	          aspectHelper.getString(BarGraphDisplayActivity.GRAPH_TYPE);
	      extraString.setText(filename);
	      // The radio button showing whether or not this is the default list view.

	      // And now prepare the listener for the settings icon.
	      final ImageView editView = (ImageView)
	          row.findViewById(org.opendatakit.hope.R.id.row_options);
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
		Intent newGraphViewIntent = new Intent(this, BarGraphDisplayActivity.class);
		newGraphViewIntent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
		newGraphViewIntent.putExtra(BarGraphDisplayActivity.POTENTIAL_GRAPH_VIEW_NAME, getPotentialGraphName());
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



}
