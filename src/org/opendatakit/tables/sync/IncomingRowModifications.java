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
package org.opendatakit.tables.sync;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.tables.sync.aggregate.SyncTag;

/**
 * An IncomingModification represents changes coming down from the server.
 *
 * @author the.dylan.price@gmail.com
 * @author sudar.sam@gmail.com
 * @author mitchellsundt@gmail.com
 *
 */
public class IncomingRowModifications {

  // tableRowsChanged iff !rows.isEmpty();
  private List<SyncRow> rows;

  private SyncTag tableSyncTag;

  public IncomingRowModifications() {
    this.rows = new ArrayList<SyncRow>();
    this.tableSyncTag = new SyncTag(null,null,null);
  }

  /**
   *
   * @return a list of rows that represent the changes in the server's state
   *         since the last synchronization
   */
  public List<SyncRow> getRows() {
    return this.rows;
  }

  /**
   *
   * @return the latest synchronization tag
   */
  public SyncTag getTableSyncTag() {
    return this.tableSyncTag;
  }

  /**
   * @return true if the server has row changes.
   */
  public boolean hasTableDataChanged() {
    return rows.size() != 0;
  }

  /**
   *
   * @param rows
   *          a list of rows that represent the changes in the server's state
   *          since the last synchronization
   */
  public void setRows(final List<SyncRow> rows) {
    this.rows = rows;
  }

  /**
   *
   * @param tableSyncTag
   *          the latest synchronization tag
   */
  public void setTableSyncTag(final SyncTag tableSyncTag) {
    this.tableSyncTag = tableSyncTag;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof IncomingRowModifications))
      return false;
    IncomingRowModifications other = (IncomingRowModifications) obj;
    if (rows == null) {
      if (other.rows != null)
        return false;
    } else if (!rows.equals(other.rows))
      return false;
    if (tableSyncTag == null) {
      if (other.tableSyncTag != null)
        return false;
    } else if (!tableSyncTag.equals(other.tableSyncTag))
      return false;
    return true;
  }

  public boolean canEqual(final java.lang.Object other) {
    return other instanceof IncomingRowModifications;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((rows == null) ? 0 : rows.hashCode());
    result = prime * result + ((tableSyncTag == null) ? 0 : tableSyncTag.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return "IncomingModification [rows=" + rows
        + ", tableSyncTag=" + tableSyncTag
        + "]";
  }
}