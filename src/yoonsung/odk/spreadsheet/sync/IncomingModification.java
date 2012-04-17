package yoonsung.odk.spreadsheet.sync;

import java.util.ArrayList;
import java.util.List;

/**
 * An IncomingModification represents changes coming down from the server.
 * 
 * @author the.dylan.price@gmail.com
 * 
 */
public class IncomingModification {
  List<SyncRow> rows;
  String tableSyncTag;

  public IncomingModification() {
    this.rows = new ArrayList<SyncRow>();
    this.tableSyncTag = null;
  }

  /**
   * Create a new IncomingModification.
   * 
   * @param rows
   *          a list of rows that represent the changes in the server's state
   *          since the last synchronization
   * @param tableSyncTag
   *          the latest synchronization tag
   */
  public IncomingModification(final List<SyncRow> rows, final String tableSyncTag) {
    this.rows = rows;
    this.tableSyncTag = tableSyncTag;
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
  public String getTableSyncTag() {
    return this.tableSyncTag;
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
  public void setTableSyncTag(final String tableSyncTag) {
    this.tableSyncTag = tableSyncTag;
  }

  @Override
  public boolean equals(final java.lang.Object o) {
    if (o == this)
      return true;
    if (!(o instanceof IncomingModification))
      return false;
    final IncomingModification other = (IncomingModification) o;
    if (!other.canEqual((java.lang.Object) this))
      return false;
    if (this.getRows() == null ? other.getRows() != null : !this.getRows().equals(
        (java.lang.Object) other.getRows()))
      return false;
    if (this.getTableSyncTag() == null ? other.getTableSyncTag() != null : !this.getTableSyncTag()
        .equals((java.lang.Object) other.getTableSyncTag()))
      return false;
    return true;
  }

  public boolean canEqual(final java.lang.Object other) {
    return other instanceof IncomingModification;
  }

  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = result * PRIME + (this.getRows() == null ? 0 : this.getRows().hashCode());
    result = result * PRIME
        + (this.getTableSyncTag() == null ? 0 : this.getTableSyncTag().hashCode());
    return result;
  }

  @Override
  public String toString() {
    return "IncomingModification(rows=" + this.getRows() + ", tableSyncTag="
        + this.getTableSyncTag() + ")";
  }
}