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
    
    ////////////////////////////// TEST ///////////////////////////////
    
    public static CustomTableView get(Context context, TableProperties tp, UserTable table, String filename, int index) {
    	CustomTableView ctv = new CustomTableView(context, filename);
    	// Create a new table with only the row specified at index.
    	// Create all of the arrays necessary to create a UserTable.
    	String[] rowIds = new String[1];
    	String[] headers = new String[table.getWidth()];
    	String[][] data = new String[1][table.getWidth()];
    	String[] footers = new String[table.getWidth()];
    	// Set all the data for the table.
    	rowIds[0] = table.getRowId(index);
    	for (int i = 0; i < table.getWidth(); i++) {
    		headers[i] = table.getHeader(i);
    		data[0][i] = table.getData(index, i);
    		footers[i] = table.getFooter(i);
    	}
    	UserTable singleRowTable = new UserTable(rowIds, headers, data, footers);
    	
    	ctv.set(tp, singleRowTable);
    	return ctv;
    }
    
    public int getWebHeight() {
    	return webView.getMeasuredHeight();
    }
    
    //////////////////////////// END TEST /////////////////////////////

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
