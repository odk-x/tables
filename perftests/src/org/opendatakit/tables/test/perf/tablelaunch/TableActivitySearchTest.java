package org.opendatakit.tables.test.perf.tablelaunch;

import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.activities.SpreadsheetDisplayActivity;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.test.perf.util.DbUtil;
import org.opendatakit.tables.test.perf.util.Timer;
import android.app.Activity;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.test.PerformanceTestCase;
import android.widget.EditText;
import android.widget.ImageButton;


public class TableActivitySearchTest
        extends ActivityInstrumentationTestCase2<SpreadsheetDisplayActivity>
        implements PerformanceTestCase {
    
    private final Timer timer;
    
    public TableActivitySearchTest() {
        super(SpreadsheetDisplayActivity.class);
        timer = new Timer();
    }
    
    @Override
    public void setUp() {
        TableProperties tp = TableProperties.getTablePropertiesForTable(
                DbHelper.getDbHelper(getInstrumentation().getContext()),
                DbUtil.TABLE_A_ID);
        Intent intent = new Intent(getInstrumentation().getContext(),
                SpreadsheetDisplayActivity.class);
        intent.putExtra(Controller.INTENT_KEY_TABLE_ID, tp.getTableId());
        intent.putExtra(Controller.INTENT_KEY_IS_OVERVIEW, true);
        setActivityIntent(intent);
    }
    
    public void testSearch() {
        Activity activity = getActivity();
        final EditText searchField = (EditText) activity.findViewById(
                Controller.VIEW_ID_SEARCH_FIELD);
        final ImageButton searchButton = (ImageButton) activity.findViewById(
                Controller.VIEW_ID_SEARCH_BUTTON);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                searchField.setText("fridge_id:0");
                timer.start();
                searchButton.performClick();
                timer.end();
                timer.print("search");
            }
        };
        try {
            runTestOnUiThread(r);
        } catch(Throwable e) {
            throw new RuntimeException(e);
        }
        getInstrumentation().waitForIdleSync();
    }
    
    public boolean isPerformanceOnly() {
        return true;
    }
    
    public int startPerformance(
            PerformanceTestCase.Intermediates intermediates) {
        return 1;
    }
}
