package yoonsung.odk.spreadsheet.SMS;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import yoonsung.odk.spreadsheet.Database.ColumnProperty;
import yoonsung.odk.spreadsheet.Database.Data;
import yoonsung.odk.spreadsheet.Database.DefaultsManager;
import android.util.Log;

public class SMSConverter {
	
	private static String[] ineqOperators = {"<", ">"};
	private static String[] dow1 = {"sun", "mon", "tue", "wed", "thu", "fri",
									"sat"};
	private static String[] dow2 = {"sunday", "monday", "tuesday", "wednesday",
									"thursday", "friday", "saturday"};
	
	private List<String> ineqList;
	private Data data;
	private ColumnProperty cp;
	private DefaultsManager dm;
	private DateFormat dbDate = new SimpleDateFormat("yyyy:MM:dd");
	private DateFormat dbDateTime =
			new SimpleDateFormat("yyyy:MM:dd:HH:mm:ss");
	private DateFormat rCompact = new SimpleDateFormat("MM/dd HH:mm");
	
	public SMSConverter() {
		cp = new ColumnProperty();
		data = new Data();
		ineqList = Arrays.asList(ineqOperators);
		dm = new DefaultsManager();
	}
		
	public HashMap<String, String> parseSMS(String sms) {
		// <Column Name, Value>
		HashMap<String, String> result = new HashMap<String, String>();
		
		// Split the message
		String[] tokens = sms.split(" ");
		
		// Parse into column names and values
		int index = 0;
		while(index < tokens.length && tokens[index].startsWith("+")) { 
			int keyIndex = index;
			String key = tokens[keyIndex];
			String val = "";
			int next = index + 1;
			while (next < tokens.length 
					&& ( tokens[keyIndex+1].startsWith("+") 
							|| !tokens[next].startsWith("+"))
			) {
				val += tokens[next].trim() + " ";
				next++;
			}
			result.put(key.substring(1, key.length()), val);
			index = next;
		}
		
		Log.e("hash", result.toString());
		
		return result;
	}
	
	/**
	 * Gets a response to a submitted query
	 * @param q the query, in the format:
	 *        [@[Table Name]] <[?[Col Name]]> /
	 *        <[=[Col Name] [[Col Value] OR [[Operator] [Col Value]]]]> /
	 *        <[~[Col Name] <T/B>[Return Number]]>
	 * @return the response
	 * @throws InvalidQueryException if the query is invalid
	 */
	public String getQueryResponse(String q) throws InvalidQueryException {
		// preparing variables for query information
		String sheetname; // the name of the sheet
		Set<String> colReqs = new HashSet<String>(); // the columns requested
		List<String> consKeys = new ArrayList<String>(); // the constraint keys
		List<String> consComp = new ArrayList<String>(); // the constraint comparators
		List<String> consVals = new ArrayList<String>(); // the constraint values
		String orderby = null; // the column to order by
		int asc = 0;
		int limit = -1; // the maximum number of rows to return
		String avail = dm.getQueryAvailCol();
		// getting split points
		List<Integer> splits = new ArrayList<Integer>();
		int index = q.indexOf('?');
		while(index > -1) {
			splits.add(index);
			index = q.indexOf('?', index + 1);
		}
		index = q.indexOf('=');
		while(index > -1) {
			splits.add(index);
			index = q.indexOf('=', index + 1);
		}
		index = q.indexOf('~');
		if(index > -1) {splits.add(index);}
		index = q.indexOf('/');
		if(index > -1) {splits.add(index);}
		splits.add(q.length());
		// parsing
		sheetname = interpretSheetname(q.substring(0, splits.get(0)));
		int duration = 1;
		for(int i=0; i<(splits.size() - 1); i++) {
			int start = splits.get(i);
			char type = q.charAt(start);
			String str = q.substring(start + 1, splits.get(i + 1)).trim();
			if(type == '?') {
				colReqs.add(str);
			} else if(type == '=') {
				interpretConstraint(str, consKeys, consComp, consVals);
			} else if(type == '~') {
				String[] oSpl = str.split(" ");
				orderby = oSpl[0];
				String lim = oSpl[1];
				if(lim.startsWith("T") || lim.startsWith("t")) {
					asc = 1;
				} else if(lim.startsWith("B") || lim.startsWith("b")) {
					asc = 2;
				} else {
					throw new InvalidQueryException(
							"invalid limit specification");
				}
				try {
					limit = new Integer(lim.substring(1));
				} catch(NumberFormatException e) {
					throw new InvalidQueryException(
							"invalid limit specification");
				}
			} else if(type == '/') {
				String[] sSpl = str.split(" ");
				if(!sSpl[0].equals(avail)) {
					throw new InvalidQueryException(
							"invalid duration specification");
				}
				duration = intvlStrToSec(sSpl[1]);
			}
		}
		// adding the default columns to include
		for(String col : dm.getQueryIncCols()) {
			colReqs.add(col);
		}
		// adding the default constraints
		Map<String, String> defCons = dm.getQueryColVals();
		for(String col : defCons.keySet()) {
			String val = defCons.get(col);
			if((val.length() != 0) && (!consKeys.contains(col))) {
				String[] spl = val.split("=");
				for(String str : spl) {
					interpretConstraint((col + " " + str.trim()), consKeys,
							consComp, consVals);
				}
			}
		}
		// getting the results and response
		if(avail == null) {
			Set<Map<String, String>> res = data.querySheet(consKeys, consComp,
					consVals, colReqs.toArray(new String[0]), orderby, asc,
					limit);
			return getResponse(res);
		} else {
			colReqs.add(avail);
			Calendar start = Calendar.getInstance();
			start.set(start.getMinimum(Calendar.YEAR), 1, 1);
			Calendar end = Calendar.getInstance();
			end.set(end.getMaximum(Calendar.YEAR), 1, 1);
			for(int i=0; i<consKeys.size(); i++) {
				if(consKeys.get(i).equals(avail)) {
					Calendar next = strToCal(consVals.get(i));
					if(consComp.get(i).startsWith(">") &&
							(start.compareTo(next) < 0)) {
						start = next;
					} else if(consComp.get(i).startsWith("<") &&
							(end.compareTo(next) > 0)) {
						end = next;
					}
				}
			}
			Set<Map<String, String>> res = data.querySheet(consKeys, consComp,
					consVals, colReqs.toArray(new String[0]), orderby, asc,
					-2);
			return getAvailResponse(res, avail, start, end, duration, limit);
		}
	}
	
