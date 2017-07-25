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
package org.opendatakit.tables.tasks;

import android.os.AsyncTask;
import org.opendatakit.builder.CsvUtil;
import org.opendatakit.builder.CsvUtilSupervisor;
import org.opendatakit.database.data.OrderedColumns;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.listener.ExportListener;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.fragments.ImportExportDialogFragment;

/**
 * Represents a task to export a table to some csv files using CsvUtil
 */
public class ExportTask extends AsyncTask<ExportRequest, Integer, Boolean>
    implements ExportListener {

  // Used for logging
  private static final String TAG = ExportTask.class.getSimpleName();
  // The app name
  private final String appName;
  // The context the progress dialog needs
  private AbsBaseActivity context;

  /**
   * Constructor that stores off its arguments
   *
   * @param appName the app name
   * @param context the activity that the progress dialog is running in
   */
  public ExportTask(String appName, AbsBaseActivity context) {
    super();
    this.appName = appName;
    this.context = context;
  }

  /**
   * Tells services to export the csv file
   *
   * @param exportRequests what request to act on
   * @return whether it was successful or not
   */
  protected Boolean doInBackground(ExportRequest... exportRequests) {
    ExportRequest request = exportRequests[0];
    CsvUtil cu = new CsvUtil(new CsvUtilSupervisor() {
      @Override
      public UserDbInterface getDatabase() {
        return Tables.getInstance().getDatabase();
      }
    }, appName);
    DbHandle db = null;
    try {
      String tableId = request.getTableId();
      db = Tables.getInstance().getDatabase().openDatabase(appName);
      OrderedColumns orderedDefinitions = Tables.getInstance().getDatabase()
              .getUserDefinedColumns(appName, db, tableId); // export goes to output/csv directory...
      return cu.exportSeparable(this, db, tableId, orderedDefinitions, request.getFileQualifier());
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(appName).e(TAG, "Unable to access database");
      WebLogger.getLogger(appName).printStackTrace(e);
      return false;
    } finally {
      if (db != null) {
        try {
          Tables.getInstance().getDatabase().closeDatabase(appName, db);
        } catch (ServicesAvailabilityException e) {
          WebLogger.getLogger(appName).printStackTrace(e);
          WebLogger.getLogger(appName).e(TAG, "Unable to close database");
        }
      }
    }
  }

  /**
   * does nothing
   *
   * @param progress unknown
   */
  protected void onProgressUpdate(Integer... progress) {
    // do nothing
  }

  /**
   * Called when the export is done. Dismisses the "Import in progress..." dialog, and displays
   * a success alert dialog or one of the failure alert dialogs, which will be dismissed by the user
   *
   * @param result Whether the import was successful
   */
  protected void onPostExecute(Boolean result) {
    ImportExportDialogFragment.activeDialogFragment.dismiss();
    if (result) {
      ImportExportDialogFragment
              .newInstance(ImportExportDialogFragment.CSVEXPORT_SUCCESS_DIALOG, context);
    } else {
      ImportExportDialogFragment
              .newInstance(ImportExportDialogFragment.CSVEXPORT_FAIL_DIALOG, context);
    }
  }

  /**
   * Updates the open progress dialog with the new status
   * just passes along the request to ImportExportDialogFragment
   *
   * @param row the string to set in the window, like "Exporting row 10"
   */
  @Override
  public void updateProgressDetail(int row, int total) {
    ImportExportDialogFragment.activeDialogFragment
            .updateProgressDialogStatusString(context, R.string.export_in_progress_row, row, total);
  }
}
