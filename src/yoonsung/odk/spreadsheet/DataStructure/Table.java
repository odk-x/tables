package yoonsung.odk.spreadsheet.DataStructure;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import yoonsung.odk.spreadsheet.Database.ColumnProperty;

public class Table {
	
	private String tableID;
	private int width;
	private int height;
	private ArrayList<Integer> rowID;
	private ArrayList<String> header;
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
		this.data = data;
		this.footer = footer;
		
		ColumnProperty cp = new ColumnProperty(tableID);
		
		// user-friendlifying the date range strings
		List<Integer> drList = new ArrayList<Integer>();
		for(int i=0; i<header.size(); i++) {
			if(("Date Range").equals(cp.getType(header.get(i)))) {
				drList.add(i);
			}
		}
		DateFormat dispForm = new SimpleDateFormat("MMM d yyyy, HH:mm");
		for(int i : drList) {
			for (int c = i; c < (height * width + i); c+=width) {
				String[] spl = data.get(c).split("/");
				if(spl.length > 1) {
					String start = dispForm.format(getTime(spl[0]));
					String end = dispForm.format(getTime(spl[1]));
					data.set(c, start + " - " + end);
				}
			}
		}
		
	}
	
	private Date getTime(String timeStr) {
		Calendar cal = Calendar.getInstance();
		String[] timeSpl = timeStr.split(":");
		cal.set(new Integer(timeSpl[0]), (new Integer(timeSpl[1]) - 1),
				new Integer(timeSpl[2]), new Integer(timeSpl[3]),
				new Integer(timeSpl[4]), new Integer(timeSpl[5]));
		return cal.getTime();
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
	
	public ArrayList<String> getFooter() {
		return this.footer;
	}
	
	public String getCellValue(int cellLoc) {
		return data.get(cellLoc);
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
