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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.tables.data.ColorRuleGroup;
import org.opendatakit.tables.data.ColorRuleGroup.ColorGuide;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;

/**
 * A view that draws a single table. A single table is essentially a grid of
 * of cells filled with text. For instance a Spreadsheet might consist of a
 * header (labels for the columns), a table of data, and a footer. Each of 
 * these would be an individual TabularView.
 * 
 * @author sudar.sam@gmail.com
 */
class TabularView extends View {

  public static final String TAG = "TabularView";

  enum TableType {
    // NB: After the change to use UserTable more heavily, there is essentially
    // no difference between the MAIN and INDEX table types. They remain for
    // now just for ease of debugging if for some reason it matters in a way
    // I don't yet see. They will probably be safe to consolidate in the 
    // future.
    MAIN_DATA, MAIN_HEADER, MAIN_FOOTER, INDEX_DATA, INDEX_HEADER,
    INDEX_FOOTER, STATUS_DATA, STATUS_HEADER, STATUS_FOOTER;
  }
  
  /**
   *  The value that appears in the spreadsheet of the status column.
   */
  private static final String DEFAULT_STATUS_COLUMN_VALUE = " ";
  public static final int DEFAULT_STATUS_COLUMN_WIDTH = 10;
  // These are the default colors for the various standard table types.
  private static final int DEFAULT_FOREGROUND_COLOR = Color.BLACK;
  private static final int DEFAULT_DATA_BACKGROUND_COLOR = Color.WHITE;
  private static final int DEFAULT_BORDER_COLOR = Color.GRAY;
  private static final int DEFAULT_HEADER_BACKGROUND_COLOR = Color.CYAN;
  private static final int DEFAULT_FOOTER_BACKGROUND_COLOR = Color.GRAY;

  private static final int ROW_HEIGHT_PADDING = 14;
  private static final int HORIZONTAL_CELL_PADDING = 5;
  private static final int VERTICAL_CELL_PADDING = 9;
  private static final int BORDER_WIDTH = 1;

  private final Controller controller;
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
   * The abstraction of the table onto which this {@link TabularView} is 
   * providing a view.
   */
  private final UserTable mTable; 
  /**
   * The list of element keys which this {@link TabularView} is responsible
   * for displaying. It is a (not strict) subset. 
   */
  private final List<String> mElementKeys;

  /**
   * The map of element key to {@link ColorRuleGroup} objects for the columns 
   * of the table. This will be responsible for coloring the cells of a column.
   */
  private Map<String, ColorRuleGroup> mColumnColorRules;
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
  // This is the number of rows represented by this TabularView. This will 
  // change based on the TableType. For instance, data objects will be all the
  // data rows of the table, header and footer will be one, etc.
  private int mNumberOfRows;
  
  /**
   * Construct the data portion of the main portion of the table. Default 
   * colors are applied.
   * @see TabularView#TabularView(Context, Controller, TableProperties, 
   * UserTable, List, int, int, int, int[], TableType, int, Map, Map)
   * @param context
   * @param controller
   * @param tp
   * @param table
   * @param elementKeysToDisplay
   * @param columnWidths
   * @param fontSize
   * @param elementKeyToColumnProperties
   * @param elementKeyToColorRuleGroup
   * @return
   */
  public static TabularView getMainDataTable(Context context, 
      Controller controller, 
      TableProperties tp, UserTable table, List<String> elementKeysToDisplay,
      int[] columnWidths, int fontSize, 
      Map<String, ColumnProperties> elementKeyToColumnProperties,
      Map<String, ColorRuleGroup> elementKeyToColorRuleGroup) {
    return new TabularView(context, controller, tp, table, 
        elementKeysToDisplay, DEFAULT_FOREGROUND_COLOR, 
        DEFAULT_DATA_BACKGROUND_COLOR, 
        DEFAULT_BORDER_COLOR, columnWidths, TableType.MAIN_DATA, fontSize, 
        elementKeyToColumnProperties, elementKeyToColorRuleGroup);
  }
  
