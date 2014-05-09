package org.opendatakit.tables.views.webkits;

import java.util.List;

import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.KeyValueStoreHelper.AspectHelper;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.activities.GraphDisplayActivity;

import android.util.Log;

public class GraphData {

  private static final String TAG = "GraphData";

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

  public GraphDataIf getJavascriptInterfaceWithWeakReference() {
    return new GraphDataIf(this);
  }

  public GraphData(TableProperties tableProperties, String graphString) {
    isModified = false;
    this.graphString = graphString;
    this.kvsh = tableProperties.getKeyValueStoreHelper(GraphDisplayActivity.KVS_PARTITION_VIEWS);
    this.aspectHelper = kvsh.getAspectHelper(this.graphString);
// TODO 
//    if (potentialGraphName != null) {
//      this.aspectHelper = saveGraphToName(potentialGraphName);
//    }
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
    if (isImmutable) {
      newAspectHelper.setString(MODIFIABLE, "immutable");
    }
  }

  // If the graph is DEFAULT_GRAPH then the aspectHelper field is replaced
  // with the new name
  // and the DEFAULT_GRAPH aspect and contents are deleted
  private AspectHelper saveGraphToName(String graphName) {
    if (graphName == null) {
      return null;
    }
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
      } else if (getGraphType().equals("Line Graph")) {
        newAspectHelper.setString("selectx", aspectHelper.getString(X_AXIS));
        newAspectHelper.setString("selecty", aspectHelper.getString(Y_AXIS));
      } else if (getGraphType().equals("Box Plot")) {
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
