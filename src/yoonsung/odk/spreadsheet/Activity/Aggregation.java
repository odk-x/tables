package yoonsung.odk.spreadsheet.Activity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.opendatakit.aggregate.odktables.client.AggregateConnection;
import org.opendatakit.aggregate.odktables.client.Column;
import org.opendatakit.aggregate.odktables.client.Row;
import org.opendatakit.aggregate.odktables.client.TableEntry;
import org.opendatakit.common.persistence.DataField.DataType;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.DataStructure.Table;
import yoonsung.odk.spreadsheet.Database.DataTable;
import yoonsung.odk.spreadsheet.Database.TableList;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class Aggregation extends Activity {

	private static int TABLESPIN_ID = 0;
	private static final int DELETETABLE_FAILED_DIALOG = 1;
	private static final int TABLE_EXISTS_DIALOG = 2;
	private static final int CONNECTION_FAILED_DIALOG = 3;
	private static final int CREATETABLE_FAILED_DIALOG = 4;
	private static final int INSERTROWS_FAILED_DIALOG = 5;
//	private static final int UPLOAD_PROGRESS_DIALOG = 6;
//	private static final int DELETE_PROGRESS_DIALOG = 7;
	private static final int IN_PROGRESS_DIALOG = 6;
	private static final int LISTTABLE_FAILED_DIALOG = 7;
	private static final int AGGREGATETABLESPIN_ID = 8;
	private static final int CREATEUSER_FAILED_DIALOG = 9;
	
	private String[] phoneTableNames;
	private String[] aggregateTableNames;
	private Spinner phoneTables;
	private Spinner aggregateTables;
	private String userId;
	private AggregateConnection conn;
	
	@Override
	public void onCreate(Bundle bund) {
		super.onCreate(bund);
		setContentView(R.layout.aggregate_activity);
		
		setTitle("ODK Tables > Aggregate");
		
		TelephonyManager teleMgr = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
		this.userId = teleMgr.getDeviceId();
		
		fillPhoneTableSpinnerList();
		
		Button createUser = (Button)findViewById(R.id.aggregate_activity_createUser);
		createUser.setOnClickListener(new createUserListener());
		
		Button connect = (Button)findViewById(R.id.aggregate_activity_connect);
		connect.setOnClickListener(new connectListener());
		
		Button upload = (Button)findViewById(R.id.aggregate_activity_upload);
		upload.setOnClickListener(new uploadListener());
		
		Button download = (Button)findViewById(R.id.aggregate_activity_download);
		download.setOnClickListener(new downloadListener());
		
		Button delete = (Button)findViewById(R.id.aggregate_activity_delete);
		delete.setOnClickListener(new deleteListener());
		
//		String pwd = ((EditText)findViewById(R.id.aggregate_activity_pwd)).getText().toString();
//		String url = ((EditText)findViewById(R.id.aggregate_activity_url)).getText().toString();
//		String def = "http://the-dylan-price.appspot.com/";
		
	}
	
	private void fillPhoneTableSpinnerList() {
		this.phoneTables = (Spinner)findViewById(R.id.aggregate_activity_tables);
		this.phoneTables.setId(TABLESPIN_ID);
		Map<String, String> tableMap = (new TableList()).getAllTableList();
		phoneTableNames = new String[tableMap.size()];
		int counter = 0;
		for(String tableName : tableMap.keySet()) {
			System.out.println(tableName);
			phoneTableNames[counter] = tableMap.get(tableName);
			counter++;
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, phoneTableNames);
		adapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);
		this.phoneTables.setAdapter(adapter);
		this.phoneTables.setSelection(0);
	}

	private String getCurrentTableName() {
		int pos = this.phoneTables.getSelectedItemPosition();
//		String tableName = "";
		if (this.phoneTableNames.length != 0) {
			return phoneTableNames[pos];
		} else {
			showDialog(TABLE_EXISTS_DIALOG);
			return null;
		}
//		TableList tList = new TableList();
//		if(!tList.isTableExist(tableName)) {
//			showDialog(TABLE_EXISTS_DIALOG);
//			return null;
//		}
//		return tableName;
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
		case DELETETABLE_FAILED_DIALOG:
			return getDialog("Failed to delete table.");
		case CREATETABLE_FAILED_DIALOG:
			return getDialog("Failed to create table.");
		case TABLE_EXISTS_DIALOG:
			return getDialog("Table does not exist.");
		case CONNECTION_FAILED_DIALOG:
			return getDialog("Failed to connect to given url.");
		case INSERTROWS_FAILED_DIALOG:
			return getDialog("Failed to insert rows to table.");
//		case UPLOAD_PROGRESS_DIALOG:
//			return getDialog("Uploading in progress...");
//		case DELETE_PROGRESS_DIALOG:
//			return getDialog("Deletion in progress...");
		case IN_PROGRESS_DIALOG:
			return getDialog("In progress...");
		case LISTTABLE_FAILED_DIALOG:
			return getDialog("Failed to load list of tables on aggregate.");
		case CREATEUSER_FAILED_DIALOG:
			return getDialog("Failed to create new user.");
		default:
			throw new IllegalArgumentException();
		}
	}
	
	private void uploadSubmission() {
		String tableName = this.getCurrentTableName();
		String tableID = (new Integer(new TableList().getTableID(tableName))).toString();
		Table current = (new DataTable(tableID)).getCompleteTable();
//		System.out.println("tableName: " + tableName);
//		System.out.println("tableId: " + tableID);
//		System.out.println("height: " + current.getHeight() + " Width: " + current.getWidth() + "data: " + current.getData() + "footer: " +current.getFooter() + "header: " +current.getHeader());
		
		showDialog(IN_PROGRESS_DIALOG);
		this.createTable(conn, tableName, tableID, current);
		this.insertRows(conn, tableID, current);
		dismissDialog(IN_PROGRESS_DIALOG);
	}
	
	private void insertRows(AggregateConnection conn, String tableID, Table table) { 
		List<Row> rows = new ArrayList<Row>();
		ArrayList<Integer> rowIds = table.getRowID();
        for (int i = 0; i < table.getHeight(); i++) {
        	System.out.println("Creating row:");
        	System.out.print("rowid: "+rowIds.get(i).toString() + " ");
        	Row row = new Row(rowIds.get(i).toString());
        	ArrayList<String> rowValues = table.getRow(i);
        	for (int j = 0; j < table.getWidth(); j++) {
        		String name = table.getColName(i);
        		if (!name.matches("[0-9a-zA-Z_].*")) {
    				name = name.replaceAll("[^a-zA-Z0-9]", "");
    			}
    			name = "COL_" + name;
    			System.out.print("columnName: " + name + " dataz:" + rowValues.get(i)+ " ");
        		row.setColumn(name, rowValues.get(j));
        	}
        	System.out.println();
//        	System.out.println(row.toString());
        	rows.add(row);
        }
        
        //check exceptions and work accordingly to those
        try {
			conn.insertRows(userId, tableID, rows);
		} catch (Exception e) {
			dismissDialog(IN_PROGRESS_DIALOG);
			showDialog(INSERTROWS_FAILED_DIALOG);
			return;
		}
	}
	
	private void createTable(AggregateConnection conn, String tableName, String tableID, Table table) {
		//if table doesnt exist already
		List<Column> columns = new ArrayList<Column>();
		
		for (int i = 0; i < table.getWidth(); i++ ) {
			String name = table.getColName(i);
			if (!name.matches("[0-9a-zA-Z_].*")) {
				name = name.replaceAll("[^a-zA-Z0-9]", "");
			}
			name = "COL_" + name;
			Column col = new Column(name, DataType.STRING, false);
			columns.add(col);
//			System.out.println(current.getColName(i));
//			System.out.println("column: " + col.toString());
		}
		System.out.println("columns: " + columns.toString());
		
		//check exceptions and work accordingly to those
		try {
			conn.createTable(userId, tableID, tableName, columns);
		} catch (Exception e) {
			dismissDialog(IN_PROGRESS_DIALOG);
			showDialog(CREATETABLE_FAILED_DIALOG);
			e.printStackTrace();
			return;
		}
	}
	
	private class uploadListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			uploadSubmission();
		}
	}
	
	private void deleteSubmission() {
		
		String tableName = this.getCurrentTableName();
		String tableId = (new Integer(new TableList().getTableID(tableName))).toString();
		
		showDialog(IN_PROGRESS_DIALOG);
		try {
			conn.deleteTable(userId, tableId);
		} catch (Exception e) {
			dismissDialog(IN_PROGRESS_DIALOG);
			showDialog(DELETETABLE_FAILED_DIALOG);
			return;
		}
		dismissDialog(IN_PROGRESS_DIALOG);
	}
	
	private class deleteListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			deleteSubmission();
		}
	}
	
	private class createUserListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			createUser();
		}
	}

	public void createUser() {
		String userName = ((EditText)findViewById(R.id.aggregate_activity_userName)).toString();
		
		showDialog(IN_PROGRESS_DIALOG);
		try {
			conn.createUser(userId, userName);
		} catch (Exception e) {
			dismissDialog(IN_PROGRESS_DIALOG);
			showDialog(CREATEUSER_FAILED_DIALOG);
			return;
		}
		dismissDialog(IN_PROGRESS_DIALOG);
	}
	
	private class connectListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			connectUser();
		}
	}
	
	private class downloadListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			downloadSubmission();
		}
	}

	public void connectUser() {
		String url = "http://" + 
		((EditText)findViewById(R.id.aggregate_activity_url)).getText().toString()
		+ ".appspot.com/";
		setConnection(url);
		
		org.opendatakit.aggregate.odktables.client.TableList aggTblLst;
		showDialog(IN_PROGRESS_DIALOG);
		try {
			aggTblLst = conn.listTables();
		} catch (Exception e) {
			dismissDialog(IN_PROGRESS_DIALOG);
			showDialog(LISTTABLE_FAILED_DIALOG);
			return;
		}
		dismissDialog(IN_PROGRESS_DIALOG);
		
		this.aggregateTables = (Spinner)findViewById(R.id.aggregate_activity_aggregateTables);
		this.aggregateTables.setId(AGGREGATETABLESPIN_ID);
		this.aggregateTableNames = new String[aggTblLst.size()];
		int counter = 0;
		Iterator<TableEntry> iter = aggTblLst.iterator();
		while (iter.hasNext()) {
			TableEntry entry = iter.next();
//			System.out.println(tableName);
			this.aggregateTableNames[counter] = entry.getTableId() 
				+ " (URI: " + entry.getUserUri() + ")";
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
		} catch (URISyntaxException e) {
			showDialog(CONNECTION_FAILED_DIALOG);
		}
		this.conn = conn;
	}

	public void downloadSubmission() {
	}
}
