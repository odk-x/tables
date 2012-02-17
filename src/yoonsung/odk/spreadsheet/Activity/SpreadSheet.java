package yoonsung.odk.spreadsheet.Activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import yoonsung.odk.spreadsheet.data.ColumnProperties;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;

public class SpreadSheet extends TableActivity {
	
	// options menu IDs
	private static final int OPEN_TABLE_MANAGER = 0;
	private static final int OPEN_COLUMN_MANAGER = 1;
	private static final int OPEN_SECURITY_MANAGER = 2;
	private static final int GRAPH = 3;
	private static final int IMPORTEXPORT = 5;
	private static final int OPEN_TPM = 6;
	// context menu IDs
	private static final int SELECT_COLUMN = 7;
	private static final int SEND_SMS_ROW = 8;
	private static final int HISTORY_IN = 9;
	private static final int DELETE_ROW = 10;
	private static final int SET_COL_AS_PRIME = 11;
	private static final int UNSET_COL_AS_PRIME = 12;
	private static final int SET_COL_AS_ORDERBY = 13;
	private static final int UNSET_COL_AS_ORDERBY = 14;
	private static final int OPEN_COL_OPTS = 15;
	private static final int SET_COL_WIDTH = 16;
	private static final int SET_FOOTER_OPT = 17;
	private static final int UNSELECT_COLUMN = 18;
	private static final int OPEN_FILE = 19;
	private static final int OPEN_DISPLAYPREFS_DIALOG = 20;
	private static final int EDIT_CELL = 21;
	private static final int OPEN_COLLECT_FORM = 22;
	// Activity IDs
	private static final int ODK_COLLECT_FORM_HANDLE = 100;
	
	private int lastHeaderMenued; // the ID of the last header cell that a
	                              // context menu was created for
	private int lastFooterMenued; // the ID of the last footer cell that a
	                              // context menu was created for
	
	private String ODKCollectFormInstancePath; // Instance path for ODK Collect Form
	
	/**
	 * Called when the activity is first created.
	 * @param savedInstanceState the data most recently saved if the activity
	 * is being re-initialized; otherwise null
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		prepOccmListeners();
		lastHeaderMenued = -1;
	}
	
	/**
	 * Initializes the contents of the standard options menu.
	 * @param menu the options menu to place items in
	 * @return true if the menu is to be displayed; false otherwise
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		int none = Menu.NONE;
		menu.add(none, OPEN_TABLE_MANAGER, none, "Table Manager");
		menu.add(none, OPEN_COLUMN_MANAGER, none, "Column Manager");
		menu.add(none, OPEN_SECURITY_MANAGER, none, "Security Manager");
		menu.add(none, GRAPH, none, "Graph");
		menu.add(none, IMPORTEXPORT, none, "Import/Export");
		menu.add(none, OPEN_TPM, none, "Table Properties");
		return true;
	}
	
	/**
	 * Called when an item in the options menu is selected.
	 * @param featureId the panel that the menu is in
	 * @param item the menu item that was selected.
	 * @return true to finish processing of selection; false to perform normal
	 * menu handling
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
		case OPEN_TABLE_MANAGER:
			openTableManager();
			return true;
		case OPEN_COLUMN_MANAGER:
			openColumnManager();
			return true;
		case OPEN_SECURITY_MANAGER:
			openSecurityManager();
			return true;
		case GRAPH:
			openGraph();
			return true;
		case IMPORTEXPORT:
			openImportExportScreen();
			return true;
		case OPEN_TPM:
		    openTablePropertiesManager();
		    return true;
		default:
			return super.onMenuItemSelected(featureId, item);
		}
	}
	
	/**
	 * Called when an item in a context menu is selected.
	 * @param item the context menu item that was selected
	 * @return false to allow context menu processing to proceed; true to
	 * consume it
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case SELECT_COLUMN: // index on this column
			indexTableView(selectedCellID % table.getWidth());
			return true;
		case UNSELECT_COLUMN:
		    indexTableView(-1);
		    return true;
		case SEND_SMS_ROW: // sends an SMS on the selected row
			sendSMSRow();
			return true;
		case EDIT_CELL:
		    editCell(selectedCellID);
		    return true;
		case HISTORY_IN: // view a collection
		    viewCollection(selectedCellID / table.getWidth());
			return true;
		case OPEN_COLLECT_FORM:
		    collect(selectedCellID / table.getWidth(),
		            table.getData(selectedCellID));
		    return true;
		case DELETE_ROW: // delete a row
			deleteRow(selectedCellID / table.getWidth());
			return true;
		case SET_COL_AS_PRIME: // set a column to be a prime column
			setAsPrimeCol(tp.getColumnOrder()[lastHeaderMenued]);
			return true;
		case UNSET_COL_AS_PRIME: // set a column to be a non-prime column
			unsetAsPrimeCol(tp.getColumnOrder()[lastHeaderMenued]);
			return true;
		case SET_COL_AS_ORDERBY: // set a column to be the sort column
			setAsSortCol(tp.getColumnOrder()[lastHeaderMenued]);
			return true;
		case UNSET_COL_AS_ORDERBY:
            setAsSortCol(null);
		    return true;
		case OPEN_COL_OPTS:
			openColPropsManager(tp.getColumnOrder()[lastHeaderMenued]);
			return true;
		case SET_COL_WIDTH:
			openColWidthDialog(tp.getColumnOrder()[lastHeaderMenued]);
			return true;
		case SET_FOOTER_OPT:
			openFooterOptDialog(tp.getColumnOrder()[lastFooterMenued]);
			return true;
		case OPEN_DISPLAYPREFS_DIALOG:
		    openDisplayPrefsDialog(tp.getColumnOrder()[lastHeaderMenued]);
		    return true;
		default:
			return super.onContextItemSelected(item);
		}
	}
	
	/**
	 * Opens up Security Manager. Available to this activity only.
	 */
	private void openSecurityManager() {
		Intent i = new Intent(this, SecurityManager.class);
		//i.putExtra("tableName", (new TableList()).getTableName(this.tableID));
		startActivity(i);
		throw new RuntimeException("openSecurityManager() called!");
	}
	
