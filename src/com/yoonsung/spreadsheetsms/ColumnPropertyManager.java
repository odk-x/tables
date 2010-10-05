package com.yoonsung.spreadsheetsms;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;
 
public class ColumnPropertyManager extends PreferenceActivity {
        
		private String colName;
		
		@Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                
                // Column Name
                this.colName = getIntent().getStringExtra("colName");
                loadPreferenceScreen();
                //getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
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
        	String result = getProperty("abreviation", colName);
        	if (result == null) {
        		return "No Abreviation Defined.";
        	}
        	return result;
        }
        
        private String getType(String colName) {
        	String result = getProperty("type", colName);
        	if (result == null) {
        		return "No Type Defined.";
        	}
        	return result;
        }
        
        private boolean getSMSIn(String colName) {
        	String result = getProperty("SMSIN", colName);
        	if (result == null) {
        		return false;
        	} else if (result.equals("0")) {
        		return false;
        	} else {
        		return true;
        	}
        }
        
        private boolean getSMSOut(String colName) {
        	String result = getProperty("SMSOUT", colName);
        	if (result == null) {
        		return false;
        	} else if (result.equals("0")) {
        		return false;
        	} else {
        		return true;
        	}
        }
        
        private String getFooterMode(String colName) {
        	String result = getProperty("footerMode", colName);
        	if (result == null) {
        		return "No Footer Mode Defined.";
        	}
        	return result;
        }
        
        private String getProperty(String propType, String colName) {
        	DBIO db = new DBIO();
        	SQLiteDatabase con = db.getConn();
        	String[] spec = {colName};
        	Cursor cs = con.rawQuery("SELECT * FROM colProperty WHERE name = ?", spec);
        	if (cs != null) {
        		int colIndex = cs.getColumnIndex(propType);
        		if (cs.moveToFirst() && !cs.isNull(colIndex)) {
                	return cs.getString(colIndex);
        		}
        	}
        	con.close();
        	return null;
        }
               
        public void onFieldChangeRouter(String key, String newValue) {
            // Get corresponding preference
        	Preference pref = findPreference(key);
        
            if (key.equals("ABR")) {
            	onEditTextChange(pref, "abreviation");
            } else if (key.equals("TYPE")) {
            	onListChange(pref, "type", newValue);
            } else if (key.equals("SMSIN")) {
            	onCheckBoxChange(pref, "SMSIN");
            } else if (key.equals("SMSOUT")) {
            	onCheckBoxChange(pref, "SMSOUT");
            } else if (key.equals("FOOTER")) {
            	onListChange(pref, "footerMode", newValue);
            } 
            // Refresh
            getPreferenceScreen().removeAll();
            loadPreferenceScreen();
        }
        
        private void onEditTextChange(Preference pref, String field) {
        	EditTextPreference etp = (EditTextPreference) pref;
        	String newStr= etp.getEditText().getText().toString();
        	ContentValues values = new ContentValues();
        	values.put("name", colName);
        	values.put(field, newStr);
        	onFieldChange(field, values);
        }
        
        private void onListChange(Preference pref, String field, String newValue) {
        	ContentValues values = new ContentValues();
        	values.put("name", colName);
        	values.put(field, newValue);
        	onFieldChange(field, values);
        }
        
        private void onCheckBoxChange(Preference pref, String field) {
        	CheckBoxPreference cbp = (CheckBoxPreference) pref;
        	String newStr;
        	// Reverse original when checked/unchecked.
        	if (cbp.isChecked()) {
        		newStr = "0";
        	} else {
        		newStr = "1";
        	}
        	ContentValues values = new ContentValues();
        	values.put("name", colName);
        	values.put(field, newStr);
        	onFieldChange(field, values);
        }
        
        private void onFieldChange(String field, ContentValues values) {
        	DBIO db = new DBIO();
            SQLiteDatabase con = db.getConn();
        	// Decide on INSERT or UPDATE
            int count = 0;
            try {
            	String query = "SELECT * FROM `colProperty` WHERE `name` = '" + values.getAsString("name") + "'";
            	Log.e("QUERY", query);
            	Cursor cs = con.rawQuery(query, null);
            	count = cs.getCount();
            } catch (Exception e) {
            	; // Do None
            }
            
            if (count == 0) {
            	// INSERT
            	try {
            		con.insertOrThrow("colProperty", null, values);
            	} catch(Exception e) {
            		;//Do None.
            	}
            } else {
            	// UPDATE
            	try {
            		con.execSQL("UPDATE `colProperty` SET `" +  field +"` = '" + values.getAsString(field) + "' WHERE `name` = '" + colName +"'");
            	} catch (Exception e) {
            		; // Do None.
            	}
            }
            
        	con.close();
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