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
package org.opendatakit.tables.utils;

/**
 * Associates a table id with its name.
 * 
 * @author sudar.sam@gmail.com
 *
 */
public class TableNameStruct {

  private String mTableId;
  private String mLocalizedDisplayName;
  
  public TableNameStruct(String tableId, String localizedDisplayName) {
    this.mTableId = tableId;
    this.mLocalizedDisplayName = localizedDisplayName;
  }
  
  public String getTableId() {
    return this.mTableId;
  }
  
  public String getLocalizedDisplayName() {
    return this.mLocalizedDisplayName;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((mLocalizedDisplayName == null) ? 0 : mLocalizedDisplayName.hashCode());
    result = prime * result + ((mTableId == null) ? 0 : mTableId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TableNameStruct other = (TableNameStruct) obj;
    if (mLocalizedDisplayName == null) {
      if (other.mLocalizedDisplayName != null)
        return false;
    } else if (!mLocalizedDisplayName.equals(other.mLocalizedDisplayName))
      return false;
    if (mTableId == null) {
      if (other.mTableId != null)
        return false;
    } else if (!mTableId.equals(other.mTableId))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "TableNameStruct [mTableId=" + mTableId + ", mLocalizedDisplayName="
        + mLocalizedDisplayName + "]";
  }
  
  

}
