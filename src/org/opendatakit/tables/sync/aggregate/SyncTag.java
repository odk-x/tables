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

  private String dataEtag;
  private String propertiesEtag;
  private String schemaEtag;

  public SyncTag(String dataEtag, String propertiesEtag, String schemaEtag) {
    // The data etag can be null if the table has been created but there's not
    // yet any data on the server.
    if (dataEtag == null || dataEtag.equals("")) {
      this.dataEtag = EMPTY_ETAG;
    } else {
      this.dataEtag = dataEtag;
    }
    if (propertiesEtag.equals("")) {
      this.propertiesEtag = EMPTY_ETAG;
    } else {
      this.propertiesEtag = propertiesEtag;
    }
    if (schemaEtag.equals("")) {
      this.schemaEtag = EMPTY_ETAG;
    } else {
      this.schemaEtag = schemaEtag;
    }
  }

  public String getDataEtag() {
    return (dataEtag == null) ? STR_NULL : dataEtag;
  }

  public String getPropertiesEtag() {
    return (propertiesEtag == null) ? STR_NULL : propertiesEtag;
  }

  public String getSchemaEtag() {
    return (schemaEtag == null) ? STR_NULL : schemaEtag;
  }

  public void setSchemaEtag(String schemaEtag) {
    this.schemaEtag = schemaEtag;
  }

  public void setPropertiesEtag(String propertiesEtag) {
    this.propertiesEtag = propertiesEtag;
  }

  public void setDataEtag(String dataEtag) {
    this.dataEtag = dataEtag;
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
    result = prime * result + dataEtag.hashCode();
    result = prime * result + propertiesEtag.hashCode();
    result = prime * result + schemaEtag.hashCode();
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
    boolean sameDataTag =  dataEtag == null ?
        other.dataEtag == null : dataEtag.equals(other.dataEtag);
    boolean samePropertiesTag = propertiesEtag == null ?
        other.propertiesEtag == null :
          propertiesEtag.equals(other.propertiesEtag);
    boolean sameSchemaTag = schemaEtag == null ?
        other.schemaEtag == null :
          schemaEtag.equals(other.schemaEtag);
    return sameDataTag && samePropertiesTag && sameSchemaTag;
  }

  @Override
  public String toString() {
    return String.format("%s%s%s%s%s", dataEtag, DELIM, propertiesEtag, DELIM, schemaEtag);
  }

  public static SyncTag valueOf(String syncTag) {
    String[] tokens = syncTag.split(DELIM);
    if (tokens.length != 3)
      throw new IllegalArgumentException("Malformed syncTag: " + syncTag);

    String dataEtag = tokens[0];
    String propertiesEtag = tokens[1];
    String schemaEtag = tokens[2];
    return new SyncTag(dataEtag, propertiesEtag, schemaEtag);
  }
}
