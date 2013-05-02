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
package org.opendatakit.tables.activities.graphs;

import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.activities.DisplayActivity;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.views.webkits.CustomGraphView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;


public class BarGraphDisplayActivity extends SherlockActivity
implements DisplayActivity {



	private static final String TAG = "GRAPHDisplayActivity";

	/**************************
	 * Strings necessary for the key value store.
	 **************************/
	/**
	 * The general partition in which table-wide ListDisplayActivity information
	 * is stored. An example might be the current list view for a table.
	 */
	public static final String KVS_PARTITION = "GraphDisplayActivity";
	/**
	 * The partition under which actual individual view information is stored.
	 * For instance if a user added a list view named "Doctor", the partition
	 * would be KVS_PARTITION_VIEWS, and all the keys relating to this view would
	 * fall within this partition and a particular aspect. (Perhaps the name
	 * "Doctor"?)
	 */
	public static final String KVS_PARTITION_VIEWS = KVS_PARTITION + ".views";

	/**
	 * This key holds the name of the list view. In the default aspect the idea
	 * is that this will then give the value of the aspect for which the default
	 * list view is set.
	 * <p>
	 * E.g. partition=KVS_PARTITION, aspect=KVS_ASPECT_DEFAULT,
	 * key="KEY_LIST_VIEW_NAME", value="My Custom List View" would mean that
	 * "My Custom List View" was an aspect under the KVS_PARTITION_VIEWS
	 * partition that had the information regarding a custom list view.
	 */
	public static final String KEY_GRAPH_VIEW_NAME = "nameOfGraphView";
	public static final String POTENTIAL_GRAPH_VIEW_NAME = "potentialGraphName";

	public static final String GRAPH_TYPE = "graphtype";

	private static final int RCODE_ODKCOLLECT_ADD_ROW =
			Controller.FIRST_FREE_RCODE;

	private DataManager dm;
	private Controller c;
	private Query query;
	private UserTable table;
	private CustomGraphView view;
	private DbHelper dbh;
	private KeyValueStoreHelper kvsh;
	private String graphName;
	private String potentialGraphName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle("");
		dbh = DbHelper.getDbHelper(this);
		this.graphName = getIntent().getStringExtra(BarGraphDisplayActivity.KEY_GRAPH_VIEW_NAME);
		this.potentialGraphName = getIntent().getStringExtra(BarGraphDisplayActivity.POTENTIAL_GRAPH_VIEW_NAME);
		if(graphName == null) {
			graphName = potentialGraphName;
		}
		c = new Controller(this, this, getIntent().getExtras());
		kvsh = c.getTableProperties().getKeyValueStoreHelper(KVS_PARTITION);
		dm = new DataManager(DbHelper.getDbHelper(this));
		// TODO: why do we get all table properties here? this is an expensive
		// call. I don't think we should do it.
		query = new Query(dm.getAllTableProperties(KeyValueStore.Type.ACTIVE),
				c.getTableProperties());
	}

	@Override
	protected void onResume() {
		super.onResume();
		init();
	}

	@Override
	public void init() {
		// I hate having to do these two refreshes here, but with the code the
		// way it is it seems the only way.
		c.refreshDbTable();
		query.clear();
		query.loadFromUserQuery(c.getSearchText());
		table = c.getIsOverview() ?
				c.getDbTable().getUserOverviewTable(query) :
					c.getDbTable().getUserTable(query);

				view = CustomGraphView.get(this, c.getTableProperties(), table, graphName, potentialGraphName);

				// change the info bar text IF necessary
				if (!c.getInfoBarText().endsWith(" (Graph: " + graphName + ")")) {
					c.setInfoBarText(c.getInfoBarText() + " (Graph: " + graphName + ")");
				}
				displayView();
	}

	private void displayView() {
		view.display();
		c.setDisplayView(view);
		setContentView(c.getContainerView());
	}

	public void backPressedWhileInGraph() {
		view.deleteDefaultGraph();
		c.onBackPressed();
	}

	@Override
	public void onBackPressed() {
		//check if it should ask to save
		if(!view.graphIsModified()) {
			backPressedWhileInGraph();
		}


		//ask if they wish to save
		final String getOldGraphName;
		if(graphName.equals(potentialGraphName)) {
			getOldGraphName = null;
		} else {
			getOldGraphName = graphName;
		}

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
				case DialogInterface.BUTTON_POSITIVE:
					alertForNewGraphName(getOldGraphName);
					break;

				case DialogInterface.BUTTON_NEGATIVE:
					backPressedWhileInGraph();
					break;
				}
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Do you want to save the graph?").setPositiveButton("Yes", dialogClickListener)
		.setNegativeButton("No", dialogClickListener).show();

		//view.handleBackButtonCall();
		// c.onBackPressed();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		if (c.handleActivityReturn(requestCode, resultCode, data)) {
			return;
		}
		switch (requestCode) {
		case RCODE_ODKCOLLECT_ADD_ROW:
			c.addRowFromOdkCollectForm(
					Integer.valueOf(data.getData().getLastPathSegment()));
			init();
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		c.buildOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		return c.handleMenuItemSelection(item);
	}

	@Override
	public void onSearch() {
		c.recordSearch();
		init();
	}


	private void alertForNewGraphName(String givenGraphName) {

		AlertDialog newColumnAlert;
		// Prompt an alert box
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Name of New Graph");


		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setFocusableInTouchMode(true);
		input.setFocusable(true);
		input.requestFocus();
		// adding the following line
		//((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
		//.showSoftInput(input, InputMethodManager.SHOW_FORCED);
		alert.setView(input);
		if (givenGraphName != null)
			input.setText(givenGraphName);

		// OK Action => Create new Column
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String graphName = input.getText().toString();
				graphName = graphName.trim();
				if (graphName == null || graphName.equals("")) {
					toastGraphNameError("Column name cannot be empty!");
					alertForNewGraphName(null);
				} else if (graphName.contains(" ")) {
					toastGraphNameError("Column name cannot contain spaces!");
					alertForNewGraphName(graphName.replace(' ', '_'));
				} else if (view.hasGraph(graphName)) {
					Log.d("stufftotest", "" + graphName);
					handleOverwriteRequest(graphName);
				} else {
					// Create new graph
					view.createNewGraph(graphName);
					backPressedWhileInGraph();
					// Load Column Property Manger
					//loadColumnPropertyManager(cp.getElementKey());
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

	private void toastGraphNameError(String msg) {
		Context context = getApplicationContext();
		Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
		toast.show();
	}

	public void handleOverwriteRequest(final String givenGraphName) {
		//Ask if the user wants to override a previous graph
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
				case DialogInterface.BUTTON_POSITIVE:
					view.createNewGraph(givenGraphName);
					backPressedWhileInGraph();
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					alertForNewGraphName(givenGraphName);
					break;
				}
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to overwrite this graph?").setPositiveButton("Yes", dialogClickListener)
		.setNegativeButton("No", dialogClickListener).show();
	}
	/*

    private static final int RCODE_ODKCOLLECT_ADD_ROW =
        Controller.FIRST_FREE_RCODE;

    private DataManager dm;
    private Controller c;
    private Query query;
    private List<String> labels;
    private List<Double> values;
    private boolean yIsCount;
    private UserTable table;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        c = new Controller(this, this, getIntent().getExtras());
        dm = new DataManager(DbHelper.getDbHelper(this));
        query = new Query(dm.getAllTableProperties(KeyValueStore.Type.ACTIVE),
            c.getTableProperties());
        init();
    }

    @Override
    public void init() {
        query.clear();
        query.loadFromUserQuery(c.getSearchText());
        table = c.getIsOverview() ?
                c.getDbTable().getUserOverviewTable(query) :
                c.getDbTable().getUserTable(query);
        view = CustomTableView.get(this, c.getTableProperties(), table,
                c.getTableViewSettings().getCustomListFilename());
        displayView();
    }

    private void openCollectionView(int rowNum) {
        query.clear();
        query.loadFromUserQuery(c.getSearchText());
        query.addConstraint(c.getTableViewSettings().getBarXCol(),
                labels.get(rowNum));
        Controller.launchTableActivity(this, c.getTableProperties(),
                query.toUserQuery(), false);
    }

    @Override
    public void onBackPressed() {
        c.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (c.handleActivityReturn(requestCode, resultCode, data)) {
            return;
        }
        switch (requestCode) {
        case RCODE_ODKCOLLECT_ADD_ROW:
            c.addRowFromOdkCollectForm(
                    Integer.valueOf(data.getData().getLastPathSegment()));
            init();
            break;
        default:
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        c.buildOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        return c.handleMenuItemSelection(item);
    }

    @Override
    public void onSearch() {
        c.recordSearch();
        init();
    }

    private class BarChartListener implements BarChart.ClickListener {

        @Override
        public void onClick(int index) {
            if (yIsCount) {
                openCollectionView(index);
            } else {
                Controller.launchDetailActivity(BarGraphDisplayActivity.this,
                        c.getTableProperties(), table, index);
            }
        }
    }*/
}
