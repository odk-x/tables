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

import org.opendatakit.builder.CsvUtilSupervisor;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.builder.CsvUtil;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.listener.ImportListener;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.utils.ImportExportDialog;
import org.opendatakit.tables.application.Tables;

import android.os.AsyncTask;

public class ImportTask
extends AsyncTask<ImportRequest, Integer, Boolean> implements ImportListener {

  private static final String TAG = "ImportTask";

	private final String appName;
	private ImportExportDialog progressDialogFragment;
  private AbsBaseActivity context;
	/**
	 * @param TODO
	 */
	public ImportTask(ImportExportDialog progressDialogFragment, String appName, AbsBaseActivity context) {
		this.progressDialogFragment = progressDialogFragment;
		this.appName = appName;
    this.context = context;
	}

	public boolean caughtDuplicateTableException = false;
	public boolean problemImportingKVSEntries = false;

	@Override
	protected Boolean doInBackground(ImportRequest... importRequests) {
		ImportRequest request = importRequests[0];
		CsvUtil cu = new CsvUtil(new CsvUtilSupervisor() {
			@Override public UserDbInterface getDatabase() {
				return Tables.getInstance().getDatabase();
			}
		}, appName);
		  try {
        return cu.importSeparable(this, request.getTableId(),
             request.getFileQualifier(), request.getCreateTable());
      } catch (ServicesAvailabilityException e) {
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
			progressDialogFragment.updateProgressDialogStatusString(this, progressString);
	  }

	protected void onProgressUpdate(Integer... progress) {
		// do nothing.
	}

	protected void onPostExecute(Boolean result) {
		progressDialogFragment.dismiss();
		if (result) {
			ImportExportDialog.newInstance(ImportExportDialog.CSVIMPORT_SUCCESS_DIALOG, context);
		} else {
			if (caughtDuplicateTableException) {
				ImportExportDialog.newInstance(ImportExportDialog
								.CSVIMPORT_FAIL_DUPLICATE_TABLE, context);
			} else if (problemImportingKVSEntries) {
				ImportExportDialog.newInstance(ImportExportDialog.CSVEXPORT_SUCCESS_SECONDARY_KVS_ENTRIES_FAIL_DIALOG, context);
			} else {
				ImportExportDialog.newInstance(ImportExportDialog.CSVIMPORT_FAIL_DIALOG, context);
			}
		}
	}
}