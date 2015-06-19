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

import org.opendatakit.common.android.data.OrderedColumns;
import org.opendatakit.common.android.utilities.CsvUtil;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.common.android.utilities.CsvUtil.ExportListener;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.tables.activities.ExportCSVActivity;
import org.opendatakit.tables.application.Tables;

import android.os.AsyncTask;
import android.os.RemoteException;

public class ExportTask
        extends AsyncTask<ExportRequest, Integer, Boolean> implements ExportListener {

  private static final String TAG = "ExportTask";
  /**
	 *
	 */
	private final ExportCSVActivity exportCSVActivity;
	private final String appName;

	/**
	 * @param exportCSVActivity
	 */
	public ExportTask(ExportCSVActivity exportCSVActivity, String appName) {
		this.exportCSVActivity = exportCSVActivity;
		this.appName = appName;
	}

// This says whether or not the secondary entries in the key value store
  // were written successfully.
  private boolean keyValueStoreSuccessful = true;

    protected Boolean doInBackground(ExportRequest... exportRequests) {
        ExportRequest request = exportRequests[0];
        CsvUtil cu = new CsvUtil(Tables.getInstance(), appName);
        OdkDbHandle db = null;
        try {
          String tableId = request.getTableId();
          db = Tables.getInstance().getDatabase().openDatabase(appName, false);
          OrderedColumns orderedDefns = Tables.getInstance().getDatabase().getUserDefinedColumns(appName, db, tableId);          // export goes to output/csv directory...
          return cu.exportSeparable(this, db, tableId, orderedDefns, request.getFileQualifier());
        } catch (RemoteException e) {
          WebLogger.getLogger(appName).printStackTrace(e);
          WebLogger.getLogger(appName).e(TAG, "Unable to access database");
          e.printStackTrace();
          return false;
        } finally {
          if ( db != null ) {
            try {
              Tables.getInstance().getDatabase().closeDatabase(appName, db);
            } catch (RemoteException e) {
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
        this.exportCSVActivity.dismissDialog(ExportCSVActivity.EXPORT_IN_PROGRESS_DIALOG);
        if (result) {
          if (keyValueStoreSuccessful) {
            this.exportCSVActivity.showDialog(ExportCSVActivity.CSVEXPORT_SUCCESS_DIALOG);
          } else {
            this.exportCSVActivity.showDialog(ExportCSVActivity.CSVEXPORT_SUCCESS_SECONDARY_KVS_ENTRIES_FAIL_DIALOG);
          }
        } else {
            this.exportCSVActivity.showDialog(ExportCSVActivity.CSVEXPORT_FAIL_DIALOG);
        }
    }
}