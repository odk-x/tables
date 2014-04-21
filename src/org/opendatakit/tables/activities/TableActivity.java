package org.opendatakit.tables.activities;

import java.util.Map;

import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.utils.TableFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.graphs.GraphDisplayActivity;
import org.opendatakit.tables.activities.graphs.GraphManagerActivity;
import org.opendatakit.tables.fragments.ITableFragment;
import org.opendatakit.tables.fragments.TableMapFragment;
import org.opendatakit.tables.types.FormType;
import org.opendatakit.tables.utils.CollectUtil;
import org.opendatakit.tables.utils.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.utils.SurveyUtil;
import org.opendatakit.tables.utils.SurveyUtil.SurveyFormParameters;
import org.opendatakit.tables.views.ClearableEditText;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

/**
 * Base activity for all fragments that display information about a database.
 * Deals with maintaining the data and the actionbar.
 *
 * @author Chris Gelon (cgelon)
 */
public class TableActivity extends SherlockFragmentActivity {

  public static final String t = "TableActivity";
  // / Static Strings ///
  public static final String INTENT_KEY_TABLE_ID = "tableId";
  public static final String INTENT_KEY_CURRENT_VIEW_TYPE = "currentViewType";

  /** Key value store key for table activity. */
  public static final String KVS_PARTITION = "TableActivity";

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
  static final int FIRST_FREE_MENU_ITEM_ID = 7;

  /** The current fragment being displayed. */
  private ITableFragment mCurrentFragment;

  /** The fragment that contains map information. */
  private TableMapFragment mMapFragment;

  /** Table that represents all of the data in the query. */
  private UserTable mTable;

  /** The properties of the user table. */
  private TableProperties mTableProperties;

  private DbTable mDbTable;

  private String mAppName;
  private String mTableId;
  private String mSqlWhereClause;
  private String[] mSqlSelectionArgs;
  private String[] mSqlGroupBy;
  private String mSqlHaving;
  private String mSqlOrderByElementKey;
  private String mSqlOrderByDirection;

  private TableViewType mCurrentViewType;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mAppName = getIntent().getStringExtra(Controller.INTENT_KEY_APP_NAME);
    if ( mAppName == null ) {
      mAppName = TableFileUtils.getDefaultAppName();
    }

    // Find the table id.
    mTableId = getIntent().getExtras().getString(INTENT_KEY_TABLE_ID);
    if (mTableId == null) {
      throw new RuntimeException("Table id was not passed in through the bundle.");
    }

    if ( savedInstanceState != null && savedInstanceState.containsKey(INTENT_KEY_CURRENT_VIEW_TYPE) ) {
      mCurrentViewType = TableViewType.valueOf(savedInstanceState.getString(INTENT_KEY_CURRENT_VIEW_TYPE));
    } else if (getIntent().getExtras().containsKey(INTENT_KEY_CURRENT_VIEW_TYPE)) {
      mCurrentViewType = TableViewType.valueOf(getIntent().getExtras().getString(INTENT_KEY_CURRENT_VIEW_TYPE));
    } else {
      mCurrentViewType = TableViewType.Map;
    }

    setContentView(R.layout.standard_table_layout);

    // Initialize data objects.
    refreshDbTable(mTableId);

    // Initialize layout fields.
    setSearchFieldText("");
    setInfoBarText("Table: " + mTableProperties.getDisplayName());


    // Create the map fragment.
    if (savedInstanceState == null) {
      mMapFragment = new TableMapFragment();
      getSupportFragmentManager().beginTransaction().add(R.id.main, mMapFragment).commit();
    } else {
      mMapFragment = (TableMapFragment) getSupportFragmentManager().findFragmentById(R.id.main);
    }

