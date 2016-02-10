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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.views.webkits.OdkTablesWebView;

/**
 * Base class for {@link Fragment}s that display information about a table
 * using a WebKit view.
 * @author sudar.sam@gmail.com
 *
 */
public abstract class AbsWebTableFragment extends AbsTableDisplayFragment
    implements IWebFragment {

  private static final String TAG = AbsWebTableFragment.class.getSimpleName();

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

    return v;
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

  @Override
  public void databaseAvailable() {

    if ( getView() != null ) {
      setWebKitVisibility();
      getWebKit().reloadPage();
    }
  }

  @Override
  public void databaseUnavailable() {
    if ( getView() != null ) {
      setWebKitVisibility();
      getWebKit().setForceLoadDuringReload();
    }
  }
}
