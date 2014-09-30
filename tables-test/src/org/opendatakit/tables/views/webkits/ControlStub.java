package org.opendatakit.tables.views.webkits;

import java.util.ArrayList;
import java.util.Map;

import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.tables.activities.AbsBaseActivity;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class ControlStub extends Control {
  
  public static ContentValues CONTENT_VALUES = null;
  public static String GENERATED_ROW_ID = null;

  public ControlStub(AbsBaseActivity activity, String appName) {
    super(activity, appName);
  }
  
  public static void resetState() {
    CONTENT_VALUES = null;
    GENERATED_ROW_ID = null;
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
