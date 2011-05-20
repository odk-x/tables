package yoonsung.odk.spreadsheet.Activity;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import yoonsung.odk.spreadsheet.Database.DBIO;
import yoonsung.odk.spreadsheet.Database.DataTable;
import yoonsung.odk.spreadsheet.Database.DefaultsManager;
import yoonsung.odk.spreadsheet.Database.TableList;
import yoonsung.odk.spreadsheet.Database.TableProperty;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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

	private static int TABLESPIN_ID = 0;
	private static int AGGREGATETABLESPIN_ID = 8;
	private static final int IN_PROGRESS_DIALOG = -1;
//	private static final int MESSAGE_DIALOG = -2;
	private static final int UPLOADTABLE_FAILED = 1;
	private static final int UPLOADTABLE_SUCCESS = 2;
	private static final int DOWNLOADTABLE_FAILED = 3;
	private static final int DOWNLOADTABLE_SUCCESS = 4;
	private static final int CREATEUSER_FAILED = 5;
	private static final int CREATEUSER_SUCCESS = 6;
	private static final int DELETEUSER_FAILED = 7;
	private static final int DELETEUSER_SUCCESS = 8;
	private static final int GETTABLELIST_FAILED = 9;
	private static final int GETTABLELIST_SUCCESS = 10;
	private static final int GETUSERURI_FAILED = 11;
	private static final int GETUSERURI_SUCCESS = 12;
	private static final int CONNECTION_FAILED = 13;
	private static final int CONNECTION_SUCCESS = 14;
	private static final int DELETETABLE_FAILED = 15;
	private static final int DELETETABLE_SUCCESS = 16;
	private static final int TABLE_NOEXIST = 17;
	
