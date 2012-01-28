package yoonsung.odk.spreadsheet.data;

/**
 * A table of all or some of the data in a database table.
 * 
 * @author hkworden
 */
public class Table {
    
    private final int[] rowIds;
    private String[] header;
    private final String[][] data;
    
    Table(int[] rowIds, String[] header, String[][] data) {
        this.rowIds = rowIds;
        this.header = header;
        this.data = data;
    }
    
    int[] getRowIds() {
        return rowIds;
    }
    
    String[][] getData() {
        return data;
    }
    
    public int getRowId(int rowNum) {
        return rowIds[rowNum];
    }
    
    public String getHeader(int colNum) {
        return header[colNum];
    }
    
    public String getData(int rowNum, int colNum) {
        return data[rowNum][colNum];
    }
    
    public int getWidth() {
        return header.length;
    }
    
    public int getHeight() {
        return data.length;
    }
}
