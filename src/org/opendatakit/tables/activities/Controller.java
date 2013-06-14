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
import java.util.Stack;

import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.graphs.GraphDisplayActivity;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DataUtil;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableViewType;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.utils.CollectUtil;
import org.opendatakit.tables.utils.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.views.CellValueView;
import org.opendatakit.tables.views.ClearableEditText;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

/**
 * A controller for the elements common to the various table display activities.
 *
 * The general weirdness of how this package is structured (i.e., a Controller
 * class used by unrelated display activities, instead of just having those
 * display activities subclass a common parent) is because the Google Maps API
 * requires that activities that use MapViews extend the Android MapActivity
 * (meaning that the MapDisplayActivity couldn't extend the common display
 * activity unless the common display activity extended the Android MapActivity
 * class, which seemed undesirable since that would require that all of the
 * display activities be children of MapActivity for no good reason).
 */
public class Controller {

  private static final String TAG = "Controller";

    public static final String INTENT_KEY_TABLE_ID = "tableId";
    public static final String INTENT_KEY_SEARCH = "search";
    public static final String INTENT_KEY_SEARCH_STACK = "searchStack";
    public static final String INTENT_KEY_IS_OVERVIEW = "isOverview";

    public static final int VIEW_ID_SEARCH_FIELD = 0;
    public static final int VIEW_ID_SEARCH_BUTTON = 1;

    private static final int MENU_ITEM_ID_SEARCH_BUTTON = 0;
    private static final int MENU_ITEM_ID_VIEW_TYPE_SUBMENU = 1;
    // The add row button serves as an edit row button in DetailDisplayActivity
	public static final int MENU_ITEM_ID_ADD_ROW_BUTTON = 2;
	private static final int MENU_ITEM_ID_SETTINGS_SUBMENU = 3;
	// Display preferences is used differently in SpreadsheetDisplayActivity
	public static final int MENU_ITEM_ID_DISPLAY_PREFERENCES = 4;
	private static final int MENU_ITEM_ID_OPEN_TABLE_PROPERTIES = 5;
    private static final int MENU_ITEM_ID_OPEN_COLUMN_MANAGER = 6;
   private static final int MENU_ITEM_ID_OPEN_LIST_VIEW_MANAGER = 7;
    static final int FIRST_FREE_MENU_ITEM_ID = 8;

    private static final int RCODE_TABLE_PROPERTIES_MANAGER = 0;
    private static final int RCODE_COLUMN_MANAGER = 1;
    public static final int RCODE_ODKCOLLECT_ADD_ROW = 2;
    public static final int RCODE_ODKCOLLECT_EDIT_ROW = 3;
    private static final int RCODE_LIST_VIEW_MANAGER = 4;
    /**
     * This is the return code for when Collect is called to add a row to a
     * table that is not the table held by the activity at the time of the
     * call. E.g. Controller holds a TableProperties for a table that is
     * currently being displayed. If we are in a list view, and the user wants
     * to add a row to a table other than the table currently being displayed
     * in the list view, this is the return code that should be used. The
     * caller also must be sure to launch the intent using
     * {@link CollectUtil#launchCollectToAddRow(Activity, Intent,
     * TableProperties)}.
     */
    public static final int RCODE_ODK_COLLECT_ADD_ROW_SPECIFIED_TABLE = 5;
    public static final int FIRST_FREE_RCODE = 6;

    private final DataUtil du;
    private final SherlockActivity activity;
    private final DisplayActivity da;
    private final DataManager dm;
    private TableProperties tp;
    private DbTable dbt;
    private final Stack<String> searchText;
    private final boolean isOverview;
    private final RelativeLayout container;
    private LinearLayout controlWrap;
    private ClearableEditText searchField;
    private TextView infoBar;
    private final ViewGroup displayWrap;
    private View overlay;
    private RelativeLayout.LayoutParams overlayLp;

