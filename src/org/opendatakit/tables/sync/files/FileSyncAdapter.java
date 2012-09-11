package org.opendatakit.tables.sync.files;

import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.Preferences;
import org.opendatakit.tables.data.TableProperties;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

/**
 * The SyncAdapter for syncing files on the server to the phone. This is called
 * a sync, but in reality at this point in time it is just a way to download 
 * the most recent version of files from the server.
 * <p>
 * This is modeled off of TablesSyncAdapter and the manifest checking from 
 * Collect.
 * @author sudar.sam@gmail.com
 *
 */
public class FileSyncAdapter extends AbstractThreadedSyncAdapter {
  
  private static final String TAG = "FileSyncAdapter";
  
  private final Context context;
  
  public FileSyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
    this.context = context;
  }

  /**
   * Download the files from the server.
   */
  @Override
  public void onPerformSync(Account account, Bundle extras, String authority,
      ContentProviderClient provider, SyncResult syncResult) {
    Log.d(TAG, "in onPerformSync");
    android.os.Debug.waitForDebugger();
    Preferences prefs = new Preferences(this.context);
    String aggregateUri = prefs.getServerUri();
    String authToken = prefs.getAuthToken();
    
    // ok, not sure, but I think that whether or not the table should have 
    // its files downloaded is from the tableProperties.isSynchronized.
    DbHelper dbh = DbHelper.getDbHelper(context);
    DataManager dm = new DataManager(dbh);
    TableProperties[] tableProperties = dm.getSynchronizedTableProperties();
    for (TableProperties tableProp : tableProperties) {
      String tableId = tableProp.getTableId();
      SyncUtilities.syncKeyValueEntriesForTable(context, aggregateUri, 
          authToken, tableId);
      
    }
    

  }

}
