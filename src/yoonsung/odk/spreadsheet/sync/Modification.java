package yoonsung.odk.spreadsheet.sync;

import java.util.Map;

/**
 * A Modification represents a set of synchronization tags to save in the
 * database after updates have been made in the server.
 * 
 * @author the.dylan.price@gmail.com
 * 
 */
public class Modification {
  Map<String, String> syncTags;
  String tableSyncTag;

  /**
   * Create a new Modification.
   * 
   * @param syncTags
   *          a map from rowIds to syncTags
   * @param tableSyncTag
   *          the table-level syncTag
   */
  public Modification(final Map<String, String> syncTags, final String tableSyncTag) {
    this.syncTags = syncTags;
    this.tableSyncTag = tableSyncTag;
  }

  public Modification() {
  }

  /**
   * 
   * @return a map from rowIds to syncTags
   */
  public Map<String, String> getSyncTags() {
    return this.syncTags;
  }

  /**
   * 
   * @return the table-level syncTag
   */
  public String getTableSyncTag() {
    return this.tableSyncTag;
  }

  /**
   * 
   * @param syncTags
   *          a map from rowIds to syncTags
   */
  public void setSyncTags(final Map<String, String> syncTags) {
    this.syncTags = syncTags;
  }

  /**
   * 
   * @param tableSyncTag
   *          the table-level syncTag
   */
  public void setTableSyncTag(final String tableSyncTag) {
    this.tableSyncTag = tableSyncTag;
  }

  @Override
  public boolean equals(final java.lang.Object o) {
    if (o == this)
      return true;
    if (!(o instanceof Modification))
      return false;
    final Modification other = (Modification) o;
    if (!other.canEqual((java.lang.Object) this))
      return false;
    if (this.getSyncTags() == null ? other.getSyncTags() != null : !this.getSyncTags().equals(
        (java.lang.Object) other.getSyncTags()))
      return false;
    if (this.getTableSyncTag() == null ? other.getTableSyncTag() != null : !this.getTableSyncTag()
        .equals((java.lang.Object) other.getTableSyncTag()))
      return false;
    return true;
  }

  public boolean canEqual(final java.lang.Object other) {
    return other instanceof Modification;
  }

  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = result * PRIME + (this.getSyncTags() == null ? 0 : this.getSyncTags().hashCode());
    result = result * PRIME
        + (this.getTableSyncTag() == null ? 0 : this.getTableSyncTag().hashCode());
    return result;
  }

  @Override
  public java.lang.String toString() {
    return "Modification(syncTags=" + this.getSyncTags() + ", tableSyncTag="
        + this.getTableSyncTag() + ")";
  }
}