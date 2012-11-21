package org.opendatakit.tables.view;

import java.util.Arrays;

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
  
  // this should hold the x location of the column. so xs[12] should hold the
  // x displacement of the left side of that column.
  private int[] xs;
  // this array should hold the column displacement. i think it should be the
  // same as xs, except that the first position should be 0.
  private int[] spans;

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
    this.xs = new int[data[0].length];
    xs[0] = BORDER_WIDTH;
    for (int i = 0; i < data[0].length - 1; i++) {
      xs[i + 1] = xs[i] + columnWidths[i] + BORDER_WIDTH;
    }
    this.spans = new int[xs.length];
//    this.spans[0] = 0;
//    int total = 1; // we need this to account for the one we did not count
    // in the xs[0] position.
    int total = 0;
    for (int i = 0; i < this.spans.length; i++) {
//      total += xs[i];
      spans[i] = total;
      total += BORDER_WIDTH + columnWidths[i];
    }
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
    
    /*
     * I am going to try and fix this method. There are several things that
     * need to be considered. First, a spreadsheet view is composed of several
     * tabular views. The base case for an un-indexed table is composed of a 
     * main_header, main_data, main_footer. It can also include an
     * index_header, index_data, and index_footer. 
     * 
     * We want to support drawing this table efficiently. We are going to do
     * this as follows. The SpreadsheetView object contains methods to get the
     * appropriate x and y offsets of the scrolls. They are getMainScroll x 
     * and y. These are coming from two different views, and are rather 
     * confusing, so we will just think about them as individual entities.
     * 
     * When we draw a table, we want always to draw the header at the top.
     * Somehow the footer is taking care of itself. Atm I'm not sure how.
     * 
     * We do not want to draw the whole spreadsheet, as this would be slower
     * and slower the more data you add. Instead we want to only draw the 
     * necessary bits for the screen to display. We have the dimensions of 
     * the screen in the metrics field, which is a DisplayMetrics object.
     * 
     * In the diagram below, we have a phone (the asterisks) viewing a small 
     * set of the table.
     * I am not bothering to draw the rows, which would fall between the header
     * and footer. We want to draw as little as possible to still cover 
     * all the phone would see. 
     * 
     * The arrow marked "X,Y" points to the X and y offset returned by the
     * getMainScroll methods. X is it's displacement from the left, Y is from
     * the top.
     * 
     * To draw efficiently, there are several other things we need to know. 
     * First, we need to see the leftmost column which must be drawn. The left
     * border of this column is pointed at with an L in the diagram below.
     * We also need to know the rightmost column which must be drawn. This is 
     * pointed at with an R in the diagram below. It is also important to note
     * that we need to know where the right borders of these columns are, as 
     * we must tell the canvas from top left to bottom right how to draw the
     * rectangle that will become the cell.
     * 
     * This information is stored in two separate arrays. One is the xs[] array
     * of integers, which tells where each column begins. xs[0] is the x 
     * location of the zeroth column. This should always be 0 (the absolute 
     * left of the canvas) + BORDER_WIDTH. At the time of this writing xs
     * is computed upon creation of the object. columnWidths[] contains the 
     * int width of each of the columns (excluding the borders, from what I
     * can tell). At the time of this writing I do not think you can alter
     * column width, but you should theoretically be able to, so we are 
     * programming it as such.
     * 
     * The dimensions of column 7 would this be from 
     * xs[7] to columnWidths[7], and it's height would be rowHeight. 
     * 
     * It is also important to note that atm borders and cells are being drawn
     * separately. This is not ideal, but when I tried to fix it I got weird
     * behavior so for now I'm going to leave it.
     * 
     * The header must always be drawn at location 0, as it always needs to 
     * stay in the top part of the sceen at location 0, even when the data 
     * TabularView object is scrolling under it. 
     * 
     * However, we also want only to draw as many rows as necessary. These 
     * begin at the top bordre of the topmost row, the arrow labeled topTopmost
     * in the diagram below. Similarly, we need to know the top of the bottom
     * most row, labled topBottommost below. If we have scrolled down in the 
     * table, there will be rows above topmost. If we are not to the bottom
     * of the table, there will be rows below bottommost. There are the top and
     * bottom -most rows that have some portion displayed on the screen. 
     * (Whether or not this means you might have one with only 5 pixels on 
     * screen and the rest hidden beneath the header, I am not sure, but it
     * seems likely. This could possibly create drawing problems? I'm not 
     * sure.)
     * 
     * Also note that there is something not working using cell highlighting 
     * when tables are indexed. It seems to only affect the right hand columns,
     * and I'm not sure why, but it is not currently a priority.
     * TODO: fix cell highlighting discussed in the previous paragraph.
     * 
     * In the code the X,Y arrow is stored in two variables:
     * xScroll
     * yScroll.
     * 
     * The leftmost column is called leftmost, and the rightmost rightmost. 
     * These are 0-indexed ints representing the ordinality of the column.
     * If the fifth column (4 in a 0-indexed system) would then be in 4.
     * leftmost
     * rightmost
     * 
     * The left border of this column
     * and the left border of the rightmost column are thus:
     * leftLeftmost  (the L arrow)
     * leftRightmost (the R arrow)
     * 
     * Topmost is the topmost row.
     * topmost
     * 
     * The other two variables we need to maintain are the topmostborder and 
     * the leftmost border. We will draw them until we are off the screen and
     * then stop.
     * topmostBorder
     * leftmostBorder
     * rightRightmostBorder;
     * These are assumed to be (and are by definition, I believe) 
     * xs[col] - BORDER_WIDTH.
     * 
     * Both of these variables might or might not be off the screen. In the
     * diagram below, both would be when drawing the data. 
     * The header, however, never would be. 
     *         
     * ______________________________________________________________
     * |       |     |      |rightmost
     * |       |     |      |         | topTopmost
     * |_______|_____|______|_________V_______________________________
     * |     X,Y->***************   |
     * |       |  *__|header|___*   |
     * |       |  *  |      |   *   |
     * |       |  *  |      |   *   |
     * |       |  *  |data  |   *   |
     * |    L->|  *  |      |   *   |
     * |       |  *  |      |   *   |  | topBottommost
     * |_______|__*__|___R->|___*______V_____________________________
     * |       |  *__|______|___*   |
     * |       |  *  |footer|   *   |
     * |       |  ***************   |
     * |       |     |      |       |
     * |       |     |      |       |<- rightRightmostBorder 
     * |       |leftmost    |       |
     * |       |     |      |       |
     */

    int xScroll = sv.getMainScrollX();
    int yScroll = sv.getMainScrollY();
    // sometimes was getting negative, maybe from bounceback?
    if (xScroll < 0) {
      xScroll = 0;
      Log.d(TAG, "xScroll was negative, reset to 0");
    }
    if (yScroll < 0) {
      yScroll = 0;
      Log.d(TAG, "yScroll was negative, reset to 0");
    }
    int leftmost;
    int rightmost;
    int topmost;
    int topmostBorder;
    int leftmostBorder;
    int topTopmost;
    int topBottommost;
    int leftLeftmost;
    int leftRightmost;
    int rightRightmostBorder;
    
    // The first thing we must do is recognize that we have to undergo 
    // different procedures if we are dealing with a header. (also maybe a
    // footer? Unsure at the moment.)
    // First we will get the correct topmost row. If this is a header of any
    // sort, the first row should be 0.
    if (this.type == TableType.INDEX_HEADER ||
        this.type == TableType.MAIN_HEADER) {
      topmost = 0;
    } else {
      // Otherwise, we need to compute the value.
      // First let's get the row.
      topmost = yScroll / (BORDER_WIDTH + rowHeight);
    }
    topmostBorder = topmost * (BORDER_WIDTH + rowHeight);
    topTopmost = topmostBorder + BORDER_WIDTH;
    // And now let's get the correct column. The math here can't be as simple,
    // b/c unlike rowHeight, columnWidth is not a fixed unit. However, we can
    // get the correct column from the xs array and the xScroll value.
    leftmost = getLeftmostColumnBasedOnXScroll(xScroll);
    if (leftmost < 0) {
      Log.e(TAG, "leftmost was < 0 from getLeftmostColumnBasedOnXScroll " +
            "for xScroll: " + xScroll + ", type is: " + this.type);
    }
    leftLeftmost = xs[leftmost];
    leftmostBorder = leftLeftmost - BORDER_WIDTH;
    rightmost = getLeftmostColumnBasedOnXScroll(xScroll + metrics.widthPixels);
    if (rightmost < 0) {
      Log.e(TAG, "rightMost was < 0 from getLeftmostColumnBasedOnXScroll " +
      		"for xScroll + metrics.widthPixels: " + xScroll + " + " 
          + metrics.widthPixels + " = " + xScroll + metrics.widthPixels +
          ", type is: " + this.type);
    }
    leftRightmost = xs[rightmost];
    rightRightmostBorder = leftRightmost + columnWidths[rightmost] 
        + BORDER_WIDTH; // i believe at the end, then, this should be total
                        // width, once it is all on the column?
    
 
