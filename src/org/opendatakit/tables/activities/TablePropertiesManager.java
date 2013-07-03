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

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.ColorRuleGroup;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.ColumnType;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.KeyValueStoreManager;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableType;
import org.opendatakit.tables.data.TableViewType;
import org.opendatakit.tables.fragments.TableMapFragment;
import org.opendatakit.tables.preferences.EditFormDialogPreference;
import org.opendatakit.tables.utils.LanguageUtil;
import org.opendatakit.tables.utils.SecurityUtil;
import org.opendatakit.tables.utils.ShortcutUtil;
import org.opendatakit.tables.views.webkits.CustomDetailView;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
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

  public static final String INTENT_KEY_TABLE_ID = "tableId";

  // these ints are used when selecting/changing the view files
  private static final int RC_DETAIL_VIEW_FILE = 0;
  private static final int RC_LIST_VIEW_FILE = 1;
  private static final int RC_MAP_LIST_VIEW_FILE = 2;

  private enum ViewPreferenceType {
    OVERVIEW_VIEW, COLLECTION_VIEW,
    // At the point where you could specify different files for an overview
    // and a collection list view, there were only the two constants
    // OVERVIEW_VIEW and COLLECTION_VIEW in this enum. I am changing this
    // to allow for only an unspecified view. The code seems to be checking
    // whether or not something is an overview or collection somehow and
    // storing the value in controller. I'm going to add a case for
    // AUTO_GENERATED to try and reflect that.
    AUTO_GENERATED
  }

  private DbHelper dbh;
  private TableProperties tp;

  private AlertDialog revertDialog;
  private AlertDialog saveAsDefaultDialog;
  private AlertDialog defaultToServerDialog;
  private AlertDialog serverToDefaultDialog;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String tableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
    if (tableId == null) {
      throw new RuntimeException("Table ID (" + tableId + ") is invalid.");
    }
    dbh = DbHelper.getDbHelper(this);
    tp = TableProperties.getTablePropertiesForTable(dbh, tableId, KeyValueStore.Type.ACTIVE);
    setTitle(getString(R.string.table_manager_title, tp.getDisplayName()));
    init();
  }

  private void init() {

    // TODO move this into the actual preference somehow.
    AlertDialog.Builder builder = new AlertDialog.Builder(TablePropertiesManager.this);
    builder.setMessage(getString(R.string.revert_warning));
    builder.setCancelable(true);
    builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
        KeyValueStore defaultKVS = kvsm.getStoreForTable(tp.getTableId(),
            KeyValueStore.Type.DEFAULT);
        if (!defaultKVS.entriesExist(db)) {
          AlertDialog.Builder noDefaultsDialog = new AlertDialog.Builder(
              TablePropertiesManager.this);
          noDefaultsDialog.setMessage(getString(R.string.no_default_no_changes));
          noDefaultsDialog.setNeutralButton(getString(R.string.ok), null);
          noDefaultsDialog.show();
        } else {
          kvsm.copyDefaultToActiveForTable(tp.getTableId());
        }
      }
    });
    builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        dialog.cancel();
      }
    });
    revertDialog = builder.create();

    builder = new AlertDialog.Builder(TablePropertiesManager.this);
    builder.setMessage(getString(R.string.are_you_sure_save_default_settings));
    builder.setCancelable(true);
    builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
        KeyValueStore activeKVS = kvsm.getStoreForTable(tp.getTableId(), KeyValueStore.Type.ACTIVE);

        kvsm.setCurrentAsDefaultPropertiesForTable(tp.getTableId());
      }
    });
    builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        dialog.cancel();
      }
    });
    saveAsDefaultDialog = builder.create();

    builder = new AlertDialog.Builder(TablePropertiesManager.this);
    builder.setMessage(getString(R.string.are_you_sure_copy_default_settings_to_server_settings));
    builder.setCancelable(true);
    builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
        KeyValueStore activeKVS = kvsm.getStoreForTable(tp.getTableId(), KeyValueStore.Type.ACTIVE);

        kvsm.copyDefaultToServerForTable(tp.getTableId());
      }
    });
    builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        dialog.cancel();
      }
    });
    defaultToServerDialog = builder.create();

    builder = new AlertDialog.Builder(TablePropertiesManager.this);
    builder
        .setMessage(getString(R.string.are_you_sure_merge_server_settings_into_default_settings));
    builder.setCancelable(true);
    builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        try {
          KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
          KeyValueStore activeKVS = kvsm.getStoreForTable(tp.getTableId(),
              KeyValueStore.Type.ACTIVE);

          kvsm.mergeServerToDefaultForTable(tp.getTableId());
        } finally {
          // TODO: fix the when to close problem
          // if ( db != null ) {
          // db.close();
          // }
        }
      }
    });
    builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        dialog.cancel();
      }
    });
    serverToDefaultDialog = builder.create();

    PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

    // general category

    PreferenceCategory genCat = new PreferenceCategory(this);
    root.addPreference(genCat);
    genCat.setTitle(getString(R.string.general_settings));

    EditTextPreference dnPref = new EditTextPreference(this);
    dnPref.setTitle(getString(R.string.table_display_name));
    dnPref.setDialogTitle(getString(R.string.change_table_display_name));
    dnPref.setText(tp.getDisplayName());
    dnPref.setSummary(tp.getDisplayName());
    dnPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        tp.setDisplayName((String) newValue);
        setTitle(getString(R.string.table_manager_title, tp.getDisplayName()));
        init();
        return false;
      }
    });
    genCat.addPreference(dnPref);

    List<String> columnOrder = tp.getColumnOrder();
    boolean canBeAccessTable = SecurityUtil.couldBeSecurityTable(columnOrder);
    boolean canBeShortcutTable =
        ShortcutUtil.couldBeShortcutTable(columnOrder);
    int tableTypeCount =
        1 + (canBeAccessTable ? 1 : 0) + (canBeShortcutTable ? 1 : 0);
    String[] tableTypeIds = new String[tableTypeCount];
    String[] tableTypeNames = new String[tableTypeCount];
    tableTypeIds[0] = TableType.data.name();
    tableTypeNames[0] = LanguageUtil.getTableTypeLabel(this, TableType.data);
    if (canBeAccessTable) {
      tableTypeIds[1] = TableType.security.name();
      tableTypeNames[1] = LanguageUtil.getTableTypeLabel(this, TableType.security);
    }
    if (canBeShortcutTable) {
      int index = canBeAccessTable ? 2 : 1;
      tableTypeIds[index] = TableType.shortcut.name();
      tableTypeNames[index] = LanguageUtil.getTableTypeLabel(this, TableType.shortcut);
    }
    ListPreference tableTypePref = new ListPreference(this);
    tableTypePref.setTitle(getString(R.string.table_type));
    tableTypePref.setDialogTitle(getString(R.string.change_table_type));
    tableTypePref.setEntryValues(tableTypeIds);
    tableTypePref.setEntries(tableTypeNames);
    tableTypePref.setValue(tp.getTableType().name());
    tableTypePref.setSummary(LanguageUtil.getTableTypeLabel(this, tp.getTableType()));
    tableTypePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        tp.setTableType(TableType.valueOf((String) newValue));
        init();
        return false;
      }
    });
    genCat.addPreference(tableTypePref);

    // display category

    PreferenceCategory displayCat = new PreferenceCategory(this);
    root.addPreference(displayCat);
    displayCat.setTitle(getString(R.string.display_settings));
    addViewPreferences(displayCat);

    FileSelectorPreference detailViewPref = new FileSelectorPreference(this, RC_DETAIL_VIEW_FILE);
    detailViewPref.setTitle(getString(R.string.detail_view_file));
    detailViewPref.setDialogTitle(getString(R.string.change_detail_view_file));
    final KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(CustomDetailView.KVS_PARTITION);
    String detailViewFilename = kvsh.getString(CustomDetailView.KEY_FILENAME);
    detailViewPref.setText(detailViewFilename);
    detailViewPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        // tp.setDetailViewFilename((String) newValue);
        kvsh.setString(CustomDetailView.KEY_FILENAME, (String) newValue);
        init();
        return false;
      }
    });
    displayCat.addPreference(detailViewPref);

    // Now let's add the pref for the Form.
    EditFormDialogPreference formPref = new EditFormDialogPreference(this, tp);
    displayCat.addPreference(formPref);
    formPref.setTitle(getString(R.string.edit_default_form));
    formPref.setDialogTitle(getString(R.string.edit_default_form));

    Preference rowColorRulePrefs = new Preference(this);
    rowColorRulePrefs.setTitle(getString(R.string.edit_table_color_rules));
    rowColorRulePrefs.setOnPreferenceClickListener(new OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent rowColorRuleManagerIntent = new Intent(TablePropertiesManager.this,
            ColorRuleManagerActivity.class);
        rowColorRuleManagerIntent.putExtra(ColorRuleManagerActivity.INTENT_KEY_TABLE_ID,
            tp.getTableId());
        rowColorRuleManagerIntent.putExtra(ColorRuleManagerActivity.INTENT_KEY_RULE_GROUP_TYPE,
            ColorRuleGroup.Type.TABLE.name());
        startActivity(rowColorRuleManagerIntent);
        return true;
      }

    });
    displayCat.addPreference(rowColorRulePrefs);

    Preference statusColumnColorRulePref = new Preference(this);
    statusColumnColorRulePref.setTitle(getString(R.string.edit_status_column_color_rules));
    statusColumnColorRulePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent rowColorRuleManagerIntent = new Intent(TablePropertiesManager.this,
            ColorRuleManagerActivity.class);
        rowColorRuleManagerIntent.putExtra(ColorRuleManagerActivity.INTENT_KEY_TABLE_ID,
            tp.getTableId());
        rowColorRuleManagerIntent.putExtra(ColorRuleManagerActivity.INTENT_KEY_RULE_GROUP_TYPE,
            ColorRuleGroup.Type.STATUS_COLUMN.name());
        startActivity(rowColorRuleManagerIntent);
        return true;
      }

    });
    displayCat.addPreference(statusColumnColorRulePref);

    // security category

    PreferenceCategory securityCat = new PreferenceCategory(this);
    root.addPreference(securityCat);
    securityCat.setTitle(getString(R.string.access_control_settings));

    /*
     * TODO: fix this -- there should probably be three access control tables
     * for an entire 'application' (i.e., shared across all tables so as to
     * centralize access control management).
     *
     * The schema for the 2 tables should probably be:
     *
     * User Access Control Table: target_table_id null -- table for which access
     * control applies access_control_type not null -- create, read, modify,
     * delete access user_id null -- e-mail or phone number (sms) (or null for
     * everyone)
     *
     * Group Access Control Table: target_table_id null -- table for which
     * access control applies access_control_type not null -- create, read,
     * modify, delete access group_id not null -- group id from assignment table
     *
     * User-to-Group Assignment Table: user_id not null -- e-mail or phone
     * number (sms) group_id not null -- group id, as referenced in Group Access
     * Control Table.
     */
    TableProperties[] accessTps = TableProperties.getTablePropertiesForSecurityTables(dbh,
        KeyValueStore.Type.ACTIVE);
    int accessTableCount = (tp.getTableType() == TableType.security) ? (accessTps.length)
        : accessTps.length + 1;
    TableProperties readTp = null;
    TableProperties writeTp = null;
    String[] accessTableIds = new String[accessTableCount];
    accessTableIds[0] = "-1";
    String[] accessTableNames = new String[accessTableCount];
    accessTableNames[0] = getString(R.string.none);
    int index = 1;
    for (TableProperties accessTp : accessTps) {
      if (accessTp.getTableId().equals(tp.getTableId())) {
        continue;
      }
      // TODO: fix this to handle access correctly. got altered during
      // schema update.
      // if ((tp.getReadSecurityTableId() != null) &&
      // accessTp.getTableId().equals(
      // tp.getReadSecurityTableId())) {
      // readTp = accessTp;
      // }
      // if ((tp.getWriteSecurityTableId() != null) &&
      // accessTp.getTableId().equals(
      // tp.getWriteSecurityTableId())) {
      // writeTp = accessTp;
      // }
      accessTableIds[index] = accessTp.getTableId();
      accessTableNames[index] = accessTp.getDisplayName();
      index++;
    }

    ListPreference readTablePref = new ListPreference(this);
    readTablePref.setTitle("Read Access Table");
    readTablePref.setDialogTitle("Change Read Access Table");
    readTablePref.setEntryValues(accessTableIds);
    readTablePref.setEntries(accessTableNames);
    if (readTp == null) {
      readTablePref.setValue("-1");
      readTablePref.setSummary(getString(R.string.none));
    } else {
      readTablePref.setValue(readTp.getTableId());
      readTablePref.setSummary(readTp.getDisplayName());
    }
    readTablePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "access stuff and .onPreferenceChange unimplented");
        // TODO: fix this, currently does nothing
        // tp.setReadSecurityTableId((String) newValue);
        init();
        return false;
      }
    });
    securityCat.addPreference(readTablePref);

    ListPreference writeTablePref = new ListPreference(this);
    writeTablePref.setTitle("Write Access Table");
    writeTablePref.setDialogTitle("Change Write Access Table");
    writeTablePref.setEntryValues(accessTableIds);
    writeTablePref.setEntries(accessTableNames);
    if (writeTp == null) {
      writeTablePref.setValue("-1");
      writeTablePref.setSummary(getString(R.string.none));
    } else {
      writeTablePref.setValue(writeTp.getTableId());
      writeTablePref.setSummary(writeTp.getDisplayName());
    }
    writeTablePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, ".onPreferenceChange unimplented");
        // TODO: fix this, currently does nothing
        // tp.setWriteSecurityTableId((String) newValue);
        init();
        return false;
      }
    });
    securityCat.addPreference(writeTablePref);

    // the managing of default properties

    // under development!
    PreferenceCategory defaultsCat = new PreferenceCategory(this);
    root.addPreference(defaultsCat);
    defaultsCat.setTitle(getString(R.string.manage_table_property_sets));

    // the actual entry that has the option above.
    Preference revertPref = new Preference(this);
    revertPref.setTitle(getString(R.string.restore_defaults));
    revertPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        revertDialog.show();
        return true;
      }
    });
    defaultsCat.addPreference(revertPref);

    Preference saveAsDefaultPref = new Preference(this);
    saveAsDefaultPref.setTitle(getString(R.string.save_to_defaults));
    saveAsDefaultPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        saveAsDefaultDialog.show();
        return true;
      }
    });
    defaultsCat.addPreference(saveAsDefaultPref);

    Preference defaultToServerPref = new Preference(this);
    defaultToServerPref.setTitle(getString(R.string.copy_defaults_to_server));
    defaultToServerPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        defaultToServerDialog.show();
        return true;
      }
    });
    defaultsCat.addPreference(defaultToServerPref);

    Preference serverToDefaultPref = new Preference(this);
    serverToDefaultPref.setTitle(getString(R.string.merge_server_into_defaults));
    serverToDefaultPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        serverToDefaultDialog.show();
        return true;
      }
    });
    defaultsCat.addPreference(serverToDefaultPref);

    setPreferenceScreen(root);
  }

  private void addViewPreferences(PreferenceCategory prefCat) {

    final List<ColumnProperties> numberCols = new ArrayList<ColumnProperties>();
    final List<ColumnProperties> locationCols = new ArrayList<ColumnProperties>();
    final List<ColumnProperties> dateCols = new ArrayList<ColumnProperties>();
    for (ColumnProperties cp : tp.getColumns().values()) {
      if (cp.getColumnType() == ColumnType.NUMBER || cp.getColumnType() == ColumnType.INTEGER) {
        numberCols.add(cp);
      } else if (cp.getColumnType() == ColumnType.GEOPOINT) {
        locationCols.add(cp);
      } else if (cp.getColumnType() == ColumnType.DATE || cp.getColumnType() == ColumnType.DATETIME
          || cp.getColumnType() == ColumnType.TIME) {
        dateCols.add(cp);
      } else if (TableProperties.isLatitudeColumn(cp) || TableProperties.isLongitudeColumn(cp)) {
        locationCols.add(cp);
      }
    }

    // int[] viewTypes = settings.getPossibleViewTypes();
    TableViewType[] viewTypes = tp.getPossibleViewTypes();
    String[] viewTypeIds = new String[viewTypes.length];
    String[] viewTypeNames = new String[viewTypes.length];
    // for (int i = 0; i < viewTypes.length; i++) {
    // int viewType = viewTypes[i];
    // viewTypeIds[i] = String.valueOf(viewType);
    // viewTypeNames[i] = LanguageUtil.getViewTypeLabel(viewType);
    // }
    // so now we need to populate the actual menu with the thing to save
    // and to the human-readable labels.
    for (int i = 0; i < viewTypes.length; i++) {
      viewTypeIds[i] = viewTypes[i].name();
      viewTypeNames[i] = viewTypes[i].name();
    }
    ListPreference viewTypePref = new ListPreference(this);
    viewTypePref.setTitle(getString(R.string.view_type));
    viewTypePref.setDialogTitle(getString(R.string.change_view_type));
    viewTypePref.setEntryValues(viewTypeIds);
    viewTypePref.setEntries(viewTypeNames);
    // viewTypePref.setValue(String.valueOf(settings.getViewType()));
    viewTypePref.setValue(tp.getCurrentViewType().name());
    // TODO: currently throwing an error i think
    // viewTypePref.setSummary(LanguageUtil.getViewTypeLabel(
    // settings.getViewType()));
    viewTypePref.setSummary(tp.getCurrentViewType().name());
    viewTypePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        // int viewType = Integer.valueOf((String) newValue);
        // settings.setViewType(viewType);
        tp.setCurrentViewType(TableViewType.valueOf((String) newValue));
        init();
        return false;
      }
    });
    prefCat.addPreference(viewTypePref);

    switch (tp.getCurrentViewType()) {
    case List: {
      Preference listViewPrefs = new Preference(this);
      listViewPrefs.setTitle(getString(R.string.list_view_manager));
      listViewPrefs.setOnPreferenceClickListener(new OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference preference) {
          Intent selectListViewIntent = new Intent(TablePropertiesManager.this,
              ListViewManager.class);
          selectListViewIntent.putExtra(ListViewManager.INTENT_KEY_TABLE_ID, tp.getTableId());
          startActivity(selectListViewIntent);
          return true;
        }

      });
      prefCat.addPreference(listViewPrefs);
    }
      break;
    case Graph:
      Log.d(TAG, "Graph view type was selected");
      break;

    case Map:
      // Grab the key value store helper from the table activity.
      final KeyValueStoreHelper kvsHelper = tp
          .getKeyValueStoreHelper(TableMapFragment.KVS_PARTITION);

      // Try to find the latitude column in the store.
      ColumnProperties latCol = tp.getColumnByElementKey(kvsHelper
          .getString(TableMapFragment.KEY_MAP_LAT_COL));
      // If there is none, take the first of the location columns and set it.
      if (latCol == null) {
        for (ColumnProperties column : locationCols) {
          if (TableProperties.isLatitudeColumn(column)) {
            latCol = column;
            break;
          }
        }
        if (latCol == null) {
          latCol = locationCols.get(0);
        }
        kvsHelper.setString(TableMapFragment.KEY_MAP_LAT_COL, latCol.getElementKey());
      }

      // Try to find the longitude column in the store.
      ColumnProperties longCol = tp.getColumnByElementKey(kvsHelper
          .getString(TableMapFragment.KEY_MAP_LONG_COL));
      // If there is none, take the first of the location columns and set it.
      if (longCol == null) {
        for (ColumnProperties column : locationCols) {
          if (TableProperties.isLongitudeColumn(column)) {
            longCol = column;
            break;
          }
        }
        if (longCol == null) {
          longCol = locationCols.get(0);
        }
        kvsHelper.setString(TableMapFragment.KEY_MAP_LONG_COL, longCol.getElementKey());
      }

      // Add every location column to the list.
      String[] locColDisplayNames = new String[locationCols.size()];
      String[] locColElementKeys = new String[locationCols.size()];
      for (int i = 0; i < locationCols.size(); i++) {
        locColDisplayNames[i] = locationCols.get(i).getDisplayName();
        locColElementKeys[i] = locationCols.get(i).getElementKey();
      }

      // Lat Preference!
      ListPreference mapLatPref = new ListPreference(this);
      mapLatPref.setTitle(getString(R.string.map_view_latitude_column));
      mapLatPref.setDialogTitle(getString(R.string.change_map_view_latitude_column));
      mapLatPref.setEntryValues(locColElementKeys);
      mapLatPref.setEntries(locColDisplayNames);
      mapLatPref.setValue(latCol.getElementKey());
      mapLatPref.setSummary(latCol.getDisplayName());
      mapLatPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
          kvsHelper.setString(TableMapFragment.KEY_MAP_LAT_COL, (String) newValue);
          init();
          return false;
        }
      });
      prefCat.addPreference(mapLatPref);

      // Long Preference!
      ListPreference mapLongPref = new ListPreference(this);
      mapLongPref.setTitle(getString(R.string.map_view_longitude_column));
      mapLongPref.setDialogTitle(getString(R.string.change_map_view_longitude_column));
      mapLongPref.setEntryValues(locColElementKeys);
      mapLongPref.setEntries(locColDisplayNames);
      mapLongPref.setValue(longCol.getElementKey());
      mapLongPref.setSummary(longCol.getDisplayName());
      mapLongPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
          kvsHelper.setString(TableMapFragment.KEY_MAP_LONG_COL, (String) newValue);
          init();
          return false;
        }
      });
      prefCat.addPreference(mapLongPref);

      // ListView Preference!
      FileSelectorPreference listFilePref = new FileSelectorPreference(this, RC_MAP_LIST_VIEW_FILE);
      listFilePref.setTitle(getString(R.string.list_view_file));
      listFilePref.setDialogTitle(getString(R.string.change_map_view_list_view_file));
      String currentFilename = kvsHelper.getString(TableMapFragment.KEY_FILENAME);
      listFilePref.setText(currentFilename);
      prefCat.addPreference(listFilePref);

      // Color Options Preference!
      String colorType = kvsHelper.getString(TableMapFragment.KEY_COLOR_RULE_TYPE);
      if (colorType == null) {
        kvsHelper
            .setString(TableMapFragment.KEY_COLOR_RULE_TYPE, TableMapFragment.COLOR_TYPE_NONE);
        colorType = TableMapFragment.COLOR_TYPE_NONE;
      }
      ListPreference colorRulePref = new ListPreference(this);
      colorRulePref.setTitle("Map View Color Rule");
      colorRulePref.setDialogTitle("Change which color rule markers adhere to.");
      String[] colorRuleTypes = { TableMapFragment.COLOR_TYPE_NONE,
          TableMapFragment.COLOR_TYPE_TABLE, TableMapFragment.COLOR_TYPE_STATUS,
          TableMapFragment.COLOR_TYPE_COLUMN };
      colorRulePref.setEntryValues(colorRuleTypes);
      colorRulePref.setEntries(colorRuleTypes);
      colorRulePref.setValue(colorType);
      colorRulePref.setSummary(colorType);
      colorRulePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
          kvsHelper.setString(TableMapFragment.KEY_COLOR_RULE_TYPE, (String) newValue);
          init();
          return false;
        }
      });
      prefCat.addPreference(colorRulePref);

      // If the color rule type is columns, add the preference to select the
      // column.
      if (colorType.equals(TableMapFragment.COLOR_TYPE_COLUMN)) {
    	int numberOfDisplayColumns = tp.getNumberOfDisplayColumns();
        String[] colorColDisplayNames = new String[numberOfDisplayColumns];
        String[] colorColElementKeys = new String[numberOfDisplayColumns];
        for (int i = 0; i < numberOfDisplayColumns; i++) {
          ColumnProperties cp = tp.getColumnByIndex(i);
          colorColDisplayNames[i] = cp.getDisplayName();
          colorColElementKeys[i] = cp.getElementKey();
        }

        ColumnProperties colorColumn = tp.getColumnByElementKey(kvsHelper
            .getString(TableMapFragment.KEY_COLOR_RULE_COLUMN));
        if (colorColumn == null) {
          kvsHelper.setString(TableMapFragment.KEY_COLOR_RULE_COLUMN,
              tp.getColumnByIndex(0).getElementKey());
          colorColumn = tp.getColumnByIndex(0);
        }

        ListPreference colorColumnPref = new ListPreference(this);
        colorColumnPref.setTitle("Color Rule Column");
        colorColumnPref.setDialogTitle("Change the column that applies the color rule.");
        colorColumnPref.setEntryValues(colorColElementKeys);
        colorColumnPref.setEntries(colorColDisplayNames);
        colorColumnPref.setValue(colorColumn.getElementKey());
        colorColumnPref.setSummary(colorColumn.getDisplayName());
        colorColumnPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
          @Override
          public boolean onPreferenceChange(Preference preference, Object newValue) {
            kvsHelper.setString(TableMapFragment.KEY_COLOR_RULE_COLUMN, (String) newValue);
            init();
            return false;
          }
        });
        prefCat.addPreference(colorColumnPref);
      }
      break;
    default:
      Log.e(TAG, "unrecognized view type: " + tp.getCurrentViewType()
          + ", resetting to spreadsheet");
      tp.setCurrentViewType(TableViewType.Spreadsheet);

    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_CANCELED) {
      return;
    }
    KeyValueStoreHelper kvsh;
    Uri uri;
    String filename;
    switch (requestCode) {
    case RC_DETAIL_VIEW_FILE:
      uri = data.getData();
      filename = uri.getPath();
      kvsh = tp.getKeyValueStoreHelper(CustomDetailView.KVS_PARTITION);
      kvsh.setString(CustomDetailView.KEY_FILENAME, filename);
      // tp.setDetailViewFilename(filename);
      init();
      break;
    case RC_LIST_VIEW_FILE:
      uri = data.getData();
      filename = uri.getPath();
      // This set it in the main partition. We actually want to set it in the
      // other partition for now.
      // kvsh =
      // tp.getKeyValueStoreHelper(ListDisplayActivity.KVS_PARTITION);
      // kvsh.setString(
      // ListDisplayActivity.KEY_FILENAME,
      // filename2);
      // Trying to get the new name to the _VIEWS partition.
      kvsh = tp.getKeyValueStoreHelper(ListDisplayActivity.KVS_PARTITION_VIEWS);
      // Set the name here statically, just to test. Later will want to
      // allow custom naming, checking for redundancy, etc.
      KeyValueHelper aspectHelper = kvsh.getAspectHelper("List View 1");
      aspectHelper.setString(ListDisplayActivity.KEY_FILENAME, filename);

      // TableViewSettings settings = tp.getOverviewViewSettings();
      // settings.setCustomListFilename(filename2);
      init();
      break;
    case RC_MAP_LIST_VIEW_FILE:
      tp.getKeyValueStoreHelper(TableMapFragment.KVS_PARTITION).setString(
          TableMapFragment.KEY_FILENAME, data.getData().getPath());
      init();
    default:
      super.onActivityResult(requestCode, resultCode, data);
    }
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
          intent.setData(Uri.parse("file:///" + getText()));
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

  // private class ConditionalRulerDialogPreference extends Preference {
  //
  // private final Dialog dialog;
  //
  // public ConditionalRulerDialogPreference(int[] values, String[] labels,
  // Map<ColumnProperties, ConditionalRuler> rulerMap) {
  // super(TablePropertiesManager.this);
  // dialog = new ConditionalRulerDialog(values, labels, rulerMap);
  // }
  //
  // @Override
  // protected void onClick() {
  // dialog.show();
  // }
  // }
  // SS: this was commented out as TableViewSettings was removed. So whatever
  // this is used for needs to be redone to use TableProperties rather than
  // tvs.
  // public class ConditionalRulerDialog extends Dialog {
  //
  // private final int[] values;
  // private final String[] labels;
  // private final Map<ColumnProperties, ConditionalRuler> rulerMap;
  // private final ColumnProperties[] columns;
  // private final String[] comparatorLabels;
  // private final String[] columnDisplays;
  // private LinearLayout ruleList;
  //
  // public ConditionalRulerDialog(int[] values, String[] labels,
  // Map<ColumnProperties, ConditionalRuler> rulerMap) {
  // super(TablePropertiesManager.this);
  // this.values = values;
  // this.labels = labels;
  // this.rulerMap = rulerMap;
  // columns = new ColumnProperties[tp.getColumns().length];
  // comparatorLabels = new String[ConditionalRuler.Comparator.COUNT];
  // for (int i = 0; i < comparatorLabels.length; i++) {
  // comparatorLabels[i] =
  // LanguageUtil.getTvsConditionalComparator(i);
  // }
  // columnDisplays = new String[tp.getColumns().length];
  // for (int i = 0; i < tp.getColumns().length; i++) {
  // ColumnProperties cp = tp.getColumns()[i];
  // columns[i] = cp;
  // columnDisplays[i] = cp.getDisplayName();
  // }
  // setContentView(buildView());
  // }
  //
  // private View buildView() {
  // ruleList = new LinearLayout(TablePropertiesManager.this);
  // ruleList.setOrientation(LinearLayout.VERTICAL);
  // String[] comparatorValues =
  // new String[ConditionalRuler.Comparator.COUNT];
  // String[] comparatorDisplays =
  // new String[ConditionalRuler.Comparator.COUNT];
  // for (int i = 0; i < ConditionalRuler.Comparator.COUNT; i++) {
  // comparatorValues[i] = String.valueOf(i);
  // comparatorDisplays[i] =
  // LanguageUtil.getTvsConditionalComparator(i);
  // }
  // LinearLayout.LayoutParams rwLp = new LinearLayout.LayoutParams(
  // LinearLayout.LayoutParams.FILL_PARENT,
  // LinearLayout.LayoutParams.FILL_PARENT);
  // Context context = TablePropertiesManager.this;
  // for (ColumnProperties cp : tp.getColumns()) {
  // ConditionalRuler cr = rulerMap.get(cp);
  // for (int i = 0; i < cr.getRuleCount(); i++) {
  // ruleList.addView(buildRuleView(cp, cr, i), rwLp);
  // }
  // }
  // LinearLayout controlWrapper = new LinearLayout(context);
  // Button addButton = new Button(context);
  // addButton.setText("Add");
  // addButton.setOnClickListener(new View.OnClickListener() {
  // @Override
  // public void onClick(View v) {
  // ColumnProperties cp = columns[0];
  // ConditionalRuler cr = rulerMap.get(cp);
  // cr.addRule(ConditionalRuler.Comparator.EQUALS, "",
  // values[0]);
  // ruleList.addView(buildRuleView(cp, cr,
  // cr.getRuleCount() - 1));
  // }
  // });
  // controlWrapper.addView(addButton);
  // Button closeButton = new Button(context);
  // closeButton.setText("Close");
  // closeButton.setOnClickListener(new View.OnClickListener() {
  // @Override
  // public void onClick(View v) {
  // dismiss();
  // }
  // });
  // controlWrapper.addView(closeButton);
  // LinearLayout wrapper = new LinearLayout(context);
  // wrapper.setOrientation(LinearLayout.VERTICAL);
  // wrapper.addView(ruleList);
  // wrapper.addView(controlWrapper);
  // return wrapper;
  // }
  //
  // private View buildRuleView(final ColumnProperties cp,
  // final ConditionalRuler cr, final int ruleIndex) {
  // Context context = TablePropertiesManager.this;
  //
  // final Spinner colSpinner = getSpinner(context, columnDisplays);
  // int columnIndex = -1;
  // for (int i = 0; i < columns.length; i++) {
  // if (cp == columns[i]) {
  // columnIndex = i;
  // break;
  // }
  // }
  // if (columnIndex == -1) {
  // throw new RuntimeException();
  // }
  // colSpinner.setSelection(columnIndex);
  //
  // final Spinner settingSpinner = getSpinner(context, labels);
  // int setting = cr.getRuleSetting(ruleIndex);
  // int settingIndex = -1;
  // for (int i = 0; i < values.length; i++) {
  // if (setting == values[i]) {
  // settingIndex = i;
  // break;
  // }
  // }
  // if (settingIndex == -1) {
  // throw new RuntimeException();
  // }
  // settingSpinner.setSelection(settingIndex);
  //
  // final Spinner compSpinner = getSpinner(context, comparatorLabels);
  // compSpinner.setSelection(cr.getRuleComparator(ruleIndex));
  //
  // final EditText valueEt = new EditText(context);
  // valueEt.setText(cr.getRuleValue(ruleIndex));
  //
  // Button deleteButton = new Button(context);
  // deleteButton.setText("Delete");
  //
  // colSpinner.setOnItemSelectedListener(
  // new AdapterView.OnItemSelectedListener() {
  // @Override
  // public void onItemSelected(AdapterView<?> parent, View view,
  // int position, long id) {
  // ColumnProperties nextCp = columns[position];
  // if (cp == nextCp) {
  // return;
  // }
  // cr.deleteRule(ruleIndex);
  // rulerMap.get(nextCp).addRule(
  // compSpinner.getSelectedItemPosition(),
  // valueEt.getText().toString(),
  // values[settingSpinner.getSelectedItemPosition()]);
  // setContentView(buildView());
  // }
  // @Override
  // public void onNothingSelected(AdapterView<?> parent) {}
  // });
  // settingSpinner.setOnItemSelectedListener(
  // new AdapterView.OnItemSelectedListener() {
  // @Override
  // public void onItemSelected(AdapterView<?> parent, View view,
  // int position, long id) {
  // if (cr.getRuleSetting(ruleIndex) == values[position]) {
  // return;
  // }
  // cr.setRuleSetting(ruleIndex, values[position]);
  // }
  // @Override
  // public void onNothingSelected(AdapterView<?> parent) {}
  // });
  // compSpinner.setOnItemSelectedListener(
  // new AdapterView.OnItemSelectedListener() {
  // @Override
  // public void onItemSelected(AdapterView<?> parent, View view,
  // int position, long id) {
  // if (cr.getRuleComparator(ruleIndex) == position) {
  // return;
  // }
  // cr.setRuleComparator(ruleIndex, position);
  // }
  // @Override
  // public void onNothingSelected(AdapterView<?> parent) {}
  // });
  // valueEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
  // @Override
  // public void onFocusChange(View v, boolean hasFocus) {
  // if (hasFocus) {
  // return;
  // }
  // cr.setRuleValue(ruleIndex, valueEt.getText().toString());
  // }
  // });
  // deleteButton.setOnClickListener(new View.OnClickListener() {
  // @Override
  // public void onClick(View v) {
  // cr.deleteRule(ruleIndex);
  // setContentView(buildView());
  // }
  // });
  //
  // LinearLayout topRow = new LinearLayout(context);
  // LinearLayout bottomRow = new LinearLayout(context);
  // topRow.addView(colSpinner);
  // topRow.addView(settingSpinner);
  // bottomRow.addView(compSpinner);
  // bottomRow.addView(valueEt);
  // bottomRow.addView(deleteButton);
  // LinearLayout rw = new LinearLayout(context);
  // rw.setOrientation(LinearLayout.VERTICAL);
  // rw.addView(topRow);
  // rw.addView(bottomRow);
  // return rw;
  // }
  //
  // private Spinner getSpinner(Context context, String[] values) {
  // Spinner spinner = new Spinner(context);
  // ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
  // android.R.layout.simple_spinner_item, values);
  // adapter.setDropDownViewResource(
  // android.R.layout.simple_spinner_dropdown_item);
  // spinner.setAdapter(adapter);
  // return spinner;
  // }
  // }
}
