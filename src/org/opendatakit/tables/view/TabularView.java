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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.tables.DataStructure.ColorRuleGroup;
import org.opendatakit.tables.DataStructure.ColorRuleGroup.ColorGuide;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.util.Constants;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
  private final int defaultBackgroundColor;
  private final int defaultForegroundColor;
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
  
  /**
   * The list of {@link ColorRuleGroup} objects for the columns of the table.
   * This will be responsible for coloring the cells of a column.
   */
  private List<ColorRuleGroup> mColumnColorRules;
  /**
   * The {@link ColorRuleGroup} object for the table. This will be responsible
   * for things like determining row color.
   */
  private ColorRuleGroup mRowColorRuleGroup;
  /**
   * Maps elementKey to column index.
   */
  private Map<String, Integer> mColumnIndexMap;
  /**
   * Maps elementKey to {@link ColumnProperties}.
   */
  private Map<String, ColumnProperties> mColumnPropertiesMap;
  private TableProperties mTp;

  // trying to get the dimensions of the screen
  private final DisplayMetrics metrics;

  // this should hold the x location of the column. so xs[12] should hold the
  // x displacement of the left side of that column.
  private int[] xs;
  // this array should hold the column displacement. i think it should be the
  // same as xs, except that the first position should be 0.
  private int[] spans;

  public TabularView(Context context, Controller controller, 
      TableProperties tp, String[][] data,
      int defaultForegroundColor,
      int defaultBackgroundColor, int borderColor,
      int[] columnWidths, TableType type, int fontSize) {
    super(context);
    this.controller = controller;
    this.mTp = tp;
    this.data = data;
    this.defaultBackgroundColor = defaultBackgroundColor;
    this.defaultForegroundColor = defaultForegroundColor;
    this.columnWidths = columnWidths;
    this.type = type;
    this.fontSize = fontSize;
    // Now let's set up the color rule things.
    Map<String, Integer> indexMap = new HashMap<String, Integer>();
    Map<String, ColumnProperties> propertiesMap = 
        new HashMap<String, ColumnProperties>();
    List<String> columnOrder = mTp.getColumnOrder();
    this.mColumnColorRules = new ArrayList<ColorRuleGroup>();
    for (int i = 0; i < columnOrder.size(); i++) {
      indexMap.put(columnOrder.get(i), i);
      propertiesMap.put(columnOrder.get(i), mTp.getColumnByIndex(i));
      mColumnColorRules.add(ColorRuleGroup.getColumnColorRuler(mTp, 
          columnOrder.get(i)));
    }
    this.mColumnIndexMap = indexMap;
    this.mColumnPropertiesMap = propertiesMap;
    this.mRowColorRuleGroup = ColorRuleGroup.getTableColorRuleGroup(tp);
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
    setVerticalScrollBarEnabled(true);
    setVerticalFadingEdgeEnabled(true);
    setHorizontalFadingEdgeEnabled(true);
    setMinimumHeight(totalHeight);
    setMinimumWidth(totalWidth);
    setClickable(true);
    this.metrics = getResources().getDisplayMetrics();
    // check to make sure you don't get out of bounds exceptions.
    if (data.length > 0) {
      this.xs = new int[data[0].length];
      xs[0] = BORDER_WIDTH;
      for (int i = 0; i < data[0].length - 1; i++) {
        xs[i + 1] = xs[i] + columnWidths[i] + BORDER_WIDTH;
      }
    } else {
      this.xs = new int[0];
    }
    this.spans = new int[xs.length];
    if (spans.length > 0) {
      int total = 0;
      for (int i = 0; i < this.spans.length; i++) {
        spans[i] = total;
        total += BORDER_WIDTH + columnWidths[i];
      }
    }
  }

  public TabularView(Context context, Controller controller, 
      TableProperties tp, String[] data,
      int defaultForegroundColor,
      int defaultBackgroundColor, 
      int borderColor, int[] columnWidths, TableType type, int fontSize) {
    this(context, controller, tp, new String[][] { data }, 
        defaultForegroundColor, defaultBackgroundColor,
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
    // drawing the background--so you're redrawing the background every time...
    canvas.drawRect(0, 0, totalWidth, totalHeight, bgPaint);

    /*
     * SS: I am going to try and fix this method. There are several things that
     * need to be considered. First, a spreadsheet view is composed of several
     * tabular views. The base case for an un-indexed table is composed of a
     * main_header, main_data, main_footer. It can also include an
     * index_header, index_data, and index_footer, if it is indexed.
     *
     * We want to support drawing this table efficiently. We are going to do
     * this as follows. The SpreadsheetView object contains methods to get the
     * appropriate x and y offsets of the scrolls. They are getMainScroll x
     * and y. These are coming from two different views, and are rather
     * confusing, so we will just think about them as individual entities.
     *
     * When we draw a table, we want always to draw the header at the top.
     * Somehow the footer is taking care of itself. Atm I'm not sure how.
     * Probably some sort of parameters happening in SpreadsheetView?
     *
     * We do not want to draw the whole spreadsheet, as this would be slower
     * and slower the more data you add. Instead we want to only draw the
     * necessary bits for the screen to display. We have the dimensions of
     * the screen in the metrics field, which is a DisplayMetrics object.
     * We are currently drawing the whole canvas. This seems ok, and we might
     * even need to do that for clipping rects.
     *
     * In the diagram below, we have a phone (the asterisks) viewing a small
     * set of the table.
     * I am not bothering to draw all the rows that would fall between the
     * header and footer. We want to draw as little as possible to still cover
     * all that the phone would see.
     *
     * The arrow marked "X,Y" points to the X and y offset returned by the
     * getMainScroll methods. X is it's displacement from the left, Y is from
     * the top (of the whole canvas, I'm pretty sure). These apparently can
     * be negative during bounceback, so I am including a check to set them
     * >= 0.
     *
     * To draw efficiently, there are several other things we need to know.
     * First, we need to see the leftmost column which must be drawn. The left
     * border of this column is pointed at with an L in the diagram below.
     * We also need to know the rightmost column which must be drawn. The left
     * border of this column is pointed at with an R in the diagram below. It
     * is also important to note that we need to know where the right borders
     * of these columns are, as we must tell the canvas from top left to
     * bottom right how to draw the rectangle that will become the cell.
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
     * The dimensions of column 7 would thus be from
     * xs[7] to columnWidths[7], and it's height would be rowHeight.
     *
     * It is also important to note that atm borders and cells are being drawn
     * separately. This is not ideal, but when I tried to fix it I got weird
     * behavior so for now I'm going to leave it.
     *
     * The header must always be drawn at location 0, as it always needs to
     * stay in the top part of the screen at location 0, even when the data
     * TabularView object is scrolling under it.
     *
     * We also want only to draw as many rows as necessary. These
     * begin at the top of the topmost row, the arrow labeled topTopmost
     * in the diagram below. Similarly, we need to know the top of the
     * bottommost row, labeled topBottommost below. If we have scrolled down in
     * the table, there will be rows above topmost. If we are not to the bottom
     * of the table, there will be rows below bottommost. These two are the top
     *  and bottom -most rows that have some portion displayed on the screen.
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
     * The leftmost column is called leftmost, and the rightmost, rightmost.
     * These are 0-indexed ints representing the ordinality of the column.
     * The fifth column (4 in a 0-indexed system) would then be in 4.
     * leftmost
     * rightmost
     *
     * The left border of this column
     * and the left border of the rightmost column are thus:
     * leftLeftmost  (the L arrow)
     * leftRightmost (the R arrow)
     *
     * Topmost is the topmost row, bottommost is the most bottom.
     * topmost
     * bottommost
     *
     * The other two variables we need to maintain are the topmost border and
     * the leftmost border. We will draw them until we are off the screen and
     * then stop.
     * topmostBorder
     * leftmostBorder
     * These are assumed to be (and are by definition, I believe)
     * xs[col] - BORDER_WIDTH.
     *
     * We also need to know the righthand border of the rightmost column, as
     * this is where we will be stopping drawing.
     * rightRightmost.
     * The definition of rightRightmost is defined in the code below, but I
     * think it is basically
     * xs[rightmost] + columnWidths[rightmost] + BORDER_WIDTH.
     *
     * Both leftLefmost and rightRightmost might be off the screen. In the
     * diagram below, both would be when drawing the data.
     * The header, however, never would be.
     *
     * ______________________________________________________________
     * |       |     |      |rightmost
     * |       |     |      |           |topTopmost
     * |_______|_____|______|___________V_______________________________
     * |     X,Y->***************   |          bottomOfTheTopmostRow
     * |       |  *__|header|___*   |topmost     |
     * |_______|__*__|______|___*___|____________V____________________
     * |       |  *  |      |   *   |  X=xScroll; y=yScroll
     * |       |  *  |data  |   *   |  L=leftLeftmost; R=leftRightmost
     * |    L->|  *  |      |   *   |
     * |       |  *  |      |   *   |  | topBottommost
     * |_______|__*__|___R->|___*______V_____________________________
     * |       |  *__|______|___*   | bottommost(from here to bottomBottommost)
     * |       |  *  |footer|   *   |                                |
     * |       |  ***************   |<-rightRightmostBorder          |
     * |_______|_____|______|_______|________________________________V____
     * |       |     |      |       |
     * |       |leftmost    |       |
     * |       |     |      |       |
     */

    int xScroll = sv.getMainScrollX();
    int yScroll = sv.getMainScrollY();
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
    int topBottommostBorder;
    int bottommost;
    int leftLeftmost;
    int leftRightmost;
    int rightRightmostBorder;
    int bottomBottommost;

    // The first thing we must do is recognize that we have to undergo
    // different procedures if we are dealing with a header. (also maybe a
    // footer? Unsure at the moment. It seems to work with and without the
    // footer check. I'm going to leave it with the footer check b/c it seems
    // like it is more correct...)
    // First we will get the correct topmost row. If this is a header of any
    // sort, the first row should be 0.
    if (this.type == TableType.INDEX_HEADER ||
        this.type == TableType.MAIN_HEADER ||
        this.type == TableType.INDEX_FOOTER ||
        this.type == TableType.MAIN_FOOTER) {
      topmost = 0;
      bottommost = 0;
    } else {
      // Otherwise, we need to compute the value.
      // First let's get the row.
      topmost = yScroll / (BORDER_WIDTH + rowHeight);
      bottommost = (yScroll + metrics.heightPixels) /
          (BORDER_WIDTH + rowHeight);
      if (bottommost >= data.length) {
        Log.d("TAG", "in if to catch, bottommost: " + bottommost);
        bottommost = data.length - 1; // don't want to go beyond the last row
        Log.d("TAG", "reset bottommost to: " + bottommost);
      }
    }
    // For a while I was getting an error where it looked like this was not
    // getting appropriately reset. However, it now seems like for whatever
    // reason it is indeed working...possibly didn't push the new code to the
    // phone? Going to leave the logs in here for now in case I stumble across
    // it again.
    Log.d(TAG, "bottom most after else is: " + bottommost);
    topmostBorder = topmost * (BORDER_WIDTH + rowHeight);
    topTopmost = topmostBorder + BORDER_WIDTH;
    topBottommostBorder = bottommost * (BORDER_WIDTH + rowHeight);
    bottomBottommost = topBottommostBorder + BORDER_WIDTH + rowHeight;
    // And now let's get the correct column. The math here can't be as simple,
    // b/c unlike rowHeight, columnWidth is not a fixed unit.
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
    int yCoord = topmostBorder;
    for (int i = topmost; i < bottommost + 1; i++) {
      canvas.drawRect(leftmostBorder, yCoord, rightRightmostBorder, yCoord +
          BORDER_WIDTH, borderPaint);
      yCoord += rowHeight + BORDER_WIDTH;
    }
    int xCoord = leftmostBorder;
    for (int i = leftmost; i < rightmost + 1; i++) {
    canvas.drawRect(xCoord, topmostBorder, xCoord + BORDER_WIDTH,
        bottomBottommost, borderPaint);
      xCoord += (i == data[0].length) ? 0 : columnWidths[i] + BORDER_WIDTH;
    }
    // drawing the cells
    int y = topTopmost;
    for (int i = topmost; i < bottommost + 1; i++) {
      for (int j = leftmost; j < rightmost + 1; j++) {
        String datum = data[i][j];
        if (datum == null) {
          datum = "";
        }
        ColorGuide rowGuide = mRowColorRuleGroup.getColorGuide(
            data[i], mColumnIndexMap, mColumnPropertiesMap);
        ColorGuide columnGuide = mColumnColorRules.get(j)
            .getColorGuide(data[i], mColumnIndexMap, mColumnPropertiesMap);
        int foregroundColor = this.defaultForegroundColor;
        int backgroundColor = this.defaultBackgroundColor;
        if (type == TableType.MAIN_DATA) {
        // First we check for a row rule.
          if (rowGuide.didMatch()) {
            foregroundColor = rowGuide.getForeground();
            backgroundColor = rowGuide.getBackground();
          }
          // Override the role rule if a column rule matched.
          if (columnGuide.didMatch()) {
            foregroundColor = columnGuide.getForeground();
            backgroundColor = columnGuide.getBackground();
          }
        }
        drawCell(canvas, xs[j], y, datum, backgroundColor, foregroundColor,
            columnWidths[j]);
      }
      y += rowHeight + BORDER_WIDTH;
      /** adding to try and fix draw **/
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
//    Log.d(TAG, "drawCell called");
    // have to do this check to reset to the default, otherwise it uses the
    // old object which was previously saved and paints all the columns the
    // wrong color.
    if (backgroundColor != this.defaultBackgroundColor) {
      bgPaint.setColor(backgroundColor);
    } else {
      bgPaint.setColor(this.defaultBackgroundColor);
    }
      canvas.drawRect(x, y, x + columnWidth, y + rowHeight, bgPaint);
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
    public ColorGuide getColor(int index, String[] rowData, 
        Map<String, Integer> columnMapping,
        Map<String, ColumnProperties> propertiesMapping);
  }

  interface Controller {
    void onCreateMainDataContextMenu(ContextMenu menu);

    void onCreateIndexDataContextMenu(ContextMenu menu);

    void onCreateHeaderContextMenu(ContextMenu menu);

    void onCreateFooterContextMenu(ContextMenu menu);
  }
}
