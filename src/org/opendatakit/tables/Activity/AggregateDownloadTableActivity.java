package org.opendatakit.tables.Activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.Preferences;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.sync.SyncProcessor;
import org.opendatakit.tables.sync.SyncUtil;
import org.opendatakit.tables.sync.Synchronizer;
import org.opendatakit.tables.sync.aggregate.AggregateSynchronizer;

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
      finishDialog.setMessage("Unable to contact server. Please try again later...");
      finishDialog.show();
    } else if (tables.isEmpty()) {
      finishDialog.setMessage("No tables on server which have not already been downloaded.");
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
    finishDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
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
      finishDialog.setMessage("Unable to download table. Please try again later...");
      finishDialog.show();
    }
    finishDialog.setMessage("Downloaded " + tableName);
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
      pd = ProgressDialog.show(AggregateDownloadTableActivity.this, "Please Wait",
          "Getting tables. Please wait...");
    }

    @Override
    protected Map<String, String> doInBackground(Void... args) {
      Synchronizer sync = new AggregateSynchronizer(aggregateUrl, authToken);

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
        TableProperties[] props = dm.getDataTableProperties();
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
      pd = ProgressDialog.show(AggregateDownloadTableActivity.this, "Please Wait",
          "Downloading table " + tableName + ". Please wait...");
    }

    @Override
    protected Void doInBackground(Void... params) {
      DbHelper dbh = DbHelper.getDbHelper(AggregateDownloadTableActivity.this);

      TableProperties tp = TableProperties.addTable(dbh,
          TableProperties.createDbTableName(dbh, tableName), tableName,
          TableProperties.TableType.DATA, tableId);
      tp.setSynchronized(true);
      tp.setSyncState(SyncUtil.State.REST);
      tp.setSyncTag(null);

      Synchronizer synchronizer = new AggregateSynchronizer(aggregateUrl, authToken);
      SyncProcessor processor = new SyncProcessor(synchronizer, new DataManager(dbh),
          new SyncResult());
      processor.synchronizeTable(tp);
      // Aggregate.requestSync(accountName);

      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      pd.dismiss();
    }

  }
}
