/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

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
