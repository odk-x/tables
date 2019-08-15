/*
 * Copyright (C) 2015 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.tables.activities;

import androidx.fragment.app.FragmentTransaction;
import android.os.Bundle;
import org.opendatakit.data.ColorRuleGroup;
import org.opendatakit.tables.fragments.ColorRuleListFragment;
import org.opendatakit.tables.fragments.ColumnListFragment;
import org.opendatakit.tables.fragments.ColumnPreferenceFragment;
import org.opendatakit.tables.fragments.EditColorRuleFragment;
import org.opendatakit.tables.fragments.StatusColorRuleListFragment;
import org.opendatakit.tables.fragments.TablePreferenceFragment;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.views.OdkData;

/**
 * Displays preferences for a table to the user. This includes all preferences
 * that apply at a table level.
 *
 * @author sudar.sam@gmail.com
 */
public class TableLevelPreferencesActivity extends AbsTableActivity {

  /**
   * The fragment type currently being used, typically a {@link TablePreferenceFragment},
   * {@link ColumnListFragment}, {@link ColumnPreferenceFragment},
   * {@link ColorRuleListFragment}, or {@link StatusColorRuleListFragment}
   */
  FragmentType mCurrentFragmentType;
  /**
   * The element key of the column we're displaying, if this activity is
   * associated with a column fragmnent.
   */
  String mElementKeyOfDisplayedColumn;

  /**
   * Static factory for a TablePreferenceFragment
   *
   * @return a new TablePreferenceFragment
   */
  static TablePreferenceFragment createTablePreferenceFragment() {
    return new TablePreferenceFragment();
  }

  /**
   * Static factory for a StatusColorRuleListFragment with a particular list of color rules
   *
   * @param colorRuleGroupType the list of color rules for the status column
   * @return a new StatusColorRuleListFragment with the correct color rules
   */
  static StatusColorRuleListFragment createStatusColorRuleListFragment(
      ColorRuleGroup.Type colorRuleGroupType) {
    return StatusColorRuleListFragment.newInstance(colorRuleGroupType);
  }

  /**
   * Static factory for a ColorRuleListFragment with a particular list of color rules
   *
   * @param colorRuleGroupType the list of color rules for the column
   * @return a new ColorRuleListFragment with the correct color rules
   */
  static ColorRuleListFragment createColorRuleListFragment(ColorRuleGroup.Type colorRuleGroupType) {
    return ColorRuleListFragment.newInstance(colorRuleGroupType);
  }

  /**
   * Static factory for a ColumnPreferenceFragment
   *
   * @return a new ColumnPreferenceFragment
   */
  static ColumnListFragment createColumnListFragment() {
    return new ColumnListFragment();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.mCurrentFragmentType = retrieveFragmentTypeFromBundleOrActivity(savedInstanceState);
    String elementKey = this.retrieveElementKeyFromBundleOrActivity(savedInstanceState);
    // May be null--that is ok.
    this.mElementKeyOfDisplayedColumn = elementKey;
    switch (this.mCurrentFragmentType) {
    case TABLE_PREFERENCE:
      this.showTablePreferenceFragment();
      break;
    case COLUMN_LIST:
      this.showColumnListFragment();
      break;
    case COLUMN_PRFERENCE:
      this.showColumnPreferenceFragment(elementKey, false);
      break;
    case COLOR_RULE_LIST:
      ColorRuleGroup.Type colorRuleGroupType = IntentUtil
          .retrieveColorRuleTypeFromBundle(this.getIntent().getExtras());
      this.showColorRuleListFragment(elementKey, colorRuleGroupType, false);
      break;
    case STATUS_COLOR_RULE_LIST:
      ColorRuleGroup.Type statusColorRuleGroupType = IntentUtil
          .retrieveColorRuleTypeFromBundle(this.getIntent().getExtras());
      this.showStatusColorRuleListFragment(statusColorRuleGroupType, false);
      break;
    default:
      throw new IllegalArgumentException(
          "Unrecognized fragment type: " + this.mCurrentFragmentType);
    }
  }

