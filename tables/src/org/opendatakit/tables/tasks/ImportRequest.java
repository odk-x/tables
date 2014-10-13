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


public class ImportRequest {

    private final String fileQualifier;

    private final boolean createTable;
    private final String tableId;

    public ImportRequest(String tableId, String fileQualifier) {
      this(true, tableId, fileQualifier);
  }

    public ImportRequest(boolean createTable, String tableId, String fileQualifier) {
      this.createTable = createTable;
      this.tableId = tableId;
      this.fileQualifier = fileQualifier;
  }

    public boolean getCreateTable() {
        return createTable;
    }

    public String getTableId() {
        return tableId;
    }

    public String getFileQualifier() {
      return fileQualifier;
    }
}