	private String interpretSheetname(String input) throws InvalidQueryException {
		if(!input.startsWith("@")) {
			throw new InvalidQueryException("no sheet name specified");
		}
		return input.substring(1);
	}
	
	private void interpretConstraint(String input, List<String> consKeys,
			List<String> consComp, List<String> consVals)
			throws InvalidQueryException {
		String[] split = input.split(" ");
		String key = split[0];
		if(!data.isColumnExist(key)) {
			key = cp.getNameByAbrv(key);
			if(key == null) {
				throw new InvalidQueryException("no such column");
			}
		}
		String comp;
		String val = "";
		int valStart = 1;
		if(ineqList.contains(split[1])) {
			valStart = 2;
			comp = split[1];
		} else {
			comp = "=";
		}
		for(int i=valStart; i<split.length; i++) {
			val += " " + split[i];
		}
		if(val.length() > 0) {
			val = val.substring(1);
		}
		if(("Date Range").equals(cp.getType(key))) {
			interpretDRVal(key, comp, val, consKeys, consComp, consVals);
		} else if(key.equals("_timestamp")) {
			interpretTimeVal(key, comp, val, consKeys, consComp, consVals);
		} else {
			consKeys.add(key);
			consComp.add(comp);
			consVals.add(val);
		}
	}
	
	private void interpretTimeVal(String key, String comp, String input,
			List<String> consKeys, List<String> consComp,
			List<String> consVals)
			throws InvalidQueryException {
		String[] split = input.split(" ");
		Calendar cal = getTimeCal(split);
		consKeys.add(key);
		if(cal == null) {
			consComp.add(comp);
			consVals.add(split[0]);
		} else if(split[0].equals("now")) {
			consComp.add(comp);
			consVals.add(calToTimeStr(cal, true, 1));
		} else if(comp.equals("=")) {
			String val = calToTimeStr(cal, false, 1);
			consKeys.add(key);
			consComp.add(">");
			consComp.add("<");
			consVals.add(val + ":00:00:00");
			consVals.add(val + ":23:59:59");
		} else if(comp.equals("<")) {
			String val = calToTimeStr(cal, false, 1);
			consComp.add(comp);
			consVals.add(val + ":00:00:00");
		} else if(comp.equals(">")) {
			String val = calToTimeStr(cal, false, 1);
			consComp.add(comp);
			consVals.add(val + ":23:59:59");
		} else {
			throw new InvalidQueryException("invalid comparison");
		}
	}
	
