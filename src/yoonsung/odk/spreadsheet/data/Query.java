package yoonsung.odk.spreadsheet.data;

import java.util.ArrayList;
import java.util.List;
import yoonsung.odk.spreadsheet.sync.SyncUtil;


public class Query {
    
    private static final String KW_JOIN = "join";
    
    private TableProperties[] tps;
    private TableProperties tp;
    private List<Constraint> constraints;
    private List<Join> joins;
    
    public Query(TableProperties[] tps, TableProperties tp) {
        this.tps = tps;
        this.tp = tp;
        constraints = new ArrayList<Constraint>();
        joins = new ArrayList<Join>();
    }
    
    public int getConstraintCount() {
        return constraints.size();
    }
    
    public Constraint getConstraint(int index) {
        return constraints.get(index);
    }
    
    public int getJoinCount() {
        return joins.size();
    }
    
    public Join getJoin(int index) {
        return joins.get(index);
    }
    
    public void clear() {
        constraints.clear();
        joins.clear();
    }
    
    /**
     * Attempts to parse a user query. If the query is malformed, it may return
     * false, but this is not guaranteed (if it does return false though, the
     * query is definitely malformed).
     */
    public boolean loadFromUserQuery(String query) {
        List<String> keys = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        int startIndex = 0;
        int colonIndex = query.indexOf(':');
        if ((colonIndex < 0) || (colonIndex == query.length() - 1)) {
            return false;
        }
        while (startIndex < query.length()) {
            // when starting an iteration, there must be at least one more
            // colon, and it must not be the last character
            colonIndex = query.indexOf(':', startIndex);
            String key = query.substring(startIndex, colonIndex);
            keys.add(key);
            int nextColonIndex = query.indexOf(':', colonIndex + 1);
            if (nextColonIndex < 0 || nextColonIndex == (query.length() - 1)) {
                // either there aren't any more colons, or the only remaining
                // colon is the last character, so call whatever remains the
                // value and break out of the loop
                values.add(query.substring(colonIndex + 1));
                break;
            }
            String value = null;
            int nextStartIndex = query.lastIndexOf(' ', nextColonIndex);
            while (nextStartIndex < colonIndex + 2) {
                if (query.indexOf(' ', nextColonIndex) < 0) {
                    // we've reached the end of the string
                    value = query.substring(colonIndex + 1);
                    break;
                }
                // nextStartIndex needs to be the index of the space before the
                // word before the next colon. Loop until this space is not
                // before (or immediately after) the current colon. This may
                // get to the end of the string, in which case it will break
                // out of this loop, call the remainder of the string the
                // value, and also break out of the outer loop.
                nextColonIndex = query.indexOf(':', nextColonIndex + 1);
                nextStartIndex = query.lastIndexOf(' ', nextColonIndex);
                if (nextColonIndex < 0 ||
                        nextColonIndex == (query.length() - 1)) {
                    value = query.substring(colonIndex + 1);
                    break;
                }
            }
            if (value != null) {
                values.add(value);
                break;
            }
            if (!KW_JOIN.equalsIgnoreCase(key)) {
                // if it's just a constraint, not a join, we don't need to
                // worry about anything else
                startIndex = nextStartIndex + 1;
                values.add(query.substring(colonIndex + 1, nextStartIndex));
                continue;
            }
            // it's a join, so the parentheses have to match up
            int parenIndex = query.indexOf('(', colonIndex);
            if (parenIndex > nextColonIndex) {
                // there aren't any parentheses until after the next colon
                // (i.e., there aren't any parentheses within the value)
                // (because there aren't any constraints on the join table), so
                // handle it just like a constraint
                startIndex = nextStartIndex + 1;
                values.add(query.substring(colonIndex + 1, nextStartIndex));
                continue;
            }
            int openParenCount = 1;
            while (openParenCount != 0) {
                int leftParenIndex = query.indexOf('(', parenIndex + 1);
                int rightParenIndex = query.indexOf(')', parenIndex + 1);
                if (rightParenIndex < 0) {
                    // there aren't any more close parentheses, so it's
                    // invalid; treat it as a normal value
                    value = query.substring(colonIndex + 1, nextStartIndex);
                    break;
                }
                if ((leftParenIndex < 0) || (rightParenIndex < leftParenIndex)) {
                    parenIndex = rightParenIndex;
                    openParenCount -= 1;
                } else {
                    parenIndex = leftParenIndex;
                    openParenCount += 1;
                }
            }
            if (value != null) {
                startIndex = nextStartIndex + 1;
                values.add(value);
                continue;
            }
            nextColonIndex = query.indexOf(':', parenIndex);
            if ((nextColonIndex < 0) ||
                    (nextColonIndex == query.length() - 1)) {
                // we've reached the end of the string
                values.add(query.substring(colonIndex + 1));
                break;
            }
            nextStartIndex = query.lastIndexOf(' ', nextColonIndex);
            while (nextStartIndex < parenIndex) {
                nextColonIndex = query.indexOf(':', nextColonIndex + 1);
                if (nextColonIndex < 0 ||
                        nextColonIndex == (query.length() - 1)) {
                    value = query.substring(colonIndex + 1);
                    break;
                }
                nextStartIndex = query.lastIndexOf(' ', nextColonIndex);
            }
            if (value != null) {
                values.add(value);
                break;
            }
            values.add(query.substring(colonIndex + 1, nextStartIndex));
            startIndex = nextStartIndex + 1;
        }
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = values.get(i);
            if (!KW_JOIN.equals(key)) {
                String cdn = getColumnByUserString(key);
                if (cdn == null) {
                    return false;
                }
                constraints.add(new Constraint(cdn, value));
                continue;
            }
            String tableName;
            String queryString;
            String matchString;
            if (value.contains("(")) {
                int leftParenIndex = value.indexOf('(');
                int rightParenIndex = value.lastIndexOf(')');
                if (rightParenIndex < leftParenIndex) {
                    String cdn = getColumnByUserString(key);
                    if (cdn == null) {
                        return false;
                    }
                    constraints.add(new Constraint(cdn, value));
                    continue;
                }
                tableName = value.substring(0, leftParenIndex).trim();
                queryString = value.substring(leftParenIndex + 1,
                        rightParenIndex);
                matchString = value.substring(rightParenIndex + 1);
            } else {
                int firstSpaceIndex = value.indexOf(' ');
                if (firstSpaceIndex < 0) {
                    String cdn = getColumnByUserString(key);
                    if (cdn == null) {
                        return false;
                    }
                    constraints.add(new Constraint(cdn, value));
                    continue;
                }
                tableName = value.substring(0, firstSpaceIndex);
                queryString = null;
                matchString = value.substring(firstSpaceIndex + 1);
            }
            TableProperties joinTp = null;
            for (TableProperties tp : tps) {
                if (tp.getDisplayName().toLowerCase().equals(tableName)) {
                    joinTp = tp;
                }
            }
            if (joinTp == null) {
                String cdn = getColumnByUserString(key);
                if (cdn == null) {
                    return false;
                }
                constraints.add(new Constraint(cdn, value));
                continue;
            }
            Query joinQuery = null;
            if (queryString != null) {
                Query q = new Query(tps, joinTp);
                if (q.loadFromUserQuery(queryString)) {
                    joinQuery = q;
                }
            }
            String[] matchSplit = matchString.trim().split("\\s+");
            String[] matchKeys = new String[matchSplit.length];
            String[] matchArgs = new String[matchSplit.length];
            boolean allValid = true;
            for (int j = 0; j < matchSplit.length; j++) {
                String match = matchSplit[j];
                boolean valid = match.contains("/");
                if (!valid) {
                    allValid = false;
                    continue;
                }
                String[] split = matchSplit[j].split("/");
                String matchKey = getColumnByUserString(split[0]);
                String matchArg = getColumnByUserString(split[1]);
                if ((matchKey == null) || (matchArg == null)) {
                    allValid = false;
                    continue;
                }
                matchKeys[j] = matchKey;
                matchArgs[j] = matchArg;
            }
            if (allValid) {
                joins.add(new Join(joinTp, joinQuery, matchKeys, matchArgs));
            } else {
                String cdn = getColumnByUserString(key);
                if (cdn == null) {
                    return false;
                }
                constraints.add(new Constraint(cdn, value));
            }
        }
        return true;
    }
    
    private String getColumnByUserString(String us) {
        String cdn = tp.getColumnByDisplayName(us);
        if (cdn != null) {
            return cdn;
        }
        return tp.getColumnByAbbreviation(cdn);
    }
    
    public void addConstraint(ColumnProperties cp, String value) {
        constraints.add(new Constraint(cp.getColumnDbName(), value));
    }
    
    public SqlData toSql(String[] columns) {
        return toSql(columns, true);
    }
    
    private SqlData toSql(String[] columns, boolean includeId) {
        SqlData sd = new SqlData();
        if (includeId) {
            sd.appendSql("SELECT " + DbTable.DB_ROW_ID);
        } else {
            sd.appendSql("SELECT " + columns[0]);
        }
        for (int i = (includeId ? 0 : 1); i < columns.length; i++) {
            sd.appendSql(", " + columns[i]);
        }
        sd.appendSql(" FROM " + tp.getDbTableName());
        sd.appendSql(" WHERE " + DbTable.DB_SYNC_STATE + " != " +
                SyncUtil.State.DELETING);
        for (int i = 0; i < constraints.size(); i++) {
            SqlData csd = constraints.get(i).toSql();
            sd.appendSql(" AND " + csd.getSql());
            sd.appendArgs(csd.getArgList());
        }
        for (int i = 0; i < joins.size(); i++) {
            sd.append(joins.get(i).toSql());
        }
        return sd;
    }
    
    /**
     * Builds the SQL string for querying the database table.
     * 
     * Supposing t is the table name, a is a prime column, b is the sort
     * column, c is another column, and the user's query was "c:12", the query
     * should be something like:
     * SELECT id, a, b, c FROM t JOIN (
     *   SELECT MAX(id) FROM
     *     (SELECT a, MAX(b) FROM t WHERE c = 12 GROUP BY a) x
     *     JOIN
     *     (SELECT id, a, b, from t WHERE c = 12) y
     *     ON x.a = y.a AND x.b = y.b
     *     GROUP BY a, b
     * ) z ON t.id = z.id
     * Or, if there is no sort column:
     * SELECT id, a, b, c FROM t JOIN (
     *   SELECT MAX(id) FROM t WHERE c = 12 GROUP BY a
     * ) z ON t.id = z.id
     * @param columns the columns to select
     * @return a SqlData object, with the SQL string and an array of arguments
     */
    public SqlData toOverviewSql(String[] columns) {
        if (tp.getPrimeColumns().length == 0) {
            return toSql(columns);
        }
        StringBuilder primeList = new StringBuilder();
        for (String prime : tp.getPrimeColumns()) {
            primeList.append(", " + prime);
        }
        primeList.delete(0, 2);
        SqlData sd = new SqlData();
        sd.appendSql("SELECT d." + DbTable.DB_ROW_ID);
        for (String column : columns) {
            sd.appendSql(", " + column);
        }
        sd.appendSql(" FROM " + tp.getDbTableName() + " d");
        sd.appendSql(" JOIN (");
        
        if (tp.getSortColumn() == null) {
            sd.append(toSql(new String[] {"MAX(" + DbTable.DB_ROW_ID +
                    ") AS " + DbTable.DB_ROW_ID}, false));
            sd.appendSql(" GROUP BY " + primeList.toString());
        } else {
            String sort = tp.getSortColumn();
            sd.appendSql("SELECT MAX(" + DbTable.DB_ROW_ID + ") AS " +
                    DbTable.DB_ROW_ID + "FROM ");
            
            String[] primes = tp.getPrimeColumns();
            String[] xCols = new String[primes.length + 1];
            String[] yCols = new String[primes.length + 1];
            for (int i = 0; i < primes.length; i++) {
                xCols[i] = primes[i];
                yCols[i] = primes[i];
            }
            xCols[primes.length] = "MAX(" + sort + ")";
            yCols[primes.length] = sort;
            
            sd.appendSql("(" + toSql(xCols, false) + " GROUP BY " +
                    primeList.toString() + ") x");
            sd.appendSql(" JOIN ");
            sd.appendSql("(" + toSql(yCols) + ") y");
            
            sd.appendSql(" ON x." + sort + " = y." + sort);
            for (String prime : tp.getPrimeColumns()) {
                sd.appendSql(" AND x." + prime + " = y." + prime);
            }
            sd.appendSql(" GROUP BY " + primeList.toString() + ", " + sort);
        }
        
        sd.appendSql(") z ON d." + DbTable.DB_ROW_ID + " = z." +
                DbTable.DB_ROW_ID);
        return sd;
    }
    
    public SqlData toSql(List<String> columns) {
        return toSql(columns.toArray(new String[0]));
    }
    
    public String toUserQuery() {
        StringBuilder sb = new StringBuilder();
        for (Constraint c : constraints) {
            sb.append(c.toUserQuery() + " ");
        }
        for (Join j : joins) {
            sb.append(j.toUserQuery() + " ");
        }
        return sb.toString().trim();
    }
    
    public class Constraint {
        
        private String cdn;
        private String value;
        
        public Constraint(String cdn, String value) {
            this.cdn = cdn;
            this.value = value;
        }
        
        public String getColumnDbName() {
            return cdn;
        }
        
        public String getValue() {
            return value;
        }
        
        public SqlData toSql() {
            SqlData sd = new SqlData();
            sd.appendSql(cdn + " = ?");
            sd.appendArg(value);
            return sd;
        }
        
        public String toUserQuery() {
            return tp.getColumnByDbName(cdn).getDisplayName() + ":" + value;
        }
    }
    
    public class Join {
        
        private TableProperties tp;
        private Query query;
        private String[] matchKeys;
        private String[] matchArgs;
        
        public Join(TableProperties tp, Query query, String[] matchKeys,
                String[] matchArgs) {
            this.tp = tp;
            if (query == null) {
                this.query = new Query(tps, tp);
            } else {
                this.query = query;
            }
            this.matchKeys = matchKeys;
            this.matchArgs = matchArgs;
        }
        
        public TableProperties getJoinTable() {
            return tp;
        }
        
        public Query getQuery() {
            return query;
        }
        
        public int getMatchCount() {
            return matchKeys.length;
        }
        
        public String getMatchKey(int index) {
            return matchKeys[index];
        }
        
        public String getMatchArg(int index) {
            return matchArgs[index];
        }
        
        public SqlData toSql() {
            SqlData sd = new SqlData();
            sd.appendSql("JOIN ");
            sd.appendSql("(" + query.toSql(tp.getColumnOrder()) + ")");
            sd.appendSql(" ON ");
            sd.appendSql(matchToSql(0));
            for (int i = 1; i < matchKeys.length; i++) {
                sd.appendSql(" AND " + matchToSql(i));
            }
            return sd;
        }
        
        private String matchToSql(int index) {
            return matchKeys[index] + " = " + matchArgs[index];
        }
        
        public String toUserQuery() {
            StringBuilder sb = new StringBuilder();
            sb.append("join:" + tp.getDisplayName());
            String qString = query.toUserQuery();
            if (qString.length() != 0) {
                sb.append(" (" + qString + ")");
            }
            for (int i = 0; i < matchKeys.length; i++) {
                sb.append(" " + matchKeys[i] + "/" + matchArgs[i]);
            }
            return sb.toString();
        }
    }
    
    public class SqlData {
        
        private StringBuilder sql;
        private List<String> args;
        
        public SqlData() {
            this.sql = new StringBuilder();
            this.args = new ArrayList<String>();
        }
        
        public void appendSql(String a) {
            sql.append(a);
        }
        
        public void appendArg(String a) {
            args.add(a);
        }
        
        private void appendArgs(List<String> a) {
            args.addAll(a);
        }
        
        public void append(SqlData a) {
            appendSql(a.getSql());
            appendArgs(a.getArgList());
        }
        
        public String getSql() {
            return sql.toString();
        }
        
        private List<String> getArgList() {
            return args;
        }
        
        public String[] getArgs() {
            return args.toArray(new String[0]);
        }
    }
}
