package yoonsung.odk.spreadsheet.DataStructure;
import java.util.ArrayList;

public class Table {
	
	private int width;
	private int height;
	private ArrayList<Integer> rowID;
	private ArrayList<String> header;
	private ArrayList<String> data;
	private ArrayList<String> footer;
		
	public Table() {
		new Table(0, 0, null, null, null, null);
	}
	
	public Table(int width, int height, 
				 ArrayList<Integer> rowID,
				 ArrayList<String> header,
				 ArrayList<String> data, 
				 ArrayList<String> footer) {
		this.width = width;
		this.height = height;
		this.rowID = rowID;
		this.header = header;
		this.data = data;
		this.footer = footer;
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
