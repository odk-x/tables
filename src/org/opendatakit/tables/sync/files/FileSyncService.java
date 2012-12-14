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
