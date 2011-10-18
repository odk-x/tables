package yoonsung.odk.spreadsheet.view;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.Activity.TableActivity;
import yoonsung.odk.spreadsheet.DataStructure.DisplayPrefs;
import yoonsung.odk.spreadsheet.DataStructure.DisplayPrefs.ColumnColorRuler;
import yoonsung.odk.spreadsheet.DataStructure.Table;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class TableDisplayView extends LinearLayout {
    
    enum TableType { MAIN_DATA, MAIN_HEADER, MAIN_FOOTER,
        INDEX_DATA, INDEX_HEADER, INDEX_FOOTER }
	
    private static final int MIN_CLICK_DURATION = 0;
    private static final int MIN_LONG_CLICK_DURATION = 1000;
    
	private TableActivity ta; // the table activity to call back to
	private Table table; // the table to display
	private int indexedCol; // the indexed column number; -1 if not indexed
	private DisplayPrefs dp; // the display preferences for the table
	
	// views
	private ScrollView indexScroll;
	private ScrollView mainScroll;
	private TableView indexData;
	private TableView indexHeader;
	private TableView indexFooter;
    private TableView mainData;
    private TableView mainHeader;
    private TableView mainFooter;
	
	// cell click listeners
	private View.OnTouchListener mainDataCellClickListener;
	private View.OnTouchListener mainHeaderCellClickListener;
	private View.OnTouchListener mainFooterCellClickListener;
	private View.OnTouchListener indexDataCellClickListener;
    private View.OnTouchListener indexHeaderCellClickListener;
	private View.OnTouchListener indexFooterCellClickListener;
	
	private int lastLongClickedCellId;
	
	public TableDisplayView(Context context, DisplayPrefs dp) {
		super(context);
		this.dp = dp;
		setOrientation(LinearLayout.VERTICAL);
	}
	
	/**
	 * Sets the data for an unindexed table.
	 * @param ta the table activity to call back to
	 * @param table the table to display
	 */
	public void setTable(TableActivity ta, Table table) {
		setTable(ta, table, -1);
	}
	
	/**
	 * Sets the data for a table.
	 * @param ta the table activity to call back to
	 * @param table the table to display
	 * @param indexedCol the number of the indexed column (or -1 for no indexed
	 * column)
	 */
	public void setTable(TableActivity ta, Table table, int indexedCol) {
	    this.ta = ta;
	    this.table = table;
	    this.indexedCol = indexedCol;
	    initListeners();
	    removeAllViews();
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
	
	private void initListeners() {
	    mainDataCellClickListener = new CellClickListener() {
            @Override
            protected int figureCellId(int x, int y) {
                int cellNum = mainData.getCellNumber(x, y);
                if (indexedCol < 0) {
                    Log.d("TDV", "oh hai cellNum:" + cellNum);
                    return cellNum;
                } else {
                    Log.d("TDV", "here cellNum:" + cellNum);
                    int colNum = cellNum % (table.getWidth() - 1);
                    int rowNum = cellNum / (table.getWidth() - 1);
                    return cellNum + rowNum + ((colNum < indexedCol) ? 0 : 1);
                }
            }
            @Override
            protected void takeClickAction(int cellId) {
                ta.regularCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId) {
                ta.openContextMenu(mainData);
            }
	    };
	    mainHeaderCellClickListener = new CellClickListener() {
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
                ta.headerCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId) {
                ta.openContextMenu(mainHeader);
            }
        };
        mainFooterCellClickListener = new CellClickListener() {
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
                ta.footerCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId) {
                ta.openContextMenu(mainFooter);
            }
        };
        indexDataCellClickListener = new CellClickListener() {
            @Override
            protected int figureCellId(int x, int y) {
                int cellNum = indexData.getCellNumber(x, y);
                return (cellNum * table.getWidth()) + indexedCol;
            }
            @Override
            protected void takeClickAction(int cellId) {
                ta.indexedColCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId) {
                ta.openContextMenu(indexData);
            }
        };
        indexHeaderCellClickListener = new CellClickListener() {
            @Override
            protected int figureCellId(int x, int y) {
                return indexedCol;
            }
            @Override
            protected void takeClickAction(int cellId) {
                ta.headerCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId) {
                ta.openContextMenu(indexHeader);
            }
        };
        indexFooterCellClickListener = new CellClickListener() {
            @Override
            protected int figureCellId(int x, int y) {
                return indexedCol;
            }
            @Override
            protected void takeClickAction(int cellId) {
                ta.footerCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId) {
                ta.openContextMenu(indexFooter);
            }
        };
	}
	
	private void buildNonIndexedTable() {
        // building wrapper
        View wrapper = buildTable(-1, false);
        HorizontalScrollView wrapScroll = new HorizontalScrollView(ta);
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
        HorizontalScrollView wrapScroll = new HorizontalScrollView(ta);
        wrapScroll.addView(mainWrapper, LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        LinearLayout wrapper = new LinearLayout(ta);
        wrapper.addView(indexWrapper);
        wrapper.addView(wrapScroll);
        addView(wrapper);
        indexScroll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                mainScroll.scrollTo(mainScroll.getScrollX(), view.getScrollY());
                return false;
            }
        });
        mainScroll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                indexScroll.scrollTo(indexScroll.getScrollX(), view.getScrollY());
                return false;
            }
        });
	}
	
	private View buildTable(int indexedCol, boolean isIndexed) {
	    String[][] header;
	    String[][] data;
	    String[][] footer;
        ColumnColorRuler[] colorRulers;
        int[] colWidths;
        int[] completeColWidths = ta.getColWidths();
	    if (isIndexed) {
	        header = new String[1][1];
	        header[0][0] = table.getColName(indexedCol);
	        data = new String[table.getHeight()][1];
	        for (int i = 0; i < table.getHeight(); i++) {
	            data[i][0] =
	                table.getCellValue((i * table.getWidth()) + indexedCol);
	        }
            footer = new String[1][1];
            footer[0][0] = table.getFooterValue(indexedCol);
            colorRulers = new ColumnColorRuler[1];
            colorRulers[0] = dp.getColColorRuler(table.getColName(indexedCol));
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
                header[0][addIndex] = table.getColName(i);
                for (int j = 0; j < table.getHeight(); j++) {
                    data[j][addIndex] =
                        table.getCellValue((j * table.getWidth()) + i);
                }
                footer[0][addIndex] = table.getFooterValue(i);
                colorRulers[addIndex] =
                    dp.getColColorRuler(table.getColName(i));
                colWidths[addIndex] = completeColWidths[i];
                addIndex++;
            }
	    }
	    int avanda = getResources().getColor(R.color.Avanda);
        int headerData = getResources().getColor(R.color.header_data);
	    int headerIndex = getResources().getColor(R.color.header_index);
	    int footerIndex = getResources().getColor(R.color.footer_index);
        ScrollView dataScroll = new ScrollView(ta);
        TableView dataTable = new TableView(ta, data, avanda, Color.BLACK,
                Color.GRAY, colWidths, colorRulers,
                (isIndexed ? TableType.INDEX_DATA : TableType.MAIN_DATA));
        dataScroll.addView(dataTable, new ViewGroup.LayoutParams(
                dataTable.getTableWidth(), dataTable.getTableHeight()));
        TableView headerTable = new TableView(ta, header,
                (isIndexed ? headerIndex : headerData), Color.BLACK,
                Color.GRAY, colWidths, null,
                (isIndexed ? TableType.INDEX_HEADER : TableType.MAIN_HEADER));
        TableView footerTable = new TableView(ta, footer, footerIndex,
                Color.BLACK, Color.GRAY, colWidths, null,
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
        LinearLayout wrapper = new LinearLayout(ta);
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
	
	private void onCreateMainDataContextMenu(ContextMenu menu, int cellId) {
	    ta.prepRegularCellOccm(menu, cellId);
	}
    
    private void onCreateIndexDataContextMenu(ContextMenu menu, int cellId) {
        ta.prepIndexedColCellOccm(menu, cellId);
    }
    
    private void onCreateHeaderContextMenu(ContextMenu menu, int cellId) {
        ta.prepHeaderCellOccm(menu, cellId);
    }
    
    private void onCreateFooterContextMenu(ContextMenu menu, int cellId) {
        ta.prepFooterCellOccm(menu, cellId);
    }
	
	private class TableView extends View {
	    
	    private static final int ROW_HEIGHT = 30;
	    private static final int HORIZONTAL_CELL_PADDING = 5;
	    private static final int VERTICAL_CELL_PADDING = 9;
	    private static final int BORDER_WIDTH = 1;
	    
	    private String[][] data;
	    private int backgroundColor;
	    private int foregroundColor;
	    private int borderColor;
	    private int[] columnWidths;
	    private ColumnColorRuler[] colorRulers;
	    private TableType type;
	    
	    private int totalHeight;
	    private int totalWidth;
	    
	    public TableView(Context context, String[][] data, int backgroundColor,
	            int foregroundColor, int borderColor, int[] columnWidths,
	            ColumnColorRuler[] colorRulers, TableType type) {
	        super(context);
            init(data, backgroundColor, foregroundColor, borderColor,
                    columnWidths, colorRulers, type);
	    }
        
        public TableView(Context context, String[] data, int backgroundColor,
                int foregroundColor, int borderColor, int[] columnWidths,
                ColumnColorRuler[] colorRulers, TableType type) {
            super(context);
            String[][] outerData = {data};
            init(outerData, backgroundColor, foregroundColor, borderColor,
                    columnWidths, colorRulers, type);
        }
        
        private void init(String[][] data, int backgroundColor,
                int foregroundColor, int borderColor, int[] columnWidths,
                ColumnColorRuler[] colorRulers, TableType type) {
            this.data = data;
            this.backgroundColor = backgroundColor;
            this.foregroundColor = foregroundColor;
            this.borderColor = borderColor;
            this.columnWidths = columnWidths;
            this.colorRulers = colorRulers;
            totalHeight = (ROW_HEIGHT + BORDER_WIDTH) * data.length +
                    BORDER_WIDTH;
            totalWidth = BORDER_WIDTH;
            for (int i = 0; i < columnWidths.length; i++) {
                totalWidth += columnWidths[i] + BORDER_WIDTH;
            }
            setMinimumHeight(totalHeight);
            setMinimumWidth(totalWidth);
            this.type = type;
            setClickable(true);
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
	    
	    @Override
	    public void onDraw(Canvas canvas) {
	        if (data.length == 0) {
	            return;
	        }
	        Paint paint = new Paint();
	        // drawing the background
	        paint.setColor(backgroundColor);
	        canvas.drawRect(0, 0, totalWidth, totalHeight, paint);
	        // drawing the borders
	        paint.setColor(borderColor);
	        int yCoord = 0;
	        for (int i = 0; i <= data.length; i++) {
	            canvas.drawRect(0, yCoord, totalWidth, yCoord + BORDER_WIDTH,
	                    paint);
	            yCoord += ROW_HEIGHT + BORDER_WIDTH;
	        }
            int xCoord = 0;
            for (int i = 0; i <= data[0].length; i++) {
                canvas.drawRect(xCoord, 0, xCoord + BORDER_WIDTH, totalHeight,
                        paint);
                xCoord += (i == data[0].length) ? 0 :
                        columnWidths[i] + BORDER_WIDTH;
            }
	        // drawing the cells
            paint.setTextSize(16);
	        int x = BORDER_WIDTH;
	        int y = BORDER_WIDTH;
	        for (int i = 0; i < data.length; i++) {
	            for (int j = 0; j < data[0].length; j++) {
	                String datum = data[i][j];
	                int columnWidth = columnWidths[j];
	                int foregroundColor = (colorRulers == null) ?
	                        this.foregroundColor :
                            colorRulers[j].getForegroundColor(datum,
                                    this.foregroundColor);
                    int backgroundColor = (colorRulers == null) ?
                            this.backgroundColor :
                            colorRulers[j].getBackgroundColor(datum,
                                    this.backgroundColor);
	                drawCell(canvas, paint, x, y, datum, backgroundColor,
	                        foregroundColor, columnWidth);
	                x += columnWidth + BORDER_WIDTH;
	            }
	            x = BORDER_WIDTH;
	            y += ROW_HEIGHT + BORDER_WIDTH;
	        }
	    }
	    
	    private void drawCell(Canvas canvas, Paint paint, int x, int y,
	            String datum, int backgroundColor, int foregroundColor,
	            int columnWidth) {
            if (backgroundColor != this.backgroundColor) {
                paint.setColor(backgroundColor);
                canvas.drawRect(x, y, x + columnWidth, y + ROW_HEIGHT, paint);
            }
	        canvas.save(Canvas.ALL_SAVE_FLAG);
	        canvas.clipRect(x + HORIZONTAL_CELL_PADDING, y,
	                x + columnWidth - (2 * HORIZONTAL_CELL_PADDING),
	                y + ROW_HEIGHT);
	        paint.setColor(foregroundColor);
	        canvas.drawText(datum, x + HORIZONTAL_CELL_PADDING,
	                (y + ROW_HEIGHT - VERTICAL_CELL_PADDING), paint);
	        canvas.restore();
	    }
	    
	    @Override
	    public void onCreateContextMenu(ContextMenu menu) {
	        switch(type) {
	        case MAIN_DATA:
	            onCreateMainDataContextMenu(menu, lastLongClickedCellId);
	            return;
	        case INDEX_DATA:
	            onCreateIndexDataContextMenu(menu, lastLongClickedCellId);
	            return;
	        case MAIN_HEADER:
	        case INDEX_HEADER:
	            onCreateHeaderContextMenu(menu, lastLongClickedCellId);
	            return;
            case MAIN_FOOTER:
            case INDEX_FOOTER:
                onCreateFooterContextMenu(menu, lastLongClickedCellId);
                return;
	        }
	    }
	}
	
	private abstract class CellClickListener implements View.OnTouchListener {
	    
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            long duration = event.getEventTime() - event.getDownTime();
            if (event.getAction() != MotionEvent.ACTION_UP ||
                    duration < MIN_CLICK_DURATION) {
                return false;
            }
            int x = (new Float(event.getX())).intValue();
            int y = (new Float(event.getY())).intValue();
            int cellId = figureCellId(x, y);
            if (duration < MIN_LONG_CLICK_DURATION) {
                takeClickAction(cellId);
                return true;
            } else {
                lastLongClickedCellId = cellId;
                takeLongClickAction(cellId);
                return true;
            }
        }
	    
	    protected abstract int figureCellId(int x, int y);
	    
	    protected abstract void takeClickAction(int cellId);
        
        protected abstract void takeLongClickAction(int cellId);
	}
}
