package org.opendatakit.tables.sync;

import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.Preferences;
import org.opendatakit.tables.sync.aggregate.AggregateSynchronizer;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

public class TablesSyncAdapter extends AbstractThreadedSyncAdapter {
  private static final String TAG = "TablesSyncAdapter";

  private final Context context;

  public TablesSyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
    this.context = context;

  }

  @Override
  public void onPerformSync(Account account, Bundle extras, String authority,
      ContentProviderClient provider, SyncResult syncResult) {
    Preferences prefs = new Preferences(this.context);
    String aggregateUri = prefs.getServerUri();
    String authToken = prefs.getAuthToken();
    if (aggregateUri != null && authToken != null) {
      DbHelper helper = DbHelper.getDbHelper(context);
      DataManager dm = new DataManager(helper);
      AggregateSynchronizer synchronizer = new AggregateSynchronizer(aggregateUri, authToken);
      SyncProcessor processor = new SyncProcessor(synchronizer, dm, syncResult);
      processor.synchronize();
    }
  }

}
