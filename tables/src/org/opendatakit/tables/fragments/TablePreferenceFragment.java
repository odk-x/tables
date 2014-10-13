/*
 * Copyright (C) 2014 University of Washington
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
package org.opendatakit.tables.fragments;

import java.io.File;

import org.opendatakit.aggregate.odktables.rest.KeyValueStoreConstants;
import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.common.android.utilities.KeyValueStoreHelper;
import org.opendatakit.common.android.utilities.LocalKeyValueStoreConstants;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.TableUtil;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.preferences.DefaultViewTypePreference;
import org.opendatakit.tables.preferences.EditFormDialogPreference;
import org.opendatakit.tables.preferences.FileSelectorPreference;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.PreferenceUtil;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.ContextMenu;

/**
 * Displays preferences and information surrounding a table.
 * 
 * @author sudar.sam@gmail.com
 *
 */
public class TablePreferenceFragment extends AbsTableLevelPreferenceFragment {

  private static final String TAG = TablePreferenceFragment.class.getSimpleName();

  public TablePreferenceFragment() {
    // required by fragments.
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Let's load it from the resource.
    this.addPreferencesFromResource(R.xml.table_preference);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
  }

  @Override
  public void onResume() {
    super.onResume();
    // Verify that we're attaching to the right activity.
    // Now we have to do the various initialization required for the different
    // preferences.
    this.initializeAllPreferences();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    String fullPath = null;
    String relativePath = null;
    WebLogger.getLogger(getAppName()).d(TAG, "[onActivityResult]");
    switch (requestCode) {
    case Constants.RequestCodes.CHOOSE_LIST_FILE:
      fullPath = getFullPathFromIntent(data);
      relativePath = getRelativePathOfFile(fullPath);
      this.setListViewFileName(relativePath);
      break;
    case Constants.RequestCodes.CHOOSE_DETAIL_FILE:
      fullPath = getFullPathFromIntent(data);
      relativePath = getRelativePathOfFile(fullPath);
      this.setDetailViewFileName(relativePath);
      break;
    case Constants.RequestCodes.CHOOSE_MAP_FILE:
      fullPath = getFullPathFromIntent(data);
      relativePath = getRelativePathOfFile(fullPath);
      WebLogger.getLogger(getAppName()).d(TAG,
          "[onActivityResult] map view relative path is: " + relativePath);
      this.setMapListViewFileName(relativePath);
      break;
    default:
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  /**
   * Return the full path of the file selected from the intent.
   * 
   * @param intent
   * @return
   */
  private String getFullPathFromIntent(Intent intent) {
    Uri uri = intent.getData();
    String fullPath = uri.getPath();
    return fullPath;
  }

  /**
   * Sets the file name for the list view of this table.
   * 
   * @param relativePath
   */
  void setListViewFileName(String relativePath) {
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(getActivity(), getAppName(), getTableId(),
        KeyValueStoreConstants.PARTITION_TABLE);
    kvsh.setString(LocalKeyValueStoreConstants.Tables.KEY_LIST_VIEW_FILE_NAME, relativePath);
  }

  /**
   * Sets the file name for the detail view of this table.
   * 
   * @param relativePath
   */
  void setDetailViewFileName(String relativePath) {
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(getActivity(), getAppName(), getTableId(),
        KeyValueStoreConstants.PARTITION_TABLE);
    kvsh.setString(LocalKeyValueStoreConstants.Tables.KEY_DETAIL_VIEW_FILE_NAME, relativePath);
  }

  /**
   * Sets the file name for the list view to be displayed in the map.
   * 
   * @param relativePath
   */
  void setMapListViewFileName(String relativePath) {
    KeyValueStoreHelper kvsh = new KeyValueStoreHelper(getActivity(), getAppName(), getTableId(),
        KeyValueStoreConstants.PARTITION_TABLE);
    kvsh.setString(LocalKeyValueStoreConstants.Tables.KEY_MAP_LIST_VIEW_FILE_NAME, relativePath);
  }

  /**
   * Convenience method for initializing all the preferences. Requires a
   * {@link ContextMenu}, so must be called in or after
   * {@link TablePreferenceFragment#onActivityCreated(Bundle)}.
   */
  protected void initializeAllPreferences() {
    SQLiteDatabase db = null;
    try {
      db = DatabaseFactory.get().getDatabase(getActivity(), getAppName());

      this.initializeDisplayNamePreference(db);
      this.initializeTableIdPreference();
      this.initializeDefaultForm();
      this.initializeDefaultViewType();
      this.initializeTableColorRules();
      this.initializeStatusColorRules();
      this.initializeMapColorRule();
      this.initializeDetailFile(db);
      this.initializeListFile(db);
      this.initializeGraphManager();
      this.initializeMapListFile(db);
      this.initializeColumns();
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  private void initializeDisplayNamePreference(SQLiteDatabase db) {
    EditTextPreference displayPref = this
        .findEditTextPreference(Constants.PreferenceKeys.Table.DISPLAY_NAME);

    String rawDisplayName = TableUtil.get().getRawDisplayName(db, getTableId());

    displayPref.setSummary(rawDisplayName);
  }

  private void initializeTableIdPreference() {
    EditTextPreference idPref = this
        .findEditTextPreference(Constants.PreferenceKeys.Table.TABLE_ID);
    idPref.setSummary(getTableId());
  }

  private void initializeDefaultViewType() {
    // We have to set the current default view and disable the entries that
    // don't apply to this table.
    DefaultViewTypePreference viewPref = (DefaultViewTypePreference) this
        .findListPreference(Constants.PreferenceKeys.Table.DEFAULT_VIEW_TYPE);
    viewPref.setFields(getTableId(), getColumnDefinitions());
    viewPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        WebLogger.getLogger(getAppName()).e(TAG,
            "[onPreferenceChange] for default view preference. Pref is: " + newValue);
        String selectedValue = newValue.toString();
        PreferenceUtil.setDefaultViewType(getActivity(), getAppName(), getTableId(),
            TableViewType.valueOf(selectedValue));
        return true;
      }
    });
  }

  private void initializeDefaultForm() {
    EditFormDialogPreference formPref = (EditFormDialogPreference) this
        .findPreference(Constants.PreferenceKeys.Table.DEFAULT_FORM);
    // TODO:
  }

  private void initializeTableColorRules() {
    Preference tableColorPref = this
        .findPreference(Constants.PreferenceKeys.Table.TABLE_COLOR_RULES);
    tableColorPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        // pop in the list of columns.
        TableLevelPreferencesActivity activity = (TableLevelPreferencesActivity) getActivity();
        activity.showColorRuleListFragment(null, ColorRuleGroup.Type.TABLE);
        return false;
      }
    });
  }

  private void initializeListFile(SQLiteDatabase db) {
    FileSelectorPreference listPref = (FileSelectorPreference) this
        .findPreference(Constants.PreferenceKeys.Table.LIST_FILE);
    listPref.setFields(this, Constants.RequestCodes.CHOOSE_LIST_FILE,
        ((AbsBaseActivity) getActivity()).getAppName());
    listPref.setSummary(TableUtil.get().getListViewFilename(db, getTableId()));
  }

  private void initializeMapListFile(SQLiteDatabase db) {
    FileSelectorPreference mapListPref = (FileSelectorPreference) this
        .findPreference(Constants.PreferenceKeys.Table.MAP_LIST_FILE);
    mapListPref.setFields(this, Constants.RequestCodes.CHOOSE_MAP_FILE,
        ((AbsBaseActivity) getActivity()).getAppName());
    String mapListViewFileName = TableUtil.get().getMapListViewFilename(db, getTableId());
    WebLogger.getLogger(getAppName()).d(TAG,
        "[initializeMapListFile] file is: " + mapListViewFileName);
    mapListPref.setSummary(mapListViewFileName);
  }

  private void initializeDetailFile(SQLiteDatabase db) {
    FileSelectorPreference detailPref = (FileSelectorPreference) this
        .findPreference(Constants.PreferenceKeys.Table.DETAIL_FILE);
    detailPref.setFields(this, Constants.RequestCodes.CHOOSE_DETAIL_FILE,
        ((AbsBaseActivity) getActivity()).getAppName());
    detailPref.setSummary(TableUtil.get().getDetailViewFilename(db, getTableId()));
  }

  private void initializeStatusColorRules() {
    Preference statusColorPref = this
        .findPreference(Constants.PreferenceKeys.Table.STATUS_COLOR_RULES);
    statusColorPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        // pop in the list of columns.
        TableLevelPreferencesActivity activity = (TableLevelPreferencesActivity) getActivity();
        activity.showColorRuleListFragment(null, ColorRuleGroup.Type.STATUS_COLUMN);
        return false;
      }
    });
  }

  private void initializeMapColorRule() {
    ListPreference mapColorPref = this
        .findListPreference(Constants.PreferenceKeys.Table.MAP_COLOR_RULE);
    // TODO:
  }

  private void initializeGraphManager() {
    Preference graphPref = this.findPreference(Constants.PreferenceKeys.Table.GRAPH_MANAGER);
    graphPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent selectGraphViewIntent = new Intent(getActivity(), TableDisplayActivity.class);
        selectGraphViewIntent.putExtra(Constants.IntentKeys.APP_NAME, getAppName());
        selectGraphViewIntent.putExtra(Constants.IntentKeys.TABLE_ID, getTableId());
        IntentUtil.addFragmentViewTypeToBundle(selectGraphViewIntent.getExtras(),
            ViewFragmentType.GRAPH_MANAGER);
        startActivityForResult(selectGraphViewIntent, Constants.RequestCodes.LAUNCH_GRAPH_MANAGER);
        return true;
      }

    });
  }

  private void initializeColumns() {
    Preference columnPref = this.findPreference(Constants.PreferenceKeys.Table.COLUMNS);
    columnPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        // pop in the list of columns.
        TableLevelPreferencesActivity activity = (TableLevelPreferencesActivity) getActivity();
        activity.showColumnListFragment();
        return false;
      }
    });
  }

  private String getRelativePathOfFile(String fullPath) {
    String relativePath = ODKFileUtils.asRelativePath(
        ((AbsBaseActivity) getActivity()).getAppName(), new File(fullPath));
    return relativePath;
  }

}
