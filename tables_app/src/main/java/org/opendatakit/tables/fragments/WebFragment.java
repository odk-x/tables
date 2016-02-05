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

import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;
import org.opendatakit.common.android.application.CommonApplication;
import org.opendatakit.common.android.listener.DatabaseConnectionListener;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.common.android.views.*;
import org.opendatakit.database.service.OdkDbInterface;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.AbsBaseWebActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.WebViewUtil;
import org.opendatakit.tables.views.webkits.OdkTables;
import org.opendatakit.tables.views.webkits.OdkTablesIf;
import org.opendatakit.tables.views.webkits.OdkTablesWebView;
import org.opendatakit.tables.views.webkits.TableDataExecutorProcessor;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Displays an HTML file that is not associated with a particular table.
 * Consequently it does not add a data JavaScript interface to its 
 * {@link WebView}. To display data about a table, see 
 * {@link AbsWebTableFragment} and its subclasses.
 * @author sudar.sam@gmail.com
 *
 */
public class WebFragment extends AbsBaseFragment implements IWebFragment {
  
  private static final String TAG = WebFragment.class.getSimpleName();

  private static final int ID = R.layout.web_view_container;
  
  /** The name of the file this fragment is displaying. */
  protected String mFileName;

  @Override
  public String retrieveFileNameFromBundle(Bundle bundle) {
    if ( bundle == null ) {
      return null;
    }
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
    // AppName may not be available...
    // Get the file name. Saved state gets precedence. Then arguments.
    String retrievedFileName = retrieveFileNameFromBundle(savedInstanceState);
    if (retrievedFileName == null) {
      retrievedFileName = this.retrieveFileNameFromBundle(this.getArguments());
    }
    this.mFileName = retrievedFileName;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    putFileNameInBundle(outState);
  }
  
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    WebLogger.getLogger(getAppName()).d(TAG, "[onCreateView] activity is: " + this.getActivity());
    
    View v = inflater.inflate(
        R.layout.web_view_container,
        container,
        false);

    return v;
  }

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
  public OdkTablesWebView getWebKit() {
    return (OdkTablesWebView) getView().findViewById(R.id.webkit);
  }

  @Override
  public void setWebKitVisibility() {
    if ( getView() == null ) {
      return;
    }

    OdkTablesWebView webView = (OdkTablesWebView) getView().findViewById(R.id.webkit);
    TextView noDatabase = (TextView) getView().findViewById(android.R.id.empty);
    
    if ( Tables.getInstance().getDatabase() != null ) {
      webView.setVisibility(View.VISIBLE);
      noDatabase.setVisibility(View.GONE);
    } else {
      webView.setVisibility(View.GONE);
      noDatabase.setVisibility(View.VISIBLE);
    }
  }
  
  @Override
  public void databaseAvailable() {

    if ( getView() != null && getFileName() != null ) {
      OdkTablesWebView webView = (OdkTablesWebView) getView().findViewById(org.opendatakit.tables.R.id.webkit);
      setWebKitVisibility();
      WebViewUtil.displayFileInWebView(
          getActivity(),
          ((AbsBaseActivity) getActivity()).getAppName(),
          webView,
          this.getFileName());
    }
  }

  @Override
  public void databaseUnavailable() {
    setWebKitVisibility();
  }
}
