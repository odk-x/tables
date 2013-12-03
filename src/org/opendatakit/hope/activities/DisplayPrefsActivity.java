package org.opendatakit.hope.activities;

import org.opendatakit.hope.R;
import org.opendatakit.hope.data.DbHelper;
import org.opendatakit.hope.data.KeyValueStore;
import org.opendatakit.hope.data.KeyValueStoreHelper;
import org.opendatakit.hope.data.Preferences;
import org.opendatakit.hope.data.TableProperties;
import org.opendatakit.hope.preferences.SliderPreference;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;


public class DisplayPrefsActivity extends PreferenceActivity {
    public static final String INTENT_KEY_TABLE_ID = "tableId";
	private Preferences prefs;
    private DbHelper dbh;
    private TableProperties tp;
    private KeyValueStoreHelper kvsh;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prefs = new Preferences(this);
		// check if this activity was called from Controller, in which case it
		// would have an extra string "tableId" bundled in
        String tableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
        if (tableId == null) {
        	// was called from TableManager
        	generalPreferences();
        } else {
        	// was called from controller so it is table specific
        	dbh = DbHelper.getDbHelper(this);
        	tp = TableProperties.getTablePropertiesForTable(dbh, tableId,
        			KeyValueStore.Type.ACTIVE);
        	kvsh = tp.getKeyValueStoreHelper("SpreadsheetView");
        	customPreferences();
        }
	}

	// set default font size for all tables
	private void generalPreferences() {
		PreferenceScreen root =
				getPreferenceManager().createPreferenceScreen(this);

		PreferenceCategory genCat = new PreferenceCategory(this);
		root.addPreference(genCat);
		genCat.setTitle(getString(R.string.general_display_preferences));

		SliderPreference fontSizePref = new SliderPreference(this, prefs.getFontSize());
		fontSizePref.setTitle(getString(R.string.font_size));
		fontSizePref.setDialogTitle(getString(R.string.change_font_size));
		fontSizePref.setMaxValue(48);
		fontSizePref.setValue(prefs.getFontSize());
		fontSizePref.setOnPreferenceChangeListener(
				new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						prefs.setFontSize((Integer) newValue);
						return true;
					}
				});
		genCat.addPreference(fontSizePref);

		setPreferenceScreen(root);
	}

	// set a custom font size for this table that overrides the general font size
	private void customPreferences() {
		PreferenceScreen root =
				getPreferenceManager().createPreferenceScreen(this);

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
		}
		else {
			fontSizePref.setValue(kvsh.getInteger("fontSize"));
			fontSizePref.checkCheckBox(false);
			fontSizePref.setSliderEnabled(true);
		}

		fontSizePref.setOnPreferenceChangeListener(
				new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						if (fontSizePref.isChecked())
							kvsh.removeKey("fontSize");
						else
							kvsh.setInteger("fontSize",(Integer) newValue);
						return true;
					}
				});
		genCat.addPreference(fontSizePref);

		setPreferenceScreen(root);
	}
}