	private void handleOpenForm(String formPath) {
		// Open up Collect
		Intent i = new Intent("org.odk.collect.android.action.FormEntry");
		
		// Form Path
		i.putExtra("formpath", formPath);
		
		// Extract Form Name
		String formName = null;
		Pattern pattern = Pattern.compile("[a-z0-9A-Z-_ ]*[.]xml");
		Matcher matcher = pattern.matcher(formPath);
		if (matcher.find()) {
			formName = matcher.group().replace(".xml", "");
		} else {
			// Wrong file name or type
			return;
		}
		
		// Instance Path
		File externalStorage = Environment.getExternalStorageDirectory();
		String sdcardPath = externalStorage.getAbsolutePath();
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		String instanceName = sdf.format(cal.getTime());
		String instanceDirPath = sdcardPath + "/odk/instances/" + formName + "_" + instanceName;
		Log.e("dirpath", instanceDirPath);
		// Create instance Dir
		File dir = new File(instanceDirPath);
		dir.mkdirs();
		// Create instance File
		String instancePath = instanceDirPath + "/" + formName + "_" + instanceName + ".xml";
		Log.e("instpath", instancePath);
		try {
			File formF = new File(formPath);
			File instanceF = new File(instancePath);
			if (!instanceF.exists()) {
				FileChannel src = new FileInputStream(formF).getChannel();
				FileChannel dst = new FileOutputStream(instanceF).getChannel();
				dst.transferFrom(src, 0, src.size());
				src.close();
				dst.close();
			}
		} catch (Exception e) {}
		i.putExtra("instancepath", instancePath);
		
		// Start the intent for call back
		ODKCollectFormInstancePath = instancePath;
		startActivityForResult(i, ODK_COLLECT_FORM_HANDLE);
	}
	
