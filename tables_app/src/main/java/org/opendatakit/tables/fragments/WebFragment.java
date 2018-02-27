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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.views.webkits.OdkTablesWebView;

/**
 * Displays an HTML file that is not associated with a particular table.
 * Consequently it does not add a data JavaScript interface to its
 * {@link WebView}. To display data about a table, see
 * {@link AbsWebTableFragment} and its subclasses.
 *
 * @author sudar.sam@gmail.com
 */
public class WebFragment extends AbsTablesFragment implements IWebFragment {

  private static final String TAG = WebFragment.class.getSimpleName();

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    WebLogger.getLogger(getAppName())
        .d(TAG, "[onCreateView] activity is: " + getActivity().getClass().getSimpleName());

    return inflater.inflate(R.layout.web_view_container, container, false);
  }

  @Override
  public void onResume() {
    super.onResume();
    OdkTablesWebView view = getWebKit();
    if ( view != null ) {
      view.onResume();
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    OdkTablesWebView view = getWebKit();
    if ( view != null ) {
      view.onPause();
    }
  }

  @Override
  public OdkTablesWebView getWebKit() {
    if (getView() == null)
      return null;
    return (OdkTablesWebView) getView().findViewById(R.id.webkit);
  }

  @Override
  public void setWebKitVisibility() {
    if (getView() == null) {
      return;
    }

    View webView = getView().findViewById(R.id.webkit);
    View noDatabase = getView().findViewById(android.R.id.empty);

    if (Tables.getInstance().getDatabase() != null) {
      webView.setVisibility(View.VISIBLE);
      noDatabase.setVisibility(View.GONE);
    } else {
      webView.setVisibility(View.GONE);
      noDatabase.setVisibility(View.VISIBLE);
    }
  }

  @Override
  public void databaseAvailable() {

    if (getView() != null) {
      setWebKitVisibility();
      getWebKit().reloadPage();
    }
  }

  @Override
  public void databaseUnavailable() {
    if (getView() != null) {
      setWebKitVisibility();
      getWebKit().setForceLoadDuringReload();
    }
  }
}
