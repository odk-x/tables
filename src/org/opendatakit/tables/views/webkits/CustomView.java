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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.common.android.provider.FileProvider;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.activities.CustomHomeScreenActivity;
import org.opendatakit.tables.activities.TableManager;
import org.opendatakit.tables.data.ColorRuleGroup;
import org.opendatakit.tables.data.ColorRuleGroup.ColorGuide;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.ColumnType;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableType;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.data.UserTable.Row;
import org.opendatakit.tables.utils.CollectUtil;
import org.opendatakit.tables.utils.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.utils.NameUtil;
import org.opendatakit.tables.utils.SurveyUtil;
import org.opendatakit.tables.utils.SurveyUtil.SurveyFormParameters;
import org.opendatakit.tables.utils.TableFileUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
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

import com.google.gson.Gson;

public abstract class CustomView extends LinearLayout {

	private static final String TAG = CustomView.class.getSimpleName();
	
	// The keys for the platformInfo json object.
	private static final String PLATFORM_INFO_KEY_CONTAINER = "container";
	private static final String PLATFORM_INFO_KEY_VERSION = "version";
	private static final String PLATFORM_INFO_KEY_APP_NAME = "appName";
	private static final String PLATFORM_INFO_KEY_BASE_URI = "baseUri";
	private static final String PLATFORM_INFO_KEY_LOG_LEVEL = "logLevel";

	protected static WebView webView;
	private static ViewGroup lastParent;

	private static ObjectMapper MAPPER = new ObjectMapper();
	private static TypeReference<HashMap<String, String>> MAP_REF = 
	    new TypeReference<HashMap<String, String>>() {};

	private static Set<String> javascriptInterfaces = new HashSet<String>();

	protected static void addJavascriptInterface(Object o, String name) {
		javascriptInterfaces.add(name);
		webView.addJavascriptInterface(o, name);
	}

	@SuppressLint("NewApi")
  private static void clearInterfaces() {
		for (String str : javascriptInterfaces) {
			webView.addJavascriptInterface(null, str);
			if (Build.VERSION.SDK_INT >= 11) {
				webView.removeJavascriptInterface(str);
			}
		}
	}

	protected final Activity mParentActivity;
	protected final String mAppName;

	private Map<String, TableProperties> tableIdToProperties;
	private CustomViewCallbacks mCallbacks;

	public CustomView(Activity parentActivity, String appName, 
	    CustomViewCallbacks callbacks) {
		super(parentActivity);
		initCommonWebView(parentActivity);
		this.mParentActivity = parentActivity;
		this.mAppName = appName;
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
		if (tableIdToProperties != null) {
			return;
		}
		tableIdToProperties = new HashMap<String, TableProperties>();
		TableProperties[] allTps = TableProperties.getTablePropertiesForAll(
				DbHelper.getDbHelper(mParentActivity, mAppName),
				KeyValueStore.Type.ACTIVE);
		for (TableProperties tp : allTps) {
			tableIdToProperties.put(tp.getTableId(), tp);
		}
	}
	
