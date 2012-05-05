package data;

import testutil.DataTest;
import org.opendatakit.tables.data.Query;


public class QueryTests extends DataTest {
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }
    
    public void testSimpleUserParse() {
        String q = "ID:2";
        Query query = new Query(allTps, tps[0][0]);
        assertTrue(query.loadFromUserQuery(q));
        assertEquals(1, query.getConstraintCount());
        assertEquals(0, query.getJoinCount());
        assertEquals("_id", query.getConstraint(0).getColumnDbName());
        assertEquals(1, query.getConstraint(0).getComparisonCount());
        assertEquals("2", query.getConstraint(0).getValue(0));
        assertEquals(Query.Comparator.EQUALS,
                query.getConstraint(0).getComparator(0));
    }
    
    public void testUserParseWithMultipleConstraints() {
        String q = "ID:2 Temperature:12.7";
        Query query = new Query(allTps, tps[0][0]);
        assertTrue(query.loadFromUserQuery(q));
        assertEquals(2, query.getConstraintCount());
        assertEquals(0, query.getJoinCount());
        assertEquals("_id", query.getConstraint(0).getColumnDbName());
        assertEquals(1, query.getConstraint(0).getComparisonCount());
        assertEquals("2", query.getConstraint(0).getValue(0));
        assertEquals(Query.Comparator.EQUALS,
                query.getConstraint(0).getComparator(0));
        assertEquals("_temperature", query.getConstraint(1).getColumnDbName());
        assertEquals(1, query.getConstraint(1).getComparisonCount());
        assertEquals("12.7", query.getConstraint(1).getValue(0));
        assertEquals(Query.Comparator.EQUALS,
                query.getConstraint(1).getComparator(0));
    }
    
    public void testUserParseWithColumnAbbreviation() {
        String q = "temp:12.7";
        Query query = new Query(allTps, tps[0][0]);
        assertTrue(query.loadFromUserQuery(q));
        assertEquals(1, query.getConstraintCount());
        assertEquals(0, query.getJoinCount());
        assertEquals("_temperature", query.getConstraint(0).getColumnDbName());
        assertEquals(1, query.getConstraint(0).getComparisonCount());
        assertEquals("12.7", query.getConstraint(0).getValue(0));
        assertEquals(Query.Comparator.EQUALS,
                query.getConstraint(0).getComparator(0));
    }
    
    public void testUserParseWithSimpleJoin() {
        String q = "join:fridges (District:Seattle) ID/ID";
        Query query = new Query(allTps, tps[0][0]);
        assertTrue(query.loadFromUserQuery(q));
        assertEquals(0, query.getConstraintCount());
        assertEquals(1, query.getJoinCount());
        assertEquals("_fridges",
                query.getJoin(0).getJoinTable().getDbTableName());
        assertEquals(1, query.getJoin(0).getMatchCount());
        assertEquals("_id", query.getJoin(0).getMatchKey(0));
        assertEquals("_id", query.getJoin(0).getMatchArg(0));
        Query joinQuery = query.getJoin(0).getQuery();
        assertEquals(1, joinQuery.getConstraintCount());
        assertEquals(0, joinQuery.getJoinCount());
        assertEquals("_district",
                joinQuery.getConstraint(0).getColumnDbName());
        assertEquals(1, joinQuery.getConstraint(0).getComparisonCount());
        assertEquals("Seattle", joinQuery.getConstraint(0).getValue(0));
        assertEquals(Query.Comparator.EQUALS,
                joinQuery.getConstraint(0).getComparator(0));
    }
    
    public void testUserParseWithTwoLevelJoin() {
        String q = "join:fridges (join:districts (Name:Northwest) " +
                "District/Name) ID/ID";
        Query query = new Query(allTps, tps[0][0]);
        assertTrue(query.loadFromUserQuery(q));
        assertEquals(0, query.getConstraintCount());
        assertEquals(1, query.getJoinCount());
        assertEquals("_fridges",
                query.getJoin(0).getJoinTable().getDbTableName());
        assertEquals(1, query.getJoin(0).getMatchCount());
        assertEquals("_id", query.getJoin(0).getMatchKey(0));
        assertEquals("_id", query.getJoin(0).getMatchArg(0));
        Query firstJq = query.getJoin(0).getQuery();
        assertEquals(0, firstJq.getConstraintCount());
        assertEquals(1, firstJq.getJoinCount());
        assertEquals("_districts",
                firstJq.getJoin(0).getJoinTable().getDbTableName());
        assertEquals(1, firstJq.getJoin(0).getMatchCount());
        assertEquals("_district", firstJq.getJoin(0).getMatchKey(0));
        assertEquals("_name", firstJq.getJoin(0).getMatchArg(0));
        Query secondJq = firstJq.getJoin(0).getQuery();
        assertEquals(1, secondJq.getConstraintCount());
        assertEquals(0, secondJq.getJoinCount());
        assertEquals("_name", secondJq.getConstraint(0).getColumnDbName());
        assertEquals(1, secondJq.getConstraint(0).getComparisonCount());
        assertEquals("Northwest", secondJq.getConstraint(0).getValue(0));
        assertEquals(Query.Comparator.EQUALS,
                secondJq.getConstraint(0).getComparator(0));
    }
}
