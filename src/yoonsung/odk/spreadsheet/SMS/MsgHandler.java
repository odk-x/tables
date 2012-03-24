package yoonsung.odk.spreadsheet.SMS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import yoonsung.odk.spreadsheet.Activity.util.SecurityUtil;
import yoonsung.odk.spreadsheet.Activity.util.ShortcutUtil;
import yoonsung.odk.spreadsheet.data.ColumnProperties;
import yoonsung.odk.spreadsheet.data.DataManager;
import yoonsung.odk.spreadsheet.data.DataUtil;
import yoonsung.odk.spreadsheet.data.DbTable;
import yoonsung.odk.spreadsheet.data.Query;
import yoonsung.odk.spreadsheet.data.Query.Constraint;
import yoonsung.odk.spreadsheet.data.Table;
import yoonsung.odk.spreadsheet.data.TableProperties;

/**
 * A class for handling incoming messages.
 */
public class MsgHandler {
    
    private enum Type {ADD, QUERY}
    
    private static final int DEFAULT_LIMIT = 25;
    
    private DataUtil du;
    private DataManager dm;
    private TableProperties[] tps;
    private TableProperties[] dataTps;
    private TableProperties[] scTps;
    private SMSSender smsSender;
    
    public MsgHandler(DataManager dm, SMSSender smsSender) {
        this.du = DataUtil.getDefaultDataUtil();
        this.dm = dm;
        this.smsSender = smsSender;
    }
    
    public boolean handleMessage(String msg, String phoneNum) {
        if (!checkIsMessage(msg)) {
            return false;
        }
        init();
        msg = standardize(msg);
        TableProperties tp = findTable(msg);
        if (tp == null) {
            return false;
        }
        Type type = determineType(msg);
        if (type == null) {
            return false;
        }
        if (!checkSecurity(msg, phoneNum, tp, type)) {
            return false;
        }
        msg = stripPassword(msg);
        switch (type) {
        case ADD:
            return handleAdd(tp, msg, phoneNum);
        case QUERY:
            return handleQuery(tp, msg, phoneNum);
        default:
            return false;
        }
    }
    
    private void init() {
        tps = dm.getAllTableProperties();
        dataTps = dm.getDataTableProperties();
        scTps = dm.getShortcutTableProperties();
    }
    
    private boolean checkIsMessage(String msg) {
        return msg.startsWith("@") && (msg.lastIndexOf(' ') > 0);
    }
    
    private String standardize(String msg) {
        msg = msg.trim();
        List<String> scNames = new ArrayList<String>();
        List<String> scInputs = new ArrayList<String>();
        List<String> scOutputs = new ArrayList<String>();
        for (TableProperties scTp : scTps) {
            String[] scCols = new String[] {
                    scTp.getColumnByDisplayName(
                            ShortcutUtil.LABEL_COLUMN_NAME),
                    scTp.getColumnByDisplayName(
                            ShortcutUtil.INPUT_COLUMN_NAME),
                    scTp.getColumnByDisplayName(
                            ShortcutUtil.OUTPUT_COLUMN_NAME)};
            DbTable dbt = dm.getDbTable(scTp.getTableId());
            Table table = dbt.getRaw(new Query(tps, scTp), scCols);
            for (int i = 0; i < table.getHeight(); i++) {
                scNames.add(table.getData(i, 0));
                scInputs.add(table.getData(i, 1));
                scOutputs.add(table.getData(i, 2));
            }
        }
        int iterCount = 0;
        while (iterCount < scNames.size()) {
            String[] split = msg.split(" ");
            String target = split[0].substring(1);
            for (int j = 0; j < scNames.size(); j++) {
                if (!target.equals(scNames.get(j))) {
                    continue;
                }
                String nextMsg = convertByShortcut(msg, scInputs.get(j),
                        scOutputs.get(j));
                if ((nextMsg != null) && checkIsMessage(nextMsg)) {
                    msg = nextMsg.trim();
                    break;
                }
            }
            iterCount++;
        }
        return msg;
    }
    
