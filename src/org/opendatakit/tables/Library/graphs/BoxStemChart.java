package org.opendatakit.tables.Library.graphs;

import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.Log;

public class BoxStemChart extends EXYChart {

	  private static final long serialVersionUID = 1L;
	  /** The point shape size. */
	  private static final float SIZE = 3;
	  /** The legend shape width. */
	  private static final int SHAPE_WIDTH = 10;
	  
	  private static final int STEM_WIDTH = 3;
	  
	  private PercentileMultDataset mDataset;

	  /**
	   * Builds a new scatter chart instance.
	   * 
	   * @param dataset the multiple series dataset
	   * @param renderer the multiple series renderer
	   */
	  protected BoxStemChart(PercentileMultDataset dataset,
			  XYMultipleSeriesRenderer renderer) {
	    super(dataset, renderer);
	    mDataset = dataset;
	  }

	  /**
	   * The graphical representation of a series.
	   * 
	   * @param canvas the canvas to paint to
	   * @param paint the paint to be used for drawing
	   * @param points the array of points to be used for drawing the series
	   * @param seriesRenderer the series renderer
	   * @param yAxisValue the minimum value of the y axis
	   * @param seriesIndex the index of the series currently being drawn
	   */
	  public void drawSeries(Canvas canvas, Paint paint, float[] points,
	      SimpleSeriesRenderer seriesRenderer, float yAxisValue, int seriesIndex) {
	    XYSeriesRenderer renderer = (XYSeriesRenderer) seriesRenderer;
	    PercentileSeries series = mDataset.getSeriesAt(seriesIndex);
	    paint.setColor(renderer.getColor());
	    if (renderer.isFillPoints()) {
	      paint.setStyle(Style.FILL);
	    } else {
	      paint.setStyle(Style.STROKE);
	    }
	    for(int i=0; i<series.getItemCount(); i++) {
	    	double x = series.getX(i);
	    	double low = series.getLow(i);
	    	float[] lowPt = getScreenPt(x, low);
	    	double high = series.getHigh(i);
	    	float[] highPt = getScreenPt(x, high);
	    	double midlow = series.getMidlow(i);
	    	float[] midlowPt = getScreenPt(x, midlow);
	    	double midhigh = series.getMidhigh(i);
	    	float[] midhighPt = getScreenPt(x, midhigh);
	    	canvas.drawLine(lowPt[0] - STEM_WIDTH, lowPt[1], lowPt[0] + STEM_WIDTH, lowPt[1], paint);
	    	canvas.drawLine(highPt[0] - STEM_WIDTH, highPt[1], highPt[0] + STEM_WIDTH, highPt[1], paint);
	    	canvas.drawLine(midlowPt[0], midlowPt[1], lowPt[0], lowPt[1], paint);
	    	canvas.drawLine(midhighPt[0], midhighPt[1], highPt[0], highPt[1], paint);
	    	canvas.drawLine(midlowPt[0] - STEM_WIDTH, midlowPt[1], midlowPt[0] + STEM_WIDTH, midlowPt[1], paint);
	    	canvas.drawLine(midhighPt[0] - STEM_WIDTH, midhighPt[1], midhighPt[0] + STEM_WIDTH, midhighPt[1], paint);
	    	canvas.drawLine(midhighPt[0] - STEM_WIDTH, midhighPt[1], midlowPt[0] - STEM_WIDTH, midlowPt[1], paint);
	    	canvas.drawLine(midhighPt[0] + STEM_WIDTH, midhighPt[1], midlowPt[0] + STEM_WIDTH, midlowPt[1], paint);
	    }
	  }

	  /**
	   * Returns the legend shape width.
	   * 
	   * @return the legend shape width
	   */
	  public int getLegendShapeWidth() {
	    return SHAPE_WIDTH;
	  }

	  /**
	   * The graphical representation of the legend shape.
	   * 
	   * @param canvas the canvas to paint to
	   * @param renderer the series renderer
	   * @param x the x value of the point the shape should be drawn at
	   * @param y the y value of the point the shape should be drawn at
	   * @param paint the paint to be used for drawing
	   */
	  public void drawLegendShape(Canvas canvas, SimpleSeriesRenderer renderer, float x, float y,
	      Paint paint) {
	    if (((XYSeriesRenderer) renderer).isFillPoints()) {
	      paint.setStyle(Style.FILL);
	    } else {
	      paint.setStyle(Style.STROKE);
	    }
	    switch (((XYSeriesRenderer) renderer).getPointStyle()) {
	    case X:
	      drawX(canvas, paint, x + SHAPE_WIDTH, y);
	      break;
	    case CIRCLE:
	      drawCircle(canvas, paint, x + SHAPE_WIDTH, y);
	      break;
	    case TRIANGLE:
	      drawTriangle(canvas, paint, new float[6], x + SHAPE_WIDTH, y);
	      break;
	    case SQUARE:
	      drawSquare(canvas, paint, x + SHAPE_WIDTH, y);
	      break;
	    case DIAMOND:
	      drawDiamond(canvas, paint, new float[8], x + SHAPE_WIDTH, y);
	      break;
	    case POINT:
	      canvas.drawPoint(x + SHAPE_WIDTH, y, paint);
	      break;
	    }
	  }

