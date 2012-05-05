package messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import testutil.DataTest;
import org.opendatakit.tables.Activity.util.SecurityUtil;
import org.opendatakit.tables.SMS.MsgHandler;
import org.opendatakit.tables.SMS.SMSSender;
import org.opendatakit.tables.data.DataUtil;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.Table;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class MsgHandlerTests extends DataTest {
    
    private static final String SRC_PHONE_NUM = "0123456789";
    
    private DataUtil du;
    private SMSSender smsSender;
    private MsgHandler mh;
    private Table scTable;
    private ArgumentCaptor<Map<String, String>> addValuesCaptor;
    private ArgumentCaptor<String> lastModTimeCaptor;
    private ArgumentCaptor<String> srcPhoneCaptor;
    private ArgumentCaptor<String> phoneNumCaptor;
    private ArgumentCaptor<String> respCaptor;
    
    // for the ArgumentCaptor casting
    @SuppressWarnings("unchecked")
    @Override
    public void setUp() throws Exception {
        super.setUp();
        du = DataUtil.getDefaultDataUtil();
        smsSender = mock(SMSSender.class);
        mh = new MsgHandler(dm, smsSender);
        // setting up the shortcut table
        scTable = buildTable(2, 0);
        Query scQuery = new Query(allTps, tps[SHORTCUT_TABLES_INDEX][0]);
        String[] scCols = COLUMN_DB_NAMES[SHORTCUT_TABLES_INDEX][0];
        when(dbts[SHORTCUT_TABLES_INDEX][0].getRaw(scQuery, scCols))
                .thenReturn(scTable);
        // initializing argument captors
        addValuesCaptor = (ArgumentCaptor<Map<String, String>>) (Object)
                ArgumentCaptor.forClass(Map.class);
        lastModTimeCaptor = ArgumentCaptor.forClass(String.class);
        srcPhoneCaptor = ArgumentCaptor.forClass(String.class);
        phoneNumCaptor = ArgumentCaptor.forClass(String.class);
        respCaptor = ArgumentCaptor.forClass(String.class);
    }
    
    public void testSimpleAdd() {
        String msg = "@applicants +Name Ted +Position Engineer +GPA 3.76";
        assertTrue(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(dbts[0][2]).addRow(addValuesCaptor.capture(),
                lastModTimeCaptor.capture(), srcPhoneCaptor.capture());
        Map<String, String> addedValues = addValuesCaptor.getValue();
        assertEquals(3, addedValues.size());
        assertEquals("Ted", addedValues.get("_name"));
        assertEquals("Engineer", addedValues.get("_position"));
        assertEquals("3.76", addedValues.get("_gpa"));
    }
    
    public void testAddWithMissingColumns() {
        String msg = "@temps +ID 2 +Temperature 12.4";
        assertTrue(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(dbts[0][0]).addRow(addValuesCaptor.capture(),
                lastModTimeCaptor.capture(), srcPhoneCaptor.capture());
        Map<String, String> addedValues = addValuesCaptor.getValue();
        assertEquals(2, addedValues.size());
        assertEquals("2", addedValues.get("_id"));
        assertEquals("12.4", addedValues.get("_temperature"));
    }
    
    public void testAddWithColumnAbbreviations() {
        String msg = "@applicants +n Ted +p Engineer +g 3.76";
        assertTrue(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(dbts[0][2]).addRow(addValuesCaptor.capture(),
                lastModTimeCaptor.capture(), srcPhoneCaptor.capture());
        Map<String, String> addedValues = addValuesCaptor.getValue();
        assertEquals(3, addedValues.size());
        assertEquals("Ted", addedValues.get("_name"));
        assertEquals("Engineer", addedValues.get("_position"));
        assertEquals("3.76", addedValues.get("_gpa"));
    }
    
    public void testAddWithDisplayNamesAndAbbreviations() {
        String msg = "@applicants +n Ted +Position Engineer +GPA 3.76";
        assertTrue(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(dbts[0][2]).addRow(addValuesCaptor.capture(),
                lastModTimeCaptor.capture(), srcPhoneCaptor.capture());
        Map<String, String> addedValues = addValuesCaptor.getValue();
        assertEquals(3, addedValues.size());
        assertEquals("Ted", addedValues.get("_name"));
        assertEquals("Engineer", addedValues.get("_position"));
        assertEquals("3.76", addedValues.get("_gpa"));
    }
    
    public void testAddInvalidNumericValue() {
        String msg = "@temps +ID 2 +Temperature banana";
        assertFalse(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(dbts[0][0], never()).addRow(addValuesCaptor.capture(),
                lastModTimeCaptor.capture(), srcPhoneCaptor.capture());
    }
    
    public void testAddInvalidMultipleChoiceValue() {
        String msg = "@applicants +Name Ted +Position Janitor +GPA 3.76";
        assertFalse(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(dbts[0][2], never()).addRow(addValuesCaptor.capture(),
                lastModTimeCaptor.capture(), srcPhoneCaptor.capture());
    }
    
    public void testAddNonexistentColumn() {
        String msg = "@temps +orange 2 +Temperature 12.4";
        assertFalse(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(dbts[0][0], never()).addRow(addValuesCaptor.capture(),
                lastModTimeCaptor.capture(), srcPhoneCaptor.capture());
    }
    
    public void testSimpleQuery() {
        Query query = new Query(allTps, tps[0][2]);
        query.addConstraint(tps[0][2].getColumnByDbName("_position"),
                Query.Comparator.EQUALS, "Engineer");
        Table table = new Table(new String[] {"wpoijgprw", "ogijewrgreo"},
                new String[] {"_name"}, new String[][] {{"Ced"}, {"Fred"}});
        when(dbts[0][2].getRaw(query, new String[] {"_name"}))
                .thenReturn(table);
        String msg = "@applicants ?Name =Position Engineer";
        assertTrue(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(smsSender).sendSMSWithCutoff(
                phoneNumCaptor.capture(), respCaptor.capture());
        assertEquals(SRC_PHONE_NUM, phoneNumCaptor.getValue());
        assertEquals("n:Ced;n:Fred", respCaptor.getValue());
    }
    
    public void testLessThanQuery() {
        Query query = new Query(allTps, tps[0][2]);
        query.addConstraint(tps[0][2].getColumnByDbName("_gpa"),
                Query.Comparator.LESS_THAN, "3.2");
        Table table = new Table(new String[] {"wpoijgprw", "ogijewrgreo"},
                new String[] {"_name"}, new String[][] {{"Fred"}, {"Zed"}});
        when(dbts[0][2].getRaw(query, new String[] {"_name"}))
                .thenReturn(table);
        String msg = "@applicants ?Name <GPA 3.2";
        assertTrue(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(smsSender).sendSMSWithCutoff(
                phoneNumCaptor.capture(), respCaptor.capture());
        assertEquals(SRC_PHONE_NUM, phoneNumCaptor.getValue());
        assertEquals("n:Fred;n:Zed", respCaptor.getValue());
    }
    
    public void testGreaterThanQuery() {
        Query query = new Query(allTps, tps[0][2]);
        query.addConstraint(tps[0][2].getColumnByDbName("_gpa"),
                Query.Comparator.GREATER_THAN, "3.2");
        Table table = new Table(new String[] {"wpoijgprw", "ogijewrgreo"},
                new String[] {"_name"}, new String[][] {{"Ced"}, {"Ned"}});
        when(dbts[0][2].getRaw(query, new String[] {"_name"}))
                .thenReturn(table);
        String msg = "@applicants ?Name >GPA 3.2";
        assertTrue(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(smsSender).sendSMSWithCutoff(
                phoneNumCaptor.capture(), respCaptor.capture());
        assertEquals(SRC_PHONE_NUM, phoneNumCaptor.getValue());
        assertEquals("n:Ced;n:Ned", respCaptor.getValue());
    }
    
    public void testNotEqualsQuery() {
        Query query = new Query(allTps, tps[0][2]);
        query.addConstraint(tps[0][2].getColumnByDbName("_position"),
                Query.Comparator.NOT_EQUALS, "Engineer");
        Table table = new Table(new String[] {"wpoijgprw", "ogijewrgreo"},
                new String[] {"_name"}, new String[][] {{"Zed"}, {"Ned"}});
        when(dbts[0][2].getRaw(query, new String[] {"_name"}))
                .thenReturn(table);
        String msg = "@applicants ?Name !Position Engineer";
        assertTrue(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(smsSender).sendSMSWithCutoff(
                phoneNumCaptor.capture(), respCaptor.capture());
        assertEquals(SRC_PHONE_NUM, phoneNumCaptor.getValue());
        assertEquals("n:Zed;n:Ned", respCaptor.getValue());
    }
    
    public void testQueryWithSortColumn() {
        Query query = new Query(allTps, tps[0][2]);
        query.addConstraint(tps[0][2].getColumnByDbName("_position"),
                Query.Comparator.NOT_EQUALS, "Engineer");
        query.setOrderBy(tps[0][2].getColumnByDbName("_name"),
                Query.SortOrder.ASCENDING);
        Table table = new Table(new String[] {"wpoijgprw", "ogijewrgreo"},
                new String[] {"_name"}, new String[][] {{"Ned"}, {"Zed"}});
        when(dbts[0][2].getRaw(query, new String[] {"_name"}))
                .thenReturn(table);
        String msg = "@applicants ?Name !Position Engineer ~Name";
        assertTrue(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(smsSender).sendSMSWithCutoff(
                phoneNumCaptor.capture(), respCaptor.capture());
        assertEquals(SRC_PHONE_NUM, phoneNumCaptor.getValue());
        assertEquals("n:Ned;n:Zed", respCaptor.getValue());
    }
    
    public void testQueryWithDescendingSort() {
        Query query = new Query(allTps, tps[0][2]);
        query.addConstraint(tps[0][2].getColumnByDbName("_position"),
                Query.Comparator.EQUALS, "Engineer");
        query.setOrderBy(tps[0][2].getColumnByDbName("_name"),
                Query.SortOrder.DESCENDING);
        Table table = new Table(new String[] {"wpoijgprw", "ogijewrgreo"},
                new String[] {"_name"}, new String[][] {{"Fred"}, {"Ced"}});
        when(dbts[0][2].getRaw(query, new String[] {"_name"}))
                .thenReturn(table);
        String msg = "@applicants ?Name =Position Engineer ~Name desc";
        assertTrue(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(smsSender).sendSMSWithCutoff(
                phoneNumCaptor.capture(), respCaptor.capture());
        assertEquals(SRC_PHONE_NUM, phoneNumCaptor.getValue());
        assertEquals("n:Fred;n:Ced", respCaptor.getValue());
    }
    
    public void testQueryWithDateToDateEquals() {
        String userTime = "10/11/2010 1:15:23";
        String dbTime = du.formatDateTimeForDb(du.tryParseInstant(userTime));
        Query query = new Query(allTps, tps[0][0]);
        query.addConstraint(tps[0][0].getColumnByDbName("_time"),
                Query.Comparator.EQUALS, dbTime);
        Table table = new Table(new String[] {"wpoijgprw"},
                new String[] {"_temperature"}, new String[][] {{"12.7"}});
        when(dbts[0][0].getRaw(query, new String[] {"_temperature"}))
                .thenReturn(table);
        String msg = "@temps ?Temperature =Time " + userTime;
        assertTrue(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(smsSender).sendSMSWithCutoff(
                phoneNumCaptor.capture(), respCaptor.capture());
        assertEquals(SRC_PHONE_NUM, phoneNumCaptor.getValue());
        assertEquals("temp:12.7", respCaptor.getValue());
    }
    
    public void testQueryWithDateToDateLessThan() {
        String userTime = "10/11/2010 1:15:20";
        String dbTime = du.formatDateTimeForDb(du.tryParseInstant(userTime));
        Query query = new Query(allTps, tps[0][0]);
        query.addConstraint(tps[0][0].getColumnByDbName("_time"),
                Query.Comparator.LESS_THAN, dbTime);
        Table table = new Table(new String[] {"wpoijgprw"},
                new String[] {"_temperature"},
                new String[][] {{"20.4"}, {"14.7"}});
        when(dbts[0][0].getRaw(query, new String[] {"_temperature"}))
                .thenReturn(table);
        String msg = "@temps ?Temperature <Time " + userTime;
        assertTrue(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(smsSender).sendSMSWithCutoff(
                phoneNumCaptor.capture(), respCaptor.capture());
        assertEquals(SRC_PHONE_NUM, phoneNumCaptor.getValue());
        assertEquals("temp:20.4;temp:14.7", respCaptor.getValue());
    }
    
    public void testQueryWithDateToDateGreaterThan() {
        String userTime = "10/12/2010 10:15:20";
        String dbTime = du.formatDateTimeForDb(du.tryParseInstant(userTime));
        Query query = new Query(allTps, tps[0][0]);
        query.addConstraint(tps[0][0].getColumnByDbName("_time"),
                Query.Comparator.LESS_THAN, dbTime);
        Table table = new Table(new String[] {"wpoijgprw"},
                new String[] {"_temperature"}, new String[][] {{"1.4"}});
        when(dbts[0][0].getRaw(query, new String[] {"_temperature"}))
                .thenReturn(table);
        String msg = "@temps ?Temperature <Time " + userTime;
        assertTrue(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(smsSender).sendSMSWithCutoff(
                phoneNumCaptor.capture(), respCaptor.capture());
        assertEquals(SRC_PHONE_NUM, phoneNumCaptor.getValue());
        assertEquals("temp:1.4", respCaptor.getValue());
    }
    
    public void testQueryWithDateToDateNotEquals() {
        String userTime = "10/11/2010 1:15:23";
        String dbTime = du.formatDateTimeForDb(du.tryParseInstant(userTime));
        Query query = new Query(allTps, tps[0][0]);
        query.addConstraint(tps[0][0].getColumnByDbName("_time"),
                Query.Comparator.LESS_THAN, dbTime);
        Table table = new Table(new String[] {"wpoijgprw", "i43jgt43w",
                        "4ijgw", "pgjew", "pgj2w", "iur3f", "p0jreqf", "3r4f"},
                new String[] {"_temperature"},
                new String[][] {{"20.4"}, {"14.7"}, {"14.8"}, {"14.8"},
                        {"14.9"}, {"14.4"}, {"1.7"}, {"1.4"}});
        when(dbts[0][0].getRaw(query, new String[] {"_temperature"}))
                .thenReturn(table);
        String msg = "@temps ?Temperature <Time " + userTime;
        assertTrue(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(smsSender).sendSMSWithCutoff(
                phoneNumCaptor.capture(), respCaptor.capture());
        assertEquals(SRC_PHONE_NUM, phoneNumCaptor.getValue());
        assertEquals("temp:20.4;temp:14.7;temp:14.8;temp:14.8;temp:14.9;" +
                "temp:14.4;temp:1.7;temp:1.4", respCaptor.getValue());
    }
    
    public void testQueryWithRangeToDateEquals() {
        String dbStart = du.formatDateTimeForDb(du.tryParseInstant(
                "10/12/2010 00:00:00"));
        String dbEnd = du.formatDateTimeForDb(du.tryParseInstant(
                "10/13/2010 00:00:00"));
        Query query = new Query(allTps, tps[0][0]);
        query.addConstraint(tps[0][0].getColumnByDbName("_time"),
                Query.Comparator.GREATER_THAN_EQUALS, dbStart);
        query.addConstraint(tps[0][0].getColumnByDbName("_time"),
                Query.Comparator.LESS_THAN, dbEnd);
        Table table = new Table(new String[] {"wpoijgprw", "i43jgt43w"},
                new String[] {"_temperature"},
                new String[][] {{"14.4"}, {"1.7"}});
        when(dbts[0][0].getRaw(query, new String[] {"_temperature"}))
                .thenReturn(table);
        String msg = "@temps ?Temperature =Time 10/12/2010";
        assertTrue(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(smsSender).sendSMSWithCutoff(
                phoneNumCaptor.capture(), respCaptor.capture());
        assertEquals(SRC_PHONE_NUM, phoneNumCaptor.getValue());
        assertEquals("temp:14.4;temp:1.7", respCaptor.getValue());
    }
    
    public void testQueryWithRangeToDateLessThan() {
        String dbStart = du.formatDateTimeForDb(du.tryParseInstant(
                "10/12/2010 00:00:00"));
        Query query = new Query(allTps, tps[0][0]);
        query.addConstraint(tps[0][0].getColumnByDbName("_time"),
                Query.Comparator.LESS_THAN, dbStart);
        Table table = new Table(new String[] {"wpoijgprw", "i43jgt43w"},
                new String[] {"_temperature"},
                new String[][] {{"14.4"}, {"1.7"}});
        when(dbts[0][0].getRaw(query, new String[] {"_temperature"}))
                .thenReturn(table);
        String msg = "@temps ?Temperature <Time 10/12/2010";
        assertTrue(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(smsSender).sendSMSWithCutoff(
                phoneNumCaptor.capture(), respCaptor.capture());
        assertEquals(SRC_PHONE_NUM, phoneNumCaptor.getValue());
        assertEquals("temp:14.4;temp:1.7", respCaptor.getValue());
    }
    
    public void testQueryWithRangeToDateGreaterThan() {
        String dbEnd = du.formatDateTimeForDb(du.tryParseInstant(
                "10/12/2010 00:00:00"));
        Query query = new Query(allTps, tps[0][0]);
        query.addConstraint(tps[0][0].getColumnByDbName("_time"),
                Query.Comparator.GREATER_THAN_EQUALS, dbEnd);
        Table table = new Table(new String[] {"wpoijgprw", "i43jgt43w"},
                new String[] {"_temperature"},
                new String[][] {{"14.4"}, {"1.7"}});
        when(dbts[0][0].getRaw(query, new String[] {"_temperature"}))
                .thenReturn(table);
        String msg = "@temps ?Temperature >Time 10/11/2010";
        assertTrue(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(smsSender).sendSMSWithCutoff(
                phoneNumCaptor.capture(), respCaptor.capture());
        assertEquals(SRC_PHONE_NUM, phoneNumCaptor.getValue());
        assertEquals("temp:14.4;temp:1.7", respCaptor.getValue());
    }
    
    public void testQueryWithRangeToDateNotEquals() {
        String dbStart = du.formatDateTimeForDb(du.tryParseInstant(
                "10/11/2010 00:00:00"));
        String dbEnd = du.formatDateTimeForDb(du.tryParseInstant(
            "10/12/2010 00:00:00"));
        Query query = new Query(allTps, tps[0][0]);
        query.addOrConstraint(tps[0][0].getColumnByDbName("_time"),
                Query.Comparator.LESS_THAN, dbStart,
                Query.Comparator.GREATER_THAN_EQUALS, dbEnd);
        Table table = new Table(new String[] {"wpoijgprw", "i43jgt43w"},
                new String[] {"_temperature"},
                new String[][] {{"14.4"}, {"1.7"}});
        when(dbts[0][0].getRaw(query, new String[] {"_temperature"}))
                .thenReturn(table);
        String msg = "@temps ?Temperature !Time 10/11/2010";
        assertTrue(mh.handleMessage(msg, SRC_PHONE_NUM));
        verify(smsSender).sendSMSWithCutoff(
                phoneNumCaptor.capture(), respCaptor.capture());
        assertEquals(SRC_PHONE_NUM, phoneNumCaptor.getValue());
        assertEquals("temp:14.4;temp:1.7", respCaptor.getValue());
    }
    
    public void testQueryOnSecuredTableWithPassword() {
        String phoneNum = DATA[SECURITY_TABLES_INDEX][0][1][1];
        String password = DATA[SECURITY_TABLES_INDEX][0][1][2];
        setUpReadSecurityTable(2, 0, phoneNum);
        Query query = new Query(allTps, tps[0][2]);
        query.addConstraint(tps[0][2].getColumnByDbName("_position"),
                Query.Comparator.EQUALS, "Engineer");
        Table table = new Table(new String[] {"wpoijgprw", "ogijewrgreo"},
                new String[] {"_name"}, new String[][] {{"Ced"}, {"Fred"}});
        when(dbts[0][2].getRaw(query, new String[] {"_name"}))
                .thenReturn(table);
        String msg = "@applicants ?Name =Position Engineer #" + password;
        assertTrue(mh.handleMessage(msg, phoneNum));
        verify(smsSender).sendSMSWithCutoff(
                phoneNumCaptor.capture(), respCaptor.capture());
        assertEquals(phoneNum, phoneNumCaptor.getValue());
        assertEquals("n:Ced;n:Fred", respCaptor.getValue());
    }
    
    public void testQueryOnSecuredTableWithoutPassword() {
        String phoneNum = DATA[SECURITY_TABLES_INDEX][0][1][1];
        setUpReadSecurityTable(2, 0, phoneNum);
        Query query = new Query(allTps, tps[0][2]);
        query.addConstraint(tps[0][2].getColumnByDbName("_position"),
                Query.Comparator.EQUALS, "Engineer");
        Table table = new Table(new String[] {"wpoijgprw", "ogijewrgreo"},
                new String[] {"_name"}, new String[][] {{"Ced"}, {"Fred"}});
        when(dbts[0][2].getRaw(query, new String[] {"_name"}))
                .thenReturn(table);
        String msg = "@applicants ?Name =Position Engineer";
        assertFalse(mh.handleMessage(msg, phoneNum));
        verify(smsSender, never()).sendSMSWithCutoff(
                phoneNumCaptor.capture(), respCaptor.capture());
    }
    
    public void testQueryOnSecuredTableWithInvalidPassword() {
        String phoneNum = DATA[SECURITY_TABLES_INDEX][0][1][1];
        setUpReadSecurityTable(2, 0, phoneNum);
        Query query = new Query(allTps, tps[0][2]);
        query.addConstraint(tps[0][2].getColumnByDbName("_position"),
                Query.Comparator.EQUALS, "Engineer");
        Table table = new Table(new String[] {"wpoijgprw", "ogijewrgreo"},
                new String[] {"_name"}, new String[][] {{"Ced"}, {"Fred"}});
        when(dbts[0][2].getRaw(query, new String[] {"_name"}))
                .thenReturn(table);
        String msg = "@applicants ?Name =Position Engineer #ihavenopassword";
        assertFalse(mh.handleMessage(msg, phoneNum));
        verify(smsSender, never()).sendSMSWithCutoff(
                phoneNumCaptor.capture(), respCaptor.capture());
    }
    
    private void setUpReadSecurityTable(int dtIndex, int stIndex,
            String phoneNum) {
        when(tps[0][dtIndex].getReadSecurityTableId()).thenReturn(
                TABLE_IDS[SECURITY_TABLES_INDEX][stIndex]);
        String[] secColsArr = new String[] {
                SecurityUtil.USER_COLUMN_NAME,
                SecurityUtil.PHONENUM_COLUMN_NAME,
                SecurityUtil.PASSWORD_COLUMN_NAME};
        List<String> passwords = new ArrayList<String>();
        for (int i = 0; i < DATA[SECURITY_TABLES_INDEX][stIndex].length; i++) {
            if (DATA[SECURITY_TABLES_INDEX][stIndex][i][1].equals(phoneNum)) {
                passwords.add(DATA[SECURITY_TABLES_INDEX][stIndex][i][2]);
            }
        }
        String[] rowIds = new String[passwords.size()];
        String[][] data = new String[passwords.size()][];
        for (int i = 0; i < rowIds.length; i++) {
            rowIds[i] = i + "ojregeg";
            data[i] = new String[] {passwords.get(i)};
        }
        Table secTable = new Table(rowIds, secColsArr, data);
        when(dbts[SECURITY_TABLES_INDEX][0].getRaw(new String[] {"_password"},
                new String[] {"_phonenum"}, new String[] {phoneNum}, null))
                .thenReturn(secTable);
    }
}
