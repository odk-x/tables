package org.opendatakit.hope.sync;

import java.util.ArrayList;
import java.util.List;

/**
 * An object for measuring the results of a synchronization call. This is 
 * especially intended to see how to display the results to the user. For 
 * example, imagine you wanted to synchronize three tables. The object should
 * contain three {@link TableResult} objects, mapping the dbTableName to the
 * {@link Status} corresponding to outcome.
 * @author sudar.sam@gmail.com
 *
 */
public class SynchronizationResult {
  
  private List<TableResult> mResults;
  
  public SynchronizationResult() {
    this.mResults = new ArrayList<TableResult>();
  }
  
  /**
   * Get all the {@link TableResult} objects in this result.
   * @return
   */
  public List<TableResult> getTableResults() {
    return this.mResults;
  }
  
  public void addTableResult(TableResult newResult) {
    this.mResults.add(newResult);
  }

}
