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
import java.util.List;

import org.joda.time.DateTime;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.ColumnType;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DataUtil;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.view.graphs.LineChart;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class LineGraphDisplayActivity extends Activity
        implements DisplayActivity {

    private static final int RCODE_ODKCOLLECT_ADD_ROW =
        Controller.FIRST_FREE_RCODE;
    
    private DataManager dm;
    private Controller c;
    private Query query;
    private UserTable table;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        c = new Controller(this, this, getIntent().getExtras());
        dm = new DataManager(DbHelper.getDbHelper(this));
        query = new Query(dm.getAllTableProperties(KeyValueStore.Type.ACTIVE), 
            c.getTableProperties());
        init();
    }
    
    @Override
    public void init() {
        ColumnProperties xCol = c.getTableViewSettings().getLineXCol();
        ColumnProperties yCol = c.getTableViewSettings().getLineYCol();
        query.clear();
        query.loadFromUserQuery(c.getSearchText());
        query.setOrderBy(Query.SortOrder.ASCENDING, xCol);
        table = c.getIsOverview() ?
                c.getDbTable().getUserOverviewTable(query) :
                c.getDbTable().getUserTable(query);
        List<Double> yValues = new ArrayList<Double>();
        int xIndex = c.getTableProperties().getColumnIndex(
                xCol.getColumnDbName());
        int yIndex = c.getTableProperties().getColumnIndex(
                yCol.getColumnDbName());
        if (xCol.getColumnType() == ColumnType.NUMBER ||
        	xCol.getColumnType() == ColumnType.INTEGER) {
            List<Double> xValues = new ArrayList<Double>();
            for (int i = 0; i < table.getHeight(); i++) {
                xValues.add(Double.valueOf(table.getData(i, xIndex)));
                yValues.add(Double.valueOf(table.getData(i, yIndex)));
            }
            c.setDisplayView(LineChart.createNumberLineChart(this, xValues,
                    yValues));
        } else {
            DataUtil du = DataUtil.getDefaultDataUtil();
            List<DateTime> xValues = new ArrayList<DateTime>();
            for (int i = 0; i < table.getHeight(); i++) {
                DateTime dt = du.parseDateTimeFromDb(table.getData(i, xIndex));
                xValues.add(dt);
                yValues.add(Double.valueOf(table.getData(i, yIndex)));
            }
            c.setDisplayView(LineChart.createDateLineChart(this, xValues,
                    yValues));
        }
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
            return;
        }
        switch (requestCode) {
        case RCODE_ODKCOLLECT_ADD_ROW:
            c.addRowFromOdkCollectForm(
                    Integer.valueOf(data.getData().getLastPathSegment()));
            init();
            break;
        default:
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
        return c.handleMenuItemSelection(item);
    }
    
    @Override
    public void onSearch() {
        c.recordSearch();
        init();
    }
}
