package yoonsung.odk.spreadsheet.Activity.importexport;

import java.io.File;
import java.util.List;
import yoonsung.odk.spreadsheet.Database.Data;
import yoonsung.odk.spreadsheet.csvie.CSVException;
import yoonsung.odk.spreadsheet.csvie.CSVExporter;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class ExportCSVActivity extends Activity {
	
	/* the list of table names */
	private List<String> tableNames;
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
		tableNames = (new Data()).getTables();
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item,
				tableNames.toArray(new String[0]));
		adapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);
		tableSpin.setAdapter(adapter);
		tableSpin.setSelection(0);
		v.addView(tableSpin);
		// adding the filename field
		LinearLayout fn = new LinearLayout(this);
		fn.setOrientation(LinearLayout.VERTICAL);
		TextView fnLabel = new TextView(this);
		fnLabel.setText("Filename:");
		fn.addView(fnLabel);
		filenameValField = new EditText(this);
		fn.addView(filenameValField);
		v.addView(fn);
		// adding the export button
		Button button = new Button(this);
		button.setText("Export");
		button.setOnClickListener(new ButtonListener());
		v.addView(button);
		return v;
	}
	
	/**
	 * Attempts to export a table.
	 */
	private void exportSubmission() {
		String filename = filenameValField.getText().toString();
		File root = Environment.getExternalStorageDirectory();
		File file = new File(root.getPath() +
				"/data/data/yoonsung.odk.spreadsheet/files/" + filename);
		String tableName = tableNames.get(tableSpin.getSelectedItemPosition());
		try {
			(new CSVExporter()).exportTable(tableName, file);
		} catch (CSVException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			notifyOfError(e.getMessage());
			return;
		}
	}
	
	/**
	 * To be called in case of errors.
	 * @param errMsg the message to display to the user
	 * TODO: make this useful
	 */
	private void notifyOfError(String errMsg) {
		Log.d("OH NOES", errMsg);
	}
	
	private class ButtonListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			exportSubmission();
		}
	}
	
}
