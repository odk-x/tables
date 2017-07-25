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

import android.content.Context;
import android.widget.Toast;
import org.opendatakit.activities.BaseActivity;
import org.opendatakit.data.TableViewType;
import org.opendatakit.data.utilities.ColumnUtil;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.application.Tables;

/**
 * @author sudar.sam@gmail.com
 */
public final class PreferenceUtil {

  /**
   * Used for logging
   */
  @SuppressWarnings("unused")
  private static final String TAG = PreferenceUtil.class.getSimpleName();

  /**
   * Do not instantiate this class
   */
  private PreferenceUtil() {
  }

  /**
   * Save viewType (i.e. spreadsheet, detail, map...) to be the default view type for the tableId
   *
   * @param context  a context used for displaying an error
   * @param appName  the app name
   * @param tableId  the id of the table to set the view type on
   * @param viewType the view type to save
   */
  public static void setDefaultViewType(Context context, String appName, String tableId,
      TableViewType viewType) {

    try {
      TableUtil.get()
          .atomicSetDefaultViewType(Tables.getInstance().getDatabase(), appName, tableId,
              viewType);
    } catch (ServicesAvailabilityException ignored) {
      Toast.makeText(context, R.string.unable_to_change_default_view_type, Toast.LENGTH_LONG)
          .show();
    }
  }

  /**
   * Get the width that has been set for the column. If none has been set,
   * returns {@see DEFAULT_COL_WIDTH}.
   *
   * @param act        an activity used to get the UserDbInterface object
   * @param appName    the app name
   * @param tableId    the table id that the column is in
   * @param elementKey the column id of the column to get the width for
   * @return a width in pixels for the column
   * @throws ServicesAvailabilityException if the database is down
   */
  public static int getColumnWidth(BaseActivity act, String appName, String tableId,
      String elementKey) throws ServicesAvailabilityException {
    Integer result = null;
    DbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(appName);
      result = ColumnUtil.get().getColumnWidth(Tables.getInstance().getDatabase(), appName, db, tableId, elementKey);
    } finally {
      if (db != null) {
        Tables.getInstance().getDatabase().closeDatabase(appName, db);
      }
    }
    return result;
  }

  /**
   * Sets the column width in the database. Reset when you sync
   *
   * @param context        A context used for displaying an error message
   * @param appName        the app name
   * @param tableId        the id of the table that has the column
   * @param elementKey     the id of the column to change the width of
   * @param newColumnWidth the new width of the column, in pixels
   */
  public static void setColumnWidth(Context context, String appName, String tableId,
      String elementKey, int newColumnWidth) {

    try {
      ColumnUtil.get()
          .atomicSetColumnWidth(Tables.getInstance().getDatabase(), appName, tableId,
              elementKey, newColumnWidth);
    } catch (ServicesAvailabilityException e) {
      Toast.makeText(context, R.string.change_column_width_error, Toast.LENGTH_LONG).show();
      WebLogger.getLogger(appName).printStackTrace(e);
    }
  }

}
