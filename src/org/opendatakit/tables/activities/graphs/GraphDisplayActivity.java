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

import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.activities.DisplayActivity;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.KeyValueStoreType;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.tables.views.webkits.CustomGraphView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * Renders the CustomGraphView for the specified graph.
 *
 */
public class GraphDisplayActivity extends SherlockActivity
implements DisplayActivity {

	private static final String TAG = "GraphDisplayActivity";

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

   /**
    * Key for KVS -- the type of graph
    */
   public static final String GRAPH_TYPE = "graphtype";


	private Controller c;
	private Query query;
	private UserTable table;
	private CustomGraphView view;
	private KeyValueStoreHelper kvsh;
	private String graphName;
	private String potentialGraphName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    String appName = getIntent().getStringExtra(Controller.INTENT_KEY_APP_NAME);
	    if ( appName == null ) {
	      appName = TableFileUtils.getDefaultAppName();
	    }
		setTitle("");
		this.graphName = getIntent().getStringExtra(GraphDisplayActivity.KEY_GRAPH_VIEW_NAME);
		this.potentialGraphName = getIntent().getStringExtra(GraphDisplayActivity.POTENTIAL_GRAPH_VIEW_NAME);
		if(graphName == null) {
			graphName = potentialGraphName;
		}
		c = new Controller(this, this, getIntent().getExtras(), savedInstanceState);
		kvsh = c.getTableProperties().getKeyValueStoreHelper(GraphDisplayActivity.KVS_PARTITION);
		// TODO: why do we get all table properties here? this is an expensive
		// call. I don't think we should do it.
		query = new Query(this, appName, KeyValueStoreType.ACTIVE, c.getTableProperties());
	}

   @Override
   protected void onSaveInstanceState(Bundle outState) {
     super.onSaveInstanceState(outState);
     c.onSaveInstanceState(outState);
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
		c.refreshDbTable(c.getTableProperties().getTableId());
		query.clear();
		query.loadFromUserQuery(c.getSearchText());
      String sqlWhereClause =
          getIntent().getExtras().getString(Controller.INTENT_KEY_SQL_WHERE);
      if (sqlWhereClause != null) {
        String[] sqlSelectionArgs = getIntent().getExtras().getStringArray(
            Controller.INTENT_KEY_SQL_SELECTION_ARGS);
        DbTable dbTable = DbTable.getDbTable(c.getTableProperties());
        table = dbTable.rawSqlQuery(sqlWhereClause, sqlSelectionArgs);
      } else {
        // We use the query.
        table = c.getIsOverview() ?
            c.getDbTable().getUserOverviewTable(query) :
            c.getDbTable().getUserTable(query);
      }

      view = CustomGraphView.get(this, table.getTableProperties().getAppName(), table,
	     graphName, potentialGraphName, c);
      c.setGraphViewInfoBarText(graphName);
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
		builder.setTitle(getString(R.string.confirm_save_graph))
		.setMessage(getString(R.string.are_you_sure_save_graph))
		.setPositiveButton(getString(R.string.yes), dialogClickListener)
		.setNegativeButton(getString(R.string.cancel), dialogClickListener).show();

		//view.handleBackButtonCall();
		// c.onBackPressed();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		if (c.handleActivityReturn(requestCode, resultCode, data)) {
			return;
		} else {
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

	    View aview = getLayoutInflater().inflate(R.layout.message_with_text_edit_field_dialog, null);
	    alert.setView(aview)
		.setTitle(R.string.save_graph_as);

	    final TextView msg = (TextView) aview.findViewById(R.id.message);
	    msg.setText(getString(R.string.enter_new_graph_name));

		// Set an EditText view to get user input
		final EditText input = (EditText) aview.findViewById(R.id.edit_field);
		input.setFocusableInTouchMode(true);
		input.setFocusable(true);
		input.requestFocus();
		// adding the following line
		//((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
		//.showSoftInput(input, InputMethodManager.SHOW_FORCED);
		if (givenGraphName != null)
			input.setText(givenGraphName);

		// OK Action => Create new Column
		alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String graphName = input.getText().toString();
				graphName = graphName.trim();
				if (graphName == null || graphName.equals("")) {
					toastGraphNameError(getString(R.string.error_graph_name_empty));
					alertForNewGraphName(null);
				} else if (graphName.contains(" ")) {
					toastGraphNameError(getString(R.string.error_graph_name_spaces));
					alertForNewGraphName(graphName.replace(' ', '_'));
				} else if (view.hasGraph(graphName)) {
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
		alert.setNegativeButton(getString(R.string.cancel),
				new DialogInterface.OnClickListener() {
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
					view.setPermissions(givenGraphName, true);
					backPressedWhileInGraph();
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					alertForNewGraphName(givenGraphName);
					break;
				}
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.confirm_overwrite_graph))
		.setMessage(getString(R.string.are_you_sure_overwrite_graph, givenGraphName))
		.setPositiveButton(getString(R.string.yes), dialogClickListener)
		.setNegativeButton(getString(R.string.cancel), dialogClickListener).show();
	}
}
