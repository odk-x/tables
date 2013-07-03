package org.opendatakit.tables.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.data.Preferences;
import org.opendatakit.tables.utils.ConfigurationUtil;
import org.opendatakit.tables.utils.CsvUtil;
import org.opendatakit.tables.utils.TableFileUtils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

public class InitializeTask extends AsyncTask<Void, Void, Boolean> {
	private static final String EMPTY_STRING = "";

	private static final String SPACE = " ";

	private static final String TOP_LEVEL_KEY_TABLE_KEYS = "table_keys";

	private static final String COMMA = ",";

	private static final String KEY_SUFFIX_CSV_FILENAME = ".filename";

	private static final String KEY_SUFFIX_TABLENAME = ".tablename";

	private static final String TAG = "InitializeTask";

	private final Callbacks mCallbacks;
	private final Context mContext;
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

	public InitializeTask(Context context, Callbacks callbacks) {
		this.mCallbacks = callbacks;
		this.mContext = context;
		this.dialog = new ProgressDialog(context);
		this.importStatus = new HashMap<String, Boolean>();
	}

	@Override
	protected void onPreExecute() {
		dialog.setTitle(mContext.getString(R.string.configuring_tables));
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setCancelable(false);
		dialog.show();
	}

	@Override
	protected synchronized Boolean doInBackground(Void... params) {
		if (ConfigurationUtil.isChanged(mCallbacks.getPrefs())) {
			Properties prop = new Properties();
			try {
				File config = new File(ODKFileUtils.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME),
						TableFileUtils.ODK_TABLES_CONFIG_PROPERTIES_FILENAME);
				prop.load(new FileInputStream(config));
			} catch (IOException ex) {
				ex.printStackTrace();
				return false;
			}

			// prop was loaded
			if (prop != null) {
				fileModifiedTime = new File(ODKFileUtils.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME),
						TableFileUtils.ODK_TABLES_CONFIG_PROPERTIES_FILENAME).lastModified();
				String table_keys = prop.getProperty(TOP_LEVEL_KEY_TABLE_KEYS);

				// table_keys is defined
				if (table_keys != null) {
					// remove spaces and split at commas to get key names
					String[] keys = table_keys.replace(SPACE,EMPTY_STRING).split(COMMA);
					fileCount = keys.length;
					curFileCount = 0;

					String tablename;
					File file;
					for (String key : keys) {
						lineCount = mContext.getString(R.string.processing_file);
						curFileCount++;
						tablename = prop.getProperty(key + KEY_SUFFIX_TABLENAME);
						filename = prop.getProperty(key + KEY_SUFFIX_CSV_FILENAME);
						file = new File(ODKFileUtils.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME),
								filename);

						// update dialog message with current filename
						publishProgress();

						// .tablename is defined
						if (tablename != null) {
							ImportRequest request = new ImportRequest(true, null, tablename, file);

							CsvUtil cu = new CsvUtil(this.mContext);

							boolean success = cu.importConfigTables(mContext, this, 
							    request.getFile(), filename, request.getTableName());
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
		dialog.setMessage(mContext.getString(R.string.importing_file,
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
		mCallbacks.onImportsComplete();

		// dismiss spinning ProgressDialog
		dialog.dismiss();

		// build AlertDialog displaying the status of the initialization
		AlertDialog.Builder alertDialogBuilder = 
		    new AlertDialog.Builder(mContext);
		alertDialogBuilder.setCancelable(true);
		alertDialogBuilder.setNeutralButton(mContext.getString(R.string.ok), 
		    new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		if (!result) {
			if (poorlyFormatedConfigFile)
				alertDialogBuilder.setTitle(
				    mContext.getString(R.string.bad_config_properties_file));
			else
				alertDialogBuilder.setTitle(
				    mContext.getString(R.string.error));
		} else {
			// update the lastModifiedTime of Tables in Preferences
			ConfigurationUtil.updateTimeChanged(
			    mCallbacks.getPrefs(), fileModifiedTime);

			// Build summary message
			alertDialogBuilder.setTitle(
			    mContext.getString(R.string.config_summary));
			StringBuffer msg = new StringBuffer();
			for (String filename : importStatus.keySet()) {
				msg.append(mContext.getString((importStatus.get(filename) ?
						R.string.imported_successfully : R.string.imported_with_errors), filename));
			}
			alertDialogBuilder.setMessage(msg);
		}

		AlertDialog dialog2 = alertDialogBuilder.create();
		dialog2.show();
	}
	
	public interface Callbacks {
	  
	  /**
	   * Get an {@link Preferences} object for handling information about the 
	   * time of the last import from a config file, etc. 
	   * @return
	   */
	  public Preferences getPrefs();
	  
	  /**
	   * Update the display to the user after the import is complete.
	   */
	  public void onImportsComplete();

	}
}