  /**
   * Construct the header of the main portion of table. Default colors are 
   * applied.
   * @see TabularView#TabularView(Context, Controller, TableProperties, 
   * UserTable, List, int, int, int, int[], TableType, int, Map, Map)
   * @param context
   * @param controller
   * @param tp
   * @param table
   * @param elementKeysToDisplay
   * @param columnWidths
   * @param fontSize
   * @param elementKeyToColumnProperties
   * @param elementKeyToColorRuleGroup
   * @return
   */
  public static TabularView getMainHeaderTable(
      Context context, Controller controller, 
      TableProperties tp, UserTable table, List<String> elementKeysToDisplay,
      int[] columnWidths, int fontSize, 
      Map<String, ColumnProperties> elementKeyToColumnProperties,
      Map<String, ColorRuleGroup> elementKeyToColorRuleGroup) {
    return new TabularView(context, controller, tp, table, 
        elementKeysToDisplay, DEFAULT_FOREGROUND_COLOR, 
        DEFAULT_HEADER_BACKGROUND_COLOR, 
        DEFAULT_BORDER_COLOR, columnWidths, TableType.MAIN_HEADER, fontSize, 
        elementKeyToColumnProperties, elementKeyToColorRuleGroup);
  }
  
  /**
   * Construct the footer of the main portion of the table. Default colors
   * are applied.
   * @see TabularView#TabularView(Context, Controller, TableProperties, 
   * UserTable, List, int, int, int, int[], TableType, int, Map, Map)
   * @param context
   * @param controller
   * @param tp
   * @param table
   * @param elementKeysToDisplay
   * @param columnWidths
   * @param fontSize
   * @param elementKeyToColumnProperties
   * @param elementKeyToColorRuleGroup
   * @return
   */
  public static TabularView getMainFooterTable(
      Context context, Controller controller, 
      TableProperties tp, UserTable table, List<String> elementKeysToDisplay,
      int[] columnWidths, int fontSize, 
      Map<String, ColumnProperties> elementKeyToColumnProperties,
      Map<String, ColorRuleGroup> elementKeyToColorRuleGroup) {
    return new TabularView(context, controller, tp, table, 
        elementKeysToDisplay, DEFAULT_FOREGROUND_COLOR, 
        DEFAULT_FOOTER_BACKGROUND_COLOR, 
        DEFAULT_BORDER_COLOR, columnWidths, TableType.MAIN_FOOTER, fontSize, 
        elementKeyToColumnProperties, elementKeyToColorRuleGroup);
  }
  
  /**
   * Construct the data portion of the indexed table. Default colors are 
   * applied.
   * @see TabularView#TabularView(Context, Controller, TableProperties, 
   * UserTable, List, int, int, int, int[], TableType, int, Map, Map)
   * @param context
   * @param controller
   * @param tp
   * @param table
   * @param elementKeysToDisplay
   * @param columnWidths
   * @param fontSize
   * @param elementKeyToColumnProperties
   * @param elementKeyToColorRuleGroup
   * @return
   */
  public static TabularView getIndexDataTable(
      Context context, Controller controller, 
      TableProperties tp, UserTable table, List<String> elementKeysToDisplay,
      int[] columnWidths, int fontSize, 
      Map<String, ColumnProperties> elementKeyToColumnProperties,
      Map<String, ColorRuleGroup> elementKeyToColorRuleGroup) {
    return new TabularView(context, controller, tp, table, 
        elementKeysToDisplay, DEFAULT_FOREGROUND_COLOR, 
        DEFAULT_DATA_BACKGROUND_COLOR, 
        DEFAULT_BORDER_COLOR, columnWidths, TableType.INDEX_DATA, fontSize, 
        elementKeyToColumnProperties, elementKeyToColorRuleGroup);
  }
  
