/*
 * Copyright (C) 2012-2014 University of Washington
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
package org.opendatakit.tables.utils;

import android.os.Bundle;
import org.opendatakit.consts.IntentConsts;
import org.opendatakit.data.ColorRuleGroup;
import org.opendatakit.database.queries.BindArgs;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.data.ViewFragmentType;
import org.opendatakit.tables.utils.Constants.IntentKeys;
import org.opendatakit.views.OdkData;

/**
 * @author sudar.sam@gmail.com
 */
public final class IntentUtil {

  /**
   * Do not instantiate this class
   */
  private IntentUtil() {
  }

  /**
   * Retrieve the file name from the saved instance state or from the
   * other bundle. Convenience method for calling
   * {@link #retrieveAppNameFromBundle(Bundle)} in the appropriate order,
   * respecting that savedInstanceState may be null.
   * <p>
   * If the file name is non-null in both bundles, savedInstanceState takes
   * precedent.
   *
   * @param savedInstanceState      The bundle used to get the filename first
   * @param argumentsOrIntentExtras the bundle from either the activity's
   *                                starting intent or the fragment's arguments.
   * @return the file name, or null if the value does not exist in either
   * bundle.
   */
  public static String retrieveFileNameFromSavedStateOrArguments(Bundle savedInstanceState,
      Bundle argumentsOrIntentExtras) {
    String result = null;
    if (savedInstanceState != null) {
      result = retrieveFileNameFromBundle(savedInstanceState);
    }
    if (result == null) {
      result = retrieveFileNameFromBundle(argumentsOrIntentExtras);
    }
    return result;
  }

  /**
   * Retrieve a {@link SQLQueryStruct} from bundle. The various components
   * should be keyed to the SQL intent keys in {@link Constants.IntentKeys}.
   *
   * @param bundle the bundle to try and pull the query params from
   * @return the sql query struct pulled from the bundle
   */
  public static SQLQueryStruct getSQLQueryStructFromBundle(Bundle bundle) {
    String sqlWhereClause = bundle.containsKey(OdkData.IntentKeys.SQL_WHERE) ?
        bundle.getString(OdkData.IntentKeys.SQL_WHERE) :
        null;

    BindArgs sqlBindArgs;
    {
      String sqlSelectionArgsString = null;
      if (sqlWhereClause != null && !sqlWhereClause.isEmpty()) {
        sqlSelectionArgsString = bundle.containsKey(OdkData.IntentKeys.SQL_SELECTION_ARGS) ?
            bundle.getString(OdkData.IntentKeys.SQL_SELECTION_ARGS) :
            null;
      }
      sqlBindArgs = new BindArgs(sqlSelectionArgsString);
    }

    String[] sqlGroupBy = bundle.containsKey(OdkData.IntentKeys.SQL_GROUP_BY_ARGS) ?
        bundle.getStringArray(OdkData.IntentKeys.SQL_GROUP_BY_ARGS) :
        null;
    String sqlHaving = null;
    if (sqlGroupBy != null && sqlGroupBy.length != 0) {
      sqlHaving = bundle.containsKey(OdkData.IntentKeys.SQL_HAVING) ?
          bundle.getString(OdkData.IntentKeys.SQL_HAVING) :
          null;
    }
    String sqlOrderByElementKey = bundle.containsKey(OdkData.IntentKeys.SQL_ORDER_BY_ELEMENT_KEY) ?
        bundle.getString(OdkData.IntentKeys.SQL_ORDER_BY_ELEMENT_KEY) :
        null;
    String sqlOrderByDirection = null;
    if (sqlOrderByElementKey != null && !sqlOrderByElementKey.isEmpty()) {
      sqlOrderByDirection = bundle.containsKey(OdkData.IntentKeys.SQL_ORDER_BY_DIRECTION) ?
          bundle.getString(OdkData.IntentKeys.SQL_ORDER_BY_DIRECTION) :
          null;
      if (sqlOrderByDirection == null || sqlOrderByDirection.isEmpty()) {
        sqlOrderByDirection = "ASC";
      }
    }
    return new SQLQueryStruct(sqlWhereClause, sqlBindArgs, sqlGroupBy, sqlHaving,
        sqlOrderByElementKey, sqlOrderByDirection);
  }

