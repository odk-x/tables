package yoonsung.odk.spreadsheet.Activity;

import java.util.List;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import yoonsung.odk.spreadsheet.Activity.util.LanguageUtil;
import yoonsung.odk.spreadsheet.Activity.util.SecurityUtil;
import yoonsung.odk.spreadsheet.Activity.util.ShortcutUtil;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.Preferences;
import yoonsung.odk.spreadsheet.data.TableProperties;

/**
 * An activity for managing a table's properties.
 * 
 * @author hkworden@gmail.com
 */
public class TablePropertiesManager extends PreferenceActivity {
    
    public static final String INTENT_KEY_TABLE_ID = "tableId";
    
    private static final int RC_DETAIL_VIEW_FILE = 0;
    
    private DbHelper dbh;
    private TableProperties tp;
    private Preferences prefs;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String tableId = getIntent().getStringExtra(INTENT_KEY_TABLE_ID);
        if (tableId == null) {
            throw new RuntimeException("Table ID (" + tableId +
                    ") is invalid.");
        }
        dbh = DbHelper.getDbHelper(this);
        tp = TableProperties.getTablePropertiesForTable(dbh, tableId);
        prefs = new Preferences(this);
        setTitle("ODK Tables > Table Manager > " + tp.getDisplayName());
        init();
    }
    
    private void init() {
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
                setTitle("ODK Tables > Table Manager > " +
                        tp.getDisplayName());
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
        tableTypeIds[0] = String.valueOf(TableProperties.TableType.DATA);
        tableTypeNames[0] = LanguageUtil.getTableTypeLabel(
                TableProperties.TableType.DATA);
        if (canBeAccessTable) {
            tableTypeIds[1] = String.valueOf(
                    TableProperties.TableType.SECURITY);
            tableTypeNames[1] = LanguageUtil.getTableTypeLabel(
                    TableProperties.TableType.SECURITY);
        }
        if (canBeShortcutTable) {
            int index = canBeAccessTable ? 2 : 1;
            tableTypeIds[index] = String.valueOf(
                    TableProperties.TableType.SHORTCUT);
            tableTypeNames[index] = LanguageUtil.getTableTypeLabel(
                    TableProperties.TableType.SHORTCUT);
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
                tp.setTableType(Integer.parseInt((String) newValue));
                init();
                return false;
            }
        });
        genCat.addPreference(tableTypePref);
        
        // display category
        
        PreferenceCategory displayCat = new PreferenceCategory(this);
        root.addPreference(displayCat);
        displayCat.setTitle("Display");
        
        FileSelectorPreference detailViewPref =
                new FileSelectorPreference(this);
        detailViewPref.setTitle("Detail View File");
        detailViewPref.setDialogTitle("Change Detail View File");
        detailViewPref.setText(tp.getDetailViewFilename());
        detailViewPref.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference,
                            Object newValue) {
                        tp.setDetailViewFilename((String) newValue);
                        init();
                        return false;
                    }
        });
        displayCat.addPreference(detailViewPref);
        
        EditTextPreference sumFormatPref = new EditTextPreference(this);
        sumFormatPref.setTitle("Summary Display Format");
        sumFormatPref.setDialogTitle("Change Summary Display Format");
        sumFormatPref.setText(tp.getSummaryDisplayFormat());
        sumFormatPref.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                    Object newValue) {
                tp.setSummaryDisplayFormat((String) newValue);
                init();
                return false;
            }
        });
        displayCat.addPreference(sumFormatPref);
        
        String[] viewTypeIds = new String[TableProperties.ViewType.COUNT];
        String[] viewTypeNames = new String[TableProperties.ViewType.COUNT];
        for (int i = 0; i < TableProperties.ViewType.COUNT; i++) {
            viewTypeIds[i] = String.valueOf(i);
            viewTypeNames[i] = LanguageUtil.getViewTypeLabel(i);
        }
        ListPreference viewTypePref = new ListPreference(this);
        viewTypePref.setTitle("Preferred View Type");
        viewTypePref.setDialogTitle("Change Preferred View Type");
        viewTypePref.setEntryValues(viewTypeIds);
        viewTypePref.setEntries(viewTypeNames);
        int preferredViewType = prefs.getPreferredViewType(tp.getTableId());
        viewTypePref.setValue(String.valueOf(preferredViewType));
        viewTypePref.setSummary(LanguageUtil.getViewTypeLabel(
                preferredViewType));
        viewTypePref.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                    Object newValue) {
                prefs.setPreferredViewType(tp.getTableId(),
                        Integer.parseInt((String) newValue));
                init();
                return false;
            }
        });
        displayCat.addPreference(viewTypePref);
        
        // security category
        
        PreferenceCategory securityCat = new PreferenceCategory(this);
        root.addPreference(securityCat);
        securityCat.setTitle("Access Control");
        
        TableProperties[] accessTps =
            TableProperties.getTablePropertiesForSecurityTables(dbh);
        int accessTableCount =
                (tp.getTableType() == TableProperties.TableType.SECURITY) ?
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
            if ((tp.getReadSecurityTableId() != null) &&
                    accessTp.getTableId().equals(
                    tp.getReadSecurityTableId())) {
                readTp = accessTp;
            }
            if ((tp.getWriteSecurityTableId() != null) &&
                    accessTp.getTableId().equals(
                    tp.getWriteSecurityTableId())) {
                writeTp = accessTp;
            }
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
                tp.setReadSecurityTableId((String) newValue);
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
                tp.setWriteSecurityTableId((String) newValue);
                init();
                return false;
            }
        });
        securityCat.addPreference(writeTablePref);
        
        setPreferenceScreen(root);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        switch (requestCode) {
        case RC_DETAIL_VIEW_FILE:
            Uri fileUri = data.getData();
            String filename = fileUri.getPath();
            tp.setDetailViewFilename(filename);
            init();
            break;
        default:
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    private class FileSelectorPreference extends EditTextPreference {
        
        FileSelectorPreference(Context context) {
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
}