    private void parseXMLAndUpdateForODKCollect(String path){
    	Log.e("CheckingThePath", path);
    	
        // Get Col List
    	final String[] colOrder = tp.getColumnOrder();
        
        try {
        	File file = new File(path);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();                           

            Map<String, String> values = new HashMap<String, String>();                                 
            
            // Browse content in xml file
            int addedvalue = 0;
         
            for(int s = 0; s < colOrder.length; s++) {                   
            	Log.e("colprint", colOrder[s]);
            	NodeList nodeLst = doc.getElementsByTagName(colOrder[s]);
            	Node fstNode = nodeLst.item(0);
                if (fstNode != null && fstNode.getNodeType() == Node.ELEMENT_NODE) {                                   
                    Element fstElmnt =  (Element) fstNode;                                                                          
                    NodeList lstNm = fstElmnt.getChildNodes();                                              
                    if(lstNm.item(0) != null) {
                        addedvalue++;
                        values.put(colOrder[s], ((Node) lstNm.item(0)).getNodeValue());   
                    } else {
                        values.put(colOrder[s], "");                                                      
                    }
                }    
            }       
                                            
            // Update to database
            if(addedvalue > 0){
                //dt.addRow(cv, "", "");
            	int rowNum = selectedCellID / table.getWidth();
				String rowId = table.getRowId(rowNum);
				dbt.updateRow(rowId, values);
            	Log.e("parseXML", values.toString());
            }
                                
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
    }
	
	/**
	 * Prepares the context menu creation listeners.
	 */
	private void prepOccmListeners() {
	}
	
	/**
	 * Prepares the context menu creation listener for regular cells.
	 */
	@Override
	public void prepRegularCellOccm(ContextMenu menu, int cellId) {
		selectContentCell(cellId);
		int none = ContextMenu.NONE;
		int selectedColType = tp.getColumns()
		        [selectedCellID % table.getWidth()].getColumnType();
		if (selectedColType == ColumnProperties.ColumnType.COLLECT_FORM) {
		    menu.add(none, OPEN_COLLECT_FORM, none, "Open Form in Collect");
		}
		menu.add(none, SELECT_COLUMN, none, "Select Column");
		menu.add(none, SEND_SMS_ROW, none, "Send SMS Row");
		menu.add(none, HISTORY_IN, none, "View Collection");
		menu.add(none, EDIT_CELL, none, "Edit Cell");
		menu.add(none, DELETE_ROW, none, "Delete Row");
	}
    
    /**
     * Prepares the context menu creation listener for indexed column cells.
     */
    @Override
    public void prepIndexedColCellOccm(ContextMenu menu, int cellId) {
        selectContentCell(cellId);
        int none = ContextMenu.NONE;
        ColumnProperties cp = tp.getColumns()
                [selectedCellID % table.getWidth()];
        if (cp.getColumnType() == ColumnProperties.ColumnType.COLLECT_FORM) {
            menu.add(none, OPEN_COLLECT_FORM, none, "Open Form in Collect");
        }
        menu.add(none, UNSELECT_COLUMN, none, "Unselect Column");
        menu.add(none, SEND_SMS_ROW, none, "Send SMS Row");
        menu.add(none, HISTORY_IN, none, "View Collection");
        menu.add(none, EDIT_CELL, none, "Edit Cell");
        menu.add(none, DELETE_ROW, none, "Delete Row");
    }
	
	/**
	 * Prepares the context menu creation listener for header cells.
	 */
    @Override
	public void prepHeaderCellOccm(ContextMenu menu, int cellId) {
		lastHeaderMenued = cellId;
		int none = ContextMenu.NONE;
		String colName = tp.getColumnOrder()[cellId];
		if(tp.isColumnPrime(colName)) {
			menu.add(none, UNSET_COL_AS_PRIME, none, "Unset as Index");
		} else if(colName.equals(tp.getSortColumn())) {
		    menu.add(none, UNSET_COL_AS_ORDERBY, none, "Unset as Sort");
		} else {
			menu.add(none, SET_COL_AS_PRIME, none, "Set as Index");
			menu.add(none, SET_COL_AS_ORDERBY, none, "Set as Sort");
		}
		menu.add(none, OPEN_COL_OPTS, none, "Column Properties");
		menu.add(none, OPEN_DISPLAYPREFS_DIALOG, none,
		        "Display Preferences");
		menu.add(none, SET_COL_WIDTH, none, "Set Column Width");
	}
	
	/**
	 * Prepares the context menu creation listener for footer cells.
	 */
    @Override
	public void prepFooterCellOccm(ContextMenu menu, int cellId) {
		lastFooterMenued = cellId;
		int none = ContextMenu.NONE;
		menu.add(none, SET_FOOTER_OPT, none, "Set Footer Mode");
	}
}
