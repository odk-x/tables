package org.opendatakit.tables.tasks;

import java.io.File;

public class ImportRequest {

    private final String fileQualifier;

    private final boolean createTable;
    private final String tableId;
    private final String tableName;
    private final File file;

    public ImportRequest(String tableId, String fileQualifier) {
      this.tableId = tableId;
      this.fileQualifier = fileQualifier;
      this.createTable = true;
      this.tableName = null;
      this.file = null;
  }

    public ImportRequest(boolean createTable, String tableId,
            String tableName, File file) {
        this.createTable = createTable;
        this.tableId = tableId;
        this.tableName = tableName;
        this.file = file;
        this.fileQualifier = null;
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

    public String getTableName() {
        return tableName;
    }

    public File getFile() {
        return file;
    }
}