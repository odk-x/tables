package yoonsung.odk.spreadsheet.view;

import java.util.HashMap;
import java.util.Map;
import yoonsung.odk.spreadsheet.Activity.SpreadSheet;
import yoonsung.odk.spreadsheet.Activity.TableActivity;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.TableProperties;
import yoonsung.odk.spreadsheet.data.UserTable;
import android.content.Context;
import android.content.Intent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * A view for displaying a customizable detail view of a row of data.
 * 
 * @author hkworden
 */
public class CustomDetailView extends WebView {
    
    private static final String DEFAULT_HTML =
        "<html><body>" +
        "<p>No detail view has been specified.</p>" +
        "</body></html>";
    
    private TableProperties tp;
    private Control control;
    private RowData jsData;
    
    public CustomDetailView(Context context, TableProperties tp) {
        super(context);
        this.tp = tp;
        getSettings().setJavaScriptEnabled(true);
        setWebViewClient(new WebViewClient() {});
        control = new Control(context);
        addJavascriptInterface(control, "control");
        jsData = new RowData();
        addJavascriptInterface(jsData, "data");
    }
    
    public void display(String tableId, String rowId, Map<String, String> data,
            Map<String, UserTable> joinData) {
        jsData.set(data, joinData);
        String filename = tp.getDetailViewFilename();
        if (filename != null) {
            loadUrl("file:///" + filename);
        } else {
            loadData(DEFAULT_HTML, "text/html", null);
        }
    }
    
    /**
     * "Unused" warnings are suppressed because the public methods of this
     * class are meant to be called through the JavaScript interface.
     */
    private class RowData {
        
        private Map<String, String> data;
        private Map<String, UserTable> joinData;
        
        RowData() {}
        
        RowData(Map<String, String> data, Map<String, UserTable> joinData) {
            this.data = data;
            this.joinData = joinData;
        }
        
        void set(Map<String, String> data, Map<String, UserTable> joinData) {
            this.data = data;
            this.joinData = joinData;
        }
        
        @SuppressWarnings("unused")
        public String get(String key) {
            return data.get(key);
        }
        
        @SuppressWarnings("unused")
        public JoinData getJoin(String key) {
            return new JoinData(joinData.get(key));
        }
    }
    
    /**
     * "Unused" warnings are suppressed because the public methods of this
     * class are meant to be called through the JavaScript interface.
     */
    private class JoinData {
        
        private UserTable table;
        
        public JoinData(UserTable table) {
            this.table = table;
        }
        
        @SuppressWarnings("unused")
        public int getCount() {
            return table.getHeight();
        }
        
        @SuppressWarnings("unused")
        public RowData getData(int index) {
            Map<String, String> data = new HashMap<String, String>();
            for (int i = 0; i < table.getWidth(); i++) {
                data.put(table.getHeader(i), table.getData(index, i));
            }
            return new RowData(data, null);
        }
    }
    
    private class Control {
        
        private Context context;
        
        public Control(Context context) {
            this.context = context;
        }
        
        @SuppressWarnings("unused")
        public void openTable(String tableName, String query) {
            TableProperties[] tps = TableProperties.getTablePropertiesForAll(
                    DbHelper.getDbHelper(context));
            String tableId = null;
            for (TableProperties tp : tps) {
                if (tp.getDisplayName().equals(tableName)) {
                    tableId = tp.getTableId();
                }
            }
            if (tableId == null) {
                return;
            }
            Intent intent = new Intent(context, SpreadSheet.class);
            intent.putExtra(TableActivity.INTENT_KEY_TABLE_ID, tableId);
            intent.putExtra(TableActivity.INTENT_KEY_QUERY, query);
            context.startActivity(intent);
        }
    }
}
