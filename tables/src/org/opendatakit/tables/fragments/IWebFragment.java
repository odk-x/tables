package org.opendatakit.tables.fragments;

import org.opendatakit.tables.views.webkits.Control;
import org.opendatakit.tables.views.webkits.ControlIf;
import org.opendatakit.tables.views.webkits.TableDataIf;

import android.app.Fragment;
import android.os.Bundle;
import android.webkit.WebView;

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
  
  /**
   * Create and return the {@link WebView} that will be added to this fragment.
   * Any JavaScript interfaces that will be added should be added to the
   * view before it is returned. If these objects are {@link ControlIf} or
   * {@link TableDataIf}, a reference must be saved in the calling fragment or
   * it will eventually return null.
   * @return
   */
  public WebView buildView();
  
  /**
   * Get the file name that is being displayed.
   * @return
   */
  public String getFileName();
  
  /**
   * Create a {@link Control} object that can be added to this webview.
   * @return
   */
  public Control createControlObject();

}
