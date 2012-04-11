package yoonsung.odk.spreadsheet.activities;

import yoonsung.odk.spreadsheet.Activity.PropertyManager;
import yoonsung.odk.spreadsheet.DataStructure.DisplayPrefs;
import yoonsung.odk.spreadsheet.data.ColumnProperties;
import yoonsung.odk.spreadsheet.data.DataManager;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.Query;
import yoonsung.odk.spreadsheet.data.UserTable;
import yoonsung.odk.spreadsheet.view.SpreadsheetView;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class SpreadsheetDisplayActivity extends Activity
        implements DisplayActivity, SpreadsheetView.Controller {
    
    private static final int MENU_ITEM_ID_HISTORY_IN =
        Controller.FIRST_FREE_MENU_ITEM_ID + 0;
    private static final int MENU_ITEM_ID_EDIT_CELL =
        Controller.FIRST_FREE_MENU_ITEM_ID + 1;
    private static final int MENU_ITEM_ID_DELETE_ROW =
        Controller.FIRST_FREE_MENU_ITEM_ID + 2;
    private static final int MENU_ITEM_ID_SET_COLUMN_AS_PRIME =
        Controller.FIRST_FREE_MENU_ITEM_ID + 3;
    private static final int MENU_ITEM_ID_UNSET_COLUMN_AS_PRIME =
        Controller.FIRST_FREE_MENU_ITEM_ID + 4;
    private static final int MENU_ITEM_ID_SET_COLUMN_AS_SORT =
        Controller.FIRST_FREE_MENU_ITEM_ID + 5;
    private static final int MENU_ITEM_ID_UNSET_COLUMN_AS_SORT =
        Controller.FIRST_FREE_MENU_ITEM_ID + 6;
    private static final int MENU_ITEM_ID_SET_AS_INDEXED_COL =
        Controller.FIRST_FREE_MENU_ITEM_ID + 7;
    private static final int MENU_ITEM_ID_UNSET_AS_INDEXED_COL =
        Controller.FIRST_FREE_MENU_ITEM_ID + 8;
    private static final int MENU_ITEM_ID_OPEN_COL_PROPS_MANAGER =
        Controller.FIRST_FREE_MENU_ITEM_ID + 9;
    
    private static final int RCODE_ODKCOLLECT_ADD_ROW = 0;
    
    private DataManager dm;
    private Controller c;
    private Query query;
    private UserTable table;
    private int indexedCol;
    
    private int lastDataCellMenued;
    private int lastHeaderCellMenued;
    
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
        table = c.getIsOverview() ?
                c.getDbTable().getUserOverviewTable(query) :
                c.getDbTable().getUserTable(query);
        indexedCol = c.getTableViewSettings().getTableIndexedColIndex();
        // setting up the view
        c.setDisplayView(buildView());
        setContentView(c.getWrapperView());
    }
    
    private View buildView() {
        return new SpreadsheetView(this, this, c.getTableViewSettings(), table,
                indexedCol,
                new DisplayPrefs(this, c.getTableProperties().getTableId()));
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
    
    void setColumnAsPrime(ColumnProperties cp) {
        String[] oldPrimes = c.getTableProperties().getPrimeColumns();
        String[] newPrimes = new String[oldPrimes.length + 1];
        newPrimes[0] = cp.getColumnDbName();
        for (int i = 0; i < oldPrimes.length; i++) {
            newPrimes[i + 1] = oldPrimes[i];
        }
        c.getTableProperties().setPrimeColumns(newPrimes);
    }
    
    void unsetColumnAsPrime(ColumnProperties cp) {
        String[] oldPrimes = c.getTableProperties().getPrimeColumns();
        if (oldPrimes.length == 0) {
            return;
        }
        String[] newPrimes = new String[oldPrimes.length - 1];
        int index = 0;
        for (String prime : oldPrimes) {
            if (prime.equals(cp.getColumnDbName())) {
                continue;
            }
            newPrimes[index] = prime;
            index++;
        }
        c.getTableProperties().setPrimeColumns(newPrimes);
    }
    
    void setColumnAsSort(ColumnProperties cp) {
        c.getTableProperties().setSortColumn(
                (cp == null) ? null : cp.getColumnDbName());
    }
    
    void setColumnAsIndexedCol(ColumnProperties cp) {
        c.getTableViewSettings().setTableIndexedCol(
                (cp == null) ? null : cp.getColumnDbName());
    }
    
    void openColumnPropertiesManager(ColumnProperties cp) {
        Intent intent = new Intent(this, PropertyManager.class);
        intent.putExtra(PropertyManager.INTENT_KEY_TABLE_ID,
                c.getTableProperties().getTableId());
        intent.putExtra(PropertyManager.INTENT_KEY_COLUMN_NAME,
                cp.getColumnDbName());
        startActivity(intent);
    }
    
    @Override
    public void onBackPressed() {
        c.onBackPressed();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
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
        if (c.handleMenuItemSelection(item.getItemId())) {
            return true;
        }
        switch (item.getItemId()) {
        case MENU_ITEM_ID_HISTORY_IN:
            openCollectionView(lastDataCellMenued / table.getWidth());
            return true;
        case MENU_ITEM_ID_EDIT_CELL:
            // TODO
            return true;
        case MENU_ITEM_ID_DELETE_ROW:
            c.deleteRow(table.getRowId(lastDataCellMenued / table.getWidth()));
            init();
            return true;
        case MENU_ITEM_ID_SET_COLUMN_AS_PRIME:
            setColumnAsPrime(c.getTableProperties()
                    .getColumns()[lastHeaderCellMenued]);
            init();
            return true;
        case MENU_ITEM_ID_UNSET_COLUMN_AS_PRIME:
            unsetColumnAsPrime(c.getTableProperties()
                    .getColumns()[lastHeaderCellMenued]);
            init();
            return true;
        case MENU_ITEM_ID_SET_COLUMN_AS_SORT:
            setColumnAsSort(c.getTableProperties()
                    .getColumns()[lastHeaderCellMenued]);
            init();
            return true;
        case MENU_ITEM_ID_UNSET_COLUMN_AS_SORT:
            setColumnAsSort(null);
            init();
            return true;
        case MENU_ITEM_ID_SET_AS_INDEXED_COL:
            setColumnAsIndexedCol(c.getTableProperties()
                    .getColumns()[lastHeaderCellMenued]);
            init();
            return true;
        case MENU_ITEM_ID_UNSET_AS_INDEXED_COL:
            setColumnAsIndexedCol(null);
            init();
            return true;
        case MENU_ITEM_ID_OPEN_COL_PROPS_MANAGER:
            openColumnPropertiesManager(c.getTableProperties()
                    .getColumns()[lastHeaderCellMenued]);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
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
    
    @Override
    public void regularCellClicked(int cellId) {
        // TODO
    }
    
    @Override
    public void headerCellClicked(int cellId) {
        // TODO
    }
    
    @Override
    public void footerCellClicked(int cellId) {
        // TODO
    }
    
    @Override
    public void indexedColCellClicked(int cellId) {
        // TODO
    }
    
    @Override
    public void prepRegularCellOccm(ContextMenu menu, int cellId) {
        lastDataCellMenued = cellId;
        if (c.getIsOverview() &&
                (c.getTableProperties().getPrimeColumns().length > 0)) {
            menu.add(ContextMenu.NONE, MENU_ITEM_ID_HISTORY_IN,
                    ContextMenu.NONE, "View Collection");
        }
        menu.add(ContextMenu.NONE, MENU_ITEM_ID_EDIT_CELL, ContextMenu.NONE,
                "Edit Cell");
        menu.add(ContextMenu.NONE, MENU_ITEM_ID_DELETE_ROW, ContextMenu.NONE,
                "Delete Row");
    }
    
    @Override
    public void prepHeaderCellOccm(ContextMenu menu, int cellId) {
        lastHeaderCellMenued = cellId;
        ColumnProperties cp = c.getTableProperties().getColumns()[cellId];
        if (c.getTableProperties().isColumnPrime(cp.getColumnDbName())) {
            menu.add(ContextMenu.NONE, MENU_ITEM_ID_UNSET_COLUMN_AS_PRIME,
                    ContextMenu.NONE, "Unset as Prime");
        } else if ((c.getTableProperties().getSortColumn() != null) &&
                c.getTableProperties().equals(cp.getColumnDbName())) {
            menu.add(ContextMenu.NONE, MENU_ITEM_ID_UNSET_COLUMN_AS_SORT,
                    ContextMenu.NONE, "Unset as Sort");
        } else {
            menu.add(ContextMenu.NONE, MENU_ITEM_ID_SET_COLUMN_AS_PRIME,
                    ContextMenu.NONE, "Set as Prime");
            menu.add(ContextMenu.NONE, MENU_ITEM_ID_SET_COLUMN_AS_SORT,
                    ContextMenu.NONE, "Set as Sort");
        }
        if (cellId == indexedCol) {
            menu.add(ContextMenu.NONE, MENU_ITEM_ID_UNSET_AS_INDEXED_COL,
                    ContextMenu.NONE, "Unreeze Column");
        } else {
            menu.add(ContextMenu.NONE, MENU_ITEM_ID_SET_AS_INDEXED_COL,
                    ContextMenu.NONE, "Freeze Column");
        }
        menu.add(ContextMenu.NONE, MENU_ITEM_ID_OPEN_COL_PROPS_MANAGER,
                ContextMenu.NONE, "Manage Column Properties");
    }
    
    @Override
    public void prepFooterCellOccm(ContextMenu menu, int cellId) {
        // TODO
    }
    
    @Override
    public void prepIndexedColCellOccm(ContextMenu menu, int cellId) {
        // TODO
    }
}
