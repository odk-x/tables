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
package org.opendatakit.tables.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable.ConflictTable;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.views.ConflictResolutionView;

import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;


public class ConflictResolutionActivity extends SherlockActivity
        implements DisplayActivity, ConflictResolutionView.Controller {

    private DbHelper dbh;
    private Controller c;
    private Query query;
    private ConflictTable table;
    private List<Stack<RowChange>> rowChanges;
    private ConflictResolutionView crv;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbh = DbHelper.getDbHelper(this);
        c = new Controller(this, this, getIntent().getExtras());
        init();
    }

    @Override
    public void init() {
        query = new Query(TableProperties.getTablePropertiesForAll(dbh,
            KeyValueStore.Type.ACTIVE),
            c.getTableProperties());
        query.loadFromUserQuery(c.getSearchText());
        table = c.getDbTable().getConflictTable(query);
        rowChanges = new ArrayList<Stack<RowChange>>(table.getCount());
        for (int i = 0; i < table.getCount(); i++) {
            rowChanges.add(new Stack<RowChange>());
        }
        // setting up the view
        crv = new ConflictResolutionView(this, this, c.getTableProperties(),
                table);
        c.setDisplayView(crv);
        setContentView(c.getContainerView());
    }

    @Override
    public void onBackPressed() {
        c.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (c.handleActivityReturn(requestCode, resultCode, data)) {
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        c.buildOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (c.handleMenuItemSelection(item)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onSearch() {
        c.recordSearch();
        init();
    }

    @Override
    public void onSet(int index) {
        Stack<RowChange> changes = rowChanges.get(index);
        TableProperties tp = c.getTableProperties();
        Map<String, String> values = new HashMap<String, String>();
        while (!changes.isEmpty()) {
            RowChange rc = changes.pop();
            values.put(tp.getColumnByIndex(rc.getColNum()).getElementKey(), rc.getNewValue());
        }
        c.getDbTable().resolveConflict(table.getRowId(index),
                table.getSyncTag(index, 1), values);
        crv.removeRow(index);
    }

    @Override
    public void onUndo(int index) {
        if (rowChanges.get(index).isEmpty()) {
            return;
        }
        RowChange rc = rowChanges.get(index).pop();
        crv.setDatum(index, 0, rc.getColNum(),
                table.getValue(index, 0, rc.getColNum()));
    }

    @Override
    public void onDoubleClick(int index, int rowNum, int colNum) {
        if (rowNum == 0) {
            return;
        }
        RowChange rc = new RowChange(colNum, table.getValue(index, 1, colNum));
        rowChanges.get(index).add(rc);
        crv.setDatum(index, 0, colNum, table.getValue(index, 1, colNum));
    }

    private class RowChange {

        private final int colNum;
        private final String newValue;

        public RowChange(int colNum, String newValue) {
            this.colNum = colNum;
            this.newValue = newValue;
        }

        public int getColNum() {
            return colNum;
        }

        public String getNewValue() {
            return newValue;
        }
    }
}
