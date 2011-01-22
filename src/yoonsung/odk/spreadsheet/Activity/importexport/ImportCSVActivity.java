package yoonsung.odk.spreadsheet.Activity.importexport;

import java.io.File;
import java.util.List;
import yoonsung.odk.spreadsheet.Database.Data;
import yoonsung.odk.spreadsheet.csvie.CSVException;
import yoonsung.odk.spreadsheet.csvie.CSVImporter;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * An activity for importing CSV files to a table.
 */
public class ImportCSVActivity extends IETabActivity {
	
	/** view IDs (for use in testing) */
	public static int NTNVAL_ID = 1;
	public static int TABLESPIN_ID = 2;
	public static int FILENAMEVAL_ID = 3;
	public static int IMPORTBUTTON_ID = 4;
	
	/* the list of table names */
	private List<String> tableNames;
	/* the view for inputting the new table name (label and text field) */
	private View newTableViews;
	/* the text field for getting the new table name */
	private EditText ntnValField;
	/* the table name spinner */
	private Spinner tableSpin;
	/* the text field for getting the filename */
	private EditText filenameValField;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getView());
	}
	
	/**
	 * @return the view
	 */
	private View getView() {
		LinearLayout v = new LinearLayout(this);
		v.setOrientation(LinearLayout.VERTICAL);
		// adding the table spinner
		tableSpin = new Spinner(this);
		tableSpin.setId(TABLESPIN_ID);
		tableNames = (new Data()).getTables();
		tableNames.add(0, "New Table");
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item,
				tableNames.toArray(new String[0]));
		adapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);
		tableSpin.setAdapter(adapter);
		tableSpin.setSelection(0);
		tableSpin.setOnItemSelectedListener(new tableSpinListener());
		v.addView(tableSpin);
		// adding the new table name field
		LinearLayout ntn = new LinearLayout(this);
		ntn.setOrientation(LinearLayout.VERTICAL);
		TextView ntnLabel = new TextView(this);
		ntnLabel.setText("New Table Name:");
		ntn.addView(ntnLabel);
		ntnValField = new EditText(this);
		ntnValField.setId(NTNVAL_ID);
		ntnValField.setText("New Table");
		ntn.addView(ntnValField);
		newTableViews = ntn;
		v.addView(newTableViews);
		// adding the filename field
		LinearLayout fn = new LinearLayout(this);
		fn.setOrientation(LinearLayout.VERTICAL);
		TextView fnLabel = new TextView(this);
		fnLabel.setText("Filename:");
		fn.addView(fnLabel);
		filenameValField = new EditText(this);
		filenameValField.setId(FILENAMEVAL_ID);
		fn.addView(filenameValField);
		v.addView(fn);
		// adding the import button
		Button button = new Button(this);
		button.setId(IMPORTBUTTON_ID);
		button.setText("Import");
		button.setOnClickListener(new ButtonListener());
		v.addView(button);
		return v;
	}
	
	/**
	 * Attempts to import a CSV file.
	 */
	private void importSubmission() {
		String filename = filenameValField.getText().toString();
		File root = Environment.getExternalStorageDirectory();
		File file = new File(root.getPath() +
				"/data/data/yoonsung.odk.spreadsheet/files/" + filename);
		String tableName;
		int pos = tableSpin.getSelectedItemPosition();
		if(pos == 0) {
			tableName = ntnValField.getText().toString();
			// TODO: make it possible to create new tables
		} else {
			tableName = tableNames.get(pos);
			// TODO: verify it is an actual table
		}
		try {
			(new CSVImporter()).importTable(tableName, file);
			showDialog(CSVIMPORT_SUCCESS_DIALOG);
		} catch (CSVException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			notifyOfError(e.getMessage());
			return;
		}
	}
	
	/**
	 * A listener for the table name spinner. Adds or removes the "New Table"
	 * name field as necessary.
	 */
	private class tableSpinListener
			implements AdapterView.OnItemSelectedListener {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view,
				int position, long id) {
			if(position == 0) {
				newTableViews.setVisibility(View.VISIBLE);
			} else {
				newTableViews.setVisibility(View.GONE);
			}
		}
		@Override
		public void onNothingSelected(AdapterView<?> parent) {
		}
	}
	
	/**
	 * A listener for the import button. Calls importSubmission() on click.
	 */
	private class ButtonListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			importSubmission();
		}
	}
	
}
