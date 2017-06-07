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

/**
 * this class describes a request to export a table to a csv file
 */
public class ExportRequest {

  // the app name
  private final String appName;
  // the id of the table to export
  private final String tableId;
  // the prefix for the filename of the exported csv files
  private final String fileQualifier;

  /**
   * All the actual exporting is handled by androidlibrary/builder/CsvUtil
   *
   * @param appName       the app name
   * @param tableId       the id of the table to export
   * @param fileQualifier the prefix for the filename of the exported csv files
   */
  public ExportRequest(String appName, String tableId, String fileQualifier) {
    this.appName = appName;
    this.tableId = tableId;
    this.fileQualifier = fileQualifier;
  }

  /**
   * standard getter for the app name
   *
   * @return the app name
   */
  public String getAppName() {
    return appName;
  }

  /**
   * standard getter for the table id
   *
   * @return the id of the table to export
   */
  public String getTableId() {
    return tableId;
  }

  /**
   * standard getter for the file qualifier
   *
   * @return the prefix for the csv filenames
   */
  public String getFileQualifier() {
    return fileQualifier;
  }
}