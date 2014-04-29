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

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.opendatakit.aggregate.odktables.rest.SyncState;
import org.opendatakit.aggregate.odktables.rest.entity.TableResource;
import org.opendatakit.common.android.data.Preferences;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.sync.SyncProcessor;
import org.opendatakit.common.android.sync.Synchronizer;
import org.opendatakit.common.android.sync.TableResult;
import org.opendatakit.common.android.sync.aggregate.AggregateSynchronizer;
import org.opendatakit.common.android.sync.aggregate.SyncTag;
import org.opendatakit.common.android.sync.exceptions.InvalidAuthTokenException;
import org.opendatakit.common.android.sync.exceptions.SchemaMismatchException;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utils.NameUtil;
import org.opendatakit.tables.R;
import org.opendatakit.tables.utils.TableFileUtils;
import org.springframework.web.client.ResourceAccessException;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SyncResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class AggregateDownloadTableActivity extends ListActivity {

  private static final String TAG = AggregateDownloadTableActivity.class.getSimpleName();

  private String appName;
  private AlertDialog.Builder finishDialog;
  private Preferences prefs;
  private String aggregateUrl;
  private String authToken;
  private List<String> tableIds;
  private List<String> tableNames;
  private List<String> tableDefinitionUris;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    appName = getIntent().getStringExtra(Controller.INTENT_KEY_APP_NAME);
    if ( appName == null ) {
      appName = TableFileUtils.getDefaultAppName();
    }

    final ListView listView = getListView();

    listView.setItemsCanFocus(false);

    initializeDialogs();

    prefs = new Preferences(this, appName);
    aggregateUrl = prefs.getServerUri();
    authToken = prefs.getAuthToken();

    GetTablesTask task = new GetTablesTask(appName, aggregateUrl, authToken);
    task.execute();

  }

  private void respondToTablesQuery(List<TableResource> tablesFromServer) {
    if (tablesFromServer == null) {
      finishDialog.setMessage(getString(R.string.error_contacting_server));
      finishDialog.show();
    } else if (tablesFromServer.isEmpty()) {
      finishDialog.setMessage(getString(R.string.all_tables_downloaded));
      finishDialog.show();
    } else {
      tableIds = new ArrayList<String>();
      tableNames = new ArrayList<String>();
      tableDefinitionUris = new ArrayList<String>();
      for (TableResource table : tablesFromServer) {
        tableIds.add(table.getTableId());
        tableNames.add(table.getDisplayName());
        tableDefinitionUris.add(table.getDefinitionUri());
      }
      setListAdapter(new ArrayAdapter<String>(this,
          android.R.layout.simple_list_item_1, tableNames));
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

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    String displayName = (String) getListView().getItemAtPosition(position);
    String tableId = tableIds.get(position);
    String tableDefinitionUri = tableDefinitionUris.get(position);
    String dbTableNameActive = NameUtil.createUniqueDbTableName(this, appName, tableId);
    DownloadTableTask task = new DownloadTableTask(prefs.getAccount(), tableId, dbTableNameActive, displayName, tableDefinitionUri);
    task.execute();
  }

  private class GetTablesTask extends AsyncTask<Void, Void, List<TableResource>> {
    private final String appName;
    private final String aggregateUrl;
    private final String authToken;
    private ProgressDialog pd;

    public GetTablesTask(String appName, String aggregateUrl, String authToken) {
      this.appName = appName;
      this.aggregateUrl = aggregateUrl;
      this.authToken = authToken;
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      pd = ProgressDialog.show(AggregateDownloadTableActivity.this,
          getString(R.string.please_wait),
          getString(R.string.fetching_tables));
    }

    @Override
    protected List<TableResource> doInBackground(Void... args) {
      //android.os.Debug.waitForDebugger();
      Synchronizer sync;
      try {
        sync = new AggregateSynchronizer(appName, aggregateUrl, authToken);
      } catch (InvalidAuthTokenException e1) {
        Aggregate.invalidateAuthToken(authToken, AggregateDownloadTableActivity.this, appName);
        return null;
      }

      // get tables (tableId -> schemaETag) from server
      List<TableResource> tables = null;
      try {
        tables = sync.getTables();
      } catch (IOException e) {
        Log.i(TAG, "Could not retrieve table list", e);
      } catch (Exception e) {
        Log.e(TAG, "Unexpected exception getting table list", e);
      }

      // we may want to download data from the server even if we already have data locally.
//      // filter tables to remove ones already downloaded
//      if (tables != null) {
//        DbHelper dbh = DbHelper.getDbHelper(AggregateDownloadTableActivity.this, appName);
//        // we're going to check for downloaded tables ONLY in the server store,
//        // b/c there will only be UUID collisions if the tables have been
//        // downloaded, which means they must be in the server KVS. The
//        // probability of a user defined table, which would NOT have entries
//        // in the server KVS, having the same UUID as another table is
//        // virtually zero.
//        TableProperties[] props = TableProperties.getTablePropertiesForDataTables(dbh,
//            KeyValueStore.Type.SERVER);
//        for (TableProperties tp : props) {
//          for ( int i = 0 ; i < tables.size() ; ++i ) {
//            TableResource table = tables.get(i);
//            if ( table.getTableId().equals(tp.getTableId()) ) {
//              SyncTag serverTag = new SyncTag(table.getDataETag(), table.getPropertiesETag(), table.getSchemaETag());
//              if ( serverTag.equals(tp.getSyncTag()) ) {
//                tables.remove(i);
//              }
//              break;
//            }
//          }
//        }
//      }

      return tables;
    }

    @Override
    protected void onPostExecute(List<TableResource> result) {
      // TODO: this is probably broken!!!
      if ( pd != null ) {
        pd.dismiss();
      }
      AggregateDownloadTableActivity.this.respondToTablesQuery(result);
    }
  }

  private class DownloadTableTask extends AsyncTask<Void, Void, Void> {

    private final String accountName;
    private final String tableId;
    private final String dbTableName;
    private final String displayName;
    private final String tableDefinitionUri;
    private ProgressDialog pd;
    private TableResult downloadResult;


    public DownloadTableTask(String accountName, String tableId, String dbTableName, String displayName, String tableDefinitionUri) {
      this.accountName = accountName;
      this.tableId = tableId;
      this.dbTableName = dbTableName;
      this.displayName = displayName;
      this.tableDefinitionUri = tableDefinitionUri;
      this.downloadResult = null;
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      pd = ProgressDialog.show(AggregateDownloadTableActivity.this,
          getString(R.string.please_wait),
          getString(R.string.fetching_this_table, displayName));
    }

    @Override
    protected Void doInBackground(Void... params) {
      //android.os.Debug.waitForDebugger();

      Synchronizer synchronizer;
      try {
        synchronizer = new AggregateSynchronizer(appName, aggregateUrl, authToken);
      } catch (InvalidAuthTokenException e) {
        Aggregate.invalidateAuthToken(authToken,
            AggregateDownloadTableActivity.this, appName);
        return null;
      }

      SyncProcessor processor = new SyncProcessor(
    		  AggregateDownloadTableActivity.this, appName, synchronizer,
          new SyncResult());
      // We're going to add a check here for the framework directory. If we
      // don't have it, you also have to sync app level files the first time.
      ODKFileUtils.assertDirectoryStructure(appName);
      try {
        synchronizer.syncAppLevelFiles(false);
      } catch (ResourceAccessException e) {
        Log.e(TAG, "ResourceAccessException trying to pull app-level files for the " +
        		"first time during table download.");
        // TODO report failure properly
        e.printStackTrace();
        // It's not going to be deemed fatal at this point if the app
        // folder doesn't pull down. Perhaps eventually it should, but not
        // for now.
        return null;
      }

      TableProperties[] props = TableProperties.getTablePropertiesForAll(
    		  	AggregateDownloadTableActivity.this, appName);
      TableProperties tpOriginal = null;
      boolean tablePresent = false;
      for ( TableProperties p : props ) {
        if ( p.getTableId().equals(tableId) ) {
          tablePresent = true;
          tpOriginal = p;
          tpOriginal.setSyncTag(new SyncTag(null, null, null));
          break;
        }
      }

      TableResource tr;
      try {
        tr = synchronizer.getTable(tableId);
      } catch (IOException e) {
        // TODO report failure properly
        e.printStackTrace();
        return null;
      }

      TableProperties tp;
      try {
        tp = processor.assertTableDefinition(tr.getDefinitionUri());
      } catch (JsonParseException e) {
        // TODO report failure properly
        e.printStackTrace();
        return null;
      } catch (JsonMappingException e) {
        // TODO report failure properly
        e.printStackTrace();
        return null;
      } catch (IOException e) {
        // TODO report failure properly
        e.printStackTrace();
        return null;
      } catch (SchemaMismatchException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        return null;
      }

      tp.setSyncState(tablePresent ? SyncState.inserting : SyncState.rest);
      // Sync the local media files with the server if the table
      // existed locally before we attempted downloading it.

      processor.synchronizeTable(tp, false, tablePresent);

      List<TableResult> results = processor.getTableResults();
      if ( results.size() == 1 ) {
        downloadResult = results.get(0);
      }

      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      pd.dismiss();
      if ( downloadResult == null ) {
        finishDialog.setMessage(getString(R.string.failed_download_table, displayName, "Unknown Error"));
      } else if ( downloadResult.getStatus() != TableResult.Status.SUCCESS ) {
        finishDialog.setMessage(getString(R.string.failed_download_table, displayName, downloadResult.getMessage()));
      } else {
        finishDialog.setMessage(getString(R.string.downloaded_table, displayName));
      }
      finishDialog.show();
    }

  }
}
