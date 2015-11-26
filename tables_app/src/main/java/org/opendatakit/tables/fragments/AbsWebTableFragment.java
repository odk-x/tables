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
package org.opendatakit.tables.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;
import org.opendatakit.common.android.activities.BaseActivity;
import org.opendatakit.common.android.application.CommonApplication;
import org.opendatakit.common.android.listener.DatabaseConnectionListener;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.common.android.views.OdkData;
import org.opendatakit.common.android.views.ExecutorContext;
import org.opendatakit.common.android.views.ExecutorProcessor;
import org.opendatakit.common.android.views.ICallbackFragment;
import org.opendatakit.database.service.OdkDbInterface;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.WebViewUtil;
import org.opendatakit.tables.views.webkits.OdkCommon;
import org.opendatakit.tables.views.webkits.OdkTables;
import org.opendatakit.tables.views.webkits.OdkTablesIf;
import org.opendatakit.tables.views.webkits.TableDataExecutorProcessor;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Base class for {@link Fragment}s that display information about a table
 * using a WebKit view.
 * @author sudar.sam@gmail.com
 *
 */
public abstract class AbsWebTableFragment extends AbsTableDisplayFragment
    implements IWebFragment, ICallbackFragment {

  private static final String TAG = AbsWebTableFragment.class.getSimpleName();
  private static final String RESPONSE_JSON = "responseJSON";
  /**
   * The {@link OdkTables} object that was used to generate the
   * {@link OdkTablesIf} that was passed to the {@link WebView}. This reference
   * must be saved to prevent garbage collection of the {@link WeakReference}
   * in {@link OdkTablesIf}.
   */
  OdkTables mOdkTablesReference;
  OdkCommon mOdkCommonReference;
  OdkData mOdkDataReference;
  LinkedList<String> queueResponseJSON = new LinkedList<String>();

  DatabaseConnectionListener listener = null;

  /** The file name this fragment is displaying. */
  String mFileName;
  
  /**
   * Retrieve the file name that should be displayed.
   * @return the file name, or null if one has not been set.
   */
  @Override
  public String retrieveFileNameFromBundle(Bundle bundle) {
    String fileName = IntentUtil.retrieveFileNameFromBundle(bundle);
    return fileName;
  }

  @Override
  public void putFileNameInBundle(Bundle bundle) {
    if (this.getFileName() != null) {
      bundle.putString(Constants.IntentKeys.FILE_NAME, this.getFileName());
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // AppName is unknown since Activity is likely null
    // Get the file name if it was there.
    String retrievedFileName = retrieveFileNameFromBundle(savedInstanceState);
    if (retrievedFileName == null) {
      // then try to get it from its arguments.
      retrievedFileName = this.retrieveFileNameFromBundle(this.getArguments());
    }
    this.mFileName = retrievedFileName;
    if ( savedInstanceState != null && savedInstanceState.containsKey(RESPONSE_JSON)) {
      String[] pendingResponseJSON = savedInstanceState.getStringArray(RESPONSE_JSON);
      queueResponseJSON.addAll(Arrays.asList(pendingResponseJSON));
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    this.putFileNameInBundle(outState);
    if ( !queueResponseJSON.isEmpty() ) {
      String[] qra = queueResponseJSON.toArray(new String[queueResponseJSON.size()]);
      outState.putStringArray(RESPONSE_JSON, qra);
    }
  }

  @Override public void onResume() {
    super.onResume();
    if ( mOdkDataReference != null ) {
      mOdkDataReference.refreshContext();
    }
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    WebLogger.getLogger(getAppName()).d(TAG, "[onCreateView]");
    
    ViewGroup v = (ViewGroup) inflater.inflate(
        R.layout.web_view_container,
        container,
        false);

    WebView webView = (WebView) v.findViewById(R.id.webkit);
    
    WebView result = WebViewUtil.getODKCompliantWebView((AbsBaseActivity) getActivity(), webView);
    return v;
  }
  
  /**
   * @throws RemoteException 
   * @see IWebFragment#createControlObject()
   */
  @Override
  public OdkTables createControlObject() throws RemoteException {
    OdkTables result = new OdkTables((AbsBaseActivity) getActivity(), getTableId(), getColumnDefinitions());
    return result;
  }

  /**
   * @throws RemoteException
   * @see IWebFragment#createCommonObject()
   */
  @Override
  public OdkCommon createCommonObject() throws RemoteException {
    OdkCommon result = new OdkCommon((AbsBaseActivity) getActivity());
    return result;
  }

  public synchronized OdkData getDataReference() throws RemoteException {
    if ( mOdkDataReference == null ) {
      mOdkDataReference = new OdkData(this, (BaseActivity)getActivity());
    }
    return mOdkDataReference;
  }

  @Override
  public void setWebKitVisibility() {
    if ( getView() == null ) {
      return;
    }
    
    WebView webView = (WebView) getView().findViewById(R.id.webkit);
    TextView noDatabase = (TextView) getView().findViewById(android.R.id.empty);
    
    if ( Tables.getInstance().getDatabase() != null ) {
      webView.setVisibility(View.VISIBLE);
      noDatabase.setVisibility(View.GONE);
    } else {
      webView.setVisibility(View.GONE);
      noDatabase.setVisibility(View.VISIBLE);
    }
  }

  /**
   * Get the file name this fragment is displaying.
   */
  @Override
  public String getFileName() {
    return this.mFileName;
  }

  @Override
  public void setFileName(String relativeFileName) {
    this.mFileName = relativeFileName;
    databaseAvailable();
  }


  @Override
  public void databaseAvailable() {
    if ( listener != null ) {
      listener.databaseAvailable();
    }
  }

  @Override
  public void databaseUnavailable() {
    if ( listener != null ) {
      listener.databaseUnavailable();
    }
  }

  @Override
  public void signalResponseAvailable(String responseJSON) {
    View vw = getView();
    if (vw != null) {
      this.queueResponseJSON.add(responseJSON);
      final WebView webView = (WebView) vw.findViewById(org.opendatakit.tables.R.id.webkit);
      this.getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          webView.loadUrl("javascript:odkData.responseAvailable();");
        }
      });
    } else {
      WebLogger.getLogger(getAppName()).d(TAG, "signalResponseAvailable: trying to deliver a "
          + "response when view is null");
    }
  }

  @Override
  public String getResponseJSON() {
    if ( queueResponseJSON.isEmpty() ) {
      return null;
    }
    String responseJSON = queueResponseJSON.removeFirst();
    return responseJSON;
  }

  @Override
  public ExecutorProcessor newExecutorProcessor(ExecutorContext context) {
    return new TableDataExecutorProcessor(context);
  }

  @Override
  public void registerDatabaseConnectionBackgroundListener(DatabaseConnectionListener listener) {
    this.listener = listener;
  }

  @Override
  public OdkDbInterface getDatabase() {
    return ((CommonApplication) this.getActivity().getApplication()).getDatabase();
  }

  @Override
  public void onDestroy() {
    if (mOdkDataReference != null) {
      this.mOdkDataReference.shutdownContext();
    }
    super.onDestroy();
  }
}
