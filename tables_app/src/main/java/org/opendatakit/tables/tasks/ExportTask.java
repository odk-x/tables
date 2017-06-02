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
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.ImportExportDialog;

public class ExportTask extends AsyncTask<ExportRequest, Integer, Boolean>
    implements ExportListener {

  private static final String TAG = "ExportTask";
  /**
   *
   */
  private final String appName;
  private ImportExportDialog progressDialogFragment;
  private AbsBaseActivity context;

  /**
   * @param appName
   */
  public ExportTask(ImportExportDialog progressDialogFragment, String appName,
      AbsBaseActivity context) {
    this.progressDialogFragment = progressDialogFragment;
    this.appName = appName;
    this.context = context;
  }

  // This says whether or not the secondary entries in the key value store
  // were written successfully.
  private boolean keyValueStoreSuccessful = true;

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
      OrderedColumns orderedDefns = Tables.getInstance().getDatabase()
          .getUserDefinedColumns(appName, db, tableId); // export goes to output/csv directory...
      return cu.exportSeparable(this, db, tableId, orderedDefns, request.getFileQualifier());
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Unable to access database");
      e.printStackTrace();
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

  @Override
  public void exportComplete(boolean outcome) {
    keyValueStoreSuccessful = outcome;
  }

  protected void onProgressUpdate(Integer... progress) {
    // do nothing
  }

  protected void onPostExecute(Boolean result) {
    progressDialogFragment.dismiss();
    if (result) {
      if (keyValueStoreSuccessful) {
        ImportExportDialog.newInstance(ImportExportDialog.CSVEXPORT_SUCCESS_DIALOG, context);
      } else {
        ImportExportDialog
            .newInstance(ImportExportDialog.CSVEXPORT_SUCCESS_SECONDARY_KVS_ENTRIES_FAIL_DIALOG,
                context);
      }
    } else {
      ImportExportDialog.newInstance(ImportExportDialog.CSVEXPORT_FAIL_DIALOG, context);
    }
  }
}