package com.yoonsung.spreadsheetsms;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ColumnManager extends ListActivity {
	
	public static final int SET_AS_PRIME = 0;
	public static final int SET_AS_ORDER_BY = 1;
	public static final int REMOVE_THIS_COLUMN = 2;
	
	private DBIO db = null;
	
	private IconicAdapter adapter = null;
	private TableProperty tp = null;
	private ArrayList<String> colOrder;
	
	private String currentCol;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.col_manager);
		
		// DB 
		db = new DBIO();
		
		// Add new column button
		createAddNewColumnButton();
			
		// Get Column Order
		tp = new TableProperty();
		colOrder = tp.getColOrder();
		
		// Drag & Drop List
		createDragAndDropList();
		
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		// Update Column Order to DB
		ArrayList<String> newOrder = new ArrayList<String>();
		for (int i = 0; i < adapter.getCount(); i++) {
			newOrder.add(adapter.getItem(i));
		}
		tp.setColOrder(newOrder);
	}
	
	@Override 
	public void onResume() {
		super.onResume();
	
		// Refresh
		tp.reload();
		colOrder = tp.getColOrder();
		
		// Create new Drag & Drop List
		createDragAndDropList();
	}
	
	private void createAddNewColumnButton() {
		// Add column button
		RelativeLayout addCol = (RelativeLayout)findViewById(R.id.add_column_button);
		addCol.setClickable(true);
		
		// Add column button clicked
		addCol.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Ask a new column name
				alertForNewColumnName();
			}
		});
	}
	
	private void createDragAndDropList() {
		// Registration
		adapter = new IconicAdapter();
		setListAdapter(adapter);
		
		// Configuration
		TouchListView tlv = (TouchListView)getListView();
		tlv.setDropListener(onDrop);
		tlv.setRemoveListener(onRemove);
		
		// Item clicked in the Drag & Drop list
		tlv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView adView, View view,
										int position, long id) {
				// Selected item in the list
				View v = adapter.getView(position, null, null);
				TextView tv = (TextView) v.findViewById(R.id.label);
				
				// Name of column from the selected item
				String name = tv.getText().toString();
				
				// Load Column Property Manger with this column name
				loadColumnPropertyManager(name);
			}
		});
		
		// Context menu for items in Drag & Drop list
		tlv.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {	
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
									
				menu.add(0, SET_AS_PRIME, 0, "Set As Prime");
				menu.add(0, SET_AS_ORDER_BY, 0, "Set AS Order By");
				menu.add(0, REMOVE_THIS_COLUMN, 0, "Remove This Column");
			}
		});
		
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		String itemTitle = (String) item.getTitle();
		Log.e("currentCol", currentCol);
		switch(item.getItemId()) {
	    case SET_AS_PRIME:
	    	tp.setPrime(currentCol); 
			tp.reload();
	    	return true;
	    case SET_AS_ORDER_BY:
	    	tp.setSortBy(currentCol); 
	    	tp.reload();
	    	return true;	
	    case REMOVE_THIS_COLUMN:
	    	// Drop the corresponding column in data table
	    	dropAColumn(currentCol);
	    	
	    	// New column order afer the drop
	    	colOrder.remove(currentCol);
	    	tp.setColOrder(colOrder);
	    	
	    	tp.reload();
 	    	return true;
	    }
	    return super.onContextItemSelected(item);
	}
	
	// Load Column Property Manager Activity
	private void loadColumnPropertyManager(String name) {
		Intent cpm = new Intent(this, ColumnPropertyManager.class);
		cpm.putExtra("colName", name);
		startActivity(cpm);
	}
	
	// Ask for a new column name
	private void alertForNewColumnName() {
		
		// Prompt an alert box
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Name of New Column");
	
		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);

		// OK Action => Create new Column
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String colName = input.getText().toString();
				
				// if not, add a new column
				if (!isColumnExist(colName)) {
					
					// Create new column
					createNewColumn(colName);
					
					// Update Column Property
					colOrder.add(colName);
					tp.setColOrder(colOrder);
					
					// Load Column Property Manger
					loadColumnPropertyManager(colName);
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

	private boolean isColumnExist(String colName) {
		// Get database
		SQLiteDatabase con = db.getConn();
		Cursor cs = con.rawQuery("SELECT * FROM data", null);
		
		// Check if such a column exist?
		for (int i = 0; i < cs.getColumnCount(); i++) {
			if (cs.getColumnName(i).equals(colName))  {
				// Existing
				cs.close();
				con.close();
				return true;
			}
		}
		
		// Not Existing
		cs.close();
		con.close();
		return false;
	}
	
	private void createNewColumn(String colName) {
		// Add new column data table
		SQLiteDatabase con = db.getConn();
		con.execSQL("ALTER TABLE data ADD " + colName + " TEXT");
		con.close();
	}
	
	private void dropAColumn(String colName) {
		String originalTable = "data";
		String backupTable = "baktable";
		String SelColumns = "rowID" + dropAColumnHelper(colName);
		String InsColumns = "rowID INTEGER PRIMARY KEY ASC" + dropAColumnHelper(colName);
		
		Log.e("SelCol", SelColumns);
		Log.e("InsCol", InsColumns);
		
		SQLiteDatabase con = db.getConn();
		try {
			con.beginTransaction();
			con.execSQL("CREATE TEMPORARY TABLE " + backupTable + "(" + InsColumns + ")");
			con.execSQL("INSERT INTO " + backupTable + " SELECT " + SelColumns + " FROM " + originalTable);
			con.execSQL("DROP TABLE " + originalTable);
			con.execSQL("CREATE TABLE " + originalTable + "(" + InsColumns + ")");
			con.execSQL("INSERT INTO " + originalTable + " SELECT " + SelColumns + " FROM " + backupTable);
			con.execSQL("DROP TABLE " + backupTable);
			con.setTransactionSuccessful();
		} catch (Exception e) {
		} finally {
			con.endTransaction();
			con.close();
		}
		Log.e("SQLTransaction", "Done");
	}
	
	private String dropAColumnHelper(String colName) {
		ArrayList<String> tempColOrder = tp.getColOrder();
		tempColOrder.remove(colName);
		
		String result = "";
		for (int i = 0; i < tempColOrder.size(); i++) {
			result += ", " + tempColOrder.get(i);
		}
		
		return result;
	}
	
		
	//								DO	NOT TOUCH BELOW								 //
	// ------------------------------------------------------------------------------//
	
	// Drag & Drop 
	private TouchListView.DropListener onDrop=new TouchListView.DropListener() {
		@Override
		public void drop(int from, int to) {
				String item = adapter.getItem(from);
				
				adapter.remove(item);
				adapter.insert(item, to);
		}
	};
	
	// Drag & Drop
	private TouchListView.RemoveListener onRemove=new TouchListView.RemoveListener() {
		@Override
		public void remove(int which) {
				adapter.remove(adapter.getItem(which));
		}
	};
	
	// Drag & Drop List Adapter
	class IconicAdapter extends ArrayAdapter<String> {
		IconicAdapter() {
			super(ColumnManager.this, R.layout.touchlistview_row2, colOrder);
		}

		public View getView(int position, View convertView,
												ViewGroup parent) {
			View row = convertView;
			
			if (row == null) {													
				LayoutInflater inflater=getLayoutInflater();
				
				row = inflater.inflate(R.layout.touchlistview_row2, parent, false);
			}
			
			TextView label = (TextView)row.findViewById(R.id.label);
			
			final int currentPosition = position;
			label.setOnLongClickListener(new View.OnLongClickListener() {
				
				@Override
				public boolean onLongClick(View v) {
					// TODO Auto-generated method stub
					currentCol = colOrder.get(currentPosition);
					return false;
				}
			});
			
			label.setText(colOrder.get(position));
			return(row);
		}
	}
	
}
