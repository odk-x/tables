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
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreManager;
import org.opendatakit.tables.data.KeyValueStoreSync;
import org.opendatakit.tables.data.TableProperties;

import com.actionbarsherlock.app.SherlockListActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class AggregateChooseTablesActivity extends SherlockListActivity {

  ListView tablesView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.aggregate_choose_tables_activity);

    setListAdapter(new ArrayAdapter<TableProperties>(this,
        android.R.layout.simple_list_item_multiple_choice, 
        getServerDataTables()));

    final ListView listView = getListView();

    listView.setItemsCanFocus(false);
    listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

    int count = listView.getCount();
    DbHelper dbh = DbHelper.getDbHelper(this);
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    for (int i = 0; i < count; i++) {
      TableProperties tp = (TableProperties) listView.getItemAtPosition(i);
      // hilary's original
      //if (tp.isSynchronized()) {
      if (kvsm.getSyncStoreForTable(tp.getTableId()).isSetToSync()) {
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
    DbHelper dbh = DbHelper.getDbHelper(this);
    DataManager dm = new DataManager(dbh);
    // hilary's original--return dm.getDataTableProperties();
    return dm.getTablePropertiesForDataTables(KeyValueStore.Type.SERVER);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    ListView listView = getListView();
    TableProperties tp = 
        (TableProperties) listView.getItemAtPosition(position);
    // hilary's original
    //tp.setSynchronized(listView.isItemChecked(position));
    DbHelper dbh = DbHelper.getDbHelper(this);
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStoreSync syncKVS = kvsm.getSyncStoreForTable(tp.getTableId());
    syncKVS.setIsSetToSync(listView.isItemChecked(position));
  }
}
