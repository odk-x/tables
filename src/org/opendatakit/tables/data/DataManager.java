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
    
    public TableProperties getTableProperties(String tableId,
        KeyValueStore.Type typeOfStore) {
        return TableProperties.getTablePropertiesForTable(dbh, tableId,
            typeOfStore);
    }
    
    public TableProperties[] getAllTableProperties(
        KeyValueStore.Type typeOfStore) {
        return TableProperties.getTablePropertiesForAll(dbh, typeOfStore);
    }
    
    /**
     * Return the active table properties for tables that are set to 
     * be synched.
     * @return
     */
    /*public TableProperties[] getSynchronizedActiveTableProperties() {
      return TableProperties.getTablePropertiesForSynchronizedTables(dbh,
          KeyValueStore.Type.ACTIVE);
    }*/
    
    /**
     * Return the default table properties for tables that are set to be
     * synched.
     * @return
     */
    /*public TableProperties[] getSynchronizedDefaultTableProperties() {
      return TableProperties.getTablePropertiesForSynchronizedTables(dbh,
          KeyValueStore.Type.DEFAULT);
    }*/
    
    /**
     * Return the server table properties for tables that are set to be
     * synched.
     * @return
     */
    public TableProperties[] getTablePropertiesForTablesSetToSync(
        KeyValueStore.Type typeOfStore) {
      return TableProperties.getTablePropertiesForSynchronizedTables(dbh,
          typeOfStore);
    }

    public TableProperties[] getTablePropertiesForDataTables(
        KeyValueStore.Type typeOfStore) {
        return TableProperties.getTablePropertiesForDataTables(dbh, 
            typeOfStore);
    }
    
    /**
     * Returns the TableProperties for all of the tables in the server KVS that
     * are of type DATA.
     * @return
     */
    /*public TableProperties[] getTablePropertiesForServerDataTables() {
      return TableProperties.getTablePropertiesForDataTables(dbh,
          KeyValueStore.Type.SERVER);
    }*/
    
    public TableProperties[] getSecurityTableProperties(
        KeyValueStore.Type typeOfStore) {
        return TableProperties.getTablePropertiesForSecurityTables(dbh, 
            typeOfStore);
    }
    
    public TableProperties[] getShortcutTableProperties(
        KeyValueStore.Type typeOfStore) {
        return TableProperties.getTablePropertiesForShortcutTables(dbh,
            typeOfStore);
    }
}
