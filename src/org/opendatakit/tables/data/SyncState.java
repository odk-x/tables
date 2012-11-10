package org.opendatakit.tables.data;

/**
 * The state of the table with regards to synching to the datastore. 
 * <p>
 * NB: Lowercase due to considerations regarding javascript and being able to
 * call the valueOf function to get the enum.
 * @author sudar.sam@gmail.com
 *
 */
public enum SyncState {
  rest,
  inserting,
  updating,
  deleting,
  conflicting;
}
