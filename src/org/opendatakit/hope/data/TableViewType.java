package org.opendatakit.hope.data;

import android.util.Log;

/**
 * This represents the current view of the table. If the value is spreadsheet,
 * then the current view of the table is spreadsheet. If the value is list then
 * it is a list, etc.
 * @author sudar.sam@gmail.com
 *
 */
public enum TableViewType {
  Spreadsheet(0),
  List(1),
  Map(2),
  Graph(3);
    
  private int id;
  
  public static String TAG = "TableViewType";
  
  private TableViewType(int id) {
    this.id = id;
  }
  
  /**
   * Get the id for this item. This is just an int, unique to each constant in
   * this enum. It is intended to be mainly to be used just for menu item 
   * selection, at least until the transition away from TableViewSettings is
   * more complete. (Dec14, 2012).
   * @return
   */
  public int getId() {
    return id;
  }
  
  public static TableViewType getViewTypeFromId(int id) {
    switch (id) {
    case 0:
      return Spreadsheet;
    case 1: 
      return List;
    case 2: 
      return Map;
    case 3: 
      return Graph;
    default:
      Log.e(TAG, "unrecognized view type: " + id + ", setting to spreadsheet");
      return Spreadsheet;
    }
  }
}
