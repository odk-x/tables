package yoonsung.odk.spreadsheet.Activity;

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
import org.opendatakit.aggregate.odktables.client.User;
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
	private static final boolean debug = true;

	private String[] phoneTableNames;
	private String[] aggregateTableNames;
	private TableEntry[] aggregateTableEntries;
	private Map<String, TableEntry> tableIDsToTableEntry;
	private Spinner phoneTables;
	private Spinner aggregateTables;
	private String userId;
	private AggregateConnection conn;
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
			setStatus("Failed to retrieve aggregate table list.");
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
				this.aggregateTableNames[counter] = entry.getTableName()+ " (owner:" + entry.getUserName()+")";
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
			setStatus("No tables exist on aggregate.");
			return null;
		}
	}

	private TableEntry getCurrentAggregateTableEntry() {
		int pos = this.aggregateTables.getSelectedItemPosition();
		if (this.aggregateTableNames.length != 0) {
			return this.aggregateTableEntries[pos];
		} else {
			setStatus("No tables exist on phone.");
			return null;
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
			setStatus("Successfully uploaded table.");
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
			setStatus("Failed to insert rows to table: row already exists");
			Log.d("aggregate","row already exists");
			if (debug) {
				e.printStackTrace();
			}
			return false;
		} catch (TableDoesNotExistException e) {
			//table does not exist on aggregate, refresh list and try again
			setStatus("Failed to insert rows to table: table does not exist");
			Log.d("aggregate","table does not exist");
			if (debug) {
				e.printStackTrace();
			}
			return false;
		} catch (Exception e) {
			//row insertion failed
			setStatus("Failed to insert rows to table.");
			if (debug) {
				e.printStackTrace();
			}
			return false;
		}
		return true;
	}

	private boolean createTable(final String tableName, final String tableID, final List<String> header) {
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
			// Prompt an alert box
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setMessage("This table already exists, replace it?");
			
			final String tempTableId = tableID;
			
			// OK Action => Create new Column
			alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					deleteTableHelper(tempTableId);
					uploadTable();
				}
			});

			// Cancel Action
			alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					//Canceled.
				}
			});
			alert.show();

