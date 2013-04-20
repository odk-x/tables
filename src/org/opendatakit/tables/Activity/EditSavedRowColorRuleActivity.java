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
package org.opendatakit.tables.Activity;

import java.util.List;

import org.opendatakit.tables.DataStructure.ColorRule;
import org.opendatakit.tables.DataStructure.ColorRuleGroup;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.KeyValueStoreHelper.AspectHelper;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.lib.EditColorPreference;
import org.opendatakit.tables.lib.EditNameDialogPreference;
import org.opendatakit.tables.lib.EditSavedViewEntryActivity;
import org.opendatakit.tables.util.ColorPickerDialog.OnColorChangedListener;
import org.opendatakit.tables.util.Constants;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.util.Log;

/**
 * Hopefully this class will be short-lived as we refactor to make the color 
 * rules more cohesive. For now, this is the activity that lets you interact
 * with a row color rule.
 * @author sudar.sam@gmail.com
 *
 */
public class EditSavedRowColorRuleActivity extends PreferenceActivity implements
    EditSavedViewEntryActivity, OnColorChangedListener {

  private static final String TAG = 
      EditSavedRowColorRuleActivity.class.getName();
  
  private static final String PREFERENCE_KEY_COMP_TYPE = "pref_comp_type";
  private static final String PREFERENCE_KEY_VALUE = "pref_value";
  private static final String PREFERENCE_KEY_TEXT_COLOR = "pref_text_color";
  private static final String PREFERENCE_KEY_ELEMENT_KEY = "pref_element_key";
  private static final String PREFERENCE_KEY_BACKGROUND_COLOR = 
      "pref_background_color";
  
  public static final String INTENT_KEY_TABLE_ID = "tableId";
  public static final String INTENT_KEY_ELEMENT_KEY = "elementKey";
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
  
  private static final String TARGET_VALUE_STRING = "target value";
  private static final String TITLE_TEXT_COLOR = "Text Color";
  private static final String TITLE_BACKGROUND_COLOR = "Background Color";
  private static final String TITLE_ACTIVITY = "Edit Rule";
  
  private String mTableId;
  private String mElementKey;
  private int mRulePosition;
  private TableProperties mTp;
  private DbHelper dbh;
  private KeyValueStoreHelper mKvsh;
  private AspectHelper mAspectHelper;
  // The values to display
  private CharSequence[] mHumanValues;
  // The values to actually set.
  private CharSequence[] mEntryVales;
  private CharSequence[] mColumnDisplayNames;
  private CharSequence[] mColumnElementKeys;
  private ColorRuleGroup mTableColorRuleGroup;
  private List<ColorRule> mColorRules;
  private EditNameDialogPreference mValuePreference;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.mTableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
    this.mRulePosition = getIntent().getIntExtra(INTENT_KEY_RULE_POSITION,
        INTENT_FLAG_NEW_RULE);
    this.dbh = DbHelper.getDbHelper(this);
    this.mTp = TableProperties.getTablePropertiesForTable(dbh, mTableId, 
        KeyValueStore.Type.ACTIVE);
    this.mKvsh = 
        mTp.getKeyValueStoreHelper(ColorRuleGroup.KVS_PARTITION_TABLE);
    this.mAspectHelper = mKvsh.getAspectHelper(mElementKey);
    addPreferencesFromResource(
        org.opendatakit.tables.R.xml.preference_row_color_rule_entry);
    this.mHumanValues = ColorRule.RuleType.getValues();
    this.mEntryVales = ColorRule.RuleType.getValues();
    ColumnProperties[] cps = mTp.getColumns();
    this.mColumnDisplayNames = new CharSequence[cps.length];
    this.mColumnElementKeys = new CharSequence[cps.length];
    for (int i = 0; i < cps.length; i++) {
      mColumnDisplayNames[i] = cps[i].getDisplayName();
      mColumnElementKeys[i] = cps[i].getElementKey();
    }
    this.setTitle(TITLE_ACTIVITY);
  }
  
  @Override
  public void onResume() {
    super.onResume();
    init();
  }
  
  private void init() {
    this.mTableColorRuleGroup = ColorRuleGroup.getTableColorRuleGroup(mTp);
    this.mColorRules = mTableColorRuleGroup.getColorRules();
    
    final ListPreference columnPreference = 
        (ListPreference) findPreference(PREFERENCE_KEY_ELEMENT_KEY);
    columnPreference.setEntries(mColumnDisplayNames);
    columnPreference.setEntryValues(mColumnElementKeys);
    if (mRulePosition != INTENT_FLAG_NEW_RULE) {
      String displayName =
          mTp.getColumnByElementKey(mColorRules.get(mRulePosition)
              .getColumnElementKey()).getDisplayName();
      columnPreference.setSummary(displayName);
    }
    columnPreference.setOnPreferenceChangeListener(
        new OnPreferenceChangeListener() {
      
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChance callback invoked for value: " 
            + newValue);
        String elementKey = (String) newValue;
        if (mRulePosition == INTENT_FLAG_NEW_RULE) {
          ColorRule newRule = new ColorRule(elementKey, 
              ColorRule.RuleType.EQUAL,
              TARGET_VALUE_STRING, 
              Constants.DEFAULT_TEXT_COLOR,
              Constants.DEFAULT_BACKGROUND_COLOR);
          mColorRules.add(newRule);
          mRulePosition = mColorRules.size() - 1;
        } else {
          mColorRules.get(mRulePosition).setColumnElementKey(elementKey);
        }
        String displayName = 
            mTp.getColumnByElementKey(elementKey).getDisplayName();
        columnPreference.setSummary(displayName);
        mTableColorRuleGroup.replaceColorRuleList(mColorRules);
        mTableColorRuleGroup.saveRuleList();
        return true;
      }
    });
    
    final ListPreference operatorPreference = 
        (ListPreference) findPreference(PREFERENCE_KEY_COMP_TYPE);
    operatorPreference.setEntries(mHumanValues);
    operatorPreference.setEntryValues(mEntryVales);
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
        if (mRulePosition == INTENT_FLAG_NEW_RULE) {
          ColorRule newRule = new ColorRule(mElementKey,
              newOperator, TARGET_VALUE_STRING, Constants.DEFAULT_TEXT_COLOR, 
              Constants.DEFAULT_BACKGROUND_COLOR);
          mColorRules.add(newRule);
          mRulePosition = mColorRules.size() - 1;
        } else {
          mColorRules.get(mRulePosition).setOperator(newOperator);
        }
        operatorPreference.setSummary(newOperator.getSymbol());
        mTableColorRuleGroup.replaceColorRuleList(mColorRules);
        mTableColorRuleGroup.saveRuleList();
        return true;
      }
    });
    if (mRulePosition != INTENT_FLAG_NEW_RULE) {
      operatorPreference.setSummary(mColorRules.get(mRulePosition)
          .getOperator().getSymbol());
    }
    
    mValuePreference = 
        (EditNameDialogPreference) findPreference(PREFERENCE_KEY_VALUE);
    mValuePreference.setCallingActivity(this);
    if (mRulePosition == INTENT_FLAG_NEW_RULE) {
      mValuePreference.setSummary(TARGET_VALUE_STRING);
    } else {
      mValuePreference.setSummary(mColorRules.get(mRulePosition).getVal());
    }
    
    EditColorPreference textColorPref = 
        (EditColorPreference) findPreference(PREFERENCE_KEY_TEXT_COLOR);
    textColorPref.setCallingActivity(this);
    int textColor;
    if (mRulePosition == INTENT_FLAG_NEW_RULE) {
      textColor = Constants.DEFAULT_TEXT_COLOR;
    } else {
      textColor = mColorRules.get(mRulePosition).getForeground();
    }
    textColorPref.initColorPickerListener(this, COLOR_PREF_KEY_TEXT,
        TITLE_TEXT_COLOR, textColor);
    
    EditColorPreference backgroundColorPref = 
        (EditColorPreference) findPreference(PREFERENCE_KEY_BACKGROUND_COLOR);
    backgroundColorPref.setCallingActivity(this);
    int backgroundColor;
    if (mRulePosition == INTENT_FLAG_NEW_RULE) {
      backgroundColor = Constants.DEFAULT_BACKGROUND_COLOR;
    } else {
      backgroundColor = mColorRules.get(mRulePosition).getBackground();
    }
    backgroundColorPref.initColorPickerListener(this, COLOR_PREF_KEY_BACKGROUND,
        TITLE_BACKGROUND_COLOR, backgroundColor);
  }

  @Override
  public void tryToSaveNewName(String value) {
    if (mRulePosition == INTENT_FLAG_NEW_RULE) {
      ColorRule newRule = new ColorRule(mElementKey,
          ColorRule.RuleType.LESS_THAN, value, Constants.DEFAULT_TEXT_COLOR, 
          Constants.DEFAULT_BACKGROUND_COLOR);
      mColorRules.add(newRule);
      mRulePosition = mColorRules.size() - 1; // b/c it's now the last
    } else {
      mColorRules.get(mRulePosition).setVal(value);
    }
    mValuePreference.setSummary(value);
    mTableColorRuleGroup.replaceColorRuleList(mColorRules);
    mTableColorRuleGroup.saveRuleList();
  }

  /**
   * Kind of overloaded this method. Returns the value of the rule here.
   * @return
   */
  @Override
  public String getCurrentViewName() {
    if (mRulePosition == INTENT_FLAG_NEW_RULE) {
      return TARGET_VALUE_STRING;
    } else {
      return mColorRules.get(mRulePosition).getVal();
    }
  }

  @Override
  public void colorChanged(String key, int color) {
    ColorRule rule;
    if (mRulePosition == INTENT_FLAG_NEW_RULE) {
      rule = new ColorRule(mElementKey,
          ColorRule.RuleType.LESS_THAN, TARGET_VALUE_STRING, 
          Constants.DEFAULT_TEXT_COLOR, Constants.DEFAULT_BACKGROUND_COLOR);
    } else {
      rule = mColorRules.get(mRulePosition);
    }
    if (key.equals(COLOR_PREF_KEY_TEXT)) {
      rule.setForeground(color);
    } else if (key.equals(COLOR_PREF_KEY_BACKGROUND)) {
      rule.setBackground(color);
    } else {
      Log.e(TAG, "unrecognized key: " + key);
    }
    if (mRulePosition == INTENT_FLAG_NEW_RULE) {
      mColorRules.add(rule);
      mRulePosition = mColorRules.size() - 1; // b/c it's now the last     
    }
    mTableColorRuleGroup.replaceColorRuleList(mColorRules);
    mTableColorRuleGroup.saveRuleList();
  }

}
