package org.opendatakit.tables.fragments;

public class SpreadsheetFragmentStub extends SpreadsheetFragment {

  public static final String DEFAULT_INDEXED_COLUMN = null;
  public static String INDEXED_COLUMN = DEFAULT_INDEXED_COLUMN;

  public static final int DEFAULT_INDEXED_COLUMN_OFFSET = -1;
  public static int INDEXED_COLUMN_OFFSET = DEFAULT_INDEXED_COLUMN_OFFSET;

  public static void resetState() {
    INDEXED_COLUMN = DEFAULT_INDEXED_COLUMN;
    INDEXED_COLUMN_OFFSET = DEFAULT_INDEXED_COLUMN_OFFSET;
  }

}
