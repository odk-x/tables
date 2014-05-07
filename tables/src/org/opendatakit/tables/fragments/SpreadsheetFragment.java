package org.opendatakit.tables.fragments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.opendatakit.aggregate.odktables.rest.SyncState;
import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.data.ColumnProperties;
import org.opendatakit.common.android.data.ColumnType;
import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.common.android.data.JoinColumn;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.common.android.data.UserTable.Row;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.common.android.utils.DataUtil;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.ColorRuleManagerActivity;
import org.opendatakit.tables.activities.ConflictResolutionRowActivity;
import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.utils.ActivityUtil;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.Constants.IntentKeys;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.SQLQueryStruct;
import org.opendatakit.tables.views.CellValueView;
import org.opendatakit.tables.views.SpreadsheetView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Fragment responsible for displaying a spreadsheet view. This class is a
 * hideous monstrosity that was copied over largely from
 * SpreadsheetDisplayActivity in the old code. A major rewrite needs to
 * take place.
 * @author sudar.sam@gmail.com
 *
 */
public class SpreadsheetFragment extends AbsTableDisplayFragment
    implements SpreadsheetView.Controller {

  private static final String TAG = SpreadsheetFragment.class.getSimpleName();

  private String mIndexedColumnElementKey;
  private int mIndexedColumnOffset;
  private int mLastDataCellMenued;
  private int mLastHeaderCellMenued;
  /**
   * From Controller.
   */
  private View mOverlay;
  private RelativeLayout.LayoutParams mOverlayLayoutParams;

  @Override
  public ViewFragmentType getFragmentType() {
    return ViewFragmentType.SPREADSHEET;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

  }

  /**
   * Retrieve the element key of the indexed column.
   * @return
   * TODO: remove?
   */
  String retrieveIndexedColumnElementKey() {
    return this.getTableProperties().getIndexColumn();
  }

  /**
   * Get the integer offset of the indexed column.
   * @return
   * TODO: remove?
   */
  int retrieveIndexedColumnOffset() {
    return this.getUserTable().getColumnIndexOfElementKey(this.mIndexedColumnElementKey);
  }

  @Override
  public View onCreateView(
      android.view.LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    this.mIndexedColumnElementKey = retrieveIndexedColumnElementKey();
    this.mIndexedColumnOffset = retrieveIndexedColumnOffset();
    return buildView();
  };

  View buildView() {
    if (this.getUserTable().getWidth() == 0) {
      TextView textView = new TextView(getActivity());
      textView.setText(getString(R.string.no_data));
      return textView;
    } else {
      return this.buildSpreadsheetView();
    }
  }

  /**
   * Build a {@link SpreadsheetView} view to display.
   * @return
   */
  SpreadsheetView buildSpreadsheetView() {
    return new SpreadsheetView(
        this.getActivity(),
        SpreadsheetFragment.this,
        this.getUserTable(),
        this.mIndexedColumnOffset);
  }

  private void addGroupByColumn(ColumnProperties cp) {
    TableProperties tp = this.getTableProperties();
    List<String> newGroupBys = tp.getGroupByColumns();
    newGroupBys.add(cp.getElementKey());
    SQLiteDatabase db = tp.getWritableDatabase();
    try {
      db.beginTransaction();
      tp.setGroupByColumns(db, newGroupBys);
      db.setTransactionSuccessful();
    } catch ( Exception e ) {
      e.printStackTrace();
      Log.e(TAG, "Error while changing groupBy columns: " + e.toString());
      Toast.makeText(
          this.getActivity(),
          getString(R.string.error_while_changing_group_by_columns),
          Toast.LENGTH_LONG).show();
    } finally {
      db.endTransaction();
      db.close();
    }
  }

  void removeGroupByColumn(ColumnProperties cp) {
    TableProperties tp = this.getTableProperties();
    List<String> newGroupBys = tp.getGroupByColumns();
    newGroupBys.remove(cp.getElementKey());
    SQLiteDatabase db = tp.getWritableDatabase();
    try {
      db.beginTransaction();
      tp.setGroupByColumns(db, newGroupBys);
      db.setTransactionSuccessful();
    } catch ( Exception e ) {
      e.printStackTrace();
      Log.e(TAG, "Error while changing groupBy columns: " + e.toString());
      Toast.makeText(
          this.getActivity(),
          getString(R.string.error_while_changing_group_by_columns),
          Toast.LENGTH_LONG).show();
    } finally {
      db.endTransaction();
      db.close();
    }
  }

  void setColumnAsSort(ColumnProperties cp) {
    TableProperties tp = this.getTableProperties();
    SQLiteDatabase db = tp.getWritableDatabase();
    try {
      db.beginTransaction();
      tp.setSortColumn(db, (cp == null) ? null : cp.getElementKey());
      db.setTransactionSuccessful();
    } catch ( Exception e ) {
      e.printStackTrace();
      Log.e(TAG, "Error while changing sort column: " + e.toString());
      Toast.makeText(
          this.getActivity(),
          this.getString(R.string.error_while_changing_sort_column),
          Toast.LENGTH_LONG).show();
    } finally {
      db.endTransaction();
      db.close();
    }
  }

  void setColumnAsIndexedCol(ColumnProperties cp) {
    TableProperties tp = this.getTableProperties();
    SQLiteDatabase db = tp.getWritableDatabase();
    try {
      db.beginTransaction();
      tp.setIndexColumn(db, (cp == null) ? null : cp.getElementKey());
      db.setTransactionSuccessful();
    } catch ( Exception e ) {
      e.printStackTrace();
      Log.e(TAG, "Error while changing index column: " + e.toString());
      Toast.makeText(
          this.getActivity(),
          this.getString(R.string.error_while_changing_index_column),
          Toast.LENGTH_LONG).show();
    } finally {
      db.endTransaction();
      db.close();
    }
  }

  private void openCollectionView(int rowNum) {

    Bundle intentExtras = this.getActivity().getIntent().getExtras();
    String sqlWhereClause =
        intentExtras.getString(IntentKeys.SQL_WHERE);
    String[] sqlSelectionArgs = null;
    if (sqlWhereClause != null && sqlWhereClause.length() != 0) {
       sqlSelectionArgs = intentExtras.getStringArray(
          IntentKeys.SQL_SELECTION_ARGS);
    }
    String[] sqlGroupBy = intentExtras.getStringArray(IntentKeys.SQL_GROUP_BY_ARGS);
    String sqlHaving = null;
    if ( sqlGroupBy != null && sqlGroupBy.length != 0 ) {
      sqlHaving = intentExtras.getString(IntentKeys.SQL_HAVING);
    }
    String sqlOrderByElementKey = intentExtras.getString(IntentKeys.SQL_ORDER_BY_ELEMENT_KEY);
    String sqlOrderByDirection = null;
    if ( sqlOrderByElementKey != null && sqlOrderByElementKey.length() != 0 ) {
      sqlOrderByDirection = intentExtras.getString(IntentKeys.SQL_ORDER_BY_DIRECTION);
      if ( sqlOrderByDirection == null || sqlOrderByDirection.length() == 0 ) {
        sqlOrderByDirection = "ASC";
      }
    }

    if ( sqlGroupBy != null && sqlGroupBy.length != 0 ) {
      StringBuilder s = new StringBuilder();
      if ( sqlWhereClause != null && sqlWhereClause.length() != 0) {
        s.append("(").append(sqlWhereClause).append(") AND ");
      }
      List<String> newSelectionArgs = new ArrayList<String>();
      if ( sqlSelectionArgs != null ) {
        newSelectionArgs.addAll(Arrays.asList(sqlSelectionArgs));
      }
      boolean first = true;
      Row row = this.getUserTable().getRowAtIndex(rowNum);
      for ( String groupByColumn : sqlGroupBy ) {
        if ( !first ) {
          s.append(", ");
        }
        first = false;
        s.append(groupByColumn).append("=?");
        newSelectionArgs.add(row.getDataOrMetadataByElementKey(groupByColumn));
      }
      sqlWhereClause = s.toString();
      sqlSelectionArgs =
          newSelectionArgs.toArray(new String[newSelectionArgs.size()]);
    }
    Controller.launchTableActivity(
        this.getActivity(),
        this.getTableProperties(),
        TableViewType.SPREADSHEET,
        sqlWhereClause,
        sqlSelectionArgs,
        null,
        null,
        sqlOrderByElementKey,
        sqlOrderByDirection);
  }

  void openCellEditDialog(String rowId, String value, int columnIndex) {
    CellEditDialog dialog = new CellEditDialog(rowId, value, columnIndex);
    dialog.show();
  }

  private void init() {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();
    activity.refreshDataTable();
  }

  void addOverlay(View overlay, int width, int height, int x, int y) {
    removeOverlay();
    this.mOverlay = overlay;
    this.mOverlayLayoutParams = new RelativeLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    this.mOverlayLayoutParams.leftMargin = x;
    this.mOverlayLayoutParams.topMargin =
        y -
        getActivity().getActionBar().getHeight();
    this.mOverlayLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
    ViewGroup container = (ViewGroup) this.getView();
    container.addView(this.mOverlay, this.mOverlayLayoutParams);
  }

  void removeOverlay() {
    if (this.mOverlay != null) {
      ViewGroup container = (ViewGroup) this.getView();
      container.removeView(this.mOverlay);
      this.mOverlay = null;
      this.mOverlayLayoutParams = null;
    }
  }

  private void deleteRow(String rowId) {
    DbTable dbTable = DbTable.getDbTable(getTableProperties());
    dbTable.markDeleted(rowId);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    // This was copied over wholesale from SpreadhseetDisplayActivity.
    // TODO: clean up.
    TableProperties tp = this.getTableProperties();
    switch (item.getItemId()) {
    case MENU_ITEM_ID_HISTORY_IN:
        openCollectionView(
            this.mLastDataCellMenued / this.getUserTable().getWidth());
        return true;
    case MENU_ITEM_ID_EDIT_CELL:
        openCellEditDialog(
                this.getUserTable().getRowAtIndex(
                    this.mLastDataCellMenued /
                    this.getUserTable().getWidth()).getRowId(),
                this.getUserTable().getData(this.mLastDataCellMenued),
                this.mLastDataCellMenued % this.getUserTable().getWidth());
        return true;
    case MENU_ITEM_ID_DELETE_ROW:
        String rowId = this.getUserTable().getRowAtIndex(
            this.mLastDataCellMenued /
            this.getUserTable().getWidth()).getRowId();
        this.deleteRow(rowId);
        init();
        return true;
    case MENU_ITEM_ID_EDIT_ROW:
      // It is possible that a custom form has been defined for this table.
      // We will get the strings we need, and then set the parameter object.
      ActivityUtil.editRow(
          (AbsBaseActivity) getActivity(),
          this.getUserTable(),
          this.mLastDataCellMenued / this.getUserTable().getWidth());
      // launch ODK Collect
      return true;
    case MENU_ITEM_ID_SET_COLUMN_AS_PRIME:
        addGroupByColumn(tp.getColumnByIndex(this.mLastHeaderCellMenued));
        init();
        return true;
    case MENU_ITEM_ID_UNSET_COLUMN_AS_PRIME:
        removeGroupByColumn(tp.getColumnByIndex(this.mLastHeaderCellMenued));
        init();
        return true;
    case MENU_ITEM_ID_SET_COLUMN_AS_SORT:
        setColumnAsSort(tp.getColumnByIndex(this.mLastHeaderCellMenued));
        init();
        return true;
    case MENU_ITEM_ID_UNSET_COLUMN_AS_SORT:
        setColumnAsSort(null);
        init();
        return true;
    case MENU_ITEM_ID_SET_AS_INDEXED_COL:
        setColumnAsIndexedCol(tp.getColumnByIndex(this.mLastHeaderCellMenued));
        init();
        return true;
    case MENU_ITEM_ID_UNSET_AS_INDEXED_COL:
        setColumnAsIndexedCol(null);
        init();
        return true;
    case MENU_ITEM_ID_EDIT_COLUMN_COLOR_RULES:
      Intent i = new Intent(
          this.getActivity(),
          ColorRuleManagerActivity.class);
      i.putExtra(
          Constants.IntentKeys.APP_NAME,
          this.getUserTable().getTableProperties().getAppName());
      i.putExtra(ColorRuleManagerActivity.INTENT_KEY_ELEMENT_KEY,
          tp.getColumnByIndex(this.mLastHeaderCellMenued).getElementKey());
      i.putExtra(ColorRuleManagerActivity.INTENT_KEY_TABLE_ID,
          this.getUserTable().getTableProperties().getTableId());
      i.putExtra(ColorRuleManagerActivity.INTENT_KEY_RULE_GROUP_TYPE,
          ColorRuleGroup.Type.COLUMN.name());
      startActivity(i);
    default:
      Log.e(TAG, "unrecognized menu item selected: " + item.getItemId());
      return super.onContextItemSelected(item);
    }
  }

  public void regularCellLongClicked(int cellId, int rawX, int rawY,
      boolean isIndexed) {
    // So we need to check for whether or not the table is indexed again and
    // alter the cellId appropriately.
    if (isIndexed) {
      int colNum = cellId % (this.getUserTable().getWidth() - 1);
      int rowNum = cellId / (this.getUserTable().getWidth() - 1);
      cellId = cellId + rowNum + ((colNum < this.mIndexedColumnOffset) ? 0 : 1);
    }
      this.addOverlay(new CellPopout(cellId), 100, 100, rawX, rawY);
  }

  @Override
  public void indexedColCellLongClicked(int cellId, int rawX, int rawY) {
    // here it's just the row plus the number of the indexed column.
    // So the true cell id is the cellId parameter, which is essentially the
    // row number, * the width of the table, plus the indexed col
    int trueNum =
        cellId *
        this.getUserTable().getWidth() +
        this.mIndexedColumnOffset;
    this.addOverlay(new CellPopout(trueNum), 100, 100, rawX, rawY);
  }

  @Override
  public void regularCellDoubleClicked(int cellId, boolean isIndexed,
      int rawX, int rawY) {
    // So it seems like the cellId is coming from the mainData table, which
    // does NOT include the index. So to get the right row here we actually
    // have to perform a little extra.
    if (!isIndexed) {
      this.addOverlay(new CellPopout(cellId), 100, 100, rawX, rawY);
    } else { // it's indexed
      int colNum = cellId % (this.getUserTable().getWidth() - 1);
      int rowNum = cellId / (this.getUserTable().getWidth() - 1);
      int trueNum = cellId + rowNum + ((colNum < this.mIndexedColumnOffset) ? 0 : 1);
      // trying to hack together correct thing for overlay
      int trueCellId = rowNum * this.getUserTable().getWidth() +
          colNum + ((colNum < this.mIndexedColumnOffset) ? 0 : 1);
      this.addOverlay(new CellPopout(trueCellId), 100, 100, rawX, rawY);
    }

  }

  /**
   * Return true if group bys are currently being displayed.
   * @return
   */
  private boolean hasGroupBys() {
    SQLQueryStruct queryStruct = IntentUtil.getSQLQueryStructFromBundle(
        this.getActivity().getIntent().getExtras());
    return queryStruct.groupBy != null;
  }

  @Override
  public void prepRegularCellOccm(ContextMenu menu, int cellId) {
      this.mLastDataCellMenued = cellId;
      if (this.hasGroupBys()) {
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
  public void openContextMenu(View view) {
    this.getActivity().openContextMenu(view);
  }

  @Override
  public void prepHeaderCellOccm(ContextMenu menu, int cellId) {
      this.mLastHeaderCellMenued = cellId;
      ColumnProperties cp =
          this.getTableProperties().getColumnByIndex(
              this.mLastHeaderCellMenued);
      if (this.getTableProperties().isGroupByColumn(cp.getElementKey())) {
          menu.add(ContextMenu.NONE, MENU_ITEM_ID_UNSET_COLUMN_AS_PRIME,
                  ContextMenu.NONE, "Unset as Prime");
      } else if ((this.getTableProperties().getSortColumn() != null) &&
              this.getTableProperties().getSortColumn()
                      .equals(cp.getElementKey())) {
          menu.add(ContextMenu.NONE, MENU_ITEM_ID_UNSET_COLUMN_AS_SORT,
                  ContextMenu.NONE, "Unset as Sort");
      } else {
          menu.add(ContextMenu.NONE, MENU_ITEM_ID_SET_COLUMN_AS_PRIME,
                  ContextMenu.NONE, "Set as Prime");
          menu.add(ContextMenu.NONE, MENU_ITEM_ID_SET_COLUMN_AS_SORT,
                  ContextMenu.NONE, "Set as Sort");
      }
      if (cellId == this.mIndexedColumnOffset) {
          menu.add(ContextMenu.NONE, MENU_ITEM_ID_UNSET_AS_INDEXED_COL,
                  ContextMenu.NONE, "Unfreeze Column");
      } else {
          menu.add(ContextMenu.NONE, MENU_ITEM_ID_SET_AS_INDEXED_COL,
                  ContextMenu.NONE, "Freeze Column");
      }

      menu.add(ContextMenu.NONE, MENU_ITEM_ID_EDIT_COLUMN_COLOR_RULES,
          ContextMenu.NONE, MENU_ITEM_MSG_EDIT_COLUMN_COLOR_RULES);

  }

  @Override
 public void regularCellClicked(int cellId) {
    this.removeOverlay();
 }

 @Override
 public void headerCellClicked(int cellId) {
    this.removeOverlay();
 }

 @Override
 public void indexedColCellClicked(int cellId) {
    this.removeOverlay();
 }

 public void indexedColCellDoubleClicked(int cellId, int rawX, int rawY) {
   // Ok, so here the cellId is also the row number, as we only allow a
   // single indexed column atm. So if you double click the 5th cell, it will
   // also have to be the 5th row.
   int trueNum =
       cellId *
       this.getUserTable().getWidth() +
       this.mIndexedColumnOffset;
   this.addOverlay(new CellPopout(trueNum), 100, 100, rawX, rawY);
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
         super(SpreadsheetFragment.this.getActivity());
         this.cellId = cellId;
         context = SpreadsheetFragment.this.getActivity();
         TextView valueView = new TextView(context);
         valueView.setText(getUserTable().getDisplayTextOfData(
             context, cellId));
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
         if (hasGroupBys()) {
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
         final int rowNumber = cellId / getUserTable().getWidth();
         String syncStateName = getUserTable().getMetadataByElementKey(
             rowNumber,
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
         TableProperties tp = getTableProperties();
         final ColumnProperties
             cp = tp.getColumnByIndex(cellId % getUserTable().getWidth());
         // First we want to check if we need to add a join item for this
         // column.
         if (cp.getColumnType() == ColumnType.TABLE_JOIN) {
           itemIds.add(MENU_ITEM_ID_OPEN_JOIN_TABLE);
           itemLabels.add(MENU_ITEM_MSG_OPEN_JOIN_TABLE);
         }
         AlertDialog.Builder builder = new AlertDialog.Builder(
                 getActivity());
         builder.setItems(itemLabels.toArray(new String[0]),
                 new DialogInterface.OnClickListener() {
           /*
            * It's not clear to me why we're dividing by table.getWidth() for
            * so many of the things below when we want row number. It seems
            * like we would want the height in some of these cases...
            */
             @Override
             public void onClick(DialogInterface dialog, int which) {
               final String appName = getAppName();
                 switch (itemIds.get(which)) {
                 case MENU_ITEM_ID_HISTORY_IN:
                     openCollectionView(cellId / getUserTable().getWidth());
                     removeOverlay();
                     break;
                 case MENU_ITEM_ID_EDIT_CELL:
                     openCellEditDialog(
                             getUserTable().getRowAtIndex(
                                 cellId / getUserTable().getWidth()).getRowId(),
                             getUserTable().getData(cellId),
                             cellId % getUserTable().getWidth());
                     removeOverlay();
                     break;
                 case MENU_ITEM_ID_RESOLVE_ROW_CONFLICT:
                   // We'll just launch the resolve activity.
                   Intent i = new Intent(context,
                       ConflictResolutionRowActivity.class);
                   i.putExtra(Constants.IntentKeys.APP_NAME,
                       getTableProperties().getAppName());
                   i.putExtra(Controller.INTENT_KEY_TABLE_ID,
                       getTableProperties().getTableId());
                   String conflictRowId =
                       getUserTable().getRowAtIndex(rowNumber).getRowId();
                   i.putExtra(
                       ConflictResolutionRowActivity.INTENT_KEY_ROW_ID,
                       conflictRowId);
                   context.startActivity(i);
                   break;
                 case MENU_ITEM_ID_DELETE_ROW:
                   AlertDialog confirmDeleteAlert;
                   // Prompt an alert box
                   final String rowId =
                       getUserTable().getRowAtIndex(
                           cellId / getUserTable().getWidth()).getRowId();
                   AlertDialog.Builder alert =
                       new AlertDialog.Builder(getActivity());
                   alert.setTitle(getString(R.string.confirm_delete_row))
                   .setMessage(getString(R.string.are_you_sure_delete_row, rowId));
                   // OK Action => delete the row
                   alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int whichButton) {
                        deleteRow(rowId);
                        removeOverlay();
                        init();
                      }
                   });

                   // Cancel Action
                   alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int whichButton) {
                       // Canceled.
                        removeOverlay();
                      }
                   });
                   // show the dialog
                   confirmDeleteAlert = alert.create();
                   confirmDeleteAlert.show();
                     break;
                 case MENU_ITEM_ID_EDIT_ROW:
                   // It is possible that a custom form has been defined for this table.
                   // We will get the strings we need, and then set the parameter object.
                     ActivityUtil.editRow(
                         (AbsBaseActivity) getActivity(),
                         getUserTable(),
                         cellId / getUserTable().getWidth());
                     removeOverlay();
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
                                  getActivity(),
                                  appName,
                                  tableId);
                         String joinedColDisplayName =
                             joinedTable.getColumnByElementKey(elementKey)
                             .getElementKey();
                         // I would prefer this kind of query to be set in another
                         // object, but alas, it looks like atm it is hardcoded.
                         Controller.launchTableActivity(context, joinedTable, joinedTable.getDefaultViewType());
                           removeOverlay();
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

 }

  private class CellEditDialog extends AlertDialog {

    private final String rowId;
    private final int colIndex;
    private final String elementKey;
    private final CellValueView.CellEditView cev;
    private DataUtil dataUtil;
    private DbTable dbTable;

    public CellEditDialog(String rowId, String value, int colIndex) {
      super(getActivity());
      this.dataUtil = new DataUtil(
          Locale.ENGLISH,
          TimeZone.getDefault());
      this.dbTable = DbTable.getDbTable(getTableProperties());
      this.rowId = rowId;
      this.colIndex = colIndex;
      ColumnProperties cp = getTableProperties().getColumnByIndex(colIndex);
      this.elementKey = cp.getElementKey();
      cev = CellValueView.getCellEditView(getActivity(), cp, value);
      this.buildView(getActivity());
    }

    private void buildView(Context context) {
      Button setButton = new Button(context);
      setButton.setText(getActivity().getResources().getString(R.string.set));
      setButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          String value = dataUtil.validifyValue(
              getTableProperties().getColumnByElementKey(
                  CellEditDialog.this.elementKey),
              cev.getValue());
          if (value == null) {
            // TODO: alert the user
            return;
          }
          Map<String, String> values = new HashMap<String, String>();
          values.put(CellEditDialog.this.elementKey, value);

          // TODO: supply reasonable values for these...
          String savepointCreator = null; // user on phone
          Long timestamp = null; // current time
          String formId = null; // formId used by ODK Collect
          String locale = null; // current locale

          dbTable.updateRow(
              rowId,
              formId,
              locale,
              timestamp,
              savepointCreator,
              values);
          init();
          dismiss();
        }
      });
      Button cancelButton = new Button(context);
      cancelButton.setText(
          getActivity().getResources().getString(R.string.cancel));
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


  private static final int MENU_ITEM_ID_HISTORY_IN = 0;
  private static final int MENU_ITEM_ID_EDIT_CELL = 1;
  private static final int MENU_ITEM_ID_DELETE_ROW = 2;
  private static final int MENU_ITEM_ID_SET_COLUMN_AS_PRIME = 3;
  private static final int MENU_ITEM_ID_UNSET_COLUMN_AS_PRIME = 4;
  private static final int MENU_ITEM_ID_SET_COLUMN_AS_SORT = 5;
  private static final int MENU_ITEM_ID_UNSET_COLUMN_AS_SORT = 6;
  private static final int MENU_ITEM_ID_SET_AS_INDEXED_COL = 7;
  private static final int MENU_ITEM_ID_UNSET_AS_INDEXED_COL = 8;
  private static final int MENU_ITEM_ID_EDIT_ROW = 9;
  // This should allow for the opening of a joined table.
  private static final int MENU_ITEM_ID_OPEN_JOIN_TABLE = 10;
  private static final int MENU_ITEM_ID_EDIT_COLUMN_COLOR_RULES = 11;
  private static final int MENU_ITEM_ID_RESOLVE_ROW_CONFLICT = 12;
  private static final String MENU_ITEM_MSG_OPEN_JOIN_TABLE =
      "Open Join Table";
  private static final String MENU_ITEM_MSG_EDIT_COLUMN_COLOR_RULES =
      "Edit Column Color Rules";


}