	  /**
	   * The graphical representation of an X point shape.
	   * 
	   * @param canvas the canvas to paint to
	   * @param paint the paint to be used for drawing
	   * @param x the x value of the point the shape should be drawn at
	   * @param y the y value of the point the shape should be drawn at
	   */
	  private void drawX(Canvas canvas, Paint paint, float x, float y) {
	    canvas.drawLine(x - SIZE, y - SIZE, x + SIZE, y + SIZE, paint);
	    canvas.drawLine(x + SIZE, y - SIZE, x - SIZE, y + SIZE, paint);
	  }

	  /**
	   * The graphical representation of a circle point shape.
	   * 
	   * @param canvas the canvas to paint to
	   * @param paint the paint to be used for drawing
	   * @param x the x value of the point the shape should be drawn at
	   * @param y the y value of the point the shape should be drawn at
	   */
	  private void drawCircle(Canvas canvas, Paint paint, float x, float y) {
	    canvas.drawCircle(x, y, SIZE, paint);
	  }

	  /**
	   * The graphical representation of a triangle point shape.
	   * 
	   * @param canvas the canvas to paint to
	   * @param paint the paint to be used for drawing
	   * @param path the triangle path
	   * @param x the x value of the point the shape should be drawn at
	   * @param y the y value of the point the shape should be drawn at
	   */
	  private void drawTriangle(Canvas canvas, Paint paint, float[] path, float x, float y) {
	    path[0] = x;
	    path[1] = y - SIZE - SIZE / 2;
	    path[2] = x - SIZE;
	    path[3] = y + SIZE;
	    path[4] = x + SIZE;
	    path[5] = path[3];
	    drawPath(canvas, path, paint, true);
	  }

	  /**
	   * The graphical representation of a square point shape.
	   * 
	   * @param canvas the canvas to paint to
	   * @param paint the paint to be used for drawing
	   * @param x the x value of the point the shape should be drawn at
	   * @param y the y value of the point the shape should be drawn at
	   */
	  private void drawSquare(Canvas canvas, Paint paint, float x, float y) {
	    canvas.drawRect(x - SIZE, y - SIZE, x + SIZE, y + SIZE, paint);
	  }

	  /**
	   * The graphical representation of a diamond point shape.
	   * 
	   * @param canvas the canvas to paint to
	   * @param paint the paint to be used for drawing
	   * @param path the diamond path
	   * @param x the x value of the point the shape should be drawn at
	   * @param y the y value of the point the shape should be drawn at
	   */
	  private void drawDiamond(Canvas canvas, Paint paint, float[] path, float x, float y) {
	    path[0] = x;
	    path[1] = y - SIZE;
	    path[2] = x - SIZE;
	    path[3] = y;
	    path[4] = x;
	    path[5] = y + SIZE;
	    path[6] = x + SIZE;
	    path[7] = y;
	    drawPath(canvas, path, paint, true);
	  }
	  
	  public double getClickedX(float x, float y) {
		  float[] pt = {x, y};
		  int seriesNum = -1;
		  int itemNum = -1;
		  double minDist = Integer.MAX_VALUE;
		  for(int i=0; i<mDataset.getSeriesCount(); i++) {
			  PercentileSeries series = mDataset.getSeriesAt(i);
			  for(int j=0; j<series.getItemCount(); j++) {
				  double nextX = series.getX(j);
				  float[] high = getScreenPt(nextX, series.getHigh(j));
				  float[] low = getScreenPt(nextX, series.getLow(j));
				  float[] next = getScreenPt(nextX, series.getY(j));
				  Log.d("ptclicked", "x: " + next[0] + " / y: " + next[1]);
				  if((Math.abs(next[0] - pt[0]) < TOUCH_MARGIN) &&
						  (pt[1] < low[1] + TOUCH_MARGIN) &&
						  (pt[1] > high[1] - TOUCH_MARGIN)) {
					  double nextDist = calcDist(next, pt);
					  if(nextDist < minDist) {
						  minDist = nextDist;
						  seriesNum = i;
						  itemNum = j;
					  }
				  }
			  }
		  }
		  if(minDist == Integer.MAX_VALUE) {
			  return -1;
		  } else {
			  return mDataset.getSeriesAt(seriesNum).getX(itemNum);
		  }
	  }

}
