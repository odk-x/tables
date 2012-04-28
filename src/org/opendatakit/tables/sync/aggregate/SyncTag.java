package org.opendatakit.tables.sync.aggregate;


public class SyncTag {

  private static final String DELIM = "::";

  private String dataEtag;
  private String propertiesEtag;

  public SyncTag(String dataEtag, String propertiesEtag) {
    this.dataEtag = dataEtag;
    this.propertiesEtag = propertiesEtag;
  }

  public String getDataEtag() {
    return dataEtag;
  }

  public void setDataEtag(String dataEtag) {
    this.dataEtag = dataEtag;
  }

  public String getPropertiesEtag() {
    return propertiesEtag;
  }

  public void setPropertiesEtag(String propertiesEtag) {
    this.propertiesEtag = propertiesEtag;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((dataEtag == null) ? 0 : dataEtag.hashCode());
    result = prime * result + ((propertiesEtag == null) ? 0 : propertiesEtag.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof SyncTag))
      return false;
    SyncTag other = (SyncTag) obj;
    if (dataEtag == null) {
      if (other.dataEtag != null)
        return false;
    } else if (!dataEtag.equals(other.dataEtag))
      return false;
    if (propertiesEtag == null) {
      if (other.propertiesEtag != null)
        return false;
    } else if (!propertiesEtag.equals(other.propertiesEtag))
      return false;
    return true;
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