  /**
   * Tries to get the table preference fragment out of the fragment manager and show it, or
   * creates a new one and adds it if it didn't exist
   */
  public void showTablePreferenceFragment() {
    this.mCurrentFragmentType = FragmentType.TABLE_PREFERENCE;
    TablePreferenceFragment tablePreferenceFragment = findTablePreferenceFragment();

    if (tablePreferenceFragment == null) {
      tablePreferenceFragment = createTablePreferenceFragment();

      getSupportFragmentManager().beginTransaction().replace(android.R.id.content,
          tablePreferenceFragment,
          Constants.FragmentTags.TABLE_PREFERENCE).commit();
    }
  }

  /**
   * Tries to get the column list fragment out of the fragment manager and show it, or
   * creates a new one and adds it if it didn't exist
   */
  public void showColumnListFragment() {
    this.mCurrentFragmentType = FragmentType.COLUMN_LIST;
    ColumnListFragment columnManagerFragment = findColumnListFragment();
    if (columnManagerFragment == null) {
      columnManagerFragment = TableLevelPreferencesActivity.createColumnListFragment();
    }
    getSupportFragmentManager().beginTransaction()
        .replace(android.R.id.content, columnManagerFragment, Constants.FragmentTags.COLUMN_LIST)
        .addToBackStack(null).commit();
  }

  /**
   * Show the fragment to edit color rules.
   *
   * @param colorRuleGroupType the type of color rule
   * @param elementKey         should be null if the color rule group is not of type
   *                           {@link ColorRuleGroup.Type#COLUMN}
   * @param rulePosition       the index into the list of color rules
   */
  public void showEditColorRuleFragmentForExistingRule(ColorRuleGroup.Type colorRuleGroupType,
      String elementKey, int rulePosition) {
    helperShowEditColorRuleFragment(false, colorRuleGroupType, elementKey, rulePosition);
  }

  /**
   * Attempts to show an EditColorRuleFragment for a new color rule
   *
   * @param colorRuleGroupType the type of color rule to add
   * @param elementKey         the column the color rule operates on
   */
  public void showEditColorRuleFragmentForNewRule(ColorRuleGroup.Type colorRuleGroupType,
      String elementKey) {
    helperShowEditColorRuleFragment(true, colorRuleGroupType, elementKey,
        EditColorRuleFragment.INVALID_RULE_POSITION);
  }

  private void helperShowEditColorRuleFragment(boolean isNewRule,
      ColorRuleGroup.Type colorRuleGroupType, String elementKey, int rulePosition) {
    this.mElementKeyOfDisplayedColumn = elementKey;
    // So much state is stored in this that we're just going to always create
    // a new one for now.
    EditColorRuleFragment fragment;
    if (isNewRule) {
      fragment = EditColorRuleFragment.newInstanceForNewRule(colorRuleGroupType, elementKey);
    } else {
      fragment = EditColorRuleFragment
          .newInstanceForExistingRule(colorRuleGroupType, elementKey, rulePosition);
    }
    getSupportFragmentManager().beginTransaction()
        .replace(android.R.id.content, fragment, Constants.FragmentTags.EDIT_COLOR_RULE)
        .addToBackStack(null).commit();
  }

  /**
   * Attempts to find the color rule list fragment in the fragment manager, or create it if it
   * doesn't exist. Adds it to the back stack
   *
   * @param colorRuleGroupType the type of color rule
   */
  public void showStatusColorRuleListFragment(ColorRuleGroup.Type colorRuleGroupType) {
    showStatusColorRuleListFragment(colorRuleGroupType, true);
  }

