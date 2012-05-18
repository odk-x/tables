package org.opendatakit.tables.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.ContextMenu;
import android.view.View;


class TabularView extends View {
    
    enum TableType { MAIN_DATA, MAIN_HEADER, MAIN_FOOTER,
        INDEX_DATA, INDEX_HEADER, INDEX_FOOTER }
    
    private static final int ROW_HEIGHT_PADDING = 14;
    private static final int HORIZONTAL_CELL_PADDING = 5;
    private static final int VERTICAL_CELL_PADDING = 9;
    private static final int BORDER_WIDTH = 1;
    
    private final Controller controller;
    private final String[][] data;
    private final ColorDecider foregroundColorDecider;
    private final int defaultBackgroundColor;
    private final ColorDecider backgroundColorDecider;
    private final int[] columnWidths;
    private final TableType type;
    private final int fontSize;
    private final int rowHeight;
    
    private int totalHeight;
    private int totalWidth;
    private int highlightedCellNum;
    
    private final Paint textPaint;
    private final Paint bgPaint;
    private final Paint borderPaint;
    private final Paint highlightPaint;
    
    public TabularView(Context context, Controller controller, String[][] data,
            int defaultForegroundColor, ColorDecider foregroundColorDecider,
            int defaultBackgroundColor, ColorDecider backgroundColorDecider,
            int borderColor, int[] columnWidths, TableType type,
            int fontSize) {
        super(context);
        this.controller = controller;
        this.data = data;
        this.foregroundColorDecider = (foregroundColorDecider == null) ?
                new DefaultColorDecider(defaultForegroundColor) :
                    foregroundColorDecider;
        this.defaultBackgroundColor = defaultBackgroundColor;
        this.backgroundColorDecider = (backgroundColorDecider == null) ?
                new DefaultColorDecider(defaultBackgroundColor) :
                    backgroundColorDecider;
        this.columnWidths = columnWidths;
        this.type = type;
        this.fontSize = fontSize;
        rowHeight = fontSize + ROW_HEIGHT_PADDING;
        highlightedCellNum = -1;
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(fontSize);
        bgPaint = new Paint();
        bgPaint.setColor(defaultBackgroundColor);
        borderPaint = new Paint();
        borderPaint.setColor(borderColor);
        highlightPaint = new Paint();
        highlightPaint.setColor(Color.CYAN);
        highlightPaint.setStrokeWidth(3);
        totalHeight = (rowHeight + BORDER_WIDTH) * data.length + BORDER_WIDTH;
        totalWidth = BORDER_WIDTH;
        for (int i = 0; i < columnWidths.length; i++) {
            totalWidth += columnWidths[i] + BORDER_WIDTH;
        }
        setMinimumHeight(totalHeight);
        setMinimumWidth(totalWidth);
        setClickable(true);
    }
    
    public TabularView(Context context, Controller controller, String[] data,
            int defaultForegroundColor, ColorDecider foregroundColorDecider,
            int defaultBackgroundColor, ColorDecider backgroundColorDecider,
            int borderColor, int[] columnWidths, TableType type,
            int fontSize) {
        this(context, controller, new String[][] {data},
                defaultForegroundColor, foregroundColorDecider,
                defaultBackgroundColor, backgroundColorDecider, borderColor,
                columnWidths, type, fontSize);
    }
    
    public int getTableHeight() {
        return totalHeight;
    }
    
    public int getTableWidth() {
        return totalWidth;
    }
    
    public int getCellNumber(int x, int y) {
        int row = y / (rowHeight + BORDER_WIDTH);
        int col = -1;
        while (x > 0) {
            col++;
            x -= columnWidths[col] + BORDER_WIDTH;
        }
        return (row * columnWidths.length) + col; 
    }
    
    public void highlight(int num) {
        highlightedCellNum = num;
        invalidate();
    }
    
