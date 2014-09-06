package org.opendatakit.common.android.data;

import android.content.Context;

/**
 * Provided purely so that the mockito library can Mock it.
 * 
 * @author mitchellsundt@gmail.com
 *
 */
public class TablePropertiesFactory {
  
  public TableProperties getTablePropertiesForTable(Context context, String appName, String tableId) {
    return TableProperties.getTablePropertiesForTable(context, appName, tableId);
  }
}
