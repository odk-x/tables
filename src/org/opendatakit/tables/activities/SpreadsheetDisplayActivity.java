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

import org.opendatakit.tables.Activity.PropertyManager;
import org.opendatakit.tables.Activity.util.CollectUtil;
import org.opendatakit.tables.Activity.util.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.DataStructure.DisplayPrefs;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.ColumnType;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.JoinColumn;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.view.SpreadsheetView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

public class SpreadsheetDisplayActivity extends SherlockActivity
        implements DisplayActivity, SpreadsheetView.Controller {
  
  private static final String TAG = "SpreadsheetDisplayActivity";
    
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
    // This should allow for the opening of a joined table.
    private static final int MENU_ITEM_ID_OPEN_JOIN_TABLE =
        Controller.FIRST_FREE_MENU_ITEM_ID + 11;
    private static final String MENU_ITEM_MSG_OPEN_JOIN_TABLE = 
        "Open Join Table";
    
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
        init();
    }
    
    @Override
    public void init() {
      TableProperties tp = c.getTableProperties();
        query = new Query(dm.getAllTableProperties(KeyValueStore.Type.ACTIVE), 
            tp);
        query.loadFromUserQuery(c.getSearchText());
        table = c.getIsOverview() ?
                c.getDbTable().getUserOverviewTable(query) :
                c.getDbTable().getUserTable(query);
        indexedCol = c.getTableViewSettings().getTableIndexedColIndex();
        // setting up the view
        c.setDisplayView(buildView(tp));
        setContentView(c.getContainerView());
    }
    
    private View buildView(TableProperties tp) {
        if (table.getWidth() == 0) {
            TextView tv = new TextView(this);
            tv.setText("No data.");
            return tv;
        } else {
            return new SpreadsheetView(this, this, tp, 
                c.getTableViewSettings(),
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
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        c.buildOptionsMenu(menu);
        return true;
    }
	
	@Override 
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
	  Log.d(TAG, "onOptionsItemSelected");
	  return true;
	}
	
	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
	  Log.d(TAG, "onContextItemSelected, android MenuItem");
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
       // It is possible that a custom form has been defined for this table.
       // We will get the strings we need, and then set the parameter object.
       String formId = c.getTableProperties().getStringEntry(
           CollectUtil.KVS_PARTITION, CollectUtil.KVS_ASPECT,
           CollectUtil.KEY_FORM_ID);
       String formVersion = c.getTableProperties().getStringEntry(
           CollectUtil.KVS_PARTITION, CollectUtil.KVS_ASPECT,
           CollectUtil.KEY_FORM_VERSION);
       String rootElement = c.getTableProperties().getStringEntry(
           CollectUtil.KVS_PARTITION, CollectUtil.KVS_ASPECT,
           CollectUtil.KEY_FORM_ROOT_ELEMENT);
       CollectFormParameters params = 
//           new CollectFormParameters(formId, formVersion, rootElement);
           CollectUtil.CollectFormParameters
             .constructCollectFormParameters(c.getTableProperties());
       c.editRow(table, (lastDataCellMenued / table.getWidth()), params);
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
       Log.e(TAG, "unrecognized menu item selected: " + item.getItemId());
         return false;
     }
	}
	

    
	/**
	 * NB: To avoid headache and wishing for death, this is the method to handle
	 * clicks stemming from the View, or from android.view.MenuItem items. The
	 * one to handle ActionBarSherlock methods is elsewhere.
	 */
