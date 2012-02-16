package yoonsung.odk.spreadsheet.sync;

/**
 * A utility class for common synchronization methods and definitions.
 */
public class SyncUtil {
    
    public class State {
        public static final int REST = 0;
        public static final int INSERTING = 1;
        public static final int UPDATING = 2;
        public static final int DELETING = 3;
        public static final int CONFLICTING = 4;
        private State() {}
    }
    
    public class Transactioning {
        public static final int FALSE = 0;
        public static final int TRUE = 1;
        private Transactioning() {}
    }
}
