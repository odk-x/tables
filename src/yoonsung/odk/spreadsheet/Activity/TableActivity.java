package yoonsung.odk.spreadsheet.Activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.kxml2.io.KXmlParser;
import org.kxml2.io.KXmlSerializer;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParserException;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.Activity.defaultopts.DefaultsActivity;
import yoonsung.odk.spreadsheet.Activity.graphs.BoxStemActivity;
import yoonsung.odk.spreadsheet.Activity.graphs.CalActivity;
import yoonsung.odk.spreadsheet.Activity.graphs.GraphSetting;
import yoonsung.odk.spreadsheet.Activity.graphs.LineActivity;
import yoonsung.odk.spreadsheet.Activity.graphs.MapViewActivity;
import yoonsung.odk.spreadsheet.Activity.graphs.PieActivity;
import yoonsung.odk.spreadsheet.Activity.importexport.ImportExportActivity;
import yoonsung.odk.spreadsheet.DataStructure.DisplayPrefs;
import yoonsung.odk.spreadsheet.DataStructure.Table;
import yoonsung.odk.spreadsheet.Database.ColumnProperty;
import yoonsung.odk.spreadsheet.Database.DataTable;
import yoonsung.odk.spreadsheet.Database.TableList;
import yoonsung.odk.spreadsheet.Database.TableProperty;
import yoonsung.odk.spreadsheet.Library.graphs.GraphClassifier;
import yoonsung.odk.spreadsheet.Library.graphs.GraphDataHelper;
import yoonsung.odk.spreadsheet.SMS.SMSSender;
import yoonsung.odk.spreadsheet.data.CollectUtil;
import yoonsung.odk.spreadsheet.view.TableDisplayView;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public abstract class TableActivity extends Activity {
    
    private static final int ODK_COLLECT_FORM_RETURN = 0;
    
    private static final String COLLECT_FORMS_URI_STRING =
        "content://org.odk.collect.android.provider.odk.forms/forms";
    private static final Uri COLLECT_FORMS_CONTENT_URI =
        Uri.parse(COLLECT_FORMS_URI_STRING);
    private static final String COLLECT_INSTANCES_URI_STRING =
        "content://org.odk.collect.android.provider.odk.instances/instances";
    private static final Uri COLLECT_INSTANCES_CONTENT_URI =
        Uri.parse(COLLECT_INSTANCES_URI_STRING);
    private static final DateFormat COLLECT_INSTANCE_NAME_DATE_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd_kk-mm-ss");
	
	protected String tableID; // the ID of the table to display
	protected DataTable dt; // the data table
	protected TableProperty tp; // the table property manager
	protected ColumnProperty cp; // the column property manager
	protected Table table; // the current table
	protected TableDisplayView tdv; // the table display view
	protected int selectedCellID; // the ID of the content cell currently
	                              // selected; -1 if none is selected
	protected int collectionRowNum; // the row number of the collection being
	                                // viewed; -1 if on main table
	protected int indexedCol; // the column to index on; -1 if not indexed
	protected Map<String, String> searchConstraints;
	
	private Map<String, Integer> collectInstances;
	
	// fields for row addition
	private int currentAddRowColPos;
	private Map<String, String> currentAddRowBuffer;
	private boolean isCollectForm;
	private static final String formPath = "/sdcard/ODK_Tables_add_row.xml";
	private static final String instancePath = "/sdcard/ODK_Table_Form_Results.xml";

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
		searchConstraints = new HashMap<String, String>();
		collectionRowNum = -1;
		selectedCellID = -1;
		dt = new DataTable(tableID);
		tp = new TableProperty(tableID);
		cp = new ColumnProperty(tableID);
		table = dt.getTable();
		tdv = new TableDisplayView(this, new DisplayPrefs(this, tableID));
		tdv.setTable(this, table);
        LinearLayout tableWrapper =
            (LinearLayout) findViewById(R.id.tableWrapper);
        LinearLayout.LayoutParams tableLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        tableLp.weight = 1;
        tableWrapper.addView(tdv, tableLp);
        collectInstances = new HashMap<String, Integer>();
		prepButtonListeners();
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
		this.indexedCol = indexedCol;
		refreshView();
	}
	
	/**
	 * Switches to a collection view.
	 * @param rowNum the row to match against
	 */
	protected void viewCollection(int rowNum) {
	    String searchTerms = "";
	    collectionRowNum = rowNum;
	    List<String> colOrder = tp.getColOrderArrayList();
	    for (int i = 0; i < colOrder.size(); i++) {
	        String col = colOrder.get(i);
	        if (!cp.getIsIndex(col)) {
	            // this isn't an index column, so it's value doesn't need to be
	            // matched
	            continue;
	        }
	        String value = table.getCellValue((rowNum * table.getWidth()) + i);
	        if (!searchConstraints.containsKey(col)) {
	            searchConstraints.put(col, value);
	            searchTerms += " " + col + ":" + value;
	        }
	    }
	    setSearchBoxText((getSearchBoxText() + searchTerms).trim());
	    search();
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
		i.putExtra(ColumnManager.INTENT_KEY_TABLE_ID, tableID);
		startActivity(i);
	}
	
	/**
	 * Opens a graph.
	 * TODO: clean this up
	 */
	protected void openGraph() {
    	Intent g = null;
		
    	// Classifier
    	GraphClassifier gcf = new GraphClassifier(this, tableID, (collectionRowNum == -1));
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
	 * @param rowNum the row index in the current table to delete (not the row
	 * ID)
	 */
	protected void deleteRow(int rowNum) {
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
		Intent i = new Intent(this, PropertyManager.class);
        i.putExtra(PropertyManager.INTENT_KEY_TABLE_ID, tableID);
		i.putExtra(PropertyManager.INTENT_KEY_COLUMN_NAME, colName);
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
	 * Opens the display preferences dialog.
	 * @param colName the column name
	 */
	protected void openDisplayPrefsDialog(String colName) {
	    DisplayPrefs dp = new DisplayPrefs(this, tableID);
	    (new DisplayPrefsDialog(this, dp, colName)).show();
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
	
	/**
	 * Opens ODK Collect with the given form and pre-filled values, and fills
	 * in matching values on return.
	 * @param rowNum the row number in the displayed table
	 * @param filepath the form's filepath
	 */
	protected void collect(int rowNum, String filename) {
	    String filepath = Environment.getExternalStorageDirectory() +
	            "/odk/forms/" + filename;
	    String jrFormId = verifyFormInCollect(filepath);
	    // reading the form
	    Document formDoc = new Document();
	    KXmlParser formParser = new KXmlParser();
	    try {
            formParser.setInput(new FileReader(filepath));
            formDoc.parse(formParser);
        } catch(FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch(XmlPullParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch(IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String namespace = formDoc.getRootElement().getNamespace();
        Element hhtmlEl = formDoc.getElement(namespace, "h:html");
        Element hheadEl = hhtmlEl.getElement(namespace, "h:head");
        Element modelEl = hheadEl.getElement(namespace, "model");
        Element instanceEl = modelEl.getElement(namespace, "instance");
        Element dataEl = instanceEl.getElement(1);
        // filling in values
        Element instance = new Element();
        instance.setName(dataEl.getName());
        Log.d("CSTF", "jrFormId in collect():" + jrFormId);
        instance.setAttribute("", "id", jrFormId);
        int childIndex = 0;
        List<String> colNames = tp.getColOrderArrayList();
        for (int i = 0; i < dataEl.getChildCount(); i++) {
            Element blankChild = dataEl.getElement(i);
            if (blankChild == null) { continue; }
            String key = blankChild.getName();
            Element child = instance.createElement("", key);
            int colIndex = colNames.indexOf(key);
            if (colIndex >= 0) {
                String value =
                    table.getCellValue((table.getWidth() * rowNum) + colIndex);
                if (value != null) {
                    child.addChild(0, Node.TEXT, value);
                }
            }
            instance.addChild(childIndex, Node.ELEMENT, child);
            childIndex++;
        }
	    // writing the instance file
        File formFile = new File(filepath);
        String formFileName = formFile.getName();
	    String instanceName = ((formFileName.endsWith(".xml") ?
	            formFileName.substring(0, formFileName.length() - 4) :
                formFileName)) +
                COLLECT_INSTANCE_NAME_DATE_FORMAT.format(new Date());
	    String instancePath = "/sdcard/odk/instances/" + instanceName;
	    (new File(instancePath)).mkdirs();
	    String instanceFilePath = instancePath + "/" + instanceName + ".xml";
	    File instanceFile = new File(instanceFilePath);
	    KXmlSerializer instanceSerializer = new KXmlSerializer();
	    try {
	        instanceFile.createNewFile();
	        FileWriter instanceWriter = new FileWriter(instanceFile);
            instanceSerializer.setOutput(instanceWriter);
            instance.write(instanceSerializer);
            instanceSerializer.endDocument();
            instanceSerializer.flush();
            instanceWriter.close();
        } catch(IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // registering the instance with Collect
        int instanceId = registerInstance(instanceName, instanceFilePath,
                jrFormId);
	    
	    Intent intent = new Intent();
	    intent.setComponent(new ComponentName("org.odk.collect.android",
	            "org.odk.collect.android.activities.FormEntryActivity"));
	    intent.setAction(Intent.ACTION_EDIT);
	    intent.setData(Uri.parse(COLLECT_INSTANCES_URI_STRING + "/" +
	            instanceId));
	    collectInstances.put(instanceFilePath, rowNum);
	    startActivityForResult(intent, ODK_COLLECT_FORM_RETURN);
	}
	
    public String verifyFormInCollect(String filepath) {
        Log.d("CSTF", "filepath<" + filepath + ">");
        for (int i = 0; i < 4; i++) {}
        String[] projection = { "jrFormId" };
        String selection = "formFilePath = ?";
        String[] selectionArgs = { filepath };
        Cursor c = managedQuery(COLLECT_FORMS_CONTENT_URI, projection,
                selection, selectionArgs, null);
        if (c.getCount() != 0) {
            c.moveToFirst();
            String value = c.getString(c.getColumnIndexOrThrow("jrFormId"));
            c.close();
            return value;
        }
        String jrFormId = CollectUtil.getJrFormId(filepath);
        ContentValues insertValues = new ContentValues();
        insertValues.put("displayName", filepath);
        insertValues.put("jrFormId", jrFormId);
        insertValues.put("formFilePath", filepath);
        getContentResolver().insert(COLLECT_FORMS_CONTENT_URI, insertValues);
        return jrFormId;
    }
    
    public int registerInstance(String name, String filepath, String jrFormId) {
        ContentValues insertValues = new ContentValues();
        insertValues.put("displayName", name);
        insertValues.put("instanceFilePath", filepath);
        insertValues.put("jrFormId", jrFormId);
        Uri insertResult = getContentResolver().insert(
                COLLECT_INSTANCES_CONTENT_URI, insertValues);
        return Integer.valueOf(insertResult.getLastPathSegment());
    }
	
	private class CellEditDialog extends Dialog {
	    
	    private Context c;
	    private int cellId;
	    private EditText et;
	    
        protected CellEditDialog(Context context, int cellId) {
            super(context);
            c = context;
            this.cellId = cellId;
        }
        
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(buildView());
        }
        
        private View buildView() {
            LinearLayout v = new LinearLayout(c);
            v.setOrientation(LinearLayout.VERTICAL);
            // preparing the text field
            et = new EditText(c);
            et.setText(table.getCellValue(cellId));
            v.addView(et);
            // preparing the button
            Button b = new Button(c);
            b.setText("Set Value");
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String value = et.getText().toString();
                    ContentValues values = new ContentValues();
                    values.put(table.getColName(cellId % table.getWidth()), value);
                    dt.updateRow(values,
                            table.getRowID().get(cellId / table.getWidth()));
                    table.setCellValue(cellId, value);
                    refreshView();
                    CellEditDialog.this.dismiss();
                }
            });
            v.addView(b);
            return v;
        }
	}
    
	// TODO: clean this up
    private class ColWidthDialog extends Dialog {
    	
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
            requestWindowFeature(Window.FEATURE_NO_TITLE);
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
    private class FooterModeDialog extends Dialog {
    	
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
            requestWindowFeature(Window.FEATURE_NO_TITLE);
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
		prepSearchButtonListener();
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
				indexedCol = -1;
				collectionRowNum = -1;
				selectContentCell(-1);
				table = dt.getTable();
				setSearchBoxText("");
				searchConstraints.clear();
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
	protected void prepSearchButtonListener() {
		View ecEnterButton = findViewById(R.id.enter);
		ecEnterButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			    searchConstraints.clear();
                String val = getSearchBoxText();
                for (String con : val.split(" ")) {
                    String[] spl = con.split(":");
                    if (spl.length != 2) {
                        continue;
                    }
                    searchConstraints.put(spl[0], spl[1]);
                }
                search();
			}
		});
	}
	
    /**
     * Prepares the add row button listener.
     */
    protected void prepAddRowButtonListener() {
        View arButton = findViewById(R.id.add_row);
        arButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /**
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(
                        "org.odk.collect.android",
                        "org.odk.collect.android.provider.FormsProvider"));
                intent.setAction("android.intent.action.EDIT");
                intent.putExtra("formpath", "/sdcard/odk/tables/addrow.xml");
                Uri uri = ContentUris.withAppendedId(Uri.parse(
                        "content://org.odk.collect.android.provider.odk.instances/instances"), 0);
                startActivityForResult(new Intent(Intent.ACTION_EDIT, uri), ODK_COLLECT_FORM_RETURN);
                **/
                backUpAddRowDialog();
            }
        });
    }
	
	private void search() {
        table = dt.getTable(collectionRowNum == -1, searchConstraints);
        tdv.setTable(TableActivity.this, table, indexedCol);
	}
	
	private String getSearchBoxText() {
        EditText et = (EditText) findViewById(R.id.edit_box);
        return et.getText().toString();
	}
	
	private void setSearchBoxText(String text) {
        EditText et = (EditText) findViewById(R.id.edit_box);
	    et.setText(text);
	}
	
	/*
	 * Create simple pops up messages
	 */
	protected void makeToast(String message) {
		Toast.makeText((Context) this, message, 
				Toast.LENGTH_LONG).show();		
	}
	
	/* 
	 * Prepares add row button only when ODK Collect is not installed
	 */
	protected void prepBackUpAddRowButtonListener() {
		
		ImageButton ar = (ImageButton)findViewById(R.id.add_row);
		ar.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				backUpAddRowDialog();
			}
		});		
	}

	/* 
	 * Construct dialog box for back up add row 
	 */
	protected void backUpAddRowDialog() {
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
				
				

				;
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
	
	
	
	/**
	 * To be called when a regular cell is clicked.
	 * Opens the cell edit dialog.
	 * @param cellId the cell's ID
	 */
	public void regularCellClicked(int cellId) {
	    //(new CellEditDialog(this, cellId)).show();
	}
	
	/**
	 * To be called when a header cell is clicked.
	 * @param cellId the cell's ID
	 */
	public void headerCellClicked(int cellId) {
	}
	
	/**
	 * To be called when a cell in an indexed column is clicked.
	 * @param cellId the cell's ID
	 */
	public void indexedColCellClicked(int cellId) {
	}
	
	/**
	 * To be called when a footer cell is clicked.
	 * @param cellId the cell's ID
	 */
	public void footerCellClicked(int cellId) {
	}
	
	public abstract void prepRegularCellOccm(ContextMenu menu, int cellId);
	
	public abstract void prepHeaderCellOccm(ContextMenu menu, int cellId);
	
	public abstract void prepIndexedColCellOccm(ContextMenu menu, int cellId);
	
	public abstract void prepFooterCellOccm(ContextMenu menu, int cellId);
	
	private void refreshView() {
		selectContentCell(-1);
		if(isCollectForm) {
			parseXML();
			isCollectForm = false;
		}		
		table = dt.getTable(collectionRowNum == -1, searchConstraints);
        tdv.setTable(TableActivity.this, table, indexedCol);
	}
	
	private void parseXML(){
	    /**
		
		String path = instancePath;
		
		// Get Col List
		TableProperty tableProp = new TableProperty(tableID);
		final List<String> currentColList = tableProp.getColOrderArrayList();
		
		
		try {
			File file = new File(path);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();				

			ContentValues cv = new ContentValues();					
			// Browse content in xml file
	    	int addedvalue = 0;
	    	
			for( int s = 0; s < currentColList.size() ; s++ ) {						
				NodeList nodeLst = doc.getElementsByTagName("col" + s);
		    	Node fstNode = nodeLst.item(0);
			    if (fstNode.getNodeType() == Node.ELEMENT_NODE) {				    	
			    	Element fstElmnt =  (Element) fstNode;				    					    	
			    	NodeList lstNm = fstElmnt.getChildNodes();					    	
			    	if(lstNm.item(0) != null) {
			    		addedvalue++;
			    		cv.put(currentColList.get(s), ((Node) lstNm.item(0)).getNodeValue());	
			    	} else {
			    		cv.put(currentColList.get(s), "");					    		
			    	}
			    }
			    
			}	
							
			// Update to database
			if(addedvalue > 0){
				dt.addRow(cv, "", "");							
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		**/
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
			//tdv.unhighlightCell(selectedCellID);
		}
		if(cellID < 0) {
			ebVal = "";
		} else {
		    ebVal = table.getCellValue(cellID);
			//tdv.highlightCell(cellID);
		}
		//EditText box = (EditText) findViewById(R.id.edit_box);
		//box.setText(ebVal);
		selectedCellID = cellID;
	}
	
	protected void editCell(int cellId) {
        (new CellEditDialog(this, cellId)).show();
	}
	
	/**
	 * Refreshes the display (but not the content).
	 */
	public void refreshDisplay() {
	    //tdv.addConditionalColors();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
	        Intent data) {
	    switch(requestCode) {
	    case ODK_COLLECT_FORM_RETURN:
	        int id = Integer.valueOf(data.getData().getLastPathSegment());
	        fillInCollectFormValues(id);
	        return;
        default:
            // TODO: something
            return;
	    }
	}
	
	private void fillInCollectFormValues(int instanceId) {
        String[] projection = { "instanceFilePath" };
        String selection = "_id = ?";
        String[] selectionArgs = { (instanceId + "") };
        Cursor c = managedQuery(COLLECT_INSTANCES_CONTENT_URI, projection,
                selection, selectionArgs, null);
        if (c.getCount() != 1) {
            return;
        }
	    c.moveToFirst();
	    String instancepath =
	        c.getString(c.getColumnIndexOrThrow("instanceFilePath"));
        Document xmlDoc = new Document();
        KXmlParser xmlParser = new KXmlParser();
        try {
            xmlParser.setInput(new FileReader(instancepath));
            xmlDoc.parse(xmlParser);
        } catch(IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch(XmlPullParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Element rootEl = xmlDoc.getRootElement();
        Node rootNode = rootEl.getRoot();
        Element dataEl = rootNode.getElement(0);
        Integer rowNum = collectInstances.get(instancepath);
        if (rowNum == null) {
            return;
        }
        List<String> colNames = tp.getColOrderArrayList();
        int rowId = table.getTableRowID(rowNum);
        ContentValues updates = new ContentValues();
        for (int i = 0; i < dataEl.getChildCount(); i++) {
            Element child = dataEl.getElement(i);
            String key = child.getName();
            int colIndex = colNames.indexOf(key);
            if (colIndex >= 0) {
                String value = child.getText(0);
                table.setCellValue((table.getWidth() * rowNum) + colIndex,
                        value);
                updates.put(key, value);
            }
        }
        if (updates.size() > 0) {
            dt.updateRow(updates, rowId);
        }
        collectInstances.remove(rowNum);
        refreshView();
	}
}
