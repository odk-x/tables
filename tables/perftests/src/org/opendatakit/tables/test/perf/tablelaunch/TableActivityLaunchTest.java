package org.opendatakit.tables.test.perf.tablelaunch;

import org.opendatakit.tables.Activity.TableManager;
import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableViewSettings;
import org.opendatakit.tables.test.perf.util.DbUtil;
import org.opendatakit.tables.test.perf.util.Timer;
import android.app.Activity;
import android.test.InstrumentationTestCase;
import android.test.PerformanceTestCase;
import android.view.KeyEvent;


public class TableActivityLaunchTest extends InstrumentationTestCase
        implements PerformanceTestCase {
    
    private final Timer timer;
    
    public TableActivityLaunchTest() {
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
        String label = "custom list";
        Activity tableManager = launchActivity("org.opendatakit.tables",
                TableManager.class, null);
        getInstrumentation().waitForIdleSync();
        TableProperties tp = TableProperties.getTablePropertiesForTable(
                DbHelper.getDbHelper(tableManager),
                DbUtil.TEMPERATURE_TABLE_ID);
        //tp.getOverviewViewSettings().setViewType(viewType);
        timer.start();
        Controller.launchTableActivity(tableManager, tp, true);
        getInstrumentation().waitForIdleSync();
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_A);
        timer.end();
        timer.print(label + " launch");
    }
}
