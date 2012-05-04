package org.opendatakit.tables.view;

import org.opendatakit.tables.DataStructure.DisplayPrefs.ColumnColorRuler;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.ContextMenu;
import android.view.View;


class TabularView extends View {
    
    enum TableType { MAIN_DATA, MAIN_HEADER, MAIN_FOOTER,
        INDEX_DATA, INDEX_HEADER, INDEX_FOOTER }
    
    private static final int ROW_HEIGHT = 30;
    private static final int HORIZONTAL_CELL_PADDING = 5;
    private static final int VERTICAL_CELL_PADDING = 9;
    private static final int BORDER_WIDTH = 1;
    
    private Controller controller;
    private String[][] data;
    private int backgroundColor;
    private int foregroundColor;
    private int[] columnWidths;
    private ColumnColorRuler[] colorRulers;
    private TableType type;
    
    private Paint textPaint;
    private Paint bgPaint;
    private Paint borderPaint;
    private Paint highlightPaint;
    
    private int totalHeight;
    private int totalWidth;
    
    private int highlightedCellNum;
    
    public TabularView(Controller controller, Context context, String[][] data,
            int backgroundColor, int foregroundColor, int borderColor,
            int[] columnWidths, ColumnColorRuler[] colorRulers,
            TableType type) {
        super(context);
        init(controller, data, backgroundColor, foregroundColor, borderColor,
                columnWidths, colorRulers, type);
    }
    
    public TabularView(Controller controller, Context context, String[] data,
            int backgroundColor, int foregroundColor, int borderColor,
            int[] columnWidths, ColumnColorRuler[] colorRulers,
            TableType type) {
        super(context);
        String[][] outerData = {data};
        init(controller, outerData, backgroundColor, foregroundColor,
                borderColor, columnWidths, colorRulers, type);
    }
    
    private void init(Controller controller, String[][] data,
            int backgroundColor, int foregroundColor, int borderColor,
            int[] columnWidths, ColumnColorRuler[] colorRulers,
            TableType type) {
        this.controller = controller;
        this.data = data;
        this.backgroundColor = backgroundColor;
        this.foregroundColor = foregroundColor;
        this.columnWidths = columnWidths;
        this.colorRulers = colorRulers;
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(16);
        bgPaint = new Paint();
        bgPaint.setColor(backgroundColor);
        borderPaint = new Paint();
        borderPaint.setColor(borderColor);
        highlightPaint = new Paint();
        highlightPaint.setColor(Color.CYAN);
        highlightPaint.setStrokeWidth(3);
        totalHeight = (ROW_HEIGHT + BORDER_WIDTH) * data.length + BORDER_WIDTH;
        totalWidth = BORDER_WIDTH;
        for (int i = 0; i < columnWidths.length; i++) {
            totalWidth += columnWidths[i] + BORDER_WIDTH;
        }
        setMinimumHeight(totalHeight);
        setMinimumWidth(totalWidth);
        this.type = type;
        setClickable(true);
        highlightedCellNum = -1;
    }
    
    public int getTableHeight() {
        return totalHeight;
    }
    
    public int getTableWidth() {
        return totalWidth;
    }
    
    public int getCellNumber(int x, int y) {
        int row = y / (ROW_HEIGHT + BORDER_WIDTH);
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
            yCoord += ROW_HEIGHT + BORDER_WIDTH;
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
                int foregroundColor = (colorRulers == null) ?
                        this.foregroundColor :
                        colorRulers[j].getForegroundColor(datum,
                                this.foregroundColor);
                int backgroundColor = (colorRulers == null) ?
                        this.backgroundColor :
                        colorRulers[j].getBackgroundColor(datum,
                                this.backgroundColor);
                drawCell(canvas, xs[j], y, datum, backgroundColor,
                        foregroundColor, columnWidths[j]);
            }
            y += ROW_HEIGHT + BORDER_WIDTH;
        }
        // highlighting cell (if necessary)
        if (highlightedCellNum != -1) {
            int x = highlightedCellNum % data[0].length;
            int rowNum = highlightedCellNum / data[0].length;
            highlightCell(canvas, xs[x], ((rowNum + 1) * BORDER_WIDTH) +
                    (rowNum * ROW_HEIGHT), columnWidths[x]);
        }
    }
    
    private void drawCell(Canvas canvas, int x, int y, String datum,
            int backgroundColor, int foregroundColor, int columnWidth) {
        if (backgroundColor != this.backgroundColor) {
            bgPaint.setColor(backgroundColor);
            canvas.drawRect(x, y, x + columnWidth, y + ROW_HEIGHT, bgPaint);
        }
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.clipRect(x + HORIZONTAL_CELL_PADDING, y,
                x + columnWidth - (2 * HORIZONTAL_CELL_PADDING),
                y + ROW_HEIGHT);
        textPaint.setColor(foregroundColor);
        canvas.drawText(datum, x + HORIZONTAL_CELL_PADDING,
                (y + ROW_HEIGHT - VERTICAL_CELL_PADDING), textPaint);
        canvas.restore();
    }
    
    private void highlightCell(Canvas canvas, int x, int y,
            int columnWidth) {
        canvas.drawLine(x + 1, y + 1, x + columnWidth - 1, y - 1,
                highlightPaint);
        canvas.drawLine(x + 1, y + 1, x + 1, y + ROW_HEIGHT - 1,
                highlightPaint);
        canvas.drawLine(x + columnWidth - 1, y + 1, x + columnWidth - 1,
                y + ROW_HEIGHT - 1, highlightPaint);
        canvas.drawLine(x + 1, y + ROW_HEIGHT - 1, x + columnWidth - 1,
                y + ROW_HEIGHT - 1, highlightPaint);
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
    
    interface Controller {
        void onCreateMainDataContextMenu(ContextMenu menu);
        void onCreateIndexDataContextMenu(ContextMenu menu);
        void onCreateHeaderContextMenu(ContextMenu menu);
        void onCreateFooterContextMenu(ContextMenu menu);
    }
}
