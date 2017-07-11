/*
 * Copyright (C) 2012 University of Washington
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
package org.opendatakit.tables.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import org.opendatakit.consts.IntentConsts;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.fragments.ImportExportDialogFragment;
import org.opendatakit.tables.tasks.ImportRequest;
import org.opendatakit.tables.tasks.ImportTask;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.utilities.ODKFileUtils;

import java.io.File;

/**
 * An activity for importing CSV files to a table.
 */
public class ImportCSVActivity extends AbsBaseActivity {
  // Used for logging
  private static final String TAG = ImportCSVActivity.class.getSimpleName();

  // the appName context within which we are running
  private String appName;
  // the text field for getting the filename
  private EditText filenameValField;
  // The button to import a table.
  private Button mImportButton;

  /**
   * Sets the app name and sets the view (what clicking the buttons should do, etc..)
   *
   * @param savedInstanceState a bundle containing the state if it was suspended, used only by
   *                           this classes parents
   */
  public void onCreate(Bundle savedInstanceState) {
    ImportExportDialogFragment.fragman = getFragmentManager();
    super.onCreate(savedInstanceState);
    appName = getIntent().getStringExtra(IntentConsts.INTENT_KEY_APP_NAME);
    if (appName == null) {
      appName = TableFileUtils.getDefaultAppName();
    }
    setContentView(getView());
  }

  /**
   * standard getter for the app name
   *
   * @return the app name
   */
  @Override
  public String getAppName() {
    return appName;
  }

