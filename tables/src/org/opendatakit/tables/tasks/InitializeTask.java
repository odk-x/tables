package org.opendatakit.tables.tasks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.opendatakit.common.android.data.Preferences;
import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.common.android.utilities.CsvUtil;
import org.opendatakit.common.android.utilities.CsvUtil.ImportListener;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.fragments.InitializeTaskDialogFragment;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

public class InitializeTask extends AsyncTask<Void, String, Boolean> implements ImportListener {

  private static final String EMPTY_STRING = "";

  private static final String SPACE = " ";

  private static final String TOP_LEVEL_KEY_TABLE_KEYS = "table_keys";

  private static final String COMMA = ",";

  private static final String KEY_SUFFIX_CSV_FILENAME = ".filename";

  private static final String TAG = "InitializeTask";

  private InitializeTaskDialogFragment mDialogFragment;
  private final Context mContext;
  private final String mAppName;
  private String filename;
  private Map<String, Boolean> importStatus;
  private Set<String> mFileNotFoundSet = new HashSet<String>();
  /** Stores the table's key to its filename. */
  private Map<String, String> mKeyToFileMap;

  private boolean mPendingSuccess = false;

  public boolean caughtDuplicateTableException = false;
  public boolean problemImportingKVSEntries = false;
  private boolean poorlyFormatedConfigFile = false;

  public InitializeTask(Context context, String appName) {
    this.mContext = context;
    this.mAppName = appName;
    this.importStatus = new HashMap<String, Boolean>();
    this.mKeyToFileMap = new HashMap<String, String>();
  }

  public void setDialogFragment(InitializeTaskDialogFragment dialogFragment) {
    this.mDialogFragment = dialogFragment;
  }

