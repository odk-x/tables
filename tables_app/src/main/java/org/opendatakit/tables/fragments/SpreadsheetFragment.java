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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.opendatakit.consts.RequestCodeConsts;
import org.opendatakit.data.JoinColumn;
import org.opendatakit.data.utilities.ColumnUtil;
import org.opendatakit.database.data.ColumnDefinition;
import org.opendatakit.database.queries.BindArgs;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ActionNotAuthorizedException;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.provider.DataTableColumns;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.ISpreadsheetFragmentContainer;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.data.ViewFragmentType;
import org.opendatakit.tables.utils.ActivityUtil;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.SQLQueryStruct;
import org.opendatakit.tables.views.CellInfo;
import org.opendatakit.tables.views.SpreadsheetProps;
import org.opendatakit.tables.views.SpreadsheetUserTable;
import org.opendatakit.tables.views.SpreadsheetUserTable.SpreadsheetCell;
import org.opendatakit.tables.views.SpreadsheetView;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Fragment responsible for displaying a spreadsheet view. This class is a hideous monstrosity
 * that was copied over largely from SpreadsheetDisplayActivity in the old code. A major rewrite
 * needs to take place.
 *
 * @author sudar.sam@gmail.com
 */
public class SpreadsheetFragment extends AbsTableDisplayFragment
    implements SpreadsheetView.Controller {

  // Used for logging
  private static final String TAG = SpreadsheetFragment.class.getSimpleName();

  // used in onContextItemSelected
  private static final int MENU_ITEM_ID_OPEN_COLLECTION = 0;
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
  private static final int MENU_ITEM_ID_SORT_ASC = 12;
  private static final int MENU_ITEM_ID_SORT_DESC = 13;
  private static final int MENU_ITEM_ID_PREFS = 14;

  /**
   * The object that contains the actual rows of the table and their data
   */
  private SpreadsheetUserTable spreadsheetTable;

  /**
   * theView is used to store the SpreadsheetView once we have one, but before we can create one
   * (before databaseAvailable) is called, it holds a text view with a database error message.
   */
  private LinearLayout theView;
  /**
   * used to post actions until all the lifecycle events have happened on them.
   */
  private View container;

  /**
   * Called when the view needs to be displayed to the user. Since it might called before the
   * database is up, it just displays an error message that will be replaced when
   * databaseAvailable is called.
   *
   * @param inflater           unused
   * @param container          unused
   * @param savedInstanceState unused
   * @return A view with an error message
   */
  @Override
  public View onCreateView(android.view.LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    theView = new LinearLayout(getActivity());
    TextView textView = new TextView(getActivity());
    textView.setText(getString(R.string.error_accessing_database));
    theView.addView(textView);
    this.container = container;
    return theView;
  }

  /**
   * When the database becomes available, replace the view in theView with a new
   * SpreadsheetUserTable, or an error message if we can't
   * <p>
   * It is a bit of a strange interaction, when the screen is rotated, first TableDisplayActivity
   * gets restored, then this gets restored, then databaseAvailable is called on
   * TableDisplayActivity which promptly detaches and destroys this fragment, then databaseAvailable
   * gets called on the destroyed fragment and then again on the new fragment. So this method
   * will be called twice, once on a recently-destroyed fragment and again on a good and attached
   * fragment. getActivity() will return null on the detached fragment, so we have to check that
   * or we'll get a null pointer exception and crash tables
   * <p>
   * Also note that the database is always up on first creation, because first creation of this
   * fragment only happens in two cases, creation on the first creation of TableDisplayActivity
   * which needs to get the default view type (spreadsheet vs list vs map) from the database, and
   * again when this fragment is destroyed and a new one created in TDA's databaseAvailable.
   * <p>
   * However in all cases, this method is called
   */
  @Override
  public void databaseAvailable() {
    WebLogger.getLogger(getAppName()).i(TAG, "SpreadsheetFragment databaseAvailable called");
    try {

      spreadsheetTable = new SpreadsheetUserTable(this);
      if (!spreadsheetTable.hasData()) {
        TextView textView = new TextView(getActivity());
        textView.setText(getString(R.string.no_data));
        theView.removeAllViews();
        theView.addView(textView);
      } else {
        theView.removeAllViews();
        final SpreadsheetView theSpreadsheetView = buildSpreadsheetView();
        theView.addView(theSpreadsheetView);
        final SpreadsheetProps props = getProps();
        container.post(new Runnable() {
          @Override
          public void run() {
            if (getActivity() == null) {
              WebLogger.getLogger(mAppName).i(TAG, "activity was null in post, this fragment "
                  + "was probably destroyed and recreated via showCurrentDisplayFragment before "
                  + "the rotation completed.");
            } else {
              if (props.headerMenuOpen) {
                theSpreadsheetView.openHeaderMenu();
              } else if (props.dataMenuOpen) {
                theSpreadsheetView.openDataMenu();
              } else if (props.deleteDialogOpen) {
                openDeleteDialog();
              }
            }
          }
        });
      }
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(getAppName()).printStackTrace(e);
      WebLogger.getLogger(getAppName()).e(TAG, "Error while constructing spreadsheet view: " + e);
      TextView textView = new TextView(getActivity());
      textView.setText(getString(R.string.error_accessing_database));
      theView.removeAllViews();
      theView.addView(textView);
    }
  }

  /**
   * Does nothing when the database goes away
   */
  @Override
  public void databaseUnavailable() {
  }

  /**
   * Build a {@link SpreadsheetView} view to display.
   *
   * @return a new spreadsheet view with the correct activity, table, etc..
   * @throws ServicesAvailabilityException if the database is down
   */
  SpreadsheetView buildSpreadsheetView() throws ServicesAvailabilityException {
    return new SpreadsheetView(this.getActivity(), this, spreadsheetTable);
  }

  /**
   * Adds a column to group the rows in the table by
   *
   * @param column which column to group by
   */
  void addGroupByColumn(String column) {
    String[] groupBy = getProps().getGroupBy();
    String[] newArr = new String[groupBy.length + 1];
    System.arraycopy(groupBy, 0, newArr, 0, groupBy.length);
    newArr[groupBy.length] = column;
    getProps().setGroupBy(newArr);
  }

  /**
   * Removes a column from the list of columns to group by
   *
   * @param column the column to remove
   */
  void removeGroupByColumn(String column) {
    // Need to wrap because Arrays.asList returns an immutable List object and asList.remove will
    // throw an UnsupportedOperationException
    ArrayList<String> asList = new ArrayList<>(Arrays.asList(getProps().getGroupBy()));
    if (asList.contains(column)) {
      asList.remove(column);
    }
    getProps().setGroupBy(asList.toArray(new String[asList.size()]));
    if (getActivity().getIntent().getExtras().containsKey("inCollection")) {
      getActivity().finish();
    }
  }

  /**
   * Returns the current order in which we are sorting, either ASC or DESC
   *
   * @return what direction the table is sorted
   */
  private String getSortOrder() {
    String sortOrder = getProps().getSortOrder();
    if (sortOrder == null || sortOrder.isEmpty()) {
      return "ASC";
    }
    return sortOrder;
  }

  /**
   * Displays a collection of elements based on what the table is currently grouped by
   *
   * @param cell the cell that the user had to double tap on to get the menu open to call this
   *             method
   */
  private void openCollectionView(SpreadsheetCell cell) {
    Bundle intentExtras = this.getActivity().getIntent().getExtras();

    // Start with the query from the intent.
    SQLQueryStruct sqlQueryStruct = IntentUtil.getSQLQueryStructFromBundle(intentExtras);
    String[] sqlGroupBy = getProps().getGroupBy();
    // Construct a new where clause
    // We don't need to set sqlQueryStruct.groupBy to an empty array because
    // TableDisplayActivity::getUserTable does that on the other end.
    StringBuilder s = new StringBuilder();
    if (sqlQueryStruct.whereClause != null && !sqlQueryStruct.whereClause.isEmpty()) {
      s.append("(").append(sqlQueryStruct.whereClause).append(") AND ");
    }
    ArrayList<Object> newSelectionArgs = new ArrayList<>();
    if (sqlQueryStruct.selectionArgs.bindArgs != null) {
      newSelectionArgs.addAll(Arrays.asList(sqlQueryStruct.selectionArgs.bindArgs));
    }
    boolean first = true;
    for (String groupByColumn : sqlGroupBy) {
      if (!first) {
        s.append(", ");
      }
      first = false;
      String value = cell.row.getStringValueByKey(groupByColumn);
      if (value == null) {
        s.append(groupByColumn).append(" IS NULL");
      } else {
        s.append(groupByColumn).append("=?");
        newSelectionArgs.add(value);
      }
    }
    sqlQueryStruct.whereClause = s.toString();
    sqlQueryStruct.selectionArgs = new BindArgs(
        newSelectionArgs.toArray(new Object[newSelectionArgs.size()]));

    Activity act = getActivity();
    if (!(act instanceof ISpreadsheetFragmentContainer)) {
      throw new IllegalStateException("Cannot view a collection in a spreadsheet using something "
          + "that can't view spreadsheets");
    }
    Intent intent = new Intent(this.getActivity(), act.getClass());
    Bundle extras = new Bundle();
    IntentUtil.addAppNameToBundle(extras, this.getAppName());
    IntentUtil.addTableIdToBundle(extras, this.getTableId());
    IntentUtil.addSQLQueryStructToBundle(extras, sqlQueryStruct);
    IntentUtil.addFragmentViewTypeToBundle(extras, ViewFragmentType.SPREADSHEET);
    extras.putParcelable("props", getProps());
    // This tells it to ignore any groupBy and just use an empty array
    extras.putString("inCollection", "");
    // This tells it to get props from the intent
    extras.putString(Constants.IntentKeys.CONTAINS_PROPS, "");
    intent.putExtras(extras);
    getActivity().startActivityForResult(intent, RequestCodeConsts.RequestCodes.LAUNCH_VIEW);
  }

  /**
   * Initializes and refreshes the activity
   */
  private void destroyAndRecreateFragment() {
    Activity act = getActivity();
    if (act instanceof TableDisplayActivity) {
      ((TableDisplayActivity) act).refreshDataAndDisplayFragment();
    }
  }

  /**
   * Deletes a row from the table
   *
   * @param rowId the id of the row to delete
   * @throws ServicesAvailabilityException if the database is down
   * @throws ActionNotAuthorizedException  if the user doesn't have permission to delete the row
   */
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

  /**
   * Gets the spreadsheet properties from the activity.
   *
   * @return the properties object that's saved across the fragment being destroyed and a new one
   * being created
   */
  private SpreadsheetProps getProps() {
    Activity act = getActivity();
    if (act instanceof ISpreadsheetFragmentContainer) {
      return ((ISpreadsheetFragmentContainer) act).getProps();
    }
    throw new IllegalStateException("Must be inside something with props");
  }

  /**
   * When the user clicks an item, handle that action based on what they clicked
   *
   * @param item the item that the user selected
   * @return whether we were able to handle the action
   */
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    getProps().headerMenuOpen = false;
    getProps().dataMenuOpen = false;
    UserDbInterface dbInterface = Tables.getInstance().getDatabase();

    // TEMP code to try and fix the crash on return-edit
    if (spreadsheetTable == null) {
      if (dbInterface == null)
        return false;
      //databaseAvailable();
      //if (spreadsheetTable == null)
      //return false;
    }
    // end temp
    SpreadsheetCell cell;
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();

    switch (item.getItemId()) {
    // When the user long taps or double taps on a cell, and they have edit permission, and the
    // table has group buys, then this option is displayed in the drop down menu. It opens a
    // collection
    case MENU_ITEM_ID_OPEN_COLLECTION:
      cell = spreadsheetTable.getSpreadsheetCell(getProps().lastDataCellMenued);
      openCollectionView(cell);
      return true;
    // This is in the Row Actions menu that pops up when you double click or long tap on a cell
    // if you have the permissions to open the menu
    case MENU_ITEM_ID_DELETE_ROW:
      openDeleteDialog();
      return true;
    // This is in the same Row Actions menu as delete row
    case MENU_ITEM_ID_EDIT_ROW:
      cell = spreadsheetTable.getSpreadsheetCell(getProps().lastDataCellMenued);
      // It is possible that a custom form has been defined for this table.
      // We will get the strings we need, and then set the parameter object.
      try {
        ActivityUtil.editRow(activity, activity.getAppName(), activity.getTableId(), cell.row);
      } catch (ServicesAvailabilityException e) {
        WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
        WebLogger.getLogger(activity.getAppName()).e(TAG, "Error while accessing database");
        Toast.makeText(activity, "Error while accessing database", Toast.LENGTH_LONG).show();
        return true;
      }
      // launch ODK Collect
      return true;
    // Also in the row actions menu, but only if applicable
    case MENU_ITEM_ID_OPEN_JOIN_TABLE:
      cell = spreadsheetTable.getSpreadsheetCell(getProps().lastDataCellMenued);
      ColumnDefinition cd = spreadsheetTable.getColumnByElementKey(cell.elementKey);
      // Get the JoinColumn.
      ArrayList<JoinColumn> joinColumns;
      DbHandle db = null;
      try {
        db = dbInterface.openDatabase(getAppName());
        joinColumns = ColumnUtil.get()
            .getJoins(dbInterface, getAppName(), db, getTableId(), cd.getElementKey());

        AlertDialog.Builder badJoinDialog;
        // TODO should check for valid table properties and column properties here. or rather valid
        // ids and keys.
        if (joinColumns == null || joinColumns.isEmpty()) {
          badJoinDialog = new AlertDialog.Builder(this.getActivity());
          badJoinDialog.setTitle("Bad Join");
          badJoinDialog.setMessage("A join column has not been set in Column Properties.");
          badJoinDialog.create().show();
          WebLogger.getLogger(getAppName()).e(TAG,
              "cp.getJoins was null but open join table was " + "requested for cp: " + cd
                  .getElementKey());
        } else if (joinColumns.size() != 1) {
          badJoinDialog = new AlertDialog.Builder(this.getActivity());
          badJoinDialog.setTitle("Bad Join");
          badJoinDialog
              .setMessage("Multiple join associations have been set in Column Properties.");
          badJoinDialog.create().show();
          WebLogger.getLogger(getAppName()).e(TAG,
              "cp.getJoins has multiple joins (missing code is needed to handle this) for cp: " + cd
                  .getElementKey());
        } else {
          JoinColumn joinColumn = joinColumns.get(0);
          if (joinColumn.getTableId().equals(JoinColumn.DEFAULT_NOT_SET_VALUE) || joinColumn
              .getElementKey().equals(JoinColumn.DEFAULT_NOT_SET_VALUE)) {
            badJoinDialog = new AlertDialog.Builder(this.getActivity());
            badJoinDialog.setTitle("Bad Join");
            badJoinDialog.setMessage("Both a table and column must be set.");
            badJoinDialog.create().show();
            WebLogger.getLogger(getAppName()).e(TAG,
                "Bad elementKey or tableId in open join table. tableId: " + joinColumn.getTableId()
                    + " elementKey: " + joinColumn.getElementKey());
          } else {
            db = null;
            try {
              db = dbInterface.openDatabase(getAppName());
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
            IntentUtil.addFragmentViewTypeToBundle(extras, ViewFragmentType.SPREADSHEET);
            // TODO: Pass a query!
            IntentUtil.addTableIdToBundle(extras, getTableId());
            extras.putParcelable("props", getProps());
            // Do not pass inCollection because that will set groupBy to null in TableDispAct
            extras.putString(Constants.IntentKeys.CONTAINS_PROPS, "");
            intent.putExtras(extras);
            getActivity().startActivityForResult(intent, RequestCodeConsts.RequestCodes.LAUNCH_VIEW);
          }
        }
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
      return true;
    // In the context menu when you double click on a column heading.
    case MENU_ITEM_ID_SET_COLUMN_AS_GROUP_BY:
      addGroupByColumn(getProps().lastHeaderCellMenued.elementKey);
      destroyAndRecreateFragment();
      return true;
    // In the same context menu you get from double tapping on a column heading
    case MENU_ITEM_ID_UNSET_COLUMN_AS_GROUP_BY:
      removeGroupByColumn(getProps().lastHeaderCellMenued.elementKey);
      destroyAndRecreateFragment();
      return true;
    // In the same context menu you get from double tapping on a column heading
    case MENU_ITEM_ID_SET_COLUMN_AS_SORT:
      getProps().setSort(getProps().lastHeaderCellMenued.elementKey);
      destroyAndRecreateFragment();
      return true;
    // In the same context menu
    case MENU_ITEM_ID_UNSET_COLUMN_AS_SORT:
      getProps().setSort(null);
      destroyAndRecreateFragment();
      return true;
    case MENU_ITEM_ID_SET_AS_INDEXED_COL:
      getProps().setFrozen(getProps().lastHeaderCellMenued.elementKey);
      destroyAndRecreateFragment();
      return true;
    case MENU_ITEM_ID_UNSET_AS_INDEXED_COL:
      getProps().setFrozen(null);
      destroyAndRecreateFragment();
      return true;
    // In the same context menu you get from double tapping on a column heading
    case MENU_ITEM_ID_EDIT_COLUMN_COLOR_RULES:
      String elementKey = getProps().lastHeaderCellMenued.elementKey;
      ActivityUtil
          .launchTablePreferenceActivityToEditColumnColorRules(this.getActivity(), getAppName(),
              getTableId(), elementKey);
      destroyAndRecreateFragment();
      return true;
    case MENU_ITEM_ID_SORT_ASC:
      getProps().setSortOrder("ASC");
      destroyAndRecreateFragment();
      return true;
    case MENU_ITEM_ID_SORT_DESC:
      getProps().setSortOrder("DESC");
      destroyAndRecreateFragment();
      return true;
    case MENU_ITEM_ID_PREFS:
      ActivityUtil
          .launchTablePreferenceActivityToEditColumn(this.getActivity(), getAppName(), getTableId(),
              getProps().lastHeaderCellMenued.elementKey);
      destroyAndRecreateFragment();
      return true;
    default:
      WebLogger.getLogger(getAppName())
          .e(TAG, "unrecognized menu item selected: " + item.getItemId());
      return super.onContextItemSelected(item);
    }

  }

  /**
   * Return true if group bys are currently being displayed.
   *
   * @return Whether group bys are displayed
   */
  private boolean hasGroupBys() {
    return getProps().getGroupBy() != null && getProps().getGroupBy().length != 0;
  }

  /**
   * Called when someone double taps or long clicks on a data cell. Adds view collection option
   * to the menu if we have group bys, adds edit or delete row options to the menu if the user
   * has permission to do that, and adds a join option if applicable
   *
   * @param menu     A context menu that will be displayed, to be populated with options.
   * @param cellInfo Info about the cell that was clicked on
   * @throws ServicesAvailabilityException if the database is down
   */
  @Override
  public void prepDataCellOccm(ContextMenu menu, CellInfo cellInfo)
      throws ServicesAvailabilityException {
    // Mark the fact that we have a data menu open so if the user rotates the screen, we can open
    // it again.
    getProps().dataMenuOpen = true;
    if (cellInfo == null) {
      // the screen was rotated after the user opened the menu
      cellInfo = getProps().lastDataCellMenued;
    } else {
      getProps().lastDataCellMenued = cellInfo;
    }

    // Set the title to rowActions
    menu.setHeaderTitle(getString(R.string.row_actions));

    // If we have group buys, give the user the "View collection" option
    if (this.hasGroupBys() && !getActivity().getIntent().getExtras().containsKey("inCollection")) {
      menu.add(ContextMenu.NONE, MENU_ITEM_ID_OPEN_COLLECTION, ContextMenu.NONE,
          R.string.view_collection);
    }

    String access = spreadsheetTable.getRowAtIndex(cellInfo.rowId)
        .getStringValueByKey(DataTableColumns.EFFECTIVE_ACCESS);
    if (access == null)
      access = "";

    if (access.contains("d")) {
      menu.add(ContextMenu.NONE, MENU_ITEM_ID_DELETE_ROW, ContextMenu.NONE,
          getString(R.string.delete_row));
    }
    if (access.contains("w")) {
      menu.add(ContextMenu.NONE, MENU_ITEM_ID_EDIT_ROW, ContextMenu.NONE,
          getString(R.string.edit_row));
    }

    // check a join association with this column; add a join... option if
    // it is applicable.
    ArrayList<JoinColumn> joinColumns;
    UserDbInterface dbInterface = Tables.getInstance().getDatabase();
    DbHandle db = null;
    try {
      db = dbInterface.openDatabase(getAppName());
      joinColumns = ColumnUtil.get()
          .getJoins(dbInterface, getAppName(), db, getTableId(), cellInfo.elementKey);
    } finally {
      if (db != null) {
        dbInterface.closeDatabase(getAppName(), db);
      }
    }

    if (joinColumns != null && !joinColumns.isEmpty()) {
      menu.add(ContextMenu.NONE, MENU_ITEM_ID_OPEN_JOIN_TABLE, ContextMenu.NONE,
          getString(R.string.open_join_table));
    }
  }

  /**
   * Opens the row actions context menu. Called when the user double clicks or long clicks on a
   * cell in SpreadsheetView
   *
   * @param view A view to put the context menu in
   */
  @Override
  public void openContextMenu(View view) {
    getActivity().openContextMenu(view);
  }

  /**
   * Called when the user double clicks or long clicks on a blue header cell in a spreadsheet
   * It includes the group by/ungroup by, freeze/unfreeze column, edit column color rules, edit
   * column preferences and change sort direction options
   *
   * @param menu     A context menu to populate with items
   * @param cellInfo Some info about the cell they clicked
   * @throws ServicesAvailabilityException if the database is down
   */
  @Override
  public void prepHeaderCellOccm(ContextMenu menu, CellInfo cellInfo)
      throws ServicesAvailabilityException {
    // Mark the menu as being open so when the user rotates the screen or changes something and
    // this fragment gets destroyed, we can re-open the menu
    getProps().headerMenuOpen = true;
    if (cellInfo == null) {
      // the screen was rotated after the user opened the menu
      cellInfo = getProps().lastHeaderCellMenued;
    } else {
      getProps().lastHeaderCellMenued = cellInfo;
    }

    // set the title
    menu.setHeaderTitle(getString(R.string.column_actions));

    // Do not let the user change group by settings if we're viewing a collection, it breaks things
    boolean isSort = cellInfo.elementKey.equals(getProps().getSort());
    boolean isGroup = Arrays.asList(getProps().getGroupBy()).contains(cellInfo.elementKey);
    boolean isCollection = getActivity().getIntent().getExtras().containsKey("inCollection");
    boolean isFrozen = cellInfo.elementKey.equals(getProps().getFrozen());
    if (isGroup) {
      menu.add(ContextMenu.NONE, MENU_ITEM_ID_UNSET_COLUMN_AS_GROUP_BY, ContextMenu.NONE,
          getString(R.string.unset_as_group_by));
    } else if (isSort) {
      menu.add(ContextMenu.NONE, MENU_ITEM_ID_UNSET_COLUMN_AS_SORT, ContextMenu.NONE,
          getString(R.string.unset_as_sort));
      if ("ASC".equals(getSortOrder())) {
        menu.add(ContextMenu.NONE, MENU_ITEM_ID_SORT_DESC, ContextMenu.NONE,
            getString(R.string.sort_desc));
      } else {
        menu.add(ContextMenu.NONE, MENU_ITEM_ID_SORT_ASC, ContextMenu.NONE,
            getString(R.string.sort_asc));
      }
    } else {
      if (!isCollection) {
        menu.add(ContextMenu.NONE, MENU_ITEM_ID_SET_COLUMN_AS_GROUP_BY, ContextMenu.NONE,
            getString(R.string.set_as_group_by));
      }
      menu.add(ContextMenu.NONE, MENU_ITEM_ID_SET_COLUMN_AS_SORT, ContextMenu.NONE,
          getString(R.string.set_as_sort));
    }
    if (isFrozen) {
      menu.add(ContextMenu.NONE, MENU_ITEM_ID_UNSET_AS_INDEXED_COL, ContextMenu.NONE,
          getString(R.string.unfreeze_column));
    } else {
      menu.add(ContextMenu.NONE, MENU_ITEM_ID_SET_AS_INDEXED_COL, ContextMenu.NONE,
          getString(R.string.freeze_column));
    }

    menu.add(ContextMenu.NONE, MENU_ITEM_ID_PREFS, ContextMenu.NONE,
        getString(R.string.column_prefs));

    menu.add(ContextMenu.NONE, MENU_ITEM_ID_EDIT_COLUMN_COLOR_RULES, ContextMenu.NONE,
        getString(R.string.edit_column_color_rules));

  }

  /**
   * Do nothing when a data cell is clicked
   *
   * @param cellInfo unused
   */
  @Override
  public void dataCellClicked(CellInfo cellInfo) {
  }

  /**
   * Do nothing when a header cell is clicked
   *
   * @param cellInfo unused
   */
  @Override
  public void headerCellClicked(CellInfo cellInfo) {
  }

  /**
   * Called when the user clicks the delete option, or the fragment is created with
   * deleteDialogOpen already set to true (they clicked delete then rotated the screen)
   */
  private void openDeleteDialog() {
    // Make sure it will re-open if we rotate the screen
    getProps().deleteDialogOpen = true;
    SpreadsheetCell cell = spreadsheetTable.getSpreadsheetCell(getProps().lastDataCellMenued);
    AlertDialog confirmDeleteAlert;
    // Prompt an alert box
    final String rowId = cell.row.getStringValueByKey(DataTableColumns.ID);
    AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
    alert.setTitle(getString(R.string.confirm_delete_row))
        .setMessage(getString(R.string.are_you_sure_delete_row, rowId));
    // OK Action => delete the row
    alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        AbsBaseActivity activity = (AbsBaseActivity) getActivity();
        try {
          getProps().deleteDialogOpen = false;
          deleteRow(rowId);
          destroyAndRecreateFragment();
        } catch (ActionNotAuthorizedException e) {
          WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
          WebLogger.getLogger(activity.getAppName())
              .e(TAG, "Not authorized for action while accessing database");
          Toast.makeText(activity, getString(R.string.no_permissions), Toast.LENGTH_LONG).show();
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
        getProps().deleteDialogOpen = false;
      }
    });
    // Set deleteDialogOpen to false when the user dismisses the dialog by tapping outside of it
    // or pressing a button, so that we won't re-create it if they then rotate the screen.
    // Unfortunately our minimum target is sdk 16, so if their OS version doesn't support onDismiss
    // listeners, try not to let them dismiss it and instead make them click the cancel button to
    // dismiss it
    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog) {
          try {
            // If they already clicked ok, this fragment will have been detached and getActivity
            // will return null. That's why we set deleteDialogOpen to false in the positive
            // onClick listener as well
            if (getActivity() != null) {
              getProps().deleteDialogOpen = false;
            }
          } catch (IllegalArgumentException ignored) {
          }
        }
      });
    } else {
      alert.setCancelable(false);
      // they can still cancel it with the back button, in which case we're screwed
    }
    // show the dialog
    confirmDeleteAlert = alert.create();
    confirmDeleteAlert.show();
  }

}
