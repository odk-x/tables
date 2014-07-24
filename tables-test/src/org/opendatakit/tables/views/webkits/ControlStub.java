package org.opendatakit.tables.views.webkits;

import java.util.Map;

import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.utils.ODKDatabaseUtilsWrapper;

import android.app.Activity;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

public class ControlStub extends Control {
  
  public static ODKDatabaseUtilsWrapper DB_UTILS_WRAPPER = null;
  public static TableProperties TABLE_PROPERTIES_FOR_ID = null;
  public static SQLiteDatabase DATABASE = null;
  public static ContentValues CONTENT_VALUES = null;
  public static String GENERATED_ROW_ID = null;

  public ControlStub(Activity activity, String appName) {
    super(activity, appName);
  }
  
  public static void resetState() {
    DB_UTILS_WRAPPER = null;
    TABLE_PROPERTIES_FOR_ID = null;
    DATABASE = null;
    CONTENT_VALUES = null;
    GENERATED_ROW_ID = null;
  }
  
  @Override
  protected ODKDatabaseUtilsWrapper getODKDatabaseUtilsWrapper() {
    return DB_UTILS_WRAPPER;
  }
  
  @Override
  TableProperties retrieveTablePropertiesForTable(String tableId) {
    return TABLE_PROPERTIES_FOR_ID;
  }
  
  @Override
  protected SQLiteDatabase getWritableDatabase() {
    return DATABASE;
  }
  
  @Override
  protected ContentValues getContentValuesFromMap(
      TableProperties tableProperties,
      Map<String, String> elementKeyToValue) {
    return CONTENT_VALUES;
  }
  
  @Override
  protected String generateRowId() {
    return GENERATED_ROW_ID;
  }
  

}
