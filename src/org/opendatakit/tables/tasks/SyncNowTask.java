/*
 * Copyright (C) 2014 University of Washington
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
package org.opendatakit.tables.tasks;

import java.util.Arrays;

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.Preferences;
import org.opendatakit.tables.sync.SyncProcessor;
import org.opendatakit.tables.sync.SynchronizationResult;
import org.opendatakit.tables.sync.Synchronizer;
import org.opendatakit.tables.sync.aggregate.AggregateSynchronizer;
import org.opendatakit.tables.sync.exceptions.InvalidAuthTokenException;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SyncResult;
import android.os.AsyncTask;
import android.util.Log;

public class SyncNowTask extends
    AsyncTask<Void, Void, SynchronizationResult> {

  private static final String t = "SyncNowTask";

  public interface SyncNowCallback {
    void syncOutcome(boolean success, String message, boolean authRequired, SynchronizationResult result);
  };

  private final Context context;
  private final String appName;
  private final SyncNowCallback callback;

  private ProgressDialog pd;
  private boolean success;
  private boolean authRequired;
  private String message;
  private Preferences prefs;

  public SyncNowTask(Context context, String appName, SyncNowCallback callback) {
    this.context = context;
    this.appName = appName;
    this.callback = callback;

    prefs = new Preferences(context, appName);
  }

  @Override
  protected void onPreExecute() {
    pd = ProgressDialog.show(context, context.getString(R.string.please_wait),
        context.getString(R.string.synchronizing));
    success = false;
    authRequired = false;
    message = null;
  }

  @Override
  protected SynchronizationResult doInBackground(Void... params) {
    SynchronizationResult result = null;
    try {
      Synchronizer synchronizer = new AggregateSynchronizer(appName, prefs.getServerUri(),
          prefs.getAuthToken());
      SyncProcessor processor = new SyncProcessor(context, appName, synchronizer, new SyncResult());
      // This is going to assume that we ALWAYS sync all three levels:
      // app, tableNonMedia, and tableMedia. This might have to be changed
      // and paramaterized using some user-input values in the future.
      result = processor.synchronize(true, true, true);
      success = true;
      Log.e(t, "[SyncNowTask#doInBackground] timestamp: " +
          System.currentTimeMillis());
    } catch (InvalidAuthTokenException e) {
      authRequired = true;
      success = false;
      message = context.getString(R.string.auth_expired);
    } catch (Exception e) {
      Log.e(t, "[exception during synchronization. stack trace:\n" +
          Arrays.toString(e.getStackTrace()));
      success = false;
      message = e.toString();
    }
    return result;
  }

  @Override
  protected void onPostExecute(SynchronizationResult result) {
    pd.dismiss();
    callback.syncOutcome(success, message, authRequired, result);
  }

}