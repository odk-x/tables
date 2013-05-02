package org.opendatakit.tables.tasks;

import java.io.File;

import org.opendatakit.tables.data.TableProperties;

public class ImportRequest {
    
    private final boolean createTable;
    private final TableProperties tp;
    private final String tableName;
    private final File file;
    
    private ImportRequest(boolean createTable, TableProperties tp,
            String tableName, File file) {
        this.createTable = createTable;
        this.tp = tp;
        this.tableName = tableName;
        this.file = file;
    }
    
    public ImportRequest(String tableName, File file) {
        this(true, null, tableName, file);
    }
    
    public ImportRequest(TableProperties tp, File file) {
        this(false, tp, null, file);
    }
    
    public boolean getCreateTable() {
        return createTable;
    }
    
    public TableProperties getTableProperties() {
        return tp;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public File getFile() {
        return file;
    }
}