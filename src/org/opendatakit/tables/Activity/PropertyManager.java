/*
 * Copyright (C) 2012 University of Washington
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
package org.opendatakit.tables.Activity;

import org.opendatakit.tables.Activity.util.SliderPreference;
import org.opendatakit.tables.DataStructure.DisplayPrefs;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.ColumnType;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.FooterMode;
import org.opendatakit.tables.data.JoinColumn;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.view.SpreadsheetView;

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

  public static final String[] COLUMN_TYPE_LABELS = { "None", "Text", "Number", "Date",
      "Date Range", "Phone Number", "File", "Collect Form", "Multiple Choice", "Join", "Location" };

  public static final String[] FOOTER_MODE_LABELS = { "none", "count", 
    "minimum", "maximum",
      "mean", "sum" };

  // Private Fields
  private String tableId;
  private String colName;
  private TableProperties tp;
  private ColumnProperties cp;
  private int colIndex;
  private boolean showingMcDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Set title of activity
    setTitle("ODK Tables > Column Property");

    // Column Name
    this.tableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
    this.colName = getIntent().getStringExtra(INTENT_KEY_COLUMN_NAME);
    DbHelper dbh = DbHelper.getDbHelper(this);
    tp = TableProperties.getTablePropertiesForTable(dbh, tableId,
        KeyValueStore.Type.ACTIVE);
    cp = tp.getColumnByDbName(colName);
    colIndex = tp.getColumnIndex(colName);
    showingMcDialog = false;
    loadPreferenceScreen();
  }

  // View that allows users to chane column properties on a specific column.
  private void loadPreferenceScreen() {
    // Preference Screen
    PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

    // Preference Category
    PreferenceCategory category = new PreferenceCategory(this);
    category.setTitle(cp.getDisplayName());
    root.addPreference(category);

    // SMS Label<EditText>
    String smsLabel = getSmsLabel(colName);
    category.addPreference(createEditTextPreference("SMSLABEL", "SMS Label",
        "Change SMS Label for Column", smsLabel, smsLabel));

    // Type<List>
    String type = getColumnTypeLabel(colName);
    category.addPreference(createListPreference("TYPE", "Type", type, type, ColumnType.getAllColumnTypeLabels(),
    		ColumnType.getAllColumnTypeLabels()));

    // SMS-IN<CheckBox>
    category
        .addPreference(createCheckBoxPreference("SMSIN", "Get from Incoming", getSMSIn(colName)));

    // SMS-OUT<CheckBox>
    category
        .addPreference(createCheckBoxPreference("SMSOUT", "Put in Outgoing", getSMSOut(colName)));

    // Footer Mode<Lis>
    String footerMode = getFooterMode(colName);
    category.addPreference(createListPreference("FOOTER", "Footer Mode", footerMode, footerMode,
        FOOTER_MODE_LABELS, FOOTER_MODE_LABELS));

    SliderPreference colWidthPref = new SliderPreference(this);
    colWidthPref.setTitle("Column Width");
    colWidthPref.setDialogTitle("Change Column Width");
    colWidthPref.setMaxValue(500);
//    colWidthPref.setValue(tp.getOverviewViewSettings()
//        .getTableColWidths()[colIndex]);
    Integer savedColumnWidth = 
        tp.getIntegerEntry(ColumnProperties.KVS_PARTITION,
        tp.getColumnByIndex(colIndex).getElementKey(), 
        SpreadsheetView.KEY_COLUMN_WIDTH);
    if (savedColumnWidth == null) {
      savedColumnWidth = SpreadsheetView.DEFAULT_COL_WIDTH;
    }
    colWidthPref.setValue(savedColumnWidth);
    colWidthPref.setOnPreferenceChangeListener(
        new Preference.OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, 
          Object newValue) {
        int width = (Integer) newValue;
        tp.setIntegerEntry(ColumnProperties.KVS_PARTITION,
            tp.getColumnByIndex(colIndex).getElementKey(),
            SpreadsheetView.KEY_COLUMN_WIDTH, width);
//        int[] widths = tp.getOverviewViewSettings().getTableColWidths();
//        widths[colIndex] = width;
//        tp.getOverviewViewSettings().setTableColWidths(widths);
//        tp.getCollectionViewSettings().setTableColWidths(widths);
        return true;
      }
    });
    category.addPreference(colWidthPref);

    category.addPreference(new DisplayPreferencesDialogPreference(this));

    if (cp.getColumnType() == ColumnType.MC_OPTIONS) {
      showingMcDialog = true;
      category.addPreference(new McOptionSettingsDialogPreference(this));
    } else if (cp.getColumnType() == ColumnType.TABLE_JOIN) {
      // first handle what happens if it's null.
      JoinColumn joins = cp.getJoins();
      if (joins == null) {
        joins = new JoinColumn(JoinColumn.DEFAULT_NOT_SET_VALUE,
            JoinColumn.DEFAULT_NOT_SET_VALUE);
        cp.setJoins(joins);
      }
      String joinTableId = cp.getJoins().getTableId();
//      String joins = cp.getJoins();
      TableProperties[] tps = TableProperties.getTablePropertiesForAll(
          DbHelper.getDbHelper(this), KeyValueStore.Type.ACTIVE);
      TableProperties selectedTp = null;
      String[] tableIds = new String[tps.length];
      String selectedTableId = tableIds[0] = null;
      String[] tableNames = new String[tps.length];
      String selectedDisplayName = tableNames[0] = "Choose a Table";
      int index = 1;
      for (TableProperties tp : tps) {
        if (tp.getTableId().equals(tableId)) {
          continue;
        }
        tableIds[index] = tp.getTableId();
        tableNames[index] = tp.getDbTableName();
        if (tp.getTableId().equals(joinTableId)) {
//        if (tp.getTableId().equals(joins)) {
          selectedTp = tp;
          selectedTableId = tp.getTableId();
          selectedDisplayName = tp.getDisplayName();
        }
        index++;
      }
      category.addPreference(createListPreference("JOIN_TABLE", "Join Table", selectedDisplayName,
          selectedTableId, tableNames, tableIds));
      if (selectedTp != null) {
        // TODO: resolve how joins work
//        String joinColName = cp.getJoinColumnName();
        String joinColName = cp.getJoins().getElementKey();
        ColumnProperties[] cps = selectedTp.getColumns();
        String[] colDbNames = new String[cps.length + 1];
        String selectedDbName = colDbNames[0] = null;
        String[] colDisplayNames = new String[cps.length + 1];
        String selectedColDisplayName = colDisplayNames[0] = "Choose a Column";
        for (int i = 0; i < cps.length; i++) {
          String colDbName = cps[i].getColumnDbName();
          colDbNames[i + 1] = colDbName;
          colDisplayNames[i + 1] = cps[i].getDisplayName();
          if ((joinColName != null) && colDbName.equals(joinColName)) {
//          if ((joins != null) && colDbName.equals(joins)) {
            selectedDbName = colDbName;
            selectedColDisplayName = cps[i].getDisplayName();
          }
        }
        category.addPreference(createListPreference("JOIN_COLUMN", "Join Column",
            selectedColDisplayName, selectedDbName, colDisplayNames, colDbNames));
      }
    }

    // Set
    // getListView().setBackgroundColor(Color.TRANSPARENT);
    // getListView().setCacheColorHint(Color.TRANSPARENT);
    // getListView().setBackgroundColor(Color.rgb(255, 255, 255));
    setPreferenceScreen(root);

  }

  // Get the SMS abbreviation on this column.
  private String getSmsLabel(String colName) {
    String result = cp.getSmsLabel();
    if (result == null) {
      return "No Sms Abbreviation Defined.";
    }
    return result;
  }

  // Get the type for this column.
  private String getColumnTypeLabel(String colName) {
    return cp.getColumnType().label();
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
//    return FOOTER_MODE_LABELS[cp.getFooterMode()];
    return cp.getFooterMode().name();
  }

  // If any of fields change, direct the request to appropriate actions.
  public void onFieldChangeRouter(String key, String newVal) {
    // Get corresponding preference
    Preference pref = findPreference(key);

    // Routing
    if (key.equals("SMSLABEL")) {
      cp.setSmsLabel(getEditBoxContent(pref));
    } else if (key.equals("TYPE")) {
      for (ColumnType t : ColumnType.getAllColumnTypes()) {
        if (t.label().equals(newVal)) {
          cp.setColumnType(t);
          if ((t == ColumnType.MC_OPTIONS) && !showingMcDialog) {
            loadPreferenceScreen();
          } else if (t == ColumnType.TABLE_JOIN) {
            loadPreferenceScreen();
          }
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
          cp.setFooterMode(FooterMode.valueOf(FOOTER_MODE_LABELS[i]));
          break;
        }
      }
    } else if (key.equals("JOIN_TABLE")) {
      // TODO: resolve the ifs here for joins
//      cp.setJoinTableId(newVal);
//      cp.setJoins(newVal);
      JoinColumn oldJoins = cp.getJoins();
      oldJoins.setTableId(newVal);
      cp.setJoins(oldJoins);
    } else if (key.equals("JOIN_COLUMN")) {
//      cp.setJoinColumnName(newVal);
      JoinColumn oldJoins = cp.getJoins();
      oldJoins.setElementKey(newVal);
      cp.setJoins(oldJoins);
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
      // result = "0";
      return false;
    } else {
      // result = "1";
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
  private EditTextPreference createEditTextPreference(String key, String title, String dTitle,
      String sum, String val) {
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

  private ListPreference createListPreference(String key, String title, String sum, String val,
      String[] ent, String[] entV) {
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
      dialog = new DisplayPrefsDialog(PropertyManager.this, new DisplayPrefs(PropertyManager.this,
          tableId), colName);
      setTitle("Display Preferences");
    }

    @Override
    protected void onClick() {
      dialog.onCreate(null); // so we don't save state.
      dialog.show();
    }
  }

  private class McOptionSettingsDialogPreference extends Preference {

    private final MultipleChoiceSettingDialog dialog;

    public McOptionSettingsDialogPreference(Context context) {
      super(context);
      dialog = new MultipleChoiceSettingDialog(PropertyManager.this, cp);
      setTitle("Multiple Choice Settings");
    }

    @Override
    protected void onClick() {
      dialog.show();
    }
  }
}