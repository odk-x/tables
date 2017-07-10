/*
 * Copyright (C) 2013 University of Washington
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

import android.graphics.Color;

/**
 * @author sudar.sam@gmail.com
 * @author unknown
 */
public final class Constants {

  /**
   * The default text color when creating a new color rule
   */
  public static final int DEFAULT_TEXT_COLOR = Color.BLACK;
  /**
   * The default background color when creating a new color rule
   */
  public static final int DEFAULT_BACKGROUND_COLOR = Color.WHITE;

  private Constants() {
  }

  /**
   * Unused
   */
  public static class HTML {
    /**
     * The default HTML to be displayed if no file name has been set.
     */
    public static final String NO_FILE_NAME =
        "<html><body>" + "<p>No filename has been specified.</p>" + "</body></html>";
  }

  /**
   * Unused
   */
  public static class MimeTypes {
    /**
     * Unused
     */
    public static final String TEXT_HTML = "text/html";
  }

  /**
   * Unused
   */
  public static class ExternalIntentStrings {
    /**
     * Survey's package name as declared in the manifest.
     * Unused
     */
    public static final String SYNC_PACKAGE_NAME = "org.opendatakit.sync";
  }

  /**
   * Intent keys to be used to communicate between activities.
   *
   * @author sudar.sam@gmail.com
   */
  public static final class IntentKeys {

    /**
     * Used in AbsBaseActivity to store the current action table id in the saved instance state
     */
    public static final String ACTION_TABLE_ID = org.opendatakit.views.OdkData.IntentKeys.ACTION_TABLE_ID;
    /**
     * tables that have conflict rows
     */
    public static final String CONFLICT_TABLES = "conflictTables";
    /**
     * tables that have checkpoint rows
     */
    public static final String CHECKPOINT_TABLES = "checkpointTables";

    /**
     * Tells TableDisplayActivity what time of view it should be
     * displaying.
     */
    public static final String TABLE_DISPLAY_VIEW_TYPE = "tableDisplayViewType";
    /**
     * Intent key to store filenames
     */
    public static final String FILE_NAME = "filename";
    // should be instanceID?    public static final String ROW_ID = "rowId";
    /**
     * The name of the graph view that should be displayed.
     */
    public static final String ELEMENT_KEY = "elementKey";
    /**
     * Intent key for storing a color rule type (column, table, status column) in a bundle
     * Only used in IntentUtil
     */
    public static final String COLOR_RULE_TYPE = "colorRuleType";
    /**
     * The TableLevelPreferencesActivity.FragmentType that should be
     * displayed when launching a TableLevelPreferencesActivity.
     */
    public static final String TABLE_PREFERENCE_FRAGMENT_TYPE = "tablePreferenceFragmentType";
    /**
     * Key to the where clause if this list view is to be opened with a more
     * complex query than permissible by the simple query object. Must conform
     * to the expectations of simpleQuery() AIDL.
     */
    public static final String SQL_WHERE = "sqlWhereClause";

    /**
     * Used when launching a TableDisplayActivity to a SpreadsheetView when you want to pass
     * non-default properties (sort column, sort direction, group by column or frozen column)
     * via the intent or activity result.
     */
    public static final String CONTAINS_PROPS = "containsProps";

    /**
     * A JSON serialization of an array of bind parameters.
     * This allows for integer, numeric, boolean and string values to be
     * passed through to the SQLite layer.
     */
    public static final String SQL_SELECTION_ARGS = "sqlSelectionArgs";
    /**
     * An array of strings giving the group by columns. What was formerly
     * 'overview' mode is a non-null groupBy list.
     */
    public static final String SQL_GROUP_BY_ARGS = "sqlGroupByArgs";
    /**
     * The having clause, if present
     */
    public static final String SQL_HAVING = "sqlHavingClause";
    /**
     * The order by column. NOTE: restricted to a single column
     */
    public static final String SQL_ORDER_BY_ELEMENT_KEY = "sqlOrderByElementKey";
    /**
     * The order by direction (ASC or DESC)
     */
    public static final String SQL_ORDER_BY_DIRECTION = "sqlOrderByDirection";

    private IntentKeys() {
    }
  }

