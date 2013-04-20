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

import java.util.Map;

import org.opendatakit.tables.DataStructure.ColorRuleGroup.ColorGuide;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DbTable.ConflictTable;
import org.opendatakit.tables.data.Preferences;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.view.TabularView.ColorDecider;
import org.opendatakit.tables.view.TabularView.TableType;
import android.content.Context;
import android.graphics.Color;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;


public class ConflictResolutionView extends HorizontalScrollView
        implements TabularView.Controller {
    
    private static final int DOUBLE_CLICK_TIME = 500;
    private static final int FOREGROUND_COLOR = Color.BLACK;
    private static final int BACKGROUND_COLOR = Color.WHITE;
    private static final int BORDER_COLOR = Color.BLACK;
    private static final int HEADER_BACKGROUND_COLOR = Color.BLUE;
    
    private final Controller controller;
    private final Context context;
//    private final TableViewSettings tvs;
    private final TableProperties tp;
    private final int fontSize;
    private final ConflictTable table;
    private String[][][] data;
    private final RowItem[] rowItems;
    private final ColorDecider[] colorDeciders;
    private LinearLayout dataWrap;
    private int lastRowClicked;
    private int lastCellClicked;
    private long lastDownTime;
    
    public ConflictResolutionView(Controller controller, Context context,
            TableProperties tp, ConflictTable table) {
        super(context);
        this.controller = controller;
        this.context = context;
        this.tp = tp;
//        this.tvs = tvs;
        fontSize = (new Preferences(context)).getFontSize();
        this.table = table;
        data = new String[table.getCount()][2][table.getWidth()];
        rowItems = new RowItem[table.getCount()];
        colorDeciders = new ColorDecider[table.getCount()];
        for (int i = 0; i < table.getCount(); i++) {
            colorDeciders[i] = new ConflictColorDecider(i, Color.WHITE,
                    Color.GRAY, Color.DKGRAY);
        }
        lastRowClicked = -1;
        lastCellClicked = -1;
        lastDownTime = -1;
        buildView();
    }
    
    private void buildView() {
        // no-conflict message
        if (table.getCount() == 0) {
            TextView tv = new TextView(context);
            tv.setText("No conflicts.");
            addView(tv);
            return;
        }
        // creating header view
        String[] header = new String[table.getWidth()];
        for (int i = 0; i < table.getWidth(); i++) {
            header[i] = table.getHeader(i);
        }
        TabularView headerView = new TabularView(context, this, tp, header,
                FOREGROUND_COLOR, HEADER_BACKGROUND_COLOR,
                BORDER_COLOR, SpreadsheetView.getColumnWidths(tp), 
                TableType.MAIN_HEADER,
                fontSize);
        // creating data views
        dataWrap = new LinearLayout(context);
        dataWrap.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < table.getCount(); i++) {
            rowItems[i] = getRowView(i);
            dataWrap.addView(rowItems[i]);
        }
        // wrapping views up
        LinearLayout wrap = new LinearLayout(context);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(headerView, headerView.getTableWidth(),
                headerView.getTableHeight());
        ScrollView dataScroll = new ScrollView(context);
        dataScroll.setFillViewport(true);
        dataScroll.addView(dataWrap);
        LinearLayout.LayoutParams dsLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.FILL_PARENT);
        dsLp.weight = 1;
        wrap.addView(dataScroll, dsLp);
        LinearLayout.LayoutParams wLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.FILL_PARENT);
        addView(wrap, wLp);
    }
    
    private RowItem getRowView(int index) {
        for (int i = 0; i < table.getWidth(); i++) {
            data[index][0][i] = table.getValue(index, 0, i);
            data[index][1][i] = table.getValue(index, 1, i);
        }
        RowItem ri = new RowItem(context, index);
        TabularView tv = new TabularView(context, this, tp, data[index],
                FOREGROUND_COLOR, BACKGROUND_COLOR,
                BORDER_COLOR, SpreadsheetView.getColumnWidths(tp),
                TableType.MAIN_DATA,
                fontSize);
        setTabularViewTouchListener(index, tv);
        ri.setTabularView(tv);
        return ri;
    }
    
    private void setTabularViewTouchListener(final int index,
            final TabularView tv) {
        tv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() != MotionEvent.ACTION_DOWN) {
                    return false;
                }
                int x = (new Float(event.getX())).intValue();
                int y = (new Float(event.getY())).intValue();
                int cellNum = tv.getCellNumber(x, y);
                if ((lastRowClicked == index) &&
                        (lastCellClicked == cellNum)) {
                    if (event.getDownTime() - lastDownTime <=
                        DOUBLE_CLICK_TIME) {
                        controller.onDoubleClick(index,
                                cellNum / table.getWidth(),
                                cellNum % table.getWidth());
                    }
                } else {
                    lastRowClicked = index;
                    lastCellClicked = cellNum;
                }
                lastDownTime = event.getDownTime();
                return true;
            }
        });
    }
    
    public void setDatum(int index, int rowNum, int colNum, String value) {
        data[index][rowNum][colNum] = value;
        TabularView tv = new TabularView(context, this, tp, data[index],
                FOREGROUND_COLOR, BACKGROUND_COLOR,
                BORDER_COLOR, SpreadsheetView.getColumnWidths(tp), 
                TableType.MAIN_DATA,
                fontSize);
        setTabularViewTouchListener(index, tv);
        rowItems[index].setTabularView(tv);
    }
    
    public void removeRow(int index) {
        dataWrap.removeView(rowItems[index]);
    }
    
    private class RowItem extends LinearLayout {
        
        private final LinearLayout controlWrap;
        
        public RowItem(Context context, final int index) {
            super(context);
            setOrientation(LinearLayout.VERTICAL);
            Button setButton = new Button(context);
            setButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    controller.onSet(index);
                }
            });
            setButton.setText("Set");
            Button undoButton = new Button(context);
            undoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    controller.onUndo(index);
                }
            });
            undoButton.setText("Undo");
            controlWrap = new LinearLayout(context);
            controlWrap.addView(setButton);
            controlWrap.addView(undoButton);
        }
        
        public void setTabularView(TabularView tv) {
            removeAllViews();
            addView(tv, tv.getTableWidth(), tv.getTableHeight());
            addView(controlWrap);
        }
    }
    
    private class ConflictColorDecider implements ColorDecider {
        
        private final int index;
        private final int defaultColor;
        private final int shadedColor;
        private final int serverRowColor;
        
        public ConflictColorDecider(int index, int defaultColor,
                int shadedColor, int serverRowColor) {
            this.index = index;
            this.defaultColor = defaultColor;
            this.shadedColor = shadedColor;
            this.serverRowColor = serverRowColor;
        }
        
        public int getColor(int rowNum, int colNum, String value) {
            if (rowNum == 1) {
                return serverRowColor;
            } else if (table.getValue(index, 1, colNum).equals(value)) {
                return defaultColor;
            } else {
                return shadedColor;
            }
        }

        @Override
        public ColorGuide getColor(int index, String[] rowData, Map<String, Integer> columnMapping,
            Map<String, ColumnProperties> propertiesMapping) {
          // TODO Auto-generated method stub
          return null;
        }
    }
    
    public interface Controller {
        public void onSet(int index);
        public void onUndo(int index);
        public void onDoubleClick(int index, int rowNum, int colNum);
    }
    
    @Override
    public void onCreateMainDataContextMenu(ContextMenu menu) {}
    
    @Override
    public void onCreateIndexDataContextMenu(ContextMenu menu) {}
    
    @Override
    public void onCreateHeaderContextMenu(ContextMenu menu) {}
    
    @Override
    public void onCreateFooterContextMenu(ContextMenu menu) {}
}
