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

import org.opendatakit.androidcommon.R;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.WebViewUtil;
import org.opendatakit.tables.views.webkits.Control;
import org.opendatakit.tables.views.webkits.TableData;

import android.app.Fragment;
import android.os.RemoteException;
import android.webkit.WebView;
import android.widget.Toast;

/**
 * {@link Fragment} for displaying a List view.
 * @author sudar.sam@gmail.com
 *
 */
public class ListViewFragment extends AbsWebTableFragment {
  
  private static final String TAG = ListViewFragment.class.getSimpleName();

  @Override
  public void databaseAvailable() {
    if ( Tables.getInstance().getDatabase() != null && getView() != null ) {
      try {
        WebView webView = (WebView) getView().findViewById(org.opendatakit.tables.R.id.webkit);
        Control control;
        control = this.createControlObject();
        webView.addJavascriptInterface(
            control.getJavascriptInterfaceWithWeakReference(),
            Constants.JavaScriptHandles.CONTROL);
        TableData tableData = this.createDataObject();
        webView.addJavascriptInterface(
            tableData.getJavascriptInterfaceWithWeakReference(),
            Constants.JavaScriptHandles.DATA);
        // Now save the references.
        this.mControlReference = control;
        this.mTableDataReference = tableData;
        setWebKitVisibility();
        WebViewUtil.displayFileInWebView(
            getActivity(),
            getAppName(),
            webView,
            getFileName());
      } catch (RemoteException e) {
        WebLogger.getLogger(getAppName()).printStackTrace(e);
        Toast.makeText(getActivity(), 
            getActivity().getString(R.string.abort_error_accessing_database), 
            Toast.LENGTH_LONG).show();
      }
    }
  }
  
  @Override
  public void databaseUnavailable() {
    setWebKitVisibility();
  }

  @Override
  public ViewFragmentType getFragmentType() {
    return ViewFragmentType.LIST;
  }

  @Override
  protected TableData createDataObject() {
    TableData result = new TableData(getActivity(), getUserTable());
    return result;
  }

}
