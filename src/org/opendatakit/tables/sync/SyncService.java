package org.opendatakit.tables.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SyncService extends Service {

	private static final Object SYNC_ADAPTER_LOCK = new Object();

	private static TablesSyncAdapter syncAdapter = null;

	@Override
	public void onCreate() {
		synchronized (SYNC_ADAPTER_LOCK) {
			if (syncAdapter == null) {
				syncAdapter = new TablesSyncAdapter(getApplicationContext(),
						true);
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return syncAdapter.getSyncAdapterBinder();
	}
}