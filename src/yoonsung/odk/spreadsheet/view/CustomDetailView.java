package yoonsung.odk.spreadsheet.view;

import java.util.HashMap;
import java.util.Map;
import yoonsung.odk.spreadsheet.data.UserTable;
import android.content.Context;
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
    
    private String filename;
    private RowData jsData;
    
    public CustomDetailView(Context context, String filename) {
        super(context);
        this.filename = filename;
        getSettings().setJavaScriptEnabled(true);
        setWebViewClient(new WebViewClient() {});
        jsData = new RowData();
        addJavascriptInterface(jsData, "data");
    }
    
    public void display(long tableId, int rowId, Map<String, String> data,
            Map<String, UserTable> joinData) {
        jsData.set(data, joinData);
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
}
