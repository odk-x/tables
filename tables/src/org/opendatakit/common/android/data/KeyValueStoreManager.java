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
package org.opendatakit.common.android.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.opendatakit.common.android.database.DataModelDatabaseHelper;
import org.opendatakit.common.android.provider.KeyValueStoreColumns;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * October, 2012:
 * This seems to be an ever changing design, so I'm including the date this
 * time.
 * <p>
 * The key value stores are where ODKTables-specific properties are kept about
 * the tables. In general this is things like "displayName" and eventually
 * view settings that do NOT affect the definition of a table.
 * <p>
 * The columns in the KVS are as follows:
 * tableId,
 * partition,
 * aspect,
 * key,
 * type,
 * value
 * <p>
 * These columns intend to restrict the scope of a key and to prevent collision
 * of keys. The primary key is essentially a composite of tableId, partition,
 * aspect, and key.
 * <p>
 * The tableId is the id of the table to which the key
 * applies.
 * <p>
 * The partition defines roughly to whom the key is relevant. A key that
 * matters to the Table as a whole (eg display name) is given the partition
 * "Table". A key that matters only to a column (eg display name of that
 * column) will be given the partition "Column". Additional partitions will be
 * generated as new classes are created that need to store information in the
 * KVS. For instance, "ListView" and "CollectUtil" would be the partitions that
 * store data for the ListView and CollectUtil classes, respectively.
 * Generally, with the exceptions of Table and Column, a rule of thumb is that
 * partitions should be class names for clarity.
 * <p>
 * The aspect is roughly equivalent to a particular instance of a partition. In
 * another sense, it refers to which specific view, or aspect, of the data the
 * key is associated with.
 * In many cases this will simply be "default". The display name of the table
 * would exist in partition=Table, aspect=default. A column's aspect would be
 * the element_key of the column. The element_key is fixed, and with the table
 * id forms a composite key for the column. The display name of a column with
 * the element key "weight" would exist in partition=Column, aspect=weight.
 * The aspect of a particular list view or graph is the name of that view. At
 * the moment (Dec, 2012) we are not allowing named list views or graph views.
 * Currently, therefore, the aspect for these views is assigned "default". A
 * font size in the list view (if such a property was able to be set) would
 * exist in partition=ListView, aspect=default, key=fontSize. If a graph view
 * was named
 * Fridge Count, the x axis would exist in partition=GraphView,
 * aspect=Fridge Count, key=xAxis. (In this example both the name of graph view
 * and the key xAxis were manufactured and do not reflect the true state of
 * the code. Further, it remains to be seen if we'll actually want to allow
 * spaces.)
 * <p>
 * Key is the key assigned to a particular property and must be unique within
 * a partition and aspect.
 * <p>
 * Type is the type of the value. The value field is set as a string, and this
 * type column allows appropriate interpretation of the string residing in
 * value. Obvious types are text, integer, double, and boolean. The complete
 * list of types is reflected in {@link KeyValueStoreEntryType}. For now, the
 * entry object itself comes from aggregate as a JAR. It is essentially just a
 * struct.
 * <p>
 * Value is the actual value of the key. A key specifying font size might have
 * a value of 12. A key specifying display name might have a value of
 * "Admin Areas". The class using the key must know how to interpret the value.
 * <p>
 * Taken in sum, the entry for a table's display name might be as follows:
 * tableId=myUUID, partition=Table, aspect=default, key=displayName, type=text,
 * value=My Custom Name.
 * <p>
 * The font size of the font size in a list view (which is currently not
 * nameable) might be:
 * tableId=myUUID, partition=ListView, aspect=default, key=fontSize,
 * type=integer, value=12.
 * <p>
 * The x axis of a graph view named Fridge Count (although graph views are
 * not currently nameable) might be (assuming that GraphView.java is the class
 * responsible for managing graph views):
 * tableId=myUUID, partition=GraphView, aspect=Fridge Count, key=xAxis,
 * type=text, value=count.
 * <p>
 * There are three of these key value stores, and they hold information about
 * the tables in different states. The three are the active, default, and
 * server key value stores.
 * <p>
 * The active holds the currently displaying version of the tables, which the
 * user modifies when they make changes. The server holds the version that was
 * pulled down from the server. This is also the key value store that is the
 * source of the table properties at the first sync. After that point, we are
 * currently not allowing definition changes to be pushed.
 * (Well, technically you can do
 * it, but in our model we are planning to make this a privileged operation, as
 * there are some very important considerations that the values in the key
 * value stores can have for the actual structure of a table.)
 * <p>
 * The default is a kind of combination between the two. Changes from the
 * server before the last sync will have to be merged from the server table
 * into the default table. Exactly how this happens is at the moment undefined.
 * Suffice it to say that it is a mixture of the two, and more closely tied
 * to active than is the server.
 * <p>
 * active<--default<--MERGE--server
 * active-->default-->server
 * The exact transitions here are up in the air as to when they can happen.
 * <p>
 * There is a fourth key value store. This handles only sync properties. It is
 * called the keyValueStoreSync, and it handles only things like "setToSync",
 * whether or not the table should be synched.
 *
 * @author sudar.sam@gmail.com
 *
 */
