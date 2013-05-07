package org.opendatakit.tables.tasks;

import org.opendatakit.tables.activities.importexport.ImportCSVActivity;
import org.opendatakit.tables.exceptions.TableAlreadyExistsException;
import org.opendatakit.tables.utils.CsvUtil;

import android.os.AsyncTask;

public class ImportTask
extends AsyncTask<ImportRequest, Integer, Boolean> {

	private final ImportCSVActivity importCSVActivity;

	/**
	 * @param importCSVActivity
	 */
	public ImportTask(ImportCSVActivity importCSVActivity) {
		this.importCSVActivity = importCSVActivity;
	}

	private static final String TAG = "ImportTask";

	public boolean caughtDuplicateTableException = false;
	public boolean problemImportingKVSEntries = false;

	@Override
	protected Boolean doInBackground(ImportRequest... importRequests) {
		ImportRequest request = importRequests[0];
		CsvUtil cu = new CsvUtil(this.importCSVActivity);
		if (request.getCreateTable()) {
			try {
				return cu.importNewTable(importCSVActivity, this, request.getFile(),
						request.getTableName());
			} catch (TableAlreadyExistsException e) {
				caughtDuplicateTableException = true;
				return false;
			}
		} else {
			return cu.importAddToTable(importCSVActivity, request.getFile(),
					request.getTableProperties().getTableId());
		}
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