package yoonsung.odk.spreadsheet.view;

import java.util.List;
import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.Activity.TableActivity;
import yoonsung.odk.spreadsheet.DataStructure.Table;
import android.content.Context;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * CELL ID NUMBERS:
 * Regular cells have IDs from 0 up ((rowNum * tableWidth) + colNum)
 * Cells in an indexed column are IDed as though they were regular cells
 * Header and footer cell IDs are their column number
 */
public class TableDisplayView extends TableRow {
	
	private TableActivity ta; // the table activity to call back to
	private Table table; // the table to display
	private int indexedCol; // the indexed column number; -1 if not indexed
	
	// scroll views
	private ScrollView indexScroll;
	private ScrollView mainScroll;
	
	// cell click listeners
	private View.OnClickListener regularCellClickListener;
	private View.OnClickListener headerCellClickListener;
	private View.OnClickListener indexedColCellClickListener;
	private View.OnClickListener footerCellClickListener;
	
	public TableDisplayView(Context context) {
		super(context);
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
	public void setTable(TableActivity ta, Table table,
			int indexedCol) {
		Log.d("REFACTOR SSJ", "table data size:" + table.getData().size());
		this.indexedCol = indexedCol;
		removeAllViews();
		this.ta = ta;
		this.table = table;
		prepCellClickListeners();
		if(indexedCol < 0) {
			buildNonIndexedTable();
		} else {
			buildIndexedTable(indexedCol);
		}
	}
	
	/**
	 * Gets the text of a content cell.
	 * @param cellID the cell ID
	 * @return the cell content
	 */
	public String getCellContent(int cellID) {
		return getCellById(cellID).getText().toString();
	}
	
	/**
	 * Sets the text of a content cell.
	 * @param cellID the cell ID
	 * @param val the new text
	 */
	public void setCellContent(int cellID, String val) {
		getCellById(cellID).setText(val);
	}
	
	/**
	 * Highlights a content cell.
	 * @param cellID the cell ID
	 */
	public void highlightCell(int cellID) {
		TextView cell = getCellById(cellID);
		cell.setBackgroundResource(R.drawable.cell_selected);
		cell.setPadding(5, 5, 5, 5);
	}
	
	/**
	 * Unhighlights a content cell.
	 * @param cellID the cell ID
	 */
	public void unhighlightCell(int cellID) {
		TextView cell = getCellById(cellID);
		cell.setBackgroundColor(getResources().getColor(R.color.Avanda));
		cell.setPadding(5, 5, 5, 5);
	}
	
	/**
	 * Gets a cell by ID.
	 * @param cellID the cell ID
	 * @return the cell
	 */
	private TextView getCellById(int cellID) {
		View view;
		if((cellID % table.getWidth()) == indexedCol) {
			view = indexScroll;
		} else {
			view = mainScroll;
		}
		return (TextView) view.findViewById(cellID);
	}
	
	/**
	 * Builds a table view with no index.
	 * @return the table
	 */
	private void buildNonIndexedTable() {
		HorizontalScrollView hsv = new HorizontalScrollView(ta);
		hsv.addView(buildTable(-1, false));
		addView(hsv, LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
	}
	
	/**
	 * Builds a table view with an index.
	 * @param indexedCol the indexed column
	 * @return the table
	 */
	private void buildIndexedTable(int indexedCol) {
		Log.d("REFACTOR SSJ", "buildIndexedTable called:" + indexedCol);
		addView(buildTable(indexedCol, true));
		HorizontalScrollView hsv = new HorizontalScrollView(ta);
		hsv.addView(buildTable(indexedCol, false));
		addView(hsv, LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		// scroll syncing
		indexScroll.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mainScroll.scrollTo(0, v.getScrollY());
				return false;
			}
		});
		mainScroll.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				indexScroll.scrollTo(0, v.getScrollY());
				return false;
			}
		});
	}
	
	/**
	 * Builds a table view.
	 * @param indexedCol the indexed column
	 * @param isIndex true for the index table; false otherwise
	 * @return the table
	 */
	private View buildTable(int indexedCol, boolean isIndex) {
		// header
		RelativeLayout header = new RelativeLayout(ta);
		if(isIndex) {
			header.addView(buildIndexColHeaderTable(indexedCol),
					LinearLayout.LayoutParams.WRAP_CONTENT, 30);
		} else {
			header.addView(buildMainHeaderTable(indexedCol),
					LinearLayout.LayoutParams.WRAP_CONTENT, 30);
		}
		// content
		RelativeLayout content = new RelativeLayout(ta);
		if(isIndex) {
			indexScroll = new ScrollView(ta);
			indexScroll.addView(buildIndexedColContentTable(indexedCol));
			content.addView(indexScroll,
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
		} else {
			mainScroll = new ScrollView(ta);
			mainScroll.addView(buildMainContentTable(indexedCol));
			content.addView(mainScroll,
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);
		}
		// footer
		RelativeLayout footer = new RelativeLayout(ta);
		if(isIndex) {
			footer.addView(buildIndexColFooterTable(indexedCol),
					LinearLayout.LayoutParams.WRAP_CONTENT, 30);
		} else {
			footer.addView(buildMainFooterTable(indexedCol),
					LinearLayout.LayoutParams.WRAP_CONTENT, 30);
		}
		// wrapping them up
		RelativeLayout wrapper = new RelativeLayout(ta);
		Display display =
			((WindowManager) ta.getSystemService(Context.WINDOW_SERVICE)).
			getDefaultDisplay();
		wrapper.addView(content, LinearLayout.LayoutParams.WRAP_CONTENT,
				display.getHeight() - 150);
		wrapper.addView(header);
		RelativeLayout.LayoutParams footerParams =
			new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.WRAP_CONTENT,
			RelativeLayout.LayoutParams.WRAP_CONTENT);
		footerParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		wrapper.addView(footer, footerParams);
		return wrapper;
	}
	
	/**
	 * Builds the main header view.
	 * @param indexedCol the number of the indexed column, or -1 if no column
	 * is indexed
	 * @return the view
	 */
	private View buildMainHeaderTable(int indexedCol) {
		return buildMainHeaderOrFooter(indexedCol, true);
	}
	
	/**
	 * Builds the index header view.
	 * @param indexedCol the number of the indexed column
	 * @return the view
	 */
	private View buildIndexColHeaderTable(int indexedCol) {
		return buildIndexColHeaderOrFooter(indexedCol, true);
	}
	
	/**
	 * Builds the main content table view.
	 * @param indexedCol the number of the indexed column, or -1 if no column
	 * is indexed
	 * @return the view
	 */
	private View buildMainContentTable(int indexedCol) {
		TableLayout ct = new TableLayout(ta);
		LinearLayout.LayoutParams cellLP = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		cellLP.setMargins(1, 1, 1, 1);
		LinearLayout.LayoutParams rowLP = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		// adding dummy row
		TableRow dummyRow = new TableRow(ta);
		TextView dummyCell = createRegularCell("", -1, 1);
		LinearLayout dummyCellWrapper = new LinearLayout(ta);
		dummyCellWrapper.addView(dummyCell, cellLP);
		dummyRow.addView(dummyCellWrapper);
		ct.addView(dummyRow, rowLP);
		// adding table data
		List<String> tableData = table.getData();
		int tableWidth = table.getWidth();
		int[] colWidths = ta.getColWidths();
		for(int i=0; i<table.getHeight(); i++) {
			TableRow row = new TableRow(ta);
			for(int j=0; j<tableWidth; j++) {
				if(j != indexedCol) {
					int id = (i * tableWidth) + j;
					View cell = createRegularCell(tableData.get(id), id,
							colWidths[j]);
					LinearLayout cellWrapper = new LinearLayout(ta);
					cellWrapper.addView(cell, cellLP);
					row.addView(cellWrapper);
				}
			}
			ct.addView(row, rowLP);
		}
		return ct;
	}
	
	/**
	 * Builds an indexed column content table view.
	 * @param indexedCol the number of the indexed column
	 * @return the view
	 */
	private View buildIndexedColContentTable(int indexedCol) {
		TableLayout ct = new TableLayout(ta);
		LinearLayout.LayoutParams cellLP = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		cellLP.setMargins(1, 1, 1, 1);
		LinearLayout.LayoutParams rowLP = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		// adding dummy row
		TableRow dummyRow = new TableRow(ta);
		TextView dummyCell = createRegularCell("", -1, 1);
		LinearLayout dummyCellWrapper = new LinearLayout(ta);
		dummyCellWrapper.addView(dummyCell, cellLP);
		dummyRow.addView(dummyCellWrapper);
		ct.addView(dummyRow, rowLP);
		List<String> tableData = table.getData();
		int tableWidth = table.getWidth();
		int width = ta.getColWidths()[indexedCol];
		for(int i=indexedCol; i<tableData.size(); i+=tableWidth) {
			TableRow row = new TableRow(ta);
			int id = i;
			View cell = createIndexedColCell(tableData.get(id), id, width);
			LinearLayout cellWrapper = new LinearLayout(ta);
			cellWrapper.addView(cell, cellLP);
			row.addView(cellWrapper);
			ct.addView(row, rowLP);
		}
		return ct;
	}
	
	/**
	 * Builds the main footer view.
	 * @param indexedCol the number of the indexed column, or -1 if no column
	 * is indexed
	 * @return the view
	 */
	private View buildMainFooterTable(int indexedCol) {
		return buildMainHeaderOrFooter(indexedCol, false);
	}
	
	/**
	 * Builds the index footer view.
	 * @param indexedCol the number of the indexed column
	 * @return the view
	 */
	private View buildIndexColFooterTable(int indexedCol) {
		return buildIndexColHeaderOrFooter(indexedCol, false);
	}
	
	/**
	 * Builds a main header or footer view.
	 * @param indexedCol the number of the indexed column, or -1 if no column
	 * is indexed
	 * @param isHeader true for building a header; false for building a footer
	 * @return the view
	 */
	private View buildMainHeaderOrFooter(int indexedCol, boolean isHeader) {
		TableLayout hft = new TableLayout(ta);
		List<String> tableHF;
		if(isHeader) {
			tableHF = table.getHeader();
		} else {
			tableHF = table.getFooter();
		}
		int tableWidth = table.getWidth();
		LinearLayout.LayoutParams cellLP = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		cellLP.setMargins(1, 1, 1, 1);
		LinearLayout.LayoutParams rowLP = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		int[] colWidths = ta.getColWidths();
		TableRow row = new TableRow(ta);
		for(int i=0; i<tableWidth; i++) {
			if(i != indexedCol) {
				View cell;
				if(isHeader) {
					cell = createHeaderCell(tableHF.get(i), i, colWidths[i]);
				} else {
					cell = createFooterCell(tableHF.get(i), i, colWidths[i]);
				}
				LinearLayout cellWrapper = new LinearLayout(ta);
				cellWrapper.addView(cell, cellLP);
				row.addView(cellWrapper);
			}
		}
		hft.addView(row, rowLP);
		return hft;
	}
	
	/**
	 * Builds an index column header or footer view.
	 * @param indexedCol the number of the indexed column
	 * @param isHeader true for building a header; false for building a footer
	 * @return the view
	 */
	private View buildIndexColHeaderOrFooter(int indexedCol,
			boolean isHeader) {
		TableLayout hft = new TableLayout(ta);
		List<String> tableHF;
		if(isHeader) {
			tableHF = table.getHeader();
		} else {
			tableHF = table.getFooter();
		}
		int width = ta.getColWidths()[indexedCol];
		TableRow row = new TableRow(ta);
		View cell;
		if(isHeader) {
			cell = createIndexedHeaderCell(tableHF.get(indexedCol), indexedCol,
					width);
		} else {
			cell = createFooterCell(tableHF.get(indexedCol), indexedCol,
					width);
		}
		LinearLayout.LayoutParams cellLP = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		cellLP.setMargins(1, 1, 1, 1);
		LinearLayout.LayoutParams rowLP = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		LinearLayout cellWrapper = new LinearLayout(ta);
		cellWrapper.addView(cell, cellLP);
		row.addView(cellWrapper);
		hft.addView(row, rowLP);
		return hft;
	}
	
	/**
	 * Prepares the cell click listeners.
	 */
	private void prepCellClickListeners() {
		regularCellClickListener = new RegularCellClickListener();
		headerCellClickListener = new HeaderCellClickListener();
		indexedColCellClickListener = new IndexedColCellClickListener();
		footerCellClickListener = new FooterCellClickListener();
	}
	
	/**
	 * Creates a cell.
	 * @param text the text to display in the cell
	 * @param id the ID to assign to the cell
	 * @param textColor the text color
	 * @param bgColor the background color
	 * @param width the width of the cell
	 * @return the text view
	 */
	private TextView createCell(String text, int id, int textColor,
			int bgColor, int width) {
		TextView cell = new TextView(ta);
		cell.setId(id);
		cell.setText(text);
		cell.setMaxLines(1);
		cell.setTextColor(textColor);
		cell.setBackgroundColor(bgColor);
		cell.setWidth(width);
		cell.setPadding(5, 5, 5, 5);
		cell.setClickable(true);
		return cell;
	}
	
	/**
	 * Creates a regular cell.
	 * @param text the text to display in the cell
	 * @param id the ID to assign to the cell
	 * @param width the width of the cell
	 */
	private TextView createRegularCell(String text, int id, int width) {
		TextView cell = createCell(text, id,
				getResources().getColor(R.color.black),
				getResources().getColor(R.color.Avanda), width);
		cell.setOnClickListener(regularCellClickListener);
		ta.prepRegularCellOccmListener(cell);
		return cell;
	}
	
	/**
	 * Creates a header cell.
	 * @param text the text to display in the cell
	 * @param id the ID to assign to the cell
	 * @param width the width of the cell
	 */
	private TextView createHeaderCell(String text, int id, int width) {
		TextView cell = createCell(text, id,
				getResources().getColor(R.color.black),
				getResources().getColor(R.color.header_data), width);
		cell.setOnClickListener(headerCellClickListener);
		ta.prepHeaderCellOccmListener(cell);
		return cell;
	}
    
    /**
     * Creates an indexed header cell.
     * @param text the text to display in the cell
     * @param id the ID to assign to the cell
     * @param width the width of the cell
     */
    private TextView createIndexedHeaderCell(String text, int id, int width) {
        TextView cell = createCell(text, id,
                getResources().getColor(R.color.black),
                getResources().getColor(R.color.header_index), width);
        cell.setOnClickListener(headerCellClickListener);
        ta.prepHeaderCellOccmListener(cell);
        return cell;
    }
	
	/**
	 * Creates an indexed column cell.
	 * @param text the text to display in the cell
	 * @param id the ID to assign to the cell
	 * @param width the width of the cell
	 */
	private TextView createIndexedColCell(String text, int id, int width) {
		TextView cell = createCell(text, id,
				getResources().getColor(R.color.black),
				getResources().getColor(R.color.Avanda), width);
		cell.setOnClickListener(indexedColCellClickListener);
		ta.prepIndexedColCellOccmListener(cell);
		return cell;
	}
	
	/**
	 * Creates a footer cell.
	 * @param text the text to display in the cell
	 * @param id the ID to assign to the cell
	 * @param width the width of the cell
	 */
	private TextView createFooterCell(String text, int id, int width) {
		TextView cell = createCell(text, id,
				getResources().getColor(R.color.black),
				getResources().getColor(R.color.footer_index), width);
		cell.setOnClickListener(footerCellClickListener);
		ta.prepFooterCellOccmListener(cell);
		return cell;
	}
	
	// CELL CLICK LISTENERS
	
	private class RegularCellClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			ta.regularCellClicked(v.getId());
		}
	}
	
	private class HeaderCellClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			ta.headerCellClicked(v.getId());
		}
	}
	
	private class IndexedColCellClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			ta.indexedColCellClicked(v.getId());
		}
	}
	
	private class FooterCellClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			ta.footerCellClicked(v.getId());
		}
	}
	
}
