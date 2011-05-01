package yoonsung.odk.spreadsheet.Database;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A utility class for data operations.
 */
public class DataUtils {
    
    private static DataUtils du; // the DataUtils object
    
    // date-formatting objects
    private DateFormat dbFormatter;
    // date-parsing objects
    private DateFormat dowParserAbbr;
    private DateFormat dowParserFull;
    private Map<Integer, Set<DateFormat>> dmParsers;
    private Map<Integer, Set<DateFormat>> timeParsers;
    
    private DataUtils() {
        // preparing the formatters
        dbFormatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        // preparing the day parsers
        dowParserAbbr = new SimpleDateFormat("E");
        dowParserFull = new SimpleDateFormat("EEEE");
        dmParsers = new HashMap<Integer, Set<DateFormat>>();
        Set<DateFormat> dmParserSet3 = new HashSet<DateFormat>();
        dmParserSet3.add(new SimpleDateFormat("M/d"));
        dmParsers.put(3, dmParserSet3);
        Set<DateFormat> dmParserSet4 = new HashSet<DateFormat>();
        dmParserSet4.add(new SimpleDateFormat("M/dd"));
        dmParserSet4.add(new SimpleDateFormat("MM/d"));
        dmParsers.put(4, dmParserSet4);
        Set<DateFormat> dmParserSet5 = new HashSet<DateFormat>();
        dmParserSet5.add(new SimpleDateFormat("MM/dd"));
        dmParsers.put(5, dmParserSet5);
        // preparing the time parsers
        timeParsers = new HashMap<Integer, Set<DateFormat>>();
        Set<DateFormat> tmParserSet3 = new HashSet<DateFormat>();
        tmParserSet3.add(new SimpleDateFormat("ha"));
        tmParserSet3.add(new SimpleDateFormat("Hmm"));
        timeParsers.put(3, tmParserSet3);
        Set<DateFormat> tmParserSet4 = new HashSet<DateFormat>();
        tmParserSet4.add(new SimpleDateFormat("HHmm"));
        timeParsers.put(4, tmParserSet4);
    }
    
    public static DataUtils getInstance() {
        if(du == null) {
            du = new DataUtils();
        }
        return du;
    }
    
    /**
     * Formats a Date for storage in the database.
     * @param date the Date
     * @return the String
     */
    public String formatDateTimeForDB(Date date) {
        return dbFormatter.format(date);
    }
    
    /**
     * Parses a String into a Date.
     * If given only a time, assumes the current day. If given only a day,
     * assumes midnight.
     * @param str a string
     * @return the date, or null if the string could not be parsed
     */
    public Date parseDateTime(String str) {
        if("now".equals(str)) {
            return new Date();
        }
        Date dayRes = null;
        Date timeRes = null;
        String[] spl = str.split(" ");
        // parsing the string
        if(spl.length == 1) {
            // one token
            timeRes = getDateTimeFromStr(spl[0]);
            if(timeRes == null) {
                dayRes = getDateDayFromStr(spl[0]);
                Calendar tempCal = Calendar.getInstance();
                tempCal.set(0, 0, 0, 0, 0, 0);
                timeRes = tempCal.getTime();
            } else {
                dayRes = new Date();
            }
        } else if(spl.length == 2) {
            // two tokens
            dayRes = getDateDayFromStr(spl[0]);
            if(dayRes == null) {
                timeRes = getDateTimeFromStr(spl[0]);
                dayRes = getDateDayFromStr(spl[1]);
            } else {
                timeRes = getDateTimeFromStr(spl[1]);
            }
        } else {
            return null;
        }
        // checking if parsing was successful
        if((dayRes == null) || (timeRes == null)) {
            return null;
        }
        // combining day and time results
        Calendar dayCal = Calendar.getInstance();
        dayCal.setTime(dayRes);
        Calendar timeCal = Calendar.getInstance();
        timeCal.setTime(timeRes);
        dayCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
        dayCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
        dayCal.set(Calendar.SECOND, timeCal.get(Calendar.SECOND));
        dayCal.set(Calendar.MILLISECOND, 0);
        return dayCal.getTime();
    }
    
    /**
     * Parses a string for day information.
     * @param str a string
     * @return the date, or null if the string could not be parsed
     */
    private Date getDateDayFromStr(String str) {
        Calendar now = Calendar.getInstance();
        Date resDate;
        Calendar resCal = Calendar.getInstance();
        // checking special values
        if("today".equals(str)) {
            return now.getTime();
        } else if("yesterday".equals(str)) {
            now.add(Calendar.DATE, -1);
            return now.getTime();
        } else if("tomorrow".equals(str) || "tmw".equals(str)) {
            now.add(Calendar.DATE, 1);
            return now.getTime();
        }
        // checking days of the week
        if(str.length() == 3) {
            resDate = dowParserAbbr.parse(str, new ParsePosition(0));
        } else {
            resDate = dowParserFull.parse(str, new ParsePosition(0));
        }
        if(resDate != null) {
            resCal.setTime(resDate);
            int dowDiff = resCal.get(Calendar.DAY_OF_WEEK) -
                    now.get(Calendar.DAY_OF_WEEK);
            now.add(Calendar.DATE, (dowDiff + 7) % 7);
            return now.getTime();
        }
        // checking days of months
        for(DateFormat df : dmParsers.get(str.length())) {
            resDate = df.parse(str, new ParsePosition(0));
            if(resDate != null) {
                Calendar tempCal = Calendar.getInstance();
                int year = tempCal.get(Calendar.YEAR);
                tempCal.setTime(resDate);
                tempCal.set(Calendar.YEAR, year);
                return tempCal.getTime();
            }
        }
        return null;
    }
    
    /**
     * Parses a string for time information.
     * @param str a string
     * @return the date, or null if the string could not be parsed
     */
    private Date getDateTimeFromStr(String str) {
        Date resDate;
        Set<DateFormat> dfSet = timeParsers.get(str.length());
        if(dfSet == null) {
            return null;
        }
        for(DateFormat df : dfSet) {
            resDate = df.parse(str, new ParsePosition(0));
            if(resDate != null) {
                return resDate;
            }
        }
        return null;
    }
    
}
