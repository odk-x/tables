/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.data;

import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.opendatakit.tables.Activity.util.UTMConverter;


public class DataUtil {
    
    private static final DateTimeFormatter DB_DATETIME_FORMATTER =
        DateTimeFormat.forPattern("yyyy-MM-dd-HH-mm-ss").withZoneUTC();
    
    private static final String[] USER_FULL_DATETIME_PATTERNS = {
        "M/d/yy h:mm:ssa",
        "M/d/yy HH:mm:ss",
        "M/d/yyyy h:mm:ssa",
        "M/d/yyyy HH:mm:ss",
        "M/d h:mm:ssa",
        "M/d HH:mm:ss",
        "d h:mm:ssa",
        "d HH:mm:ss",
        "E h:mm:ssa",
        "E HH:mm:ss"
    };
    private static final String[][] USER_PARTIAL_DATETIME_PATTERNS = {
        {
            // minute
            "M/d/yy h:mma",
            "M/d/yy HH:mm",
            "M/d/yyyy h:mma",
            "M/d/yyyy HH:mm",
            "M/d h:mma",
            "M/d HH:mm",
            "d h:mma",
            "d HH:mm",
            "E h:mma",
            "E HH:mm"
        },
        {
            // hour
            "M/d/yy ha",
            "M/d/yy HH",
            "M/d/yyyy ha",
            "M/d/yyyy HH",
            "M/d ha",
            "M/d HH",
            "d ha",
            "d HH",
            "E ha",
            "E HH"
        },
        {
            // day
            "M/d/yy",
            "M/d/yyyy",
            "M/d",
            "d",
            "E"
        }
    };
    private static final int[] USER_INTERVAL_DURATIONS = {60, 3600, 86400};
    
    private static final Pattern USER_DURATION_FORMAT =
        Pattern.compile("(\\d+)(s|m|h|d)");
    
    private static final Pattern USER_NOW_RELATIVE_FORMAT =
        Pattern.compile("now\\s*(-|\\+)\\s*(\\d+S)");
    
    private static final Pattern USER_LOCATION_LAT_LON_FORMAT =
        Pattern.compile("\\(?(-?(\\d+|\\.\\d+|\\d+\\.\\d+))," +
                "\\s*(-?(\\d+|\\.\\d+|\\d+\\.\\d+))\\)?");
    private static final Pattern USER_LOCATION_UTM_COMMA_FORMAT =
        Pattern.compile("(-?(\\d+|\\.\\d+|\\d+\\.\\d+)),\\s*" +
                "(-?(\\d+|\\.\\d+|\\d+\\.\\d+)),\\s*(\\d+),\\s*(n|N|s|S)");
    private static final Pattern USER_LOCATION_UTM_SPACE_FORMAT =
        Pattern.compile("(\\d+)(n|N|s|S)\\s+(-?(\\d+|\\.\\d+|\\d+\\.\\d+))" +
                "\\s+(-?(\\d+|\\.\\d+|\\d+\\.\\d+))");
    
    private static final String USER_SHORT_FORMAT = "M/d h:mma";
    private static final String USER_LONG_FORMAT = "M/d/yyyy h:mm:ssa";
    
    private static DataUtil du;
    
    private final Locale locale;
    private final DateTimeZone tz;
    private final DateTimeFormatter userFullParser;
    private final DateTimeFormatter[] userPartialParsers;
    private final DateTimeFormatter userShortFormatter;
    private final DateTimeFormatter userLongFormatter;
    
    public static DataUtil getDefaultDataUtil() {
        if (du == null) {
            du = new DataUtil();
        }
        return du;
    }
    
    private DataUtil() {
        this(Locale.ENGLISH, TimeZone.getDefault());
    }
    
