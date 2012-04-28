package org.opendatakit.tables.data;


public class UserTable {
    
    private final String[] rowIds;
    private final String[] header;
    private final String[][] data;
    private final String[][] userData;
    private final String[] footer;
    
    public UserTable(String[] rowIds, String[] header, String[][] data,
            String[] footer) {
        this.rowIds = rowIds;
        this.header = header;
        this.data = data;
        int columnCount = data.length > 0 ? data[0].length : 0;
        userData = new String[data.length][columnCount];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < columnCount; j++) {
                userData[i][j] = data[i][j];
            }
        }
        this.footer = footer;
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
    
    public String getData(int cellNum) {
        int rowNum = cellNum / getWidth();
        int colNum = cellNum % getWidth();
        return getData(rowNum, colNum);
    }
    
    public String getUserData(int rowNum, int colNum) {
        return userData[rowNum][colNum];
    }
    
    public String getFooter(int colNum) {
        return footer[colNum];
    }
    
    public int getWidth() {
        return header.length;
    }
    
    public int getHeight() {
        return data.length;
    }
    
    public void setData(int rowNum, int colNum, String value) {
        data[rowNum][colNum] = value;
    }
    
    public void setData(int cellNum, String value) {
        int rowNum = cellNum / getWidth();
        int colNum = cellNum % getWidth();
        setData(rowNum, colNum, value);
    }
}
