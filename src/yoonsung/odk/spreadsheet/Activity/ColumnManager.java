package yoonsung.odk.spreadsheet.Activity;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.Library.TouchListView;
import yoonsung.odk.spreadsheet.data.ColumnProperties;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.TableProperties;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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
import android.widget.Toast;

/*
 * Activity that allows users to change table properties 
 * such as colum orders, prime, and sort by. Also, users
 * can create new columns and remove columns.
 * 
 * @Author : YoonSung Hong (hys235@cs.washington.edu)
 */
public class ColumnManager extends ListActivity {
	
    public static final String INTENT_KEY_TABLE_ID = "tableId";
    
	// Menu IDs
	public static final int SET_AS_PRIME = 1;
	public static final int SET_AS_ORDER_BY = 2;
	public static final int REMOVE_THIS_COLUMN = 3;
	public static final int SET_AS_NONPRIME = 4;
	
	public static final int ADD_NEW_COL = 0;
	
	// For Drop & Drop Menu
	private IconicAdapter adapter;
	
	// Private Fields
	private long tableId;
	private TableProperties tp;
	private ColumnProperties[] cps;
	private String[] columnOrder;
	private String currentCol;
	
	// Initialize fields.
	private void init() {
	    tableId = getIntent().getLongExtra(INTENT_KEY_TABLE_ID, -1);
	    DbHelper dbh = new DbHelper(this);
	    tp = TableProperties.getTablePropertiesForTable(dbh, tableId);
	    cps = tp.getColumns();
	    columnOrder = tp.getColumnOrder();
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
		
		// Initialize
		init();
		
		// Add new column button
		//createAddNewColumnButton();
				
		// Drag & Drop List
		createDragAndDropList();
	}
	
	@Override 
	public void onResume() {
		super.onResume();
	
		// Refresh column order
		columnOrder = tp.getColumnOrder();
		
		// Create new Drag & Drop List
		createDragAndDropList();
	}
	
	// Retrieve current order of Drag & Drop List.
	private String[] getNewColOrderFromList() {
	    String[] order = new String[adapter.getCount()];
		for (int i = 0; i < adapter.getCount(); i++) {
		    order[i] = adapter.getItem(i);
		}
		return order;
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
    		alertForNewColumnName(null);
    		return true;
    	case SET_AS_PRIME:
    	    String[] aoldPrimes = tp.getPrimeColumns();
    	    String[] anewPrimes = new String[aoldPrimes.length + 1];
    	    for (int i = 0; i < aoldPrimes.length; i++) {
    	        anewPrimes[i] = aoldPrimes[i];
    	    }
    	    anewPrimes[aoldPrimes.length] = currentCol;
    	    tp.setPrimeColumns(anewPrimes); 
	    	onResume();
	    	return true;
    	case SET_AS_NONPRIME:
            String[] roldPrimes = tp.getPrimeColumns();
            String[] rnewPrimes = new String[roldPrimes.length - 1];
            int index = 0;
            for (int i = 0; i < roldPrimes.length; i++) {
                if (roldPrimes[i].equals(currentCol)) {
                    continue;
                }
                rnewPrimes[index] = roldPrimes[i];
                index++;
            }
            tp.setPrimeColumns(rnewPrimes); 
	    	onResume();
    		return true;
	    case SET_AS_ORDER_BY:
	    	tp.setSortColumn(currentCol); 
	    	onResume();
	    	return true;	
	    case REMOVE_THIS_COLUMN:
	    	// Drop the column from 'data' table
	        tp.deleteColumn(currentCol);
	    	
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
		cpm.putExtra(PropertyManager.INTENT_KEY_TABLE_ID, tableId);
        cpm.putExtra(PropertyManager.INTENT_KEY_COLUMN_NAME, name);
		startActivity(cpm);
	}
	
	// Ask for a new column name.
	private void alertForNewColumnName(String givenColName) {
		
		// Prompt an alert box
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Name of New Column");
	
		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);
		if (givenColName != null) 
			input.setText(givenColName);

		// OK Action => Create new Column
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String colName = input.getText().toString();
				colName = colName.trim();
				
				// if not, add a new column
				if (tp.getColumnIndex(colName) < 0) {
					if (colName == null || colName.equals("")) {
						toastColumnNameError("Column name cannot be empty!");
						alertForNewColumnName(null);
					} else if (colName.contains(" ")) {
						toastColumnNameError("Column name cannot contain spaces!");
						alertForNewColumnName(colName.replace(' ', '_'));
					} else {
						// Create new column
					    tp.addColumn(colName);
					    cps = tp.getColumns();
					    columnOrder = tp.getColumnOrder();
						
						// Load Column Property Manger
					    loadColumnPropertyManager(colName);
					}
				} else {
					toastColumnNameError(colName + " is already existing column!");
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
	
	private void toastColumnNameError(String msg) {
		 Context context = getApplicationContext();
		 Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
		 toast.show();
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
			super(ColumnManager.this, R.layout.touchlistview_row2, columnOrder);
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
			String currentColName = columnOrder[position];
			
			// Register name of colunm at each row in the list view
			TextView label = (TextView)row.findViewById(R.id.row_label);		
			label.setText(currentColName);
			
			// Register ext info for columns
			TextView ext = (TextView)row.findViewById(R.id.row_ext);
			String extStr = "";
			if (tp.isColumnPrime(currentColName)) {
				extStr += "Collection Column";
			} else if (currentColName.equals(tp.getSortColumn())) {
				extStr += "Sort Column";
			}
			ext.setText(extStr);
			
			// Edit column properties
			ImageView edit = (ImageView)row.findViewById(R.id.row_options);
			edit.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {	
				@Override
				public void onCreateContextMenu(ContextMenu menu, View v,
						ContextMenuInfo menuInfo) {
					
					// Current column selected
					currentCol = columnOrder[currentPosition];
					
					// Options for each item on the list
					if(tp.isColumnPrime(currentCol)) {
						menu.add(0, SET_AS_NONPRIME, 0,
								"Unset as Collection View Based on This");
					} else {
						menu.add(0, SET_AS_PRIME, 0, "Set as Collection View Based on This");
					}
					menu.add(0, SET_AS_ORDER_BY, 0, "Set as Sort Column");
					menu.add(0, REMOVE_THIS_COLUMN, 0, "Delete this Column");
				}
			});
			return(row);
		}
	}

}