class KeyValueStoreManager {

  public static final String TAG = "KeyValueStoreManager";


  public static final String WHERE_SQL_KEY_VALUE = KeyValueStoreColumns.KEY + " = ? " + " and " +
      KeyValueStoreColumns.VALUE + " = ? ";

  /**
   * Compare the two {@link KeyValueStoreEntry} objects based on
   * their partition, aspect, and key, in that order. Must be from the same
   * table (i.e. have the same tableId) to have any meaning.
   * @author sudar.sam@gmail.com
   *
   */
  public static class KVSEntryComparator implements
      Comparator<KeyValueStoreEntry> {

    @Override
    public int compare(KeyValueStoreEntry lhs,
        KeyValueStoreEntry rhs) {
      int partitionComparison = lhs.partition.compareTo(rhs.partition);
      if (partitionComparison != 0) {
        return partitionComparison;
      }
      int aspectComparison = lhs.aspect.compareTo(rhs.aspect);
      if (aspectComparison != 0) {
        return aspectComparison;
      }
      // Otherwise, we'll just return the value of the key, b/c if the key
      // is also the same, we're equal.
      int keyComparison = lhs.key.compareTo(rhs.key);
      return keyComparison;
    }

  }


  public KeyValueStoreManager() {
  }

  /**
   * Return the typed key value store for the given table id.
   * @param tableId
   * @return
   */
  public KeyValueStore getStoreForTable(String tableId) {
    String backingName = DataModelDatabaseHelper.KEY_VALUE_STORE_ACTIVE_TABLE_NAME;
    return new KeyValueStore(backingName, tableId);
  }

  /**
   * Get all the tableIds from the specified store.
   * @param db
   * @param typeOfStore
   * @return
   */
  public List<String> getAllIdsFromStore(SQLiteDatabase db) {
    String backingName = DataModelDatabaseHelper.KEY_VALUE_STORE_ACTIVE_TABLE_NAME;
    Cursor c = null;
    try {
	    c = db.query(true, backingName, new String[] {KeyValueStoreColumns.TABLE_ID},
	        null, null, null, null, null, null);
	    return getTableIdsFromCursor(c);
    } finally {
    	if ( c != null && !c.isClosed() ) {
    		c.close();
    	}
    }
  }

  /*
   * Get all the table ids from a cursor based.
   */
  private List<String> getTableIdsFromCursor(Cursor c) {
    List<String> ids = new ArrayList<String>();
    int tableIdIndex = c.getColumnIndexOrThrow(KeyValueStoreColumns.TABLE_ID);
    int i = 0;
    c.moveToFirst();
    while (i < c.getCount()) {
      ids.add(ODKDatabaseUtils.getIndexAsString(c, tableIdIndex));
      i++;
      c.moveToNext();
    }
    c.close();
    return ids;
  }

  /**
   * The table creation SQL for the active store.
   * @return
   */
  static String getActiveTableCreateSql() {
	return KeyValueStoreColumns.getTableCreateSql(DataModelDatabaseHelper.KEY_VALUE_STORE_ACTIVE_TABLE_NAME);
  }

  /**
   * The table creation SQL for the sync store.
   */
  static String getSyncTableCreateSql() {
	return KeyValueStoreColumns.getTableCreateSql(DataModelDatabaseHelper.KEY_VALULE_STORE_SYNC_TABLE_NAME);
  }

}
