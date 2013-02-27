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

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.opendatakit.tables.R;
import org.opendatakit.tables.Activity.ColumnManager;
import org.opendatakit.tables.Activity.DisplayPrefsActivity;
import org.opendatakit.tables.Activity.ListOfListViewsActivity;
import org.opendatakit.tables.Activity.TableManager;
import org.opendatakit.tables.Activity.TablePropertiesManager;
import org.opendatakit.tables.Activity.util.CollectUtil;
import org.opendatakit.tables.Activity.util.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DataUtil;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.Query.Constraint;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableViewType;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.lib.ClearableEditText;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
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
	private static final int MENU_ITEM_ID_DISPLAY_PREFERENCES = 4;
	private static final int MENU_ITEM_ID_OPEN_TABLE_PROPERTIES = 5;
    private static final int MENU_ITEM_ID_OPEN_COLUMN_MANAGER = 6;
    static final int FIRST_FREE_MENU_ITEM_ID = 7;
        
    private static final int RCODE_TABLE_PROPERTIES_MANAGER = 0;
    private static final int RCODE_COLUMN_MANAGER = 1;
    private static final int RCODE_ODKCOLLECT_ADD_ROW = 2;
    private static final int RCODE_ODKCOLLECT_EDIT_ROW = 3;
    static final int FIRST_FREE_RCODE = 4;
    
    private static final String COLLECT_FORMS_URI_STRING =
        "content://org.odk.collect.android.provider.odk.forms/forms";
    private static final Uri ODKCOLLECT_FORMS_CONTENT_URI =
        Uri.parse(COLLECT_FORMS_URI_STRING);
    private static final String COLLECT_INSTANCES_URI_STRING =
        "content://org.odk.collect.android.provider.odk.instances/instances";
    private static final Uri COLLECT_INSTANCES_CONTENT_URI =
        Uri.parse(COLLECT_INSTANCES_URI_STRING);
    
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
    private String rowId = null;
    
    
    Controller(SherlockActivity activity, final DisplayActivity da,
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
        infoBar.setText("Table: " + tp.getDisplayName());
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
    
    /**
     *  Set the text in the info bar
     * @param text String to display in info bar
     */
    public void setInfoBarText(String text) {
    	infoBar.setText(text);
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
  TableProperties getTableProperties() {
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
  void refreshDbTable() {
    this.dbt = dm.getDbTable(tp.getTableId());
  }

  /**
   * @return DbTable this data table
   */
  DbTable getDbTable() {
    tp.refreshColumns();
    return dbt;
  }

  /**
   * @return True if this is an overview type, false if this is 
   *         collection view type
   */
  boolean getIsOverview() {
      return isOverview;
  }

  /**
   * @return String text currently in the search bar
   */
  String getSearchText() {
    return searchText.peek();
  }

  /**
   * @return the view generated for this
   */
  View getContainerView() {
    return container;
  }

  void setDisplayView(View dv) {
    displayWrap.removeAllViews();
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
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

  void recordSearch() {
    searchText.add(searchField.getEditText().getText().toString());
  }

  void onBackPressed() {
    if (searchText.size() == 1) {
      activity.finish();
    } else {
      searchText.pop();
      searchField.getEditText().setText(searchText.peek());
      da.init();
    }
  }
 

  /*
   * Original method.
   */
//  void editRow(UserTable table, int rowNum) {
//    Intent intent = getIntentForOdkCollectEditRow(table, rowNum);
//    if (intent != null) {
//      this.rowId = table.getRowId(rowNum);
//      activity.startActivityForResult(intent, RCODE_ODKCOLLECT_EDIT_ROW);
//    }
//  }
  
  /**
   * This should launch Collect to edit the data for the row. If there is a 
   * custom form defined for the table, its info should be loaded in params.
   * If the formId in params is null, then the default form is generated, which
   * is just every column with its own entry field on a single screen.
   * @param table
   * @param rowNum
   * @param params
   */
  void editRow(UserTable table, int rowNum, CollectFormParameters params) {
    Intent intent = null;
//    if (params.getFormId() == null) {
//      intent = getIntentForOdkCollectEditRow(table, rowNum);
//    } else {
//      // a custom form has been assigned to the table.
//      // So, we need to write the data file and then insert it and launch
//      // the intent.
//      intent = getIntentForOdkCollectEditRowRevised(table, rowNum, params);
//    }
    intent = getIntentForOdkCollectEditRow(table, rowNum, params);
    if (intent != null) {
      this.rowId = table.getRowId(rowNum);
      activity.startActivityForResult(intent, RCODE_ODKCOLLECT_EDIT_ROW);
    }
  }

  boolean handleActivityReturn(int requestCode, int returnCode, Intent data) {
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
    default:
      return false;
    }
  }

  private void handleTablePropertiesManagerReturn() {
//    int oldViewType = tvs.getViewType();
    // so for now I think that the boolean of whether or not the current view
    // is an overview of a collection view is stored here in Controller.
    // This should eventually move, if we decide to keep this architecture. but
    // for now I'm going to just hardcode in a solution.
    TableViewType oldViewType = tp.getCurrentViewType();
    tp = dm.getTableProperties(tp.getTableId(), KeyValueStore.Type.ACTIVE);
    dbt = dm.getDbTable(tp.getTableId());
//    if (oldViewType == tvs.getViewType()) {
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
    void buildOptionsMenu(Menu menu) {
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
                "Search"); 
        searchItem.setIcon(R.drawable.ic_action_search);
        searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        searchItem.setEnabled(enabled);
        
        
        // view type submenu
        // 	  -determine the possible view types
        final TableViewType[] viewTypes = tp.getPossibleViewTypes();
        // 	  -build a checkable submenu to select the view type
        SubMenu viewTypeSubMenu = menu.addSubMenu(Menu.NONE, MENU_ITEM_ID_VIEW_TYPE_SUBMENU, Menu.NONE, "ViewType");
        MenuItem viewType = viewTypeSubMenu.getItem();
        viewType.setIcon(R.drawable.view);
        viewType.setEnabled(enabled);
        viewType.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        MenuItem item;
        // This will be the list filename, which we need to have here so we 
        // know whether or not it's specified.
        KeyValueStoreHelper kvsh = 
            tp.getKeyValueStoreHelper(ListDisplayActivity.KVS_PARTITION);
        String listViewFilename = kvsh.getString( 
            ListDisplayActivity.KEY_FILENAME);
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
                listViewFilename == null) {
               item.setEnabled(false);
            }
        }


        viewTypeSubMenu.setGroupCheckable(MENU_ITEM_ID_VIEW_TYPE_SUBMENU, 
            true, true);
        
        // Add Row
        MenuItem addItem = menu.add(Menu.NONE, MENU_ITEM_ID_ADD_ROW_BUTTON, Menu.NONE,
              "Add Row").setEnabled(enabled);
        addItem.setIcon(R.drawable.content_new);
        addItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        
        // Settings submenu
        SubMenu settings = menu.addSubMenu(Menu.NONE, MENU_ITEM_ID_SETTINGS_SUBMENU, Menu.NONE, "Settings");
        MenuItem settingsItem = settings.getItem();
        settingsItem.setIcon(R.drawable.settings_icon2);
        settingsItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS); 
        // TODO: change to setEnabled(enabled) once DisplayPrefActivity is finished
        MenuItem display = settings.add(Menu.NONE, MENU_ITEM_ID_DISPLAY_PREFERENCES, Menu.NONE, 
        		"Display Preferences").setEnabled(false);
        // always disable DisplayPreferences if it is currently in list view
        if (tp.getCurrentViewType() == TableViewType.List)
        	display.setEnabled(false);
        settings.add(Menu.NONE, MENU_ITEM_ID_OPEN_TABLE_PROPERTIES, Menu.NONE,
    			"Table Properties").setEnabled(enabled);
        settings.add(Menu.NONE, MENU_ITEM_ID_OPEN_COLUMN_MANAGER, Menu.NONE,
              "Column Manager").setEnabled(enabled);
    }

    /**
     * Handle menu item that was selected by user
     * @param selectedItem MenuItem
     * @return true if selectedItem was handled 
     */
	boolean handleMenuItemSelection(MenuItem selectedItem) {
		int itemId = selectedItem.getItemId();
		// if the item is part of the sub-menu for view type, set the view type with its itemId
	    // else, handle accordingly
		if(selectedItem.getGroupId() == MENU_ITEM_ID_VIEW_TYPE_SUBMENU) {
		  tp.setCurrentViewType(
		      TableViewType.getViewTypeFromId(selectedItem.getItemId()));
		  // Here we will check if it is a list view. If it is, we need to for 
		  // now launch the special case of the list selector thing.
		  TableViewType viewType = tp.getCurrentViewType();
		  if (viewType == TableViewType.List) {
		    Intent selectListViewIntent = 
		        new Intent(activity, ListOfListViewsActivity.class);
		    selectListViewIntent.putExtra(
		        ListOfListViewsActivity.INTENT_KEY_TABLE_ID, tp.getTableId());
		    activity.startActivity(selectListViewIntent);
		    activity.finish();
		    return true;
		  }
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
	          Intent intentAddRow;
	          if (getSearchText().equals("")) {
	            intentAddRow = getIntentForOdkCollectAddRow(params, null);
	          } else {
    	          Map<String, String> elementNameToValue = 
    	              getMapFromLimitedQuery();
    	         intentAddRow = getIntentForOdkCollectAddRow(params, 
    	             elementNameToValue);
	          }      
	          if (intentAddRow != null) {
	              Controller.this.activity.startActivityForResult(intentAddRow,
	                      RCODE_ODKCOLLECT_ADD_ROW);
	          }
	          return true;
	        case MENU_ITEM_ID_SETTINGS_SUBMENU:
	        	return true;
	        case MENU_ITEM_ID_DISPLAY_PREFERENCES:
	        	Intent k = new Intent(activity, DisplayPrefsActivity.class);
	        	k.putExtra("tableId", tp.getTableId());
	        	
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

	  /**
	   * The idea here is that we might want to edit a row of the table using a 
	   * pre-set Collect form. This form would be user-defined and would be a more
	   * user-friendly thing that would display only the pertinent information for
	   * a particular user. 
	   * @param table
	   * @param rowNum
	   * @return
	   */
	  /*
	   * This is a move away from the general "odk add row" usage that is going on
	   * when no row is defined. As I understand it, the new case will work as 
	   * follows. 
	   * 
	   * There exits an "tableEditRow" form for a particular table. This form, as I
	   * understand it, must exist both in the tables directory, as well as in
	   * Collect so that Collect can launch it with an Intent. 
	   * 
	   * You then also construct a "values" sort of file, that is the data from the
	   * database that will pre-populate the fields. Mitch referred to something 
	   * like this as the "instance" file. 
	   * 
	   * Once you have both of these files, the form and the data, you insert the 
	   * data into the form. When you launch the form, it is then pre-populated
	   * with data from the database.
	   * 
	   * In order to make this work, the form must exist both within the places
	   * Collect knows to look, as well as in the Tables folder. You also must know
	   * the:
	   * 
	   * collectFormVersion
	   * collectFormId
	   * collectXFormRootElement (default to "data")
	   * 
	   * These will most likely exist as keys in the key value store. They must 
	   * match the form.
	   * 
	   * Other things needed will be:
	   * 
	   * instanceFilePath  // I think the filepath with all the values
	   * displayName       // just text, eg a row ID
	   * formId            // the same thing as collectFormId?
	   * formVersion
	   * status            // either INCOMPLETE or COMPLETE
	   * 
	   * Examples for how this is done in Collect can be found in the Collect code
	   * in org.odk.collect.android.tasks.SaveToDiskTask.java, in the 
	   * updateInstanceDatabase() method.
	   */
	  public Intent getIntentForOdkCollectEditRow(UserTable table, 
	      int rowNum, CollectFormParameters params) {
	    // Check if there is a custom form. If there is not, we want to delete
	    // the old form and write the new form.
	    if (!params.isCustom()) {
	      boolean formIsReady = CollectUtil.deleteWriteAndInsertFormIntoCollect(
	          activity.getContentResolver(), params, tp);
	      if (!formIsReady) {
	        Log.e(TAG, "could not delete, write, or insert a generated form");
	        return null;
	      }
	    }
	    Map<String, String> elementNameToValue = new HashMap<String, String>();
	    for (ColumnProperties cp : tp.getColumns()) {
	      String value = table.getData(rowNum,
	          tp.getColumnIndex(cp.getElementName()));
	      elementNameToValue.put(cp.getElementName(), value);
	    }
	    boolean writeDataSuccessful = 
	        CollectUtil.writeRowDataToBeEdited(elementNameToValue, tp, params);
	    if (!writeDataSuccessful) {
	      Log.e(TAG, "could not write instance file successfully!");
	    }
	    Uri insertUri = 
	        CollectUtil.getUriForInsertedData(params, rowNum, 
	            activity.getContentResolver()); 
	    // Copied the below from getIntentForOdkCollectEditRow().
	    Intent intent = new Intent();
	    intent.setComponent(new ComponentName("org.odk.collect.android",
	            "org.odk.collect.android.activities.FormEntryActivity"));
	    intent.setAction(Intent.ACTION_EDIT);
	    intent.setData(insertUri);
	    return intent;
	  }
	  
	/**
	 * Generate the Intent to add a row using Collect. For safety, the params 
	 * object, particularly it's isCustom field, determines exactly which action
	 * is taken. If a custom form is defined, it launches that form. If there
	 * is not, it writes a new form, inserts it into collect, and launches it.
	 * @param params
	 * @return
	 */
  public Intent getIntentForOdkCollectAddRow(CollectFormParameters params,
      Map<String, String> elementNameToValue) {
    /*
     * So, there are several things to check here. The first thing we want to 
     * do is see if a custom form has been defined for this table. If there is
     * not, then we will need to write a custom one. When we do this, we will
     * then have to call delete on Collect to remove the old form, which will
     * have used the same id. This will not fail if a form has not been already
     * been written--delete will simply return 0.
     */
    // Check if there is a custom form. If there is not, we want to delete
    // the old form and write the new form.
    if (!params.isCustom()) {
      boolean formIsReady = CollectUtil.deleteWriteAndInsertFormIntoCollect(
          activity.getContentResolver(), params, tp);
      if (!formIsReady) {
        Log.e(TAG, "could not delete, write, or insert a generated form");
        return null;
      }
    }
    Uri formToLaunch;
    if (elementNameToValue == null) {
      formToLaunch = CollectUtil.getUriOfForm(activity.getContentResolver(), 
          params.getFormId());
      if (formToLaunch == null) {
        Log.e(TAG, "URI of the form to pass to Collect and launch was null");
        return null;
      }
    } else { 
      // we've received some values to prepopulate the add row with.
      boolean writeDataSuccessful = 
          CollectUtil.writeRowDataToBeEdited(elementNameToValue, tp, params);
      if (!writeDataSuccessful) {
        Log.e(TAG, "could not write instance file successfully!");
      }
      // Here we'll just act as if we're inserting 0, which 
      // really doesn't matter?
      formToLaunch = 
          CollectUtil.getUriForInsertedData(params, 0, 
              activity.getContentResolver()); 
    }
    // And now finally create the intent.
    Intent intent = new Intent();
    intent.setComponent(new ComponentName("org.odk.collect.android",
        "org.odk.collect.android.activities.FormEntryActivity"));
    intent.setAction(Intent.ACTION_EDIT);    
    intent.setData(formToLaunch);
    return intent;
  }
  
  private Map<String, String> getMapFromLimitedQuery() {
    Map<String, String> elementNameToValue = 
        new HashMap<String, String>();
    // First add all empty strings. We will overwrite the ones that are queried
    // for in the search box. We need this so that if an add is canceled, we 
    // can check for equality and know not to add it. If we didn't do this,
    // but we've prepopulated an add with a query, when we return and don't do
    // a check, we'll add a blank row b/c there are values in the key value 
    // pairs, even though they were our prepopulated values.
    for (ColumnProperties cp : tp.getColumns()) {
      elementNameToValue.put(cp.getElementName(), "");
    }
    Query currentQuery = new Query(null, tp);
    currentQuery.loadFromUserQuery(getSearchText());
    for (int i = 0; i < currentQuery.getConstraintCount(); i++) {
      Constraint constraint = currentQuery.getConstraint(i);
      // NB: This is predicated on their only ever being a single
     // search value. I'm not sure how additional values could be
     // added.
      elementNameToValue.put(constraint.getColumnDbName(),
          constraint.getValue(0));
    }
    return elementNameToValue;
  }

  boolean addRowFromOdkCollectForm(int instanceId) {
    Map<String, String> formValues = getOdkCollectFormValues(instanceId);
    if (formValues == null) {
      return false;
    }
    Map<String, String> values = new HashMap<String, String>();
    for (String key : formValues.keySet()) {
        ColumnProperties cp = tp.getColumnByElementKey(key);
        if (cp == null) {
            continue;
        }
        String value = du.validifyValue(cp, formValues.get(key));
        if (value != null) {
            values.put(key, value);
        }
    }
    // Now we want to check for equality of this and the query map. If they
    // are the same, we know we hit ignore and didn't save anything.
    Map<String, String> prepopulatedValues = getMapFromLimitedQuery();
    if (prepopulatedValues.equals(values)) {
      return false;
    }
    dbt.addRow(values);
    return true;
  }
    
    private void handleOdkCollectAddReturn(int returnCode, Intent data) {
        if (returnCode != SherlockActivity.RESULT_OK) {
            return;
        }
        int instanceId = Integer.valueOf(data.getData().getLastPathSegment());
        addRowFromOdkCollectForm(instanceId);
        da.init();
    }
    
    private void handleOdkCollectEditReturn(int returnCode, Intent data) {
        if (returnCode != SherlockActivity.RESULT_OK) {
            return;
        }
        int instanceId = Integer.valueOf(data.getData().getLastPathSegment());
        updateRowFromOdkCollectForm(instanceId);
        da.init();
    }
    
    boolean updateRowFromOdkCollectForm(int instanceId) {
      Map<String, String> formValues = getOdkCollectFormValues(instanceId);
      if (formValues == null) {
          return false;
      }
      Map<String, String> values = getMapForInsertion(formValues);
//      Map<String, String> values = new HashMap<String, String>();
//
//      for (ColumnProperties cp : tp.getColumns()) {
//        // we want to use element name here, b/c that is what Collect should be
//        // using to access all of the columns/elements.
//          String elementName = cp.getElementName();
//          String value = du.validifyValue(cp, formValues.get(elementName));
//          if (value != null) {
//              values.put(elementName,value);
//          }
//      }
      dbt.updateRow(rowId, values);
      rowId = null;
      return true;
  }
    
  /**
   * This gets a map of values for insertion into a row after returning from
   * a Collect form.
   * @param formValues
   * @return
   */
  Map<String, String> getMapForInsertion(
      Map<String, String> formValues) {
    Map<String, String> values = new HashMap<String, String>();

    for (ColumnProperties cp : tp.getColumns()) {
      // we want to use element name here, b/c that is what Collect should be
      // using to access all of the columns/elements.
        String elementName = cp.getElementName();
        String value = du.validifyValue(cp, formValues.get(elementName));
        if (value != null) {
            values.put(elementName,value);
        }
    }
    return values;
  }

  protected Map<String, String> getOdkCollectFormValues(int instanceId) {
    String[] projection = { "instanceFilePath" };
    String selection = "_id = ?";
    String[] selectionArgs = { (instanceId + "") };
    Cursor c = activity.managedQuery(COLLECT_INSTANCES_CONTENT_URI, projection, selection,
        selectionArgs, null);
    if (c.getCount() != 1) {
      return null;
    }
    c.moveToFirst();
    String instancepath = c.getString(c.getColumnIndexOrThrow("instanceFilePath"));
    Document xmlDoc = new Document();
    KXmlParser xmlParser = new KXmlParser();
    try {
      xmlParser.setInput(new FileReader(instancepath));
      xmlDoc.parse(xmlParser);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    } catch (XmlPullParserException e) {
      e.printStackTrace();
      return null;
    }
    Element rootEl = xmlDoc.getRootElement();
    Node rootNode = rootEl.getRoot();
    Element dataEl = rootNode.getElement(0);
    Map<String, String> values = new HashMap<String, String>();
    for (int i = 0; i < dataEl.getChildCount(); i++) {
      Element child = dataEl.getElement(i);
      String key = child.getName();
      String value = child.getChildCount() > 0 ? child.getText(0) : null;
      values.put(key, value);
    }
    return values;
  }

  void openCellEditDialog(String rowId, String value, int colIndex) {
    (new CellEditDialog(rowId, value, colIndex)).show();
  }

  public static void launchTableActivity(Context context, TableProperties tp, boolean isOverview) {
    Controller.launchTableActivity(context, tp, null, null, isOverview);
  }

  public static void launchTableActivity(Context context, TableProperties tp, String searchText,
      boolean isOverview) {
    Controller.launchTableActivity(context, tp, searchText, null, isOverview);
  }

  private static void launchTableActivity(Activity context, TableProperties tp,
      Stack<String> searchStack, boolean isOverview) {
    Controller.launchTableActivity(context, tp, null, searchStack, isOverview);
     context.finish();
  }

  private static void launchTableActivity(Context context, TableProperties tp, 
      String searchText,
      Stack<String> searchStack, boolean isOverview) {
    //TODO: need to figure out how CollectionViewSettings should work. 
    // make them work.
//    TableViewSettings tvs = isOverview ? tp.getOverviewViewSettings() : tp
//        .getCollectionViewSettings();
    TableViewType viewType = tp.getCurrentViewType();
    Intent intent;
//    switch (tvs.getViewType()) {
    switch (viewType) {
//    case TableViewSettings.Type.LIST:
    //TODO: figure out which of these graph was originally and update it.
    case List:
      intent = new Intent(context, ListDisplayActivity.class);
      break;
//    case TableViewSettings.Type.LINE_GRAPH:
//      intent = new Intent(context, LineGraphDisplayActivity.class);
//      break;
//    case TableViewSettings.Type.BOX_STEM:
//      intent = new Intent(context, BoxStemGraphDisplayActivity.class);
//      break;
//    case TableViewSettings.Type.BAR_GRAPH:
    case Graph:
      intent = new Intent(context, BarGraphDisplayActivity.class);
      break;
//    case TableViewSettings.Type.MAP:
    case Map:
      intent = new Intent(context, MapDisplayActivity.class);
      break;
    case Spreadsheet:
      intent = new Intent(context, SpreadsheetDisplayActivity.class);
      break;
    default:
      intent = new Intent(context, SpreadsheetDisplayActivity.class);
    }
    intent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
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
    context.startActivity(intent);
  }

  public static void launchDetailActivity(Context context, TableProperties tp, 
      UserTable table,
      int rowNum) {
    String[] keys = new String[table.getWidth()];
    String[] values = new String[table.getWidth()];
    for (int i = 0; i < table.getWidth(); i++) {
      keys[i] = tp.getColumns()[i].getElementKey();
      values[i] = table.getData(rowNum, i);
    }
    Intent intent = new Intent(context, DetailDisplayActivity.class);
    intent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
    intent.putExtra(DetailDisplayActivity.INTENT_KEY_ROW_ID, table.getRowId(rowNum));
    intent.putExtra(DetailDisplayActivity.INTENT_KEY_ROW_KEYS, keys);
    intent.putExtra(DetailDisplayActivity.INTENT_KEY_ROW_VALUES, values);
    context.startActivity(intent);
  }


//    public class SearchActionProvider extends ActionProvider implements OnDragListener {  	 
//        Context mContext;
//        public SearchActionProvider(Context context) {
//            super(context);
//            mContext = context;
//        }
//     
//        @Override
//        public View onCreateActionView() {
//    		controlWrap = new LinearLayout(mContext);
//            searchField = new EditText(mContext);
//            searchField.setId(VIEW_ID_SEARCH_FIELD);
//            searchField.setText(searchText.peek());
//            ImageButton searchButton = new ImageButton(mContext);
//            searchButton.setId(VIEW_ID_SEARCH_BUTTON);
//            searchButton.setImageResource(R.drawable.ic_action_search);
//            searchButton.setOnClickListener(new OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    da.onSearch();
//                }
//            });
//
//            LinearLayout.LayoutParams searchFieldParams =
//                    new LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.MATCH_PARENT,
//                    LinearLayout.LayoutParams.WRAP_CONTENT);
//            searchFieldParams.weight = 1;
//            controlWrap.addView(searchField, searchFieldParams);
//            
////            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
////                    LinearLayout.LayoutParams.WRAP_CONTENT,
////                    LinearLayout.LayoutParams.WRAP_CONTENT);
////            buttonParams.weight = 0;
////            controlWrap.addView(searchButton, buttonParams);
//            return controlWrap;
//        }
//
//        @Override
//        public boolean onDrag(View v, DragEvent event) {
//        	int action = event.getAction();
//        	switch (event.getAction()) {
//        	case DragEvent.ACTION_DRAG_STARTED:
//        		// Do nothing
//        		break;
//        	case DragEvent.ACTION_DRAG_ENTERED:
//        		// Do nothing
//        		break;
//        	case DragEvent.ACTION_DRAG_EXITED:      
//        		// Do nothing
//        		break;
//        	case DragEvent.ACTION_DROP:
//        		// Dropped, reassign View to ViewGroup
//        		View view = (View) event.getLocalState();
//        		ViewGroup owner = (ViewGroup) view.getParent();
//        		owner.removeView(view);
//        		LinearLayout container = (LinearLayout) v;
//        		container.addView(view);
//        		view.setVisibility(View.VISIBLE);
//        		break;
//        	case DragEvent.ACTION_DRAG_ENDED:
//        		// Do nothing
//        	default:
//        		break;
//        	}
//        	return true;
//        }
//    }
    
    private class CellEditDialog extends AlertDialog {
        
        private final String rowId;
        private final int colIndex;
        private final CellValueView.CellEditView cev;
        
        public CellEditDialog(String rowId, String value, int colIndex) {
            super(activity);
            this.rowId = rowId;
            this.colIndex = colIndex;
            cev = CellValueView.getCellEditView(activity,
                    tp.getColumns()[colIndex], value);
            buildView(activity);
        }
        private void buildView(Context context) {
          Button setButton = new Button(context);
          setButton.setText(activity.getResources().getString(R.string.set));
          setButton.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                  String value = du.validifyValue(tp.getColumns()[colIndex],
                          cev.getValue());
                  if (value == null) {
                      // TODO: alert the user
                      return;
                  }
                  Map<String, String> values = new HashMap<String, String>();
                  values.put(tp.getColumns()[colIndex].getElementKey(),
                          value);
                  dbt.updateRow(rowId, values);
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
