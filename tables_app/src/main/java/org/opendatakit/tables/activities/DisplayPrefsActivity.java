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

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.widget.Toast;
import org.opendatakit.common.android.activities.BasePreferenceActivity;
import org.opendatakit.common.android.logic.PropertiesSingleton;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.TableUtil;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.tables.R;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.IntentUtil;

import java.io.File;

public class DisplayPrefsActivity extends BasePreferenceActivity {

  public static final String INTENT_KEY_TABLE_ID = "tableId";
  private static final int ABOUT_ACTIVITY_CODE = 1;
  
  private String appName;
  private String tableId;

  /** Alerts to confirm the output of the debug objects. */
  private AlertDialog mOutputDebugObjectsDialog;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    appName = IntentUtil.retrieveAppNameFromBundle(
        this.getIntent().getExtras());
    if (appName == null) {
      throw new IllegalStateException("App name not passed to activity.");
    }
    // check if this activity was called from Controller, in which case it
    // would have an extra string "tableId" bundled in
    tableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
    if (tableId == null) {
      throw new IllegalStateException("Table id not passed to activity");
    } else {
      // was called from controller so it is table specific
      try {
        customPreferences();
      } catch (RemoteException e) {
        WebLogger.getLogger(appName).printStackTrace(e);
        Toast.makeText(DisplayPrefsActivity.this, "Unable to access database", Toast.LENGTH_LONG).show();
      }
    }
  }
  
  @Override
  public String getAppName() {
    return appName;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    // don't care about outcome:
    // ABOUT_ACTIVITY_CODE
  }

  // set a custom font size for this table that overrides the general font size
  private void customPreferences() throws RemoteException {
    PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

    String localizedDisplayName;
    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(appName);
      localizedDisplayName = TableUtil.get().getLocalizedDisplayName(Tables.getInstance(), appName, db, tableId);
    } finally {
      if ( db != null ) {
        Tables.getInstance().getDatabase().closeDatabase(appName, db);
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