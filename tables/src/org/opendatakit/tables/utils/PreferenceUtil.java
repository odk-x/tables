/*
 * Copyright (C) 2012-2014 University of Washington
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

import org.opendatakit.aggregate.odktables.rest.KeyValueStoreConstants;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.common.android.utilities.KeyValueHelper;
import org.opendatakit.common.android.utilities.KeyValueStoreHelper;
import org.opendatakit.common.android.utilities.LocalKeyValueStoreConstants;
import org.opendatakit.common.android.utilities.TableUtil;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.views.SpreadsheetView;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

/**
 * 
 * @author sudar.sam@gmail.com
 *
 */
public class PreferenceUtil {
  
  private static final String TAG = PreferenceUtil.class.getSimpleName();
    
  /**
   * Save viewType to be the default view type for the tableId
   * 
   * @param context
   * @param appName
   * @param tableId
   * @param viewType
   */
  public static void setDefaultViewType(
      Context context,
      String appName,
      String tableId,
      TableViewType viewType) {
    SQLiteDatabase db = null;
    try {
      db = DatabaseFactory.get().getDatabase(context, appName);
      db.beginTransaction();
      TableUtil.get().setDefaultViewType(db, tableId, viewType);
      db.setTransactionSuccessful();
    } catch ( Exception e ) {
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Unable to change default view type: " + e.toString());
      Toast.makeText(
          context,
          "Unable to change default view type",
          Toast.LENGTH_LONG).show();
    } finally {
      if ( db != null ) {
        db.endTransaction();
        db.close();
      }
    }
  }
  
  /**
   * Get the width thast has been set for the column. If none has been set,
   * returns {@link SpreadsheetView#DEFAULT_COL_WIDTH}.
   * @param tableProperties
   * @param elementKey
   * @return
   */
  public static int getColumnWidth(
      Context context, String appName, String tableId,
      String elementKey) {
    Integer result;
    SQLiteDatabase db = null;
    try {
      db = DatabaseFactory.get().getDatabase(context, appName);
      KeyValueStoreHelper kvsh =
          new KeyValueStoreHelper(db, tableId, KeyValueStoreConstants.PARTITION_COLUMN);
      KeyValueHelper aspectHelper = kvsh.getAspectHelper(elementKey);
      result = aspectHelper.getInteger(
          LocalKeyValueStoreConstants.Spreadsheet.KEY_COLUMN_WIDTH);
    } finally {
      if ( db != null ) {
        db.close();
      }
    }
    if (result == null) {
      result = SpreadsheetView.DEFAULT_COL_WIDTH;
    }
    return result;
  }
  
  public static void setColumnWidth(
      Context context,
      String appName,
      String tableId,
      String elementKey,
      int newColumnWith) {
    SQLiteDatabase db = null;
    try {
      db = DatabaseFactory.get().getDatabase(context, appName);
      db.beginTransaction();
      KeyValueStoreHelper kvsh =
          new KeyValueStoreHelper(db, tableId, KeyValueStoreConstants.PARTITION_COLUMN);
      KeyValueHelper aspectHelper = kvsh.getAspectHelper(elementKey);
      aspectHelper.setInteger(
          LocalKeyValueStoreConstants.Spreadsheet.KEY_COLUMN_WIDTH,
          newColumnWith);
      db.setTransactionSuccessful();
    } finally {
      if ( db != null ) {
        db.endTransaction();
        db.close();
      }
    }
  }

}
