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
package org.opendatakit.tables.views;

import android.content.Context;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import org.opendatakit.data.ColorRuleGroup;
import org.opendatakit.data.utilities.ColumnUtil;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.data.ColumnDefinition;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.application.Tables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A view similar to a spreadsheet. Builds TabularViews for the header and body
 * (builds two sets of these if a column is frozen to the left).
 * <p>
 * SS: I made some changes to this to try and make scrolling more efficient. I
 * am leaving some of the seemingly unreferenced and now unnecessary
 * methods/fields in case changes someone has made to this class in parallel
 * rely on these changes.
 *
 * @author sudar.sam@gmail.com
 * @author unknown
 */
public class SpreadsheetView extends LinearLayout implements TabularView.Controller {

  private static final String TAG = SpreadsheetView.class.getSimpleName();

  // Used by onTouch to determine how long counts as a click vs how long counts as a long click
  private static final int MIN_CLICK_DURATION = 0;
  private static final int MAX_DOUBLE_CLICK_TIME = 500;
  private static final int MIN_LONG_CLICK_DURATION = 1000;

  private final Context context;
  private final Controller controller;
  private final SpreadsheetUserTable table;
  private final int fontSize;
  private final int completeColWidths[];

  private final Map<String, ColorRuleGroup> mElementKeyToColorRuleGroup;

  private final ColorRuleGroup mStatusColumnRuleGroup;
  private final ColorRuleGroup mTableColorRuleGroup;

  // Keeping this for now in case someone else needs to work with the code
  // and relied on this variable.
  private ScrollView dataStatusScroll;
  private HorizontalScrollView wrapScroll;

  private ScrollView mainScroll = null;
  private ScrollView indexScroll;
  private TabularView mainData = null;
  private TabularView mainHeader = null;
  private TabularView indexData;
  private TabularView indexHeader;

  private View.OnTouchListener mainDataCellClickListener;
  private View.OnTouchListener mainHeaderCellClickListener;
  private View.OnTouchListener indexDataCellClickListener;
  private View.OnTouchListener indexHeaderCellClickListener;

  private CellInfo lastHighlightedCellId;
  /**
   * used for making sure the user double tapped the same cell twice instead of differenct cells
   */
  private CellInfo lastLastHighlightedCellId;

  /**
   * Initializes a new spreadsheet view to the specified table. It pulls the app name out of the
   * context, detects the correct font size, gets the column definitions from the database, and
   * handles whether the table is indexed or not
   *
   * @param context    The context the spreadsheet is executing in, saved
   * @param controller a SpreadsheetFragment
   * @param table      the table to be displayed by the spreadsheet
   * @throws ServicesAvailabilityException if the database is down
   */
  public SpreadsheetView(Context context, Controller controller, SpreadsheetUserTable table)
      throws ServicesAvailabilityException {
    super(context);
    this.context = context;
    this.controller = controller;
    this.table = table;

    // TODO: figure out if we can invalidate a screen region to get it to render the screen
    // rather than disabling the hardware acceleration on this view. Disable it so that you don't
    // have to tap the screen to after a scroll action to see the new portion of the spreadsheet.
    this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

    // We have to initialize the items that will be shared across the TabularView objects.
    this.mElementKeyToColorRuleGroup = new HashMap<>();

    // if a custom font size is defined in the KeyValueStore, use that if not, use the general
    // font size defined in preferences
    String appName = table.getAppName();
    UserDbInterface dbInterface = Tables.getInstance().getDatabase();
    DbHandle db = null;
    try {
      db = dbInterface.openDatabase(appName);
      String[] adminColumns = dbInterface.getAdminColumns();
      for (ColumnDefinition cd : table.getColumnDefinitions().getColumnDefinitions()) {
        mElementKeyToColorRuleGroup.put(cd.getElementKey(),
            table.getColumnColorRuleGroup(dbInterface, db, cd.getElementKey(), adminColumns));
      }
      mStatusColumnRuleGroup = ColorRuleGroup
          .getStatusColumnRuleGroup(dbInterface, appName, db, table.getTableId(), adminColumns);
      mTableColorRuleGroup = ColorRuleGroup
          .getTableColorRuleGroup(dbInterface, appName, db, table.getTableId(), adminColumns);
      completeColWidths = getColumnWidths(db);
      fontSize = TableUtil.get()
          .getSpreadsheetViewFontSize(getContext(), dbInterface, appName, db, table.getTableId());
    } finally {
      if (db != null) {
        dbInterface.closeDatabase(appName, db);
      }
    }

    initListeners();
    if (!table.isIndexed()) {
      buildNonIndexedTable();
    } else {
      buildIndexedTable();
      indexData.setOnTouchListener(indexDataCellClickListener);
      indexHeader.setOnTouchListener(indexHeaderCellClickListener);
    }
    mainData.setOnTouchListener(mainDataCellClickListener);
    mainHeader.setOnTouchListener(mainHeaderCellClickListener);
  }

