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

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.view.graphs.BoxStemChart;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * An activity for display box-stem graphs.
 */
public class BoxStemGraphDisplayActivity extends Activity
        implements DisplayActivity, BoxStemChart.ClickListener {

    private static final int RCODE_ODKCOLLECT_ADD_ROW =
        Controller.FIRST_FREE_RCODE;
    
    private Controller c;
    private Query query;
    private UserTable table;
    private List<String> xValues;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        c = new Controller(this, this, getIntent().getExtras());
        DataManager dm = new DataManager(DbHelper.getDbHelper(this));
        query = new Query(dm.getAllTableProperties(), c.getTableProperties());
        init();
    }
    
    @Override
    public void init() {
        ColumnProperties xCol = c.getTableViewSettings().getBoxStemXCol();
        ColumnProperties yCol = c.getTableViewSettings().getBoxStemYCol();
        if ((xCol == null) || (yCol == null)) {
            handleInvalidSettings();
            return;
        }
        query.clear();
        query.loadFromUserQuery(c.getSearchText());
        query.setOrderBy(Query.SortOrder.ASCENDING, xCol, yCol);
        table = c.getDbTable().getUserTable(query);
        View view = buildView();
        c.setDisplayView(view);
        setContentView(c.getContainerView());
    }
    
    private View buildView() {
        if (table.getHeight() == 0) {
            return buildNoDataView();
        }
        int xCol = c.getTableProperties().getColumnIndex(
                c.getTableViewSettings().getBoxStemXCol().getColumnDbName());
        int yCol = c.getTableProperties().getColumnIndex(
                c.getTableViewSettings().getBoxStemYCol().getColumnDbName());
        xValues = new ArrayList<String>();
        Map<String, List<Double>> lists = new HashMap<String, List<Double>>();
        for (int i = 0; i < table.getHeight(); i++) {
            String x = table.getData(i, xCol);
            if (!lists.containsKey(x)) {
                xValues.add(x);
                lists.put(x, new ArrayList<Double>());
            }
            lists.get(x).add(Double.parseDouble(table.getData(i, yCol)));
        }
        //List<GValuePercentilePoint> data =
        //    new ArrayList<GValuePercentilePoint>();
        BoxStemChart.DataPoint[] data =
            new BoxStemChart.DataPoint[lists.size()];
        int index = 0;
        for (String x : xValues) {
            List<Double> yValues = lists.get(x);
            int count = yValues.size();
            double low = yValues.get(0);
            double high = yValues.get(count - 1);
            double midLow;
            double midHigh;
            if (yValues.size() == 1) {
                midLow = yValues.get(0);
                midHigh = yValues.get(0);
            } else if (yValues.size() % 2 == 0) {
                int mid = yValues.size() / 2;
                midLow = findMid(yValues, 0, mid);
                midHigh = findMid(yValues, mid + 1, yValues.size() - 1);
            } else if (yValues.size() == 3) {
                midLow = findMid(yValues, 0, 1);
                midHigh = findMid(yValues, 1, 2);
            } else {
                int mid = yValues.size() / 2;
                midLow = findMid(yValues, 1, mid);
                midHigh = findMid(yValues, mid + 2, yValues.size() - 1);
            }
            data[index] = new BoxStemChart.DataPoint(x, low, midLow, midHigh,
                    high);
            index++;
            //data.add(new GValuePercentilePoint(x, y, low, midLow, midHigh,
            //        high));
        }
        return new BoxStemChart(this, data, this);
        //return gFactory.getBoxStemGraph(data, "", "", "");
    }
    
    private double findMid(List<Double> list, int startIndex, int endIndex) {
        int range = endIndex - startIndex;
        if (range == 0) {
            return list.get(startIndex);
        } else if (range % 2 == 0) {
            int hr = range / 2;
            return (list.get(hr + startIndex) +
                    list.get(hr + startIndex + 1)) / 2;
        } else {
            return list.get((range / 2) + startIndex + 1);
        }
    }
    
    private View buildNoDataView() {
        TextView tv = new TextView(this);
        tv.setText("No data.");
        return tv;
    }
    
    private void handleInvalidSettings() {
        (new SettingsDialog(this)).show();
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
        return c.handleMenuItemSelection(item.getItemId());
    }
    
    @Override
    public void onSearch() {
        c.recordSearch();
        init();
    }
    
    @Override
    public void onClick(int index) {
        if (!c.getIsOverview()) {
            return;
        }
        ColumnProperties xCol = c.getTableViewSettings().getBoxStemXCol();
        String searchText = xCol.getDisplayName() + ":" + xValues.get(index);
        Controller.launchTableActivity(this, c.getTableProperties(),
                searchText, false);
    }
    
    private class SettingsDialog extends AlertDialog {
        
        private List<ColumnProperties> numberCols;
        
        public SettingsDialog(Context context) {
            super(context);
            numberCols = new ArrayList<ColumnProperties>();
            for (ColumnProperties cp : c.getTableProperties().getColumns()) {
                if (cp.getColumnType() == ColumnProperties.ColumnType.NUMBER) {
                    numberCols.add(cp);
                }
            }
            if (numberCols.size() == 0) {
                buildImpossibleSettingsView();
            } else {
                buildView(context);
            }
        }
        
        private void buildImpossibleSettingsView() {
            setMessage(getResources().getString(R.string.impossible_box_stem));
            setButton(getResources().getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    BoxStemGraphDisplayActivity.this.finish();
                }
            });
        }
        
        private void buildView(Context context) {
            setTitle(getResources().getString(
                    R.string.box_stem_settings_title));
            LinearLayout wrapper = new LinearLayout(context);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            // adding the x-axis spinner
            ColumnProperties selectedXCp =
                c.getTableViewSettings().getBoxStemXCol();
            int xSelection = -1;
            final ColumnProperties[] xCols =
                c.getTableProperties().getColumns();
            String[] xColDisplayNames = new String[xCols.length];
            for (int i = 0; i < xCols.length; i++) {
                if ((selectedXCp != null) && xCols[i].equals(selectedXCp)) {
                    xSelection = i;
                }
                xColDisplayNames[i] = xCols[i].getDisplayName();
            }
            ArrayAdapter<String> xAdapter = new ArrayAdapter<String>(context,
                    android.R.layout.simple_spinner_item, xColDisplayNames);
            xAdapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            final Spinner xSpinner = new Spinner(context);
            xSpinner.setAdapter(xAdapter);
            if (xSelection > 0) {
                xSpinner.setSelection(xSelection);
            }
            TextView xLabel = new TextView(context);
            xLabel.setText(getResources().getString(R.string.xaxis));
            xLabel.setPadding(10, 0, 0, 0);
            wrapper.addView(xLabel);
            wrapper.addView(xSpinner);
            // adding the y-axis spinner
            ColumnProperties selectedYCp =
                c.getTableViewSettings().getBoxStemYCol();
            int ySelection = -1;
            String[] yColDisplayNames = new String[numberCols.size()];
            for (int i = 0; i < numberCols.size(); i++) {
                if ((selectedYCp != null) &&
                        numberCols.get(i).equals(selectedYCp)) {
                    ySelection = i;
                }
                yColDisplayNames[i] = numberCols.get(i).getDisplayName();
            }
            ArrayAdapter<String> yAdapter = new ArrayAdapter<String>(context,
                    android.R.layout.simple_spinner_item, yColDisplayNames);
            yAdapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            final Spinner ySpinner = new Spinner(context);
            ySpinner.setAdapter(yAdapter);
            if (ySelection > 0) {
                ySpinner.setSelection(ySelection);
            }
            TextView yLabel = new TextView(context);
            yLabel.setText(getResources().getString(R.string.yaxis));
            yLabel.setPadding(10, 0, 0, 0);
            wrapper.addView(yLabel);
            wrapper.addView(ySpinner);
            // adding the set and cancel buttons
            Button setButton = new Button(context);
            setButton.setText(getResources().getString(R.string.set));
            setButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    c.getTableViewSettings().setBoxStemXCol(
                            xCols[xSpinner.getSelectedItemPosition()]);
                    c.getTableViewSettings().setBoxStemYCol(numberCols.get(
                            ySpinner.getSelectedItemPosition()));
                }
            });
            Button cancelButton = new Button(context);
            cancelButton.setText(getResources().getString(R.string.cancel));
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BoxStemGraphDisplayActivity.this.finish();
                }
            });
            LinearLayout buttonWrapper = new LinearLayout(context);
            buttonWrapper.addView(setButton);
            buttonWrapper.addView(cancelButton);
            wrapper.addView(buttonWrapper);
            // setting the dialog view
            setView(wrapper);
        }
    }
}