  /**
   * Attempts to find the color rule list fragment in the fragment manager, or create it if it
   * doesn't exist. Optionally adds it to the backstack
   *
   * @param colorRuleGroupType the type of color rule
   * @param addToBackStack     whether to add it to the fragment stack
   */
  public void showStatusColorRuleListFragment(ColorRuleGroup.Type colorRuleGroupType,
      boolean addToBackStack) {
    mCurrentFragmentType = FragmentType.STATUS_COLOR_RULE_LIST;
    StatusColorRuleListFragment statusColorRuleListFragment = this
        .findStatusColorRuleListFragment();
    if (statusColorRuleListFragment == null) {
      statusColorRuleListFragment = TableLevelPreferencesActivity
          .createStatusColorRuleListFragment(colorRuleGroupType);
    }
    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
    fragmentTransaction.replace(android.R.id.content, statusColorRuleListFragment,
        Constants.FragmentTags.STATUS_COLOR_RULE_LIST);
    if (addToBackStack) {
      fragmentTransaction.addToBackStack(null);
    }
    fragmentTransaction.commit();
  }

  /**
   * Wrapper around {@link #showColorRuleListFragment(String, ColorRuleGroup.Type, boolean)}
   * with addToBackStack set to true.
   *
   * @param elementKey         the column the rule operates on
   * @param colorRuleGroupType the type of color rule
   */
  public void showColorRuleListFragment(String elementKey, ColorRuleGroup.Type colorRuleGroupType) {
    showColorRuleListFragment(elementKey, colorRuleGroupType, true);
  }

  /**
   * Shows the color rule list fragment if it already exists, or creates a new one. Optionally
   * add it to the backstack
   *
   * @param elementKey         the column to list color rules for
   * @param colorRuleGroupType the type of color rules
   * @param addToBackStack     whether to add it to the back stack or not
   */
  public void showColorRuleListFragment(String elementKey, ColorRuleGroup.Type colorRuleGroupType,
      boolean addToBackStack) {
    this.mElementKeyOfDisplayedColumn = elementKey;
    this.mCurrentFragmentType = FragmentType.COLOR_RULE_LIST;
    ColorRuleListFragment colorRuleListFragment = findColorRuleListFragment();
    if (colorRuleListFragment == null) {
      colorRuleListFragment = TableLevelPreferencesActivity
          .createColorRuleListFragment(colorRuleGroupType);
    }
    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
    fragmentTransaction.replace(android.R.id.content, colorRuleListFragment,
        Constants.FragmentTags.COLOR_RULE_LIST);
    if (addToBackStack) {
      fragmentTransaction.addToBackStack(null);
    }
    fragmentTransaction.commit();
  }

  /**
   * Tries to get a column preferences fragment out of the fragment manager or create it if it
   * doesn't exist. Adds to back stack
   *
   * @param elementKey the column to open preferences for
   */
  public void showColumnPreferenceFragment(String elementKey) {
    showColumnPreferenceFragment(elementKey, true);
  }

  /**
   * Tries to get a column preferences fragment out of the fragment manager or create it if it
   * doesn't exist. Optionally adds to back stack
   *
   * @param elementKey     the column to open preferences for
   * @param addToBackStack whether to add to the back stack or not
   */
  public void showColumnPreferenceFragment(String elementKey, boolean addToBackStack) {
    mElementKeyOfDisplayedColumn = elementKey;
    mCurrentFragmentType = FragmentType.COLUMN_PRFERENCE;
    ColumnPreferenceFragment columnPreferenceFragment = findColumnPreferenceFragment();
    if (columnPreferenceFragment == null) {
      columnPreferenceFragment = new ColumnPreferenceFragment();
    }
    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    transaction.replace(android.R.id.content, columnPreferenceFragment,
        Constants.FragmentTags.COLUMN_PREFERENCE);
    if (addToBackStack) {
      transaction.addToBackStack(null);
    }
    transaction.commit();

  }

  /**
   * Get the element key of the column being displayed. May be null if there
   * is no associated column.
   *
   * @return the column that the current fragment is operating on
   */
  public String getElementKey() {
    return this.mElementKeyOfDisplayedColumn;
  }

  /**
   * Find the {@link TablePreferenceFragment} associated with this activity or
   * null if one does not exist.
   *
   * @return the preference fragment from the fragment manager if it exists, else null
   */
  TablePreferenceFragment findTablePreferenceFragment() {
    return (TablePreferenceFragment) getSupportFragmentManager()
        .findFragmentByTag(Constants.FragmentTags.TABLE_PREFERENCE);
  }

