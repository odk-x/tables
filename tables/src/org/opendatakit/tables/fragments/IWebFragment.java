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

import android.app.Fragment;
import android.os.Bundle;
import android.os.RemoteException;

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
   * Get the file name that is being displayed.
   * @return
   */
  public String getFileName();
  
  /**
   * Set the file name that is to be displayed.
   * 
   * @param relativeFileName
   */
  public void setFileName(String relativeFileName);
  
  /**
   * Create a {@link Control} object that can be added to this webview.
   * @return
   * @throws RemoteException 
   */
  public Control createControlObject() throws RemoteException;
  
  /**
   * Toggles visibility of the "database unavailable" text box and 
   * the webkit based upon the accessibility of the database.
   */
  public abstract void setWebKitVisibility();

}
