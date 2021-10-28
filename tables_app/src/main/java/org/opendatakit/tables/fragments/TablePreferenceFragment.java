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
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference.OnPreferenceClickListener;
import android.view.ContextMenu;
import android.widget.Toast;

import org.opendatakit.activities.BaseActivity;
import org.opendatakit.activities.IAppAwareActivity;
import org.opendatakit.activities.utils.FilePickerUtil;
import org.opendatakit.consts.RequestCodeConsts;
import org.opendatakit.data.ColorRuleGroup;
import org.opendatakit.data.TableViewType;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.LocalKeyValueStoreConstants;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.listener.DatabaseConnectionListener;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.properties.CommonToolProperties;
import org.opendatakit.properties.PropertiesSingleton;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsTableActivity;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.preferences.DefaultViewTypePreference;
import org.opendatakit.tables.preferences.FileSelectorPreference;
import org.opendatakit.tables.types.FormType;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.PreferenceUtil;
import org.opendatakit.utilities.ODKFileUtils;
import org.opendatakit.utilities.ODKXFileUriUtils;

import java.io.File;

/**
 * Displays preferences and information surrounding a table.
 *
 * @author sudar.sam@gmail.com
 */
public class TablePreferenceFragment extends AbsTableLevelPreferenceFragment
    implements DatabaseConnectionListener // not really
{

  // Used for logging
  private static final String TAG = TablePreferenceFragment.class.getSimpleName();

  /**
   * Return the full path of the file selected from the intent.
   *
   * @param intent the intent to pull the path out of
   * @return the path
   */
  private static String getFullPathFromIntent(Intent intent) {
    Uri uri = intent.getData();
    return uri.getPath();
  }

  /**
   * Called when the user opens the fragment or the fragment is resumed
   *
   * @param savedInstanceState the bundle that we saved when being destroyed/paused
   */
  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    // AppName may not be available...
    // Let's load preferences from the resource.
    setPreferencesFromResource(R.xml.table_preference, rootKey);
//    this.addPreferencesFromResource(R.xml.table_preference);
  }

  /**
   * Called when we resume. Try to initialize all the preferences in the fragment, or show a
   * message if we can't
   */
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

  /**
   * Called when an activity returns the UI to us.
   *
   * @param requestCode Which activity they're returning from
   * @param resultCode  whether it was a success or whether it was canceled
   * @param data        the intent that we used to start the activity
   */
  @Override
  public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

    // this way still sucks, just slightly less
    if (Tables.getInstance().getDatabase() == null) {
      //WebLogger.getLogger(getAppName()).i(TAG, "Database not up yet! Sleeping");
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          // sleep 100ms then hope the database is up and recurse. if it isn't, we'll sleep again
          try {
            Thread.sleep(100);
          } catch (InterruptedException ignored) {
          }
          getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              onActivityResult(requestCode, resultCode, data);
            }
          });
        }
      });
      t.setDaemon(true);
      t.start();
      return;
    }
    //WebLogger.getLogger(getAppName()).i(TAG, "Database now up, attempting");
    String relativePath;
    // temp
    //WebLogger.getLogger(getAppName()).i(TAG, String.format(Locale.getDefault(), "%d", requestCode));

    if (data == null ||
            ((RequestCodeConsts.RequestCodes.CHOOSE_LIST_FILE != requestCode)
                    && (RequestCodeConsts.RequestCodes.CHOOSE_DETAIL_FILE != requestCode)
                    && (RequestCodeConsts.RequestCodes.CHOOSE_MAP_FILE != requestCode))
      ) {
      super.onActivityResult(requestCode, resultCode, data);
      return;
    }

    Uri resultUri = FilePickerUtil.getUri(data);
    if(resultUri != null) {
      relativePath = ODKXFileUriUtils.ODKXRemainingPath(getAppName(), resultUri);
      if (relativePath != null) {
        switch (requestCode) {
          case RequestCodeConsts.RequestCodes.CHOOSE_LIST_FILE:
            this.setListViewFileName(relativePath);
            break;
          case RequestCodeConsts.RequestCodes.CHOOSE_DETAIL_FILE:
            this.setDetailViewFileName(relativePath);
            break;
          case RequestCodeConsts.RequestCodes.CHOOSE_MAP_FILE:
            this.setMapListViewFileName(relativePath);
            break;
        }
        try {
          WebLogger.getLogger(getAppName()).i(TAG, "Attempting to reinit prefs");
          this.initializeAllPreferences();
        } catch (ServicesAvailabilityException e) {
          WebLogger.getLogger(getAppName()).e(TAG, "failed");
          WebLogger.getLogger(getAppName()).printStackTrace(e);
          Toast.makeText(getActivity(), "Unable to access database", Toast.LENGTH_LONG).show();
        }
      } else {
        Toast.makeText(getActivity(),
                getString(R.string.file_not_under_app_dir, ODKFileUtils.getAppFolder(getAppName())),
                Toast.LENGTH_LONG).show();
      }
    }
  }

  /**
   * Sets the file name for the list view of this table.
   *
   * @param relativePath the path to set the list view file to
   */
  void setListViewFileName(String relativePath) {
    try {
      TableUtil.get()
          .atomicSetListViewFilename(Tables.getInstance().getDatabase(), getAppName(),
              getTableId(), relativePath);
    } catch (ServicesAvailabilityException ignored) {
      Toast.makeText(getActivity(), "Unable to save List View filename", Toast.LENGTH_LONG).show();
    }
  }

  /**
   * Sets the file name for the detail view of this table.
   *
   * @param relativePath the path to set the detail view file to
   */
  void setDetailViewFileName(String relativePath) {
    try {
      TableUtil.get()
          .atomicSetDetailViewFilename(Tables.getInstance().getDatabase(), getAppName(),
              getTableId(), relativePath);
    } catch (ServicesAvailabilityException ignored) {
      Toast.makeText(getActivity(), "Unable to set Detail View filename", Toast.LENGTH_LONG).show();
    }
  }

  /**
   * Sets the file name for the list view to be displayed in the map.
   *
   * @param relativePath the path to be used to view the table on a map
   */
  void setMapListViewFileName(String relativePath) {
    try {
      TableUtil.get()
          .atomicSetMapListViewFilename(Tables.getInstance().getDatabase(), getAppName(),
              getTableId(), relativePath);
    } catch (ServicesAvailabilityException ignored) {
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
      }
      db = temp.openDatabase(getAppName());

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

  /**
   * Sets up the (not editable) table display name preference's text
   *
   * @param db the database to pull the display name out of
   * @throws ServicesAvailabilityException if the database is down
   */
  private void initializeDisplayNamePreference(DbHandle db) throws ServicesAvailabilityException {
    EditTextPreference displayPref = this
        .findEditTextPreference(Constants.PreferenceKeys.Table.DISPLAY_NAME);

    /*
     * String rawDisplayName = TableUtil.get().getRawDisplayName(((BaseActivity) getActivity()
     * .getDatabase(), getAppName(), db, getTableId());
     */

    PropertiesSingleton props = CommonToolProperties.get(getActivity(), getAppName());
    String userSelectedDefaultLocale = props.getUserSelectedDefaultLocale();
    String localizedDisplayName = TableUtil.get().getLocalizedDisplayName(userSelectedDefaultLocale,
        Tables.getInstance().getDatabase(), getAppName(), db, getTableId());
    displayPref.setSummary(localizedDisplayName);

  }

  /**
   * Sets up the (not editable) table id preference's text
   */
  private void initializeTableIdPreference() {
    EditTextPreference idPref = this
        .findEditTextPreference(Constants.PreferenceKeys.Table.TABLE_ID);
    idPref.setSummary(getTableId());
  }

  /**
   * Sets up the (editable) dropdown menu for the default view type, either Spreadsheet, List or Map
   *
   * @throws ServicesAvailabilityException if the database is down
   */
  private void initializeDefaultViewType() throws ServicesAvailabilityException {
    // We have to set the current default view and disable the entries that
    // don't apply to this table.
    final AbsTableActivity mActivity = ((AbsTableActivity) getActivity());
    DefaultViewTypePreference viewPref = (DefaultViewTypePreference) this
        .findListPreference(Constants.PreferenceKeys.Table.DEFAULT_VIEW_TYPE);
    viewPref.setFields(getTableId(), getColumnDefinitions());
    viewPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        WebLogger.getLogger(mActivity.getAppName())
            .e(TAG, "[onPreferenceChange] for default view preference. Pref is: " + newValue);
        String selectedValue = newValue.toString();
        DefaultViewTypePreference pref = ((DefaultViewTypePreference) preference);
        if (pref.isValidSelection(selectedValue)) {
          PreferenceUtil.setDefaultViewType(mActivity, mActivity.getAppName(), getTableId(),
              TableViewType.valueOf(selectedValue));
          return true;
        } else {
          Toast.makeText(mActivity, mActivity.getString(R.string.invalid_default_view_type),
              Toast.LENGTH_LONG).show();
          return false;
        }
      }
    });
  }

  /**
   * Sets EditTextPreference for Survey form
   */
  private void initializeDefaultForm() {
    EditTextPreference editFormPref = this
        .findPreference(Constants.PreferenceKeys.Table.DEFAULT_FORM);
    final AbsTableActivity mActivity = ((AbsTableActivity) getActivity());

    try {
      String text = FormType.constructFormType((BaseActivity) getContext(), mActivity.getAppName(),
          mActivity.getTableId()).getFormId();
      editFormPref.setText(text);
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(mActivity.getAppName()).printStackTrace(e);
      Toast.makeText(getContext(), getContext().getString(R.string.unable_to_retrieve_form_type),
          Toast.LENGTH_LONG).show();
    }

    editFormPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        String formId = (String) newValue;
        if (formId.isEmpty()) {
          Toast.makeText(getContext(), mActivity.getString(R.string.invalid_form), Toast.LENGTH_LONG).show();
          return false;
        }

        if (!ODKFileUtils.VALID_FOLDER_PATTERN.matcher(formId).matches()) {
          Toast
              .makeText(requireContext(), getString(R.string.invalid_form_name), Toast.LENGTH_LONG)
              .show();
          return false;
        }

        String formDir = ODKFileUtils
            .getFormFolder(mActivity.getAppName(), mActivity.getTableId(), formId);
        File f = new File(formDir);
        File formDefJson = new File(f, ODKFileUtils.FORMDEF_JSON_FILENAME);
        if (!f.exists() || !f.isDirectory() || !formDefJson.exists() || !formDefJson.isFile()) {
          Toast.makeText(getContext(), mActivity.getString(R.string.invalid_form), Toast.LENGTH_LONG).show();
          return false;
        }

        UserDbInterface dbInt = Tables.getInstance().getDatabase();
        DbHandle db = null;
        try {
          FormType formType = FormType.constructFormType((BaseActivity) getContext(),
              mActivity.getAppName(), mActivity.getTableId());
          formType.setFormId(formId);
          AbsTableActivity tableActivity = (AbsTableActivity) getContext();

          db = dbInt.openDatabase(tableActivity.getAppName());
          formType.persist(dbInt, tableActivity.getAppName(), db, tableActivity.getTableId());
        } catch (ServicesAvailabilityException e) {
          WebLogger.getLogger(mActivity.getAppName()).printStackTrace(e);
          Toast.makeText(getContext(), getContext().getString(R.string.unable_to_save_db_changes),
              Toast.LENGTH_LONG).show();
        } finally {
          if (db != null) {
            try {
              dbInt.closeDatabase(mActivity.getAppName(), db);
            } catch (ServicesAvailabilityException e) {
              Toast.makeText(mActivity, R.string.unable_to_save_db_changes, Toast.LENGTH_LONG)
                  .show();
              WebLogger.getLogger(mActivity.getAppName()).e(TAG, "Failed to save default form");
              WebLogger.getLogger(mActivity.getAppName()).printStackTrace(e);
            }
          }
        }

        return true;
      }
    });
  }

  /**
   * Sets the onClick listener for the Table Color Rules menu
   */
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

  /**
   * Sets up the file picker option to select the list file to use
   *
   * @param db the database to use
   * @throws ServicesAvailabilityException if the database is down
   */
  private void initializeListFile(DbHandle db) throws ServicesAvailabilityException {
    FileSelectorPreference listPref = this
        .findPreference(Constants.PreferenceKeys.Table.LIST_FILE);
    listPref.setFields(this, RequestCodeConsts.RequestCodes.CHOOSE_LIST_FILE,
        ((IAppAwareActivity) getActivity()).getAppName());
    listPref.setSummary(TableUtil.get()
        .getListViewFilename(Tables.getInstance().getDatabase(), getAppName(), db,
            getTableId()));
  }

  /**
   * Sets up the file picker option to select the map file to use
   *
   * @param db the database to use
   * @throws ServicesAvailabilityException if the database is down
   */
  private void initializeMapListFile(DbHandle db) throws ServicesAvailabilityException {
    FileSelectorPreference mapListPref = this
        .findPreference(Constants.PreferenceKeys.Table.MAP_LIST_FILE);
    mapListPref.setFields(this, RequestCodeConsts.RequestCodes.CHOOSE_MAP_FILE,
        ((IAppAwareActivity) getActivity()).getAppName());
    String mapListViewFileName = TableUtil.get()
        .getMapListViewFilename(Tables.getInstance().getDatabase(), getAppName(), db,
            getTableId());
    WebLogger.getLogger(getAppName())
        .d(TAG, "[initializeMapListFile] file is: " + mapListViewFileName);
    mapListPref.setSummary(mapListViewFileName);
  }

  /**
   * Sets up the file picker option to select the detail view file to use
   *
   * @param db the database to use
   * @throws ServicesAvailabilityException if the database is down
   */
  private void initializeDetailFile(DbHandle db) throws ServicesAvailabilityException {
    FileSelectorPreference detailPref = this
        .findPreference(Constants.PreferenceKeys.Table.DETAIL_FILE);
    detailPref.setFields(this, RequestCodeConsts.RequestCodes.CHOOSE_DETAIL_FILE,
        ((IAppAwareActivity) getActivity()).getAppName());
    detailPref.setSummary(TableUtil.get()
        .getDetailViewFilename(Tables.getInstance().getDatabase(), getAppName(), db,
            getTableId()));
  }

  /**
   * Sets up the onclick listener for the (view only) Column Status Color Rules menu
   */
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

  /**
   * Handles the (editable) "Color Rule for Map" dropdown, with options "None", "Table Color
   * Rules" and "Status Column Color Rules"
   *
   * @param db the database to use
   * @throws ServicesAvailabilityException if the database is down
   */
  private void initializeMapColorRule(DbHandle db) throws ServicesAvailabilityException {
    ListPreference mapColorPref = this
        .findListPreference(Constants.PreferenceKeys.Table.MAP_COLOR_RULE);

    TableUtil.MapViewColorRuleInfo mvcri = TableUtil.get()
        .getMapListViewColorRuleInfo(Tables.getInstance().getDatabase(), getAppName(), db,
            getTableId());

    String initColorType;

    if (mvcri != null && mvcri.colorType != null) {
      switch (mvcri.colorType) {
      case LocalKeyValueStoreConstants.Map.COLOR_TYPE_STATUS:
        initColorType = getString(R.string.color_rule_type_values_status);
        break;
      case LocalKeyValueStoreConstants.Map.COLOR_TYPE_TABLE:
        initColorType = getString(R.string.color_rule_type_values_table);
        break;
      default:
        initColorType = getString(R.string.color_rule_type_values_none);
        break;
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
        String colorRuleType;
        try {
          String selectedValue = newValue.toString();

          if (selectedValue == null) {
            return false;
          }

          // Support for selectable color rules seems to have been removed
          // This should just be taken out of Tables for now
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
          TableUtil.get().setMapListViewColorRuleInfo(Tables.getInstance().getDatabase(),
              getAppName(), db, getTableId(), mvcri);
          return true;

        } catch (ServicesAvailabilityException re) {
          WebLogger.getLogger(getAppName())
              .e(TAG, "[onPreferenceChange] for map color rule preference. RemoteException : ");
          WebLogger.getLogger(getAppName()).printStackTrace(re);
          return false;
        } finally {
          if (db != null) {
            try {
              Tables.getInstance().getDatabase().closeDatabase(getAppName(), db);
            } catch (ServicesAvailabilityException re) {
              WebLogger.getLogger(getAppName()).e(TAG,
                  "[onPreferenceChange] for map color rule preference. "
                      + "RemoteException while closing db: ");
              WebLogger.getLogger(getAppName()).printStackTrace(re);
            }
          }
        }
      }
    });
  }

  /**
   * Sets up the onclick listener for opening the "Columns" list
   */
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


  //private boolean dbUp;
  //private int savedReq, savedRes; private Intent savedIntent = null;
  @Override
  public void databaseAvailable() { // NOT BEING CALLED!
    // I honestly have no idea why databaseAvailable is never being called, even down to
    // AbsBaseActivity it should be getting this fragment from the backstack and calling it
    WebLogger.getLogger(getAppName()).i(TAG, "databaseAvailable called!");
    //if (!dbUp) {
    //onActivityResult(savedReq, savedRes, savedIntent);
    //}
    //dbUp = true;
  }

  @Override
  public void databaseUnavailable() {
    //dbUp = false;
  }
}
