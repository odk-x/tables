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

import java.lang.ref.WeakReference;

import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.WebViewUtil;
import org.opendatakit.tables.views.webkits.Control;
import org.opendatakit.tables.views.webkits.ControlIf;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

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
  
  /** The name of the file this fragment is displaying. */
  protected String mFileName;
  
  /** The {@link Control} object that was jused to generate the
   * {@link ControlIf} that was passed to the {@link WebView}. This reference
   * must be saved to prevent garbage collection of the {@link WeakReference}
   * in {@link ControlIf}.
   */
  protected Control mControlReference;

  @Override
  public String retrieveFileNameFromBundle(Bundle bundle) {
    String fileName = IntentUtil.retrieveFileNameFromBundle(bundle);
    return fileName;
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    WebLogger.getLogger(getAppName()).d(TAG, "[onCreate]");
    // Get the file name. Saved state gets precedence. Then arguments.
    String retrievedFileName = retrieveFileNameFromBundle(savedInstanceState);
    if (retrievedFileName == null) {
      retrievedFileName = this.retrieveFileNameFromBundle(this.getArguments());
    }
    this.mFileName = retrievedFileName;
  }
  
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    WebLogger.getLogger(getAppName()).d(TAG, "[onCreateView] activity is: " + this.getActivity());
    WebView webView = this.buildView();
    return webView;
  }

  @Override
  public void putFileNameInBundle(Bundle bundle) {
    if (this.getFileName() != null) {
      bundle.putString(Constants.IntentKeys.FILE_NAME, this.getFileName());
    }
  }

  @Override
  public WebView buildView() {
    WebLogger.getLogger(getAppName()).d(TAG, "[buildView] activity is: " + this.getActivity());
    WebView result = WebViewUtil.getODKCompliantWebView((AbsBaseActivity) getActivity());
    Control control = this.createControlObject();
    result.addJavascriptInterface(
        control.getJavascriptInterfaceWithWeakReference(),
        Constants.JavaScriptHandles.CONTROL);
    WebViewUtil.displayFileInWebView(
        getActivity(),
        ((AbsBaseActivity) getActivity()).getAppName(),
        result,
        this.getFileName());
    // save the strong reference
    this.mControlReference = control;
    return result;
  }

  @Override
  public String getFileName() {
    return this.mFileName;
  }

  /**
   * @see IWebFragment#createControlObject()
   */
  @Override
  public Control createControlObject() {
    Control result = new Control((AbsBaseActivity) this.getActivity(), this.getAppName(), null, null);
    return result;
  }
  
}
