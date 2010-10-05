package com.yoonsung.spreadsheetsms;

import java.util.HashMap;

public class Parser {
	
	public Parser() {
		
	}
	
	public HashMap<String, String> parseMapColValue(String sms) {
		HashMap<String, String> result = new HashMap<String, String>();
		String[] tokens = sms.split(" ");
		
		int index = 0;
		while(index < tokens.length && tokens[index].startsWith("+")) { 
			String key = tokens[index];
			String val = "";
			int next = index + 1;
			while (next < tokens.length && !tokens[next].startsWith("+")) {
				val += tokens[next].trim() + " ";
				next++;
			}
			result.put(key.substring(1, key.length()), val);
			index = next;
		}
		return result;
	}
}
