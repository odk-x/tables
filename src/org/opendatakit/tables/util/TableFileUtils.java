/*
 * Copyright (C) 2012 University of Washington
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
package org.opendatakit.tables.util;

/**
 * This is a general place for utils regarding odktables files. These are files
 * that are associated with various tables, such as html files for different
 * views, etc.
 * @author sudar.sam@gmail.com
 *
 */
public class TableFileUtils {

  /** The default app name for ODK Tables */
  public static final String ODK_TABLES_APP_NAME = "tables";

  /** Timeout (in ms) we specify for each http request */
  public static final int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;

  /** URI from base to get the manifest for a server. */
  public static final String MANIFEST_ADDR_URI = "/tableKeyValueManifest";

  /** The url parameter name of the tableId. */
  public static final String TABLE_ID_PARAM = "tableId";

  /** The response type expected from the server for a json object. */
  public static final String RESP_TYPE_JSON = "application/json; charset=utf-8";

}
