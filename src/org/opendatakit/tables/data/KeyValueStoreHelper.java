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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.opendatakit.aggregate.odktables.entity.OdkTablesKeyValueStoreEntry;
import org.opendatakit.tables.sync.SyncUtil;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

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
  public static final String DEFAULT_ASPECT = "default";
  
  /*
   * This is the partition which this helper will be restricted to.
   */
  private final String partition;
  private final KeyValueStore kvs;
  private final DbHelper dbh;
  private final ObjectMapper mapper;
  
  /**
   * @param kvs
   * @param partition
   */
  public KeyValueStoreHelper(KeyValueStore kvs, String partition) {
    this.partition = partition;
    this.kvs = kvs;
    this.dbh = kvs.getDbHelper();
    this.mapper = new ObjectMapper();
    mapper.setVisibilityChecker(mapper.getVisibilityChecker()
        .withFieldVisibility(Visibility.ANY));
  }

  @Override
  public Integer getInteger(String key) {
    OdkTablesKeyValueStoreEntry entry = getEntry(key);
    if (entry == null) {
      return null;
    }
    if (!entry.type.equals(KeyValueStoreEntryType.INTEGER.getLabel())) {
      throw new IllegalArgumentException("requested int entry for " +
          "key: " + key + ", but the corresponding entry in the store was " +
          "not of type: " + KeyValueStoreEntryType.INTEGER.getLabel());     
    }
    return Integer.parseInt(entry.value);
  }

  @Override
  public ArrayList<Object> getList(String key) {
    OdkTablesKeyValueStoreEntry entry = getEntry(key);
    if (entry == null) {
      return null;
    }
    if (!entry.type.equals(KeyValueStoreEntryType.ARRAYLIST.getLabel())) {
      throw new IllegalArgumentException("requested int entry for " +
          "key: " + key + ", but the corresponding entry in the store was " +
          "not of type: " + KeyValueStoreEntryType.ARRAYLIST.getLabel());     
    }
    ArrayList<Object> result = null;
    try {
      result = mapper.readValue(entry.value, ArrayList.class);
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
    OdkTablesKeyValueStoreEntry entry = getEntry(key);
    if (entry == null) {
      return null;
    }
    if (!entry.type.equals(KeyValueStoreEntryType.TEXT.getLabel())) {
      throw new IllegalArgumentException("requested int entry for " +
          "key: " + key + ", but the corresponding entry in the store was " +
          "not of type: " + KeyValueStoreEntryType.TEXT.getLabel());     
    }
    return entry.value;
  }

  @Override
  public String getObject(String key) {
    OdkTablesKeyValueStoreEntry entry = getEntry(key);
    if (entry == null) {
      return null;
    }
    if (!entry.type.equals(KeyValueStoreEntryType.OBJECT.getLabel())) {
      throw new IllegalArgumentException("requested int entry for " +
          "key: " + key + ", but the corresponding entry in the store was " +
          "not of type: " + KeyValueStoreEntryType.OBJECT.getLabel());     
    }
    return entry.value;
  }

  @Override
  public Boolean getBoolean(String key) {
    OdkTablesKeyValueStoreEntry entry = getEntry(key);
    if (entry == null) {
      return null;
    }
    if (!entry.type.equals(KeyValueStoreEntryType.BOOLEAN.getLabel())) {
      throw new IllegalArgumentException("requested int entry for " +
          "key: " + key + ", but the corresponding entry in the store was " +
          "not of type: " + KeyValueStoreEntryType.BOOLEAN.getLabel());     
    }
    return SyncUtil.intToBool(Integer.parseInt(entry.value));
  }

  @Override
  public Double getNumeric(String key) {
    OdkTablesKeyValueStoreEntry entry = getEntry(key);
    if (entry == null) {
      return null;
    }
    if (!entry.type.equals(KeyValueStoreEntryType.NUMBER.getLabel())) {
      throw new IllegalArgumentException("requested int entry for " +
          "key: " + key + ", but the corresponding entry in the store was " +
          "not of type: " + KeyValueStoreEntryType.NUMBER.getLabel());     
    }
    return Double.parseDouble(entry.value);
  }

  @Override
  public void setIntegerEntry(String key, Integer value) {
    if (value == null) {
      removeEntry(key);
      return;
    }
    SQLiteDatabase db = dbh.getWritableDatabase();
    kvs.insertOrUpdateKey(db, this.partition, DEFAULT_ASPECT, key, 
        KeyValueStoreEntryType.INTEGER.getLabel(), Integer.toString(value));
    Log.d(TAG, "updated partition: " + partition + ", aspect: " + 
        DEFAULT_ASPECT + ", key: " + key + " to " + value);
  }

  @Override
  public void setNumericEntry(String key, Double value) {
    if (value == null) {
      removeEntry(key);
      return;
    }
    SQLiteDatabase db = dbh.getWritableDatabase();
    kvs.insertOrUpdateKey(db, this.partition, DEFAULT_ASPECT, key, 
        KeyValueStoreEntryType.NUMBER.getLabel(), Double.toString(value));
    Log.d(TAG, "updated partition: " + partition + ", aspect: " + 
        DEFAULT_ASPECT + ", key: " + key + " to " + value);
  }

  @Override
  public void setObjectEntry(String key, String jsonOfObject) {
    if (jsonOfObject == null) {
      removeEntry(key);
      return;
    }
    SQLiteDatabase db = dbh.getWritableDatabase();
    kvs.insertOrUpdateKey(db, this.partition, DEFAULT_ASPECT, key, 
        KeyValueStoreEntryType.OBJECT.getLabel(), jsonOfObject);
    Log.d(TAG, "updated partition: " + partition + ", aspect: " + 
        DEFAULT_ASPECT + ", key: " + key + " to " + jsonOfObject);
  }

  @Override
  public void setBooleanEntry(String key, Boolean value) {
    if (value == null) {
      removeEntry(key);
      return;
    }
    SQLiteDatabase db = dbh.getWritableDatabase();
    kvs.insertOrUpdateKey(db, this.partition, DEFAULT_ASPECT, key, 
        KeyValueStoreEntryType.BOOLEAN.getLabel(), 
        Integer.toString(SyncUtil.boolToInt(value)));
    Log.d(TAG, "updated partition: " + partition + ", aspect: " + 
        DEFAULT_ASPECT + ", key: " + key + " to " + value);
  }

  @Override
  public void setStringEntry(String key, String value) {
    if (value == null) {
      removeEntry(key);
      return;
    }
    SQLiteDatabase db = dbh.getWritableDatabase();
    kvs.insertOrUpdateKey(db, this.partition, DEFAULT_ASPECT, key, 
        KeyValueStoreEntryType.TEXT.getLabel(), value);
    Log.d(TAG, "updated partition: " + partition + ", aspect: " + 
        DEFAULT_ASPECT + ", key: " + key + " to " + value);
  }

  @Override
  public void setListEntry(String key, ArrayList<Object> value) {
    if (value == null) {
      removeEntry(key);
      return;
    }
    String entryValue = null;
    try {
      entryValue = mapper.writeValueAsString(value);
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
    SQLiteDatabase db = dbh.getWritableDatabase();
    kvs.insertOrUpdateKey(db, this.partition, DEFAULT_ASPECT, key, 
        KeyValueStoreEntryType.ARRAYLIST.getLabel(), entryValue);
    Log.d(TAG, "updated partition: " + partition + ", aspect: " + 
        DEFAULT_ASPECT + ", key: " + key + " to " + value);
  }

  @Override
  public int removeEntry(String key) {
    SQLiteDatabase db = dbh.getWritableDatabase();
    return kvs.deleteKey(db, this.partition, DEFAULT_ASPECT, key);
  }

  @Override
  public OdkTablesKeyValueStoreEntry getEntry(String key) {
    SQLiteDatabase db = dbh.getReadableDatabase();
    List<String> keyList = new ArrayList<String>();
    keyList.add(key);
    List<OdkTablesKeyValueStoreEntry> entries = 
        kvs.getEntriesForKeys(db, partition, DEFAULT_ASPECT, keyList);
    // Do some sanity checking. There should only ever be one entry per key.
    if (entries.size() > 1) {
      Log.e(TAG, "request for key: " + key + " in KVS " + 
          kvs.getDbBackingName() +
          " for table: " + kvs.getTableId() + " returned " + entries.size() + 
          "entries. It should return at most 1, as it is a key in a set.");
    }
    if (entries.size() == 0) {
      return null;
    } else {
      return entries.get(0);
    }
  }

 
}
