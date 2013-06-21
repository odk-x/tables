package org.opendatakit.tables.sync;

/**
 * The mapping of a table to the status of its synchronization.
 * @author sudar.sam@gmail.com
 *
 */
public class TableResult {
  
  private String mDbTableName;
  private Status mStatus;
  private String mMessage;
  
  public TableResult(String dbTableName, Status status) {
    this.mDbTableName = dbTableName;
    this.mStatus = status;
    this.mMessage = status.name();
  }
  
  public String getDbTableName() {
    return this.mDbTableName;
  }
  
  public Status getStatus() {
    return this.mStatus;
  }
  
  /**
   * Set a message that might be passed back to the user. Likely a place
   * to pass the error message back to the user in case of exceptions.
   * @param message
   */
  public void setMessage(String message) {
    this.mMessage = message;
  }
  
  public String getMessage() {
    return this.mMessage;
  }
  
  /**
   * Update the status of this result. 
   * @param newStatus
   * @throws UnsupportedOperationException if the satus has been set to 
   * {@link Status#EXCEPTION} and the newStatus is something other than
   * {@link Status#EXCEPTION}.
   */
  public void setStatus(Status newStatus) {
    if (this.mStatus == Status.EXCEPTION && newStatus != Status.EXCEPTION) {
      throw new UnsupportedOperationException("Tried to set TableResult " +
      		"status" +
      		" to something other than exception when it had alread been set" +
      		" to exception.");
    }
    this.mStatus = newStatus;
  }
  
  /**
   * The result of an individual table. 
   * @author sudar.sam@gmail.com
   *
   */
  public enum Status {
    SUCCESS, FAILURE, EXCEPTION;
  }
  
}
