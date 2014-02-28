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

import org.opendatakit.aggregate.odktables.rest.entity.PropertiesResource;
import org.opendatakit.aggregate.odktables.rest.entity.TableDefinitionResource;
import org.opendatakit.tables.sync.aggregate.SyncTag;

/**
 * An IncomingModification represents changes coming down from the server.
 *
 * @author the.dylan.price@gmail.com
 * @author sudar.sam@gmail.com
 *
 */
public class IncomingModification {
  private List<SyncRow> rows;

  /*
   * The two resource objects are XML representations of the eponymous objects
   * from the server. TableDefinitionResource holds the information about the
   * actual definition of the datastructure--a composite of the data in
   * the phone's TableDefinitions and ColumnDefinitions tables.
   * TablePropertiesResource holds all the key values from the key value store.
   */
  private boolean tableSchemaChanged;
  private TableDefinitionResource tableDefinitionRes;

  private boolean tablePropertiesChanged;
  private PropertiesResource tablePropertiesRes;

  private SyncTag tableSyncTag;

  public IncomingModification() {
    this.rows = new ArrayList<SyncRow>();
    tableDefinitionRes = null;
    tablePropertiesRes = null;
    this.tableSchemaChanged = false;
    this.tablePropertiesChanged = false;
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
   * @return true if the table properties have changed since the last
   *         synchronization
   */
  public boolean hasTableSchemaChanged() {
    return tableSchemaChanged;
  }

  /**
   * Return the {@link TableDefinitionResource} holding the changes to the
   * datastructure of the table if {@link #hasTableSchemaChanged()} is
   * true. As with {@link #getTableProperties()},
   * if {@link IncomingModification#hasTableSchemaChanged()} is false, the
   * value is undefined and means nothing.
   * @return
   */
  public TableDefinitionResource getTableDefinitionResource() {
    return tableDefinitionRes;
  }

  /**
   * @return true if the table properties have changed since the last
   *         synchronization
   */
  public boolean hasTablePropertiesChanged() {
    return tablePropertiesChanged;
  }

  /**
   * @return true if the server has row changes.
   */
  public boolean hasTableDataChanged() {
    return rows.size() != 0;
  }

  /**
   * @return the new table properties if {@link #hasTablePropertiesChanged()} is
   *         true. Otherwise the value is undefined and means nothing.
   */
  public PropertiesResource getTableProperties() {
    return tablePropertiesRes;
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
  public void setTableProperties(PropertiesResource tableProperties) {
    this.tablePropertiesRes = tableProperties;
  }

  /**
   * @param tablePropertiesChanged
   *          true if the table properties have changed since the last
   *          synchronization
   */
  public void setTableSchemaChanged(boolean tableSchemaChanged) {
    this.tableSchemaChanged = tableSchemaChanged;
  }

  /**
   * @param definition
   *     the new table definition if the table properties have changed since
   *     the last synchronization.
   */
  public void setTableDefinitionResource(TableDefinitionResource definition) {
    this.tableDefinitionRes = definition;
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
    if (tableDefinitionRes == null) {
      if (other.tableDefinitionRes != null)
        return false;
    } else if (!tableDefinitionRes.equals(other.tableDefinitionRes))
      return false;
    if (tableSchemaChanged != other.tableSchemaChanged)
      return false;
    if (tablePropertiesRes == null) {
      if (other.tablePropertiesRes != null) {
        return false;
      } else if (!tablePropertiesRes.equals(other.tablePropertiesRes))
        return false;
    }
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
    result = prime * result + ((tableDefinitionRes == null) ? 0 : tableDefinitionRes.hashCode());
    result = prime * result + ((tablePropertiesRes == null) ? 0 : tablePropertiesRes.hashCode());
    result = prime * result + (tableSchemaChanged ? 1231 : 1237);
    result = prime * result + (tablePropertiesChanged ? 1231 : 1237);
    result = prime * result + ((tableSyncTag == null) ? 0 : tableSyncTag.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return "IncomingModification [rows=" + rows
        + ", tableSchemaChanged=" + tableSchemaChanged
        + ", tableDefinitionResource=" + tableDefinitionRes.toString()
        + ", tablePropertiesChanged=" + tablePropertiesChanged
        + ", tablePropertiesResource=" + tablePropertiesRes.toString()
        + ", tableSyncTag=" + tableSyncTag
        + "]";
  }
}