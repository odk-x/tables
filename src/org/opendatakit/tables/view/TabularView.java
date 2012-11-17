package org.opendatakit.tables.view;

import android.app.Application;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Region;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;

/**
 * A view that draws a single table.
 */
class TabularView extends View {
  
  public static final String TAG = "TabularView";

  enum TableType {
    MAIN_DATA, MAIN_HEADER, MAIN_FOOTER, INDEX_DATA, INDEX_HEADER, INDEX_FOOTER
  }

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
  
  // trying to get the dimensions of the screen
  private final DisplayMetrics metrics;

  public TabularView(Context context, Controller controller, String[][] data,
      int defaultForegroundColor, ColorDecider foregroundColorDecider, 
      int defaultBackgroundColor,
      ColorDecider backgroundColorDecider, int borderColor, 
      int[] columnWidths, TableType type, int fontSize) {
    super(context);
    this.controller = controller;
    this.data = data;
    this.foregroundColorDecider = 
        (foregroundColorDecider == null) ? new DefaultColorDecider(
            defaultForegroundColor) : foregroundColorDecider;
    this.defaultBackgroundColor = defaultBackgroundColor;
    this.backgroundColorDecider = 
        (backgroundColorDecider == null) ? new DefaultColorDecider(
            defaultBackgroundColor) : backgroundColorDecider;
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
    this.metrics = getResources().getDisplayMetrics();
  }

