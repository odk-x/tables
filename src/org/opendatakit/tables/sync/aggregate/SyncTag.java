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

  private static final String DELIM = "::";

  private int dataEtag;
  private int propertiesEtag;

  public SyncTag(String dataEtag, String propertiesEtag) {
    this.dataEtag = Integer.parseInt(dataEtag);
    this.propertiesEtag = Integer.parseInt(propertiesEtag);
  }

  public String getDataEtag() {
    return String.valueOf(dataEtag);
  }

  public String getPropertiesEtag() {
    return String.valueOf(propertiesEtag);
  }

  public void incrementDataEtag() {
    this.dataEtag++;
  }

  public void incrementPropertiesEtag() {
    this.propertiesEtag++;
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
    result = prime * result + dataEtag;
    result = prime * result + propertiesEtag;
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof SyncTag))
      return false;
    SyncTag other = (SyncTag) obj;
    if (dataEtag != other.dataEtag)
      return false;
    if (propertiesEtag != other.propertiesEtag)
      return false;
    return true;
  }

  @Override
  public String toString() {
    return String.format("%d%s%d", dataEtag, DELIM, propertiesEtag);
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
