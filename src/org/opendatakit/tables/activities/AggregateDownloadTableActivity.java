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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreManager;
import org.opendatakit.tables.data.KeyValueStoreSync;
import org.opendatakit.tables.data.Preferences;
import org.opendatakit.tables.data.SyncState;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableType;
import org.opendatakit.tables.sync.SyncProcessor;
import org.opendatakit.tables.sync.Synchronizer;
import org.opendatakit.tables.sync.aggregate.AggregateSynchronizer;
import org.opendatakit.tables.sync.exceptions.InvalidAuthTokenException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SyncResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListActivity;

public class AggregateDownloadTableActivity extends SherlockListActivity {

  private static final String TAG = AggregateDownloadTableActivity.class.getSimpleName();

  private AlertDialog.Builder finishDialog;
  private Preferences prefs;
  private String aggregateUrl;
  private String authToken;
  private List<String> tableIds;
  private List<String> tableNames;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ListView listView = getListView();

    listView.setItemsCanFocus(false);

    initializeDialogs();

    prefs = new Preferences(this);
    aggregateUrl = prefs.getServerUri();
    authToken = prefs.getAuthToken();

    Map<String, String> tables = getTables();

    if (tables == null) {
      finishDialog.setMessage(getString(R.string.error_contacting_server));
      finishDialog.show();
    } else if (tables.isEmpty()) {
      finishDialog.setMessage(getString(R.string.all_tables_downloaded));
      finishDialog.show();
    } else {
      tableIds = new ArrayList<String>();
      tableNames = new ArrayList<String>();
      for (String tableId : tables.keySet()) {
        tableIds.add(tableId);
        tableNames.add(tables.get(tableId));
      }
      setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, tableNames));
    }
  }

  private void initializeDialogs() {
    finishDialog = new AlertDialog.Builder(AggregateDownloadTableActivity.this);
    finishDialog.setCancelable(false);
    finishDialog.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        AggregateDownloadTableActivity.this.finish();
      }
    });
  }

  private Map<String, String> getTables() {
    GetTablesTask task = new GetTablesTask(aggregateUrl, authToken);
    task.execute();
    Map<String, String> tables = null;
    try {
      tables = task.get();
    } catch (ExecutionException e) {
      Log.i(TAG, "ExecutionException in getTables()", e);
    } catch (InterruptedException e) {
      Log.i(TAG, "InterrruptedException in getTables()", e);
    } catch (Exception e) {
      Log.e(TAG, "Exception in getTables()", e);
    }
    return tables;
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    String tableName = (String) getListView().getItemAtPosition(position);
    String tableId = tableIds.get(position);
    DownloadTableTask task = new DownloadTableTask(prefs.getAccount(), tableId, tableName);
    try {
      task.execute();
      task.get();
    } catch (Exception e) {
      Log.i(TAG, "Exception downloading table " + tableName, e);
      finishDialog.setMessage(getString(R.string.error_downloading_table));
      finishDialog.show();
    }
    finishDialog.setMessage(getString(R.string.downloaded_table, tableName));
    finishDialog.show();
  }

  private class GetTablesTask extends AsyncTask<Void, Void, Map<String, String>> {
    private String aggregateUrl;
    private String authToken;
    private ProgressDialog pd;

    public GetTablesTask(String aggregateUrl, String authToken) {
      this.aggregateUrl = aggregateUrl;
      this.authToken = authToken;
    }

    @Override
    protected void onPreExecute() {
      pd = ProgressDialog.show(AggregateDownloadTableActivity.this, getString(R.string.please_wait),
          getString(R.string.fetching_tables));
    }

    @Override
    protected Map<String, String> doInBackground(Void... args) {
      Synchronizer sync;
      try {
        sync = new AggregateSynchronizer(aggregateUrl, authToken);
      } catch (InvalidAuthTokenException e1) {
        Aggregate.invalidateAuthToken(authToken, AggregateDownloadTableActivity.this);
        return null;
      }

      // get tables from server
      Map<String, String> tables = null;
      try {
        tables = sync.getTables();
      } catch (IOException e) {
        Log.i(TAG, "Could not retrieve table list", e);
      } catch (Exception e) {
        Log.e(TAG, "Unexpected exception getting table list", e);
      }

      // filter tables to remove ones already downloaded
      if (tables != null) {
        DataManager dm = new DataManager(DbHelper.getDbHelper(AggregateDownloadTableActivity.this));
        // we're going to check for downloaded tables ONLY in the server store,
        // b/c there will only be UUID collisions if the tables have been
        // downloaded, which means they must be in the server KVS. The
        // probability of a user defined table, which would NOT have entries
        // in the server KVS, having the same UUID as another table is
        // virtually zero.
        TableProperties[] props = dm.getTablePropertiesForDataTables(
            KeyValueStore.Type.SERVER);
        Set<String> tableIds = tables.keySet();
        for (TableProperties tp : props) {
          String tableId = tp.getTableId();
          if (tableIds.contains(tableId)) {
            tables.remove(tableId);
          }
        }
      }

      return tables;
    }

    @Override
    protected void onPostExecute(Map<String, String> result) {
      pd.dismiss();
    }
  }

  private class DownloadTableTask extends AsyncTask<Void, Void, Void> {

    private String tableId;
    private String tableName;
    private String accountName;
    private ProgressDialog pd;

    public DownloadTableTask(String accountName, String tableId, String tableName) {
      this.accountName = accountName;
      this.tableId = tableId;
      this.tableName = tableName;
    }

    @Override
    protected void onPreExecute() {
      pd = ProgressDialog.show(AggregateDownloadTableActivity.this, getString(R.string.please_wait),
          getString(R.string.fetching_this_table, tableName));
    }

    @Override
    protected Void doInBackground(Void... params) {
      android.os.Debug.waitForDebugger();
      DbHelper dbh = DbHelper.getDbHelper(AggregateDownloadTableActivity.this);
//TODO the order of synching should probably be re-arranged so that you first
// get the table properties and column entries (ie the table definition) and
// THEN get the row data. This would make it more resilient to network failures
// during the process. along those lines, the same process should exist in the
// table creation on the phone. or rather, THAT should try and follow the same
// order.
      TableProperties tp = TableProperties.addTable(dbh, tableName, tableName,
          tableName, TableType.data, tableId, KeyValueStore.Type.SERVER);
      tp.setSyncState(SyncState.rest);
      tp.setSyncTag(null);

      Synchronizer synchronizer;
      try {
        synchronizer = new AggregateSynchronizer(aggregateUrl, authToken);
      } catch (InvalidAuthTokenException e) {
        Aggregate.invalidateAuthToken(authToken, 
            AggregateDownloadTableActivity.this);
        return null;
      }
      SyncProcessor processor = new SyncProcessor(dbh, synchronizer, 
          new DataManager(dbh), new SyncResult());
      processor.synchronizeTable(tp, true);
      // Aggregate.requestSync(accountName);
      // Now copy the properties from the server to the default to the active.
      KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
      // TODO: this code not working. these two methods gone wrong. 
      kvsm.mergeServerToDefaultForTable(tableId);
      kvsm.copyDefaultToActiveForTable(tableId);

      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      pd.dismiss();
    }

  }
}