  /**
   * Thin wrapper for addSQLKeysToBundle
   *
   * @param bundle      the bundle to put the query parameters into
   * @param queryStruct the query parameters to put into the bundle
   */
  public static void addSQLQueryStructToBundle(Bundle bundle, SQLQueryStruct queryStruct) {
    IntentUtil.addSQLKeysToBundle(bundle, queryStruct.whereClause, queryStruct.selectionArgs,
        queryStruct.groupBy, queryStruct.having, queryStruct.orderByElementKey,
        queryStruct.orderByDirection);

  }

  /**
   * Return the file name from the bundle. Convenience method for calling
   * {@link Bundle#getString(String)} with
   * {@link OdkData.IntentKeys#FILE_NAME}.
   *
   * @param bundle the bundle to try and pull the filename from
   * @return the file name, null if it does not exist or if bundle is null
   */
  public static String retrieveFileNameFromBundle(Bundle bundle) {
    if (bundle == null) {
      return null;
    }
    return bundle.getString(OdkData.IntentKeys.FILE_NAME);
  }

  /**
   * Return the query type from the bundle. Convenience method for calling
   * {@link Bundle#getString(String)} with
   * {@link OdkData.IntentKeys#QUERY_TYPE}.
   *
   * @param bundle the bundle to try and pull the filename from
   * @return the query type, null if it does not exist or if bundle is null
   */
  public static String retrieveQueryTypeFromBundle(Bundle bundle) {
    if (bundle == null) {
      return null;
    }
    return bundle.getString(OdkData.IntentKeys.QUERY_TYPE);
  }

  /**
   * Return the sql command from the bundle. Convenience method for calling
   * {@link Bundle#getString(String)} with
   * {@link OdkData.IntentKeys#SQL_COMMAND}.
   *
   * @param bundle the bundle to try and pull the filename from
   * @return the sql command, null if it does not exist or if bundle is null
   */
  public static String retrieveSqlCommandFromBundle(Bundle bundle) {
    if (bundle == null) {
      return null;
    }
    return bundle.getString(OdkData.IntentKeys.SQL_COMMAND);
  }

  /**
   * Return the {@link ColorRuleGroup.Type} from the bundle. Convenience method
   * for calling {@link Bundle#get(String)} with
   * {@link OdkData.IntentKeys#COLOR_RULE_TYPE} and parsing the resultant
   * value.
   *
   * @param bundle a bundle to try and pull a color rule group type from
   * @return the color rule group type in the bundle if it exists, otherwise null
   */
  public static ColorRuleGroup.Type retrieveColorRuleTypeFromBundle(Bundle bundle) {
    if (bundle == null) {
      return null;
    }
    String typeStr = bundle.getString(OdkData.IntentKeys.COLOR_RULE_TYPE);
    if (typeStr == null) {
      return null;
    } else {
      return ColorRuleGroup.Type.valueOf(typeStr);
    }
  }

  /**
   * Return the table id from the bundle. Convenience method for calling
   * {@link Bundle#getString(String)} with
   * {@link IntentConsts#INTENT_KEY_TABLE_ID}.
   *
   * @param bundle the bundle to get the table id from
   * @return the table id, null if it does not exist or if bundle is null
   */
  public static String retrieveTableIdFromBundle(Bundle bundle) {
    if (bundle == null) {
      return null;
    }
    return bundle.getString(IntentConsts.INTENT_KEY_TABLE_ID);
  }

  /**
   * Return the fragment view type from the bundle.
   *
   * @param bundle the bundle to get the fragment view type from
   * @return the fragment view type, null if it does not exist or if the bundle is null
   */
  public static String retrieveFragmentViewTypeFromBundle(Bundle bundle) {
    if (bundle == null) {
      return null;
    }
    return bundle.getString(OdkData.IntentKeys.TABLE_DISPLAY_VIEW_TYPE);
  }

  /**
   * Return the app name from the bundle. Convenience method for calling
   * {@link Bundle#getString(String)} with
   * {@link IntentConsts#INTENT_KEY_APP_NAME}.
   *
   * @param bundle the bundle for getting an app name from
   * @return the app name, null if it does not exist or if bundle is null
   */
  public static String retrieveAppNameFromBundle(Bundle bundle) {
    if (bundle == null) {
      return null;
    }
    return bundle.getString(IntentConsts.INTENT_KEY_APP_NAME);
  }

