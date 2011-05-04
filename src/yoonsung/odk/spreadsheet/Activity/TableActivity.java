package yoonsung.odk.spreadsheet.Activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.Activity.defaultopts.DefaultsActivity;
import yoonsung.odk.spreadsheet.Activity.graphs.BoxStemActivity;
import yoonsung.odk.spreadsheet.Activity.graphs.CalActivity;
import yoonsung.odk.spreadsheet.Activity.graphs.GraphSetting;
import yoonsung.odk.spreadsheet.Activity.graphs.LineActivity;
import yoonsung.odk.spreadsheet.Activity.graphs.MapViewActivity;
import yoonsung.odk.spreadsheet.Activity.graphs.PieActivity;
import yoonsung.odk.spreadsheet.Activity.importexport.ImportExportActivity;
import yoonsung.odk.spreadsheet.DataStructure.Table;
import yoonsung.odk.spreadsheet.Database.ColumnProperty;
import yoonsung.odk.spreadsheet.Database.DataTable;
import yoonsung.odk.spreadsheet.Database.TableList;
import yoonsung.odk.spreadsheet.Database.TableProperty;
import yoonsung.odk.spreadsheet.Library.graphs.GraphClassifier;
import yoonsung.odk.spreadsheet.Library.graphs.GraphDataHelper;
import yoonsung.odk.spreadsheet.SMS.SMSSender;
import yoonsung.odk.spreadsheet.view.TableDisplayView;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;

public abstract class TableActivity extends Activity {
	
	protected String tableID; // the ID of the table to display
	protected DataTable dt; // the data table
	protected TableProperty tp; // the table property manager
	protected ColumnProperty cp; // the column property manager
	protected Table table; // the table
	protected TableDisplayView tdv; // the table display view
	protected int selectedCellID; // the ID of the content cell currently
	                              // selected; -1 if none is selected
	protected int collectionRowNum; // the row number of the collection being
	                                // viewed; -1 if on main table
	protected int indexedCol; // the column to index on; -1 if not indexed
	
	// fields for row addition
	// TODO: remove these once row addition uses Collect
	private int currentAddRowColPos;
	private Map<String, String> currentAddRowBuffer;
	
	/**
	 * Called when the activity is first created.
	 * @param savedInstanceState the data most recently saved if the activity
	 * is being re-initialized; otherwise null
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.table_layout);
		if(!setTableID()) {
			// No table exist
			bounceToTableManager();
			return;
		}
		TextView led = (TextView) findViewById(R.id.tableNameLed);
		led.setText((new TableList()).getTableName(tableID));
		indexedCol = -1;
		collectionRowNum = -1;
		selectedCellID = -1;
		dt = new DataTable(tableID);
		tp = new TableProperty(tableID);
		cp = new ColumnProperty(tableID);
		table = dt.getTable();
		tdv = new TableDisplayView(this);
		tdv.setTable(this, table);
		prepButtonListeners();
		TableLayout tl = (TableLayout) findViewById(R.id.spreadsheetWrapper);
		tl.addView(tdv);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if(!setTableID()) {
			// No table exist
			bounceToTableManager();
			return;
		}
		refreshView();
	}
	
	/**
	 * Indexes the table on the given column.
	 * @param indexedCol the column to index on
	 */
	protected void indexTableView(int indexedCol) {
		Log.d("REFACTOR SSJ", "indexTableView called:" + indexedCol);
		this.indexedCol = indexedCol;
		refreshView();
	}
	
	/**
	 * Switches to a collection view.
	 * @param rowNum the row to match against
	 */
	protected void viewCollection(int rowNum) {
		collectionRowNum = rowNum;
		selectContentCell(-1);
		table = dt.getTable(collectionRowNum);
		tdv.setTable(TableActivity.this, table, indexedCol);
	}
	
	/**
	 * Opens the table manager.
	 */
	protected void openTableManager() {
		startActivity(new Intent(this, TableManager.class));
	}
	
	/**
	 * Opens the column manager.
	 */
	protected void openColumnManager() {
		Intent i = new Intent(this, ColumnManager.class);
		i.putExtra("tableID", tableID);
		startActivity(i);
	}
	
