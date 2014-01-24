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

import java.util.Map;

/**
 * A SyncRow is an in-between class to map rows in the database to rows in the
 * cloud.
 *
 * @author the.dylan.price@gmail.com
 *
 */
public class SyncRow {
  private String rowId;
  private String syncTag;

  /**
   * Sync status field.
   */
  private boolean deleted;

  /**
   * OdkTables metadata column.
   */
  private String uriAccessControl;

  /**
   * OdkTables metadata column.
   */
  private String formId;

  /**
   * OdkTables metadata column.
   */
  private String locale;

  /**
   * OdkTables metadata column.
   */
  private Long savepointTimestamp;

  private Map<String, String> values;

  public SyncRow(final String rowId, final String syncTag, final boolean deleted,
      final String uriAccessControl, final String formId, final String locale,
      final Long savepointTimestamp, final Map<String, String> values) {
    this.rowId = rowId;
    this.syncTag = syncTag;
    this.deleted = deleted;
    this.uriAccessControl = uriAccessControl;
    this.formId = formId;
    this.locale = locale;
    this.savepointTimestamp = savepointTimestamp;
    this.values = values;
  }

  public String getRowId() {
    return this.rowId;
  }

  public void setRowId(final String rowId) {
    this.rowId = rowId;
  }

  public String getSyncTag() {
    return this.syncTag;
  }

  public void setSyncTag(final String syncTag) {
    this.syncTag = syncTag;
  }

  public boolean isDeleted() {
    return this.deleted;
  }

  public void setDeleted(final boolean deleted) {
    this.deleted = deleted;
  }

  public String getUriAccessControl() {
    return uriAccessControl;
  }

  public void setUriAccessControl(String uriAccessControl) {
    this.uriAccessControl = uriAccessControl;
  }

  public String getFormId() {
    return formId;
  }

  public void setFormId(String formId) {
    this.formId = formId;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public Long getSavepointTimestamp() {
    return savepointTimestamp;
  }

  public void setSavepointTimestamp(Long savepointTimestamp) {
    this.savepointTimestamp = savepointTimestamp;
  }

  public Map<String, String> getValues() {
    return this.values;
  }

  public void setValues(final Map<String, String> values) {
    this.values = values;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object o) {
    if (o == this)
      return true;
    if (!(o instanceof SyncRow))
      return false;
    final SyncRow other = (SyncRow) o;
    if (!other.canEqual((java.lang.Object) this))
      return false;
    // primary key
    if (this.getRowId() == null ? other.getRowId() != null : !this.getRowId().equals(other.getRowId()))
      return false;

    // sync status
    if (this.getSyncTag() == null ? other.getSyncTag() != null : !this.getSyncTag().equals(other.getSyncTag()))
      return false;
    if (this.isDeleted() != other.isDeleted())
      return false;

    // sync'd metadata
    if (this.getUriAccessControl() == null ? other.getUriAccessControl() != null : !this.getUriAccessControl().equals(other.getUriAccessControl()))
      return false;
    if (this.getFormId() == null ? other.getFormId() != null : !this.getFormId().equals(other.getFormId()))
      return false;
    if (this.getLocale() == null ? other.getLocale() != null : !this.getLocale().equals(other.getLocale()))
      return false;
    if (this.getSavepointTimestamp() == null ? other.getSavepointTimestamp() != null : !this.getSavepointTimestamp().equals(other.getSavepointTimestamp()))
      return false;

    // data
    if (this.getValues() == null ? other.getValues() != null : !this.getValues().equals(
        (java.lang.Object) other.getValues()))
      return false;
    return true;
  }

  public boolean canEqual(final java.lang.Object other) {
    return other instanceof SyncRow;
  }

  @java.lang.Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    // primary key
    result = result * PRIME + (this.getRowId() == null ? 0 : this.getRowId().hashCode());
    // sync status
    result = result * PRIME + (this.getSyncTag() == null ? 0 : this.getSyncTag().hashCode());
    result = result * PRIME + (this.isDeleted() ? 1231 : 1237);
    // sync'd metadata
    result = result * PRIME + (this.getUriAccessControl() == null ? 0 : this.getUriAccessControl().hashCode());
    result = result * PRIME + (this.getFormId() == null ? 0 : this.getFormId().hashCode());
    result = result * PRIME + (this.getLocale() == null ? 0 : this.getLocale().hashCode());
    result = result * PRIME + (this.getSavepointTimestamp() == null ? 0 : this.getSavepointTimestamp().hashCode());
    // data
    result = result * PRIME + (this.getValues() == null ? 0 : this.getValues().hashCode());
    return result;
  }

  @java.lang.Override
  public java.lang.String toString() {
    return "SyncRow[rowId=" + this.getRowId() + ", syncTag=" + this.getSyncTag()
        + ", deleted="+ this.isDeleted()
        + ", uriAccessControl=" + this.getUriAccessControl()
        + ", formId=" + this.getFormId()
        + ", locale=" + this.getLocale()
        + ", savepointTimestamp=" + this.getSavepointTimestamp()
        + ", values=" + this.getValues()
        + "[";
  }
}