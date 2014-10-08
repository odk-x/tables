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
