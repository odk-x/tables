package org.opendatakit.util;

public class TestConstants {
  //UI Automator Timeouts
  //Timeout for launching an app
  public static final int APP_START_TIMEOUT = 20 * 1000;
  //Timeout for "Configuring ..."
  public static final int APP_INIT_TIMEOUT = 5 * 60 * 1000;
  //Timeout for view rendering
  public static final int OBJ_WAIT_TIMEOUT = 3000;
  //Timeout for opening table manager
  public static final int TABLE_MGR_WAIT = 10 * 1000;
  //Table names and related
  public static final String T_HOUSE_DISPLAY_NAME = "Tea Houses";
  public static final String T_HOUSE_TABLE_ID = "Tea_houses";
  public static final String T_INVENTORY_DISPLAY_NAME = "Tea inventory";
  public static final String T_INVENTORY_TABLE_ID = "Tea_inventory";
  public static final String T_HOUSE_E_DISPLAY_NAME = "Tea Houses Editable";
  public static final String T_HOUSE_E_TABLE_ID = "Tea_houses_editable";
  //Web names
  public static final String LAUNCH_DEMO_ID = "launch-button";
  public static final String GEO_TAB_ID = "geotaggerTab";
  public static final String HOPE_TAB_ID = "hopeTab";
  //Preference keys
  public static final String DEFAULT_VIEW_TYPE = "table_pref_default_view_type";
  public static final String STATUS_COL_COLOR = "table_pref_status_column_color_rules";
  public static final String LIST_VIEW_FILE = "table_pref_list_file";
  public static final String DETAIL_VIEW_FILE = "table_pref_detail_file";
  public static final String MAP_LIST_VIEW_FILE = "table_pref_map_list_file";
  public static final String COLUMNS_LIST = "table_pref_columns";
  public static final String TABLE_COLOR = "table_pref_table_color_rules";
  public static final String TABLE_COLOR_TEXT = "pref_color_rule_text_color";
  public static final String TABLE_DISPLAY_NAME = "table_pref_display_name";
  public static final String TABLE_ID = "table_pref_table_id";
  public static final String COL_DISPLAY_NAME = "column_pref_display_name";
  public static final String COL_KEY = "column_pref_element_key";
  public static final String DEFAULT_FORM = "table_pref_default_form";
  //Misc.
  public static final String OI_PICK_FILE = "org.openintents.action.PICK_FILE";
  public static final String APP_NAME = "default";
  //ODK Services menu
  public static final String TABLES_SPECIFIC_SETTINGS = "Tables-specific Settings";
  public static final String CUSTOM_HOME = "Use custom home screen";
  //Package names
  public static String TABLES_PKG_NAME = "org.opendatakit.tables";
  public static String SURVEY_PKG_NAME = "org.opendatakit.survey";
  //Espresso Web
  public static int WEB_WAIT_TIMEOUT = 6000;
}
