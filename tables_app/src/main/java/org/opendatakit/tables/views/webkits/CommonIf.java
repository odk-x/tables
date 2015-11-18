/*
 * Copyright (C) 2015 University of Washington
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

package org.opendatakit.tables.views.webkits;

import java.lang.ref.WeakReference;

/**
 * Created by clarice on 11/3/15.
 */
public class CommonIf {

   public static final String TAG = "CommonIf";

   private WeakReference<Common> weakControl;

   CommonIf(Common common) {
      weakControl = new WeakReference<Common>(common);
   }

   /**
    * Return the platform info as a stringified json object. This is an object
    * containing the keys: container, version, appName, baseUri, logLevel.
    *
    * @return a stringified json object with the above keys
    */
   @android.webkit.JavascriptInterface
   public String getPlatformInfo() {
      return weakControl.get().getPlatformInfo();
   }

   /**
    * Take the path of a file relative to the app folder and return a url by
    * which it can be accessed.
    *
    * @param relativePath
    * @return an absolute URI to the file
    */
   @android.webkit.JavascriptInterface
   public String getFileAsUrl(String relativePath) {
      return weakControl.get().getFileAsUrl(relativePath);
   }

   /**
    * Convert the rowpath value for a media attachment (e.g., uriFragment) field
    * into a url by which it can be accessed.
    *
    * @param tableId
    * @param rowId
    * @param rowPathUri
    * @return
    */
   @android.webkit.JavascriptInterface
   public String getRowFileAsUrl(String tableId, String rowId, String rowPathUri) {
      return weakControl.get().getRowFileAsUrl(tableId, rowId, rowPathUri);
   }

   /**
    * Log messages using WebLogger.
    *
    *
    * @param level - levels are A, D, E, I, S, V, W
    * @param loggingString - actual message to log
    * @return
    */
   @android.webkit.JavascriptInterface
   public void log(String level, String loggingString) {
      weakControl.get().log(level, loggingString);
   }

   /**
    * Get device properties
    *
    * @param propertyId
    * @return
    */
   @android.webkit.JavascriptInterface
   public String getProperty(String propertyId) {
      return weakControl.get().getProperty(propertyId);
   }

   /**
    * Get the base url
    *
    * @return
    */
   @android.webkit.JavascriptInterface
   public String getBaseUrl() {
      return weakControl.get().getBaseUrl();
   }
}
