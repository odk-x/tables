/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
    
    /**
     * Return the active table properties for tables that are set to 
     * be synched.
     * @return
     */
    public TableProperties[] getSynchronizedTableProperties() {
      return TableProperties.getTablePropertiesForSynchronizedTables(dbh);
    }
    
    /**
     * Return the default table properties for tables that are set to be
     * synched.
     * @return
     */
    public TableProperties[] getSynchronizedDefaultTableProperties() {
      return TableProperties.getDefaultPropertiesForSynchronizedTables(dbh);
    }
    
    /**
     * Return the server table properties for tables that are set to be
     * synched.
     * @return
     */
    public TableProperties[] getSynchronizedServerTableProperties() {
      return TableProperties.getServerPropertiesForSynchronizedTables(dbh);
    }

    public TableProperties[] getDataTableProperties() {
        return TableProperties.getTablePropertiesForDataTables(dbh);
    }
    
    /**
     * Returns the TableProperties for all of the tables in the server KVS that
     * are of type DATA.
     * @return
     */
    public TableProperties[] getTablePropertiesForServerDataTables() {
      return TableProperties.getTablePropertiesForServerDataTables(dbh);
    }
    
    public TableProperties[] getSecurityTableProperties() {
        return TableProperties.getTablePropertiesForSecurityTables(dbh);
    }
    
    public TableProperties[] getShortcutTableProperties() {
        return TableProperties.getTablePropertiesForShortcutTables(dbh);
    }
}
