package org.opendatakit.tables.view.custom;

import java.util.HashMap;
import java.util.Map;
import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.Table;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;
import android.content.Context;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;


public abstract class CustomView extends LinearLayout {
    
    protected static WebView webView;
    private static ViewGroup lastParent;
    
    protected CustomView(Context context) {
        super(context);
        initCommonWebView(context);
    }
    
    public static void initCommonWebView(Context context) {
        if (webView != null) {
            return;
        }
        webView = new WebView(context);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {});
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
            return data.get(cp.getColumnDbName());
        }
    }
    
    /**
     * "Unused" warnings are suppressed because the public methods of this
     * class are meant to be called through the JavaScript interface.
     */
    protected class TableData {
        private final Table rawTable;
        private final UserTable userTable;
        private Map<String, Integer> colMap;			//Maps the column names with an index number
        private Map<Integer, Integer> collectionMap;	//Maps each collection with the number of rows under it
        private String[] primeColumns;					//Holds the db names of indexed columns
        protected Context context;
        private TableProperties tp;
    
        public TableData(TableProperties tp, Table table) {
            rawTable = table;
            userTable = null;
            this.tp = tp;
            initMaps(tp);
        }
        
        public TableData(TableProperties tp, UserTable table) {
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
                String abbr = cps[i].getAbbreviation();
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
        
        //Maps the number of rows to every collection of a table.
        private void initCollectionMap(TableProperties tp) {
        	Control c = new Control(context);
        	collectionMap = new HashMap<Integer, Integer>();
        	String colName = primeColumns[0].substring(1);			//Assumes that the first col is the main, indexed col
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
        	return (primeColumns.length != 0);
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
        
        protected Context context;
        private TableProperties[] allTps;
        private Map<String, TableProperties> tpMap;
        
        public Control(Context context) {
            this.context = context;
        }
        
        private void initTpInfo() {
            if (tpMap != null) {
                return;
            }
            tpMap = new HashMap<String, TableProperties>();
            allTps = TableProperties.getTablePropertiesForAll(
                    DbHelper.getDbHelper(context));
            for (TableProperties tp : allTps) {
                tpMap.put(tp.getDisplayName(), tp);
            }
        }
        
        public boolean openTable(String tableName, String query) {
            initTpInfo();
            if (!tpMap.containsKey(tableName)) {
                return false;
            }
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
            return new TableData(tp, dbt.getRaw(query, tp.getColumnOrder()));
        }
    }
}
