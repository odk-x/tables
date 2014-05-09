package org.opendatakit.tables.utils;

/**
 * Holds basic information about a saved graph view.
 * @author sudar.sam@gmail.com
 *
 */
public class GraphViewStruct {
  
  public String graphName;
  public String graphType;
  public boolean isDefault;
  
  public GraphViewStruct(
      String graphName,
      String graphType,
      boolean isDefault) {
    this.graphName = graphName;
    this.graphType = graphType;
    this.isDefault = isDefault;
  }
}