  /**
   * Find the {@link ColumnListFragment} associated with this activity or
   * null if one does not exist.
   *
   * @return the active ColumnListFragment if it exists or null
   */
  ColumnListFragment findColumnListFragment() {
    return (ColumnListFragment) getSupportFragmentManager()
        .findFragmentByTag(Constants.FragmentTags.COLUMN_LIST);
  }

  /**
   * Find the {@link ColumnPreferenceFragment} associated with this activity or
   * null if one does not exist.
   *
   * @return the active column preferences fragment if it exists, or null
   */
  ColumnPreferenceFragment findColumnPreferenceFragment() {
    return (ColumnPreferenceFragment) getSupportFragmentManager()
        .findFragmentByTag(Constants.FragmentTags.COLUMN_PREFERENCE);
  }

  /**
   * Find the {@link StatusColorRuleListFragment} associated with this activity or null if
   * one does not exist.
   *
   * @return the active color rule list fragment if it exists, or null
   */
  StatusColorRuleListFragment findStatusColorRuleListFragment() {
    return (StatusColorRuleListFragment) getSupportFragmentManager()
        .findFragmentByTag(Constants.FragmentTags.STATUS_COLOR_RULE_LIST);
  }

  /**
   * Find the {@link ColorRuleListFragment} associated with this activity or null if
   * one does not exist.
   *
   * @return the active ColorRuleListFragment if it exists, or null
   */
  ColorRuleListFragment findColorRuleListFragment() {
    return (ColorRuleListFragment) getSupportFragmentManager()
        .findFragmentByTag(Constants.FragmentTags.COLOR_RULE_LIST);
  }

  /**
   * Retrieve the {@link FragmentType}. A value stored in
   * savedInstanceState takes precedence. If not present of savedInstanceState is null, returns
   * the value from the intent.
   *
   * @param savedInstanceState the bundle we saved in onSaveInstanceState
   * @return the fragment type stored in the bundle
   */
  FragmentType retrieveFragmentTypeFromBundleOrActivity(Bundle savedInstanceState) {
    String fragmentTypeStr = null;
    if (savedInstanceState != null && savedInstanceState
        .containsKey(Constants.IntentKeys.TABLE_PREFERENCE_FRAGMENT_TYPE)) {
      fragmentTypeStr = savedInstanceState
          .getString(Constants.IntentKeys.TABLE_PREFERENCE_FRAGMENT_TYPE);
    }
    if (fragmentTypeStr == null) {
      fragmentTypeStr = this.getIntent()
          .getStringExtra(Constants.IntentKeys.TABLE_PREFERENCE_FRAGMENT_TYPE);
    }
    return FragmentType.valueOf(fragmentTypeStr);
  }

  /**
   * Retrieve the element key from either the savedInstanceState or this
   * activity's intent. Any value in savedInstanceState gets precedence.
   *
   * @param savedInstanceState the bundle we saved in onSaveInstanceState
   * @return the column key stored in the bundle
   */
  String retrieveElementKeyFromBundleOrActivity(Bundle savedInstanceState) {
    String result = null;
    if (savedInstanceState != null && savedInstanceState
        .containsKey(OdkData.IntentKeys.ELEMENT_KEY)) {
      result = savedInstanceState.getString(OdkData.IntentKeys.ELEMENT_KEY);
    }
    if (result == null) {
      result = this.getIntent().getStringExtra(OdkData.IntentKeys.ELEMENT_KEY);
    }
    return result;
  }

  /**
   * Fragment types this activity could be displaying. Used in restoring
   * instance state.
   *
   * @author sudar.sam@gmail.com
   */
  @SuppressWarnings("JavaDoc")
  public enum FragmentType {
    TABLE_PREFERENCE, COLUMN_LIST, COLUMN_PRFERENCE, COLOR_RULE_LIST, STATUS_COLOR_RULE_LIST
  }

}
