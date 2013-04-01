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
package org.opendatakit.tables.Activity.importexport;

import java.io.File;

import org.opendatakit.tables.R;
import org.opendatakit.tables.Task.ExportRequest;
import org.opendatakit.tables.Task.ExportTask;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.TableProperties;

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
public class ExportCSVActivity extends IETabActivity {

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
	private CheckBox incPropsCheck;
	/* the checkbox for including source phone numbers */
	private CheckBox incPNCheck;
	/* the checkbox for including timestamps */
	private CheckBox incTSCheck;
	/* the pick file button */
	private Button pickFileButton;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dbh = DbHelper.getDbHelper(this);
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
		est.setText("Exporting Table:");
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
		opt.setText("Options:");
		opt.setTextColor(getResources().getColor(R.color.white));
		v.addView(opt);
		// adding the include properties checkbox
		LinearLayout incProps = new LinearLayout(this);
		incPropsCheck = new CheckBox(this);
		incPropsCheck.setChecked(true);
		incProps.addView(incPropsCheck);
		TextView incPropsLabel = new TextView(this);
        incPropsLabel.setTextColor(getResources().getColor(R.color.white));
		incPropsLabel.setText("Include Metadata to Allow for Import");
		incProps.addView(incPropsLabel);
		v.addView(incProps);
		// adding the include source phone numbers checkbox
		LinearLayout incPN = new LinearLayout(this);
		incPNCheck = new CheckBox(this);
		incPNCheck.setChecked(true);
		incPN.addView(incPNCheck);
		TextView incPNLabel = new TextView(this);
		incPNLabel.setText("Include Phone Number for Incoming Rows");
		incPNLabel.setTextColor(getResources().getColor(R.color.white));
		incPN.addView(incPNLabel);
		v.addView(incPN);
		// adding the include timestamps checkbox
		LinearLayout incTS = new LinearLayout(this);
		incTSCheck = new CheckBox(this);
		incTSCheck.setChecked(true);
		incTS.addView(incTSCheck);
		TextView incTSLabel = new TextView(this);
		incTSLabel.setText("Include Last Modification Timestamp");
		incTSLabel.setTextColor(getResources().getColor(R.color.white));
		incTS.addView(incTSLabel);
		v.addView(incTS);
		// Horizontal divider
		View ruler2 = new View(this); ruler2.setBackgroundColor(getResources().getColor(R.color.black));
		v.addView(ruler2, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
		// adding the filename field
		LinearLayout fn = new LinearLayout(this);
		fn.setOrientation(LinearLayout.VERTICAL);
		TextView fnLabel = new TextView(this);
		fnLabel.setText("Filename:");
		fnLabel.setTextColor(getResources().getColor(R.color.white));
		fn.addView(fnLabel);
		filenameValField = new EditText(this);
		filenameValField.setId(FILENAMEVAL_ID);
		fn.addView(filenameValField);
		v.addView(fn);
        pickFileButton = new Button(this);
        pickFileButton.setText("Pick File");
        pickFileButton.setOnClickListener(new PickFileButtonListener());
        v.addView(pickFileButton);
		// Horizontal divider
		View ruler3 = new View(this); ruler3.setBackgroundColor(getResources().getColor(R.color.black));
		v.addView(ruler3, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
		// adding the export button
		Button button = new Button(this);
		button.setId(EXPORTBUTTON_ID);
		button.setText("Export");
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
        File file = new File(filenameValField.getText().toString());
        TableProperties tp = tps[tableSpin.getSelectedItemPosition()];
        boolean incProps = incPropsCheck.isChecked();
        boolean incTs = incTSCheck.isChecked();
        boolean incPn = incPNCheck.isChecked();
        ExportTask task = new ExportTask(this);
        showDialog(EXPORT_IN_PROGRESS_DIALOG);
        task.execute(new ExportRequest(tp, file, incProps, incTs, incPn));
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if(resultCode == RESULT_CANCELED) {return;}
        Uri fileUri = data.getData();
        String filepath = fileUri.getPath();
        filenameValField.setText(filepath);
    }

	private class ButtonListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			exportSubmission();
		}
	}
}
