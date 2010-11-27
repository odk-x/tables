package yoonsung.odk.spreadsheet.SMS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import yoonsung.odk.spreadsheet.Database.ColumnProperty;
import yoonsung.odk.spreadsheet.Database.Data;
import android.util.Log;

public class SMSConverter {
	
	private static String[] ineqOperators = {"<", "<=", ">", ">="};
		
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
		List<String> ineqList = Arrays.asList(ineqOperators);
		String[] spl = q.split(" ");
		if(!spl[0].startsWith("@")) {
			throw new InvalidQueryException("no sheet specified");
		}
		String sheetname = spl[0].substring(1); // the name of the sheet
		Set<String> colReqs = new HashSet<String>(); // the columns requested
		List<String> consKeys = new ArrayList<String>(); // the constraint keys
		List<String> consComp = new ArrayList<String>(); // the constraint comparators
		List<String> consVals = new ArrayList<String>(); // the constraint values
		String orderby = null; // the column to order by
		int asc = 0;
		int limit = -1; // the maximum number of rows to return
		ColumnProperty cp = new ColumnProperty();
		int i = 1;
		while(i < spl.length) {
			String next = spl[i];
			if(next.startsWith("?")) {
				colReqs.add(next.substring(1));
				i += 1;
			} else if(next.startsWith("=")) {
				String key = next.substring(1);
				consKeys.add(key);
				i += 1;
				if(i == spl.length) {throw new InvalidQueryException("invalid argument");}
				next = spl[i];
				String comp;
				if(ineqList.contains(next)) {
					comp = next;
					i += 1;
					if(i == spl.length) {throw new InvalidQueryException("invalid argument");}
					next = spl[i];
				} else {
					comp = "=";
				}
				consComp.add(comp);
				String val = next.trim();
				if(("Date Range").equals(cp.getType(key))) {
					return interpretDRVal(val, comp);
				} else {
					consVals.add(val);
				}
				i += 1;
			} else if(next.startsWith("~")) {
				orderby = next.substring(1);
				i += 1;
				if(i == spl.length) {throw new InvalidQueryException("invalid argument");}
				next = spl[i];
				if(next.startsWith("T") || next.startsWith("t")) {
					asc = 1;
					next = next.substring(1);
				} else if(next.startsWith("B") || next.startsWith("b")) {
					asc = 2;
					next = next.substring(1);
				}
				try {
					limit = new Integer(next.trim());
				} catch(NumberFormatException e) {
					throw new InvalidQueryException("invalid limit");
				}
				i += 1;
			} else {
				throw new InvalidQueryException("invalid argument");
			}
		}
		Data data = new Data();
		// adding default constraints
		Map<String, Boolean> defCons = data.getDefaultCons(sheetname);
		for(String con : defCons.keySet()) {
			String[] conSpl = con.split(" ");
			if(defCons.get(con) || !consKeys.contains(conSpl[0])) {
				consKeys.add(conSpl[0]);
				consComp.add(conSpl[1]);
				consVals.add(conSpl[2]);
			}
		}
		Set<Map<String, String>> res = data.querySheet(consKeys, consComp,
				consVals, colReqs.toArray(new String[0]), orderby, asc, limit);
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
	
	private String interpretDRVal(String val, String comp) {
		return val;
	}
	
}