//			setStatus("Failed to create new table: table already exists");
			if (debug) {
				e.printStackTrace();
			}
			return false;
		} catch (UserDoesNotExistException e) {
			//user does not exist, failed to add user to aggregate
			setStatus("Failed to create new table: user does not exist.");
			if (debug) {
				e.printStackTrace();
			}
			return false;
		} catch (Exception e) {
			//creating table failed, try again
			setStatus("Failed to upload table.");
			if (debug) {
				e.printStackTrace();
			}
			return false;
		}
		return true;
	}

	private void deleteTable() {
		final TableEntry entry = this.getCurrentAggregateTableEntry();

		Log.d("aggregate","deletion.name: " + entry.getTableName() + ", userId: " + userId);

		//ask if they're cool with this first
		// Prompt an alert box
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage("Are you sure you want to delete table " + entry.getTableName() + " (owner:" + entry.getUserName()+")?");

		// OK Action => Create new Column
		alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				deleteTableHelper(entry.getTableId());
			}
		});

		// Cancel Action
		alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				//Canceled.
			}
		});

		alert.show();
	}

	private void deleteTableHelper(String tableId) {
		try {
			Log.d("aggregate",conn.deleteTable(userId, tableId));
			Log.d("aggregate",conn.deleteTable(userId, "COLPROP_" + tableId));
			fillAggTableListSpinner();
			setStatus("Successfully deleted table.");
		} catch (Exception e) {
			//failed to delete table, try again
			setStatus("Failed to delete table.");
			if (debug) {
				e.printStackTrace();
			}
			return;
		}
	}

	public void createUser() {
		String userName = ((EditText)findViewById(R.id.aggregate_activity_userName)).getText().toString();

		try {
			conn.createUser(userId, userName);
			noUserAccount = false;
			setStatus("Successfully created user.");
		} catch (UserAlreadyExistsException e) {
			//user already exists, should not be here (checked getUserUri first, then this)
			//just show the UI then
			noUserAccount = false;
			Log.d("aggregate","user already exists.");
			if (debug) {
				e.printStackTrace();
			}
			setStatus("Failed to create user: this user already exists.");
		} catch (Exception e) {
			//retry
			//failed to create a user account, try again
			setStatus("Failed to create user.");
			if (debug) {
				e.printStackTrace();
			}
		}
		checkViews();
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
			setStatus("Failed to connect, check wifi connection and/or provided url and retry.");
			return;
		}

		this.connected = true;
		//check if user is set, if so then show options, else make them create user first		
		User user = this.getUserURI();
		if (user == null) {
			//user does not exist, create user
			noUserAccount = true;
		} else {
			//user exists
			noUserAccount = false;
		}
		checkViews();
		fillAggTableListSpinner();
		setStatus("Connection established.");
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
			getUserURI();
		}
	}

	private User getUserURI() {
		TextView text = (TextView) findViewById(R.id.aggregate_activity_user_uri_text);
		User uri;
		try {
			uri = conn.getUser(userId);
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
		text.setText("\nUser Name: " + uri.getUserName());
		return uri;
	}

	private List<Row> downloadHelper(String userUri, String tableId) {
		List<Row> rows = null;
		try {
			rows = conn.getRows(userUri, tableId);
		} catch (TableDoesNotExistException e) {
			setStatus("Failed to download table: table does not exist, refresh list.");
			Log.d("aggregate","table does not exist");
			if (debug) {
				e.printStackTrace();
			}
		} catch (UserDoesNotExistException e) {
			setStatus("Failed to download table: user does not exist, refresh list.");
			Log.d("aggregate","user does not exist");
			if (debug) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			setStatus("Failed to download table.");
			if (debug) {
				e.printStackTrace();
			}
		}
		return rows;
	}

	public void downloadTable() {
		final TableEntry entry = this.getCurrentAggregateTableEntry();

		Log.d("aggregate","got table entry: " + entry);
		for (String s: this.tableIDsToTableEntry.keySet()) {
			Log.d("aggregate","map has key: " + s + ", value:" + this.tableIDsToTableEntry.get(s));
		}

		if (entry == null) {
			Log.d("aggregate","table does not exist");
			setStatus("Failed to download table: table does not exist, refresh list.");
			return;
		}

		//give user option to change table name, here and on upload (if table already exists)
		// Prompt an alert box
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Name of Table Downloaded");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);
		input.setText(entry.getTableName());

		// OK Action => Create new Column
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String newTableName = input.getText().toString();
				finishDownload(entry, newTableName);
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

	private void finishDownload(TableEntry entry, String tableName) { 
		Log.d("aggregate","tablename: " + tableName);
		TableList tl = new TableList();
		if (tl.isTableExist(tableName)) {
			setStatus("Table "+tableName+" already exists. Specify different table name.");
			return;
		} 

		String res = tl.registerNewTable(tableName);
		if (res != null) {
			setStatus("Failed to download table: bad table name provided.");
			throw new IllegalArgumentException(res);
		}

		Integer tID = tl.getTableID(tableName);
		String tableID = tID.toString();
		String userUri = entry.getUserUri();
		List<Row> rows = downloadHelper(userUri,entry.getTableId());

		if (rows.size() == 0) {
			setStatus("Failed to download table: table empty.");
			return;
		}
		ArrayList<String> heads = new ArrayList<String>();
		for (String s: rows.get(0).getColumnValuePairs().keySet()) {
			heads.add(s.substring(4).toLowerCase());
		}

		for (Row row: rows) {
			Log.d("aggregate","rowID: "+row.getRowId() +", data: "+ row.getColumnValuePairs());
		}

		String stat = "CREATE TABLE IF NOT EXISTS `" + tableName + "` ("
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

		//Log.d("aggregate","so i set up the columns now");
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
			data.addRow(cv, pn, ts);
		}

		//set column properties

		String tableId = "COLPROP_"+entry.getTableId();
		List<Row> rows2 = downloadHelper(userUri,tableId);

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
					//boolean bool = Boolean.parseBoolean(value);
					//Log.d("aggregate","smsout value: " + bool);
					cp.setSMSOUT(colName, Boolean.parseBoolean(value));
				} else if (property.equals("NAME")) {
					//cp.setName(colName, newVal)
				} else if (property.equals("SMSIN")) {
					//boolean bool = Boolean.parseBoolean(value);
					//Log.d("aggregate","smsin value: " + bool);
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
		setStatus("Successfully downloaded table.");
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
		//ask if they're cool with this first
		// Prompt an alert box
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage("Are you sure? This deletes the user and all tables owned.");

		// OK Action => Create new Column
		alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				deleteUserHelper();
			}
		});

		// Cancel Action
		alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				//Canceled.
			}
		});

		alert.show();
	}

	private void deleteUserHelper() {
		try {
			conn.deleteUser(userId);
		} catch (Exception e) {
			setStatus("Failed to delete user.");
			return;
		}

		setStatus("Successfully deleted user.");
		this.noUserAccount = true;
		checkViews();
		fillAggTableListSpinner();
	}

	private void setStatus(String Message) {
		TextView status = (TextView)findViewById(R.id.aggregate_activity_status);
		status.setText("Recent Status: " + Message);
	}
}
