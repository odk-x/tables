/*
 * Copyright (C) 2015 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.tables.views.webkits;

import org.opendatakit.common.android.listener.DatabaseConnectionListener;
import org.opendatakit.database.service.OdkDbInterface;

/**
 * @author mitchellsundt@gmail.com
 */
public interface ICallbackFragment {
    /**
     * The fragment should queue the response in a saveInstanceState queue and notify
     * the JS that there is data available. The JS will then retrieve the responseJSON,
     * decode it and access the callbackJSON to identify the callback and then invoke
     * the identified callback (in an implementation-dependent manner).
     *
     * @param responseJSON
     */
    public void signalResponseAvailable(String responseJSON );

  /**
   * Access the queued responseJSON
   *
   * @return responseJSON or null if there is none available
   */
  public String getResponseJSON();

    /**
     * The fragment should remember this listener and notify it of any databse connection
     * status changes.  There will be only one database connection listener registered at
     * any one time.
     *
     * @param listener
     */
    public void registerDatabaseConnectionBackgroundListener(DatabaseConnectionListener listener);

    /**
     * Get the active database interface, if any.
     *
     * @return null if not available.
     */
    public OdkDbInterface getDatabase();

    /**
     * Get our application name
     *
     * @return the appName under which we are running
     */
    public String getAppName();
}
