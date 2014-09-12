package org.opendatakit.tables.tasks;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.database.DataModelDatabaseHelper;
import org.opendatakit.common.android.database.DataModelDatabaseHelperFactory;
import org.opendatakit.common.android.utilities.CsvUtil;
import org.opendatakit.common.android.utilities.CsvUtil.ExportListener;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
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
          String tableId = request.getTableProperties().getTableId();
          DataModelDatabaseHelper dbh = DataModelDatabaseHelperFactory.getDbHelper(this.exportCSVActivity, appName);
          db = dbh.getReadableDatabase();
          List<Column> columns = ODKDatabaseUtils.getUserDefinedColumns(db, tableId);
          ArrayList<ColumnDefinition> orderedDefns = ColumnDefinition.buildColumnDefinitions(columns);
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