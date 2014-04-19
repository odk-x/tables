package org.opendatakit.tables.data;

/**
 * Simple conversion utilities to intepret stored values.
 *
 * @author mitchellsundt@gmail.com
 *
 */
public class DataHelper {

  public static boolean intToBool(int i) {
    return i != 0;
  }

  public static int boolToInt(boolean b) {
    return b ? 1 : 0;
  }

  public static boolean stringToBool(String bool) {
    return (bool == null) ? true : bool.equalsIgnoreCase("true");
  }

}
