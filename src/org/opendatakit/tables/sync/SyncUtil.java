package org.opendatakit.tables.sync;

/**
 * A utility class for common synchronization methods and definitions.
 */
public class SyncUtil {

  /**
   * <p>
   * Synchronization state.
   * </p>
   * <p>
   * Here is a brief overview of the rules for transitions between states on
   * basic write operations:
   * 
   * <pre>
   * insert: 
   *     state = INSERTING
   *   
   * update:
   *     if state == REST:
   *        state = UPDATING
   *     
   * delete:
   *     if state == REST or state == UPDATING:
   *        state = DELETING
   *        don't actually delete yet
   *     else if state == INSERTING:
   *        actually delete
   * </pre>
   * 
   * </p>
   * <p>
   * The {@link SyncProcessor} handles moving resources from the INSERTING,
   * UPDATING, or DELETING states back to the REST state. CONFLICTING is a
   * special state set by the SyncProcessor to signify conflicts between local
   * and remote updates to the same resource and is handled separately from the
   * basic write operations.
   * 
   */
  public class State {
    public static final int REST = 0;
    public static final int INSERTING = 1;
    public static final int UPDATING = 2;
    public static final int DELETING = 3;
    public static final int CONFLICTING = 4;

    private State() {
    }
  }

  public static boolean intToBool(int i) {
    return i != 0;
  }

  public static int boolToInt(boolean b) {
    return b ? 1 : 0;
  }
}
