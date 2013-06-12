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
package org.opendatakit.tables.views.webkits;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.activities.CustomHomeScreenActivity;
import org.opendatakit.tables.activities.TableManager;
import org.opendatakit.tables.data.ColorRuleGroup;
import org.opendatakit.tables.data.ColorRuleGroup.ColorGuide;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableType;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.utils.CollectUtil;
import org.opendatakit.tables.utils.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.utils.TableFileUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebStorage.QuotaUpdater;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public abstract class CustomView extends LinearLayout {

	protected static WebView webView;
	private static ViewGroup lastParent;
	private Activity mParentActivity;

	protected CustomView(Activity parentActivity) {
		super(parentActivity);
		initCommonWebView(parentActivity);
		this.mParentActivity = parentActivity;
	}

	public static void initCommonWebView(Context context) {
		if (webView != null) {
			// do this every time to try and clear the old data.
			// webView.clearView();
			// webView.loadData(CustomViewUtil.LOADING_HTML_MESSAGE,
			// "text/html",
			// null);
			return;
		}
		webView = new WebView(context);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(new WebViewClient() {

			@Override
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				super.onReceivedError(view, errorCode, description, failingUrl);
				Log.e("CustomView", "onReceivedError: " + description + " at "
						+ failingUrl);
			}
		});

		webView.setWebChromeClient(new WebChromeClient() {

			@Override
			public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
				Log.i("CustomView",
						"onConsoleMessage "
								+ consoleMessage.messageLevel().name()
								+ consoleMessage.message());

				return super.onConsoleMessage(consoleMessage);
			}

			@Override
			@Deprecated
			public void onConsoleMessage(String message, int lineNumber,
					String sourceID) {
				// TODO Auto-generated method stub
				super.onConsoleMessage(message, lineNumber, sourceID);
				Log.i("CustomView", "onConsoleMessage " + message);
			}

			@Override
			public void onReachedMaxAppCacheSize(long requiredStorage,
					long quota, QuotaUpdater quotaUpdater) {
				// TODO Auto-generated method stub
				super.onReachedMaxAppCacheSize(requiredStorage, quota,
						quotaUpdater);
				Log.i("CustomView",
						"onReachedMaxAppCacheSize " + Long.toString(quota));
			}
		});
	}

	protected void initView() {
		if (lastParent != null) {
			lastParent.removeView(webView);
		}
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT);
		addView(webView, lp);
		lastParent = this;
	}

	protected void load(String url) {
		webView.clearView();
		webView.loadUrl(url);
	}

	protected void loadData(String data, String mimeType, String encoding) {
		webView.clearView();
		webView.loadData(data, mimeType, encoding);
	}

	/**
	 * Get the activity that contains the view.
	 *
	 * @return
	 */
	protected Activity getContainerActivity() {
		return this.mParentActivity;
	}

	/**
	 * Add a row using collect and the default form.
	 *
	 * @param tableName
	 * @param tp
	 */
	private void addRowWithCollect(String tableName, TableProperties tp) {
		CollectFormParameters formParameters = CollectFormParameters
				.constructCollectFormParameters(tp);
		prepopulateRowAndLaunchCollect(formParameters, tp);
	}

	/**
	 * Add a row using Collect. This is the hook into the javascript. The
	 * activity holding this view must have implemented the onActivityReturn
	 * method appropriately to handle the result.
	 * <p>
	 * It allows you to specify a form other than that which may be the default
	 * for the table. It differs in {@link #addRow(String)} in that it lets you
	 * add the row using an arbitrary form.
	 */
	private void addRowWithCollectAndSpecificForm(String tableName,
			String formId, String formVersion, String formRootElement,
			TableProperties tp) {
		// TODO: should these add methods be moved to the TableData and RowData
		// objects?
		CollectFormParameters formParameters = CollectFormParameters
				.constructCollectFormParameters(tp);
		if (formId != null && !formId.equals("")) {
			formParameters.setFormId(formId);
		}
		if (formVersion != null && !formVersion.equals("")) {
			formParameters.setFormVersion(formVersion);
		}
		if (formRootElement != null && !formRootElement.equals("")) {
			formParameters.setRootElement(formRootElement);
		}
		prepopulateRowAndLaunchCollect(formParameters, tp);
	}

	/**
	 * This is called by the internal data classes. It prepopulates the form as
	 * it needs based on the query and launches the form.
	 *
	 * @param params
	 * @param tp
	 */
	private void prepopulateRowAndLaunchCollect(CollectFormParameters params,
			TableProperties tp) {
		String currentQueryString = tp.getKeyValueStoreHelper(
				TableProperties.KVS_PARTITION).getString(
				TableProperties.KEY_CURRENT_QUERY);

		Intent addRowIntent = CollectUtil.getIntentForOdkCollectAddRowByQuery(
				CustomView.this.getContainerActivity(), tp, params,
				currentQueryString);

		CustomView.this.getContainerActivity().startActivityForResult(
				addRowIntent, Controller.RCODE_ODKCOLLECT_ADD_ROW);
	}

	/**
	 * "Unused" warnings are suppressed because the public methods of this class
	 * are meant to be called through the JavaScript interface.
	 */
	protected class RowData {

		public final String TAG = RowData.class.getSimpleName();

		private final TableProperties tp;
		private Map<String, String> data;
		private String mRowId;

		RowData(TableProperties tp) {
			this.tp = tp;
		}

		RowData(TableProperties tp, Map<String, String> data) {
			this.tp = tp;
			this.data = data;
		}

		void set(String rowId, Map<String, String> data) {
			this.data = data;
			this.mRowId = rowId;
		}

		/**
		 * Edit the row with collect. Uses the form specified in the table
		 * properties or else the ODKTables-generated default form.
		 */
		public void editRowWithCollect() {
			Intent editRowIntent = CollectUtil.getIntentForOdkCollectEditRow(
					CustomView.this.getContainerActivity(), tp, data, null,
					null, null);
			CollectUtil.launchCollectToEditRow(
					CustomView.this.getContainerActivity(), editRowIntent,
					mRowId);
		}

		/**
		 * Edit the row with collect.
		 * <p>
		 * Similar to {@link #editRowWithCollect()}, except that it allows you
		 * to edit the row with a specific form.
		 *
		 * @param tableName
		 * @param formId
		 * @param formVersion
		 * @param formRootElement
		 */
		public void editRowWithCollectAndSpecificForm(String formId,
				String formVersion, String formRootElement) {
			Intent editRowIntent = CollectUtil.getIntentForOdkCollectEditRow(
					CustomView.this.getContainerActivity(), tp, data, formId,
					formVersion, formRootElement);
			// We have to launch it through this method so that the rowId is
			// persisted in the SharedPreferences.
			CollectUtil.launchCollectToEditRow(
					CustomView.this.getContainerActivity(), editRowIntent,
					mRowId);
		}

		/**
		 * Add a row using collect and the default form.
		 *
		 * @param tableName
		 */
		public void addRowWithCollect(String tableName) {
			CustomView.this.addRowWithCollect(tableName, tp);
		}

		/**
		 * Add a row using Collect. This is the hook into the javascript. The
		 * activity holding this view must have implemented the onActivityReturn
		 * method appropriately to handle the result.
		 * <p>
		 * It allows you to specify a form other than that which may be the
		 * default for the table. It differs in {@link #addRow(String)} in that
		 * it lets you add the row using an arbitrary form.
		 */
		public void addRowWithCollectAndSpecificForm(String tableName,
				String formId, String formVersion, String formRootElement) {
			CustomView.this.addRowWithCollectAndSpecificForm(tableName, formId,
					formVersion, formRootElement, tp);
		}

		/**
		 * Takes the user label for the column and returns the value in that
		 * column. Any null values are replaced by the empty string.
		 * <p>
		 * Returns null if the column matching the passed in user label could
		 * not be found.
		 *
		 * @param key
		 * @return
		 */
		public String get(String key) {
			ColumnProperties cp = tp.getColumnByUserLabel(key);
			if (cp == null) {
				return null;
			}
			String result = data.get(cp.getElementKey());
			if (result == null) {
				return "";
			} else {
				return result;
			}
		}

	}

	/**
	 * "Unused" warnings are suppressed because the public methods of this class
	 * are meant to be called through the JavaScript interface.
	 */
	protected class TableData {

		private static final String TAG = "TableData";

		private final UserTable mTable;
		private Map<String, Integer> colMap; // Maps the column names with an
												// index number
		private Map<Integer, Integer> collectionMap; // Maps each collection
														// with the number of
														// rows under it
		private List<String> primeColumns; // Holds the db names of indexed
											// columns
		/**
		 * A simple cache of color rules so they're not recreated unnecessarily
		 * each time. Maps the column display name to {@link ColorRuleGroup} for
		 * that column.
		 */
		private Map<String, ColorRuleGroup> mColumnDisplayNameToColorRuleGroup;
		protected Context mContext;
		private TableProperties tp;

		public TableData(Context context, TableProperties tp, UserTable table) {
			Log.d(TAG, "calling TableData constructor with Table");
			this.mContext = context;
			this.mTable = table;
			this.tp = tp;
			initMaps(tp);
		}

		public boolean inCollectionMode() {
			if (!isIndexed()) {
				return false;
			}

			// Test 1: Check that every cell value under the indexed column are
			// equal (characteristic of a collection)
			String test = getData(0, primeColumns.get(0).substring(1));
			for (int i = 1; i < getCount(); i++) {
				if (!getData(i, primeColumns.get(0).substring(1)).equals(test)) {
					return false;
				}
			}

			// Test 2: The number of rows in the table equal the number of rows
			// in the (corresponding) collection
			return (getCount() == collectionMap.get(0));
		}

		public TableData(TableProperties tp, UserTable table) {
			Log.d(TAG, "calling TableData constructor with UserTable");
			this.mTable = table;
			this.tp = tp;
			initMaps(tp);

			// The collectionMap will be initialized if the table is indexed.
			if (isIndexed()) {
				initCollectionMap(tp);
			}
		}

		// Initializes the colMap and primeColumns that provide methods quick
		// access to the current table's state.
		private void initMaps(TableProperties tp) {
			mColumnDisplayNameToColorRuleGroup = new HashMap<String, ColorRuleGroup>();
			primeColumns = tp.getPrimeColumns();
			Map<String, ColumnProperties> elementKeyToColumnProperties = tp
					.getColumns();
			Map<String, Integer> ekToIndex = mTable.getMapOfUserDataToIndex();
			colMap = new HashMap<String, Integer>();
			for (ColumnProperties cp : elementKeyToColumnProperties.values()) {
				String smsLabel = cp.getSmsLabel();
				colMap.put(cp.getDisplayName(),
						ekToIndex.get(cp.getElementKey()));
				if (smsLabel != null) {
					// TODO: this doesn't look to ever be used, and ignores the
					// possibility
					// of conflicting element keys and sms labels.
					colMap.put(smsLabel, colMap.get(cp.getElementKey()));
				}
			}
		}

		// Returns the number of rows in the table being viewed.
		public int getCount() {
			return this.mTable.getHeight();
		}

		/*
		 * @param: colName, column name in the userTable/rawTable
		 *
		 * @return: returns a String in JSONArray format containing all the row
		 * data for the given column name format: [row1, row2, row3, row4]
		 */
		public String getColumnData(String colName) {
			ArrayList<String> arr = new ArrayList<String>();
			for (int i = 0; i < getCount(); i++) {
				if (colMap.containsKey(colName)) {
					arr.add(i, mTable.getData(i, colMap.get(colName)));
				} else {
					arr.add(i, "");
				}
			}
			return new JSONArray(arr).toString();
		}

		public String getColumns() {
			Map<String, String> colInfo = new HashMap<String, String>();
			for (String column : colMap.keySet()) {
				ColumnProperties cp = tp.getColumnByDisplayName(column);
				String dbName = cp.getElementKey();
				String label = tp.getColumnByElementKey(dbName).getColumnType()
						.label();
				colInfo.put(column, label);
			}
			return new JSONObject(colInfo).toString();
		}

		/**
		 * Get the foreground color for the given value according to the color
		 * rules for the column specified by colName. The default is -16777216.
		 *
		 * @param colName
		 *            the display name of the column
		 * @param value
		 *            the string value of the datum
		 * @return
		 */
		public String getForegroundColor(String colName, String value) {
			ColumnProperties cp = tp.getColumnByDisplayName(colName);
			String elementKey = cp.getElementKey();
			ColorRuleGroup colRul = this.mColumnDisplayNameToColorRuleGroup
					.get(colName);
			if (colRul == null) {
				// If it's not already there, cache it for future use.
				colRul = ColorRuleGroup.getColumnColorRuleGroup(tp, elementKey);
				this.mColumnDisplayNameToColorRuleGroup.put(colName, colRul);
			}
			// Rather than hand off the whole row data, we'll just dummy up the
			// info requested, as this will be easier for the html programmer
			// to use than to have to give in the whole row.
			Map<String, Integer> indexOfDataMap = new HashMap<String, Integer>();
			indexOfDataMap.put(elementKey, 0);
			Map<String, Integer> indexOfMetadataMap = new HashMap<String, Integer>();
			indexOfMetadataMap.put(elementKey, 0);
			// We need to construct a dummy UserTable for the ColorRule to
			// interpret.
			String[] header = new String[] { colName };
			String[] rowId = new String[] { "dummyRowId" };
			String[][] data = new String[1][1];
			String[][] metadata = new String[1][1];
			data[0][0] = value;
			metadata[0][0] = "dummyMetadata";
			UserTable table = new UserTable(tp, rowId, header, data,
					indexOfDataMap, metadata, indexOfMetadataMap, null);
			ColorGuide guide = colRul.getColorGuide(table.getRowAtIndex(0));
			int foregroundColor;
			if (guide.didMatch()) {
				foregroundColor = guide.getForeground();
			} else {
				foregroundColor = -16777216; // this crazy value was found here
			}
			// I think this formatting needs to take place for javascript
			return String.format("#%06X", (0xFFFFFF & foregroundColor));
		}

		// Maps the number of rows to every collection of a table.
		private void initCollectionMap(TableProperties tp) {
			Control c = new Control(mContext);
			collectionMap = new HashMap<Integer, Integer>();
			String colName = primeColumns.get(0).substring(1); // Assumes that
																// the first col
																// is the main,
																// indexed col
			for (String col : colMap.keySet()) {
				if (col.equalsIgnoreCase(colName)) {
					colName = col;
				}
			}
			// Queries the original table for the rows in every collection and
			// stores the number of resulting rows for each.
			for (int i = 0; i < getCount(); i++) {
				String tableName = tp.getDisplayName();
				String searchText = colName + ":" + getData(i, colName);
				TableData data = c.query(tableName, searchText);
				collectionMap.put(i, data.getCount());
			}
		}

		// Returns the number of rows in the collection at the given row index.
		public int getCollectionSize(int rowNum) {
			return collectionMap.get(rowNum);
		}

		// Returns whether the table is indexed.
		public boolean isIndexed() {
			return (!primeColumns.isEmpty());
		}

		/**
		 * Returns the value of the column with the given user-label at the
		 * given row number. Null values are returned as the empty string.
		 * <p>
		 * Null is returned if the column could not be found.
		 *
		 * @param rowNum
		 * @param colName
		 * @return
		 */
		public String getData(int rowNum, String colName) {
			if (colMap.containsKey(colName)) {
				String result = mTable.getData(rowNum, colMap.get(colName));
				if (result == null) {
					return "";
				} else {
					return result;
				}
			} else {
				return null;
			}
		}

		/**
		 * Edit the row with collect. Uses the form specified in the table
		 * properties or else the ODKTables-generated default form.
		 *
		 * @param rowNumber
		 *            the number of the row to edit.
		 */
		public void editRowWithCollect(int rowNumber) {
			String rowId = this.mTable.getRowId(rowNumber);
			Map<String, String> elementKeyToValue = getElementKeyToValueMapForRow(rowNumber);
			Intent editRowIntent = CollectUtil.getIntentForOdkCollectEditRow(
					CustomView.this.getContainerActivity(), tp,
					elementKeyToValue, null, null, null);
			CollectUtil.launchCollectToEditRow(
					CustomView.this.getContainerActivity(), editRowIntent,
					rowId);
		}

		/**
		 * Edit the row with collect.
		 * <p>
		 * Similar to {@link #editRowWithCollect()}, except that it allows you
		 * to edit the row with a specific form.
		 *
		 * @param rowNumber
		 *            the number of the row to be edited
		 * @param tableName
		 * @param formId
		 * @param formVersion
		 * @param formRootElement
		 */
		public void editRowWithCollectAndSpecificForm(int rowNumber,
				String formId, String formVersion, String formRootElement) {
			String rowId = this.mTable.getRowId(rowNumber);
			Map<String, String> elementKeyToValue = getElementKeyToValueMapForRow(rowNumber);
			Intent editRowIntent = CollectUtil.getIntentForOdkCollectEditRow(
					CustomView.this.getContainerActivity(), tp,
					elementKeyToValue, formId, formVersion, formRootElement);
			CollectUtil.launchCollectToEditRow(
					CustomView.this.getContainerActivity(), editRowIntent,
					rowId);
		}

		/**
		 * Add a row using collect and the default form.
		 *
		 * @param tableName
		 */
		public void addRowWithCollect(String tableName) {
			CustomView.this.addRowWithCollect(tableName, tp);
		}

		/**
		 * Add a row using Collect. This is the hook into the javascript. The
		 * activity holding this view must have implemented the onActivityReturn
		 * method appropriately to handle the result.
		 * <p>
		 * It allows you to specify a form other than that which may be the
		 * default for the table. It differs in {@link #addRow(String)} in that
		 * it lets you add the row using an arbitrary form.
		 */
		public void addRowWithCollectAndSpecificForm(String tableName,
				String formId, String formVersion, String formRootElement) {
			CustomView.this.addRowWithCollectAndSpecificForm(tableName, formId,
					formVersion, formRootElement, tp);
		}

		/**
		 * Get a map of elementKey to value for the given row number.
		 *
		 * @param rowNum
		 * @return
		 */
		private Map<String, String> getElementKeyToValueMapForRow(int rowNum) {
			Map<String, String> elementKeyToValue = new HashMap<String, String>();
			for (Entry<String, Integer> entry : colMap.entrySet()) {
				ColumnProperties cp = tp.getColumnByDisplayName(entry.getKey());
				String elementKey = cp.getElementKey();
				String value = this.mTable.getData(rowNum, entry.getValue());
				elementKeyToValue.put(elementKey, value);
			}
			return elementKeyToValue;
		}

	}

	protected class Control {

		private static final String TAG = "CustomView.Control";

		protected Context mContext;
		private TableProperties[] allTps;
		private Map<String, TableProperties> tpMap;
		private DbHelper dbh;

		/**
		 * This construct requires an activity rather than a context because we
		 * want to be able to launch intents for result rather than merely
		 * launch them on their own.
		 *
		 * @param activity
		 *            the activity that will be holding the view
		 */
		public Control(Context context) {
			this.mContext = context;
			dbh = DbHelper.getDbHelper(mContext);
			Log.d(TAG, "calling Control Constructor");
		}

		private void initTpInfo() {
			if (tpMap != null) {
				return;
			}
			tpMap = new HashMap<String, TableProperties>();
			allTps = TableProperties.getTablePropertiesForAll(
					DbHelper.getDbHelper(mContext), KeyValueStore.Type.ACTIVE);
			for (TableProperties tp : allTps) {
				tpMap.put(tp.getDisplayName(), tp);
			}
		}

		/**
		 * Opens the table specified by the tableName and searches this table
		 * with the given query. Uses the default view specified on the table.
		 * E.g. if it has been set to map view, the table will be opened to the
		 * map view.
		 *
		 * @param tableName
		 * @param query
		 * @return
		 */
		public boolean openTable(String tableName, String query) {
			Log.d(TAG, "in openTable for table: " + tableName);
			initTpInfo();
			if (!tpMap.containsKey(tableName)) {
				Log.e(TAG, "tableName [" + tableName + "] not in map");
				return false;
			}
			Log.e(TAG, "launching table activity for " + tableName);
			Controller.launchTableActivity(mContext, tpMap.get(tableName),
					query, false);
			return true;
		}

		/**
		 * Open the table specified by tableName as a list view with the
		 * filename specified by filename. The filename is relative to the odk
		 * tables path.
		 *
		 * @param tableName
		 * @param filename
		 * @return false if the table properties cannot be found, true if it
		 *         opens.
		 */
		public boolean openTableToListViewWithFile(String tableName,
				String searchText, String filename) {
			initTpInfo();
			TableProperties tp = tpMap.get(tableName);
			if (tp == null) {
				Log.e(TAG, "tableName [" + tableName + "] not in map");
				return false;
			}
			String pathToTablesFolder = ODKFileUtils
					.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME);
			String pathToFile = pathToTablesFolder + File.separator + filename;
			Controller.launchListViewWithFileName(mContext, tp, searchText,
					null, false, pathToFile);
			return true;
		}

		public TableData query(String tableName, String searchText) {
			initTpInfo();
			if (!tpMap.containsKey(tableName)) {
				return null;
			}
			TableProperties tp = tpMap.get(tableName);
			Query query = new Query(allTps, tp);
			query.loadFromUserQuery(searchText);
			DbTable dbt = DbTable.getDbTable(DbHelper.getDbHelper(mContext),
					tp.getTableId());
			List<String> columnOrder = tp.getColumnOrder();
			return new TableData(mContext, tp, dbt.getRaw(query,
					columnOrder.toArray(new String[columnOrder.size()])));
		}

		/**
		 * Return a list of the display names for all the tables in the database
		 * sorted in case insensitive order.
		 *
		 * @return
		 */
		public JSONArray getTableDisplayNames() {
			Log.d(TAG, "called getTableDisplayNames()");
			initTpInfo();
			List<String> allNames = Arrays.asList(tpMap.keySet().toArray(
					new String[0]));
			Collections.sort(allNames, String.CASE_INSENSITIVE_ORDER);
			JSONArray result = new JSONArray((Collection<String>) allNames);
			return result;
		}

		/**
		 * Launch the {@link CustomHomeScreenActivity} with the custom filename
		 * to display.
		 *
		 * @param filename
		 */
		public void launchHTML(String filename) {
			Log.d(TAG, "in launchHTML with filename: " + filename);
			Intent i = new Intent(mContext, CustomHomeScreenActivity.class);
			i.putExtra(CustomHomeScreenActivity.INTENT_KEY_FILENAME, filename);
			mContext.startActivity(i);
		}

		/**
		 * Create an alert that will allow for a new table name. This might be
		 * to rename an existing table, if isNewTable false, or it could be a
		 * new table, if isNewTable is true.
		 * <p>
		 * This method is based on {@link TableManager.alertForNewTableName}.
		 * The parameters are the same for the sake of consistency.
		 * <p>
		 * As this method does not access the javascript, the caller is
		 * responsible for refreshing the displayed information.
		 *
		 * @param isNewTable
		 * @param tableType
		 *            this is the string representation of TableType. It must
		 *            construct the correct value for {@link TableType.valueOf}.
		 * @param tp
		 * @param givenTableName
		 */
		public void alertForNewTableName(final boolean isNewTable,
				final String tableTypeStr, final TableProperties tp,
				String givenTableName) {
			Log.d(TAG, "alertForNewTableName called");
			Log.d(TAG, "isNewTable: " + Boolean.toString(isNewTable));
			Log.d(TAG, "finalTableTypeStr: " + tableTypeStr);
			Log.d(TAG, "tp: " + tp);
			Log.d(TAG, "givenTableName: " + givenTableName);
			final TableType tableType = TableType.valueOf(tableTypeStr);
			AlertDialog newTableAlert;
			AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
			alert.setTitle(mContext.getString(R.string.name_of_new_table));
			// An edit text for getting user input.
			final EditText input = new EditText(mContext);
			alert.setView(input);
			if (givenTableName != null) {
				input.setText(givenTableName);
			}
			// OK Action: create a new table.
			alert.setPositiveButton(mContext.getString(R.string.ok),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							String newTableName = input.getText().toString()
									.trim();
							if (newTableName == null || newTableName.equals("")) {
								Toast toast = Toast.makeText(
										mContext,
										mContext.getString(R.string.error_table_name_empty),
										Toast.LENGTH_LONG);
								toast.show();
							} else {
								if (isNewTable) {
									addTable(newTableName, tableType);
								} else {
									tp.setDisplayName(newTableName);
								}
							}
						}
					});

			alert.setNegativeButton(R.string.cancel,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// Cancel it, do nothing.
						}
					});
			newTableAlert = alert.create();
			newTableAlert.getWindow().setSoftInputMode(
					WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
			newTableAlert.show();
		}

		private void addTable(String tableName, TableType tableType) {
			String dbTableName = TableProperties.createDbTableName(dbh,
					tableName);
			TableProperties tp = TableProperties.addTable(dbh, dbTableName,
					dbTableName, tableName, tableType,
					KeyValueStore.Type.ACTIVE);
		}
	}

}