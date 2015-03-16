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

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.common.android.utilities.KeyValueStoreHelper;
import org.opendatakit.common.android.utilities.KeyValueStoreHelper.AspectHelper;
import org.opendatakit.common.android.utilities.LocalKeyValueStoreConstants;
import org.opendatakit.database.service.KeyValueStoreEntry;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.application.Tables;

import android.app.Activity;
import android.os.RemoteException;

public class GraphData {

  private static final String TAG = "GraphData";

  // These are the partition and aspect helpers for setting info in the KVS.
  private final AbsBaseActivity mAbsBaseActivity;
  private final String mTableId;
  private final String graphString;
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

  public GraphData(AbsBaseActivity absBaseActivity, String tableId, String graphString) {
    isModified = false;
    this.graphString = graphString;
    this.mAbsBaseActivity = absBaseActivity;
    this.mTableId = tableId;
    
// TODO
//    if (potentialGraphName != null) {
//      this.aspectHelper = saveGraphToName(potentialGraphName);
//    }
  }
  
  public String getAppName() {
    return mAbsBaseActivity.getAppName();
  }
  
  public Activity getActivity() {
    return mAbsBaseActivity;
  }

  public boolean isModified() {
    // TODO Auto-generated method stub
    return isModified;
  }

  // determine if the graph is mutable or only for viewing
  public boolean isModifiable() throws RemoteException {
    String appName = mAbsBaseActivity.getAppName();
    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(appName, false);
      KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName, db, 
          mTableId, LocalKeyValueStoreConstants.Graph.PARTITION_VIEWS);
      AspectHelper aspectHelper = kvsh.getAspectHelper(graphString);
      String result = aspectHelper.getString(MODIFIABLE);
      if (result == null) {
        return true;
      } else {
        return false;
      }
    } finally {
      if ( db != null ) {
        Tables.getInstance().getDatabase().closeDatabase(appName, db);
      }
    }
  }

  public void setPermissions(String graphName, boolean isImmutable) throws RemoteException {
    String appName = mAbsBaseActivity.getAppName();
    OdkDbHandle db = null;
    boolean successful = false;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(appName, true);
      KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName, db, 
          mTableId, LocalKeyValueStoreConstants.Graph.PARTITION_VIEWS);
      AspectHelper aspectHelper = kvsh.getAspectHelper(graphName);
      if (isImmutable) {
        aspectHelper.setString(MODIFIABLE, "immutable");
      }
      successful = true;
    } finally {
      if ( db != null ) {
        Tables.getInstance().getDatabase().closeTransactionAndDatabase(appName, db, successful);
      }
    }
  }

  // If the graph is DEFAULT_GRAPH then the aspectHelper field is replaced
  // with the new name
  // and the DEFAULT_GRAPH aspect and contents are deleted
  private void saveGraphToName(String graphName) throws RemoteException {
    if (graphName == null) {
      return;
    }
    String appName = mAbsBaseActivity.getAppName();
    OdkDbHandle db = null;
    boolean successful = false;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(appName, true);
      KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName, db, 
          mTableId, LocalKeyValueStoreConstants.Graph.PARTITION_VIEWS);
      AspectHelper aspectHelper = kvsh.getAspectHelper(graphString);
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
      successful = true;
    } finally {
      if ( db != null ) {
        Tables.getInstance().getDatabase().closeTransactionAndDatabase(appName, db, successful);
      }
    }
  }

  public boolean hasGraph(String graph) throws RemoteException {
    List<KeyValueStoreEntry> graphViewEntries = new ArrayList<KeyValueStoreEntry>();
    String appName = mAbsBaseActivity.getAppName();
    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(appName, false);
      graphViewEntries = Tables.getInstance().getDatabase().getDBTableMetadata( appName, db, mTableId, 
          LocalKeyValueStoreConstants.Graph.PARTITION_VIEWS, null, LocalKeyValueStoreConstants.Graph.KEY_GRAPH_TYPE);
    } finally {
      if ( db != null ) {
        Tables.getInstance().getDatabase().closeDatabase(appName, db);
      }
    }
    
    for ( KeyValueStoreEntry e : graphViewEntries ) {
      if ( e.aspect.equals(graph)) {
        return true;
      }
    }
    return false;
  }

  public String getGraphType() throws RemoteException {
    String appName = mAbsBaseActivity.getAppName();
    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(appName, false);
      KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName, db, 
          mTableId, LocalKeyValueStoreConstants.Graph.PARTITION_VIEWS);
      AspectHelper aspectHelper = kvsh.getAspectHelper(graphString);
      String graphType = aspectHelper.getString(
          LocalKeyValueStoreConstants.Graph.KEY_GRAPH_TYPE);
      if (graphType == null || graphType.equals("unset type")) {
        return "";
      } else {
        return graphType;
      }
    } finally {
      if ( db != null ) {
        Tables.getInstance().getDatabase().closeDatabase(appName, db);
      }
    }
  }

  public String getBoxOperation() throws RemoteException {
    return loadSelection(BOX_OPTION);
  }

  public String getBoxSource() throws RemoteException {
    return loadSelection(BOX_SOURCE);
  }

  public String getBoxValues() throws RemoteException {
    return loadSelection(BOX_VALUES);
  }

  public String getBoxIterations() throws RemoteException {
    return loadSelection(ITER_COUNTER);
  }

  public String getGraphXAxis() throws RemoteException {
    return loadSelection(X_AXIS);
  }

  public String getGraphYAxis() throws RemoteException {
    return loadSelection(Y_AXIS);
  }

  public String getGraphRAxis() throws RemoteException {
    return loadSelection(R_AXIS);
  }

  public String getGraphOp() throws RemoteException {
    return loadSelection(AGREG);
  }

  public void saveSelection(String aspect, String value) throws RemoteException {
    String appName = mAbsBaseActivity.getAppName();
    OdkDbHandle db = null;
    boolean successful = false;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(appName, true);
      KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName, db, 
          mTableId, LocalKeyValueStoreConstants.Graph.PARTITION_VIEWS);
      AspectHelper aspectHelper = kvsh.getAspectHelper(graphString);
      String oldValue = aspectHelper.getString(aspect);
      if (oldValue == null || !oldValue.equals(value)) {
        isModified = true;
      }
      aspectHelper.setString(aspect, value);
      successful = true;
    } finally {
      if ( db != null ) {
        Tables.getInstance().getDatabase().closeTransactionAndDatabase(appName, db, successful);
      }
    }
  }

  private String loadSelection(String value) throws RemoteException {
    String appName = mAbsBaseActivity.getAppName();
    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(appName, false);
      KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName, db, 
          mTableId, LocalKeyValueStoreConstants.Graph.PARTITION_VIEWS);
      AspectHelper aspectHelper = kvsh.getAspectHelper(graphString);
      String result = aspectHelper.getString(value);
      if (result == null) {
        return "";
      } else {
        return result;
      }
    } finally {
      if ( db != null ) {
        Tables.getInstance().getDatabase().closeDatabase(appName, db);
      }
    }
  }

  public void deleteDefaultGraph() throws RemoteException {
    String appName = mAbsBaseActivity.getAppName();
    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(appName, false);
      KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName, db, 
          mTableId, LocalKeyValueStoreConstants.Graph.PARTITION_VIEWS);
      AspectHelper aspectHelper = kvsh.getAspectHelper(graphString);
      aspectHelper.deleteAllEntriesInThisAspect();
    } finally {
      if ( db != null ) {
        Tables.getInstance().getDatabase().closeDatabase(appName, db);
      }
    }
  }
}
