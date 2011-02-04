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

/*
 * Activity that allows users to change column property.
 * Column properties includes abreviations for the column
 * names, SMS-IN, SMS-OUT. Please see ColumnProperty.java 
 * for more information about the column property.
 * 
 *  @Author : YoonSung Hong (hys235@cs.washington.edu)
 */
public class PropertyManager extends PreferenceActivity {
        
		// Private Fields
		private ColumnProperty cp;
		private String tableID;
		private String colName;
		
		// Initialize private fields
		private void init() {
			this.cp = new ColumnProperty(tableID);
		}
		
		@Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
              
                // Column Name
                this.colName = getIntent().getStringExtra("colName");
                this.tableID = getIntent().getStringExtra("tableID");
                init();
                loadPreferenceScreen();
        }
        
		// View that allows users to chane column properties on a specific column.
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
            String[] typeChoices = {"None", "Text", "Numeric Value", "Date", "Phone Number", "Date Range"};
            category.addPreference(createListPreference("TYPE", "TYPE", type, type, typeChoices, typeChoices));                
            
            // SMS-IN<CheckBox>
            category.addPreference(createCheckBoxPreference("SMSIN", "SMS-IN", getSMSIn(colName)));
            
            // SMS-OUT<CheckBox>
            category.addPreference(createCheckBoxPreference("SMSOUT", "SMS-OUT", getSMSOut(colName)));
            
            // Footer Mode<Lis>
            String footerMode = getFooterMode(colName); 
            String[] footerModesChoices = {"None", "Average", "Count", "Max", "Min"};
            category.addPreference(createListPreference("FOOTER", "Footer Mode", footerMode, footerMode, footerModesChoices, footerModesChoices));
            
            // Set 
            setPreferenceScreen(root);
        }
        
        // Get the abreviation on this column.
        private String getAbrev(String colName) {
        	String result = cp.getAbrev(colName);
        	if (result == null) {
        		return "No Abreviation Defined.";
        	}
        	return result;
        }
        
        // Get the type for this column.
        private String getType(String colName) {
        	String result = cp.getType(colName);
        	if (result == null) {
        		return "No Type Defined.";
        	}
        	return result;
        }
        
        // Check if this is SMS-IN column.
        private boolean getSMSIn(String colName) {
        	return cp.getSMSIN(colName);
        }
        
        // Check if this is SMS-OUT column.
        private boolean getSMSOut(String colName) {
        	return cp.getSMSOUT(colName);
        }
        
        // Get the footer mode for this column.
        private String getFooterMode(String colName) {
        	String result = cp.getFooterMode(colName);
        	if (result == null) {
        		return "No Footer Mode Defined.";
        	}
        	return result;
        }
         
        // If any of fields change, direct the request to appropriate actions.
        public void onFieldChangeRouter(String key, String newVal) {
            // Get corresponding preference
        	Preference pref = findPreference(key);
        
        	// Routing
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
        
        // What value is in this edit box?
        private String getEditBoxContent(Preference pref) { 
        	EditTextPreference etp = (EditTextPreference) pref;
        	return etp.getEditText().getText().toString();
        }
        
        // What value is in this check box?
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
                  
        // Create a check box with the specified specifications.
        private CheckBoxPreference createCheckBoxPreference(String key, String title, boolean val) {
        	// Create a check box & configure
        	CheckBoxPreference cbp = new CheckBoxPreference(this);
            cbp.setKey(key);
            cbp.setTitle(title);
            cbp.setPersistent(false);
            cbp.setSummaryOn("True");
            cbp.setSummaryOff("False");
            cbp.setChecked(val);
            
            // Reaction if the check box gets changes
            cbp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					// Ask the router what to do
					onFieldChangeRouter(preference.getKey(), null);
					return false;
				}
			});
            
            return cbp;
        }
        
        // Create edit box with the specified specifications.
        private EditTextPreference createEditTextPreference(String key, String title, String dTitle, String sum, String val) {
        	// Create a edit box & configure
        	EditTextPreference etp = new EditTextPreference(this);
        	etp.setKey(key);
        	etp.setTitle(title);
        	etp.setPersistent(false);
        	etp.setDialogTitle(dTitle);
        	etp.setSummary(sum);
        	etp.setText(val);
        	
        	// Reaction if the edit box changes
        	etp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					// Ask the router what to do
					onFieldChangeRouter(preference.getKey(), null);
					return false;
				}
			});
        	
        	return etp;
        }
        
        private ListPreference createListPreference(String key, String title, String sum, String val, String[] ent, String[] entV) {
        	// Create a list preference & configure
        	ListPreference lp = new ListPreference(this);
        	lp.setEntries(ent);
        	lp.setEntryValues(entV);
        	lp.setKey(key);
        	lp.setTitle(title);
        	lp.setPersistent(false);
        	lp.setSummary(sum);
        	lp.setValue(val);
        	
        	// When an option chosen by the user
        	lp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					// Ask the router what to do
					onFieldChangeRouter(preference.getKey(), (String) newValue);
					return false;
				}
			});
        	
        	return lp;	
        }
}