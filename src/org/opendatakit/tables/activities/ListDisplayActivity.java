package org.opendatakit.tables.activities;

import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.view.ListDisplayView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class ListDisplayActivity extends Activity implements DisplayActivity {

    private static final int RCODE_ODKCOLLECT_ADD_ROW =
        Controller.FIRST_FREE_RCODE;
    
    private DataManager dm;
    private Controller c;
    private Query query;
    private ListViewController listController;
    private UserTable table;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        c = new Controller(this, this, getIntent().getExtras());
        dm = new DataManager(DbHelper.getDbHelper(this));
        query = new Query(dm.getAllTableProperties(), c.getTableProperties());
        listController = new ListViewController();
        init();
    }
    
    @Override
    public void init() {
        query.clear();
        query.loadFromUserQuery(c.getSearchText());
        table = c.getIsOverview() ?
                c.getDbTable().getUserOverviewTable(query) :
                c.getDbTable().getUserTable(query);
        c.setDisplayView(buildView());
        setContentView(c.getWrapperView());
    }
    
    private View buildView() {
        return ListDisplayView.buildView(this, c.getTableProperties(),
                c.getTableViewSettings(), listController, table);
    }
    
    private void openCollectionView(int rowNum) {
        query.clear();
        query.loadFromUserQuery(c.getSearchText());
        for (String prime : c.getTableProperties().getPrimeColumns()) {
            ColumnProperties cp = c.getTableProperties()
                    .getColumnByDbName(prime);
            int colNum = c.getTableProperties().getColumnIndex(prime);
            query.addConstraint(cp, table.getData(rowNum, colNum));
        }
        Controller.launchTableActivity(this, c.getTableProperties(),
                query.toUserQuery(), false);
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
    public void onAddRow() {
        Intent intent = c.getIntentForOdkCollectAddRow();
        if (intent != null) {
            startActivityForResult(intent, RCODE_ODKCOLLECT_ADD_ROW);
        }
    }
    
    private class ListViewController implements ListDisplayView.Controller {
        @Override
        public void onListItemClick(int rowNum) {
            if (c.getIsOverview() &&
                    (c.getTableProperties().getPrimeColumns().length > 0)) {
                openCollectionView(rowNum);
            } else {
                Controller.launchDetailActivity(ListDisplayActivity.this,
                        c.getTableProperties(), table, rowNum);
            }
        }
    }
}
