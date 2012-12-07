package org.opendatakit.tables.sync.files;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * A service modeled on {@link org.opendatakit.tables.sync.SyncService} that 
 * is somehow necessary for syncing...
 * @author sudar.sam@gmail.com
 *
 */
public class FileSyncService extends Service {
  
  private static final Object FILE_SYNC_ADAPTER_LOCK = new Object();
  
  private static FileSyncAdapter syncAdapter = null;
  
  @Override
  public void onCreate() {
    synchronized(FILE_SYNC_ADAPTER_LOCK) {
      if (syncAdapter == null) {
        syncAdapter = new FileSyncAdapter(getApplicationContext(), true);
      }
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return syncAdapter.getSyncAdapterBinder();
  }

}
