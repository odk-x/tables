package org.opendatakit.tables.activities;

import java.util.Stack;

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;

/**
 * Base activity for all fragments that display information about a database.
 * Deals with maintaining the data and the actionbar.
 *
 * @author Chris Gelon (cgelon)
 */
public class TableActivity extends SherlockFragmentActivity {
	private static final String INTENT_KEY_TABLE_ID = "tableId";
	private static final String INTENT_KEY_SEARCH = "search";
	private static final String INTENT_KEY_SEARCH_STACK = "searchStack";
	private static final String INTENT_KEY_IS_OVERVIEW = "isOverview";
    public static final String KVS_PARTITION = "TableActivity";
    public static final String MAP_COLUMN_KEY = "MapColumnKey";
    public static final String MAP_LABEL_KEY = "MapLabelKey";

    private static final int RCODE_ODKCOLLECT_ADD_ROW =
            Controller.FIRST_FREE_RCODE;

    private static final String COLLECT_INSTANCES_URI_STRING =
        "content://org.odk.collect.android.provider.odk.instances/instances";
    private static final Uri COLLECT_INSTANCES_CONTENT_URI =
            Uri.parse(COLLECT_INSTANCES_URI_STRING);

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

	/**@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //if (handleActivityReturn(requestCode, resultCode, data)) {
        //    return;
        //}
        switch (requestCode) {
        	case RCODE_ODKCOLLECT_ADD_ROW:
        		addRowFromOdkCollectForm(Integer.valueOf(data.getData().getLastPathSegment()));
        		init();
        		break;
        	default:
        		super.onActivityResult(requestCode, resultCode, data);
        }
    }

	boolean addRowFromOdkCollectForm(int instanceId) {
		Map<String, String> formValues = getOdkCollectFormValues(instanceId);
		if (formValues == null) {
			return false;
		}

		Map<String, String> values = new HashMap<String, String>();
		for (String key : formValues.keySet()) {
			ColumnProperties cp = mTableProperties.getColumnByElementKey(key);
			if (cp == null) {
				continue;
			}
			String value = du.validifyValue(cp, formValues.get(key));
			if (value != null) {
				values.put(key, value);
			}
		}
		Map<String, String> prepopulatedValues = getMapFromLimitedQuery();
		if (prepopulatedValues.equals(values)) {
			return false;
		}
		dbt.addRow(values);
		return true;
	}

	protected Map<String, String> getOdkCollectFormValues(int instanceId) {
		String[] projection = { "instanceFilePath" };
		String selection = "_id = ?";
		String[] selectionArgs = { (instanceId + "") };
		Cursor c = managedQuery(COLLECT_INSTANCES_CONTENT_URI, projection, selection,
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
	}*/
}
