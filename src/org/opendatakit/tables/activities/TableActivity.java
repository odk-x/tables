package org.opendatakit.tables.activities;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.graphs.GraphDisplayActivity;
import org.opendatakit.tables.data.ColumnProperties;
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
import org.opendatakit.tables.fragments.ITableFragment;
import org.opendatakit.tables.fragments.TableMapFragment;
import org.opendatakit.tables.utils.CollectUtil;
import org.opendatakit.tables.utils.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.views.CellValueView;
import org.opendatakit.tables.views.ClearableEditText;
import org.opendatakit.tables.views.webkits.CustomView.CustomViewCallbacks;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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
public class TableActivity extends SherlockFragmentActivity
    implements CustomViewCallbacks{

  // / Static Strings ///
  public static final String INTENT_KEY_TABLE_ID = "tableId";
  public static final String INTENT_KEY_SEARCH = "search";
  public static final String INTENT_KEY_SEARCH_STACK = "searchStack";
  public static final String INTENT_KEY_IS_OVERVIEW = "isOverview";
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
  static final int FIRST_FREE_MENU_ITEM_ID = 8;

  // Return codes from different activities.
  private static final int RCODE_TABLE_PROPERTIES_MANAGER = 0;
  private static final int RCODE_COLUMN_MANAGER = 1;
  private static final int RCODE_ODKCOLLECT_ADD_ROW = 2;
  private static final int RCODE_ODKCOLLECT_EDIT_ROW = 3;
  private static final int RCODE_LIST_VIEW_MANAGER = 4;

  /** The current fragment being displayed. */
  private ITableFragment mCurrentFragment;

  /** The fragment that contains map information. */
  private TableMapFragment mMapFragment;

  /** Table that represents all of the data in the query. */
  private UserTable mTable;

  public UserTable getTable() {
    return mTable;
  }

  /** The properties of the user table. */
  private TableProperties mTableProperties;

  public TableProperties getTableProperties() {
    return mTableProperties;
  }

  private Query mQuery;

  private String mRowId;

  private DataUtil mDataUtil;
  private DbHelper mDbh;
  private DbTable mDbTable;
  private Stack<String> mSearchText;
  private boolean mIsOverview;
  private Activity mActivity;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.standard_table_layout);

    mActivity = this;

    // Set up the data utility.
    mDataUtil = DataUtil.getDefaultDataUtil();

    // Find the table id.
    String tableId = getIntent().getExtras().getString(INTENT_KEY_TABLE_ID);
    if (tableId == null) {
      throw new RuntimeException("Table id was not passed in through the bundle.");
    }

    // Add the search texts.
    mSearchText = new Stack<String>();
    if (getIntent().getExtras().containsKey(INTENT_KEY_SEARCH_STACK)) {
      String[] searchValues = getIntent().getExtras().getStringArray(INTENT_KEY_SEARCH_STACK);
      for (String searchValue : searchValues) {
        mSearchText.add(searchValue);
      }
    } else {
      String initialSearchText = getIntent().getExtras().getString(INTENT_KEY_SEARCH);
      mSearchText.add((initialSearchText == null) ? "" : initialSearchText);
    }

    mIsOverview = getIntent().getExtras().getBoolean(INTENT_KEY_IS_OVERVIEW, false);

    // Initialize data objects.
    mDbh = DbHelper.getDbHelper(this);
    refreshDbTable(tableId);
    mQuery = new Query(mDbh, KeyValueStore.Type.ACTIVE,
        mTableProperties);

    // Initialize layout fields.
    setSearchFieldText(mSearchText.peek());
    setInfoBarText("Table: " + mTableProperties.getDisplayName());

    mQuery.clear();
    mQuery.loadFromUserQuery(mSearchText.peek());

    // There are two options here. The first is that we get the data using the
    // {@link Query} object. The other is that we use a sql where clause. The
    // two currently don't play nice together, so figure out which one. The
    // sql statement gets precedence.
    String sqlWhereClause =
        getIntent().getExtras().getString(Controller.INTENT_KEY_SQL_WHERE);
    if (sqlWhereClause != null) {
      String[] sqlSelectionArgs = getIntent().getExtras().getStringArray(
          Controller.INTENT_KEY_SQL_SELECTION_ARGS);
      mTable = mDbTable.rawSqlQuery(sqlWhereClause, sqlSelectionArgs);
    } else {
      // We use the query.
      mTable = mIsOverview ? mDbTable.getUserOverviewTable(mQuery) :
        mDbTable.getUserTable(mQuery);
    }

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

  public void init() {
    refreshDbTable(mTableProperties.getTableId());
    mQuery = new Query(mDbh, KeyValueStore.Type.ACTIVE,
        mTableProperties);
    mQuery.clear();
    mQuery.loadFromUserQuery(mSearchText.peek());
    mTable = mIsOverview ? mDbTable.getUserOverviewTable(mQuery) : mDbTable.getUserTable(mQuery);
    mCurrentFragment.init();
  }

  public void onSearchButtonClick(View v) {
    // when you click the search button, save that query.
    KeyValueStoreHelper kvsh = mTableProperties
        .getKeyValueStoreHelper(TableProperties.KVS_PARTITION);
    kvsh.setString(TableProperties.KEY_CURRENT_QUERY, getSearchFieldText());
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
   * Appends the text to the search box.
   */
  public void appendToSearchBoxText(String text) {
    setSearchFieldText((getSearchFieldText() + text).trim());
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
    mTableProperties = TableProperties.getTablePropertiesForTable(mDbh,
        tableId,
        KeyValueStore.Type.ACTIVE);
    mDbTable = DbTable.getDbTable(mDbh, mTableProperties);
  }

  /**
   * @return DbTable this data table
   */
  DbTable getDbTable() {
    mTableProperties.refreshColumns();
    return mDbTable;
  }

  /**
   * @return True if this is an overview type, false if this is collection view
   *         type
   */
  boolean getIsOverview() {
    return mIsOverview;
  }

  /**
   * @return String text currently in the search bar
   */
  String getSearchText() {
    return mSearchText.peek();
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

  void recordSearch() {
    mSearchText.add(getSearchFieldText());
  }

  public void onBackPressed() {
    if (mSearchText.size() == 1) {
      finish();
    } else {
      mSearchText.pop();
      setSearchFieldText(mSearchText.peek());
    }
  }

  /**
   * This should launch Collect to edit the data for the row. If there is a
   * custom form defined for the table, its info should be passed in. Otherwise,
   * null values will cause the default form to be generated, which is
   * just every column with its own entry field on a single screen.
   *
   * @param table
   * @param rowNum
   * @param formId
   * @param formVersion
   * @param formRootElement
   */
  void editRow(UserTable table, int rowNum,
          String formId, String formVersion, String formRootElement) {
    Map<String, String> elementKeyToValue = new HashMap<String, String>();
    for (ColumnProperties cp : mTableProperties.getColumns().values()) {
      String value = table.getData(rowNum, mTableProperties.getColumnIndex(
          cp.getElementKey()));
      elementKeyToValue.put(cp.getElementKey(), value);
    }
    Intent collectEditIntent =
    	CollectUtil.getIntentForOdkCollectEditRow(this, mTableProperties,
    	    elementKeyToValue, formId, formVersion, formRootElement, mRowId,
    	    table.getInstanceName(rowNum));
    if (collectEditIntent != null) {
      mRowId = table.getRowId(rowNum);
      CollectUtil.launchCollectToEditRow(this, collectEditIntent, mRowId);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
    case RCODE_TABLE_PROPERTIES_MANAGER:
      handleTablePropertiesManagerReturn();
      break;
    case RCODE_COLUMN_MANAGER:
      handleColumnManagerReturn();
      break;
    case RCODE_ODKCOLLECT_ADD_ROW:
      handleOdkCollectAddReturn(resultCode, data);
      break;
    case RCODE_ODKCOLLECT_EDIT_ROW:
      handleOdkCollectEditReturn(resultCode, data);
      break;
    case RCODE_LIST_VIEW_MANAGER:
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
    TableViewType oldViewType = mTableProperties.getCurrentViewType();
    refreshDbTable(mTableProperties.getTableId());
    if (oldViewType == mTableProperties.getCurrentViewType()) {
      init();
    } else {
      launchTableActivity(this, mTableProperties, mSearchText, mIsOverview);
    }
  }

  private void handleColumnManagerReturn() {
    refreshDbTable(mTableProperties.getTableId());
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
      if (mTableProperties.getCurrentViewType() == viewTypes[i]) {
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
    if (mTableProperties.getCurrentViewType() == TableViewType.List) {
      display.setEnabled(false);
    }
    settings.add(Menu.NONE, MENU_ITEM_ID_OPEN_TABLE_PROPERTIES, Menu.NONE, "Table Properties")
        .setEnabled(true);
    settings.add(Menu.NONE, MENU_ITEM_ID_OPEN_COLUMN_MANAGER, Menu.NONE, "Column Manager")
        .setEnabled(true);
    // Now an option for editing list views.
    MenuItem manageListViews = settings.add(Menu.NONE, MENU_ITEM_ID_OPEN_LIST_VIEW_MANAGER,
        Menu.NONE, "List View Manager").setEnabled(true);
    // TODO: add manageListViews to the menu?
    return true;
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    // If the item is part of the sub-menu for view type, set the view type
    // with its itemId else, handle accordingly.
    if (item.getGroupId() == MENU_ITEM_ID_VIEW_TYPE_SUBMENU) {
      mTableProperties.setCurrentViewType(TableViewType.getViewTypeFromId(item.getItemId()));
      launchTableActivity(this, mTableProperties, mSearchText, mIsOverview);
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
        if (!getSearchText().equals("")) {
          addRow(getMapFromLimitedQuery());
        } else {
          addRow(null);
        }
        return true;
      case MENU_ITEM_ID_SETTINGS_SUBMENU:
        return true;
      case MENU_ITEM_ID_DISPLAY_PREFERENCES:
        Intent k = new Intent(this, DisplayPrefsActivity.class);
        k.putExtra(DisplayPrefsActivity.INTENT_KEY_TABLE_ID, mTableProperties.getTableId());
        startActivity(k);
        return true;
      case MENU_ITEM_ID_OPEN_TABLE_PROPERTIES:
        Intent tablePropertiesIntent = new Intent(this, TablePropertiesManager.class);
        tablePropertiesIntent.putExtra(TablePropertiesManager.INTENT_KEY_TABLE_ID,
            mTableProperties.getTableId());
        startActivityForResult(tablePropertiesIntent, RCODE_TABLE_PROPERTIES_MANAGER);
        return true;
      case MENU_ITEM_ID_OPEN_COLUMN_MANAGER:
        Intent columnManagerIntent = new Intent(this, ColumnManager.class);
        columnManagerIntent.putExtra(ColumnManager.INTENT_KEY_TABLE_ID,
            mTableProperties.getTableId());
        startActivityForResult(columnManagerIntent, RCODE_COLUMN_MANAGER);
        return true;
      case MENU_ITEM_ID_OPEN_LIST_VIEW_MANAGER:
        Intent listViewManagerIntent = new Intent(this, ListViewManager.class);
        listViewManagerIntent.putExtra(ListViewManager.INTENT_KEY_TABLE_ID,
            mTableProperties.getTableId());
        startActivityForResult(listViewManagerIntent, RCODE_LIST_VIEW_MANAGER);
        return true;
      case android.R.id.home:
        Intent tableManagerIntent = new Intent(this, TableManager.class);
        // Add this flag so that you don't back from TableManager back
        // into the table.
        tableManagerIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(tableManagerIntent);
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
    CollectFormParameters params = CollectUtil.CollectFormParameters
        .constructCollectFormParameters(mTableProperties);
    // Try to construct the values currently in the search bar to
    // prepopulate with the form. We're going to ignore joins. This
    // means that if there IS a join column, we'll throw an error!!!
    // So be careful.
    Intent collectAddIntent =
      CollectUtil.getIntentForOdkCollectAddRow(this, mTableProperties, params, elementNameToValue);
    if (collectAddIntent != null) {
      CollectUtil.launchCollectToAddRow(this, collectAddIntent, mTableProperties);
    }
  }

  private int getControlWrapVisibility() {
    return findViewById(R.id.control_wrap).getVisibility();
  }

  private void setControlWrapVisiblity(int visibility) {
    findViewById(R.id.control_wrap).setVisibility(visibility);
  }

  private Map<String, String> getMapFromLimitedQuery() {
    Map<String, String> elementKeyToValue = new HashMap<String, String>();
    // First add all empty strings. We will overwrite the ones that are
    // queried
    // for in the search box. We need this so that if an add is canceled, we
    // can check for equality and know not to add it. If we didn't do this,
    // but we've prepopulated an add with a query, when we return and don't
    // do
    // a check, we'll add a blank row b/c there are values in the key value
    // pairs, even though they were our prepopulated values.
    for (ColumnProperties cp : mTableProperties.getColumns().values()) {
      elementKeyToValue.put(cp.getElementKey(), "");
    }
    Query currentQuery = new Query(mDbh, KeyValueStore.Type.ACTIVE, mTableProperties);
    currentQuery.loadFromUserQuery(getSearchText());
    for (int i = 0; i < currentQuery.getConstraintCount(); i++) {
      Constraint constraint = currentQuery.getConstraint(i);
      // NB: This is predicated on their only ever being a single
      // search value. I'm not sure how additional values could be
      // added.
      elementKeyToValue.put(constraint.getColumnDbName(), constraint.getValue(0));
    }
    return elementKeyToValue;
  }

  private void handleOdkCollectAddReturn(int returnCode, Intent data) {
	if (!CollectUtil.handleOdkCollectAddReturn(this, mTableProperties, returnCode, data)) {
      return;
    }
	// TODO: refresh display???
	refreshDbTable(mTableProperties.getTableId());
  }

  private void handleOdkCollectEditReturn(int returnCode, Intent data) {
    if (!CollectUtil.handleOdkCollectEditReturn(this, mTableProperties, returnCode, data)) {
      return;
    }
    mRowId = null;
    // TODO: refresh display???
    refreshDbTable(mTableProperties.getTableId());
  }

  /** TODO: What does this do? */
  void openCellEditDialog(String rowId, String value, int colIndex) {
    (new CellEditDialog(rowId, value, colIndex)).show();
  }

  public static void launchTableActivity(Context context, TableProperties tp, boolean isOverview) {
    launchTableActivity(context, tp, null, null, isOverview, null);
  }

  public static void launchTableActivity(Context context, TableProperties tp, String searchText,
      boolean isOverview) {
    launchTableActivity(context, tp, searchText, null, isOverview, null);
  }

  private static void launchTableActivity(Activity context, TableProperties tp,
      Stack<String> searchStack, boolean isOverview) {
    launchTableActivity(context, tp, null, searchStack, isOverview, null);
    context.finish();
  }

  /**
   * This is based on the other launch table activity methods. This one,
   * however, allows a filename to be passed to the launching activity. This is
   * intended to be used to launch things like list view activities with a file
   * other than the default.
   */
  public static void launchTableActivityWithFilename(Activity context, TableProperties tp,
      Stack<String> searchStack, boolean isOverview, String filename) {
    launchTableActivity(context, tp, null, searchStack, isOverview, filename);
    context.finish();
  }

  /**
   * This method should launch the custom app view that is a generic user-
   * customizable home screen or html page for the app.
   */
  public static void launchAppViewActivity(Context context) {

  }

  private static void launchTableActivity(Context context, TableProperties tp, String searchText,
      Stack<String> searchStack, boolean isOverview, String filename) {
    // TODO: need to figure out how CollectionViewSettings should work.
    // make them work.
    // TableViewSettings tvs = isOverview ? tp.getOverviewViewSettings() :
    // tp
    // .getCollectionViewSettings();
    TableViewType viewType = tp.getCurrentViewType();
    Intent intent;
    // switch (tvs.getViewType()) {
    switch (viewType) {
    // case TableViewSettings.Type.LIST:
    // TODO: figure out which of these graph was originally and update it.
    case List:
      intent = new Intent(context, ListDisplayActivity.class);
      if (filename != null) {
        intent.putExtra(ListDisplayActivity.INTENT_KEY_FILENAME, filename);
      }
      break;
    // case TableViewSettings.Type.LINE_GRAPH:
    // intent = new Intent(context, LineGraphDisplayActivity.class);
    // break;
    // case TableViewSettings.Type.BOX_STEM:
    // intent = new Intent(context, BoxStemGraphDisplayActivity.class);
    // break;
    // case TableViewSettings.Type.BAR_GRAPH:
    case Graph:
      intent = new Intent(context, GraphDisplayActivity.class);
      break;
    // case TableViewSettings.Type.MAP:
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
    if (searchStack != null) {
      String[] stackValues = new String[searchStack.size()];
      for (int i = 0; i < searchStack.size(); i++) {
        stackValues[i] = searchStack.get(i);
      }
      intent.putExtra(INTENT_KEY_SEARCH_STACK, stackValues);
    } else if (searchText != null) {
      intent.putExtra(INTENT_KEY_SEARCH, searchText);
    } else if (searchText == null) {
      KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(TableProperties.KVS_PARTITION);
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
      UserTable table, int rowNum) {
    Intent intent = new Intent(context, DetailDisplayActivity.class);
    intent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
    intent.putExtra(DetailDisplayActivity.INTENT_KEY_ROW_ID, table.getRowId(rowNum));
    context.startActivity(intent);
  }

  private class CellEditDialog extends AlertDialog {
    private final String rowId;
    private final int colIndex;
    private final String mColumnElementKey;
    private final CellValueView.CellEditView cev;

    public CellEditDialog(String rowId, String value, int colIndex) {
      super(mActivity);

      this.rowId = rowId;
      this.colIndex = colIndex;
      ColumnProperties cp = mTableProperties.getColumnByIndex(colIndex);
      this.mColumnElementKey = cp.getElementKey();
      cev = CellValueView.getCellEditView(mActivity, cp, value);
      buildView(mActivity);
    }

    private void buildView(Context context) {
      Button setButton = new Button(context);
      setButton.setText(getResources().getString(R.string.set));
      setButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          String value = mDataUtil.validifyValue(
              mTableProperties.getColumnByElementKey(mColumnElementKey),
              cev.getValue());
          if (value == null) {
            // TODO: alert the user
            return;
          }
          Map<String, String> values = new HashMap<String, String>();
          values.put(mColumnElementKey, value);
          // TODO: Update these nulls.
          mDbTable.updateRow(rowId, values, null, null, null, null, null);
          dismiss();
        }
      });
      Button cancelButton = new Button(context);
      cancelButton.setText(getResources().getString(R.string.cancel));
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

  @Override
  public String getSearchString() {
    return getSearchFieldText();
  }
}