  /**
   * Construct the header of the indexed portion of the table. Default colors
   * are applied.
   * @see TabularView#TabularView(Context, Controller, TableProperties, 
   * UserTable, List, int, int, int, int[], TableType, int, Map, Map)
   * @param context
   * @param controller
   * @param tp
   * @param table
   * @param elementKeysToDisplay
   * @param columnWidths
   * @param fontSize
   * @param elementKeyToColumnProperties
   * @param elementKeyToColorRuleGroup
   * @return
   */
  public static TabularView getIndexHeaderTable(
      Context context, Controller controller, 
      TableProperties tp, UserTable table, List<String> elementKeysToDisplay,
      int[] columnWidths, int fontSize, 
      Map<String, ColumnProperties> elementKeyToColumnProperties,
      Map<String, ColorRuleGroup> elementKeyToColorRuleGroup) {
    return new TabularView(context, controller, tp, table, 
        elementKeysToDisplay, DEFAULT_FOREGROUND_COLOR, 
        DEFAULT_HEADER_BACKGROUND_COLOR, 
        DEFAULT_BORDER_COLOR, columnWidths, TableType.INDEX_HEADER, fontSize, 
        elementKeyToColumnProperties, elementKeyToColorRuleGroup);
  }
  
  /**
   * Construct the footer table of the indexed portion of the table. Default
   * colors are applied.
   * @see TabularView#TabularView(Context, Controller, TableProperties, 
   * UserTable, List, int, int, int, int[], TableType, int, Map, Map)
   * @param context
   * @param controller
   * @param tp
   * @param table
   * @param elementKeysToDisplay
   * @param columnWidths
   * @param fontSize
   * @param elementKeyToColumnProperties
   * @param elementKeyToColorRuleGroup
   * @return
   */
  public static TabularView getIndexFooterTable(
      Context context, Controller controller, 
      TableProperties tp, UserTable table, List<String> elementKeysToDisplay,
      int[] columnWidths, int fontSize, 
      Map<String, ColumnProperties> elementKeyToColumnProperties,
      Map<String, ColorRuleGroup> elementKeyToColorRuleGroup) {
    return new TabularView(context, controller, tp, table, 
        elementKeysToDisplay, DEFAULT_FOREGROUND_COLOR, 
        DEFAULT_FOOTER_BACKGROUND_COLOR, 
        DEFAULT_BORDER_COLOR, columnWidths, TableType.INDEX_FOOTER, fontSize, 
        elementKeyToColumnProperties, elementKeyToColorRuleGroup);
  }
  
  /**
   * Construct the data portion of the status table. Default colors are 
   * applied. No data is displayed in the status table.
   * @see TabularView#TabularView(Context, Controller, TableProperties, 
   * UserTable, List, int, int, int, int[], TableType, int, Map, Map)
   * @param context
   * @param controller
   * @param tp
   * @param table
   * @param columnWidths
   * @param fontSize
   * @param elementKeyToColumnProperties
   * @param elementKeyToColorRuleGroup
   * @return
   */
  public static TabularView getStatusDataTable(
      Context context, Controller controller, 
      TableProperties tp, UserTable table,
      int[] columnWidths, int fontSize, 
      Map<String, ColumnProperties> elementKeyToColumnProperties,
      Map<String, ColorRuleGroup> elementKeyToColorRuleGroup) {
    List<String> dummyElementKeys = new ArrayList<String>();
    // We need to make this a size one so that the status table knows there's
    // something to display.
    dummyElementKeys.add("data");
    return new TabularView(context, controller, tp, table, 
        dummyElementKeys, DEFAULT_FOREGROUND_COLOR, 
        DEFAULT_DATA_BACKGROUND_COLOR, 
        DEFAULT_BORDER_COLOR, columnWidths, TableType.STATUS_DATA, fontSize, 
        elementKeyToColumnProperties, elementKeyToColorRuleGroup);
  }
  
  /**
   * Construct the header for the status table. Default colors are applied.
   * No data is displayed in the status column.
   * @see TabularView#TabularView(Context, Controller, TableProperties, 
   * UserTable, List, int, int, int, int[], TableType, int, Map, Map)
   * @param context
   * @param controller
   * @param tp
   * @param table
   * @param columnWidths
   * @param fontSize
   * @param elementKeyToColumnProperties
   * @param elementKeyToColorRuleGroup
   * @return
   */
  public static TabularView getStatusHeaderTable(
      Context context, Controller controller, 
      TableProperties tp, UserTable table,
      int[] columnWidths, int fontSize, 
      Map<String, ColumnProperties> elementKeyToColumnProperties,
      Map<String, ColorRuleGroup> elementKeyToColorRuleGroup) {
    List<String> dummyElementKeys = new ArrayList<String>();
    // We need to make this a size one so that the status table knows there's
    // something to display.
    dummyElementKeys.add("header");
    return new TabularView(context, controller, tp, table, 
        dummyElementKeys, DEFAULT_FOREGROUND_COLOR, 
        DEFAULT_HEADER_BACKGROUND_COLOR, 
        DEFAULT_BORDER_COLOR, columnWidths, TableType.STATUS_HEADER, fontSize, 
        elementKeyToColumnProperties, elementKeyToColorRuleGroup);
  }
  
