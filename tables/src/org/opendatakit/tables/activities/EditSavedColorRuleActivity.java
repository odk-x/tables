/*
 * Copyright (C) 2012 University of Washington
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
package org.opendatakit.tables.activities;

import java.util.List;

import org.opendatakit.common.android.data.ColorRule;
import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.data.ColumnProperties;
import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.KeyValueStoreHelper.AspectHelper;
import org.opendatakit.tables.R;
import org.opendatakit.tables.preferences.EditColorPreference;
import org.opendatakit.tables.preferences.EditNameDialogPreference;
import org.opendatakit.tables.preferences.EditSavedViewEntryHandler;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.views.ColorPickerDialog.OnColorChangedListener;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.util.Log;

public class EditSavedColorRuleActivity extends PreferenceActivity
    implements EditSavedViewEntryHandler, OnColorChangedListener {

  private static final String TAG =
      EditSavedColorRuleActivity.class.getName();

  private static final String PREFERENCE_KEY_COMP_TYPE = "pref_comp_type";
  private static final String PREFERENCE_KEY_VALUE = "pref_value";
  private static final String PREFERENCE_KEY_TEXT_COLOR = "pref_text_color";
  private static final String PREFERENCE_KEY_ELEMENT_KEY = "pref_element_key";
  private static final String PREFERENCE_KEY_BACKGROUND_COLOR =
      "pref_background_color";
  private static final String PREFERENCE_KEY_SAVE_BUTTON = "save_button";

  public static final String INTENT_KEY_TABLE_ID = "tableId";
  public static final String INTENT_KEY_ELEMENT_KEY = "elementKey";
  /**
   * The type of the color rule group you're editing.
   */
  public static final String INTENT_KEY_RULE_GROUP_TYPE = "ruleGroupType";

  /**
   * The position of the rule to be edited. {@link INTENT_FLAG_NEW_RULE}
   * indicates that it is in fact a new rule being added.
   */
  public static final String INTENT_KEY_RULE_POSITION = "rulePosition";
  public static final int INTENT_FLAG_NEW_RULE = -1;

  /*
   * The keys for communicating with EditColorPreference.
   */
  public static final String COLOR_PREF_KEY_TEXT = "textKey";
  public static final String COLOR_PREF_KEY_BACKGROUND = "backgroundKey";

  private String mAppName;
  private String mTableId;
  private int mRulePosition;
  private TableProperties mTp;
  private KeyValueStoreHelper mKvsh;
  private AspectHelper mAspectHelper;
  // The values to display
  private CharSequence[] mHumanValues;
  // The values to actually set.
  private CharSequence[] mEntryVales;
  private CharSequence[] mColumnDisplayNames;
  private CharSequence[] mColumnElementKeys;
  private ColorRuleGroup mColorRuleGroup;
  private List<ColorRule> mColorRules;
  private EditNameDialogPreference mValuePreference;
  private ColorRuleGroup.Type mType;
  private Preference mSaveButton;

  // These are the fields that define the rule.
  private String mElementKey;
  private String mRuleValue;
  private ColorRule.RuleType mRuleOperator;
  private Integer mForegroundColor;
  private Integer mBackgroundColor;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.mAppName = getIntent().getStringExtra(Constants.IntentKeys.APP_NAME);
    this.mTableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
    this.mElementKey = getIntent().getStringExtra(INTENT_KEY_ELEMENT_KEY);
    this.mRulePosition = getIntent().getIntExtra(INTENT_KEY_RULE_POSITION,
        INTENT_FLAG_NEW_RULE);
    this.mType = ColorRuleGroup.Type.valueOf(
        getIntent().getStringExtra(INTENT_KEY_RULE_GROUP_TYPE));
    this.mTp = TableProperties.getTablePropertiesForTable(this, mAppName, mTableId);
    this.mKvsh =
        mTp.getKeyValueStoreHelper(ColorRuleGroup.KVS_PARTITION_COLUMN);
    this.mAspectHelper = mKvsh.getAspectHelper(mElementKey);
    addPreferencesFromResource(R.xml.preference_row_color_rule_entry);
    this.mHumanValues = ColorRule.RuleType.getValues();
    this.mEntryVales = ColorRule.RuleType.getValues();
    int numberOfDisplayColumns = mTp.getNumberOfDisplayColumns();
    this.mColumnDisplayNames = new CharSequence[numberOfDisplayColumns];
    this.mColumnElementKeys = new CharSequence[numberOfDisplayColumns];
    for (int i = 0; i < numberOfDisplayColumns; i++) {
      ColumnProperties cp = mTp.getColumnByIndex(i);
      mColumnDisplayNames[i] = cp.getLocalizedDisplayName();
      mColumnElementKeys[i] = cp.getElementKey();
    }
    this.setTitle(getString(R.string.edit_rule));
  }

  @Override
  public void onResume() {
    super.onResume();
    init();
  }

  private void init() {
    // Which rule group we call depends on the column of interest. If the
    // column is editable, then we want to get it from the row.
    switch (mType) {
    case COLUMN:
      this.mColorRuleGroup =
        ColorRuleGroup.getColumnColorRuleGroup(mTp, mElementKey);
      break;
    case TABLE:
      this.mColorRuleGroup = ColorRuleGroup.getTableColorRuleGroup(mTp);
      break;
    case STATUS_COLUMN:
      this.mColorRuleGroup = ColorRuleGroup.getStatusColumnRuleGroup(mTp);
      break;
    default:
      Log.e(TAG, "unrecognized type in init: " + mType);
    }
    this.mColorRules = mColorRuleGroup.getColorRules();
    // Set the appropriate state backing this rule.
    if (mRulePosition == INTENT_FLAG_NEW_RULE) {
      mRuleValue = getString(R.string.compared_to_value);
      mRuleOperator = null;
      mForegroundColor = Constants.DEFAULT_TEXT_COLOR;
      mBackgroundColor = Constants.DEFAULT_BACKGROUND_COLOR;
    } else {
      mElementKey = mColorRules.get(mRulePosition).getColumnElementKey();
      mRuleValue = mColorRules.get(mRulePosition).getVal();
      mRuleOperator = mColorRules.get(mRulePosition).getOperator();
      mForegroundColor = mColorRules.get(mRulePosition).getForeground();
      mBackgroundColor = mColorRules.get(mRulePosition).getBackground();
    }

    if (mType == ColorRuleGroup.Type.TABLE ||
        mType == ColorRuleGroup.Type.STATUS_COLUMN) {
      final ListPreference columnPreference =
          (ListPreference) findPreference(PREFERENCE_KEY_ELEMENT_KEY);
      columnPreference.setEntries(mColumnDisplayNames);
      columnPreference.setEntryValues(mColumnElementKeys);
      columnPreference.setPersistent(false);
      if (mRulePosition != INTENT_FLAG_NEW_RULE) {
        String displayName =
            mTp.getColumnByElementKey(mColorRules.get(mRulePosition)
                .getColumnElementKey()).getLocalizedDisplayName();
        columnPreference.setSummary(displayName);
        columnPreference.setValueIndex(
            columnPreference.findIndexOfValue(mElementKey));
      }
      columnPreference.setOnPreferenceChangeListener(
          new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
          Log.d(TAG, "onPreferenceChance callback invoked for value: "
              + newValue);
          mElementKey = (String) newValue;
          String displayName =
              mTp.getColumnByElementKey(mElementKey).getLocalizedDisplayName();
          columnPreference.setSummary(displayName);
          columnPreference.setValueIndex(
              columnPreference.findIndexOfValue(mElementKey));
          updateStateOfSaveButton();
          // false so we don't persist the value and pass it b/w rules
          return false;
        }
      });
    } else {
      getPreferenceScreen().removePreference(
          (ListPreference) findPreference(PREFERENCE_KEY_ELEMENT_KEY));
    }

    final ListPreference operatorPreference =
        (ListPreference) findPreference(PREFERENCE_KEY_COMP_TYPE);
    operatorPreference.setEntries(mHumanValues);
    operatorPreference.setEntryValues(mEntryVales);
    operatorPreference.setPersistent(false);
    operatorPreference.setOnPreferenceChangeListener(
        new OnPreferenceChangeListener() {

      @Override
      public boolean onPreferenceChange(Preference preference,
          Object newValue) {
        // Here we want to update the rule and also persist it.
        Log.d(TAG, "onPreferenceChange callback invoked for value: " +
            (String) newValue);
        ColorRule.RuleType newOperator =
            ColorRule.RuleType.getEnumFromString((String) newValue);
        mRuleOperator = newOperator;
        preference.setSummary(mRuleOperator.getSymbol());
        operatorPreference.setValueIndex(
            operatorPreference.findIndexOfValue(mRuleOperator.getSymbol()));
        updateStateOfSaveButton();
        // false so we don't persist the value and pass it b/w rules
        return false;
      }
    });
    if (mRuleOperator != null) {
      operatorPreference.setSummary(mRuleOperator.getSymbol());
      operatorPreference.setValueIndex(
          operatorPreference.findIndexOfValue(mRuleOperator.getSymbol()));
    }

    this.mValuePreference =
        (EditNameDialogPreference) findPreference(PREFERENCE_KEY_VALUE);
    mValuePreference.setCallingActivity(this);
    mValuePreference.setSummary(mRuleValue);

    EditColorPreference textColorPref =
        (EditColorPreference) findPreference(PREFERENCE_KEY_TEXT_COLOR);
    textColorPref.setCallingActivity(this);
    textColorPref.initColorPickerListener(this, COLOR_PREF_KEY_TEXT,
        getString(R.string.text_color), mForegroundColor);

    EditColorPreference backgroundColorPref =
        (EditColorPreference) findPreference(PREFERENCE_KEY_BACKGROUND_COLOR);
    backgroundColorPref.setCallingActivity(this);
    backgroundColorPref.initColorPickerListener(this, COLOR_PREF_KEY_BACKGROUND,
        getString(R.string.background_color), mBackgroundColor);

    // Set up the save button.
    mSaveButton =  (Preference) findPreference(PREFERENCE_KEY_SAVE_BUTTON);
    mSaveButton.setOnPreferenceClickListener(
        new Preference.OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        if (ruleIsValid()) {
          saveRule();
        }
        return true;
      }
    });
    updateStateOfSaveButton();
  }

  /**
   * Puts the value of the rule into the state of the current object and
   * updates the preference summary to display this value.
   */
  @Override
  public void tryToSaveNewName(String value) {
    if (value == null) return;
    mRuleValue = value;
    mValuePreference.setSummary(value);
    mColorRuleGroup.replaceColorRuleList(mColorRules);
    mColorRuleGroup.saveRuleList();
    updateStateOfSaveButton();
  }

  /**
   * Kind of overloaded this method. Returns the value of the rule.
   * @return
   */
  @Override
  public String getCurrentViewName() {
    if (mRulePosition == INTENT_FLAG_NEW_RULE) {
      return getString(R.string.compared_to_value);
    } else {
      return mRuleValue;
    }
  }

  @Override
  public void colorChanged(String key, int color) {
    if (key.equals(COLOR_PREF_KEY_TEXT)) {
      mForegroundColor = color;
    } else if (key.equals(COLOR_PREF_KEY_BACKGROUND)) {
      mBackgroundColor = color;
    } else {
      Log.e(TAG, "unrecognized key: " + key);
    }
    updateStateOfSaveButton();
  }

  /**
   * Checks state of the objects backing the color rule activity. Returns true
   * if the constructed rule would be valid (e.g. no null values), otherwise
   * false.
   * @return
   */
  private boolean ruleIsValid() {
    return mElementKey != null
        && mRuleValue != null
        && mRuleOperator != null
        && mForegroundColor != null
        && mBackgroundColor != null;
  }

  /**
   * Constructs a new rule from the fields and saves the existing rule into the
   *  database. Note that this MUST not be
   * called unless ruleIsValid returns true. Otherwise you could get null
   * values in the database that will crash the app.
   */
  private void saveRule() {
    ColorRule newRule = constructColorRuleFromState();
    if (mRulePosition == INTENT_FLAG_NEW_RULE) {
      mColorRules.add(newRule);
      mRulePosition = mColorRules.size() - 1; // it's the new last one.
    } else {
      mColorRules.set(mRulePosition, newRule);
    }
    mColorRuleGroup.replaceColorRuleList(mColorRules);
    mColorRuleGroup.saveRuleList();
    updateStateOfSaveButton();
  }

  /**
   * Return a new color rule based on the state of the activity. If the
   * ruleIsValid() currently returns null, null is returned.
   * @return
   */
  private ColorRule constructColorRuleFromState() {
    if (ruleIsValid()) {
      return new ColorRule(mElementKey, mRuleOperator, mRuleValue,
          mForegroundColor.intValue(), mBackgroundColor.intValue());
    } else {
      return null;
    }
  }

  /**
   * If the rule is valid and the intent flag for a new rule is set, then
   * enables the button because you can save a new one. If the rule is valid
   * and it's the same as the one that currently exists at that spot in the
   * rule list, it is disabled.
   */
  private void updateStateOfSaveButton() {
    if (ruleIsValid()) {
      if (mRulePosition != INTENT_FLAG_NEW_RULE) {
        ColorRule localRule = constructColorRuleFromState();
        if (localRule.equalsWithoutId(mColorRules.get(mRulePosition))) {
          mSaveButton.setEnabled(false);
        } else {
          mSaveButton.setEnabled(true);
        }
      } else {
        mSaveButton.setEnabled(true);
      }
    } else {
      mSaveButton.setEnabled(false);
    }
  }


}
