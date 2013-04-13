package org.opendatakit.tables.Activity;

import java.util.List;

import org.opendatakit.tables.DataStructure.ColColorRule;
import org.opendatakit.tables.DataStructure.ColumnColorRuler;
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

public class EditSavedColorRuleActivity extends PreferenceActivity 
    implements EditSavedViewEntryActivity, OnColorChangedListener {
  
  private static final String TAG =
      EditSavedListViewEntryActivity.class.getName();
  
  private static final String PREFERENCE_KEY_COMP_TYPE = "pref_comp_type";
  private static final String PREFERENCE_KEY_VALUE = "pref_value";
  private static final String PREFERENCE_KEY_TEXT_COLOR = "pref_text_color";
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
  private ColumnColorRuler mColorRuler;
  private List<ColColorRule> mColorRules;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.mTableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
    this.mElementKey = getIntent().getStringExtra(INTENT_KEY_ELEMENT_KEY);
    this.mRulePosition = getIntent().getIntExtra(INTENT_KEY_RULE_POSITION,
        INTENT_FLAG_NEW_RULE);
    this.dbh = DbHelper.getDbHelper(this);
    this.mTp = TableProperties.getTablePropertiesForTable(dbh, mTableId, 
        KeyValueStore.Type.ACTIVE);
    this.mKvsh = mTp.getKeyValueStoreHelper(ColumnColorRuler.KVS_PARTITION);
    this.mAspectHelper = mKvsh.getAspectHelper(mElementKey);
    addPreferencesFromResource(
        org.opendatakit.tables.R.xml.preference_color_rule_entry);
    this.mHumanValues = ColColorRule.RuleType.getValues();
    this.mEntryVales = ColColorRule.RuleType.getValues();
  }
  
  @Override
  public void onResume() {
    super.onResume();
    init();
  }
  
  private void init() {
    this.mColorRuler = ColumnColorRuler.getColumnColorRuler(mTp, mElementKey);
    this.mColorRules = mColorRuler.getColorRules();
    ListPreference operatorPreference = 
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
        ColColorRule.RuleType newOperator = 
            ColColorRule.RuleType.getEnumFromString((String) newValue);
        if (mRulePosition == INTENT_FLAG_NEW_RULE) {
          ColColorRule newRule = new ColColorRule(mElementKey,
              newOperator, TARGET_VALUE_STRING, Constants.DEFAULT_TEXT_COLOR, 
              Constants.DEFAULT_BACKGROUND_COLOR);
          mColorRules.add(newRule);
          mRulePosition = mColorRules.size() - 1;
        } else {
          mColorRules.get(mRulePosition).setOperator(newOperator);
        }
        mColorRuler.replaceColorRuleList(mColorRules);
        mColorRuler.saveRuleList();
        return true;
      }
    });
    if (mRulePosition != INTENT_FLAG_NEW_RULE) {
      operatorPreference.setSummary(mColorRules.get(mRulePosition)
          .getOperator().getSymbol());
    }
    
    EditNameDialogPreference valuePreference = 
        (EditNameDialogPreference) findPreference(PREFERENCE_KEY_VALUE);
    valuePreference.setCallingActivity(this);
    if (mRulePosition == INTENT_FLAG_NEW_RULE) {
      valuePreference.setSummary(TARGET_VALUE_STRING);
    } else {
      valuePreference.setSummary(mColorRules.get(mRulePosition).getVal());
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
      ColColorRule newRule = new ColColorRule(mElementKey,
          ColColorRule.RuleType.LESS_THAN, value, Constants.DEFAULT_TEXT_COLOR, 
          Constants.DEFAULT_BACKGROUND_COLOR);
      mColorRules.add(newRule);
      mRulePosition = mColorRules.size() - 1; // b/c it's now the last
    } else {
      mColorRules.get(mRulePosition).setVal(value);
    }
    mColorRuler.replaceColorRuleList(mColorRules);
    mColorRuler.saveRuleList();
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
    ColColorRule rule;
    if (mRulePosition == INTENT_FLAG_NEW_RULE) {
      rule = new ColColorRule(mElementKey,
          ColColorRule.RuleType.LESS_THAN, TARGET_VALUE_STRING, 
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
    mColorRuler.replaceColorRuleList(mColorRules);
    mColorRuler.saveRuleList();
  }
  

}
