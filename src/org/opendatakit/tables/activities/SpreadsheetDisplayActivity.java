/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.activities;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.tables.R;
import org.opendatakit.tables.Activity.PropertyManager;
import org.opendatakit.tables.DataStructure.DisplayPrefs;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.view.SpreadsheetView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;


public class SpreadsheetDisplayActivity extends SherlockActivity
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
    private static final int MENU_ITEM_ID_EDIT_ROW =
        Controller.FIRST_FREE_MENU_ITEM_ID + 10;
    
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
        // remove a title
        setTitle("");
        dm = new DataManager(DbHelper.getDbHelper(this));
        c = new Controller(this, this, getIntent().getExtras());
    	ActionBar actionBar = getSupportActionBar();
    	actionBar.setDisplayHomeAsUpEnabled(true);
        init();
    }
    
    @Override
    public void init() {
        query = new Query(dm.getAllTableProperties(KeyValueStore.Type.ACTIVE), 
            c.getTableProperties());
        query.loadFromUserQuery(c.getSearchText());
        table = c.getIsOverview() ?
                c.getDbTable().getUserOverviewTable(query) :
                c.getDbTable().getUserTable(query);
        indexedCol = c.getTableViewSettings().getTableIndexedColIndex();
        // setting up the view
        c.setDisplayView(buildView());
        setContentView(c.getContainerView());
    }
    
    private View buildView() {
        if (table.getWidth() == 0) {
            TextView tv = new TextView(this);
            tv.setText("No data.");
            return tv;
        } else {
            return new SpreadsheetView(this, this, c.getTableViewSettings(),
                    table, indexedCol, new DisplayPrefs(this,
                            c.getTableProperties().getTableId()));
        }
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
    	ArrayList<String> oldPrimes = c.getTableProperties().getPrimeColumns();
    	ArrayList<String> newPrimes = new ArrayList<String>();
        newPrimes.add(cp.getColumnDbName());
        for (int i = 0; i < oldPrimes.size(); i++) {
            newPrimes.add(oldPrimes.get(i));
        }
        c.getTableProperties().setPrimeColumns(newPrimes);
    }
    
    void unsetColumnAsPrime(ColumnProperties cp) {
        ArrayList<String> oldPrimes = c.getTableProperties().getPrimeColumns();
        if (oldPrimes.size() == 0) {
            return;
        }
        ArrayList<String> newPrimes = new ArrayList<String>();
        for (String prime : oldPrimes) {
            if (prime.equals(cp.getColumnDbName())) {
                continue;
            }
            newPrimes.add(prime);
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
        if (c.handleActivityReturn(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    @Override
	public void onSearch() {
	    c.recordSearch();
	    init();
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        c.buildOptionsMenu(menu);
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (c.handleMenuItemSelection(item)) {
            return true;
        }
        switch (item.getItemId()) {
        case MENU_ITEM_ID_HISTORY_IN:
            openCollectionView(lastDataCellMenued / table.getWidth());
            return true;
        case MENU_ITEM_ID_EDIT_CELL:
            c.openCellEditDialog(
                    table.getRowId(lastDataCellMenued / table.getWidth()),
                    table.getData(lastDataCellMenued),
                    lastDataCellMenued % table.getWidth());
            return true;
        case MENU_ITEM_ID_DELETE_ROW:
            c.deleteRow(table.getRowId(lastDataCellMenued / table.getWidth()));
            init();
            return true;
        case MENU_ITEM_ID_EDIT_ROW:
    		c.editRow(table, (lastDataCellMenued / table.getWidth()));
        	// launch ODK Collect
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
            return false;
        }
    }
    
    @Override
    public void regularCellLongClicked(int cellId, int rawX, int rawY) {
        c.addOverlay(new CellPopout(cellId), 100, 100, rawX, rawY);
    }
    
    @Override
    public void regularCellDoubleClicked(int cellId) {
        c.openCellEditDialog(table.getRowId(cellId / table.getWidth()),
                table.getData(cellId), cellId % table.getWidth());
    }
    
    @Override
    public void prepRegularCellOccm(ContextMenu menu, int cellId) {
        lastDataCellMenued = cellId;
        if (c.getIsOverview() &&
                (c.getTableProperties().getPrimeColumns().size() > 0)) {
            menu.add(ContextMenu.NONE, MENU_ITEM_ID_HISTORY_IN,
                    ContextMenu.NONE, "View Collection");
        }
        menu.add(ContextMenu.NONE, MENU_ITEM_ID_EDIT_CELL, ContextMenu.NONE,
                "Edit Cell");
        menu.add(ContextMenu.NONE, MENU_ITEM_ID_DELETE_ROW, ContextMenu.NONE,
                "Delete Row");
        menu.add(ContextMenu.NONE, MENU_ITEM_ID_EDIT_ROW, ContextMenu.NONE,
        		"Edit Row");
    }
    
    @Override
    public void prepHeaderCellOccm(ContextMenu menu, int cellId) {
        lastHeaderCellMenued = cellId;
        ColumnProperties cp = c.getTableProperties().getColumns()[cellId];
        if (c.getTableProperties().isColumnPrime(cp.getColumnDbName())) {
            menu.add(ContextMenu.NONE, MENU_ITEM_ID_UNSET_COLUMN_AS_PRIME,
                    ContextMenu.NONE, "Unset as Prime");
        } else if ((c.getTableProperties().getSortColumn() != null) &&
                c.getTableProperties().getSortColumn()
                        .equals(cp.getColumnDbName())) {
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
	public void regularCellClicked(int cellId) {
		c.removeOverlay();
		// TODO Auto-generated method stub
		
	}

	@Override
	public void headerCellClicked(int cellId) {
		c.removeOverlay();
		// TODO Auto-generated method stub
		
	}

	@Override
	public void footerCellClicked(int cellId) {
		c.removeOverlay();
		// TODO Auto-generated method stub
		
	}

	@Override
	public void indexedColCellClicked(int cellId) {
		c.removeOverlay();
		// TODO Auto-generated method stub
		
	}

	@Override
	public void prepFooterCellOccm(ContextMenu menu, int cellId) {
		lastHeaderCellMenued = cellId;
        ColumnProperties cp = c.getTableProperties().getColumns()[cellId];
        if (c.getTableProperties().isColumnPrime(cp.getColumnDbName())) {
            menu.add(ContextMenu.NONE, MENU_ITEM_ID_UNSET_COLUMN_AS_PRIME,
                    ContextMenu.NONE, "Unset as Prime");
        } else if ((c.getTableProperties().getSortColumn() != null) &&
                c.getTableProperties().getSortColumn()
                        .equals(cp.getColumnDbName())) {
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
	public void prepIndexedColCellOccm(ContextMenu menu, int cellId) {
		// TODO Auto-generated method stub
		
	}

    private class CellPopout extends LinearLayout {
	    
	    private final int cellId;
	    private int lastDownX;
	    private int lastDownY;
	    
	    public CellPopout(int cellId) {
	        super(SpreadsheetDisplayActivity.this);
	        this.cellId = cellId;
	        Context context = SpreadsheetDisplayActivity.this;
	        TextView valueView = new TextView(context);
	        valueView.setText(table.getData(cellId));
	        Button menuButton = new Button(context);
	        menuButton.setText("M");
	        menuButton.setOnClickListener(new View.OnClickListener() {
	            @Override
	            public void onClick(View v) {
	                openCellMenu();
	            }
	        });
	        setBackgroundColor(Color.WHITE);
	        addView(valueView);
	        addView(menuButton);
	        lastDownX = 0;
	        lastDownY = 0;
	    }
	    
	    private void openCellMenu() {
	        final List<Integer> itemIds = new ArrayList<Integer>();
	        List<String> itemLabels = new ArrayList<String>();
	        if (c.getIsOverview() &&
	                (c.getTableProperties().getPrimeColumns().size() > 0)) {
	            itemIds.add(MENU_ITEM_ID_HISTORY_IN);
	            itemLabels.add("View Collection");
	        }
	        itemIds.add(MENU_ITEM_ID_EDIT_CELL);
	        itemLabels.add("Edit Cell");
	        itemIds.add(MENU_ITEM_ID_DELETE_ROW);
	        itemLabels.add("Delete Row");
	        itemIds.add(MENU_ITEM_ID_EDIT_ROW);
	        itemLabels.add("Edit Row");
	        AlertDialog.Builder builder = new AlertDialog.Builder(
	                SpreadsheetDisplayActivity.this);
	        builder.setItems(itemLabels.toArray(new String[0]),
	                new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int which) {
	                switch (itemIds.get(which)) {
	                case MENU_ITEM_ID_HISTORY_IN:
	                    openCollectionView(cellId / table.getWidth());
	                    c.removeOverlay();
	                    break;
	                case MENU_ITEM_ID_EDIT_CELL:
	                    c.openCellEditDialog(
	                            table.getRowId(cellId / table.getWidth()),
	                            table.getData(cellId),
	                            cellId % table.getWidth());
	                    c.removeOverlay();
	                    break;
	                case MENU_ITEM_ID_DELETE_ROW:
	                    c.deleteRow(table.getRowId(cellId / table.getWidth()));
	                    c.removeOverlay();
	                    init();
	                    break;
	                case MENU_ITEM_ID_EDIT_ROW:
	                    c.editRow(table, cellId / table.getWidth());
	                    c.removeOverlay();
	                    init();
	                    break;
	                }
	            }
	        });
	        builder.create().show();
	    }
	    
	    @Override
	    public boolean onTouchEvent(MotionEvent event) {
	        if (event.getAction() == MotionEvent.ACTION_DOWN) {
	            lastDownX = (Float.valueOf(event.getX())).intValue();
	            lastDownY = (Float.valueOf(event.getY())).intValue();
	            return true;
	        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
	            int x = (Float.valueOf(event.getRawX())).intValue();
	            int y = (Float.valueOf(event.getRawY())).intValue();
	            c.setOverlayLocation(x - lastDownX, y - lastDownY);
	            return true;
	        } else if (event.getAction() == MotionEvent.ACTION_UP) {
	            int x = (Float.valueOf(event.getRawX())).intValue();
	            int y = (Float.valueOf(event.getRawY())).intValue();
	            if (c.isInSearchBox(x, y)) {
	                String colName = c.getTableProperties().getColumns()
	                        [cellId % table.getWidth()].getDisplayName();
	                String value = table.getData(cellId);
	                c.appendToSearchBoxText(" " + colName + ":" + value);
	                c.removeOverlay();
	            }
	            return true;
	        } else {
	            return false;
	        }
	    }
	}
}
