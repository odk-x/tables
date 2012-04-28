package org.opendatakit.tables.Library.graphs;

import java.util.Calendar;
import java.util.Date;

public class GEventPoint implements Comparable<GEventPoint> {
	
	private Calendar start;
	private Calendar end;
	private String name;
	private String desc;
	
	public GEventPoint(Date start, Date end, String name, String desc) {
		this.start = Calendar.getInstance();
		this.start.setTime(start);
		this.end = Calendar.getInstance();
		this.end.setTime(end);
		this.name = name;
		this.desc = desc;
	}
	
	protected String getName() {
		return name;
	}
	
	protected String getDesc() {
		return desc;
	}
	
	protected boolean overlap(GEventPoint other) {
		if(start.before(other.start) && end.after(other.start)) {
			return true;
		} else if(start.before(other.end) && end.after(other.end)) {
			return true;
		} else if(start.after(other.start) && end.before(other.end)) {
			return true;
		} else {
			return (start.equals(other.start) || end.equals(other.end));
		}
	}
	
	protected boolean startBefore(GEventPoint other) {
		return start.before(other.start);
	}
	
	protected boolean endAfter(GEventPoint other) {
		return end.after(other.end);
	}
	
	protected int getStartHour() {
		return start.get(Calendar.HOUR_OF_DAY);
	}
	
	protected int getStartMinute() {
		return start.get(Calendar.MINUTE);
	}
	
	protected int getEndHour() {
		return end.get(Calendar.HOUR_OF_DAY);
	}
	
	protected int getEndMinute() {
		return end.get(Calendar.MINUTE);
	}
	
	public int compareTo(GEventPoint other) {
		if(start.before(other.start)) {
			return -1;
		} else if(start.after(other.start)) {
			return 1;
		} else if(name.compareTo(other.name) != 0) {
			return name.compareTo(other.name);
		} else {
			return desc.compareTo(other.desc);
		}
	}
	
}
