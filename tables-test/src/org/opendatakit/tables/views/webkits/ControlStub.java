package org.opendatakit.tables.views.webkits;

import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.utils.ODKDatabaseUtilsWrapper;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;

public class ControlStub extends Control {
  
  public static ODKDatabaseUtilsWrapper DB_UTILS_WRAPPER = null;
  public static TableProperties TABLE_PROPERTIES_FOR_ID = null;
  public static SQLiteDatabase DATABASE = null;

  public ControlStub(Activity activity, String appName) {
    super(activity, appName);
  }
  
  public static void resetState() {
    DB_UTILS_WRAPPER = null;
    TABLE_PROPERTIES_FOR_ID = null;
    DATABASE = null;
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
  

}
