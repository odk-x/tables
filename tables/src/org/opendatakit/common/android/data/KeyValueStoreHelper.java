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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opendatakit.aggregate.odktables.rest.KeyValueStoreConstants;
import org.opendatakit.common.android.database.DataModelDatabaseHelper;
import org.opendatakit.common.android.database.DataModelDatabaseHelperFactory;
import org.opendatakit.common.android.utilities.DataHelper;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.common.android.utilities.ODKFileUtils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * A helper class to access values in the key value store. The partition must
 * be set in the creation of the object, ensuring that this helper can only
 * access those keys in its partition.
 * @author sudar.sam@gmail.com
 *
 */
public class KeyValueStoreHelper implements KeyValueHelper {

  private static final String TAG = KeyValueStoreHelper.class.getName();

  /**
   * This is the default aspect that will be used when interacting with the
   * key value store via this object. If a named aspect is required (note that
   * it cannot share the same name as this field),
   * {@link AspectKeyValueStoreHelper} must be used.
   */

  /*
   * This is the partition which this helper will be restricted to.
   */
  private final String partition;
  private final String tableId;
  private final String appName;
  private final Context context;
  
  private final SQLiteDatabase db;

  public KeyValueStoreHelper(Context context, String appName, String tableId, String partition) {
    this.context = context;
    this.appName = appName;
    this.tableId = tableId;
    this.partition = partition;
    this.db = null;
  }

  public KeyValueStoreHelper(SQLiteDatabase db, String tableId, String partition) {
    this.context = null;
    this.appName = null;
    this.tableId = tableId;
    this.partition = partition;
    this.db = db;
  }

  private SQLiteDatabase getWritableDatabase() {
    if ( this.db != null ) {
      return db;
    }
    DataModelDatabaseHelper dbh = DataModelDatabaseHelperFactory.getDbHelper(context, appName);
    return dbh.getWritableDatabase();
  }

  private SQLiteDatabase getReadableDatabase() {
    if ( this.db != null ) {
      return db;
    }
    DataModelDatabaseHelper dbh = DataModelDatabaseHelperFactory.getDbHelper(context, appName);
    return dbh.getReadableDatabase();
  }
  
  /**
   * Get the accessor for the partition specified by this object as well as
   * the given aspect.
   * @param aspect
   * @return
   */
  public AspectHelper getAspectHelper(String aspect) {
    return new AspectHelper(aspect);
  }

  /**
   * The partition of the key value store.
   * @return
   */
  public String getPartition() {
    return this.partition;
  }
  
  public String getTableId() {
    return this.tableId;
  }

  public String getAppName() {
    return this.appName;
  }
  
  @Override
  public Integer getInteger(String key) {
    return getInteger(KeyValueStoreConstants.ASPECT_DEFAULT, key);
  }

  private Integer getInteger(String aspect, String key) {
    KeyValueStoreEntry entry = getEntry(aspect, key);
    if (entry == null) {
      return null;
    }
    if (!entry.type.equals(ElementDataType.integer.name())) {
      throw new IllegalArgumentException("requested int entry for " +
          "key: " + key + ", but the corresponding entry in the store was " +
          "not of type: " + ElementDataType.integer.name());
    }
    return Integer.parseInt(entry.value);
  }

  @Override
  public <T> ArrayList<T> getArray(String key, Class<T> clazz) {
    return getArray(KeyValueStoreConstants.ASPECT_DEFAULT, key, clazz);
  }

  private <T> ArrayList<T> getArray(String aspect, String key, Class<T> clazz) {
    KeyValueStoreEntry entry = getEntry(aspect, key);
    if (entry == null) {
      return null;
    }
    if (!entry.type.equals(ElementDataType.array.name())) {
      throw new IllegalArgumentException("requested list entry for " +
          "key: " + key + ", but the corresponding entry in the store was " +
          "not of type: " + ElementDataType.array.name());
    }
    ArrayList<T> result = null;
    try {
      if ( entry.value != null && entry.value.length() != 0 ) {
        result = ODKFileUtils.mapper.readValue(entry.value, ArrayList.class);
      }
    } catch (JsonParseException e) {
      Log.e(TAG, "problem parsing json list entry from the kvs");
      e.printStackTrace();
    } catch (JsonMappingException e) {
      Log.e(TAG, "problem mapping json list entry from the kvs");
      e.printStackTrace();
    } catch (IOException e) {
      Log.e(TAG, "i/o problem with json for list entry from the kvs");
      e.printStackTrace();
    }
    return result;
  }

