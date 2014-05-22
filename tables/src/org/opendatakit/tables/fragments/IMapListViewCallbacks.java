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
  public void setIndexOfSelectedItem(int index);

  /**
   * Set the state to indicate that no row is selected. Resets the state set
   * with a call to {@link #setIndexOfSelectedItem(int)}.
   */
  public void setNoItemSelected();

  /**
   * Set the indices of the rows that should be displayed by this fragment.
   * This will be a subset of all the rows in the table.
   * @param indices
   */
  public void setSubsetOfIndicesToDisplay(ArrayList<Integer> indices);

  /**
   * Set the state to indicate that all items are being displayed rather than a
   * subset of the rows. Resets the state that was initialized with a call to
   * {@link IMapListViewCallbacks#setSubsetOfIndicesToDisplay(ArrayList)}.
   */
  public void setDisplayAllItems();

  /**
   * Get the indices of the rows that are being displayed.
   * @return
   */
  public ArrayList<Integer> getMapListIndices();


}
