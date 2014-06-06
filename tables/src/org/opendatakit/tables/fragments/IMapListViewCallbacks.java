package org.opendatakit.tables.fragments;


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

}