  /**
   * Called when the user double taps or long taps a data cell, picks the view to open a context
   * menu on and passes that through to the controller
   */
  public void openDataMenu() {
    if (table.isIndexed()) {
      controller.openContextMenu(indexData);
    } else {
      controller.openContextMenu(mainData);
    }
  }

  /**
   * Called when the user double taps or long taps a header cell, picks the view to open a context
   * menu on and passes that through to the controller
   */
  public void openHeaderMenu() {
    if (table.isIndexed()) {
      controller.openContextMenu(indexHeader);
    } else {
      controller.openContextMenu(mainHeader);
    }
  }

  /**
   * Initializes the click listeners. There are four right now
   */
  private void initListeners() {
    // The logic here is a bit crazy.
    // header and data click listeners both receive the full
    // TODO: the full what? Looks like someone forgot to finish this comment
    mainDataCellClickListener = new CellTouchListener() {
      /**
       * Called when the user taps on a cell. Sets some variables and highlights it
       * @param cellId the id of the cell that the user tapped
       */
      @Override
      protected void takeDownAction(CellInfo cellId) {
        lastLastHighlightedCellId = lastHighlightedCellId;
        lastHighlightedCellId = cellId;
        if (table.isIndexed()) {
          indexData.highlight(null);
        }
        mainData.highlight(cellId);
      }

      /**
       * Called when the user has held down their tap for at least MIN_CLICK_DURATION
       * milliseconds, currently zero
       */
      @Override
      protected void takeClickAction() {
        controller.dataCellClicked(lastHighlightedCellId);
      }

      /**
       * Called when the user taps for at least MIN_LONG_CLICK_DURATION milliseconds. Opens the
       * context menu to the selected row id
       * @param rawX unused
       * @param rawY unused
       */
      @Override
      protected void takeLongClickAction(int rawX, int rawY) {
        controller.openContextMenu(mainData);
      }

      /**
       * Checks if the user clicked on the same object twice, and if so, does the same thing as a
       * long click action
       * @param rawX unused
       * @param rawY unused
       */
      @Override
      protected void takeDoubleClickAction(int rawX, int rawY) {
        // Because the cellId.equals method is screwed up
        if (lastHighlightedCellId.colPos == lastLastHighlightedCellId.colPos
            && lastHighlightedCellId.rowId == lastLastHighlightedCellId.rowId) {
          takeLongClickAction(rawX, rawY);
        }
      }
    };
    mainHeaderCellClickListener = new CellTouchListener() {
      /**
       * Called when the user taps on a cell. Sets some variables and highlights it
       * @param cellId the id of the cell that the user tapped
       */
      @Override
      protected void takeDownAction(CellInfo cellId) {
        lastLastHighlightedCellId = lastHighlightedCellId;
        if (table.isIndexed()) {
          indexData.highlight(null);
        }
        lastHighlightedCellId = cellId;
        mainData.highlight(null);
      }

      /**
       * Called when the user has held down their tap for at least MIN_CLICK_DURATION
       * milliseconds, currently zero
       */
      @Override
      protected void takeClickAction() {
        controller.headerCellClicked(lastHighlightedCellId);
      }

      /**
       * Called when the user taps for at least MIN_LONG_CLICK_DURATION milliseconds. Opens the
       * context menu to the selected row id
       * @param rawX unused
       * @param rawY unused
       */
      @Override
      protected void takeLongClickAction(int rawX, int rawY) {
        controller.openContextMenu(mainHeader);
      }

      /**
       * Checks if the user clicked on the same object twice, and if so, does the same thing as a
       * long click action
       * @param rawX unused
       * @param rawY unused
       */
      @Override
      protected void takeDoubleClickAction(int rawX, int rawY) {
        // Because the cellId.equals method is screwed up
        if (lastHighlightedCellId.colPos == lastLastHighlightedCellId.colPos
            && lastHighlightedCellId.rowId == lastLastHighlightedCellId.rowId) {
          takeLongClickAction(rawX, rawY);
        }
      }
    };
    indexDataCellClickListener = new CellTouchListener() {
      /**
       * Called when the user taps on a cell. Sets some variables and highlights it
       * @param cellId the id of the cell that the user tapped
       */
      @Override
      protected void takeDownAction(CellInfo cellId) {
        mainData.highlight(null);
        lastLastHighlightedCellId = lastHighlightedCellId;
        lastHighlightedCellId = cellId;
        indexData.highlight(cellId);
      }

      /**
       * Called when the user has held down their tap for at least MIN_CLICK_DURATION
       * milliseconds, currently zero
       */
      @Override
      protected void takeClickAction() {
        controller.dataCellClicked(lastHighlightedCellId);
      }

      /**
       * Called when the user taps for at least MIN_LONG_CLICK_DURATION milliseconds. Opens the
       * context menu to the selected row id
       * @param rawX unused
       * @param rawY unused
       */
      @Override
      protected void takeLongClickAction(int rawX, int rawY) {
        controller.openContextMenu(indexData);
      }

      /**
       * Checks if the user clicked on the same object twice, and if so, does the same thing as a
       * long click action
       * @param rawX unused
       * @param rawY unused
       */
      @Override
      protected void takeDoubleClickAction(int rawX, int rawY) {
        // Because the cellId.equals method is screwed up
        if (lastHighlightedCellId.colPos == lastLastHighlightedCellId.colPos
            && lastHighlightedCellId.rowId == lastLastHighlightedCellId.rowId) {
          takeLongClickAction(rawX, rawY);
        }
      }
    };
    indexHeaderCellClickListener = new CellTouchListener() {
      /**
       * Called when the user taps on a cell. Sets some variables and highlights it
       * @param cellId the id of the cell that the user tapped
       */
      @Override
      protected void takeDownAction(CellInfo cellId) {
        mainData.highlight(null);
        indexData.highlight(null);
        lastLastHighlightedCellId = lastHighlightedCellId;
        lastHighlightedCellId = cellId;
      }

      /**
       * Called when the user has held down their tap for at least MIN_CLICK_DURATION
       * milliseconds, currently zero
       */
      @Override
      protected void takeClickAction() {
        controller.headerCellClicked(lastHighlightedCellId);
      }

      /**
       * Called when the user taps for at least MIN_LONG_CLICK_DURATION milliseconds. Opens the
       * context menu to the selected row id
       * @param rawX unused
       * @param rawY unused
       */
      @Override
      protected void takeLongClickAction(int rawX, int rawY) {
        controller.openContextMenu(indexHeader);
      }

      /**
       * Checks if the user clicked on the same object twice, and if so, does the same thing as a
       * long click action
       * @param rawX unused
       * @param rawY unused
       */
      @Override
      protected void takeDoubleClickAction(int rawX, int rawY) {
        // Because the cellId.equals method is screwed up
        if (lastHighlightedCellId.colPos == lastLastHighlightedCellId.colPos
            && lastHighlightedCellId.rowId == lastLastHighlightedCellId.rowId) {
          takeLongClickAction(rawX, rawY);
        }
      }
    };
  }

