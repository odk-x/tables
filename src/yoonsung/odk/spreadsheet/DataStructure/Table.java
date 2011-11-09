package yoonsung.odk.spreadsheet.DataStructure;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import yoonsung.odk.spreadsheet.Database.ColumnProperty;
import yoonsung.odk.spreadsheet.Database.DataUtils;

public class Table {
	
	private String tableID;
	private int width;
	private int height;
	private ArrayList<Integer> rowID;
	private ArrayList<String> header;
	private ArrayList<String> rawData;
	private ArrayList<String> data;
	private ArrayList<String> footer;
		
	public Table() {
		new Table(null, 0, 0, null, null, null, null);
	}
	
	public Table(String tableID,
				 int width, int height, 
				 ArrayList<Integer> rowID,
				 ArrayList<String> header,
				 ArrayList<String> data, 
				 ArrayList<String> footer) {
		
		this.tableID = tableID;
		this.width = width;
		this.height = height;
		this.rowID = rowID;
		this.header = header;
		this.rawData = data;
		this.footer = footer;
		
		ColumnProperty cp = new ColumnProperty(tableID);
		
		List<Integer> drList = new ArrayList<Integer>();
		List<Integer> dtList = new ArrayList<Integer>();
		for(int i=0; i<header.size(); i++) {
			if(("Date Range").equals(cp.getType(header.get(i)))) {
				drList.add(i);
			} else if(("Date").equals(cp.getType(header.get(i)))) {
			    dtList.add(i);
			}
		}

		this.data = new ArrayList<String>(height * width);
        DataUtils du = DataUtils.getInstance();
		for (int i = 0; i < height; i++) {
		    for (int j = 0; j < width; j++) {
	            boolean isDr = drList.contains(j);
	            boolean isDt = dtList.contains(j);
		        int index = (i * width) + j;
		        if (isDr) {
		            try {
		                Date[] dr = du.parseDateRangeFromDB(
		                        rawData.get(index));
		                this.data.add(du.formatDateRangeForDisplay(dr));
		            } catch(ParseException e) {
		                e.printStackTrace();
		            }
		        } else if (isDt) {
		            try {
		                Date d = du.parseDateTimeFromDB(rawData.get(index));
		                this.data.add(du.formatDateTimeForDisplay(d));
		            } catch(ParseException e) {
		                e.printStackTrace();
		            }
		        } else {
		            this.data.add(rawData.get(index));
		        }
		    }
		}
	}
	
	public int getWidth() {
		return this.width;
	}
	
	public int getHeight() {
		return this.height;
	}
		
	public ArrayList<Integer> getRowID() {
		return this.rowID;
	}
	
	public ArrayList<String> getHeader() {
		return this.header;
	}
	
	public ArrayList<String> getData() {
		return this.data;
	}
	
	public ArrayList<String> getRawData() {
	    return this.rawData;
	}
	
	public ArrayList<String> getFooter() {
		return this.footer;
	}
	
	public String getCellValue(int cellLoc) {
		return data.get(cellLoc);
	}
	
	public void setCellValue(int cellLoc, String value) {
	    data.set(cellLoc, value);
	}
	
	public ArrayList<String> getRow(int rowNum) {
		if (height > rowNum) {
			ArrayList<String> row = new ArrayList<String>();
			for (int r = (rowNum * width); r < (rowNum * width) + width; r++)
				row.add(data.get(r));
			return row;
		} else {
			return null;
		}
	}
	
	public ArrayList<String> getCol(int colNum) {
		if (width > colNum) {
			ArrayList<String> col = new ArrayList<String>();
			for (int c = colNum; c < (height * width + colNum); c = c + width)
				col.add(data.get(c));
			return col;
		} else {
			return null;
		}
	}
	
	public ArrayList<String> getRawColumn(int colNum) {
	    ArrayList<String> col = new ArrayList<String>();
	    for (int i = colNum; i < ((height * width) + colNum); i += width) {
	        col.add(rawData.get(i));
	    }
	    return col;
	}
	
	public int getRowNum(int position) {
		return (position / width);
	}
	
	public int getColNum(int position) {
		return (position % width);
	}
	
	public int getTableRowID(int rowNum) {
		return rowID.get(rowNum);
	}
	
	public String getColName(int colNum) {
		return header.get(colNum);
	}
	
	public String getFooterValue(int colNum) {
	    return footer.get(colNum);
	}
	
	public int getColNum(String colName) {
		int result = -1;
		for (int i = 0; i < header.size(); i++){
			if (header.get(i).equals(colName)) {
				result = i;
			}
		}
		return result;
	}
}
