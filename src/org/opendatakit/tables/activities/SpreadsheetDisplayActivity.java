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

import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.common.android.provider.SyncState;
import org.opendatakit.tables.R;
import org.opendatakit.tables.data.ColorRuleGroup;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.ColumnType;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.JoinColumn;
import org.opendatakit.tables.data.KeyValueStoreType;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.tables.views.SpreadsheetView;

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
import com.actionbarsherlock.view.Menu;

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
    private static final int MENU_ITEM_ID_EDIT_COLUMN_COLOR_RULES =
        Controller.FIRST_FREE_MENU_ITEM_ID + 12;
    private static final int MENU_ITEM_ID_RESOLVE_ROW_CONFLICT =
        Controller.FIRST_FREE_MENU_ITEM_ID + 13;
    private static final String MENU_ITEM_MSG_OPEN_JOIN_TABLE =
        "Open Join Table";
    private static final String MENU_ITEM_MSG_EDIT_COLUMN_COLOR_RULES =
        "Edit Column Color Rules";

    private String appName;
    private Controller c;
    private UserTable table;
    private int indexedCol;

    private int lastDataCellMenued;
    private int lastHeaderCellMenued;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appName = getIntent().getStringExtra(Controller.INTENT_KEY_APP_NAME);
        if ( appName == null ) {
          appName = TableFileUtils.getDefaultAppName();
        }

        // remove a title
        setTitle("");
        c = new Controller(this, this, getIntent().getExtras(), savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      c.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
      super.onResume();
      init();
    }

    @Override
    public void init() {
      TableProperties tp = c.getTableProperties();
      Query query = new Query(this, appName, KeyValueStoreType.ACTIVE, tp);
        query.loadFromUserQuery(c.getSearchText());
        // There are two options here. The first is that we get the data using
        // the {@link Query} object. The other is that we use a sql where
        // clause. The two currently don't play nice together, so figure out
        // which one. The sql statement gets precedence.
        String sqlWhereClause =
            getIntent().getExtras().getString(Controller.INTENT_KEY_SQL_WHERE);
        if (sqlWhereClause != null) {
          String[] sqlSelectionArgs = getIntent().getExtras().getStringArray(
              Controller.INTENT_KEY_SQL_SELECTION_ARGS);
          DbTable dbTable = DbTable.getDbTable(c.getTableProperties());
          table = dbTable.rawSqlQuery(sqlWhereClause, sqlSelectionArgs);
        } else {
          // We use the query.
          table = c.getIsOverview() ?
              c.getDbTable().getUserOverviewTable(query) :
              c.getDbTable().getUserTable(query);
        }

        String indexedColElementKey = c.getTableProperties().getIndexColumn();
        indexedCol =
            c.getTableProperties().getColumnIndex(indexedColElementKey);
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
            return new SpreadsheetView(this, this, table, indexedCol);
        }
    }

    private void openCollectionView(int rowNum) {
      Query query = new Query(this, appName, KeyValueStoreType.ACTIVE, table.getTableProperties());
      query.clear();
        query.loadFromUserQuery(c.getSearchText());
        for (String groupByColumn : c.getTableProperties().getGroupByColumns()) {
            ColumnProperties cp = c.getTableProperties()
                    .getColumnByElementKey(groupByColumn);
            int colNum = c.getTableProperties().getColumnIndex(groupByColumn);
            query.addConstraint(cp, table.getData(rowNum, colNum));
        }
        Controller.launchTableActivity(this, c.getTableProperties(),
                query.toUserQuery(), false, null, null, c.getCurrentSearchText());
    }

    void addGroupByColumn(ColumnProperties cp) {
    	List<String> newPrimes = c.getTableProperties().getGroupByColumns();
    	newPrimes.add(cp.getElementKey());
      c.getTableProperties().setGroupByColumns(newPrimes);
    }

    void removeGroupByColumn(ColumnProperties cp) {
        List<String> newPrimes = c.getTableProperties().getGroupByColumns();
        newPrimes.remove(cp.getElementKey());
        c.getTableProperties().setGroupByColumns(newPrimes);
    }

    void setColumnAsSort(ColumnProperties cp) {
        c.getTableProperties().setSortColumn(
                (cp == null) ? null : cp.getElementKey());
    }

    void setColumnAsIndexedCol(ColumnProperties cp) {
      c.getTableProperties().setIndexColumn(
          (cp == null) ? null : cp.getElementKey());
    }

    void openColumnPropertiesManager(ColumnProperties cp) {
        Intent intent = new Intent(this, PropertyManager.class);
        intent.putExtra(Controller.INTENT_KEY_APP_NAME,
            c.getTableProperties().getAppName());
        intent.putExtra(PropertyManager.INTENT_KEY_TABLE_ID,
                c.getTableProperties().getTableId());
        intent.putExtra(PropertyManager.INTENT_KEY_ELEMENT_KEY,
                cp.getElementKey());
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
        // TODO:
//        MenuItem displayPref = menu.getItem(Controller.MENU_ITEM_ID_DISPLAY_PREFERENCES);
//        if (displayPref != null)
//        	displayPref.setEnabled(true);
        return true;
    }

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
	  return true;
	}

	/**
	 * It's unclear to me when this is getting used. I don't think it should be
	 * offering to handle the things like delete row, which should only be
	 * accessible through the cell popout menu.
	 */
	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
     TableProperties tp = c.getTableProperties();

     switch (item.getItemId()) {
     case MENU_ITEM_ID_HISTORY_IN:
         openCollectionView(lastDataCellMenued / table.getWidth());
         return true;
     case MENU_ITEM_ID_EDIT_CELL:
         c.openCellEditDialog(
                 table.getRowAtIndex(
                     lastDataCellMenued / table.getWidth()).getRowId(),
                 table.getData(lastDataCellMenued),
                 lastDataCellMenued % table.getWidth());
         return true;
     case MENU_ITEM_ID_DELETE_ROW:
         c.deleteRow(table.getRowAtIndex(
             lastDataCellMenued / table.getWidth()).getRowId());
         init();
         return true;
     case MENU_ITEM_ID_EDIT_ROW:
       // It is possible that a custom form has been defined for this table.
       // We will get the strings we need, and then set the parameter object.
       c.editRow(table, (lastDataCellMenued / table.getWidth()));
       // launch ODK Collect
       return true;
     case MENU_ITEM_ID_SET_COLUMN_AS_PRIME:
         addGroupByColumn(tp.getColumnByIndex(lastHeaderCellMenued));
         init();
         return true;
     case MENU_ITEM_ID_UNSET_COLUMN_AS_PRIME:
         removeGroupByColumn(tp.getColumnByIndex(lastHeaderCellMenued));
         init();
         return true;
     case MENU_ITEM_ID_SET_COLUMN_AS_SORT:
         setColumnAsSort(tp.getColumnByIndex(lastHeaderCellMenued));
         init();
         return true;
     case MENU_ITEM_ID_UNSET_COLUMN_AS_SORT:
         setColumnAsSort(null);
         init();
         return true;
     case MENU_ITEM_ID_SET_AS_INDEXED_COL:
         setColumnAsIndexedCol(tp.getColumnByIndex(lastHeaderCellMenued));
         init();
         return true;
     case MENU_ITEM_ID_UNSET_AS_INDEXED_COL:
         setColumnAsIndexedCol(null);
         init();
         return true;
     case MENU_ITEM_ID_OPEN_COL_PROPS_MANAGER:
         openColumnPropertiesManager(tp.getColumnByIndex(lastHeaderCellMenued));
         return true;
     case MENU_ITEM_ID_EDIT_COLUMN_COLOR_RULES:
       Intent i = new Intent(this, ColorRuleManagerActivity.class);
       i.putExtra(
           Controller.INTENT_KEY_APP_NAME, table.getTableProperties().getAppName());
       i.putExtra(ColorRuleManagerActivity.INTENT_KEY_ELEMENT_KEY,
           tp.getColumnByIndex(lastHeaderCellMenued).getElementKey());
       i.putExtra(ColorRuleManagerActivity.INTENT_KEY_TABLE_ID,
           table.getTableProperties().getTableId());
       i.putExtra(ColorRuleManagerActivity.INTENT_KEY_RULE_GROUP_TYPE,
           ColorRuleGroup.Type.COLUMN.name());
       startActivity(i);
     default:
       Log.e(TAG, "unrecognized menu item selected: " + item.getItemId());
         return false;
     }
	}

    /**
     * NB: This is the onMenuItemSelected for the action bar. NOT for context
     * menus.
     */
    @Override
    public boolean onMenuItemSelected(int featureId,
        com.actionbarsherlock.view.MenuItem item) {
      return c.handleMenuItemSelection(item);
    }

    @Override
    public void regularCellLongClicked(int cellId, int rawX, int rawY,
        boolean isIndexed) {
      // So we need to check for whether or not the table is indexed again and
      // alter the cellId appropriately.
      if (isIndexed) {
        int colNum = cellId % (table.getWidth() - 1);
        int rowNum = cellId / (table.getWidth() - 1);
        cellId = cellId + rowNum + ((colNum < indexedCol) ? 0 : 1);
      }
        c.addOverlay(new CellPopout(cellId), 100, 100, rawX, rawY);
    }

    @Override
    public void indexedColCellLongClicked(int cellId, int rawX, int rawY) {
      // here it's just the row plus the number of the indexed column.
      // So the true cell id is the cellId parameter, which is essentially the
      // row number, * the width of the table, plus the indexed col
      int trueNum = cellId * table.getWidth() + indexedCol;
      c.addOverlay(new CellPopout(trueNum), 100, 100, rawX, rawY);
    }

    @Override
    public void regularCellDoubleClicked(int cellId, boolean isIndexed,
        int rawX, int rawY) {
      // So it seems like the cellId is coming from the mainData table, which
      // does NOT include the index. So to get the right row here we actually
      // have to perform a little extra.
      if (!isIndexed) {
        c.addOverlay(new CellPopout(cellId), 100, 100, rawX, rawY);
      } else { // it's indexed
        int colNum = cellId % (table.getWidth() - 1);
        int rowNum = cellId / (table.getWidth() - 1);
        int trueNum = cellId + rowNum + ((colNum < indexedCol) ? 0 : 1);
        // trying to hack together correct thing for overlay
        int trueCellId = rowNum * table.getWidth() +
            colNum + ((colNum < indexedCol) ? 0 : 1);
        c.addOverlay(new CellPopout(trueCellId), 100, 100, rawX, rawY);
      }

    }

    @Override
    public void prepRegularCellOccm(ContextMenu menu, int cellId) {
        lastDataCellMenued = cellId;
        if (c.getIsOverview() && c.getTableProperties().hasGroupByColumns()) {
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
        ColumnProperties cp =
            c.getTableProperties().getColumnByIndex(lastHeaderCellMenued);
        if (c.getTableProperties().isGroupByColumn(cp.getElementKey())) {
            menu.add(ContextMenu.NONE, MENU_ITEM_ID_UNSET_COLUMN_AS_PRIME,
                    ContextMenu.NONE, "Unset as Prime");
        } else if ((c.getTableProperties().getSortColumn() != null) &&
                c.getTableProperties().getSortColumn()
                        .equals(cp.getElementKey())) {
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
                    ContextMenu.NONE, "Unfreeze Column");
        } else {
            menu.add(ContextMenu.NONE, MENU_ITEM_ID_SET_AS_INDEXED_COL,
                    ContextMenu.NONE, "Freeze Column");
        }

        menu.add(ContextMenu.NONE, MENU_ITEM_ID_EDIT_COLUMN_COLOR_RULES,
            ContextMenu.NONE, MENU_ITEM_MSG_EDIT_COLUMN_COLOR_RULES);

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
	public void indexedColCellClicked(int cellId) {
		c.removeOverlay();
		// TODO Auto-generated method stub

	}

	@Override
	public void indexedColCellDoubleClicked(int cellId, int rawX, int rawY) {
     // Ok, so here the cellId is also the row number, as we only allow a
     // single indexed column atm. So if you double click the 5th cell, it will
     // also have to be the 5th row.
     int trueNum = cellId * table.getWidth() + indexedCol;
     c.addOverlay(new CellPopout(trueNum), 100, 100, rawX, rawY);
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
	        context = SpreadsheetDisplayActivity.this;
	        TextView valueView = new TextView(context);
	        valueView.setText(table.getDisplayTextOfData(SpreadsheetDisplayActivity.this, cellId));
	        valueView.setTextColor(Color.parseColor("#000000"));
	        Button menuButton = new Button(context);
	        menuButton.setText("Menu");
	        menuButton.setTextColor(Color.parseColor("#000000"));
	        menuButton.setOnClickListener(new View.OnClickListener() {
	            @Override
	            public void onClick(View v) {
	                openCellMenu();
	            }
	        });

	        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
	        lp.setMargins(5, 5, 5, 5);
	        setLayoutParams(lp);
	        setBackgroundResource(R.drawable.my_border);
	        addView(valueView);
	        addView(menuButton);
	        lastDownX = 0;
	        lastDownY = 0;
	    }

	    private void openCellMenu() {
	        final List<Integer> itemIds = new ArrayList<Integer>();
	        List<String> itemLabels = new ArrayList<String>();
	        if (c.getIsOverview() && c.getTableProperties().hasGroupByColumns()) {
	            itemIds.add(MENU_ITEM_ID_HISTORY_IN);
	            itemLabels.add("View Collection");
	        }
	        // These appear to be the menu items that are generated when you
	        // long press on a cell. I don't know what the other menus up above
	        // that also include these do, nor when they are generated.
	        itemIds.add(MENU_ITEM_ID_EDIT_CELL);
	        itemLabels.add(context.getString(R.string.edit_cell));
	        itemIds.add(MENU_ITEM_ID_DELETE_ROW);
	        itemLabels.add(context.getString(R.string.delete_row));
	        itemIds.add(MENU_ITEM_ID_EDIT_ROW);
	        itemLabels.add(context.getString(R.string.edit_row));
	        // Now we need to check to see if we are a row in conflict, in which
	        // case we want to allow resolution of that row.
	        final int rowNumber = cellId / table.getWidth();
	        String syncStateName = table.getMetadataByElementKey(rowNumber,
                                     DataTableColumns.SYNC_STATE);
	        if ( syncStateName != null && syncStateName.length() != 0 &&
	            SyncState.valueOf(syncStateName) == SyncState.conflicting ) {
	          // Then huzzah, we need to add an option to resolve.
	          itemIds.add(MENU_ITEM_ID_RESOLVE_ROW_CONFLICT);
	          itemLabels.add(context.getString(R.string.resolve_conflict));
	        }
	        // now we're going to check for the join column, and add it if
	        // it is applicable.
	        // indexed col is the index of the column that is frozen on the
	        // left. If it is -1 then it is not indexed.
	        // We want the column properties for the given column. Using the
	        // same math as is being used by the code below for editing cells.
	        // TODO by declaring this final (which you have to do to use it in
	        // the on click method down there), does it mean that if you have a
	        // table open and edit the join you will get the wrong information?
	        TableProperties tp = c.getTableProperties();
	        final ColumnProperties
	            cp = tp.getColumnByIndex(cellId % table.getWidth());
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
	                            table.getRowAtIndex(
	                                cellId / table.getWidth()).getRowId(),
	                            table.getData(cellId),
	                            cellId % table.getWidth());
	                    c.removeOverlay();
	                    break;
	                case MENU_ITEM_ID_RESOLVE_ROW_CONFLICT:
	                  // We'll just launch the resolve activity.
	                  Intent i = new Intent(context,
	                      ConflictResolutionRowActivity.class);
                     i.putExtra(Controller.INTENT_KEY_APP_NAME,
                         table.getTableProperties().getAppName());
	                  i.putExtra(Controller.INTENT_KEY_TABLE_ID,
	                      table.getTableProperties().getTableId());
	                  String conflictRowId =
	                      table.getRowAtIndex(rowNumber).getRowId();
	                  i.putExtra(
	                      ConflictResolutionRowActivity.INTENT_KEY_ROW_ID,
	                      conflictRowId);
	                  context.startActivity(i);
	                  break;
	                case MENU_ITEM_ID_DELETE_ROW:
	                  AlertDialog confirmDeleteAlert;
	                  // Prompt an alert box
	                  final String rowId =
	                      table.getRowAtIndex(cellId / table.getWidth()).getRowId();
	                  AlertDialog.Builder alert =
	                      new AlertDialog.Builder(SpreadsheetDisplayActivity.this);
	                  alert.setTitle(getString(R.string.confirm_delete_row))
	                  .setMessage(getString(R.string.are_you_sure_delete_row, rowId));
	                  // OK Action => delete the row
	                  alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
	                     public void onClick(DialogInterface dialog, int whichButton) {
	                       c.deleteRow(rowId);
	                       c.removeOverlay();
	                       init();
	                     }
	                  });

	                  // Cancel Action
	                  alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
	                     public void onClick(DialogInterface dialog, int whichButton) {
	                      // Canceled.
	                       c.removeOverlay();
	                     }
	                  });
	                  // show the dialog
	                  confirmDeleteAlert = alert.create();
	                  confirmDeleteAlert.show();
	                    break;
	                case MENU_ITEM_ID_EDIT_ROW:
	                  // It is possible that a custom form has been defined for this table.
	                  // We will get the strings we need, and then set the parameter object.
	                    c.editRow(table, cellId / table.getWidth());
	                    c.removeOverlay();
	                    init();
	                    break;
	                case MENU_ITEM_ID_OPEN_JOIN_TABLE:
	                  // Get the JoinColumn.
	                  ArrayList<JoinColumn> joinColumns = cp.getJoins();
	                  AlertDialog.Builder badJoinDialog;
	                  // TODO should check for valid table properties and
	                  // column properties here. or rather valid ids and keys.
	                  if (joinColumns == null || joinColumns.size() == 0) {
	                    badJoinDialog = new AlertDialog.Builder(context);
	                    badJoinDialog.setTitle("Bad Join");
	                    badJoinDialog.setMessage("A join column has not been " +
	                    		"set in Column Properties.");
	                    badJoinDialog.create().show();
	                    Log.e(TAG, "cp.getJoins was null but open join table " +
	                    		"was requested for cp: " +
	                    cp.getElementKey());
	                  } else if (joinColumns.size() != 1) {
	                       badJoinDialog = new AlertDialog.Builder(context);
	                       badJoinDialog.setTitle("Bad Join");
	                       badJoinDialog.setMessage("Multiple join associations have been " +
	                           "set in Column Properties.");
	                       badJoinDialog.create().show();
	                       Log.e(TAG, "cp.getJoins has multiple joins " +
	                       		"(missing code is needed to handle this) for cp: " +
	                       		cp.getElementKey());
	                  } else {
	                    JoinColumn joinColumn = joinColumns.get(0);
	                    if (joinColumn.getTableId()
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
                       } else {
      	                  String tableId = joinColumn.getTableId();
      	                  String elementKey = joinColumn.getElementKey();
      	                  TableProperties joinedTable =
                               TableProperties.getTablePropertiesForTable(
                            		   SpreadsheetDisplayActivity.this, appName, tableId,
                                   KeyValueStoreType.ACTIVE);
      	                  String joinedColDisplayName =
      	                      joinedTable.getColumnByElementKey(elementKey)
      	                      .getDisplayName();
      	                  // I would prefer this kind of query to be set in another
      	                  // object, but alas, it looks like atm it is hardcoded.
      	                  String queryText = "_id:" +
      	                      table.getData(cellId);
      	                    Controller.launchTableActivity(context, joinedTable,
      	                        queryText, c.getIsOverview(), null, null, c.getCurrentSearchText());
      	                    c.removeOverlay();
                       }
                     }
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
	            if (c.isInSearchBox(x, y)) {
	              c.invertSearchBoxColor(true);
	            } else {
	              c.invertSearchBoxColor(false);
	            }
	            return true;
	        } else if (event.getAction() == MotionEvent.ACTION_UP) {
	            int x = (Float.valueOf(event.getRawX())).intValue();
	            int y = (Float.valueOf(event.getRawY())).intValue();
	            if (c.isInSearchBox(x, y)) {
	              int columnIndex = cellId % table.getWidth();
	              TableProperties tp = c.getTableProperties();
	              ColumnProperties cp = tp.getColumnByIndex(columnIndex);
	                String colName = cp.getDisplayName();
	                String value = table.getData(cellId);
	                c.appendToSearchBoxText(" " + colName + ":" + value);
	                c.invertSearchBoxColor(false);
	                c.removeOverlay();
	            }
	            return true;
	        } else {
	            return false;
	        }
	    }
	}

    private class DragCell extends LinearLayout {

	    private final int cellId;
	    private int lastDownX;
	    private int lastDownY;

	    public DragCell(int cellId) {
	        super(SpreadsheetDisplayActivity.this);
	        this.cellId = cellId;
	        Context context = SpreadsheetDisplayActivity.this;
	        TextView valueView = new TextView(context);
	        valueView.setText(table.getData(cellId));

	        setBackgroundColor(Color.TRANSPARENT);
	        addView(valueView);
	        lastDownX = 0;
	        lastDownY = 0;
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
	              int colIndex = cellId % table.getWidth();
	              TableProperties tp = c.getTableProperties();
	              ColumnProperties cp = tp.getColumnByIndex(colIndex);
	                String colName = cp.getDisplayName();
	                String value = table.getData(cellId);
	                c.appendToSearchBoxText(" " + colName + ":" + value);
	                c.removeOverlay();
	            } else {
	            	c.removeOverlay();
	            }
	            return true;
	        } else {
	            return false;
	        }
	    }
	}
}