  /**
   * Internal helper method to build a non indexed table, called by the SpreadsheetView constructor
   * It constructs some views and sets the onTouch event for the main scroll view
   */
  private void buildNonIndexedTable() {
    // the false is to indicate that we're building a non-indexed table
    View wrapper = buildTable(null, false);
    wrapScroll = new HorizontalScrollView(context);
    wrapScroll.addView(wrapper, LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.MATCH_PARENT);
    // this was all here before
    LinearLayout.LayoutParams wrapLp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
    wrapLp.weight = 1;
    wrapScroll.setHorizontalFadingEdgeEnabled(true); // works

    ViewGroup completeWrapper = new LinearLayout(context);
    View statusWrapper = buildStatusTable();
    statusWrapper.setHorizontalFadingEdgeEnabled(true);
    statusWrapper.setVerticalFadingEdgeEnabled(true);
    completeWrapper.addView(statusWrapper);
    completeWrapper.addView(wrapScroll);
    completeWrapper.setHorizontalFadingEdgeEnabled(true);
    completeWrapper.setVerticalFadingEdgeEnabled(true);

    addView(completeWrapper, wrapLp);
    mainScroll.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent event) {
        dataStatusScroll.scrollTo(dataStatusScroll.getScrollX(), view.getScrollY());
        /*
        if (event.getAction() == MotionEvent.ACTION_UP) {
          mainScroll.startScrollerTask();
        }
        */
        return false;
      }
    });
  }

  /**
   * This is also called by the SpreadsheetView constructor. It makes a non indexed table for the
   * main wrapper, an indexed table for the index wrapper, and a status wrapper. It puts them in a
   * scroll view and adds it, then it sets the on touch listener to get the x and y values from
   * the scroll view and lock one of them while the other scrolls
   */
  private void buildIndexedTable() {
    String indexElementKey = table.getIndexedColumnElementKey();
    // build a non-indexed table for the main wrapper
    View mainWrapper = buildTable(indexElementKey, false);
    // Here the true indicates that we are building an indexed table
    View indexWrapper = buildTable(indexElementKey, true);
    wrapScroll = new HorizontalScrollView(context);
    wrapScroll.addView(mainWrapper, LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.MATCH_PARENT);
    wrapScroll.setHorizontalFadingEdgeEnabled(true);
    ViewGroup wrapper = new LinearLayout(context);
    wrapper.addView(indexWrapper);
    wrapper.addView(wrapScroll);

    ViewGroup completeWrapper = new LinearLayout(context);
    View statusWrapper = buildStatusTable();
    completeWrapper.addView(statusWrapper);
    completeWrapper.addView(wrapper);

    addView(completeWrapper);

    indexScroll.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent event) {
        mainScroll.scrollTo(mainScroll.getScrollX(), view.getScrollY());
        dataStatusScroll.scrollTo(mainScroll.getScrollX(), view.getScrollY());
        /*
        if (event.getAction() == MotionEvent.ACTION_UP) {
          indexScroll.startScrollerTask();
          mainScroll.startScrollerTask();
        }
        */
        return false;
      }
    });
    mainScroll.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent event) {
        indexScroll.scrollTo(indexScroll.getScrollX(), view.getScrollY());
        dataStatusScroll.scrollTo(indexScroll.getScrollX(), view.getScrollY());
        /*
        if (event.getAction() == MotionEvent.ACTION_UP) {
          indexScroll.startScrollerTask();
          mainScroll.startScrollerTask();
        }
        */
        return false;
      }
    });
  }

  /**
   * Builds a (piece of a) table. The table may either be the indexed column of
   * an indexed table, the non-indexed columns of an indexed table, or the
   * entirety of an unindexed table.
   * It returns a LinearLayout that contains all the relevant cells
   *
   * @param indexElementKey the column that is indexed (or null)
   * @param isIndexed       whether this table is for the indexed column
   * @return a view including the header and body of the table
   */
  private View buildTable(String indexElementKey, boolean isIndexed) {
    // WebLogger.getLogger(table.getAppName()).i(TAG, "entering buildTable. indexedCol: " + indexedCol + "isIndexed: " + isIndexed);
    List<String> elementKeysToDisplay = new ArrayList<>();
    int[] colWidths;
    TabularView dataTable;
    TabularView headerTable;
    if (isIndexed) {
      ColumnDefinition cd = table.getColumnByElementKey(indexElementKey);
      elementKeysToDisplay.add(cd.getElementKey());
      colWidths = new int[1];
      colWidths[0] = completeColWidths[table.getColumnIndexOfElementKey(indexElementKey)];
      dataTable = TabularView
          .getIndexDataTable(context, this, table, elementKeysToDisplay, colWidths, fontSize,
              this.mElementKeyToColorRuleGroup, mTableColorRuleGroup);
      headerTable = TabularView
          .getIndexHeaderTable(context, this, table, elementKeysToDisplay, colWidths, fontSize,
              this.mElementKeyToColorRuleGroup, mTableColorRuleGroup);
    } else {
      int width = indexElementKey == null || indexElementKey.isEmpty() ?
          table.getWidth() :
          table.getWidth() - 1;
      colWidths = new int[width];
      int addIndex = 0;
      for (int i = 0; i < table.getWidth(); i++) {
        ColumnDefinition cd = table.getColumnByIndex(i);
        if (cd.getElementKey().equals(indexElementKey)) {
          continue;
        }
        elementKeysToDisplay.add(cd.getElementKey());
        colWidths[addIndex] = completeColWidths[i];
        addIndex++;
      }
      dataTable = TabularView
          .getMainDataTable(context, this, table, elementKeysToDisplay, colWidths, fontSize,
              this.mElementKeyToColorRuleGroup, mTableColorRuleGroup);
      headerTable = TabularView
          .getMainHeaderTable(context, this, table, elementKeysToDisplay, colWidths, fontSize,
              this.mElementKeyToColorRuleGroup, mTableColorRuleGroup);
    }

    ScrollView dataScroll = new ScrollView(context);
    dataScroll.addView(dataTable,
        new ViewGroup.LayoutParams(dataTable.getTableWidth(), dataTable.getTableHeight()));
    dataScroll.setVerticalFadingEdgeEnabled(true);
    dataScroll.setHorizontalFadingEdgeEnabled(true);
    if (isIndexed) {
      indexData = dataTable;
      indexHeader = headerTable;
      indexScroll = dataScroll;
    } else {
      mainData = dataTable;
      mainHeader = headerTable;
      mainScroll = dataScroll;
    }
    LinearLayout wrapper = new LinearLayout(context);
    wrapper.setOrientation(LinearLayout.VERTICAL);
    wrapper.addView(headerTable, headerTable.getTableWidth(), headerTable.getTableHeight());
    LinearLayout.LayoutParams dataLp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
    dataLp.weight = 1;
    wrapper.addView(dataScroll, dataLp);
    return wrapper;
  }

  /**
   * Helper method to build a status table, used by both buildIndexedTable and buildNonIndexedTable
   *
   * @return a view that contains the TabularViews for the StatusDataTable and StatusHeaderTable
   * in a wrapper
   */
  private View buildStatusTable() {
    int[] colWidths;
    colWidths = new int[1];
    colWidths[0] = TabularView.DEFAULT_STATUS_COLUMN_WIDTH;

    dataStatusScroll = new ScrollView(context);
    TabularView dataTable = TabularView
        .getStatusDataTable(context, this, table, colWidths, fontSize,
            this.mElementKeyToColorRuleGroup, mStatusColumnRuleGroup);
    dataTable.setVerticalFadingEdgeEnabled(true);
    dataTable.setVerticalScrollBarEnabled(false);
    dataStatusScroll.addView(dataTable,
        new ViewGroup.LayoutParams(dataTable.getTableWidth(), dataTable.getTableHeight()));
    dataStatusScroll.setVerticalFadingEdgeEnabled(true);
    dataStatusScroll.setHorizontalFadingEdgeEnabled(true);
    TabularView headerTable = TabularView
        .getStatusHeaderTable(context, this, table, colWidths, fontSize,
            this.mElementKeyToColorRuleGroup, mTableColorRuleGroup);
    LinearLayout wrapper = new LinearLayout(context);
    wrapper.setOrientation(LinearLayout.VERTICAL);
    wrapper.addView(headerTable, headerTable.getTableWidth(), headerTable.getTableHeight());
    LinearLayout.LayoutParams dataLp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
    dataLp.weight = 1;
    wrapper.addView(dataStatusScroll, dataLp);
    wrapper.setVerticalFadingEdgeEnabled(true);
    wrapper.setHorizontalFadingEdgeEnabled(true);
    return wrapper;
  }

  /**
   * Gets the x translation of the scroll. This is in particular how far you
   * have scrolled to look at columns that do not begin onscreen.
   *
   * @return the coordinate of where the user scrolled
   */
  @Override
  public int getMainScrollX() {
    // this is getting the correct x
    return wrapScroll.getScrollX();
    // return from wrapScroll, because getMainScrollY uses mainScroll
  }

  /**
   * Gets the y translation of the scroll. This is in particular the y offset
   * for the actual scrolling of the rows, so that a positive offset will
   * indicate that you have scrolled to some non-zero row.
   *
   * @return the y coordinate of where the user scrolled
   */
  @Override
  public int getMainScrollY() {
    // this is getting the correct y
    return mainScroll.getScrollY();
    // return from mainScroll, because getMainScrollX uses wrapScroll
  }

  /**
   * Called when someone with permission to edit the table double clicks or long clicks on a cell.
   * Forwards the request to the controller, which is in fragments.SpreadsheetFragment
   *
   * @param menu the menu to be populated with items then displayed
   */
  @Override
  public void onCreateDataContextMenu(ContextMenu menu) {
    try {
      controller.prepDataCellOccm(menu, lastHighlightedCellId);
    } catch (ServicesAvailabilityException e) {
      String appName = SpreadsheetView.this.table.getAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e);
      Toast.makeText(getContext(), R.string.error_accessing_database, Toast.LENGTH_LONG).show();
    }
  }

  /**
   * Called when someone with permission to edit the table double clicks or long clicks on a
   * header cell. Forwards the request to the controller, which is in fragments.SpreadsheetFragment
   *
   * @param menu the menu to be populated with items then displayed
   */
  @Override
  public void onCreateHeaderContextMenu(ContextMenu menu) {
    try {
      controller.prepHeaderCellOccm(menu, lastHighlightedCellId);
    } catch (ServicesAvailabilityException e) {
      String appName = SpreadsheetView.this.table.getAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e);
      Toast.makeText(getContext(), R.string.error_accessing_database, Toast.LENGTH_LONG).show();
    }
  }

  /**
   * Get the column widths for the table. The values in the array match the
   * order specified in the column order.
   * <p>
   * NB: If getting this from outside of spreadsheet view, you should really
   * consider if you need to be accessing column widths.
   *
   * @param db The database to use
   * @return an array of the widths for each column, taken from the database
   * @throws ServicesAvailabilityException if the database is down
   */
  public int[] getColumnWidths(DbHandle db) throws ServicesAvailabilityException {
    // So what we want to do is go through and get the column widths for each
    // column. A problem here is that there is no caching, and if you have a
    // lot of columns you're really working the gut of the database.
    int numberOfDisplayColumns = table.getNumberOfDisplayColumns();
    int[] columnWidths = new int[numberOfDisplayColumns];
    String appName = table.getAppName();

    Map<String, Integer> colWidths = ColumnUtil.get()
        .getColumnWidths(Tables.getInstance().getDatabase(), appName, db,
            table.getTableId(), table.getColumnDefinitions());

    for (int i = 0; i < numberOfDisplayColumns; i++) {
      ColumnDefinition cd = table.getColumnByIndex(i);
      String elementKey = cd.getElementKey();
      columnWidths[i] = colWidths.get(elementKey);
    }
    return columnWidths;
  }

  /**
   * Implemented by fragments.SpreadsheetFragment
   */
  public interface Controller {

    /**
     * Called when the user clicks a header cell
     *
     * @param cellId The ID of the cell that the user clicked
     */
    void headerCellClicked(CellInfo cellId);

    /**
     * Called when the user activates a menu on a header cell, populates the list of options in the
     * menu.
     *
     * @param menu   the ContextMenu about to be created
     * @param cellId the cell id that was double clicked or long clicked to trigger the menu
     * @throws ServicesAvailabilityException if the database is down
     */
    void prepHeaderCellOccm(ContextMenu menu, CellInfo cellId) throws ServicesAvailabilityException;

    /**
     * Called when the user clicks a data cell
     *
     * @param cellId The ID of the cell that the user clicked
     */
    void dataCellClicked(CellInfo cellId);

    /**
     * Called when the user activates a menu on a data cell, populates the list of options in the
     * menu.
     *
     * @param menu   the ContextMenu about to be created
     * @param cellId the cell id that was double clicked or long clicked to trigger the menu
     * @throws ServicesAvailabilityException if the database is down
     */
    void prepDataCellOccm(ContextMenu menu, CellInfo cellId) throws ServicesAvailabilityException;

    /**
     * Opens a menu on the last clicked cell, as appropriate
     *
     * @param view the view to open the menu on
     */
    void openContextMenu(View view);
  }

  /**
   * An abstract helper class that gets anonymously extended four times in initListeners. It
   * extends an OnTouchListener and receives events when the user taps down and releases a tap
   * from a cell, then it determines if they've clicked, double clicked, long clicked or done
   * nothing (doing nothing is unsupported right now because MIN_CLICK_DURATION is zero), then
   * forwards that on to one of its methods that should be overridden
   */
  private abstract class CellTouchListener implements View.OnTouchListener {

    // The last time the user tapped
    private long lastDownTime = -1;

    /**
     * Called when the user performs a tap action on a cell, including a "up" (user let go) event
     *
     * @param view  the view that the user clicked on, expected to be a TabularView
     * @param event The type of action that the user performed
     * @return whether we could handle the event or not.
     */
    @Override
    public boolean onTouch(View view, MotionEvent event) {
      // Get where the user tapped out of the event
      int x = Float.valueOf(event.getX()).intValue();
      int y = Float.valueOf(event.getY()).intValue();
      // Figure out which cell it was that they tapped on, and put it in a CellInfo object
      CellInfo cellId = null;
      if (view instanceof TabularView) {
        cellId = ((TabularView) view).getCellInfo(x, y);
        if (cellId == null) {
          return false;
        }
      } else {
        WebLogger.getLogger(table.getAppName()).e(TAG, "Unexpected view type!");
      }
      // Figure out what action the user took and call the correct helper method
      long duration = event.getEventTime() - event.getDownTime();
      if (event.getAction() == MotionEvent.ACTION_UP && duration >= MIN_CLICK_DURATION) {
        if (event.getEventTime() - lastDownTime < MAX_DOUBLE_CLICK_TIME) {
          takeDoubleClickAction(Float.valueOf(event.getRawX()).intValue(),
              Float.valueOf(event.getRawY()).intValue());
        } else if (duration < MIN_LONG_CLICK_DURATION) {
          takeClickAction();
        } else {
          // rawX and rawY are taken from the ending of the long click, not the beginning
          // but rawX and rawY are unused anyways
          int rawX = Float.valueOf(event.getRawX()).intValue();
          int rawY = Float.valueOf(event.getRawY()).intValue();
          takeLongClickAction(rawX, rawY);
        }
        lastDownTime = event.getDownTime();
        return true;
      } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
        // cellId might be null!
        takeDownAction(cellId);
        return true;
      } else {
        return false;
      }
    }

    protected abstract void takeDownAction(CellInfo cellId);

    protected abstract void takeClickAction();

    protected abstract void takeLongClickAction(int rawX, int rawY);

    protected abstract void takeDoubleClickAction(int rawX, int rawY);
  }
}
