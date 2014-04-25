/*
 * Copyright (C) 2013 University of Washington
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

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifestEntry;
import org.opendatakit.common.android.sync.aggregate.AggregateSynchronizer;
import org.opendatakit.common.android.sync.exceptions.InvalidAuthTokenException;
import org.opendatakit.tables.R;
import org.springframework.web.client.ResourceAccessException;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Debug;

/**
 * Task that communicates with the server and retrieves the file manifest,
 * indicating the list of the files on the server.
 * @author sudar.sam@gmail.com
 *
 */
public class RetrieveFileManifestTask extends AsyncTask<Void, Void, List<OdkTablesFileManifestEntry>> {

  public enum RequestType {
    APP_FILES, TABLE_FILES;
  }

  private final String mAppId;
  private final Context mContext;
  private final String mAggregateUri;
  private final RequestType mRequestType;
  private final String mTableId;
  private final AggregateSynchronizer synchronizer;

  private ProgressDialog mProgressDialog;

  /**
   * Create the task that will retrieve the manifest from the server.
   * <p>
   * A manifest can be generated at three levels: all the files in the app (
   * {@link RequestType#ALL_FILES}), only the files in the app that are
   * <b>not</b> associated with a particular table
   * ({@link RequestType#APP_FILES}), and
   * only those for a particular table ({@link RequestType#TABLE_FILES}).
   * @param context
   * @param appId
   * @param aggregateUri
   * @param authToken
   * @param requestType the request type and level you are interested in.
   * @param tableId the table id. If not requesting a particular table, null.
   * If the requestType is not {@link RequestType#TABLE_FILES}, this will be
   * ignored.
   * @throws InvalidAuthTokenException
   */
  public RetrieveFileManifestTask(Context context, String appId,
      String aggregateUri, String authToken, RequestType requestType,
      String tableId) throws InvalidAuthTokenException {
    this.mRequestType = requestType;
    if (requestType == RequestType.TABLE_FILES && tableId == null) {
      throw new IllegalArgumentException("Requested files for a table but " +
      		"did not supply a table id.");
    }
    this.mTableId = tableId;
    this.mAggregateUri = aggregateUri;
    this.mAppId = appId;
    this.mContext = context;

    synchronizer = new AggregateSynchronizer(mAppId, mAggregateUri, authToken);
  }

  @Override
  protected void onPreExecute() {
    this.mProgressDialog = ProgressDialog.show(mContext,
        mContext.getString(R.string.please_wait),
        mContext.getString(R.string.synchronizing));  }

  @Override
  protected List<OdkTablesFileManifestEntry> doInBackground(Void... params) {
    // All we want to do is get and parse the manifest from the server.
    Debug.waitForDebugger();
    List<OdkTablesFileManifestEntry> entries = new ArrayList<OdkTablesFileManifestEntry>();
    try {
    switch (mRequestType) {
    case APP_FILES:
      entries = synchronizer.getAppLevelFileManifest();
      break;
    case TABLE_FILES:
      entries = synchronizer.getTableLevelFileManifest(mTableId);
      break;
    }
    } catch (ResourceAccessException e) {
      e.printStackTrace();
    }
    return entries;
  }

  @Override
  protected void onPostExecute(List<OdkTablesFileManifestEntry> entries) {
    // The response from the webpage.
    this.mProgressDialog.dismiss();
  }

}
