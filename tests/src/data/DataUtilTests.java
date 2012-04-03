package data;

import java.util.Locale;
import java.util.TimeZone;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import yoonsung.odk.spreadsheet.data.DataUtil;
import junit.framework.TestCase;


public class DataUtilTests extends TestCase {
    
    private Locale locale;
    private TimeZone tz;
    private DataUtil du;
    private DateTimeFormatter dtBuilder;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        locale = Locale.ENGLISH;
        tz = TimeZone.getDefault();
        du = new DataUtil(locale, tz);
        dtBuilder = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss");
    }
    
    public void testParseIntervals() {
        String[] inputs = {
                "10/11/10 4:05",
                "10/11/10 04:59",
                "10/11/10 4:05pm",
                "10/11/2010 4",
                "10/11/2010 4pm",
                "10/11/10",
                "10/11/2010"
        };
        String[] expected = {
                "11/10/2010 04:05:00", "11/10/2010 04:06:00",
                "11/10/2010 04:59:00", "11/10/2010 05:00:00",
                "11/10/2010 16:05:00", "11/10/2010 16:06:00",
                "11/10/2010 04:00:00", "11/10/2010 05:00:00",
                "11/10/2010 16:00:00", "11/10/2010 17:00:00",
                "11/10/2010 00:00:00", "12/10/2010 00:00:00",
                "11/10/2010 00:00:00", "12/10/2010 00:00:00"
        };
        for (int i = 0; i < inputs.length; i++) {
            Interval interval = du.tryParseInterval(inputs[i]);
            assertNotNull(interval);
            assertEquals(dtBuilder.parseDateTime(expected[i * 2]),
                    interval.getStart());
            assertEquals(dtBuilder.parseDateTime(expected[(i * 2) + 1]),
                    interval.getEnd());
        }
    }
    
    public void testParseRelativeDays() {
        // testing "today"
        DateTime today = new DateMidnight().toDateTime();
        Interval todayInterval = du.tryParseInterval("today");
        assertNotNull(todayInterval);
        assertEquals(today, todayInterval.getStart());
        assertEquals(today.plusDays(1), todayInterval.getEnd());
        // testing "tomorrow"
        DateTime tomorrow = today.plusDays(1);
        Interval tomorrowInterval = du.tryParseInterval("tomorrow");
        assertNotNull(tomorrowInterval);
        assertEquals(tomorrow, tomorrowInterval.getStart());
        assertEquals(tomorrow.plusDays(1), tomorrowInterval.getEnd());
        // testing "tmw"
        Interval tmwInterval = du.tryParseInterval("tmw");
        assertNotNull(tmwInterval);
        assertEquals(tomorrow, tmwInterval.getStart());
        assertEquals(tomorrow.plusDays(1), tmwInterval.getEnd());
        // testing "yesterday"
        DateTime yesterday = today.minusDays(1);
        Interval yesterdayInterval = du.tryParseInterval("yesterday");
        assertNotNull(yesterdayInterval);
        assertEquals(yesterday, yesterdayInterval.getStart());
        assertEquals(yesterday.plusDays(1), yesterdayInterval.getEnd());
    }
    
    public void testParseInvalidIntervals() {
        String[] inputs = {
                "apple",
                "10/11/10 25:05",
                "10/11/10 23:05 lookasquirrel"
        };
        for (String input : inputs) {
            assertNull(du.tryParseInterval(input));
        }
    }
}
