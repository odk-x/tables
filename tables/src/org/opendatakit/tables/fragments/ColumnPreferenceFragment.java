package org.opendatakit.tables.fragments;

import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.data.ColumnProperties;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.PreferenceUtil;
import org.opendatakit.tables.views.SpreadsheetView;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.Log;

public class ColumnPreferenceFragment extends AbsTableLevelPreferenceFragment {

  private static final String TAG =
      ColumnPreferenceFragment.class.getSimpleName();

  public ColumnPreferenceFragment() {
    // required for fragments.
  }

  public void onAttach(android.app.Activity activity) {
    super.onAttach(activity);
    if (!(activity instanceof TableLevelPreferencesActivity)) {
      throw new IllegalStateException("fragment must be attached to "
          + TableLevelPreferencesActivity.class.getSimpleName());
    }
  }

  /**
   * Get the {@link TableLevelPreferencesActivity} associated with this
   * activity.
   */
  TableLevelPreferencesActivity retrieveTableLevelPreferenceActivity() {
    TableLevelPreferencesActivity result =
        (TableLevelPreferencesActivity) this.getActivity();
    return result;
  }

  /**
   * Get the {@link TableProperties} associated with this activity.
   */
  TableProperties retrieveTableProperties() {
    TableLevelPreferencesActivity activity =
        this.retrieveTableLevelPreferenceActivity();
    TableProperties result = activity.getTableProperties();
    return result;
  }

  /**
   * Retrieve the {@link ColumnProperties} associated with the column this
   * activity is displaying.
   * 
   * @return
   */
  ColumnProperties retrieveColumnProperties() {
    String elementKey =
        this.retrieveTableLevelPreferenceActivity().getElementKey();
    ColumnProperties result =
        this.retrieveTableProperties().getColumnByElementKey(elementKey);
    if (result == null) {
      Log.e(
          TAG,
          "[retrieveColumnProperties] did not find column for element key: " +
              elementKey);
    }
    return result;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.addPreferencesFromResource(R.xml.preference_column);
  }

  @Override
  public void onResume() {
    super.onResume();
    this.initializeAllPreferences();
  }

  void initializeAllPreferences() {
    this.initializeColumnType();
    this.initializeColumnWidth();
    this.initializeDisplayName();
    this.initializeElementKey();
    this.initializeElementName();
    this.initializeColorRule();
  }

  private void initializeDisplayName() {
    EditTextPreference pref = this
        .findEditTextPreference(Constants.PreferenceKeys.Column.DISPLAY_NAME);
    pref.setSummary(this.retrieveColumnProperties().getDisplayName());

  }

  private void initializeElementKey() {
    EditTextPreference pref = this
        .findEditTextPreference(Constants.PreferenceKeys.Column.ELEMENT_KEY);
    pref.setSummary(this.retrieveColumnProperties().getElementKey());
  }

  private void initializeColumnType() {
    EditTextPreference pref =
        this.findEditTextPreference(Constants.PreferenceKeys.Column.TYPE);
    pref.setSummary(this.retrieveColumnProperties().getColumnType().label());
  }

  private void initializeElementName() {
    EditTextPreference pref = this
        .findEditTextPreference(Constants.PreferenceKeys.Column.DISPLAY_NAME);
    pref.setSummary(this.retrieveColumnProperties().getElementName());
  }

  private void initializeColumnWidth() {
    final EditTextPreference pref =
        this.findEditTextPreference(Constants.PreferenceKeys.Column.WIDTH);
    int columnWidth = PreferenceUtil.getColumnWidth(getTableProperties(),
        retrieveColumnProperties().getElementKey());
    pref.setSummary(Integer.toString(columnWidth));

    pref.setOnPreferenceChangeListener(
        new Preference.OnPreferenceChangeListener() {

      @Override
      public boolean onPreferenceChange(
          Preference preference,
          Object newValue) {
        String newValueStr = (String) newValue;
        Integer newWidth = Integer.parseInt(newValueStr);
        if (newWidth > SpreadsheetView.MAX_COL_WIDTH) {
          Log.e(TAG, "column width bigger than allowed, doing nothing");
          return false;
        }
        PreferenceUtil.setColumnWidth(
            getTableProperties(),
            retrieveColumnProperties().getElementKey(),
            newWidth);
        pref.setSummary(Integer.toString(newWidth));
        return true;
      }
    });

  }
  
  private void initializeColorRule() {
    Preference pref =
        this.findPreference(Constants.PreferenceKeys.Column.COLOR_RULES);
    pref.setOnPreferenceClickListener(
        new Preference.OnPreferenceClickListener() {
      
      @Override
      public boolean onPreferenceClick(Preference preference) {
        TableLevelPreferencesActivity activity =
            (TableLevelPreferencesActivity) getActivity();
        activity.showColorRuleListFragment(
            retrieveColumnProperties().getElementKey(),
            ColorRuleGroup.Type.COLUMN);
        return true;
      }
      
    });
  }

}
