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

import org.opendatakit.common.android.utilities.CsvUtil;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.common.android.utilities.CsvUtil.ImportListener;
import org.opendatakit.tables.activities.ImportCSVActivity;
import org.opendatakit.tables.application.Tables;

import android.os.AsyncTask;
import android.os.RemoteException;

public class ImportTask
extends AsyncTask<ImportRequest, Integer, Boolean> implements ImportListener {

  private static final String TAG = "ImportTask";

	private final ImportCSVActivity importCSVActivity;
	private final String appName;

	/**
	 * @param importCSVActivity
	 */
	public ImportTask(ImportCSVActivity importCSVActivity, String appName) {
		this.importCSVActivity = importCSVActivity;
		this.appName = appName;
	}

	public boolean caughtDuplicateTableException = false;
	public boolean problemImportingKVSEntries = false;

	@Override
	protected Boolean doInBackground(ImportRequest... importRequests) {
		ImportRequest request = importRequests[0];
		CsvUtil cu = new CsvUtil(Tables.getInstance(), appName);
		  try {
        return cu.importSeparable(this, request.getTableId(),
             request.getFileQualifier(), request.getCreateTable());
      } catch (RemoteException e) {
        WebLogger.getLogger(appName).printStackTrace(e);
        WebLogger.getLogger(appName).e(TAG, "Unable to access database");
        return false;
      }
	}

	  @Override
	  public void importComplete(boolean outcome) {
	    problemImportingKVSEntries = !outcome;
	  }

	  @Override
	  public void updateProgressDetail(String progressString) {
	    // TODO present progressString in a dialog
	  }

	protected void onProgressUpdate(Integer... progress) {
		// do nothing.
	}

	protected void onPostExecute(Boolean result) {
		this.importCSVActivity.dismissDialog(ImportCSVActivity.IMPORT_IN_PROGRESS_DIALOG);
		if (result) {
			this.importCSVActivity.showDialog(ImportCSVActivity.CSVIMPORT_SUCCESS_DIALOG);
		} else {
			if (caughtDuplicateTableException) {
				this.importCSVActivity.showDialog(ImportCSVActivity.CSVIMPORT_FAIL_DUPLICATE_TABLE);
			} else if (problemImportingKVSEntries) {
				this.importCSVActivity.showDialog(ImportCSVActivity.CSVEXPORT_SUCCESS_SECONDARY_KVS_ENTRIES_FAIL_DIALOG);
			} else {
				this.importCSVActivity.showDialog(ImportCSVActivity.CSVIMPORT_FAIL_DIALOG);
			}
		}
	}
}