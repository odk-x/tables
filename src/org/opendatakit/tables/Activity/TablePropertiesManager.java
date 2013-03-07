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

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.tables.Activity.util.LanguageUtil;
import org.opendatakit.tables.Activity.util.SecurityUtil;
import org.opendatakit.tables.Activity.util.ShortcutUtil;
import org.opendatakit.tables.activities.ListDisplayActivity;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.ColumnType;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.KeyValueStoreManager;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableType;
import org.opendatakit.tables.data.TableViewType;
import org.opendatakit.tables.view.custom.CustomDetailView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;

/**
 * An activity for managing a table's properties.
 *
 * @author hkworden@gmail.com
 * @author sudar.sam@gmail.com
 */
public class TablePropertiesManager extends PreferenceActivity {

  private static final String TAG = "TablePropertiesManager";

    public static final String INTENT_KEY_TABLE_ID = "tableId";

    // these ints are used when selecting/changing the view files
    private static final int RC_DETAIL_VIEW_FILE = 0;
    private static final int RC_LIST_VIEW_FILE = 1;

    private enum ViewPreferenceType {
        OVERVIEW_VIEW,
        COLLECTION_VIEW,
        // At the point where you could specify different files for an overview
        // and a collection list view, there were only the two constants
        // OVERVIEW_VIEW and COLLECTION_VIEW in this enum. I am changing this
        // to allow for only an unspecified view. The code seems to be checking
        // whether or not something is an overview or collection somehow and
        // storing the value in controller. I'm going to add a case for
        // AUTO_GENERATED to try and reflect that.
        AUTO_GENERATED
    }

    private DbHelper dbh;
    private TableProperties tp;