  @Override
  public String getString(String key) {
    return getString(KeyValueStoreConstants.ASPECT_DEFAULT, key);
  }

  private String getString(String aspect, String key) {
    KeyValueStoreEntry entry = getEntry(aspect, key);
    if (entry == null) {
      return null;
    }
    if (!entry.type.equals(ElementDataType.string.name())) {
      throw new IllegalArgumentException("requested string entry for " +
          "key: " + key + ", but the corresponding entry in the store was " +
          "not of type: " + ElementDataType.string.name());
    }
    return entry.value;
  }

  @Override
  public String getObject(String key) {
    return getObject(KeyValueStoreConstants.ASPECT_DEFAULT, key);
  }

  private String getObject(String aspect, String key) {
    KeyValueStoreEntry entry = getEntry(aspect, key);
    if (entry == null) {
      return null;
    }
    if (!entry.type.equals(ElementDataType.object.name())) {
      throw new IllegalArgumentException("requested object entry for " +
          "key: " + key + ", but the corresponding entry in the store was " +
          "not of type: " + ElementDataType.object.name());
    }
    return entry.value;
  }

  @Override
  public Boolean getBoolean(String key) {
    return getBoolean(KeyValueStoreConstants.ASPECT_DEFAULT, key);
  }

  private Boolean getBoolean(String aspect, String key) {
    KeyValueStoreEntry entry = getEntry(aspect, key);
    if (entry == null) {
      return null;
    }
    if (!entry.type.equals(ElementDataType.bool.name())) {
      throw new IllegalArgumentException("requested boolean entry for " +
          "key: " + key + ", but the corresponding entry in the store was " +
          "not of type: " + ElementDataType.bool.name());
    }
    return DataHelper.intToBool(Integer.parseInt(entry.value));
  }

  @Override
  public Double getNumber(String key) {
    return getNumber(KeyValueStoreConstants.ASPECT_DEFAULT, key);
  }

  private Double getNumber(String aspect, String key) {
    KeyValueStoreEntry entry = getEntry(aspect, key);
    if (entry == null) {
      return null;
    }
    if (!entry.type.equals(ElementDataType.number.name())) {
      throw new IllegalArgumentException("requested number entry for " +
          "key: " + key + ", but the corresponding entry in the store was " +
          "not of type: " + ElementDataType.number.name());
    }
    return Double.parseDouble(entry.value);
  }

  @Override
  public void setInteger(String key, Integer value) {
    setIntegerEntry(KeyValueStoreConstants.ASPECT_DEFAULT, key, value);
  }

  private void setIntegerEntry(String aspect, String key, Integer value) {
    SQLiteDatabase db = this.getWritableDatabase();
    try {
      db.beginTransaction();
      KeyValueStoreEntry entry = new KeyValueStoreEntry();
      entry.tableId = this.getTableId();
      entry.partition = this.getPartition();
      entry.aspect = aspect;
      entry.key = key;
      entry.type = ElementDataType.integer.name();
      entry.value = Integer.toString(value);
      ODKDatabaseUtils.replaceDBTableMetadata(db, entry);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
      db.close();
    }
  }

  @Override
  public void setNumber(String key, Double value) {
    setNumberEntry(KeyValueStoreConstants.ASPECT_DEFAULT, key, value);
  }

  private void setNumberEntry(String aspect, String key, Double value) {
    SQLiteDatabase db = this.getWritableDatabase();
    try {
      db.beginTransaction();
      KeyValueStoreEntry entry = new KeyValueStoreEntry();
      entry.tableId = this.getTableId();
      entry.partition = this.getPartition();
      entry.aspect = aspect;
      entry.key = key;
      entry.type = ElementDataType.number.name();
      entry.value = Double.toString(value);
      ODKDatabaseUtils.replaceDBTableMetadata(db, entry);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
      db.close();
    }
  }

  @Override
  public void setObject(String key, String jsonOfObject) {
    setObjectEntry(KeyValueStoreConstants.ASPECT_DEFAULT, key, jsonOfObject);
  }

  private void setObjectEntry(String aspect, String key, String jsonOfObject) {
    SQLiteDatabase db = this.getWritableDatabase();
    try {
      db.beginTransaction();
      KeyValueStoreEntry entry = new KeyValueStoreEntry();
      entry.tableId = this.getTableId();
      entry.partition = this.getPartition();
      entry.aspect = aspect;
      entry.key = key;
      entry.type = ElementDataType.object.name();
      entry.value = jsonOfObject;
      ODKDatabaseUtils.replaceDBTableMetadata(db, entry);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
      db.close();
    }
  }

