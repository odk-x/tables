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
package org.opendatakit.tables.sync;

import org.opendatakit.tables.activities.Aggregate;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.Preferences;
import org.opendatakit.tables.sync.aggregate.AggregateSynchronizer;
import org.opendatakit.tables.sync.exceptions.InvalidAuthTokenException;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

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
    Log.d(TAG, "in onPerformSync");
    Preferences prefs = new Preferences(this.context);
    String aggregateUri = prefs.getServerUri();
    String authToken = prefs.getAuthToken();
    if (aggregateUri != null && authToken != null) {
      DbHelper helper = DbHelper.getDbHelper(context);
      DataManager dm = new DataManager(helper);
      AggregateSynchronizer synchronizer;
      try {
        synchronizer = new AggregateSynchronizer(aggregateUri, authToken);
      } catch (InvalidAuthTokenException e) {
        Aggregate.invalidateAuthToken(authToken, context);
        syncResult.stats.numAuthExceptions++;
        return;
      }
      SyncProcessor processor = new SyncProcessor(helper, synchronizer, dm, 
          syncResult);
      processor.synchronize();
    }
  }

}
