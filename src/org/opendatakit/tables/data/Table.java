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
package org.opendatakit.tables.data;

/**
 * A table of all or some of the data in a database table.
 * 
 * @author hkworden
 */
public class Table {
    
    private final String[] rowIds;
    private String[] header;
    private final String[][] data;
    
    public Table(String[] rowIds, String[] header, String[][] data) {
        this.rowIds = rowIds;
        this.header = header;
        this.data = data;
    }
    
    public String[] getRowIds() {
        return rowIds;
    }
    
    public String[][] getData() {
        return data;
    }
    
    public String getRowId(int rowNum) {
        return rowIds[rowNum];
    }
    
    public String getHeader(int colNum) {
        return header[colNum];
    }
    
    public String getData(int rowNum, int colNum) {
        return data[rowNum][colNum];
    }
    
    public int getWidth() {
        return header.length;
    }
    
    public int getHeight() {
        return data.length;
    }
}
