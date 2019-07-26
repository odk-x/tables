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

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.opendatakit.consts.IntentConsts;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.properties.CommonToolProperties;
import org.opendatakit.properties.PropertiesSingleton;
import org.opendatakit.tables.R;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.fragments.ImportExportDialogFragment;
import org.opendatakit.tables.tasks.ExportRequest;
import org.opendatakit.tables.tasks.ExportTask;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.utilities.ODKFileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for exporting a table to CSV from the phone.
 * <p>
 * There appear to me to be two possible reasons for doing this. The first is to
 * o present the data to a user or admin so that they can view it and use it in
 * a spreadsheet application.
 * <p>
 * The second is to create a CSV that Tables can import to create the table as
 * it existed on the phone at the time of the export.
 * <p>
 * There are several options available to the user, such as including the user
 * who modified it, and including the timestamp. An additional option is to
 * include the table settings. This checkbox includes the table settings as a
 * metadata string, and it also includes all the metadata columns of each row
 * for which the saved status is complete. In other words, it exports the table
 * that the user is able to see (the saved == complete rows) and all the
 * metadata for those rows at the time of the export.
 *
 * @author unknown
 * @author sudar.sam@gmail.com
 */
public class ExportCSVActivity extends AbsBaseActivity {

  // the app name
  private String appName;
  // the list of table names
  private String[] tableNames;
  // the list of table Ids
  private String[] tableIds;
  // the table name spinner
  private Spinner tableSpin;
  // the text field where the user enters the qualifier
  private EditText qualifierTextBox;

  /**
   * Called when the user navigates to this screen. Sets the app name and sets up the view
   *
   * @param savedInstanceState the state from before the app was suspended. Largely unused
   */
  public void onCreate(Bundle savedInstanceState) {
    // We have to set the fragment manager here because if we don't, then the ImportTask will try
    // to display an alert dialog by grabbing the fragment manager
    ImportExportDialogFragment.fragman = getSupportFragmentManager();
    super.onCreate(savedInstanceState);
    appName = getIntent().getStringExtra(IntentConsts.INTENT_KEY_APP_NAME);
    if (appName == null) {
      appName = TableFileUtils.getDefaultAppName();
    }
    setContentView(getView());
  }

  /**
   * Standard getter for the app name
   *
   * @return the app name
   */
  @Override
  public String getAppName() {
    return appName;
  }

  /**
   * Private helper method to return the view with all the strings set up, onclick handlers set,
   * etc..
   *
   * @return the view
   */
  private View getView() {
    LinearLayout v = new LinearLayout(this);
    v.setOrientation(LinearLayout.VERTICAL);
    // selecting table
    TextView est = new TextView(this);
    est.setText(getString(R.string.export_csv));
    v.addView(est);
    // adding the table spinner
    tableSpin = new Spinner(this);
    tableSpin.setId(R.id.TABLESPIN_ID);
    v.addView(tableSpin);
    // Horizontal divider
    View ruler1 = new View(this);
    ruler1.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
    v.addView(ruler1, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
    // adding the filename field
    TextView fnLabel = new TextView(this);
    fnLabel.setText(getString(R.string.export_file_qualifier));
    v.addView(fnLabel);
    qualifierTextBox = new EditText(this);
    qualifierTextBox.setId(R.id.FILENAMEVAL_ID);
    v.addView(qualifierTextBox);
    // Horizontal divider
    View ruler3 = new View(this);
    ruler3.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.black));
    v.addView(ruler3, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
    // adding the export button
    TextView button = new Button(this);
    button.setId(R.id.EXPORTBUTTON_ID);
    button.setText(getString(R.string.export_button));
    button.setOnClickListener(new ExportButtonListener());
    v.addView(button);
    // wrapping in a scroll view
    ViewGroup scroll = new ScrollView(this);
    scroll.addView(v);
    return scroll;
  }

  /**
   * Attempts to export a table.
   */
  private void exportSubmission() {
    if (tableSpin.getSelectedItemPosition() == Spinner.INVALID_POSITION) {
      Toast
          .makeText(this, R.string.export_no_table, Toast.LENGTH_LONG)
          .show();

      return;
    }

    String tableId = tableIds[tableSpin.getSelectedItemPosition()];
    ImportExportDialogFragment
        .newInstance(ImportExportDialogFragment.EXPORT_IN_PROGRESS_DIALOG, this);
    AsyncTask<ExportRequest, Integer, Boolean> task = new ExportTask(appName, this);
    task.execute(new ExportRequest(appName, tableId, qualifierTextBox.getText().toString().trim()));
  }

  /**
   * Called when the user selects which table they want to export. Sets qualifierTextBox to the
   * filename for the table that needs to be exported
   *
   * @param requestCode unused because there's only one activity
   * @param resultCode  whether the user canceled selecting a table or not
   * @param data        the table that the user selected
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_CANCELED) {
      return;
    }
    Uri fileUri = data.getData();
    File filepath = new File(fileUri.getPath());
    String relativePath = ODKFileUtils.asRelativePath(appName, filepath);
    qualifierTextBox.setText(relativePath);
  }

  /**
   * Called when the database becomes available. Sets the tableIds and tableNames
   */
  @Override
  public void databaseAvailable() {
    super.databaseAvailable();

    UserDbInterface dbInterface = Tables.getInstance().getDatabase();
    if (dbInterface != null) {
      PropertiesSingleton props = CommonToolProperties.get(getApplication(), appName);
      String userSelectedDefaultLocale = props.getUserSelectedDefaultLocale();
      DbHandle db = null;
      try {
        ArrayList<String> localizedNames = new ArrayList<>();
        db = dbInterface.openDatabase(appName);
        List<String> rawTableIds = dbInterface.getAllTableIds(appName, db);
        for (String tableId : rawTableIds) {
          String localizedDisplayName;
          localizedDisplayName = TableUtil.get()
              .getLocalizedDisplayName(userSelectedDefaultLocale, dbInterface, appName, db,
                  tableId);
          localizedNames.add(localizedDisplayName);
        }
        tableIds = rawTableIds.toArray(new String[rawTableIds.size()]);
        tableNames = localizedNames.toArray(new String[localizedNames.size()]);
      } catch (ServicesAvailabilityException e) {
        WebLogger.getLogger(appName).printStackTrace(e);
      } finally {
        if (db != null) {
          try {
            dbInterface.closeDatabase(appName, db);
          } catch (ServicesAvailabilityException e) {
            WebLogger.getLogger(appName).printStackTrace(e);
          }
        }
      }
      ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
          tableNames);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
      tableSpin.setAdapter(adapter);
      tableSpin.setSelection(0);
    }
  }

  /**
   * Called when the database goes away.
   */
  @Override
  public void databaseUnavailable() {
    super.databaseUnavailable();
  }

  /**
   * Used in the view, passed to the export button
   */
  private class ExportButtonListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      exportSubmission();
    }
  }

}
