package org.opendatakit.tables.view.custom;

import java.util.HashMap;
import java.util.Map;
import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;
import android.content.Context;
import android.util.Log;


public class CustomTableView extends CustomView {
    
    private static final String DEFAULT_HTML =
        "<html><body>" +
        "<p>No filename has been specified.</p>" +
        "</body></html>";
    
    private Context context;
    private Map<String, Integer> colIndexTable;
    private TableProperties tp;
    private UserTable table;
    private String filename;
    
    private CustomTableView(Context context, String filename) {
        super(context);
        this.context = context;
        this.filename = filename;
        colIndexTable = new HashMap<String, Integer>();
    }
    
    public static CustomTableView get(Context context, TableProperties tp,
            UserTable table, String filename) {
        CustomTableView ctv = new CustomTableView(context, filename);
        ctv.set(tp, table);
        return ctv;
    }
    
    private void set(TableProperties tp, UserTable table) {
        this.tp = tp;
        this.table = table;
        colIndexTable.clear();
        ColumnProperties[] cps = tp.getColumns();
        for (int i = 0; i < cps.length; i++) {
            colIndexTable.put(cps[i].getDisplayName(), i);
            String abbr = cps[i].getAbbreviation();
            if (abbr != null) {
                colIndexTable.put(abbr, i);
            }
        }
    }
    
    public void display() {
        webView.addJavascriptInterface(new TableControl(context), "control");
        webView.addJavascriptInterface(new TableData(tp, table), "data");
        if (filename != null) {
            load("file:///" + filename);
        } else {
            loadData(DEFAULT_HTML, "text/html", null);
        }
        initView();
    }
    
    private class TableControl extends Control {
        
        public TableControl(Context context) {
            super(context);
        }
        
        @SuppressWarnings("unused")
        public boolean openItem(int index) {
            Controller.launchDetailActivity(context, tp, table, index);
            return true;
        }
    }
}