  /**
   * Construct a TabularView to represent the footer of the status table. 
   * Default colors are applied. No data from the table is displayed in the 
   * status column.
   * @see TabularView#TabularView(Context, Controller, TableProperties, 
   * UserTable, List, int, int, int, int[], TableType, int, Map, Map)
   * @param context
   * @param controller
   * @param tp
   * @param table
   * @param columnWidths
   * @param fontSize
   * @param elementKeyToColumnProperties
   * @param elementKeyToColorRuleGroup
   * @return
   */
  public static TabularView getStatusFooterTable(
      Context context, Controller controller, 
      TableProperties tp, UserTable table,
      int[] columnWidths, int fontSize, 
      Map<String, ColumnProperties> elementKeyToColumnProperties,
      Map<String, ColorRuleGroup> elementKeyToColorRuleGroup) {
    List<String> dummyElementKeys = new ArrayList<String>();
    // We need to make this a size one so that the status table knows there's
    // something to display.
    dummyElementKeys.add("footer");
    return new TabularView(context, controller, tp, table, 
        dummyElementKeys, DEFAULT_FOREGROUND_COLOR, 
        DEFAULT_FOOTER_BACKGROUND_COLOR, 
        DEFAULT_BORDER_COLOR, columnWidths, TableType.STATUS_FOOTER, fontSize, 
        elementKeyToColumnProperties, elementKeyToColorRuleGroup);
  }
  
