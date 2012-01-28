package yoonsung.odk.spreadsheet.Activity;

import yoonsung.odk.spreadsheet.DataStructure.DisplayPrefs;
import yoonsung.odk.spreadsheet.data.ColumnProperties;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.TableProperties;
import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

/*
 * Activity that allows users to change column property.
 * Column properties includes abreviations for the column
 * names, SMS-IN, SMS-OUT. Please see ColumnProperty.java 
 * for more information about the column property.
 * 
 *  @Author : YoonSung Hong (hys235@cs.washington.edu)
 */
public class PropertyManager extends PreferenceActivity {
    
    public static final String INTENT_KEY_TABLE_ID = "tableId";
    public static final String INTENT_KEY_COLUMN_NAME = "colName";
    
    public static final String[] COLUMN_TYPE_LABELS = {
        "None",
        "Text",
        "Number",
        "Date",
        "Date Range",
        "Phone Number",
        "File",
        "Collect Form"
    };
    
    public static final String[] FOOTER_MODE_LABELS = {
        "None",
        "Count",
        "Minimum",
        "Maximum",
        "Mean"
    };
    
    // Private Fields
        private long tableId;
        private String colName;
        private ColumnProperties cp;
        
        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
              
                // Set title of activity
                setTitle("ODK Tables > Column Property");
                
                // Column Name
                this.tableId = getIntent().getLongExtra(INTENT_KEY_TABLE_ID, -1);
                this.colName = getIntent().getStringExtra(INTENT_KEY_COLUMN_NAME);
                DbHelper dbh = new DbHelper(this);
                cp = TableProperties.getTablePropertiesForTable(dbh, tableId)
                        .getColumnByDbName(colName);
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
            category.addPreference(createEditTextPreference("ABR", "Abbreviation", "Change Column Abreviation", abr, abr));
            
            // Type<List>
            String type = getType(colName);
            category.addPreference(createListPreference("TYPE", "Type", type, type, COLUMN_TYPE_LABELS, COLUMN_TYPE_LABELS));
            
            // SMS-IN<CheckBox>
            category.addPreference(createCheckBoxPreference("SMSIN", "Get from Incoming", getSMSIn(colName)));
            
            // SMS-OUT<CheckBox>
            category.addPreference(createCheckBoxPreference("SMSOUT", "Put in Outgoing", getSMSOut(colName)));
            
            // Footer Mode<Lis>
            String footerMode = getFooterMode(colName); 
            category.addPreference(createListPreference("FOOTER", "Footer Mode", footerMode, footerMode, FOOTER_MODE_LABELS, FOOTER_MODE_LABELS));
            
            category.addPreference(new DisplayPreferencesDialogPreference(this));
            
            // Set 
            //getListView().setBackgroundColor(Color.TRANSPARENT);
            //getListView().setCacheColorHint(Color.TRANSPARENT);
            //getListView().setBackgroundColor(Color.rgb(255, 255, 255));
            setPreferenceScreen(root);
            
        }
        
        // Get the abreviation on this column.
        private String getAbrev(String colName) {
            String result = cp.getAbbreviation();
            if (result == null) {
                return "No Abreviation Defined.";
            }
            return result;
        }
        
        // Get the type for this column.
        private String getType(String colName) {
            return COLUMN_TYPE_LABELS[cp.getColumnType()];
        }
        
        // Check if this is SMS-IN column.
        private boolean getSMSIn(String colName) {
            return cp.getSmsIn();
        }
        
        // Check if this is SMS-OUT column.
        private boolean getSMSOut(String colName) {
            return cp.getSmsOut();
        }
        
        // Get the footer mode for this column.
        private String getFooterMode(String colName) {
            return FOOTER_MODE_LABELS[cp.getFooterMode()];
        }
         
        // If any of fields change, direct the request to appropriate actions.
        public void onFieldChangeRouter(String key, String newVal) {
            // Get corresponding preference
            Preference pref = findPreference(key);
        
            // Routing
            if (key.equals("ABR")) {
                cp.setAbbreviation(getEditBoxContent(pref));
            } else if (key.equals("TYPE")) {
                for (int i = 0; i < COLUMN_TYPE_LABELS.length; i++) {
                    if (COLUMN_TYPE_LABELS[i].equals(newVal)) {
                        cp.setColumnType(i);
                        break;
                    }
                }
            } else if (key.equals("SMSIN")) {
                cp.setSmsIn(getCheckBoxContent(pref));
            } else if (key.equals("SMSOUT")) {
                cp.setSmsOut(getCheckBoxContent(pref));
            } else if (key.equals("FOOTER")) {
                for (int i = 0; i < FOOTER_MODE_LABELS.length; i++) {
                    if (FOOTER_MODE_LABELS[i].equals(newVal)) {
                        cp.setFooterMode(i);
                        break;
                    }
                }
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
    
    private class DisplayPreferencesDialogPreference extends Preference {

        private final DisplayPrefsDialog dialog;
        
        public DisplayPreferencesDialogPreference(Context context) {
            super(context);
            dialog = new DisplayPrefsDialog(PropertyManager.this,
                    new DisplayPrefs(PropertyManager.this,
                    String.valueOf(tableId)), colName);
            setTitle("Display Preferences");
        }
        
        @Override
        protected void onClick() {
            dialog.show();
        }
    }
}