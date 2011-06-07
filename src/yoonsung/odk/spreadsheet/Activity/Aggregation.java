package yoonsung.odk.spreadsheet.Activity;

/*
 * UI: connect first, 
 * then check if userui exists 
 * (get dylan to have it be able to return user name), 
 * make them create user if not, else display username and general ui
 * (maybe split up the ui into different sections if possible)
 * 
 * Downloading: Dylan made it possible to have table names returned,
 * please check code for occurrences of table id,
 * then get downloading column information stored into column properties
 * 
 * also somewhere in here do that thing about verifications (deleteing user and/or tables)
 * as well as new name for table downloaded or uploaded
 */

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.aggregate.odktables.client.AggregateConnection;
import org.opendatakit.aggregate.odktables.client.Column;
import org.opendatakit.aggregate.odktables.client.Row;
import org.opendatakit.aggregate.odktables.client.TableEntry;
import org.opendatakit.aggregate.odktables.client.exception.RowAlreadyExistsException;
import org.opendatakit.aggregate.odktables.client.exception.TableAlreadyExistsException;
import org.opendatakit.aggregate.odktables.client.exception.TableDoesNotExistException;
import org.opendatakit.aggregate.odktables.client.exception.UserAlreadyExistsException;
import org.opendatakit.aggregate.odktables.client.exception.UserDoesNotExistException;
import org.opendatakit.common.persistence.DataField.DataType;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.DataStructure.Table;
import yoonsung.odk.spreadsheet.Database.ColumnProperty;
import yoonsung.odk.spreadsheet.Database.DBIO;
import yoonsung.odk.spreadsheet.Database.DataTable;
import yoonsung.odk.spreadsheet.Database.DefaultsManager;
import yoonsung.odk.spreadsheet.Database.TableList;
import yoonsung.odk.spreadsheet.Database.TableProperty;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class Aggregation extends Activity {

	private static int TABLESPIN_ID = 2000;
	private static int AGGREGATETABLESPIN_ID = 1000;
	//	private static final int IN_PROGRESS_DIALOG = -1;
	//	private static final int MESSAGE_DIALOG = -2;
	private static final int UPLOADTABLE_FAILURE = 0;
	//	private static final int UPLOADTABLE_SUCCESS = 2;
	//	private static final int DOWNLOADTABLE_FAILED = 3;
	//	private static final int DOWNLOADTABLE_SUCCESS = 4;
	//	private static final int CREATEUSER_FAILED = 5;
	//	private static final int CREATEUSER_SUCCESS = 6;
	//	private static final int DELETEUSER_FAILED = 7;
	//	private static final int DELETEUSER_SUCCESS = 8;
	//	private static final int GETTABLELIST_FAILED = 9;
	//	private static final int GETTABLELIST_SUCCESS = 10;
	//	private static final int GETUSERURI_FAILED = 11;
	//	private static final int GETUSERURI_SUCCESS = 12;
	//	private static final int CONNECTION_FAILED = 13;
	//	private static final int CONNECTION_SUCCESS = 14;
	//	private static final int DELETETABLE_FAILED = 15;
	//	private static final int DELETETABLE_SUCCESS = 16;
	//	private static final int TABLE_NOEXIST = 17;
	//	private static String message;
	private static final boolean debug = true;
	private static final int GET_LIST_FAILURE = 1;	
	private static final int NO_AGGTABLES_EXIST_FAILURE = 2;
	private static final int NO_PHONETABLES_EXIST_FAILURE = 3;
	private static final int UPLOAD_ROWS_FAILURE = 4;
	private static final int UPLOAD_NOTABLE_FAILURE = 0;
	private static final int UPLOAD_TABLEEXISTS_FAILURE = 0;
	private static final int UPLOAD_USERNOTEXIST_FAILURE = 0;
	private static final int DELETETABLE_FAILURE = 0;
	private static final int CREATEUSER_FAILURE = 0;
	private static final int CONNECTION_FAILURE = 0;

	private String[] phoneTableNames;
	private String[] aggregateTableNames;
	private TableEntry[] aggregateTableEntries;
	private Map<String, TableEntry> tableIDsToTableEntry;
	private Spinner phoneTables;
	private Spinner aggregateTables;
	private String userId;
	private AggregateConnection conn;
	private String message;
	private boolean noUserAccount;
	private boolean connected;

	@Override
	public void onCreate(Bundle bund) {
		super.onCreate(bund);
		setContentView(R.layout.aggregate_activity);

		setTitle("ODK Tables > Aggregate");

		tableIDsToTableEntry = new HashMap<String, TableEntry>();

		TelephonyManager teleMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
		this.userId = teleMgr.getDeviceId();

		this.aggregateTables = (Spinner)findViewById(R.id.aggregate_activity_getAggregateTables);
		this.aggregateTables.setId(AGGREGATETABLESPIN_ID);

		this.phoneTables = (Spinner)findViewById(R.id.aggregate_activity_getPhoneTables);
		this.phoneTables.setId(TABLESPIN_ID);

		fillPhoneTableSpinnerList();
		makeButtonListeners();
		hideViews();
		noUserAccount = true;
		connected = false;
	}

	private void makeButtonListeners() {
		Button connect = (Button)findViewById(R.id.aggregate_activity_connect);
		connect.setOnClickListener(new connectListener());

		Button createUser = (Button)findViewById(R.id.aggregate_activity_createUser);
		createUser.setOnClickListener(new createUserListener());

		Button deleteUser = (Button)findViewById(R.id.aggregate_activity_deleteUser);
		deleteUser.setOnClickListener(new deleteUserListener());

		Button uploadTable = (Button)findViewById(R.id.aggregate_activity_uploadTable);
		uploadTable.setOnClickListener(new uploadListener());

		Button downloadTable = (Button)findViewById(R.id.aggregate_activity_downloadTable);
		downloadTable.setOnClickListener(new downloadListener());

		Button deleteTable = (Button)findViewById(R.id.aggregate_activity_deleteTable);
		deleteTable.setOnClickListener(new deleteListener());

		Button getTableList = (Button)findViewById(R.id.aggregate_activity_getAggregateTables_button);
		getTableList.setOnClickListener(new getTableListener());
	}

	private void hideViews() {
		findViewById(R.id.aggregate_activity_createUser).setVisibility(View.GONE);
		findViewById(R.id.aggregate_activity_deleteUser).setVisibility(View.GONE);
		findViewById(R.id.aggregate_activity_user_uri_text).setVisibility(View.GONE);
		findViewById(R.id.aggregate_activity_uploadTable).setVisibility(View.GONE);
		findViewById(R.id.aggregate_activity_downloadTable).setVisibility(View.GONE);
		findViewById(R.id.aggregate_activity_deleteTable).setVisibility(View.GONE);
		findViewById(R.id.aggregate_activity_getAggregateTables_text).setVisibility(View.GONE);
		this.aggregateTables.setVisibility(View.GONE);
		findViewById(R.id.aggregate_activity_getAggregateTables_button).setVisibility(View.GONE);
		findViewById(R.id.aggregate_activity_userName_text).setVisibility(View.GONE);
		findViewById(R.id.aggregate_activity_userName).setVisibility(View.GONE);
		findViewById(R.id.aggregate_activity_getPhoneTables_text).setVisibility(View.GONE);
		this.phoneTables.setVisibility(View.GONE);
	}

	//	private void makeVisible() {
	//		Log.d("aggregate","made it to makeVisible");
	////		findViewById(R.id.aggregate_activity_createUser).setVisibility(View.VISIBLE);
	////		findViewById(R.id.aggregate_activity_userName_text).setVisibility(View.VISIBLE);
	////		findViewById(R.id.aggregate_activity_userName).setVisibility(View.VISIBLE);
	////		findViewById(R.id.aggregate_activity_downloadTable).setVisibility(View.VISIBLE);
	////		findViewById(R.id.aggregate_activity_getAggregateTables_text).setVisibility(View.VISIBLE);
	////		this.aggregateTables.setVisibility(View.VISIBLE);
	////		findViewById(R.id.aggregate_activity_getAggregateTables_button).setVisibility(View.VISIBLE);
	//		findViewById(R.id.aggregate_activity_deleteUser).setVisibility(View.VISIBLE);
	//		findViewById(R.id.aggregate_activity_user_uri_text).setVisibility(View.VISIBLE);
	//		findViewById(R.id.aggregate_activity_uploadTable).setVisibility(View.VISIBLE);
	//		findViewById(R.id.aggregate_activity_deleteTable).setVisibility(View.VISIBLE);
	//		findViewById(R.id.aggregate_activity_getPhoneTables_text).setVisibility(View.VISIBLE);
	//		this.phoneTables.setVisibility(View.VISIBLE);
	//	}

	private void fillPhoneTableSpinnerList() {
		Map<String, String> tableMap = (new TableList()).getAllTableList();
		phoneTableNames = new String[tableMap.size()];
		int counter = 0;
		for(String tableId : tableMap.keySet()) {
			Log.d("aggregate","tableId"+tableId);
			phoneTableNames[counter] = tableMap.get(tableId);
			counter++;
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, phoneTableNames);
		adapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);
		this.phoneTables.setAdapter(adapter);
		this.phoneTables.setSelection(0);
	}

	private void fillAggTableListSpinner() {
		//get list of tables on aggregate
		org.opendatakit.aggregate.odktables.client.TableList aggTblLst = new org.opendatakit.aggregate.odktables.client.TableList();
		try {
			aggTblLst = conn.listTables();
		} catch (Exception e) {
			//could not retrieve table list
			showDialog(GET_LIST_FAILURE);
			if (debug) {
				e.printStackTrace();
			}
			return;
		}

		tableIDsToTableEntry = new HashMap<String, TableEntry>();
		int counter = 0;
		for (TableEntry entry: aggTblLst) {
			if (!entry.getTableId().startsWith("COLPROP_")) {
				counter++;
			}
			this.tableIDsToTableEntry.put(entry.getTableId(), entry);
		}

		this.aggregateTableNames = new String[counter];
		this.aggregateTableEntries = new TableEntry[counter];

		counter = 0;
		for (TableEntry entry: aggTblLst) {
			if (!entry.getTableId().startsWith("COLPROP_")) {
				this.aggregateTableNames[counter] = entry.getTableName();
				this.aggregateTableEntries[counter] = entry;
				counter++;
			}
			this.tableIDsToTableEntry.put(entry.getTableId(), entry);
		}

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, this.aggregateTableNames);
		adapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);
		this.aggregateTables.setAdapter(adapter);
		this.aggregateTables.setSelection(0);
	}

	private String getCurrentPhoneTableName() {
		int pos = this.phoneTables.getSelectedItemPosition();
		if (this.phoneTableNames.length != 0) {
			return phoneTableNames[pos];
		} else {
			showDialog(NO_AGGTABLES_EXIST_FAILURE);
			return null;
		}
	}

	private TableEntry getCurrentAggregateTableEntry() {
		int pos = this.aggregateTables.getSelectedItemPosition();
		if (this.aggregateTableNames.length != 0) {

			return this.aggregateTableEntries[pos];
		} else {
			showDialog(NO_PHONETABLES_EXIST_FAILURE);
			return null;
		}
	}

	/**
	 * Creates a simple alert dialog.
	 * @param message the dialog's message
	 * @return the dialog
	 */
	private AlertDialog getDialog(String message) {
		AlertDialog.Builder adBuilder = new AlertDialog.Builder(this);
		adBuilder = adBuilder.setMessage(this.message);
		adBuilder = adBuilder.setNeutralButton("OK",
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		AlertDialog d = adBuilder.create();
		return d;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case GET_LIST_FAILURE:
			return getDialog("Failed to update aggregate list.");
		case NO_PHONETABLES_EXIST_FAILURE:
			return getDialog("No tables exist on phone.");
			//		case UPLOADTABLE_FAILED:
			//			return getDialog("Failed to upload table.");
			//		case UPLOADTABLE_SUCCESS:
			//			return getDialog("Table uploaded.");
			//		case DOWNLOADTABLE_FAILED:
			//			return getDialog("Failed to download table.");
			//		case DOWNLOADTABLE_SUCCESS:
			//			return getDialog("Table downloaded.");
			//		case CREATEUSER_FAILED:
			//			return getDialog("Failed to create user.");
			//		case CREATEUSER_SUCCESS:
			//			return getDialog("User created.");
			//		case DELETEUSER_FAILED:
			//			return getDialog("Failed to delete user and assosciated tables.");
			//		case DELETEUSER_SUCCESS:
			//			return getDialog("User and assosciated tables deleted.");
			//		case GETTABLELIST_FAILED:
			//			return getDialog("Failed to get table list.");
			//		case GETTABLELIST_SUCCESS:
			//			return getDialog("Table lists retrieved.");
			//		case GETUSERURI_FAILED:
			//			return getDialog("Failed to acquire user uri.");
			//		case GETUSERURI_SUCCESS:
			//			return getDialog("User uri acquired.");
			//		case CONNECTION_FAILED:
			//			return getDialog("Failed to connect.");
			//		case CONNECTION_SUCCESS:
			//			return getDialog("Connected.");
			//		case DELETETABLE_FAILED:
			//			return getDialog("Failed to delete table.");
			//		case DELETETABLE_SUCCESS:
			//			return getDialog("Table deleted.");
		default:
			throw new IllegalArgumentException();
		}
	}

	private class connectListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			connect();
		}
	}

	private class downloadListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			downloadTable();
		}
	}

	private class deleteListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			deleteTable();
		}
	}

	private class createUserListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			createUser();
		}
	}

	private class deleteUserListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			deleteUser();
		}
	}

	private class uploadListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			uploadTable();
		}
	}

	private class getTableListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			fillAggTableListSpinner();
		}
	}

	private void uploadTable() {
		String tableName = this.getCurrentPhoneTableName();
		String tableID = (new Integer(new TableList().getTableID(tableName))).toString();
		Table current = (new DataTable(tableID)).getCompleteTable();

		List<String> updated = new ArrayList<String>();

		for (String name: current.getHeader()) {
			if (!name.matches("[^0-9a-zA-Z].*|[^_].*")) {
				name = name.replaceAll("[^0-9a-zA-Z]|[^_]", "");
			}
			name = "COL_" + name;
			Log.d("aggregate","newColName: " + name);
			updated.add(name);
		}

		List<Row> rows = new ArrayList<Row>();
		ArrayList<String> data = current.getData();
		ArrayList<Integer> rowIDs = current.getRowID();

		int width = updated.size();
		for (int i = 0; i < current.getHeight(); i++) {
			Row row = new Row("" + rowIDs.get(i));
			for (int j = 0; j < width; j++) {
				String name = updated.get(j);
				String value = data.get(i * width + j);
				if (value == null || value.equals("")) {
					value = "VAL_";
				} else {
					value = "VAL_" + value;
				}
				row.setColumn(name, value);
			}
			rows.add(row);
		}

		if (this.createTable(tableName, tableID, updated) && 
				this.insertRows(tableID, rows) && 
				this.storeColProps(updated, tableID, tableName)) {
			fillAggTableListSpinner();
			//			showDialog(UPLOAD_SUCCESS);
		} else {
			//failed
		}
	}

	private boolean storeColProps(List<String> header, String tableId, String tableName) {
		ColumnProperty cp = new ColumnProperty(tableId);

		List<String> colpro = new ArrayList<String>();
		colpro.add("name");
		colpro.add("abrev");
		colpro.add("type");
		colpro.add("smsin");
		colpro.add("smsout");
		colpro.add("footer");
		colpro.add("index");

		this.createTable("COLPROP_"+tableName, "COLPROP_"+tableId, colpro);

		List<Row> rows = new ArrayList<Row>();
		int counter = 0;
		for (String col: header) {
			String name = col.substring(4);
			if (!col.equals("COL__timestamp") && !col.equals("COL__phoneNumberIn")) {
				String abrev = cp.getAbrev(name) == null? "": cp.getAbrev(name);
				String type = cp.getType(name) == null? "": cp.getType(name);
				String footer = cp.getFooterMode(name) == null? "": cp.getFooterMode(name);

				Log.d("aggregate","column? "+ col + " abrev " + abrev + " type " + type + " footer " + footer + " smsin " + String.valueOf(cp.getSMSIN(name)) + " smsout " +String.valueOf(cp.getSMSOUT(name)));

				Row row = new Row("" + counter);
				row.setColumn("name", name);
				row.setColumn("abrev", abrev);
				row.setColumn("type", type);
				row.setColumn("smsin", String.valueOf(cp.getSMSIN(name)));
				row.setColumn("smsout", String.valueOf(cp.getSMSOUT(name)));
				row.setColumn("footer", footer);
				row.setColumn("index", String.valueOf(cp.getIsIndex(name)));
				rows.add(row);
				counter++;
			}
		}

		return this.insertRows("COLPROP_"+tableId, rows);
	}

	private boolean insertRows(String tableID, List<Row> rows) {  
		for (Row row: rows) {
			Log.d("aggregate","rowID: "+row.getRowId() +", data: "+ row.getColumnValuePairs());
		}

		try {
			conn.insertRows(userId, tableID, rows);
		} catch (RowAlreadyExistsException e) {
			//should not get this exception, if so then query person to remove current table
			showDialog(UPLOAD_ROWS_FAILURE);
			Log.d("aggregate","row already exists");
			if (debug) {
				e.printStackTrace();
			}
			return false;
		} catch (TableDoesNotExistException e) {
			//table does not exist on aggregate, refresh list and try again
			showDialog(UPLOAD_NOTABLE_FAILURE);
			Log.d("aggregate","table does not exist");
			if (debug) {
				e.printStackTrace();
			}
			return false;
		} catch (Exception e) {
			//row insertion failed
			showDialog(UPLOADTABLE_FAILURE);
			if (debug) {
				e.printStackTrace();
			}
			return false;
		}
		return true;
	}

	private boolean createTable(String tableName, String tableID, List<String> header) {
		//if table doesnt exist already
		List<Column> columns = new ArrayList<Column>();

		for (int i = 0; i < header.size(); i++ ) {
			String name = header.get(i);
			Column col = new Column(name, DataType.STRING, true);
			columns.add(col);
			Log.d("aggregate","column name: "+col.getName());
		}

		for (Column col: columns) {
			Log.d("aggregate","column name: "+col.getName());
		}

		try {
			conn.createTable(userId, tableID, tableName, columns);
		} catch (TableAlreadyExistsException e) {
			//ask if user wants to remove previous table or change this tables name
			showDialog(UPLOAD_TABLEEXISTS_FAILURE);
			if (debug) {
				e.printStackTrace();
			}
			return false;
		} catch (UserDoesNotExistException e) {
			//user does not exist, failed to add user to aggregate
			showDialog(UPLOAD_USERNOTEXIST_FAILURE);
			if (debug) {
				e.printStackTrace();
			}
			return false;
		} catch (Exception e) {
			//creating table failed, try again
			showDialog(UPLOADTABLE_FAILURE);
			if (debug) {
				e.printStackTrace();
			}
			return false;
		}
		return true;
	}

	private void deleteTable() {
		TableEntry tableName = this.getCurrentAggregateTableEntry();

		Log.d("aggregate","deletion.name: " + tableName.getTableName() + ", userId: " + userId);

		try {
			conn.deleteTable(userId, tableName.getTableId());
			conn.deleteTable(userId, "COLPROP_" + tableName.getTableId());
		} catch (Exception e) {
			//failed to delete table, try again
			showDialog(DELETETABLE_FAILURE);
			if (debug) {
				e.printStackTrace();
			}
			return;
		}
		fillAggTableListSpinner();
		//		showDialog(DELETETABLE_SUCCESS);
	}

	public void createUser() {
		String userName = ((EditText)findViewById(R.id.aggregate_activity_userName)).getText().toString();

		try {
			conn.createUser(userId, userName);
			noUserAccount = false;
		} catch (UserAlreadyExistsException e) {
			//user already exists, should not be here (checked getUserUri first, then this)
			//just show the UI then
			noUserAccount = false;
			Log.d("aggregate","user already exists.");
			if (debug) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			//retry
			//failed to create a user account, try again
			showDialog(CREATEUSER_FAILURE);
			if (debug) {
				e.printStackTrace();
			}
		}
		checkViews();
		//		showDialog(CREATEUSER_SUCCESS);
	}

	public void connect() {
		String url = "http://" + 
		//		((EditText)findViewById(R.id.aggregate_activity_url)).getText().toString()
		"the-dylan-price"
		+ ".appspot.com/";

		try {
			this.conn = new AggregateConnection(
					new URI(url));
		} catch (URISyntaxException e) {
			//could not connect to instance, please check wifi settings or url and retry
			if (debug) {
				e.printStackTrace();
			}
			this.connected = false;
			checkViews();
			showDialog(CONNECTION_FAILURE);
			return;
		}

		this.connected = true;
		//check if user is set, if so then show options, else make them create user first		
		String uri = this.getUserURI();
		if (uri == null) {
			//user does not exist, create user
			noUserAccount = true;
		} else {
			//user exists
			noUserAccount = false;
		}
		checkViews();
		fillAggTableListSpinner();
//		showDialog(CONNECTION_SUCCESS);
	}

	public void checkViews(){
		Log.d("aggregate","made it to checkViews: hasNoAccount? " + this.noUserAccount + ", connected?" + this.connected);

		if (connected) {
			findViewById(R.id.aggregate_activity_connect).setVisibility(View.GONE);
			findViewById(R.id.aggregate_activity_url).setVisibility(View.GONE);
			findViewById(R.id.aggregate_activity_url_text).setVisibility(View.GONE);
		} else {
			this.hideViews();
			findViewById(R.id.aggregate_activity_connect).setVisibility(View.VISIBLE);
			findViewById(R.id.aggregate_activity_url).setVisibility(View.VISIBLE);
			EditText connect = (EditText)findViewById(R.id.aggregate_activity_url);
			//change when deploy, removing Dylan price thing
			connect.setText("http://"
					+"the-dylan-price"+ 
			".appspot.com/");
			findViewById(R.id.aggregate_activity_url_text).setVisibility(View.VISIBLE);
			return;
		}

		if (noUserAccount) {
			findViewById(R.id.aggregate_activity_createUser).setVisibility(View.VISIBLE);
			findViewById(R.id.aggregate_activity_deleteUser).setVisibility(View.GONE);
			findViewById(R.id.aggregate_activity_user_uri_text).setVisibility(View.GONE);
			findViewById(R.id.aggregate_activity_uploadTable).setVisibility(View.GONE);
			findViewById(R.id.aggregate_activity_downloadTable).setVisibility(View.VISIBLE);
			findViewById(R.id.aggregate_activity_deleteTable).setVisibility(View.GONE);
			findViewById(R.id.aggregate_activity_getAggregateTables_text).setVisibility(View.VISIBLE);
			this.aggregateTables.setVisibility(View.VISIBLE);
			findViewById(R.id.aggregate_activity_getAggregateTables_button).setVisibility(View.VISIBLE);
			findViewById(R.id.aggregate_activity_userName_text).setVisibility(View.VISIBLE);
			findViewById(R.id.aggregate_activity_userName).setVisibility(View.VISIBLE);
			findViewById(R.id.aggregate_activity_getPhoneTables_text).setVisibility(View.GONE);
			this.phoneTables.setVisibility(View.GONE);
		} else {
			findViewById(R.id.aggregate_activity_createUser).setVisibility(View.GONE);
			findViewById(R.id.aggregate_activity_deleteUser).setVisibility(View.VISIBLE);
			findViewById(R.id.aggregate_activity_user_uri_text).setVisibility(View.VISIBLE);
			findViewById(R.id.aggregate_activity_uploadTable).setVisibility(View.VISIBLE);
			findViewById(R.id.aggregate_activity_downloadTable).setVisibility(View.VISIBLE);
			findViewById(R.id.aggregate_activity_deleteTable).setVisibility(View.VISIBLE);
			findViewById(R.id.aggregate_activity_getAggregateTables_text).setVisibility(View.VISIBLE);
			this.aggregateTables.setVisibility(View.VISIBLE);
			findViewById(R.id.aggregate_activity_getAggregateTables_button).setVisibility(View.VISIBLE);
			findViewById(R.id.aggregate_activity_userName_text).setVisibility(View.GONE);
			findViewById(R.id.aggregate_activity_userName).setVisibility(View.GONE);
			findViewById(R.id.aggregate_activity_getPhoneTables_text).setVisibility(View.VISIBLE);
			this.phoneTables.setVisibility(View.VISIBLE);
		}
	}

	private String getUserURI() {
		TextView text = (TextView) findViewById(R.id.aggregate_activity_user_uri_text);
		String uri = "";
		try {
			uri += conn.getUserUri(userId);
		} catch (UserDoesNotExistException e) {
			//user does not exist, return null so that connect() method knows it needs to create a user
			Log.d("aggregate","User does not exist.");
			if (debug) {
				e.printStackTrace();
			}
			return null;
		} catch (Exception e) {
			//could not retrieve user uri, unknown error occurred, please create user
			if (debug) {
				e.printStackTrace();
			}
			return null;
		}
		text.setText("Phone ID on aggregate: " + uri);
		return uri;
	}

	//***********************************************************
	
	public void downloadTable() {
		TableEntry tableEntryToDownload = this.getCurrentAggregateTableEntry();

		Log.d("aggregate","got table entry: " + tableEntryToDownload);
		for (String s: this.tableIDsToTableEntry.keySet()) {
			Log.d("aggregate","map has key: " + s + ", value:" + this.tableIDsToTableEntry.get(s));
		}

		if (tableEntryToDownload == null) {
			Log.d("aggregate","table does not exist");
			//showDialog(DOWNLOADTABLE_TABLEDOESNOTEXIST_FAILURE);
			return;
		}
		String userUri = tableEntryToDownload.getUserUri();
		List<Row> rows;

		//MAKE NEW METHOD TO DO THIS, REDUNDANT
		try {
			rows = conn.getRows(userUri, tableEntryToDownload.getTableId());
		} catch (TableDoesNotExistException e) {
			//showDialog(DOWNLOADTABLE_TABLEDOESNOTEXIST_FAILURE);
			Log.d("aggregate","table does not exist");
			if (debug) {
				e.printStackTrace();
			}
			return;
		} catch (UserDoesNotExistException e) {
			//showDialog(DOWNLOADTABLE_FAILED);
			Log.d("aggregate","user does not exist");
			if (debug) {
				e.printStackTrace();
			}
			return;
		} catch (Exception e) {
			//showDialog(DOWNLOADTABLE_FAILED);
			if (debug) {
				e.printStackTrace();
			}
			return;
		}

		//give user option to change table name, here and on upload (if table already exists)
		String tableName2 = 
			tableEntryToDownload.getTableName();
		//			((EditText)findViewById(R.id.aggregate_activity_userName)).getText().toString();
		System.out.println("tablename2: " + tableName2);

		for (Row row: rows) {
			Log.d("aggregate","rowID: "+row.getRowId() +", data: "+ row.getColumnValuePairs());
		}

		//======================================================

		if (rows.size() == 0) {
			//cannot have empty table
			return;
		}
		ArrayList<String> heads = new ArrayList<String>();
		for (String s: rows.get(0).getColumnValuePairs().keySet()) {
			heads.add(s.substring(4).toLowerCase());
		}

		TableList tl = new TableList();
		String res = tl.registerNewTable(tableName2);
		if(res != null) {
			throw new IllegalArgumentException(res);
		}
		String stat = "CREATE TABLE IF NOT EXISTS `" + tableName2 + "` ("
		+ DataTable.DATA_ROWID + " INTEGER PRIMARY KEY,"
		+ DataTable.DATA_PHONE_NUMBER_IN + " TEXT,"
		+ DataTable.DATA_TIMESTAMP + " TEXT";
		for(String col : heads) {
			if(!col.equals("_phonenumberin") && !col.equals("_timestamp")) {
				stat += ", `" + col + "` TEXT";
			}
		}
		stat += ");";
		DBIO db = new DBIO();
		SQLiteDatabase con = db.getConn();
		con.execSQL(stat);
		con.close();
		Integer tID = tl.getTableID(tableName2);
		String tableID = tID.toString();
		DefaultsManager dm = new DefaultsManager(tableID);
		TableProperty tp = new TableProperty(tableID);
		ArrayList<String> colOrder = tp.getColOrderArrayList();
		for(String col : heads) {
			if(!col.equals("_phonenumberin") && !col.equals("_timestamp")) {
				Log.d("aggregate", "starting col add:" + col);
				dm.prepForNewCol(col);
				Log.d("aggregate", "just called dm.prepForNewCol");
				colOrder.add(col);
			}
		}
		tp.setColOrder(colOrder);

		//		Log.d("aggregate","so i set up the columns now");
		DataTable data = new DataTable(tableID);

		for (Row row : rows) {
			Map<String, String> temp = row.getColumnValuePairs();
			ArrayList<String> columns = new ArrayList<String>();
			ArrayList<String> values = new ArrayList<String>();
			String pn = "";
			String ts = "";
			for (String s : temp.keySet()) {
				String column = s.substring(4).toLowerCase();
				String value = temp.get(s).substring(4);
				if (column.equals("_timestamp")) {
					ts = value;
				} else if (column.equals("_phonenumberin")) {
					pn = value;
				} else {
					columns.add(column);
					values.add(value);
				}
			}
			ContentValues cv = getValues(columns, values);
			Log.d("aggregate","addin row: " + cv);
			try {
				data.addRow(cv, pn, ts);
			} catch(IllegalArgumentException e) {
				//				showDialog(DOWNLOADTABLE_FAILED);
			}
		}


		//============================
		//column properties

		String tableId = "COLPROP_"+tableEntryToDownload.getTableId();
		String otherUserUri = tableEntryToDownload.getUserUri();
		List<Row> rows2;

		try {
			rows2 = conn.getRows(otherUserUri, tableId);
		} catch (TableDoesNotExistException e) {
			//			showDialog(DOWNLOADTABLE_FAILED);
			Log.d("aggregate","table does not exist");
			if (debug) {
				e.printStackTrace();
			}
			return;
		} catch (UserDoesNotExistException e) {
			//			showDialog(DOWNLOADTABLE_FAILED);
			Log.d("aggregate","user does not exist");
			if (debug) {
				e.printStackTrace();
			}
			return;
		} catch (Exception e) {
			//			showDialog(DOWNLOADTABLE_FAILED);
			if (debug) {
				e.printStackTrace();
			}
			return;
		}

		for (Row row: rows2) {
			Log.d("aggregate","rowID: "+row.getRowId() +", data: "+ row.getColumnValuePairs());
		}

		for (Row row: rows2) {
			ColumnProperty cp = new ColumnProperty(tableID);
			String colName = row.getColumnValuePairs().get("NAME");
			cp.setName("colName", colName);

			Map<String,String> propToValue = row.getColumnValuePairs();
			for (String property: propToValue.keySet()) {
				String value = propToValue.get(property);
				Log.d("aggregate", "setting property " + property + " to " + value);
				if (property.equals("SMSOUT")) {
					boolean bool = Boolean.parseBoolean(value);
					Log.d("aggregate","smsout value: " + bool);
					cp.setSMSOUT(colName, Boolean.parseBoolean(value));
				} else if (property.equals("NAME")) {
					//					cp.setName(colName, newVal)
				} else if (property.equals("SMSIN")) {
					boolean bool = Boolean.parseBoolean(value);
					Log.d("aggregate","smsin value: " + bool);
					cp.setSMSIN(colName, Boolean.parseBoolean(value));
				} else if (property.equals("TYPE")) {
					cp.setType(colName, value);
				} else if (property.equals("FOOTER")) {
					cp.setFooterMode(colName, value);
				} else if (property.equals("ABREV")) {
					cp.setAbrev(colName, value);
				} else if (property.equals("INDEX")) {
					cp.setIsIndex(colName, Boolean.parseBoolean(value));
				}
			}
		}

		fillPhoneTableSpinnerList();
		//=================
		//		showDialog(DOWNLOADTABLE_SUCCESS);
	}

	private ContentValues getValues(ArrayList<String> columns, ArrayList<String> values) {
		ContentValues vals = new ContentValues();
		for(int i=0; i<columns.size(); i++) {
			String head = "`" + columns.get(i) + "`";
			if(!head.equals("_phonenumberin") && !head.equals("_timestamp")) {
				vals.put(head, values.get(i));
			}
		}
		return vals;
	}

	public void deleteUser() {
		//ask if they're kewl with this first
		try {
			conn.deleteUser(userId);
		} catch (Exception e) {
			//			showDialog(DELETEUSER_FAILED);
			return;
		}
		//		showDialog(DELETEUSER_SUCCESS);
		//		getUserURI();
		this.noUserAccount = true;
		checkViews();
		fillAggTableListSpinner();
	}
}
