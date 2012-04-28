package org.opendatakit.tables.data;

/**
 * A table of all or some of the data in a database table.
 * 
 * @author hkworden
 */
public class Table {
    
    private final String[] rowIds;
    private String[] header;
    private final String[][] data;
    
    public Table(String[] rowIds, String[] header, String[][] data) {
        this.rowIds = rowIds;
        this.header = header;
        this.data = data;
    }
    
    public String[] getRowIds() {
        return rowIds;
    }
    
    public String[][] getData() {
        return data;
    }
    
    public String getRowId(int rowNum) {
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
