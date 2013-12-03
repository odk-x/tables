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
package org.opendatakit.hope.views;

import org.opendatakit.hope.data.ColumnProperties;
import org.opendatakit.hope.data.TableProperties;
import org.opendatakit.hope.data.UserTable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/**
 * A class for displaying data in list form.
 *
 * @author hkworden
 */
public class ListDisplayView extends LinearLayout {

    private int BACKGROUND_COLOR = Color.WHITE;
    private int TEXT_COLOR = Color.BLACK;
    private int BORDER_COLOR = Color.BLACK;

    private Controller controller; // the table activity to call back to
    private UserTable table; // the table to display
    private int[] lineHeights;
    private String[][] lineTextSpecs;
    private int[][] lineColSpecs;
    private Paint[] colPaints;
    private Paint borderPaint;

    public static ListDisplayView buildView(Context context,
            Controller controller,
            UserTable table) {
        return new ListDisplayView(context, controller, table);
    }

    private ListDisplayView(Context context,
            Controller controller, UserTable table) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        this.controller = controller;
        this.table = table;
        setFormatInfo();
        borderPaint = new Paint();
        borderPaint.setColor(BORDER_COLOR);
        removeAllViews();
        setBackgroundColor(BACKGROUND_COLOR);
        buildList(context);
    }

    private void setFormatInfo() {
// removed this when TableViewSettings was removed.
//        String format = tvs.getListFormat();
      String format = null;
        if (format == null || format.length() == 0) {
            format = getDefaultFormat();
        }
        Log.d("LDV", "format:" + format);
        String[] lines = (format.length() == 0) ?
                new String[] {} : format.split("\n");
        lineHeights = new int[lines.length];
        lineTextSpecs = new String[lines.length][];
        lineColSpecs = new int[lines.length][];
        colPaints = new Paint[lines.length];
        for (int i = 0; i < lines.length; i++) {
            String[] lineData = lines[i].split(":", 2);
            lineHeights[i] = 20;
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setTextSize(16);
            paint.setColor(TEXT_COLOR);
            for (int j = 0; j < lineData[0].length(); j++) {
                char c = lineData[0].charAt(j);
                if (c == 'b') {
                    paint.setFakeBoldText(true);
                } else if (c == 'u') {
                    paint.setUnderlineText(true);
                } else if (c == 'l') {
                    paint.setTextSize(24);
                    lineHeights[i] = 28;
                }
            }
            colPaints[i] = paint;
            String[] lineSplit = (lineData.length < 2) ? new String[] {} :
                    lineData[1].split("%");
            lineTextSpecs[i] = new String[(lineSplit.length / 2) +
                    (lineSplit.length % 2 == 0 ? 0 : 1)];
            lineColSpecs[i] = new int[lineSplit.length / 2];
            for (int j = 0; j < lineSplit.length; j += 2) {
                lineTextSpecs[i][j / 2] = lineSplit[j];
            }
            TableProperties tp = table.getTableProperties();
            for (int j = 1; j < lineSplit.length; j += 2) {
                int colIndex = tp.getColumnIndex(lineSplit[j]);
                if (colIndex < 0) {
                    ColumnProperties cp = tp.getColumnByDisplayName(lineSplit[j]);
                    if (cp == null) {
                    	cp = tp.getColumnByAbbreviation(lineSplit[j]);
                    }
                    if (cp == null) {
                    	colIndex = -1;
                    } else {
	                    String colDbName = cp.getElementKey();
	                    colIndex = tp.getColumnIndex(colDbName);
                    }
                }
                lineColSpecs[i][j / 2] = colIndex;
            }
        }
    }

    private void buildList(Context context) {
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.onListItemClick(v.getId());
            }
        };
        ScrollView scroll = new ScrollView(context);
        LinearLayout wrapper = new LinearLayout(context);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        int count = table.getHeight();
        for (int i = 0; i < count; i++) {
            LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            View v = new ItemView(context, i);
            v.setId(i);
            v.setOnClickListener(clickListener);
            wrapper.addView(v, itemLp);
        }
        scroll.addView(wrapper);
        addView(scroll);
    }

    private String getDefaultFormat() {
        StringBuilder builder = new StringBuilder();
        if (table.getWidth() > 0) {
            builder.append("bl:%" + table.getHeader(0) + "%\n");
        }
        for (int i = 1; i < 4 && i < table.getWidth(); i++) {
            builder.append(":%" + table.getHeader(i) + "%\n");
        }
        return builder.toString();
    }

    private class ItemView extends View {

        private int rowNum;

        public ItemView(Context context, int rowNum) {
            super(context);
            this.rowNum = rowNum;
            int height = 11;
            for (int i = 0; i < lineHeights.length; i++) {
                height += lineHeights[i];
            }
            setMinimumHeight(height);
        }

        @Override
        public void onDraw(Canvas canvas) {
            int y = 0;
            for (int i = 0; i < lineHeights.length; i++) {
                y += lineHeights[i];
                canvas.drawText(getText(i), 1, y, colPaints[i]);
            }
            canvas.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1,
                    borderPaint);
        }

        private String getText(int lineNum) {
            StringBuilder builder = new StringBuilder();
            int i;
            for (i = 0; i < lineColSpecs[lineNum].length; i++) {
                builder.append(lineTextSpecs[lineNum][i]);
                String value = table.getData(rowNum, lineColSpecs[lineNum][i]);
                builder.append((value == null) ? "" : value);
            }
            if (lineTextSpecs[lineNum].length > i) {
                builder.append(lineTextSpecs[lineNum][i]);
            }
            return builder.toString();
        }
    }

    public interface Controller {

        public void onListItemClick(int rowNum);
    }
}
