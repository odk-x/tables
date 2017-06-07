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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.JsonReader;
import android.view.ContextMenu;
import android.widget.Toast;
import org.opendatakit.data.ColorRuleGroup;
import org.opendatakit.data.TableViewType;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.LocalKeyValueStoreConstants;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.preferences.DefaultViewTypePreference;
import org.opendatakit.tables.preferences.EditFormDialogPreference;
import org.opendatakit.tables.preferences.FileSelectorPreference;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.PreferenceUtil;
import org.opendatakit.utilities.ODKFileUtils;

import java.io.File;
import java.io.StringReader;
import java.net.ProtocolException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Displays preferences and information surrounding a table.
 *
 * @author sudar.sam@gmail.com
 */
public class TablePreferenceFragment extends AbsTableLevelPreferenceFragment {

  private static final String TAG = TablePreferenceFragment.class.getSimpleName();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // AppName may not be available...
    // Let's load preferences from the resource.
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
    try {
      this.initializeAllPreferences();
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(getAppName()).printStackTrace(e);
      Toast.makeText(getActivity(), "Unable to access database", Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    String fullPath = null;
    String relativePath = null;
    WebLogger.getLogger(getAppName()).d(TAG, "[onActivityResult]");

    switch (requestCode) {
    case Constants.RequestCodes.CHOOSE_LIST_FILE:
      if (data != null) {
        try {
          fullPath = getFullPathFromIntent(data);
          relativePath = getRelativePathOfFile(fullPath);
          this.setListViewFileName(relativePath);
        } catch (IllegalArgumentException e) {
          WebLogger.getLogger(getAppName()).printStackTrace(e);
          Toast.makeText(getActivity(),
              getString(R.string.file_not_under_app_dir, ODKFileUtils.getAppFolder(getAppName())),
              Toast.LENGTH_LONG).show();
        }
      }
      break;
    case Constants.RequestCodes.CHOOSE_DETAIL_FILE:
      if (data != null) {
        try {
          fullPath = getFullPathFromIntent(data);
          relativePath = getRelativePathOfFile(fullPath);
          this.setDetailViewFileName(relativePath);
        } catch (IllegalArgumentException e) {
          WebLogger.getLogger(getAppName()).printStackTrace(e);
          Toast.makeText(getActivity(),
              getString(R.string.file_not_under_app_dir, ODKFileUtils.getAppFolder(getAppName())),
              Toast.LENGTH_LONG).show();
        }
      }
      break;
    case Constants.RequestCodes.CHOOSE_MAP_FILE:
      if (data != null) {
        try {
          fullPath = getFullPathFromIntent(data);
          relativePath = getRelativePathOfFile(fullPath);
          this.setMapListViewFileName(relativePath);
        } catch (IllegalArgumentException e) {
          WebLogger.getLogger(getAppName()).printStackTrace(e);
          Toast.makeText(getActivity(),
              getString(R.string.file_not_under_app_dir, ODKFileUtils.getAppFolder(getAppName())),
              Toast.LENGTH_LONG).show();
        }
      }
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
    try {
      TableUtil.get()
          .atomicSetListViewFilename(Tables.getInstance().getDatabase(), getAppName(), getTableId(),
              relativePath);
    } catch (ServicesAvailabilityException e) {
      Toast.makeText(getActivity(), "Unable to save List View filename", Toast.LENGTH_LONG).show();
    }
  }

  /**
   * Sets the file name for the detail view of this table.
   *
   * @param relativePath
   */
  void setDetailViewFileName(String relativePath) {
    try {
      TableUtil.get().atomicSetDetailViewFilename(Tables.getInstance().getDatabase(), getAppName(),
          getTableId(), relativePath);
    } catch (ServicesAvailabilityException e) {
      Toast.makeText(getActivity(), "Unable to set Detail View filename", Toast.LENGTH_LONG).show();
    }
  }

  /**
   * Sets the file name for the list view to be displayed in the map.
   *
   * @param relativePath
   */
  void setMapListViewFileName(String relativePath) {
    try {
      TableUtil.get().atomicSetMapListViewFilename(Tables.getInstance().getDatabase(), getAppName(),
          getTableId(), relativePath);
    } catch (ServicesAvailabilityException e) {
      Toast.makeText(getActivity(), "Unable to set Map List View Filename", Toast.LENGTH_LONG)
          .show();
    }
  }

  /**
   * Convenience method for initializing all the preferences. Requires a
   * {@link ContextMenu}, so must be called in or after
   * {@link TablePreferenceFragment#onActivityCreated(Bundle)}.
   *
   * @throws ServicesAvailabilityException if the database is down
   */
  protected void initializeAllPreferences() throws ServicesAvailabilityException {
    DbHandle db = null;
    try {
      UserDbInterface temp = Tables.getInstance().getDatabase();
      // This happens when the user clicks the back button in the file picker for some reason
      if (temp == null) {
        WebLogger.getLogger(getAppName()).a(TAG, "DbInterface is null. This shouldn't happen!");
        // If the database hasn't been connected yet, wait until it has
        // We have to wait using an extra thread because CommonApplication won't call
        // doServiceConnected for the database until onCreate (and therefore this method) returns
        Thread t = new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              Thread.sleep(100);
              getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  try {
                    // Recursive call
                    TablePreferenceFragment.this.initializeAllPreferences();
                  } catch (Throwable e) {
                    WebLogger.getLogger(getAppName()).printStackTrace(e);
                  }
                }
              });
            } catch (Throwable e) {
              WebLogger.getLogger(getAppName()).printStackTrace(e);
            }
          }
        });
        t.setDaemon(true);
        t.start();
        return;
      } db = temp.openDatabase(getAppName());

      this.initializeDisplayNamePreference(db);
      this.initializeTableIdPreference();
      this.initializeDefaultForm();
      this.initializeDefaultViewType();
      this.initializeTableColorRules();
      this.initializeStatusColorRules();
      this.initializeMapColorRule(db);
      this.initializeDetailFile(db);
      this.initializeListFile(db);
      this.initializeMapListFile(db);
      this.initializeColumns();
    } finally {
      if (db != null) {
        Tables.getInstance().getDatabase().closeDatabase(getAppName(), db);
      }
    }
  }

  private void initializeDisplayNamePreference(DbHandle db) throws ServicesAvailabilityException {
    EditTextPreference displayPref = this
        .findEditTextPreference(Constants.PreferenceKeys.Table.DISPLAY_NAME);

    String rawDisplayName = TableUtil.get()
        .getRawDisplayName(Tables.getInstance().getDatabase(), getAppName(), db, getTableId());

    JsonReader parser = new JsonReader(new StringReader(rawDisplayName));
    try {
      parser.beginObject();
      if (!parser.nextName().equals("text")) {
        throw new ProtocolException();
      }
      rawDisplayName = parser.nextString();
      parser.close();
    } catch (Throwable e) {
      WebLogger.getLogger(getAppName()).printStackTrace(e);
    }
    displayPref.setSummary(rawDisplayName);
  }

  private void initializeTableIdPreference() {
    EditTextPreference idPref = this
        .findEditTextPreference(Constants.PreferenceKeys.Table.TABLE_ID);
    idPref.setSummary(getTableId());
  }

  private void initializeDefaultViewType() throws ServicesAvailabilityException {
    // We have to set the current default view and disable the entries that
    // don't apply to this table.
    DefaultViewTypePreference viewPref = (DefaultViewTypePreference) this
        .findListPreference(Constants.PreferenceKeys.Table.DEFAULT_VIEW_TYPE);
    viewPref.setFields(getTableId(), getColumnDefinitions());
    viewPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        WebLogger.getLogger(getAppName())
            .e(TAG, "[onPreferenceChange] for default view preference. Pref is: " + newValue);
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

  private void initializeListFile(DbHandle db) throws ServicesAvailabilityException {
    FileSelectorPreference listPref = (FileSelectorPreference) this
        .findPreference(Constants.PreferenceKeys.Table.LIST_FILE);
    listPref.setFields(this, Constants.RequestCodes.CHOOSE_LIST_FILE,
        ((AbsBaseActivity) getActivity()).getAppName());
    listPref.setSummary(TableUtil.get()
        .getListViewFilename(Tables.getInstance().getDatabase(), getAppName(), db, getTableId()));
  }

  private void initializeMapListFile(DbHandle db) throws ServicesAvailabilityException {
    FileSelectorPreference mapListPref = (FileSelectorPreference) this
        .findPreference(Constants.PreferenceKeys.Table.MAP_LIST_FILE);
    mapListPref.setFields(this, Constants.RequestCodes.CHOOSE_MAP_FILE,
        ((AbsBaseActivity) getActivity()).getAppName());
    String mapListViewFileName = TableUtil.get()
        .getMapListViewFilename(Tables.getInstance().getDatabase(), getAppName(), db, getTableId());
    WebLogger.getLogger(getAppName())
        .d(TAG, "[initializeMapListFile] file is: " + mapListViewFileName);
    mapListPref.setSummary(mapListViewFileName);
  }

  private void initializeDetailFile(DbHandle db) throws ServicesAvailabilityException {
    FileSelectorPreference detailPref = (FileSelectorPreference) this
        .findPreference(Constants.PreferenceKeys.Table.DETAIL_FILE);
    detailPref.setFields(this, Constants.RequestCodes.CHOOSE_DETAIL_FILE,
        ((AbsBaseActivity) getActivity()).getAppName());
    detailPref.setSummary(TableUtil.get()
        .getDetailViewFilename(Tables.getInstance().getDatabase(), getAppName(), db, getTableId()));
  }

  private void initializeStatusColorRules() {
    Preference statusColorPref = this
        .findPreference(Constants.PreferenceKeys.Table.STATUS_COLOR_RULES);
    statusColorPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        // pop in the list of columns.
        TableLevelPreferencesActivity activity = (TableLevelPreferencesActivity) getActivity();
        activity.showStatusColorRuleListFragment(ColorRuleGroup.Type.STATUS_COLUMN);
        return false;
      }
    });
  }

  private void initializeMapColorRule(DbHandle db) throws ServicesAvailabilityException {
    ListPreference mapColorPref = this
        .findListPreference(Constants.PreferenceKeys.Table.MAP_COLOR_RULE);

    TableUtil.MapViewColorRuleInfo mvcri = TableUtil.get()
        .getMapListViewColorRuleInfo(Tables.getInstance().getDatabase(), getAppName(), db,
            getTableId());

    String initColorType = null;

    if (mvcri != null && mvcri.colorType != null) {
      if (mvcri.colorType.equals(LocalKeyValueStoreConstants.Map.COLOR_TYPE_STATUS)) {
        initColorType = getString(R.string.color_rule_type_values_status);
      } else if (mvcri.colorType.equals(LocalKeyValueStoreConstants.Map.COLOR_TYPE_TABLE)) {
        initColorType = getString(R.string.color_rule_type_values_table);
      } else {
        initColorType = getString(R.string.color_rule_type_values_none);
      }

      mapColorPref.setValue(initColorType);
    }

    // Set color rule for map view
    mapColorPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        WebLogger.getLogger(getAppName())
            .e(TAG, "[onPreferenceChange] for map color rule preference. Pref is: " + newValue);
        DbHandle db = null;
        String colorRuleType = null;
        try {
          String selectedValue = newValue.toString();

          if (selectedValue == null) {
            return false;
          }

          // Support for selectable color rules seems to have been removed
          // This should just be taken out of Tables for now
          String colorElementKey = null;
          if (selectedValue.equals(getString(R.string.color_rule_type_values_status))) {
            colorRuleType = LocalKeyValueStoreConstants.Map.COLOR_TYPE_STATUS;
          } else if (selectedValue.equals(getString(R.string.color_rule_type_values_table))) {
            colorRuleType = LocalKeyValueStoreConstants.Map.COLOR_TYPE_TABLE;
          } else {
            colorRuleType = LocalKeyValueStoreConstants.Map.COLOR_TYPE_NONE;
          }

          db = Tables.getInstance().getDatabase().openDatabase(getAppName());
          TableUtil.MapViewColorRuleInfo mvcri = new TableUtil.MapViewColorRuleInfo(colorRuleType,
              null);
          TableUtil.get()
              .setMapListViewColorRuleInfo(Tables.getInstance().getDatabase(), getAppName(), db,
                  getTableId(), mvcri);
          return true;

        } catch (ServicesAvailabilityException re) {
          WebLogger.getLogger(getAppName())
              .e(TAG, "[onPreferenceChange] for map color rule preference. RemoteException : ");
          re.printStackTrace();
          return false;
        } finally {
          if (db != null) {
            try {
              Tables.getInstance().getDatabase().closeDatabase(getAppName(), db);
            } catch (ServicesAvailabilityException re) {
              WebLogger.getLogger(getAppName()).e(TAG,
                  "[onPreferenceChange] for map color rule preference. "
                      + "RemoteException while closing db: ");
              re.printStackTrace();
            }
          }
        }
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
    return ODKFileUtils
        .asRelativePath(((AbsBaseActivity) getActivity()).getAppName(), new File(fullPath));
  }

}
