package org.opendatakit.tables.view;

import java.util.HashMap;
import java.util.Map;
import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;
import android.content.Context;


public class CustomTableView extends CustomView {
    
    private static final String DEFAULT_HTML =
        "<html><body>" +
        "<p>No filename has been specified.</p>" +
        "</body></html>";
    
    private Map<String, Integer> colIndexTable;
    private TableProperties tp;
    private UserTable table;
    
    private CustomTableView(Context context) {
        super(context);
        addJavascriptInterface(new TableControl(context), "control");
        colIndexTable = new HashMap<String, Integer>();
    }
    
    public static CustomTableView get(Context context, TableProperties tp,
            UserTable table, String filename) {
        CustomTableView ctv = new CustomTableView(context);
        ctv.set(tp, table);
        if (filename != null) {
            ctv.loadUrl("file:///" + filename);
        } else {
            ctv.loadData(DEFAULT_HTML, "text/html", null);
        }
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
        addJavascriptInterface(new TableData(tp, table), "data");
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