	private TableProperties getTablePropertiesById(String tableId) {
	  initTpInfo();
	  TableProperties tp = tableIdToProperties.get(tableId);
	  if (tp == null) {
	    Log.d(TAG, "table properties returning null for table id: " + tableId);
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
					CustomView.this.getContainerActivity(),
					mAppName, tp, params,
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
	 * Actually acquire the Intents and launch the forms.
	 * @param tp
	 * @param surveyFormParameters
	 * @param elementKeyToValueToPrepopulate
	 */
	private void prepopulateRowAndLaunchSurveyToAddRow(TableProperties tp,
	    SurveyFormParameters surveyFormParameters,
	    Map<String, String> elementKeyToValueToPrepopulate) {
	  Intent addRowIntent = SurveyUtil.getIntentForOdkSurveyAddRow(
	      getContainerActivity(), tp, mAppName,
	      surveyFormParameters, elementKeyToValueToPrepopulate);
	  SurveyUtil.launchSurveyToAddRow(getContainerActivity(), addRowIntent, tp);
	}

	/**
	 * Should eventually handle similar things to the analagous Collect method.
	 * For now just is a wrapper.
	 * @param tableName
	 * @param tp
	 * @param formId
	 * @param screenPath
	 * @param formPath
	 * @param refId
	 * @param prepopulateValues
	 */
   private void addRowWithSurveyAndSpecificForm(String tableName,
       TableProperties tp, String formId, String screenPath,
       Map<String, String> prepopulateValues) {
     SurveyFormParameters surveyFormParameters = new
         SurveyFormParameters(true, formId, screenPath);
     prepopulateRowAndLaunchSurveyToAddRow(tp, surveyFormParameters,
         prepopulateValues);
   }

   /**
    * Construct the Intent and launch survey to edit the given row.
    * @param tableName
    * @param tp
    * @param instanceId
    * @param formId
    * @param screenPath
    * @param formPath
    * @param refId
    */
   private void editRowWithSurveyAndSpecificForm(TableProperties tp,
       String instanceId, String formId, String screenPath) {
     SurveyFormParameters surveyFormParameters =
         new SurveyFormParameters(true, formId, screenPath);
     Intent surveyEditRowIntent = SurveyUtil.getIntentForOdkSurveyEditRow(
         getContainerActivity(), tp, mAppName,
         surveyFormParameters, instanceId);
     SurveyUtil.launchSurveyToEditRow(getContainerActivity(),
         surveyEditRowIntent, tp, instanceId);
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
	 * Returns all the metadata and user-defined data element keys to value in
	 * a map.
	 * @param tableId
	 * @param rowId
	 * @return
	 */
	private Map<String, String> getElementKeyToValues(String tableId, 
	    String rowId) {
	  TableProperties tableProperties = getTablePropertiesById(tableId);
	  String sqlQuery = "WHERE " + DataTableColumns.ID + " = ? ";
	  String[] selectionArgs = {rowId};
	  DbHelper dbHelper = DbHelper.getDbHelper(getContext(), 
	      TableFileUtils.ODK_TABLES_APP_NAME);
	  DbTable dbTable = DbTable.getDbTable(dbHelper, tableProperties);
	  UserTable userTable = dbTable.rawSqlQuery(sqlQuery, selectionArgs);
	  if (userTable.getHeight() > 1) {
	    Log.e(TAG, "query returned > 1 rows for tableId: " + tableId + " and " +
	    		"rowId: " + rowId);
	  } else if (userTable.getHeight() == 0) {
	    Log.e(TAG, "query returned no rows for tableId: " + tableId +
	        " and rowId: " + rowId);
	  }
	  Map<String, String> elementKeyToValue = new HashMap<String, String>();
	  Row requestedRow = userTable.getRowAtIndex(0);
	  List<String> userDefinedElementKeys = 
	      userTable.getTableProperties().getColumnOrder();
	  Set<String> metadataElementKeys = 
	      userTable.getMapOfUserDataToIndex().keySet();
	  List<String> allElementKeys = new ArrayList<String>();
	  allElementKeys.addAll(userDefinedElementKeys);
	  allElementKeys.addAll(metadataElementKeys);
	  for (String elementKey : allElementKeys) {
	    elementKeyToValue.put(elementKey, 
	        requestedRow.getDataOrMetadataByElementKey(elementKey));
	  }
	  return elementKeyToValue;
	}

	/**
	 * "Unused" warnings are suppressed because the public methods of this class
	 * are meant to be called through the JavaScript interface.
	 */
	public class TableData {

		private static final String TAG = "TableData";

		public TableDataIf getJavascriptInterfaceWithWeakReference() {
			return new TableDataIf(this);
		}

		private final UserTable mTable; // contains TableProperties
		/**
		 * Maps the column element keys and sms labels to their index in the
		 * table. Thus the values in the map are not unique.
		 */
		private Map<String, Integer> columnIdentifierToIndex;
		/**
		 * Maps each collection with the number of rows under it.
		 */
		private Map<Integer, Integer> collectionMap;
		/**
		 * Holds the element keys of the indexed columns.
		 */
		private List<String> primeColumns;
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
			collectionMap = new HashMap<Integer, Integer>();
			mColumnDisplayNameToColorRuleGroup = 
			    new HashMap<String, ColorRuleGroup>();
			primeColumns = tp.getPrimeColumns();
			Map<String, ColumnProperties> elementKeyToColumnProperties = tp
					.getDatabaseColumns();
			columnIdentifierToIndex = new HashMap<String, Integer>();
			for (ColumnProperties cp : elementKeyToColumnProperties.values()) {
				String smsLabel = cp.getSmsLabel();
				Integer idx = mTable.getColumnIndexOfElementKey(cp
						.getElementKey());
				if (idx != null) {
					columnIdentifierToIndex.put(cp.getDisplayName(), idx);
					if (smsLabel != null) {
						// TODO: this doesn't look to ever be used, and ignores
						// the
						// possibility of conflicting element keys and sms
						// labels.
						columnIdentifierToIndex.put(smsLabel, idx);
					}
				}
			}
		}

		// Returns the number of rows in the table being viewed.
		public int getCount() {
			return this.mTable.getHeight();
		}

		/**
		 * @see {@link TableDataIf#getColumnData(String)}
		 */
		public String getColumnData(String elementPath) {
		  // Return all the rows.
		  return getColumnData(elementPath, getCount());
		}
		
		/**
		 * Return a strinfigied JSON array of the data in the columns. Returns
		 * null and logs an error if the column is not found. 
		 * @param elementPath
		 * @param requestedRows
		 * @return returns a String in JSONArray format containing all the row
       * data for the given column name format: [row1, row2, row3, row4]
		 */
		public String getColumnData(String elementPath, int requestedRows) {
		  String elementKey = 
		      this.mTable.getTableProperties().getElementKeyFromElementPath(
		          elementPath);
        ArrayList<String> rowValues = new ArrayList<String>();
        if (!columnIdentifierToIndex.containsKey(elementKey)) {
          Log.e(TAG, "column not found with element path: " + elementPath +
              " and key: " + elementKey);
          return null;
        }
        int columnIndex = columnIdentifierToIndex.get(elementKey);
        for (int i = 0; i < requestedRows; i++) {
           rowValues.add(this.mTable.getData(i, columnIndex));
        }
        return new JSONArray(rowValues).toString();
		}

		/**
		 * Return a map of element key to the {@link ColumnType#label()}.
		 */
		public String getColumns() {
			Map<String, String> colInfo = new HashMap<String, String>();
			for (String column : columnIdentifierToIndex.keySet()) {
			   String label = getColumnTypeLabelForElementKey(column);
				colInfo.put(column, label);
			}
			return new JSONObject(colInfo).toString();
		}
		
		/**
		 * Get the element {@link ColumnType#label()} for the column with the
		 * given elementKey.
		 * @param elementKey
		 * @return
		 */
		private String getColumnTypeLabelForElementKey(String elementKey) {
        TableProperties tp = mTable.getTableProperties();
        ColumnProperties cp = tp.getColumnByElementKey(elementKey);
        String label = cp.getColumnType().label();
        return label;
		}

		/**
		 * @see {@link TableDataIf#getForegroundColor(String, String)}
		 */
		public String getForegroundColor(String elementPath, String value) {
			TableProperties tp = mTable.getTableProperties();
			String elementKey = 
			    tp.getElementKeyFromElementPath(elementPath);
			ColorRuleGroup colRul = this.mColumnDisplayNameToColorRuleGroup
					.get(elementPath);
			if (colRul == null) {
				// If it's not already there, cache it for future use.
				colRul = ColorRuleGroup.getColumnColorRuleGroup(tp, elementKey);
				this.mColumnDisplayNameToColorRuleGroup.put(elementPath, colRul);
			}
			// Rather than hand off the whole row data, we'll just dummy up the
			// info requested, as this will be easier for the html programmer
			// to use than to have to give in the whole row.
			Map<String, Integer> indexOfDataMap = new HashMap<String, Integer>();
			indexOfDataMap.put(elementKey, 0);
			String[] elementKeyForIndex = new String[] { elementKey };
			Map<String, Integer> indexOfMetadataMap = 
			    new HashMap<String, Integer>();
			indexOfMetadataMap.put(elementKey, 0);
			// We need to construct a dummy UserTable for the ColorRule to
			// interpret.
			String[] header = new String[] { elementPath };
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
			for (String col : columnIdentifierToIndex.keySet()) {
				if (col.equalsIgnoreCase(colName)) {
					colName = col;
				}
			}
			// Queries the original table for the rows in every collection and
			// stores the number of resulting rows for each.
			String tableName = mTable.getTableProperties().getDisplayName();
			for (int i = 0; i < getCount(); i++) {
				String searchText = colName + ":" + getData(i, colName);
				
				// TODO: fix this now that we're not allowing search text.
//				TableData data = c.query(tableName, searchText);
//				collectionMap.put(i, data.getCount());
			}
		}

		/** 
		 * Returns the number of rows in the collection at the given row index.
		 * Returns -1 if it is not in collection mode.
		 * @param rowNum
		 * @return
		 */
		public int getCollectionSize(int rowNum) {
		   if (!this.inCollectionMode()) {
		     return -1;
		   } else {
		     return collectionMap.get(rowNum);  
		   }
		}

		// Returns whether the table is indexed.
		public boolean isIndexed() {
			return (!primeColumns.isEmpty());
		}

		/**
		 * @see {@link TableDataIf#getData(int, String)}.
		 */
		public String getData(int rowNum, String elementPath) {
		  String elementKey =
		      mTable.getTableProperties().getElementKeyFromElementPath(
		          elementPath);
			if (columnIdentifierToIndex.containsKey(elementKey)) {
				String result = mTable.getData(rowNum, columnIdentifierToIndex.get(elementKey));
				return result;
			} else {
				return null;
			}
		}
		
		public String getTableId() {
		  return mTable.getTableProperties().getTableId();
		}
		
		
//	   /**
//	    * This should write a version of this object as json that we can then 
//	    * use in debugging on the javascript and server side.
//	    */
//	   public void writeDataObjectAsJSON() {
//	     /* 
//	      * The object here is expected to be the following:
//	      * {
//	      * JSON_KEY_IN_COLLECTION_MODE: boolean,
//	      * JSON_KEY_COUNT: int,
//	      * JSON_KEY_COLLECTION_SIZE: Array, // array of ints
//	      * JSON_KEY_IS_INDEXED: boolean,
//	      * JSON_KEY_DATA: Array,
//	      * JSON_KEY_COLUMN_DATA: {elementKey: Array, ...},
//	      * JSON_KEY_COLUMNS: {elementKey: string, ...}
//	      * }
//	      */
//	     // We'll essentially just be caching some of the values.
//	     // First figure out how many rows we're going to be writing--the lesser
//	     // of the max and the count.
//	     int numRowsToWrite = Math.min(NUM_ROWS_IN_DEBUG_OBJECT, getCount());
//	     // First let's get the string values. All should be for js.
//	     boolean inCollectionMode = 
//	         this.inCollectionMode() ? true : false;
//	     int count = this.getCount();
//	     boolean isIndexed = this.isIndexed();
//	     int[] collectionSize = new int[numRowsToWrite];
//	     for (int i = 0; i < numRowsToWrite; i++) {
//	       collectionSize[i] = getCollectionSize(i);
//	     }
//	     Map<String, String> columns = new HashMap<String, String>();
//	     Map<String, List<String>> allColumnData = 
//	         new HashMap<String, List<String>>();
//	     // Here we're using this object b/c these appear to be the columns
//	     // available to the client--are metadata columns exposed to them? It's
//	     // not obvious to me here.
//	     Set<String> columnKeys = columnIdentifierToIndex.keySet();
//        String[][] partialData = 
//            new String[numRowsToWrite][columnKeys.size()];
//	     // Now construct up the objects we need.
//        int columnIndex = 0;
//        for (String elementKey : columnKeys) {
//          // Get the column type
//          columns.put(elementKey, getColumnTypeLabelForElementKey(elementKey));
//          // get the column data and the table data.
//          List<String> columnData = new ArrayList<String>();
//          for (int i = 0; i < numRowsToWrite; i++) {
//            String value = getData(i, elementKey);
//            columnData.add(value);
//            partialData[i][columnIndex] = value; 
//          }
//          allColumnData.put(elementKey, columnData);
//          columnIndex++;
//        }
//        Map<String, Object> outputObject = new HashMap<String, Object>();
//        outputObject.put(JSON_KEY_IN_COLLECTION_MODE, inCollectionMode);
//        outputObject.put(JSON_KEY_COUNT, count);
//        outputObject.put(JSON_KEY_COLLECTION_SIZE, collectionSize);
//        outputObject.put(JSON_KEY_IS_INDEXED, isIndexed);
//        outputObject.put(JSON_KEY_COLUMN_DATA, allColumnData);
//        outputObject.put(JSON_KEY_COLUMNS, columns);
//        outputObject.put(JSON_KEY_DATA, partialData);
//        Gson gson = new Gson();
//        String outputString = gson.toJson(outputObject);
//        String fileName = 
//            ODKFileUtils.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME) +
//            mTable.getTableProperties().getDbTableName() + "_data.json";
//        try {
//          PrintWriter writer = new PrintWriter(fileName, "UTF-8");
//          Log.d(TAG, "writing data out to: " + fileName);
//          writer.println(outputString);
//          writer.flush();
//          writer.close();
//        } catch (FileNotFoundException e) {
//          // TODO Auto-generated catch block
//          e.printStackTrace();
//        } catch (UnsupportedEncodingException e) {
//          // TODO Auto-generated catch block
//          e.printStackTrace();
//        }
//	   }

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
			dbh = DbHelper.getDbHelper(mActivity, mAppName);
			Log.d(TAG, "calling Control Constructor");
		}

		/**
		 * @see {@link ControlIf#openDetailView(String, String, String)}
		 */
		public boolean openDetailViewWithFile(String tableId, String rowId,
		     String relativePath) {
		  TableProperties tableProperties = getTablePropertiesById(tableId);
			if (tableProperties == null) {
			  Log.e(TAG, "table could not be found with id: " + tableId);
			  return false;
			}
			String pathToTablesFolder = ODKFileUtils
					.getAppFolder(mAppName);
			String pathToFile = 
			    pathToTablesFolder + File.separator + relativePath;
			Controller.launchDetailActivity(mActivity, tableId, rowId,
					pathToFile);
			return true;
		}

		/**
		 * Actually open the table. The sql-related parameters are null safe, so
		 * only pass them in if necessary.
		 * @see {@link ControlIf#openTableWithSqlQuery(String, String, String[])}
		 * @param tableId
		 * @param sqlWhereClause
		 * @param sqlSelectionArgs
		 * @return
		 */
		public boolean helperOpenTable(String tableId,
				String sqlWhereClause, String[] sqlSelectionArgs) {
			TableProperties tpToOpen = getTablePropertiesById(tableId);
			if (tpToOpen == null) {
				Log.e(TAG, "tableId [" + tableId + "] not in map");
				return false;
			}
			// We're not going to support search text from the js, so pass null.
			Controller.launchTableActivity(mActivity, tpToOpen, null,
					false, sqlWhereClause, sqlSelectionArgs);
			return true;
		}

		/**
		 * Actually open the table with the file.
       * see {@link ControlIf#openTableToListViewWithFileAndSqlQuery(String, 
       * String, String, String[])}
		 * @param tableId
		 * @param relativePath the path relative to the app folder
		 * @param sqlWhereClause
		 * @param sqlSelectionArgs
		 * @return
		 */
		public boolean helperOpenTableWithFile(String tableId, 
		     String relativePath, String sqlWhereClause,
			  String[] sqlSelectionArgs) {
			TableProperties tp = getTablePropertiesById(tableId);
			if (tp == null) {
				Log.e(TAG, "tableId [" + tableId + "] not in map");
				return false;
			}
			String pathToTablesFolder = ODKFileUtils
					.getAppFolder(mAppName);
			String pathToFile = 
			    pathToTablesFolder + File.separator + relativePath;
			// We're not supporting search text, so pass in null.
			Controller.launchListViewWithFilenameAndSqlQuery(mActivity, tp,
					null, null, false, pathToFile, sqlWhereClause,
					sqlSelectionArgs);
			return true;
		}

		/**
		 * Open the table to the map view.
		 * @see {@link ControlIf#openTableToMapViewWithSqlQuery(String, String, 
		 * String[])}
		 * @param tableId
		 * @param sqlWhereClause
		 * @param sqlSelectionArgs
		 * @return
		 */
		public boolean helperOpenTableToMapView(String tableId,
				String sqlWhereClause, String[] sqlSelectionArgs,
				String relativePath) {
			TableProperties tp = getTablePropertiesById(tableId);
			if (tp == null) {
				Log.e(TAG, "tableName [" + tableId + "] not in map");
				return false;
			}
			Log.e(TAG, "NOTE THAT THE SPECIFIC MAP VIEW FILE IS NOT SUPPORTED");
			// We're not supporting search text, so pass in null.
			Controller.launchMapView(mActivity, tp, null, null, false,
					sqlWhereClause, sqlSelectionArgs);
			return true;
		}

		/**
		 * Open the table to the spreadsheet view.
		 * @see {@link ControlIf#openTableToSpreadsheetViewWithSqlQuery(String, 
		 * String, String[])}
		 * @param tableId
		 * @param sqlWhereClause
		 * @param sqlSelectionArgs
		 * @return
		 */
		public boolean helperOpenTableToSpreadsheetView(String tableId,
				String sqlWhereClause,
				String[] sqlSelectionArgs) {
			initTpInfo();
			TableProperties tp = tableIdToProperties.get(tableId);
			if (tp == null) {
				Log.e(TAG, "tableId [" + tableId + "] not in map");
				return false;
			}
			// We're not supporting search text, so pass in null.
			Controller.launchSpreadsheetView(mActivity, tp, null, null,
					false, sqlWhereClause, sqlSelectionArgs);
			return true;
		}

		/**
		 * @see {@link ControlIf#releaseQueryResources(String)}
		 */
		public void releaseQueryResources(String tableId) {
			Iterator<TableData> iter = queryResults.iterator();
			while (iter.hasNext()) {
				TableData td = iter.next();
				if (td.getTableId().equals(tableId)) {
					iter.remove();
				}
			}
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
		 * @see {@link ControlIf#queryWithSql(String, String, String[])}
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
			TableProperties tp = getTablePropertiesById(tableName);
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
		 * @see {@link ControlIf#getAllTableIds()}
		 */
		public String getAllTableIds() {
		  Set<String> tableIdsSet = tableIdToProperties.keySet();
		  JSONArray result = new JSONArray(tableIdsSet);
		  return result.toString();
		}
		
		/**
		 * @see {@link ControlIf#getElementKey(String, String)}
		 * @param tableId
		 * @param elementPath
		 * @return
		 */
		public String getElementKey(String tableId, String elementPath) {
		  TableProperties tableProperties = getTablePropertiesById(tableId);
		  return tableProperties.getElementKeyFromElementPath(elementPath);
		}
		
		/**
		 * @see {@link ControlIf#getColumnDisplayName(String, String)}
		 * @param tableId
		 * @param elementPath
		 * @return
		 */
		public String getColumnDisplayName(String tableId, String elementPath) {
		  String elementKey = this.getElementKey(tableId, elementPath);
		  TableProperties tableProperties = getTablePropertiesById(tableId);
		  String displayName = 
		      tableProperties.getColumnByElementKey(elementKey).getDisplayName();
		  return displayName;
		}
		
		/**
		 * @see {@link ControlIf#getTableDisplayName(String)}
		 * @param tableId
		 * @return
		 */
		public String getTableDisplayName(String tableId) {
		  TableProperties tableProperties = getTablePropertiesById(tableId);
		  return tableProperties.getDisplayName();
		}
		
		/**
		 * @see {@link ControlIf#getPlatformInfo()}
		 * @return
		 */
		public String getPlatformInfo() {
		  // This is based on:
		  // org.opendatakit.survey.android.views.ODKShimJavascriptCallback
		  Map<String, String> platformInfo = new HashMap<String, String>();
		  platformInfo.put(PLATFORM_INFO_KEY_VERSION, Build.VERSION.RELEASE);
		  platformInfo.put(PLATFORM_INFO_KEY_CONTAINER, "Android");
		  platformInfo.put(PLATFORM_INFO_KEY_APP_NAME, 
		      TableFileUtils.ODK_TABLES_APP_NAME);
		  platformInfo.put(PLATFORM_INFO_KEY_BASE_URI, getBaseContentUri());
		  platformInfo.put(PLATFORM_INFO_KEY_LOG_LEVEL, "D");
		  JSONObject jsonObject = new JSONObject(platformInfo);
		  String result = jsonObject.toString();
		  return result;
		}
		
		/**
		 * @see {@link ControlIf#getFileAsUrl(String)}
		 * @param relativePath
		 * @return
		 */
		public String getFileAsUrl(String relativePath) {
		  String baseUri = getBaseContentUri();
		  String result = baseUri + relativePath;
		  return result;
		}
		
		/**
		 * @see ControlIf#columnExists(String, String)
		 * @param tableId
		 * @param elementPath
		 * @return
		 */
		public boolean columnExists(String tableId, String elementPath) {
		  String elementKey = this.getElementKey(tableId, elementPath);
		  TableProperties tableProperties = getTablePropertiesById(tableId);
		  ColumnProperties columnProperties =
		      tableProperties.getColumnByElementKey(elementKey);
		  return columnProperties != null;
		}
		
		/**
		 * Return the base uri for the Tables app name with a trailing 
		 * separator.
		 * @return
		 */
		private String getBaseContentUri() {
		  Uri contentUri = FileProvider.getWebViewContentUri(getContext());
		  contentUri = Uri.withAppendedPath(contentUri, 
		      Uri.encode(TableFileUtils.ODK_TABLES_APP_NAME));
		  return contentUri.toString() + File.separator;
		}

		/**
		 * Launch the {@link CustomHomeScreenActivity} with the custom filename
		 * to display. The return type on this method currently is always true,
		 * should probably check if the file exists first.
		 *
		 * @param relativePath
		 */
		public boolean launchHTML(String relativePath) {
			Log.d(TAG, "in launchHTML with filename: " + relativePath);
         String pathToTablesFolder = ODKFileUtils
             .getAppFolder(mAppName);
         String pathToFile = 
             pathToTablesFolder + File.separator + relativePath;
			Intent i = new Intent(mActivity, CustomHomeScreenActivity.class);
			i.putExtra(CustomHomeScreenActivity.INTENT_KEY_FILENAME, pathToFile);
			mActivity.startActivity(i);
			return true;
		}

		/**
		 * Add a row with survey using the specified formId and screenPath. The
		 * jsonMap should be a Stringified json map mapping elementName to values
		 * to prepopulate with the add row request.
		 * @param tableId
		 * @param formId if null, uses the default form
		 * @param screenPath
		 * @param jsonMap
		 * @return true if the launch succeeded, false if something went wrong
		 */
		public boolean helperAddRowWithSurvey(
		    String tableId, String formId, String screenPath, String jsonMap) {
        // does this "to receive add" call make sense with survey? unclear.
        TableProperties tpToReceiveAdd = getTablePropertiesById(tableId);
        if (tpToReceiveAdd == null) {
          Log.e(TAG, "table [" + tableId + "] could not be found. " +
               "returning.");
          return false;
        }
        if (formId == null) {
          SurveyFormParameters surveyFormParameters = SurveyFormParameters
              .ConstructSurveyFormParameters(tpToReceiveAdd);
          formId = surveyFormParameters.getFormId();
        }
        Map<String, String> map = null;
        // Do this null check and only parse and return errors if the jsonMap
        // is not null. This allows other methods doing similar things to call
        // through using this method and passing null values.
        if (jsonMap != null) {
          map = CustomView.this.getMapFromJson(jsonMap);
          if (map == null) {
            Log.e(TAG, "couldn't parse values into map to give to Survey");
            return false;
          }
        }
        CustomView.this.addRowWithSurveyAndSpecificForm(tableId,
            tpToReceiveAdd, formId, screenPath, map);
        return true;
		}

      /**
       * Add a row with collect.
       * <p>
       * The check for null on formId is to try and minimize the amount of 
       * similarly-named methods in this class, which makes it hard to
       * maintain.
       * @param tableId
       * @param formId if this is null, it is assumed the caller is not
       * specifying a specific form, and instead the default form parameters
       * are tried to be constructed.
       * @param formVersion
       * @param formRootElement
       * @param jsonMap a json string of values to prepopulate the form with. a
       * null value won't prepopulate any values.
       * @return true if the launch succeeded, else false
       */
		public boolean helperAddRowWithCollect(
				String tableId, String formId, String formVersion,
				String formRootElement, String jsonMap) {
			// The first thing we need to do is get the correct TableProperties.
			TableProperties tpToReceiveAdd = getTablePropertiesById(tableId);
			if (tpToReceiveAdd == null) {
				Log.e(TAG, "table [" + tableId + "] cannot have a row added"
						+ " because it could not be found");
				return false;
			}
			Map<String, String> map = null;
			if (jsonMap != null) {
    			map = CustomView.this.getMapFromJson(jsonMap);
    			if (map == null) {
    				Log.e(TAG, "couldn't parse jsonString: " + jsonMap);
    				return false;
    			}
			}
			if (formId == null) {
			  // Then we're going to try and construct a form parameters using the
			  // default values
		     CollectFormParameters formParameters = CollectFormParameters
	               .constructCollectFormParameters(tpToReceiveAdd);
		     formId = formParameters.getFormId();
		     formVersion = formParameters.getFormVersion();
		     formRootElement = formParameters.getRootElement();
			}
			CustomView.this.addRowWithCollectAndSpecificForm(tableId, formId,
					formVersion, formRootElement, tpToReceiveAdd, map);
			return true;
		}
		
	    /**
	     * Launch survey to edit the row.
	     * @param tableId
	     * @param rowId
	     * @param formId
	     * @param screenPath
	     * @return true if the edit was launched successfully, else false
	     */
      public boolean helperEditRowWithSurvey(String tableId, 
          String rowId, String formId, String screenPath) {
        TableProperties tableToReceiveAdd = 
            getTablePropertiesById(tableId);
        if (tableToReceiveAdd == null) {
          Log.e(TAG, "table [" + tableId + "] cannot have a row edited with" +
          		" survey because it cannot be found");
          return false;
        }
        CustomView.this.editRowWithSurveyAndSpecificForm(tableToReceiveAdd,
            rowId, formId, screenPath);
        return true;
      }
      
      /**
       * Edit the given row with Collect. Returns true if things went well,
       * or false if something went wrong.
       * <p>
       * formId is checked for null--if it is, it tries to use the default
       * form. If not null, it uses that form.
       * @param tableId
       * @param rowId
       * @param formId
       * @param formVersion
       * @param formRootElement
       * @return
       */
      public boolean helperEditRowWithCollect(String tableId,
          String rowId, String formId, String formVersion, 
          String formRootElement) {
        TableProperties tpToReceiveAdd = getTablePropertiesById(tableId);
        if (tpToReceiveAdd == null) {
          Log.e(TAG, "table [" + tableId + "] cannot have row edited, " +
          		"because it cannot be found");
          return false;
        }
        if (formId == null) {
          // Then we want to construct the form parameters using default
          // values.
          CollectFormParameters formParameters = CollectFormParameters
              .constructCollectFormParameters(tpToReceiveAdd);
          formId = formParameters.getFormId();
          formVersion = formParameters.getFormVersion();
          formRootElement = formParameters.getRootElement();
        }
        Map<String, String> elementKeyToValue = 
            getElementKeyToValues(tableId, rowId);
        Intent editRowIntent = CollectUtil.getIntentForOdkCollectEditRow(
               CustomView.this.getContainerActivity(), tpToReceiveAdd,
               elementKeyToValue, formId, formVersion, formRootElement,
               rowId);
        if (editRowIntent == null) {
          Log.e(TAG, "the edit row with collect intent was null, returning " +
          		"false");
          return false;
        }
        CollectUtil.launchCollectToEditRow(
               CustomView.this.getContainerActivity(), editRowIntent,
               rowId);
        return true;
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
		  // TODO: if this avenue to create a table remains, we need to also
		  // prompt them for a tableId name.
		  TableProperties[] allTableProperties =
		      TableProperties.getTablePropertiesForAll(dbh,
		          KeyValueStore.Type.ACTIVE);
			String dbTableName = NameUtil.createUniqueDbTableName(tableName,dbh);
			TableProperties tp = TableProperties.addTable(dbh,
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