  @Override
  public void setBoolean(String key, Boolean value) {
    setBooleanEntry(KeyValueStoreConstants.ASPECT_DEFAULT, key, value);
  }

  /**
   * Set the boolean entry for this aspect and key.
   * @param aspect
   * @param key
   * @param value
   */
  private void setBooleanEntry(String aspect, String key, Boolean value) {
    SQLiteDatabase db = this.getWritableDatabase();
    try {
      db.beginTransaction();
      KeyValueStoreEntry entry = new KeyValueStoreEntry();
      entry.tableId = this.getTableId();
      entry.partition = this.getPartition();
      entry.aspect = aspect;
      entry.key = key;
      entry.type = ElementDataType.bool.name();
      entry.value = Integer.toString(DataHelper.boolToInt(value));
      ODKDatabaseUtils.replaceDBTableMetadata(db, entry);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
      db.close();
    }
  }

  @Override
  public void setString(String key, String value) {
    setStringEntry(KeyValueStoreConstants.ASPECT_DEFAULT, key, value);
  }

  /**
   * Set the given String entry.
   * @param aspect
   * @param key
   * @param value
   */
  private void setStringEntry(String aspect, String key, String value) {
    SQLiteDatabase db = this.getWritableDatabase();
    try {
      db.beginTransaction();
      KeyValueStoreEntry entry = new KeyValueStoreEntry();
      entry.tableId = this.getTableId();
      entry.partition = this.getPartition();
      entry.aspect = aspect;
      entry.key = key;
      entry.type = ElementDataType.string.name();
      entry.value = value;
      ODKDatabaseUtils.replaceDBTableMetadata(db, entry);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
      db.close();
    }
  }

  /**
   * API fo ruse when called within a transaction.
   *
   * @param db
   * @param key
   * @param value
   */
  public void setString(SQLiteDatabase db, String key, String value) {
	  setStringEntry(db, KeyValueStoreConstants.ASPECT_DEFAULT, key, value);
  }

  /**
   * API for use when called within a transaction.
   *
   * @param db
   * @param aspect
   * @param key
   * @param value
   */
  public void setStringEntry(SQLiteDatabase db, String aspect, String key, String value) {
    KeyValueStoreEntry entry = new KeyValueStoreEntry();
    entry.tableId = this.getTableId();
    entry.partition = this.getPartition();
    entry.aspect = aspect;
    entry.key = key;
    entry.type = ElementDataType.string.name();
    entry.value = value;
    ODKDatabaseUtils.replaceDBTableMetadata(db, entry);
  }

  @Override
  public <T> void setArray(String key, ArrayList<T> value) {
    setArrayEntry(KeyValueStoreConstants.ASPECT_DEFAULT, key, value);
  }

  /**
   * Set the list entry for the given aspect and key.
   * @param aspect
   * @param key
   * @param value
   */
  private <T> void setArrayEntry(String aspect, String key,
      ArrayList<T> value) {
    String entryValue = null;
    try {
      if (value != null && value.size() > 0) {
        entryValue = ODKFileUtils.mapper.writeValueAsString(value);
      } else {
        entryValue = ODKFileUtils.mapper.writeValueAsString(new ArrayList<T>());
      }
    } catch (JsonGenerationException e) {
      Log.e(TAG, "problem parsing json list entry while writing to the kvs");
      e.printStackTrace();
    } catch (JsonMappingException e) {
      Log.e(TAG, "problem mapping json list entry while writing to the kvs");
      e.printStackTrace();
    } catch (IOException e) {
      Log.e(TAG, "i/o exception with json list entry while writing to the" +
            " kvs");
      e.printStackTrace();
    }
    if (entryValue == null) {
      Log.e(TAG, "problem parsing list to json, not updating key");
      return;
    }
    SQLiteDatabase db = this.getWritableDatabase();
    try {
      db.beginTransaction();
      KeyValueStoreEntry entry = new KeyValueStoreEntry();
      entry.tableId = this.getTableId();
      entry.partition = this.getPartition();
      entry.aspect = aspect;
      entry.key = key;
      entry.type = ElementDataType.array.name();
      entry.value = entryValue;
      ODKDatabaseUtils.replaceDBTableMetadata(db, entry);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
      db.close();
    }
  }

  @Override
  public void removeKey(String key) {
    removeEntry(KeyValueStoreConstants.ASPECT_DEFAULT, key);
  }

