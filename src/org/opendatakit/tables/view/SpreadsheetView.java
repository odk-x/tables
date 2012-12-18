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
package org.opendatakit.tables.view;

import org.opendatakit.tables.R;
import org.opendatakit.tables.DataStructure.DisplayPrefs;
import org.opendatakit.tables.DataStructure.DisplayPrefs.ColumnColorRuler;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.Preferences;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.view.TabularView.ColorDecider;
import org.opendatakit.tables.view.TabularView.TableType;
import org.opendatakit.tables.view.util.LockableHorizontalScrollView;
import org.opendatakit.tables.view.util.LockableScrollView;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
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
    private final DisplayPrefs dp;
    private final int fontSize;
    
    private final TableProperties tp;
    
    // Keeping this for now in case someone else needs to work with the code
    // and relied on this variable.
//    private LockableHorizontalScrollView wrapScroll;
    /** trying to fix slow draw **/
    private LockableScrollView dataScroll;
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
            TableProperties tp, UserTable table, int indexedCol,
            DisplayPrefs dp) {
        super(context);
        this.context = context;
        this.controller = controller;
        this.tp = tp;
        this.table = table;
        this.indexedCol = indexedCol;
        this.dp = dp;
        fontSize = (new Preferences(context)).getFontSize();
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
                if (indexedCol < 0) {
                    return cellNum;
                } else {
                    int colNum = cellNum % (table.getWidth() - 1);
                    int rowNum = cellNum / (table.getWidth() - 1);
                    return cellNum + rowNum + ((colNum < indexedCol) ? 0 : 1);
                }
            }
            @Override
            protected void takeDownAction(int cellId) {
                mainData.highlight(cellId);
            }
            @Override
            protected void takeClickAction(int cellId) {
                controller.regularCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId, int rawX,
                    int rawY) {
                lastLongClickedCellId = cellId;
                controller.regularCellLongClicked(cellId, rawX, rawY);
            }
            @Override
            protected void takeDoubleClickAction(int cellId) {
                controller.regularCellDoubleClicked(cellId);
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
            @Override
            protected void takeDoubleClickAction(int cellId) {}
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
            @Override
            protected void takeDoubleClickAction(int cellId) {}
        };
        indexDataCellClickListener = new CellTouchListener() {
            @Override
            protected int figureCellId(int x, int y) {
                int cellNum = indexData.getCellNumber(x, y);
                return (cellNum * table.getWidth()) + indexedCol;
            }
            @Override
            protected void takeDownAction(int cellId) {}
            @Override
            protected void takeClickAction(int cellId) {
                mainData.highlight(-1);
                controller.indexedColCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId, int rawX,
                    int rawY) {
                lastLongClickedCellId = cellId;
                controller.openContextMenu(indexData);
            }
            @Override
            protected void takeDoubleClickAction(int cellId) {}
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
            @Override
            protected void takeDoubleClickAction(int cellId) {}
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
            @Override
            protected void takeDoubleClickAction(int cellId) {}
        };
    }
    
  private void buildNonIndexedTable() {
    wrapper = buildTable(-1, false);
    // Keeping this for now in case someone relied on this.
    // HorizontalScrollView wrapScroll = new HorizontalScrollView(context);
    wrapScroll = new HorizontalScrollView(context);
    wrapScroll.addView(wrapper, LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.MATCH_PARENT);
    /*** this was all here before ***/
    LinearLayout.LayoutParams wrapLp = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
    wrapLp.weight = 1;
    wrapScroll.setHorizontalFadingEdgeEnabled(true); // works
    addView(wrapScroll, wrapLp);
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
        addView(wrapper);
        indexScroll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                mainScroll.scrollTo(mainScroll.getScrollX(),
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
                Log.i(TAG, "stopped in onStopped of indexScroll");             
              }
            });
        mainScroll.setOnScrollStoppedListener(new 
            LockableScrollView.OnScrollStoppedListener() {
              
              @Override
              public void onScrollStopped() {
                Log.i(TAG, "stopped in onStopped of mainScroll");
                
              }
            });
        mainScroll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                indexScroll.scrollTo(indexScroll.getScrollX(),
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
      Log.i(TAG, "entering buildTable. indexedCol: " + indexedCol + 
          "isIndexed: " + isIndexed);
        String[][] header;
        String[][] data;
        String[][] footer;
        ColumnColorRuler[] colorRulers;
        int[] colWidths;
//        int[] completeColWidths = tvs.getTableColWidths();
        int[] completeColWidths = getColumnWidths();
        if (isIndexed) {
            header = new String[1][1];
            header[0][0] = table.getHeader(indexedCol);
            data = new String[table.getHeight()][1];
            for (int i = 0; i < table.getHeight(); i++) {
                data[i][0] = table.getData(i, indexedCol);
            }
            footer = new String[1][1];
            footer[0][0] = table.getFooter(indexedCol);
            colorRulers = new ColumnColorRuler[1];
            colorRulers[0] = dp.getColColorRuler(tp, 
                table.getHeader(indexedCol));
            colWidths = new int[1];
            colWidths[0] = completeColWidths[indexedCol];
        } else {
            int width = (indexedCol < 0) ? table.getWidth() :
                table.getWidth() - 1;
            header = new String[1][width];
            data = new String[table.getHeight()][width];
            footer = new String[1][width];
            colorRulers = new ColumnColorRuler[width];
            colWidths = new int[width];
            int addIndex = 0;
            for (int i = 0; i < table.getWidth(); i++) {
                if (i == indexedCol) {
                    continue;
                }
                header[0][addIndex] = table.getHeader(i);
                for (int j = 0; j < table.getHeight(); j++) {
                    data[j][addIndex] = table.getData(j, i);
                }
                footer[0][addIndex] = table.getFooter(i);
                colorRulers[addIndex] =
                    dp.getColColorRuler(tp, header[0][addIndex]);
                colWidths[addIndex] = completeColWidths[i];
                addIndex++;
            }
        }
        Log.i(TAG, "out of the else / for loop in buildTable");
        int avanda = getResources().getColor(R.color.Avanda);
        int headerData = getResources().getColor(R.color.header_data);
        int headerIndex = getResources().getColor(R.color.header_index);
        int footerIndex = getResources().getColor(R.color.footer_index);
//        LockableScrollView dataScroll = new LockableScrollView(context);
        dataScroll = new LockableScrollView(context);
        ColorDecider fgColorDecider = new ColorRulerColorDecider(colorRulers,
                Color.BLACK, false);
        ColorDecider bgColorDecider = new ColorRulerColorDecider(colorRulers,
                Color.WHITE, true);
        TabularView dataTable = new TabularView(context, this, data,
                Color.BLACK, fgColorDecider, Color.WHITE, bgColorDecider,
                Color.GRAY, colWidths,
                (isIndexed ? TableType.INDEX_DATA : TableType.MAIN_DATA),
                fontSize);
        dataScroll.addView(dataTable, new ViewGroup.LayoutParams(
                dataTable.getTableWidth(), dataTable.getTableHeight()));
        dataScroll.setVerticalFadingEdgeEnabled(true);
        dataScroll.setHorizontalFadingEdgeEnabled(true);
        TabularView headerTable = new TabularView(context, this, header,
                Color.BLACK, null, Color.CYAN, null, Color.GRAY, colWidths,
                (isIndexed ? TableType.INDEX_HEADER : TableType.MAIN_HEADER),
                fontSize);
        TabularView footerTable = new TabularView(context, this, footer,
                Color.BLACK, null, Color.GRAY, null, Color.GRAY, colWidths,
                (isIndexed ? TableType.INDEX_FOOTER : TableType.MAIN_FOOTER),
                fontSize);
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
    
    // This method was never called, and in order to make scrolling more 
    // efficient I had to change the type. I am leaving it for now b/c other
    // people are working on this code and I don't want make them rollback
    // if they need to use it.
//    public void setScrollEnabled(boolean enabled) {
//        wrapScroll.setScrollable(enabled);
//        if (indexScroll != null) {
//            indexScroll.setScrollable(enabled);
//        }
//        mainScroll.setScrollable(enabled);
//    }
    
    /**
     * Gets the x translation of the scroll. This is in particular how far 
     * you have scrolled to look at columns that do not begin onscreen.
     * @return
     */
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
            int x = (new Float(event.getX())).intValue();
            int y = (new Float(event.getY())).intValue();
            int cellId = figureCellId(x, y);
            long duration = event.getEventTime() - event.getDownTime();
            if (event.getAction() == MotionEvent.ACTION_UP &&
                    duration >= MIN_CLICK_DURATION) {
                if (event.getEventTime() - lastDownTime <
                        MAX_DOUBLE_CLICK_TIME) {
                    takeDoubleClickAction(cellId);
                } else if (duration < MIN_LONG_CLICK_DURATION) {
                    takeClickAction(cellId);
                } else {
                    int rawX = (new Float(event.getRawX())).intValue();
                    int rawY = (new Float(event.getRawY())).intValue();
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
        
        protected abstract void takeDoubleClickAction(int cellId);
    }
    
    private class ColorRulerColorDecider implements ColorDecider {
        
        private final ColumnColorRuler[] rulers;
        private final int defaultColor;
        private final boolean isBackground;
        
        public ColorRulerColorDecider(ColumnColorRuler[] rulers,
                int defaultColor, boolean isBackground) {
            this.rulers = rulers;
            this.defaultColor = defaultColor;
            this.isBackground = isBackground;
        }
        
        public int getColor(int rowNum, int colNum, String value) {
            return isBackground ?
                    rulers[colNum].getBackgroundColor(value, defaultColor) :
                        rulers[colNum].getForegroundColor(value, defaultColor);
        }
    }
    
    public interface Controller {
        
        public void regularCellClicked(int cellId);
        
        public void headerCellClicked(int cellId);
        
        public void footerCellClicked(int cellId);
        
        public void indexedColCellClicked(int cellId);
        
        public void regularCellLongClicked(int cellId, int rawX, int rawY);
        
        public void regularCellDoubleClicked(int cellId);
        
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
      ColumnProperties[] colProps = tp.getColumns();
      int[] columnWidths = new int[colProps.length];
      for (int i = 0; i < columnWidths.length; i++) {
        String elementKey = colProps[i].getElementKey();
        Integer value = tp.getIntegerEntry(ColumnProperties.KVS_PARTITION,
            elementKey, SpreadsheetView.KEY_COLUMN_WIDTH);
        if (value == null) {
          columnWidths[i] = DEFAULT_COL_WIDTH;
        } else {
          columnWidths[i] = value;
        }
      }
      return columnWidths;
    }
}