  public TabularView(Context context, Controller controller, String[] data,
      int defaultForegroundColor, ColorDecider foregroundColorDecider, 
      int defaultBackgroundColor, ColorDecider backgroundColorDecider, 
      int borderColor, int[] columnWidths, TableType type, int fontSize) {
    this(context, controller, new String[][] { data }, defaultForegroundColor,
        foregroundColorDecider, defaultBackgroundColor, backgroundColorDecider,
        borderColor, columnWidths, type, fontSize);
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
    // Logging when this is called to see why it is so slow to view a table.
    Log.d(TAG, "onDraw called");
    if (data.length == 0) {
      return;
    }
    /** trying to fix the slow draw **/
    SpreadsheetView sv = null;
    if (controller instanceof SpreadsheetView) {
      sv = (SpreadsheetView) controller;
    } else {
      Log.e(TAG, "controller was not instance of spreadsheet view, " +
            "cannot cast, will have null pointers");
    }
    // drawing the background --so you're redrawing the background every time...
    canvas.drawRect(0, 0, totalWidth, totalHeight, bgPaint);
    /** trying to change the constantly drawn background **/
    // this seemed to add very little. it didn't outright break, but it did 
    // mean the footer and headers got removed somehow.
//    canvas.drawRect(sv.getMainScrollX(), sv.getMainScrollY(), sv.getMainScrollX() + metrics.widthPixels,
//        sv.getMainScrollY() + metrics.heightPixels, bgPaint);
    // drawing the borders
//    int yCoord = 0;

    int yCoord = sv.getMainScrollY();
//    int yCoord = 0;
    int aboveScroll = 0; 
    // now populate as much as we can of the left of scroll.
    int row = 0;
    // first we want to check we're not at the beginning.
    if (yCoord > 0) {
      aboveScroll = BORDER_WIDTH; // for the first border
      for (row = 0; row < data.length; row++) {
        aboveScroll += rowHeight;
        aboveScroll += BORDER_WIDTH;
        if (aboveScroll > yCoord) {
          // if we got too big, undo the damage.
          aboveScroll -= rowHeight;
          aboveScroll -= BORDER_WIDTH;
          //TODO: could there be an edge case here we we only break by border_
          //width and undo too much?
          break;
        }
      }
    }
    yCoord = aboveScroll; // to actually make what we just checked matter
    for (int i = row; i < data.length; i++) {
      canvas.drawRect(0, yCoord, totalWidth, yCoord + 
//      canvas.clipRect(0, yCoord, totalWidth, yCoord+ 
          BORDER_WIDTH, borderPaint);
      yCoord += rowHeight + BORDER_WIDTH;
      /** adding to try and fix draw**/
      if (yCoord > sv.getMainScrollY() + metrics.heightPixels) 
        break;
    }
//    int xCoord = 0;
    int xCoord = sv.getMainScrollX();
    /*
     * Ok, so now the xcoordinate for drawing the borders around the cells is
     * set to be where we are in our scrollable view. We want to now compute
     * the offset correctly. As it was, it was stopping when it got too big,
     * but that meant everything to the left of the screen was always getting
     * drawn. That isn't what we want.
     * 
     * There are several ways we might do this. Atm column widths are all set
     * at 125, so we could do some clever math to figure out where to start.
     * I think it would be something like start drawing at:
     * (scroll - borderWidth) % (col + borderWidth). The minus border width
     * is because we have one too many--the first one doesn't pair nicely with
     * a column. We would then continue drawing at:
     * colWidth[(scroll - width) / (col + width)] + 2.
     * So we would then pick up at the right spot in the columnWidths.
     * 
     * I'm not positive the above is correct, but it gives you an idea of my
     * thinking. Instead, however, it seems like we should allow for non-
     * uniform column widths. This means we'll have to parse through the array
     * each time. This makes things more flexible, although it might make them
     * a little bit slower.
     */
    int leftOfScroll = 0; // for the first border.
    // now populate as much as we can of the left of scroll.
    int col = 0;
    // first we want to check we're not at the beginning.
    if (xCoord > 0) {
      leftOfScroll = BORDER_WIDTH;
      for (col = 0; col < columnWidths.length; col++) {
        leftOfScroll += columnWidths[col];
        leftOfScroll += BORDER_WIDTH;
        if (leftOfScroll > xCoord) {
          // if we got too big, undo the damage.
          leftOfScroll -= columnWidths[col];
          leftOfScroll -= BORDER_WIDTH;
          //TODO: could there be an edge case here we we only break by border_
          //width and undo too much?
          break;
        }
      }
    }
    // else we just proceed.
    // and now we know where we are in the scrollable.
    xCoord = leftOfScroll;
//    int xCoord = this.getScrollX();
    // i used to be zero. changing it.
    for (int i = col; i < data[0].length; i++) {
      canvas.drawRect(xCoord, 0, xCoord + BORDER_WIDTH, totalHeight, 
          borderPaint);
      xCoord += (i == data[0].length) ? 0 : columnWidths[i] + BORDER_WIDTH;
      /** adding to try and fix draw **/
      if (xCoord > sv.getMainScrollX() + metrics.widthPixels) 
        break;
    }
    // drawing the cells
    int[] xs = new int[data[0].length];
    xs[0] = BORDER_WIDTH;
    for (int i = 0; i < data[0].length - 1; i++) {
      xs[i + 1] = xs[i] + columnWidths[i] + BORDER_WIDTH;
    }
    int y = BORDER_WIDTH;
//    boolean broke = false;
    for (int i = 0; i < data.length; i++) {
//      if (!broke) { // so we don't check each time of we already broke
      for (int j = 0; j < data[0].length; j++) {
        /** adding to try and fix draw **/
        if (xs[j] > sv.getMainScrollX() + metrics.widthPixels){
          break;
        }
        String datum = data[i][j];
        if (datum == null) {
          datum = "";
        }
        int foregroundColor = foregroundColorDecider.getColor(i, j, datum);
        int backgroundColor = backgroundColorDecider.getColor(i, j, datum);
        drawCell(canvas, xs[j], y, datum, backgroundColor, foregroundColor, 
            columnWidths[j]);
      }
      y += rowHeight + BORDER_WIDTH;
      /** adding to try and fix draw **/
      if (y > sv.getMainScrollY() + metrics.heightPixels)
        break;
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
    Log.d(TAG, "drawCell called");
    if (backgroundColor != this.defaultBackgroundColor) {
      bgPaint.setColor(backgroundColor);
      canvas.drawRect(x, y, x + columnWidth, y + rowHeight, bgPaint);
    }
    canvas.save(Canvas.ALL_SAVE_FLAG);
    canvas.clipRect(x + HORIZONTAL_CELL_PADDING, y,
        x + columnWidth - (2 * HORIZONTAL_CELL_PADDING), y + rowHeight);
//        Region.Op.REPLACE);
    textPaint.setColor(foregroundColor);
    canvas.drawText(datum, x + HORIZONTAL_CELL_PADDING, (y + rowHeight 
        - VERTICAL_CELL_PADDING), textPaint);
    canvas.restore();
  }

  private void highlightCell(Canvas canvas, int x, int y, int columnWidth) {
    canvas.drawLine(x + 1, y + 1, x + columnWidth - 1, y - 1, highlightPaint);
    canvas.drawLine(x + 1, y + 1, x + 1, y + rowHeight - 1, highlightPaint);
    canvas.drawLine(x + columnWidth - 1, y + 1, x + columnWidth - 1, y + 
        rowHeight - 1, highlightPaint);
    canvas.drawLine(x + 1, y + rowHeight - 1, x + columnWidth - 1, y + 
        rowHeight - 1, highlightPaint);
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu) {
    switch (type) {
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
