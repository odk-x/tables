package yoonsung.odk.spreadsheet.data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class DataUtil {
    
    private static final DateFormat dbDateFormat =
        new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss");
    
    public static String getNowInDbFormat() {
        return dbDateFormat.format(new Date());
    }
}
