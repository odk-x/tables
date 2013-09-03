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
import org.opendatakit.tables.exceptions.TableAlreadyExistsException;
import org.opendatakit.tables.fragments.InitializeTaskDialogFragment;
import org.opendatakit.tables.utils.ConfigurationUtil;
import org.opendatakit.tables.utils.CsvUtil;
import org.opendatakit.tables.utils.TableFileUtils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

public class InitializeTask extends AsyncTask<Void, Void, Boolean> {
	private static final String EMPTY_STRING = "";

	private static final String SPACE = " ";

	private static final String TOP_LEVEL_KEY_TABLE_KEYS = "table_keys";

	private static final String COMMA = ",";

	private static final String KEY_SUFFIX_CSV_FILENAME = ".filename";

	private static final String KEY_SUFFIX_TABLENAME = ".tablename";

	private static final String TAG = "InitializeTask";

	private InitializeTaskDialogFragment mDialogFragment;
	private final Context mContext;
	private String filename;
	private long fileModifiedTime;
	private int fileCount;
	private int curFileCount;
	private String lineCount;
	private Map<String, Boolean> importStatus;
	private Map<String, Boolean> mTableAlreadyExistsMap;

	public boolean caughtDuplicateTableException = false;
	public boolean problemImportingKVSEntries = false;
	private boolean poorlyFormatedConfigFile = false;

	public InitializeTask(Context context) {
		this.mContext = context;
		this.importStatus = new HashMap<String, Boolean>();
		this.mTableAlreadyExistsMap = new HashMap<String, Boolean>();
	}
	
	public void setDialogFragment(InitializeTaskDialogFragment dialogFragment) {
	  this.mDialogFragment = dialogFragment;
	}

	@Override
	protected synchronized Boolean doInBackground(Void... params) {
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
               CsvUtil cu = new CsvUtil(this.mContext);
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

							boolean success = false;
							try {
    							success = cu.importConfigTables(mContext, this, 
    							    request.getFile(), filename, 
    							    request.getTableName());
    							mTableAlreadyExistsMap.put(filename, false);
							} catch (TableAlreadyExistsException e) {
							  mTableAlreadyExistsMap.put(filename, true);
							  Log.e(TAG, "caught able already exists, setting " +
							  		"success to: " + success);
							}
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
		return true;
	}
	
	@Override
	protected void onProgressUpdate(Void... values) {
     if (mDialogFragment != null) {
       mDialogFragment.updateProgress(curFileCount, 
           fileCount, filename, lineCount);
     } else {
       Log.e(TAG, "dialog fragment is null! Not updating " +
            "progress.");
     }
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
	  if (this.mDialogFragment == null) {
	    Log.e(TAG, "dialog fragment is null! Task can't report back. " +
	    		"Returning.");
	    return;
	  }
	  // From this point forward we'll assume that the dialog fragment is not 
	  // null.

		if (!result) {
		  this.mDialogFragment
		    .onTaskFinishedWithErrors(poorlyFormatedConfigFile);
		} else {
			// Build summary message
			StringBuffer msg = new StringBuffer();
			for (String filename : importStatus.keySet()) {
			  Log.e(TAG, "filename: " + filename);
			  if (importStatus.get(filename)) {
			    Log.e(TAG, "import status from map: " + importStatus.get(filename));
			    msg.append(mContext.getString(R.string.imported_successfully, 
			        filename));
			  } else {
			    // maybe there was an existing table already, maybe there were 
			    // just errors.
			    if (mTableAlreadyExistsMap.get(filename)) {
			      Log.e(TAG, "table already exists map was true");
			      msg.append(mContext.getString(R.string.table_already_exists, 
	                 filename));
			    } else {
			      Log.e(TAG, "table already exists map was false");
	            msg.append(mContext.getString(R.string.imported_with_errors, 
	                filename));			      
			    }
			  }

			}
			this.mDialogFragment.onTaskFinishedSuccessfully(msg.toString(), 
			    fileModifiedTime);
		}
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
