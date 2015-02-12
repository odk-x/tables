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

import java.util.ArrayList;

import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.common.android.utilities.ColumnUtil;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.ElementTypeManipulator;
import org.opendatakit.tables.utils.ElementTypeManipulator.ITypeManipulatorFragment;
import org.opendatakit.tables.utils.ElementTypeManipulatorFactory;
import org.opendatakit.tables.utils.PreferenceUtil;
import org.opendatakit.tables.views.SpreadsheetView;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;

public class ColumnPreferenceFragment extends AbsTableLevelPreferenceFragment {

  private static final String TAG =
      ColumnPreferenceFragment.class.getSimpleName();

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
   * Retrieve the {@link ColumnDefinition} associated with the column this
   * activity is displaying.
   * 
   * @return
   */
  ColumnDefinition retrieveColumnDefinition() {
    TableLevelPreferencesActivity activity = retrieveTableLevelPreferenceActivity();
    String elementKey = activity.getElementKey();
    try {
      ArrayList<ColumnDefinition> orderedDefns = activity.getColumnDefinitions();
      ColumnDefinition result =
          ColumnDefinition.find(orderedDefns, elementKey);
      return result;
    } catch ( IllegalArgumentException e ) {
      WebLogger.getLogger(activity.getAppName()).e(
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

    String rawDisplayName;
    SQLiteDatabase db = null;
    try {
      db = DatabaseFactory.get().getDatabase(getActivity(), getAppName());
      rawDisplayName = ColumnUtil.get().getRawDisplayName(db, getTableId(), 
          this.retrieveColumnDefinition().getElementKey());
    } finally {
      if ( db != null ) {
        db.close();
      }
    }

    pref.setSummary(rawDisplayName);

  }

  private void initializeElementKey() {
    EditTextPreference pref = this
        .findEditTextPreference(Constants.PreferenceKeys.Column.ELEMENT_KEY);
    pref.setSummary(this.retrieveColumnDefinition().getElementKey());
  }

  private void initializeColumnType() {
    EditTextPreference pref =
        this.findEditTextPreference(Constants.PreferenceKeys.Column.TYPE);
    ElementTypeManipulator m = ElementTypeManipulatorFactory.getInstance(this.getAppName());
    ITypeManipulatorFragment r = m.getDefaultRenderer(this.retrieveColumnDefinition().getType());
    pref.setSummary(r.getElementTypeDisplayLabel());
  }

  private void initializeElementName() {
    EditTextPreference pref = this
        .findEditTextPreference(Constants.PreferenceKeys.Column.DISPLAY_NAME);
    pref.setSummary(this.retrieveColumnDefinition().getElementName());
  }

  private void initializeColumnWidth() {
    TableLevelPreferencesActivity activity = retrieveTableLevelPreferenceActivity();
    final String appName = activity.getAppName();
    final EditTextPreference pref =
        this.findEditTextPreference(Constants.PreferenceKeys.Column.WIDTH);
    int columnWidth = PreferenceUtil.getColumnWidth(getActivity(),
        getAppName(), getTableId(),
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
          WebLogger.getLogger(appName).e(TAG, "column width bigger than allowed, doing nothing");
          return false;
        }
        PreferenceUtil.setColumnWidth(
            getActivity(), getAppName(), getTableId(),
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
