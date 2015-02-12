package org.opendatakit.tables.test.perf.tablelaunch;

import org.opendatakit.tables.Activity.TableManager;
import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.test.perf.util.DbUtil;
import org.opendatakit.tables.test.perf.util.Timer;
import android.app.Activity;
import android.test.InstrumentationTestCase;
import android.test.PerformanceTestCase;


public class TableCollectionLaunchTest extends InstrumentationTestCase
        implements PerformanceTestCase {
    
    private final Timer timer;
    
    public TableCollectionLaunchTest() {
        timer = new Timer();
    }
    
    public boolean isPerformanceOnly() {
        return true;
    }
    
    public int startPerformance(
            PerformanceTestCase.Intermediates intermediates) {
        return 1;
    }
    
    public void testLaunches() {
        //int viewType = TableViewSettings.Type.SPREADSHEET;
        String label = "line";
        Activity tableManager = launchActivity("org.opendatakit.tables",
                TableManager.class, null);
        getInstrumentation().waitForIdleSync();
        TableProperties tp = TableProperties.getTablePropertiesForTable(
                DbHelper.getDbHelper(tableManager),
                DbUtil.TEMPERATURE_TABLE_ID);
        //tp.getOverviewViewSettings().setViewType(viewType);
        Controller.launchTableActivity(tableManager, tp, true);
        getInstrumentation().waitForIdleSync();
        timer.start();
        Controller.launchTableActivity(tableManager, tp, "fridge_id:0", false);
        getInstrumentation().waitForIdleSync();
        timer.end();
        timer.print(label + " launch");
    }
}
