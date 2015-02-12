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

import java.util.ArrayList;

import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.common.android.utilities.CsvUtil;
import org.opendatakit.common.android.utilities.CsvUtil.ExportListener;
import org.opendatakit.common.android.utilities.TableUtil;
import org.opendatakit.tables.activities.ExportCSVActivity;

import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

public class ExportTask
        extends AsyncTask<ExportRequest, Integer, Boolean> implements ExportListener {

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
        CsvUtil cu = new CsvUtil(this.exportCSVActivity, appName);
        SQLiteDatabase db = null;
        try {
          String tableId = request.getTableId();
          db = DatabaseFactory.get().getDatabase(this.exportCSVActivity, appName);
          ArrayList<ColumnDefinition> orderedDefns = TableUtil.get().getColumnDefinitions(db, appName, tableId);
          // export goes to output/csv directory...
          return cu.exportSeparable(this, db, tableId, orderedDefns, request.getFileQualifier());
        } finally {
          if ( db != null ) {
            db.close();
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