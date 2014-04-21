package org.opendatakit.tables.activities;

import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.Preferences;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.utils.TableFileUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.preferences.SliderPreference;
import org.opendatakit.tables.utils.OutputUtil;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public class DisplayPrefsActivity extends PreferenceActivity {
  public static final String INTENT_KEY_TABLE_ID = "tableId";
  private String appName;
  private Preferences prefs;
  private TableProperties tp;
  private KeyValueStoreHelper kvsh;

  /** Alerts to confirm the output of the debug objects. */
  private AlertDialog mOutputDebugObjectsDialog;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    appName = getIntent().getStringExtra(Controller.INTENT_KEY_APP_NAME);
    if ( appName == null ) {
      appName = TableFileUtils.getDefaultAppName();
    }
    prefs = new Preferences(this, appName);
    // check if this activity was called from Controller, in which case it
    // would have an extra string "tableId" bundled in
    String tableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
    if (tableId == null) {
      // was called from TableManager
      generalPreferences();
    } else {
      // was called from controller so it is table specific
      tp = TableProperties.getTablePropertiesForTable(this, appName, tableId);
      kvsh = tp.getKeyValueStoreHelper("SpreadsheetView");
      customPreferences();
    }
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
    SliderPreference fontSizePref = new SliderPreference(this, prefs.getFontSize());
    fontSizePref.setTitle(getString(R.string.font_size));
    fontSizePref.setDialogTitle(getString(R.string.change_font_size));
    fontSizePref.setMaxValue(48);
    fontSizePref.setValue(prefs.getFontSize());
    fontSizePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        prefs.setFontSize((Integer) newValue);
        return true;
      }
    });
    genCat.addPreference(fontSizePref);

    /*********************************
     * The homescreen preference.
     *********************************/
    CheckBoxPreference useHomescreenPref = new CheckBoxPreference(this);
    useHomescreenPref.setChecked(prefs.getUseHomeScreen());
    if (TableFileUtils.tablesHomeScreenFileExists(appName)) {
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

    setPreferenceScreen(root);
  }

  // set a custom font size for this table that overrides the general font size
  private void customPreferences() {
    PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

    PreferenceCategory genCat = new PreferenceCategory(this);
    root.addPreference(genCat);
    genCat.setTitle(getString(R.string.display_prefs_for, tp.getDbTableName()));

    final SliderPreference fontSizePref = new SliderPreference(this, prefs.getFontSize());
    fontSizePref.addDefaultOption(true);
    fontSizePref.setTitle(getString(R.string.font_size));
    fontSizePref.setDialogTitle(getString(R.string.change_font_size));
    fontSizePref.setMaxValue(48);

    // if a custom font size hasn't been set, set to general font size
    // and check "use default" checkbox
    if (kvsh.getInteger("fontSize") == null) {
      fontSizePref.setValue(prefs.getFontSize());
      fontSizePref.checkCheckBox(true);
      fontSizePref.setSliderEnabled(false);
    } else {
      fontSizePref.setValue(kvsh.getInteger("fontSize"));
      fontSizePref.checkCheckBox(false);
      fontSizePref.setSliderEnabled(true);
    }

    fontSizePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (fontSizePref.isChecked())
          kvsh.removeKey("fontSize");
        else
          kvsh.setInteger("fontSize", (Integer) newValue);
        return true;
      }
    });
    genCat.addPreference(fontSizePref);

    setPreferenceScreen(root);
  }
}