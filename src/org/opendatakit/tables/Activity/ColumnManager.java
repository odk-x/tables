/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.Activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.opendatakit.tables.R;
import org.opendatakit.tables.Library.TouchListView;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.TableProperties;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.GpsStatus.Listener;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
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
	private String tableId;
	private TableProperties tp;
	private ColumnProperties[] cps;
	private final List<String> columnOrder = new LinkedList<String>();
	private String currentCol;
	
	// Initialize fields.
	private void init() {
	    tableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
	    DbHelper dbh = DbHelper.getDbHelper(this);
	    tp = TableProperties.getTablePropertiesForTable(dbh, tableId,
	        KeyValueStore.Type.ACTIVE);
	    cps = tp.getColumns();
	    columnOrder.clear();
	    for ( String s : tp.getColumnOrder() ) {
	    	columnOrder.add(s);
	    }
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
	}
	
	@Override 
	public void onResume() {
		super.onResume();

		init();
		
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
				// Load Column Property Manger with this column name
				loadColumnPropertyManager(cps[position].getColumnDbName());
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
	  
	  AlertDialog newColumnAlert;
		
		// Prompt an alert box
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Name of New Column");
		
	
		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		input.setFocusableInTouchMode(true);
		input.setFocusable(true);
		input.requestFocus();
		// adding the following line
		//((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
      //.showSoftInput(input, InputMethodManager.SHOW_FORCED);
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
					    ColumnProperties cp = tp.addColumn(colName);
					    cps = tp.getColumns();
					    columnOrder.clear();
					    for ( String s : tp.getColumnOrder() ) {
					    	columnOrder.add(s);
					    }
					    adapter.notifyDataSetChanged();						
						// Load Column Property Manger
					    loadColumnPropertyManager(cp.getColumnDbName());
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

		newColumnAlert = alert.create();
		newColumnAlert.getWindow().setSoftInputMode(WindowManager.LayoutParams.
		    SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		newColumnAlert.show();
		//alert.show();
	}
	
	private void toastColumnNameError(String msg) {
		 Context context = getApplicationContext();
		 Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
		 toast.show();
	}
    
    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        finish();
    }
		
	//								DO	NOT TOUCH BELOW		(says who?--not sure)	 //
	// ----------------------------------------------------------------------//
	
	// Drag & Drop 
	private TouchListView.DropListener onDrop=new TouchListView.DropListener() {
		@Override
		public void drop(int from, int to) {	
		  String item = columnOrder.get(from);
		  columnOrder.remove(from);
		  columnOrder.add(to, item);
		  String[] newOrder = new String[columnOrder.size()];
		  for (int i = 0; i < columnOrder.size(); i++) {
		    newOrder[i] = columnOrder.get(i);
		  }
		  tp.setColumnOrder(newOrder);
		  columnOrder.clear();
		  for (String s : tp.getColumnOrder()) {
		    columnOrder.add(s);
		  }
	     // have to call this so that displayName refers to the correct column
	     DbHelper dbh = DbHelper.getDbHelper(ColumnManager.this);
	     tp = TableProperties.getTablePropertiesForTable(dbh, tableId,
	         KeyValueStore.Type.ACTIVE);
	     cps = tp.getColumns();
		  adapter.notifyDataSetChanged();
		}
	};
	
	// Drag & Drop
	private TouchListView.RemoveListener onRemove=new TouchListView.RemoveListener() {
		@Override
		public void remove(int which) {
			String item = adapter.getItem(which);
			if ( item != null ) {
				adapter.remove(item);
			}
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
			String currentColName = columnOrder.get(position);
			
			// Register name of colunm at each row in the list view
			TextView label = (TextView)row.findViewById(R.id.row_label);		
			label.setText(cps[position].getDisplayName());
			
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
					currentCol = columnOrder.get(currentPosition);
					
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
