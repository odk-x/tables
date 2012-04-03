package yoonsung.odk.spreadsheet.activities;

import yoonsung.odk.spreadsheet.data.DataManager;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.DbTable;
import yoonsung.odk.spreadsheet.data.Query;
import yoonsung.odk.spreadsheet.data.TableProperties;
import yoonsung.odk.spreadsheet.data.TableViewSettings;
import yoonsung.odk.spreadsheet.data.UserTable;
import yoonsung.odk.spreadsheet.view.ListDisplayView;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;


public class ListDisplayActivity extends Activity implements DisplayActivity {
    
    private String searchText;
    private boolean isOverview;
    private TableProperties tp;
    private TableViewSettings tvs;
    private DbTable dbt;
    private Query query;
    private UserTable table;
    private Controller controller;
    private ListViewController listController;
    
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
        listController = new ListViewController();
        // initializing
        init();
    }
    
    private void init() {
        query.clear();
        query.loadFromUserQuery(searchText);
        table = dbt.getUserTable(query);
        controller.setSearchText(searchText);
        View view = buildView();
        controller.setDisplayView(view);
        setContentView(controller.getWrapperView());
    }
    
    private View buildView() {
        return ListDisplayView.buildView(this, tp, tvs, listController, table);
    }
    
    @Override
    public void onSearch(String searchText) {
        this.searchText = searchText;
        init();
    }
    
    @Override
    public void onAddRow() {
        // TODO Auto-generated method stub
        
    }
    
    private class ListViewController implements ListDisplayView.Controller {
        @Override
        public void onListItemClick(int rowNum) {
            // TODO Auto-generated method stub
            
        }
    }
}
