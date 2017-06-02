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

import org.opendatakit.tables.views.webkits.OdkTablesWebView;

/**
 * Interface defining behavior for those Fragments that display a
 * {@link org.opendatakit.views.ODKWebView }.
 *
 * @author sudar.sam@gmail.com
 */
public interface IWebFragment {

  /**
   * Get the webkit in this fragment
   *
   * @return
   */
  OdkTablesWebView getWebKit();

  /**
   * Toggles visibility of the "database unavailable" text box and
   * the webkit based upon the accessibility of the database.
   */
  void setWebKitVisibility();

}