	/**
	 * Opens a graph.
	 * TODO: clean this up
	 */
	protected void openGraph() {
    	Intent g = null;
		
    	// Classifier
    	GraphClassifier gcf = new GraphClassifier(this, tableID, (indexedCol == -1));
    	String graphType = gcf.getGraphType();
    	String colOne = gcf.getColOne(); // i.e. X
    	String colTwo = gcf.getColTwo(); // i.e. Y
    	
    	// Process Helper
    	GraphDataHelper gdh = new GraphDataHelper(dt);
    	
    	Log.e("report", "graph type: " + graphType + " " + colOne + " " + colTwo);
    	
	  	if ((graphType == null) ||(colOne == null) || (colTwo == null) ||
	  			!dt.isColumnExist(colOne) || !dt.isColumnExist(colTwo)) {
	  		Log.e("GRAPTH", "Such a graph type does not exists");
	  		bounceToGraphSettings();
	  		return;
		} else if (graphType.equals(GraphClassifier.LINE_GRAPH)) {
    		g = new Intent(this, LineActivity.class); 
    		ArrayList<String> x = table.getCol(table.getColNum(colOne));
    		ArrayList<String> yStrList = table.getCol(table.getColNum(colTwo));
    		double[] y = new double[yStrList.size()];
    		for(int i=0; i<yStrList.size(); i++) {
    			try {
    				y[i] = Double.parseDouble(yStrList.get(i));
    			} catch(NumberFormatException e) {
        	  		bounceToGraphSettings();
        	  		return;
    			}
    		}
    		//Collections.reverse(x);
    		//Collections.reverse(y);
    		g.putExtra("x", x);
    		g.putExtra("y", y);
    		g.putExtra("xname", colOne);
    		g.putExtra("yname", colTwo);
	    } else if (graphType.equals(GraphClassifier.STEM_GRAPH)) {
	    	g = new Intent(this, BoxStemActivity.class);
	    	ArrayList<String> x = table.getCol(table.getColNum(colOne));
	    	HashMap<String, ArrayList<Double>> stemResult = 
	    			gdh.prepareYForStemGraph(table, colOne, colTwo);
	    	
	    	// Check if the data is valid to draw a stem graph
	    	if (stemResult == null) {
	    		Log.e("GRAPH", "Cannot draw");
	    		g = new Intent(this, GraphSetting.class);
	    	} else {
	    		g.putExtra("xname", colOne);
	    		g.putExtra("yname", colTwo);
    	    	g.putExtra("x", x);
    	    	g.putExtra("min", gdh.arraylistToArray(stemResult.get("Q0s")));
        		g.putExtra("low", gdh.arraylistToArray(stemResult.get("Q1s")));
    			g.putExtra("mid", gdh.arraylistToArray(stemResult.get("Q2s")));
    			g.putExtra("high",gdh.arraylistToArray(stemResult.get("Q3s")));
    			g.putExtra("max", gdh.arraylistToArray(stemResult.get("Q4s")));
	    	}
	    } else if(graphType.equals(GraphClassifier.PIE_CHART)) {
	    	Map<String, Integer> vals;
	    	if(indexedCol == -1) {
	    		vals = dt.getCounts(colOne);
	    	} else {
	    		vals = new HashMap<String, Integer>();
	    		int colIndex = table.getColNum(colOne);
	    		List<String> columnVals = table.getCol(colIndex);
	    		for(String val : columnVals) {
	    			Integer count = vals.get(val);
	    			if(count == null) {
	    				count = 1;
	    			} else {
	    				count += 1;
	    			}
	    			vals.put(val, count);
	    		}
	    	}
	    	String[] xVals = new String[vals.size()];
	    	int[] yVals = new int[vals.size()];
	    	int i = 0;
	    	for(String val : vals.keySet()) {
	    		xVals[i] = val;
	    		yVals[i] = vals.get(val);
	    		i++;
	    	}
	    	g = new Intent(this, PieActivity.class);
	    	g.putExtra("xName", colOne);
	    	g.putExtra("xVals", xVals);
	    	g.putExtra("yVals", yVals);
	    } else if (graphType.equals(GraphClassifier.MAP)) {
	    	g = new Intent(this, MapViewActivity.class);
	    	ArrayList<String> location = table.getCol(table.getColNum(colOne));
	    	g.putExtra("location", location);
	    } else if (graphType.equals(GraphClassifier.CALENDAR)) {
    		g = new Intent(this, CalActivity.class); 
    		ArrayList<String> x = table.getCol(table.getColNum(colOne));
    		ArrayList<String> y = table.getCol(table.getColNum(colTwo));
    		//Collections.reverse(x);
    		//Collections.reverse(y);
    		g.putExtra("x", x);
    		g.putExtra("y", y);
	    } else {
	    	Log.e("GRAPTH", "Such a graph type does not exists");
	    	g = new Intent(this, GraphSetting.class);
	    }
	  	g.putExtra("tableID", tableID);
	  	startActivity(g);
	}
	
