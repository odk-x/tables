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

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.opendatakit.data.JoinColumn;
import org.opendatakit.data.utilities.ColumnUtil;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.data.ColumnDefinition;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ActionNotAuthorizedException;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.properties.CommonToolProperties;
import org.opendatakit.properties.PropertiesSingleton;
import org.opendatakit.provider.DataTableColumns;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.*;
import org.opendatakit.tables.utils.Constants.IntentKeys;
import org.opendatakit.tables.views.CellInfo;
import org.opendatakit.tables.views.CellValueView;
import org.opendatakit.tables.views.SpreadsheetUserTable;
import org.opendatakit.tables.views.SpreadsheetUserTable.SpreadsheetCell;
import org.opendatakit.tables.views.SpreadsheetView;
import org.opendatakit.utilities.DateUtils;

import java.util.*;

/**
 * Fragment responsible for displaying a spreadsheet view. This class is a
 * hideous monstrosity that was copied over largely from
 * SpreadsheetDisplayActivity in the old code. A major rewrite needs to take
 * place.
 *
 * @author sudar.sam@gmail.com
 */
public class SpreadsheetFragment extends AbsTableDisplayFragment
    implements SpreadsheetView.Controller {

  private static final String TAG = SpreadsheetFragment.class.getSimpleName();

  private static final int MENU_ITEM_ID_HISTORY_IN = 0;
  //  private static final int MENU_ITEM_ID_EDIT_CELL = 1;
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
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(getAppName()).printStackTrace(e);
      WebLogger.getLogger(getAppName())
          .e(TAG, "Error while constructing spreadsheet view: " + e.toString());
      TextView textView = new TextView(getActivity());
      textView.setText(getString(R.string.error_accessing_database));
      return textView;
    }
  }

  @Override
  public void databaseAvailable() {
    if (Tables.getInstance().getDatabase() != null && getView() != null) {
    }
  }

  @Override
  public void databaseUnavailable() {
  }

  /**
   * Build a {@link SpreadsheetView} view to display.
   *
   * @return
   * @throws ServicesAvailabilityException
   */
  SpreadsheetView buildSpreadsheetView() throws ServicesAvailabilityException {
    return new SpreadsheetView(this.getActivity(), this, spreadsheetTable);
  }

  void addGroupByColumn(ColumnDefinition cd) {
    try {
      TableUtil.get()
          .atomicAddGroupByColumn(Tables.getInstance().getDatabase(), getAppName(), getTableId(),
              cd.getElementKey());
    } catch (ServicesAvailabilityException e) {
      Toast.makeText(getActivity(), "Unable to add column to Group By list", Toast.LENGTH_LONG)
          .show();
    }
  }

  void removeGroupByColumn(ColumnDefinition cd) {
    try {
      TableUtil.get()
          .atomicRemoveGroupByColumn(Tables.getInstance().getDatabase(), getAppName(), getTableId(),
              cd.getElementKey());
    } catch (ServicesAvailabilityException e) {
      Toast.makeText(getActivity(), "Unable to remove column from Group By list", Toast.LENGTH_LONG)
          .show();
    }
  }

  void setColumnAsSort(ColumnDefinition cd) {
    try {
      TableUtil.get()
          .atomicSetSortColumn(Tables.getInstance().getDatabase(), getAppName(), getTableId(),
              (cd == null) ? null : cd.getElementKey());
    } catch (ServicesAvailabilityException e) {
      Toast.makeText(getActivity(), "Unable to set Sort Column", Toast.LENGTH_LONG).show();
    }
  }

  void setColumnAsIndexedCol(ColumnDefinition cd) {
    try {
      TableUtil.get()
          .atomicSetIndexColumn(Tables.getInstance().getDatabase(), getAppName(), getTableId(),
              (cd == null) ? null : cd.getElementKey());
    } catch (ServicesAvailabilityException e) {
      Toast.makeText(getActivity(), "Unable to set Index Column", Toast.LENGTH_LONG).show();
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
        String value = cell.row.getDataByKey(groupByColumn);
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

  private void init() {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();
    activity.refreshDataAndDisplayFragment();
  }

  private void deleteRow(String rowId)
      throws ServicesAvailabilityException, ActionNotAuthorizedException {
    DbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(getAppName());
      Tables.getInstance().getDatabase()
          .deleteRowWithId(getAppName(), db, getTableId(), getColumnDefinitions(), rowId);
    } finally {
      if (db != null) {
        Tables.getInstance().getDatabase().closeDatabase(getAppName(), db);
      }
    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    PropertiesSingleton props = CommonToolProperties.get(Tables.getInstance(), getAppName());
    String userSelectedDefaultLocale = props.getUserSelectedDefaultLocale();

    UserDbInterface dbInterface = Tables.getInstance().getDatabase();

    SpreadsheetCell cell;
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();

    switch (item.getItemId()) {
    case MENU_ITEM_ID_HISTORY_IN:
      cell = spreadsheetTable.getSpreadsheetCell(activity, this.mLastDataCellMenued);
      openCollectionView(cell);
      return true;
    //    case MENU_ITEM_ID_EDIT_CELL:
    //      cell = spreadsheetTable.getSpreadsheetCell(activity, this.mLastDataCellMenued);
    //      openCellEditDialog(cell);
    //      return true;
    case MENU_ITEM_ID_DELETE_ROW:
      cell = spreadsheetTable.getSpreadsheetCell(activity, this.mLastDataCellMenued);
      AlertDialog confirmDeleteAlert;
      // Prompt an alert box
      final String rowId = cell.row.getDataByKey(DataTableColumns.ID);
      AlertDialog.Builder alert = new AlertDialog.Builder(activity);
      alert.setTitle(getString(R.string.confirm_delete_row))
          .setMessage(getString(R.string.are_you_sure_delete_row, rowId));
      // OK Action => delete the row
      alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          TableDisplayActivity activity = (TableDisplayActivity) getActivity();
          try {
            deleteRow(rowId);
            init();
          } catch (ActionNotAuthorizedException e) {
            WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
            WebLogger.getLogger(activity.getAppName())
                .e(TAG, "Not authorized for action while " + "accessing database");
            Toast.makeText(activity, "Not authorized for action while accessing database",
                Toast.LENGTH_LONG).show();
          } catch (ServicesAvailabilityException e) {
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
      } catch (ServicesAvailabilityException e) {
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
      DbHandle db = null;
      try {
        db = dbInterface.openDatabase(getAppName());
        joinColumns = ColumnUtil.get()
            .getJoins(dbInterface, getAppName(), db, getTableId(), cd.getElementKey());
      } catch (ServicesAvailabilityException e) {
        WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
        WebLogger.getLogger(activity.getAppName()).e(TAG, "Error while accessing database");
        Toast.makeText(activity, "Error while accessing database", Toast.LENGTH_LONG).show();
        return true;
      } finally {
        if (db != null) {
          try {
            dbInterface.closeDatabase(getAppName(), db);
          } catch (ServicesAvailabilityException e) {
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
        WebLogger.getLogger(getAppName()).e(TAG,
            "cp.getJoins was null but open join table " + "was requested for cp: " + cd
                .getElementKey());
      } else if (joinColumns.size() != 1) {
        badJoinDialog = new AlertDialog.Builder(this.getActivity());
        badJoinDialog.setTitle("Bad Join");
        badJoinDialog
            .setMessage("Multiple join associations have been " + "set in Column Properties.");
        badJoinDialog.create().show();
        WebLogger.getLogger(getAppName()).e(TAG,
            "cp.getJoins has multiple joins " + "(missing code is needed to handle this) for cp: "
                + cd.getElementKey());
      } else {
        JoinColumn joinColumn = joinColumns.get(0);
        if (joinColumn.getTableId().equals(JoinColumn.DEFAULT_NOT_SET_VALUE) || joinColumn
            .getElementKey().equals(JoinColumn.DEFAULT_NOT_SET_VALUE)) {
          badJoinDialog = new AlertDialog.Builder(this.getActivity());
          badJoinDialog.setTitle("Bad Join");
          badJoinDialog.setMessage("Both a table and column " + "must be set.");
          badJoinDialog.create().show();
          WebLogger.getLogger(getAppName()).e(TAG,
              "Bad elementKey or tableId in open join " + "table. tableId: " + joinColumn
                  .getTableId() + " elementKey: " + joinColumn.getElementKey());
        } else {
          String tableId = joinColumn.getTableId();
          String elementKey = joinColumn.getElementKey();
          String joinedColTableDisplayName;
          db = null;
          try {
            db = dbInterface.openDatabase(getAppName());
            joinedColTableDisplayName = ColumnUtil.get()
                .getLocalizedDisplayName(userSelectedDefaultLocale, dbInterface, getAppName(), db,
                    tableId, elementKey);
          } catch (ServicesAvailabilityException e) {
            WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
            WebLogger.getLogger(activity.getAppName()).e(TAG, "Error while accessing database");
            Toast.makeText(activity, "Error while accessing database", Toast.LENGTH_LONG).show();
          } finally {
            if (db != null) {
              try {
                dbInterface.closeDatabase(getAppName(), db);
              } catch (ServicesAvailabilityException e) {
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
      addGroupByColumn(
          spreadsheetTable.getColumnByElementKey(this.mLastHeaderCellMenued.elementKey));
      init();
      return true;
    case MENU_ITEM_ID_UNSET_COLUMN_AS_GROUP_BY:
      removeGroupByColumn(
          spreadsheetTable.getColumnByElementKey(this.mLastHeaderCellMenued.elementKey));
      init();
      return true;
    case MENU_ITEM_ID_SET_COLUMN_AS_SORT:
      setColumnAsSort(
          spreadsheetTable.getColumnByElementKey(this.mLastHeaderCellMenued.elementKey));
      init();
      return true;
    case MENU_ITEM_ID_UNSET_COLUMN_AS_SORT:
      setColumnAsSort(null);
      init();
      return true;
    case MENU_ITEM_ID_SET_AS_INDEXED_COL:
      setColumnAsIndexedCol(
          spreadsheetTable.getColumnByElementKey(this.mLastHeaderCellMenued.elementKey));
      init();
      return true;
    case MENU_ITEM_ID_UNSET_AS_INDEXED_COL:
      setColumnAsIndexedCol(null);
      init();
      return true;
    case MENU_ITEM_ID_EDIT_COLUMN_COLOR_RULES:
      String elementKey = this.mLastHeaderCellMenued.elementKey;
      ActivityUtil
          .launchTablePreferenceActivityToEditColumnColorRules(this.getActivity(), getAppName(),
              getTableId(), elementKey);
    default:
      WebLogger.getLogger(getAppName())
          .e(TAG, "unrecognized menu item selected: " + item.getItemId());
      return super.onContextItemSelected(item);
    }
  }

  /**
   * Return true if group bys are currently being displayed.
   *
   * @return
   */
  private boolean hasGroupBys() {
    SQLQueryStruct queryStruct = IntentUtil
        .getSQLQueryStructFromBundle(this.getActivity().getIntent().getExtras());
    return queryStruct.groupBy != null;
  }

  @Override
  public void prepDataCellOccm(ContextMenu menu, CellInfo cellInfo)
      throws ServicesAvailabilityException {
    PropertiesSingleton props = CommonToolProperties.get(Tables.getInstance(), getAppName());
    String userSelectedDefaultLocale = props.getUserSelectedDefaultLocale();

    this.mLastDataCellMenued = cellInfo;
    ColumnDefinition cd = spreadsheetTable.getColumnByElementKey(cellInfo.elementKey);
    String localizedDisplayName;

    UserDbInterface dbInterface = Tables.getInstance().getDatabase();
    DbHandle db = null;
    try {
      db = dbInterface.openDatabase(getAppName());
      localizedDisplayName = ColumnUtil.get()
          .getLocalizedDisplayName(userSelectedDefaultLocale, dbInterface, getAppName(), db,
              getTableId(), cd.getElementKey());
    } finally {
      if (db != null) {
        dbInterface.closeDatabase(getAppName(), db);
      }
    }

    // menu.setHeaderTitle(localizedDisplayName);
    menu.setHeaderTitle(getString(R.string.row_actions));

    MenuItem mi;
    if (this.hasGroupBys()) {
      mi = menu.add(ContextMenu.NONE, MENU_ITEM_ID_HISTORY_IN, ContextMenu.NONE, "View Collection");
      mi.setIcon(R.drawable.ic_view_headline_black_24dp);
    }
    // TODO: display value and use edit icon...
    //    mi = menu.add(ContextMenu.NONE, MENU_ITEM_ID_EDIT_CELL, ContextMenu.NONE,
    //        getString(R.string.edit_cell, viewString));
    //    mi.setIcon(R.drawable.ic_action_edit);

    String access = spreadsheetTable.getRowAtIndex(cellInfo.rowId)
        .getDataByKey(DataTableColumns.EFFECTIVE_ACCESS);

    if (access.contains("d")) {
      mi = menu.add(ContextMenu.NONE, MENU_ITEM_ID_DELETE_ROW, ContextMenu.NONE,
          getString(R.string.delete_row));
      mi.setIcon(R.drawable.ic_action_content_discard);
    }
    if (access.contains("w")) {
      mi = menu.add(ContextMenu.NONE, MENU_ITEM_ID_EDIT_ROW, ContextMenu.NONE,
          getString(R.string.edit_row));
      mi.setIcon(R.drawable.ic_mode_edit_black_24dp);
    }

    // check a join association with this column; add a join... option if
    // it is applicable.
    ArrayList<JoinColumn> joinColumns;
    db = null;
    try {
      db = dbInterface.openDatabase(getAppName());
      joinColumns = ColumnUtil.get()
          .getJoins(dbInterface, getAppName(), db, getTableId(), cd.getElementKey());
    } finally {
      if (db != null) {
        dbInterface.closeDatabase(getAppName(), db);
      }
    }

    if (joinColumns != null && joinColumns.size() != 0) {
      mi = menu.add(ContextMenu.NONE, MENU_ITEM_ID_OPEN_JOIN_TABLE, ContextMenu.NONE,
          getString(R.string.open_join_table));
      mi.setIcon(R.drawable.ic_search_black_24dp);
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
  public void prepHeaderCellOccm(ContextMenu menu, CellInfo cellInfo)
      throws ServicesAvailabilityException {
    this.mLastHeaderCellMenued = cellInfo;

    String sortColumn;
    String indexColumn;
    ArrayList<String> groupByColumns;
    DbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(getAppName());
      sortColumn = TableUtil.get()
          .getSortColumn(Tables.getInstance().getDatabase(), getAppName(), db, getTableId());
      indexColumn = TableUtil.get()
          .getIndexColumn(Tables.getInstance().getDatabase(), getAppName(), db, getTableId());
      groupByColumns = TableUtil.get()
          .getColumnOrder(Tables.getInstance().getDatabase(), getAppName(), db, getTableId(),
              spreadsheetTable.getColumnDefinitions());
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
      menu.add(ContextMenu.NONE, MENU_ITEM_ID_SET_AS_INDEXED_COL, ContextMenu.NONE,
          "Freeze Column");
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
    private DateUtils dataUtil;

    public CellEditDialog(SpreadsheetCell cell) {
      super(getActivity());
      this.cell = cell;
      this.dataUtil = new DateUtils(Locale.ENGLISH, TimeZone.getDefault());
      ColumnDefinition cd = spreadsheetTable.getColumnByElementKey(cell.elementKey);
      CellValueView.CellEditView cevTemp = null;
      try {
        cevTemp = CellValueView
            .getCellEditView(Tables.getInstance(), getActivity(), getAppName(), getTableId(), cd,
                cell.value);
      } catch (ServicesAvailabilityException e) {
        WebLogger.getLogger(getAppName()).printStackTrace(e);
        WebLogger.getLogger(getAppName()).e(TAG, "Unable to access database");
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
          DbHandle db = null;
          try {
            try {
              db = Tables.getInstance().getDatabase().openDatabase(getAppName());

              String value = ParseUtil.validifyValue(getAppName(), dataUtil,
                  spreadsheetTable.getColumnDisplayChoicesList(CellEditDialog.this.cell.elementKey),
                  spreadsheetTable.getColumnByElementKey(CellEditDialog.this.cell.elementKey),
                  cev.getValue());

              if (value == null) {
                // TODO: alert the user
                return;
              }

              ContentValues values = new ContentValues();
              values.put(CellEditDialog.this.cell.elementKey, value);

              Tables.getInstance().getDatabase()
                  .updateRowWithId(getAppName(), db, getTableId(), getColumnDefinitions(), values,
                      cell.row.getDataByKey(DataTableColumns.ID));
            } finally {
              if (db != null) {
                Tables.getInstance().getDatabase().closeDatabase(getAppName(), db);
              }
            }

            init();
          } catch (ActionNotAuthorizedException e) {
            WebLogger.getLogger(getAppName()).printStackTrace(e);
            WebLogger.getLogger(getAppName())
                .e(TAG, "Action not authorized while accessing " + "database");
            Toast.makeText(CellEditDialog.this.getContext(),
                "Action not authorized while accessing database", Toast.LENGTH_LONG).show();
          } catch (ServicesAvailabilityException e) {
            WebLogger.getLogger(getAppName()).printStackTrace(e);
            WebLogger.getLogger(getAppName()).e(TAG, "Error while accessing database");
            Toast.makeText(CellEditDialog.this.getContext(), "Error while accessing database",
                Toast.LENGTH_LONG).show();
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
