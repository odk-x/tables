package org.opendatakit.tables.utils;

/**
 * Basic holder for the components of a SQL query.
 * @author sudar.sam@gmail.com
 *
 */
public class SQLQueryStruct {
  
  public String whereClause;
  public String[] selectionArgs;
  public String[] groupBy;
  public String having;
  public String orderByElementKey;
  public String orderByDirection;
  
  public SQLQueryStruct(
      String whereClause,
      String[] selectionArgs,
      String[] groupBy,
      String having,
      String orderByElementKey,
      String orderByDirection) {
    this.whereClause = whereClause;
    this.selectionArgs = selectionArgs;
    this.groupBy = groupBy;
    this.having = having;
    this.orderByElementKey = orderByElementKey;
    this.orderByDirection = orderByDirection;
  }

}