  /**
   * Return the element key from the bundle. Convenience method for calling
   * {@link Bundle#getString(String)} with
   * {@link OdkData.IntentKeys#ELEMENT_KEY}.
   *
   * @param bundle the bundle to get the element key from
   * @return the element key, null if it does not exist or if the bundle is
   * null
   */
  public static String retrieveElementKeyFromBundle(Bundle bundle) {
    if (bundle == null) {
      return null;
    }
    return bundle.getString(OdkData.IntentKeys.ELEMENT_KEY);
  }

  /**
   * Return the row id from the bundle. Convenience method for calling
   * {@link Bundle#getString(String)} with
   * {@link IntentConsts#INTENT_KEY_INSTANCE_ID}.
   *
   * @param bundle the bundle to get a row id from
   * @return the row id, null if it does not exist or if bundle is null
   */
  public static String retrieveRowIdFromBundle(Bundle bundle) {
    if (bundle == null) {
      return null;
    }
    return bundle.getString(IntentConsts.INTENT_KEY_INSTANCE_ID);
  }

  /**
   * Return the default row id from the bundle. Convenience method for calling
   * {@link Bundle#getString(String)} with
   * {@link IntentConsts#INTENT_KEY_DEFAULT_ROW_ID}.
   *
   * @param bundle the bundle to get a row id from
   * @return the row id, null if it does not exist or if bundle is null
   */
  public static String retrieveDefaultRowIdFromBundle(Bundle bundle) {
    if (bundle == null) {
      return null;
    }
    return bundle.getString(IntentConsts.INTENT_KEY_DEFAULT_ROW_ID);
  }

  /**
   * Return the selection args from the bundle. Convenience method for calling
   * {@link Bundle#getString(String)} with
   * {@link OdkData.IntentKeys#SQL_SELECTION_ARGS}.
   *
   * @param bundle the bundle to get a row id from
   * @return the selection args, null if it does not exist or if bundle is null
   */
  public static BindArgs retrieveSelectionArgsFromBundle(Bundle bundle) {
    if (bundle == null) {
      return null;
    }
    String sqlSelectionArgsString = null;
    sqlSelectionArgsString = bundle.containsKey(OdkData.IntentKeys.SQL_SELECTION_ARGS) ?
            bundle.getString(OdkData.IntentKeys.SQL_SELECTION_ARGS) : null;
    BindArgs sqlBindArgs = new BindArgs(sqlSelectionArgsString);
    return sqlBindArgs;
  }

  /**
   * Add viewFragmentType's {@link ViewFragmentType#name()} to bundle. If
   * bundle or viewFragmentType is null, does nothing.
   *
   * @param bundle           the bundle to put the fragment view type in
   * @param viewFragmentType the fragment view type to put in the bundle
   */
  public static void addFragmentViewTypeToBundle(Bundle bundle, ViewFragmentType viewFragmentType) {
    if (bundle != null && viewFragmentType != null) {
      bundle.putString(IntentKeys.TABLE_DISPLAY_VIEW_TYPE, viewFragmentType.name());
    }
  }

  /**
   * Add the sql keys to the bundle. Convenience method for calling the
   * corresponding add methods in this class.
   *
   * @param bundle            the bundle to put the other arguments in
   * @param whereClause       A sql clause that narrows down the list of returned rows
   * @param selectionArgs     TODO
   * @param groupBy           A list of columns to group by
   * @param having            A SQL having clause
   * @param orderByElementKey The column id of the column to sort the results by
   * @param orderByDirection  the direction to sort by, ASC for ascending, DESC for descending
   */
  public static void addSQLKeysToBundle(Bundle bundle, String whereClause, BindArgs selectionArgs,
      String[] groupBy, String having, String orderByElementKey, String orderByDirection) {
    addQueryTypeToBundle(bundle, OdkData.QueryTypes.SIMPLE_QUERY);
    addWhereClauseToBundle(bundle, whereClause);
    addSelectionArgsToBundle(bundle, selectionArgs);
    addGroupByToBundle(bundle, groupBy);
    addHavingToBundle(bundle, having);
    addOrderByElementKeyToBundle(bundle, orderByElementKey);
    addOrderByDirectionToBundle(bundle, orderByDirection);
  }

