package org.opendatakit.tables.data;


public class DataManager {
    
    private DbHelper dbh;
    
    public DataManager(DbHelper dbh) {
        this.dbh = dbh;
    }
    
    public DbTable getDbTable(String tableId) {
        return DbTable.getDbTable(dbh, tableId);
    }
    
    public TableProperties getTableProperties(String tableId) {
        return TableProperties.getTablePropertiesForTable(dbh, tableId);
    }
    
    public TableProperties[] getAllTableProperties() {
        return TableProperties.getTablePropertiesForAll(dbh);
    }
    
    public TableProperties[] getSynchronizedTableProperties() {
      return TableProperties.getTablePropertiesForSynchronizedTables(dbh);
    }

    public TableProperties[] getDataTableProperties() {
        return TableProperties.getTablePropertiesForDataTables(dbh);
    }
    
    public TableProperties[] getSecurityTableProperties() {
        return TableProperties.getTablePropertiesForSecurityTables(dbh);
    }
    
    public TableProperties[] getShortcutTableProperties() {
        return TableProperties.getTablePropertiesForShortcutTables(dbh);
    }
}
