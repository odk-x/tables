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

import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.tasks.ExportRequest;
import org.opendatakit.tables.tasks.ExportTask;
import org.opendatakit.tables.utils.TableFileUtils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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


/**
 * This class is responsible for exporting a table to CSV from the phone.
 * <p>
 * There appear to me to be two possible reasons for doing this. The first is
 * to o present the
 * data to a user or admin so that they can view it and use it in a
 * spreadsheet application.
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
 *
 * @author unknown
 * @author sudar.sam@gmail.com
 *
 */
public class ExportCSVActivity extends AbstractImportExportActivity {

	/** view IDs (for use in testing) */
	public static final int TABLESPIN_ID = 1;
	public static final int FILENAMEVAL_ID = 2;
	public static final int EXPORTBUTTON_ID = 3;

	private String appName;
	/* the list of table names */
	private String[] tableNames;
	/* the list of TableProperties */
	private TableProperties[] tps;
	/* the table name spinner */
	private Spinner tableSpin;
	/* the text field for getting the filename */
	private EditText filenameValField;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		appName = getIntent().getStringExtra(Controller.INTENT_KEY_APP_NAME);
		if ( appName == null ) {
		  appName = TableFileUtils.getDefaultAppName();
		}
		setContentView(getView());
	}

	/**
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
		tableSpin.setId(TABLESPIN_ID);
		tps = TableProperties.getTablePropertiesForAll(this, appName);
		tableNames = new String[tps.length];
		for (int i = 0; i < tps.length; i++) {
		    tableNames[i] = tps[i].getLocalizedDisplayName();
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, tableNames);
		adapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);
		tableSpin.setAdapter(adapter);
		tableSpin.setSelection(0);
		v.addView(tableSpin);
		// Horizontal divider
		View ruler1 = new View(this); ruler1.setBackgroundColor(getResources().getColor(R.color.black));
		v.addView(ruler1, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
		// adding the filename field
		TextView fnLabel = new TextView(this);
		fnLabel.setText(getString(R.string.export_file_qualifier));
		v.addView(fnLabel);
		filenameValField = new EditText(this);
		filenameValField.setId(FILENAMEVAL_ID);
		v.addView(filenameValField);
		// Horizontal divider
		View ruler3 = new View(this); ruler3.setBackgroundColor(getResources().getColor(R.color.black));
		v.addView(ruler3, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
		// adding the export button
		Button button = new Button(this);
		button.setId(EXPORTBUTTON_ID);
		button.setText(getString(R.string.export_button));
		button.setOnClickListener(new ButtonListener());
		v.addView(button);
		// wrapping in a scroll view
		ScrollView scroll = new ScrollView(this);
		scroll.addView(v);
		return scroll;
	}

	/**
	 * Attempts to export a table.
	 */
	private void exportSubmission() {
        File file = ODKFileUtils.asAppFile(
            appName,
            filenameValField.getText().toString().trim()
        );
        TableProperties tp = tps[tableSpin.getSelectedItemPosition()];
        ExportTask task = new ExportTask(this, appName);
        showDialog(EXPORT_IN_PROGRESS_DIALOG);
        task.execute(new ExportRequest(tp, filenameValField.getText().toString().trim()));
	}

	@Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if(resultCode == RESULT_CANCELED) {return;}
        Uri fileUri = data.getData();
        File filepath = new File(fileUri.getPath());
        String relativePath = ODKFileUtils.asRelativePath(appName, filepath);
        filenameValField.setText(relativePath);
    }

	private class ButtonListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			exportSubmission();
		}
	}
}