    private AlertDialog revertDialog;
    private AlertDialog saveAsDefaultDialog;
    private AlertDialog defaultToServerDialog;
    private AlertDialog serverToDefaultDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String tableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
        if (tableId == null) {
            throw new RuntimeException("Table ID (" + tableId +
                    ") is invalid.");
        }
        dbh = DbHelper.getDbHelper(this);
        tp = TableProperties.getTablePropertiesForTable(dbh, tableId,
            KeyValueStore.Type.ACTIVE);
        setTitle("Table Manager > " + tp.getDisplayName());
        init();
    }

    private void init() {

      // TODO move this into the actual preference somehow.
      AlertDialog.Builder builder = new AlertDialog.Builder(
          TablePropertiesManager.this);
      builder.setMessage(
          "Revert to default settings? Any modifications" +
          " you have made will be lost.");
      builder.setCancelable(true);
      builder.setPositiveButton("Yes",
          new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          SQLiteDatabase db = dbh.getWritableDatabase();
          KeyValueStoreManager kvsm =
              KeyValueStoreManager.getKVSManager(dbh);
          KeyValueStore defaultKVS =
              kvsm.getStoreForTable(tp.getTableId(),
                  KeyValueStore.Type.DEFAULT);
          if (!defaultKVS.entriesExist(db)) {
            AlertDialog.Builder noDefaultsDialog = new AlertDialog.Builder(
                TablePropertiesManager.this);
            noDefaultsDialog.setMessage("There are no default settings! " +
            		"No changes have been made.");
            noDefaultsDialog.setNeutralButton("OK", null);
            noDefaultsDialog.show();

            /*
            Toast.makeText(TablePropertiesManager.this,
                "No default settings!",
                Toast.LENGTH_SHORT).show();
                */
          } else {
            kvsm.revertToDefaultPropertiesForTable(tp.getTableId());
          }
        }
      });
      builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          dialog.cancel();
        }
      });
      revertDialog = builder.create();

      builder = new AlertDialog.Builder(
          TablePropertiesManager.this);
      builder.setMessage(
          "Save settings as default? Any modifications" +
          " old default settings will be lost, and these settings will be " +
          "pushed to the server at next synch.");
      builder.setCancelable(true);
      builder.setPositiveButton("Yes",
          new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          SQLiteDatabase db = dbh.getWritableDatabase();
          KeyValueStoreManager kvsm =
              KeyValueStoreManager.getKVSManager(dbh);
          KeyValueStore activeKVS =
              kvsm.getStoreForTable(tp.getTableId(),
                  KeyValueStore.Type.ACTIVE);

          kvsm.setCurrentAsDefaultPropertiesForTable(tp.getTableId());
        }
      });
      builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          dialog.cancel();
        }
      });
      saveAsDefaultDialog = builder.create();

      builder = new AlertDialog.Builder(
          TablePropertiesManager.this);
      builder.setMessage(
          "Copy default to server store?");
      builder.setCancelable(true);
      builder.setPositiveButton("Yes",
          new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          SQLiteDatabase db = dbh.getWritableDatabase();
          KeyValueStoreManager kvsm =
              KeyValueStoreManager.getKVSManager(dbh);
          KeyValueStore activeKVS =
              kvsm.getStoreForTable(tp.getTableId(),
                  KeyValueStore.Type.ACTIVE);

          kvsm.copyDefaultToServerForTable(tp.getTableId());
        }
      });
      builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          dialog.cancel();
        }
      });
      defaultToServerDialog = builder.create();

      builder = new AlertDialog.Builder(
          TablePropertiesManager.this);
      builder.setMessage(
          "Merge server settings to default settings?");
      builder.setCancelable(true);
      builder.setPositiveButton("Yes",
          new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          SQLiteDatabase db = dbh.getWritableDatabase();
          try {
	          KeyValueStoreManager kvsm =
	              KeyValueStoreManager.getKVSManager(dbh);
	          KeyValueStore activeKVS =
	              kvsm.getStoreForTable(tp.getTableId(),
	                  KeyValueStore.Type.ACTIVE);

	          kvsm.mergeServerToDefaultForTable(tp.getTableId());
          } finally {
            // TODO: fix the when to close problem
//        	  if ( db != null ) {
//        		  db.close();
//        	  }
          }
        }
      });
      builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          dialog.cancel();
        }
      });
      serverToDefaultDialog = builder.create();


        PreferenceScreen root =
            getPreferenceManager().createPreferenceScreen(this);

        // general category

        PreferenceCategory genCat = new PreferenceCategory(this);
        root.addPreference(genCat);
        genCat.setTitle("General");

        EditTextPreference dnPref = new EditTextPreference(this);
        dnPref.setTitle("Display Name");
        dnPref.setDialogTitle("Change Display Name");
        dnPref.setText(tp.getDisplayName());
        dnPref.setSummary(tp.getDisplayName());
        dnPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                    Object newValue) {
                tp.setDisplayName((String) newValue);
                setTitle("Table Manager > " + tp.getDisplayName());
                init();
                return false;
            }
        });
        genCat.addPreference(dnPref);

        boolean canBeAccessTable = SecurityUtil.couldBeSecurityTable(
                tp.getColumnOrder());
        boolean canBeShortcutTable = ShortcutUtil.couldBeShortcutTable(
                tp.getColumnOrder());
        int tableTypeCount = 1 + (canBeAccessTable ? 1 : 0) +
                (canBeShortcutTable ? 1 : 0);
        String[] tableTypeIds = new String[tableTypeCount];
        String[] tableTypeNames = new String[tableTypeCount];
        tableTypeIds[0] = String.valueOf(TableType.data);
        tableTypeNames[0] = LanguageUtil.getTableTypeLabel(
                TableType.data);
        if (canBeAccessTable) {
            tableTypeIds[1] = String.valueOf(
                    TableType.security);
            tableTypeNames[1] = LanguageUtil.getTableTypeLabel(
                    TableType.security);
        }
        if (canBeShortcutTable) {
            int index = canBeAccessTable ? 2 : 1;
            tableTypeIds[index] = String.valueOf(
                    TableType.shortcut);
            tableTypeNames[index] = LanguageUtil.getTableTypeLabel(
                    TableType.shortcut);
        }
        ListPreference tableTypePref = new ListPreference(this);
        tableTypePref.setTitle("Table Type");
        tableTypePref.setDialogTitle("Change Table Type");
        tableTypePref.setEntryValues(tableTypeIds);
        tableTypePref.setEntries(tableTypeNames);
        tableTypePref.setValue(String.valueOf(tp.getTableType()));
        tableTypePref.setSummary(LanguageUtil.getTableTypeLabel(
                tp.getTableType()));
        tableTypePref.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                    Object newValue) {
                tp.setTableType(TableType.valueOf((String) newValue));
                init();
                return false;
            }
        });
        genCat.addPreference(tableTypePref);

        // display category

        PreferenceCategory displayCat = new PreferenceCategory(this);
        root.addPreference(displayCat);
        displayCat.setTitle("Display");