  /**
   * For most fragments, this is either the name() of ScreenType or {@see ViewFragmentType}.
   * <p>
   * Others are listed here.
   */
  public static final class FragmentTags {
    /**
     * ViewFragmentType.MAP has two inner fragments and a separate mapping
     * layout that is made visible. The inner fragments are controlled with
     * these fragment tags.
     */
    public static final String MAP_INNER_MAP = "tagInnerMapFragment";
    /**
     * Tag for the map list in the fragment manager
     */
    public static final String MAP_LIST = "tagMapListFragment";

    /**
     * ViewFragmentType.DETAIL_WITH_LIST has two inner fragments.
     * The inner fragments are controlled with these fragment tags.
     */
    public static final String DETAIL_WITH_LIST_DETAIL = "tagDetailWithListDetailFragment";
    /**
     * Tag for the detail with list list webview fragment in the fragment manager
     */
    public static final String DETAIL_WITH_LIST_LIST = "tagDetailWithListListFragment";

    /**
     * Preference screens have their own tags...
     */
    public static final String COLUMN_LIST = "tagColumnList";
    /**
     * fragment manager tag for table preferences
     */
    public static final String TABLE_PREFERENCE = "tagTablePreference";
    /**
     * fragment manager tag for column preferences
     */
    public static final String COLUMN_PREFERENCE = "tagColumnPreference";
    /**
     * fragment manager tag for a color rule list
     */
    public static final String COLOR_RULE_LIST = "tagColorRuleList";
    /**
     * fragment manager tag for the list of status color rules
     */
    public static final String STATUS_COLOR_RULE_LIST = "tagStatusColorRuleList";
    /**
     * fragment manager tag for editing a color rule
     */
    public static final String EDIT_COLOR_RULE = "tagEditColorRule";

    /**
     * Do not instantiate this
     */
    private FragmentTags() {
    }
  }

  /**
   * Holds preference keys
   */
  public static class PreferenceKeys {

    /**
     * Preference keys for table-level preferences.
     *
     * @author sudar.sam@gmail.com
     */
    public static final class Table {
      /**
       * The (not editable) localized display name showed at the top of the table preferences screen
       */
      public static final String DISPLAY_NAME = "table_pref_display_name";
      /**
       * The (not editable) table id showed at the top of the table preferences screen
       */
      public static final String TABLE_ID = "table_pref_table_id";
      /**
       * The (editable) default view type (spreadsheet, list, map) dropdown in the table preferences
       * screen
       */
      public static final String DEFAULT_VIEW_TYPE = "table_pref_default_view_type";
      /**
       * Unused because it's handled in
       * {@link org.opendatakit.tables.preferences.EditFormDialogPreference}, which uses view
       * .findViewById instead of the traditional fragment.findListPreference
       */
      public static final String DEFAULT_FORM = "table_pref_default_form";
      /**
       * The (editable) default view type (spreadsheet, list, map) dropdown in the table preferences
       * screen
       */
      public static final String TABLE_COLOR_RULES = "table_pref_table_color_rules";
      /**
       * The preference button to open a list of color rules for the status column
       */
      public static final String STATUS_COLOR_RULES = "table_pref_status_column_color_rules";
      /**
       * The preference button to open a menu that lets you select which set of color rules (if
       * any) will be applied to the map view
       */
      public static final String MAP_COLOR_RULE = "table_pref_map_color_rule";
      /**
       * The preference button for the file picker to pick a list view html file
       */
      public static final String LIST_FILE = "table_pref_list_file";
      /**
       * The preference button for the file picker to pick a detail view html file
       */
      public static final String DETAIL_FILE = "table_pref_detail_file";
      /**
       * The preference button for the file picker to pick a map view html file
       */
      public static final String MAP_LIST_FILE = "table_pref_map_list_file";
      /**
       * The preference button to open the list of column
       */
      public static final String COLUMNS = "table_pref_columns";

      /**
       * Do not instantiate this
       */
      private Table() {
      }
    }

    /**
     * Holds tags for column level preferences fragments
     */
    public static final class Column {
      /**
       * The (non editable) preference that displays the column's localized display name
       */
      public static final String DISPLAY_NAME = "column_pref_display_name";
      /**
       * The (non editable) preference that displays the column's raw column id
       */
      public static final String ELEMENT_KEY = "column_pref_element_key";
      /**
       * The (non editable) preference that displays the column's "element name", seems to always
       * be the same as the element key?
       */
      public static final String ELEMENT_NAME = "column_pref_element_name";
      /**
       * The (non editable) preference that displays the column's data type (String, Number,
       * Date, etc..)
       */
      public static final String TYPE = "column_pref_column_type";
      /**
       * The  preference button that opens the change column width dialog
       */
      public static final String WIDTH = "column_pref_column_width";
      /**
       * The  preference button that opens the edit color rules dialog
       */
      public static final String COLOR_RULES = "column_pref_color_rules";

