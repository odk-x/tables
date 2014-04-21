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
package org.opendatakit.tables.activities;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.opendatakit.common.android.data.ColumnProperties;
import org.opendatakit.common.android.data.ColumnType;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.R;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.tables.views.TouchListView;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/*
 * Activity that allows users to change table properties
 * such as colum orders, group-by, and sort-by. Also, users
 * can create new columns and remove columns.
 *
 * @Author : YoonSung Hong (hys235@cs.washington.edu)
 */
public class ColumnManager extends ListActivity {
  private static final String t = "ColumnManager";

	private static final char UNDERSCORE_CHAR = '_';
	private static final char SPACE_CHAR = ' ';

	private static final String SPACE = " ";

	private static final String EMPTY_STRING = "";

	// Menu IDs
	public static final int SET_AS_GROUP_BY = 1;
	public static final int SET_AS_ORDER_BY = 2;
	public static final int REMOVE_THIS_COLUMN = 3;
	public static final int SET_AS_NON_GROUP_BY = 4;

	public static final int ADD_NEW_COL = 0;

	// For Drop & Drop Menu
	private IconicAdapter adapter;

	// Private Fields
	private String appName;
	private String tableId;
	private TableProperties tp;
	private ColumnProperties[] cps;
	private final List<String> columnOrder = new LinkedList<String>();
	private String currentCol;

