package yoonsung.odk.spreadsheet.Library.graphs;

import java.util.List;

import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import android.app.Activity;
import android.graphics.Color;

public class GraphFactory {
	
	private Activity a;
	
	public GraphFactory(Activity a) {
		this.a = a;
	}
	
	/**
	 * Constructs a new EGraphicalView with a line chart. X and Y values must
	 * be numbers.
	 * @param data a list of data points (x,y)
	 * @param title the title of the chart
	 * @param xTitle the x-axis title
	 * @param yTitle the y-axis title
	 * @return a view with the chart
	 */
	public EGraphicalView getXYLineGraph(List<GXYPoint> data,
			String title, String xTitle, String yTitle) {
		XYMultipleSeriesRenderer r = getRenderer(title, xTitle, yTitle);
    	XYSeriesRenderer seriesRenderer = new XYSeriesRenderer();
    	seriesRenderer.setColor(Color.BLUE);
    	r.addSeriesRenderer(seriesRenderer);
		XYSeries series = new XYSeries(yTitle);
		for(GXYPoint datum : data) {
			series.add(datum.getX(), datum.getY());
		}
    	XYMultipleSeriesDataset d = new XYMultipleSeriesDataset();
    	d.addSeries(series);
    	EXYChart chart = new ELineChart(d, r);
    	return new EGraphicalView(a, chart);
	}
	
	/**
	 * Constructs a new EGraphicalView with a line chart. The X value must be a
	 * string.
	 * @param data a list of data points (value,y)
	 * @param title the title of the chart
	 * @param xTitle the x-axis title
	 * @param yTitle the y-axis title
	 * @return a view with the chart
	 */
	public EGraphicalView getValueYLineGraph(List<GValueYPoint> data,
			String title, String xTitle, String yTitle) {
		XYMultipleSeriesRenderer r = getRenderer(title, xTitle, yTitle);
    	XYSeriesRenderer seriesRenderer = new XYSeriesRenderer();
    	seriesRenderer.setColor(Color.BLUE);
    	r.addSeriesRenderer(seriesRenderer);
		XYSeries series = new XYSeries(yTitle);
		for(int i=0; i<data.size(); i++) {
			GValueYPoint datum = data.get(i);
			series.add(i, datum.getY());
			r.addTextLabel(i, datum.getX());
		}
    	XYMultipleSeriesDataset d = new XYMultipleSeriesDataset();
    	d.addSeries(series);
    	EXYChart chart = new ELineChart(d, r);
    	return new EGraphicalView(a, chart);
	}
	
	/**
	 * Constructs a new EGraphicalView with a scatter chart. X and Y values
	 * must be numbers.
	 * @param data a list of data points (x,y)
	 * @param title the title of the chart
	 * @param xTitle the x-axis title
	 * @param yTitle the y-axis title
	 * @return a view with the chart
	 */
	public EGraphicalView getXYScatterGraph(List<GXYPoint> data,
			String title, String xTitle, String yTitle) {
		XYMultipleSeriesRenderer r = getRenderer(title, xTitle, yTitle);
    	XYSeriesRenderer seriesRenderer = new XYSeriesRenderer();
    	seriesRenderer.setColor(Color.BLUE);
    	seriesRenderer.setPointStyle(PointStyle.CIRCLE);
    	r.addSeriesRenderer(seriesRenderer);
		XYSeries series = new XYSeries(yTitle);
		for(GXYPoint datum : data) {
			series.add(datum.getX(), datum.getY());
		}
    	XYMultipleSeriesDataset d = new XYMultipleSeriesDataset();
    	d.addSeries(series);
    	EXYChart chart = new EScatterChart(d, r);
    	return new EGraphicalView(a, chart);
	}
	
	/**
	 * Constructs a new EGraphicalView with a scatter chart. The X value must
	 * be a string.
	 * @param data a list of data points (value,y)
	 * @param title the title of the chart
	 * @param xTitle the x-axis title
	 * @param yTitle the y-axis title
	 * @return a view with the chart
	 */
	public EGraphicalView getValueYScatterGraph(List<GValueYPoint> data,
			String title, String xTitle, String yTitle) {
		XYMultipleSeriesRenderer r = getRenderer(title, xTitle, yTitle);
    	XYSeriesRenderer seriesRenderer = new XYSeriesRenderer();
    	seriesRenderer.setColor(Color.BLUE);
    	seriesRenderer.setPointStyle(PointStyle.CIRCLE);
    	r.addSeriesRenderer(seriesRenderer);
		XYSeries series = new XYSeries(yTitle);
		for(int i=0; i<data.size(); i++) {
			GValueYPoint datum = data.get(i);
			series.add(i, datum.getY());
			r.addTextLabel(i, datum.getX());
		}
    	XYMultipleSeriesDataset d = new XYMultipleSeriesDataset();
    	d.addSeries(series);
    	EXYChart chart = new EScatterChart(d, r);
    	return new EGraphicalView(a, chart);
	}
	
	/**
	 * Constructs a new EGraphicalView with a box-stem chart.
	 * @param data a list of data points
	 * @param title the title of the chart
	 * @param xTitle the x-axis title
	 * @param yTitle the y-axis title
	 * @return a view with the chart
	 */
	public EGraphicalView getBoxStemGraph(List<GValuePercentilePoint> data,
			String title, String xTitle, String yTitle) {
		XYMultipleSeriesRenderer r = getRenderer(title, xTitle, yTitle);
    	XYSeriesRenderer seriesRenderer = new XYSeriesRenderer();
    	seriesRenderer.setColor(Color.BLUE);
    	r.addSeriesRenderer(seriesRenderer);
		PercentileSeries series = new PercentileSeries(yTitle);
		for(int i=0; i<data.size(); i++) {
			GValuePercentilePoint datum = data.get(i);
			series.add(i, datum.getY(), datum.getLow(), datum.getMidlow(),
					datum.getMidhigh(), datum.getHigh());
			r.addTextLabel(i, datum.getX());
		}
    	PercentileMultDataset d = new PercentileMultDataset();
    	d.addSeries(series);
    	EXYChart chart = new BoxStemChart(d, r);
    	return new EGraphicalView(a, chart);
	}
	
	/**
	 * Gets a renderer.
	 * @param title the chart title
	 * @param xTitle the x-axis title
	 * @param yTitle the y-axis title
	 * @return
	 */
	private XYMultipleSeriesRenderer getRenderer(String title,
			String xTitle, String yTitle) {
    	XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
    	renderer.setBackgroundColor(Color.WHITE);
    	renderer.setApplyBackgroundColor(true);
    	renderer.setShowLegend(false);
    	renderer.setAxesColor(Color.DKGRAY);
    	renderer.setLabelsColor(Color.DKGRAY);
    	renderer.setShowGrid(true);
    	renderer.setChartTitle(title);
    	renderer.setXTitle(xTitle);
    	renderer.setYTitle(yTitle);
    	return renderer;
	}
	
}
