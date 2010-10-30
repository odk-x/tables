package yoonsung.odk.spreadsheet.Library.graphs;

import java.util.ArrayList;
import java.util.List;

import org.achartengine.model.XYMultipleSeriesDataset;

public class PercentileMultDataset extends XYMultipleSeriesDataset {
	
	private static final long serialVersionUID = 1L;
	
	private List<PercentileSeries> mSeries = new ArrayList<PercentileSeries>();
	
	public void addSeries(PercentileSeries series) {
		mSeries.add(series);
	}
	
	public void removeSeries(int index) {
		mSeries.remove(index);
	}

	public void removeSeries(PercentileSeries series) {
		mSeries.remove(series);
	}

	public PercentileSeries getSeriesAt(int index) {
		return mSeries.get(index);
	}

	public int getSeriesCount() {
		return mSeries.size();
	}

	public PercentileSeries[] getSeries() {
		return mSeries.toArray(new PercentileSeries[0]);
	}
	
}
