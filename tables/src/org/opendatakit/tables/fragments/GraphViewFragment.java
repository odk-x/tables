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

import java.io.File;

import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.WebViewUtil;
import org.opendatakit.tables.views.webkits.Control;
import org.opendatakit.tables.views.webkits.GraphData;
import org.opendatakit.tables.views.webkits.TableData;

import android.os.Bundle;
import android.webkit.WebView;

public class GraphViewFragment extends AbsWebTableFragment {

  private static final String TAG = GraphViewFragment.class.getSimpleName();

  /** The name of the base graphing file. */
  private static final String TABLES_GRAPH_BASE_FILE_NAME = "optionspane.html";

  private String mGraphName;

  /** A strong reference that must be kept or else nulls will be thrown. */
  protected GraphData mGraphDataStrongReference;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    // AppName may not be available
    super.onCreate(savedInstanceState);
    String graphName = this.getArguments().getString(Constants.IntentKeys.GRAPH_NAME);
    this.mGraphName = graphName;
  }

  /**
   * Return the name of the graph that is being displayed.
   * 
   * @return
   */
  String getGraphName() {
    if (mGraphName == null) {
      WebLogger.getLogger(getAppName()).e(TAG, "[getGraphName] graphName was null!");
    }
    return this.mGraphName;
  }

  @Override
  public WebView buildView() {
    WebView result = WebViewUtil.getODKCompliantWebView((AbsBaseActivity) getActivity());
    Control control = this.createControlObject();
    result.addJavascriptInterface(control.getJavascriptInterfaceWithWeakReference(),
        Constants.JavaScriptHandles.CONTROL);
    TableData tableData = this.createDataObject();
    result.addJavascriptInterface(tableData.getJavascriptInterfaceWithWeakReference(),
        Constants.JavaScriptHandles.DATA);
    String relativePathToGraphFile = ODKFileUtils.getFrameworkFolder(getAppName()) + File.separator
        + TABLES_GRAPH_BASE_FILE_NAME;
    String relativePath = ODKFileUtils.asUriFragment(getAppName(),
        new File(relativePathToGraphFile));
    GraphData graphData = new GraphData(this.getActivity(), getAppName(), getTableId(),
        this.getGraphName());
    result.addJavascriptInterface(graphData.getJavascriptInterfaceWithWeakReference(),
        Constants.JavaScriptHandles.GRAPH);
    WebViewUtil.displayFileInWebView(getActivity(), getAppName(), result, relativePath);
    // Save the references
    this.mControlReference = control;
    this.mTableDataReference = tableData;
    this.mGraphDataStrongReference = graphData;
    return result;
  }

  @Override
  protected TableData createDataObject() {
    // Graph view displays everything.
    TableData result = new TableData(getUserTable());
    return result;
  }

  @Override
  public ViewFragmentType getFragmentType() {
    // TODO Auto-generated method stub
    return null;
  }

}
