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
package org.opendatakit.tables.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendatakit.tables.sync.SyncUtil;


public class Query {
    
    public enum GroupQueryType { COUNT, AVERAGE, MINIMUM, MAXIMUM, SUM }
    
    private static final String KW_JOIN = "join";
    
    public class Comparator {
        public static final int EQUALS = 0;
        public static final int NOT_EQUALS = 1;
        public static final int LESS_THAN = 2;
        public static final int LESS_THAN_EQUALS = 3;
        public static final int GREATER_THAN = 4;
        public static final int GREATER_THAN_EQUALS = 5;
        public static final int LIKE = 6;
        private Comparator() {}
    }
    
    public class SortOrder {
        public static final int ASCENDING = 0;
        public static final int DESCENDING = 1;
        private SortOrder() {};
    }
    
    private TableProperties[] tps;
    private TableProperties tp;
    private List<Constraint> constraints;
    private List<Join> joins;
    private String orderBy;
    private int sortOrder;
    
    public Query(TableProperties[] tps, TableProperties tp) {
        this.tps = tps;
        this.tp = tp;
        constraints = new ArrayList<Constraint>();
        joins = new ArrayList<Join>();
        orderBy = tp.getSortColumn();
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
                constraints.add(new Constraint(cdn, Comparator.EQUALS, value));
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
                    constraints.add(new Constraint(cdn, Comparator.EQUALS,
                            value));
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
                    constraints.add(new Constraint(cdn, Comparator.EQUALS,
                            value));
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
                constraints.add(new Constraint(cdn, Comparator.EQUALS, value));
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
                ColumnProperties matchArgCp =
                    joinTp.getColumnByUserLabel(split[1]);
                if ((matchKey == null) || (matchArgCp == null)) {
                    allValid = false;
                    continue;
                }
                matchKeys[j] = matchKey;
                matchArgs[j] = matchArgCp.getColumnDbName();
            }
            if (allValid) {
                joins.add(new Join(joinTp, joinQuery, matchKeys, matchArgs));
            } else {
                String cdn = getColumnByUserString(key);
                if (cdn == null) {
                    return false;
                }
                constraints.add(new Constraint(cdn, Comparator.EQUALS, value));
            }
        }
        return true;
    }
    
    private String getColumnByUserString(String us) {
        ColumnProperties cp = tp.getColumnByUserLabel(us);
        return (cp == null) ? null : cp.getColumnDbName();
    }
    
    public void addConstraint(ColumnProperties cp, String value) {
        constraints.add(new Constraint(cp.getColumnDbName(), Comparator.EQUALS,
                value));
    }
    
    public void addConstraint(ColumnProperties cp, int comparator,
            String value) {
        constraints.add(new Constraint(cp.getColumnDbName(), comparator,
                value));
    }
    
    public void addOrConstraint(ColumnProperties cp, int comparator1,
            String value1, int comparator2, String value2) {
        Constraint oc = new Constraint(cp.getColumnDbName(), comparator1,
                value1);
        oc.addComparison(comparator2, value2);
        constraints.add(oc);
    }
    
    public void removeConstraint(int index) {
        constraints.remove(index);
    }
    
    public void setOrderBy(ColumnProperties cp, int sortOrder) {
        orderBy = cp.getColumnDbName();
        this.sortOrder = sortOrder;
    }
    
    public SqlData toSql(String[] columns) {
        SqlData sd = toSql(columns, true);
        if (orderBy != null) {
            if (sortOrder == SortOrder.ASCENDING) {
                sd.appendSql(" ORDER BY " + orderBy + " ASC");
            } else {
                sd.appendSql(" ORDER BY " + orderBy + " DESC");
            }
        }
        return sd;
    }
    
    private SqlData toSql(String[] columns, boolean includeId) {
        String dbTn = tp.getDbTableName();
        StringBuilder sb = new StringBuilder();
        if (includeId) {
            sb.append(dbTn + "." + DbTable.DB_ROW_ID + " AS " +
                    DbTable.DB_ROW_ID);
        } else {
            sb.append(dbTn + "." + columns[0] + " AS " + columns[0]);
        }
        for (int i = (includeId ? 0 : 1); i < columns.length; i++) {
            sb.append(", " + dbTn + "." + columns[i] + " AS " + columns[i]);
        }
        return toSql(sb.toString());
    }
    
    private SqlData toSql(String selection) {
        SqlData sd = new SqlData();
        sd.appendSql("SELECT " + selection);
        sd.appendSql(" FROM " + tp.getDbTableName());
        for (int i = 0; i < joins.size(); i++) {
            SqlData joinSd = joins.get(i).toSql();
            sd.appendSql(" " + joinSd.getSql());
            sd.appendArgs(joinSd.getArgList());
        }
        sd.appendSql(" WHERE " + tp.getDbTableName() + "." +
                DbTable.DB_SYNC_STATE + " != " + SyncUtil.State.DELETING);
        for (int i = 0; i < constraints.size(); i++) {
            SqlData csd = constraints.get(i).toSql();
            sd.appendSql(" AND " + csd.getSql());
            sd.appendArgs(csd.getArgList());
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
            sd.appendSql(", d." + column);
        }
        sd.appendSql(" FROM " + tp.getDbTableName() + " d");
        sd.appendSql(" JOIN (");
        
        if (tp.getSortColumn() == null) {
            sd.append(toSql("MAX(" + tp.getDbTableName() + "." +
                    DbTable.DB_ROW_ID + ") AS " + DbTable.DB_ROW_ID));
            sd.appendSql(" GROUP BY " + primeList.toString());
        } else {
            String sort = tp.getSortColumn();
            sd.appendSql("SELECT MAX(" + DbTable.DB_ROW_ID + ") AS " +
                    DbTable.DB_ROW_ID + " FROM ");
            
            String[] primes = tp.getPrimeColumns();
            String[] xCols = new String[primes.length];
            String[] yCols = new String[primes.length + 1];
            for (int i = 0; i < primes.length; i++) {
                xCols[i] = primes[i];
                yCols[i] = primes[i];
            }
            yCols[primes.length] = sort;
            
            StringBuilder xSelectionSb = new StringBuilder();
            for (String xCol : xCols) {
                xSelectionSb.append(tp.getDbTableName() + "." + xCol + " AS " +
                        xCol + ", ");
            }
            xSelectionSb.append("MAX(" + sort + ") AS " + sort);
            SqlData xSqlData = toSql(xSelectionSb.toString());
            SqlData ySqlData = toSql(yCols);
            sd.appendSql("(" + xSqlData.getSql() + " GROUP BY " +
                    primeList.toString() + ") x");
            sd.appendSql(" JOIN ");
            sd.appendSql("(" + ySqlData.getSql() + ") y");
            sd.appendArgs(xSqlData.getArgList());
            sd.appendArgs(ySqlData.getArgList());
            
            sd.appendSql(" ON x." + sort + " = y." + sort);
            for (String prime : tp.getPrimeColumns()) {
                sd.appendSql(" AND x." + prime + " = y." + prime);
            }
            sd.appendSql(" GROUP BY ");
            for (String prime : tp.getPrimeColumns()) {
                sd.appendSql("x." + prime + ", ");
                primeList.append(", " + prime);
            }
            sd.appendSql("x." + sort);
        }
        
        sd.appendSql(") z ON d." + DbTable.DB_ROW_ID + " = z." +
                DbTable.DB_ROW_ID);
        return sd;
    }
    
    public SqlData toSql(List<String> columns) {
        return toSql(columns.toArray(new String[0]));
    }
    
    public SqlData toGroupSql(String groupColumn, GroupQueryType type) {
        String typeSql;
        switch (type) {
        case AVERAGE:
            typeSql = "(SUM(" + groupColumn + ") / COUNT(" + groupColumn +
                    "))";
            break;
        case COUNT:
            typeSql = "COUNT(" + groupColumn + ")";
            break;
        case MAXIMUM:
            typeSql = "MAX(" + groupColumn + ")";
            break;
        case MINIMUM:
            typeSql = "MIN(" + groupColumn + ")";
            break;
        case SUM:
            typeSql = "SUM(" + groupColumn + ")";
            break;
        default:
            throw new RuntimeException();
        }
        SqlData sd = toSql(groupColumn + ", " + typeSql + " AS g");
        sd.appendSql(" GROUP BY " + groupColumn);
        return sd;
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
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Query)) {
            return false;
        }
        Query other = (Query) obj;
        if (!tp.equals(other.tp) ||
                (constraints.size() != other.constraints.size()) ||
                (joins.size() != other.joins.size()) ||
                (sortOrder != other.sortOrder)) {
            return false;
        }
        if (((orderBy == null) && (other.orderBy != null)) ||
                ((orderBy != null) && (other.orderBy == null)) ||
                ((orderBy != null) && !orderBy.equals(other.orderBy))) {
            return false;
        }
        Set<Integer> indices = new HashSet<Integer>();
        for (int i = 0; i < constraints.size(); i++) {
            indices.add(i);
        }
        for (int i = 0; i < constraints.size(); i++) {
            int matchIndex = -1;
            for (int index : indices) {
                if (constraints.get(i).equals(other.constraints.get(index))) {
                    matchIndex = index;
                }
            }
            indices.remove(matchIndex);
        }
        if (!indices.isEmpty()) {
            return false;
        }
        indices.clear();
        for (int i = 0; i < joins.size(); i++) {
            indices.add(i);
        }
        for (int i = 0; i < joins.size(); i++) {
            int matchIndex = -1;
            for (int index : indices) {
                if (joins.get(i).equals(other.joins.get(index))) {
                    matchIndex = index;
                }
            }
            indices.remove(matchIndex);
        }
        return indices.isEmpty();
    }
    
    @Override
    public int hashCode() {
        int hashCode = tp.hashCode();
        if (orderBy != null) {
            hashCode += orderBy.hashCode() + sortOrder;
        }
        for (Constraint c : constraints) {
            hashCode += c.hashCode();
        }
        for (Join j : joins) {
            hashCode += j.hashCode();
        }
        return hashCode;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(tp.toString());
        if (orderBy != null) {
            sb.append("/order:" + orderBy + "-" + sortOrder);
        }
        sb.append(constraints.toString());
        sb.append(joins.toString());
        return sb.toString();
    }
    
    public class Constraint {
        
        private String cdn;
        private List<Integer> comparators;
        private List<String> values;
        
        public Constraint(String cdn, int comparator, String value) {
            this.cdn = cdn;
            this.comparators = new ArrayList<Integer>();
            this.values = new ArrayList<String>();
            comparators.add(comparator);
            values.add(value);
        }
        
        public String getColumnDbName() {
            return cdn;
        }
        
        public int getComparisonCount() {
            return comparators.size();
        }
        
        public int getComparator(int index) {
            return comparators.get(index);
        }
        
        public String getValue(int index) {
            return values.get(index);
        }
        
        public void addComparison(int comparator, String value) {
            comparators.add(comparator);
            values.add(value);
        }
        
        public SqlData toSql() {
            SqlData sd = new SqlData();
            sd.appendSql("(" + getSqlString(0));
            sd.appendArg(values.get(0));
            for (int i = 1; i < comparators.size(); i++) {
                sd.appendSql(" OR " + getSqlString(i));
                sd.appendArg(values.get(i));
            }
            sd.appendSql(")");
            return sd;
        }
        
        private String getSqlString(int index) {
            String cName = tp.getDbTableName() + "." + cdn;
            switch (comparators.get(index)) {
            case Comparator.EQUALS:
                return cName + " = ?";
            case Comparator.NOT_EQUALS:
                return cName + " != ?";
            case Comparator.LESS_THAN:
                return cName + " < ?";
            case Comparator.LESS_THAN_EQUALS:
                return cName + " <= ?";
            case Comparator.GREATER_THAN:
                return cName + " > ?";
            case Comparator.GREATER_THAN_EQUALS:
                return cName + " >= ?";
            case Comparator.LIKE:
                return cName + " LIKE ?";
            default:
                throw new RuntimeException("Invalid comparator: " +
                        comparators.get(index));
            }
        }
        
        public String toUserQuery() {
            StringBuilder sb = new StringBuilder(
                    tp.getColumnByDbName(cdn).getDisplayName() + ":");
            sb.append(values.get(0));
            for (int i = 1; i < comparators.size(); i++) {
                sb.append("|" + values.get(i));
            }
            return sb.toString();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Constraint)) {
                return false;
            }
            Constraint other = (Constraint) obj;
            if ((comparators.size() != other.comparators.size()) ||
                    !cdn.equals(other.cdn)) {
                return false;
            }
            Set<Integer> indices = new HashSet<Integer>();
            for (int i = 0; i < comparators.size(); i++) {
                indices.add(i);
            }
            for (int i = 0; i < comparators.size(); i++) {
                int matchIndex = -1;
                for (int index : indices) {
                    if ((comparators.get(i) == other.comparators.get(index)) &&
                            values.get(i).equals(other.values.get(index))) {
                        matchIndex = index;
                    }
                }
                indices.remove(matchIndex);
            }
            return indices.isEmpty();
        }
        
        @Override
        public int hashCode() {
            int hashCode = cdn.hashCode();
            for (int i = 0; i < comparators.size(); i++) {
                hashCode += comparators.get(i) + values.get(i).hashCode();
            }
            return hashCode;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(cdn);
            for (int i = 0; i < comparators.size(); i++) {
                switch (comparators.get(i)) {
                case Comparator.EQUALS:
                    sb.append("/=" + values.get(i));
                    break;
                case Comparator.LESS_THAN:
                    sb.append("/<" + values.get(i));
                    break;
                case Comparator.LESS_THAN_EQUALS:
                    sb.append("/<=" + values.get(i));
                    break;
                case Comparator.GREATER_THAN:
                    sb.append("/>" + values.get(i));
                    break;
                case Comparator.GREATER_THAN_EQUALS:
                    sb.append("/>=" + values.get(i));
                    break;
                case Comparator.NOT_EQUALS:
                    sb.append("/!" + values.get(i));
                    break;
                }
            }
            return sb.toString();
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
            SqlData qSql = query.toSql(tp.getColumnOrder());
            sd.appendSql("(" + qSql.getSql() + ")");
            sd.appendArgs(qSql.getArgList());
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
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Join)) {
                return false;
            }
            Join other = (Join) obj;
            if (!tp.equals(other.tp) || !query.equals(other.query) ||
                    (matchKeys.length != other.matchKeys.length)) {
                return false;
            }
            Set<Integer> indices = new HashSet<Integer>();
            for (int i = 0; i < matchKeys.length; i++) {
                indices.add(i);
            }
            for (int i = 0; i < matchKeys.length; i++) {
                int matchIndex = -1;
                for (int index : indices) {
                    if (matchKeys[i].equals(other.matchKeys[index]) &&
                            matchArgs[i].equals(other.matchArgs[index])) {
                        matchIndex = index;
                    }
                }
                indices.remove(matchIndex);
            }
            return indices.isEmpty();
        }
        
        @Override
        public int hashCode() {
            int hashCode = tp.hashCode() + query.hashCode();
            for (int i = 0; i < matchKeys.length; i++) {
                hashCode += matchKeys[i].hashCode() + matchArgs[i].hashCode();
            }
            return hashCode;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(tp.getDbTableName());
            sb.append("/" + query.toString());
            for (int i = 0; i < matchKeys.length; i++) {
                sb.append("/" + matchKeys[i] + ":" + matchArgs[i]);
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
