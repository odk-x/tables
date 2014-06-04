package org.opendatakit.tables.views.components;

/**
 * Represents a column that is not in conflict--i.e. one that has the same
 * value locally and on the server.
 *
 * @author sudar.sam@gmail.com
 *
 */
public final class ConcordantColumn {
  private final int position;
  private final String displayValue;

  public ConcordantColumn(int position, String displayValue) {
    this.position = position;
    this.displayValue = displayValue;
  }

  public int getPosition() {
    return this.position;
  }

  public String getDisplayValue() {
    return this.displayValue;
  }
}