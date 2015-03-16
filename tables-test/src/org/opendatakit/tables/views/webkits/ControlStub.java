package org.opendatakit.tables.views.webkits;

import java.util.Map;

import org.opendatakit.common.android.data.OrderedColumns;
import org.opendatakit.tables.activities.AbsBaseActivity;

import android.content.ContentValues;
import android.content.Context;
import android.os.RemoteException;

public class ControlStub extends Control {
  
  public static ContentValues CONTENT_VALUES = null;
  public static String GENERATED_ROW_ID = null;

  public ControlStub(AbsBaseActivity activity, String tableId, OrderedColumns orderedDefns) throws RemoteException {
    super(activity, tableId, orderedDefns);
  }
  
  public static void resetState() {
    CONTENT_VALUES = null;
    GENERATED_ROW_ID = null;
  }
  
  @Override
  protected ContentValues getContentValuesFromMap(
      Context context, String appName, String tableId,
      OrderedColumns orderedDefns,
      Map<String, String> elementKeyToValue) {
    return CONTENT_VALUES;
  }
  
  @Override
  protected String generateRowId() {
    return GENERATED_ROW_ID;
  }
  

}
