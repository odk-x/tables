/*
 * Copyright (C) 2014 University of Washington
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
package org.opendatakit.tables.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.opendatakit.aggregate.odktables.rest.KeyValueStoreConstants;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.ElementDataType;
import org.opendatakit.common.android.data.JoinColumn;
import org.opendatakit.common.android.data.KeyValueStoreEntry;
import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.KeyValueStoreHelper.AspectHelper;
import org.opendatakit.common.android.utilities.NameUtil;
import org.opendatakit.common.android.utilities.ODKDataUtils;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.common.android.utilities.ODKFileUtils;

import android.database.sqlite.SQLiteDatabase;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ColumnUtil {
  
  private static ColumnUtil columnUtil = new ColumnUtil();
  
  public static ColumnUtil get() {
    return columnUtil;
  }
 
  /**
   * For mocking -- supply a mocked object.
   * 
   * @param util
   */
  public static void set(ColumnUtil util) {
    columnUtil = util;
  }

  protected ColumnUtil() {}

  /**
   * Return the element key for the column based on the element path.
   * By convention, if the key has nested elements, we can just replace 
   * all dots with underscores to generate the elementKey from the path.
   *
   * @param elementPath
   * @return elementKey
   */
  public String getElementKeyFromElementPath(String elementPath) {
    // TODO: should we verify that this key actually exists?
    String hackPath = elementPath.replace(".", "_");
    return hackPath;
  }

  public String getLocalizedDisplayName(SQLiteDatabase db, String tableId, String elementKey) {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, tableId, KeyValueStoreConstants.PARTITION_COLUMN);
    AspectHelper ah = kvsh.getAspectHelper(elementKey);
    String displayName = null;
    String jsonDisplayName = ah.getObject(KeyValueStoreConstants.COLUMN_DISPLAY_NAME);
    if ( jsonDisplayName != null ) {
      displayName = ODKDataUtils.getLocalizedDisplayName(jsonDisplayName);
    }
    if ( displayName == null ) {
      displayName = NameUtil.constructSimpleDisplayName(elementKey);
    }
    return displayName;
  }

  public String getRawDisplayName(SQLiteDatabase db, String tableId, String elementKey) {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, tableId, KeyValueStoreConstants.PARTITION_COLUMN);
    AspectHelper ah = kvsh.getAspectHelper(elementKey);
    String jsonDisplayName = ah.getObject(KeyValueStoreConstants.COLUMN_DISPLAY_NAME);
    if ( jsonDisplayName == null ) {
      jsonDisplayName = NameUtil.normalizeDisplayName(NameUtil.constructSimpleDisplayName(elementKey));
    }
    return jsonDisplayName;
  }

  public ArrayList<? extends Map<String,Object>> getDisplayChoicesList(SQLiteDatabase db, String tableId, String elementKey) {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, tableId, KeyValueStoreConstants.PARTITION_COLUMN);
    AspectHelper ah = kvsh.getAspectHelper(elementKey);
    @SuppressWarnings("unchecked")
    ArrayList<? extends Map<String,Object>> jsonDisplayChoices = (ArrayList<? extends Map<String, Object>>) 
        ah.getArray(KeyValueStoreConstants.COLUMN_DISPLAY_CHOICES_LIST, Map.class);
    if ( jsonDisplayChoices != null ) {
      return jsonDisplayChoices;
    }
    return new ArrayList<Map<String,Object>>();
  }

  public void setDisplayChoicesList( SQLiteDatabase db, String tableId, ColumnDefinition cd, ArrayList<? extends Map<String,Object>> choices) {
    KeyValueStoreEntry e = new KeyValueStoreEntry();
    e.tableId = tableId;
    e.partition = KeyValueStoreConstants.PARTITION_COLUMN;
    e.aspect = cd.getElementKey();
    e.key = KeyValueStoreConstants.COLUMN_DISPLAY_CHOICES_LIST;
    e.type = ElementDataType.array.name();
    try {
      e.value = ODKFileUtils.mapper.writeValueAsString(choices);
    } catch (JsonProcessingException e1) {
      e1.printStackTrace();
      throw new IllegalArgumentException("Unexpected displayChoices conversion failure!");
    }
    ODKDatabaseUtils.get().replaceDBTableMetadata(db, e);
  }
  
  public ArrayList<JoinColumn> getJoins(SQLiteDatabase db, String tableId, String elementKey) {
    
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, tableId, KeyValueStoreConstants.PARTITION_COLUMN);
    AspectHelper ah = kvsh.getAspectHelper(elementKey);
    ArrayList<JoinColumn> joins = null; 
    try {
      joins = JoinColumn.fromSerialization(ah.getObject(KeyValueStoreConstants.COLUMN_JOINS));
    } catch (JsonParseException e) {
      e.printStackTrace();
    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return (joins == null) ? new ArrayList<JoinColumn>() : joins;
  }
  
  public Class<?> getDataType(ElementDataType dataType) {
    
    if ( dataType == ElementDataType.array ) {
      return ArrayList.class;
    }
    if (dataType == ElementDataType.object) {
      return HashMap.class;
    }
    
    if ( dataType == ElementDataType.integer ) {
      return Integer.class;
    }

    if ( dataType == ElementDataType.number ) {
      return Double.class;
    }

    if ( dataType == ElementDataType.bool ) {
      return Boolean.class;
    }

    return String.class;
  }
}
