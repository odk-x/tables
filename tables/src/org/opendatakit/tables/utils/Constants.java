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

import org.opendatakit.tables.fragments.TopLevelTableMenuFragment;

import android.graphics.Color;

/**
 * 
 * @author sudar.sam@gmail.com
 * @author unknown
 *
 */
public class Constants {
  
  public static final int DEFAULT_TEXT_COLOR = Color.BLACK;
  public static final int DEFAULT_BACKGROUND_COLOR = Color.WHITE;
  
  /**
   * Intent keys to be used to communicate between activities.
   * @author sudar.sam@gmail.com
   *
   */
  public static class IntentKeys {
    public static final String TABLE_ID = "tableId";
    public static final String APP_NAME = "appName";
  }
  
  public static class FragmentTags {
    /** Tag for {@link TopLevelTableMenuFragment} */
    public static final String TABLE_MENU = "tableMenuFragment";
  }

  public static class PreferenceKeys {
    
    /**
     * Preference keys for table-level preferences.
     * @author sudar.sam@gmail.com
     *
     */
    public static class Table {
      public static String DISPLAY_NAME = "table_pref_display_name";
      public static String TABLE_ID = "table_pref_table_id";
      public static String DEFAULT_VIEW_TYPE = "table_pref_default_view_type";
      public static String DEFAULT_FORM = "table_pref_default_form";
      public static String TABLE_COLOR_RULES = "table_pref_table_color_rules";
      public static String STATUS_COLOR_RULES = 
          "table_pref_status_column_color_rules";
      public static String MAP_COLOR_RULE = "table_pref_map_color_rule";
      public static String LIST_FILE = "table_pref_list_file";
      public static String DETAIL_FILE = "table_pref_detail_file";
      public static String GRAPH_MANAGER = "table_pref_graph_view_manager";
    }
  }

}