	private void interpretDRVal(String key, String comp, String input,
			List<String> consKeys, List<String> consComp,
			List<String> consVals)
			throws InvalidQueryException {
		String[] split = input.split(" ");
		Calendar cal = getTimeCal(split);
		consKeys.add(key);
		if(cal == null) {
			consComp.add(comp);
			consVals.add(split[0]);
		} else if(split[0].equals("now")) {
			consComp.add(comp);
			consVals.add(calToTimeStr(cal, true, 2));
		} else if(comp.equals("=")) {
			String val = calToTimeStr(cal, false, 2);
			consKeys.add(key);
			consComp.add(">=");
			consComp.add("<=");
			consVals.add(val + ":00:00:00");
			consVals.add(val + ":23:59:59");
		} else if(comp.equals("<")) {
			String val = calToTimeStr(cal, false, 2);
			consComp.add(comp);
			consVals.add(val + ":00:00:00");
		} else if(comp.equals(">")) {
			String val = calToTimeStr(cal, false, 2);
			consComp.add(comp);
			consVals.add(val + ":23:59:59");
		} else {
			throw new InvalidQueryException("invalid comparison");
		}
	}
	
	private Calendar getTimeCal(String[] split) throws InvalidQueryException {
		Calendar cal = Calendar.getInstance();
		String val = split[0];
		boolean found = false;
		if(val.equals("now")) {
			found = true;
			if(split.length > 1) {
				cal.add(Calendar.SECOND, getSecondAdd(split[1], split[2]));
			}
		} else if(val.equals("today")) {
			found = true;
		} else {
			int dow = -1;
			int i = 0;
			while(!found && (i<7)) {
				if(dow1[i].equalsIgnoreCase(val) ||
						dow2[i].equalsIgnoreCase(val)) {
					dow = i + 1;
					found = true;
				}
				i++;
			}
			if(found) {
				int curDow = cal.get(Calendar.DAY_OF_WEEK);
				if(dow <= curDow) {
					dow += 7;
				}
				cal.add(Calendar.DATE, dow - curDow);
			}
		}
		if(found) {
			return cal;
		} else {
			return null;
		}
	}
	
	private int getSecondAdd(String oper, String val)
			throws InvalidQueryException {
		int secs;
		char unit = val.charAt(val.length() - 1);
		Integer quant;
		try {
			quant = new Integer(val.substring(0, val.length() - 1));
		} catch(NumberFormatException e) {
			throw new InvalidQueryException("invalid number format");
		}
		if((unit == 'd') || (unit == 'D')) {
			secs = 24 * 60 * 60 * quant;
		} else if((unit == 'h') || (unit == 'H')) {
			secs = 60 * 60 * quant;
		} else if((unit == 'm') || (unit == 'M')) {
			secs = 60 * quant;
		} else {
			secs = 0;
		}
		if(oper.equals("-")) {
			secs = 0 - secs;
		}
		return secs;
	}
	
	private String calToTimeStr(Calendar cal, boolean incTime, int type) {
		String str;
		if(type == 1) {
			str = ((cal.get(Calendar.MONTH) + 1) + "/" +
					cal.get(Calendar.DAY_OF_MONTH) + "/" +
					cal.get(Calendar.YEAR));
		} else {
			str = (cal.get(Calendar.YEAR) + ":" +
					String.format("%02d", (cal.get(Calendar.MONTH) + 1)) + ":" +
					String.format("%02d", cal.get(Calendar.DAY_OF_MONTH)));
		}
		if(incTime && (type == 1)) {
			str += (" " + String.format("%02d", cal.get(Calendar.HOUR_OF_DAY)) +
					":" + String.format("%02d", cal.get(Calendar.MINUTE)) +
					":" + String.format("%02d", cal.get(Calendar.SECOND)));
		} else if(incTime && (type == 2)) {
			str += (":" + String.format("%02d", cal.get(Calendar.HOUR_OF_DAY)) +
					":" + String.format("%02d", cal.get(Calendar.MINUTE)) +
					":" + String.format("%02d", cal.get(Calendar.SECOND)));
		}
		return str;
	}
	
