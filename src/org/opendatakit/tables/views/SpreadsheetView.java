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

import org.opendatakit.tables.data.ColorRuleGroup;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.KeyValueHelper;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.Preferences;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.views.components.LockableHorizontalScrollView;
import org.opendatakit.tables.views.components.LockableScrollView;

import android.content.Context;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

/**
 * A view similar to a spreadsheet. Builds TabularViews for the header, body,
 * and footer (builds two sets of these if a column is frozen to the left).
 * <p>
 * SS: I made some changes to this to try and make scrolling
 * more efficient. I am leaving some of the seemingly unreferenced and now
 * unnecessary methods/fields in case changes someone has made to this class
 * in parallel rely on these changes.
 * @author sudar.sam@gmail.com
 * @author unknown
 */
public class SpreadsheetView extends LinearLayout
        implements TabularView.Controller {

  private static final String TAG = "SpreadsheetView";

  // moved this from the old TableViewSettings
  public static final int DEFAULT_COL_WIDTH = 125;

  /******************************
   * These are constants needed for the key value store.
   ******************************/
  public static final String KVS_PARTITION = "SpreadsheetView";
  public static final String KVS_ASPECT_DEFAULT = "default";
  // So this key should go into the column aspect, b/c it is a column key value
  // entry that needs to be associated with a single column, but also needs
  // to have this naming convention to avoid namespace collisions.
  public static final String KEY_COLUMN_WIDTH = "SpreadsheetView.columnWidth";
  public static final String DEFAULT_KEY_COLUMN_WIDTHS =
      Integer.toString(DEFAULT_COL_WIDTH);

    private static final int MIN_CLICK_DURATION = 0;
    private static final int MIN_LONG_CLICK_DURATION = 1000;

    private final Context context;
    private final Controller controller;
    private final UserTable table;
    private final int indexedCol;
    private final int fontSize;

    private final Map<String, ColumnProperties> mElementKeyToProperties;
    private final Map<String, ColorRuleGroup> mElementKeyToColorRuleGroup;

    private final TableProperties tp;

    // Keeping this for now in case someone else needs to work with the code
    // and relied on this variable.
    private LockableScrollView dataScroll;
    private LockableScrollView dataStatusScroll;
    private View wrapper;
    private HorizontalScrollView wrapScroll;

    private LockableScrollView indexScroll;
    private LockableScrollView mainScroll;
    private TabularView indexData;
    private TabularView indexHeader;
    private TabularView indexFooter;
    private TabularView mainData;
    private TabularView mainHeader;
    private TabularView mainFooter;

    private View.OnTouchListener mainDataCellClickListener;
    private View.OnTouchListener mainHeaderCellClickListener;
    private View.OnTouchListener mainFooterCellClickListener;
    private View.OnTouchListener indexDataCellClickListener;
    private View.OnTouchListener indexHeaderCellClickListener;
    private View.OnTouchListener indexFooterCellClickListener;

    private int lastLongClickedCellId;

    public SpreadsheetView(Context context, Controller controller,
            TableProperties tp, UserTable table, int indexedCol) {
        super(context);
        this.context = context;
        this.controller = controller;
        this.tp = tp;
        this.table = table;
        this.indexedCol = indexedCol;

        // We have to initialize the items that will be shared across the
        // TabularView objects.
        this.mElementKeyToColorRuleGroup =
            new HashMap<String, ColorRuleGroup>();
        this.mElementKeyToProperties =
            new HashMap<String, ColumnProperties>();
        for (int i = 0; i < tp.getNumberOfDisplayColumns(); i++) {
          ColumnProperties cp = tp.getColumnByIndex(i);
          mElementKeyToColorRuleGroup.put(cp.getElementKey(),
              ColorRuleGroup.getColumnColorRuleGroup(tp, cp.getElementKey()));
          mElementKeyToProperties.put(cp.getElementKey(),cp);
        }

        // if a custom font size is defined in the KeyValueStore, use that
        // if not, use the general font size defined in preferences
        KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper("SpreadsheetView");
        if (kvsh.getInteger("fontSize") == null)
        	fontSize = (new Preferences(context)).getFontSize();
        else
        	fontSize = kvsh.getInteger("fontSize");

        initListeners();
        if (indexedCol < 0) {
            buildNonIndexedTable();
        } else {
            buildIndexedTable(indexedCol);
            indexData.setOnTouchListener(indexDataCellClickListener);
            indexHeader.setOnTouchListener(indexHeaderCellClickListener);
            indexFooter.setOnTouchListener(indexFooterCellClickListener);
        }
        mainData.setOnTouchListener(mainDataCellClickListener);
        mainHeader.setOnTouchListener(mainHeaderCellClickListener);
        mainFooter.setOnTouchListener(mainFooterCellClickListener);
    }

    /**
     * Initializes the click listeners.
     */
    private void initListeners() {
        mainDataCellClickListener = new CellTouchListener() {
            @Override
            protected int figureCellId(int x, int y) {
                int cellNum = mainData.getCellNumber(x, y);
//                Log.d(TAG, "mainDataCellClickListener cellId: " + cellNum);
                if (indexedCol < 0) {
                    return cellNum;
                } else {
                  return cellNum;
                }
            }
            @Override
            protected void takeDownAction(int cellId) {
              if (indexedCol >= 0) {
                indexData.highlight(-1);
              }
                mainData.highlight(cellId);
            }
            @Override
            protected void takeClickAction(int cellId) {
                controller.regularCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId, int rawX,
                    int rawY) {
              boolean isIndexed = indexedCol >= 0;
                lastLongClickedCellId = cellId;
                controller.regularCellLongClicked(cellId, rawX, rawY,
                    isIndexed);
            }
            @Override
            protected void takeDoubleClickAction(int cellId, int rawX,
                int rawY) {
              boolean isIndexed = indexedCol >= 0;
                controller.regularCellDoubleClicked(cellId, isIndexed,
                    rawX, rawY);
            }
        };
        mainHeaderCellClickListener = new CellTouchListener() {
            @Override
            protected int figureCellId(int x, int y) {
                int cellNum = mainHeader.getCellNumber(x, y);
                if (indexedCol < 0) {
                    return cellNum;
                } else {
                    int colNum = cellNum % (table.getWidth() - 1);
                    return cellNum + ((colNum < indexedCol) ? 0 : 1);
                }
            }
            @Override
            protected void takeDownAction(int cellId) {}
            @Override
            protected void takeClickAction(int cellId) {
                mainData.highlight(-1);
                controller.headerCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId, int rawX,
                    int rawY) {
                lastLongClickedCellId = cellId;
                controller.openContextMenu(mainHeader);
            }
            /**
             * Make this do the same thing as a long click.
             */
            @Override
            protected void takeDoubleClickAction(int cellId, int rawX,
                int rawY) {
              takeLongClickAction(cellId, rawX, rawY);
            }
        };
        mainFooterCellClickListener = new CellTouchListener() {
            @Override
            protected int figureCellId(int x, int y) {
                int cellNum = mainFooter.getCellNumber(x, y);
                if (indexedCol < 0) {
                    return cellNum;
                } else {
                    int colNum = cellNum % (table.getWidth() - 1);
                    return cellNum + ((colNum < indexedCol) ? 0 : 1);
                }
            }
            @Override
            protected void takeDownAction(int cellId) {}
            @Override
            protected void takeClickAction(int cellId) {
                mainData.highlight(-1);
                controller.footerCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId, int rawX,
                    int rawY) {
                lastLongClickedCellId = cellId;
                controller.openContextMenu(mainFooter);
            }
            /**
             * Make this do the same thing as long press.
             */
            @Override
            protected void takeDoubleClickAction(int cellId, int rawX,
                int rawY) {
              takeLongClickAction(cellId, rawX, rawY);
            }
        };
        indexDataCellClickListener = new CellTouchListener() {
            @Override
            protected int figureCellId(int x, int y) {
                int cellNum = indexData.getCellNumber(x, y);
//                Log.d(TAG, "indexDataCellClickListener cellNum: " + cellNum);
                return cellNum;
            }
            @Override
            protected void takeDownAction(int cellId) {
              mainData.highlight(-1);
              indexData.highlight(cellId);
            }
            @Override
            protected void takeClickAction(int cellId) {
                mainData.highlight(-1);
                controller.indexedColCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId, int rawX,
                    int rawY) {
              // TODO: THIS IS STORING THE WRONG CELLID
                lastLongClickedCellId = cellId;
                controller.indexedColCellLongClicked(cellId, rawX, rawY);
            }
            @Override
            protected void takeDoubleClickAction(int cellId, int rawX,
                int rawY) {
              controller.indexedColCellDoubleClicked(cellId, rawX, rawY);
            }
        };
        indexHeaderCellClickListener = new CellTouchListener() {
            @Override
            protected int figureCellId(int x, int y) {
                return indexedCol;
            }
            @Override
            protected void takeDownAction(int cellId) {}
            @Override
            protected void takeClickAction(int cellId) {
                mainData.highlight(-1);
                controller.headerCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId, int rawX,
                    int rawY) {
                lastLongClickedCellId = cellId;
                controller.openContextMenu(indexHeader);
            }
            /**
             * Do the same thing as a long click.
             */
            @Override
            protected void takeDoubleClickAction(int cellId, int rawX,
                int rawY) {
              takeLongClickAction(cellId, rawX, rawY);
            }
        };
        indexFooterCellClickListener = new CellTouchListener() {
            @Override
            protected int figureCellId(int x, int y) {
                return indexedCol;
            }
            @Override
            protected void takeDownAction(int cellId) {}
            @Override
            protected void takeClickAction(int cellId) {
                mainData.highlight(-1);
                controller.footerCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId, int rawX,
                    int rawY) {
                lastLongClickedCellId = cellId;
                controller.openContextMenu(indexFooter);
            }
            /**
             * Do the same thing as a long click.
             */
            @Override
            protected void takeDoubleClickAction(int cellId, int rawX,
                int rawY) {
              takeLongClickAction(cellId, rawX, rawY);
            }
        };
    }

    private void buildNonIndexedTable() {
		wrapper = buildTable(-1, false);
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
            dataStatusScroll.scrollTo(dataStatusScroll.getScrollX(),
                view.getScrollY());
            if (event.getAction() == MotionEvent.ACTION_UP) {
              mainScroll.startScrollerTask();
            }
            return false;
        }
    });
    }

    private void buildIndexedTable(int indexedCol) {
        View mainWrapper = buildTable(indexedCol, false);
        View indexWrapper = buildTable(indexedCol, true);
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
                mainScroll.scrollTo(mainScroll.getScrollX(),
                        view.getScrollY());
                dataStatusScroll.scrollTo(mainScroll.getScrollX(),
                    view.getScrollY());
                if (event.getAction() == MotionEvent.ACTION_UP) {
                  indexScroll.startScrollerTask();
                  mainScroll.startScrollerTask();
                }
                return false;
            }
        });
        indexScroll.setOnScrollStoppedListener(new
            LockableScrollView.OnScrollStoppedListener() {

              @Override
              public void onScrollStopped() {
//                Log.i(TAG, "stopped in onStopped of indexScroll");
              }
            });
        mainScroll.setOnScrollStoppedListener(new
            LockableScrollView.OnScrollStoppedListener() {

              @Override
              public void onScrollStopped() {
//                Log.i(TAG, "stopped in onStopped of mainScroll");

              }
            });
        mainScroll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                indexScroll.scrollTo(indexScroll.getScrollX(),
                        view.getScrollY());
                dataStatusScroll.scrollTo(indexScroll.getScrollX(),
                    view.getScrollY());
                if (event.getAction() == MotionEvent.ACTION_UP) {
                  indexScroll.startScrollerTask();
                  mainScroll.startScrollerTask();
                }
                return false;
            }
        });
    }

    /**
     * Builds a (piece of a) table. The table may either be the indexed column
     * of an indexed table, the non-indexed columns of an indexed table, or the
     * entirety of an unindexed table.
     * @param indexedCol the column that is indexed (or -1)
     * @param isIndexed whether this table is for the indexed column
     * @return a view including the header, body, and footer of the table
     */
    private View buildTable(int indexedCol, boolean isIndexed) {
//      Log.i(TAG, "entering buildTable. indexedCol: " + indexedCol +
//          "isIndexed: " + isIndexed);
        List<String> elementKeysToDisplay =
            new ArrayList<String>(tp.getNumberOfDisplayColumns());
        int[] colWidths;
        int[] completeColWidths = getColumnWidths();
        TabularView dataTable;
        TabularView headerTable;
        TabularView footerTable;
        if (isIndexed) {
        	ColumnProperties cp = tp.getColumnByIndex(indexedCol);
            elementKeysToDisplay.add(cp.getElementKey());
            colWidths = new int[1];
            colWidths[0] = completeColWidths[indexedCol];
            dataTable = TabularView.getIndexDataTable(context, this,
                tp, table, elementKeysToDisplay, colWidths, fontSize,
                this.mElementKeyToProperties,
                this.mElementKeyToColorRuleGroup);
            headerTable = TabularView.getIndexHeaderTable(context,
                this, tp, table, elementKeysToDisplay, colWidths, fontSize,
                this.mElementKeyToProperties,
                this.mElementKeyToColorRuleGroup);
            footerTable = TabularView.getIndexFooterTable(context,
                this, tp, table, elementKeysToDisplay, colWidths, fontSize,
                this.mElementKeyToProperties,
                this.mElementKeyToColorRuleGroup);
        } else {
            int width = (indexedCol < 0) ? table.getWidth() :
                table.getWidth() - 1;
            colWidths = new int[width];
            int addIndex = 0;
            for (int i = 0; i < table.getWidth(); i++) {
                if (i == indexedCol) {
                    continue;
                }
                ColumnProperties cp = tp.getColumnByIndex(i);
                elementKeysToDisplay.add(cp.getElementKey());
                colWidths[addIndex] = completeColWidths[i];
                addIndex++;
            }
            dataTable = TabularView.getMainDataTable(context, this,
                tp, table, elementKeysToDisplay, colWidths, fontSize,
                this.mElementKeyToProperties,
                this.mElementKeyToColorRuleGroup);
            headerTable = TabularView.getMainHeaderTable(context,
                this, tp, table, elementKeysToDisplay, colWidths, fontSize,
                this.mElementKeyToProperties,
                this.mElementKeyToColorRuleGroup);
            footerTable = TabularView.getMainFooterTable(context,
                this, tp, table, elementKeysToDisplay, colWidths, fontSize,
                this.mElementKeyToProperties,
                this.mElementKeyToColorRuleGroup);
        }
        dataScroll = new LockableScrollView(context);
        dataScroll.addView(dataTable, new ViewGroup.LayoutParams(
                dataTable.getTableWidth(), dataTable.getTableHeight()));
        dataScroll.setVerticalFadingEdgeEnabled(true);
        dataScroll.setHorizontalFadingEdgeEnabled(true);
        if (isIndexed) {
            indexData = dataTable;
            indexHeader = headerTable;
            indexFooter = footerTable;
            indexScroll = dataScroll;
        } else {
            mainData = dataTable;
            mainHeader = headerTable;
            mainFooter = footerTable;
            mainScroll = dataScroll;
        }
        LinearLayout wrapper = new LinearLayout(context);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(headerTable, headerTable.getTableWidth(),
                headerTable.getTableHeight());
        LinearLayout.LayoutParams dataLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        dataLp.weight = 1;
        wrapper.addView(dataScroll, dataLp);
        wrapper.addView(footerTable, footerTable.getTableWidth(),
                footerTable.getTableHeight());
        return wrapper;
    }


    private View buildStatusTable() {
        int[] colWidths;
        List<String> dummyHeaderElementKeys = new ArrayList<String>();
        dummyHeaderElementKeys.add("header");
        List<String> dummyDataElementKeys = new ArrayList<String>();
        dummyDataElementKeys.add("data");
        List<String> dummyFooterElementKeys = new ArrayList<String>();
        dummyFooterElementKeys.add("footer");
        colWidths = new int[1];
        colWidths[0] = TabularView.DEFAULT_STATUS_COLUMN_WIDTH;

        dataStatusScroll = new LockableScrollView(context);
        TabularView dataTable = TabularView.getStatusDataTable(context,
            this, tp, table, colWidths, fontSize,
            this.mElementKeyToProperties, this.mElementKeyToColorRuleGroup);
        dataTable.setVerticalFadingEdgeEnabled(true);
        dataTable.setVerticalScrollBarEnabled(false);
        dataStatusScroll.addView(dataTable, new ViewGroup.LayoutParams(
                dataTable.getTableWidth(), dataTable.getTableHeight()));
        dataStatusScroll.setVerticalFadingEdgeEnabled(true);
        dataStatusScroll.setHorizontalFadingEdgeEnabled(true);
        TabularView headerTable = TabularView.getStatusHeaderTable(context,
            this, tp, table, colWidths, fontSize,
            this.mElementKeyToProperties, this.mElementKeyToColorRuleGroup);
        TabularView footerTable = TabularView.getStatusFooterTable(context,
            this, tp, table, colWidths, fontSize,
            this.mElementKeyToProperties, this.mElementKeyToColorRuleGroup);
        LinearLayout wrapper = new LinearLayout(context);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(headerTable, headerTable.getTableWidth(),
                headerTable.getTableHeight());
        LinearLayout.LayoutParams dataLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        dataLp.weight = 1;
        wrapper.addView(dataStatusScroll, dataLp);
        wrapper.addView(footerTable, footerTable.getTableWidth(),
                footerTable.getTableHeight());
        wrapper.setVerticalFadingEdgeEnabled(true);
        wrapper.setHorizontalFadingEdgeEnabled(true);
        return wrapper;
    }

    /**
     * Gets the x translation of the scroll. This is in particular how far
     * you have scrolled to look at columns that do not begin onscreen.
     * @return
     */
    @Override
    public int getMainScrollX() {
      // this is getting the correct x
      int result = this.wrapScroll.getScrollX();
      return result;
    }

    /**
     * Get the column widths for the table. The values in the array match the
     * order specified in {@link TableProperties.getColumns}.
     * @return
     */
    public int[] getColumnWidths() {
      // So what we want to do is go through and get the column widths for each
      // column. A problem here is that there is no caching, and if you have a
      // lot of columns you're really working the gut of the database.
      return SpreadsheetView.getColumnWidths(tp);
    }

    /**
     * Gets the y translation of the scroll. This is in particular the y
     * offset for the actual scrolling of the rows, so that a positive
     * offset will indicate that you have scrolled to some non-zero row.
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
        controller.prepRegularCellOccm(menu, lastLongClickedCellId);
    }

    @Override
    public void onCreateIndexDataContextMenu(ContextMenu menu) {
        controller.prepIndexedColCellOccm(menu, lastLongClickedCellId);
    }

    @Override
    public void onCreateHeaderContextMenu(ContextMenu menu) {
        controller.prepHeaderCellOccm(menu, lastLongClickedCellId);
    }

    @Override
    public void onCreateFooterContextMenu(ContextMenu menu) {
        controller.prepFooterCellOccm(menu, lastLongClickedCellId);
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
            int cellId = figureCellId(x, y);
            long duration = event.getEventTime() - event.getDownTime();
            if (event.getAction() == MotionEvent.ACTION_UP &&
                    duration >= MIN_CLICK_DURATION) {
                if (event.getEventTime() - lastDownTime <
                        MAX_DOUBLE_CLICK_TIME) {
                    takeDoubleClickAction(cellId,
                        (Float.valueOf(event.getRawX())).intValue(),
                        (Float.valueOf(event.getRawY())).intValue());
                } else if (duration < MIN_LONG_CLICK_DURATION) {
                    takeClickAction(cellId);
                } else {
                    int rawX = (Float.valueOf(event.getRawX())).intValue();
                    int rawY = (Float.valueOf(event.getRawY())).intValue();
                    takeLongClickAction(cellId, rawX, rawY);
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

        protected abstract int figureCellId(int x, int y);

        protected abstract void takeDownAction(int cellId);

        protected abstract void takeClickAction(int cellId);

        protected abstract void takeLongClickAction(int cellId, int rawX,
                int rawY);

        protected abstract void takeDoubleClickAction(int cellId, int rawX,
            int rawY);
    }

    public interface Controller {

        public void regularCellClicked(int cellId);

        public void headerCellClicked(int cellId);

        public void footerCellClicked(int cellId);

        public void indexedColCellClicked(int cellId);

        public void regularCellLongClicked(int cellId, int rawX, int rawY,
            boolean isIndexed);

        public void regularCellDoubleClicked(int cellId, boolean isIndexed,
            int rawX, int rawY);

        public void indexedColCellDoubleClicked(int cellId, int rawX,
            int rawY);

        public void indexedColCellLongClicked(int cellId, int rawX, int rawY);

        public void openContextMenu(View view);

        public void prepRegularCellOccm(ContextMenu menu, int cellId);

        public void prepHeaderCellOccm(ContextMenu menu, int cellId);

        public void prepFooterCellOccm(ContextMenu menu, int cellId);

        public void prepIndexedColCellOccm(ContextMenu menu, int cellId);

    }

    /**
     * Get the column widths for the table. The values in the array match the
     * order specified in {@link TableProperties.getColumns}.
     * <p>
     * NB: If getting this from outside of spreadsheet view, you should really
     * consider if you need to be accessing column widths.
     * @return
     */
    public static int[] getColumnWidths(TableProperties tp) {
      // So what we want to do is go through and get the column widths for each
      // column. A problem here is that there is no caching, and if you have a
      // lot of columns you're really working the gut of the database.
      int numberOfDisplayColumns = tp.getNumberOfDisplayColumns();
      int[] columnWidths = new int[numberOfDisplayColumns];
      KeyValueStoreHelper columnKVSH =
          tp.getKeyValueStoreHelper(ColumnProperties.KVS_PARTITION);
      for (int i = 0; i < numberOfDisplayColumns; i++) {
    	ColumnProperties cp = tp.getColumnByIndex(i);
        String elementKey = cp.getElementKey();
        KeyValueHelper aspectHelper = columnKVSH.getAspectHelper(elementKey);
        Integer value =
            aspectHelper.getInteger(SpreadsheetView.KEY_COLUMN_WIDTH);
        if (value == null) {
          columnWidths[i] = DEFAULT_COL_WIDTH;
        } else {
          columnWidths[i] = value;
        }
      }
      return columnWidths;
    }
}
