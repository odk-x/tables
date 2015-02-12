/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