//	private static String message;
	private static final boolean debug = true;
	
	private String[] phoneTableNames;
	private String[] aggregateTableNames;
	private Map<String, TableEntry> tableIDsToURIs;
	private Spinner phoneTables;
	private Spinner aggregateTables;
	private String userId;
	private AggregateConnection conn;
	
	@Override
	public void onCreate(Bundle bund) {
		super.onCreate(bund);
		setContentView(R.layout.aggregate_activity);
		
		setTitle("ODK Tables > Aggregate");
		
		tableIDsToURIs = new HashMap<String, TableEntry>();
		
		TelephonyManager teleMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
		this.userId = teleMgr.getDeviceId();
		
		this.aggregateTables = (Spinner)findViewById(R.id.aggregate_activity_getAggregateTables);
		this.aggregateTables.setId(AGGREGATETABLESPIN_ID);
		
		this.phoneTables = (Spinner)findViewById(R.id.aggregate_activity_getPhoneTables);
		this.phoneTables.setId(TABLESPIN_ID);
		
		fillPhoneTableSpinnerList();
		makeButtonListeners();
		hideViews();
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
		
//		Button getList = (Button)findViewById(R.id.aggregate_activity_getAggregateTables_button);
//		getList.setOnClickListener(new getTableList());
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
	
	private void makeVisible() {
		System.out.println("made it to makeVisible");
		findViewById(R.id.aggregate_activity_createUser).setVisibility(View.VISIBLE);
		findViewById(R.id.aggregate_activity_deleteUser).setVisibility(View.VISIBLE);
		findViewById(R.id.aggregate_activity_user_uri_text).setVisibility(View.VISIBLE);
		findViewById(R.id.aggregate_activity_uploadTable).setVisibility(View.VISIBLE);
		findViewById(R.id.aggregate_activity_downloadTable).setVisibility(View.VISIBLE);
		findViewById(R.id.aggregate_activity_deleteTable).setVisibility(View.VISIBLE);
		findViewById(R.id.aggregate_activity_getAggregateTables_text).setVisibility(View.VISIBLE);
		this.aggregateTables.setVisibility(View.VISIBLE);
		findViewById(R.id.aggregate_activity_getAggregateTables_button).setVisibility(View.VISIBLE);
		findViewById(R.id.aggregate_activity_userName_text).setVisibility(View.VISIBLE);
		findViewById(R.id.aggregate_activity_userName).setVisibility(View.VISIBLE);
		findViewById(R.id.aggregate_activity_getPhoneTables_text).setVisibility(View.VISIBLE);
		this.phoneTables.setVisibility(View.VISIBLE);
	}
	
	private void fillPhoneTableSpinnerList() {
		Map<String, String> tableMap = (new TableList()).getAllTableList();
		phoneTableNames = new String[tableMap.size()];
		int counter = 0;
		for(String tableId : tableMap.keySet()) {
			System.out.println("tableId"+tableId);
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

	private String getCurrentPhoneTableName() {
		int pos = this.phoneTables.getSelectedItemPosition();
		if (this.phoneTableNames.length != 0) {
			return phoneTableNames[pos];
		} else {
			showDialog(TABLE_NOEXIST);
			return null;
		}
	}
	
	private String getCurrentAggregateTableName() {
		int pos = this.aggregateTables.getSelectedItemPosition();
		if (this.aggregateTableNames.length != 0) {
			return aggregateTableNames[pos];
		} else {
			showDialog(TABLE_NOEXIST);
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
		adBuilder = adBuilder.setMessage(message);
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
//		case MESSAGE_DIALOG:
//			return getDialog("");
		case IN_PROGRESS_DIALOG:
			return getDialog("In progress...");
		case UPLOADTABLE_FAILED:
			return getDialog("Failed to upload table.");
		case UPLOADTABLE_SUCCESS:
			return getDialog("Table uploaded.");
		case DOWNLOADTABLE_FAILED:
			return getDialog("Failed to download table.");
		case DOWNLOADTABLE_SUCCESS:
			return getDialog("Table downloaded.");
		case CREATEUSER_FAILED:
			return getDialog("Failed to create user.");
		case CREATEUSER_SUCCESS:
			return getDialog("User created.");
		case DELETEUSER_FAILED:
			return getDialog("Failed to delete user and assosciated tables.");
		case DELETEUSER_SUCCESS:
			return getDialog("User and assosciated tables deleted.");
		case GETTABLELIST_FAILED:
			return getDialog("Failed to get table list.");
		case GETTABLELIST_SUCCESS:
			return getDialog("Table lists retrieved.");
		case GETUSERURI_FAILED:
			return getDialog("Failed to acquire user uri.");
		case GETUSERURI_SUCCESS:
			return getDialog("User uri acquired.");
		case CONNECTION_FAILED:
			return getDialog("Failed to connect.");
		case CONNECTION_SUCCESS:
			return getDialog("Connected.");
		case DELETETABLE_FAILED:
			return getDialog("Failed to delete table.");
		case DELETETABLE_SUCCESS:
			return getDialog("Table deleted.");
		default:
			throw new IllegalArgumentException();
		}
	}

	private class connectListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			showDialog(IN_PROGRESS_DIALOG);
			connectUser();
		}
	}
	
	private class downloadListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			showDialog(IN_PROGRESS_DIALOG);
			downloadTable();
		}
	}

	private class deleteListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			showDialog(IN_PROGRESS_DIALOG);
			deleteTable();
		}
	}
	
	private class createUserListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			showDialog(IN_PROGRESS_DIALOG);
			createUser();
		}
	}
	
	private class deleteUserListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			showDialog(IN_PROGRESS_DIALOG);
			deleteUser();
		}
	}

	private class uploadListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			showDialog(IN_PROGRESS_DIALOG);
			uploadTable();
		}
	}
	