    public Controller(SherlockActivity activity, final DisplayActivity da,
            Bundle intentBundle) {
        du = DataUtil.getDefaultDataUtil();
        this.activity = activity;
        this.da = da;
        // getting intent information
        String tableId = intentBundle.getString(INTENT_KEY_TABLE_ID);
        if (tableId == null) {
            throw new RuntimeException();
        }
        searchText = new Stack<String>();
        if (intentBundle.containsKey(INTENT_KEY_SEARCH_STACK)) {
            String[] searchValues = intentBundle.getStringArray(
                    INTENT_KEY_SEARCH_STACK);
            for (String searchValue : searchValues) {
                searchText.add(searchValue);
            }
        } else {
            String initialSearchText = intentBundle.getString(
                    INTENT_KEY_SEARCH);
            searchText.add((initialSearchText == null) ? "" :
                initialSearchText);
        }
        isOverview = intentBundle.getBoolean(INTENT_KEY_IS_OVERVIEW, false);
        // initializing data objects
        dm = new DataManager(DbHelper.getDbHelper(activity));
        tp = dm.getTableProperties(tableId, KeyValueStore.Type.ACTIVE);
        dbt = dm.getDbTable(tableId);

        // INITIALIZING VIEW OBJECTS
        // controlWrap will hold the search bar and search button
        controlWrap = new LinearLayout(activity);
        // searchField is the search bar
        searchField = new ClearableEditText(activity);
        // displayWrap holds the spreadsheet/listView/etc
        displayWrap = new LinearLayout(activity);
        // container holds the entire view of the activity
        container = new RelativeLayout(activity);

        // BUILD VIEW OBJECTS
        // controlWrap is initialized to be hidden. clicking Action Item, search,
        // will show/hide it
        searchField.setId(VIEW_ID_SEARCH_FIELD);
        searchField.getEditText().setText(searchText.peek());
        ImageButton searchButton = new ImageButton(activity);
        searchButton.setId(VIEW_ID_SEARCH_BUTTON);
        searchButton.setImageResource(R.drawable.ic_action_search);
        searchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
              // when you click the search button, save that query.
              KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(
                  TableProperties.KVS_PARTITION);
              kvsh.setString(
                  TableProperties.KEY_CURRENT_QUERY,
                  searchField.getEditText().getText().toString());
                da.onSearch();
            }
        });
        LinearLayout.LayoutParams searchFieldParams =
                new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        searchFieldParams.weight = 1;
        controlWrap.addView(searchField, searchFieldParams);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.weight = 0;
        controlWrap.addView(searchButton, buttonParams);
        controlWrap.setVisibility(View.GONE);

        // info bar currently displays just the name of the table
        infoBar = new TextView(activity);
        setSimpleInfoBarText();
        infoBar.setBackgroundColor(Color.parseColor("#B0B0B0"));
        infoBar.setTextColor(Color.BLACK);

        LinearLayout wrapper = new LinearLayout(activity);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams controlParams =
                new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        wrapper.addView(controlWrap, controlParams);
        wrapper.addView(infoBar, controlParams);

        LinearLayout.LayoutParams displayParams =
                new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        wrapper.addView(displayWrap, displayParams);
        container.addView(wrapper, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public void setSimpleInfoBarText() {
        infoBar.setText(activity.getString(R.string.info_bar_plain_title, tp.getDisplayName()));
    }

    public void setListViewInfoBarText() {
        infoBar.setText(activity.getString(R.string.info_bar_list_title, tp.getDisplayName()));
    }

    public void setDetailViewInfoBarText() {
        infoBar.setText(activity.getString(R.string.info_bar_detail_title, tp.getDisplayName()));
    }

    public void setGraphViewInfoBarText(String graphName) {
        infoBar.setText(activity.getString(R.string.info_bar_graph_title, tp.getDisplayName(), graphName));
    }

    /**
     * @return the current text in info bar
     */
    public String getInfoBarText() {
    	return infoBar.getText().toString();
    }

    /**
     * @return TableProperties properties of this table
     */
  public TableProperties getTableProperties() {
    return tp;
  }

  /**
   * Update the dbTable that Controller is monitoring. This should be called
   * only
   * if there is no way to update the dbTable held by the
   * Controller if a change happens outside of the Controller's realm of
   * control. For instance, changing a column display name in PropertyManager
   * does not get updated to the dbTable without calling this method. This is
   * a messy way of doing things, and a refactor should probably end up fixing
   * this.
   */
  public void refreshDbTable() {
    this.dbt = dm.getDbTable(tp.getTableId());
  }

  /**
   * @return DbTable this data table
   */
  public DbTable getDbTable() {
    tp.refreshColumns();
    return dbt;
  }

  /**
   * @return True if this is an overview type, false if this is
   *         collection view type
   */
  public boolean getIsOverview() {
      return isOverview;
  }

  /**
   * @return String text currently in the search bar
   */
  public String getSearchText() {
    return searchText.peek();
  }

  /**
   * @return the view generated for this
   */
  public View getContainerView() {
    return container;
  }

  public void setDisplayView(View dv) {
    displayWrap.removeAllViews();
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
    displayWrap.addView(dv, params);
  }

  void addOverlay(View overlay, int width, int height, int x, int y) {
    removeOverlay();
    this.overlay = overlay;
    overlayLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    overlayLp.leftMargin = x;
    overlayLp.topMargin = y - activity.getSupportActionBar().getHeight() -
        infoBar.getHeight();
    overlayLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
    container.addView(overlay, overlayLp);
  }

  void removeOverlay() {
    if (overlay != null) {
      container.removeView(overlay);
      overlay = null;
      overlayLp = null;
    }
  }

  void setOverlayLocation(int x, int y) {
    overlayLp.leftMargin = x;
    overlayLp.topMargin = y - activity.getSupportActionBar().getHeight() -
        infoBar.getHeight();
    container.requestLayout();
  }

  void releaseView(View v) {
    displayWrap.removeView(v);
  }

  boolean isInSearchBox(int x, int y) {
    Log.d("CNTRLR", "isInSearchBox(" + x + "," + y + ")");
    y -= controlWrap.getHeight();
    Rect bounds = new Rect();
    searchField.getHitRect(bounds);
    Log.d("CNTRLR", bounds.toString());
    return ((bounds.left <= x) && (bounds.right >= x) && (bounds.top <= y) && (bounds.bottom >= y));
  }

  /**
   * This is used to invert the color of the search box. The boolean parameter
   * specifies whether or not the color should be inverted or returned to
   * normal.
   * <p>
   * The inversion values
   * are not tied to any particular theme, but are set using the
   * ActionBarSherlock themes. These need to change if the app themes are
   * changed.
   * @param invert
   */
  void invertSearchBoxColor(boolean invert) {
    if (invert) {
      searchField.setBackgroundResource(R.color.abs__background_holo_light);
      searchField.getEditText().setTextColor(
          searchField.getContext().getResources()
          .getColor(R.color.abs__background_holo_dark));
      searchField.getClearButton()
        .setBackgroundResource(R.drawable.content_remove_dark);
    } else {
      searchField.setBackgroundResource(R.color.abs__background_holo_dark);
      searchField.getEditText().setTextColor(searchField.getContext()
          .getResources()
          .getColor(R.color.abs__background_holo_light));
      searchField.getClearButton()
        .setBackgroundResource(R.drawable.content_remove_light);
    }
  }

  void appendToSearchBoxText(String text) {
    searchField.getEditText().setText((
        searchField.getEditText().getText() + text).trim());
  }

  public void recordSearch() {
    searchText.add(searchField.getEditText().getText().toString());
  }

  public void onBackPressed() {
    if (searchText.size() == 1) {
      activity.finish();
    } else {
      searchText.pop();
      searchField.getEditText().setText(searchText.peek());
      da.init();
    }
  }

  /**
   * This should launch Collect to edit the data for the row. If there is a
   * custom form defined for the table, its info should be loaded in params.
   * If the formId in params is null, then the default form is generated, which
   * is just every column with its own entry field on a single screen.
   * @param table
   * @param rowNum
   * @param params
   */
  /*
   * Examples for how this is done elsewhere can be found in:
   * Examples for how this is done in Collect can be found in the Collect code
   * in org.odk.collect.android.tasks.SaveToDiskTask.java, in the
   * updateInstanceDatabase() method.
   */
  void editRow(UserTable table, int rowNum) {
	Map<String, String> elementKeyToValue = new HashMap<String, String>();
	for (ColumnProperties cp : tp.getColumns().values()) {
	  String value = table.getData(rowNum,
	      tp.getColumnIndex(cp.getElementKey()));
	  elementKeyToValue.put(cp.getElementKey(), value);
	}

	Intent intent = CollectUtil.getIntentForOdkCollectEditRow(activity, tp, elementKeyToValue,
          null, null, null, table.getRowId(rowNum), table.getInstanceName(rowNum));

	if (intent != null) {
      CollectUtil.launchCollectToEditRow(activity, intent,
          table.getRowId(rowNum));
    } else {
      Log.e(TAG, "intent null when trying to create for edit row.");
    }
  }

  public boolean handleActivityReturn(int requestCode, int returnCode,
      Intent data) {
    switch (requestCode) {
    case RCODE_TABLE_PROPERTIES_MANAGER:
      handleTablePropertiesManagerReturn();
      return true;
    case RCODE_COLUMN_MANAGER:
      handleColumnManagerReturn();
      return true;
    case RCODE_ODKCOLLECT_ADD_ROW:
      handleOdkCollectAddReturn(returnCode, data);
      return true;
    case RCODE_ODKCOLLECT_EDIT_ROW:
      handleOdkCollectEditReturn(returnCode, data);
      return true;
    case RCODE_LIST_VIEW_MANAGER:
      handleListViewManagerReturn();
      return true;
    case RCODE_ODK_COLLECT_ADD_ROW_SPECIFIED_TABLE:
      handleOdkCollectAddReturnForSpecificTable(returnCode, data);
      return true;
    default:
      return false;
    }
  }

  private void handleListViewManagerReturn() {
    tp = dm.getTableProperties(tp.getTableId(), KeyValueStore.Type.ACTIVE);
    dbt = dm.getDbTable(tp.getTableId());
    da.init();
  }

  private void handleTablePropertiesManagerReturn() {
    // so for now I think that the boolean of whether or not the current view
    // is an overview of a collection view is stored here in Controller.
    // This should eventually move, if we decide to keep this architecture. but
    // for now I'm going to just hardcode in a solution.
    TableViewType oldViewType = tp.getCurrentViewType();
    tp = dm.getTableProperties(tp.getTableId(), KeyValueStore.Type.ACTIVE);
    dbt = dm.getDbTable(tp.getTableId());
    if (oldViewType == tp.getCurrentViewType()) {
      da.init();
    } else {
      launchTableActivity(activity, tp, searchText, isOverview);
    }
  }

  private void handleColumnManagerReturn() {
    tp = dm.getTableProperties(tp.getTableId(), KeyValueStore.Type.ACTIVE);
    dbt = dm.getDbTable(tp.getTableId());
    da.init();
  }

  void deleteRow(String rowId) {
    dbt.markDeleted(rowId);
  }

    /**
     * Builds the option menu (menus in the action bar and overflow)
     * with menu items enabled
     * @param menu Menu
     */
    public void buildOptionsMenu(Menu menu) {
    	this.buildOptionsMenu(menu, true);
    }

    /**
     * Builds the option menu (menus in the action bar and overflow)
     * Menu items can be enabled (true) or disabled (false)
     * @param menu Menu
     * @param enabled boolean
     */
    void buildOptionsMenu(Menu menu, boolean enabled) {
		// set the app icon as an action to go home
    	ActionBar actionBar = activity.getSupportActionBar();
    	actionBar.setDisplayHomeAsUpEnabled(true);

        // search
        MenuItem searchItem = menu.add(Menu.NONE, MENU_ITEM_ID_SEARCH_BUTTON, Menu.NONE,
                activity.getString(R.string.search));
        searchItem.setIcon(R.drawable.ic_action_search);
        searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        searchItem.setEnabled(enabled);


        // view type submenu
        // 	  -determine the possible view types
        final TableViewType[] viewTypes = tp.getPossibleViewTypes();
        // 	  -build a checkable submenu to select the view type
        SubMenu viewTypeSubMenu =
            menu.addSubMenu(Menu.NONE, MENU_ITEM_ID_VIEW_TYPE_SUBMENU,
                Menu.NONE, activity.getString(R.string.view_type));
        MenuItem viewType = viewTypeSubMenu.getItem();
        viewType.setIcon(R.drawable.view);
        viewType.setEnabled(enabled);
        viewType.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        MenuItem item;
        // This will be the name of the default list view, which if exists
        // means we should display the list view as an option.
        KeyValueStoreHelper kvsh =
            tp.getKeyValueStoreHelper(ListDisplayActivity.KVS_PARTITION);
        String nameOfView = kvsh.getString(
            ListDisplayActivity.KEY_LIST_VIEW_NAME);
        for(int i = 0; i < viewTypes.length; i++) {
        	item = viewTypeSubMenu.add(MENU_ITEM_ID_VIEW_TYPE_SUBMENU,
        	    viewTypes[i].getId(), i,
        	    viewTypes[i].name());
        	// mark the current viewType as selected
          	if (tp.getCurrentViewType() == viewTypes[i]) {
          	  item.setChecked(true);
          	}
            // disable list view if no file is specified
            if (viewTypes[i] == TableViewType.List &&
                nameOfView == null) {
               item.setEnabled(false);
            }
        }


        viewTypeSubMenu.setGroupCheckable(MENU_ITEM_ID_VIEW_TYPE_SUBMENU,
            true, true);

        // Add Row
        MenuItem addItem = menu.add(Menu.NONE, MENU_ITEM_ID_ADD_ROW_BUTTON,
            Menu.NONE,
              activity.getString(R.string.add_row)).setEnabled(enabled);
        addItem.setIcon(R.drawable.content_new);
        addItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        // Settings submenu
        SubMenu settings =
            menu.addSubMenu(Menu.NONE, MENU_ITEM_ID_SETTINGS_SUBMENU,
                Menu.NONE, activity.getString(R.string.settings));
        MenuItem settingsItem = settings.getItem();
        settingsItem.setIcon(R.drawable.settings_icon2);
        settingsItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        MenuItem display =
            settings.add(Menu.NONE, MENU_ITEM_ID_DISPLAY_PREFERENCES,
                Menu.NONE,
        		activity.getString(R.string.display_prefs)).setEnabled(enabled);
        // always disable DisplayPreferences if it is currently in list view
        if (tp.getCurrentViewType() == TableViewType.List)
        	display.setEnabled(false);
        settings.add(Menu.NONE, MENU_ITEM_ID_OPEN_TABLE_PROPERTIES, Menu.NONE,
    			activity.getString(R.string.table_props)).setEnabled(enabled);
        settings.add(Menu.NONE, MENU_ITEM_ID_OPEN_COLUMN_MANAGER, Menu.NONE,
              activity.getString(R.string.column_manager)).setEnabled(enabled);
        // Now an option for editing list views.
        MenuItem manageListViews =
            settings.add(Menu.NONE, MENU_ITEM_ID_OPEN_LIST_VIEW_MANAGER,
                Menu.NONE, activity.getString(R.string.list_view_manager)).setEnabled(true);
    }

    /**
     * Handle menu item that was selected by user
     * @param selectedItem MenuItem
     * @return true if selectedItem was handled
     */
	public boolean handleMenuItemSelection(MenuItem selectedItem) {
		int itemId = selectedItem.getItemId();
		// if the item is part of the sub-menu for view type, set the view type with its itemId
	    // else, handle accordingly
		if(selectedItem.getGroupId() == MENU_ITEM_ID_VIEW_TYPE_SUBMENU) {
		  tp.setCurrentViewType(
		      TableViewType.getViewTypeFromId(selectedItem.getItemId()));
		  Controller.launchTableActivity(activity, tp, searchText, isOverview);
        return true;
		} else {
	        switch (itemId) {
	        case MENU_ITEM_ID_SEARCH_BUTTON:
	        	int visible = controlWrap.getVisibility();
	        	if (visible == View.GONE)
	        		controlWrap.setVisibility(View.VISIBLE);
	        	else
	        		controlWrap.setVisibility(View.GONE);
	        	return true;
	        case MENU_ITEM_ID_VIEW_TYPE_SUBMENU:
	        	return true;
	        case MENU_ITEM_ID_ADD_ROW_BUTTON:
	          CollectFormParameters params =
	            CollectUtil.CollectFormParameters
	              .constructCollectFormParameters(tp);
	          // Try to construct the values currently in the search bar to
	          // prepopulate with the form. We're going to ignore joins. This
	          // means that if there IS a join column, we'll throw an error!!!
	          // So be careful.
	          Intent intentAddRow = CollectUtil.getIntentForOdkCollectAddRowByQuery(
	        		  activity, tp, params, getSearchText());

	          if (intentAddRow != null) {
	              Controller.this.activity.startActivityForResult(intentAddRow,
	                      RCODE_ODKCOLLECT_ADD_ROW);
	          }
	          return true;
	        case MENU_ITEM_ID_SETTINGS_SUBMENU:
	        	return true;
	        case MENU_ITEM_ID_DISPLAY_PREFERENCES:
	        	Intent k = new Intent(activity, DisplayPrefsActivity.class);
	        	k.putExtra(DisplayPrefsActivity.INTENT_KEY_TABLE_ID, tp.getTableId());

			    activity.startActivity(k);
	        	return true;
	        case MENU_ITEM_ID_OPEN_TABLE_PROPERTIES:
	            {
	            Intent intent = new Intent(activity, TablePropertiesManager.class);
	            intent.putExtra(TablePropertiesManager.INTENT_KEY_TABLE_ID,
	                    tp.getTableId());
	            activity.startActivityForResult(intent,
	                    RCODE_TABLE_PROPERTIES_MANAGER);
	            }
	            return true;
	        case MENU_ITEM_ID_OPEN_COLUMN_MANAGER:
	            {
	            Intent intent = new Intent(activity, ColumnManager.class);
	            intent.putExtra(ColumnManager.INTENT_KEY_TABLE_ID,
	                    tp.getTableId());
	            activity.startActivityForResult(intent, RCODE_COLUMN_MANAGER);
	            }
	            return true;
	        case MENU_ITEM_ID_OPEN_LIST_VIEW_MANAGER:
	          {
	          Intent intent =
	              new Intent(activity, ListViewManager.class);
	          intent.putExtra(ListViewManager.INTENT_KEY_TABLE_ID,
	              tp.getTableId());
	          activity.startActivityForResult(intent, RCODE_LIST_VIEW_MANAGER);
	          }
	          return true;
	        case android.R.id.home:
	          Intent tableManagerIntent = new Intent(activity,
	              TableManager.class);
	          // Add this flag so that you don't back from TableManager back
	          // into the table.
	          tableManagerIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	          activity.startActivity(tableManagerIntent);
	            return true;
	        default:
	            return false;
	        }
        }
  }

    private void handleOdkCollectAddReturn(int returnCode, Intent data) {
        if (!CollectUtil.handleOdkCollectAddReturn(activity, tp, returnCode,
            data)) {
          return;
        } else {
          // the add succeeded.
          da.init();
        }
    }

    private void handleOdkCollectEditReturn(int returnCode, Intent data) {
      if (!CollectUtil.handleOdkCollectEditReturn(activity, tp, returnCode,
          data)) {
        return;
      } else {
        // The update succeeded.
        da.init();
      }
    }

    /**
     * Handle the add return from Collect if the user has specified a table
     * other than that which is currently held in the Controller. Note that the
     * Intent to launch Collect must have been launched using {@link
     * CollectUtil#launchCollectToAddRow(Activity, Intent, TableProperties)}.
     * @param returnCode
     * @param data
     */
    private void handleOdkCollectAddReturnForSpecificTable(int returnCode,
        Intent data) {
      String tableId = CollectUtil.retrieveAndRemoveTableIdForAddRow(activity);
      if (tableId == null) {
        Log.e(TAG, "return from ODK Collect expected to find a tableId " +
        		"specifying the target of the add row, but was null.");
        return;
      }
      TableProperties tpToReceiveAdd =
          TableProperties.getTablePropertiesForTable(
              DbHelper.getDbHelper(activity), tableId,
              KeyValueStore.Type.ACTIVE);
      CollectUtil.handleOdkCollectAddReturn(activity, tpToReceiveAdd,
          returnCode, data);
    }

  void openCellEditDialog(String rowId, String value, int colIndex) {
    (new CellEditDialog(rowId, value, colIndex)).show();
  }

  public static void launchTableActivity(Context context, TableProperties tp,
      boolean isOverview) {
    Controller.launchTableActivity(context, tp, null, null, isOverview, null);
  }

  public static void launchTableActivity(Context context, TableProperties tp,
      String searchText, boolean isOverview) {
    Controller.launchTableActivity(context, tp, searchText, null, isOverview,
        null);
  }

  private static void launchTableActivity(Activity context, TableProperties tp,
      Stack<String> searchStack, boolean isOverview) {
    Controller.launchTableActivity(context, tp, null, searchStack, isOverview,
        null);
     context.finish();
  }

  /**
   * This is based on the other launch table activity methods. This one,
   * however, allows a filename to be passed to the launching activity. This is
   * intended to be used to launch things like list view activities with a file
   * other than the default.
   * @param context
   * @param tp
   * @param searchStack
   * @param isOverview
   * @param filename
   */
  public static void launchTableActivityWithFilename(Activity context,
      TableProperties tp, Stack<String> searchStack, boolean isOverview,
      String filename) {
    Controller.launchTableActivity(context, tp, null, searchStack, isOverview,
        filename);
    context.finish();
  }

  /**
   * This method should launch the custom app view that is a generic user-
   * customizable home screen or html page for the app.
   */
  public static void launchAppViewActivity(Context context) {

  }

  /**
   * Launches the Table pointed to by tp as a list view with the specified
   * filename.
   * @param context
   * @param tp
   * @param searchText
   * @param searchStack
   * @param isOverview
   * @param filename
   */
  public static void launchListViewWithFileName(Context context,
      TableProperties tp, String searchText, Stack<String> searchStack,
      boolean isOverview, String filename) {
    Intent intent = new Intent(context, ListDisplayActivity.class);
    if (filename != null) {
      intent.putExtra(ListDisplayActivity.INTENT_KEY_FILENAME, filename);
    }
    intent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
    prepareIntentForLaunch(intent, tp, searchStack, searchText, isOverview);
    context.startActivity(intent);
  }

  /*
   * A helper method that was introduced just to eliminate redundant code.
   * Adds the appropriate extras to the intent.
   */
  private static void prepareIntentForLaunch(Intent intent, TableProperties tp,
      Stack<String> searchStack, String searchText, boolean isOverview) {
    if (searchStack != null) {
      String[] stackValues = new String[searchStack.size()];
      for (int i = 0; i < searchStack.size(); i++) {
        stackValues[i] = searchStack.get(i);
      }
      intent.putExtra(INTENT_KEY_SEARCH_STACK, stackValues);
    } else if (searchText != null) {
      intent.putExtra(INTENT_KEY_SEARCH, searchText);
    } else if (searchText == null) {
      KeyValueStoreHelper kvsh =
          tp.getKeyValueStoreHelper(TableProperties.KVS_PARTITION);
      String savedQuery = kvsh.getString(TableProperties.KEY_CURRENT_QUERY);
      if (savedQuery == null) {
        savedQuery = "";
      }
      intent.putExtra(INTENT_KEY_SEARCH, savedQuery);
    }
    intent.putExtra(INTENT_KEY_IS_OVERVIEW, isOverview);
  }

  private static void launchTableActivity(Context context, TableProperties tp,
      String searchText,
      Stack<String> searchStack, boolean isOverview, String filename) {
    TableViewType viewType = tp.getCurrentViewType();
    Intent intent;
    switch (viewType) {
    case List:
      intent = new Intent(context, ListDisplayActivity.class);
      if (filename != null) {
        intent.putExtra(ListDisplayActivity.INTENT_KEY_FILENAME, filename);
      }
      break;
    case Graph:
      intent = new Intent(context, GraphDisplayActivity.class);
      break;
    case Map:
      intent = new Intent(context, TableActivity.class);
      break;
    case Spreadsheet:
      intent = new Intent(context, SpreadsheetDisplayActivity.class);
      break;
    default:
      intent = new Intent(context, SpreadsheetDisplayActivity.class);
    }
    intent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
    prepareIntentForLaunch(intent, tp, searchStack, searchText, isOverview);
//    if (searchStack != null) {
//      String[] stackValues = new String[searchStack.size()];
//      for (int i = 0; i < searchStack.size(); i++) {
//        stackValues[i] = searchStack.get(i);
//      }
//      intent.putExtra(INTENT_KEY_SEARCH_STACK, stackValues);
//    } else if (searchText != null) {
//      intent.putExtra(INTENT_KEY_SEARCH, searchText);
//    } else if (searchText == null) {
//      KeyValueStoreHelper kvsh =
//          tp.getKeyValueStoreHelper(TableProperties.KVS_PARTITION);
//      String savedQuery = kvsh.getString(TableProperties.KEY_CURRENT_QUERY);
//      if (savedQuery == null) {
//        savedQuery = "";
//      }
//      intent.putExtra(INTENT_KEY_SEARCH, savedQuery);
//    }
//    intent.putExtra(INTENT_KEY_IS_OVERVIEW, isOverview);
    context.startActivity(intent);
  }

  /**
   * Launch a detail view for the given table showing the given rowNum.
   * @param context
   * @param tp
   * @param table
   * @param rowNum
   * @param filename the filename to be used if the filename differs than that
   * set in the key value store.
   */
  public static void launchDetailActivity(Activity activity, TableProperties tp,
      UserTable table,
      int rowNum, String filename) {
    Intent intent = new Intent(activity, DetailDisplayActivity.class);
    intent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
    intent.putExtra(DetailDisplayActivity.INTENT_KEY_ROW_ID, table.getRowId(rowNum));
    if (filename != null) {
      // a null value informs the DetailDisplayActivity that the filename in
      // the kvs should be used, so only add it if it has been set.
      intent.putExtra(DetailDisplayActivity.INTENT_KEY_FILENAME, filename);
    }
    activity.startActivity(intent);
  }

    private class CellEditDialog extends AlertDialog {

        private final String rowId;
        private final int colIndex;
        private final String elementKey;
        private final CellValueView.CellEditView cev;

        public CellEditDialog(String rowId, String value, int colIndex) {
            super(activity);
            this.rowId = rowId;
            this.colIndex = colIndex;
            ColumnProperties cp = tp.getColumnByIndex(colIndex);
            this.elementKey = cp.getElementKey();
            cev = CellValueView.getCellEditView(activity, cp, value);
            buildView(activity);
        }
        private void buildView(Context context) {
          Button setButton = new Button(context);
          setButton.setText(activity.getResources().getString(R.string.set));
          setButton.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                  String value = du.validifyValue(
                      tp.getColumnByElementKey(CellEditDialog.this.elementKey),
                          cev.getValue());
                  if (value == null) {
                      // TODO: alert the user
                      return;
                  }
                  Map<String, String> values = new HashMap<String, String>();
                  values.put(CellEditDialog.this.elementKey,
                          value);

                  // TODO: supply reasonable values for these...
                  String uriUser = null; // user on phone
                  Long timestamp = null; // current time
                  String instanceName = null; // meta/instanceName if present in form
                  String formId = null; // formId used by ODK Collect
                  String locale = null; // current locale

                  dbt.updateRow(rowId, values, uriUser, timestamp,
                      instanceName, formId, locale);
                  da.init();
                  dismiss();
              }
          });
      Button cancelButton = new Button(context);
      cancelButton.setText(activity.getResources().getString(R.string.cancel));
      cancelButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          dismiss();
        }
      });
      LinearLayout buttonWrapper = new LinearLayout(context);
      buttonWrapper.addView(setButton);
      buttonWrapper.addView(cancelButton);
      LinearLayout wrapper = new LinearLayout(context);
      wrapper.setOrientation(LinearLayout.VERTICAL);
      wrapper.addView(cev);
      wrapper.addView(buttonWrapper);
      setView(wrapper);
    }
  }
}
