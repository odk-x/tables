package org.opendatakit.tables.fragments;

import org.opendatakit.tables.views.webkits.CustomView;

import android.app.Fragment;
import android.os.Bundle;

/**
 * Interface defining behavior for those {@link Fragment}s that display a
 * {@link CustomView}.
 * <p>
 * All such fragments should set and retrieve the file name in
 * {@link Fragment#onSaveInstanceState(Bundle)} and
 * {@link Fragment#onCreate(Bundle)}. 
 * @author sudar.sam@gmail.com
 *
 */
public interface IWebFragment {
  
  /**
   * Retrieve the file name that should be displayed.
   * @param bundle
   * @return the file name, or null if one has not been set.
   */
  public String retrieveFileNameFromBundle(Bundle bundle);
  
  /**
   * Store the file name in a bundle.
   * @param bundle
   */
  public void putFileNameInBundle(Bundle bundle);
  
  public CustomView buildView();
  
  /**
   * Get the file name that is being displayed.
   * @return
   */
  public String getFileName();

}
