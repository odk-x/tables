package org.opendatakit.tables.activities;

import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.tables.fragments.ColorRuleListFragment;
import org.opendatakit.tables.fragments.ColumnListFragment;
import org.opendatakit.tables.fragments.ColumnPreferenceFragment;
import org.opendatakit.tables.fragments.TablePreferenceFragment;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;

import android.app.FragmentManager;
import android.os.Bundle;


/**
 * Displays preferences for a table to the user. This includes all preferences
 * that apply at a table level.
 * @author sudar.sam@gmail.com
 *
 */
public class TableLevelPreferencesActivity extends AbsTableActivity {
  
  /**
   * Fragment types this activity could be displaying. Used in restoring
   * instance state.
   * @author sudar.sam@gmail.com
   *
   */
  public enum FragmentType {
    TABLE_PREFERENCE,
    COLUMN_LIST,
    COLUMN_PRFERENCE,
    COLOR_RULE_LIST;
  }
      
  FragmentType mCurrentFragmentType;
  /**
   * The element key of the column we're displaying, if this activity is
   *  associated with a column fragmnent.
   */
  String mElementKeyOfDisplayedColumn;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    FragmentType fragmentTypeToDisplay =
        this.retrieveFragmentTypeFromBundleOrActivity(savedInstanceState);
    this.mCurrentFragmentType = fragmentTypeToDisplay;
    String elementKey =
        this.retrieveElementKeyFromBundleOrActivity(savedInstanceState);
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
      this.showColumnPreferenceFragment(elementKey);
      break;
    case COLOR_RULE_LIST:
      ColorRuleGroup.Type colorRuleGroupType =
        IntentUtil.retrieveColorRuleTypeFromBundle(
            this.getIntent().getExtras());
      this.showColorRuleListFragment(elementKey, colorRuleGroupType);
    default:
      throw new IllegalArgumentException(
          "Unrecognized fragment type: " +
              this.mCurrentFragmentType);
    }
  }
  
  public void showTablePreferenceFragment() {
    this.mCurrentFragmentType = FragmentType.TABLE_PREFERENCE;
    FragmentManager fragmentManager = this.getFragmentManager();
    TablePreferenceFragment tablePreferenceFragment =
        this.findTablePreferenceFragment();
      if (tablePreferenceFragment == null ) {
        tablePreferenceFragment = this.createTablePreferenceFragment();
      }
      fragmentManager.beginTransaction().replace(
          android.R.id.content,
          tablePreferenceFragment,
          Constants.FragmentTags.TABLE_PREFERENCE).commit();
  }
  
  public void showColumnListFragment() {
    this.mCurrentFragmentType = FragmentType.COLUMN_LIST;
    FragmentManager fragmentManager = this.getFragmentManager();
    ColumnListFragment columnManagerFragment =
        this.findColumnListFragment();
      if (columnManagerFragment == null) {
        columnManagerFragment = this.createColumnListFragment();
      }
      fragmentManager.beginTransaction().replace(
          android.R.id.content,
          columnManagerFragment,
          Constants.FragmentTags.COLUMN_LIST)
        .addToBackStack(null)
        .commit();
  }
  
  public void showColorRuleListFragment(
      String elementKey,
      ColorRuleGroup.Type colorRuleGroupType) {
    this.mElementKeyOfDisplayedColumn = elementKey;
    this.mCurrentFragmentType = FragmentType.COLOR_RULE_LIST;
    ColorRuleListFragment colorRuleListFragment =
        this.findColorRuleListFragment();
    if (colorRuleListFragment == null) {
      colorRuleListFragment =
          this.createColorRuleListFragment(colorRuleGroupType);
    }
    FragmentManager fragmentManager = this.getFragmentManager();
    fragmentManager.beginTransaction().replace(
        android.R.id.content,
        colorRuleListFragment,
        Constants.FragmentTags.COLOR_RULE_LIST)
      .addToBackStack(null)
      .commit();
  }
  
  TablePreferenceFragment createTablePreferenceFragment() {
    TablePreferenceFragment result = new TablePreferenceFragment();
    return result;
  }
  
  ColorRuleListFragment createColorRuleListFragment(
      ColorRuleGroup.Type colorRuleGroupType) {
    ColorRuleListFragment result =
        ColorRuleListFragment.newInstance(colorRuleGroupType);
    return result;
  }
  
  ColumnListFragment createColumnListFragment() {
    ColumnListFragment result = new ColumnListFragment();
    return result;
  }
  
  public void showColumnPreferenceFragment(String elementKey) {
    this.mElementKeyOfDisplayedColumn = elementKey;
    this.mCurrentFragmentType = FragmentType.COLUMN_PRFERENCE;
    FragmentManager fragmentManager = this.getFragmentManager();
    ColumnPreferenceFragment columnPreferenceFragment =
        this.findColumnPreferenceFragment();
    if (columnPreferenceFragment == null) {
      columnPreferenceFragment = new ColumnPreferenceFragment();
    }
    fragmentManager.beginTransaction().replace(
        android.R.id.content,
        columnPreferenceFragment,
        Constants.FragmentTags.COLUMN_PREFERENCE)
      .addToBackStack(null)
      .commit();
    
  }
  
  /**
   * Get the element key of the column being displayed. May be null if there
   * is no associated column.
   * @return
   */
  public String getElementKey() {
    return this.mElementKeyOfDisplayedColumn;
  }
  
  /**
   * Find the {@link TablePreferenceFragment} associated with this activity or
   * null if one does not exist.
   * @return
   */
  TablePreferenceFragment findTablePreferenceFragment() {
    FragmentManager fragmentManager = this.getFragmentManager();
    TablePreferenceFragment result = (TablePreferenceFragment) 
        fragmentManager.findFragmentByTag(
            Constants.FragmentTags.TABLE_PREFERENCE);
    return result;
  }
  
  /**
   * Find the {@link ColumnListFragment} associated with this activity or
   * null if one does not exist.
   * @return
   */
  ColumnListFragment findColumnListFragment() {
    FragmentManager fragmentManager = this.getFragmentManager();
    ColumnListFragment result = (ColumnListFragment) 
        fragmentManager.findFragmentByTag(
            Constants.FragmentTags.COLUMN_LIST);
    return result;
  }
  
  /**
   * Find the {@link ColumnPreferenceFragment} associated with this activity or
   * null if one does not exist.
   * @return
   */
  ColumnPreferenceFragment findColumnPreferenceFragment() {
    FragmentManager fragmentManager = this.getFragmentManager();
    ColumnPreferenceFragment result = (ColumnPreferenceFragment) 
        fragmentManager.findFragmentByTag(
            Constants.FragmentTags.COLUMN_PREFERENCE);
    return result;
  }
  
  /**
   * Find the {@link ColorRuleListFragment} associated with this activity or null if
   * one does not exist.
   * @return
   */
  ColorRuleListFragment findColorRuleListFragment() {
    FragmentManager fragmentManager = this.getFragmentManager();
    ColorRuleListFragment result = (ColorRuleListFragment)
        fragmentManager.findFragmentByTag(
            Constants.FragmentTags.COLOR_RULE_LIST);
    return result;
  }
  
  /**
   * Retrieve the {@link FragmentType}. A value stored in
   * savedInstanceState takes precedence. If not present of savedInstanceState
   * is null, returns the value from the intent.
   * @param savedInstanceState
   * @return
   */
  FragmentType retrieveFragmentTypeFromBundleOrActivity(
      Bundle savedInstanceState) {
    String fragmentTypeStr = null;
    if (savedInstanceState != null &&
        savedInstanceState.containsKey(
            Constants.IntentKeys.TABLE_PREFERENCE_FRAGMENT_TYPE)) {
      fragmentTypeStr = savedInstanceState.getString(
          Constants.IntentKeys.TABLE_PREFERENCE_FRAGMENT_TYPE);
    }
    if (fragmentTypeStr == null) {
      fragmentTypeStr = this.getIntent().getStringExtra(
          Constants.IntentKeys.TABLE_PREFERENCE_FRAGMENT_TYPE);
    }
    FragmentType result = FragmentType.valueOf(fragmentTypeStr);
    return result;
  }
  
  /**
   * Retrieve the element key from either the savedInstanceState or this
   * activity's intent. Any value in savedInstanceState gets precedence.
   * @param savedInstanceState
   * @return
   */
  String retrieveElementKeyFromBundleOrActivity(Bundle savedInstanceState) {
    String result = null;
    if (savedInstanceState != null &&
        savedInstanceState.containsKey(Constants.IntentKeys.ELEMENT_KEY)) {
      result = savedInstanceState.getString(Constants.IntentKeys.ELEMENT_KEY);
    }
    if (result == null) {
      result =
          this.getIntent().getStringExtra(Constants.IntentKeys.ELEMENT_KEY);
    }
    return result;
  }

}
