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

import org.opendatakit.common.android.data.OrderedColumns;
import org.opendatakit.tables.activities.AbsTableActivity;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;

/**
 * This fragment should be extended to display any preferences that apply at a
 * table level. Any preference fragments that expect a tableId
 * to be available should extend this fragment.
 * <p>
 * This Fragment can only be used inside a {@link AbsTableActivity}.
 * @author sudar.sam@gmail.com
 *
 */
public abstract class AbsTableLevelPreferenceFragment
    extends PreferenceFragment {

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    // We have to verify that we are attached to an AbsTableActivity, or else
    // we won't have a tableId available.
    if (!(this.getActivity() instanceof AbsTableActivity)) {
      throw new IllegalStateException("AbsTableLevelPreferenceFragment can " +
      		"only be used with AbsTableActivity. " +
            this.getActivity().getClass() +
      		" is not appropriate.");
    }
  }

  String getAppName() {
    AbsTableActivity activity = (AbsTableActivity) this.getActivity();
    return activity.getAppName();
  }
  
  String getTableId() {
    AbsTableActivity activity = (AbsTableActivity) this.getActivity();
    return activity.getTableId();
  }
  
  OrderedColumns getColumnDefinitions() {
    AbsTableActivity activity = (AbsTableActivity) this.getActivity();
    return activity.getColumnDefinitions();
  }

  /**
   * Find an {@link EditTextPreference} with the given key.
   * Convenience method for
   * calling {@link PreferenceFragment#findPreference(CharSequence)} and
   * casting it to {@link EditTextPreference}.
   * @param key
   * @return
   */
  EditTextPreference findEditTextPreference(String key) {
    EditTextPreference preference =
        (EditTextPreference) this.findPreference(key);
    return preference;
  }

  /**
   * Find a {@link ListPreference} with the given key. Convenience method for
   * calling {@link PreferenceFragment#findPreference(CharSequence)} and
   * casting it to {@link ListPreference}.
   * @param key
   * @return
   */
  ListPreference findListPreference(String key) {
    ListPreference preference = (ListPreference) findPreference(key);
    return preference;
  }

}
