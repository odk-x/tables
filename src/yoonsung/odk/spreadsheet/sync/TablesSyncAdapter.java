package yoonsung.odk.spreadsheet.sync;

import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.Preferences;
import yoonsung.odk.spreadsheet.sync.aggregate.AggregateSynchronizer;
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
    if (aggregateUri != null) {
      DbHelper helper = DbHelper.getDbHelper(context);
      AggregateSynchronizer synchronizer = new AggregateSynchronizer(aggregateUri);
      SyncProcessor processor = new SyncProcessor(synchronizer, helper,
          syncResult);
      processor.synchronize();
    }
  }

}