  public static void addArbitraryQueryToBundle(Bundle bundle, String sqlCommand,
                                               BindArgs selectionArgs) {
    addQueryTypeToBundle(bundle, OdkData.QueryTypes.ARBITRARY_QUERY);
    addSqlCommandToBundle(bundle, sqlCommand);
    addSelectionArgsToBundle(bundle, selectionArgs);
  }

  /**
   * Add orderByElementKey to bundle keyed to
   * {@link OdkData.IntentKeys#SQL_ORDER_BY_ELEMENT_KEY}.
   * If bundle or orderByElementKey is null, does nothing.
   *
   * @param bundle            the bundle to put the order by column in
   * @param orderByElementKey the order by column to put in the bundle
   */
  public static void addOrderByElementKeyToBundle(Bundle bundle, String orderByElementKey) {
    if (bundle != null && orderByElementKey != null) {
      bundle.putString(OdkData.IntentKeys.SQL_ORDER_BY_ELEMENT_KEY, orderByElementKey);
    }
  }

  /**
   * Add orderByDirection to bundle keyed to
   * {@link OdkData.IntentKeys#SQL_ORDER_BY_DIRECTION}.
   * If bundle or orderByDirection is null, does nothing.
   *
   * @param bundle           the bundle to put the order by direction in
   * @param orderByDirection the order by direction
   */
  public static void addOrderByDirectionToBundle(Bundle bundle, String orderByDirection) {
    if (bundle != null && orderByDirection != null) {
      bundle.putString(OdkData.IntentKeys.SQL_ORDER_BY_DIRECTION, orderByDirection);
    }
  }

  /**
   * Add whereClause to bundle keyed to {@link OdkData.IntentKeys#SQL_WHERE}.
   * If bundle or whereClause is null, does nothing.
   *
   * @param bundle      the bundle to put the where clause in
   * @param whereClause the sql where clause
   */
  public static void addWhereClauseToBundle(Bundle bundle, String whereClause) {
    if (bundle != null && whereClause != null) {
      bundle.putString(OdkData.IntentKeys.SQL_WHERE, whereClause);
    }
  }

  /**
   * Add SQL command to bundle keyed to {@link OdkData.IntentKeys#SQL_COMMAND}.
   * If bundle, where clause, and sql command are null, does nothing.
   *
   * @param bundle      the bundle to put the where clause in
   * @param sqlCommand the sql command
   */
  public static void addSqlCommandToBundle(Bundle bundle, String sqlCommand) {
    if (bundle != null && sqlCommand != null) {
      bundle.putString(OdkData.IntentKeys.SQL_COMMAND, sqlCommand);
    }
  }

  /**
   * Add selectionArgs to bundle keyed to
   * {@link OdkData.IntentKeys#SQL_SELECTION_ARGS}.
   * If bundle or selectionArgs is null, does nothing.
   *
   * @param bundle        the bundle to put the selection args in
   * @param selectionArgs TODO
   */
  public static void addSelectionArgsToBundle(Bundle bundle, BindArgs selectionArgs) {
    if (bundle != null && selectionArgs != null) {
      bundle.putString(OdkData.IntentKeys.SQL_SELECTION_ARGS, selectionArgs.asJSON());
    }
  }

  /**
   * Add having to bundle keyed to
   * {@link OdkData.IntentKeys#SQL_HAVING}.
   * If bundle or having is null, does nothing.
   *
   * @param bundle the bundle to put the SQL having clause in
   * @param having the SQL having clause
   */
  public static void addHavingToBundle(Bundle bundle, String having) {
    if (bundle != null && having != null) {
      bundle.putString(OdkData.IntentKeys.SQL_HAVING, having);
    }
  }

  /**
   * Add groupBy to bundle keyed to
   * {@link OdkData.IntentKeys#SQL_GROUP_BY_ARGS}.
   * If bundle or groupBy is null, does nothing.
   *
   * @param bundle  the bundle to put the group by columns in
   * @param groupBy an array of group by column IDs
   */
  public static void addGroupByToBundle(Bundle bundle, String[] groupBy) {
    if (bundle != null && groupBy != null) {
      bundle.putStringArray(OdkData.IntentKeys.SQL_GROUP_BY_ARGS, groupBy);
    }
  }

