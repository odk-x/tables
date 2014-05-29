package org.opendatakit.tables.views;

public class CellInfo {
  // elementKey may be null if we ever need a touch
  // listener on status column. For now, everything works.

  public final String elementKey;
  public final int rowId;
  // this is ONLY relevant to this TabularView
  final int colPos;

  public CellInfo(String elementKey, int colPos, int rowId) {
    this.elementKey = elementKey;
    this.colPos = colPos;
    this.rowId = rowId;
  }
}