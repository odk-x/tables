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
 * Describes a request to import a csv file
 */
public class ImportRequest {

  // The prefix for the csv file to import
  private final String fileQualifier;
  // whether to create the table if it doesn't already exist
  private final boolean createTable;
  // the id of the table to import
  private final String tableId;

  /**
   * forwards request to the three argument constructor
   *
   * @param tableId       table id
   * @param fileQualifier filename prefix
   */
  public ImportRequest(String tableId, String fileQualifier) {
    this(true, tableId, fileQualifier);
  }

  /**
   * simple constructor that stores its three arguments
   *
   * @param createTable   whether to create the table if it doesn't exist
   * @param tableId       the id of the table
   * @param fileQualifier the prefix for the csv file to import
   */
  public ImportRequest(boolean createTable, String tableId, String fileQualifier) {
    this.createTable = createTable;
    this.tableId = tableId;
    this.fileQualifier = fileQualifier;
  }

  /**
   * standard getter for whether we should create the table if it doesn't already exist
   *
   * @return whether we should create the table if it doesn't already exist
   */
  public boolean getCreateTable() {
    return createTable;
  }

  /**
   * standard getter for the table id
   *
   * @return the table id
   */
  public String getTableId() {
    return tableId;
  }

  /**
   * standard getter for the filename prefix
   *
   * @return the prefix for the filename
   */
  public String getFileQualifier() {
    return fileQualifier;
  }
}