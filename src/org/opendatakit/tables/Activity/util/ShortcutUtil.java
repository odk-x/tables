package org.opendatakit.tables.Activity.util;

/**
 * A utility class for functions related to shortcuts.
 * 
 * @author hkworden@gmail.com
 */
public class ShortcutUtil {
    
    public static final String LABEL_COLUMN_NAME = "label";
    public static final String INPUT_COLUMN_NAME = "input";
    public static final String OUTPUT_COLUMN_NAME = "output";
    
    public static boolean couldBeShortcutTable(String[] columns) {
        return (getShortcutIndices(columns) != null);
    }
    
    private static int[] getShortcutIndices(String[] columns) {
        int[] indices = {-1, -1, -1};
        for (int i = 0; i < columns.length; i++) {
            String column = columns[i];
            if (column.contains(LABEL_COLUMN_NAME)) {
                if (indices[0] != -1) {
                    return null;
                }
                indices[0] = i;
            }
            if (column.contains(INPUT_COLUMN_NAME)) {
                if (indices[1] != -1) {
                    return null;
                }
                indices[1] = i;
            }
            if (column.contains(OUTPUT_COLUMN_NAME)) {
                if (indices[2] != -1) {
                    return null;
                }
                indices[2] = i;
            }
        }
        for (int index : indices) {
            if (index == -1) {
                return null;
            }
        }
        return indices;
    }
}
