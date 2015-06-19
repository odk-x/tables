/*
 * Copyright (C) 2014 University of Washington
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
package org.opendatakit.tables.fragments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.JoinColumn;
import org.opendatakit.common.android.data.Row;
import org.opendatakit.common.android.utilities.ColumnUtil;
import org.opendatakit.common.android.utilities.DataUtil;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsTableActivity;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.ActivityUtil;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.Constants.IntentKeys;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.ParseUtil;
import org.opendatakit.tables.utils.SQLQueryStruct;
import org.opendatakit.tables.utils.TableUtil;
import org.opendatakit.tables.views.CellInfo;
import org.opendatakit.tables.views.CellValueView;
import org.opendatakit.tables.views.SpreadsheetUserTable;
import org.opendatakit.tables.views.SpreadsheetUserTable.SpreadsheetCell;
import org.opendatakit.tables.views.SpreadsheetView;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Fragment responsible for displaying a spreadsheet view. This class is a
 * hideous monstrosity that was copied over largely from
 * SpreadsheetDisplayActivity in the old code. A major rewrite needs to take
 * place.
 *
 * @author sudar.sam@gmail.com
 *
 */
public class SpreadsheetFragment extends AbsTableDisplayFragment implements
    SpreadsheetView.Controller {

  private static final String TAG = SpreadsheetFragment.class.getSimpleName();

  private static final int MENU_ITEM_ID_HISTORY_IN = 0;
  private static final int MENU_ITEM_ID_EDIT_CELL = 1;
  private static final int MENU_ITEM_ID_DELETE_ROW = 2;
  private static final int MENU_ITEM_ID_SET_COLUMN_AS_GROUP_BY = 3;
  private static final int MENU_ITEM_ID_UNSET_COLUMN_AS_GROUP_BY = 4;
  private static final int MENU_ITEM_ID_SET_COLUMN_AS_SORT = 5;
  private static final int MENU_ITEM_ID_UNSET_COLUMN_AS_SORT = 6;
  private static final int MENU_ITEM_ID_SET_AS_INDEXED_COL = 7;
  private static final int MENU_ITEM_ID_UNSET_AS_INDEXED_COL = 8;
  private static final int MENU_ITEM_ID_EDIT_ROW = 9;
  // This should allow for the opening of a joined table.
  private static final int MENU_ITEM_ID_OPEN_JOIN_TABLE = 10;
  private static final int MENU_ITEM_ID_EDIT_COLUMN_COLOR_RULES = 11;

  private SpreadsheetUserTable spreadsheetTable;

  private CellInfo mLastDataCellMenued;
  private CellInfo mLastHeaderCellMenued;

  @Override
  public ViewFragmentType getFragmentType() {
    return ViewFragmentType.SPREADSHEET;
  }

  @Override
  public View onCreateView(android.view.LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    try {
      spreadsheetTable = new SpreadsheetUserTable(this);
      if (!spreadsheetTable.hasData()) {
        TextView textView = new TextView(getActivity());
        textView.setText(getString(R.string.no_data));
        return textView;
      } else {
        return this.buildSpreadsheetView();
      }
    } catch (RemoteException e) {
      WebLogger.getLogger(getAppName()).printStackTrace(e);
      WebLogger.getLogger(getAppName()).e(TAG,
          "Error while constructing spreadsheet view: " + e.toString());
      TextView textView = new TextView(getActivity());
      textView.setText(getString(R.string.error_accessing_database));
      return textView;
    }
  }
  
  @Override
  public void databaseAvailable() {
    if ( Tables.getInstance().getDatabase() != null && getView() != null ) {
    }
  }

  @Override
  public void databaseUnavailable() {
  }

  /**
   * Build a {@link SpreadsheetView} view to display.
   *
   * @return
   * @throws RemoteException 
   */
  SpreadsheetView buildSpreadsheetView() throws RemoteException {
    return new SpreadsheetView((TableDisplayActivity) this.getActivity(), this, spreadsheetTable);
  }

  private void addGroupByColumn(ColumnDefinition cd) throws RemoteException {

    ArrayList<String> newGroupBys;
    boolean successful = false;
    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(getAppName(), true);
      newGroupBys = TableUtil.get().getColumnOrder(getAppName(), db, getTableId());
      newGroupBys.add(cd.getElementKey());
      TableUtil.get().setGroupByColumns(getAppName(), db, getTableId(), newGroupBys);
      successful = true;
    } catch (Exception e) {
      WebLogger.getLogger(getAppName()).printStackTrace(e);
      WebLogger.getLogger(getAppName()).e(TAG,
          "Error while changing groupBy columns: " + e.toString());
      Toast.makeText(this.getActivity(), getString(R.string.error_while_changing_group_by_columns),
          Toast.LENGTH_LONG).show();
    } finally {
      if (db != null) {
        Tables.getInstance().getDatabase().closeTransactionAndDatabase(getAppName(), db, successful);
      }
    }
  }

  void removeGroupByColumn(ColumnDefinition cd) throws RemoteException {
    ArrayList<String> newGroupBys;
    boolean successful = false;
    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(getAppName(), true);
      newGroupBys = TableUtil.get().getColumnOrder(getAppName(), db, getTableId());
      newGroupBys.remove(cd.getElementKey());
      TableUtil.get().setGroupByColumns(getAppName(), db, getTableId(), newGroupBys);
      successful = true;
    } catch (Exception e) {
      WebLogger.getLogger(getAppName()).printStackTrace(e);
      WebLogger.getLogger(getAppName()).e(TAG,
          "Error while changing groupBy columns: " + e.toString());
      Toast.makeText(this.getActivity(), getString(R.string.error_while_changing_group_by_columns),
          Toast.LENGTH_LONG).show();
    } finally {
      if (db != null) {
        Tables.getInstance().getDatabase().closeTransactionAndDatabase(getAppName(), db, successful);
      }
    }
  }

  void setColumnAsSort(ColumnDefinition cd) throws RemoteException {
    boolean successful = false;
    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(getAppName(), true);
      TableUtil.get().setSortColumn(getAppName(), db, getTableId(), (cd == null) ? null : cd.getElementKey());
      successful = true;
    } catch (Exception e) {
      WebLogger.getLogger(getAppName()).printStackTrace(e);
      WebLogger.getLogger(getAppName()).e(TAG, "Error while changing sort column: " + e.toString());
      Toast.makeText(this.getActivity(), this.getString(R.string.error_while_changing_sort_column),
          Toast.LENGTH_LONG).show();
    } finally {
      if (db != null) {
        Tables.getInstance().getDatabase().closeTransactionAndDatabase(getAppName(), db, successful);
      }
    }
  }

  void setColumnAsIndexedCol(ColumnDefinition cd) throws RemoteException {
    boolean successful = false;
    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(getAppName(), true);
      TableUtil.get().setIndexColumn(getAppName(), db, getTableId(), (cd == null) ? null : cd.getElementKey());
      successful = true;
    } catch (Exception e) {
      WebLogger.getLogger(getAppName()).printStackTrace(e);
      WebLogger.getLogger(getAppName())
          .e(TAG, "Error while changing index column: " + e.toString());
      Toast.makeText(this.getActivity(),
          this.getString(R.string.error_while_changing_index_column), Toast.LENGTH_LONG).show();
    } finally {
      if (db != null) {
        Tables.getInstance().getDatabase().closeTransactionAndDatabase(getAppName(), db, successful);
      }
    }
  }

  private void openCollectionView(SpreadsheetCell cell) {

    Bundle intentExtras = this.getActivity().getIntent().getExtras();
    String sqlWhereClause = intentExtras.getString(IntentKeys.SQL_WHERE);
    String[] sqlSelectionArgs = null;
    if (sqlWhereClause != null && sqlWhereClause.length() != 0) {
      sqlSelectionArgs = intentExtras.getStringArray(IntentKeys.SQL_SELECTION_ARGS);
    }
    String[] sqlGroupBy = intentExtras.getStringArray(IntentKeys.SQL_GROUP_BY_ARGS);
    String sqlHaving = null;
    if (sqlGroupBy != null && sqlGroupBy.length != 0) {
      sqlHaving = intentExtras.getString(IntentKeys.SQL_HAVING);
    }
    String sqlOrderByElementKey = intentExtras.getString(IntentKeys.SQL_ORDER_BY_ELEMENT_KEY);
    String sqlOrderByDirection = null;
    if (sqlOrderByElementKey != null && sqlOrderByElementKey.length() != 0) {
      sqlOrderByDirection = intentExtras.getString(IntentKeys.SQL_ORDER_BY_DIRECTION);
      if (sqlOrderByDirection == null || sqlOrderByDirection.length() == 0) {
        sqlOrderByDirection = "ASC";
      }
    }

    if (sqlGroupBy != null && sqlGroupBy.length != 0) {
      StringBuilder s = new StringBuilder();
      if (sqlWhereClause != null && sqlWhereClause.length() != 0) {
        s.append("(").append(sqlWhereClause).append(") AND ");
      }
      List<String> newSelectionArgs = new ArrayList<String>();
      if (sqlSelectionArgs != null) {
        newSelectionArgs.addAll(Arrays.asList(sqlSelectionArgs));
      }
      boolean first = true;
      for (String groupByColumn : sqlGroupBy) {
        if (!first) {
          s.append(", ");
        }
        first = false;
        String value = cell.row.getRawDataOrMetadataByElementKey(groupByColumn);
        if (value == null) {
          s.append(groupByColumn).append(" IS NULL");
        } else {
          s.append(groupByColumn).append("=?");
          newSelectionArgs.add(value);
        }
      }
      sqlWhereClause = s.toString();
      sqlSelectionArgs = newSelectionArgs.toArray(new String[newSelectionArgs.size()]);
    }
    Intent intent = new Intent(this.getActivity(), TableDisplayActivity.class);
    Bundle extras = new Bundle();
    IntentUtil.addSQLKeysToBundle(extras, sqlWhereClause, sqlSelectionArgs, sqlGroupBy, sqlHaving,
        sqlOrderByElementKey, sqlOrderByDirection);
    IntentUtil.addFragmentViewTypeToBundle(extras, ViewFragmentType.SPREADSHEET);
    IntentUtil.addAppNameToBundle(extras, this.getAppName());
    intent.putExtras(extras);
    this.startActivityForResult(intent, Constants.RequestCodes.LAUNCH_VIEW);
  }

  void openCellEditDialog(SpreadsheetCell cell) {
    CellEditDialog dialog = new CellEditDialog(cell);
    dialog.show();
  }

  private void init() throws RemoteException {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();
    activity.refreshDataTable();
    activity.refreshDisplayFragment();
  }

  private void deleteRow(String rowId) throws RemoteException {
    boolean successful = false;
    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(getAppName(), true);
      Tables.getInstance().getDatabase().deleteDataInExistingDBTableWithId(getAppName(), db,
          getTableId(), rowId);
      successful = true;
    } finally {
      if (db != null) {
        Tables.getInstance().getDatabase().closeTransactionAndDatabase(getAppName(), db, successful);
      }
    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    SpreadsheetCell cell;
    AbsTableActivity activity = (AbsTableActivity) getActivity();

    switch (item.getItemId()) {
    case MENU_ITEM_ID_HISTORY_IN:
      cell = spreadsheetTable.getSpreadsheetCell(activity, this.mLastDataCellMenued);
      openCollectionView(cell);
      return true;
    case MENU_ITEM_ID_EDIT_CELL:
      cell = spreadsheetTable.getSpreadsheetCell(activity, this.mLastDataCellMenued);
      openCellEditDialog(cell);
      return true;
    case MENU_ITEM_ID_DELETE_ROW:
      cell = spreadsheetTable.getSpreadsheetCell(activity, this.mLastDataCellMenued);
      AlertDialog confirmDeleteAlert;
      // Prompt an alert box
      final String rowId = cell.row.getRowId();
      AlertDialog.Builder alert = new AlertDialog.Builder(activity);
      alert.setTitle(getString(R.string.confirm_delete_row)).setMessage(
          getString(R.string.are_you_sure_delete_row, rowId));
      // OK Action => delete the row
      alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          AbsTableActivity activity = (AbsTableActivity) getActivity();
          try {
            deleteRow(rowId);
            init();
          } catch (RemoteException e) {
            WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
            WebLogger.getLogger(activity.getAppName()).e(TAG, "Error while accessing database");
            Toast.makeText(activity, "Error while accessing database", Toast.LENGTH_LONG).show();
          }
        }
      });

      // Cancel Action
      alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          // Canceled.
        }
      });
      // show the dialog
      confirmDeleteAlert = alert.create();
      confirmDeleteAlert.show();
      return true;
    case MENU_ITEM_ID_EDIT_ROW:
      cell = spreadsheetTable.getSpreadsheetCell(activity, this.mLastDataCellMenued);
      // It is possible that a custom form has been defined for this table.
      // We will get the strings we need, and then set the parameter object.
      try {
        ActivityUtil.editRow(activity, activity.getAppName(), activity.getTableId(),
            activity.getColumnDefinitions(), cell.row);
      } catch (RemoteException e) {
        WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
        WebLogger.getLogger(activity.getAppName()).e(TAG, "Error while accessing database");
        Toast.makeText(activity, "Error while accessing database", Toast.LENGTH_LONG).show();
        return true;
      }
      // launch ODK Collect
      return true;
    case MENU_ITEM_ID_OPEN_JOIN_TABLE:
      cell = spreadsheetTable.getSpreadsheetCell(getActivity(), this.mLastDataCellMenued);
      ColumnDefinition cd = spreadsheetTable.getColumnByElementKey(cell.elementKey);
      // Get the JoinColumn.
      ArrayList<JoinColumn> joinColumns;
      OdkDbHandle db = null;
      try {
        db = Tables.getInstance().getDatabase().openDatabase(getAppName(), false);
        joinColumns = ColumnUtil.get().getJoins(Tables.getInstance(), getAppName(), db, getTableId(), cd.getElementKey());
      } catch (RemoteException e) {
        WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
        WebLogger.getLogger(activity.getAppName()).e(TAG, "Error while accessing database");
        Toast.makeText(activity, "Error while accessing database", Toast.LENGTH_LONG).show();
        return true;
      } finally {
        if (db != null) {
          try {
            Tables.getInstance().getDatabase().closeDatabase(getAppName(), db);
          } catch (RemoteException e) {
            WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
            WebLogger.getLogger(activity.getAppName()).e(TAG, "Error closing database");
            Toast.makeText(activity, "Error closing database", Toast.LENGTH_LONG).show();
          }
        }
      }

      AlertDialog.Builder badJoinDialog;
      // TODO should check for valid table properties and
      // column properties here. or rather valid ids and keys.
      if (joinColumns == null || joinColumns.size() == 0) {
        badJoinDialog = new AlertDialog.Builder(this.getActivity());
        badJoinDialog.setTitle("Bad Join");
        badJoinDialog.setMessage("A join column has not been " + "set in Column Properties.");
        badJoinDialog.create().show();
        WebLogger.getLogger(getAppName()).e(
            TAG,
            "cp.getJoins was null but open join table " + "was requested for cp: "
                + cd.getElementKey());
      } else if (joinColumns.size() != 1) {
        badJoinDialog = new AlertDialog.Builder(this.getActivity());
        badJoinDialog.setTitle("Bad Join");
        badJoinDialog.setMessage("Multiple join associations have been "
            + "set in Column Properties.");
        badJoinDialog.create().show();
        WebLogger.getLogger(getAppName()).e(
            TAG,
            "cp.getJoins has multiple joins " + "(missing code is needed to handle this) for cp: "
                + cd.getElementKey());
      } else {
        JoinColumn joinColumn = joinColumns.get(0);
        if (joinColumn.getTableId().equals(JoinColumn.DEFAULT_NOT_SET_VALUE)
            || joinColumn.getElementKey().equals(JoinColumn.DEFAULT_NOT_SET_VALUE)) {
          badJoinDialog = new AlertDialog.Builder(this.getActivity());
          badJoinDialog.setTitle("Bad Join");
          badJoinDialog.setMessage("Both a table and column " + "must be set.");
          badJoinDialog.create().show();
          WebLogger.getLogger(getAppName()).e(
              TAG,
              "Bad elementKey or tableId in open join " + "table. tableId: "
                  + joinColumn.getTableId() + " elementKey: " + joinColumn.getElementKey());
        } else {
          String tableId = joinColumn.getTableId();
          String elementKey = joinColumn.getElementKey();
          String joinedColTableDisplayName;
          db = null;
          try {
            db = Tables.getInstance().getDatabase().openDatabase(getAppName(), false);
            joinedColTableDisplayName = ColumnUtil.get().getLocalizedDisplayName(Tables.getInstance(), getAppName(), 
                db, tableId,
                elementKey);
          } catch (RemoteException e) {
            WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
            WebLogger.getLogger(activity.getAppName()).e(TAG, "Error while accessing database");
            Toast.makeText(activity, "Error while accessing database", Toast.LENGTH_LONG).show();
          } finally {
            if (db != null) {
              try {
                Tables.getInstance().getDatabase().closeDatabase(getAppName(), db);
              } catch (RemoteException e) {
                WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
                WebLogger.getLogger(activity.getAppName()).e(TAG, "Error closing database");
                Toast.makeText(activity, "Error closing database", Toast.LENGTH_LONG).show();
              }
            }
          }

          // I would prefer this kind of query to be set in another
          // object, but alas, it looks like atm it is hardcoded.
          Intent intent = new Intent(this.getActivity(), TableDisplayActivity.class);
          Bundle extras = new Bundle();
          IntentUtil.addAppNameToBundle(extras, getAppName());
          // TODO: pass the correct view type.
          IntentUtil.addFragmentViewTypeToBundle(extras, ViewFragmentType.SPREADSHEET);
          intent.putExtras(extras);
          getActivity().startActivityForResult(intent, Constants.RequestCodes.LAUNCH_VIEW);
          // Controller.launchTableActivity(context, joinedTable,
          // joinedTable.getDefaultViewType());
        }
      }
      return true;
    case MENU_ITEM_ID_SET_COLUMN_AS_GROUP_BY:
      try {
        addGroupByColumn(spreadsheetTable
            .getColumnByElementKey(this.mLastHeaderCellMenued.elementKey));
        init();
      } catch (RemoteException e) {
        WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
        WebLogger.getLogger(activity.getAppName()).e(TAG, "Error while accessing database");
        Toast.makeText(activity, "Error while accessing database", Toast.LENGTH_LONG).show();
      }
      return true;
    case MENU_ITEM_ID_UNSET_COLUMN_AS_GROUP_BY:
      try {
        removeGroupByColumn(spreadsheetTable
            .getColumnByElementKey(this.mLastHeaderCellMenued.elementKey));
        init();
      } catch (RemoteException e) {
        WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
        WebLogger.getLogger(activity.getAppName()).e(TAG, "Error while accessing database");
        Toast.makeText(activity, "Error while accessing database", Toast.LENGTH_LONG).show();
      }
      return true;
    case MENU_ITEM_ID_SET_COLUMN_AS_SORT:
      try {
        setColumnAsSort(spreadsheetTable.getColumnByElementKey(this.mLastHeaderCellMenued.elementKey));
        init();
      } catch (RemoteException e) {
        WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
        WebLogger.getLogger(activity.getAppName()).e(TAG, "Error while accessing database");
        Toast.makeText(activity, "Error while accessing database", Toast.LENGTH_LONG).show();
      }
      return true;
    case MENU_ITEM_ID_UNSET_COLUMN_AS_SORT:
      try {
        setColumnAsSort(null);
        init();
      } catch (RemoteException e) {
        WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
        WebLogger.getLogger(activity.getAppName()).e(TAG, "Error while accessing database");
        Toast.makeText(activity, "Error while accessing database", Toast.LENGTH_LONG).show();
      }
      return true;
    case MENU_ITEM_ID_SET_AS_INDEXED_COL:
      try {
        setColumnAsIndexedCol(spreadsheetTable
            .getColumnByElementKey(this.mLastHeaderCellMenued.elementKey));
        init();
      } catch (RemoteException e) {
        WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
        WebLogger.getLogger(activity.getAppName()).e(TAG, "Error while accessing database");
        Toast.makeText(activity, "Error while accessing database", Toast.LENGTH_LONG).show();
      }
      return true;
    case MENU_ITEM_ID_UNSET_AS_INDEXED_COL:
      try {
        setColumnAsIndexedCol(null);
        init();
      } catch (RemoteException e) {
        WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
        WebLogger.getLogger(activity.getAppName()).e(TAG, "Error while accessing database");
        Toast.makeText(activity, "Error while accessing database", Toast.LENGTH_LONG).show();
      }
      return true;
    case MENU_ITEM_ID_EDIT_COLUMN_COLOR_RULES:
      String elementKey = this.mLastHeaderCellMenued.elementKey;
      ActivityUtil.launchTablePreferenceActivityToEditColumnColorRules(this.getActivity(),
          getAppName(), getTableId(), elementKey);
    default:
      WebLogger.getLogger(getAppName()).e(TAG,
          "unrecognized menu item selected: " + item.getItemId());
      return super.onContextItemSelected(item);
    }
  }

  /**
   * Return true if group bys are currently being displayed.
   *
   * @return
   */
  private boolean hasGroupBys() {
    SQLQueryStruct queryStruct = IntentUtil.getSQLQueryStructFromBundle(this.getActivity()
        .getIntent().getExtras());
    return queryStruct.groupBy != null;
  }

  @Override
  public void prepDataCellOccm(ContextMenu menu, CellInfo cellInfo) throws RemoteException {
    this.mLastDataCellMenued = cellInfo;
    ColumnDefinition cd = spreadsheetTable.getColumnByElementKey(cellInfo.elementKey);
    String localizedDisplayName;
    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(getAppName(), false);
      localizedDisplayName = ColumnUtil.get().getLocalizedDisplayName(Tables.getInstance(), getAppName(),
          db, getTableId(),
          cd.getElementKey());
    } finally {
      if (db != null) {
        Tables.getInstance().getDatabase().closeDatabase(getAppName(), db);
      }
    }

    menu.setHeaderTitle(localizedDisplayName);

    MenuItem mi;
    Row row = spreadsheetTable.getRowAtIndex(cellInfo.rowId);
    if (this.hasGroupBys()) {
      mi = menu.add(ContextMenu.NONE, MENU_ITEM_ID_HISTORY_IN, ContextMenu.NONE, "View Collection");
      mi.setIcon(R.drawable.view);
    }
    String viewString = row.getDisplayTextOfData(cd.getType(), cellInfo.elementKey);
    // TODO: display value and use edit icon...
    mi = menu.add(ContextMenu.NONE, MENU_ITEM_ID_EDIT_CELL, ContextMenu.NONE,
        getString(R.string.edit_cell, viewString));
    mi.setIcon(R.drawable.ic_action_edit);

    mi = menu.add(ContextMenu.NONE, MENU_ITEM_ID_DELETE_ROW, ContextMenu.NONE,
        getString(R.string.delete_row));
    mi.setIcon(R.drawable.ic_action_discard);
    mi = menu.add(ContextMenu.NONE, MENU_ITEM_ID_EDIT_ROW, ContextMenu.NONE,
        getString(R.string.edit_row));
    mi.setIcon(R.drawable.ic_action_edit);

    // check a join association with this column; add a join... option if
    // it is applicable.
    ArrayList<JoinColumn> joinColumns;
    db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(getAppName(), false);
      joinColumns = ColumnUtil.get().getJoins(Tables.getInstance(), getAppName(), 
          db, getTableId(), cd.getElementKey());
    } finally {
      if (db != null) {
        Tables.getInstance().getDatabase().closeDatabase(getAppName(), db);
      }
    }

    if (joinColumns != null && joinColumns.size() != 0) {
      mi = menu.add(ContextMenu.NONE, MENU_ITEM_ID_OPEN_JOIN_TABLE, ContextMenu.NONE,
          getString(R.string.open_join_table));
      mi.setIcon(R.drawable.ic_action_search);
    }
  }

  @Override
  public void openHeaderContextMenu(View view) {
    this.getActivity().openContextMenu(view);
  }

  @Override
  public void openDataContextMenu(View view) {
    this.getActivity().openContextMenu(view);
  }

  @Override
  public void prepHeaderCellOccm(ContextMenu menu, CellInfo cellInfo) throws RemoteException {
    this.mLastHeaderCellMenued = cellInfo;

    String sortColumn;
    String indexColumn;
    ArrayList<String> groupByColumns;
    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(getAppName(), false);
      sortColumn = TableUtil.get().getSortColumn(getAppName(), db, getTableId());
      indexColumn = TableUtil.get().getIndexColumn(getAppName(), db, getTableId());
      groupByColumns = TableUtil.get().getColumnOrder(getAppName(), db, getTableId());
    } finally {
      if (db != null) {
        Tables.getInstance().getDatabase().closeDatabase(getAppName(), db);
      }
    }

    ColumnDefinition cd = spreadsheetTable.getColumnByElementKey(cellInfo.elementKey);
    if (groupByColumns.contains(cd.getElementKey())) {
      menu.add(ContextMenu.NONE, MENU_ITEM_ID_UNSET_COLUMN_AS_GROUP_BY, ContextMenu.NONE,
          "Unset as Group By");
    } else if ((sortColumn != null) && cellInfo.elementKey.equals(sortColumn)) {
      menu.add(ContextMenu.NONE, MENU_ITEM_ID_UNSET_COLUMN_AS_SORT, ContextMenu.NONE,
          "Unset as Sort");
    } else {
      menu.add(ContextMenu.NONE, MENU_ITEM_ID_SET_COLUMN_AS_GROUP_BY, ContextMenu.NONE,
          "Set as Group By");
      menu.add(ContextMenu.NONE, MENU_ITEM_ID_SET_COLUMN_AS_SORT, ContextMenu.NONE, "Set as Sort");
    }
    if (cellInfo.elementKey.equals(indexColumn)) {
      menu.add(ContextMenu.NONE, MENU_ITEM_ID_UNSET_AS_INDEXED_COL, ContextMenu.NONE,
          "Unfreeze Column");
    } else {
      menu.add(ContextMenu.NONE, MENU_ITEM_ID_SET_AS_INDEXED_COL, ContextMenu.NONE, "Freeze Column");
    }

    menu.add(ContextMenu.NONE, MENU_ITEM_ID_EDIT_COLUMN_COLOR_RULES, ContextMenu.NONE,
        getString(R.string.edit_column_color_rules));

  }

  @Override
  public void dataCellClicked(CellInfo cellInfo) {
    // noop
  }

  @Override
  public void headerCellClicked(CellInfo cellInfo) {
    // noop
  }

  private class CellEditDialog extends AlertDialog {

    private final SpreadsheetCell cell;
    private final CellValueView.CellEditView cev;
    private DataUtil dataUtil;

    public CellEditDialog(SpreadsheetCell cell) {
      super(getActivity());
      this.cell = cell;
      this.dataUtil = new DataUtil(Locale.ENGLISH, TimeZone.getDefault());
      ColumnDefinition cd = spreadsheetTable.getColumnByElementKey(cell.elementKey);
      CellValueView.CellEditView cevTemp = null;
      try {
        cevTemp = CellValueView
            .getCellEditView(Tables.getInstance(), getActivity(), getAppName(), getTableId(), cd, cell.value);
      } catch (RemoteException e) {
        WebLogger.getLogger(getAppName()).printStackTrace(e);
        WebLogger.getLogger(getAppName()).e(TAG,  "Unable to access database");
        return;
      } finally {
        cev = cevTemp;
      }
      this.buildView(getActivity());
    }

    private void buildView(Context context) {
      Button setButton = new Button(context);
      setButton.setText(getActivity().getResources().getString(R.string.set));
      setButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          boolean successful = false;
          OdkDbHandle db = null;
          ArrayList<Map<String, Object>> choices;
          try {
            try {
              db = Tables.getInstance().getDatabase().openDatabase(getAppName(), true);
              choices = (ArrayList<Map<String, Object>>) ColumnUtil.get().getDisplayChoicesList(
                  Tables.getInstance(), getAppName(), db,
                  getTableId(), cell.elementKey);
  
              String value = ParseUtil.validifyValue(getAppName(), dataUtil, choices,
                  spreadsheetTable.getColumnByElementKey(CellEditDialog.this.cell.elementKey),
                  cev.getValue());
              if (value == null) {
                // TODO: alert the user
                return;
              }
    
              ContentValues values = new ContentValues();
              values.put(CellEditDialog.this.cell.elementKey, value);
    
              Tables.getInstance().getDatabase().updateDataInExistingDBTableWithId(getAppName(), db,
                  getTableId(),
                  getColumnDefinitions(), values, cell.row.getRowId());
              successful = true;
            } finally {
              if (db != null) {
                Tables.getInstance().getDatabase().closeTransactionAndDatabase(getAppName(), db, successful);
              }
            }
  
            init();
          } catch (RemoteException e) {
            WebLogger.getLogger(getAppName()).printStackTrace(e);
            WebLogger.getLogger(getAppName()).e(TAG, "Error while accessing database");
            Toast.makeText(CellEditDialog.this.getContext(), "Error while accessing database", Toast.LENGTH_LONG).show();
          }
          dismiss();
        }
      });
      Button cancelButton = new Button(context);
      cancelButton.setText(getActivity().getResources().getString(R.string.cancel));
      cancelButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          dismiss();
        }
      });
      LinearLayout buttonWrapper = new LinearLayout(context);
      buttonWrapper.addView(setButton);
      buttonWrapper.addView(cancelButton);
      LinearLayout wrapper = new LinearLayout(context);
      wrapper.setOrientation(LinearLayout.VERTICAL);
      wrapper.addView(cev);
      wrapper.addView(buttonWrapper);
      setView(wrapper);
    }
  }

}
