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

import android.content.Context;
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
	private final String mAppName;
	private String filename;
	private long fileModifiedTime;
	private int fileCount;
	private int curFileCount;
	private String lineCount;
	private Map<String, Boolean> importStatus;
	/** Holds the key to whether or not the table already exists. */
	private Map<String, Boolean> mKeyToTableAlreadyExistsMap;
	/** Stores the tables key to whether or not the file was found. */
	private Map<String, Boolean> mKeyToFileNotFoundMap;
	/** Stores the table's key to its filename. */
	private Map<String, String> mKeyToFileMap;

	public boolean caughtDuplicateTableException = false;
	public boolean problemImportingKVSEntries = false;
	private boolean poorlyFormatedConfigFile = false;

	public InitializeTask(Context context, String appName) {
		this.mContext = context;
		this.mAppName = appName;
		this.importStatus = new HashMap<String, Boolean>();
		this.mKeyToTableAlreadyExistsMap = new HashMap<String, Boolean>();
		this.mKeyToFileNotFoundMap = new HashMap<String, Boolean>();
		this.mKeyToFileMap = new HashMap<String, String>();
	}

	public void setDialogFragment(InitializeTaskDialogFragment dialogFragment) {
	  this.mDialogFragment = dialogFragment;
	}

	@Override
	protected synchronized Boolean doInBackground(Void... params) {
			Properties prop = new Properties();
			try {
				File config = new File(ODKFileUtils.getAppFolder(mAppName),
						TableFileUtils.ODK_TABLES_CONFIG_PROPERTIES_FILENAME);
				prop.load(new FileInputStream(config));
			} catch (IOException ex) {
				ex.printStackTrace();
				return false;
			}

			// prop was loaded
			if (prop != null) {
			  // This is an unpleasant solution. We're currently saving the file
			  // time as used the instant a properties file is loaded. In theory
			  // it seems like this should only be saved AFTER the file is
			  // successfully used. This is not being done for several reasons.
			  // First, despite my best efforts to get the DialogFragments and
			  // asynctasks to play nice, somehow it still is not consistently
			  // finding the InitializeTaskDialogFragment in onCreate. It usually,
			  // usually does, but this is seriously annoying to not work
			  // consistently. I can't find a good reason as to why.
			  // Second, say that a config attempt did fail, perhaps causing a
			  // force close. Without this fix, it would consistently crash,
			  // trying each time to load the same misconfigured config file. This
			  // is similarly unacceptable. So, this seems a way to try and avoid
			  // both problems, while perhaps eliminating a very annoying problem.
			  // However, it still feels like a hack, and I wish the AsyncTask/
			  // Fragment situation wasn't so damned irritating.
				fileModifiedTime = new File(ODKFileUtils.getAppFolder(mAppName),
						TableFileUtils.ODK_TABLES_CONFIG_PROPERTIES_FILENAME).lastModified();
				ConfigurationUtil.updateTimeChanged(
				    this.mDialogFragment.getPreferencesFromContext(),
				    fileModifiedTime);
				String table_keys = prop.getProperty(TOP_LEVEL_KEY_TABLE_KEYS);

				// table_keys is defined
				if (table_keys != null) {
					// remove spaces and split at commas to get key names
					String[] keys = table_keys.replace(SPACE,EMPTY_STRING).split(COMMA);
					fileCount = keys.length;
					curFileCount = 0;

					String tablename;
					File file;
               CsvUtil cu = new CsvUtil(this.mContext, this.mAppName);
					for (String key : keys) {
						lineCount = mContext.getString(R.string.processing_file);
						curFileCount++;
						tablename = prop.getProperty(key + KEY_SUFFIX_TABLENAME);
						filename = prop.getProperty(key + KEY_SUFFIX_CSV_FILENAME);
	               this.importStatus.put(key, false);
						file = new File(ODKFileUtils.getAppFolder(mAppName),
								filename);
						this.mKeyToFileMap.put(key, filename);
						if (!file.exists()) {
						  this.mKeyToFileNotFoundMap.put(key, true);
						  Log.e(TAG, "putting in file not found map true: " + key);
						  continue;
						} else {
						  this.mKeyToFileNotFoundMap.put(key, false);
						  Log.e(TAG, "putting in file not found map false: " + key);
						  // and proceed.
						}

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
    							mKeyToTableAlreadyExistsMap.put(key, false);
							} catch (TableAlreadyExistsException e) {
							  mKeyToTableAlreadyExistsMap.put(key, true);
							  Log.e(TAG, "caught able already exists, setting " +
							  		"success to: " + success);
							}
							importStatus.put(key, success);
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
			for (String key : mKeyToFileMap.keySet()) {
			  Log.e(TAG, "key: " + key);
			  if (importStatus.get(key)) {
			    String nameOfFile = mKeyToFileMap.get(key);
			    Log.e(TAG, "import status from map: " + importStatus.get(key));
			    msg.append(mContext.getString(R.string.imported_successfully,
			        nameOfFile));
			  } else {
			    // maybe there was an existing table already, maybe there were
			    // just errors.
			    if (mKeyToTableAlreadyExistsMap.containsKey(key) &&
			        mKeyToTableAlreadyExistsMap.get(key)) {
			      Log.e(TAG, "table already exists map was true");
			      msg.append(mContext.getString(R.string.table_already_exists,
	                 key));
			    } else if (mKeyToFileNotFoundMap.containsKey(key) &&
			        mKeyToFileNotFoundMap.get(key)) {
			      // We'll first retrieve the file to which this key was pointing.
			      String nameOfFile = mKeyToFileMap.get(key);
			      Log.e(TAG, "file wasn't found: " + key);
			      msg.append(mContext.getString(R.string.file_not_found,
			          nameOfFile));
			    } else {
			      // a general error.
			      Log.e(TAG, "table already exists map was false");
	            msg.append(mContext.getString(R.string.imported_with_errors,
	                key));
			    }
			  }

			}
			this.mDialogFragment.onTaskFinishedSuccessfully(msg.toString());
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
