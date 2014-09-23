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

/**
 * Defines various methods used for getting and setting keys in the key value
 * store. The key value store holds various persistent information about a
 * table.
 * @author sudar.sam@gmail.com
 *
 */
public interface KeyValueHelper {

  /**
   * Retrieve a value of type {@link KeyValueStoreEntryType.INTEGER} mapping to
   * the given key.
   * <p>
   * If the key does not exist it returns null.
   * @param key
   * @return
   * @throws IllegalArgumentException if the type of the entry does not match
   */
  public Integer getInteger(String key);

  /**
   * Retrieve a value of type {@link KeyValueStoreEntryType.ARRAY} mapping
   * to
   * the given key. The caller must know what type of Object exists in the
   * list, and must ensure that the objects can be parsed and read by the JSON
   * mapping library.
   * <p>
   * If the key does not exist it returns null. If the key exists but there are
   * problems with reclaiming the list from the JSON representation, an error
   * is logged and null is returned.
   * @param key
   * @return
   * @throws IllegalArgumentException if the type of the entry does not match
   */
  public <T> ArrayList<T> getArray(String key, Class<T> clazz);

  /**
   * Retrieve a value of type {@link KeyValueStoreEntryType.STRING} mapping to
   * the given key.
   * <p>
   * If the key does not exist it returns null.
   * @param key
   * @return
   * @throws IllegalArgumentException if the type of the entry does not match
   */
  public String getString(String key);

  /**
   * Retrieve a value of type {@link KeyValueStoreEntryType.OBJECT} mapping to
   * the given key. As this returns a string, the caller must handle reclaiming
   * the object from the string.
   * <p>
   * If the key does not exist it returns null.
   * @param key
   * @return
   * @throws IllegalArgumentException if the type of the entry does not match
   */
  public String getObject(String key);

  /**
   * Retrieve a value of type {@link KeyValueStoreEntryType.BOOLEAN} mapping to
   * the given key.
   * <p>
   * If the key does not exist it returns null.
   * @param key
   * @return
   * @throws IllegalArgumentException if the type of the entry does not match
   */
  public Boolean getBoolean(String key);

  /**
   * Retrieve a value of type {@link KeyValueStoreEntryType.NUMBER} mapping to
   * the given key.
   * <p>
   * If the key does not exist it returns null.
   * @param key
   * @return
   * @throws IllegalArgumentException if the type of the entry does not match
   */
  public Double getNumber(String key);

  /**
   * Set an entry of type {@link KeyValueSToreEntryType.INTEGER} in the key
   * value store.
   * @param key
   * @param value
   */
  public void setInteger(String key, Integer value);

  /**
   * Set an entry of type {@link KeyValueSToreEntryType.NUMBER} in the key
   * value store.
   * @param key
   * @param value
   */
  public void setNumber(String key, Double value);

  /**
   * Set an entry of type {@link KeyValueSToreEntryType.OBJECT} in the key
   * value store. The value is a JSON serialization. The caller is responsible
   * for providing and interpreting it.
   * @param key
   * @param value
   */
  public void setObject(String key, String jsonOfObject);

  /**
   * Set an entry of type {@link KeyValueSToreEntryType.BOOLEAN} in the key
   * value store.
   * @param key
   * @param value
   */
  public void setBoolean(String key, Boolean value);

  /**
   * Set an entry of type {@link KeyValueSToreEntryType.STRING} in the key
   * value store.
   * @param key
   * @param value
   */
  public void setString(String key, String value);

  /**
   * Set an entry of type {@link KeyValueSToreEntryType.ARRAY} in the key
   * value store. The value will be converted to a String via a JSON
   * serialization. Unlike setJson, the assumption here is that the
   * list contains items of the same type. If you need a more generic
   * storage mechanism, use setJson and do the serialization yourself.
   *
   * @param key
   * @param value
   */
  public <T> void setArray(String key, ArrayList<T> value);

  /**
   * Remove the given key from the key value store.
   * @param key
   * @return
   */
  public void removeKey(String key);

  /**
   * Return the entry matching this key.
   * <p>
   * Return null if the key is not found.
   * @param key
   * @return
   */
  public KeyValueStoreEntry getEntry(String key);

}
