package yoonsung.odk.spreadsheet.data;

import java.util.ArrayList;
import java.util.List;


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
            boolean allValid = true;
            for (String match : matchSplit) {
                allValid = allValid && match.contains("/");
            }
            if (!allValid) {
                String cdn = getColumnByUserString(key);
                if (cdn == null) {
                    return false;
                }
                constraints.add(new Constraint(cdn, value));
                continue;
            }
            String[] matchKeys = new String[matchSplit.length];
            String[] matchArgs = new String[matchSplit.length];
            for (int j = 0; j < matchSplit.length; j++) {
                String[] split = matchSplit[j].split("/");
                matchKeys[j] = split[0];
                matchArgs[j] = split[1];
            }
            joins.add(new Join(joinTp, joinQuery, matchKeys, matchArgs));
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
    }
    
    public class Join {
        
        private TableProperties tp;
        private Query query;
        private String[] matchKeys;
        private String[] matchArgs;
        
        public Join(TableProperties tp, Query query, String[] matchKeys,
                String[] matchArgs) {
            this.tp = tp;
            this.query = query;
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
    }
}
