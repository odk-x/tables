package org.opendatakit.tables.views.components;

/**
 * Represents a column that is in conflict--i.e. the contents differ between
 * the server and local versions.
 *
 * @author sudar.sam@gmail.com
 *
 */
public final class ConflictColumn {
  private final int position;
  private final String elementKey;
  private final String localRawValue;
  private final String localDisplayValue;
  private final String serverRawValue;
  private final String serverDisplayValue;

  public ConflictColumn(int position, String elementKey, String localRawValue,
      String localDisplayValue, String serverRawValue, String serverDisplayValue) {
    this.position = position;
    this.elementKey = elementKey;
    this.localRawValue = localRawValue;
    this.localDisplayValue = localDisplayValue;
    this.serverRawValue = serverRawValue;
    this.serverDisplayValue = serverDisplayValue;
  }

  public int getPosition() {
    return this.position;
  }

  public String getElementKey() {
    return this.elementKey;
  }

  public String getLocalRawValue() {
    return this.localRawValue;
  }

  public String getServerRawValue() {
    return this.serverRawValue;
  }

  public String getLocalDisplayValue() {
    return this.localDisplayValue;
  }

  public String getServerDisplayValue() {
    return this.serverDisplayValue;
  }

}