    public DataUtil(Locale locale, TimeZone tz) {
        this.locale = locale;
        this.tz = DateTimeZone.forTimeZone(tz);
        DateTimeFormatterBuilder fpBuilder = new DateTimeFormatterBuilder();
        for (String pattern : USER_FULL_DATETIME_PATTERNS) {
            DateTimeFormatter f = DateTimeFormat.forPattern(pattern);
            fpBuilder.appendOptional(f.getParser());
        }
        userFullParser = fpBuilder.toFormatter()
                .withLocale(locale).withZone(this.tz);
        userPartialParsers =
            new DateTimeFormatter[USER_PARTIAL_DATETIME_PATTERNS.length];
        for (int i = 0; i < USER_PARTIAL_DATETIME_PATTERNS.length; i++) {
            DateTimeFormatterBuilder dtfb = new DateTimeFormatterBuilder();
            for (String pattern : USER_PARTIAL_DATETIME_PATTERNS[i]) {
                DateTimeFormatter f = DateTimeFormat.forPattern(pattern);
                dtfb.appendOptional(f.getParser());
            }
            userPartialParsers[i] = dtfb.toFormatter()
                    .withLocale(locale).withZone(this.tz);
        }
        userShortFormatter = DateTimeFormat.forPattern(USER_SHORT_FORMAT);
        userLongFormatter = DateTimeFormat.forPattern(USER_LONG_FORMAT);
    }
    
    public String validifyValue(ColumnProperties cp, String input) {
        switch (cp.getColumnType()) {
        case ColumnProperties.ColumnType.DATE:
            return validifyDateValue(input);
        case ColumnProperties.ColumnType.DATE_RANGE:
            return validifyDateRangeValue(input);
        case ColumnProperties.ColumnType.NUMBER:
            return validifyNumberValue(input);
        case ColumnProperties.ColumnType.MC_OPTIONS:
            return validifyMultipleChoiceValue(cp, input);
        case ColumnProperties.ColumnType.LOCATION:
            return validifyLocationValue(input);
        default:
            return input;
        }
    }
    
    private String validifyDateValue(String input) {
        DateTime instant = tryParseInstant(input);
        if (instant != null) {
            return formatDateTimeForDb(instant);
        }
        Interval interval = tryParseInterval(input);
        if (interval != null) {
            return formatDateTimeForDb(interval.getStart());
        }
        return null;
    }
    
    private String validifyDateRangeValue(String input) {
        Interval interval = tryParseInterval(input);
        if (interval != null) {
            return formatIntervalForDb(interval);
        }
        return null;
    }
    
