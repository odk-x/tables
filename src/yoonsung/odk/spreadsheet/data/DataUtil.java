package yoonsung.odk.spreadsheet.data;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class DataUtil {
    
    private static final DateFormat dbDateFormat =
        new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss");
    
    public static String getNowInDbFormat() {
        return dbDateFormat.format(new Date());
    }
    
    /**
     * Attempts to convert user input into something that can be used in the
     * database. Returns null if the value is invalid.
     */
    public static String validifyValue(String value, ColumnProperties cp) {
        switch (cp.getColumnType()) {
        case ColumnProperties.ColumnType.NUMBER:
            return validifyNumericValue(value);
        case ColumnProperties.ColumnType.DATE:
            return validifyDateValue(value);
        case ColumnProperties.ColumnType.DATE_RANGE:
            return validifyDateRangeValue(value);
        case ColumnProperties.ColumnType.MC_OPTIONS:
            return validifyMcOptionsValue(value,
                    cp.getMultipleChoiceOptions());
        default:
            return value;
        }
    }
    
    private static String validifyNumericValue(String value) {
        value = value.trim();
        try {
            Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
        return value;
    }
    
    private static String validifyDateValue(String value) {
        Date d = tryDateFormat(value, dbDateFormat);
        if (d != null) {
            return dbDateFormat.format(d);
        }
        return null;
    }
    
    private static Date tryDateFormat(String value, DateFormat format) {
        try {
            return format.parse(value);
        } catch (ParseException e) {
            return null;
        }
    }
    
    private static String validifyDateRangeValue(String value) {
        String[] values = value.split(" - ");
        String value0 = validifyDateValue(values[0]);
        String value1 = validifyDateValue(values[1]);
        if ((value0 == null) || (value1 == null)) {
            return null;
        }
        return value0 + "/" + value1;
    }
    
    private static String validifyMcOptionsValue(String value,
            String[] options) {
        String bestMatch = null;
        for (String option : options) {
            if (option.equals(value)) {
                return value;
            }
            if ((bestMatch == null) && option.equalsIgnoreCase(value)) {
                bestMatch = option;
            }
        }
        return bestMatch;
    }
}
