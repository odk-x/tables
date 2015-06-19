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
package org.opendatakit.tables.tasks;


public class ExportRequest {

  private final String appName;
  private final String tableId;
    private final String fileQualifier;

    /**
     * New style CSV export.
     * Exports two csv files to the output/csv directory under the appName:
     * <ul>
     * <li>tableid.fileQualifier.csv - data table</li>
     * <li>tableid.fileQualifier.properties.csv - metadata definition of this table</li>
     * </ul>
     * If fileQualifier is null or an empty string, then it emits to
     * <ul>
     * <li>tableid.csv - data table</li>
     * <li>tableid.properties.csv - metadata definition of this table</li>
     * </ul>
     *
     * @param tp
     * @param directory
     * @param fileQualifier
     */
    public ExportRequest(String appName, String tableId, String fileQualifier) {
      this.appName = appName;
      this.tableId = tableId;
      this.fileQualifier = fileQualifier;
    }

    public String getAppName() {
      return appName;
    }
    
    public String getTableId() {
      return tableId;
    }

    public String getFileQualifier() {
      return fileQualifier;
    }
}