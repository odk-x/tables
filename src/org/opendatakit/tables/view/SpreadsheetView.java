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
import org.opendatakit.tables.data.TableViewSettings;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.view.TabularView.TableType;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;


public class SpreadsheetView extends LinearLayout
        implements TabularView.Controller {
    
    private static final int MIN_CLICK_DURATION = 0;
    private static final int MIN_LONG_CLICK_DURATION = 1000;
    
    private final Context context;
    private final Controller controller;
    private final TableViewSettings tvs;
    private final UserTable table;
    private final int indexedCol;
    private final DisplayPrefs dp;
    
    private ScrollView indexScroll;
    private ScrollView mainScroll;
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
            TableViewSettings tvs, UserTable table, int indexedCol,
            DisplayPrefs dp) {
        super(context);
        this.context = context;
        this.controller = controller;
        this.tvs = tvs;
        this.table = table;
        this.indexedCol = indexedCol;
        this.dp = dp;
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
            protected void takeClickAction(int cellId) {
                mainData.highlight(-1);
                controller.regularCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId) {
                mainData.highlight(-1);
                controller.openContextMenu(mainData);
            }
            @Override
            protected void takeHoverAction(int cellId) {
                int cellNum;
                if (indexedCol < 0) {
                    cellNum = cellId;
                } else {
                    int colNum = cellId % table.getWidth();
                    int rowNum = cellId / table.getWidth();
                    cellNum = cellId - rowNum -
                            ((colNum < indexedCol) ? 0 : 1);
                }
                mainData.highlight(cellNum);
            }
            @Override
            protected void takeCancelAction(int cellId) {
                mainData.highlight(-1);
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
            protected void takeClickAction(int cellId) {
                controller.headerCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId) {
                controller.openContextMenu(mainHeader);
            }
            @Override
            protected void takeHoverAction(int cellId) {}
            @Override
            protected void takeCancelAction(int cellId) {}
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
            protected void takeClickAction(int cellId) {
                controller.footerCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId) {
                controller.openContextMenu(mainFooter);
            }
            @Override
            protected void takeHoverAction(int cellId) {}
            @Override
            protected void takeCancelAction(int cellId) {}
        };
        indexDataCellClickListener = new CellTouchListener() {
            @Override
            protected int figureCellId(int x, int y) {
                int cellNum = indexData.getCellNumber(x, y);
                return (cellNum * table.getWidth()) + indexedCol;
            }
            @Override
            protected void takeClickAction(int cellId) {
                controller.indexedColCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId) {
                controller.openContextMenu(indexData);
            }
            @Override
            protected void takeHoverAction(int cellId) {}
            @Override
            protected void takeCancelAction(int cellId) {}
        };
        indexHeaderCellClickListener = new CellTouchListener() {
            @Override
            protected int figureCellId(int x, int y) {
                return indexedCol;
            }
            @Override
            protected void takeClickAction(int cellId) {
                controller.headerCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId) {
                controller.openContextMenu(indexHeader);
            }
            @Override
            protected void takeHoverAction(int cellId) {}
            @Override
            protected void takeCancelAction(int cellId) {}
        };
        indexFooterCellClickListener = new CellTouchListener() {
            @Override
            protected int figureCellId(int x, int y) {
                return indexedCol;
            }
            @Override
            protected void takeClickAction(int cellId) {
                controller.footerCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId) {
                controller.openContextMenu(indexFooter);
            }
            @Override
            protected void takeHoverAction(int cellId) {}
            @Override
            protected void takeCancelAction(int cellId) {}
        };
    }
    
    private void buildNonIndexedTable() {
        View wrapper = buildTable(-1, false);
        HorizontalScrollView wrapScroll = new HorizontalScrollView(context);
        wrapScroll.addView(wrapper, LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        LinearLayout.LayoutParams wrapLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        wrapLp.weight = 1;
        addView(wrapScroll, wrapLp);
    }
    
    private void buildIndexedTable(int indexedCol) {
        View mainWrapper = buildTable(indexedCol, false);
        View indexWrapper = buildTable(indexedCol, true);
        HorizontalScrollView wrapScroll = new HorizontalScrollView(context);
        wrapScroll.addView(mainWrapper, LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        LinearLayout wrapper = new LinearLayout(context);
        wrapper.addView(indexWrapper);
        wrapper.addView(wrapScroll);
        addView(wrapper);
        indexScroll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                mainScroll.scrollTo(mainScroll.getScrollX(),
                        view.getScrollY());
                return false;
            }
        });
        mainScroll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                indexScroll.scrollTo(indexScroll.getScrollX(),
                        view.getScrollY());
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
        String[][] header;
        String[][] data;
        String[][] footer;
        ColumnColorRuler[] colorRulers;
        int[] colWidths;
        int[] completeColWidths = tvs.getTableColWidths();
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
            colorRulers[0] = dp.getColColorRuler(table.getHeader(indexedCol));
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
                    dp.getColColorRuler(table.getHeader(i));
                colWidths[addIndex] = completeColWidths[i];
                addIndex++;
            }
        }
        int avanda = getResources().getColor(R.color.Avanda);
        int headerData = getResources().getColor(R.color.header_data);
        int headerIndex = getResources().getColor(R.color.header_index);
        int footerIndex = getResources().getColor(R.color.footer_index);
        ScrollView dataScroll = new ScrollView(context);
        TabularView dataTable = new TabularView(this, context, data, avanda,
                Color.BLACK, Color.GRAY, colWidths, colorRulers,
                (isIndexed ? TableType.INDEX_DATA : TableType.MAIN_DATA));
        dataScroll.addView(dataTable, new ViewGroup.LayoutParams(
                dataTable.getTableWidth(), dataTable.getTableHeight()));
        TabularView headerTable = new TabularView(this, context, header,
                (isIndexed ? headerIndex : headerData), Color.BLACK,
                Color.GRAY, colWidths, null,
                (isIndexed ? TableType.INDEX_HEADER : TableType.MAIN_HEADER));
        TabularView footerTable = new TabularView(this, context, footer,
                footerIndex, Color.BLACK, Color.GRAY, colWidths, null,
                (isIndexed ? TableType.INDEX_FOOTER : TableType.MAIN_FOOTER));
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
        
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            int x = (new Float(event.getX())).intValue();
            int y = (new Float(event.getY())).intValue();
            int cellId = figureCellId(x, y);
            Log.d("SSV", "cell:" + cellId + " / action:" + event.getAction());
            long duration = event.getEventTime() - event.getDownTime();
            if ((event.getAction() == MotionEvent.ACTION_DOWN) ||
                    (event.getAction() == MotionEvent.ACTION_MOVE)) {
                //takeHoverAction(cellId);
                //return true;
                return false;
            } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                //takeCancelAction(cellId);
                //return true;
                return false;
            } else if (event.getAction() == MotionEvent.ACTION_UP &&
                    duration >= MIN_CLICK_DURATION) {
                if (duration < MIN_LONG_CLICK_DURATION) {
                    takeClickAction(cellId);
                } else {
                    lastLongClickedCellId = cellId;
                    takeLongClickAction(cellId);
                }
                return true;
            } else {
                Log.d("SSV", "here");
                return false;
            }
        }
        
        protected abstract int figureCellId(int x, int y);
        
        protected abstract void takeClickAction(int cellId);
        
        protected abstract void takeLongClickAction(int cellId);
        
        protected abstract void takeHoverAction(int cellId);
        
        protected abstract void takeCancelAction(int cellId);
    }
    
    public interface Controller {
        
        public void regularCellClicked(int cellId);
        
        public void headerCellClicked(int cellId);
        
        public void footerCellClicked(int cellId);
        
        public void indexedColCellClicked(int cellId);
        
        public void openContextMenu(View view);
        
        public void prepRegularCellOccm(ContextMenu menu, int cellId);
        
        public void prepHeaderCellOccm(ContextMenu menu, int cellId);
        
        public void prepFooterCellOccm(ContextMenu menu, int cellId);
        
        public void prepIndexedColCellOccm(ContextMenu menu, int cellId);
    }
}