    // Set the current fragment.
    mCurrentFragment = mMapFragment;
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    if ( mCurrentViewType != null ) {
      outState.putString(INTENT_KEY_CURRENT_VIEW_TYPE, mCurrentViewType.name());
    }
  }

  public TableViewType getCurrentViewType() {
    if ( mCurrentViewType == null ) {
      mCurrentViewType = mTableProperties.getDefaultViewType();
    }
    return mCurrentViewType;
  }

  public void setCurrentViewType(TableViewType viewType) {
    mCurrentViewType = viewType;
  }

  public UserTable getTable() {
    return mTable;
  }

  public TableProperties getTableProperties() {
    return mTableProperties;
  }

  public void init() {
    refreshDbTable(mTableProperties.getTableId());
    mCurrentFragment.init();
  }

  public void onSearchButtonClick(View v) {
    mCurrentFragment.onSearch();
  }

  /**
   * @return The text in the search field.
   */
  public String getSearchFieldText() {
    return ((ClearableEditText) findViewById(R.id.search_field)).getEditText().getText().toString();
  }

  /**
   * Set the text in the search field.
   */
  public void setSearchFieldText(String text) {
    ((ClearableEditText) findViewById(R.id.search_field)).getEditText().setText(text);
  }

  /**
   * Set the text in the info bar.
   */
  public void setInfoBarText(String text) {
    ((TextView) findViewById(R.id.info_bar)).setText(text);
  }

  /**
   * @return The text in the info bar.
   */
  public String getInfoBarText() {
    return ((TextView) findViewById(R.id.info_bar)).getText().toString();
  }

  /**
   * Update the dbTable that Controller is monitoring. This should be called
   * only if there is no way to update the dbTable held by the Controller if a
   * change happens outside of the Controller's realm of control. For instance,
   * changing a column display name in PropertyManager does not get updated to
   * the dbTable without calling this method. This is a messy way of doing
   * things, and a refactor should probably end up fixing this.
   */
  void refreshDbTable(String tableId) {
    mTableProperties = TableProperties.getTablePropertiesForTable(this, mAppName,
        tableId);
    mDbTable = DbTable.getDbTable(mTableProperties);

    Bundle intentExtras = getIntent().getExtras();
    mSqlWhereClause = intentExtras.getString(Controller.INTENT_KEY_SQL_WHERE);
    mSqlSelectionArgs = null;
    if (mSqlWhereClause != null && mSqlWhereClause.length() != 0) {
      mSqlSelectionArgs = intentExtras.getStringArray(Controller.INTENT_KEY_SQL_SELECTION_ARGS);
    }
    mSqlGroupBy = intentExtras.getStringArray(Controller.INTENT_KEY_SQL_GROUP_BY_ARGS);
    mSqlHaving = null;
    if ( mSqlGroupBy != null && mSqlGroupBy.length != 0 ) {
      mSqlHaving = intentExtras.getString(Controller.INTENT_KEY_SQL_HAVING);
    }
    mSqlOrderByElementKey = intentExtras.getString(Controller.INTENT_KEY_SQL_ORDER_BY_ELEMENT_KEY);
    mSqlOrderByDirection = null;
    if ( mSqlOrderByElementKey != null && mSqlOrderByElementKey.length() != 0 ) {
      mSqlOrderByDirection = intentExtras.getString(Controller.INTENT_KEY_SQL_ORDER_BY_DIRECTION);
      if ( mSqlOrderByDirection != null && mSqlOrderByDirection.length() == 0 ) {
        mSqlOrderByDirection = "ASC";
      }
    }

    mTable = mDbTable.rawSqlQuery(mSqlWhereClause, mSqlSelectionArgs, mSqlGroupBy, mSqlHaving, mSqlOrderByElementKey, mSqlOrderByDirection );
  }

  /**
   * @return DbTable this data table
   */
  DbTable getDbTable() {
    return mDbTable;
  }

  /**
   * True if the x and y are in the search box, false otherwise.
   */
  boolean isInSearchBox(int x, int y) {
    y -= findViewById(R.id.control_wrap).getHeight();
    Rect bounds = new Rect();
    findViewById(R.id.search_field).getHitRect(bounds);
    return ((bounds.left <= x) && (bounds.right >= x) && (bounds.top <= y) && (bounds.bottom >= y));
  }

  /**
   * This is used to invert the color of the search box. The boolean parameter
   * specifies whether or not the color should be inverted or returned to
   * normal.
   * <p>
   * The inversion values are not tied to any particular theme, but are set
   * using the ActionBarSherlock themes. These need to change if the app themes
   * are changed.
   *
   * @param invert
   */
  void invertSearchBoxColor(boolean invert) {
    ClearableEditText searchField = (ClearableEditText) findViewById(R.id.search_field);
    if (invert) {
      searchField.setBackgroundResource(R.color.abs__background_holo_light);
      searchField.getEditText().setTextColor(
          searchField.getContext().getResources().getColor(R.color.abs__background_holo_dark));
      searchField.getClearButton().setBackgroundResource(R.drawable.content_remove_dark);
    } else {
      searchField.setBackgroundResource(R.color.abs__background_holo_dark);
      searchField.getEditText().setTextColor(
          searchField.getContext().getResources().getColor(R.color.abs__background_holo_light));
      searchField.getClearButton().setBackgroundResource(R.drawable.content_remove_light);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
    case Controller.RCODE_TABLE_PROPERTIES_MANAGER:
      handleTablePropertiesManagerReturn();
      break;
    case Controller.RCODE_DISPLAY_PROPERTIES:
      handleDisplayPropertiesReturn();
      break;
    case Controller.RCODE_COLUMN_MANAGER:
      handleColumnManagerReturn();
      break;
    case Controller.RCODE_ODK_COLLECT_ADD_ROW:
      handleOdkCollectAddReturn(resultCode, data);
      break;
    case Controller.RCODE_ODK_COLLECT_EDIT_ROW:
      handleOdkCollectEditReturn(resultCode, data);
      break;
    case Controller.RCODE_ODK_SURVEY_ADD_ROW:
      handleOdkSurveyAddReturn(resultCode, data);
      break;
    case Controller.RCODE_ODK_SURVEY_EDIT_ROW:
      handleOdkSurveyEditReturn(resultCode, data);
      break;
    case Controller.RCODE_LIST_VIEW_MANAGER:
      handleListViewManagerReturn();
      break;
    default:
      break;
    }
    if (resultCode == SherlockActivity.RESULT_OK) {
      init();
    }
  }

  private void handleListViewManagerReturn() {
    refreshDbTable(mTableProperties.getTableId());
  }

  private void handleTablePropertiesManagerReturn() {
    refreshDbTable(mTableId);
  }

  private void handleDisplayPropertiesReturn() {
    refreshDbTable(mTableId);
  }

  private void handleColumnManagerReturn() {
    refreshDbTable(mTableId);
  }

  void deleteRow(String rowId) {
    mDbTable.markDeleted(rowId);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Set the app icon as an action to go home.
    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setTitle("");

    // Search
    MenuItem searchItem = menu.add(Menu.NONE, MENU_ITEM_ID_SEARCH_BUTTON, Menu.NONE, "Search");
    searchItem.setIcon(R.drawable.ic_action_search);
    searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    searchItem.setEnabled(true);

    // View type submenu
    // -determine the possible view types
    final TableViewType[] viewTypes = mTableProperties.getPossibleViewTypes();
    // -build a checkable submenu to select the view type
    SubMenu viewTypeSubMenu = menu.addSubMenu(Menu.NONE, MENU_ITEM_ID_VIEW_TYPE_SUBMENU, Menu.NONE,
        "ViewType");
    MenuItem viewType = viewTypeSubMenu.getItem();
    viewType.setIcon(R.drawable.view);
    viewType.setEnabled(true);
    viewType.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    // This will be the name of the default list view, which if exists
    // means we should display the list view as an option.
    KeyValueStoreHelper kvsh = mTableProperties
        .getKeyValueStoreHelper(ListDisplayActivity.KVS_PARTITION);
    String nameOfView = kvsh.getString(ListDisplayActivity.KEY_LIST_VIEW_NAME);
    for (int i = 0; i < viewTypes.length; i++) {
      MenuItem item = viewTypeSubMenu.add(MENU_ITEM_ID_VIEW_TYPE_SUBMENU, viewTypes[i].getId(), i,
          viewTypes[i].name());
      // Mark the current viewType as selected.
      if (getCurrentViewType() == viewTypes[i]) {
        item.setChecked(true);
      }
      // Disable list view if no file is specified
      if (viewTypes[i] == TableViewType.List && nameOfView == null) {
        item.setEnabled(false);
      }
    }

    viewTypeSubMenu.setGroupCheckable(MENU_ITEM_ID_VIEW_TYPE_SUBMENU, true, true);

    // Add Row
    MenuItem addItem = menu.add(Menu.NONE, MENU_ITEM_ID_ADD_ROW_BUTTON, Menu.NONE, "Add Row")
        .setEnabled(true);
    addItem.setIcon(R.drawable.content_new);
    addItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

    // Settings submenu
    SubMenu settings = menu.addSubMenu(Menu.NONE, MENU_ITEM_ID_SETTINGS_SUBMENU, Menu.NONE,
        "Settings");
    MenuItem settingsItem = settings.getItem();
    settingsItem.setIcon(R.drawable.settings_icon2);
    settingsItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

    MenuItem display = settings.add(Menu.NONE, MENU_ITEM_ID_DISPLAY_PREFERENCES, Menu.NONE,
        "Display Preferences").setEnabled(true);
    // Always disable DisplayPreferences if it is currently in list view
    if (getCurrentViewType() == TableViewType.List) {
      display.setEnabled(false);
    }
    settings.add(Menu.NONE, MENU_ITEM_ID_OPEN_TABLE_PROPERTIES, Menu.NONE, "Table Properties")
        .setEnabled(true);

    settings.add(Menu.NONE, MENU_ITEM_ID_OPEN_COLUMN_MANAGER, Menu.NONE, "Column Manager")
        .setEnabled(true);
    // Now an option for editing list views.
    settings.add(Menu.NONE, MENU_ITEM_ID_OPEN_LIST_VIEW_MANAGER,
        Menu.NONE, "List View Manager").setEnabled(true);
    return true;
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    // If the item is part of the sub-menu for view type, set the view type
    // with its itemId else, handle accordingly.
    if (item.getGroupId() == MENU_ITEM_ID_VIEW_TYPE_SUBMENU) {
      setCurrentViewType(TableViewType.getViewTypeFromId(item.getItemId()));
      launchTableActivity(mTableProperties, TableViewType.getViewTypeFromId(item.getItemId()),
          mSqlWhereClause, mSqlSelectionArgs, mSqlGroupBy, mSqlHaving,
          mSqlOrderByElementKey, mSqlOrderByDirection);
      return true;
    } else {
      switch (item.getItemId()) {
      case MENU_ITEM_ID_SEARCH_BUTTON:
        if (getControlWrapVisibility() == View.GONE)
          setControlWrapVisiblity(View.VISIBLE);
        else
          setControlWrapVisiblity(View.GONE);
        return true;
      case MENU_ITEM_ID_VIEW_TYPE_SUBMENU:
        return true;
      case MENU_ITEM_ID_ADD_ROW_BUTTON:
        // TODO: pre-initialize values based upon WHERE clause equality constraints
        addRow(null);
        return true;
      case MENU_ITEM_ID_SETTINGS_SUBMENU:
        return true;
      case MENU_ITEM_ID_DISPLAY_PREFERENCES:
        Intent k = new Intent(this, DisplayPrefsActivity.class);
        k.putExtra(Controller.INTENT_KEY_APP_NAME, mTableProperties.getAppName());
        k.putExtra(DisplayPrefsActivity.INTENT_KEY_TABLE_ID, mTableProperties.getTableId());
        startActivityForResult(k, Controller.RCODE_DISPLAY_PROPERTIES);
        return true;
      case MENU_ITEM_ID_OPEN_TABLE_PROPERTIES:
        Intent tablePropertiesIntent = new Intent(this, TablePropertiesManager.class);
        tablePropertiesIntent.putExtra(Controller.INTENT_KEY_APP_NAME,
            mTableProperties.getAppName());
        tablePropertiesIntent.putExtra(Controller.INTENT_KEY_TABLE_ID,
            mTableProperties.getTableId());
        startActivityForResult(tablePropertiesIntent, Controller.RCODE_TABLE_PROPERTIES_MANAGER);
        return true;
      case MENU_ITEM_ID_OPEN_COLUMN_MANAGER:
        Intent columnManagerIntent = new Intent(this, ColumnManager.class);
        columnManagerIntent.putExtra(Controller.INTENT_KEY_APP_NAME,
            mTableProperties.getAppName());
        columnManagerIntent.putExtra(Controller.INTENT_KEY_TABLE_ID,
            mTableProperties.getTableId());
        startActivityForResult(columnManagerIntent, Controller.RCODE_COLUMN_MANAGER);
        return true;
      case MENU_ITEM_ID_OPEN_LIST_VIEW_MANAGER:
        Intent listViewManagerIntent = new Intent(this, ListViewManager.class);
        listViewManagerIntent.putExtra(Controller.INTENT_KEY_APP_NAME,
            mTableProperties.getAppName());
        listViewManagerIntent.putExtra(Controller.INTENT_KEY_TABLE_ID,
            mTableProperties.getTableId());
        startActivityForResult(listViewManagerIntent, Controller.RCODE_LIST_VIEW_MANAGER);
        return true;
      case android.R.id.home:
        Intent tableManagerIntent = new Intent(this, TableManager.class);
        tableManagerIntent.putExtra(Controller.INTENT_KEY_APP_NAME, mTableProperties.getAppName());
        // Add this flag so that you don't back from TableManager back
        // into the table.
        tableManagerIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(tableManagerIntent);
        finish();
        return true;
      default:
        return false;
      }
    }
  }

  /**
   * Adds a row to the table.
   *
   * @param elementNameToValue
   *          Element names to values to prepopulate the row with before
   *          launching the activity.
   */
  public void addRow(Map<String, String> elementNameToValue) {
    FormType formType = FormType.constructFormType(mTableProperties);
    if ( formType.isCollectForm() ) {
      CollectFormParameters params = formType.getCollectFormParameters();
      // Try to construct the values currently in the search bar to
      // prepopulate with the form. We're going to ignore joins. This
      // means that if there IS a join column, we'll throw an error!!!
      // So be careful.
      Intent collectAddIntent =
        CollectUtil.getIntentForOdkCollectAddRow(this, mTableProperties, params, elementNameToValue);
      if (collectAddIntent != null) {
        CollectUtil.launchCollectToAddRow(this, collectAddIntent, mTableProperties);
      }
    } else {
      SurveyFormParameters params = formType.getSurveyFormParameters();
      Intent surveyAddIntent =
          SurveyUtil.getIntentForOdkSurveyAddRow(this, mTableProperties, mAppName, params, null);
      if (surveyAddIntent != null) {
        SurveyUtil.launchSurveyToAddRow(this, surveyAddIntent, mTableProperties);
      }
    }
  }

  private int getControlWrapVisibility() {
    return findViewById(R.id.control_wrap).getVisibility();
  }

  private void setControlWrapVisiblity(int visibility) {
    findViewById(R.id.control_wrap).setVisibility(visibility);
  }

  private void handleOdkCollectAddReturn(int returnCode, Intent data) {
	if (!CollectUtil.handleOdkCollectAddReturn(this, mAppName, mTableProperties, returnCode, data)) {
      return;
    }
	refreshDbTable(mTableId);
  }

  private void handleOdkCollectEditReturn(int returnCode, Intent data) {
    if (!CollectUtil.handleOdkCollectEditReturn(this, mAppName, mTableProperties, returnCode, data)) {
      return;
    }
    refreshDbTable(mTableId);
  }

  private void handleOdkSurveyAddReturn(int returnCode, Intent data) {
    if (returnCode != SherlockActivity.RESULT_OK) {
      Log.i(t, "return code wasn't sherlock_ok, add was not finalized and will not appear.");
      return;
    }
   refreshDbTable(mTableId);
  }

  private void handleOdkSurveyEditReturn(int returnCode, Intent data) {
    if (returnCode != SherlockActivity.RESULT_OK) {
      Log.i(t, "return code wasn't sherlock_ok, add was not finalized and will not appear.");
      return;
    }
    refreshDbTable(mTableId);
  }
  /**
   * This method should launch the custom app view that is a generic user-
   * customizable home screen or html page for the app.
   */
  public void launchAppViewActivity(Context context) {

  }

  public void launchTableActivity(TableProperties tp, TableViewType viewType,
      String sqlWhereClause, String[] sqlSelectionArgs, String[] sqlGroupBy, String sqlHaving,
      String sqlOrderByElementKey, String sqlOrderByDirection) {
    if ( viewType == null ) {
      viewType = tp.getDefaultViewType();
    }
    Intent intent;
    switch (viewType) {
    case List: {
      String defaultListView = ListDisplayActivity.getDefaultListFileName(tp);
      if (defaultListView != null) {
        intent = new Intent(this, ListDisplayActivity.class);
        intent.putExtra(ListDisplayActivity.INTENT_KEY_FILENAME, defaultListView);
      } else {
        intent = new Intent(this, ListViewManager.class);
      }
    }
      break;
    case Graph: {
      String defaultGraph = GraphManagerActivity.getDefaultGraphName(tp);
      if ( defaultGraph != null ) {
        intent = new Intent(this, GraphDisplayActivity.class);
        intent.putExtra(GraphDisplayActivity.KEY_GRAPH_VIEW_NAME, defaultGraph);
      } else {
        intent = new Intent(this, GraphManagerActivity.class);
      }
    }
      break;
    case Map:
      intent = new Intent(this, TableActivity.class);
      break;
    case Spreadsheet:
      intent = new Intent(this, SpreadsheetDisplayActivity.class);
      break;
    default:
      intent = new Intent(this, SpreadsheetDisplayActivity.class);
    }
    intent.putExtra(Controller.INTENT_KEY_APP_NAME, tp.getAppName());
    intent.putExtra(Controller.INTENT_KEY_TABLE_ID, tp.getTableId());
    prepareIntentForLaunch(intent, tp, sqlWhereClause,
        sqlSelectionArgs, sqlGroupBy, sqlHaving, sqlOrderByElementKey, sqlOrderByDirection);
    startActivity(intent);
    finish();
  }

  /*
   * A helper method that was introduced just to eliminate redundant code.
   * Adds the appropriate extras to the intent.
   */
  private void prepareIntentForLaunch(Intent intent, TableProperties tp,
                                             String sqlWhereClause, String[] sqlSelectionArgs,
                                             String[] sqlGroupBy, String sqlHaving,
                                             String sqlOrderByElementKey, String sqlOrderByDirection) {
    if (sqlWhereClause != null && sqlWhereClause.length() != 0) {
      intent.putExtra(Controller.INTENT_KEY_SQL_WHERE, sqlWhereClause);
      if (sqlSelectionArgs != null && sqlSelectionArgs.length != 0) {
        intent.putExtra(Controller.INTENT_KEY_SQL_SELECTION_ARGS, sqlSelectionArgs);
      }
    }
    if (sqlGroupBy != null && sqlGroupBy.length != 0) {
      intent.putExtra(Controller.INTENT_KEY_SQL_GROUP_BY_ARGS, sqlGroupBy);
      if (sqlHaving != null && sqlHaving.length() != 0) {
        intent.putExtra(Controller.INTENT_KEY_SQL_HAVING, sqlHaving);
      }
    }
    if (sqlOrderByElementKey != null && sqlOrderByElementKey.length() != 0) {
      intent.putExtra(Controller.INTENT_KEY_SQL_ORDER_BY_ELEMENT_KEY, sqlOrderByElementKey);
      if ( sqlOrderByDirection != null ) {
        intent.putExtra(Controller.INTENT_KEY_SQL_ORDER_BY_DIRECTION, sqlOrderByDirection);
      } else {
        intent.putExtra(Controller.INTENT_KEY_SQL_ORDER_BY_DIRECTION, "ASC");
      }
    }
  }
}
