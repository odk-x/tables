package org.opendatakit.tables.fragments;

import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.utils.ColumnUtil;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.ElementTypeManipulator;
import org.opendatakit.tables.utils.ElementTypeManipulator.ITypeManipulatorFragment;
import org.opendatakit.tables.utils.ElementTypeManipulatorFactory;
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
   * Retrieve the {@link ColumnDefinition} associated with the column this
   * activity is displaying.
   * 
   * @return
   */
  ColumnDefinition retrieveColumnDefinition() {
    String elementKey =
        this.retrieveTableLevelPreferenceActivity().getElementKey();
    try {
      ColumnDefinition result =
        this.retrieveTableProperties().getColumnDefinitionByElementKey(elementKey);
      return result;
    } catch ( IllegalArgumentException e ) {
      Log.e(
          TAG,
          "[retrieveColumnDefinition] did not find column for element key: " +
              elementKey);
      return null;
    }
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
    pref.setSummary(ColumnUtil.getRawDisplayName(getTableProperties(), 
                      this.retrieveColumnDefinition().getElementKey()));

  }

  private void initializeElementKey() {
    EditTextPreference pref = this
        .findEditTextPreference(Constants.PreferenceKeys.Column.ELEMENT_KEY);
    pref.setSummary(this.retrieveColumnDefinition().getElementKey());
  }

  private void initializeColumnType() {
    EditTextPreference pref =
        this.findEditTextPreference(Constants.PreferenceKeys.Column.TYPE);
    ElementTypeManipulator m = ElementTypeManipulatorFactory.getInstance();
    ITypeManipulatorFragment r = m.getDefaultRenderer(this.retrieveColumnDefinition().getType());
    pref.setSummary(r.getElementTypeDisplayLabel());
  }

  private void initializeElementName() {
    EditTextPreference pref = this
        .findEditTextPreference(Constants.PreferenceKeys.Column.DISPLAY_NAME);
    pref.setSummary(this.retrieveColumnDefinition().getElementName());
  }

  private void initializeColumnWidth() {
    final EditTextPreference pref =
        this.findEditTextPreference(Constants.PreferenceKeys.Column.WIDTH);
    int columnWidth = PreferenceUtil.getColumnWidth(getTableProperties(),
        retrieveColumnDefinition().getElementKey());
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
            retrieveColumnDefinition().getElementKey(),
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
            retrieveColumnDefinition().getElementKey(),
            ColorRuleGroup.Type.COLUMN);
        return true;
      }
      
    });
  }

}
