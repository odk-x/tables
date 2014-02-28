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
package org.opendatakit.tables.sync;

import org.opendatakit.tables.sync.aggregate.SyncTag;

/**
 * A RowModification represents the update to a row's ETag and the dataset's
 * sync tag after the server update completes.
 *
 * @author mitchellsundt@gmail.com
 *
 */
public class RowModification {
  String rowId;
  String rowETag;
  SyncTag tableSyncTag;

  /**
   * Create a new Modification.
   *
   * @param rowId
   *          rowId that was being modified
   * @param rowETag
   *          updated rowETag from server
   * @param tableSyncTag
   *          the table-level syncTag
   */
  public RowModification(final String rowId, final String rowETag, final SyncTag tableSyncTag) {
    this.rowId = rowId;
    this.rowETag = rowETag;
    this.tableSyncTag = tableSyncTag;
  }

  public RowModification() {
  }

  /**
   *
   * @return the rowId
   */
  public String getRowId() {
    return this.rowId;
  }

  /**
   *
   * @return the rowETag
   */
  public String getRowETag() {
    return this.rowETag;
  }

  /**
   *
   * @return the table-level syncTag
   */
  public SyncTag getTableSyncTag() {
    return this.tableSyncTag;
  }

  /**
   *
   * @param tableSyncTag
   *          the table-level syncTag
   */
  public void setTableSyncTag(final SyncTag tableSyncTag) {
    this.tableSyncTag = tableSyncTag;
  }

  @Override
  public boolean equals(final java.lang.Object o) {
    if (o == this)
      return true;
    if (!(o instanceof RowModification))
      return false;
    final RowModification other = (RowModification) o;
    if (!other.canEqual((java.lang.Object) this))
      return false;
    if (this.getRowId() == null ? other.getRowId() != null : !this.getRowId().equals(other.getRowId()))
      return false;
    if (this.getRowETag() == null ? other.getRowETag() != null : !this.getRowETag().equals(other.getRowETag()))
      return false;
    if (this.getTableSyncTag() == null ? other.getTableSyncTag() != null : !this.getTableSyncTag()
        .equals((java.lang.Object) other.getTableSyncTag()))
      return false;
    return true;
  }

  public boolean canEqual(final java.lang.Object other) {
    return other instanceof RowModification;
  }

  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = result * PRIME + (this.getRowId() == null ? 0 : this.getRowId().hashCode());
    result = result * PRIME + (this.getRowETag() == null ? 0 : this.getRowETag().hashCode());
    result = result * PRIME
        + (this.getTableSyncTag() == null ? 0 : this.getTableSyncTag().hashCode());
    return result;
  }

  @Override
  public java.lang.String toString() {
    return "RowModification(rowId=" + this.getRowId() + ", rowETag=" + this.getRowETag() + ", tableSyncTag="
        + this.getTableSyncTag() + ")";
  }
}