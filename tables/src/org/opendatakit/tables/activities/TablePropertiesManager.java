/*
 * Copyright (C) 2012 University of Washington
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
package org.opendatakit.tables.activities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opendatakit.aggregate.odktables.rest.KeyValueStoreConstants;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.KeyValueHelper;
import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.common.android.database.DataModelDatabaseHelperFactory;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.utils.ColumnUtil;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.LocalKeyValueStoreConstants;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.tables.utils.TableUtil;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

/**
 * An activity for managing a table's properties.
 *
 * @author hkworden@gmail.com
 * @author sudar.sam@gmail.com
 */
public class TablePropertiesManager extends PreferenceActivity {

  private static final String TAG = "TablePropertiesManager";

  // these ints are used when selecting/changing the view files
  private static final int RC_DETAIL_VIEW_FILE = 0;
  private static final int RC_LIST_VIEW_FILE = 1;
  private static final int RC_MAP_LIST_VIEW_FILE = 2;

  /** The key for the type of color rule to use on the map. */
  public static final String KEY_COLOR_RULE_TYPE = "keyColorRuleType";
  /**
   * The key for, if the color rule is based off of a column, the column to use.
   */
  public static final String KEY_COLOR_RULE_COLUMN = "keyColorRuleColumn";

  /** The constant if we want no color rules. */
  public static final String COLOR_TYPE_NONE = "None";
  /** The constant if we want the color rules based off of the table. */
  public static final String COLOR_TYPE_TABLE = "Table Color Rules";
  /** The constant if we want the color rules based off of the status column. */
  public static final String COLOR_TYPE_STATUS = "Status Column Color Rules";
  /** The constant if we want the color rules based off of a column. */
  public static final String COLOR_TYPE_COLUMN = "Selectable Column Color Rules";

  private String appName;
  private String tableId;
  private ArrayList<ColumnDefinition> orderedDefns;
//  private TableProperties tp;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    appName = getIntent().getStringExtra(Constants.IntentKeys.APP_NAME);
    if (appName == null) {
      appName = TableFileUtils.getDefaultAppName();
    }
    tableId = getIntent().getStringExtra(Constants.IntentKeys.TABLE_ID);
    if (tableId == null) {
      throw new RuntimeException("Table ID (" + tableId + ") is invalid.");
    }

    String localizedDisplayName;
    SQLiteDatabase db = null;
    try {
      db = DataModelDatabaseHelperFactory.getDatabase(this, appName);
      localizedDisplayName = TableUtil.get().getLocalizedDisplayName(db, tableId);
    } finally {
      if (db != null) {
        db.close();
      }
    }

