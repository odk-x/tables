package yoonsung.odk.spreadsheet.Library.graphs;

import java.util.ArrayList;
import java.util.List;

import org.achartengine.model.XYSeries;
import org.achartengine.util.MathHelper;

public class PercentileSeries extends XYSeries {
	
	private static final long serialVersionUID = 1L;
	
	private double mYMin;
	private double mYMax;
	private List<Double> mLow = new ArrayList<Double>();
	private List<Double> mMidlow = new ArrayList<Double>();
	private List<Double> mMidhigh = new ArrayList<Double>();
	private List<Double> mHigh = new ArrayList<Double>();
	
	protected PercentileSeries(String title) {
		super(title);
		mYMin = MathHelper.NULL_VALUE;
		mYMax = -MathHelper.NULL_VALUE;
	}
	
	public void add(double x, double y, double low, double midlow,
			double midhigh, double high) {
		super.add(x, y);
		mLow.add(low);
		mMidlow.add(midlow);
		mMidhigh.add(midhigh);
		mHigh.add(high);
		mYMin = Math.min(mYMin, low);
		mYMax = Math.max(mYMax, high);
	}
	
	public double getMinY() {
		return mYMin;
	}
	
	public double getMaxY() {
		return mYMax;
	}
	
	public double getLow(int index) {
		return mLow.get(index);
	}
	
	public double getMidlow(int index) {
		return mMidlow.get(index);
	}
	
	public double getMidhigh(int index) {
		return mMidhigh.get(index);
	}
	
	public double getHigh(int index) {
		return mHigh.get(index);
	}
	
}
