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
package org.opendatakit.tables.sync.aggregate;

public class SyncTag {

  private static final String STR_NULL = "null";

private static final String DELIM = "::";

  /**
   * This is the value that is replacing -1 as the value of the empty string
   * in the sync tag.
   */
  public static final String EMPTY_ETAG = "NONE";

  private String dataETag;
  private String propertiesETag;
  private String schemaETag;

  public SyncTag(String dataETag, String propertiesETag, String schemaETag) {
    // The data ETag can be null if the table has been created but there's not
    // yet any data on the server.
    if (dataETag == null || dataETag.equals("")) {
      this.dataETag = EMPTY_ETAG;
    } else {
      this.dataETag = dataETag;
    }
    if (propertiesETag == null || propertiesETag.equals("")) {
      this.propertiesETag = EMPTY_ETAG;
    } else {
      this.propertiesETag = propertiesETag;
    }
    if (schemaETag == null || schemaETag.equals("")) {
      this.schemaETag = EMPTY_ETAG;
    } else {
      this.schemaETag = schemaETag;
    }
  }

  public String getDataETag() {
    return (dataETag == null) ? STR_NULL : dataETag;
  }

  public String getPropertiesETag() {
    return (propertiesETag == null) ? STR_NULL : propertiesETag;
  }

  public String getSchemaETag() {
    return (schemaETag == null) ? STR_NULL : schemaETag;
  }

  public void setSchemaETag(String schemaETag) {
    this.schemaETag = schemaETag;
  }

  public void setPropertiesETag(String propertiesETag) {
    this.propertiesETag = propertiesETag;
  }

  public void setDataETag(String dataETag) {
    this.dataETag = dataETag;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + dataETag.hashCode();
    result = prime * result + propertiesETag.hashCode();
    result = prime * result + schemaETag.hashCode();
    return result;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SyncTag))
      return false;
    SyncTag other = (SyncTag) obj;
    boolean sameDataTag =  dataETag == null ?
        other.dataETag == null : dataETag.equals(other.dataETag);
    boolean samePropertiesTag = propertiesETag == null ?
        other.propertiesETag == null :
          propertiesETag.equals(other.propertiesETag);
    boolean sameSchemaTag = schemaETag == null ?
        other.schemaETag == null :
          schemaETag.equals(other.schemaETag);
    return sameDataTag && samePropertiesTag && sameSchemaTag;
  }

  @Override
  public String toString() {
    return String.format("%s%s%s%s%s", dataETag, DELIM, propertiesETag, DELIM, schemaETag);
  }

  public static SyncTag valueOf(String syncTag) {
    String[] tokens = syncTag.split(DELIM);
    if (tokens.length != 3)
      throw new IllegalArgumentException("Malformed syncTag: " + syncTag);

    String dataETag = tokens[0];
    String propertiesETag = tokens[1];
    String schemaETag = tokens[2];
    return new SyncTag(dataETag, propertiesETag, schemaETag);
  }
}
