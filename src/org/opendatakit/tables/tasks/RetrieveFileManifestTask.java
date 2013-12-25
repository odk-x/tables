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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.opendatakit.aggregate.odktables.rest.interceptor.AggregateRequestInterceptor;
import org.opendatakit.tables.R;
import org.opendatakit.tables.sync.SyncUtil;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Debug;

/**
 * Task that communicates with the server and retrieves the file manifest,
 * indicating the list of the files on the server.
 * @author sudar.sam@gmail.com
 *
 */
public class RetrieveFileManifestTask extends AsyncTask<Void, Void, String> {

  public enum RequestType {
    ALL_FILES, APP_FILES, TABLE_FILES;
  }

  private final String mAppId;
  private final Context mContext;
  private final String mAggregateUri;
  private final String mFileManifestPath;
  private final RestTemplate mRestTemplate;
  private final RequestType mRequestType;
  private final String mTableId;

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
   */
  public RetrieveFileManifestTask(Context context, String appId,
      String aggregateUri, String authToken, RequestType requestType,
      String tableId) {
    this.mRequestType = requestType;
    if (requestType == RequestType.TABLE_FILES && tableId == null) {
      throw new IllegalArgumentException("Requested files for a table but " +
      		"did not supply a table id.");
    }
    this.mTableId = tableId;
    this.mAggregateUri = aggregateUri;
    this.mAppId = appId;
    this.mContext = context;
    this.mFileManifestPath = aggregateUri +
        SyncUtil.getFileManifestServerPath();
    // Now get the rest template.
    List<ClientHttpRequestInterceptor> interceptors =
        new ArrayList<ClientHttpRequestInterceptor>();
    interceptors.add(new AggregateRequestInterceptor(URI.create(aggregateUri).normalize(), authToken));
    this.mRestTemplate = SyncUtil.getRestTemplateForString();
    this.mRestTemplate.setInterceptors(interceptors);
  }

  @Override
  protected void onPreExecute() {
    this.mProgressDialog = ProgressDialog.show(mContext,
        mContext.getString(R.string.please_wait),
        mContext.getString(R.string.synchronizing));  }

  @Override
  protected String doInBackground(Void... params) {
    // All we want to do is get and parse the manifest from the server.
    Debug.waitForDebugger();
//    MultiValueMap<String, String> parameterValues =
//        new LinkedMultiValueMap<String, String>();
    // And now we need to build up our requiest appropriately.
    Uri.Builder uriBuilder = Uri.parse(mFileManifestPath).buildUpon();
    uriBuilder.appendQueryParameter("app_id", mAppId);
//    parameterValues.add("app_id", mAppId);
    switch (this.mRequestType) {
    case ALL_FILES:
      // we don't need to do anything.
      break;
    case APP_FILES:
//      parameterValues.add("app_level_files", "true");
      uriBuilder.appendQueryParameter("app_level_files", "true");
      break;
    case TABLE_FILES:
      // Rely on the throw exception in the constructor to ensure this is
      // never null.
//      parameterValues.add("table_id", "true");
      uriBuilder.appendQueryParameter("table_id", mTableId);
      break;
    }
    ResponseEntity<String> responseEntity =
        mRestTemplate.exchange(uriBuilder.build().toString(),
            HttpMethod.GET, null, String.class);
    String response = responseEntity.getBody();
    return response;
  }

  @Override
  protected void onPostExecute(String response) {
    // The response is the response of the webpage. Here we should try to
    // deserialize it.


    this.mProgressDialog.dismiss();
  }

}
