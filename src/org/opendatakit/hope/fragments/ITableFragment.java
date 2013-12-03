package org.opendatakit.hope.fragments;

/**
 * Interface for a fragment in tables. The methods defined here must be made.
 * 
 * @author Chris Gelon (cgelon)
 */
public interface ITableFragment {
  /** Called when there is a change in data, or if the fragment is being created. */
  public void init();
  /** Called when the user is searching in the fragment. */
  public void onSearch();
}