    private String convertByShortcut(String msg, String input, String output) {
        Map<String, String> values = new HashMap<String, String>();
        String[] inSplit = input.split("%");
        if (!msg.startsWith(inSplit[0])) {
            return null;
        }
        int index = 0;
        for (int i = 1; i < inSplit.length; i += 2) {
            index += inSplit[i - 1].length();
            int nextIndex = msg.indexOf(inSplit[i + 1], index);
            if (nextIndex < 0) {
                return null;
            }
            values.put(inSplit[i], msg.substring(index, nextIndex));
        }
        String[] outSplit = output.split("%");
        int start = output.startsWith("%") ? 0 : 1;
        for (int i = start; i < outSplit.length; i += 2) {
            String key = outSplit[i];
            if (!values.containsKey(key)) {
                return null;
            }
            outSplit[i] = values.get(key);
        }
        StringBuilder sb = new StringBuilder();
        for (String s : outSplit) {
            sb.append(s);
        }
        return sb.toString();
    }
    
    private TableProperties findTable(String msg) {
        String target = msg.split(" ")[0].substring(1);
        for (TableProperties tp : dataTps) {
            if (tp.getDisplayName().equals(target)) {
                return tp;
            }
        }
        return null;
    }
    
    private Type determineType(String msg) {
        String[] split = msg.split(" ");
        if (split.length < 2) {
            return null;
        }
        if (split[1].startsWith("+")) {
            return Type.ADD;
        } else {
            return Type.QUERY;
        }
    }
    
    private boolean checkSecurity(String msg, String phoneNum,
            TableProperties tp, Type type) {
        String secTableId;
        if (type == Type.ADD) {
            secTableId = tp.getWriteSecurityTableId();
        } else {
            secTableId = tp.getReadSecurityTableId();
        }
        if (secTableId == null) {
            return true;
        }
        String password = "";
        int lastHashIndex = msg.lastIndexOf('#');
        if ((lastHashIndex > 0) && (msg.length() > lastHashIndex + 2) &&
                (msg.charAt(lastHashIndex - 1) == ' ')) {
            password = msg.substring(lastHashIndex + 1);
        }
        TableProperties secTp = dm.getTableProperties(secTableId);
        DbTable sDbt = dm.getDbTable(secTableId);
        Table table = sDbt.getRaw(
                new String[] {secTp.getColumnByDisplayName(
                        SecurityUtil.PASSWORD_COLUMN_NAME)},
                new String[] {secTp.getColumnByDisplayName(
                        SecurityUtil.PHONENUM_COLUMN_NAME)},
                new String[] {phoneNum}, null);
        for (int i = 0; i < table.getHeight(); i++) {
            if (password.equals(table.getData(i, 0))) {
                return true;
            }
        }
        return false;
    }
    
    private String stripPassword(String msg) {
        int lastSpaceIndex = msg.lastIndexOf(' ');
        if ((msg.length() > lastSpaceIndex + 1) &&
                (msg.charAt(lastSpaceIndex + 1) == '#')) {
            return msg.substring(0, lastSpaceIndex).trim();
        }
        return msg;
    }
    
    private boolean handleAdd(TableProperties tp, String msg,
            String phoneNum) {
        Map<String, String> values = new HashMap<String, String>();
        int plusIndex = msg.indexOf(" +") + 1;
        while (plusIndex > 0) {
            int spaceIndex = msg.indexOf(' ', plusIndex + 2);
            String key = msg.substring(plusIndex + 1, spaceIndex).trim();
            int secondSpaceIndex = msg.indexOf(' ', spaceIndex + 1);
            if (secondSpaceIndex < 0) {
                String value = msg.substring(spaceIndex).trim();
                values.put(key, value);
                break;
            }
            plusIndex = msg.indexOf(" +", secondSpaceIndex) + 1;
            if (plusIndex < 0) {
                String value = msg.substring(spaceIndex).trim();
                values.put(key, value);
                break;
            }
            String value = msg.substring(spaceIndex, plusIndex).trim();
            values.put(key, value);
        }
        Map<String, String> rowValues = new HashMap<String, String>();
        for (String key : values.keySet()) {
            ColumnProperties cp = tp.getColumnByUserLabel(key);
            if (cp == null) {
                return false;
            }
            String value = du.validifyValue(cp, values.get(key));
            if (value == null) {
                return false;
            } else if (cp.getSmsIn()) {
                rowValues.put(cp.getColumnDbName(), value);
            }
        }
        DbTable dbt = dm.getDbTable(tp.getTableId());
        dbt.addRow(rowValues, du.formatNowForDb(), phoneNum);
        return true;
    }
    
