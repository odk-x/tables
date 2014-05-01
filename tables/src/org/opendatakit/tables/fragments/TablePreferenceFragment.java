package org.opendatakit.tables.fragments;

import org.opendatakit.tables.R;
import org.opendatakit.tables.preferences.EditFormDialogPreference;
import org.opendatakit.tables.utils.Constants;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.view.ContextMenu;

/**
 * Displays preferences and information surrounding a table.
 * @author sudar.sam@gmail.com
 *
 */
public class TablePreferenceFragment extends AbsTableLevelPreferenceFragment {
  
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
    // Verify that we're attaching to the right activity.
    // Now we have to do the various initialization required for the different
    // preferences.
    this.initializeAllPreferences();
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
    ListPreference viewPref = this.findListPreference(
        Constants.PreferenceKeys.Table.DEFAULT_VIEW_TYPE);
    // TODO: see above
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
  
  
}
