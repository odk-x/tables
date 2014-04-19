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

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesKeyValueStoreEntry;

import android.database.sqlite.SQLiteDatabase;

/**
 * A key value store to store sync state. It exists outside of the active,
 * default, and server stores because all interactions with a table must point
 * to it, and it doesn't make sense that if someone pushed their sync setting
 * to the server that it should affect the sync behavior of other users.
 * @author sudar.sam@gmail.com
 *
 */
public class KeyValueStoreSync extends KeyValueStore {

  public static final String KVS_PARTITION = "Table";
  public static final String KVS_ASPECT = "global";

  public KeyValueStoreSync(String dbName, String tableId) {
    super(dbName, tableId);
  }

  /**
   * Returns whether or not the table is set to sync, according to the sync
   * key value store. If there is no entry in the sync KVS, which will happen
   * if there are no table properties for the table in the server KVS, then
   * this will return false. (Is this the right decision?)
   * @return
   */
  public boolean isSetToSync(SQLiteDatabase db) {
    try {
	    List<String> isSetToSyncKey = new ArrayList<String>();
	    isSetToSyncKey.add(SyncPropertiesKeys.IS_SET_TO_SYNC.getKey());
	    List<OdkTablesKeyValueStoreEntry> isSetToSyncEntry =
	        this.getEntriesForKeys(db, KeyValueStoreSync.KVS_PARTITION,
	            KeyValueStoreSync.KVS_ASPECT, isSetToSyncKey);
	    if (isSetToSyncEntry.size() == 0)
	      return false;
	    // otherwise there is a single entry and it is the one we want.
	    if (DataHelper.intToBool(
	          Integer.parseInt(isSetToSyncEntry.get(0).value))) {
	      return true;
	    } else {
	      return false;
	    }
    } finally {
    }
  }

  /**
   * Set in the sync KVS whether or not the table is set to be synched.
   * @param val
   */
  public void setIsSetToSync(boolean val, SQLiteDatabase db) {
    try {
	    int newValue = DataHelper.boolToInt(val);
	    this.insertOrUpdateKey(db, KeyValueStoreSync.KVS_PARTITION,
	        KeyValueStoreSync.KVS_ASPECT,
	        SyncPropertiesKeys.IS_SET_TO_SYNC.getKey(),
	        ColumnType.INTEGER.name(),
	        Integer.toString(newValue));
    } finally {
    }
  }




  /**
   * These are the keys that have assigned functions in the key value store
   * that holds sync properties.
   */
  public static enum SyncPropertiesKeys {
    /**
     * Holds an integer (1 or 0) of whether or not this table is set to be
     * synched with the server. This is only applicable to tables that have
     * properties in the server key value store. The reasoning behind this is
     * that synching with the server is based on the properties in the server
     * key value store. This means that a table is NOT in the server key value
     * store you will not be able to select it to sync. Upon copying data into
     * from the default to the server key value store, an entry for the table
     * with this key created and added to the sync key value store (if an entry
     * for the table in the sync key value store does not already exist) and
     * its value is set to 0.
     */
    IS_SET_TO_SYNC("isSetToSync");

    private String key;

    private SyncPropertiesKeys(String key) {
      this.key = key;
    }

    public String getKey() {
      return key;
    }
  }
}
