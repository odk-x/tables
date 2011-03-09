package yoonsung.odk.spreadsheet.Activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.Database.ColumnProperty;
import yoonsung.odk.spreadsheet.Database.DBIO;
import yoonsung.odk.spreadsheet.Database.DataTable;
import yoonsung.odk.spreadsheet.Database.TableList;
import yoonsung.odk.spreadsheet.Database.TableProperty;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class TableManager extends ListActivity {

	public static final int SET_DEFAULT_TABLE = 0;
	public static final int CHANGE_TABLE_NAME = 1;
	public static final int ADD_NEW_TABLE     = 2;
	public static final int REMOVE_TABLE      = 3;
	
	private static String[] from = new String[] {"label", "ext"};
	private static int[] to = new int[] { android.R.id.text1, android.R.id.text2 };
	 	
	private DBIO db;
	private TableList tl;
	
	private SimpleAdapter arrayAdapter;
	
	 @Override
	 public void onCreate(Bundle savedInstanceState) {
		 super.onCreate(savedInstanceState);
		 this.db = new DBIO();
		 this.tl = new TableList();
		 
		 // Set title of activity
		 setTitle("ODK Tables > Table Manager");
		 
		 // Set Content View
		 setContentView(R.layout.white_list);
		 
		 HashMap<String, String> tableListTmp = tl.getTableList();
		 boolean loadError = getIntent().getBooleanExtra("loadError", false);
		 if (loadError && tableListTmp.size() < 1) {
			List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();
			HashMap<String, String> temp = new HashMap<String, String>();
			temp.put("label", "Click menu to add new table");
			fillMaps.add(temp);
			arrayAdapter = new SimpleAdapter(this, fillMaps, R.layout.white_list_row, from, to);
			setListAdapter(arrayAdapter);
		 } else {
			 refreshList();
		 }
	 }
	 
	 public void refreshList() {
		 registerForContextMenu(getListView());
		 
		 HashMap<String, String> tableList = tl.getTableList();
		 Log.e("TableList", tableList.toString());
		 
		 List<HashMap<String, String>> fillMaps = new ArrayList<HashMap<String, String>>();
		 for (String tableID : tableList.keySet()) {
			 String tableName = tableList.get(tableID);
			 SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
			 String defTableID = settings.getString("ODKTables:tableID", "");
			 String defTableName = "";
			 if (defTableID != null && !defTableID.equals("")) {
				 defTableName = tl.getTableName(defTableID);
			 }
			 HashMap<String, String> map = new HashMap<String, String>();
			 map.put("label", tableName);
			 if (tableName.equals(defTableName)) {
				 map.put("ext", "Default Table");
			 } else {
				 map.put("ext", "");
			 }
			 fillMaps.add(map);
		 }
		 	 
		 // fill in the grid_item layout
		 arrayAdapter = new SimpleAdapter(this, fillMaps, R.layout.white_list_row, from, to); 
		 setListAdapter(arrayAdapter);
		
		 ListView lv = getListView();
		 lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				
			 	@Override
				public void onItemClick(AdapterView adView, View view,
											int position, long id) {
					
			 		HashMap<String, String> current = (HashMap)arrayAdapter.getItem(position);
			 		String tableName = current.get("label");
			 		
					// Load Selected Table
					TableList tl = new TableList();
					int tableID = tl.getTableID(tableName);
					Log.e("Selected Table", tableName + " " + tableID);
					loadSelectedTable(Integer.toString(tableID));
				}
		 });
	 } 
	 
	 private void loadSelectedTable(String tableID) {
		 Intent i = new Intent(this, SpreadSheet.class);
		 i.putExtra("tableID", tableID);
		 startActivity(i);
	 }
	 
	 @Override
	 public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		 super.onCreateContextMenu(menu, v, menuInfo);
        
		 menu.add(0, SET_DEFAULT_TABLE, 0, "Set as Default Table");
		 menu.add(0, CHANGE_TABLE_NAME, 1, "Change Table Name");
		 menu.add(0, REMOVE_TABLE, 2, "Remove the Table");
	 }
	 
	 @Override
	 public boolean onContextItemSelected(MenuItem item) {
	 
		 AdapterView.AdapterContextMenuInfo info= (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		 HashMap<String, String> sel = (HashMap)arrayAdapter.getItem(info.position);
		 
		 String tableName =  sel.get("label");
		 int tableID   = tl.getTableID(tableName);
		 
		 switch (item.getItemId()) {
		 case SET_DEFAULT_TABLE:
			 setDefaultTable(Integer.toString(tableID));
			 refreshList();
			 return true;
		 case CHANGE_TABLE_NAME:
			 // TO be Done
			 alertForNewTableName(Integer.toString(tableID));
			 return true; 
		 case REMOVE_TABLE:
			 // To be Done
			 removeTable(Integer.toString(tableID));
			 refreshList();
			 return true;
		 }
		 return(super.onOptionsItemSelected(item));
	 }
	 
	 // CREATE OPTION MENU
	 @Override
	 public boolean onCreateOptionsMenu(Menu menu) {
		 super.onCreateOptionsMenu(menu);
		 menu.add(0, ADD_NEW_TABLE, 0, "Add New Table");
		 return true;
	 }
    
	 // HANDLE OPTION MENU
	 @Override
	 public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	
		 Log.d("timing", "menu item selected");
        
		 // HANDLES DIFFERENT MENU OPTIONS
		 switch(item.getItemId()) {
		 case ADD_NEW_TABLE:
			 alertForNewTableName();
			 return true;
		 }
    	
		 return super.onMenuItemSelected(featureId, item);
	 }
	 
	 // Ask for a new column name.
	 private void alertForNewTableName() {
		
		 // Prompt an alert box
		 AlertDialog.Builder alert = new AlertDialog.Builder(this);
		 alert.setTitle("Name of New Table");
	
		 // Set an EditText view to get user input 
		 final EditText input = new EditText(this);
		 alert.setView(input);

		 // OK Action => Create new Column
		 alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			 public void onClick(DialogInterface dialog, int whichButton) {
				 String newTableName = input.getText().toString().trim();
				 addTable(newTableName);
				 refreshList();
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
	 
	 // Ask for a new column name.
	 private void alertForNewTableName(final String tableID) {
		
		 // Prompt an alert box
		 AlertDialog.Builder alert = new AlertDialog.Builder(this);
		 alert.setTitle("Name of New Table");
	
		 // Set an EditText view to get user input 
		 final EditText input = new EditText(this);
		 alert.setView(input);

		 // OK Action => Create new Column
		 alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			 public void onClick(DialogInterface dialog, int whichButton) {
				 String newTableName = input.getText().toString();
				 changeTableName(tableID, newTableName);
				 refreshList();
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

	 private void setDefaultTable(String tableID) {
		// Share preference editor
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putString("ODKTables:tableID", tableID);
	    editor.commit();
	 }
	 
	 private void addTable(String tableName) { 
		// Register new table in TableList
		 String msg = tl.registerNewTable(tableName);
		 // No duplicate table exist?
		 if (msg == null) {
			 // Create a new table in the database
			 createNewDataTable(tableName);
		 } else {
			 // Name already exist
			 Toast.makeText(this, msg, Toast.LENGTH_LONG);
		 }
		  
	 }
	 
	 private void createNewDataTable(String tableName) {
		 SQLiteDatabase con = db.getConn();
		 con.execSQL("CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
	                + DataTable.DATA_ROWID + " INTEGER PRIMARY KEY,"
	                + DataTable.DATA_PHONE_NUMBER_IN + " TEXT,"
	                + DataTable.DATA_TIMESTAMP + " TEXT"
	                + ");");
		 con.close();
	 }
	 
	 private void removeTable(String tableID) {
		 String tableName = tl.getTableName(tableID);
		 
		 // Remove Actual Table
		 SQLiteDatabase con = db.getConn();
		 try {
			 con.execSQL("DROP TABLE `" + tableName + "`;");
			 con.close();
		 } catch (Exception e) {
			 Log.e("TableManager", "Error While Drop a Table");
		 }
		 
		 // Unregister Table from TableList
		 tl.unregisterTable(tableID);
		 
		 // Clean up Table Property
		 TableProperty tp = new TableProperty(tableID);
		 tp.removeAll();
		 
		 // Clean up Column Property
		 ColumnProperty cp = new ColumnProperty(tableID);
		 cp.removeAll();
	 }
	 
	 private void changeTableName(String tableID, String newTableName) {
		 String tableName = tl.getTableName(tableID);
		 
		 // Change actual table
		 SQLiteDatabase con = db.getConn();
		 con.execSQL("ALTER TABLE `"
				  	+ tableName
				  	+ "` RENAME TO `"
				  	+ newTableName
				  	+ "`;");
		 
		 // Change on TableList
		 ContentValues cv = new ContentValues();
		 cv.put(TableList.TABLE_NAME, newTableName);
		 con.update(TableList.TABLE_LIST, cv, TableList.TABLE_ID+" = "+tableID, null);
		
		 con.close();
	 }
	 
 }