    setTitle(getString(R.string.table_manager_title, localizedDisplayName));
    init();
  }

  private void init() {

    PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

    // general category

    PreferenceCategory genCat = new PreferenceCategory(this);
    root.addPreference(genCat);
    genCat.setTitle(getString(R.string.general_settings));

    String rawDisplayName;
    SQLiteDatabase db = null;
    try {
      db = DataModelDatabaseHelperFactory.getDatabase(this, appName);
      rawDisplayName = TableUtil.get().getRawDisplayName(db, tableId);
    } finally {
      if (db != null) {
        db.close();
      }
    }

    EditTextPreference dnPref = new EditTextPreference(this);
    dnPref.setTitle(getString(R.string.table_display_name));
    dnPref.setDialogTitle(getString(R.string.change_table_display_name));
    dnPref.setText(rawDisplayName);
    dnPref.setSummary(rawDisplayName);
    dnPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        SQLiteDatabase db = null;
        String localizedDisplayName;
        try {
          db = DataModelDatabaseHelperFactory.getDatabase(
              TablePropertiesManager.this, appName);
          db.beginTransaction();
          TableUtil.get().setRawDisplayName(db, tableId, (String) newValue);
          localizedDisplayName = TableUtil.get().getLocalizedDisplayName(db, tableId);
          db.setTransactionSuccessful();
        } catch (Exception e) {
          e.printStackTrace();
          Log.e(TAG, "Unable to change display name: " + e.toString());
          Toast.makeText(getParent(), "Unable to change display name", Toast.LENGTH_LONG).show();
          init();
          return false;
        } finally {
          if ( db != null ) {
            db.endTransaction();
            db.close();
          }
        }
        setTitle(getString(R.string.table_manager_title, localizedDisplayName));
        init();
        return false;
      }
    });
    genCat.addPreference(dnPref);

    // display category

    {
      PreferenceCategory displayCat = new PreferenceCategory(this);
      root.addPreference(displayCat);
      displayCat.setTitle(getString(R.string.display_settings));
      addViewPreferences(displayCat);
    }
    PreferenceCategory displayListViewCat = new PreferenceCategory(this);
    root.addPreference(displayListViewCat);
    displayListViewCat.setTitle(getString(R.string.display_list_view_settings));
    addListViewPreferences(displayListViewCat);

    PreferenceCategory displayGraphViewCat = new PreferenceCategory(this);
    root.addPreference(displayGraphViewCat);
    displayGraphViewCat.setTitle(getString(R.string.display_graph_view_settings));
    addGraphViewPreferences(displayGraphViewCat);

    PreferenceCategory displayMapViewCat = new PreferenceCategory(this);
    root.addPreference(displayMapViewCat);
    displayMapViewCat.setTitle(getString(R.string.display_map_view_settings));
    addMapViewPreferences(displayMapViewCat);

    setPreferenceScreen(root);
  }

  private void addViewPreferences(PreferenceCategory prefCat) {

    // int[] viewTypes = settings.getPossibleViewTypes();

    // This code got all commented out with the rewrite of TableViewType.
    // Set<TableViewType> viewTypes = tp.getPossibleViewTypes();
    // String[] viewTypeIds = new String[viewTypes.length];
    // String[] viewTypeNames = new String[viewTypes.length];
    // // for (int i = 0; i < viewTypes.length; i++) {
    // // int viewType = viewTypes[i];
    // // viewTypeIds[i] = String.valueOf(viewType);
    // // viewTypeNames[i] = LanguageUtil.getViewTypeLabel(viewType);
    // // }
    // // so now we need to populate the actual menu with the thing to save
    // // and to the human-readable labels.
    // for (int i = 0; i < viewTypes.length; i++) {
    // viewTypeIds[i] = viewTypes[i].name();
    // viewTypeNames[i] = viewTypes[i].name();
    // }
    ListPreference viewTypePref = new ListPreference(this);
    viewTypePref.setTitle(getString(R.string.default_view_type));
    viewTypePref.setDialogTitle(getString(R.string.change_default_view_type));
    // viewTypePref.setEntryValues(viewTypeIds);
    // viewTypePref.setEntries(viewTypeNames);
    // viewTypePref.setValue(String.valueOf(settings.getViewType()));

    TableViewType type;
    SQLiteDatabase db = null;
    try {
      db = DataModelDatabaseHelperFactory.getDatabase(this, appName);
      type = TableUtil.get().getDefaultViewType(db, tableId);
    } finally {
      if (db != null) {
        db.close();
      }
    }
    viewTypePref.setValue(type.name());
    // TODO: currently throwing an error i think
    // viewTypePref.setSummary(LanguageUtil.getViewTypeLabel(
    // settings.getViewType()));
    viewTypePref.setSummary(type.name());
    viewTypePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        SQLiteDatabase db = null;
        try {
          db = DataModelDatabaseHelperFactory.getDatabase(
              TablePropertiesManager.this, appName);
          db.beginTransaction();
          TableUtil.get().setDefaultViewType(db, tableId,
              TableViewType.valueOf((String) newValue));
          db.setTransactionSuccessful();
        } catch (Exception e) {
          e.printStackTrace();
          Log.e(TAG, "Unable to change default view type: " + e.toString());
          Toast.makeText(getParent(), "Unable to change default view type", Toast.LENGTH_LONG)
              .show();
        } finally {
          if ( db != null ) {
            db.endTransaction();
            db.close();
          }
        }
        init();
        return false;
      }
    });
    prefCat.addPreference(viewTypePref);

    // // Now let's add the pref for the Form.
    // EditFormDialogPreference formPref = new EditFormDialogPreference(this,
    // tp);
    // prefCat.addPreference(formPref);
    // formPref.setTitle(getString(R.string.default_survey_form));
    // formPref.setDialogTitle(getString(R.string.edit_default_form));

    Preference rowColorRulePrefs = new Preference(this);
    rowColorRulePrefs.setTitle(getString(R.string.edit_table_color_rules));
    // rowColorRulePrefs.setOnPreferenceClickListener(new
    // OnPreferenceClickListener() {

    // @Override
    // public boolean onPreferenceClick(Preference preference) {
    // Intent rowColorRuleManagerIntent = new
    // Intent(TablePropertiesManager.this,
    // ColorRuleManagerActivity.class);
    // rowColorRuleManagerIntent.putExtra(
    // Constants.IntentKeys.APP_NAME, tp.getAppName());
    // rowColorRuleManagerIntent.putExtra(ColorRuleManagerActivity.INTENT_KEY_TABLE_ID,
    // tp.getTableId());
    // rowColorRuleManagerIntent.putExtra(ColorRuleManagerActivity.INTENT_KEY_RULE_GROUP_TYPE,
    // ColorRuleGroup.Type.TABLE.name());
    // startActivity(rowColorRuleManagerIntent);
    // return true;
    // }

    // });
    // prefCat.addPreference(rowColorRulePrefs);
    //
    // Preference statusColumnColorRulePref = new Preference(this);
    // statusColumnColorRulePref.setTitle(getString(R.string.edit_status_column_color_rules));
    // statusColumnColorRulePref.setOnPreferenceClickListener(new
    // OnPreferenceClickListener() {
    //
    // @Override
    // public boolean onPreferenceClick(Preference preference) {
    // Intent rowColorRuleManagerIntent = new
    // Intent(TablePropertiesManager.this,
    // ColorRuleManagerActivity.class);
    // rowColorRuleManagerIntent.putExtra(Constants.IntentKeys.APP_NAME,
    // tp.getAppName());
    // rowColorRuleManagerIntent.putExtra(ColorRuleManagerActivity.INTENT_KEY_TABLE_ID,
    // tp.getTableId());
    // rowColorRuleManagerIntent.putExtra(ColorRuleManagerActivity.INTENT_KEY_RULE_GROUP_TYPE,
    // ColorRuleGroup.Type.STATUS_COLUMN.name());
    // startActivity(rowColorRuleManagerIntent);
    // return true;
    // }
    //
    // });
    // prefCat.addPreference(statusColumnColorRulePref);

    // Grab the key value store helper from the table activity.
    final KeyValueStoreHelper kvsHelper = new KeyValueStoreHelper(this, appName, tableId,
        KeyValueStoreConstants.PARTITION_TABLE);

    // Color Options Preference!
    String colorType = kvsHelper.getString(KEY_COLOR_RULE_TYPE);
    if (colorType == null) {
      kvsHelper.setString(KEY_COLOR_RULE_TYPE, COLOR_TYPE_NONE);
      colorType = COLOR_TYPE_NONE;
    }
    ListPreference colorRulePref = new ListPreference(this);
    colorRulePref.setTitle("Color Rule for Graph and Map Markers");
    colorRulePref.setDialogTitle("Change which color rule markers adhere to.");
    String[] colorRuleTypes = { COLOR_TYPE_NONE, COLOR_TYPE_TABLE, COLOR_TYPE_STATUS,
        COLOR_TYPE_COLUMN };
    colorRulePref.setEntryValues(colorRuleTypes);
    colorRulePref.setEntries(colorRuleTypes);
    colorRulePref.setValue(colorType);
    colorRulePref.setSummary(colorType);
    colorRulePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        kvsHelper.setString(KEY_COLOR_RULE_TYPE, (String) newValue);
        init();
        return false;
      }
    });
    prefCat.addPreference(colorRulePref);

    // If the color rule type is columns, add the preference to select the
    // column.
    if (colorType.equals(COLOR_TYPE_COLUMN)) {
      ArrayList<String> colorColDisplayNames = new ArrayList<String>();
      ArrayList<String> colorColElementKeys = new ArrayList<String>();
      db = null;
      try {
        db = DataModelDatabaseHelperFactory.getDatabase(this, appName);
        List<Column> columns = ODKDatabaseUtils.getUserDefinedColumns(db, tableId);
        ArrayList<ColumnDefinition> orderedDefns = 
            ColumnDefinition.buildColumnDefinitions(columns);
        for (ColumnDefinition cd : orderedDefns) {
          if (cd.isUnitOfRetention()) {
            String localizedDisplayName;
            localizedDisplayName = ColumnUtil.get().getLocalizedDisplayName(db, tableId,
                cd.getElementKey());

            colorColDisplayNames.add(localizedDisplayName);
            colorColElementKeys.add(cd.getElementKey());
          }
        }
      } finally {
        if (db != null) {
          db.close();
        }
      }

      ColumnDefinition colorColumn = null;
      try {
        colorColumn = ColumnDefinition.find(orderedDefns,
            kvsHelper.getString(KEY_COLOR_RULE_COLUMN));
      } catch (IllegalArgumentException e) {
        // no-op
      }
      if (colorColumn == null && colorColElementKeys.size() > 0) {
        kvsHelper.setString(KEY_COLOR_RULE_COLUMN, colorColElementKeys.get(0));
        colorColumn = ColumnDefinition.find(orderedDefns, colorColElementKeys.get(0));
      }

      String localizedDisplayName;
      db = null;
      try {
        db = DataModelDatabaseHelperFactory.getDatabase(this, appName);
        localizedDisplayName = ColumnUtil.get().getLocalizedDisplayName(db, tableId,
            colorColumn.getElementKey());
      } finally {
        if (db != null) {
          db.close();
        }
      }

      ListPreference colorColumnPref = new ListPreference(this);
      colorColumnPref.setTitle("Color Rule Column");
      colorColumnPref.setDialogTitle("Change the column that applies the color rule.");
      colorColumnPref.setEntryValues(colorColElementKeys.toArray(new String[colorColElementKeys
          .size()]));
      colorColumnPref.setEntries(colorColDisplayNames.toArray(new String[colorColDisplayNames
          .size()]));
      colorColumnPref.setValue(colorColumn.getElementKey());
      colorColumnPref.setSummary(localizedDisplayName);
      colorColumnPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
          kvsHelper.setString(KEY_COLOR_RULE_COLUMN, (String) newValue);
          init();
          return false;
        }
      });
      prefCat.addPreference(colorColumnPref);
    }
  }

  private void addListViewPreferences(PreferenceCategory prefCat) {
    Preference listViewPrefs = new Preference(this);
    listViewPrefs.setTitle(getString(R.string.list_view_manager));
    listViewPrefs.setOnPreferenceClickListener(new OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        // Intent selectListViewIntent = new Intent(TablePropertiesManager.this,
        // ListViewManager.class);
        // selectListViewIntent.putExtra(Constants.IntentKeys.APP_NAME,
        // tp.getAppName());
        // selectListViewIntent.putExtra(Controller.INTENT_KEY_TABLE_ID,
        // tp.getTableId());
        // startActivity(selectListViewIntent);
        return true;
      }

    });
    prefCat.addPreference(listViewPrefs);

    FileSelectorPreference detailViewPref = new FileSelectorPreference(this, RC_DETAIL_VIEW_FILE);
    detailViewPref.setTitle(getString(R.string.detail_view_file));
    detailViewPref.setDialogTitle(getString(R.string.change_detail_view_file));
    // final KeyValueStoreHelper kvsh =
    // tp.getKeyValueStoreHelper(DetailDisplayActivity.KVS_PARTITION);
    // String detailViewFilename =
    // kvsh.getString(DetailDisplayActivity.KEY_FILENAME);
    // detailViewPref.setText(detailViewFilename);
    detailViewPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        // tp.setDetailViewFilename((String) newValue);
        // kvsh.setString(DetailDisplayActivity.KEY_FILENAME, (String)
        // newValue);
        init();
        return false;
      }
    });
    prefCat.addPreference(detailViewPref);

  }

  private void addGraphViewPreferences(PreferenceCategory prefCat) {
    Log.d(TAG, "Graph view type was selected");
    // TODO -- should we really do the graph manager here?
    Preference graphViewPrefs = new Preference(this);
    graphViewPrefs.setTitle(getString(R.string.graph_view_manager));
    graphViewPrefs.setOnPreferenceClickListener(new OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        // Intent selectGraphViewIntent = new
        // Intent(TablePropertiesManager.this,
        // GraphManagerActivity.class);
        // selectGraphViewIntent.putExtra(Constants.IntentKeys.APP_NAME,
        // tp.getAppName());
        // selectGraphViewIntent.putExtra(
        // Constants.IntentKeys.TABLE_ID,
        // tp.getTableId());
        // startActivity(selectGraphViewIntent);
        return true;
      }

    });
    prefCat.addPreference(graphViewPrefs);
  }

  private void addMapViewPreferences(PreferenceCategory prefCat) {
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_CANCELED) {
      return;
    }
    KeyValueStoreHelper kvsh;
    Uri uri;
    String filename;
    String relativePath;
    switch (requestCode) {
    case RC_DETAIL_VIEW_FILE:
      uri = data.getData();
      filename = uri.getPath();
      relativePath = getRelativePathOfFile(filename);
      // kvsh = tp.getKeyValueStoreHelper(DetailDisplayActivity.KVS_PARTITION);
      // kvsh.setString(DetailDisplayActivity.KEY_FILENAME, relativePath);
      // tp.setDetailViewFilename(filename);
      init();
      break;
    case RC_LIST_VIEW_FILE:
      uri = data.getData();
      filename = uri.getPath();
      // We need to get the relative path under the app name.
      relativePath = getRelativePathOfFile(filename);
      // Trying to get the new name to the _VIEWS partition.
      kvsh = new KeyValueStoreHelper(this, this.appName, this.tableId,
          LocalKeyValueStoreConstants.ListViews.PARTITION_VIEWS);
      // Set the name here statically, just to test. Later will want to
      // allow custom naming, checking for redundancy, etc.
      KeyValueHelper aspectHelper = kvsh.getAspectHelper("List View 1");
      aspectHelper.setString(LocalKeyValueStoreConstants.ListViews.KEY_FILENAME, relativePath);
      init();
      break;
    case RC_MAP_LIST_VIEW_FILE:
      // tp.getKeyValueStoreHelper(TableMapFragment.KVS_PARTITION).setString(
      // TableMapFragment.KEY_FILENAME,
      // getRelativePathOfFile(data.getData().getPath()));
      init();
    default:
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  /**
   * Get the relative filepath under the app directory for the full path as
   * returned from OI file picker.
   *
   * @param fullPath
   * @return
   */
  private String getRelativePathOfFile(String fullPath) {
    String relativePath = ODKFileUtils.asRelativePath(appName, new File(fullPath));
    return relativePath;
  }

  @Override
  public void onBackPressed() {
    setResult(RESULT_OK);
    finish();
  }

  /**
   * This preference allows the user to select a file from their SD card. If the
   * user does not have a file picker installed on their phone, then a toast
   * will indicate so.
   *
   * @author Chris Gelon (cgelon)
   */
  private class FileSelectorPreference extends EditTextPreference {
    /** Indicates which preference we are using the selector for. */
    private int mRequestCode;

    public FileSelectorPreference(Context context, int requestCode) {
      super(context);
      mRequestCode = requestCode;
    }

    @Override
    protected void onClick() {
      if (hasFilePicker()) {
        Intent intent = new Intent("org.openintents.action.PICK_FILE");
        if (getText() != null) {
          File fullFile = ODKFileUtils.asAppFile(appName, getText());
          try {
            intent.setData(Uri.parse("file://" + fullFile.getCanonicalPath()));
          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(TablePropertiesManager.this,
                getString(R.string.file_not_found, fullFile.getAbsolutePath()), Toast.LENGTH_LONG)
                .show();
          }
        }
        try {
          startActivityForResult(intent, mRequestCode);
        } catch (ActivityNotFoundException e) {
          e.printStackTrace();
          Toast.makeText(TablePropertiesManager.this, getString(R.string.file_picker_not_found),
              Toast.LENGTH_LONG).show();
        }
      } else {
        super.onClick();
        Toast.makeText(TablePropertiesManager.this, getString(R.string.file_picker_not_found),
            Toast.LENGTH_LONG).show();
      }
    }

    /**
     * @return True if the phone has a file picker installed, false otherwise.
     */
    private boolean hasFilePicker() {
      PackageManager packageManager = getPackageManager();
      Intent intent = new Intent("org.openintents.action.PICK_FILE");
      List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
          PackageManager.MATCH_DEFAULT_ONLY);
      return (list.size() > 0);
    }
  }

}
