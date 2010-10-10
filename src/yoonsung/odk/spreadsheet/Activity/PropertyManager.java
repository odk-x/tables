package yoonsung.odk.spreadsheet.Activity;

import yoonsung.odk.spreadsheet.Database.ColumnProperty;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
 
public class PropertyManager extends PreferenceActivity {
        
		private ColumnProperty cp;
		private String colName;
		
		private void init() {
			this.cp = new ColumnProperty();
		}
		
		@Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
              
                // Column Name
                this.colName = getIntent().getStringExtra("colName");
                init();
                loadPreferenceScreen();
        }
        
        private void loadPreferenceScreen() {
        	// Preference Screen
            PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
            
            // Preference Category
            PreferenceCategory category = new PreferenceCategory(this);
            category.setTitle(colName);
            root.addPreference(category);
            
            // Abreviation<EditText>
            String abr = getAbrev(colName);
            category.addPreference(createEditTextPreference("ABR", "Abreviation", "Change Column Abreviation", abr, abr));
            
            // Type<List>
            String type = getType(colName);
            String[] typeChoices = {"Text", "Numeric Value", "Date", "Phone Number"};
            category.addPreference(createListPreference("TYPE", "TYPE", type, type, typeChoices, typeChoices));                
            
            // SMS-IN<CheckBox>
            category.addPreference(createCheckBoxPreference("SMSIN", "SMS-IN", getSMSIn(colName)));
            
            // SMS-OUT<CheckBox>
            category.addPreference(createCheckBoxPreference("SMSOUT", "SMS-OUT", getSMSOut(colName)));
            
            // Footer Mode<Lis>
            String footerMode = getFooterMode(colName); 
            String[] footerModesChoices = {"Average", "Count", "Mode", "Median"};
            category.addPreference(createListPreference("FOOTER", "Footer Mode", footerMode, footerMode, footerModesChoices, footerModesChoices));
            
            // Set 
            setPreferenceScreen(root);
        }
                
        private String getAbrev(String colName) {
        	String result = cp.getAbrev(colName);
        	if (result == null) {
        		return "No Abreviation Defined.";
        	}
        	return result;
        }
        
        private String getType(String colName) {
        	String result = cp.getType(colName);
        	if (result == null) {
        		return "No Type Defined.";
        	}
        	return result;
        }
        
        private boolean getSMSIn(String colName) {
        	return cp.getSMSIN(colName);
        }
        
        private boolean getSMSOut(String colName) {
        	return cp.getSMSOUT(colName);
        }
        
        private String getFooterMode(String colName) {
        	String result = cp.getFooterMode(colName);
        	if (result == null) {
        		return "No Footer Mode Defined.";
        	}
        	return result;
        }
               
        public void onFieldChangeRouter(String key, String newVal) {
            // Get corresponding preference
        	Preference pref = findPreference(key);
        
            if (key.equals("ABR")) {
            	cp.setAbrev(colName, getEditBoxContent(pref));
            } else if (key.equals("TYPE")) {
            	cp.setType(colName, newVal);
            } else if (key.equals("SMSIN")) {
            	cp.setSMSIN(colName, getCheckBoxContent(pref));
            } else if (key.equals("SMSOUT")) {
            	cp.setSMSOUT(colName, getCheckBoxContent(pref));
            } else if (key.equals("FOOTER")) {
            	cp.setFooterMode(colName, newVal);
            } 
            // Refresh
            getPreferenceScreen().removeAll();
            loadPreferenceScreen();
        }
        
        private String getEditBoxContent(Preference pref) { 
        	EditTextPreference etp = (EditTextPreference) pref;
        	return etp.getEditText().getText().toString();
        }
        
        private boolean getCheckBoxContent(Preference pref) {
        	CheckBoxPreference cbp = (CheckBoxPreference) pref;
        	// Reverse original when checked/unchecked.
        	if (cbp.isChecked()) {
        		//result = "0";
        		return false;
        	} else {
        		//result = "1";
        		return true;
        	}
        }
                     
        private CheckBoxPreference createCheckBoxPreference(String key, String title, boolean val) {
        	CheckBoxPreference cbp = new CheckBoxPreference(this);
            cbp.setKey(key);
            cbp.setTitle(title);
            cbp.setPersistent(false);
            cbp.setSummaryOn("True");
            cbp.setSummaryOff("False");
            //cbp.setDefaultValue(defaultVal);
            cbp.setChecked(val);
            cbp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					// TODO Auto-generated method stub
					onFieldChangeRouter(preference.getKey(), null);
					return false;
				}
			});
            return cbp;
        }
        
        private EditTextPreference createEditTextPreference(String key, String title, String dTitle, String sum, String val) {
        	EditTextPreference etp = new EditTextPreference(this);
        	etp.setKey(key);
        	etp.setTitle(title);
        	etp.setPersistent(false);
        	etp.setDialogTitle(dTitle);
        	etp.setSummary(sum);
        	etp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					// TODO Auto-generated method stub
					onFieldChangeRouter(preference.getKey(), null);
					return false;
				}
			});
        	etp.setText(val);
        	return etp;
        }
        
        private ListPreference createListPreference(String key, String title, String sum, String val, String[] ent, String[] entV) {
        	ListPreference lp = new ListPreference(this);
        	lp.setEntries(ent);
        	lp.setEntryValues(entV);
        	lp.setKey(key);
        	lp.setTitle(title);
        	lp.setPersistent(false);
        	lp.setSummary(sum);
        	lp.setValue(val);
        	lp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					// TODO Auto-generated method stub
					Log.e("NEWVAL", "" + newValue);
					onFieldChangeRouter(preference.getKey(), (String) newValue);
					return false;
				}
			});
        	return lp;	
        }
}