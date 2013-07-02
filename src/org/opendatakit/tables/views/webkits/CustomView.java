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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
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
import android.os.Build;
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

	private static final String TAG = CustomView.class.getSimpleName();

	protected static WebView webView;
	private static ViewGroup lastParent;

	private static ObjectMapper MAPPER = new ObjectMapper();
	private static TypeReference<HashMap<String, String>> MAP_REF = new TypeReference<HashMap<String, String>>() {
	};

	private static Set<String> javascriptInterfaces = new HashSet<String>();

	protected static void addJavascriptInterface(Object o, String name) {
		javascriptInterfaces.add(name);
		webView.addJavascriptInterface(o, name);
	}

	private static void clearInterfaces() {
		for (String str : javascriptInterfaces) {
			if (Build.VERSION.SDK_INT >= 11) {
				webView.removeJavascriptInterface(str);
			} else {
				webView.addJavascriptInterface(null, str);
			}
		}
	}

	private Activity mParentActivity;

	private Map<String, TableProperties> tpMap;
	private CustomViewCallbacks mCallbacks;

	protected CustomView(Activity parentActivity, CustomViewCallbacks callbacks) {
		super(parentActivity);
		initCommonWebView(parentActivity);
		this.mParentActivity = parentActivity;
		this.mCallbacks = callbacks;
	}

	public static void initCommonWebView(Context context) {
		if (webView != null) {
			clearInterfaces();
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

	private void initTpInfo() {
		if (tpMap != null) {
			return;
		}
		tpMap = new HashMap<String, TableProperties>();
		TableProperties[] allTps = TableProperties.getTablePropertiesForAll(
				DbHelper.getDbHelper(mParentActivity),
				KeyValueStore.Type.ACTIVE);
		for (TableProperties tp : allTps) {
			tpMap.put(tp.getDisplayName(), tp);
		}
	}

	private List<String> getTableDisplayNames() {
		initTpInfo();
		List<String> allNames = Arrays.asList(tpMap.keySet().toArray(
				new String[0]));
		Collections.sort(allNames, String.CASE_INSENSITIVE_ORDER);
		return allNames;
	}

	private TableProperties getTablePropertiesByDisplayName(TableProperties tp,
			String tableName) {
		if (tp == null || !tableName.equals(tp.getDisplayName())) {
			initTpInfo();
			tp = tpMap.get(tableName);
		}
		return tp;
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
	 * @param prepopulateValues
	 *            a map of elementKey to value for the rows with which you want
	 *            to prepopulate the add row.
	 */
	private void addRowWithCollect(String tableName, TableProperties tp,
			Map<String, String> prepopulateValues) {
		CollectFormParameters formParameters = CollectFormParameters
				.constructCollectFormParameters(tp);
		prepopulateRowAndLaunchCollect(formParameters, tp, prepopulateValues);
	}

	/**
	 * Add a row using Collect. This is the hook into the javascript. The
	 * activity holding this view must have implemented the onActivityReturn
	 * method appropriately to handle the result.
	 * <p>
	 * It allows you to specify a form other than that which may be the default
	 * for the table. It differs in {@link #addRow(String)} in that it lets you
	 * add the row using an arbitrary form.
	 *
	 * @param tableName
	 * @param formId
	 * @param formVersion
	 * @param formRootElement
	 * @param tp
	 * @param prepopulateValues
	 *            a map of elementKey to value for the rows with which you want
	 *            to prepopulate the add row.
	 */
	private void addRowWithCollectAndSpecificForm(String tableName,
			String formId, String formVersion, String formRootElement,
			TableProperties tp, Map<String, String> prepopulateValues) {
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
		prepopulateRowAndLaunchCollect(formParameters, tp, prepopulateValues);
	}

	/**
	 * This is called by the internal data classes. It prepopulates the form as
	 * it needs based on the query (or the elKeyToValueToPrepopulate parameter)
	 * and launches the form.
	 *
	 * @param params
	 * @param tp
	 * @param elKeyToValueToPrepopulate
	 *            a map of element key to value that will prepopulate the
	 *            Collect form for the new add row. Must be a map of column
	 *            element key to value. If this parameter is null, it
	 *            prepopulates based on the searchString, if there is one. If
	 *            this value is not null, it ignores the queryString and uses
	 *            only the map.
	 */
	private void prepopulateRowAndLaunchCollect(CollectFormParameters params,
			TableProperties tp, Map<String, String> elKeyToValueToPrepopulate) {
		Intent addRowIntent;
		if (elKeyToValueToPrepopulate == null) {
			// The prepopulated values we need to get from the query string.
			String currentQueryString = mCallbacks.getSearchString();

			addRowIntent = CollectUtil.getIntentForOdkCollectAddRowByQuery(
					CustomView.this.getContainerActivity(), tp, params,
					currentQueryString);
		} else {
			// We've received a map to prepopulate with.
			addRowIntent = CollectUtil.getIntentForOdkCollectAddRow(
					CustomView.this.getContainerActivity(), tp, params,
					elKeyToValueToPrepopulate);
		}
		// Now just launch the intent to add the row.
		CollectUtil.launchCollectToAddRow(getContainerActivity(), addRowIntent,
				tp);
	}

	/**
	 * Retrieve a map from a simple json map that has been stringified.
	 *
	 * @param jsonMap
	 * @return null if the mapping fails, else the map
	 */
	private Map<String, String> getMapFromJson(String jsonMap) {
		Map<String, String> map = null;
		try {
			map = MAPPER.readValue(jsonMap, MAP_REF);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}

	/**
	 * This class is invoked by the RowDataIf class, which is the class invoked
	 * through the JavaScript interface.
	 */
	protected class RowData {

		public final String TAG = RowData.class.getSimpleName();

		private final TableProperties tp;
		private Map<String, String> data;
		private String mRowId;
		private String mInstanceName;

		public RowDataIf getJavascriptInterfaceWithWeakReference() {
			return new RowDataIf(this);
		}

		RowData(TableProperties tp) {
			this.tp = tp;
		}

		void set(String rowId, String mInstanceName, Map<String, String> data) {
			this.data = data;
			this.mRowId = rowId;
			this.mInstanceName = mInstanceName;
		}

		/**
		 * Edit the row with collect. Uses the form specified in the table
		 * properties or else the ODKTables-generated default form.
		 */
		public void editRowWithCollect() {
			Intent editRowIntent = CollectUtil.getIntentForOdkCollectEditRow(
					CustomView.this.getContainerActivity(), tp, data, null,
					null, null, mRowId, mInstanceName);
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
					formVersion, formRootElement, mRowId, mInstanceName);
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
			// The first thing we need to do is get the correct TableProperties.
			TableProperties tpToReceiveAdd = getTablePropertiesByDisplayName(
					tp, tableName);
			if (tpToReceiveAdd == null) {
				Log.e(TAG, "table [" + tableName + "] cannot have a row added"
						+ " because it could not be found");
				return;
			}
			CustomView.this.addRowWithCollect(tableName, tpToReceiveAdd, null);
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
			// The first thing we need to do is get the correct TableProperties.
			TableProperties tpToReceiveAdd = getTablePropertiesByDisplayName(
					tp, tableName);
			if (tpToReceiveAdd == null) {
				Log.e(TAG, "table [" + tableName + "] cannot have a row added"
						+ " because it could not be found");
				return;
			}
			CustomView.this.addRowWithCollectAndSpecificForm(tableName, formId,
					formVersion, formRootElement, tpToReceiveAdd, null);
		}

		public void addRowWithCollectAndSpecificFormAndPrepopulatedValues(
				String tableName, String formId, String formVersion,
				String formRootElement, String jsonMap) {
			// The first thing we need to do is get the correct TableProperties.
			TableProperties tpToReceiveAdd = getTablePropertiesByDisplayName(
					tp, tableName);
			if (tpToReceiveAdd == null) {
				Log.e(TAG, "table [" + tableName + "] cannot have a row added"
						+ " because it could not be found");
				return;
			}
			Map<String, String> map = CustomView.this.getMapFromJson(jsonMap);
			if (map == null) {
				Log.e(TAG, "couldn't parse jsonString: " + jsonMap);
				return;
			}
			CustomView.this.addRowWithCollectAndSpecificForm(tableName, formId,
					formVersion, formRootElement, tpToReceiveAdd, map);
		}

		public void addRowWithCollectAndPrepopulatedValues(String tableName,
				String jsonMap) {
			// The first thing we need to do is get the correct TableProperties.
			TableProperties tpToReceiveAdd = getTablePropertiesByDisplayName(
					tp, tableName);
			if (tpToReceiveAdd == null) {
				Log.e(TAG, "table [" + tableName + "] cannot have a row added"
						+ " because it could not be found");
				return;
			}
			Map<String, String> map = CustomView.this.getMapFromJson(jsonMap);
			if (map == null) {
				Log.e(TAG, "couldn't parse jsonString: " + jsonMap);
				return;
			}
			CustomView.this.addRowWithCollect(tableName, tpToReceiveAdd, map);
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

		public TableDataIf getJavascriptInterfaceWithWeakReference() {
			return new TableDataIf(this);
		}

		private final UserTable mTable; // contains TableProperties
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
		protected Activity mActivity;

		public TableData(Activity activity, UserTable table) {
			Log.d(TAG, "calling TableData constructor with Table");
			this.mActivity = activity;
			this.mTable = table;
			initMaps();
		}

		boolean inCollectionMode() {
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

		public TableData(UserTable table) {
			Log.d(TAG, "calling TableData constructor with UserTable");
			this.mTable = table;
			initMaps();

			// The collectionMap will be initialized if the table is indexed.
			if (isIndexed()) {
				initCollectionMap();
			}
		}

		// Initializes the colMap and primeColumns that provide methods quick
		// access to the current table's state.
		private void initMaps() {
			TableProperties tp = mTable.getTableProperties();
			mColumnDisplayNameToColorRuleGroup = new HashMap<String, ColorRuleGroup>();
			primeColumns = tp.getPrimeColumns();
			Map<String, ColumnProperties> elementKeyToColumnProperties = tp
					.getColumns();
			colMap = new HashMap<String, Integer>();
			for (ColumnProperties cp : elementKeyToColumnProperties.values()) {
				String smsLabel = cp.getSmsLabel();
				Integer idx = mTable.getColumnIndexOfElementKey(cp
						.getElementKey());
				if (idx != null) {
					colMap.put(cp.getDisplayName(), idx);
					if (smsLabel != null) {
						// TODO: this doesn't look to ever be used, and ignores
						// the
						// possibility of conflicting element keys and sms
						// labels.
						colMap.put(smsLabel, idx);
					}
				}
			}
		}

		// Returns the number of rows in the table being viewed.
		int getCount() {
			return this.mTable.getHeight();
		}

		/*
		 * @param: colName, column name in the userTable/rawTable
		 *
		 * @return: returns a String in JSONArray format containing all the row
		 * data for the given column name format: [row1, row2, row3, row4]
		 */
		String getColumnData(String colName) {
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

		String getColumns() {
			TableProperties tp = mTable.getTableProperties();
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
		String getForegroundColor(String colName, String value) {
			TableProperties tp = mTable.getTableProperties();
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
			String[] elementKeyForIndex = new String[] { elementKey };
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
					elementKeyForIndex, indexOfDataMap, metadata,
					indexOfMetadataMap, null);
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
		private void initCollectionMap() {
			Control c = new Control(mActivity);
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
			String tableName = mTable.getTableProperties().getDisplayName();
			for (int i = 0; i < getCount(); i++) {
				String searchText = colName + ":" + getData(i, colName);
				TableData data = c.query(tableName, searchText);
				collectionMap.put(i, data.getCount());
			}
		}

		// Returns the number of rows in the collection at the given row index.
		int getCollectionSize(int rowNum) {
			return collectionMap.get(rowNum);
		}

		// Returns whether the table is indexed.
		boolean isIndexed() {
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
		String getData(int rowNum, String colName) {
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
		void editRowWithCollect(int rowNumber) {
			TableProperties tp = mTable.getTableProperties();
			String rowId = this.mTable.getRowId(rowNumber);
			Map<String, String> elementKeyToValue = getElementKeyToValueMapForRow(rowNumber);
			Intent editRowIntent = CollectUtil.getIntentForOdkCollectEditRow(
					CustomView.this.getContainerActivity(), tp,
					elementKeyToValue, null, null, null, rowId,
					mTable.getInstanceName(rowNumber));
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
		void editRowWithCollectAndSpecificForm(int rowNumber, String formId,
				String formVersion, String formRootElement) {
			TableProperties tp = mTable.getTableProperties();
			String rowId = this.mTable.getRowId(rowNumber);
			Map<String, String> elementKeyToValue = getElementKeyToValueMapForRow(rowNumber);
			Intent editRowIntent = CollectUtil.getIntentForOdkCollectEditRow(
					CustomView.this.getContainerActivity(), tp,
					elementKeyToValue, formId, formVersion, formRootElement,
					rowId, mTable.getInstanceName(rowNumber));
			CollectUtil.launchCollectToEditRow(
					CustomView.this.getContainerActivity(), editRowIntent,
					rowId);
		}

		/**
		 * Add a row using collect and the default form.
		 *
		 * @param tableName
		 */
		void addRowWithCollect(String tableName) {
			// The first thing we need to do is get the correct TableProperties.
			TableProperties tp = mTable.getTableProperties();
			TableProperties tpToReceiveAdd = getTablePropertiesByDisplayName(
					tp, tableName);
			if (tpToReceiveAdd == null) {
				Log.e(TAG, "table [" + tableName + "] cannot have a row added"
						+ " because it could not be found");
				return;
			}
			CustomView.this.addRowWithCollect(tableName, tpToReceiveAdd, null);
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
		void addRowWithCollectAndSpecificForm(String tableName, String formId,
				String formVersion, String formRootElement) {
			// The first thing we need to do is get the correct TableProperties.
			TableProperties tp = mTable.getTableProperties();
			TableProperties tpToReceiveAdd = getTablePropertiesByDisplayName(
					tp, tableName);
			if (tpToReceiveAdd == null) {
				Log.e(TAG, "table [" + tableName + "] cannot have a row added"
						+ " because it could not be found");
				return;
			}
			CustomView.this.addRowWithCollectAndSpecificForm(tableName, formId,
					formVersion, formRootElement, tpToReceiveAdd, null);
		}

		void addRowWithCollectAndSpecificFormAndPrepopulatedValues(
				String tableName, String formId, String formVersion,
				String formRootElement, String jsonMap) {
			TableProperties tp = mTable.getTableProperties();
			// The first thing we need to do is get the correct TableProperties.
			TableProperties tpToReceiveAdd = getTablePropertiesByDisplayName(
					tp, tableName);
			if (tpToReceiveAdd == null) {
				Log.e(TAG, "table [" + tableName + "] cannot have a row added"
						+ " because it could not be found");
				return;
			}
			Map<String, String> map = CustomView.this.getMapFromJson(jsonMap);
			if (map == null) {
				Log.e(TAG, "couldn't parse jsonString: " + jsonMap);
				return;
			}
			CustomView.this.addRowWithCollectAndSpecificForm(tableName, formId,
					formVersion, formRootElement, tpToReceiveAdd, map);
		}

		void addRowWithCollectAndPrepopulatedValues(String tableName,
				String jsonMap) {
			TableProperties tp = mTable.getTableProperties();
			// The first thing we need to do is get the correct TableProperties.
			TableProperties tpToReceiveAdd = getTablePropertiesByDisplayName(
					tp, tableName);
			if (tpToReceiveAdd == null) {
				Log.e(TAG, "table [" + tableName + "] cannot have a row added"
						+ " because it could not be found");
				return;
			}
			Map<String, String> map = CustomView.this.getMapFromJson(jsonMap);
			if (map == null) {
				Log.e(TAG, "couldn't parse jsonString: " + jsonMap);
				return;
			}
			CustomView.this.addRowWithCollect(tableName, tpToReceiveAdd, map);
		}

		/**
		 * Get a map of elementKey to value for the given row number.
		 *
		 * @param rowNum
		 * @return
		 */
		private Map<String, String> getElementKeyToValueMapForRow(int rowNum) {
			TableProperties tp = mTable.getTableProperties();
			Map<String, String> elementKeyToValue = new HashMap<String, String>();
			for (Entry<String, Integer> entry : colMap.entrySet()) {
				ColumnProperties cp = tp.getColumnByDisplayName(entry.getKey());
				String elementKey = cp.getElementKey();
				String value = this.mTable.getData(rowNum, entry.getValue());
				elementKeyToValue.put(elementKey, value);
			}
			return elementKeyToValue;
		}

		public String getTableDisplayName() {
			return mTable.getTableProperties().getDisplayName();
		}

	}

	/**
	 * This class is exposed via the javascript interface returned by
	 * getJavascriptInteface() to the WebKit code.
	 *
	 */
	protected class Control {

		public Object getJavascriptInterfaceWithWeakReference() {
			return new ControlIf(this);
		}

		private static final String TAG = "CustomView.Control";

		protected final Activity mActivity;
		private final DbHelper dbh;
		private final UserTable mTable;

		// hold onto references to all the results returned to the WebKit
		private LinkedList<TableData> queryResults = new LinkedList<TableData>();

		/**
		 * This construct requires an activity rather than a context because we
		 * want to be able to launch intents for result rather than merely
		 * launch them on their own.
		 *
		 * @param activity
		 *            the activity that will be holding the view
		 */
		public Control(Activity activity) {
			this(activity, null);
		}

		public Control(Activity activity, UserTable table) {
			this.mActivity = activity;
			this.mTable = table;
			dbh = DbHelper.getDbHelper(mActivity);
			Log.d(TAG, "calling Control Constructor");
		}

		/**
		 * This only makes sense when invoked on a list view....
		 *
		 * @param index
		 * @return
		 */
		public boolean openItem(int index) {
			if (mTable == null) {
				return false;
			}
			Controller.launchDetailActivity(mActivity, mTable, index, null);
			return true;
		}

		/**
		 * Open the item specified by the index to the detail view specified by
		 * the given filename. The filename is relative to the odk tables
		 * directory.
		 *
		 * @param index
		 * @param filename
		 * @return
		 */
		public boolean openDetailViewWithFile(int index, String filename) {
			if (mTable == null) {
				return false;
			}
			String pathToTablesFolder = ODKFileUtils
					.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME);
			String pathToFile = pathToTablesFolder + File.separator + filename;
			Controller.launchDetailActivity(mActivity, mTable, index,
					pathToFile);
			return true;
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
			return helperOpenTable(tableName, query, null, null);
		}

		public boolean openTableWithSqlQuery(String tableName,
				String sqlWhereClause, String[] sqlSelectionArgs) {
			return helperOpenTable(tableName, null, sqlWhereClause,
					sqlSelectionArgs);
		}

		private boolean helperOpenTable(String tableName, String searchText,
				String sqlWhereClause, String[] sqlSelectionArgs) {
			TableProperties tpToOpen = getTablePropertiesByDisplayName(null,
					tableName);
			if (tpToOpen == null) {
				Log.e(TAG, "tableName [" + tableName + "] not in map");
				return false;
			}
			Controller.launchTableActivity(mActivity, tpToOpen, searchText,
					false, sqlWhereClause, sqlSelectionArgs);
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
			return helperOpenTableWithFile(tableName, searchText, filename,
					null, null);
		}

		public boolean openTableToListViewWithFileAndSqlQuery(String tableName,
				String filename, String sqlWhereClause,
				String[] sqlSelectionArgs) {
			return helperOpenTableWithFile(tableName, null, filename,
					sqlWhereClause, sqlSelectionArgs);
		}

		private boolean helperOpenTableWithFile(String tableName,
				String searchText, String filename, String sqlWhereClause,
				String[] sqlSelectionArgs) {
			TableProperties tp = getTablePropertiesByDisplayName(null,
					tableName);
			if (tp == null) {
				Log.e(TAG, "tableName [" + tableName + "] not in map");
				return false;
			}
			String pathToTablesFolder = ODKFileUtils
					.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME);
			String pathToFile = pathToTablesFolder + File.separator + filename;
			Controller.launchListViewWithFilenameAndSqlQuery(mActivity, tp,
					searchText, null, false, pathToFile, sqlWhereClause,
					sqlSelectionArgs);
			return true;
		}

		public boolean openTableToMapViewWithSqlQuery(String tableName,
				String sqlWhereClause, String[] sqlSelectionArgs) {
			return helperOpenTableToMapView(tableName, null, sqlWhereClause,
					sqlSelectionArgs);
		}

		public boolean openTableToMapView(String tableName, String searchText) {
			return helperOpenTableToMapView(tableName, searchText, null, null);
		}

		private boolean helperOpenTableToMapView(String tableName,
				String searchText, String sqlWhereClause,
				String[] sqlSelectionArgs) {
			TableProperties tp = getTablePropertiesByDisplayName(null,
					tableName);
			if (tp == null) {
				Log.e(TAG, "tableName [" + tableName + "] not in map");
				return false;
			}
			Controller.launchMapView(mActivity, tp, searchText, null, false,
					sqlWhereClause, sqlSelectionArgs);
			return true;
		}

		public boolean openTableToSpreadsheetView(String tableName,
				String searchText) {
			return helperOpenTableToSpreadsheetView(tableName, searchText,
					null, null);
		}

		public boolean openTableToSpreadsheetViewWithSqlQuery(String tableName,
				String sqlWhereClause, String[] sqlSelectionArgs) {
			return helperOpenTableToSpreadsheetView(tableName, null,
					sqlWhereClause, sqlSelectionArgs);
		}

		private boolean helperOpenTableToSpreadsheetView(String tableName,
				String searchText, String sqlWhereClause,
				String[] sqlSelectionArgs) {
			initTpInfo();
			TableProperties tp = tpMap.get(tableName);
			if (tp == null) {
				Log.e(TAG, "tableName [" + tableName + "] not in map");
				return false;
			}
			Controller.launchSpreadsheetView(mActivity, tp, searchText, null,
					false, sqlWhereClause, sqlSelectionArgs);
			return true;
		}

		public void releaseQueryResources(String tableName) {
			Iterator<TableData> iter = queryResults.iterator();
			while (iter.hasNext()) {
				TableData td = iter.next();
				if (td.getTableDisplayName().equals(tableName)) {
					iter.remove();
				}
			}
		}

		public TableData query(String tableName, String searchText) {
			TableProperties tp = getTablePropertiesByDisplayName(null,
					tableName);
			if (tp == null) {
				return null;
			}
			Query query = new Query(dbh, KeyValueStore.Type.ACTIVE, tp);
			query.loadFromUserQuery(searchText);
			DbTable dbt = DbTable.getDbTable(dbh, tp);
			List<String> columnOrder = tp.getColumnOrder();

			TableData td = new TableData(mActivity, dbt.getRaw(query,
					columnOrder.toArray(new String[columnOrder.size()])));
			/**
			 * IMPORTANT: remember the td. The interfaces will hold weak
			 * references to them, so we need a strong reference to prevent GC.
			 */
			queryResults.add(td);
			return td;
		}

		/**
		 * Query the database using sql. Only returns the columns for the table
		 * specified by the tableName parameter.
		 * <p>
		 * Any arguments in the WHERE statement must be replaced by "?" and
		 * contained in order in the selectionArgs array.
		 * <p>
		 * For example, if you wanted all the rows where the column foo equaled
		 * bar, the where clause would be "WHERE foo = ? " and the selection
		 * args would be ["bar"].
		 *
		 * @param tableName
		 *            the display name of the table for which you want the
		 *            columns to be returned.
		 * @param whereClause
		 *            the where clause for the selection. Must begin with
		 *            "WHERE", as if it was appended immediately after "SELECT *
		 *            FROM tableName ". References to other tables, e.g. for
		 *            joins, in this statement must use the name of the table as
		 *            returned by {@link getDbNameForTable}.
		 * @param selectionArgs
		 * @return
		 */
		public TableData queryWithSql(String tableName, String whereClause,
				String[] selectionArgs) {
			// We're going to handle this by passing it off to the DbTable
			// rawSqlQuery(String whereClause, String[] selectionArgs) argument.
			TableProperties tp = getTablePropertiesByDisplayName(null,
					tableName);
			if (tp == null) {
				Log.e(TAG, "request for table with displayName [" + tableName
						+ "] cannot be found.");
				return null;
			}
			DbTable dbTable = DbTable.getDbTable(dbh, tp);
			UserTable userTable = dbTable.rawSqlQuery(whereClause,
					selectionArgs);
			TableData td = new TableData(mActivity, userTable);
			/**
			 * IMPORTANT: remember the td. The interfaces will hold weak
			 * references to them, so we need a strong reference to prevent GC.
			 */
			queryResults.add(td);
			return td;
		}

		/**
		 * Return the database name of the table. Important for use in
		 * {@link queryWithSql}. Returns null if the table could not be found.
		 *
		 * @param displayName
		 * @return the database name of the table, or null if it could not be
		 *         found.
		 */
		public String getDbNameForTable(String displayName) {
			TableProperties tp = getTablePropertiesByDisplayName(null,
					displayName);
			if (tp == null) {
				Log.e(TAG, "request for table with displayName [" + displayName
						+ "] cannot be found.");
				return null;
			}
			return tp.getDbTableName();
		}

		/**
		 * Get the element key of the column with the given display name from
		 * the given table. Both the table and the column are retrieved by their
		 * display names. If the underlying table or column cannot be found by
		 * the given display names, returns null.
		 *
		 * @param tableDisplayName
		 * @param columnDisplayName
		 * @return
		 */
		public String getElementKeyForColumn(String tableDisplayName,
				String columnDisplayName) {
			TableProperties tp = getTablePropertiesByDisplayName(null,
					tableDisplayName);
			if (tp == null) {
				Log.e(TAG, "request for table with displayName ["
						+ tableDisplayName + "] cannot be found.");
				return null;
			}
			ColumnProperties columnProperties = tp
					.getColumnByDisplayName(columnDisplayName);
			if (columnProperties == null) {
				return null;
			}
			return columnProperties.getElementKey();
		}

		/**
		 * Return a list of the display names for all the tables in the database
		 * sorted in case insensitive order.
		 *
		 * @return
		 */
		public JSONArray getTableDisplayNames() {
			Log.d(TAG, "called getTableDisplayNames()");
			List<String> allNames = CustomView.this.getTableDisplayNames();
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
         String pathToTablesFolder = ODKFileUtils
             .getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME);
         String pathToFile = pathToTablesFolder + File.separator + filename;
			Intent i = new Intent(mActivity, CustomHomeScreenActivity.class);
			i.putExtra(CustomHomeScreenActivity.INTENT_KEY_FILENAME, pathToFile);
			mActivity.startActivity(i);
		}

		/**
		 * Add a row using collect and the default form.
		 *
		 * @param tableName
		 */
		public void addRowWithCollect(String tableName) {
			// The first thing we need to do is get the correct TableProperties.
			TableProperties tp = mTable.getTableProperties();
			TableProperties tpToReceiveAdd = getTablePropertiesByDisplayName(
					null, tableName);
			if (tpToReceiveAdd == null) {
				Log.e(TAG, "table [" + tableName + "] cannot have a row added"
						+ " because it could not be found");
				return;
			}
			CustomView.this.addRowWithCollect(tableName, tpToReceiveAdd, null);
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
			// The first thing we need to do is get the correct TableProperties.
			TableProperties tpToReceiveAdd = getTablePropertiesByDisplayName(
					null, tableName);
			if (tpToReceiveAdd == null) {
				Log.e(TAG, "table [" + tableName + "] cannot have a row added"
						+ " because it could not be found");
				return;
			}
			CustomView.this.addRowWithCollectAndSpecificForm(tableName, formId,
					formVersion, formRootElement, tpToReceiveAdd, null);
		}

		public void addRowWithCollectAndSpecificFormAndPrepopulatedValues(
				String tableName, String formId, String formVersion,
				String formRootElement, String jsonMap) {
			// The first thing we need to do is get the correct TableProperties.
			TableProperties tpToReceiveAdd = getTablePropertiesByDisplayName(
					null, tableName);
			if (tpToReceiveAdd == null) {
				Log.e(TAG, "table [" + tableName + "] cannot have a row added"
						+ " because it could not be found");
				return;
			}
			Map<String, String> map = CustomView.this.getMapFromJson(jsonMap);
			if (map == null) {
				Log.e(TAG, "couldn't parse jsonString: " + jsonMap);
				return;
			}
			CustomView.this.addRowWithCollectAndSpecificForm(tableName, formId,
					formVersion, formRootElement, tpToReceiveAdd, map);
		}

		public void addRowWithCollectAndPrepopulatedValues(String tableName,
				String jsonMap) {
			// The first thing we need to do is get the correct TableProperties.
			TableProperties tp = mTable.getTableProperties();
			TableProperties tpToReceiveAdd = getTablePropertiesByDisplayName(
					null, tableName);
			if (tpToReceiveAdd == null) {
				Log.e(TAG, "table [" + tableName + "] cannot have a row added"
						+ " because it could not be found");
				return;
			}
			Map<String, String> map = CustomView.this.getMapFromJson(jsonMap);
			if (map == null) {
				Log.e(TAG, "couldn't parse jsonString: " + jsonMap);
				return;
			}
			CustomView.this.addRowWithCollect(tableName, tpToReceiveAdd, map);
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
			AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
			alert.setTitle(mActivity.getString(R.string.name_of_new_table));
			// An edit text for getting user input.
			final EditText input = new EditText(mActivity);
			alert.setView(input);
			if (givenTableName != null) {
				input.setText(givenTableName);
			}
			// OK Action: create a new table.
			alert.setPositiveButton(mActivity.getString(R.string.ok),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							String newTableName = input.getText().toString()
									.trim();
							if (newTableName == null || newTableName.equals("")) {
								Toast toast = Toast.makeText(
										mActivity,
										mActivity
												.getString(R.string.error_table_name_empty),
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

	public interface CustomViewCallbacks {
		/**
		 * Get the string currently in the searchbox.
		 *
		 * @return
		 */
		public String getSearchString();
	}

}