    private boolean handleQuery(TableProperties tp, String msg,
            String phoneNum) {
        List<Integer> indices = new ArrayList<Integer>();
        int spaceIndex = msg.indexOf(' ');
        int charIndex = minNonNegative(msg.indexOf(" =", spaceIndex),
                msg.indexOf(" <", spaceIndex), msg.indexOf(" >", spaceIndex),
                msg.indexOf(" !", spaceIndex), msg.indexOf(" /", spaceIndex),
                msg.indexOf(" ~", spaceIndex), msg.indexOf(" ?", spaceIndex));
        while (charIndex > 0) {
            indices.add(charIndex + 1);
            int index = charIndex + 2;
            charIndex = minNonNegative(msg.indexOf(" =", index),
                    msg.indexOf(" <", index), msg.indexOf(" >", index),
                    msg.indexOf(" !", index), msg.indexOf(" /", index),
                    msg.indexOf(" ~", index), msg.indexOf(" ?", index));
        }
        indices.add(msg.length());
        List<ColumnProperties> cols = new ArrayList<ColumnProperties>();
        Query query = new Query(tps, tp);
        ColumnProperties drSlotColumn = null;
        int drSlotDuration = 0;
        for (int i = 0; i < indices.size() - 1; i++) {
            String token = msg.substring(indices.get(i), indices.get(i + 1));
            char c = token.charAt(0);
            spaceIndex = token.indexOf(' ', 2);
            String value = (spaceIndex < 0) ? null :
                    token.substring(spaceIndex).trim();
            String colName = (spaceIndex < 0) ? token.substring(1).trim() :
                    token.substring(1, spaceIndex).trim();
            ColumnProperties cp = tp.getColumnByUserLabel(colName);
            if (cp == null) {
                return false;
            }
            if (c == '?') {
                cols.add(cp);
            } else if (c == '~') {
                if ((value != null) && value.startsWith("d")) {
                    query.setOrderBy(cp, Query.SortOrder.DESCENDING);
                } else {
                    query.setOrderBy(cp, Query.SortOrder.ASCENDING);
                }
            } else if (c == '/') {
                if ((drSlotColumn != null) || (value == null) ||
                    (cp.getColumnType() !=
                    ColumnProperties.ColumnType.DATE_RANGE)) {
                    return false;
                }
                drSlotColumn = cp;
                drSlotDuration = du.tryParseDuration(value);
                if (drSlotDuration < 0) {
                    return false;
                }
            } else {
                if (!addConstraint(query, cp, c, value)) {
                    return false;
                }
            }
        }
        if (drSlotColumn == null) {
            return respondToSimpleQuery(phoneNum, tp, cols, query);
        } else {
            return respondToDrSlotQuery(phoneNum, tp, query, drSlotColumn,
                    drSlotDuration);
        }
    }
    
