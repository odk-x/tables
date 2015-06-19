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
package org.opendatakit.tables.views.webkits;

import java.lang.ref.WeakReference;

import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.R;

import android.os.RemoteException;
import android.widget.Toast;

public class GraphDataIf {
  private static final String TAG = "GraphDataIf";
  private WeakReference<GraphData> weakGraphData;

  GraphDataIf(GraphData graphData) {
    this.weakGraphData = new WeakReference<GraphData>(graphData);
  }

  // @JavascriptInterface
  public boolean isModified() {
    return weakGraphData.get().isModified();
  }

  // @JavascriptInterface
  public boolean isModifiable() {
    try {
      return weakGraphData.get().isModifiable();
    } catch (RemoteException e) {
      String appName = weakGraphData.get().getAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakGraphData.get().getActivity(), R.string.error_accessing_database,
          Toast.LENGTH_LONG).show();
      return false;
    }
  }

  // @JavascriptInterface
  public boolean hasGraph(String graph) {
    try {
      return weakGraphData.get().hasGraph(graph);
    } catch (RemoteException e) {
      String appName = weakGraphData.get().getAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakGraphData.get().getActivity(), R.string.error_accessing_database,
          Toast.LENGTH_LONG).show();
      return false;
    }
  }

  // @JavascriptInterface
  public String getGraphType() {
    try {
      return weakGraphData.get().getGraphType();
    } catch (RemoteException e) {
      String appName = weakGraphData.get().getAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakGraphData.get().getActivity(), R.string.error_accessing_database,
          Toast.LENGTH_LONG).show();
      return null;
    }
  }

  // @JavascriptInterface
  public String getGraphXAxis() {
    try {
      return weakGraphData.get().getGraphXAxis();
    } catch (RemoteException e) {
      String appName = weakGraphData.get().getAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakGraphData.get().getActivity(), R.string.error_accessing_database,
          Toast.LENGTH_LONG).show();
      return null;
    }
  }

  // @JavascriptInterface
  public String getGraphYAxis() {
    try {
      return weakGraphData.get().getGraphYAxis();
    } catch (RemoteException e) {
      String appName = weakGraphData.get().getAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakGraphData.get().getActivity(), R.string.error_accessing_database,
          Toast.LENGTH_LONG).show();
      return null;
    }
  }

  // @JavascriptInterface
  public String getGraphRAxis() {
    try {
      return weakGraphData.get().getGraphRAxis();
    } catch (RemoteException e) {
      String appName = weakGraphData.get().getAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakGraphData.get().getActivity(), R.string.error_accessing_database,
          Toast.LENGTH_LONG).show();
      return null;
    }
  }

  // @JavascriptInterface
  public String getBoxSource() {
    try {
      return weakGraphData.get().getBoxSource();
    } catch (RemoteException e) {
      String appName = weakGraphData.get().getAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakGraphData.get().getActivity(), R.string.error_accessing_database,
          Toast.LENGTH_LONG).show();
      return null;
    }
  }

  // @JavascriptInterface
  public String getBoxValues() {
    try {
      return weakGraphData.get().getBoxValues();
    } catch (RemoteException e) {
      String appName = weakGraphData.get().getAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakGraphData.get().getActivity(), R.string.error_accessing_database,
          Toast.LENGTH_LONG).show();
      return null;
    }
  }

  // @JavascriptInterface
  public String getBoxIterations() {
    try {
      return weakGraphData.get().getBoxIterations();
    } catch (RemoteException e) {
      String appName = weakGraphData.get().getAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakGraphData.get().getActivity(), R.string.error_accessing_database,
          Toast.LENGTH_LONG).show();
      return null;
    }
  }

  // @JavascriptInterface
  public String getBoxOperation() {
    try {
      return weakGraphData.get().getBoxOperation();
    } catch (RemoteException e) {
      String appName = weakGraphData.get().getAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakGraphData.get().getActivity(), R.string.error_accessing_database,
          Toast.LENGTH_LONG).show();
      return null;
    }
  }

  // @JavascriptInterface
  public String getGraphOp() {
    try {
      return weakGraphData.get().getGraphOp();
    } catch (RemoteException e) {
      String appName = weakGraphData.get().getAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakGraphData.get().getActivity(), R.string.error_accessing_database,
          Toast.LENGTH_LONG).show();
      return null;
    }
  }

  // @JavascriptInterface
  public void saveSelection(String aspect, String value) {
    try {
      weakGraphData.get().saveSelection(aspect, value);
    } catch (RemoteException e) {
      String appName = weakGraphData.get().getAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakGraphData.get().getActivity(), R.string.error_accessing_database,
          Toast.LENGTH_LONG).show();
    }
  }

  // @JavascriptInterface
  public void deleteDefaultGraph() {
    try {
      weakGraphData.get().deleteDefaultGraph();
    } catch (RemoteException e) {
      String appName = weakGraphData.get().getAppName();
      WebLogger.getLogger(appName).printStackTrace(e);
      WebLogger.getLogger(appName).e(TAG, "Error accessing database: " + e.toString());
      Toast.makeText(weakGraphData.get().getActivity(), R.string.error_accessing_database,
          Toast.LENGTH_LONG).show();
    }
  }
}