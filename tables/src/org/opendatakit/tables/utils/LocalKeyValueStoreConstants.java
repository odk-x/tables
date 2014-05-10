package org.opendatakit.tables.utils;

/**
 * 
 * @author sudar.sam@gmail.com
 *
 */
public class LocalKeyValueStoreConstants {
  
  public static class Graph {
    public static final String PARTITION = "GraphDisplayActivity";
    public static final String PARTITION_VIEWS = PARTITION + ".views";
    public static final String KEY_GRAPH_VIEW_NAME = "nameOfGraphView";
    public static final String KEY_GRAPH_TYPE = "graphtype";
  }
  
  public static class Map {
    public static final String PARTITION = "TableMapFragment";
    /** The key to grab which column is being used for latitude. */
    public static final String KEY_MAP_LAT_COL = "keyMapLatCol";
    /** The key to grab which column is being used for longitude. */
    public static final String KEY_MAP_LONG_COL = "keyMapLongCol";
    /** The key to grab which file is being used for the list view. */
    public static final String KEY_FILENAME = "keyFilename";
  }

}
