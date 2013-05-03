package org.opendatakit.tables.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableManager;
import org.opendatakit.tables.utils.ConfigurationUtil;
import org.opendatakit.tables.utils.CsvUtil;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;

public class InitializeTask extends AsyncTask<Void, Void, Boolean> {
	private static final String TAG = "InitializeTask";

	private final String root = Environment.getExternalStorageDirectory().getPath();
	private final String filepath = "/odk/tables/config.properties";
	private final TableManager tm;
	private ProgressDialog dialog;
	private String filename;
	private long fileModifiedTime;
	private int fileCount;
	private int curFileCount;
	private String lineCount;
	private Map<String, Boolean> importStatus;

	public boolean caughtDuplicateTableException = false;
	public boolean problemImportingKVSEntries = false;
	private boolean poorlyFormatedConfigFile = false;

	public InitializeTask(TableManager tm) {
		this.tm = tm;
		this.dialog = new ProgressDialog(tm);
		this.importStatus = new HashMap<String, Boolean>();
	}

	@Override
	protected void onPreExecute() {
		dialog.setTitle(tm.getString(R.string.configuring_tables));
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setCancelable(false);
		dialog.show();
	}

	@Override
	protected synchronized Boolean doInBackground(Void... params) {
		if (ConfigurationUtil.isChanged(tm.getPrefs())) {
			Properties prop = new Properties();
			try {
				File config = new File(root, filepath);
				prop.load(new FileInputStream(config));
			} catch (IOException ex) {
				ex.printStackTrace();
				return false;
			}

			// prop was loaded
			if (prop != null) {
				fileModifiedTime = new File(root, filepath).lastModified();
				String table_keys = prop.getProperty("table_keys");

				// "table_keys" is defined
				if (table_keys != null) {
					String[] keys = table_keys.split(",");
					fileCount = keys.length;
					curFileCount = 0;

					String tablename;
					String filepath;
					File file;
					for (String key : keys) {
						lineCount = tm.getString(R.string.processing_file);
						curFileCount++;
						tablename = prop.getProperty(key + ".tablename");
						filename = prop.getProperty(key + ".filename");
						filepath = root + "/odk/tables/" +
								prop.getProperty(key + ".filename");
						file = new File(filepath);

						// update dialog message with current filename
						publishProgress();

						// .tablename is defined
						if (tablename != null) {
							ImportRequest request = new ImportRequest(tablename, file);

							CsvUtil cu = new CsvUtil(this.tm);

							boolean success = cu.importConfigTables(this, request.getFile(),
									filename, request.getTableName());
							importStatus.put(filename, success);
							if (success) {
								publishProgress();
							}
						} else {
							poorlyFormatedConfigFile = true;
							return false;
						}
					}
				} else {
					poorlyFormatedConfigFile = true;
					return false;
				}
			}
		}
		return true;
	}

	// refresh TableManager after each successful import
	protected void onProgressUpdate(Void... progress) {
		dialog.setMessage(tm.getString(R.string.importing_file,
				curFileCount, fileCount, filename, lineCount ));
	}

	public void updateLineCount(String lineCount) {
		this.lineCount = lineCount;
		publishProgress();
	}

	// dismiss ProgressDialog and create an AlertDialog with one
	// button to confirm that the user read the postExecute message
	@Override
	protected void onPostExecute(Boolean result) {
		// refresh TableManager to show newly imported tables
		tm.refreshList();

		// dismiss spinning ProgressDialog
		dialog.dismiss();

		// build AlertDialog displaying the status of the initialization
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(tm);
		alertDialogBuilder.setCancelable(true);
		alertDialogBuilder.setNeutralButton(tm.getString(R.string.ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		if (!result) {
			if (poorlyFormatedConfigFile)
				alertDialogBuilder.setTitle(tm.getString(R.string.bad_config_properties_file));
			else
				alertDialogBuilder.setTitle(tm.getString(R.string.error));
		} else {
			// update the lastModifiedTime of Tables in Preferences
			ConfigurationUtil.updateTimeChanged(tm.getPrefs(), fileModifiedTime);

			// Build summary message
			alertDialogBuilder.setTitle(tm.getString(R.string.config_summary));
			StringBuffer msg = new StringBuffer();
			for (String filename : importStatus.keySet()) {
				msg.append(tm.getString((importStatus.get(filename) ?
						R.string.imported_successfully : R.string.imported_with_errors), filename));
			}
			alertDialogBuilder.setMessage(msg);
		}

		AlertDialog dialog2 = alertDialogBuilder.create();
		dialog2.show();
	}
}