//	private class getTableList implements OnClickListener {
//		@Override
//		public void onClick(View v) {
//			showDialog(IN_PROGRESS_DIALOG);
//			getTablesOnAggregate();
//		}
//	}
	
	private void uploadTable() {
		String tableName = this.getCurrentPhoneTableName();
		String tableID = (new Integer(new TableList().getTableID(tableName))).toString();
		Table current = (new DataTable(tableID)).getCompleteTable();
		
		List<String> header = current.getHeader();
		List<String> updated = new ArrayList<String>();
		
		for (String name: header) {
			if (!name.matches("[^0-9a-zA-Z].*|[^_].*")) {
				name = name.replaceAll("[^0-9a-zA-Z]|[^_]", "");
			}
			name = "COL_" + name;
			System.out.println("newColName: " + name);
			updated.add(name);
		}
		
		this.createTable(conn, tableName, tableID, current, updated);
		this.insertRows(conn, tableID, current, updated);
		dismissDialog(IN_PROGRESS_DIALOG);
	}

	private void insertRows(AggregateConnection conn, String tableID, Table table, List<String> header) { 
		List<Row> rows = new ArrayList<Row>();
		ArrayList<String> data = table.getData();
		ArrayList<Integer> rowIDs = table.getRowID();
		
		int width = header.size();
		for (int i = 0; i < table.getHeight(); i++) {
			Row row = new Row("" + rowIDs.get(i));
			for (int j = 0; j < width; j++) {
				String name = header.get(j);
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
        
		for (Row row: rows) {
			System.out.println("rowID: "+row.getRowId() +", data: "+ row.getColumnValuePairs());
		}
		
//        check exceptions and work accordingly to those
        try {
			conn.insertRows(userId, tableID, rows);
		} catch (RowAlreadyExistsException e) {
			dismissDialog(IN_PROGRESS_DIALOG);
			showDialog(UPLOADTABLE_FAILED);
			System.out.println("row already exists");
			if (debug) {
				e.printStackTrace();
			}
			return;
		} catch (TableDoesNotExistException e) {
			dismissDialog(IN_PROGRESS_DIALOG);
			showDialog(UPLOADTABLE_FAILED);
			System.out.println("table does not exist");
			if (debug) {
				e.printStackTrace();
			}
			return;
		} catch (Exception e) {
			dismissDialog(IN_PROGRESS_DIALOG);
			showDialog(UPLOADTABLE_FAILED);
			if (debug) {
				e.printStackTrace();
			}
			return;
		}
		showDialog(UPLOADTABLE_SUCCESS);
	}
	
	private void createTable(AggregateConnection conn, String tableName, String tableID, Table table, List<String> header) {
		//if table doesnt exist already
		List<Column> columns = new ArrayList<Column>();
		
		for (int i = 0; i < header.size(); i++ ) {
			String name = header.get(i);
			Column col = new Column(name, DataType.STRING, true);
			columns.add(col);
			System.out.println("column name: "+col.getName());
		}
		
		for (Column col: columns) {
			System.out.println("column name: "+col.getName());
		}
		
		try {
			conn.createTable(userId, tableID, tableName, columns);
		} catch (TableAlreadyExistsException e) {
			//DELETE TABLE
			dismissDialog(IN_PROGRESS_DIALOG);
			showDialog(UPLOADTABLE_FAILED);
//			try {
//				conn.deleteTable(userId, tableID);
//				conn.createTable(userId, tableID, tableName, columns);
//			} catch (Exception e1) {
//				dismissDialog(IN_PROGRESS_DIALOG);
//				showDialog(UPLOADTABLE_FAILED);
//				if (debug) {
//					e1.printStackTrace();
//				}
//				return;
//			}
			if (debug) {
				e.printStackTrace();
			}
			return;
		} catch (Exception e) {
			dismissDialog(IN_PROGRESS_DIALOG);
			showDialog(UPLOADTABLE_FAILED);
			if (debug) {
				e.printStackTrace();
			}
			return;
		}
		showDialog(UPLOADTABLE_SUCCESS);
	}
	
	private void deleteTable() {
		String tableName = this.getCurrentAggregateTableName();
//		String tableId = (new Integer(new TableList().getTableID(tableName))).toString();
		
		System.out.println("deletion.name: " + tableName + ", userId: " + userId);
		
		try {
			conn.deleteTable(userId, tableName);
		} catch (Exception e) {
			dismissDialog(IN_PROGRESS_DIALOG);
			showDialog(DELETETABLE_FAILED);
			if (debug) {
				e.printStackTrace();
			}
			return;
		}
		dismissDialog(IN_PROGRESS_DIALOG);
		showDialog(DELETETABLE_SUCCESS);
	}

	public void createUser() {
		String userName = ((EditText)findViewById(R.id.aggregate_activity_userName)).getText().toString();
		
		try {
			conn.createUser(userId, userName);
		} catch (UserAlreadyExistsException e) {
			dismissDialog(IN_PROGRESS_DIALOG);
			showDialog(CREATEUSER_FAILED);
			System.out.println("user already exists.");
			if (debug) {
				e.printStackTrace();
			}
			return;
		} catch (Exception e) {
			dismissDialog(IN_PROGRESS_DIALOG);
			showDialog(CREATEUSER_FAILED);
			if (debug) {
				e.printStackTrace();
			}
			return;
		}
		dismissDialog(IN_PROGRESS_DIALOG);
		showDialog(CREATEUSER_SUCCESS);
		getUserURI();
	}

	public void connectUser() {
		String url = "http://" + 
//		((EditText)findViewById(R.id.aggregate_activity_url)).getText().toString()
		"the-dylan-price"
		+ ".appspot.com/";
		setConnection(url);
		if (conn == null) {
			return;
		}
		makeVisible();
		findViewById(R.id.aggregate_activity_connect).setVisibility(View.GONE);
		findViewById(R.id.aggregate_activity_url).setVisibility(View.GONE);
		findViewById(R.id.aggregate_activity_url_text).setVisibility(View.GONE);
		getTablesOnAggregate();
	}
	

	public void getTablesOnAggregate() {
		//get list of tables on aggregate
		org.opendatakit.aggregate.odktables.client.TableList aggTblLst = new org.opendatakit.aggregate.odktables.client.TableList();
		try {
			aggTblLst = conn.listTables();
			dismissDialog(IN_PROGRESS_DIALOG);
			showDialog(GETTABLELIST_SUCCESS);
		} catch (Exception e) {
			dismissDialog(IN_PROGRESS_DIALOG);
			showDialog(GETTABLELIST_FAILED);
			if (debug) {
				e.printStackTrace();
			}
		}
		fillAggTableListSpinner(aggTblLst);
	}

	
	private void fillAggTableListSpinner(org.opendatakit.aggregate.odktables.client.TableList aggTblLst) {
		this.aggregateTableNames = new String[aggTblLst.size()];
		tableIDsToURIs = new HashMap<String, TableEntry>();
		int counter = 0;
		for (TableEntry entry: aggTblLst) {
			this.aggregateTableNames[counter] = entry.getTableId()
				;
//				+ " (URI: " + entry.getUserUri() + ", Username: " + entry.getUserName() + ")";
			this.tableIDsToURIs.put(entry.getTableId(), entry);
			counter++;
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, this.aggregateTableNames);
		adapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);
		this.aggregateTables.setAdapter(adapter);
		this.aggregateTables.setSelection(0);
	}

	public void setConnection(String url) {
		AggregateConnection conn = null;
		try {
			conn = new AggregateConnection(
					new URI(url));
			showDialog(CONNECTION_SUCCESS);
		} catch (URISyntaxException e) {
			showDialog(CONNECTION_FAILED);
			if (debug) {
				e.printStackTrace();
			}
		}
		System.out.println("conn is not set? "+conn == null );
		this.conn = conn;
		getUserURI();
	}

	private void getUserURI() {
		TextView text = (TextView) findViewById(R.id.aggregate_activity_user_uri_text);
		String uri = "Phone ID on aggregate: ";
		try {
			uri += conn.getUserUri(userId);
		} catch (UserDoesNotExistException e) {
			dismissDialog(IN_PROGRESS_DIALOG);
			System.out.println("User does not exist.");
			if (debug) {
				e.printStackTrace();
			}
			return;
		} catch (Exception e) {
			dismissDialog(IN_PROGRESS_DIALOG);
			if (debug) {
				e.printStackTrace();
			}
			return;
		}
		text.setText(uri);
	}

	public void downloadTable() {
		String tableName = this.getCurrentAggregateTableName();
		System.out.println("got table name: " + tableName);
		for (String s: this.tableIDsToURIs.keySet()) {
				System.out.println("map has key: " + s + ", value:" + this.tableIDsToURIs.get(s));
			}
		if (!this.tableIDsToURIs.containsKey(tableName)) {
			showDialog(DOWNLOADTABLE_FAILED);
			System.out.println("table does not exist");
			return;
		}
		String userUri = this.tableIDsToURIs.get(tableName).getUserUri();
		List<Row> rows;
//		System.out.println("table to be retrieved: " + tableName);
		try {
			rows = conn.getRows(userUri, tableName);
		} catch (TableDoesNotExistException e) {
			dismissDialog(IN_PROGRESS_DIALOG);
			showDialog(DOWNLOADTABLE_FAILED);
			System.out.println("table does not exist");
			if (debug) {
				e.printStackTrace();
			}
			return;
		} catch (UserDoesNotExistException e) {
			dismissDialog(IN_PROGRESS_DIALOG);
			showDialog(DOWNLOADTABLE_FAILED);
			System.out.println("user does not exist");
			if (debug) {
				e.printStackTrace();
			}
			return;
		} catch (Exception e) {
			dismissDialog(IN_PROGRESS_DIALOG);
			showDialog(DOWNLOADTABLE_FAILED);
			if (debug) {
				e.printStackTrace();
			}
			return;
		}
		
		tableName = ((EditText)findViewById(R.id.aggregate_activity_userName)).getText().toString();
		
		for (Row row: rows) {
			System.out.println("rowID: "+row.getRowId() +", data: "+ row.getColumnValuePairs());
		}
		
		//======================================================
		
//		ArrayList<String> header = new ArrayList<String>(rows.get(0).getColumnValuePairs().keySet());
		if (rows.size() == 0) {
			//cannot have empty table
			return;
		}
		ArrayList<String> heads = new ArrayList<String>();
		for (String s: rows.get(0).getColumnValuePairs().keySet()) {
			heads.add(s.substring(4).toLowerCase());
		}
		
		TableList tl = new TableList();
		String res = tl.registerNewTable(tableName);
		if(res != null) {
			throw new IllegalArgumentException(res);
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
		Integer tID = tl.getTableID(tableName);
		String tableID = tID.toString();
		DefaultsManager dm = new DefaultsManager(tableID);
		TableProperty tp = new TableProperty(tableID);
		ArrayList<String> colOrder = tp.getColOrderArrayList();
		for(String col : heads) {
            if(!col.equals("_phonenumberin") && !col.equals("_timestamp")) {
                Log.d("csvi", "starting col add:" + col);
                dm.prepForNewCol(col);
                Log.d("csvi", "just called dm.prepForNewCol");
                colOrder.add(col);
            }
		}
		tp.setColOrder(colOrder);
		
		System.out.println("so i set up the columns now");
		
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
			System.out.println("addin row: " + cv);
			try {
			    data.addRow(cv, pn, ts);
			} catch(IllegalArgumentException e) {
			    // TODO: something to handle invalid values
			}
		}
		
//		TableList lst = new TableList();
//		//might already exist, fix this
//		
//		String temp = lst.registerNewTable("farmerJohn");
//		String copy = tableName;
//		int counter = 1;
////		&& temp.equals("Table already exists.")
//		while (temp != null ) {
//			copy = tableName+ "(" + counter + ")";
//			System.out.println("error here registering table?");
//			temp = lst.registerNewTable(copy); 
//			counter++;
//		}
//		
//		System.out.println("registered table successfully.");
//		
//		int tableId = lst.getTableID(copy);
//		System.out.println("got table id");
//		DataTable table = new DataTable("" + tableId);
//		System.out.println("initialized data table");
//		
//		if (!rows.isEmpty() && rows.size() > 0) {
//			Row r = rows.get(0);
//			Map<String, String> temp3 = r.getColumnValuePairs();
//			for (String s: temp3.keySet()) {
//				if (s.startsWith("COL_", 0)) {
//					s = s.substring(4).toLowerCase();;
//				}
//				if (!s.equals("_phonenumberin") && !s.equals("_timestamp")) {
//					table.addNewColumn(s);
//					System.out.println("added column: " + s);
//				}
//			}
//		}
//		
//		System.out.println("added columns to table");
//		
//		for (Row row: rows) {
//			Map<String, String> temp3 = row.getColumnValuePairs();
//			int size = temp3.keySet().size();
//			String[] headers = new String[size];
//			String[] values = new String[size];
//			String time = "";
//			String phone = "";
//			counter = 0;
//			for (String s: temp3.keySet()) {
//				headers[counter] = s;
//				if (s.equals("COL__TIMESTAMP")) {
//					time = temp3.get(s);
//				} else if (s.equals("COL__PHONENUMBERIN")) {
//					phone = temp3.get(s);
//				}
//				values[counter] = temp3.get(s);
//				System.out.println("string arrays: " + headers[counter] + " | " + values[counter]);
//				counter++;
//			}
//			ContentValues vals = getValues(headers, values);
//			table.addRow(vals, phone, time);
//			System.out.println("added row with: " + vals);
//		}
//		
//		Table tbl = table.getTable();
//		System.out.println(tbl.getData());
		
//		System.out.println("table retrieved: ");
//		for (Row r: rows) {
//			System.out.print("row: " + r.getRowId()+", ");
//			Map<String, String> columns = r.getColumnValuePairs();
//			System.out.print("ColumnName/Value:");
//			for (String s: columns.keySet()) {
//				System.out.print(" "+ s + "/" + columns.get(s));
//			}
//			System.out.println();
//		}
		
		dismissDialog(IN_PROGRESS_DIALOG);
		showDialog(DOWNLOADTABLE_SUCCESS);
		
//		rowID: 1, data: {COL_QUALITY=VAL_goood, COL__TIMESTAMP=VAL_, COL_COST=VAL_5, COL__PHONENUMBERIN=VAL_, COL_HEINZ=VAL_catsup}
//		rowID: 2, data: {COL_QUALITY=VAL_pooor, COL__TIMESTAMP=VAL_, COL_COST=VAL_3, COL__PHONENUMBERIN=VAL_, COL_HEINZ=VAL_ketchup}
//		rowID: 3, data: {COL_QUALITY=VAL_duh best, COL__TIMESTAMP=VAL_, COL_COST=VAL_1, COL__PHONENUMBERIN=VAL_, COL_HEINZ=VAL_tomatoe sauce}
//		rowID: 4, data: {COL_QUALITY=VAL_meh, COL__TIMESTAMP=VAL_, COL_COST=VAL_0, COL__PHONENUMBERIN=VAL_, COL_HEINZ=VAL_idk}

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
	
//	private ContentValues getValues(String[] header, String[] rows) {
//		ContentValues vals = new ContentValues();
//		for(int i=0; i<header.length; i++) {
//			String head = header[i];
//			String row = rows[i].substring(4).toLowerCase();
//			if (head.startsWith("COL_", 0)) {
//				head = head.substring(4).toLowerCase();;
//			}
//			if (row.startsWith("VAL_", 0)) {
//				row = row.substring(4).toLowerCase();;
//			}
////			String head = "`" + header[i] + "`";
//			if (!head.equals("_phonenumberin") && !head.equals("_timestamp")) {
//				vals.put(head, row);
//			}
//		}
//		return vals;
//	}
	
	public void deleteUser() {
		try {
			conn.deleteUser(userId);
		} catch (Exception e) {
			dismissDialog(IN_PROGRESS_DIALOG);
			showDialog(DELETEUSER_FAILED);
			return;
		}
		showDialog(DELETEUSER_SUCCESS);
		getUserURI();
	}
}
