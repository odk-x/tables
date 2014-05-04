package org.opendatakit.tables.utils;

import org.opendatakit.tables.utils.Constants.IntentKeys;

import android.os.Bundle;

/**
 * 
 * @author sudar.sam@gmail.com
 *
 */
public class IntentUtil {
  
  /**
   * Retrieve a {@link SQLQueryStruct} from bundle. The various components
   * should be keyed to the SQL intent keys in {@link Constants.IntentKeys}.
   * @param bundle
   * @return
   */
  public static SQLQueryStruct getSQLQueryStructFromBundle(Bundle bundle) {
    String sqlWhereClause =
        bundle.getString(IntentKeys.SQL_WHERE);
    String[] sqlSelectionArgs = null;
    if (sqlWhereClause != null && sqlWhereClause.length() != 0) {
       sqlSelectionArgs = bundle.getStringArray(
          IntentKeys.SQL_SELECTION_ARGS);
    }
    String[] sqlGroupBy = bundle.getStringArray(IntentKeys.SQL_GROUP_BY_ARGS);
    String sqlHaving = null;
    if ( sqlGroupBy != null && sqlGroupBy.length != 0 ) {
      sqlHaving = bundle.getString(IntentKeys.SQL_HAVING);
    }
    String sqlOrderByElementKey = bundle.getString(
        IntentKeys.SQL_ORDER_BY_ELEMENT_KEY);
    String sqlOrderByDirection = null;
    if ( sqlOrderByElementKey != null && sqlOrderByElementKey.length() != 0 ) {
      sqlOrderByDirection = bundle.getString(
          IntentKeys.SQL_ORDER_BY_DIRECTION);
      if ( sqlOrderByDirection == null || sqlOrderByDirection.length() == 0 ) {
        sqlOrderByDirection = "ASC";
      }
    }
    SQLQueryStruct result = new SQLQueryStruct(
        sqlWhereClause,
        sqlSelectionArgs,
        sqlGroupBy,
        sqlHaving,
        sqlOrderByElementKey,
        sqlOrderByDirection);
    return result;
  }

}