// commented all this out after adding the lengthy business above.
//    int yCoord = sv.getMainScrollY();
////    int yCoord = 0;
//    int aboveScroll = 0; 
//    // now populate as much as we can of the left of scroll.
//    int row = 0;
//    // first we want to check we're not at the beginning.
//    if (yCoord > 0) {
//      aboveScroll = BORDER_WIDTH; // for the first border
//      for (row = 0; row < data.length; row++) {
//        aboveScroll += rowHeight;
//        aboveScroll += BORDER_WIDTH;
//        if (aboveScroll > yCoord) {
//          // if we got too big, undo the damage.
//          aboveScroll -= rowHeight;
//          aboveScroll -= BORDER_WIDTH;
//          //TODO: could there be an edge case here we we only break by border_
//          //width and undo too much?
//          break;
//        }
//      }
//    }
//    yCoord = aboveScroll; // to actually make what we just checked matter
    // this will store the int that is the top left of our borders.
    int yCoord = topmostBorder;
    for (int i = topmost; i < data.length; i++) {
      // i hate this total width thing. it should probably be changed at some 
      // point if i can figure out how it's being used.
      canvas.drawRect(leftmostBorder, yCoord, rightRightmostBorder, yCoord + 
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
  
  /*
   * This should return the leftmost column of which anything should be 
   * displayed on the screen, where the screen position is specified by the
   * xScroll int.
   * 
   * This will be given by 
   */
  private int getLeftmostColumnBasedOnXScroll(int xScroll) {
    int bsResult; // will hold the binary search result.
    bsResult = Arrays.binarySearch(this.spans, xScroll);
    // now we need to do some checking. If we've found an exact match, we know
    // that we can just return it.
    if (bsResult >= 0) {
      return bsResult;
    } else {
      // we need to do a calculation.
      // in case of a miss binary search return (-insertionPoint - 1). 
      int col = (bsResult + 1) * -1;
      col -= 1; // to set it correctly.
      // however, we need to do another check. it is possible that we will have
      // added a new element to the array. Let's do this check.
      if (col >= xs.length) {
        return col - 1;
      } else {
        return col;
      }
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
