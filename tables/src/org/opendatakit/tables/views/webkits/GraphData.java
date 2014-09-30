package org.opendatakit.tables.views.webkits;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.common.android.data.KeyValueStoreEntry;
import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.KeyValueStoreHelper.AspectHelper;
import org.opendatakit.common.android.database.DataModelDatabaseHelper;
import org.opendatakit.common.android.database.DataModelDatabaseHelperFactory;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.LocalKeyValueStoreConstants;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class GraphData {

  private static final String TAG = "GraphData";

  // These are the partition and aspect helpers for setting info in the KVS.
  private final Context mContext;
  private final String mAppName;
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

  public GraphData(Context context, String appName, String tableId, String graphString) {
    isModified = false;
    this.graphString = graphString;
    this.mContext = context;
    this.mAppName = appName;
    this.mTableId = tableId;
    
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
    SQLiteDatabase db = null;
    try {
      db = DataModelDatabaseHelperFactory.getDatabase(mContext, mAppName);
      KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, 
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
        db.close();
      }
    }
  }

  public void setPermissions(String graphName, boolean isImmutable) {
    SQLiteDatabase db = null;
    try {
      db = DataModelDatabaseHelperFactory.getDatabase(mContext, mAppName);
      KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, 
          mTableId, LocalKeyValueStoreConstants.Graph.PARTITION_VIEWS);
      AspectHelper aspectHelper = kvsh.getAspectHelper(graphName);
      if (isImmutable) {
        aspectHelper.setString(MODIFIABLE, "immutable");
      }
    } finally {
      if ( db != null ) {
        db.close();
      }
    }
  }

  // If the graph is DEFAULT_GRAPH then the aspectHelper field is replaced
  // with the new name
  // and the DEFAULT_GRAPH aspect and contents are deleted
  private void saveGraphToName(String graphName) {
    if (graphName == null) {
      return;
    }
    SQLiteDatabase db = null;
    try {
      db = DataModelDatabaseHelperFactory.getDatabase(mContext, mAppName);
      KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, 
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
    } finally {
      if ( db != null ) {
        db.close();
      }
    }
  }

  public boolean hasGraph(String graph) {
    List<KeyValueStoreEntry> graphViewEntries = new ArrayList<KeyValueStoreEntry>();
    SQLiteDatabase db = null;
    try {
      DataModelDatabaseHelper dbh = DataModelDatabaseHelperFactory.getDbHelper(Tables.getInstance().getApplicationContext(), mAppName);
      db = dbh.getReadableDatabase();
      graphViewEntries = ODKDatabaseUtils.getDBTableMetadata(db, mTableId, 
          LocalKeyValueStoreConstants.Graph.PARTITION_VIEWS, null, LocalKeyValueStoreConstants.Graph.KEY_GRAPH_TYPE);
    } finally {
      if ( db != null ) {
        db.close();
      }
    }
    
    for ( KeyValueStoreEntry e : graphViewEntries ) {
      if ( e.aspect.equals(graph)) {
        return true;
      }
    }
    return false;
  }

  public String getGraphType() {
    SQLiteDatabase db = null;
    try {
      db = DataModelDatabaseHelperFactory.getDatabase(mContext, mAppName);
      KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, 
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
        db.close();
      }
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
    SQLiteDatabase db = null;
    try {
      db = DataModelDatabaseHelperFactory.getDatabase(mContext, mAppName);
      KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, 
          mTableId, LocalKeyValueStoreConstants.Graph.PARTITION_VIEWS);
      AspectHelper aspectHelper = kvsh.getAspectHelper(graphString);
      String oldValue = aspectHelper.getString(aspect);
      if (oldValue == null || !oldValue.equals(value)) {
        isModified = true;
      }
      aspectHelper.setString(aspect, value);
    } finally {
      if ( db != null ) {
        db.close();
      }
    }
  }

  private String loadSelection(String value) {
    SQLiteDatabase db = null;
    try {
      db = DataModelDatabaseHelperFactory.getDatabase(mContext, mAppName);
      KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, 
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
        db.close();
      }
    }
  }

  public void deleteDefaultGraph() {
    SQLiteDatabase db = null;
    try {
      db = DataModelDatabaseHelperFactory.getDatabase(mContext, mAppName);
      KeyValueStoreHelper kvsh = new KeyValueStoreHelper(db, 
          mTableId, LocalKeyValueStoreConstants.Graph.PARTITION_VIEWS);
      AspectHelper aspectHelper = kvsh.getAspectHelper(graphString);
      aspectHelper.deleteAllEntriesInThisAspect();
    } finally {
      if ( db != null ) {
        db.close();
      }
    }
  }
}
