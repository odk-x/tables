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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.opendatakit.aggregate.odktables.rest.interceptor.AggregateRequestInterceptor;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.sync.SyncUtil;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

/**
 * Task for uploading files to the server. Based on Collect's
 * InstanceUploaderTask.
 * @author sudar.sam@gmail.com
 *
 */
public class FileUploaderTask extends AsyncTask<String, Integer,
    FileUploaderTask.Outcome> {

  private static final String SEPARATOR = "/";

  // The id of the app whose files will be synced.
  private final String mAppId;
  private final Context mContext;
  private final String mAggregateUri;
  private final String mFileServerPath;
  private final RestTemplate mRestTemplate;

  private ProgressDialog mProgressDialog;

  public static class Outcome {
    public Uri mAuthRequestingServer = null;
    public HashMap<String, String> mResults = new HashMap<String, String>();
  }

  public FileUploaderTask(Context context, String appId, String aggregateUri,
      String authtoken) {
    this.mContext = context;
    this.mAppId = appId;
    this.mAggregateUri = aggregateUri;
    URI uriBase = URI.create(aggregateUri).normalize();
    URI uri = uriBase.resolve(SyncUtil.getFileServerPath()).normalize();
    this.mFileServerPath = uri.toString();
    // Now get the rest template.
    List<ClientHttpRequestInterceptor> interceptors =
        new ArrayList<ClientHttpRequestInterceptor>();
    interceptors.add(new AggregateRequestInterceptor(uriBase, authtoken));
    RestTemplate rt = SyncUtil.getRestTemplateForFiles();
    rt.setInterceptors(interceptors);
    this.mRestTemplate = rt;
  }

  /**
   * Upload a single file and add the result to the runningOutcome.
   * @param file the actual file to be uploaded
   * @param relativePath the path of the file relative to the odk directory--
   * i.e. including the app id, and beginning with "/".
   * @param runningOutcome
   * @return
   */
  private boolean uploadOneFile(FileSystemResource file, String relativePath,
      Outcome runningOutcome) {
    String escapedPath = SyncUtil.formatPathForAggregate(relativePath);
    URI filePostUri =
        URI.create(mFileServerPath).resolve(mAppId + escapedPath).normalize();
    URI responseUri = this.mRestTemplate.postForLocation(filePostUri, file);
    runningOutcome.mResults.put(relativePath, "tried something");
    return true;
  }

  @Override
  protected void onPreExecute() {
    this.mProgressDialog = ProgressDialog.show(mContext,
        mContext.getString(R.string.please_wait),
        mContext.getString(R.string.synchronizing));
  }

  @Override
  protected Outcome doInBackground(String... filePaths) {
    // filePaths should contain all of the files (relative to the odk folder--
    // e.g. /sdcard/odk/tables/list.html would pass in just "tables/list.html",
    // as in this case tables would be the app id.
    Outcome outcome = new Outcome();
    String appFolderPath = ODKFileUtils.getAppFolder(this.mAppId);
    for (String filePath : filePaths) {
      File file = new File(appFolderPath + SEPARATOR + filePath);
      FileSystemResource resource = new FileSystemResource(file);
      uploadOneFile(resource, filePath, outcome);
    }
    return outcome;
  }

  @Override
  protected void onPostExecute(Outcome outcome) {
    this.mProgressDialog.dismiss();
  }

}