    private boolean addConstraint(Query query, ColumnProperties cp,
            char comparator, String value) {
        if ((cp.getColumnType() == ColumnProperties.ColumnType.DATE_RANGE) ||
                (cp.getColumnType() == ColumnProperties.ColumnType.DATE)) {
            Interval interval = du.tryParseInterval(value);
            if (interval == null) {
                DateTime dt = du.tryParseInstant(value);
                if (dt == null) {
                    return false;
                }
                String dbValue = du.formatDateTimeForDb(dt);
                if (comparator == '=') {
                    if (cp.getColumnType() ==
                        ColumnProperties.ColumnType.DATE_RANGE) {
                        return false;
                    }
                    query.addConstraint(cp, Query.Comparator.EQUALS, dbValue);
                } else if (comparator == '<') {
                    query.addConstraint(cp, Query.Comparator.LESS_THAN,
                            dbValue);
                } else if (comparator == '>') {
                    query.addConstraint(cp, Query.Comparator.GREATER_THAN,
                            dbValue);
                } else {
                    if (cp.getColumnType() ==
                        ColumnProperties.ColumnType.DATE_RANGE) {
                        return false;
                    }
                    query.addConstraint(cp, Query.Comparator.NOT_EQUALS,
                            dbValue);
                }
            } else {
                if (comparator == '=') {
                    if (cp.getColumnType() ==
                        ColumnProperties.ColumnType.DATE) {
                        query.addConstraint(cp,
                                Query.Comparator.GREATER_THAN_EQUALS,
                                du.formatDateTimeForDb(interval.getStart()));
                        query.addConstraint(cp, Query.Comparator.LESS_THAN,
                                du.formatDateTimeForDb(interval.getEnd()));
                    } else {
                        query.addConstraint(cp, Query.Comparator.EQUALS,
                                du.formatIntervalForDb(interval));
                    }
                } else if (comparator == '<') {
                    query.addConstraint(cp, Query.Comparator.LESS_THAN,
                            du.formatDateTimeForDb(interval.getStart()));
                } else if (comparator == '>') {
                    query.addConstraint(cp,
                            Query.Comparator.GREATER_THAN_EQUALS,
                            du.formatDateTimeForDb(interval.getEnd()));
                } else {
                    if (cp.getColumnType() ==
                        ColumnProperties.ColumnType.DATE) {
                        query.addOrConstraint(cp, Query.Comparator.LESS_THAN,
                                du.formatDateTimeForDb(interval.getStart()),
                                Query.Comparator.GREATER_THAN_EQUALS,
                                du.formatDateTimeForDb(interval.getEnd()));
                    } else {
                        query.addConstraint(cp, Query.Comparator.NOT_EQUALS,
                                du.formatIntervalForDb(interval));
                    }
                }
            }
        } else {
            value = du.validifyValue(cp, value);
            if (value == null) {
                return false;
            }
            if (comparator == '=') {
                query.addConstraint(cp, Query.Comparator.EQUALS, value);
            } else if (comparator == '<') {
                query.addConstraint(cp, Query.Comparator.LESS_THAN, value);
            } else if (comparator == '>') {
                query.addConstraint(cp, Query.Comparator.GREATER_THAN, value);
            } else {
                query.addConstraint(cp, Query.Comparator.NOT_EQUALS, value);
            }
        }
        return true;
    }
    
