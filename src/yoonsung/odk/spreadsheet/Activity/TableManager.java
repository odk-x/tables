package yoonsung.odk.spreadsheet.Activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.Activity.importexport.ImportExportActivity;
import yoonsung.odk.spreadsheet.data.ColumnProperties;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.Preferences;
import yoonsung.odk.spreadsheet.data.TableProperties;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class TableManager extends ListActivity {

	public static final int ADD_NEW_TABLE     		= 0;
	public static final int ADD_NEW_SECURITY_TABLE 	= 1;
	public static final int IMPORT_EXPORT			= 2;
	public static final int SET_DEFAULT_TABLE 		= 3;
	public static final int SET_SECURITY_TABLE      = 4;
	public static final int SET_SHORTCUT_TABLE      = 5;
	public static final int CHANGE_TABLE_NAME 		= 6;
	public static final int REMOVE_TABLE      		= 7;
	public static final int ADD_NEW_SHORTCUT_TABLE  = 8;
	public static final int UNSET_DEFAULT_TABLE     = 9;
	public static final int UNSET_SECURITY_TABLE    = 10;
	public static final int UNSET_SHORTCUT_TABLE    = 11;
	public static final int AGGREGATE               = 12;
	public static final int DETAIL_VIEW_FILE        = 13;
	public static final int LIST_FORMAT             = 14;
	public static final int LAUNCH_TPM              = 15;
	
	private static String[] from = new String[] {"label", "ext"};
	private static int[] to = new int[] { android.R.id.text1, android.R.id.text2 };
	
	private DbHelper dbh;
	private Preferences prefs;
	private TableProperties[] tableProps;
	
	private SimpleAdapter arrayAdapter;
	
	 @Override
	 public void onCreate(Bundle savedInstanceState) {
		 super.onCreate(savedInstanceState);
		 dbh = new DbHelper(this);
		 prefs = new Preferences(this);
		 
		 // Set title of activity
		 setTitle("ODK Tables > Table Manager");
		 
		 // Set Content View
		 setContentView(R.layout.white_list);
		 
		 refreshList();
	 }
	 
	 @Override
	 public void onResume() {
		 super.onResume();
		 refreshList();
	 }
	 
	 private void makeNoTableNotice() {
		 List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();
		 HashMap<String, String> temp = new HashMap<String, String>();
		 temp.put("label", "Click menu to add new table");
		 fillMaps.add(temp);
		 arrayAdapter = new SimpleAdapter(this, fillMaps, R.layout.white_list_row, from, to);
		 setListAdapter(arrayAdapter);
	 }
	 
	 public void refreshList() {
		 registerForContextMenu(getListView());
		 tableProps = TableProperties.getTablePropertiesForAll(dbh);
		 Log.d("TM", "refreshing list, tableProps.length=" + tableProps.length);
		 if (tableProps.length == 0) {
		     makeNoTableNotice();
		     return;
		 }
		 long defTableId = prefs.getDefaultTableId();
		 List<Map<String, String>> fMaps =
		     new ArrayList<Map<String, String>>();
		 for(TableProperties tp : tableProps) {
		     Map<String, String> map = new HashMap<String, String>();
		     map.put("label", tp.getDisplayName());
		     if (tp.getTableType() == TableProperties.TableType.SECURITY) {
		         map.put("ext", "Access Control Table");
		     } else if (tp.getTableType() ==
		         TableProperties.TableType.SHORTCUT) {
		         map.put("ext", "Shortcut Table");
		     }
		     if(tp.getTableId() == defTableId) {
		         if(map.get("ext") == null) {
		             map.put("ext", "Default Table");
		         } else {
		             map.put("ext", map.get("ext") + "; Default Table");
		         }
		     }
		     fMaps.add(map);
		 }
		 	 
		 // fill in the grid_item layout
		 arrayAdapter = new SimpleAdapter(this, fMaps, R.layout.white_list_row, from, to); 
		 setListAdapter(arrayAdapter);
		
		 ListView lv = getListView();
		 lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				
			 	@Override
				public void onItemClick(AdapterView adView, View view,
											int position, long id) {
					// Load Selected Table
					loadSelectedTable(position);
				}
		 });
	 } 
	 
	 private void loadSelectedTable(int index) {
	     TableProperties tp = tableProps[index];
	     Intent i;
	     switch (tp.getTableType()) {
	     case TableProperties.TableType.DATA:
	         i = new Intent(this, SpreadSheet.class);
	         break;
	     case TableProperties.TableType.SECURITY:
	         i = new Intent(this, SpreadSheet.class);
	         break;
	     case TableProperties.TableType.SHORTCUT:
	         i = new Intent(this, ShortcutTableActivity.class);
	         break;
         default:
             return;
	     }
	     i.putExtra(TableActivity.INTENT_KEY_TABLE_ID, tp.getTableId());
		 startActivity(i);
	 }
	 
	 @Override
	 public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		 super.onCreateContextMenu(menu, v, menuInfo);
		 AdapterView.AdapterContextMenuInfo acmi =
		     (AdapterView.AdapterContextMenuInfo) menuInfo;
		 TableProperties tp = tableProps[acmi.position];
		 if(tp.getTableId() == prefs.getDefaultTableId()) {
	         menu.add(0, UNSET_DEFAULT_TABLE, 0, "Unset as Default Table");
		 } else {
	         menu.add(0, SET_DEFAULT_TABLE, 0, "Set as Default Table");
		 }
		 int tableType = tp.getTableType();
		 if (tableType == TableProperties.TableType.DATA) {
		     if (couldBeSecurityTable(tp)) {
                 menu.add(0, SET_SECURITY_TABLE, 0, "Set as Access Control Table");
		     }
		     if (couldBeShortcutTable(tp)) {
                 menu.add(0, SET_SHORTCUT_TABLE, 0, "Set as Shortcut Table");
		     }
		 } else if (tableType == TableProperties.TableType.SECURITY) {
		     menu.add(0, UNSET_SECURITY_TABLE, 0, "Unset as Access Control Table");
		 } else if (tableType == TableProperties.TableType.SHORTCUT) {
		     menu.add(0, UNSET_SHORTCUT_TABLE, 0, "Unset as Shortcut Table");
		 }
		 menu.add(0, CHANGE_TABLE_NAME, 1, "Change Table Name");
		 menu.add(0, REMOVE_TABLE, 2, "Delete the Table");
         menu.add(0, DETAIL_VIEW_FILE, 3, "Change Detail View File");
		 menu.add(0, LIST_FORMAT, 4, "Change List Format");
		 menu.add(0, LAUNCH_TPM, 5, "Edit Table Properties");
	 }
	 
	 private boolean couldBeSecurityTable(TableProperties tp) {
	     String[] expected = { "phone_number", "id", "password" };
	     return checkTable(expected, tp);
	 }
	 
	 private boolean couldBeShortcutTable(TableProperties tp) {
         String[] expected = { "name", "input_format", "output_format" };
         return checkTable(expected, tp);
	 }
	 
	 private boolean checkTable(String[] expectedCols, TableProperties tp) {
	     ColumnProperties[] columns = tp.getColumns();
         if (columns.length < expectedCols.length) {
             return false;
         }
         for (int i = 0; i < expectedCols.length; i++) {
             if (!expectedCols[i].equals(columns[i].getColumnDbName())) {
                 return false;
             }
         }
         return true;
	 }
	 
	 @Override
	 public boolean onContextItemSelected(MenuItem item) {
	 
		 AdapterView.AdapterContextMenuInfo info= (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		 
		 TableProperties tp = tableProps[info.position];
		 
		 switch (item.getItemId()) {
		 case SET_DEFAULT_TABLE:
		     prefs.setDefaultTableId(tp.getTableId());
			 refreshList();
			 return true;
		 case SET_SECURITY_TABLE:
		     tp.setTableType(TableProperties.TableType.SECURITY);
             refreshList();
			 return true;
		 case UNSET_SECURITY_TABLE:
             tp.setTableType(TableProperties.TableType.DATA);
		     refreshList();
		     return true;
		 case SET_SHORTCUT_TABLE:
             tp.setTableType(TableProperties.TableType.SHORTCUT);
             refreshList();
		     return true;
         case UNSET_SHORTCUT_TABLE:
             tp.setTableType(TableProperties.TableType.DATA);
             refreshList();
             return true;
		 case CHANGE_TABLE_NAME:
			 // TO be Done
			 alertForNewTableName(false, -1, tp, null);
			 return true; 
		 case REMOVE_TABLE:
		     tp.deleteTable();
			 refreshList();
			 return true;
		 case DETAIL_VIEW_FILE:
		     alertForDetailViewFileChange(tp);
		     return true;
		 case LIST_FORMAT:
		     alertForListFormatChange(tp);
		     return true;
		 case LAUNCH_TPM:
		     Intent i = new Intent(this, TablePropertiesManager.class);
		     i.putExtra(TablePropertiesManager.INTENT_KEY_TABLE_ID, tp.getTableId());
		     startActivity(i);
		     return true;
		 }
		 return(super.onOptionsItemSelected(item));
	 }
	 
	 // CREATE OPTION MENU
	 @Override
	 public boolean onCreateOptionsMenu(Menu menu) {
		 super.onCreateOptionsMenu(menu);
		 menu.add(0, ADD_NEW_TABLE, 0, "Add New Data Table");
		 menu.add(0, ADD_NEW_SECURITY_TABLE, 0, "Add New Access Control Table");
		 menu.add(0, ADD_NEW_SHORTCUT_TABLE, 0, "Add New Shortcut Table");
		 menu.add(0, IMPORT_EXPORT, 0, "File Import/Export");
		 menu.add(0, AGGREGATE, 0, "Sync");
		 return true;
	 }
    
	 // HANDLE OPTION MENU
	 @Override
	 public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	
		 Log.d("timing", "menu item selected");
        
		 // HANDLES DIFFERENT MENU OPTIONS
		 switch(item.getItemId()) {
		 case ADD_NEW_TABLE:
			 alertForNewTableName(true, TableProperties.TableType.DATA, null, null);
			 return true;
		 case ADD_NEW_SECURITY_TABLE:
			 alertForNewTableName(true, TableProperties.TableType.SECURITY, null, null);
			 return true;
		 case ADD_NEW_SHORTCUT_TABLE:
             alertForNewTableName(true, TableProperties.TableType.SHORTCUT, null, null);
             return true;
		 case IMPORT_EXPORT:
			 Intent i = new Intent(this, ImportExportActivity.class);
			 startActivity(i);
			 return true;
		 case AGGREGATE:
			 Intent j = new Intent(this, Aggregate.class);
			 startActivity(j);
			 return true;
		 }
    	
		 return super.onMenuItemSelected(featureId, item);
	 }
	 
	 private void alertForListFormatChange(final TableProperties tp) {
	     AlertDialog.Builder alert = new AlertDialog.Builder(this);
	     alert.setTitle("Change List Format");
	     final EditText input = new EditText(this);
	     input.setText(tp.getListDisplayFormat());
	     alert.setView(input);
	     alert.setPositiveButton("Change", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                tp.setListDisplayFormat(input.getText().toString());
            }
	     });
	     alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
	     });
	     alert.show();
	 }
     
     private void alertForDetailViewFileChange(final TableProperties tp) {
         AlertDialog.Builder alert = new AlertDialog.Builder(this);
         alert.setTitle("Change Detail View File");
         final EditText input = new EditText(this);
         input.setText(tp.getDetailViewFilename());
         alert.setView(input);
         alert.setPositiveButton("Change", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                tp.setDetailViewFilename(input.getText().toString());
            }
         });
         alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
         });
         alert.show();
     }
	 
	 // Ask for a new table name.
	 private void alertForNewTableName(final boolean isNewTable, 
			 final int tableType, final TableProperties tp, String givenTableName) {
		
		 // Prompt an alert box
		 AlertDialog.Builder alert = new AlertDialog.Builder(this);
		 alert.setTitle("Name of New Table");
	
		 // Set an EditText view to get user input 
		 final EditText input = new EditText(this);
		 alert.setView(input);
		 if (givenTableName != null) 
			 input.setText(givenTableName);
		 
		 // OK Action => Create new Column
		 alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			 public void onClick(DialogInterface dialog, int whichButton) {
				 String newTableName = input.getText().toString().trim();
				
				 if (newTableName == null || newTableName.equals("")) {
					// Table name is empty string
					toastTableNameError("Table name cannot be empty!");
					alertForNewTableName(isNewTable, tableType, tp, null);
			 	 } else {
			 		 if (isNewTable) 
			 			 addTable(newTableName, tableType);
			 		 else
			 		     tp.setDisplayName(newTableName);
			 		 Log.d("TM", "got here");
			 		 refreshList();
				 }
			 }
		 });

		 // Cancel Action
		 alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			 public void onClick(DialogInterface dialog, int whichButton) {
				 // Canceled.
			 }
		 });

		 alert.show();
	 }
	 
	 private void toastTableNameError(String msg) {
		 Context context = getApplicationContext();
		 Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
		 toast.show();
	 }
	 
	 private void addTable(String tableName, int tableType) {
	     String dbTableName =
	             TableProperties.createDbTableName(dbh, tableName);
	     TableProperties tp = TableProperties.addTable(dbh, dbTableName,
	             tableName, tableType);
	 }
	 
 }