	/**
	 * Bounces the user to the graph settings screen.
	 * TODO: clean this up
	 */
	private void bounceToGraphSettings() {
  		Intent g = new Intent(this, GraphSetting.class);
  		g.putExtra("tableID", tableID);
  		startActivity(g);
	}
	
	/**
	 * Opens the default message options screen.
	 */
	protected void openDefOptsManager() {
		startActivity(new Intent(this, DefaultsActivity.class));
	}
	
	/**
	 * Opens the import/export screen.
	 */
	protected void openImportExportScreen() {
		startActivity(new Intent(this, ImportExportActivity.class));
	}
	
	/**
	 * Deletes a row.
	 * @param rowNum the row in the table view to delete
	 */
	protected void deleteRow(int rowNum) {
		selectContentCell(-1);
		int rowId = table.getTableRowID(rowNum);
		dt.deleteRow(rowId);
		refreshView();
	}
	
	/**
	 * Sets a column to be a prime column.
	 * @param colName the name of the column
	 */
	protected void setAsPrimeCol(String colName) {
		cp.setIsIndex(colName, true);
		refreshView();
	}
	
	/**
	 * Sets a column to be a non-prime column.
	 * @param colName the name of the column
	 */
	protected void unsetAsPrimeCol(String colName) {
		cp.setIsIndex(colName, false);
		refreshView();
	}
	
	/**
	 * Sets a column to be the sort column.
	 * @param colName the name of the column
	 */
	protected void setAsSortCol(String colName) {
		tp.setSortBy(colName);
		refreshView();
	}
	
	/**
	 * Opens the column properties screen.
	 * @param colName the column name
	 */
	protected void openColPropsManager(String colName) {
		Log.d("REFACTOR SSJ", "openColPropsManager called:" + colName);
		Intent i = new Intent(this, PropertyManager.class);
		i.putExtra("colName", colName);
		i.putExtra("tableID", tableID);
		startActivity(i);
	}
	
	/**
	 * Opens the column width dialog.
	 * @param colName the column name
	 */
	protected void openColWidthDialog(String colName) {
		(new ColWidthDialog(this, colName)).show();
	}
	
	/**
	 * Opens the footer mode option dialog.
	 * @param colName the column name
	 */
	protected void openFooterOptDialog(String colName) {
		(new FooterModeDialog(this, colName)).show();
	}
	