  /**
   * @return the view, with all the scrollbars, buttons, EditTexts and stuff
   */
  private View getView() {
    LinearLayout v = new LinearLayout(this);
    v.setOrientation(LinearLayout.VERTICAL);
    // adding the filename field
    LinearLayout fn = new LinearLayout(this);
    fn.setOrientation(LinearLayout.VERTICAL);
    TextView fnLabel = new TextView(this);
    fnLabel.setText(getString(R.string.import_csv_filename));
    fnLabel.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
    fn.addView(fnLabel);
    filenameValField = new EditText(this);
    filenameValField.setId(R.id.FILENAMEVAL_ID);
    fn.addView(filenameValField);
    v.addView(fn);
    // The button for selecting a file
    TextView pickFileButton = new Button(this);
    pickFileButton.setText(getString(R.string.import_choose_csv_file));
    pickFileButton.setOnClickListener(
        new PickFileButtonListener(this.appName, getString(R.string.import_choose_csv_file)));
    v.addView(pickFileButton);
    // Horizontal divider
    View ruler1 = new View(this);
    ruler1.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
    v.addView(ruler1, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
    // adding the import button
    this.mImportButton = new Button(this);
    this.mImportButton.setId(R.id.IMPORTBUTTON_ID);
    this.mImportButton.setText(getString(R.string.import_append_table));
    this.mImportButton.setOnClickListener(new ImportButtonListener());
    v.addView(this.mImportButton);
    this.mImportButton.setEnabled(getDatabase() != null);
    // wrapping in a scroll view
    ViewGroup scroll = new ScrollView(this);
    scroll.addView(v);
    return scroll;
  }

  /**
   * Attempts to import a CSV file based on the filename in filenameValField.
   * First, we get a path that's relative to the assets csv folder. If the filename starts with the
   * assets csv folder, it's stripped off. So (assets csv folder)/test.csv becomes test.csv, but
   * ../../test.csv stays the same.
   * Then we split it by \. and try to parse the tableId and fileQualifier out of the filename.
   * If it had too many dots (or not enough), we display a Toast notification that the filename
   * was invalid and return.
   * Then we make an ImportTask and pass it a new ImportExportDialogFragment. The ImportTask will tell
   * the ImportExportDialogFragment to update the dialog text to "Importing row 5" and so on, and it will
   * also handle closing the dialog and displaying a Completed or Failed dialog.
   */
  private void importSubmission() {

    String filenamePath = filenameValField.getText().toString().trim();

    ImportRequest request = null;
    String assetsCsvRelativePath = ODKFileUtils
        .asRelativePath(appName, new File(ODKFileUtils.getAssetsCsvFolder(appName)));
    if (filenamePath.startsWith(assetsCsvRelativePath)) {
      String remainingPath = filenamePath.substring(assetsCsvRelativePath.length() + 1);
      String[] terms = remainingPath.split("\\.");
      String tableId = null;
      String fileQualifier = null;
      if (terms.length == 2 && "csv".equals(terms[1]) || terms.length == 3 && (
          "properties".equals(terms[1]) || "definition".equals(terms[1])) && "csv"
          .equals(terms[2])) {
        tableId = terms[0];
        fileQualifier = null;
      } else if (terms.length == 3 && "csv".equals(terms[2]) || terms.length == 4 && (
          "properties".equals(terms[2]) || "definition".equals(terms[2])) && "csv"
          .equals(terms[3])) {
        tableId = terms[0];
        fileQualifier = terms[1];
      }
      if (tableId != null && fileQualifier != null) {
        request = new ImportRequest(tableId, fileQualifier);
      }
    }

    if (request == null) {
      Toast.makeText(this, "Invalid csv filename: " + filenameValField.getText(), Toast.LENGTH_LONG)
          .show();
      return;
    }

    ImportExportDialogFragment
        .newInstance(ImportExportDialogFragment.IMPORT_IN_PROGRESS_DIALOG, this);
    AsyncTask<ImportRequest, Integer, Boolean> task = new ImportTask(appName, this);
    task.execute(request);
  }

  /**
   * Despite what the name implies, it isn't called when the ImportTask completes, it's called
   * after the user selects a file from the file picker. It validates the filename, determines
   * the relative path to the filename and puts that path in the filenameValField. If the
   * filename was invalid, it displays a Toast notification and returns
   *
   * @param requestCode unused because there's only one file picker/"activity"
   * @param resultCode  whether the user canceled the file picker or not
   * @param data        the path to the file the user selected
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_CANCELED) {
      return;
    }
    Uri fileUri = data.getData();
    String filepath = fileUri.getPath();
    File csvFile = new File(filepath);
    // We have to first hand this off to account for the difference in
    // external storage directories on different versions of android.
    String relativePath;
    try {
      relativePath = ODKFileUtils.asRelativePath(appName, csvFile);
    } catch (IllegalArgumentException iae) {
      WebLogger.getLogger(getAppName()).printStackTrace(iae);
      Toast.makeText(this,
          getString(R.string.file_not_under_app_dir, ODKFileUtils.getAppFolder(getAppName())),
          Toast.LENGTH_LONG).show();
      return;

    }
    WebLogger.getLogger(appName).d(TAG, "relative path of import file: " + relativePath);
    File assetCsv = new File(ODKFileUtils.getAssetsCsvFolder(appName));
    String assetRelativePath = ODKFileUtils.asRelativePath(appName, assetCsv);
    if (relativePath.startsWith(assetRelativePath)) {
      String name = csvFile.getName();
      String[] terms = name.split("\\.");
      if (terms.length < 2 || terms.length > 4) {
        Toast.makeText(this,
            "Import filename must be of the form tableId.csv, tableId.definition.csv, tableId.properties.csv or tableId.qualifier.csv",
            Toast.LENGTH_LONG).show();
        return;
      } else {
        if (!"csv".equals(terms[terms.length - 1])) {
          Toast.makeText(this, "Import filename must end in .csv", Toast.LENGTH_LONG).show();
          return;
        }
        if (terms.length == 4 && !("properties".equals(terms[2]) || "definition"
            .equals(terms[2]))) {
          Toast.makeText(this,
              "Import filename must be of the form tableId.qualifier.properties.csv or tableId.qualifier.definition.csv",
              Toast.LENGTH_LONG).show();
          return;
        }
      }
      filenameValField.setText(relativePath);
    } else {
      Toast.makeText(this, "Import file must reside in " + assetRelativePath + File.separator,
          Toast.LENGTH_LONG).show();
    }
  }

  /**
   * enables the import button if the database is available
   */
  @Override
  public void databaseAvailable() {
    super.databaseAvailable();
    this.mImportButton.setEnabled(getDatabase() != null);
  }

  /**
   * disables the import button if the database is not available
   */
  @Override
  public void databaseUnavailable() {
    super.databaseUnavailable();
    this.mImportButton.setEnabled(getDatabase() != null);
  }

  /**
   * This class is used in getView, used for the onClick listener for the file picker
   */
  private class PickFileButtonListener implements OnClickListener {
    private String appName;
    private String title;

    PickFileButtonListener(String appName, String title) {
      this.appName = appName;
      this.title = title;
    }

    /**
     * When the user clicks a file, try and open it. If it didn't exist, log the error and show a
     * toast message about it
     *
     * @param v the view that contains the file picker that was clicked on. Unused
     */
    @Override
    public void onClick(View v) {
      Intent intent = new Intent("org.openintents.action.PICK_FILE");
      intent.setData(Uri.parse("file://" + ODKFileUtils.getAssetsCsvFolder(appName)));
      intent.putExtra("org.openintents.extra.TITLE", title);
      try {
        startActivityForResult(intent, 1);
      } catch (ActivityNotFoundException e) {
        WebLogger.getLogger(getAppName()).printStackTrace(e);
        Toast.makeText(ImportCSVActivity.this, getString(R.string.file_picker_not_found),
            Toast.LENGTH_LONG).show();
      }
    }
  }

  /**
   * A listener for the import button. Calls importSubmission() on click.
   */
  private class ImportButtonListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      importSubmission();
    }
  }

}
