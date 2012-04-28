package org.opendatakit.tables.Library.graphs;

public class GValuePercentilePoint {
	
	private String x;
	private double y;
	private double low;
	private double midlow;
	private double midhigh;
	private double high;
	
	public GValuePercentilePoint(String x, double y, double low, double midlow,
			double midhigh, double high) {
		this.x = x;
		this.y = y;
		this.low = low;
		this.midlow = midlow;
		this.midhigh = midhigh;
		this.high = high;
	}
	
	public String getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public double getLow() {
		return low;
	}
	
	public double getMidlow() {
		return midlow;
	}
	
	public double getMidhigh() {
		return midhigh;
	}
	
	public double getHigh() {
		return high;
	}
	
}
