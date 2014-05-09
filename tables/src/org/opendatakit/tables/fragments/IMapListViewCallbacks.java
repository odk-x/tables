package org.opendatakit.tables.fragments;

import java.util.ArrayList;

/**
 * 
 * @author sudar.sam@gmail.com
 *
 */
public interface IMapListViewCallbacks {
  
  /**
   * Sets the index of the row that is selected.
   * @param index
   */
  public void setMapListIndex(int index);
  
  /**
   * Set the indices of the rows that should be displayed by this fragment.
   * This will be a subset of all the rows in the table.
   * @param indices
   */
  public void setMapListIndices(ArrayList<Integer> indices);
  
  /**
   * Get the indices of the rows that are being displayed.
   * @return
   */
  public ArrayList<Integer> getMapListIndices();
  

}
