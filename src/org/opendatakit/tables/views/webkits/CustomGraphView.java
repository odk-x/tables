/*
 * Copyright (C) 2012 University of Washington
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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.common.android.provider.FileProvider;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.activities.graphs.GraphDisplayActivity;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.KeyValueStoreHelper.AspectHelper;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.utils.TableFileUtils;

import android.app.Activity;
import android.util.Log;

public class CustomGraphView extends CustomView {

  private static final String DEFAULT_HTML = "<html><body>"
      + "<p>No filename has been specified.</p>" + "</body></html>";

  private Map<String, Integer> colIndexTable;
  private UserTable table;
  private String filename;
  private String graphName;
  private String potentialGraphName;
  // IMPORTANT: hold a strong reference to control because Webkit holds a weak
  // reference
  private Control control;
  // IMPORTANT: hold a strong reference to tableData because Webkit holds a weak
  // reference
  private TableData tableData;
  // IMPORTANT: hold a strong reference to graphData because Webkit holds a weak
  // reference
  private GraphData graphData;

  private CustomGraphView(Activity activity, String appName, String graphName,
                          String potentialGraphName, CustomViewCallbacks callbacks) {
    super(activity, appName, callbacks);
    this.filename = TableFileUtils.getRelativePathToGraphFile(appName);
    this.graphName = graphName;
    this.potentialGraphName = potentialGraphName;
    Log.i("CustomGraphView", "IDDD: " + graphName);
    colIndexTable = new HashMap<String, Integer>();
  }

  public static CustomGraphView get(Activity activity, String appName, UserTable table,
                                    String graphName, String potentialGraphName,
                                    Controller controller) {
    CustomGraphView ctv = new CustomGraphView(activity, appName, graphName, potentialGraphName,
                                              controller);
    ctv.set(table);
    return ctv;
  }

  private void set(UserTable table) {
    this.table = table;
    colIndexTable.clear();
    Map<String, ColumnProperties> elementKeyToColumnProperties = table.getTableProperties()
        .getDatabaseColumns();
    colIndexTable.putAll(table.getMapOfUserDataToIndex());
    for (ColumnProperties cp : elementKeyToColumnProperties.values()) {
      String smsLabel = cp.getSmsLabel();
      if (smsLabel != null) {
        // TODO: this doesn't look to ever be used, and ignores the possibility
        // of conflicting element keys and sms labels.
        colIndexTable.put(smsLabel, colIndexTable.get(cp.getElementKey()));
      }
    }
    graphData = new GraphData(graphName);
  }

  public void display() {
    control = new Control(mParentActivity);
    tableData = new TableData(table);
    addJavascriptInterface(control.getJavascriptInterfaceWithWeakReference(), "control");
    addJavascriptInterface(tableData.getJavascriptInterfaceWithWeakReference(), "data");
    addJavascriptInterface(graphData.getJavascriptInterfaceWithWeakReference(), "graph_data");
    if (filename != null) {
      String fullPath = FileProvider.getAsWebViewUri(getContext(), mAppName,
          ODKFileUtils.asUriFragment(mAppName, new File(filename)));
      load(fullPath);
    } else {
      loadData(DEFAULT_HTML, "text/html", null);
    }
    initView();
  }

  public void createNewGraph(String graphName) {
    graphData.saveGraphToName(graphName);
  }

  public void setPermissions(String givenGraphName, boolean isImmutable) {
		graphData.setPermissions(givenGraphName, isImmutable);
  }

  public boolean hasGraph(String graph) {
    return graphData.hasGraph(graph);
  }

  public boolean graphIsModified() {
    return graphData.isModified();
  }

  /**
   * "Unused" warnings are suppressed because the public methods of this class
   * are meant to be called through the JavaScript interface.
   */
  protected class GraphData {

    // These are the partition and aspect helpers for setting info in the KVS.
    private KeyValueStoreHelper kvsh;
    private AspectHelper aspectHelper;
    private String graphString;
    private boolean isModified;
    private static final String GRAPH_TYPE = "graphtype";
    private static final String X_AXIS = "selectx";
    private static final String Y_AXIS = "selecty";
    private static final String AGREG = "operation";
    private static final String R_AXIS = "selectr";
    private static final String BOX_OPTION = "box_operation";
    private static final String BOX_SOURCE = "box_source";
    private static final String ITER_COUNTER = "iteration_counter";
    private static final String BOX_VALUES = "box_values";
    private static final String MODIFIABLE = "modifiable";

    private static final String TAG = "GraphData";

    public GraphDataIf getJavascriptInterfaceWithWeakReference() {
      return new GraphDataIf(this);
    }

    private GraphData(String graphString) {
      isModified = false;
      this.graphString = graphString;
      this.kvsh = table.getTableProperties().getKeyValueStoreHelper(
          GraphDisplayActivity.KVS_PARTITION_VIEWS);
      this.aspectHelper = kvsh.getAspectHelper(this.graphString);
      this.aspectHelper = saveGraphToName(potentialGraphName);
    }

    public boolean isModified() {
      // TODO Auto-generated method stub
      return isModified;
    }

    // determine if the graph is mutable or only for viewing
    public boolean isModifiable() {
    	String result = aspectHelper.getString(MODIFIABLE);
    	if (result == null) {
    		return true;
    	} else {
    		return false;
    	}
	}

    public void setPermissions(String graphName, boolean isImmutable) {
    	AspectHelper newAspectHelper = kvsh.getAspectHelper(graphName);
	    if(isImmutable) {
	    	newAspectHelper.setString(MODIFIABLE, "immutable");
	    }
    }
    // If the graph is DEFAULT_GRAPH then the aspectHelper field is replaced
    // with the new name
    // and the DEFAULT_GRAPH aspect and contents are deleted
    private AspectHelper saveGraphToName(String graphName) {
      AspectHelper newAspectHelper = kvsh.getAspectHelper(graphName);
      String graphType = aspectHelper.getString(GRAPH_TYPE);
      if (graphType != null) {
        if (hasGraph(graphName)) {
          newAspectHelper.deleteAllEntriesInThisAspect();
        }
        newAspectHelper.setString(GRAPH_TYPE, getGraphType());
        if (getGraphType().equals("Bar Graph") || getGraphType().equals("Pie Chart")) {
          newAspectHelper.setString("selectx", aspectHelper.getString(X_AXIS));
          newAspectHelper.setString("selecty", aspectHelper.getString(Y_AXIS));
          newAspectHelper.setString("operation", aspectHelper.getString(AGREG));
        } else if (getGraphType().equals("Scatter Plot")) {
          newAspectHelper.setString("selectx", aspectHelper.getString(X_AXIS));
          newAspectHelper.setString("selecty", aspectHelper.getString(Y_AXIS));
          newAspectHelper.setString("selectr", aspectHelper.getString(R_AXIS));
          newAspectHelper.setString("operation", aspectHelper.getString(AGREG));
        } else if(getGraphType().equals("Line Graph")) {
        	newAspectHelper.setString("selectx", aspectHelper.getString(X_AXIS));
            newAspectHelper.setString("selecty", aspectHelper.getString(Y_AXIS));
        } else if(getGraphType().equals("Box Plot")) {
        	newAspectHelper.setString("box_operation", aspectHelper.getString(BOX_OPTION));
        	newAspectHelper.setString("box_source", aspectHelper.getString(BOX_SOURCE));
        	newAspectHelper.setString("iteration_counter", aspectHelper.getString(ITER_COUNTER));
        	newAspectHelper.setString("box_values", aspectHelper.getString(BOX_VALUES));
        }
      } else {
        newAspectHelper.setString(GRAPH_TYPE, "unset type");
      }
      return newAspectHelper;
    }

    public boolean hasGraph(String graph) {
      List<String> list = kvsh.getAspectsForPartition();
      for (String s : list) {
        Log.d("stufftotest", "in list: " + s);
        if (graph.equals(s))
          return true;
      }
      return false;
    }

    public String getGraphType() {
      String graphType = aspectHelper.getString(GraphDisplayActivity.GRAPH_TYPE);
      if (graphType == null || graphType.equals("unset type")) {
        return "";
      } else {
        return graphType;
      }
    }

    public String getBoxOperation() {
    	return loadSelection(BOX_OPTION);
    }

    public String getBoxSource() {
    	return loadSelection(BOX_SOURCE);
    }

    public String getBoxValues() {
    	return loadSelection(BOX_VALUES);
    }

    public String getBoxIterations() {
    	return loadSelection(ITER_COUNTER);
    }

    public String getGraphXAxis() {
      return loadSelection(X_AXIS);
    }

    public String getGraphYAxis() {
      return loadSelection(Y_AXIS);
    }

    public String getGraphRAxis() {
      return loadSelection(R_AXIS);
    }

    public String getGraphOp() {
      return loadSelection(AGREG);
    }

    public void saveSelection(String aspect, String value) {
      String oldValue = aspectHelper.getString(aspect);
      if (oldValue == null || !oldValue.equals(value)) {
        isModified = true;
      }
      aspectHelper.setString(aspect, value);
    }

    private String loadSelection(String value) {
      String result = aspectHelper.getString(value);
      if (result == null) {
        return "";
      } else {
        return result;
      }
    }

    public void deleteDefaultGraph() {
      aspectHelper.deleteAllEntriesInThisAspect();
    }
  }

  // WARNING this destroys the GraphData field object. Use only to prevent
  // saving the default
  // graph when exiting this class
  public void deleteDefaultGraph() {
    graphData.deleteDefaultGraph();
  }
}
