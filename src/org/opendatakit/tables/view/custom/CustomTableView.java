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

import java.util.HashMap;
import java.util.Map;

import org.opendatakit.tables.Activity.util.CustomViewUtil;
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
            String smsLabel = cps[i].getSmsLabel();
            if (smsLabel != null) {
                colIndexTable.put(smsLabel, i);
            }
        }
    }
    
    public void display() {
      // Load a basic screen as you're getting the other stuff ready to 
      // clear the old data.
      //load("file:////sdcard/odk/tables/loadingHtml.html");
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
