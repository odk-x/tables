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

import org.opendatakit.tables.Activity.util.CsvUtil;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.TableProperties;

import android.R;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
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
		est.setTextColor(getResources().getColor(getResources().getColor(R.color.black)));
		v.addView(est);
		// adding the table spinner
		tableSpin = new Spinner(this);
		tableSpin.setId(TABLESPIN_ID);
		tps = TableProperties.getTablePropertiesForAll(dbh);
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
		v.addView(ruler1, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 2));
		// options
		TextView opt = new TextView(this);
		opt.setText("Options:");
		opt.setTextColor(getResources().getColor(getResources().getColor(R.color.black)));
		v.addView(opt);
		// adding the include properties checkbox
		LinearLayout incProps = new LinearLayout(this);
		incPropsCheck = new CheckBox(this);
		incPropsCheck.setChecked(true);
		incProps.addView(incPropsCheck);
		TextView incPropsLabel = new TextView(this);
        incPropsLabel.setTextColor(getResources().getColor(R.color.black));
		incPropsLabel.setText("Include Table Settings");
		incProps.addView(incPropsLabel);
		v.addView(incProps);
		// adding the include source phone numbers checkbox
		LinearLayout incPN = new LinearLayout(this);
		incPNCheck = new CheckBox(this);
		incPNCheck.setChecked(true);
		incPN.addView(incPNCheck);
		TextView incPNLabel = new TextView(this);
		incPNLabel.setText("Include Phone Number for Incoming Rows");
		incPNLabel.setTextColor(getResources().getColor(R.color.black));
		incPN.addView(incPNLabel);
		v.addView(incPN);
		// adding the include timestamps checkbox
		LinearLayout incTS = new LinearLayout(this);
		incTSCheck = new CheckBox(this);
		incTSCheck.setChecked(true);
		incTS.addView(incTSCheck);
		TextView incTSLabel = new TextView(this);
		incTSLabel.setText("Include Last Modification Timestamp");
		incTSLabel.setTextColor(getResources().getColor(R.color.black));
		incTS.addView(incTSLabel);
		v.addView(incTS);
		// Horizontal divider
		View ruler2 = new View(this); ruler2.setBackgroundColor(getResources().getColor(R.color.black));
		v.addView(ruler2, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 2));
		// adding the filename field
		LinearLayout fn = new LinearLayout(this);
		fn.setOrientation(LinearLayout.VERTICAL);
		TextView fnLabel = new TextView(this);
		fnLabel.setText("Filename:");
		fnLabel.setTextColor(getResources().getColor(R.color.black));
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
		v.addView(ruler3, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 2));
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
        ExportTask task = new ExportTask();
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
    
    private class ExportTask
            extends AsyncTask<ExportRequest, Integer, Boolean> {
        
        protected Boolean doInBackground(ExportRequest... exportRequests) {
            ExportRequest request = exportRequests[0];
            CsvUtil cu = new CsvUtil(ExportCSVActivity.this);
            if (request.getIncludeProperties()) {
                return cu.exportWithProperties(request.getFile(),
                        request.getTableProperties().getTableId(),
                        request.getIncludeTimestamps(),
                        request.getIncludePhoneNums());
            } else {
                return cu.export(request.getFile(),
                        request.getTableProperties().getTableId(),
                        request.getIncludeTimestamps(),
                        request.getIncludePhoneNums());
            }
        }
        
        protected void onProgressUpdate(Integer... progress) {
            // do nothing
        }
        
        protected void onPostExecute(Boolean result) {
            dismissDialog(EXPORT_IN_PROGRESS_DIALOG);
            if (result) {
                showDialog(CSVEXPORT_SUCCESS_DIALOG);
            } else {
                showDialog(CSVEXPORT_FAIL_DIALOG);
            }
        }
    }
	
	private class ExportRequest {
	    
	    private final TableProperties tp;
	    private final File file;
	    private final boolean includeProperties;
	    private final boolean includeTimestamps;
	    private final boolean includePhoneNums;
	    
	    public ExportRequest(TableProperties tp, File file,
	            boolean includeProperties, boolean includeTimestamps,
	            boolean includePhoneNums) {
	        this.tp = tp;
	        this.file = file;
	        this.includeProperties = includeProperties;
	        this.includeTimestamps = includeTimestamps;
	        this.includePhoneNums = includePhoneNums;
	    }
	    
	    public TableProperties getTableProperties() {
	        return tp;
	    }
	    
	    public File getFile() {
	        return file;
	    }
	    
	    public boolean getIncludeProperties() {
	        return includeProperties;
	    }
	    
	    public boolean getIncludeTimestamps() {
	        return includeTimestamps;
	    }
	    
	    public boolean getIncludePhoneNums() {
	        return includePhoneNums;
	    }
	}
}
