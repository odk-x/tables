package yoonsung.odk.spreadsheet.activities;

import java.util.ArrayList;
import java.util.List;
import yoonsung.odk.spreadsheet.Library.graphs.GXYPoint;
import yoonsung.odk.spreadsheet.Library.graphs.GraphFactory;
import yoonsung.odk.spreadsheet.data.DataManager;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.DbTable;
import yoonsung.odk.spreadsheet.data.Query;
import yoonsung.odk.spreadsheet.data.TableProperties;
import yoonsung.odk.spreadsheet.data.TableViewSettings;
import yoonsung.odk.spreadsheet.data.UserTable;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;


public class LineGraphDisplayActivity extends Activity
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
        query.loadFromUserQuery(searchText);
        table = isOverview ? dbt.getUserOverviewTable(query) :
                dbt.getUserTable(query);
        // setting up display
        controller = new Controller(this, this);
        controller.setSearchText(searchText);
        View view = buildView();
        if (view == null) {
            // TODO
            return;
        }
        controller.setDisplayView(view);
        setContentView(controller.getWrapperView());
    }
    
    private View buildView() {
        String xColDbName = tvs.getLineXCol().getColumnDbName();
        String yColDbName = tvs.getLineYCol().getColumnDbName();
        int xCol = tp.getColumnIndex(xColDbName);
        int yCol = tp.getColumnIndex(yColDbName);
        GraphFactory gFactory = new GraphFactory(this);
        List<GXYPoint> points = new ArrayList<GXYPoint>();
        for (int i = 0; i < table.getHeight(); i++) {
            double x = Double.parseDouble(table.getData(i, xCol));
            double y = Double.parseDouble(table.getData(i, yCol));
            points.add(new GXYPoint(x, y));
        }
        return gFactory.getXYLineGraph(points, "", "", "");
    }
    
    @Override
    public void onSearch(String searchText) {
        
    }
    
    @Override
    public void onAddRow() {
        
    }
}
