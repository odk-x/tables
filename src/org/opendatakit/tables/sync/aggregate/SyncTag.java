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

  public SyncTag(String dataEtag, String propertiesEtag) {
    // SS: adding cases for if the passed in tags are null. not sure which
    // is correct, going to try -1.
    if (dataEtag.equals("")) {
      this.dataEtag = EMPTY_ETAG;
    } else {
      this.dataEtag = dataEtag;
    }
    if (propertiesEtag.equals("")) {
      this.propertiesEtag = EMPTY_ETAG;
    } else {
      this.propertiesEtag = propertiesEtag;
    }
  }

  public String getDataEtag() {
    return (dataEtag == null) ? STR_NULL : dataEtag;
  }

  public String getPropertiesEtag() {
    return (propertiesEtag == null) ? STR_NULL : propertiesEtag;
  }

  /**
   * Sets the dataEtag to the current system time in millis.
   */
  public void incrementDataEtag() {
    Long currentMillis = System.currentTimeMillis();
    this.dataEtag = Long.toString(currentMillis);
  }

  /**
   * Sets the dataEtag to the current system time in millis.
   */
  public void incrementPropertiesEtag() {
    Long currentMillis = System.currentTimeMillis();
    this.propertiesEtag = Long.toString(currentMillis);
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
    return sameDataTag && samePropertiesTag;
  }

  @Override
  public String toString() {
    return String.format("%s%s%s", dataEtag, DELIM, propertiesEtag);
  }

  public static SyncTag valueOf(String syncTag) {
    String[] tokens = syncTag.split(DELIM);
    if (tokens.length != 2)
      throw new IllegalArgumentException("Malformed syncTag: " + syncTag);

    String dataEtag = tokens[0];
    String propertiesEtag = tokens[1];
    return new SyncTag(dataEtag, propertiesEtag);
  }
}