    @Override
    public void onDraw(Canvas canvas) {
        if (data.length == 0) {
            return;
        }
        // drawing the background
        canvas.drawRect(0, 0, totalWidth, totalHeight, bgPaint);
        // drawing the borders
        int yCoord = 0;
        for (int i = 0; i <= data.length; i++) {
            canvas.drawRect(0, yCoord, totalWidth, yCoord + BORDER_WIDTH,
                    borderPaint);
            yCoord += rowHeight + BORDER_WIDTH;
        }
        int xCoord = 0;
        for (int i = 0; i <= data[0].length; i++) {
            canvas.drawRect(xCoord, 0, xCoord + BORDER_WIDTH, totalHeight,
                    borderPaint);
            xCoord += (i == data[0].length) ? 0 :
                columnWidths[i] + BORDER_WIDTH;
        }
        // drawing the cells
        int[] xs = new int[data[0].length];
        xs[0] = BORDER_WIDTH;
        for (int i = 0; i < data[0].length - 1; i++) {
            xs[i + 1] = xs[i] + columnWidths[i] + BORDER_WIDTH;
        }
        int y = BORDER_WIDTH;
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                String datum = data[i][j];
                if (datum == null) {
                    datum = "";
                }
                int foregroundColor = foregroundColorDecider.getColor(i, j,
                        datum);
                int backgroundColor = backgroundColorDecider.getColor(i, j,
                        datum);
                drawCell(canvas, xs[j], y, datum, backgroundColor,
                        foregroundColor, columnWidths[j]);
            }
            y += rowHeight + BORDER_WIDTH;
        }
        // highlighting cell (if necessary)
        if (highlightedCellNum != -1) {
            int x = highlightedCellNum % data[0].length;
            int rowNum = highlightedCellNum / data[0].length;
            highlightCell(canvas, xs[x], ((rowNum + 1) * BORDER_WIDTH) +
                    (rowNum * rowHeight), columnWidths[x]);
        }
    }
    
    private void drawCell(Canvas canvas, int x, int y, String datum,
            int backgroundColor, int foregroundColor, int columnWidth) {
        if (backgroundColor != this.defaultBackgroundColor) {
            bgPaint.setColor(backgroundColor);
            canvas.drawRect(x, y, x + columnWidth, y + rowHeight, bgPaint);
        }
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.clipRect(x + HORIZONTAL_CELL_PADDING, y,
                x + columnWidth - (2 * HORIZONTAL_CELL_PADDING),
                y + rowHeight);
        textPaint.setColor(foregroundColor);
        canvas.drawText(datum, x + HORIZONTAL_CELL_PADDING,
                (y + rowHeight - VERTICAL_CELL_PADDING), textPaint);
        canvas.restore();
    }
    
    private void highlightCell(Canvas canvas, int x, int y,
            int columnWidth) {
        canvas.drawLine(x + 1, y + 1, x + columnWidth - 1, y - 1,
                highlightPaint);
        canvas.drawLine(x + 1, y + 1, x + 1, y + rowHeight - 1,
                highlightPaint);
        canvas.drawLine(x + columnWidth - 1, y + 1, x + columnWidth - 1,
                y + rowHeight - 1, highlightPaint);
        canvas.drawLine(x + 1, y + rowHeight - 1, x + columnWidth - 1,
                y + rowHeight - 1, highlightPaint);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu) {
        switch(type) {
        case MAIN_DATA:
            controller.onCreateMainDataContextMenu(menu);
            return;
        case INDEX_DATA:
            controller.onCreateIndexDataContextMenu(menu);
            return;
        case MAIN_HEADER:
        case INDEX_HEADER:
            controller.onCreateHeaderContextMenu(menu);
            return;
        case MAIN_FOOTER:
        case INDEX_FOOTER:
            controller.onCreateFooterContextMenu(menu);
            return;
        }
    }
    
    interface ColorDecider {
        public int getColor(int rowNum, int colNum, String value);
    }
    
    private class DefaultColorDecider implements ColorDecider {
        
        private final int color;
        
        public DefaultColorDecider(int color) {
            this.color = color;
        }
        
        public int getColor(int rowNum, int colNum, String value) {
            return color;
        }
    }
    
    interface Controller {
        void onCreateMainDataContextMenu(ContextMenu menu);
        void onCreateIndexDataContextMenu(ContextMenu menu);
        void onCreateHeaderContextMenu(ContextMenu menu);
        void onCreateFooterContextMenu(ContextMenu menu);
    }
}
