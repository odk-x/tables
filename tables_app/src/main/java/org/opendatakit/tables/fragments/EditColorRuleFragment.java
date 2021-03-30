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

import android.os.Bundle;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import android.widget.Toast;
import org.opendatakit.data.ColorRule;
import org.opendatakit.data.ColorRuleGroup;
import org.opendatakit.data.utilities.ColumnUtil;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.properties.CommonToolProperties;
import org.opendatakit.properties.PropertiesSingleton;
import org.opendatakit.tables.R;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.preferences.EditColorPreference;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.views.ColorPickerDialog.OnColorChangedListener;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author sudar.sam@gmail.com
 */
public class EditColorRuleFragment extends AbsTableLevelPreferenceFragment
    implements OnColorChangedListener {

  /**
   * Key for communicating text color with EditColorPreference.
   */
  public static final String COLOR_PREF_KEY_TEXT = "textKey";
  /**
   * Key for communicating background color with EditColorPreference.
   */
  public static final String COLOR_PREF_KEY_BACKGROUND = "backgroundKey";
  /**
   * A value signifying the fragment is being used to add a new rule.
   */
  public static final int INVALID_RULE_POSITION = -1;
  private static final String TAG = EditColorRuleFragment.class.getSimpleName();
  // These are the fields that define the rule.
  private String mElementKey;
  private String mRuleValue;
  private ColorRule.RuleType mRuleOperator;
  private Integer mTextColor;
  private Integer mBackgroundColor;

  private CharSequence[] mOperatorHumanFriendlyValues;
  private CharSequence[] mOperatorEntryValues;
  private CharSequence[] mColumnDisplayNames;
  private CharSequence[] mColumnElementKeys;
  private ColorRuleGroup mColorRuleGroup;
  /**
   * The position in the rule list that we are editing.
   */
  private int mRulePosition;
  private ColorRuleGroup.Type mColorRuleGroupType;

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
  }

  /**
   * @param colorRuleGroupType added to the arguments for the new EditColorRuleFragment
   * @param elementKey         the elementKey the rule is for, if this is in a group of type
   *                           {@link ColorRuleGroup.Type#COLUMN}. If it is not, pass null.
   * @return a new EditColorRuleFragment with the requested color rule group type and column
   */
  public static EditColorRuleFragment newInstanceForNewRule(ColorRuleGroup.Type colorRuleGroupType,
      String elementKey) {
    return newInstanceForExistingRule(colorRuleGroupType, elementKey, INVALID_RULE_POSITION);
  }

  /**
   * @param colorRuleGroupType added to the arguments for the new EditColorRuleFragment
   * @param elementKey         the element key the rule is for if this is in a group of type
   *                           {@link ColorRuleGroup.Type#COLUMN}. If it is not, pass null.
   * @param rulePosition       the position of the rule in the group you're editing
   * @return a new EditColorRuleFragment with the requested options
   */
  public static EditColorRuleFragment newInstanceForExistingRule(
      ColorRuleGroup.Type colorRuleGroupType, String elementKey, int rulePosition) {
    Bundle arguments = new Bundle();
    IntentUtil.addColorRuleGroupTypeToBundle(arguments, colorRuleGroupType);
    IntentUtil.addElementKeyToBundle(arguments, elementKey);
    arguments.putInt(IntentKeys.RULE_POSITION, rulePosition);
    EditColorRuleFragment result = new EditColorRuleFragment();
    result.setArguments(arguments);
    return result;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.addPreferencesFromResource(R.xml.preference_color_rule_entry);
    String elementKey = IntentUtil.retrieveElementKeyFromBundle(savedInstanceState);
    if (elementKey == null) {
      elementKey = IntentUtil.retrieveElementKeyFromBundle(getArguments());
    }
    this.mElementKey = elementKey;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Bundle arguments = this.getArguments();
    ColorRuleGroup.Type colorRuleType = IntentUtil.retrieveColorRuleTypeFromBundle(arguments);
    int rulePosition = arguments.getInt(IntentKeys.RULE_POSITION);
    this.mColorRuleGroupType = colorRuleType;
    this.mRulePosition = rulePosition;
    if (this.mColorRuleGroupType == ColorRuleGroup.Type.COLUMN) {
      // then we also need to pull out the element key.
      mElementKey = IntentUtil.retrieveElementKeyFromBundle(arguments);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    DbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(getAppName());
      this.initializeStateRequiringContext(db);
      this.initializeAllPreferences(db);
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(getAppName()).printStackTrace(e);
      Toast.makeText(getActivity(), "Unable to access database", Toast.LENGTH_LONG).show();
    } finally {
      if (db != null) {
        try {
          Tables.getInstance().getDatabase().closeDatabase(getAppName(), db);
        } catch (ServicesAvailabilityException e) {
          WebLogger.getLogger(getAppName()).printStackTrace(e);
          Toast.makeText(getActivity(), "Error releasing database", Toast.LENGTH_LONG).show();
        }
      }
    }
  }

  /**
   * Set up the objects that require a context.
   *
   * @throws ServicesAvailabilityException if the database is down
   */
  private void initializeStateRequiringContext(DbHandle db) throws ServicesAvailabilityException {
    PropertiesSingleton props = CommonToolProperties
        .get(getActivity().getApplication(), getAppName());
    String userSelectedDefaultLocale = props.getUserSelectedDefaultLocale();
    UserDbInterface dbInterface = Tables.getInstance().getDatabase();
    // 1) First fill in the color rule group and list.
    TableUtil.TableColumns tc = TableUtil.get()
        .getTableColumns(userSelectedDefaultLocale, dbInterface, getAppName(), db, getTableId());

    switch (this.mColorRuleGroupType) {
    case COLUMN:
      this.mColorRuleGroup = ColorRuleGroup
          .getColumnColorRuleGroup(dbInterface, getAppName(), db, getTableId(), this.mElementKey,
              tc.adminColumns);
      break;
    case TABLE:
      this.mColorRuleGroup = ColorRuleGroup
          .getTableColorRuleGroup(dbInterface, getAppName(), db, getTableId(), tc.adminColumns);
      break;
    case STATUS_COLUMN:
      this.mColorRuleGroup = ColorRuleGroup
          .getStatusColumnRuleGroup(dbInterface, getAppName(), db, getTableId(), tc.adminColumns);
      break;
    default:
      throw new IllegalArgumentException(
          "unrecognized color rule group type: " + this.mColorRuleGroupType);
    }
    // 2) then fill in the starting state for the rule.
    if (this.isUnpersistedNewRule()) {
      // fill in dummy holder values
      this.mRuleValue = this.getActivity().getString(R.string.compared_to_value);
      this.mRuleOperator = null;
      this.mTextColor = Constants.DEFAULT_TEXT_COLOR;
      this.mBackgroundColor = Constants.DEFAULT_BACKGROUND_COLOR;
    } else {
      // fill in the values for the rule.
      // Get the rule we're editing.
      ColorRule startingRule = this.mColorRuleGroup.getColorRules().get(this.mRulePosition);
      this.mRuleValue = startingRule.getVal();
      this.mRuleOperator = startingRule.getOperator();
      this.mTextColor = startingRule.getForeground();
      this.mBackgroundColor = startingRule.getBackground();
      this.mElementKey = startingRule.getColumnElementKey();
    }
    // 3) then fill in the static things backing the dialogs.
    this.mOperatorHumanFriendlyValues = ColorRule.RuleType.getValues();
    this.mOperatorEntryValues = ColorRule.RuleType.getValues();

    ArrayList<String> colorColElementKeys = new ArrayList<>(
        tc.orderedDefns.getRetentionColumnNames());
    ArrayList<String> colorColDisplayNames = new ArrayList<>();
    for (String elementKey : colorColElementKeys) {
      if (elementKey.charAt(0) == '_') continue;
      String localizedDisplayName = tc.localizedDisplayNames.get(elementKey);
      colorColDisplayNames.add(localizedDisplayName);
    }

    colorColDisplayNames.addAll(Arrays.asList(tc.adminColumns));
    colorColElementKeys.addAll(Arrays.asList(tc.adminColumns));

    mColumnDisplayNames = colorColDisplayNames.toArray(new String[colorColDisplayNames.size()]);
    mColumnElementKeys = colorColElementKeys.toArray(new String[colorColElementKeys.size()]);
  }

  private void initializeAllPreferences(DbHandle db) throws ServicesAvailabilityException {
    // We have several things to do here. First we'll initialize all the
    // individual preferences.
    this.initializeColumns(db);
    this.initializeComparisonType();
    this.initializeRuleValue();
    this.initializeTextColor();
    this.initializeBackgroundColor();
    this.initializeSave();
    this.updateStateOfSaveButton();
  }

  /**
   * Return true if we are currently displaying a new rule that has not yet been
   * saved.
   *
   * @return whether the rule hasn't been saved yet
   */
  boolean isUnpersistedNewRule() {
    return this.mRulePosition == INVALID_RULE_POSITION;
  }

  private void initializeColumns(DbHandle db) throws ServicesAvailabilityException {
    final ListPreference pref = this
        .findListPreference(Constants.PreferenceKeys.ColorRule.ELEMENT_KEY);
    // And now we have to consider that we don't display this at all.
    if (this.mColorRuleGroupType == ColorRuleGroup.Type.COLUMN) {
      this.getPreferenceScreen().removePreference(pref);
      return;
    }
    PropertiesSingleton props = CommonToolProperties
        .get(getActivity().getApplication(), getAppName());
    String userSelectedDefaultLocale = props.getUserSelectedDefaultLocale();
    pref.setEntries(this.mColumnDisplayNames);
    pref.setEntryValues(mColumnElementKeys);
    UserDbInterface dbInterface = Tables.getInstance().getDatabase();
    if (!isUnpersistedNewRule()) {
      String localizedDisplayName;
      localizedDisplayName = ColumnUtil.get()
          .getLocalizedDisplayName(userSelectedDefaultLocale, dbInterface, getAppName(), db,
              getTableId(),
              this.mColorRuleGroup.getColorRules().get(mRulePosition).getColumnElementKey());
      pref.setSummary(localizedDisplayName);
      pref.setValueIndex(pref.findIndexOfValue(mElementKey));
    }
    pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        WebLogger.getLogger(getAppName())
            .d(TAG, "onPreferenceChance callback invoked for value: " + newValue);
        PropertiesSingleton props = CommonToolProperties
            .get(getActivity().getApplication(), getAppName());
        String userSelectedDefaultLocale = props.getUserSelectedDefaultLocale();

        UserDbInterface dbInterface = Tables.getInstance().getDatabase();
        mElementKey = (String) newValue;
        String localizedDisplayName = null;
        DbHandle db = null;
        try {
          db = dbInterface.openDatabase(getAppName());
          localizedDisplayName = ColumnUtil.get()
              .getLocalizedDisplayName(userSelectedDefaultLocale, dbInterface, getAppName(), db,
                  getTableId(), mElementKey);
        } catch (ServicesAvailabilityException e) {
          WebLogger.getLogger(getAppName()).printStackTrace(e);
        } finally {
          if (db != null) {
            try {
              dbInterface.closeDatabase(getAppName(), db);
            } catch (ServicesAvailabilityException e) {
              WebLogger.getLogger(getAppName()).printStackTrace(e);
            }
          }
        }
        if (localizedDisplayName == null) {
          return false;
        }
        pref.setSummary(localizedDisplayName);
        pref.setValueIndex(pref.findIndexOfValue(mElementKey));
        updateStateOfSaveButton();
        // false so we don't persist the value and pass it b/w rules
        return false;
      }
    });
  }

  private void initializeComparisonType() {
    final ListPreference pref = this
        .findListPreference(Constants.PreferenceKeys.ColorRule.COMPARISON_TYPE);
    pref.setEntries(this.mOperatorHumanFriendlyValues);
    pref.setEntryValues(this.mOperatorEntryValues);
    // now set the correct one as checked
    if (this.mRuleOperator != null) {
      pref.setSummary(this.mRuleOperator.getSymbol());
      pref.setValueIndex(pref.findIndexOfValue(mRuleOperator.getSymbol()));
    }
    pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        // Here we want to update the rule and also persist it.
        WebLogger.getLogger(getAppName())
            .d(TAG, "onPreferenceChange callback invoked for value: " + newValue);
        mRuleOperator = ColorRule.RuleType.getEnumFromString((String) newValue);
        preference.setSummary(mRuleOperator.getSymbol());
        pref.setValueIndex(pref.findIndexOfValue(mRuleOperator.getSymbol()));
        updateStateOfSaveButton();
        // false so we don't persist the value and pass it b/w rules
        return false;
      }
    });
  }

  private void initializeRuleValue() {
    final EditTextPreference pref = this
        .findEditTextPreference(Constants.PreferenceKeys.ColorRule.RULE_VALUE);
    pref.setSummary(this.mRuleValue);
    pref.setText(this.mRuleValue);
    pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        String newValueStr = (String) newValue;
        pref.setSummary(newValueStr);
        pref.setText(newValueStr);
        mRuleValue = newValueStr;
        updateStateOfSaveButton();
        return false;
      }
    });
  }

  private void initializeTextColor() {
    EditColorPreference pref = this
        .findPreference(Constants.PreferenceKeys.ColorRule.TEXT_COLOR);
    pref.initColorPickerListener(this, COLOR_PREF_KEY_TEXT,
        getActivity().getString(R.string.text_color), this.mTextColor);
  }

  private void initializeBackgroundColor() {
    EditColorPreference pref = this
        .findPreference(Constants.PreferenceKeys.ColorRule.BACKGROUND_COLOR);
    pref.initColorPickerListener(this, COLOR_PREF_KEY_BACKGROUND,
        getActivity().getString(R.string.background_color), this.mBackgroundColor);
  }

  private void initializeSave() {
    Preference pref = this.findPreference(Constants.PreferenceKeys.ColorRule.SAVE);
    pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        if (preferencesDefineValidRule()) {
          try {
            saveRule();
            return true;
          } catch (ServicesAvailabilityException e) {
            WebLogger.getLogger(getAppName()).printStackTrace(e);
            return false;
          }
        }
        return false;
      }
    });
  }

  private void saveRule() throws ServicesAvailabilityException {
    ColorRule newRule = constructColorRuleFromState();
    if (this.isUnpersistedNewRule()) {
      this.mColorRuleGroup.getColorRules().add(newRule);
      // We just added the rule to the end of the existing list, so the new
      // position is the last item in the list.
      mRulePosition = this.mColorRuleGroup.getColorRules().size() - 1;
    } else {
      this.mColorRuleGroup.getColorRules().set(mRulePosition, newRule);
    }
    mColorRuleGroup.saveRuleList(Tables.getInstance().getDatabase());
    updateStateOfSaveButton();
  }

  @Override
  public void colorChanged(String key, int color) {
    switch (key) {
    case COLOR_PREF_KEY_TEXT:
      this.mTextColor = color;
      break;
    case COLOR_PREF_KEY_BACKGROUND:
      this.mBackgroundColor = color;
      break;
    default:
      WebLogger.getLogger(getAppName()).e(TAG, "unrecognized key: " + key);
      break;
    }
    updateStateOfSaveButton();
  }

  /**
   * Return a new {@link ColorRule} based on the state. If the rule is not
   * valid, returns null
   *
   * @return the rule, or null if it is not valid
   */
  ColorRule constructColorRuleFromState() {
    if (preferencesDefineValidRule()) {
      return new ColorRule(mElementKey, mRuleOperator, mRuleValue, mTextColor, mBackgroundColor);
    } else {
      return null;
    }
  }

  /**
   * Sets the save button to be enabled or disabled based on whether it should be enabled or not
   */
  void updateStateOfSaveButton() {
    Preference savePref = findPreference(Constants.PreferenceKeys.ColorRule.SAVE);
    savePref.setEnabled(saveButtonShouldBeEnabled());
  }

  /**
   * Return true if the save button should be enabled. This depends on if the
   * rule is valid as well as if it differs from the saved version.
   *
   * @return whether the save button should be enabled
   */
  boolean saveButtonShouldBeEnabled() {
    if (preferencesDefineValidRule()) {
      // A valid rule can only be saved if it differs from the previous rule
      // OR if it is a new rule.
      ColorRule userDefinedRule = constructColorRuleFromState();
      if (this.isUnpersistedNewRule()) {
        return true;
      } else { // isUnpersistedRule
        ColorRule existingRule = this.mColorRuleGroup.getColorRules().get(this.mRulePosition);
        return !userDefinedRule.equalsWithoutId(existingRule);
      }
    } else { // preferencesDefineValidRule
      return false;
    }
  }

  /**
   * @return true if the current preference fields in the UI define a valid rule
   */
  boolean preferencesDefineValidRule() {
    return this.mElementKey != null && this.mRuleValue != null && this.mRuleOperator != null
        && this.mTextColor != null && this.mBackgroundColor != null;
  }

  /**
   * Keys used for storing and retreiving things from bundles
   */
  public static final class IntentKeys {
    /**
     * Key used for pulling a rule position out of a bundle
     */
    static final String RULE_POSITION = "rulePosition";

    /**
     * Do not instantiate this
     */
    private IntentKeys() {
    }
  }

}