//    @Override
//    public boolean onMenuItemSelected(int featureId, 
//        android.view.MenuItem item) {
//      Log.d(TAG, "entered android's onMenuItemSelected");
////        if (c.handleMenuItemSelection(item)) {
////          Log.d(TAG, "item was already handled");
////            return true;
////        }
//      Log.d(TAG, "item instance of sherlock: " + 
//          (item instanceof com.actionbarsherlock.view.MenuItem));
//        switch (item.getItemId()) {
//        case MENU_ITEM_ID_HISTORY_IN:
//            openCollectionView(lastDataCellMenued / table.getWidth());
//            return true;
//        case MENU_ITEM_ID_EDIT_CELL:
//            c.openCellEditDialog(
//                    table.getRowId(lastDataCellMenued / table.getWidth()),
//                    table.getData(lastDataCellMenued),
//                    lastDataCellMenued % table.getWidth());
//            return true;
//        case MENU_ITEM_ID_DELETE_ROW:
//            c.deleteRow(table.getRowId(lastDataCellMenued / table.getWidth()));
//            init();
//            return true;
//        case MENU_ITEM_ID_EDIT_ROW:
//    		c.editRow(table, (lastDataCellMenued / table.getWidth()));
//        	// launch ODK Collect
//        	return true;
//        case MENU_ITEM_ID_SET_COLUMN_AS_PRIME:
//            setColumnAsPrime(c.getTableProperties()
//                    .getColumns()[lastHeaderCellMenued]);
//            init();
//            return true;
//        case MENU_ITEM_ID_UNSET_COLUMN_AS_PRIME:
//            unsetColumnAsPrime(c.getTableProperties()
//                    .getColumns()[lastHeaderCellMenued]);
//            init();
//            return true;
//        case MENU_ITEM_ID_SET_COLUMN_AS_SORT:
//            setColumnAsSort(c.getTableProperties()
//                    .getColumns()[lastHeaderCellMenued]);
//            init();
//            return true;
//        case MENU_ITEM_ID_UNSET_COLUMN_AS_SORT:
//            setColumnAsSort(null);
//            init();
//            return true;
//        case MENU_ITEM_ID_SET_AS_INDEXED_COL:
//            setColumnAsIndexedCol(c.getTableProperties()
//                    .getColumns()[lastHeaderCellMenued]);
//            init();
//            return true;
//        case MENU_ITEM_ID_UNSET_AS_INDEXED_COL:
//            setColumnAsIndexedCol(null);
//            init();
//            return true;
//        case MENU_ITEM_ID_OPEN_COL_PROPS_MANAGER:
//            openColumnPropertiesManager(c.getTableProperties()
//                    .getColumns()[lastHeaderCellMenued]);
//            return true;
//        default:
//          Log.e(TAG, "unrecognized menu item selected: " + item.getItemId());
//            return false;
//        }
//    }
    
    /**
     * NB: This is the onMenuItemSelected for the action bar. NOT for context
     * menus.
     */
    @Override
    public boolean onMenuItemSelected(int featureId, 
        com.actionbarsherlock.view.MenuItem item) {
      Log.d(TAG, "entered actionbarsherlock's onMenuItemSelected");
      return c.handleMenuItemSelection(item);
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
	    private Context context;
	    
	    public CellPopout(int cellId) {
	        super(SpreadsheetDisplayActivity.this);
	        this.cellId = cellId;
//	        Context context = SpreadsheetDisplayActivity.this;
	        context = SpreadsheetDisplayActivity.this;
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
	        // These appear to be the menu items that are generated when you 
	        // long press on a cell. I don't know what the other menus up above
	        // that also include these do, nor when they are generated.
	        itemIds.add(MENU_ITEM_ID_EDIT_CELL);
	        itemLabels.add("Edit Cell");
	        itemIds.add(MENU_ITEM_ID_DELETE_ROW);
	        itemLabels.add("Delete Row");
	        itemIds.add(MENU_ITEM_ID_EDIT_ROW);
	        itemLabels.add("Edit Row");
	        // now we're going to check for the join column, and add it if 
	        // it is applicable.
	        // indexed col is the index of the column that is frozen on the 
	        // left. If it is -1 then it is not indexed.
	        // We want the column properties for the given column. Using the 
	        // same math as is being used by the code below for editing cells.
	        // TODO by declaring this final (which you have to do to use it in
	        // the on click method down there), does it mean that if you have a 
	        // table open and edit the join you will get the wrong information?
	        final ColumnProperties cp = c.getTableProperties().getColumnByIndex(
	            cellId % table.getWidth());
	        // First we want to check if we need to add a join item for this 
	        // column.
	        if (cp.getColumnType() == ColumnType.TABLE_JOIN) {
	          itemIds.add(MENU_ITEM_ID_OPEN_JOIN_TABLE);
	          itemLabels.add(MENU_ITEM_MSG_OPEN_JOIN_TABLE);
	        }
	        AlertDialog.Builder builder = new AlertDialog.Builder(
	                SpreadsheetDisplayActivity.this);
	        builder.setItems(itemLabels.toArray(new String[0]),
	                new DialogInterface.OnClickListener() {
	          /*
	           * It's not clear to me why we're dividing by table.getWidth() for
	           * so many of the things below when we want row number. It seems
	           * like we would want the height in some of these cases...
	           */
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
	                  // It is possible that a custom form has been defined for this table.
	                  // We will get the strings we need, and then set the parameter object.
//	                  String formId = c.getTableProperties().getStringEntry(
//	                      CollectUtil.KEY_FORM_ID);
//	                  String formVersion = c.getTableProperties().getStringEntry(
//	                      CollectUtil.KEY_FORM_VERSION);
//	                  String rootElement = c.getTableProperties().getStringEntry(
//	                      CollectUtil.KEY_FORM_ROOT_ELEMENT);
	                  CollectFormParameters params = 
//	                      new CollectFormParameters(formId, formVersion, rootElement);
	                    CollectUtil.CollectFormParameters
	                      .constructCollectFormParameters(
	                        c.getTableProperties());
	                    c.editRow(table, cellId / table.getWidth(), params);
	                    c.removeOverlay();
	                    init();
	                    break;
	                case MENU_ITEM_ID_OPEN_JOIN_TABLE:
	                  // Get the JoinColumn.
	                  JoinColumn joinColumn = cp.getJoins();
	                  AlertDialog.Builder badJoinDialog;
	                  // TODO should check for valid table properties and 
	                  // column properties here. or rather valid ids and keys.
	                  if (joinColumn == null) {
	                    badJoinDialog = new AlertDialog.Builder(context);
	                    badJoinDialog.setTitle("Bad Join");
	                    badJoinDialog.setMessage("A join column has not been " +
	                    		"set in Column Properties.");
	                    badJoinDialog.create().show();
	                    Log.e(TAG, "cp.getJoins was null but open join table " +
	                    		"was requested for cp: " + 
	                    cp.getElementKey());
	                  } else if (joinColumn.getTableId()
	                      .equals(JoinColumn.DEFAULT_NOT_SET_VALUE) || 
	                      joinColumn.getElementKey()
	                      .equals(JoinColumn.DEFAULT_NOT_SET_VALUE)) {
                       badJoinDialog = new AlertDialog.Builder(context);
                       badJoinDialog.setTitle("Bad Join");
                       badJoinDialog.setMessage("Both a table and column " +
                       		"must be set.");
                       badJoinDialog.create().show();
                       Log.e(TAG, "Bad elementKey or tableId in open join " +
                       		"table. tableId: " + joinColumn.getTableId() + 
                       		" elementKey: " + joinColumn.getElementKey());
                     }
	                  String tableId = joinColumn.getTableId();
	                  String elementKey = joinColumn.getElementKey();
	                  TableProperties joinedTable = 
	                      dm.getTableProperties(tableId,
	                          KeyValueStore.Type.ACTIVE);
	                  String joinedColDisplayName = 
	                      joinedTable.getColumnByDbName(elementKey)
	                      .getDisplayName();
	                  // I would prefer this kind of query to be set in another
	                  // object, but alas, it looks like atm it is hardcoded.
	                  String queryText = joinedColDisplayName + ":" + 
	                      table.getData(cellId);
	                    c.launchTableActivity(context, 
	                        dm.getTableProperties(tableId, 
	                            KeyValueStore.Type.ACTIVE), 
	                        queryText, c.getIsOverview());
	                    c.removeOverlay();
	                  break;
	                default:
	                  Log.e(TAG, "unrecognized menu action: " + 
	                      itemIds.get(which));
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
