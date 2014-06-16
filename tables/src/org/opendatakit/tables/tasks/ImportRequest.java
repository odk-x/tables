package org.opendatakit.tables.tasks;


public class ImportRequest {

    private final String fileQualifier;

    private final boolean createTable;
    private final String tableId;

    public ImportRequest(String tableId, String fileQualifier) {
      this(true, tableId, fileQualifier);
  }

    public ImportRequest(boolean createTable, String tableId, String fileQualifier) {
      this.createTable = createTable;
      this.tableId = tableId;
      this.fileQualifier = fileQualifier;
  }

    public boolean getCreateTable() {
        return createTable;
    }

    public String getTableId() {
        return tableId;
    }

    public String getFileQualifier() {
      return fileQualifier;
    }
}