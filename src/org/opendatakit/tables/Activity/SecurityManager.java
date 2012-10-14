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

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.TableProperties;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class SecurityManager extends Activity {

    public static final String INTENT_KEY_TABLE_ID = "tableId";
    
    private TableProperties tp;
	private TableProperties[] tps;
	private TableProperties[] securityTps;
	
	private Spinner readSpin;
	private Spinner writeSpin;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.security_activity);
		
		// Set title of activity
		setTitle("ODK Tables > Security");

		// Settings
		String tableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
		DbHelper dbh = DbHelper.getDbHelper(this);
		tp = TableProperties.getTablePropertiesForTable(dbh, tableId,
		    KeyValueStore.Type.ACTIVE);
		tps = TableProperties.getTablePropertiesForAll(dbh,
		    KeyValueStore.Type.ACTIVE);
		securityTps = TableProperties.getTablePropertiesForSecurityTables(dbh,
		    KeyValueStore.Type.ACTIVE);
    
        // Set current table name
        TextView tv = (TextView)findViewById(R.id.security_activity_table_name);
        String currentTableName = tp.getDisplayName();
        tv.setText(currentTableName);
		
		// Get current read & write security table value
		String currentReadT = tp.getReadSecurityTableId();
		String currentWriteT = tp.getWriteSecurityTableId();
		
		// Get a list of available security tables
		String[] secTables = new String[securityTps.length+1];
		secTables[0] = "";
		int i = 1;
		int readTableIndex = 0;
		int writeTableIndex = 0;
		for (TableProperties secTp : securityTps) {
		    String id = secTp.getTableId();
		    if (id.equals(currentReadT)) {
		        readTableIndex = i;
		    }
		    if (id.equals(currentWriteT)) {
		        writeTableIndex = i;
		    }
		    secTables[i] = secTp.getDisplayName();
		    i++;
		}
		
		// Create a spinner for readable 
		readSpin = (Spinner)findViewById(R.id.security_activity_read_spinner);
		ArrayAdapter<String> readAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, secTables);
		readAdapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);
		readSpin.setAdapter(readAdapter);
		readSpin.setOnItemSelectedListener(new SpinnerSelectionListener("read"));
		readSpin.setSelection(readTableIndex);
		
		// Create a spinner for writable
		writeSpin = (Spinner)findViewById(R.id.security_activity_write_spinner);
		ArrayAdapter<String> writeAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, secTables);
		writeAdapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);
		writeSpin.setAdapter(writeAdapter);
		writeSpin.setOnItemSelectedListener(new SpinnerSelectionListener("write"));
		writeSpin.setSelection(writeTableIndex);
	}
	
	private class SpinnerSelectionListener implements OnItemSelectedListener {
		
		private String type;
		
		SpinnerSelectionListener(String type) {
			this.type = type;
		}
		
		@Override
		public void onItemSelected(AdapterView<?> adapter, View view, int position,
				long arg3) {
			String item = (String)adapter.getItemAtPosition(position);
			Log.e("readSpinner", ""+item);
			String tableId = (position == 0) ? null :
			    securityTps[position - 1].getTableId();
			if (type.equals("read")) {
				tp.setReadSecurityTableId(tableId);
			} else if (type.equals("write")) {
				tp.setWriteSecurityTableId(tableId);
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			;
		}
	}
	
}