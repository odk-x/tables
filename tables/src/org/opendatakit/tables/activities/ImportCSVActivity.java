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

import java.io.File;

import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.tasks.ImportRequest;
import org.opendatakit.tables.tasks.ImportTask;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.TableFileUtils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An activity for importing CSV files to a table.
 */
public class ImportCSVActivity extends AbstractImportExportActivity {

  private static final String TAG = ImportCSVActivity.class.getSimpleName();

  /** view IDs (for use in testing) */
  public static final int FILENAMEVAL_ID = 3;
  public static final int IMPORTBUTTON_ID = 4;

  /* the appName context within which we are running */
  private String appName;
  /* the text field for getting the filename */
  private EditText filenameValField;
  /* the button for selecting a file */
  private Button pickFileButton;
  /** The button to import a table. */
  private Button mImportButton;

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    appName = getIntent().getStringExtra(Constants.IntentKeys.APP_NAME);
    if (appName == null) {
      appName = TableFileUtils.getDefaultAppName();
    }
    setContentView(getView());
  }
  
  @Override
  public String getAppName() {
    return appName;
  }

  /**
   * @return the view
   */
  private View getView() {
    LinearLayout v = new LinearLayout(this);
    v.setOrientation(LinearLayout.VERTICAL);
    // adding the filename field
    LinearLayout fn = new LinearLayout(this);
    fn.setOrientation(LinearLayout.VERTICAL);
    TextView fnLabel = new TextView(this);
    fnLabel.setText(getString(R.string.import_csv_filename));
    fnLabel.setTextColor(getResources().getColor(R.color.black));
    fn.addView(fnLabel);
    filenameValField = new EditText(this);
    filenameValField.setId(FILENAMEVAL_ID);
    fn.addView(filenameValField);
    v.addView(fn);
    pickFileButton = new Button(this);
    pickFileButton.setText(getString(R.string.import_choose_csv_file));
    pickFileButton.setOnClickListener(new PickFileButtonListener(this.appName,
        getString(R.string.import_choose_csv_file)));
    v.addView(pickFileButton);
    // Horizontal divider
    View ruler1 = new View(this);
    ruler1.setBackgroundColor(getResources().getColor(R.color.black));
    v.addView(ruler1, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
    // adding the import button
    this.mImportButton = new Button(this);
    this.mImportButton.setId(IMPORTBUTTON_ID);
    this.mImportButton.setText(getString(R.string.import_append_table));
    this.mImportButton.setOnClickListener(new ImportButtonListener());
    v.addView(this.mImportButton);
    this.mImportButton.setEnabled(Tables.getInstance().getDatabase() != null);
    // wrapping in a scroll view
    ScrollView scroll = new ScrollView(this);
    scroll.addView(v);
    return scroll;
  }

  /**
   * Attempts to import a CSV file.
   */
  private void importSubmission() {

    String filenamePath = filenameValField.getText().toString().trim();

    ImportRequest request = null;
    String[] pathParts = filenamePath.split("/");
    if ((pathParts.length == 3) && pathParts[0].equals("assets") && pathParts[1].equals("csv")) {
      String[] terms = pathParts[2].split("\\.");
      if (terms.length == 2 && terms[1].equals("csv")) {
        String tableId = terms[0];
        String fileQualifier = null;
        request = new ImportRequest(tableId, fileQualifier);
      } else if (terms.length == 3
          && (terms[1].equals("properties") || terms[1].equals("definition"))
          && terms[2].equals("csv")) {
        String tableId = terms[0];
        String fileQualifier = null;
        request = new ImportRequest(tableId, fileQualifier);
      } else if (terms.length == 3 && terms[2].equals("csv")) {
        String tableId = terms[0];
        String fileQualifier = terms[1];
        request = new ImportRequest(tableId, fileQualifier);
      } else if (terms.length == 4
          && (terms[2].equals("properties") || terms[2].equals("definition"))
          && terms[3].equals("csv")) {
        String tableId = terms[0];
        String fileQualifier = terms[1];
        request = new ImportRequest(tableId, fileQualifier);
      }
    }

    if (request == null) {
      Toast
          .makeText(this, "Invalid csv filename: " + filenameValField.getText(), Toast.LENGTH_LONG)
          .show();
      return;
    }

    showDialog(IMPORT_IN_PROGRESS_DIALOG);
    ImportTask task = new ImportTask(this, appName);
    task.execute(request);
  }

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
    String relativePath = ODKFileUtils.asRelativePath(appName, csvFile);
    WebLogger.getLogger(appName).d(TAG, "relative path of import file: " + relativePath);
    File assetCsv = new File(ODKFileUtils.getAssetsCsvFolder(appName));
    String assetRelativePath = ODKFileUtils.asRelativePath(appName, assetCsv);
    if (relativePath.startsWith(assetRelativePath)) {
      String name = csvFile.getName();
      String[] terms = name.split("\\.");
      if (terms.length < 2 || terms.length > 4) {
        Toast
            .makeText(
                this,
                "Import filename must be of the form tableId.csv, tableId.definition.csv, tableId.properties.csv or tableId.qualifier.csv",
                Toast.LENGTH_LONG).show();
        return;
      } else {
        if (!terms[terms.length - 1].equals("csv")) {
          Toast.makeText(this, "Import filename must end in .csv", Toast.LENGTH_LONG).show();
          return;
        }
        if (terms.length == 4 && !(terms[2].equals("properties") || terms[2].equals("definition"))) {
          Toast
              .makeText(
                  this,
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
   * A listener for the import button. Calls importSubmission() on click.
   */
  private class ImportButtonListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      importSubmission();
    }
  }

  @Override
  public void onPostResume() {
    super.onPostResume();
    Tables.getInstance().establishDatabaseConnectionListener(this);
  }

  @Override
  public void databaseAvailable() {
    this.mImportButton.setEnabled(Tables.getInstance().getDatabase() != null);
  }

  @Override
  public void databaseUnavailable() {
    this.mImportButton.setEnabled(Tables.getInstance().getDatabase() != null);
  }

}
