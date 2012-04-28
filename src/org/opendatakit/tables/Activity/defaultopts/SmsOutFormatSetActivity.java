package org.opendatakit.tables.Activity.defaultopts;

import java.util.Map;

import org.opendatakit.tables.Database.TableProperty;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;

public class SmsOutFormatSetActivity extends ListActivity {
	
	private static final int ADD_FORMAT = 0;
	private static final int EDIT_FORMAT = 1;
	private static final int DELETE_FORMAT = 2;
	
	private TableProperty tp;
	private int[] idArr;
	String[] formatArr;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		int tableID = getIntent().getExtras().getInt("tableID");
		tp = new TableProperty(Integer.toString(tableID));
		init();
	}
	
	private void init() {
		registerForContextMenu(getListView());
		Map<Integer, String> formats = tp.getDefOutMsg();
		idArr = new int[formats.size()];
		formatArr = new String[formats.size()];
		int i = 0;
		for(int id : formats.keySet()) {
			idArr[i] = id;
			formatArr[i] = formats.get(id);
			i++;
		}
		setListAdapter(new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, formatArr));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, ADD_FORMAT, 0, "Add Default");
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
		case ADD_FORMAT:
			alertForNewFormat();
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, EDIT_FORMAT, 0, "Edit Format");
		menu.add(0, DELETE_FORMAT, 1, "Delete Format");
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info =
			(AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		int pos = info.position;
		switch(item.getItemId()) {
		case EDIT_FORMAT:
			alertForFormatEdit(idArr[pos], formatArr[pos]);
			return true;
		case DELETE_FORMAT:
			tp.removeDefOutMsg(idArr[pos]);
			init();
			return true;
		}
		return super.onContextItemSelected(item);
	}
	
	private void alertForNewFormat() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Add Format");
		final EditText input = new EditText(this);
		alert.setView(input);
		alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				tp.addDefOutMsg(input.getText().toString());
				init();
			}
		});
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {}
		});
		alert.show();
	}
	
	private void alertForFormatEdit(final int id, String currentVal) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Edit Format");
		final EditText input = new EditText(this);
		input.setText(currentVal);
		alert.setView(input);
		alert.setPositiveButton("Edit", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				tp.changeDefOutMsg(id, input.getText().toString());
				init();
			}
		});
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {}
		});
		alert.show();
	}
	
}
