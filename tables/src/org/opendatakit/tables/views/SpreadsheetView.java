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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.aggregate.odktables.rest.KeyValueStoreConstants;
import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.KeyValueHelper;
import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.Preferences;
import org.opendatakit.tables.views.components.LockableHorizontalScrollView;
import org.opendatakit.tables.views.components.LockableScrollView;

import android.content.Context;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

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

  private static final String TAG = "SpreadsheetView";

  // moved this from the old TableViewSettings
  public static final int DEFAULT_COL_WIDTH = 125;

  public static final int MAX_COL_WIDTH = 1000;

  /******************************
   * These are constants needed for the key value store.
   ******************************/
  public static final String KVS_PARTITION = "SpreadsheetView";
  public static final String KVS_ASPECT_DEFAULT = "default";
  // So this key should go into the column aspect, b/c it is a column key value
  // entry that needs to be associated with a single column, but also needs
  // to have this naming convention to avoid namespace collisions.
  public static final String KEY_COLUMN_WIDTH = "SpreadsheetView.columnWidth";
  public static final String DEFAULT_KEY_COLUMN_WIDTHS = Integer.toString(DEFAULT_COL_WIDTH);

  private static final int MIN_CLICK_DURATION = 0;
  private static final int MIN_LONG_CLICK_DURATION = 1000;

  private final Context context;
  private final Controller controller;
  private final SpreadsheetUserTable table;
  private final int fontSize;

  private final Map<String, ColorRuleGroup> mElementKeyToColorRuleGroup;

  // Keeping this for now in case someone else needs to work with the code
  // and relied on this variable.
  private LockableScrollView dataStatusScroll;
  private View wrapper;
  private HorizontalScrollView wrapScroll;

  private LockableScrollView indexScroll;
  private LockableScrollView mainScroll;
  private TabularView indexData;
  private TabularView indexHeader;
  private TabularView mainData;
  private TabularView mainHeader;

  private View.OnTouchListener mainDataCellClickListener;
  private View.OnTouchListener mainHeaderCellClickListener;
  private View.OnTouchListener indexDataCellClickListener;
  private View.OnTouchListener indexHeaderCellClickListener;

  private CellInfo lastHighlightedCellId;

  public SpreadsheetView(Context context, Controller controller, SpreadsheetUserTable table) {
    super(context);
    this.context = context;
    this.controller = controller;
    this.table = table;

    // TODO: figure out if we can invalidate a screen region
    // to get it to render the screen rather than
    // disabling the hardware acceleration on this view.
    // Disable it so that you don't have to tap the screen to
    // after a scroll action to see the new portion of the
    // spreadsheet.
    this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

    // We have to initialize the items that will be shared across the
    // TabularView objects.
    this.mElementKeyToColorRuleGroup = new HashMap<String, ColorRuleGroup>();
    for (ColumnDefinition cd : table.getColumnDefinitions()) {
      mElementKeyToColorRuleGroup.put(cd.getElementKey(),
          table.getColumnColorRuleGroup(cd.getElementKey()));
    }

    // if a custom font size is defined in the KeyValueStore, use that
    // if not, use the general font size defined in preferences
    KeyValueStoreHelper kvsh = table.getKeyValueStoreHelper("SpreadsheetView");
    if (kvsh.getInteger("fontSize") == null)
      fontSize = (new Preferences(context, table.getAppName())).getFontSize();
    else
      fontSize = kvsh.getInteger("fontSize");

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
   * Initializes the click listeners.
   */
  private void initListeners() {
    // The logic here is a bit crazy.
    // header and data click listeners both receive the full
    //
    mainDataCellClickListener = new CellTouchListener() {
      @Override
      protected void takeDownAction(CellInfo cellId) {
        if (table.isIndexed()) {
          indexData.highlight(null);
        }
        lastHighlightedCellId = cellId;
        mainData.highlight(cellId);
      }

      @Override
      protected void takeClickAction() {
        controller.dataCellClicked(lastHighlightedCellId);
      }

      @Override
      protected void takeLongClickAction(int rawX, int rawY) {
        controller.openDataContextMenu(mainData);
      }

      @Override
      protected void takeDoubleClickAction(int rawX, int rawY) {
        takeLongClickAction(rawX, rawY);
      }
    };
    mainHeaderCellClickListener = new CellTouchListener() {
      @Override
      protected void takeDownAction(CellInfo cellId) {
        if (table.isIndexed()) {
          indexData.highlight(null);
        }
        lastHighlightedCellId = cellId;
        mainData.highlight(null);
      }

      @Override
      protected void takeClickAction() {
        controller.headerCellClicked(lastHighlightedCellId);
      }

      @Override
      protected void takeLongClickAction(int rawX, int rawY) {
        controller.openHeaderContextMenu(mainHeader);
      }

      /**
       * Make this do the same thing as a long click.
       */
      @Override
      protected void takeDoubleClickAction(int rawX, int rawY) {
        takeLongClickAction(rawX, rawY);
      }
    };
    indexDataCellClickListener = new CellTouchListener() {
      @Override
      protected void takeDownAction(CellInfo cellId) {
        mainData.highlight(null);
        lastHighlightedCellId = cellId;
        indexData.highlight(cellId);
      }

      @Override
      protected void takeClickAction() {
        controller.dataCellClicked(lastHighlightedCellId);
      }

      @Override
      protected void takeLongClickAction(int rawX, int rawY) {
        controller.openDataContextMenu(indexData);
      }

      @Override
      protected void takeDoubleClickAction(int rawX, int rawY) {
        takeLongClickAction(rawX, rawY);
      }
    };
    indexHeaderCellClickListener = new CellTouchListener() {
      @Override
      protected void takeDownAction(CellInfo cellId) {
        mainData.highlight(null);
        indexData.highlight(null);
        lastHighlightedCellId = cellId;
      }

      @Override
      protected void takeClickAction() {
        controller.headerCellClicked(lastHighlightedCellId);
      }

      @Override
      protected void takeLongClickAction(int rawX, int rawY) {
        controller.openHeaderContextMenu(indexHeader);
      }

      /**
       * Do the same thing as a long click.
       */
      @Override
      protected void takeDoubleClickAction(int rawX, int rawY) {
        takeLongClickAction(rawX, rawY);
      }
    };
  }

  private void buildNonIndexedTable() {
    wrapper = buildTable(null, false);
    wrapScroll = new HorizontalScrollView(context);
    wrapScroll.addView(wrapper, LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.MATCH_PARENT);
    /*** this was all here before ***/
    LinearLayout.LayoutParams wrapLp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
    wrapLp.weight = 1;
    wrapScroll.setHorizontalFadingEdgeEnabled(true); // works

    LinearLayout completeWrapper = new LinearLayout(context);
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
        if (event.getAction() == MotionEvent.ACTION_UP) {
          mainScroll.startScrollerTask();
        }
        return false;
      }
    });
  }

  private void buildIndexedTable() {
    String indexElementKey = table.getIndexedColumnElementKey();
    View mainWrapper = buildTable(indexElementKey, false);
    View indexWrapper = buildTable(indexElementKey, true);
    wrapScroll = new LockableHorizontalScrollView(context);
    wrapScroll.addView(mainWrapper, LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.MATCH_PARENT);
    wrapScroll.setHorizontalFadingEdgeEnabled(true);
    LinearLayout wrapper = new LinearLayout(context);
    wrapper.addView(indexWrapper);
    wrapper.addView(wrapScroll);

    LinearLayout completeWrapper = new LinearLayout(context);
    View statusWrapper = buildStatusTable();
    completeWrapper.addView(statusWrapper);
    completeWrapper.addView(wrapper);

    addView(completeWrapper);

    indexScroll.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent event) {
        mainScroll.scrollTo(mainScroll.getScrollX(), view.getScrollY());
        dataStatusScroll.scrollTo(mainScroll.getScrollX(), view.getScrollY());
        if (event.getAction() == MotionEvent.ACTION_UP) {
          indexScroll.startScrollerTask();
          mainScroll.startScrollerTask();
        }
        return false;
      }
    });
    indexScroll.setOnScrollStoppedListener(new LockableScrollView.OnScrollStoppedListener() {

      @Override
      public void onScrollStopped() {
        // Log.i(TAG, "stopped in onStopped of indexScroll");
      }
    });
    mainScroll.setOnScrollStoppedListener(new LockableScrollView.OnScrollStoppedListener() {

      @Override
      public void onScrollStopped() {
        // Log.i(TAG, "stopped in onStopped of mainScroll");

      }
    });
    mainScroll.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent event) {
        indexScroll.scrollTo(indexScroll.getScrollX(), view.getScrollY());
        dataStatusScroll.scrollTo(indexScroll.getScrollX(), view.getScrollY());
        if (event.getAction() == MotionEvent.ACTION_UP) {
          indexScroll.startScrollerTask();
          mainScroll.startScrollerTask();
        }
        return false;
      }
    });
  }

  /**
   * Builds a (piece of a) table. The table may either be the indexed column of
   * an indexed table, the non-indexed columns of an indexed table, or the
   * entirety of an unindexed table.
   *
   * @param indexElementKey
   *          the column that is indexed (or null)
   * @param isIndexed
   *          whether this table is for the indexed column
   * @return a view including the header and body of the table
   */
  private View buildTable(String indexElementKey, boolean isIndexed) {
    // Log.i(TAG, "entering buildTable. indexedCol: " + indexedCol +
    // "isIndexed: " + isIndexed);
    List<String> elementKeysToDisplay = new ArrayList<String>();
    int[] colWidths;
    int[] completeColWidths = getColumnWidths();
    TabularView dataTable;
    TabularView headerTable;
    if (isIndexed) {
      ColumnDefinition cd = table.getColumnByElementKey(indexElementKey);
      elementKeysToDisplay.add(cd.getElementKey());
      colWidths = new int[1];
      colWidths[0] = completeColWidths[table.getColumnIndexOfElementKey(indexElementKey)];
      dataTable = TabularView.getIndexDataTable(context, this, table, elementKeysToDisplay,
          colWidths, fontSize, this.mElementKeyToColorRuleGroup);
      headerTable = TabularView.getIndexHeaderTable(context, this, table, elementKeysToDisplay,
          colWidths, fontSize, this.mElementKeyToColorRuleGroup);
    } else {
      int width = (indexElementKey == null || indexElementKey.length() == 0) ? table.getWidth() : table.getWidth() - 1;
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
      dataTable = TabularView.getMainDataTable(context, this, table, elementKeysToDisplay,
          colWidths, fontSize, this.mElementKeyToColorRuleGroup);
      headerTable = TabularView.getMainHeaderTable(context, this, table, elementKeysToDisplay,
          colWidths, fontSize, this.mElementKeyToColorRuleGroup);
    }

    LockableScrollView dataScroll;
    dataScroll = new LockableScrollView(context);
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

  private View buildStatusTable() {
    int[] colWidths;
    colWidths = new int[1];
    colWidths[0] = TabularView.DEFAULT_STATUS_COLUMN_WIDTH;

    dataStatusScroll = new LockableScrollView(context);
    TabularView dataTable = TabularView.getStatusDataTable(context, this, table, colWidths,
        fontSize, this.mElementKeyToColorRuleGroup);
    dataTable.setVerticalFadingEdgeEnabled(true);
    dataTable.setVerticalScrollBarEnabled(false);
    dataStatusScroll.addView(dataTable, new ViewGroup.LayoutParams(dataTable.getTableWidth(),
        dataTable.getTableHeight()));
    dataStatusScroll.setVerticalFadingEdgeEnabled(true);
    dataStatusScroll.setHorizontalFadingEdgeEnabled(true);
    TabularView headerTable = TabularView.getStatusHeaderTable(context, this, table, colWidths,
        fontSize, this.mElementKeyToColorRuleGroup);
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
   * @return
   */
  @Override
  public int getMainScrollX() {
    // this is getting the correct x
    int result = this.wrapScroll.getScrollX();
    return result;
  }

  /**
   * Gets the y translation of the scroll. This is in particular the y offset
   * for the actual scrolling of the rows, so that a positive offset will
   * indicate that you have scrolled to some non-zero row.
   *
   * @return
   */
  @Override
  public int getMainScrollY() {
    // this is getting the correct y
    int result = this.mainScroll.getScrollY();
    return result;
  }

  @Override
  public void onCreateMainDataContextMenu(ContextMenu menu) {
    controller.prepDataCellOccm(menu, lastHighlightedCellId);
  }

  @Override
  public void onCreateIndexDataContextMenu(ContextMenu menu) {
    controller.prepDataCellOccm(menu, lastHighlightedCellId);
  }

  @Override
  public void onCreateHeaderContextMenu(ContextMenu menu) {
    controller.prepHeaderCellOccm(menu, lastHighlightedCellId);
  }

  private abstract class CellTouchListener implements View.OnTouchListener {

    private static final int MAX_DOUBLE_CLICK_TIME = 500;

    private long lastDownTime;

    public CellTouchListener() {
      lastDownTime = -1;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
      int x = (Float.valueOf(event.getX())).intValue();
      int y = (Float.valueOf(event.getY())).intValue();
      CellInfo cellId = null;
      if (view instanceof TabularView) {
        cellId = ((TabularView) view).getCellInfo(x, y);
      } else {
        Log.e(TAG, "Unexpected view type!");
      }
      long duration = event.getEventTime() - event.getDownTime();
      if (event.getAction() == MotionEvent.ACTION_UP && duration >= MIN_CLICK_DURATION) {
        if (event.getEventTime() - lastDownTime < MAX_DOUBLE_CLICK_TIME) {
          takeDoubleClickAction((Float.valueOf(event.getRawX())).intValue(),
              (Float.valueOf(event.getRawY())).intValue());
        } else if (duration < MIN_LONG_CLICK_DURATION) {
          takeClickAction();
        } else {
          int rawX = (Float.valueOf(event.getRawX())).intValue();
          int rawY = (Float.valueOf(event.getRawY())).intValue();
          takeLongClickAction(rawX, rawY);
        }
        lastDownTime = event.getDownTime();
        return true;
      } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
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

  public interface Controller {

    public void headerCellClicked(CellInfo cellId);

    public void prepHeaderCellOccm(ContextMenu menu, CellInfo cellId);

    public void openHeaderContextMenu(View view);

    public void dataCellClicked(CellInfo cellId);

    public void prepDataCellOccm(ContextMenu menu, CellInfo cellId);

    public void openDataContextMenu(View view);
  }

  /**
   * Get the column widths for the table. The values in the array match the
   * order specified in the column order.
   * <p>
   * NB: If getting this from outside of spreadsheet view, you should really
   * consider if you need to be accessing column widths.
   *
   * @return
   */
  public int[] getColumnWidths() {
    // So what we want to do is go through and get the column widths for each
    // column. A problem here is that there is no caching, and if you have a
    // lot of columns you're really working the gut of the database.
    int numberOfDisplayColumns = table.getNumberOfDisplayColumns();
    int[] columnWidths = new int[numberOfDisplayColumns];
    KeyValueStoreHelper columnKVSH = table.getKeyValueStoreHelper(KeyValueStoreConstants.PARTITION_COLUMN);
    for (int i = 0; i < numberOfDisplayColumns; i++) {
      ColumnDefinition cd = table.getColumnByIndex(i);
      String elementKey = cd.getElementKey();
      KeyValueHelper aspectHelper = columnKVSH.getAspectHelper(elementKey);
      Integer value = aspectHelper.getInteger(SpreadsheetView.KEY_COLUMN_WIDTH);
      if (value == null) {
        columnWidths[i] = DEFAULT_COL_WIDTH;
      } else {
        columnWidths[i] = value;
      }
    }
    return columnWidths;
  }
}