	/**
	 * Opens a simple dialog.
	 * @param text the dialog text
	 * TODO: clean this up
	 */
	protected void showSimpleDialog(String text) {
		AlertDialog.Builder adBuilder = new AlertDialog.Builder(this);
		adBuilder = adBuilder.setMessage(text);
		adBuilder = adBuilder.setNeutralButton("OK",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
		});
		AlertDialog d = adBuilder.create();
		d.show();
	}
    
	// TODO: clean this up
    private class ColWidthDialog extends AlertDialog {
    	
    	private Context c;
    	private String colName;
    	private SharedPreferences settings;
    	private EditText et;
    	
    	ColWidthDialog(Context c, String colName) {
    		super(c);
    		this.c = c;
    		this.colName = colName;
        	settings = PreferenceManager.getDefaultSharedPreferences(c);
    	}
    	
    	@Override
    	public void onCreate(Bundle savedInstanceState) {
    		super.onCreate(savedInstanceState);
    		setContentView(prepView());
    	}
    	
    	private View prepView() {
    		LinearLayout v = new LinearLayout(c);
    		v.setOrientation(LinearLayout.VERTICAL);
    		int colWidth = settings.getInt("tablewidths-" + tableID +
    				"-" + colName, 125);
    		// preparing the text field
    		et = new EditText(c);
    		et.setText((new Integer(colWidth)).toString());
    		v.addView(et);
    		// preparing the button
    		ColWidthListener cwl = new ColWidthListener(this, colName, et,
    				settings.edit());
    		Button b = new Button(c);
    		b.setText("Set Width");
    		b.setOnClickListener(cwl);
    		v.addView(b);
    		return v;
    	}
    	
    }
    
    // TODO: clean this up
    private class ColWidthListener implements View.OnClickListener {
    	private Dialog d;
    	private String colName;
    	private EditText et;
    	private SharedPreferences.Editor prefEditor;
    	ColWidthListener(Dialog d, String colName, EditText et,
    			SharedPreferences.Editor prefEditor) {
    		this.d = d;
    		this.colName = colName;
    		this.et = et;
    		this.prefEditor = prefEditor;
    	}
		@Override
		public void onClick(View v) {
			Integer newVal;
			try {
				newVal = new Integer(et.getText().toString());
			} catch(NumberFormatException e) {
				showSimpleDialog("Please enter a number.");
				return;
			}
			prefEditor.putInt("tablewidths-" + tableID + "-" + colName,
					newVal);
			prefEditor.commit();
			d.dismiss();
			refreshView();
		}
    }
    
    // TODO: clean this up
    private class FooterModeDialog extends AlertDialog {
    	
    	private Context c;
    	private String colName;
    	private ColumnProperty cp;
    	private Spinner optSpinner;
    	
    	FooterModeDialog(Context c, String colName) {
    		super(c);
    		this.c = c;
    		this.colName = colName;
    		cp = new ColumnProperty(tableID);
    	}
    	
    	@Override
    	public void onCreate(Bundle savedInstanceState) {
    		super.onCreate(savedInstanceState);
    		setContentView(prepView());
    	}
    	
    	private View prepView() {
    		LinearLayout v = new LinearLayout(c);
    		v.setOrientation(LinearLayout.VERTICAL);
    		// preparing the text field
    		optSpinner = new Spinner(c);
            String[] footerModeChoices = {"None", "Average", "Count", "Max",
            		"Min"};
    		ArrayAdapter<String> adapter = new ArrayAdapter<String>(c,
    				android.R.layout.simple_spinner_item, footerModeChoices);
    		adapter.setDropDownViewResource(
    				android.R.layout.simple_spinner_dropdown_item);
    		optSpinner.setAdapter(adapter);
    		String selMode = cp.getFooterMode(colName);
    		if(selMode != null) {
    			for(int i=0; i<footerModeChoices.length; i++) {
    				if(footerModeChoices[i].equals(selMode)) {
    					optSpinner.setSelection(i);
    				}
    			}
    		}
    		v.addView(optSpinner);
    		// preparing the button
    		FooterModeListener fml = new FooterModeListener(this, colName, optSpinner, cp);
    		Button b = new Button(c);
    		b.setText("Set Footer Mode");
    		b.setOnClickListener(fml);
    		v.addView(b);
    		return v;
    	}
        
    	// TODO: clean this up
        private class FooterModeListener implements View.OnClickListener {
        	private Dialog d;
        	private String colName;
        	private Spinner opts;
        	private ColumnProperty cp;
        	FooterModeListener(Dialog d, String colName, Spinner opts,
        			ColumnProperty cp) {
        		this.d = d;
        		this.colName = colName;
        		this.opts = opts;
        		this.cp = cp;
        	}
    		@Override
    		public void onClick(View v) {
    			cp.setFooterMode(colName, opts.getSelectedItem().toString());
    			d.dismiss();
    			refreshView();
    		}
        }
    	
    }
	
	/**
	 * Sends an SMS on the row.
	 */
	protected void sendSMSRow() {
		// TODO: clean this up
    	final List<String> curHeader = table.getHeader();
    	final List<String> curRow = table.getRow(table.getRowNum(selectedCellID));
    	tp = new TableProperty(tableID);
    	final List<String> formats = new ArrayList<String>();
    	Map<Integer, String> formatMap = tp.getDefOutMsg();
    	for(int i : formatMap.keySet()) {
    		formats.add(formatMap.get(i));
    	}
    	final int[] formatIndex = new int[1];
    	formatIndex[0] = 0;
    	String content = "";
    	if(formats.size() > 0) {
    		content = formRowMsg(formats.get(0), curHeader, curRow);
    	}
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);
    	alert.setTitle("Send Row");
    	LinearLayout v = new LinearLayout(this);
    	v.setOrientation(LinearLayout.VERTICAL);
    	TextView pnLabel = new TextView(this);
    	pnLabel.setText("send to:");
    	final AutoCompleteTextView pnET = new AutoCompleteTextView(this);
    	pnET.setAdapter(getContactsAdapter());
    	TextView msgLabel = new TextView(this);
    	msgLabel.setText("message:");
    	final EditText msgET = new EditText(this);
    	msgET.setText(content);
    	Button prevFormat = new Button(this);
    	prevFormat.setLayoutParams(new LinearLayout.LayoutParams(
    			LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0));
    	prevFormat.setText("<");
    	prevFormat.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				formats.set(formatIndex[0], msgET.getText().toString());
				formatIndex[0] = (formatIndex[0] + formats.size() - 1) % formats.size();
				msgET.setText(formRowMsg(formats.get(formatIndex[0]), curHeader, curRow));
			}
    	});
    	Button nextFormat = new Button(this);
    	nextFormat.setLayoutParams(new LinearLayout.LayoutParams(
    			LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0));
    	nextFormat.setText(">");
    	nextFormat.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				formats.set(formatIndex[0], msgET.getText().toString());
				formatIndex[0] = (formatIndex[0] + 1) % formats.size();
				msgET.setText(formRowMsg(formats.get(formatIndex[0]), curHeader, curRow));
			}
    	});
    	LinearLayout msgOpts = new LinearLayout(this);
    	if(formats.size() > 1) {
    		msgOpts.addView(prevFormat);
    	}
    	msgET.setLayoutParams(new LinearLayout.LayoutParams(
    			LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1));
    	msgOpts.addView(msgET);
    	if(formats.size() > 1) {
    		msgOpts.addView(nextFormat);
    	}
    	v.addView(pnLabel);
    	v.addView(pnET);
    	v.addView(msgLabel);
    	v.addView(msgOpts);
    	alert.setView(v);
    	alert.setPositiveButton("Send", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    			String pn = pnET.getText().toString();
    			String msg = msgET.getText().toString();
    			sendSmsRow(pn, msg);
    		}
    	});
    	alert.show();
	}
	
	// TODO: clean this up
    private String formRowMsg(String format, List<String> curHeader,
    		List<String> curRow) {
    	Log.d("ss.j frmts", "curHeader:" + curHeader.toString());
    	Log.d("ss.j frmts", "curRow:" + curRow.toString());
    	int lastPCSign = -1;
    	for(int i=0; i<format.length(); i++) {
    		if(format.charAt(i) == '%') {
    			if(lastPCSign < 0) {
    				lastPCSign = i;
    			} else {
    				String origColName = format.substring(lastPCSign + 1, i);
    				String colName;
    				if(dt.isColumnExist(origColName)) {
    					colName = origColName;
    				} else {
    					colName = cp.getNameByAbrv(origColName);
    				}
    				Log.d("ss.j formats", "colName:" + colName);
    				int index = curHeader.indexOf(colName);
    				if(index >= 0) {
    					String colVal = curRow.get(index);
    					if(colVal == null) {
    						colVal = "";
    					}
    					format = format.substring(0, lastPCSign) +
    						colVal + format.substring(i + 1);
    					i -= (origColName.length() - colVal.length()) + 1;
    				}
    				lastPCSign = -1;
    			}
    		}
    	}
    	return format;
    }
	
    // TODO: clean this up
	private void sendSmsRow(String pn, String msg) {
		 Cursor c = getContentResolver().query(ContactsContract.Data.CONTENT_URI,
		          new String[] {Phone.NUMBER},  ContactsContract.Data.DISPLAY_NAME +
		          "=?" + " AND " + ContactsContract.Data.MIMETYPE + "='" +
		          Phone.CONTENT_ITEM_TYPE + "'", new String[] {pn}, null);
		String phone;
		if(c.getCount() > 0) {
			c.moveToFirst();
			int index = c.getColumnIndexOrThrow(Phone.NUMBER);
			phone = c.getString(index);
		} else {
			phone = pn;
		}
		c.close();
		Log.d("ss.j", "sending (" + msg + ") to:" + phone);
		SMSSender sender = new SMSSender();
		sender.sendSMS(phone, msg);
	}
	
	// TODO: clean this up
	private ContactsAdapter getContactsAdapter() {
    	Uri uri = ContactsContract.Contacts.CONTENT_URI;
    	String[] projection = new String[] {
    			ContactsContract.Contacts._ID,
    			ContactsContract.Contacts.DISPLAY_NAME
    	};
    	String selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + " = 1";
    	String sort = ContactsContract.Contacts.DISPLAY_NAME;
    	Cursor cs = managedQuery(uri, projection, selection, null, sort);
    	startManagingCursor(cs);
		return new ContactsAdapter(this, cs);
	}
	
	// TODO: clean this up
    private class ContactsAdapter extends SimpleCursorAdapter {
    	private ContactsAdapter(Context c, Cursor cs) {
	    	super(c, android.R.layout.simple_dropdown_item_1line, cs,
	    			new String[] {ContactsContract.Contacts.DISPLAY_NAME},
	    			new int[] {android.R.id.text1});
    	}
    	@Override
    	public CharSequence convertToString(Cursor c) {
    		int index = c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME);
    		return c.getString(index);
    	}
    }
	
	/**
	 * Prepares the button click listeners.
	 */
	protected void prepButtonListeners() {
		prepHomeTableButton();
		prepRefreshTableButtonListener();
		prepCellEditEnterButtonListener();
		prepAddRowButtonListener();
	}
	
	/**
	 * Prepares the home table button listener.
	 */
	protected void prepHomeTableButton() {
		ImageButton homeButton = (ImageButton) findViewById(R.id.back);
		homeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if((indexedCol == -1) && (collectionRowNum == -1)) {
					return;
				}
				indexedCol = -1;
				collectionRowNum = -1;
				selectContentCell(-1);
				table = dt.getTable();
				tdv.setTable(TableActivity.this, table);
			}
		});
	}
	
	/**
	 * Prepares the refresh table button listener.
	 */
	protected void prepRefreshTableButtonListener() {
		ImageButton refreshButton = (ImageButton) findViewById(R.id.refresh);
		refreshButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				refreshView();
			}
		});
	}
	
	/**
	 * Prepares the cell edit enter button listener.
	 */
	protected void prepCellEditEnterButtonListener() {
		Button ecEnterButton = (Button) findViewById(R.id.enter);
		ecEnterButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(selectedCellID < 0) {
					return;
				}
				// getting the new value
				EditText ecEditText = (EditText) findViewById(R.id.edit_box);
				String val = ecEditText.getText().toString();
				// updating the database
				int colNum = table.getColNum(selectedCellID);
				int rowNum = table.getRowNum(selectedCellID);
				int rowID = table.getTableRowID(rowNum);
				String colName = table.getColName(colNum);
				ContentValues vals = new ContentValues();
				vals.put(colName, val);
				dt.updateRow(vals, rowID);
				// updating the cell's content
				tdv.setCellContent(selectedCellID, val);
			}
		});
	}
	
	/**
	 * Prepares the add row button listener.
	 * TODO: change this to use the Collect launcher
	 */
	protected void prepAddRowButtonListener() {
        ImageButton ar = (ImageButton)findViewById(R.id.add_row);
		ar.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.d("REFACTOR SSJ", "addRow clicked");
				// Get Col List
				TableProperty tableProp = new TableProperty(tableID);
				final List<String> currentColList = tableProp.getColOrderArrayList();
				
				// No column exists
				if (currentColList.size() == 0) {
					return;
				}
				
				// Reset
				currentAddRowColPos = 0;
				
				// Create a buffer
				currentAddRowBuffer = new HashMap<String, String>();
				
				final Dialog dia = new Dialog(TableActivity.this);
				dia.setContentView(R.layout.add_row_dialog);
				dia.setTitle("Add New Row");
				
				TextView colName = (TextView)dia.findViewById(R.id.add_row_col);
				colName.setText(currentColList.get(currentAddRowColPos));
				
				TextView prog = (TextView)dia.findViewById(R.id.add_row_progress);
				prog.setText("1 / " + currentColList.size());
				
				Button prev = (Button)dia.findViewById(R.id.add_row_prev);
				prev.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						if (currentAddRowColPos > 0) {
							// Save what's in edit box
							EditText edit = (EditText)dia.findViewById(R.id.add_row_edit);
							String txt = edit.getText().toString();
							txt = txt.trim();
							currentAddRowBuffer.put(currentColList.get(currentAddRowColPos), txt);
							
							// Update column name with prev
							TextView colName = (TextView)dia.findViewById(R.id.add_row_col);
							colName.setText(currentColList.get(currentAddRowColPos-1));
							currentAddRowColPos--;
							
							// Update progress 
							TextView prog = (TextView)dia.findViewById(R.id.add_row_progress);
							prog.setText(Integer.toString(currentAddRowColPos+1) + " / " + currentColList.size());
							
							// Refresh Editbox
							String nextTxt = currentAddRowBuffer.get(currentColList.get(currentAddRowColPos));
							if (nextTxt == null) {
								edit.setText("");
							} else {
								edit.setText(nextTxt);
							}
						}
					}
				});
				
				
				Button save = (Button)dia.findViewById(R.id.add_row_save);
				save.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// Save what's in edit box
						EditText edit = (EditText)dia.findViewById(R.id.add_row_edit);
						String txt = edit.getText().toString();
						currentAddRowBuffer.put(currentColList.get(currentAddRowColPos), txt);
						
						// Update to database
						ContentValues cv = new ContentValues();
						for (String key : currentAddRowBuffer.keySet()) {
							cv.put(key, currentAddRowBuffer.get(key));
						}
						try {
						    dt.addRow(cv, "", "");
						} catch(IllegalArgumentException e) {
						    // TODO: something when the input is invalid for the columns
						    dia.cancel();
						}
						
						Log.e("Buffer Hash", currentAddRowBuffer.toString());
						
						refreshView();
						dia.cancel();
					}
				});
				
				Button next = (Button)dia.findViewById(R.id.add_row_next);
				next.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						if (currentAddRowColPos < currentColList.size()-1) {
							// Save what's in edit box
							EditText edit = (EditText)dia.findViewById(R.id.add_row_edit);
							String txt = edit.getText().toString();
							currentAddRowBuffer.put(currentColList.get(currentAddRowColPos), txt);
							
							// Update column name with next
							TextView colName = (TextView)dia.findViewById(R.id.add_row_col);
							colName.setText(currentColList.get(currentAddRowColPos+1));
							currentAddRowColPos++;
							
							// Update progress 
							TextView prog = (TextView)dia.findViewById(R.id.add_row_progress);
							prog.setText(Integer.toString(currentAddRowColPos+1) + " / " + currentColList.size());
							
							// Refresh Editbox
							String nextTxt = currentAddRowBuffer.get(currentColList.get(currentAddRowColPos));
							if (nextTxt == null) {
								edit.setText("");
							} else {
								edit.setText(nextTxt);
							}
						}
					}
				});
				
				Button cancle = (Button)dia.findViewById(R.id.add_row_cancle);
				cancle.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						dia.cancel();
					}
				});
				
				dia.show();
			}
		});
	}
	
	/**
	 * To be called when a regular cell is clicked.
	 * Sets the selected cell ID and the cell edit box text.
	 * @param cellID the cell's ID
	 */
	public void regularCellClicked(int cellID) {
		selectContentCell(cellID);
	}
	
	/**
	 * To be called when a header cell is clicked.
	 * Sets the selected cell ID to -1 and empties the cell edit box.
	 * @param cellID the cell's ID
	 */
	public void headerCellClicked(int cellID) {
		selectContentCell(-1);
	}
	
	/**
	 * To be called when a cell in an indexed column is clicked.
	 * Sets the selected cell ID and the cell edit box text.
	 * @param cellID the cell's ID
	 */
	public void indexedColCellClicked(int cellID) {
		selectContentCell(cellID);
	}
	
	/**
	 * To be called when a footer cell is clicked.
	 * Sets the selected cell ID to -1 and empties the cell edit box.
	 * @param cellID the cell's ID
	 */
	public void footerCellClicked(int cellID) {
		selectContentCell(-1);
	}
	
	/**
	 * Sets the OnCreateContextMenu listener for a regular cell.
	 * @param cell the cell
	 */
	public abstract void prepRegularCellOccmListener(TextView cell);
	
	/**
	 * Sets the OnCreateContextMenu listener for a header cell.
	 * @param cell the cell
	 */
	public abstract void prepHeaderCellOccmListener(TextView cell);
	
	/**
	 * Sets the OnCreateContextMenu listener for an indexed column cell.
	 * @param cell the cell
	 */
	public abstract void prepIndexedColCellOccmListener(TextView cell);
	
	/**
	 * Sets the OnCreateContextMenu listener for a footer cell.
	 * @param cell the cell
	 */
	public abstract void prepFooterCellOccmListener(TextView cell);
	
	private void refreshView() {
		selectContentCell(-1);
		if(collectionRowNum == -1) {
			table = dt.getTable();
			tdv.setTable(TableActivity.this, table, indexedCol);
		} else {
			table = dt.getTable(collectionRowNum);
			tdv.setTable(TableActivity.this, table, indexedCol);
		}
	}
	
	/**
	 * Sets the tableID field.
	 * @return true if a table ID was found; false otherwise
	 */
	private boolean setTableID() {
		tableID = getIntent().getStringExtra("tableID");
		if(tableID == null) {
			SharedPreferences settings =
				PreferenceManager.getDefaultSharedPreferences(this);
			tableID = settings.getString("ODKTables:tableID", null);
		}
		
		// Check if table really exists
		TableList tl = new TableList();
		if (!tl.isTableExistByTableID(tableID)) {
			tableID = null;
		}
		
		return (tableID != null);
	}
	
	/**
	 * Starts the table manager in cases where no table ID was found.
	 */
	private void bounceToTableManager() {
		Intent i = new Intent(this, TableManager.class);
		i.putExtra("loadError", true);
		startActivity(i);
	}
	
	/**
	 * Gets an array of column widths for the table.
	 * @return the array of widths
	 */
	public int[] getColWidths() {
		int[] colWidths = new int[table.getWidth()];
		SharedPreferences settings =
			PreferenceManager.getDefaultSharedPreferences(this);
		for(int i=0; i<colWidths.length; i++) {
			String key = "tablewidths-" + tableID + "-" + table.getColName(i);
			colWidths[i] = new Integer(settings.getInt(key, 125));
		}
		return colWidths;
	}
	
	/**
	 * Handles cell selection.
	 * @param cellID the cell to select, or -1 for no cell
	 */
	protected void selectContentCell(int cellID) {
		String ebVal;
		if(selectedCellID >= 0) {
			tdv.unhighlightCell(selectedCellID);
		}
		if(cellID < 0) {
			ebVal = "";
		} else {
			ebVal = tdv.getCellContent(cellID);
			tdv.highlightCell(cellID);
		}
		EditText box = (EditText) findViewById(R.id.edit_box);
		box.setText(ebVal);
		selectedCellID = cellID;
	}
	
}