    private String validifyNumberValue(String input) {
        try {
            Double.parseDouble(input);
            return input;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private String validifyMultipleChoiceValue(ColumnProperties cp,
            String input) {
        for (String opt :cp.getMultipleChoiceOptions()) {
            if (opt.equalsIgnoreCase(input)) {
                return opt;
            }
        }
        return null;
    }
    
    private String validifyLocationValue(String input) {
        Matcher matcher = USER_LOCATION_LAT_LON_FORMAT.matcher(input);
        if (matcher.matches()) {
            return matcher.group(1) + "," + matcher.group(3);
        }
        matcher = USER_LOCATION_UTM_COMMA_FORMAT.matcher(input);
        if (matcher.matches()) {
            double x = Double.parseDouble(matcher.group(1));
            double y = Double.parseDouble(matcher.group(3));
            int zone = Integer.parseInt(matcher.group(5));
            String hemi = matcher.group(6);
            boolean isSouthHemi = hemi.equalsIgnoreCase("s");
            double[] latLon = UTMConverter.parseUTM(x, y, zone, isSouthHemi);
            if (latLon == null) {
                return null;
            }
            String latStr = String.format("%.5g", latLon[0]);
            String lonStr = String.format("%.5g", latLon[1]);
            return latStr + "," + lonStr;
        }
        matcher = USER_LOCATION_UTM_SPACE_FORMAT.matcher(input);
        if (matcher.matches()) {
            double x = Double.parseDouble(matcher.group(3));
            double y = Double.parseDouble(matcher.group(5));
            int zone = Integer.parseInt(matcher.group(1));
            String hemi = matcher.group(2);
            boolean isSouthHemi = hemi.equalsIgnoreCase("s");
            double[] latLon = UTMConverter.parseUTM(x, y, zone, isSouthHemi);
            if (latLon == null) {
                return null;
            }
            String latStr = String.format("%.5g", latLon[0]);
            String lonStr = String.format("%.5g", latLon[1]);
            return latStr + "," + lonStr;
        }
        return null;
    }
    
    public DateTime tryParseInstant(String input) {
        try {
            return userFullParser.parseDateTime(input);
        } catch (IllegalArgumentException e) {}
        if (!locale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
            return null;
        }
        if (input.equalsIgnoreCase("now")) {
            return new DateTime();
        }
        Matcher matcher = USER_NOW_RELATIVE_FORMAT.matcher(input);
        if (!matcher.matches()) {
            return null;
        }
        int delta = tryParseDuration(matcher.group(2));
        if (delta < 0) {
            return null;
        } else if (matcher.group(1).equals("-")) {
            return new DateTime().minusSeconds(delta);
        } else {
            return new DateTime().plusSeconds(delta);
        }
    }
    
    public Interval tryParseInterval(String input) {
        for (int i = 0; i < userPartialParsers.length; i++) {
            try {
                DateTime start = userPartialParsers[i].parseDateTime(input);
                DateTime end = start.plusSeconds(USER_INTERVAL_DURATIONS[i]);
                return new Interval(start, end);
            } catch (IllegalArgumentException e) {}
        }
        if (!locale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
            return null;
        }
        DateTime start = new DateMidnight().toDateTime();
        boolean match = false;
        if (input.equalsIgnoreCase("today")) {
            match = true;
        } else if (input.equalsIgnoreCase("yesterday")) {
            start = start.minusDays(1);
            match = true;
        } else if (input.equalsIgnoreCase("tomorrow") ||
                input.equalsIgnoreCase("tmw")) {
            start = start.plusDays(1);
            match = true;
        }
        if (match) {
            DateTime end = start.plusDays(1);
            return new Interval(start, end);
        }
        return null;
    }
    
    public int tryParseDuration(String input) {
        Matcher matcher = USER_DURATION_FORMAT.matcher(input);
        if (!matcher.matches()) {
            return -1;
        }
        int quant = Integer.parseInt(matcher.group(1));
        char unit = matcher.group(2).charAt(0);
        switch (unit) {
        case 's':
            return quant;
        case 'm':
            return quant * 60;
        case 'h':
            return quant * 3600;
        case 'd':
            return quant * 86400;
        default:
            return -1;
        }
    }
    
    public String formatDateTimeForDb(DateTime dt) {
        return DB_DATETIME_FORMATTER.print(dt);
    }
    
    public String formatIntervalForDb(Interval interval) {
        return formatDateTimeForDb(interval.getStart()) + "/" +
                formatDateTimeForDb(interval.getEnd());
    }
    
    public String formatNowForDb() {
        return formatDateTimeForDb(new DateTime());
    }
    
    public DateTime parseDateTimeFromDb(String dbString) {
        return DB_DATETIME_FORMATTER.parseDateTime(dbString);
    }
    
    public Interval parseIntervalFromDb(String dbString) {
        String[] split = dbString.split("/");
        return new Interval(DB_DATETIME_FORMATTER.parseDateTime(split[0]),
                DB_DATETIME_FORMATTER.parseDateTime(split[1]));
    }
    
    public String formatForUserDisplay(ColumnProperties cp, String value) {
        switch (cp.getColumnType()) {
        case ColumnProperties.ColumnType.DATE:
            return formatLongDateTimeForUser(parseDateTimeFromDb(value));
        case ColumnProperties.ColumnType.DATE_RANGE:
            return formatLongIntervalForUser(parseIntervalFromDb(value));
        default:
            return value;
        }
    }
    
    public String formatShortDateTimeForUser(DateTime dt) {
        return userShortFormatter.print(dt);
    }
    
    public String formatLongDateTimeForUser(DateTime dt) {
        return userLongFormatter.print(dt);
    }
    
    public String formatShortIntervalForUser(Interval interval) {
        return formatShortDateTimeForUser(interval.getStart()) + "-" +
                formatShortDateTimeForUser(interval.getEnd());
    }
    
    public String formatLongIntervalForUser(Interval interval) {
        return formatLongDateTimeForUser(interval.getStart()) + " - " +
                formatLongDateTimeForUser(interval.getEnd());
    }
    
    public double[] parseLocationFromDb(String dbString) {
        String[] split = dbString.split(",");
        return new double[] {Double.parseDouble(split[0]),
                Double.parseDouble(split[1])};
    }
}
