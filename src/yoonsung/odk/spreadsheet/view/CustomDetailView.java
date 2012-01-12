package yoonsung.odk.spreadsheet.view;

import java.util.Collections;
import java.util.Map;
import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * A view for displaying a customizable detail view of a row of data.
 * 
 * @author hkworden
 */
public class CustomDetailView extends WebView {

    private RowData jsData;
    
    public CustomDetailView(Context context) {
        super(context);
        getSettings().setJavaScriptEnabled(true);
        setWebViewClient(new WebViewClient() {});
        jsData = new RowData();
        addJavascriptInterface(jsData, "data");
    }
    
    public void display(Map<String, String> data) {
        jsData.setData(data);
        loadUrl("file:///sdcard/odk/tables/detailview.html");
    }
    
    private class RowData {
        
        private Map<String, String> data;
        
        public RowData() {
            data = Collections.emptyMap();
        }
        
        public void setData(Map<String, String> data) {
            this.data = data;
        }
        
        @SuppressWarnings("unused")
        public String get(String key) {
            return data.get(key);
        }
    }
}
