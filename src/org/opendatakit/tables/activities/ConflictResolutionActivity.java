package org.opendatakit.tables.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable.ConflictTable;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.view.ConflictResolutionView;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class ConflictResolutionActivity extends Activity
        implements DisplayActivity, ConflictResolutionView.Controller {
    
    private DataManager dm;
    private Controller c;
    private Query query;
    private ConflictTable table;
    private List<Stack<RowChange>> rowChanges;
    private ConflictResolutionView crv;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dm = new DataManager(DbHelper.getDbHelper(this));
        c = new Controller(this, this, getIntent().getExtras());
        init();
    }
    
    @Override
    public void init() {
        query = new Query(dm.getAllTableProperties(), c.getTableProperties());
        query.loadFromUserQuery(c.getSearchText());
        table = c.getDbTable().getConflictTable(query);
        rowChanges = new ArrayList<Stack<RowChange>>(table.getCount());
        for (int i = 0; i < table.getCount(); i++) {
            rowChanges.add(new Stack<RowChange>());
        }
        // setting up the view
        crv = new ConflictResolutionView(this, this, c.getTableViewSettings(),
                table);
        c.setDisplayView(crv);
        setContentView(c.getWrapperView());
    }
    
    @Override
    public void onBackPressed() {
        c.onBackPressed();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (c.handleActivityReturn(requestCode, resultCode, data)) {
        } else {
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
        if (c.handleMenuItemSelection(item.getItemId())) {
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }
    
    @Override
    public void onSearch() {
        c.recordSearch();
        init();
    }
    
    @Override
    public void onAddRow() {}
    
    @Override
    public void onSet(int index) {
        Stack<RowChange> changes = rowChanges.get(index);
        String[] colDbNames = c.getTableProperties().getColumnOrder();
        Map<String, String> values = new HashMap<String, String>();
        while (!changes.isEmpty()) {
            RowChange rc = changes.pop();
            values.put(colDbNames[rc.getColNum()], rc.getNewValue());
        }
        c.getDbTable().resolveConflict(table.getRowId(index),
                table.getSyncTag(index, 1), values);
        crv.removeRow(index);
    }
    
    @Override
    public void onUndo(int index) {
        if (rowChanges.get(index).isEmpty()) {
            return;
        }
        RowChange rc = rowChanges.get(index).pop();
        crv.setDatum(index, 0, rc.getColNum(),
                table.getValue(index, 0, rc.getColNum()));
    }
    
    @Override
    public void onDoubleClick(int index, int rowNum, int colNum) {
        if (rowNum == 0) {
            return;
        }
        RowChange rc = new RowChange(colNum, table.getValue(index, 1, colNum));
        rowChanges.get(index).add(rc);
        crv.setDatum(index, 0, colNum, table.getValue(index, 1, colNum));
    }
    
    private class RowChange {
        
        private final int colNum;
        private final String newValue;
        
        public RowChange(int colNum, String newValue) {
            this.colNum = colNum;
            this.newValue = newValue;
        }
        
        public int getColNum() {
            return colNum;
        }
        
        public String getNewValue() {
            return newValue;
        }
    }
}
