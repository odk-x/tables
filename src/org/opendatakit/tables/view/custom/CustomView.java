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
package org.opendatakit.tables.view.custom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendatakit.tables.Activity.TableManager;
import org.opendatakit.tables.DataStructure.ColumnColorRuler;
import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.Table;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableType;
import org.opendatakit.tables.data.UserTable;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

	protected CustomView(Context context) {
		super(context);
		initCommonWebView(context);
	}

	public static void initCommonWebView(Context context) {
		if (webView != null) {
			// do this every time to try and clear the old data.
			//webView.clearView();
			//webView.loadData(CustomViewUtil.LOADING_HTML_MESSAGE, "text/html", 
			//    null);
			return;
		}
		webView = new WebView(context);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(new WebViewClient() {

			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				super.onReceivedError(view, errorCode, description, failingUrl);
				Log.e("CustomView", "onReceivedError: " + description + " at " + failingUrl);
			}});

		webView.setWebChromeClient(new WebChromeClient(){

			@Override
			public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
				Log.i("CustomView", "onConsoleMessage " + 
						consoleMessage.messageLevel().name() + consoleMessage.message());

				return super.onConsoleMessage(consoleMessage);
			}

			@Override
			@Deprecated
			public void onConsoleMessage(String message, int lineNumber, String sourceID) {
				// TODO Auto-generated method stub
				super.onConsoleMessage(message, lineNumber, sourceID);
				Log.i("CustomView", "onConsoleMessage " + message);
			}

			@Override
			public void onReachedMaxAppCacheSize(long requiredStorage, long quota,
					QuotaUpdater quotaUpdater) {
				// TODO Auto-generated method stub
				super.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
				Log.i("CustomView", "onReachedMaxAppCacheSize " + Long.toString(quota));
			}});
	}

	protected void initView() {
		if (lastParent != null) {
			lastParent.removeView(webView);
		}
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.FILL_PARENT);
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
	 * "Unused" warnings are suppressed because the public methods of this
	 * class are meant to be called through the JavaScript interface.
	 */
	protected class RowData {

		private final TableProperties tp;
		private Map<String, String> data;

		RowData(TableProperties tp) {
			this.tp = tp;
		}

		RowData(TableProperties tp, Map<String, String> data) {
			this.tp = tp;
			this.data = data;
		}

		void set(Map<String, String> data) {
			this.data = data;
		}

		public String get(String key) {
			ColumnProperties cp = tp.getColumnByUserLabel(key);
			if (cp == null) {
				return null;
			}
			return data.get(cp.getElementKey());
		}
	}

	/**
	 * "Unused" warnings are suppressed because the public methods of this
	 * class are meant to be called through the JavaScript interface.
	 */
	protected class TableData {

		private static final String TAG = "TableData";

		private final Table rawTable;
		private final UserTable userTable;
		private Map<String, Integer> colMap;			//Maps the column names with an index number
		private Map<Integer, Integer> collectionMap;	//Maps each collection with the number of rows under it
		private ArrayList<String> primeColumns;			//Holds the db names of indexed columns
		protected Context context;
		private TableProperties tp;

		public TableData(TableProperties tp, Table table) {
			Log.d(TAG, "calling TableData constructor with Table");
			rawTable = table;
			userTable = null;
			this.tp = tp;
			initMaps(tp);
		}

		public TableData(TableProperties tp, UserTable table) {
			Log.d(TAG, "calling TableData constructor with UserTable");
			rawTable = null;
			userTable = table;
			this.tp = tp;
			initMaps(tp);

			//The collectionMap will be initialized if the table is indexed.
			if(isIndexed()) {
				initCollectionMap(tp);
			}
		}

		//Initializes the colMap and primeColumns that provide methods quick access to the current table's state.
		private void initMaps(TableProperties tp) {
			colMap = new HashMap<String, Integer>();

			ColumnProperties[] cps = tp.getColumns();
			primeColumns = tp.getPrimeColumns();

			for (int i = 0; i < cps.length; i++) {
				colMap.put(cps[i].getDisplayName(), i);
				String abbr = cps[i].getSmsLabel();
				if (abbr != null) {
					colMap.put(abbr, i);
				}
			}
		}

		//Returns the number of rows in the table being viewed.
		public int getCount() {
			if (rawTable == null) {
				return userTable.getHeight();
			} else {
				return rawTable.getHeight();
			}
		}
		/*
		 * @param: colName, column name in the userTable/rawTable
		 * @return: returns a String in JSONArray format containing all
		 * the row data for the given column name
		 * format: [row1, row2, row3, row4]
		 */
		public String getColumnData(String colName) {
			ArrayList<String> arr = new ArrayList<String>();
			for(int i = 0; i < getCount(); i++) {
				if (colMap.containsKey(colName)) {
					if (rawTable == null) {
						arr.add(i, userTable.getData(i, colMap.get(colName)));
					} else {
						arr.add(i, rawTable.getData(i, colMap.get(colName)));
					}
				} else {
					arr.add(i, "");
				}
			}
			return new JSONArray(arr).toString();
		}

		public String getColumns() {
			Map<String, String> colInfo = new HashMap<String, String>();
			for(String column: colMap.keySet()) {
				String dBName = tp.getColumnByDisplayName(column);
				String label = tp.getColumnByElementKey(dBName).getColumnType().label();
				colInfo.put(column, label);
			}			
			return new JSONObject(colInfo).toString();
		}
		
		//get color rules for colName
		public String getColumnColorRules(String colName, int value) {
			String elementKey = tp.getColumnByDisplayName(colName);
			ColumnColorRuler colRul = new ColumnColorRuler(tp, elementKey);
			return String.format("#%06X", (0xFFFFFF & colRul.getForegroundColor("" + value, -16777216)));
		}

		//Maps the number of rows to every collection of a table.
		private void initCollectionMap(TableProperties tp) {
			Control c = new Control(context);
			collectionMap = new HashMap<Integer, Integer>();
			String colName = primeColumns.get(0).substring(1);			//Assumes that the first col is the main, indexed col
			for(String col : colMap.keySet()) {
				if(col.equalsIgnoreCase(colName)) {
					colName = col;
				}
			}
			//Queries the original table for the rows in every collection and stores the number of resulting rows for each.
			for(int i = 0; i < getCount(); i++) {	            	
				String tableName = tp.getDisplayName();
				String searchText = colName + ":" + getData(i, colName);
				TableData data = c.query(tableName, searchText);
				collectionMap.put(i, data.getCount());
			}
		}

		//Returns the number of rows in the collection at the given row index.
		public int getCollectionSize(int rowNum) {
			return collectionMap.get(rowNum);
		}

		//Returns whether the table is indexed.
		public boolean isIndexed() {
			return (!primeColumns.isEmpty());
		}

		//Returns the cell data at the given offset into the table. 

		public String getData(int rowNum, String colName) {
			if (colMap.containsKey(colName)) {
				if (rawTable == null) {
					return userTable.getData(rowNum, colMap.get(colName));
				} else {
					return rawTable.getData(rowNum, colMap.get(colName));
				}
			} else {
				return null;
			}
		}

	}

	protected class Control {

		private static final String TAG = "CustomView.Control";

		protected Context context;
		private TableProperties[] allTps;
		private Map<String, TableProperties> tpMap;
		private DbHelper dbh;

		public Control(Context context) {
			this.context = context;
			dbh = DbHelper.getDbHelper(context);
			Log.d(TAG, "calling Control Constructor");
		}

		private void initTpInfo() {
			if (tpMap != null) {
				return;
			}
			tpMap = new HashMap<String, TableProperties>();
			allTps = TableProperties.getTablePropertiesForAll(
					DbHelper.getDbHelper(context),
					KeyValueStore.Type.ACTIVE);
			for (TableProperties tp : allTps) {
				tpMap.put(tp.getDisplayName(), tp);
			}
		}

		public boolean openTable(String tableName, String query) {
		  Log.d(TAG, "in openTable for table: " + tableName);
			initTpInfo();
			if (!tpMap.containsKey(tableName)) {
			  Log.e(TAG, "tableName [" + tableName + "] not in map");
				return false;
			}
			Log.e(TAG, "launching table activity for " + tableName);
			Controller.launchTableActivity(context, tpMap.get(tableName),
					query, false);
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
			DbTable dbt = DbTable.getDbTable(DbHelper.getDbHelper(context),
					tp.getTableId());
			ArrayList<String> columnOrder = tp.getColumnOrder();
			return new TableData(tp, dbt.getRaw(query, columnOrder.toArray(new String[columnOrder.size()])));
		}
		
		/**
		 * Return a list of the display names for all the tables in the database
		 * sorted in case insensitive order.
		 * @return
		 */
		public JSONArray getTableDisplayNames() {
		  Log.d(TAG, "called getTableDisplayNames()");
		  initTpInfo();
		  List<String> allNames = 
		      Arrays.asList(tpMap.keySet().toArray(new String[0]));
		  Collections.sort(allNames, String.CASE_INSENSITIVE_ORDER);
		  JSONArray result = new JSONArray((Collection<String>) allNames);
		  return result;
		}
		
		public void testVoid() {
		  Log.e(TAG, "testVoid() reporting!");
		}
		
		/**
		 * Create an alert that will allow for a new table name. This might be
		 * to rename an existing table, if isNewTable false, or it could be a new
		 * table, if isNewTable is true.
		 * <p>
		 * This method is based on {@link TableManager.alertForNewTableName}.
		 * The parameters are the same for the sake of consistency.
		 * <p>
		 * As this method does not access the javascript, the caller is 
		 * responsible for refreshing the displayed information.
		 * @param isNewTable
		 * @param tableType this is the string representation of TableType. It 
		 * must construct the correct value for {@link TableType.valueOf}.
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
		  AlertDialog.Builder alert = new AlertDialog.Builder(context);
		  alert.setTitle(context.getString(
		      org.opendatakit.tables.R.string.name_of_new_table));
		  // An edit text for getting user input.
		  final EditText input = new EditText(context);
		  alert.setView(input);
		  if (givenTableName != null) {
		    input.setText(givenTableName);
		  }
		  // OK Action: create a new table.
		  alert.setPositiveButton(
		      context.getString(org.opendatakit.tables.R.string.ok),
		      new DialogInterface.OnClickListener() {
              
              @Override
              public void onClick(DialogInterface dialog, int which) {
                String newTableName = input.getText().toString().trim();
                if (newTableName == null || newTableName.equals("")) {
                  Toast toast = Toast.makeText(context, 
                      "Table name cannot be empty!", 
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
		   
		  alert.setNegativeButton(org.opendatakit.tables.R.string.cancel, 
		      new DialogInterface.OnClickListener() {
              
              @Override
              public void onClick(DialogInterface dialog, int which) {
                // Cancel it, do nothing.
              }
            });
		  newTableAlert = alert.create();
		  newTableAlert.getWindow().setSoftInputMode(WindowManager.
		      LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		  newTableAlert.show();
		}
		
		private void addTable(String tableName, TableType tableType) {
		  String dbTableName =
		      TableProperties.createDbTableName(dbh, tableName);
		  TableProperties tp = TableProperties.addTable(dbh, dbTableName, 
		      tableName, tableType, KeyValueStore.Type.ACTIVE);
		}
	}
	
}