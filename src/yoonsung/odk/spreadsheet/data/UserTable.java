package yoonsung.odk.spreadsheet.data;


public class UserTable {
    
    private final int[] rowIds;
    private final String[][] data;
    private final String[][] userData;
    private final String[] footer;
    
    public UserTable(int[] rowIds, String[][] data, String[] footer) {
        this.rowIds = rowIds;
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
    
    public int getRowId(int rowNum) {
        return rowIds[rowNum];
    }
    
    public String getData(int rowNum, int colNum) {
        return data[rowNum][colNum];
    }
    
    public String getUserData(int rowNum, int colNum) {
        return userData[rowNum][colNum];
    }
    
    public String getFooter(int colNum) {
        return footer[colNum];
    }
}
