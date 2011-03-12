package yoonsung.odk.spreadsheet.Activity;

import java.util.ArrayList;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.Database.ColumnProperty;
import yoonsung.odk.spreadsheet.Database.DataTable;
import yoonsung.odk.spreadsheet.Database.TableProperty;
import yoonsung.odk.spreadsheet.Library.TouchListView;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

/*
 * Activity that allows users to change table properties 
 * such as colum orders, prime, and sort by. Also, users
 * can create new columns and remove columns.
 * 
 * @Author : YoonSung Hong (hys235@cs.washington.edu)
 */
public class ColumnManager extends ListActivity {
	
	// Menu IDs
	public static final int SET_AS_PRIME = 1;
	public static final int SET_AS_ORDER_BY = 2;
	public static final int REMOVE_THIS_COLUMN = 3;
	public static final int SET_AS_NONPRIME = 4;
	
	public static final int ADD_NEW_COL = 0;
	
	// For Drop & Drop Menu
	private IconicAdapter adapter;
	
	// Private Fields
	private String tableID;
	private TableProperty tp;
	private DataTable data;
	private ArrayList<String> colOrder;
	private String currentCol;
	private ColumnProperty cp;
	
	// Initialize fields.
	private void init() {
		this.tp = new TableProperty(tableID);
		this.data = new DataTable(tableID);
		this.cp = new ColumnProperty(tableID);
		this.colOrder = tp.getColOrderArrayList(); 
		
		//updatePrimeOrderbyInfo();
	}
	
	/*
	private void updatePrimeOrderbyInfo() {
		// Set prime and sort by information
		//TextView primeTV = (TextView)findViewById(R.id.prime_tv);
		//primeTV.setText(tp.getPrime());
		//TextView sortbyTV = (TextView)findViewById(R.id.sortby_tv);
		//sortbyTV.setText(tp.getSortBy());
	}
	*/
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.col_manager);
		
		// Set title of activity
		setTitle("ODK Tables > Column Manager");
		
		// Retrieve Intent
		this.tableID = getIntent().getStringExtra("tableID");
		
		// Initialize
		init();
		
		// Add new column button
		//createAddNewColumnButton();
				
		// Drag & Drop List
		createDragAndDropList();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		// Refresh column order (for saftety)
		tp.setColOrder(getNewColOrderFromList());
		colOrder = tp.getColOrderArrayList();
	}
	
	@Override 
	public void onResume() {
		super.onResume();
	
		// Refresh column order
		colOrder = tp.getColOrderArrayList();
		
		// Create new Drag & Drop List
		createDragAndDropList();
	}
	
	// Retrieve current order of Drag & Drop List.
	private ArrayList<String> getNewColOrderFromList() {
		ArrayList<String> newOrder = new ArrayList<String>();
		for (int i = 0; i < adapter.getCount(); i++) {
			newOrder.add(adapter.getItem(i));
		}
		return newOrder;
	}
	
	/*
	// Button that allows user to add a new column.
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
	*/
	
	// Create a Drag & Drop List view.
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
				TextView tv = (TextView) v.findViewById(R.id.row_label);
				
				// Name of column from the selected item
				String name = tv.getText().toString();
				
				// Load Column Property Manger with this column name
				loadColumnPropertyManager(name);
			}
		});
		
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ADD_NEW_COL, 0, "Add New Column");
        return true;
    }
	
	@Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        
    	// HANDLES DIFFERENT MENU OPTIONS
    	switch(item.getItemId()) {
    	case ADD_NEW_COL:
    		alertForNewColumnName();
    		return true;
    	case SET_AS_PRIME:
	    	cp.setIsIndex(currentCol, true); 
	    	onResume();
	    	return true;
    	case SET_AS_NONPRIME:
	    	cp.setIsIndex(currentCol, false); 
	    	onResume();
    		return true;
	    case SET_AS_ORDER_BY:
	    	tp.setSortBy(currentCol); 
	    	onResume();
	    	return true;	
	    case REMOVE_THIS_COLUMN:
	    	// Drop the column from 'data' table
	    	data.dropColumn(currentCol);
	    	
	    	// New column order after the drop
	    	colOrder.remove(currentCol);
	    	tp.setColOrder(colOrder);
	    	
	    	// Update changes in other tables
	    	// To be done
	    	
	    	// Resume UI
	    	onResume();
 	    	return true;
    	}
    	return true;
	}
		
	// Load Column Property Manager Activity.
	private void loadColumnPropertyManager(String name) {
		Intent cpm = new Intent(this, PropertyManager.class);
		cpm.putExtra("colName", name);
		cpm.putExtra("tableID", tableID);
		startActivity(cpm);
	}
	
	// Ask for a new column name.
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
				colName = colName.trim();
				
				// if not, add a new column
				if (!data.isColumnExist(colName)) {
					
					// Create new column
					data.addNewColumn(colName);
					
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
			
			// Current Position in the List
			final int currentPosition = position;
			String currentColName = colOrder.get(position);
			
			// Register name of colunm at each row in the list view
			TextView label = (TextView)row.findViewById(R.id.row_label);		
			label.setText(currentColName);
			
			// Register ext info for columns
			TextView ext = (TextView)row.findViewById(R.id.row_ext);
			String extStr = "";
			if (cp.getIsIndex(currentColName)) {
				extStr += "Index Column";
			} else if (currentColName.equals(tp.getSortBy())) {
				extStr += "Order-By Column";
			}
			ext.setText(extStr);
			
			// Edit column properties
			ImageView edit = (ImageView)row.findViewById(R.id.row_options);
			edit.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {	
				@Override
				public void onCreateContextMenu(ContextMenu menu, View v,
						ContextMenuInfo menuInfo) {
					
					// Current column selected
					currentCol = colOrder.get(currentPosition);
					
					// Options for each item on the list
					if(cp.getIsIndex(currentCol)) {
						menu.add(0, SET_AS_NONPRIME, 0,
								"Set as Non-Index Column");
					} else {
						menu.add(0, SET_AS_PRIME, 0, "Set as Index Column");
					}
					menu.add(0, SET_AS_ORDER_BY, 0, "Set Order-By Column");
					menu.add(0, REMOVE_THIS_COLUMN, 0, "Remove This Column");
				}
			});
			return(row);
		}
	}

}
