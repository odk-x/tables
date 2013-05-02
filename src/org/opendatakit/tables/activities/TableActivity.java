package org.opendatakit.tables.activities;

import java.util.Stack;

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableViewType;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.fragments.TableMapFragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.ActionBar;
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
	private static final String INTENT_KEY_TABLE_ID = "tableId";
	private static final String INTENT_KEY_SEARCH_STACK = "searchStack";
	private static final String INTENT_KEY_IS_OVERVIEW = "isOverview";
    public static final String KVS_PARTITION = "TableActivity";
    public static final String MAP_COLUMN_KEY = "MapColumnKey";
    public static final String MAP_LABEL_KEY = "MapLabelKey";
    
    
    private static final int MENU_ITEM_ID_VIEW_TYPE_SUBMENU = 1;
    

	/** The current fragment being displayed. */
	private Fragment mCurrentFragment;

	/** The fragment that contains map information. */
	private TableMapFragment mMapFragment;

	/** ??? */
	private DataManager mDataManager;
	/** The query of the current database. */
	private Query mQuery;

	/** Table that represents all of the data in the query. */
	private UserTable mTable;
	private TableProperties mTableProperties;

	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.standard_table_layout);

        // Set up the data.
 		mDataManager = new DataManager(DbHelper.getDbHelper(this));
 		String tableId = getIntent().getExtras().getString(INTENT_KEY_TABLE_ID);
 		mTableProperties = mDataManager.getTableProperties(tableId, KeyValueStore.Type.ACTIVE);
 		mQuery = new Query(mDataManager.getAllTableProperties(KeyValueStore.Type.ACTIVE), mTableProperties);

        // Init some stuff?
        init();

        // Create the map fragment.
        mMapFragment = new TableMapFragment();
		mMapFragment.setTable(mTable, mTableProperties);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.main, mMapFragment).commit();

        // Set the current fragment.
        mCurrentFragment = mMapFragment;
    }

	public void init() {
		boolean isOverview = getIntent().getExtras().getBoolean(INTENT_KEY_IS_OVERVIEW, false);
		// Find the search text?

		Stack<String> searchText = getSearchTextFromIntent(INTENT_KEY_SEARCH_STACK, getIntent().getExtras());

		DbTable dataBaseTable = mDataManager.getDbTable(mTableProperties.getTableId());
		mQuery.clear();
		mQuery.loadFromUserQuery(searchText.peek());

		// Create the table.
		mTable = isOverview ?
				dataBaseTable.getUserOverviewTable(mQuery) :
				dataBaseTable.getUserTable(mQuery);
    }

	private Stack<String> getSearchTextFromIntent(String key, Bundle intent) {
		Stack<String> searchText = new Stack<String>();
        if (intent.containsKey(key)) {
            String[] searchValues = intent.getStringArray(key);
            for (String searchValue : searchValues) {
                searchText.add(searchValue);
            }
        } else {
            String initialSearchText = intent.getString(key);
            searchText.add((initialSearchText == null) ? "" :
                initialSearchText);
        }
        return searchText;
    }
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		// Set the app icon as an action to go home.
    	ActionBar actionBar = this.getSupportActionBar();
    	actionBar.setDisplayHomeAsUpEnabled(true);
    	// Set the actionbar title to nothing.
    	actionBar.setTitle("");
		
		// View type submenu.
        // Determine the possible view types.
        final TableViewType[] viewTypes = mTableProperties.getPossibleViewTypes();
        // Build a checkable submenu to select the view type.
        SubMenu viewTypeSubMenu = 
            menu.addSubMenu(Menu.NONE, MENU_ITEM_ID_VIEW_TYPE_SUBMENU, 
                Menu.NONE, "ViewType");
        MenuItem viewType = viewTypeSubMenu.getItem();
        viewType.setIcon(R.drawable.view);
        viewType.setEnabled(true);
        viewType.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        MenuItem item;
        // This will be the name of the default list view, which if exists
        // means we should display the list view as an option.
        KeyValueStoreHelper kvsh = 
            mTableProperties.getKeyValueStoreHelper(ListDisplayActivity.KVS_PARTITION);
        String nameOfView = kvsh.getString( 
            ListDisplayActivity.KEY_LIST_VIEW_NAME);
        for(int i = 0; i < viewTypes.length; i++) {
        	item = viewTypeSubMenu.add(MENU_ITEM_ID_VIEW_TYPE_SUBMENU, 
        	    viewTypes[i].getId(), i, 
        	    viewTypes[i].name());
        	// mark the current viewType as selected
          	if (mTableProperties.getCurrentViewType() == viewTypes[i]) {
          	  item.setChecked(true);
          	}
            // disable list view if no file is specified
            if (viewTypes[i] == TableViewType.List && nameOfView == null) {
            	item.setEnabled(false);
            }
        }


        viewTypeSubMenu.setGroupCheckable(MENU_ITEM_ID_VIEW_TYPE_SUBMENU, 
            true, true);
        
        return true;
    }
	
	@Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if(item.getGroupId() == MENU_ITEM_ID_VIEW_TYPE_SUBMENU) {
			mTableProperties.setCurrentViewType(TableViewType.getViewTypeFromId(item.getItemId()));
			Controller.launchTableActivity(this, mTableProperties, false);
			return true;
		}
		return false;
    }
}
