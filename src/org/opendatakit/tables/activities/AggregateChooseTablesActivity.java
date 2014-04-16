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

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.KeyValueStoreType;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.utils.TableFileUtils;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListActivity;

public class AggregateChooseTablesActivity extends SherlockListActivity {

  private String appName;
  private ListView tablesView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    appName = getIntent().getStringExtra(Controller.INTENT_KEY_APP_NAME);
    if ( appName == null ) {
      appName = TableFileUtils.getDefaultAppName();
    }
    setContentView(R.layout.aggregate_choose_tables_activity);

    setListAdapter(new ArrayAdapter<TableProperties>(this,
        android.R.layout.simple_list_item_multiple_choice,
        getServerDataTables()));

    final ListView listView = getListView();

    listView.setItemsCanFocus(false);
    listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

    int count = listView.getCount();
    for (int i = 0; i < count; i++) {
      TableProperties tp = (TableProperties) listView.getItemAtPosition(i);
      if (tp.isSetToSync()) {
        listView.setItemChecked(i, true);
      }
    }
  }

  /*
   * This should only display the data tables that are in the server KVS.
   * An invariant that must be maintained is that any table in the server KVS
   * must also have an "isSetToSync" entry in the sync KVS.
   */
  private TableProperties[] getServerDataTables() {
    TableProperties[] activeProps =
        TableProperties.getTablePropertiesForAll(this, appName, KeyValueStoreType.ACTIVE);
    // silently promote all active values to default if there is no default
    TableProperties[] defaultProps =
        TableProperties.getTablePropertiesForAll(this, appName, KeyValueStoreType.DEFAULT);
    for ( int i = 0 ; i < activeProps.length ; ++i ) {
      boolean found = false;
      for ( int j = 0 ; j < defaultProps.length ; ++j ) {
        if ( defaultProps[j].getTableId().equals(activeProps[i].getTableId()) ) {
          found = true;
          break;
        }
      }
      if ( !found ) {
        activeProps[i].setCurrentAsDefaultPropertiesForTable();
      }
    }
    // silently promote all default values to server sync if there is no server sync
    TableProperties[] serverProps =
        TableProperties.getTablePropertiesForAll(this, appName, KeyValueStoreType.SERVER);
    for ( int i = 0 ; i < defaultProps.length ; ++i ) {
      boolean found = false;
      for ( int j = 0 ; j < serverProps.length ; ++j ) {
        if ( serverProps[j].getTableId().equals(defaultProps[i].getTableId()) ) {
          found = true;
          break;
        }
      }
      if ( !found ) {
        defaultProps[i].copyDefaultToServerForTable();
      }
    }

    // and return the server properties
    return TableProperties.getTablePropertiesForAll(this, appName, KeyValueStoreType.SERVER);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    ListView listView = getListView();
    TableProperties tp =
        (TableProperties) listView.getItemAtPosition(position);
    boolean wantToSync;
    if (tp.isSetToSync()) {
      wantToSync = false;
    } else {
      wantToSync = true;
    }
    tp.setIsSetToSync(wantToSync);
  }
}