// this was hilary's original code to always display options to set individual
// files for collection and overview views. we are not allowing that, so I am
// removing this.
//        addViewPreferences(ViewPreferenceType.OVERVIEW_VIEW, displayCat);
//        addViewPreferences(ViewPreferenceType.COLLECTION_VIEW, displayCat);
        addViewPreferences(ViewPreferenceType.AUTO_GENERATED, displayCat);

        DetailViewFileSelectorPreference detailViewPref =
                new DetailViewFileSelectorPreference(this);
        detailViewPref.setTitle("Detail View File");
        detailViewPref.setDialogTitle("Change Detail View File");
        final KeyValueStoreHelper kvsh =
            tp.getKeyValueStoreHelper(CustomDetailView.KVS_PARTITION);
        String detailViewFilename =
            kvsh.getString(CustomDetailView.KEY_FILENAME);
        detailViewPref.setText(detailViewFilename);
        detailViewPref.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference,
                            Object newValue) {
//                        tp.setDetailViewFilename((String) newValue);
                      kvsh.setString(
                          CustomDetailView.KEY_FILENAME,
                          (String) newValue);
                        init();
                        return false;
                    }
        });
        displayCat.addPreference(detailViewPref);

        // security category

        PreferenceCategory securityCat = new PreferenceCategory(this);
        root.addPreference(securityCat);
        securityCat.setTitle("Access Control");

        TableProperties[] accessTps =
            TableProperties.getTablePropertiesForSecurityTables(dbh,
                KeyValueStore.Type.ACTIVE);
        int accessTableCount =
                (tp.getTableType() == TableType.security) ?
                (accessTps.length) : accessTps.length + 1;
        TableProperties readTp = null;
        TableProperties writeTp = null;
        String[] accessTableIds = new String[accessTableCount];
        accessTableIds[0] = "-1";
        String[] accessTableNames = new String[accessTableCount];
        accessTableNames[0] = "None";
        int index = 1;
        for (TableProperties accessTp : accessTps) {
            if (accessTp.getTableId().equals(tp.getTableId())) {
                continue;
            }
            // TODO: fix this to handle access correctly. got altered during
            // schema update.
//            if ((tp.getReadSecurityTableId() != null) &&
//                    accessTp.getTableId().equals(
//                    tp.getReadSecurityTableId())) {
//                readTp = accessTp;
//            }
//            if ((tp.getWriteSecurityTableId() != null) &&
//                    accessTp.getTableId().equals(
//                    tp.getWriteSecurityTableId())) {
//                writeTp = accessTp;
//            }
            accessTableIds[index] = String.valueOf(accessTp.getTableId());
            accessTableNames[index] = accessTp.getDisplayName();
            index++;
        }

        ListPreference readTablePref = new ListPreference(this);
        readTablePref.setTitle("Read Access Table");
        readTablePref.setDialogTitle("Change Read Access Table");
        readTablePref.setEntryValues(accessTableIds);
        readTablePref.setEntries(accessTableNames);
        if (readTp == null) {
            readTablePref.setValue("-1");
            readTablePref.setSummary("None");
        } else {
            readTablePref.setValue(String.valueOf(readTp.getTableId()));
            readTablePref.setSummary(readTp.getDisplayName());
        }
        readTablePref.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                    Object newValue) {
              Log.d(TAG, "access stuff and .onPreferenceChange unimplented");
              // TODO: fix this, currently does nothing
                //tp.setReadSecurityTableId((String) newValue);
                init();
                return false;
            }
        });
        securityCat.addPreference(readTablePref);

        ListPreference writeTablePref = new ListPreference(this);
        writeTablePref.setTitle("Write Access Table");
        writeTablePref.setDialogTitle("Change Write Access Table");
        writeTablePref.setEntryValues(accessTableIds);
        writeTablePref.setEntries(accessTableNames);
        if (writeTp == null) {
            writeTablePref.setValue("-1");
            writeTablePref.setSummary("None");
        } else {
            writeTablePref.setValue(String.valueOf(writeTp.getTableId()));
            writeTablePref.setSummary(writeTp.getDisplayName());
        }
        writeTablePref.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                    Object newValue) {
              Log.d(TAG, ".onPreferenceChange unimplented");
              // TODO: fix this, currently does nothing
//                tp.setWriteSecurityTableId((String) newValue);
                init();
                return false;
            }
        });
        securityCat.addPreference(writeTablePref);

        // the managing of default properties

        // under development!
        PreferenceCategory defaultsCat = new PreferenceCategory(this);
        root.addPreference(defaultsCat);
        defaultsCat.setTitle("Manage Default Properties");

        // the actual entry that has the option above.
        Preference revertPref = new Preference(this);
        revertPref.setTitle("active<--default");
        revertPref.setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener() {

              @Override
              public boolean onPreferenceClick(Preference preference) {
                revertDialog.show();
                return true;
              }
            });
        defaultsCat.addPreference(revertPref);

        Preference saveAsDefaultPref = new Preference(this);
        saveAsDefaultPref.setTitle("active-->default");
        saveAsDefaultPref.setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener() {

              @Override
              public boolean onPreferenceClick(Preference preference) {
                saveAsDefaultDialog.show();
                return true;
              }
            });
        defaultsCat.addPreference(saveAsDefaultPref);

        Preference defaultToServerPref = new Preference(this);
        defaultToServerPref.setTitle("default-->server");
        defaultToServerPref.setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener() {

              @Override
              public boolean onPreferenceClick(Preference preference) {
                defaultToServerDialog.show();
                return true;
              }
            });
        defaultsCat.addPreference(defaultToServerPref);

        Preference serverToDefaultPref = new Preference(this);
        serverToDefaultPref.setTitle("default<--MERGE--server");
        serverToDefaultPref.setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener() {

              @Override
              public boolean onPreferenceClick(Preference preference) {
                serverToDefaultDialog.show();
                return true;
              }
            });
        defaultsCat.addPreference(serverToDefaultPref);

        setPreferenceScreen(root);
    }

    private void addViewPreferences(final ViewPreferenceType type,
            PreferenceCategory prefCat) {
//        final TableViewSettings settings;
        String label;
        switch (type) {
        case OVERVIEW_VIEW:
//            settings = tp.getOverviewViewSettings();
            label = "Overview View";
            break;
        case COLLECTION_VIEW:
//            settings = tp.getCollectionViewSettings();
            label = "Collection View";
            break;
        case AUTO_GENERATED:
            label = "View";
          break;
        default:
          Log.e(TAG, "the view type (collection vs overview vs auto) was not" +
          		"recognized.");
          label = "Unrecognized View, check log";
        }

        final List<ColumnProperties> numberCols =
            new ArrayList<ColumnProperties>();
        final List<ColumnProperties> locationCols =
            new ArrayList<ColumnProperties>();
        final List<ColumnProperties> dateCols =
            new ArrayList<ColumnProperties>();
        for (ColumnProperties cp : tp.getColumns()) {
            if (cp.getColumnType() == ColumnType.NUMBER ||
            	cp.getColumnType() == ColumnType.INTEGER) {
                numberCols.add(cp);
            } else if (cp.getColumnType() ==
                    ColumnType.GEOPOINT) {
                locationCols.add(cp);
            } else if (cp.getColumnType() ==
                ColumnType.DATE ||
                cp.getColumnType() ==
                ColumnType.DATETIME ||
                cp.getColumnType() ==
                ColumnType.TIME) {
                dateCols.add(cp);
            }
        }

//        int[] viewTypes = settings.getPossibleViewTypes();
        TableViewType[] viewTypes = tp.getPossibleViewTypes();
        String[] viewTypeIds = new String[viewTypes.length];
        String[] viewTypeNames = new String[viewTypes.length];
//        for (int i = 0; i < viewTypes.length; i++) {
//            int viewType = viewTypes[i];
//            viewTypeIds[i] = String.valueOf(viewType);
//            viewTypeNames[i] = LanguageUtil.getViewTypeLabel(viewType);
//        }
        // so now we need to populate the actual menu with the thing to save
        // and to the human-readable labels.
        for (int i = 0; i < viewTypes.length; i++ ) {
          viewTypeIds[i] = viewTypes[i].name();
          viewTypeNames[i] = viewTypes[i].name();
        }
        ListPreference viewTypePref = new ListPreference(this);
        viewTypePref.setTitle(label + " Type");
        viewTypePref.setDialogTitle("Change " + label + " Type");
        viewTypePref.setEntryValues(viewTypeIds);
        viewTypePref.setEntries(viewTypeNames);
//        viewTypePref.setValue(String.valueOf(settings.getViewType()));
        viewTypePref.setValue(tp.getCurrentViewType().name());
        // TODO: currently throwing an error i think
//        viewTypePref.setSummary(LanguageUtil.getViewTypeLabel(
//                settings.getViewType()));
        viewTypePref.setSummary(tp.getCurrentViewType().name());
        viewTypePref.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                    Object newValue) {
//                int viewType = Integer.valueOf((String) newValue);
//                settings.setViewType(viewType);
              tp.setCurrentViewType(TableViewType.valueOf((String)newValue));
                init();
                return false;
            }
        });
        prefCat.addPreference(viewTypePref);

        switch (tp.getCurrentViewType()) {
        case List:
            {
              Preference listViewPrefs = new Preference(this);
              listViewPrefs.setTitle("List View Manager");
              listViewPrefs.setOnPreferenceClickListener(
                  new OnPreferenceClickListener() {

                @Override
                public boolean onPreferenceClick(Preference preference) {
                  Intent selectListViewIntent = 
                      new Intent(TablePropertiesManager.this, 
                          ListViewManager.class);
                  selectListViewIntent.putExtra(
                      ListViewManager.INTENT_KEY_TABLE_ID, 
                      tp.getTableId());
                  startActivity(selectListViewIntent);
                  return true;
                }
                
              });
              prefCat.addPreference(listViewPrefs);
              }
              break;
        case Graph:
          Log.d(TAG, "Graph view type was selected");
          break;

        case Map:
// SS: i'm unsure about the implementation state of this, and get possible
// views is set up to never return map, so I'm just commenting this out but
// leaving a place for it.
//            {
//            ColumnProperties labelCol = settings.getMapLabelCol();
//            if (labelCol == null) {
//                settings.setMapLabelCol(tp.getColumns()[0]);
//                labelCol = tp.getColumns()[0];
//            }
//            ColumnProperties locCol = settings.getMapLocationCol();
//            if (locCol == null) {
//                settings.setMapLocationCol(locationCols.get(0));
//                locCol = locationCols.get(0);
//            }
//            ColumnProperties[] cps = tp.getColumns();
//            String[] colDbNames = new String[cps.length];
//            String[] colDisplayNames = new String[cps.length];
//            for (int i = 0; i < cps.length; i++) {
//                colDbNames[i] = cps[i].getColumnDbName();
//                colDisplayNames[i] = cps[i].getDisplayName();
//            }
//            String[] locColDbNames = new String[locationCols.size()];
//            String[] locColDisplayNames = new String[locationCols.size()];
//            for (int i = 0; i < locationCols.size(); i++) {
//                locColDbNames[i] = locationCols.get(i).getColumnDbName();
//                locColDisplayNames[i] = locationCols.get(i).getDisplayName();
//            }
//
//            ListPreference mapLocPref = new ListPreference(this);
//            mapLocPref.setTitle(label + " Location Column");
//            mapLocPref.setDialogTitle("Change " + label + " Location Column");
//            mapLocPref.setEntryValues(locColDbNames);
//            mapLocPref.setEntries(locColDisplayNames);
//            mapLocPref.setValue(locCol.getColumnDbName());
//            mapLocPref.setSummary(locCol.getDisplayName());
//            mapLocPref.setOnPreferenceChangeListener(
//                    new OnPreferenceChangeListener() {
//                @Override
//                public boolean onPreferenceChange(Preference preference,
//                        Object newValue) {
//                    settings.setMapLocationCol(tp.getColumnByDbName(
//                            (String) newValue));
//                    init();
//                    return false;
//                }
//            });
//            prefCat.addPreference(mapLocPref);
//
//            ListPreference mapLabelPref = new ListPreference(this);
//            mapLabelPref.setTitle(label + " Label Column");
//            mapLabelPref.setDialogTitle("Change " + label + " Label Column");
//            mapLabelPref.setEntryValues(colDbNames);
//            mapLabelPref.setEntries(colDisplayNames);
//            mapLabelPref.setValue(labelCol.getColumnDbName());
//            mapLabelPref.setSummary(labelCol.getDisplayName());
//            mapLabelPref.setOnPreferenceChangeListener(
//                    new OnPreferenceChangeListener() {
//                @Override
//                public boolean onPreferenceChange(Preference preference,
//                        Object newValue) {
//                    settings.setMapLabelCol(tp.getColumnByDbName(
//                            (String) newValue));
//                    init();
//                    return false;
//                }
//            });
//            prefCat.addPreference(mapLabelPref);
//
//            String[] mapColorLabels =
//                new String[TableViewSettings.MAP_COLOR_OPTIONS.length];
//            for (int i = 0; i < TableViewSettings.MAP_COLOR_OPTIONS.length;
//                    i++) {
//                mapColorLabels[i] = LanguageUtil.getMapColorLabel(
//                        TableViewSettings.MAP_COLOR_OPTIONS[i]);
//            }
//            Map<ColumnProperties, ConditionalRuler> colorRulers =
//                new HashMap<ColumnProperties, ConditionalRuler>();
//            for (ColumnProperties cp : tp.getColumns()) {
//                colorRulers.put(cp, settings.getMapColorRuler(cp));
//            }
//            ConditionalRulerDialogPreference ccPref =
//                new ConditionalRulerDialogPreference(
//                        TableViewSettings.MAP_COLOR_OPTIONS, mapColorLabels,
//                        colorRulers);
//            ccPref.setTitle(label + " Map Color Options");
//            prefCat.addPreference(ccPref);
//            }
            break;
        default:
          Log.e(TAG, "unrecognized view type: " + tp.getCurrentViewType() +
              ", resetting to spreadsheet");
          tp.setCurrentViewType(TableViewType.Spreadsheet);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        KeyValueStoreHelper kvsh;
        switch (requestCode) {
        case RC_DETAIL_VIEW_FILE:
            Uri fileUri = data.getData();
            String filename = fileUri.getPath();
            kvsh = tp.getKeyValueStoreHelper(CustomDetailView.KVS_PARTITION);
            kvsh.setString(
                CustomDetailView.KEY_FILENAME,
                filename);
//            tp.setDetailViewFilename(filename);
            init();
            break;
        case RC_LIST_VIEW_FILE:
        	Uri fileUri2 = data.getData();
            String filename2 = fileUri2.getPath();
// This set it in the main partition. We actually want to set it in the 
            // other partition for now.
//            kvsh =
//                tp.getKeyValueStoreHelper(ListDisplayActivity.KVS_PARTITION);
//            kvsh.setString(
//                ListDisplayActivity.KEY_FILENAME,
//                filename2);
            // Trying to get the new name to the _VIEWS partition.
            kvsh = tp.getKeyValueStoreHelper(
                ListDisplayActivity.KVS_PARTITION_VIEWS);
            // Set the name here statically, just to test. Later will want to
            // allow custom naming, checking for redundancy, etc.
            KeyValueHelper aspectHelper = kvsh.getAspectHelper("List View 1");
            aspectHelper.setString(ListDisplayActivity.KEY_FILENAME, 
                filename2);
            
//            TableViewSettings settings = tp.getOverviewViewSettings();
//            settings.setCustomListFilename(filename2);
            init();
            break;
        default:
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        finish();
    }

    private class DetailViewFileSelectorPreference extends EditTextPreference {

        DetailViewFileSelectorPreference(Context context) {
            super(context);
        }

        @Override
        protected void onClick() {
            if (hasFilePicker()) {
                Intent intent = new Intent("org.openintents.action.PICK_FILE");
                if (getText() != null) {
                    intent.setData(Uri.parse("file:///" + getText()));
                }
                startActivityForResult(intent, RC_DETAIL_VIEW_FILE);
            } else {
                super.onClick();
            }
        }

        private boolean hasFilePicker() {
            PackageManager packageManager = getPackageManager();
            Intent intent = new Intent("org.openintents.action.PICK_FILE");
            List<ResolveInfo> list = packageManager.queryIntentActivities(
                    intent, PackageManager.MATCH_DEFAULT_ONLY);
            return (list.size() > 0);
        }
    }

private class ListViewFileSelectorPreference extends EditTextPreference {

        ListViewFileSelectorPreference(Context context) {
            super(context);
        }

        @Override
        protected void onClick() {
            if (hasFilePicker()) {
                Intent intent = new Intent("org.openintents.action.PICK_FILE");
                if (getText() != null) {
                    intent.setData(Uri.parse("file:///" + getText()));
                }
                startActivityForResult(intent, RC_LIST_VIEW_FILE);
            } else {
                super.onClick();
            }
        }

        private boolean hasFilePicker() {
            PackageManager packageManager = getPackageManager();
            Intent intent = new Intent("org.openintents.action.PICK_FILE");
            List<ResolveInfo> list = packageManager.queryIntentActivities(
                    intent, PackageManager.MATCH_DEFAULT_ONLY);
            return (list.size() > 0);
        }
    }

//    private class ConditionalRulerDialogPreference extends Preference {
//
//        private final Dialog dialog;
//
//        public ConditionalRulerDialogPreference(int[] values, String[] labels,
//                Map<ColumnProperties, ConditionalRuler> rulerMap) {
//            super(TablePropertiesManager.this);
//            dialog = new ConditionalRulerDialog(values, labels, rulerMap);
//        }
//
//        @Override
//        protected void onClick() {
//            dialog.show();
//        }
//    }
// SS: this was commented out as TableViewSettings was removed. So whatever
    // this is used for needs to be redone to use TableProperties rather than
    // tvs.
//    public class ConditionalRulerDialog extends Dialog {
//
//        private final int[] values;
//        private final String[] labels;
//        private final Map<ColumnProperties, ConditionalRuler> rulerMap;
//        private final ColumnProperties[] columns;
//        private final String[] comparatorLabels;
//        private final String[] columnDisplays;
//        private LinearLayout ruleList;
//
//        public ConditionalRulerDialog(int[] values, String[] labels,
//                Map<ColumnProperties, ConditionalRuler> rulerMap) {
//            super(TablePropertiesManager.this);
//            this.values = values;
//            this.labels = labels;
//            this.rulerMap = rulerMap;
//            columns = new ColumnProperties[tp.getColumns().length];
//            comparatorLabels = new String[ConditionalRuler.Comparator.COUNT];
//            for (int i = 0; i < comparatorLabels.length; i++) {
//                comparatorLabels[i] =
//                    LanguageUtil.getTvsConditionalComparator(i);
//            }
//            columnDisplays = new String[tp.getColumns().length];
//            for (int i = 0; i < tp.getColumns().length; i++) {
//                ColumnProperties cp = tp.getColumns()[i];
//                columns[i] = cp;
//                columnDisplays[i] = cp.getDisplayName();
//            }
//            setContentView(buildView());
//        }
//
//        private View buildView() {
//            ruleList = new LinearLayout(TablePropertiesManager.this);
//            ruleList.setOrientation(LinearLayout.VERTICAL);
//            String[] comparatorValues =
//                new String[ConditionalRuler.Comparator.COUNT];
//            String[] comparatorDisplays =
//                new String[ConditionalRuler.Comparator.COUNT];
//            for (int i = 0; i < ConditionalRuler.Comparator.COUNT; i++) {
//                comparatorValues[i] = String.valueOf(i);
//                comparatorDisplays[i] =
//                    LanguageUtil.getTvsConditionalComparator(i);
//            }
//            LinearLayout.LayoutParams rwLp = new LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.FILL_PARENT,
//                    LinearLayout.LayoutParams.FILL_PARENT);
//            Context context = TablePropertiesManager.this;
//            for (ColumnProperties cp : tp.getColumns()) {
//                ConditionalRuler cr = rulerMap.get(cp);
//                for (int i = 0; i < cr.getRuleCount(); i++) {
//                    ruleList.addView(buildRuleView(cp, cr, i), rwLp);
//                }
//            }
//            LinearLayout controlWrapper = new LinearLayout(context);
//            Button addButton = new Button(context);
//            addButton.setText("Add");
//            addButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    ColumnProperties cp = columns[0];
//                    ConditionalRuler cr = rulerMap.get(cp);
//                    cr.addRule(ConditionalRuler.Comparator.EQUALS, "",
//                            values[0]);
//                    ruleList.addView(buildRuleView(cp, cr,
//                            cr.getRuleCount() - 1));
//                }
//            });
//            controlWrapper.addView(addButton);
//            Button closeButton = new Button(context);
//            closeButton.setText("Close");
//            closeButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    dismiss();
//                }
//            });
//            controlWrapper.addView(closeButton);
//            LinearLayout wrapper = new LinearLayout(context);
//            wrapper.setOrientation(LinearLayout.VERTICAL);
//            wrapper.addView(ruleList);
//            wrapper.addView(controlWrapper);
//            return wrapper;
//        }
//
//        private View buildRuleView(final ColumnProperties cp,
//                final ConditionalRuler cr, final int ruleIndex) {
//            Context context = TablePropertiesManager.this;
//
//            final Spinner colSpinner = getSpinner(context, columnDisplays);
//            int columnIndex = -1;
//            for (int i = 0; i < columns.length; i++) {
//                if (cp == columns[i]) {
//                    columnIndex = i;
//                    break;
//                }
//            }
//            if (columnIndex == -1) {
//                throw new RuntimeException();
//            }
//            colSpinner.setSelection(columnIndex);
//
//            final Spinner settingSpinner = getSpinner(context, labels);
//            int setting = cr.getRuleSetting(ruleIndex);
//            int settingIndex = -1;
//            for (int i = 0; i < values.length; i++) {
//                if (setting == values[i]) {
//                    settingIndex = i;
//                    break;
//                }
//            }
//            if (settingIndex == -1) {
//                throw new RuntimeException();
//            }
//            settingSpinner.setSelection(settingIndex);
//
//            final Spinner compSpinner = getSpinner(context, comparatorLabels);
//            compSpinner.setSelection(cr.getRuleComparator(ruleIndex));
//
//            final EditText valueEt = new EditText(context);
//            valueEt.setText(cr.getRuleValue(ruleIndex));
//
//            Button deleteButton = new Button(context);
//            deleteButton.setText("Delete");
//
//            colSpinner.setOnItemSelectedListener(
//                    new AdapterView.OnItemSelectedListener() {
//                @Override
//                public void onItemSelected(AdapterView<?> parent, View view,
//                        int position, long id) {
//                    ColumnProperties nextCp = columns[position];
//                    if (cp == nextCp) {
//                        return;
//                    }
//                    cr.deleteRule(ruleIndex);
//                    rulerMap.get(nextCp).addRule(
//                            compSpinner.getSelectedItemPosition(),
//                            valueEt.getText().toString(),
//                            values[settingSpinner.getSelectedItemPosition()]);
//                    setContentView(buildView());
//                }
//                @Override
//                public void onNothingSelected(AdapterView<?> parent) {}
//            });
//            settingSpinner.setOnItemSelectedListener(
//                    new AdapterView.OnItemSelectedListener() {
//                @Override
//                public void onItemSelected(AdapterView<?> parent, View view,
//                        int position, long id) {
//                    if (cr.getRuleSetting(ruleIndex) == values[position]) {
//                        return;
//                    }
//                    cr.setRuleSetting(ruleIndex, values[position]);
//                }
//                @Override
//                public void onNothingSelected(AdapterView<?> parent) {}
//            });
//            compSpinner.setOnItemSelectedListener(
//                    new AdapterView.OnItemSelectedListener() {
//                        @Override
//                public void onItemSelected(AdapterView<?> parent, View view,
//                        int position, long id) {
//                    if (cr.getRuleComparator(ruleIndex) == position) {
//                        return;
//                    }
//                    cr.setRuleComparator(ruleIndex, position);
//                }
//                @Override
//                public void onNothingSelected(AdapterView<?> parent) {}
//            });
//            valueEt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//                @Override
//                public void onFocusChange(View v, boolean hasFocus) {
//                    if (hasFocus) {
//                        return;
//                    }
//                    cr.setRuleValue(ruleIndex, valueEt.getText().toString());
//                }
//            });
//            deleteButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    cr.deleteRule(ruleIndex);
//                    setContentView(buildView());
//                }
//            });
//
//            LinearLayout topRow = new LinearLayout(context);
//            LinearLayout bottomRow = new LinearLayout(context);
//            topRow.addView(colSpinner);
//            topRow.addView(settingSpinner);
//            bottomRow.addView(compSpinner);
//            bottomRow.addView(valueEt);
//            bottomRow.addView(deleteButton);
//            LinearLayout rw = new LinearLayout(context);
//            rw.setOrientation(LinearLayout.VERTICAL);
//            rw.addView(topRow);
//            rw.addView(bottomRow);
//            return rw;
//        }
//
//        private Spinner getSpinner(Context context, String[] values) {
//            Spinner spinner = new Spinner(context);
//            ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
//                    android.R.layout.simple_spinner_item, values);
//            adapter.setDropDownViewResource(
//                    android.R.layout.simple_spinner_dropdown_item);
//            spinner.setAdapter(adapter);
//            return spinner;
//        }
//    }
}