      /**
       * Do not instantiate this
       */
      private Column() {
      }
    }

    /**
     * Preference keys for the color rules menu
     */
    public static final class ColorRule {
      /**
       * The preference option for comparison type (=, &lt;, &lt;=, &gt;, &gt;=)
       */
      public static final String COMPARISON_TYPE = "pref_color_rule_comp_type";
      /**
       * The preference option for the value to compare against
       */
      public static final String RULE_VALUE = "pref_color_rule_value";
      /**
       * The preference option for the text color to set if the rule is matched
       */
      public static final String TEXT_COLOR = "pref_color_rule_text_color";
      /**
       * The preference option for the background color to set if the rule is matched
       */
      public static final String BACKGROUND_COLOR = "pref_color_rule_background_color";
      /**
       * The preference option for the save button
       */
      public static final String SAVE = "pref_color_rule_save";
      /**
       * The (HIDDEN!) preference option for the column associated with the rule
       */
      public static final String ELEMENT_KEY = "pref_color_rule_element_key";

      /**
       * Do not instantiate this
       */
      private ColorRule() {
      }
    }
  }

  /**
   * Request codes, used with startActivityForResult
   */
  public static final class RequestCodes {
    /**
     * Used to view a collection or a join table in a SpreadsheetFragment in
     * TableDisplayActivity
     */
    public static final int DISPLAY_VIEW = 1;
    /**
     * Used when launching the file picker to change the detail view file
     */
    public static final int CHOOSE_DETAIL_FILE = 2;
    /**
     * Used when launching the file picker to change the list view file
     */
    public static final int CHOOSE_LIST_FILE = 3;
    /**
     * Used when launching the file picker to change the map view file
     */
    public static final int CHOOSE_MAP_FILE = 4;
    /**
     * A generic code for now. Can refactor to make more specific if needed.
     */
    public static final int LAUNCH_VIEW = 5;
    /**
     * Used to launch the device preferences screen (the one with the server settings, device
     * settings, tables specific settings, user restrictions, etc... options)
     */
    public static final int LAUNCH_DISPLAY_PREFS = 6;
    /**
     * For launching an intent to import csv files into a table
     */
    public static final int LAUNCH_IMPORT = 7;
    /**
     * For launching an intent to sync
     */
    public static final int LAUNCH_SYNC = 8;
    /**
     * For launching an HTML file not associated with a table. Unused
     */
    public static final int LAUNCH_WEB_VIEW = 10;
    /**
     * For launching an intent to edit a table's properties.
     */
    public static final int LAUNCH_TABLE_PREFS = 11;
    /**
     * For launching an intent to list a column's color rules
     */
    public static final int LAUNCH_COLOR_RULE_LIST = 16;
    /**
     * For launching an intent to add a row in survey
     */
    public static final int ADD_ROW_SURVEY = 17;
    /**
     * For launching an intent to edit a row in survey
     */
    public static final int EDIT_ROW_SURVEY = 18;
    /**
     * For launching an intent to resolve checkpoints
     */
    public static final int LAUNCH_CHECKPOINT_RESOLVER = 19;
    /**
     * For launching an intent to resolve conflicts
     */
    public static final int LAUNCH_CONFLICT_RESOLVER = 20;
    /**
     * For launching an intent to export a table to a csv
     */
    public static final int LAUNCH_EXPORT = 21;
    /**
     * Used when the javascript has started a DoAction and needs to be notified of its result
     */
    public static final int LAUNCH_DOACTION = 22;
    /**
     * Used to launch an intent to open a column preference fragment via table level preference
     * activity
     */
    public static final int LAUNCH_COLUMN_PREFS = 23;

    /**
     * Do not instantiate this class
     */
    private RequestCodes() {
    }
  }

  /**
   * The names of the JavaScript interfaces that are attached to the window
   * object.
   *
   * @author sudar.sam@gmail.com
   */
  public static final class JavaScriptHandles {
    /**
     * The id of the webkit to add the javascript interface to (?)
     */
    public static final String ODK_TABLES_IF = "odkTablesIf";

    /**
     * Do not instantiate this class
     */
    private JavaScriptHandles() {
    }
  }

}
