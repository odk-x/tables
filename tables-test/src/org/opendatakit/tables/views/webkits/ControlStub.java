package org.opendatakit.tables.views.webkits;

import java.util.ArrayList;
import java.util.Map;

import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.utils.ODKDatabaseUtilsWrapper;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class ControlStub extends Control {
  
  public static ODKDatabaseUtilsWrapper DB_UTILS_WRAPPER = null;
  public static TableProperties TABLE_PROPERTIES_FOR_ID = null;
  public static SQLiteDatabase DATABASE = null;
  public static ContentValues CONTENT_VALUES = null;
  public static String GENERATED_ROW_ID = null;

  public ControlStub(AbsBaseActivity activity, String appName) {
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
  protected SQLiteDatabase getWritableDatabase() {
    return DATABASE;
  }
  
  @Override
  protected ContentValues getContentValuesFromMap(
      Context context, String appName, String tableId,
      ArrayList<ColumnDefinition> orderedDefns,
      Map<String, String> elementKeyToValue) {
    return CONTENT_VALUES;
  }
  
  @Override
  protected String generateRowId() {
    return GENERATED_ROW_ID;
  }
  

}
