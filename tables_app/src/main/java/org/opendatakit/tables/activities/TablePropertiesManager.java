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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.*;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;
import org.opendatakit.activities.BasePreferenceActivity;
import org.opendatakit.activities.IAppAwareActivity;
import org.opendatakit.consts.IntentConsts;
import org.opendatakit.data.TableViewType;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.LocalKeyValueStoreConstants;
import org.opendatakit.database.data.ColumnDefinition;
import org.opendatakit.database.data.OrderedColumns;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.listener.DatabaseConnectionListener;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.properties.CommonToolProperties;
import org.opendatakit.properties.PropertiesSingleton;
import org.opendatakit.tables.R;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.utilities.LocalizationUtils;
import org.opendatakit.utilities.ODKFileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * An activity for managing a table's properties.
 *
 * @author hkworden@gmail.com
 * @author sudar.sam@gmail.com
 */
public class TablePropertiesManager extends BasePreferenceActivity
    implements DatabaseConnectionListener, IAppAwareActivity {

  private static final String TAG = "TablePropertiesManager";

  // these ints are used when selecting/changing the view files
  private static final int RC_DETAIL_VIEW_FILE = 0;
  private static final int RC_LIST_VIEW_FILE = 1;
  private static final int RC_MAP_LIST_VIEW_FILE = 2;

  private String appName;
  private String tableId;
  private OrderedColumns orderedDefns;

  // private TableProperties tp;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    appName = getIntent().getStringExtra(IntentConsts.INTENT_KEY_APP_NAME);
    if (appName == null) {
      appName = TableFileUtils.getDefaultAppName();
    }
    tableId = getIntent().getStringExtra(IntentConsts.INTENT_KEY_TABLE_ID);
    if (tableId == null) {
      throw new RuntimeException("Table ID (" + tableId + ") is invalid.");
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    ((Tables) getApplication()).establishDoNotFireDatabaseConnectionListener(this);
  }

  @Override
  protected void onPostResume() {
    super.onPostResume();
    ((Tables) getApplication()).fireDatabaseConnectionListener();
  }

  @Override
  public String getAppName() {
    return appName;
  }

  private void createFromDatabase() {
    PropertiesSingleton props = CommonToolProperties.get(Tables.getInstance(), appName);
    String userSelectedDefaultLocale = props.getUserSelectedDefaultLocale();

    UserDbInterface dbInterface = Tables.getInstance().getDatabase();
    String localizedDisplayName;
    DbHandle db = null;
    try {
      db = dbInterface.openDatabase(appName);
      localizedDisplayName = TableUtil.get()
          .getLocalizedDisplayName(userSelectedDefaultLocale, dbInterface, appName, db, tableId);
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(appName).printStackTrace(e);
      Toast.makeText(this, "Unable to access database", Toast.LENGTH_LONG).show();
      PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
      setPreferenceScreen(root);
      return;
    } finally {
      if (db != null) {
        try {
          dbInterface.closeDatabase(appName, db);
        } catch (ServicesAvailabilityException e) {
          WebLogger.getLogger(appName).printStackTrace(e);
          Toast.makeText(this, "Unable to close database", Toast.LENGTH_LONG).show();
        }
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

    UserDbInterface dbInterface = Tables.getInstance().getDatabase();

    String rawDisplayName;
    DbHandle db = null;
    try {
      db = dbInterface.openDatabase(appName);
      rawDisplayName = TableUtil.get().getRawDisplayName(dbInterface, appName, db, tableId);
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(appName).printStackTrace(e);
      Toast.makeText(this, "Unable to access database", Toast.LENGTH_LONG).show();
      setPreferenceScreen(root);
      return;
    } finally {
      if (db != null) {
        try {
          dbInterface.closeDatabase(appName, db);
        } catch (ServicesAvailabilityException e) {
          WebLogger.getLogger(appName).printStackTrace(e);
          Toast.makeText(this, "Unable to close database", Toast.LENGTH_LONG).show();
        }
      }
    }

    EditTextPreference dnPref = new EditTextPreference(this);
    dnPref.setTitle(getString(R.string.table_display_name));
    dnPref.setDialogTitle(getString(R.string.change_table_display_name));
    dnPref.setText(rawDisplayName);
    dnPref.setSummary(rawDisplayName);
    dnPref.setOnPreferenceChangeListener(new TableDisplayNameChangeListener());
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

    UserDbInterface dbInterface = Tables.getInstance().getDatabase();

    TableViewType type;
    DbHandle db = null;
    try {
      db = dbInterface.openDatabase(appName);
      type = TableUtil.get().getDefaultViewType(dbInterface, appName, db, tableId);

      viewTypePref.setValue(type.name());
      // TODO: currently throwing an error i think
      // viewTypePref.setSummary(LanguageUtil.getViewTypeLabel(
      // settings.getViewType()));
      viewTypePref.setSummary(type.name());
      viewTypePref.setOnPreferenceChangeListener(new DefaultViewTypeChangeListener());
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

      TableUtil.MapViewColorRuleInfo info = TableUtil.get()
          .getMapListViewColorRuleInfo(dbInterface, getAppName(), db, tableId);

      ColumnDefinition colorColumn = null;
      // If the color rule type is columns, find the column that it identifies.
      // If that column cannot be found, then reset to color type none.
      //      if (info.colorType.equals(LocalKeyValueStoreConstants.Map.COLOR_TYPE_COLUMN)) {
      //        try {
      //          colorColumn = orderedDefns.find(info.colorElementKey);
      //        } catch (IllegalArgumentException e) {
      //          // no-op
      //        }
      //        if (colorColumn == null) {
      //          info = new TableUtil.MapViewColorRuleInfo(LocalKeyValueStoreConstants.Map.COLOR_TYPE_NONE, null);
      //          TableUtil.get().setMapListViewColorRuleInfo(Tables.getInstance(), appName, db, tableId, info);
      //        }
      //      }

      // Color Options Preference!
      ListPreference colorRulePref = new ListPreference(this);
      colorRulePref.setTitle("Color Rule for Graph and Map Markers");
      colorRulePref.setDialogTitle("Change which color rule markers adhere to.");
      String[] colorRuleTypes = { LocalKeyValueStoreConstants.Map.COLOR_TYPE_NONE,
          LocalKeyValueStoreConstants.Map.COLOR_TYPE_TABLE,
          LocalKeyValueStoreConstants.Map.COLOR_TYPE_STATUS };
      colorRulePref.setEntryValues(colorRuleTypes);
      colorRulePref.setEntries(colorRuleTypes);
      colorRulePref.setValue(info.colorType);
      colorRulePref.setSummary(info.colorType);

      // This functionality needs to be revisited!!
      //      colorRulePref.setOnPreferenceChangeListener(new ColorRuleTypeChangeListener(
      //              (colorColumn == null) ? orderedDefns.getColumnDefinitions().get(0) : colorColumn));
      prefCat.addPreference(colorRulePref);

      // This functionality will need to be revisited!!
      //      if ( colorColumn != null ) {
      //
      //        TableUtil.TableColumns tc = TableUtil.get().getTableColumns(Tables.getInstance(), appName, db, tableId);
      //
      //        ArrayList<String> colorColElementKeys = new ArrayList<String>(tc.orderedDefns.getRetentionColumnNames());
      //        ArrayList<String> colorColDisplayNames = new ArrayList<String>();
      //        for (String elementKey : colorColElementKeys) {
      //          String localizedDisplayName = tc.localizedDisplayNames.get(elementKey);
      //          colorColDisplayNames.add(localizedDisplayName);
      //        }
      //
      //        String localizedDisplayName;
      //        localizedDisplayName = tc.localizedDisplayNames.get(colorColumn.getElementKey());
      //
      //        ListPreference colorColumnPref = new ListPreference(this);
      //        colorColumnPref.setTitle("Color Rule Column");
      //        colorColumnPref.setDialogTitle("Change the column that applies the color rule.");
      //        colorColumnPref.setEntryValues(colorColElementKeys.toArray(new String[colorColElementKeys
      //                .size()]));
      //        colorColumnPref.setEntries(colorColDisplayNames.toArray(new String[colorColDisplayNames
      //                .size()]));
      //        colorColumnPref.setValue(colorColumn.getElementKey());
      //        colorColumnPref.setSummary(localizedDisplayName);
      //        colorColumnPref.setOnPreferenceChangeListener(new ColorRuleColumnChangeListener());
      //        prefCat.addPreference(colorColumnPref);
      //      }
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(appName).printStackTrace(e);
      Toast.makeText(this, "Unable to access database", Toast.LENGTH_LONG).show();
    } finally {
      if (db != null) {
        try {
          dbInterface.closeDatabase(appName, db);
        } catch (ServicesAvailabilityException e) {
          WebLogger.getLogger(appName).printStackTrace(e);
          Toast.makeText(this, "Unable to close database", Toast.LENGTH_LONG).show();
        }
      }
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

  private void addMapViewPreferences(PreferenceCategory prefCat) {
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_CANCELED) {
      return;
    }
    UserDbInterface dbInterface = Tables.getInstance().getDatabase();
    Uri uri;
    String filename;
    String relativePath;
    switch (requestCode) {
    case RC_DETAIL_VIEW_FILE:
      uri = data.getData();
      filename = uri.getPath();
      // We need to get the relative path under the app name.
      relativePath = getRelativePathOfFile(filename);
      try {
        TableUtil.get().atomicSetDetailViewFilename(dbInterface, appName, tableId, relativePath);
      } catch (ServicesAvailabilityException e) {
        Toast.makeText(getParent(), "Unable to set Detail View Filename", Toast.LENGTH_LONG).show();
      }
      init();
      break;
    case RC_LIST_VIEW_FILE:
      uri = data.getData();
      filename = uri.getPath();
      // We need to get the relative path under the app name.
      relativePath = getRelativePathOfFile(filename);
      try {
        TableUtil.get().atomicSetListViewFilename(dbInterface, appName, tableId, relativePath);
      } catch (ServicesAvailabilityException e) {
        Toast.makeText(getParent(), "Unable to set List View Filename", Toast.LENGTH_LONG).show();
      }
      init();
      break;
    case RC_MAP_LIST_VIEW_FILE:
      uri = data.getData();
      filename = uri.getPath();
      // We need to get the relative path under the app name.
      relativePath = getRelativePathOfFile(filename);
      try {
        TableUtil.get().atomicSetMapListViewFilename(dbInterface, appName, tableId, relativePath);
      } catch (ServicesAvailabilityException e) {
        Toast.makeText(getParent(), "Unable to set Map List View Filename", Toast.LENGTH_LONG)
            .show();
      }
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

  @Override
  public void databaseAvailable() {
    createFromDatabase();
  }

  // This functionality should be revisited!!
  //  private final class ColorRuleColumnChangeListener implements OnPreferenceChangeListener {
  //    @Override
  //    public boolean onPreferenceChange(Preference preference, Object newValue) {
  //      DbHandle db = null;
  //      try {
  //        db = Tables.getInstance().getDatabase().openDatabase(appName);
  //        TableUtil.get().setMapListViewColorRuleInfo(Tables.getInstance(), getAppName(), db, tableId,
  //                new TableUtil.MapViewColorRuleInfo(LocalKeyValueStoreConstants.Map.COLOR_TYPE_COLUMN, (String) newValue));
  //      } catch (ServicesAvailabilityException e) {
  //        WebLogger.getLogger(appName).printStackTrace(e);
  //        Toast.makeText(TablePropertiesManager.this, "Error while saving color rule column selection", Toast.LENGTH_LONG).show();
  //      } finally {
  //        if (db != null) {
  //          try {
  //            Tables.getInstance().getDatabase().closeDatabase(appName, db);
  //          } catch (ServicesAvailabilityException e) {
  //            WebLogger.getLogger(appName).printStackTrace(e);
  //            Toast.makeText(TablePropertiesManager.this, "Unable to close database", Toast.LENGTH_LONG).show();
  //          }
  //        }
  //      }
  //      init();
  //      return false;
  //    }
  //  }

  //  private final class ColorRuleTypeChangeListener implements OnPreferenceChangeListener {
  //
  //    private String defaultColorElementKey;
  //
  //    public ColorRuleTypeChangeListener(ColumnDefinition columnDefinition) {
  //      defaultColorElementKey = columnDefinition.getElementKey();
  //    }
  //
  //    @Override
  //    public boolean onPreferenceChange(Preference preference, Object newValue) {
  //      DbHandle db = null;
  //      try {
  //        String colorType = (String) newValue;
  //        TableUtil.MapViewColorRuleInfo info = null;
  //        if ( colorType.equals(LocalKeyValueStoreConstants.Map.COLOR_TYPE_COLUMN) ) {
  //          info = new TableUtil.MapViewColorRuleInfo(colorType, defaultColorElementKey);
  //        } else {
  //          info = new TableUtil.MapViewColorRuleInfo(colorType, null);
  //        }
  //        db = Tables.getInstance().getDatabase().openDatabase(appName);
  //        TableUtil.get().setMapListViewColorRuleInfo(Tables.getInstance(), appName, db, tableId, info);
  //      } catch (ServicesAvailabilityException e) {
  //        WebLogger.getLogger(appName).printStackTrace(e);
  //        Toast.makeText(TablePropertiesManager.this, "Error while saving color rule type selection", Toast.LENGTH_LONG).show();
  //      } finally {
  //        if (db != null) {
  //          try {
  //            Tables.getInstance().getDatabase().closeDatabase(appName, db);
  //          } catch (ServicesAvailabilityException e) {
  //            WebLogger.getLogger(appName).printStackTrace(e);
  //            Toast.makeText(TablePropertiesManager.this, "Unable to close database", Toast.LENGTH_LONG).show();
  //          }
  //        }
  //      }
  //      init();
  //      return false;
  //    }
  //  }

  @Override
  public void databaseUnavailable() {
    // TODO Auto-generated method stub

  }

  private final class TableDisplayNameChangeListener implements OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      String localizedDisplayName;
      PropertiesSingleton props = CommonToolProperties.get(getApplicationContext(), getAppName());
      try {
        localizedDisplayName = LocalizationUtils
            .getLocalizedDisplayName(appName, tableId, props.getUserSelectedDefaultLocale(),
                TableUtil.get()
                    .atomicSetRawDisplayName(Tables.getInstance().getDatabase(), appName, tableId,
                        (String) newValue));
      } catch (ServicesAvailabilityException e) {
        Toast.makeText(getParent(), "Unable to change display name", Toast.LENGTH_LONG).show();
        init();
        return false;
      }
      setTitle(getString(R.string.table_manager_title, localizedDisplayName));
      init();
      return false;
    }
  }

  private final class DefaultViewTypeChangeListener implements OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      try {
        TableUtil.get()
            .atomicSetDefaultViewType(Tables.getInstance().getDatabase(), appName, tableId,
                TableViewType.valueOf((String) newValue));
      } catch (ServicesAvailabilityException e) {
        Toast.makeText(getParent(), "Unable to change default view type", Toast.LENGTH_LONG).show();
      }
      init();
      return false;
    }
  }

  /**
   * This preference allows the user to select a file from their SD card. If the
   * user does not have a file picker installed on their phone, then a toast
   * will indicate so.
   *
   * @author Chris Gelon (cgelon)
   */
  private class FileSelectorPreference extends EditTextPreference {
    /**
     * Indicates which preference we are using the selector for.
     */
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
            WebLogger.getLogger(appName).printStackTrace(e);
            Toast.makeText(TablePropertiesManager.this,
                getString(R.string.file_not_found, fullFile.getAbsolutePath()), Toast.LENGTH_LONG)
                .show();
          }
        }
        try {
          startActivityForResult(intent, mRequestCode);
        } catch (ActivityNotFoundException e) {
          WebLogger.getLogger(appName).printStackTrace(e);
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
      List<ResolveInfo> list = packageManager
          .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
      return (list.size() > 0);
    }
  }

}
