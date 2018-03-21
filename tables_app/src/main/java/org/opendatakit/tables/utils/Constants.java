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
     * The TableLevelPreferencesActivity.FragmentType that should be
     * displayed when launching a TableLevelPreferencesActivity.
     */
    public static final String TABLE_PREFERENCE_FRAGMENT_TYPE = "tablePreferenceFragmentType";

    /**
     * Used when launching a TableDisplayActivity to a SpreadsheetView when you want to pass
     * non-default properties (sort column, sort direction, group by column or frozen column)
     * via the intent or activity result.
     */
    public static final String CONTAINS_PROPS = "containsProps";

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
     * ViewFragmentType.NAVIGATE has two inner fragments.
     * The inner fragments are controlled with these fragment tags.
     * Tag for the detail with navigate fragment in the fragment manager
     */
    public static final String NAVIGATE = "tagNavigateFragment";

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

  /**
   * Enum for storing SORT ORDER for Table's table list.
   */
  public enum TABLE_SORT_ORDER {
      SORT_ASC, SORT_DESC
  }
}