  @Override
  protected synchronized Boolean doInBackground(Void... params) {

    // Verify that the APK version of the frameworks files matches that of this
    // APK
    mPendingSuccess = true;

    String message = null;
    ArrayList<String> result = new ArrayList<String>();

    if (!ODKFileUtils.isConfiguredTablesApp(mAppName, Tables.getInstance().getVersionCodeString())) {
      publishProgress(mContext.getString(R.string.expansion_unzipping_begins), null);

      extractFromRawZip(R.raw.frameworkzip, true, result);
      extractFromRawZip(R.raw.assetszip, false, result);

      ODKFileUtils.assertConfiguredTablesApp(mAppName, Tables.getInstance().getVersionCodeString());
    }

    // /////////////////////////////////////////
    // /////////////////////////////////////////
    // /////////////////////////////////////////
    // Scan the tables directory, looking for tableIds with definition.csv
    // files.
    // If the tableId does not exist, try to create it using these files.
    // If the tableId already exists, do nothing -- assume everything is
    // up-to-date.
    // This means we don't pick up properties.csv changes, but the
    // definition.csv
    // should never change. If properties.csv changes, we assume the process
    // that
    // changed it will be triggering a reload of it through other means.

    CsvUtil util = new CsvUtil(mContext, mAppName);
    File tablesDir = new File(ODKFileUtils.getTablesFolder(mAppName));
    File[] tableIdDirs = tablesDir.listFiles(new FileFilter() {

      @Override
      public boolean accept(File pathname) {
        return pathname.isDirectory();
      }
    });

    List<String> tableIds;
    ODKFileUtils.assertDirectoryStructure(mAppName);
    SQLiteDatabase db = null;
    try {
      db = DatabaseFactory.get().getDatabase(mContext, mAppName);
      tableIds = ODKDatabaseUtils.get().getAllTableIds(db);
    } finally {
      if (db != null) {
        db.close();
      }
    }

    for (int i = 0; i < tableIdDirs.length; ++i) {
      File tableIdDir = tableIdDirs[i];
      String tableId = tableIdDir.getName();
      if (tableIds.contains(tableId)) {
        // assume it is up-to-date
        continue;
      }

      File definitionCsv = new File(ODKFileUtils.getTableDefinitionCsvFile(mAppName, tableId));
      File propertiesCsv = new File(ODKFileUtils.getTablePropertiesCsvFile(mAppName, tableId));
      if (definitionCsv.exists() && definitionCsv.isFile() && propertiesCsv.exists()
          && propertiesCsv.isFile()) {

        String formattedString = mContext.getString(R.string.scanning_for_table_definitions,
            tableId, (i + 1), tableIdDirs.length);
        String detail = mContext.getString(R.string.processing_file);
        publishProgress(formattedString, detail);

        try {
          util.updateTablePropertiesFromCsv(this, tableId);
        } catch (IOException e) {
          WebLogger.getLogger(mAppName).e(TAG, "Unexpected error during update from csv");
        }
      }
    }

    // /////////////////////////////////////////
    // /////////////////////////////////////////
    // /////////////////////////////////////////
    // and now process tables.init file
    File init = new File(ODKFileUtils.getTablesInitializationFile(mAppName));
    File completedFile = new File(ODKFileUtils.getTablesInitializationCompleteMarkerFile(mAppName));
    if (!init.exists()) {
      // no initialization file -- we are done!
      return true;
    }
    boolean processFile = false;
    if (!completedFile.exists()) {
      processFile = true;
    } else {
      String initMd5 = ODKFileUtils.getMd5Hash(mAppName, init);
      String completedFileMd5 = ODKFileUtils.getMd5Hash(mAppName, completedFile);
      processFile = !initMd5.equals(completedFileMd5);
    }
    if (!processFile) {
      // we are done!
      return true;
    }

    Properties prop = new Properties();
    try {
      prop.load(new FileInputStream(init));
    } catch (IOException ex) {
      WebLogger.getLogger(mAppName).printStackTrace(ex);
      return false;
    }

    // assume if we load it, we have processed it.

    // We shouldn't really do this, but it avoids an infinite
    // recycle if there is an error during the processing of the
    // file.
    try {
      FileUtils.copyFile(init, completedFile);
    } catch (IOException e) {
      WebLogger.getLogger(mAppName).printStackTrace(e);
      // ignore this.
    }

    // prop was loaded
    if (prop != null) {
      String table_keys = prop.getProperty(TOP_LEVEL_KEY_TABLE_KEYS);

      // table_keys is defined
      if (table_keys != null) {
        // remove spaces and split at commas to get key names
        String[] keys = table_keys.replace(SPACE, EMPTY_STRING).split(COMMA);
        int fileCount = keys.length;
        int curFileCount = 0;
        String detail = mContext.getString(R.string.processing_file);

        File file;
        CsvUtil cu = new CsvUtil(this.mContext, this.mAppName);
        for (String key : keys) {
          curFileCount++;
          filename = prop.getProperty(key + KEY_SUFFIX_CSV_FILENAME);
          this.importStatus.put(key, false);
          file = new File(ODKFileUtils.getAppFolder(mAppName), filename);
          this.mKeyToFileMap.put(key, filename);
          if (!file.exists()) {
            mFileNotFoundSet.add(key);
            WebLogger.getLogger(mAppName).i(TAG, "putting in file not found map true: " + key);
            continue;
          }

          // update dialog message with current filename
          publishProgress(mContext.getString(R.string.importing_file_without_detail, curFileCount,
              fileCount, filename), detail);
          ImportRequest request = null;

          // If the import file is in the assets/csv directory
          // and if it is of the form tableId.csv or tableId.fileQualifier.csv
          // and fileQualifier is not 'properties', then assume it is the
          // new-style CSV format.
          //
          String[] pathParts = filename.split("/");
          if ((pathParts.length == 3) && pathParts[0].equals("assets")
              && pathParts[1].equals("csv")) {
            String[] terms = pathParts[2].split("\\.");
            if (terms.length == 2 && terms[1].equals("csv")) {
              String tableId = terms[0];
              String fileQualifier = null;
              request = new ImportRequest(tableId, fileQualifier);
            } else if (terms.length == 3 && terms[1].equals("properties") && terms[2].equals("csv")) {
              String tableId = terms[0];
              String fileQualifier = null;
              request = new ImportRequest(tableId, fileQualifier);
            } else if (terms.length == 3 && terms[2].equals("csv")) {
              String tableId = terms[0];
              String fileQualifier = terms[1];
              request = new ImportRequest(tableId, fileQualifier);
            } else if (terms.length == 4 && terms[2].equals("properties") && terms[3].equals("csv")) {
              String tableId = terms[0];
              String fileQualifier = terms[1];
              request = new ImportRequest(tableId, fileQualifier);
            }

            if (request != null) {
              boolean success = false;
              success = cu.importSeparable(this, request.getTableId(), request.getFileQualifier(),
                  true);
              importStatus.put(key, success);
              if (success) {
                detail = mContext.getString(R.string.import_success);
                publishProgress(mContext.getString(R.string.importing_file_without_detail,
                    curFileCount, fileCount, filename), detail);
              }
            }
          }

          if (request == null) {
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

  private void extractFromRawZip(int resourceId, boolean overwrite, ArrayList<String> result) {
    String message = null;
    AssetFileDescriptor fd = null;
    try {
      fd = mContext.getResources().openRawResourceFd(resourceId);
      final long size = fd.getLength() / 2L; // apparently over-counts by 2x?
      InputStream rawInputStream = null;
      try {
        rawInputStream = fd.createInputStream();
        ZipInputStream zipInputStream = null;
        ZipEntry entry = null;
        try {

          // count the number of files in the zip
          zipInputStream = new ZipInputStream(rawInputStream);
          int totalFiles = 0;
          while ((entry = zipInputStream.getNextEntry()) != null) {
            message = null;
            if (isCancelled()) {
              message = "cancelled";
              result.add(entry.getName() + " " + message);
              break;
            }
            ++totalFiles;
          }
          zipInputStream.close();

          // and re-open the stream, reading it this time...
          fd = mContext.getResources().openRawResourceFd(resourceId);
          rawInputStream = fd.createInputStream();
          zipInputStream = new ZipInputStream(rawInputStream);

          long bytesProcessed = 0L;
          long lastBytesProcessedThousands = 0L;
          int nFiles = 0;
          while ((entry = zipInputStream.getNextEntry()) != null) {
            message = null;
            if (isCancelled()) {
              message = "cancelled";
              result.add(entry.getName() + " " + message);
              break;
            }
            ++nFiles;
            File tempFile = new File(ODKFileUtils.getAppFolder(mAppName), entry.getName());
            String formattedString = mContext.getString(
                R.string.expansion_unzipping_without_detail, entry.getName(), nFiles, totalFiles);
            String detail;
            if (entry.isDirectory()) {
              detail = mContext.getString(R.string.expansion_create_dir_detail);
              publishProgress(formattedString, detail);
              tempFile.mkdirs();
            } else {
              int bufferSize = 8192;
              OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile, false),
                  bufferSize);
              byte buffer[] = new byte[bufferSize];
              int bread;
              while ((bread = zipInputStream.read(buffer)) != -1) {
                bytesProcessed += bread;
                long curThousands = (bytesProcessed / 1000L);
                if (curThousands != lastBytesProcessedThousands) {
                  detail = mContext.getString(R.string.expansion_unzipping_detail, bytesProcessed,
                      size);
                  publishProgress(formattedString, detail);
                  lastBytesProcessedThousands = curThousands;
                }
                out.write(buffer, 0, bread);
              }
              out.flush();
              out.close();

              detail = mContext
                  .getString(R.string.expansion_unzipping_detail, bytesProcessed, size);
              publishProgress(formattedString, detail);
            }
            WebLogger.getLogger(mAppName).i(TAG, "Extracted ZipEntry: " + entry.getName());

            message = mContext.getString(R.string.success);
            result.add(entry.getName() + " " + message);
          }

          ODKFileUtils.assertConfiguredTablesApp(mAppName, 
              Tables.getInstance().getVersionCodeString());

          String completionString = mContext.getString(R.string.expansion_unzipping_complete,
              totalFiles);
          publishProgress(completionString, null);
        } catch (IOException e) {
          WebLogger.getLogger(mAppName).printStackTrace(e);
          mPendingSuccess = false;
          if (e.getCause() != null) {
            message = e.getCause().getMessage();
          } else {
            message = e.getMessage();
          }
          if (entry != null) {
            result.add(entry.getName() + " " + message);
          } else {
            result.add("Error accessing zipfile resource " + message);
          }
        } finally {
          if (zipInputStream != null) {
            try {
              zipInputStream.close();
              rawInputStream = null;
              fd = null;
            } catch (IOException e) {
              WebLogger.getLogger(mAppName).printStackTrace(e);
              WebLogger.getLogger(mAppName).e(TAG, "Closing of ZipFile failed: " + e.toString());
            }
          }
          if (rawInputStream != null) {
            try {
              rawInputStream.close();
              fd = null;
            } catch (IOException e) {
              WebLogger.getLogger(mAppName).printStackTrace(e);
              WebLogger.getLogger(mAppName).e(TAG, "Closing of ZipFile failed: " + e.toString());
            }
          }
          if (fd != null) {
            try {
              fd.close();
            } catch (IOException e) {
              WebLogger.getLogger(mAppName).printStackTrace(e);
              WebLogger.getLogger(mAppName).e(TAG, "Closing of ZipFile failed: " + e.toString());
            }
          }
        }
      } catch (Exception e) {
        WebLogger.getLogger(mAppName).printStackTrace(e);
        mPendingSuccess = false;
        if (e.getCause() != null) {
          message = e.getCause().getMessage();
        } else {
          message = e.getMessage();
        }
        result.add("Error accessing zipfile resource " + message);
      } finally {
        if (rawInputStream != null) {
          try {
            rawInputStream.close();
          } catch (IOException e) {
            WebLogger.getLogger(mAppName).printStackTrace(e);
          }
        }
      }
    } finally {
      if (fd != null) {
        try {
          fd.close();
        } catch (IOException e) {
          WebLogger.getLogger(mAppName).printStackTrace(e);
        }
      } else {
        result.add("Error accessing zipfile resource.");
      }
    }
  }

  private String displayOverall;
  private String displayDetail;

  @Override
  protected void onProgressUpdate(String... values) {
    this.displayOverall = values[0];
    this.displayDetail = values[1];

    if (mDialogFragment != null) {
      mDialogFragment.updateProgress(displayOverall
          + ((displayDetail != null) ? "\n(" + displayDetail + ")" : ""));
    } else {
      WebLogger.getLogger(mAppName).e(TAG, "dialog fragment is null! Not updating progress.");
    }
  }

  @Override
  public void updateProgressDetail(String displayDetail) {
    publishProgress(displayOverall, displayDetail);
  }

  @Override
  public void importComplete(boolean outcome) {
    problemImportingKVSEntries = problemImportingKVSEntries || !outcome;
  }

  // dismiss ProgressDialog and create an AlertDialog with one
  // button to confirm that the user read the postExecute message
  @Override
  protected void onPostExecute(Boolean result) {
    // refresh TableManager to show newly imported tables
    if (this.mDialogFragment == null) {
      WebLogger.getLogger(mAppName).e(TAG,
          "dialog fragment is null! Task can't report back. " + "Returning.");
      return;
    }
    // From this point forward we'll assume that the dialog fragment is not
    // null.

    if (!result) {
      this.mDialogFragment.onTaskFinishedWithErrors(poorlyFormatedConfigFile);
    } else {
      // Build summary message
      StringBuffer msg = new StringBuffer();
      for (String key : mKeyToFileMap.keySet()) {
        WebLogger.getLogger(mAppName).e(TAG, "key: " + key);
        if (importStatus.get(key)) {
          String nameOfFile = mKeyToFileMap.get(key);
          WebLogger.getLogger(mAppName).e(TAG, "import status from map: " + importStatus.get(key));
          msg.append(mContext.getString(R.string.imported_successfully, nameOfFile));
        } else {
          // maybe there was an existing table already, maybe there were
          // just errors.
          if (mFileNotFoundSet.contains(key)) {
            // We'll first retrieve the file to which this key was pointing.
            String nameOfFile = mKeyToFileMap.get(key);
            WebLogger.getLogger(mAppName).e(TAG, "file wasn't found: " + key);
            msg.append(mContext.getString(R.string.file_not_found, nameOfFile));
          } else {
            // a general error.
            WebLogger.getLogger(mAppName).e(TAG, "table already exists map was false");
            msg.append(mContext.getString(R.string.imported_with_errors, key));
          }
        }

      }
      this.mDialogFragment.onTaskFinishedSuccessfully(msg.toString());
    }
  }

  public interface Callbacks {

    /**
     * Get an {@link Preferences} object for handling information about the time
     * of the last import from a config file, etc.
     *
     * @return
     */
    public Preferences getPrefs();

    /**
     * Update the display to the user after the import is complete.
     */
    public void onImportsComplete();

  }
}
