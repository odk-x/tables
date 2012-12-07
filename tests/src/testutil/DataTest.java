package testutil;

import org.opendatakit.tables.Activity.util.SecurityUtil;
import org.opendatakit.tables.Activity.util.ShortcutUtil;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.ColumnProperties.ColumnType;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.Table;
import org.opendatakit.tables.data.TableProperties;
import junit.framework.TestCase;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public abstract class DataTest extends TestCase {
    
    protected static final int DATA_TABLES_INDEX = 0;
    protected static final int SECURITY_TABLES_INDEX = 1;
    protected static final int SHORTCUT_TABLES_INDEX = 2;
    
    protected static final String[][] TABLE_IDS = {
        {"f43jg", "gi4j3gt4", "ogt4", "4ewjg", "utw2t"},
        {"ogjr4t", "o4i3hjtw"},
        {"p0ju43t"}
    };
    protected static final String[][] TABLE_DB_NAMES = {
        {"_tableA", "_tableB", "_tableC", "_fridges", "_districts"},
        {"_tableD", "_tableE"},
        {"_tableF"}
    };
    protected static final String[][] TABLE_DISPLAY_NAMES = {
        {"temps", "appointments", "applicants", "fridges", "districts"},
        {"TableD", "TableE"},
        {"TableF"}
    };
    protected static final String[][][] COLUMN_DB_NAMES = {
        {
            {"_id", "_temperature", "_time"},
            {"_person", "_time", "_topic"},
            {"_name", "_position", "_gpa"},
            {"_id", "_district"},
            {"_id", "_name", "_region"}
        },
        {
            {"_user", "_phonenum", "_password"},
            {"_user", "_phonenum", "_password"}
        },
        {
            {"_label", "_input", "_output"}
        }
    };
    protected static final String[][][] COLUMN_DISPLAY_NAMES = {
        {
            {"ID", "Temperature", "Time"},
            {"Person", "Time", "Topic"},
            {"Name", "Position", "GPA"},
            {"ID", "District"},
            {"ID", "Name", "Region"}
        },
        {
            {SecurityUtil.USER_COLUMN_NAME, SecurityUtil.PHONENUM_COLUMN_NAME,
                SecurityUtil.PASSWORD_COLUMN_NAME},
            {SecurityUtil.USER_COLUMN_NAME, SecurityUtil.PHONENUM_COLUMN_NAME,
                SecurityUtil.PASSWORD_COLUMN_NAME}
        },
        {
            {ShortcutUtil.LABEL_COLUMN_NAME, ShortcutUtil.INPUT_COLUMN_NAME,
                ShortcutUtil.OUTPUT_COLUMN_NAME}
        }
    };
    protected static final String[][][] COLUMN_ABBREVIATIONS = {
        {
            {"id", "temp", null},
            {"p", "time", "t"},
            {"n", "p", "g"},
            {"id", "d"},
            {"id", "n", "r"}
        },
        {
            {"uname", "num", "pass"},
            {"uname", "num", "pass"}
        },
        {
            {"l", "i", "o"}
        }
    };
    protected static final int[][][] COLUMN_TYPES = {
        {
            {ColumnType.NONE, ColumnType.NUMBER, ColumnType.DATE},
            {ColumnType.NONE, ColumnType.DATE_RANGE, ColumnType.NONE},
            {ColumnType.NONE, ColumnType.MC_OPTIONS, ColumnType.NUMBER},
            {ColumnType.NONE, ColumnType.NONE},
            {ColumnType.NONE, ColumnType.NONE, ColumnType.NONE}
        },
        {
            {ColumnType.NONE, ColumnType.PHONE_NUMBER, ColumnType.NONE},
            {ColumnType.NONE, ColumnType.PHONE_NUMBER, ColumnType.NONE}
        },
        {
            {ColumnType.NONE, ColumnType.NONE, ColumnType.NONE}
        }
    };
    protected static final String[][][][] DATA = {
        {
            {
                {"1", "20.4", "2010-10-11-01-00-23"},
                {"1", "12.7", "2010-10-11-01-15-23"},
                {"2", "14.7", "2010-10-11-01-15-14"},
                {"1", "14.8", "2010-10-11-01-30-23"},
                {"2", "14.8", "2010-10-11-01-30-14"},
                {"1", "14.9", "2010-10-11-01-45-23"},
                {"1", "14.4", "2010-10-12-02-15-23"},
                {"2", "1.7", "2010-10-12-02-15-14"},
                {"2", "1.4", "2010-10-13-04-45-14"},
                {"3", "13.2", "2010-10-12-04-15-47"}
            },
            {
                {"Ced", "2010-10-11-04-00-00/2010-10-11-05-00-00", "fruit"},
                {"Ced", "2010-10-11-05-20-00/2010-10-11-05-40-00", "trucks"},
                {"Ned", "2010-10-11-04-00-00/2010-10-11-05-00-00", "tubes"},
                {"Ced", "2010-10-11-02-00-00/2010-10-11-03-00-00", "cats"}
            },
            {
                {"Ced", "Engineer", "3.70"},
                {"Fred", "Engineer", "3.05"},
                {"Zed", "Secretary", "3.12"},
                {"Ned", "Manager", "3.22"}
            },
            {
                {"1", "Seattle"},
                {"2", "New York"},
                {"3", "Seattle"}
            },
            {
                {"1", "Seattle", "Northwest"},
                {"2", "Portland", "Northwest"},
                {"3", "New York", "East Coast"}
            }
        },
        {
            {
                {"fred", "5551234567", "fred's password"},
                {"ced", "5180987654", "cedpass"},
                {"ned", "2061357986", "iamned"}
            },
            {
                {"zed", "5552310807", "opensesame"},
                {"ted", "0295124029", "reallyted"}
            }
        },
        {
            {
                {"app", "%name% for %pos%", "@applicants +n %name% +p %pos%"},
                {"f", "%id% %temp%", "@fridges +f %id% +temp %temp%"}
            }
        }
    };
    protected static final String[] JOBAPP_MC_OPTIONS =
        {"Engineer", "Secretary", "Manager"};
    
    protected DataManager dm;
    protected TableProperties[] allTps;
    protected TableProperties[][] tps;
    protected DbTable[][] dbts;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        tps = new TableProperties[TABLE_IDS.length][];
        dbts = new DbTable[TABLE_IDS.length][];
        dm = mock(DataManager.class);
        for (int i = 0; i < tps.length; i++) {
            tps[i] = new TableProperties[TABLE_IDS[i].length];
            dbts[i] = new DbTable[TABLE_IDS[i].length];
            for (int j = 0; j < tps[i].length; j++) {
                tps[i][j] = buildTp(i, j);
                when(dm.getTableProperties(TABLE_IDS[i][j])).thenReturn(
                        tps[i][j]);
                dbts[i][j] = mock(DbTable.class);
                when(dm.getDbTable(TABLE_IDS[i][j])).thenReturn(dbts[i][j]);
            }
        }
        int allTpCount = 0;
        for (TableProperties[] tpArr : tps) {
            allTpCount += tpArr.length;
        }
        allTps = new TableProperties[allTpCount];
        int index = 0;
        for (TableProperties[] tpArr : tps) {
            for (TableProperties tp : tpArr) {
                allTps[index] = tp;
                index++;
            }
        }
        when(tps[0][2].getColumns()[1].getMultipleChoiceOptions())
                .thenReturn(JOBAPP_MC_OPTIONS);
        when(dm.getAllTableProperties()).thenReturn(allTps);
        when(dm.getDataTableProperties()).thenReturn(tps[DATA_TABLES_INDEX]);
        when(dm.getSecurityTableProperties()).thenReturn(
                tps[SECURITY_TABLES_INDEX]);
        when(dm.getShortcutTableProperties()).thenReturn(
                tps[SHORTCUT_TABLES_INDEX]);
    }
    
    private TableProperties buildTp(int type, int index) {
        TableProperties tp = mock(TableProperties.class);
        when(tp.getTableId()).thenReturn(TABLE_IDS[type][index]);
        when(tp.getDbTableName()).thenReturn(TABLE_DB_NAMES[type][index]);
        when(tp.getDisplayName()).thenReturn(TABLE_DISPLAY_NAMES[type][index]);
        String[] colOrder = new String[COLUMN_DB_NAMES[type][index].length];
        ColumnProperties[] cps =
            new ColumnProperties[COLUMN_DB_NAMES[type][index].length];
        for (int i = 0; i < cps.length; i++) {
            String colDbName = COLUMN_DB_NAMES[type][index][i];
            String displayName = COLUMN_DISPLAY_NAMES[type][index][i];
            String abbrev = COLUMN_ABBREVIATIONS[type][index][i];
            int colType = COLUMN_TYPES[type][index][i];
            ColumnProperties cp = mock(ColumnProperties.class);
            when(cp.getColumnDbName()).thenReturn(colDbName);
            when(cp.getDisplayName()).thenReturn(displayName);
            when(cp.getSmsLabel()).thenReturn(abbrev);
            when(cp.getColumnType()).thenReturn(colType);
            when(cp.getSmsIn()).thenReturn(true);
            when(cp.getSmsOut()).thenReturn(true);
            cps[i] = cp;
            colOrder[i] = colDbName;
            when(tp.getColumnByAbbreviation(abbrev)).thenReturn(colDbName);
            when(tp.getColumnByDbName(colDbName)).thenReturn(cp);
            when(tp.getColumnByDisplayName(displayName)).thenReturn(colDbName);
            when(tp.getColumnByUserLabel(displayName)).thenReturn(cp);
            when(tp.getColumnByUserLabel(abbrev)).thenReturn(cp);
            when(tp.getColumnIndex(colDbName)).thenReturn(i);
        }
        when(tp.getColumns()).thenReturn(cps);
        when(tp.getColumnOrder()).thenReturn(colOrder);
        return tp;
    }
    
    protected Table buildTable(int type, int index) {
        String[][] data = DATA[type][index];
        Table allTable = mock(Table.class);
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                when(allTable.getData(i, j)).thenReturn(data[i][j]);
            }
        }
        when(allTable.getHeight()).thenReturn(data.length);
        return allTable;
    }
}