  /**
   * Add appName to the bundle keyed to {@link IntentConsts#INTENT_KEY_APP_NAME}.
   * If bundle or appName is null, does nothing.
   *
   * @param bundle  a bundle to put the app name in
   * @param appName an app name to put in the bundle
   */
  public static void addAppNameToBundle(Bundle bundle, String appName) {
    if (bundle != null && appName != null) {
      bundle.putString(IntentConsts.INTENT_KEY_APP_NAME, appName);
    }
  }

  /**
   * Add tye name of type to the bundle keyed to
   * {@link OdkData.IntentKeys#COLOR_RULE_TYPE}. If bundle or type is null,
   * does nothing.
   *
   * @param bundle a bundle to put a color rule group type into
   * @param type   the color rule group type to store
   */
  public static void addColorRuleGroupTypeToBundle(Bundle bundle, ColorRuleGroup.Type type) {
    if (bundle != null && type != null) {
      bundle.putString(OdkData.IntentKeys.COLOR_RULE_TYPE, type.name());
    }
  }

  /**
   * Add elementKey to bundle keyed to
   * {@link OdkData.IntentKeys#ELEMENT_KEY}. If bundle of elementKey is null,
   * does nothing.
   *
   * @param bundle     a bundle to put the element key into
   * @param elementKey the element key to put into the bundle
   */
  public static void addElementKeyToBundle(Bundle bundle, String elementKey) {
    if (bundle != null && elementKey != null) {
      bundle.putString(OdkData.IntentKeys.ELEMENT_KEY, elementKey);
    }
  }

  /**
   * Add tableId to bundle keyed to {@link IntentConsts#INTENT_KEY_TABLE_ID}.
   * If bundle or appName is null, does nothing.
   *
   * @param bundle  a bundle to put the table id into
   * @param tableId the table id to put into the bundle
   */
  public static void addTableIdToBundle(Bundle bundle, String tableId) {
    if (bundle != null && tableId != null) {
      bundle.putString(IntentConsts.INTENT_KEY_TABLE_ID, tableId);
    }
  }

  /**
   * Add the name of fragmentType to bundle, keyed to
   * {@link Constants.IntentKeys#TABLE_PREFERENCE_FRAGMENT_TYPE}. If bundle
   * or fragmentType is null, does nothing.
   *
   * @param bundle       a bundle to put the table preference fragment type into
   * @param fragmentType a table preference fragment type to put into the bundle
   */
  public static void addTablePreferenceFragmentTypeToBundle(Bundle bundle,
      TableLevelPreferencesActivity.FragmentType fragmentType) {
    if (bundle != null && fragmentType != null) {
      bundle.putString(Constants.IntentKeys.TABLE_PREFERENCE_FRAGMENT_TYPE, fragmentType.name());
    }
  }

  /**
   * Add rowId to bundle keyed to {@link IntentConsts#INTENT_KEY_INSTANCE_ID}.
   * If bundle or rowId is null, does nothing.
   *
   * @param bundle a bundle to put the row id into
   * @param rowId  the row id to put into the bundle
   */
  public static void addRowIdToBundle(Bundle bundle, String rowId) {
    if (bundle != null && rowId != null) {
      bundle.putString(IntentConsts.INTENT_KEY_INSTANCE_ID, rowId);
    }
  }

  /**
   * Add fileName to bundle keyed to {@link OdkData.IntentKeys#FILE_NAME}.
   * If bundle or fileName is null, does nothing.
   *
   * @param bundle   a bundle to put the filename into
   * @param fileName the filename to add to the bundle
   */
  public static void addFileNameToBundle(Bundle bundle, String fileName) {
    if (bundle != null && fileName != null) {
      bundle.putString(OdkData.IntentKeys.FILE_NAME, fileName);
    }
  }

  /**
   * Specify the query type in the bundle keyed to {@link OdkData.IntentKeys#QUERY_TYPE}.
   * If bundle or query type is null, does nothing.
   *
   * @param bundle   a bundle to put the filename into
   * @param queryType the query type to add to the bundle
   */
  public static void addQueryTypeToBundle(Bundle bundle, String queryType) {
    if (bundle != null && queryType != null) {
      bundle.putString(OdkData.IntentKeys.QUERY_TYPE, queryType);
    }
  }


}