	/**
	 * Gets a response for a regular query
	 * @param res a set of mappings from keys to values
	 * @return the response
	 */
	private String getResponse(Set<Map<String, String>> res) {
		String r = "";
		for(Map<String, String> map : res) {
			String next = "";
			for(String key : map.keySet()) {
				next += "," + key + ":" + map.get(key);
			}
			r += "/" + next.substring(1);
		}
		if(r.length() > 0) {
			return r.substring(1);
		} else {
			return "no data found";
		}
	}
	
	/**
	 * Gets a response for an availability query
	 * @param res a set of mappings from keys to values
	 * @param gav the column to ensure availability with
	 * @param start the earliest time
	 * @param end the latest time
	 * @return the response
	 */
	private String getAvailResponse(Set<Map<String, String>> res, String gav,
			Calendar start, Calendar end, int duration, int limit) {
		if(limit < 0) {limit = 5;}
		duration *= 1000;
		Set<String> tree = new TreeSet<String>();
		for(Map<String, String> map : res) {
			tree.add(map.get(gav));
		}
		List<Calendar> startTimes = new ArrayList<Calendar>();
		List<Calendar> endTimes = new ArrayList<Calendar>();
		for(String str : tree) {
			String[] spl = str.split("/");
			startTimes.add(strToCal(spl[0]));
			endTimes.add(strToCal(spl[1]));
		}
		List<Calendar[]> gaps = new ArrayList<Calendar[]>();
		if(!startTimes.isEmpty()) {
			Calendar[] firstTime = {start, startTimes.get(0)};
			gaps.add(firstTime);
		}
		for(int i=0; i<(startTimes.size() - 1); i++) {
			Calendar[] nextTime = {endTimes.get(i), startTimes.get(i+1)};
			gaps.add(nextTime);
		}
		if(!startTimes.isEmpty()) {
			Calendar[] lastTime = {endTimes.get(endTimes.size()-1), end};
			gaps.add(lastTime);
		}
		String r = "";
		int i = 0;
		int count = 0;
		while((count < limit) && (i < gaps.size())) {
			Calendar[] gap = gaps.get(i);
			long gapTime = gap[1].getTimeInMillis() - gap[0].getTimeInMillis();
			if(gapTime >= duration) {
				r += "; " + intvlToStr(gap);
				count++;
			}
			i++;
		}
		if(r.length() > 0) {
			return r.substring(2);
		} else {
			return "nothing available";
		}
	}
	
	private String intvlToStr(Calendar[] intvl) {
		String start = rCompact.format(intvl[0].getTime());
		String end = rCompact.format(intvl[1].getTime());
		if(start.substring(0, 5).equals(end.substring(0, 5))) {
			end = end.substring(6);
		}
		if(intvl[1].get(Calendar.YEAR) == intvl[1].getMaximum(Calendar.YEAR)) {
			return (start + " and on");
		} else {
			return (start + " to " + end);
		}
	}
	
	/**
	 * Gets a calendar for the string given
	 * @param str the string (formatted as yyyy:mm:dd:hh:mm:ss)
	 * @return a calendar for the time
	 */
	private Calendar strToCal(String str) {
		Calendar cal = Calendar.getInstance();
		String[] spl = str.split(":");
		cal.set(new Integer(spl[0]), (new Integer(spl[1]) - 1),
				new Integer(spl[2]), new Integer(spl[3]), new Integer(spl[4]),
				new Integer(spl[5]));
		return cal;
	}
	
	/**
	 * Gets the number of seconds represented by the interval string
	 * @param intvl the interval string (e.g. 30m)
	 * @return the number of seconds
	 * @throws InvalidQueryException if the interval is not properly formatted
	 */
	private int intvlStrToSec(String intvl) throws InvalidQueryException {
		char unit = intvl.charAt(intvl.length() - 1);
		String quant = intvl.substring(0, intvl.length() - 1);
		try {
			if((unit == 'm') || (unit == 'M')) {
				return (60 * new Integer(quant));
			} else if((unit == 'h') || (unit == 'H')) {
				return (3600 * new Integer(quant));
			} else {
				throw new InvalidQueryException("invalid duration");
			}
		} catch(NumberFormatException e) {
			throw new InvalidQueryException("invalid duration");
		}
	}
	
}
