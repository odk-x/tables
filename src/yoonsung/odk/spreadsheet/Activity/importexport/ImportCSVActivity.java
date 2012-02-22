package yoonsung.odk.spreadsheet.Activity.importexport;

import java.io.File;
import java.util.Map;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.Activity.util.CsvUtil;
import yoonsung.odk.spreadsheet.csvie.CSVException;
import yoonsung.odk.spreadsheet.csvie.CSVImporter;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.TableProperties;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
	
	/* the list of table properties */
	private TableProperties[] tps;
	/* the list of table names */
	private String[] tableNames;
	/* the view for inputting the new table name (label and text field) */
	private View newTableViews;
	/* the text field for getting the new table name */
	private EditText ntnValField;
	/* the table name spinner */
	private Spinner tableSpin;
	/* the text field for getting the filename */
	private EditText filenameValField;
	/* the button for selecting a file */
	private Button pickFileButton;
	
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
		// adding the filename field
		LinearLayout fn = new LinearLayout(this);
		fn.setOrientation(LinearLayout.VERTICAL);
		TextView fnLabel = new TextView(this);
		fnLabel.setText("Filename:");
		fnLabel.setTextColor(R.color.black);
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
		View ruler1 = new View(this); ruler1.setBackgroundColor(R.color.black);
		v.addView(ruler1,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 2));
		// adding the table spinner
		TextView etn = new TextView(this);
		etn.setText("Import to new table or add to existing table:");
		etn.setTextColor(R.color.black);
		tableSpin = new Spinner(this);
		tableSpin.setId(TABLESPIN_ID);
		tps = TableProperties.getTablePropertiesForAll(
		        DbHelper.getDbHelper(this));
		tableNames = new String[tps.length + 1];
		tableNames[0] = "New Table";
		int counter = 1;
		for (TableProperties tp : tps) {
		    tableNames[counter] = tp.getDisplayName();
		    counter++;
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, tableNames);
		adapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);
		tableSpin.setAdapter(adapter);
		tableSpin.setSelection(0);
		tableSpin.setOnItemSelectedListener(new tableSpinListener());
		v.addView(etn);
		v.addView(tableSpin);
		// adding the new table name field
		LinearLayout ntn = new LinearLayout(this);
		ntn.setOrientation(LinearLayout.VERTICAL);
		TextView ntnLabel = new TextView(this);
		ntnLabel.setText("New Table Name:");
		ntnLabel.setTextColor(R.color.black);
		ntn.addView(ntnLabel);
		ntnValField = new EditText(this);
		ntnValField.setId(NTNVAL_ID);
		ntnValField.setText("New Table");
		ntn.addView(ntnValField);
		newTableViews = ntn;
		v.addView(newTableViews);
		// Horizontal divider
		View ruler2 = new View(this); ruler2.setBackgroundColor(R.color.black);
		v.addView(ruler2,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 2));
		// adding the import button
		Button importB = new Button(this);
		importB.setId(IMPORTBUTTON_ID);
		importB.setText("Add to Table");
		importB.setOnClickListener(new ImportButtonListener());
		v.addView(importB);
		// wrapping in a scroll view
		ScrollView scroll = new ScrollView(this);
		scroll.addView(v);
		return scroll;
	}
	
	/**
	 * Attempts to import a CSV file.
	 */
	private void importSubmission() {
		File file = new File(filenameValField.getText().toString().trim());
		String tableName = null;
		TableProperties tp = null;
		int pos = tableSpin.getSelectedItemPosition();
		if(pos == 0) {
			tableName = ntnValField.getText().toString();
		} else {
		    tp = tps[pos - 1];
		}
		Handler iHandler = new ImporterHandler();
		ImporterThread iThread = new ImporterThread(iHandler, tableName, tp,
		        file, (pos == 0));
		showDialog(IMPORT_IN_PROGRESS_DIALOG);
		iThread.start();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
	        Intent data) {
	    if(resultCode == RESULT_CANCELED) {return;}
	    Uri fileUri = data.getData();
	    String filepath = fileUri.getPath();
	    filenameValField.setText(filepath);
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
	private class ImportButtonListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			importSubmission();
		}
	}
	
	private class ImporterThread extends Thread {
		
		private Handler mHandler;
		private String tableName;
		private TableProperties tp;
		private File file;
		private boolean createTable;
		
		ImporterThread(Handler h, String tableName, TableProperties tp,
		        File file, boolean createTable) {
			mHandler = h;
			this.tableName = tableName;
			this.tp = tp;
			this.file = file;
			this.createTable = createTable;
		}
		
		public void run() {
		    CsvUtil cu = new CsvUtil(ImportCSVActivity.this);
		    boolean success;
		    if (createTable) {
		        success = cu.importNewTable(file, tableName);
		    } else {
		        success = cu.importAddToTable(file, tp.getTableId());
		    }
		    Bundle b = new Bundle();
		    b.putBoolean("success", success);
		    if (!success) {
		        b.putString("errmsg", "Failed to import file.");
		    }
		    Message msg = mHandler.obtainMessage();
		    msg.setData(b);
		    mHandler.sendMessage(msg);
		    /**
			CSVImporter importer = new CSVImporter(ImportCSVActivity.this);
			boolean success = true;
			String errorMsg = null;
			try {
				if(createTable) {
					importer.buildTable(tableName, file);
				} else {
					importer.importTable(tp, file);
				}
			} catch(CSVException e) {
				success = false;
				errorMsg = e.getMessage();
			}
			Bundle b = new Bundle();
			b.putBoolean("success", success);
			if(!success) {
				b.putString("errmsg", errorMsg);
			}
			Message msg = mHandler.obtainMessage();
			msg.setData(b);
			mHandler.sendMessage(msg);
			**/
		}
		
	}
	
	private class ImporterHandler extends Handler {
		
		public void handleMessage(Message msg) {
			dismissDialog(IMPORT_IN_PROGRESS_DIALOG);
			Bundle b = msg.getData();
			if(b.getBoolean("success")) {
				showDialog(CSVIMPORT_SUCCESS_DIALOG);
			} else {
				showDialog(CSVIMPORT_FAIL_DIALOG);
			}
		}
		
	}
	
}
