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
package org.opendatakit.tables.activities;

import java.io.File;

import org.opendatakit.common.android.data.Preferences;
import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.TableUtil;
import org.opendatakit.tables.R;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.OutputUtil;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public class DisplayPrefsActivity extends PreferenceActivity {

  public static final String INTENT_KEY_TABLE_ID = "tableId";
  private static final int ABOUT_ACTIVITY_CODE = 1;
  
  private String appName;
  private String tableId;
  private Preferences prefs;

  /** Alerts to confirm the output of the debug objects. */
  private AlertDialog mOutputDebugObjectsDialog;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    appName = IntentUtil.retrieveAppNameFromBundle(
        this.getIntent().getExtras());
    if (appName == null) {
      throw new IllegalStateException("App name not passed to activitity.");
    }
    prefs = new Preferences(this, appName);
    // check if this activity was called from Controller, in which case it
    // would have an extra string "tableId" bundled in
    tableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
    if (tableId == null) {
      // was called from TableManager
      generalPreferences();
    } else {
      // was called from controller so it is table specific
      customPreferences();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    // don't care about outcome:
    // ABOUT_ACTIVITY_CODE
  }

  // set default font size for all tables
  private void generalPreferences() {
    PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

    PreferenceCategory genCat = new PreferenceCategory(this);
    root.addPreference(genCat);
    genCat.setTitle(getString(R.string.general_display_preferences));

    /*********************************
     * The app-wide fontsize preference.
     *********************************/
//    SliderPreference fontSizePref = new SliderPreference(this, prefs.getFontSize());
//    fontSizePref.setTitle(getString(R.string.font_size));
//    fontSizePref.setDialogTitle(getString(R.string.change_font_size));
//    fontSizePref.setMaxValue(48);
//    fontSizePref.setValue(prefs.getFontSize());
//    fontSizePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
//      @Override
//      public boolean onPreferenceChange(Preference preference, Object newValue) {
//        prefs.setFontSize((Integer) newValue);
//        return true;
//      }
//    });
//    genCat.addPreference(fontSizePref);

    /*********************************
     * The homescreen preference.
     *********************************/
    CheckBoxPreference useHomescreenPref = new CheckBoxPreference(this);
    useHomescreenPref.setChecked(prefs.getUseHomeScreen());
    File homeScreen = new File(ODKFileUtils.getTablesHomeScreenFile(appName));
    if (homeScreen.exists()) {
      useHomescreenPref.setTitle(R.string.use_index_html);
      useHomescreenPref.setEnabled(true);
    } else {
      useHomescreenPref.setTitle(R.string.no_index);
      useHomescreenPref.setEnabled(false);
    }
    useHomescreenPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        prefs.setUseHomeScreen((Boolean) newValue);
        return true;
      }
    });
    genCat.addPreference(useHomescreenPref);

    /*********************************
     * The write debug objects preference.
     *********************************/
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(getString(R.string.are_you_sure_write_debug_objects));
    builder.setCancelable(true);
    builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        // So now we have to write the data and control objects.
        OutputUtil.writeControlObject(
            DisplayPrefsActivity.this,DisplayPrefsActivity.this.appName);
        OutputUtil.writeAllDataObjects(
            DisplayPrefsActivity.this,DisplayPrefsActivity.this.appName);
      }
    });
    mOutputDebugObjectsDialog = builder.create();

    // The preference for debugging stuff.
    PreferenceCategory developerCategory = new PreferenceCategory(this);
    root.addPreference(developerCategory);
    developerCategory.setTitle(getString(R.string.developer));

    // the actual entry that has the option above.
    Preference writeDebugObjectsPref = new Preference(this);
    writeDebugObjectsPref.setTitle(getString(R.string.write_debug_objects));
    writeDebugObjectsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        mOutputDebugObjectsDialog.show();
        return true;
      }
    });
    developerCategory.addPreference(writeDebugObjectsPref);

    // And the about screen
    PreferenceCategory aboutCategory = new PreferenceCategory(this);
    root.addPreference(aboutCategory);
    aboutCategory.setTitle(R.string.about);
    
    Preference aboutPref = new Preference(this);
    aboutPref.setTitle(R.string.about);
    aboutPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      
      @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent i = new Intent(DisplayPrefsActivity.this, AboutWrapperActivity.class);
        i.putExtra(Constants.IntentKeys.APP_NAME, appName);        startActivityForResult(i, ABOUT_ACTIVITY_CODE);
        return true;
      }
    });
    aboutCategory.addPreference(aboutPref);
    
    setPreferenceScreen(root);
  }

  // set a custom font size for this table that overrides the general font size
  private void customPreferences() {
    PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

    String localizedDisplayName;
    SQLiteDatabase db = null;
    try {
      db = DatabaseFactory.get().getDatabase(this, appName);
      localizedDisplayName = TableUtil.get().getLocalizedDisplayName(db, tableId);
    } finally {
      if ( db != null ) {
        db.close();
      }
    }

    PreferenceCategory genCat = new PreferenceCategory(this);
    root.addPreference(genCat);
    genCat.setTitle(getString(R.string.display_prefs_for, localizedDisplayName));

//    final SliderPreference fontSizePref = new SliderPreference(this, prefs.getFontSize());
//    fontSizePref.addDefaultOption(true);
//    fontSizePref.setTitle(getString(R.string.font_size));
//    fontSizePref.setDialogTitle(getString(R.string.change_font_size));
//    fontSizePref.setMaxValue(48);

    // if a custom font size hasn't been set, set to general font size
    // and check "use default" checkbox
//    if (kvsh.getInteger("fontSize") == null) {
//      fontSizePref.setValue(prefs.getFontSize());
//      fontSizePref.checkCheckBox(true);
//      fontSizePref.setSliderEnabled(false);
//    } else {
//      fontSizePref.setValue(kvsh.getInteger("fontSize"));
//      fontSizePref.checkCheckBox(false);
//      fontSizePref.setSliderEnabled(true);
//    }

//    fontSizePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
//      @Override
//      public boolean onPreferenceChange(Preference preference, Object newValue) {
//        if (fontSizePref.isChecked())
//          kvsh.removeKey("fontSize");
//        else
//          kvsh.setInteger("fontSize", (Integer) newValue);
//        return true;
//      }
//    });
//    genCat.addPreference(fontSizePref);

    setPreferenceScreen(root);
  }
}