  /**
   * Construct a TabularView. Most uses will likely be able to use
   * one of the static factory methods.
   * <p>
   * A TabularView is essentially a view onto the data contained in the 
   * UserTable pointed to by the table parameter. The columns it displays are
   * must be all or a strict subset of the columns contained in the UserTable.
   * The columns it is responsible for displaying are specified by the 
   * elementKeys parameter.
   * @param context
   * @param controller
   * @param tp
   * @param table the {@link UserTable} into which this TabularView is 
   * providing a view.
   * @param elementKeys the list of element keys which this tabular view 
   * is responsible for displaying. E.g. if it is a data column without any
   * frozen columns, it would be the entire column order. If it was a single
   * frozen column, it would be just that element key. Must be nonzero length.
   * @param defaultForegroundColor
   * @param defaultBackgroundColor
   * @param borderColor
   * @param columnWidths
   * @param type
   * @param fontSize
   * @param elementKeyToColumnProperties mapping of element key to the
   * corresponding {@link ColumnProperties} object. Must be all the columns,
   * NOT just those displayed in thie TabularView.
   * @param elementKeyToColorRuleGroup mapping of element key to their
   * corresponding {@link ColorRuleGroup} objects.
   */
  private TabularView(Context context, Controller controller,
      TableProperties tp, UserTable table, List<String> elementKeys,
      int defaultForegroundColor,
      int defaultBackgroundColor, int borderColor,
      int[] columnWidths, TableType type, int fontSize,
      Map<String, ColumnProperties> elementKeyToColumnProperties,
      Map<String, ColorRuleGroup> elementKeyToColorRuleGroup) {
    super(context);
    this.controller = controller;
    this.mTp = tp;
    this.mTable = table;
    this.mElementKeys = elementKeys;
    this.defaultBackgroundColor = defaultBackgroundColor;
    this.defaultForegroundColor = defaultForegroundColor;
    this.columnWidths = columnWidths;
    this.type = type;
    this.fontSize = fontSize;
    if (this.type == TableType.INDEX_DATA || 
        this.type == TableType.MAIN_DATA ||
        this.type == TableType.STATUS_DATA) {
      this.mNumberOfRows = this.mTable.getHeight();
    } else if (this.type == TableType.INDEX_FOOTER ||
        this.type == TableType.MAIN_FOOTER || 
        this.type == TableType.STATUS_FOOTER ||
        this.type == TableType.INDEX_HEADER ||
        this.type == TableType.MAIN_HEADER ||
        this.type == TableType.STATUS_HEADER) {
      this.mNumberOfRows = 1;
    } else {
      Log.e(TAG, "Unrecognized TableType in constructor: " + this.type.name());
      this.mNumberOfRows = this.mTable.getHeight();
    }
    this.mColumnIndexMap = this.mTable.getMapOfUserDataToIndex();
    this.mColumnPropertiesMap = elementKeyToColumnProperties;
    this.mColumnColorRules = elementKeyToColorRuleGroup;
    if (this.type != TableType.STATUS_DATA) {
      this.mRowColorRuleGroup = ColorRuleGroup.getTableColorRuleGroup(tp);
    } else {
      this.mRowColorRuleGroup = ColorRuleGroup.getStatusColumnRuleGroup(tp);
    }
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
    totalHeight = (rowHeight + BORDER_WIDTH) * 
        this.mNumberOfRows + BORDER_WIDTH;
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
    if (this.mNumberOfRows > 0) {
      this.xs = new int[this.mElementKeys.size()];
      xs[0] = BORDER_WIDTH;
      for (int i = 0; i < this.mElementKeys.size() - 1; i++) {
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
  
//  public TabularView(Context context, Controller controller,
//      TableProperties tp, UserTable table, List<String> elementKeysToDisplay,
//      int defaultForegroundColor,
//      int defaultBackgroundColor,
//      int borderColor, int[] columnWidths, TableType type, int fontSize) {
//    this(context, controller, tp, new String[][] { data }, null,
//        defaultForegroundColor, defaultBackgroundColor,
//        borderColor, columnWidths, type, fontSize);
//    Log.e(TAG, "wholeData param for this TabularView constructor not " +
//    		"implemented! Use with extreme caution");
//  }

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
    // We don't want to do anything if we're not responsible for drawing any
    // of the rows or columns.
    if (this.mNumberOfRows == 0 || this.mElementKeys.size() == 0) {
      return;
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

    int xScroll = controller.getMainScrollX();
    int yScroll = controller.getMainScrollY();
    if (xScroll < 0) {
      xScroll = 0;
    }
    if (yScroll < 0) {
      yScroll = 0;
    }
    // The element key of the leftmost column of which to display anything, as 
    // determined by the scroll location. It is the leftmost given that 
    // mElementKeys contains the correct order.
    String leftmostElementKey;
    // The element of the rightmost column of which to display anything.
    String rightmostElementKey;
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
        this.type == TableType.MAIN_FOOTER ||
        this.type == TableType.STATUS_HEADER ||
        this.type == TableType.STATUS_FOOTER) {
      topmost = 0;
      bottommost = 0;
    } else {
      // Otherwise, we need to compute the value.
      // First let's get the row.
      topmost = yScroll / (BORDER_WIDTH + rowHeight);
      bottommost = (yScroll + metrics.heightPixels) /
          (BORDER_WIDTH + rowHeight);
      if (bottommost >= this.mNumberOfRows) {
        bottommost = this.mNumberOfRows - 1; // don't want to go beyond the last row
      }
    }
    topmostBorder = topmost * (BORDER_WIDTH + rowHeight);
    topTopmost = topmostBorder + BORDER_WIDTH;
    topBottommostBorder = bottommost * (BORDER_WIDTH + rowHeight);
    bottomBottommost = topBottommostBorder + BORDER_WIDTH + rowHeight;
    // And now let's get the correct column. The math here can't be as simple,
    // b/c unlike rowHeight, columnWidth is not a fixed unit.
    int indexOfLeftmostColumn = getLeftmostColumnBasedOnXScroll(xScroll);
    leftmostElementKey = this.mElementKeys.get(indexOfLeftmostColumn);
    leftLeftmost = xs[indexOfLeftmostColumn];
    leftmostBorder = leftLeftmost - BORDER_WIDTH;
    int indexOfRightmostColumn = 
        getLeftmostColumnBasedOnXScroll(xScroll + metrics.widthPixels);
    rightmostElementKey = this.mElementKeys.get(indexOfRightmostColumn);
    leftRightmost = xs[indexOfRightmostColumn];
    rightRightmostBorder = leftRightmost + columnWidths[indexOfRightmostColumn]
        + BORDER_WIDTH; // i believe at the end, then, this should be total
                        // width, once it is all on the column?
    int yCoord = topmostBorder;
    for (int i = topmost; i < bottommost + 1; i++) {
      canvas.drawRect(leftmostBorder, yCoord, rightRightmostBorder, yCoord +
          BORDER_WIDTH, borderPaint);
      yCoord += rowHeight + BORDER_WIDTH;
    }
    int xCoord = leftmostBorder;
    for (int i = indexOfLeftmostColumn; i < indexOfRightmostColumn + 1; i++) {
    canvas.drawRect(xCoord, topmostBorder, xCoord + BORDER_WIDTH,
        bottomBottommost, borderPaint);
      xCoord += (i == this.mElementKeys.size()) ? 0 : columnWidths[i] + BORDER_WIDTH;
    }
    // drawing the cells
    int y = topTopmost;
    for (int i = topmost; i < bottommost + 1; i++) {
      for (int j = indexOfLeftmostColumn; j < indexOfRightmostColumn + 1; j++) {
        String datum;
        if (this.type == TableType.STATUS_DATA || 
            this.type == TableType.STATUS_HEADER || 
            this.type == TableType.STATUS_FOOTER) {
          datum = DEFAULT_STATUS_COLUMN_VALUE;
        } else if (this.type == TableType.INDEX_HEADER || 
                   this.type == TableType.MAIN_HEADER) {
          datum = 
            this.mTable.getHeaderByElementKey(this.mElementKeys.get(j));
        } else if (this.type == TableType.INDEX_FOOTER || 
                   this.type == TableType.MAIN_FOOTER) {
          datum = this.mTable.getFooterByElementKey(this.mElementKeys.get(j));
        } else if (this.type == TableType.INDEX_DATA ||
                   this.type == TableType.MAIN_DATA) {
          datum = 
              this.mTable.getUserDataByElementKey(i, this.mElementKeys.get(j));
        } else {
          Log.e(TAG, "unrecognized table type: " + this.type.name());
          datum = null;
        }
        if (datum == null) {
          datum = "";
        }
        int foregroundColor = this.defaultForegroundColor;
        int backgroundColor = this.defaultBackgroundColor;
        if (type == TableType.INDEX_DATA ||
            type == TableType.MAIN_DATA) {
          ColorGuide rowGuide = mRowColorRuleGroup.getColorGuide(
              this.mTable.getRowData(i), mColumnIndexMap, mColumnPropertiesMap);
          ColorGuide columnGuide = mColumnColorRules.get(
              this.mElementKeys.get(j)).getColorGuide(
                  this.mTable.getRowData(i), mColumnIndexMap, 
                  mColumnPropertiesMap);
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
        if (type == TableType.STATUS_DATA) {
          ColorGuide statusGuide = mRowColorRuleGroup.getColorGuide(
              this.mTable.getRowData(i), mColumnIndexMap, mColumnPropertiesMap);
          if (statusGuide.didMatch()) {
            foregroundColor = statusGuide.getForeground();
            backgroundColor = statusGuide.getBackground();
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
      int x = highlightedCellNum % this.mElementKeys.size();
      int rowNum = highlightedCellNum / this.mElementKeys.size();
      highlightCell(canvas, xs[x], ((rowNum + 1) * BORDER_WIDTH) +
          (rowNum * rowHeight), columnWidths[x]);
    }
  }

  /**
   * This should return the leftmost column of which anything should be
   * displayed on the screen, where the screen position is specified by the
   * xScroll int.
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
    
    /**
     * Gets the x translation of the scroll. This is in particular how far
     * you have scrolled to look at columns that do not begin onscreen.
     * @return
     */
    int getMainScrollX();
    
    /**
     * Gets the y translation of the scroll. This is in particular the y
     * offset for the actual scrolling of the rows, so that a positive
     * offset will indicate that you have scrolled to some non-zero row.
     * @return
     */
    int getMainScrollY();
  }
}
