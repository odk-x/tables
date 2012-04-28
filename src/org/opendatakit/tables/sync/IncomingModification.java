package org.opendatakit.tables.sync;

import java.util.ArrayList;
import java.util.List;

/**
 * An IncomingModification represents changes coming down from the server.
 * 
 * @author the.dylan.price@gmail.com
 * 
 */
public class IncomingModification {
  private List<SyncRow> rows;
  private boolean tablePropertiesChanged;
  private String tableProperties;
  private String tableSyncTag;

  public IncomingModification() {
    this.rows = new ArrayList<SyncRow>();
    this.tableProperties = null;
    this.tablePropertiesChanged = false;
    this.tableSyncTag = null;
  }

  /**
   * Create a new IncomingModification.
   * 
   * @param rows
   *          a list of rows that represent the changes in the server's state
   *          since the last synchronization
   * @param tablePropertiesChanged
   *          true if the table properties have changed since the last
   *          synchronization
   * @param tableProperties
   *          if tablePropertiesChanged is true, then this should be the new
   *          table properties. Otherwise it will be ignored and may be null.
   * @param tableSyncTag
   *          the latest synchronization tag
   */
  public IncomingModification(List<SyncRow> rows, boolean tablePropertiesChanged,
      String tableProperties, String tableSyncTag) {
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
   * @return true if the table properties have changed since the last
   *         synchronization
   */
  public boolean hasTablePropertiesChanged() {
    return tablePropertiesChanged;
  }

  /**
   * @return the new table properties if {@link #hasTablePropertiesChanged()} is
   *         true. Otherwise the value is undefined and means nothing.
   */
  public String getTableProperties() {
    return tableProperties;
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

  /**
   * @param tablePropertiesChanged
   *          true if the table properties have changed since the last
   *          synchronization
   */
  public void setTablePropertiesChanged(boolean tablePropertiesChanged) {
    this.tablePropertiesChanged = tablePropertiesChanged;
  }

  /**
   * @param tableProperties
   *          the new table properties if the table properties have changed
   *          since the last synchronization
   */
  public void setTableProperties(String tableProperties) {
    this.tableProperties = tableProperties;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof IncomingModification))
      return false;
    IncomingModification other = (IncomingModification) obj;
    if (rows == null) {
      if (other.rows != null)
        return false;
    } else if (!rows.equals(other.rows))
      return false;
    if (tableProperties == null) {
      if (other.tableProperties != null)
        return false;
    } else if (!tableProperties.equals(other.tableProperties))
      return false;
    if (tablePropertiesChanged != other.tablePropertiesChanged)
      return false;
    if (tableSyncTag == null) {
      if (other.tableSyncTag != null)
        return false;
    } else if (!tableSyncTag.equals(other.tableSyncTag))
      return false;
    return true;
  }

  public boolean canEqual(final java.lang.Object other) {
    return other instanceof IncomingModification;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((rows == null) ? 0 : rows.hashCode());
    result = prime * result + ((tableProperties == null) ? 0 : tableProperties.hashCode());
    result = prime * result + (tablePropertiesChanged ? 1231 : 1237);
    result = prime * result + ((tableSyncTag == null) ? 0 : tableSyncTag.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return "IncomingModification [rows=" + rows + ", tablePropertiesChanged="
        + tablePropertiesChanged + ", tableProperties=" + tableProperties + ", tableSyncTag="
        + tableSyncTag + "]";
  }
}