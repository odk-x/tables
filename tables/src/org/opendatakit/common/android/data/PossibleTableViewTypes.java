package org.opendatakit.common.android.data;

import java.util.HashSet;
import java.util.Set;

/**
 * Contains information about which {@link TableViewType}s are valid for a
 * table based on its configuration. A List view may only be appropriate if a
 * list file has been set, for example.
 * @author sudar.sam@gmail.com
 *
 */
public class PossibleTableViewTypes {
  
  private boolean mSpreadsheetIsValid;
  private boolean mListIsValid;
  private boolean mMapIsValid;
  private boolean mGraphIsValid;
  
  public PossibleTableViewTypes(
      boolean spreadsheetIsValid,
      boolean listIsValid,
      boolean mapIsValid,
      boolean graphIsValid) {
    this.mSpreadsheetIsValid = spreadsheetIsValid;
    this.mListIsValid = listIsValid;
    this.mMapIsValid = mapIsValid;
    this.mGraphIsValid = graphIsValid;
  }
  
  /**
   * Get a set with all the {@link TableViewType}s that are valid. If only a
   * spreadsheet and list view are possible, for instance, it will contain
   * {@link TableViewType#SPREADSHEET} and {@link TableViewType#LIST}.
   * @return a {@link Set} of the possible view types.
   */
  public Set<TableViewType> getAllPossibleViewTypes() {
    Set<TableViewType> result = new HashSet<TableViewType>();
    if (this.spreadsheetViewIsPossible()) {
      result.add(TableViewType.SPREADSHEET);
    }
    if (this.listViewIsPossible()) {
      result.add(TableViewType.LIST);
    }
    if (this.mapViewIsPossible()) {
      result.add(TableViewType.MAP);
    }
    if (this.graphViewIsPossible()) {
      result.add(TableViewType.GRAPH);
    }
    return result;
  }
  
  /**
   * 
   * @return true if the table can be viewed as a spreadsheet
   */
  public boolean spreadsheetViewIsPossible() {
    return this.mSpreadsheetIsValid;
  }
  
  /**
   * 
   * @return true if the table can be displayed as a list
   */
  public boolean listViewIsPossible() {
    return this.mListIsValid;
  }
  
  /**
   * 
   * @return true if the table can be displayed as a map
   */
  public boolean mapViewIsPossible() {
    return this.mMapIsValid;
  }
  
  /**
   * 
   * @return true if the table can be displayed as a graph
   */
  public boolean graphViewIsPossible() {
    return this.mGraphIsValid;
  }
  

}