  /**
   * Remove the entries for the given aspect and key.
   * @param aspect
   * @param key
   * @return
   */
  private void removeEntry(String aspect, String key) {
    SQLiteDatabase db = this.getWritableDatabase();
    try {
      db.beginTransaction();
      ODKDatabaseUtils.deleteDBTableMetadata(db, this.getTableId(), this.getPartition(), aspect, key);
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
      db.close();
    }
  }

  @Override
  public KeyValueStoreEntry getEntry(String key) {
    return getEntry(KeyValueStoreConstants.ASPECT_DEFAULT, key);
  }

  /**
   * Return the entry for the given aspect and key, using the partition field.
   * <p>
   * Return null if the given entry doesn't exist. Logging is done if there is
   * more than one key matching the specifications, as this as an error. The
   * first entry in the list is still returned, however.
   * @param aspect
   * @param key
   * @return
   */
  private KeyValueStoreEntry getEntry(String aspect, String key) {
    SQLiteDatabase db = null;
    try {
      db = this.getReadableDatabase();
      List<KeyValueStoreEntry> entries =
          ODKDatabaseUtils.getDBTableMetadata(db, this.getTableId(), this.getPartition(), aspect, key);
      // Do some sanity checking. There should only ever be one entry per key.
      if (entries.size() > 1) {
        Log.e(TAG, "request for key: " + key + " in KVS " +
            DataModelDatabaseHelper.KEY_VALUE_STORE_ACTIVE_TABLE_NAME +
            " for table: " + this.getTableId() + " returned " + entries.size() +
            "entries. It should return at most 1, as it is a key in a set.");
      }
      if (entries.size() == 0) {
        return null;
      } else {
        return entries.get(0);
      }
    } finally {
      db.close();
    }
  }

  /**
   * Much like the outer KeyValueStoreHelper class, except that this also
   * specifies an aspect. All the methods apply to the partition of the
   * enclosing class and the aspect of this class.
   * @author sudar.sam@gmail.com
   *
   */
  public class AspectHelper implements KeyValueHelper {

    private final String aspect;

    /**
     * Private so that you can only get it via the factory class.
     * @param aspect
     */
    private AspectHelper(String aspect) {
      this.aspect = aspect;
    }

    @Override
    public Integer getInteger(String key) {
      return KeyValueStoreHelper.this.getInteger(aspect, key);
    }

    @Override
    public <T> ArrayList<T> getArray(String key, Class<T> clazz) {
      return KeyValueStoreHelper.this.getArray(aspect, key, clazz);
    }

    @Override
    public String getString(String key) {
      return KeyValueStoreHelper.this.getString(aspect, key);
    }

    @Override
    public String getObject(String key) {
      return KeyValueStoreHelper.this.getObject(aspect, key);
    }

    @Override
    public Boolean getBoolean(String key) {
      return KeyValueStoreHelper.this.getBoolean(aspect, key);
    }

    @Override
    public Double getNumber(String key) {
      return KeyValueStoreHelper.this.getNumber(aspect, key);
    }

    @Override
    public void setInteger(String key, Integer value) {
      KeyValueStoreHelper.this.setIntegerEntry(aspect, key, value);
    }

    @Override
    public void setNumber(String key, Double value) {
      KeyValueStoreHelper.this.setNumberEntry(aspect, key, value);
    }

    @Override
    public void setObject(String key, String jsonOfObject) {
      KeyValueStoreHelper.this.setObjectEntry(aspect, key, jsonOfObject);
    }

    @Override
    public void setBoolean(String key, Boolean value) {
      KeyValueStoreHelper.this.setBooleanEntry(aspect, key, value);
    }

    @Override
    public void setString(String key, String value) {
      KeyValueStoreHelper.this.setStringEntry(aspect, key, value);
    }

    @Override
    public <T> void setArray(String key, ArrayList<T> value) {
      KeyValueStoreHelper.this.setArrayEntry(aspect, key, value);
    }

    @Override
    public void removeKey(String key) {
      KeyValueStoreHelper.this.removeEntry(aspect, key);
    }

    @Override
    public KeyValueStoreEntry getEntry(String key) {
      return KeyValueStoreHelper.this.getEntry(aspect, key);
    }

    /**
     * Delete all the entries in the given aspect.
     * @return
     */
    public void deleteAllEntriesInThisAspect() {
      SQLiteDatabase db = KeyValueStoreHelper.this.getWritableDatabase();
      try {
        db.beginTransaction();
        ODKDatabaseUtils.deleteDBTableMetadata(db, KeyValueStoreHelper.this.getTableId(), partition, aspect, null);
        db.setTransactionSuccessful();
      } finally {
        db.endTransaction();
        db.close();
      }
    }

  }


}