    private boolean respondToSimpleQuery(String phoneNum, TableProperties tp,
            List<ColumnProperties> cols, Query query) {
        if (cols.isEmpty()) {
            return false;
        }
        String[] colNames = new String[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            colNames[i] = cols.get(i).getColumnDbName();
        }
        DbTable dbt = dm.getDbTable(tp.getTableId());
        Table table = dbt.getRaw(query, colNames);
        String resp;
        if (table.getHeight() == 0) {
            resp = "No rows found.";
        } else {
            int limit = Math.min(DEFAULT_LIMIT, table.getHeight());
            List<String> rows = new ArrayList<String>();
            for (int i = 0; i < limit; i++) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < cols.size(); j++) {
                    String colName = cols.get(j).getAbbreviation();
                    if (colName == null) {
                        colName = cols.get(j).getDisplayName();
                    }
                    sb.append("," + colName + ":" + table.getData(i, j));
                }
                sb.deleteCharAt(0);
                rows.add(sb.toString());
            }
            StringBuilder sb = new StringBuilder(rows.get(0));
            for (int i = 1; i < rows.size(); i++) {
                sb.append(";" + rows.get(i));
            }
            resp = sb.toString();
        }
        smsSender.sendSMSWithCutoff(phoneNum, resp);
        return true;
    }
    
    private boolean respondToDrSlotQuery(String phoneNum, TableProperties tp,
            Query query, ColumnProperties drSlotColumn, int drSlotDuration) {
        Set<Constraint> constraints = new HashSet<Constraint>();
        for (int i = query.getConstraintCount(); i >= 0; i--) {
            Constraint c = query.getConstraint(i);
            if (c.getColumnDbName().equals(drSlotColumn.getColumnDbName())) {
                constraints.add(c);
                query.removeConstraint(i);
            }
        }
        query.setOrderBy(drSlotColumn, Query.SortOrder.ASCENDING);
        DbTable dbt = dm.getDbTable(tp.getTableId());
        Table table = dbt.getRaw(query,
                new String[] {drSlotColumn.getColumnDbName()});
        List<String[]> rawRanges = new ArrayList<String[]>();
        for (int i = 0; i < table.getHeight(); i++) {
            rawRanges.add(table.getData(i, 0).split("/"));
        }
        String earlyDate = null;
        String lateDate = null;
        for (int i = 0; i < query.getConstraintCount(); i++) {
            Constraint c = query.getConstraint(i);
            if (c.getComparisonCount() == 2) {
                String[] range;
                if (c.getComparator(0) == Query.Comparator.LESS_THAN) {
                    range = new String[] {c.getValue(0), c.getValue(1)};
                } else {
                    range = new String[] {c.getValue(1), c.getValue(0)};
                }
                boolean inserted = false;
                for (int j = 0; j < rawRanges.size(); j++) {
                    if (range[0].compareTo(rawRanges.get(j)[0]) < 0) {
                        rawRanges.add(j, range);
                        inserted = true;
                        break;
                    }
                }
                if (!inserted) {
                    rawRanges.add(range);
                }
            } else if (c.getComparator(0) == Query.Comparator.LESS_THAN) {
                if (lateDate == null) {
                    lateDate = c.getValue(0);
                } else if (c.getValue(0).compareTo(lateDate) < 0){
                    lateDate = c.getValue(0);
                }
            } else if (c.getComparator(0) == Query.Comparator.GREATER_THAN) {
                if (earlyDate == null) {
                    earlyDate = c.getValue(0);
                } else if (c.getValue(0).compareTo(earlyDate) > 0){
                    earlyDate = c.getValue(0);
                }
            }
        }
        if (earlyDate != null) {
            rawRanges.add(new String[] {null, earlyDate});
        }
        if (lateDate != null) {
            for (int j = 0; j < rawRanges.size(); j++) {
                if (lateDate.compareTo(rawRanges.get(j)[0]) < 0) {
                    rawRanges.add(j, new String[] {lateDate, null});
                    break;
                }
            }
        }
        if (rawRanges.isEmpty()) {
            String resp = "anytime";
            smsSender.sendSMSWithCutoff(phoneNum, resp);
            return true;
        }
        List<String[]> ranges = new ArrayList<String[]>();
        ranges.add(rawRanges.get(0));
        for (int i = 1; i < rawRanges.size(); i++) {
            String[] lastRange = ranges.get(ranges.size() - 1);
            String[] nextRange = rawRanges.get(i);
            if (nextRange[1] == null) {
                ranges.add(nextRange);
                break;
            }
            if (nextRange[0].compareTo(lastRange[1]) > 0) {
                ranges.add(nextRange);
            } else if (nextRange[1].compareTo(lastRange[1]) > 0) {
                lastRange[1] = nextRange[1];
            }
        }
        StringBuilder sb = new StringBuilder();
        if (ranges.get(0)[0] != null) {
            DateTime dt = du.parseDateTimeFromDb(ranges.get(0)[0]);
            sb.append(";before" + du.formatShortDateTimeForUser(dt));
        }
        for (int i = 1; i < ranges.size(); i++) {
            DateTime start = du.parseDateTimeFromDb(ranges.get(i - 1)[1]);
            DateTime end = du.parseDateTimeFromDb(ranges.get(i)[0]);
            Duration duration = new Duration(start, end);
            if (duration.getStandardSeconds() >= drSlotDuration) {
                sb.append(";" + du.formatShortDateTimeForUser(start) +
                        "-" + du.formatShortDateTimeForUser(end));
            }
        }
        if (ranges.get(ranges.size() - 1)[1] != null) {
            DateTime dt = du.parseDateTimeFromDb(
                    ranges.get(ranges.size() - 1)[1]);
            sb.append(";after" + du.formatShortDateTimeForUser(dt));
        }
        sb.deleteCharAt(0);
        String resp = sb.toString();
        smsSender.sendSMSWithCutoff(phoneNum, resp);
        return true;
    }
    
    private int minNonNegative(int...vals) {
        int min = -1;
        for (int val : vals) {
            if (val >= 0) {
                min = (min == -1) ? val : Math.min(min, val);
            }
        }
        return min;
    }
}
