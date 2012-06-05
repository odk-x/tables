package org.opendatakit.tables.Activity.util;

import org.opendatakit.tables.Activity.TableManager;
import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.Preferences;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.view.CustomView;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;


public class Launcher extends Activity {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CustomView.initCommonWebView(this);
        String tableId = (new Preferences(this)).getDefaultTableId();
        if (tableId == null) {
            Intent i = new Intent(this, TableManager.class);
            startActivity(i);
        } else {
            TableProperties tp = TableProperties.getTablePropertiesForTable(
                    DbHelper.getDbHelper(this), tableId);
            Controller.launchTableActivity(this, tp, true);
        }
        finish();
    }
}
