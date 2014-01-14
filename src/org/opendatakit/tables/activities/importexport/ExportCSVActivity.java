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
package org.opendatakit.tables.activities.importexport;

import java.io.File;

import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.TableProperties;
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
import android.widget.CheckBox;
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

	private DbHelper dbh;

	/* the list of table names */
	private String[] tableNames;
	/* the list of TableProperties */
	private TableProperties[] tps;
	/* the table name spinner */
	private Spinner tableSpin;
	/* the text field for getting the filename */
	private EditText filenameValField;
	/* the checkbox for including properties */
	private CheckBox incAllPropertiesCheck;
	/* the checkbox for including access control field */
	private CheckBox incAccessControlCheck;
	/* the checkbox for including timestamps */
	private CheckBox incTimestampsCheck;
	/* the checkbox for including form ids */
	private CheckBox incFormIdsCheck;
	/* the checkbox for including locales */
	private CheckBox incLocalesCheck;
	/* the pick file button */
	private Button pickFileButton;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dbh = DbHelper.getDbHelper(this, TableFileUtils.ODK_TABLES_APP_NAME);
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
		est.setTextColor(getResources().getColor(R.color.white));
		v.addView(est);
		// adding the table spinner
		tableSpin = new Spinner(this);
		tableSpin.setId(TABLESPIN_ID);
		tps = TableProperties.getTablePropertiesForAll(dbh,
		    KeyValueStore.Type.ACTIVE);
		tableNames = new String[tps.length];
		for (int i = 0; i < tps.length; i++) {
		    tableNames[i] = tps[i].getDisplayName();
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
		// options
		TextView opt = new TextView(this);
		opt.setText(getString(R.string.export_options));
		opt.setTextColor(getResources().getColor(R.color.white));
		v.addView(opt);
		// adding the include properties checkbox
		LinearLayout incProps = new LinearLayout(this);
		incAllPropertiesCheck = new CheckBox(this);
		incAllPropertiesCheck.setChecked(true);
		incProps.addView(incAllPropertiesCheck);
		TextView incPropsLabel = new TextView(this);
        incPropsLabel.setTextColor(getResources().getColor(R.color.white));
		incPropsLabel.setText(getString(R.string.export_opt_include_metadata));
		incProps.addView(incPropsLabel);
		v.addView(incProps);
		// adding the include user id checkbox
		{
			LinearLayout incUI = new LinearLayout(this);
			incAccessControlCheck = new CheckBox(this);
			incAccessControlCheck.setChecked(true);
			incUI.addView(incAccessControlCheck);
			TextView incUILabel = new TextView(this);
			incUILabel.setText(getString(R.string.export_opt_include_user_id));
			incUILabel.setTextColor(getResources().getColor(R.color.white));
			incUI.addView(incUILabel);
			v.addView(incUI);
		}
		// adding the include timestamps checkbox
		{
			LinearLayout incTS = new LinearLayout(this);
			incTimestampsCheck = new CheckBox(this);
			incTimestampsCheck.setChecked(true);
			incTS.addView(incTimestampsCheck);
			TextView incTSLabel = new TextView(this);
			incTSLabel.setText(getString(R.string.export_opt_include_modification_datetime));
			incTSLabel.setTextColor(getResources().getColor(R.color.white));
			incTS.addView(incTSLabel);
			v.addView(incTS);
		}
		// adding the include form id checkbox
		{
			LinearLayout incFI = new LinearLayout(this);
			incFormIdsCheck = new CheckBox(this);
			incFormIdsCheck.setChecked(true);
			incFI.addView(incFormIdsCheck);
			TextView incFILabel = new TextView(this);
			incFILabel.setText(getString(R.string.export_opt_include_form_id));
			incFILabel.setTextColor(getResources().getColor(R.color.white));
			incFI.addView(incFILabel);
			v.addView(incFI);
		}
		// adding the include locale checkbox
		{
			LinearLayout incLO = new LinearLayout(this);
			incLocalesCheck = new CheckBox(this);
			incLocalesCheck.setChecked(true);
			incLO.addView(incLocalesCheck);
			TextView incLOLabel = new TextView(this);
			incLOLabel.setText(getString(R.string.export_opt_include_locale));
			incLOLabel.setTextColor(getResources().getColor(R.color.white));
			incLO.addView(incLOLabel);
			v.addView(incLO);
		}
		// Horizontal divider
		View ruler2 = new View(this); ruler2.setBackgroundColor(getResources().getColor(R.color.black));
		v.addView(ruler2, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
		// adding the filename field
		LinearLayout fn = new LinearLayout(this);
		fn.setOrientation(LinearLayout.VERTICAL);
		TextView fnLabel = new TextView(this);
		fnLabel.setText(getString(R.string.export_csv_file));
		fnLabel.setTextColor(getResources().getColor(R.color.white));
		fn.addView(fnLabel);
		filenameValField = new EditText(this);
		filenameValField.setId(FILENAMEVAL_ID);
		fn.addView(filenameValField);
		v.addView(fn);
        pickFileButton = new Button(this);
        pickFileButton.setText(getString(R.string.export_choose_csv_file));
        pickFileButton.setOnClickListener(new PickFileButtonListener(getString(R.string.export_choose_csv_file)));
        v.addView(pickFileButton);
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
            TableFileUtils.ODK_TABLES_APP_NAME, 
            filenameValField.getText().toString().trim()
        );
        TableProperties tp = tps[tableSpin.getSelectedItemPosition()];
        boolean incProps = incAllPropertiesCheck.isChecked();
        boolean incTs = incTimestampsCheck.isChecked();
        boolean incAC = incAccessControlCheck.isChecked();
        boolean incFI = incFormIdsCheck.isChecked();
        boolean incLo = incLocalesCheck.isChecked();
        ExportTask task = new ExportTask(this, TableFileUtils.ODK_TABLES_APP_NAME);
        showDialog(EXPORT_IN_PROGRESS_DIALOG);
        task.execute(new ExportRequest(tp, file, incTs, incAC, incFI, incLo, incProps));
	}

	@Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if(resultCode == RESULT_CANCELED) {return;}
        Uri fileUri = data.getData();
        String filepath = fileUri.getPath();
        String relativePath = TableFileUtils.getRelativePath(filepath);
        filenameValField.setText(relativePath);
    }

	private class ButtonListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			exportSubmission();
		}
	}
}
