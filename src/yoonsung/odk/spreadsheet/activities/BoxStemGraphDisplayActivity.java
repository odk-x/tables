package yoonsung.odk.spreadsheet.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.Library.graphs.GValuePercentilePoint;
import yoonsung.odk.spreadsheet.Library.graphs.GraphFactory;
import yoonsung.odk.spreadsheet.data.ColumnProperties;
import yoonsung.odk.spreadsheet.data.DataManager;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.DbTable;
import yoonsung.odk.spreadsheet.data.Query;
import yoonsung.odk.spreadsheet.data.TableProperties;
import yoonsung.odk.spreadsheet.data.TableViewSettings;
import yoonsung.odk.spreadsheet.data.UserTable;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * An activity for display box-stem graphs.
 */
public class BoxStemGraphDisplayActivity extends Activity
        implements DisplayActivity {
    
    private String searchText;
    private boolean isOverview;
    private TableProperties tp;
    private TableViewSettings tvs;
    private DbTable dbt;
    private Query query;
    private UserTable table;
    private Controller controller;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // getting intent information
        String tableId = getIntent().getStringExtra(
                Controller.INTENT_KEY_TABLE_ID);
        if (tableId == null) {
            throw new RuntimeException("null table ID");
        }
        searchText = getIntent().getStringExtra(Controller.INTENT_KEY_SEARCH);
        if (searchText == null) {
            searchText = "";
        }
        isOverview = getIntent().getBooleanExtra(
                Controller.INTENT_KEY_IS_OVERVIEW, false);
        // initializing data objects
        DataManager dm = new DataManager(DbHelper.getDbHelper(this));
        tp = dm.getTableProperties(tableId);
        tvs = isOverview ? tp.getOverviewViewSettings() :
                tp.getCollectionViewSettings();
        dbt = dm.getDbTable(tableId);
        query = new Query(dm.getAllTableProperties(), tp);
        controller = new Controller(this, this);
        // initializing
        init();
    }
    
    private void init() {
        if ((tvs.getBoxStemXCol() == null) || (tvs.getBoxStemYCol() == null)) {
            handleInvalidSettings();
            return;
        }
        query.clear();
        query.loadFromUserQuery(searchText);
        query.setOrderBy(tvs.getBoxStemYCol(), Query.SortOrder.ASCENDING);
        table = dbt.getUserTable(query);
        controller.setSearchText(searchText);
        View view = buildView();
        controller.setDisplayView(view);
        setContentView(controller.getWrapperView());
    }
    
    private View buildView() {
        if (table.getHeight() == 0) {
            return buildNoDataView();
        }
        int xCol = tp.getColumnIndex(tvs.getBoxStemXCol().getColumnDbName());
        int yCol = tp.getColumnIndex(tvs.getBoxStemYCol().getColumnDbName());
        Map<String, List<Double>> lists = new HashMap<String, List<Double>>();
        for (int i = 0; i < table.getHeight(); i++) {
            String x = table.getData(i, xCol);
            if (!lists.containsKey(x)) {
                lists.put(x, new ArrayList<Double>());
            }
            lists.get(x).add(Double.parseDouble(table.getData(i, yCol)));
        }
        GraphFactory gFactory = new GraphFactory(this);
        List<GValuePercentilePoint> data =
            new ArrayList<GValuePercentilePoint>();
        for (String x : lists.keySet()) {
            List<Double> yValues = lists.get(x);
            int count = yValues.size();
            double low = yValues.get(0);
            double high = yValues.get(count - 1);
            double y = findMid(yValues, 0, yValues.size() - 1);
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
            data.add(new GValuePercentilePoint(x, y, low, midLow, midHigh,
                    high));
        }
        return gFactory.getBoxStemGraph(data, "", "", "");
    }
    
    private double findMid(List<Double> list, int startIndex, int endIndex) {
        int range = endIndex - startIndex;
        if (range == 0) {
            return list.get(startIndex);
        } else if (range % 2 == 0) {
            int hr = range / 2;
            return (list.get(hr) + list.get(hr + 1)) / 2;
        } else {
            return list.get((range / 2) + 1);
        }
    }
    
    private View buildNoDataView() {
        EditText et = new EditText(this);
        et.setText("No data.");
        return et;
    }
    
    private void handleInvalidSettings() {
        (new SettingsDialog(this)).show();
    }
    
    @Override
    public void onSearch(String searchText) {
        this.searchText = searchText;
        init();
    }
    
    @Override
    public void onAddRow() {
        // TODO
    }
    
    private class SettingsDialog extends AlertDialog {
        
        private List<ColumnProperties> numberCols;
        
        public SettingsDialog(Context context) {
            super(context);
            numberCols = new ArrayList<ColumnProperties>();
            for (ColumnProperties cp : tp.getColumns()) {
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
            ColumnProperties selectedXCp = tvs.getBoxStemXCol();
            int xSelection = -1;
            final ColumnProperties[] xCols = tp.getColumns();
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
            ColumnProperties selectedYCp = tvs.getBoxStemYCol();
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
                    tvs.setBoxStemXCol(
                            xCols[xSpinner.getSelectedItemPosition()]);
                    tvs.setBoxStemYCol(numberCols.get(
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
