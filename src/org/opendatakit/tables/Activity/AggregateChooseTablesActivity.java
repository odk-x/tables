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
import org.opendatakit.tables.data.TableProperties;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class AggregateChooseTablesActivity extends ListActivity {

  ListView tablesView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.aggregate_choose_tables_activity);

    setListAdapter(new ArrayAdapter<TableProperties>(this,
        android.R.layout.simple_list_item_multiple_choice, getTables()));

    final ListView listView = getListView();

    listView.setItemsCanFocus(false);
    listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

    int count = listView.getCount();
    for (int i = 0; i < count; i++) {
      TableProperties tp = (TableProperties) listView.getItemAtPosition(i);
      if (tp.isSynchronized()) {
        listView.setItemChecked(i, true);
      }
    }
  }

  private TableProperties[] getTables() {
    DbHelper dbh = DbHelper.getDbHelper(this);
    DataManager dm = new DataManager(dbh);
    return dm.getDataTableProperties();
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    ListView listView = getListView();
    TableProperties tp = (TableProperties) listView.getItemAtPosition(position);
    tp.setSynchronized(listView.isItemChecked(position));
  }
}
