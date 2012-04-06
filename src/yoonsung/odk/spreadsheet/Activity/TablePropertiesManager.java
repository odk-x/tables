package yoonsung.odk.spreadsheet.Activity;

import java.util.ArrayList;
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
import yoonsung.odk.spreadsheet.data.ColumnProperties;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.TableProperties;
import yoonsung.odk.spreadsheet.data.TableViewSettings;

/**
 * An activity for managing a table's properties.
 * 
 * @author hkworden@gmail.com
 */
public class TablePropertiesManager extends PreferenceActivity {
    
    public static final String INTENT_KEY_TABLE_ID = "tableId";
    
    private static final int RC_DETAIL_VIEW_FILE = 0;
    
    private enum ViewPreferenceType {
        OVERVIEW_VIEW,
        COLLECTION_VIEW
    }
    
    private DbHelper dbh;
    private TableProperties tp;
    
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
        
        addViewPreferences(ViewPreferenceType.OVERVIEW_VIEW, displayCat);
        addViewPreferences(ViewPreferenceType.COLLECTION_VIEW, displayCat);
        
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
    
    private void addViewPreferences(final ViewPreferenceType type,
            PreferenceCategory prefCat) {
        final TableViewSettings settings;
        String label;
        switch (type) {
        case OVERVIEW_VIEW:
            settings = tp.getOverviewViewSettings();
            label = "Overview View";
            break;
        case COLLECTION_VIEW:
            settings = tp.getCollectionViewSettings();
            label = "Collection View";
            break;
        default:
            throw new RuntimeException();
        }
        
        final List<ColumnProperties> numberCols = getNumberColumns();
        
        List<Integer> viewTypeIndices = new ArrayList<Integer>();
        for (int i = 0; i < TableViewSettings.Type.COUNT; i++) {
            if ((numberCols.size() == 0) && (
                    (i == TableViewSettings.Type.LINE_GRAPH) ||
                    (i == TableViewSettings.Type.BOX_STEM))) {
                continue;
            }
            viewTypeIndices.add(i);
        }
        String[] viewTypeIds = new String[viewTypeIndices.size()];
        String[] viewTypeNames = new String[viewTypeIndices.size()];
        for (int i = 0; i < viewTypeIndices.size(); i++) {
            int index = viewTypeIndices.get(i);
            viewTypeIds[i] = String.valueOf(index);
            viewTypeNames[i] = LanguageUtil.getViewTypeLabel(index);
        }
        ListPreference viewTypePref = new ListPreference(this);
        viewTypePref.setTitle(label + " Type");
        viewTypePref.setDialogTitle("Change " + label + " Type");
        viewTypePref.setEntryValues(viewTypeIds);
        viewTypePref.setEntries(viewTypeNames);
        viewTypePref.setValue(String.valueOf(settings.getViewType()));
        viewTypePref.setSummary(LanguageUtil.getViewTypeLabel(
                settings.getViewType()));
        viewTypePref.setOnPreferenceChangeListener(
                new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference,
                    Object newValue) {
                int viewType = Integer.valueOf((String) newValue);
                settings.setViewType(viewType);
                init();
                return false;
            }
        });
        prefCat.addPreference(viewTypePref);
        
        switch (settings.getViewType()) {
        
        case TableViewSettings.Type.LIST:
            {
            EditTextPreference formatPref = new EditTextPreference(this);
            formatPref.setTitle(label + " List Format");
            formatPref.setDialogTitle("Change " + label + " List Format");
            if (settings.getListFormat() != null) {
                formatPref.setDefaultValue(settings.getListFormat());
            }
            formatPref.setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                    settings.setListFormat((String) newValue);
                    init();
                    return false;
                }
            });
            prefCat.addPreference(formatPref);
            }
            break;
        
        case TableViewSettings.Type.LINE_GRAPH:
            {
            ColumnProperties xCol = settings.getLineXCol();
            if (xCol == null) {
                xCol = numberCols.get(0);
            }
            ColumnProperties yCol = settings.getLineYCol();
            if (yCol == null) {
                yCol = numberCols.get(0);
            }
            String[] numberColDbNames = new String[numberCols.size()];
            String[] numberColDisplayNames = new String[numberCols.size()];
            for (int i = 0; i < numberCols.size(); i++) {
                numberColDbNames[i] = numberCols.get(i).getColumnDbName();
                numberColDisplayNames[i] = numberCols.get(i).getDisplayName();
            }
            
            ListPreference lineXColPref = new ListPreference(this);
            lineXColPref.setTitle(label + " X Column");
            lineXColPref.setDialogTitle("Change " + label + " X Column");
            lineXColPref.setEntryValues(numberColDbNames);
            lineXColPref.setEntries(numberColDisplayNames);
            lineXColPref.setValue(xCol.getColumnDbName());
            lineXColPref.setSummary(xCol.getDisplayName());
            lineXColPref.setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                    settings.setLineXCol(tp.getColumnByDbName(
                            (String) newValue));
                    init();
                    return false;
                }
            });
            prefCat.addPreference(lineXColPref);
            
            ListPreference lineYColPref = new ListPreference(this);
            lineYColPref.setTitle(label + " Y Column");
            lineYColPref.setDialogTitle("Change " + label + " Y Column");
            lineYColPref.setEntryValues(numberColDbNames);
            lineYColPref.setEntries(numberColDisplayNames);
            lineYColPref.setValue(yCol.getColumnDbName());
            lineYColPref.setSummary(yCol.getDisplayName());
            lineYColPref.setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                    settings.setLineYCol(tp.getColumnByDbName(
                            (String) newValue));
                    init();
                    return false;
                }
            });
            prefCat.addPreference(lineYColPref);
            }
            break;
        
        case TableViewSettings.Type.BOX_STEM:
            {
            ColumnProperties xCol = settings.getBoxStemXCol();
            if (xCol == null) {
                xCol = tp.getColumns()[0];
            }
            ColumnProperties yCol = settings.getBoxStemYCol();
            if (yCol == null) {
                yCol = numberCols.get(0);
            }
            ColumnProperties[] cps = tp.getColumns();
            String[] colDbNames = new String[cps.length];
            String[] colDisplayNames = new String[cps.length];
            for (int i = 0; i < cps.length; i++) {
                colDbNames[i] = cps[i].getColumnDbName();
                colDisplayNames[i] = cps[i].getDisplayName();
            }
            String[] numberColDbNames = new String[numberCols.size()];
            String[] numberColDisplayNames = new String[numberCols.size()];
            for (int i = 0; i < numberCols.size(); i++) {
                numberColDbNames[i] = numberCols.get(i).getColumnDbName();
                numberColDisplayNames[i] = numberCols.get(i).getDisplayName();
            }
            
            ListPreference boxStemXColPref = new ListPreference(this);
            boxStemXColPref.setTitle(label + " X Column");
            boxStemXColPref.setDialogTitle("Change " + label + " X Column");
            boxStemXColPref.setEntryValues(colDbNames);
            boxStemXColPref.setEntries(colDisplayNames);
            boxStemXColPref.setValue(xCol.getColumnDbName());
            boxStemXColPref.setSummary(xCol.getDisplayName());
            boxStemXColPref.setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                    settings.setBoxStemXCol(tp.getColumnByDbName(
                            (String) newValue));
                    init();
                    return false;
                }
            });
            prefCat.addPreference(boxStemXColPref);
            
            ListPreference boxStemYColPref = new ListPreference(this);
            boxStemYColPref.setTitle(label + " Y Column");
            boxStemYColPref.setDialogTitle("Change " + label + " Y Column");
            boxStemYColPref.setEntryValues(numberColDbNames);
            boxStemYColPref.setEntries(numberColDisplayNames);
            boxStemYColPref.setValue(yCol.getColumnDbName());
            boxStemYColPref.setSummary(yCol.getDisplayName());
            boxStemYColPref.setOnPreferenceChangeListener(
                    new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference,
                        Object newValue) {
                    settings.setBoxStemYCol(tp.getColumnByDbName(
                            (String) newValue));
                    init();
                    return false;
                }
            });
            prefCat.addPreference(boxStemYColPref);
            }
            break;
        
        }
    }
    
    private List<ColumnProperties> getNumberColumns() {
        List<ColumnProperties> cols = new ArrayList<ColumnProperties>();
        for (ColumnProperties cp : tp.getColumns()) {
            if (cp.getColumnType() == ColumnProperties.ColumnType.NUMBER) {
                cols.add(cp);
            }
        }
        return cols;
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
