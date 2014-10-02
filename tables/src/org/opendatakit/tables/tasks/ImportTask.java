package org.opendatakit.tables.tasks;

import org.opendatakit.common.android.utilities.CsvUtil;
import org.opendatakit.common.android.utilities.CsvUtil.ImportListener;
import org.opendatakit.tables.activities.ImportCSVActivity;

import android.os.AsyncTask;

public class ImportTask
extends AsyncTask<ImportRequest, Integer, Boolean> implements ImportListener {

	private final ImportCSVActivity importCSVActivity;
	private final String appName;

	/**
	 * @param importCSVActivity
	 */
	public ImportTask(ImportCSVActivity importCSVActivity, String appName) {
		this.importCSVActivity = importCSVActivity;
		this.appName = appName;
	}

	private static final String TAG = "ImportTask";

	public boolean caughtDuplicateTableException = false;
	public boolean problemImportingKVSEntries = false;

	@Override
	protected Boolean doInBackground(ImportRequest... importRequests) {
		ImportRequest request = importRequests[0];
		CsvUtil cu = new CsvUtil(this.importCSVActivity, appName);
		  return cu.importSeparable(this, request.getTableId(),
		       request.getFileQualifier(), request.getCreateTable());
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