	// Initialize fields.
	private void init() {
	   appName = getIntent().getStringExtra(Controller.INTENT_KEY_APP_NAME);
	   if ( appName == null ) {
	     appName = TableFileUtils.getDefaultAppName();
	   }
		tableId = getIntent().getStringExtra(Controller.INTENT_KEY_TABLE_ID);
		tp = TableProperties.getTablePropertiesForTable(this, appName, tableId);
		// We need to order the ColumnProperties appropriately
		this.cps = new ColumnProperties[tp.getNumberOfDisplayColumns()];
		columnOrder.clear();
		for ( int i = 0 ; i < tp.getNumberOfDisplayColumns() ; ++ i) {
			cps[i] = tp.getColumnByIndex(i);
			columnOrder.add(cps[i].getElementKey());
		}
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.col_manager);
		// Set title of activity
		setTitle(getString(R.string.column_manager));
		// set the app icon as an action to go home
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
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
		tlv.setOnItemClickListener(new TouchListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adView, View view,
					int position, long id) {
				// Load Column Property Manger with this column name
				loadColumnPropertyManager(cps[position].getElementKey());
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem addItem = menu.add(0, ADD_NEW_COL, 0, getString(R.string.add_column));
		addItem.setIcon(R.drawable.content_new);
		addItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		// HANDLES DIFFERENT MENU OPTIONS
		switch(item.getItemId()) {
		case ADD_NEW_COL:
			alertForNewColumnName(null);
			return true;
		case android.R.id.home:
	      Intent i = new Intent(this, TableManager.class);
	      i.putExtra(Controller.INTENT_KEY_APP_NAME, appName);
			startActivity(i);
			return true;
		}
		return false;
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		switch(item.getItemId()) {
		case SET_AS_GROUP_BY: {
			List<String> anewGroupBys = tp.getGroupByColumns();
			anewGroupBys.add(currentCol);
         try {
           SQLiteDatabase db = tp.getWritableDatabase();
           db.beginTransaction();
           try {
             tp.setGroupByColumns(db, anewGroupBys);
             db.setTransactionSuccessful();
           } finally {
             db.endTransaction();
             db.close();
           }
         } catch ( Exception e ) {
           e.printStackTrace();
           Log.e(t, "Exception changing group-by columns: " + e.toString());
           Toast.makeText(getParent(), "Error while revising group-by columns", Toast.LENGTH_LONG).show();
         }
			onResume();
		}
			return true;
		case SET_AS_NON_GROUP_BY: {
			List<String> rnewGroupBys = tp.getGroupByColumns();
			rnewGroupBys.remove(currentCol);
         try {
           SQLiteDatabase db = tp.getWritableDatabase();
           db.beginTransaction();
           try {
             tp.setGroupByColumns(db, rnewGroupBys);
             db.setTransactionSuccessful();
           } finally {
             db.endTransaction();
             db.close();
           }
         } catch ( Exception e ) {
           e.printStackTrace();
           Log.e(t, "Exception changing group-by columns: " + e.toString());
           Toast.makeText(getParent(), "Error while revising group-by columns", Toast.LENGTH_LONG).show();
         }
			onResume();
		}
			return true;
		case SET_AS_ORDER_BY: {
        try {
            SQLiteDatabase db = tp.getWritableDatabase();
            db.beginTransaction();
            try {
              tp.setSortColumn(db, currentCol);
              db.setTransactionSuccessful();
            } finally {
              db.endTransaction();
              db.close();
            }
        } catch ( Exception e ) {
          e.printStackTrace();
          Log.e(t, "Exception changing sort columns: " + e.toString());
          Toast.makeText(getParent(), "Error while changing sort column", Toast.LENGTH_LONG).show();
        }
			onResume();
		}
			return true;
		case REMOVE_THIS_COLUMN:
			// Drop the column from 'data' table
         AlertDialog confirmDeleteAlert;
         // Prompt an alert box
         AlertDialog.Builder alert =
         new AlertDialog.Builder(ColumnManager.this);
         alert.setTitle(getString(R.string.confirm_delete_column))
         .setMessage(getString(R.string.are_you_sure_delete_column,
             tp.getColumnByElementKey(currentCol).getDisplayName()));
         // OK Action => delete the column
         alert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
              try {
                tp.deleteColumn(currentCol);
              } catch ( Exception e ) {
                e.printStackTrace();
                Log.e(t, "Exception while deleting column: " + e.toString());
                Toast.makeText(getParent(), "Error while deleting column", Toast.LENGTH_LONG).show();
              }

              // Update changes in other tables
              // To be done
              // TODO: the above

              // Resume UI to refresh the list.
              onResume();
            }
         });

         // Cancel Action
         alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
               // Canceled.
            }
         });
         // show the dialog
         confirmDeleteAlert = alert.create();
         confirmDeleteAlert.show();

			return true;
		}
		return false;
	}

	// Load Column Property Manager Activity.
	private void loadColumnPropertyManager(String elementKey) {
		Intent cpm = new Intent(this, PropertyManager.class);
		cpm.putExtra(Controller.INTENT_KEY_APP_NAME, appName);
		cpm.putExtra(PropertyManager.INTENT_KEY_TABLE_ID, tableId);
      cpm.putExtra(PropertyManager.INTENT_KEY_ELEMENT_KEY, elementKey);
		startActivity(cpm);
	}

	// Ask for a new column name.
	private void alertForNewColumnName(String givenColName) {

		AlertDialog newColumnAlert;

		// Prompt an alert box
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
	    View view = getLayoutInflater().inflate(R.layout.message_with_text_edit_field_dialog, null);
	    alert.setView(view)
		.setTitle(R.string.add_column);

	    final TextView msg = (TextView) view.findViewById(R.id.message);
	    msg.setText(getString(R.string.name_of_new_column));

		// Set an EditText view to get user input
		final EditText input = (EditText) view.findViewById(R.id.edit_field);
		input.setFocusableInTouchMode(true);
		input.setFocusable(true);
		input.requestFocus();
		if (givenColName != null)
			input.setText(givenColName);

		// OK Action => Create new Column
		alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String colName = input.getText().toString();
				colName = colName.trim();

				// if not, add a new column
				if (tp.getColumnIndex(colName) < 0) {
					if (colName == null || colName.equals(EMPTY_STRING)) {
						toastColumnNameError(getString(R.string.error_empty_column_name));
						alertForNewColumnName(null);
					} else if (tp.getColumnByDisplayName(colName) != null) {
					  toastColumnNameError(getString(R.string.error_display_name_in_use_column_name, colName));
					  alertForNewColumnName(null);
					} else {
						// Create new column
					  ColumnProperties cp = tp.addColumn(colName, null, null,
					        ColumnType.STRING, null, true);
					  cps = new ColumnProperties[tp.getNumberOfDisplayColumns()];
					  columnOrder.clear();
					  for ( int i = 0 ; i < tp.getNumberOfDisplayColumns() ; ++ i) {
						cps[i] = tp.getColumnByIndex(i);
						columnOrder.add(cps[i].getElementKey());
					  }
					  adapter.notifyDataSetChanged();
					  // Load Column Property Manger
					  loadColumnPropertyManager(cp.getElementKey());
					}
				} else {
					toastColumnNameError(getString(R.string.error_in_use_column_name, colName));
				}
			}
		});

		// Cancel Action
		alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
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
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
	}

	@Override
	public void onBackPressed() {
		setResult(RESULT_OK);
		finish();
	}

	// Drag & Drop
	private TouchListView.DropListener onDrop=new TouchListView.DropListener() {
		@Override
		public void drop(int from, int to) {
			String item = columnOrder.get(from);
			columnOrder.remove(from);
			columnOrder.add(to, item);
			ArrayList<String> newOrder = new ArrayList<String>();
			for (int i = 0; i < columnOrder.size(); i++) {
				newOrder.add(columnOrder.get(i));
			}
			SQLiteDatabase db = tp.getWritableDatabase();
			try {
			  db.beginTransaction();
			  tp.setColumnOrder(db, newOrder);
			  db.setTransactionSuccessful();
			} catch ( Exception e ) {
			  Toast.makeText(getParent(), "Error while changing column ordering", Toast.LENGTH_LONG).show();
			} finally {
			  db.endTransaction();
			  db.close();
			}
			// reload from tp -- the ordering may or may not have changed
			columnOrder.clear();
			for ( int i = 0 ; i < tp.getNumberOfDisplayColumns() ; ++ i) {
				cps[i] = tp.getColumnByIndex(i);
				columnOrder.add(cps[i].getElementKey());
			}
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
			String extStr = EMPTY_STRING;
			if (tp.isGroupByColumn(currentColName)) {
				extStr += getString(R.string.group_by_column);
			} else if (currentColName.equals(tp.getSortColumn())) {
				extStr += getString(R.string.sort_column);
			}
			ext.setText(extStr);

			// clicking this image button opens a contextual menu
			// to edit column properties
			final ImageView edit = (ImageView)row.findViewById(R.id.row_options);

			edit.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
				@Override
				public void onCreateContextMenu(ContextMenu menu, View v,
						ContextMenuInfo menuInfo) {

					// Current column selected
					currentCol = columnOrder.get(currentPosition);

					// Options for each item on the list
					if(tp.isGroupByColumn(currentCol)) {
						menu.add(0, SET_AS_NON_GROUP_BY, 0,
								getString(R.string.unset_collection_view_column));
					} else {
						menu.add(0, SET_AS_GROUP_BY, 0, getString(R.string.set_collection_view_column));
					}
					menu.add(0, SET_AS_ORDER_BY, 0, getString(R.string.set_sort_column));
					menu.add(0, REMOVE_THIS_COLUMN, 0, getString(R.string.delete_column));
				}
			});

			edit.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					edit.showContextMenu();

				}});

			edit.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					return true;
				}});

			return(row);
		}
	}
}
