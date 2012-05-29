package org.opendatakit.tables.Activity;

import org.opendatakit.tables.Activity.util.SliderPreference;
import org.opendatakit.tables.data.Preferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;


public class DisplayPrefsActivity extends PreferenceActivity {
    
    private Preferences prefs;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Preferences(this);
        init();
    }
    
    private void init() {
        PreferenceScreen root =
            getPreferenceManager().createPreferenceScreen(this);
        
        PreferenceCategory genCat = new PreferenceCategory(this);
        root.addPreference(genCat);
        genCat.setTitle("General");
        
        SliderPreference fontSizePref = new SliderPreference(this);
        fontSizePref.setTitle("Font Size");
        fontSizePref.setDialogTitle("Change Font Size");
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
}
