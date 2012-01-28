package yoonsung.odk.spreadsheet.data;

/**
 * A table of all or some of the data in a database table.
 * 
 * @author hkworden
 */
public class Table {
    
    private final int[] rowIds;
    private final String[][] data;
    
    Table(int[] rowIds, String[][] data) {
        this.rowIds = rowIds;
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
    
    public String getData(int rowNum, int colNum) {
        return data[rowNum][colNum];
    }
}
