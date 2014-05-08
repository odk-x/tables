package org.opendatakit.tables.fragments;

import java.io.File;

import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.GraphManagerActivity;
import org.opendatakit.tables.preferences.DefaultViewTypePreference;
import org.opendatakit.tables.preferences.EditFormDialogPreference;
import org.opendatakit.tables.preferences.FileSelectorPreference;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.PreferenceUtil;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.ContextMenu;

/**
 * Displays preferences and information surrounding a table.
 * @author sudar.sam@gmail.com
 *
 */
public class TablePreferenceFragment extends AbsTableLevelPreferenceFragment {
  
  private static final String TAG =
      TablePreferenceFragment.class.getSimpleName();
  
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
    Log.d(TAG, "[onActivityResult]");
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
    }
    super.onActivityResult(requestCode, resultCode, data);
  }
  
  /**
   * Return the full path of the file selected from the intent.
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
   * @param relativePath
   */
  void setListViewFileName(String relativePath) {
    KeyValueStoreHelper kvsh = getTableProperties().getKeyValueStoreHelper(
        TableProperties.KVS_PARTITION);
    kvsh.setString(TableProperties.KEY_LIST_VIEW_FILE_NAME, relativePath);
  }
  
  /**
   * Sets the file name for the detail view of this table.
   * @param relativePath
   */
  void setDetailViewFileName(String relativePath) {
    KeyValueStoreHelper kvsh = getTableProperties().getKeyValueStoreHelper(
        TableProperties.KVS_PARTITION);
    kvsh.setString(TableProperties.KEY_DETAIL_VIEW_FILE_NAME, relativePath);
  }
  
  /**
   * Convenience method for initializing all the preferences. Requires a
   * {@link ContextMenu}, so must be called in or after 
   * {@link TablePreferenceFragment#onActivityCreated(Bundle)}.
   */
  private void initializeAllPreferences() {
    this.initializeDisplayNamePreference();
    this.initializeTableIdPreference();
    this.initializeDefaultForm();
    this.initializeDefaultViewType();
    this.initializeTableColorRules();
    this.initializeStatusColorRules();
    this.initializeMapColorRule();
    this.initializeDetailFile();
    this.initializeListFile();
    this.initializeGraphManager();
  }

  private void initializeDisplayNamePreference() {
    EditTextPreference displayPref = this.findEditTextPreference(
        Constants.PreferenceKeys.Table.DISPLAY_NAME);
    displayPref.setSummary(getTableProperties().getDisplayName());
  }
  
  private void initializeTableIdPreference() {
    EditTextPreference idPref = this.findEditTextPreference(
        Constants.PreferenceKeys.Table.TABLE_ID);
    idPref.setSummary(getTableProperties().getTableId());
  }
  
  private void initializeDefaultViewType() {
    // We have to set the current default view and disable the entries that
    // don't apply to this table.
    DefaultViewTypePreference viewPref = (DefaultViewTypePreference)
        this.findListPreference(
            Constants.PreferenceKeys.Table.DEFAULT_VIEW_TYPE);
    viewPref.setFields(getTableProperties());
    viewPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.e(TAG, "[onPreferenceChange] for default view preference. Pref is: " + newValue);
        String selectedValue = newValue.toString();
        PreferenceUtil.setDefaultViewType(
            getActivity(),
            getTableProperties(),
            TableViewType.valueOf(selectedValue));
        return true;
      }
    });
  }
  
  private void initializeDefaultForm() {
    EditFormDialogPreference formPref = (EditFormDialogPreference)
        this.findPreference(Constants.PreferenceKeys.Table.DEFAULT_FORM);
    // TODO:
  }
  
  private void initializeTableColorRules() {
    Preference tableColorPref = this.findPreference(
        Constants.PreferenceKeys.Table.TABLE_COLOR_RULES);
    // TODO:
  }
  
  private void initializeListFile() {
    FileSelectorPreference listPref = (FileSelectorPreference)
        this.findPreference(Constants.PreferenceKeys.Table.LIST_FILE);
    listPref.setFields(
        this, 
        Constants.RequestCodes.CHOOSE_LIST_FILE,
        ((AbsBaseActivity) getActivity()).getAppName());
    TableProperties tableProperties = getTableProperties();
    listPref.setSummary(tableProperties.getListViewFileName());
  }
  
  private void initializeDetailFile() {
    FileSelectorPreference detailPref = (FileSelectorPreference)
        this.findPreference(Constants.PreferenceKeys.Table.DETAIL_FILE);
    detailPref.setFields(
        this,
        Constants.RequestCodes.CHOOSE_DETAIL_FILE,
        ((AbsBaseActivity) getActivity()).getAppName());
    TableProperties tableProperties = getTableProperties();
    detailPref.setSummary(tableProperties.getDetailViewFileName());
  }
  
  private void initializeStatusColorRules() {
    Preference statusColorPref = this.findPreference(
        Constants.PreferenceKeys.Table.STATUS_COLOR_RULES);
    // TODO:
  }
  
  private void initializeMapColorRule() {
    ListPreference mapColorPref = this.findListPreference(
        Constants.PreferenceKeys.Table.MAP_COLOR_RULE);
    // TODO:
  }
  
  private void initializeGraphManager() {
    Preference graphPref = this.findPreference(
        Constants.PreferenceKeys.Table.GRAPH_MANAGER);
    graphPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent selectGraphViewIntent = new Intent(
            getActivity(),
            GraphManagerActivity.class);
        selectGraphViewIntent.putExtra(
            Constants.IntentKeys.APP_NAME, getAppName());
        selectGraphViewIntent.putExtra(
            Constants.IntentKeys.TABLE_ID,
            getTableProperties().getTableId());
        startActivity(selectGraphViewIntent);
        return true;
      }

    });
  }
  
  private String getRelativePathOfFile(String fullPath) {
    String relativePath = ODKFileUtils.asRelativePath(
        ((AbsBaseActivity) getActivity()).getAppName(),
        new File(fullPath));
    return relativePath;
  }
  
  
}
