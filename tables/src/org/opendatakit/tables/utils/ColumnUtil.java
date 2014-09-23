package org.opendatakit.tables.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.opendatakit.aggregate.odktables.rest.KeyValueStoreConstants;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.ElementDataType;
import org.opendatakit.common.android.data.JoinColumn;
import org.opendatakit.common.android.data.KeyValueStoreEntry;
import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.KeyValueStoreHelper.AspectHelper;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.utilities.NameUtil;
import org.opendatakit.common.android.utilities.ODKDataUtils;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.common.android.utilities.ODKFileUtils;

import android.database.sqlite.SQLiteDatabase;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ColumnUtil {

  public static String getLocalizedDisplayName(TableProperties tp, String elementKey) {
    
    KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(KeyValueStoreConstants.PARTITION_COLUMN);
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

  public static String getRawDisplayName(TableProperties tp, String elementKey) {
    
    KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(KeyValueStoreConstants.PARTITION_COLUMN);
    AspectHelper ah = kvsh.getAspectHelper(elementKey);
    String jsonDisplayName = ah.getObject(KeyValueStoreConstants.COLUMN_DISPLAY_NAME);
    if ( jsonDisplayName == null ) {
      jsonDisplayName = NameUtil.normalizeDisplayName(NameUtil.constructSimpleDisplayName(elementKey));
    }
    return jsonDisplayName;
  }

  public static ArrayList<String> getDisplayChoicesList(TableProperties tp, String elementKey) {
    
    KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(KeyValueStoreConstants.PARTITION_COLUMN);
    AspectHelper ah = kvsh.getAspectHelper(elementKey);
    ArrayList<String> jsonDisplayChoices = ah.getArray(KeyValueStoreConstants.COLUMN_DISPLAY_CHOICES_LIST, String.class);
    if ( jsonDisplayChoices != null ) {
      return jsonDisplayChoices;
    }
    return new ArrayList<String>();
  }

  public static void setDisplayChoicesList( SQLiteDatabase db, String tableId, ColumnDefinition cd, ArrayList<String> choices) {
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
    ODKDatabaseUtils.replaceDBTableMetadata(db, e);
  }
  
  public static ArrayList<JoinColumn> getJoins(TableProperties tp, String elementKey) {
    
    KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(KeyValueStoreConstants.PARTITION_COLUMN);
    AspectHelper ah = kvsh.getAspectHelper(elementKey);
    ArrayList<JoinColumn> joins = null; 
    try {
      joins = JoinColumn.fromSerialization(ah.getString(KeyValueStoreConstants.COLUMN_DISPLAY_CHOICES_LIST));
    } catch (JsonParseException e) {
      e.printStackTrace();
    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return (joins == null) ? new ArrayList<JoinColumn>() : joins;
  }
  
  public static Class<?> getDataType(ElementDataType dataType) {
    
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
