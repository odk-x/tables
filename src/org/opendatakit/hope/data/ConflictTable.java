package org.opendatakit.hope.data;

/**
 * Holds data from both the server and local databases about the rows that 
 * are in conflict. Of all the rows in the entire database, these are the 
 * subset that are in conflict. There is a notion of a server-side and a local
 * version of each of these rows.
 * @author sudar.sam@gmail.com
 *
 */
public class ConflictTable {
  
  private UserTable mLocalTable;
  private UserTable mServerTable;
  
  /**
   * Construct a conflict table. For the tables to have any sort of ability to
   * relate to one another, their rows must have been sorted on something that
   * will be shared between them, e.g. their UUID. This way the ith row will in
   * each table will be sure to point to the same row, assuming that there is
   * nothing gone wrong.
   * @param localTable
   * @param serverTable
   */
  public ConflictTable(UserTable localTable, UserTable serverTable) {
    this.mLocalTable = localTable;
    this.mServerTable = serverTable;
  }
  
  public UserTable getLocalTable() {
    return this.mLocalTable;
  }
  
  public UserTable getServerTable() {
    return this.mServerTable;
  }

}
