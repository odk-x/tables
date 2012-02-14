package yoonsung.odk.spreadsheet.Activity.util;

/**
 * A utility class for functions related to security.
 * 
 * @author hkworden@gmail.com
 */
public class SecurityUtil {
    
    public static final String USER_COLUMN_NAME = "user";
    public static final String PHONENUM_COLUMN_NAME = "phone";
    public static final String PASSWORD_COLUMN_NAME = "pass";
    
    public static boolean couldBeSecurityTable(String[] columns) {
        return (getSecurityIndices(columns) != null);
    }
    
    private static int[] getSecurityIndices(String[] columns) {
        int[] indices = {-1, -1, -1};
        for (int i = 0; i < columns.length; i++) {
            String column = columns[i];
            if (column.contains(USER_COLUMN_NAME)) {
                if (indices[0] != -1) {
                    return null;
                }
                indices[0] = i;
            }
            if (column.contains(PHONENUM_COLUMN_NAME)) {
                if (indices[1] != -1) {
                    return null;
                }
                indices[1] = i;
            }
            if (column.contains(PASSWORD_COLUMN_NAME)) {
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
