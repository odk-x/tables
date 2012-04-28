package org.opendatakit.tables.Library.graphs;

import java.util.List;

import org.achartengine.chart.AbstractChart;
import org.achartengine.chart.ScatterChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer.Orientation;
import org.achartengine.util.MathHelper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;

/**
 * The XY chart rendering class.
 */
public abstract class EXYChart extends AbstractChart {

  private static final long serialVersionUID = 1L;
  /** The multiple series dataset. */
  protected XYMultipleSeriesDataset mDataset;
  /** The multiple series renderer. */
  protected XYMultipleSeriesRenderer mRenderer;
  /** The current scale value. */
  private float mScale;
  /** The current translate value. */
  private float mTranslate;
  /** The canvas center point. */
  private PointF mCenter;
  /** The visible chart area, in screen coordinates. */
  private Rect screenR;
  /** The calculated range. */
  private double[] calcRange = new double[4];
  /** The grid color. */
  protected static final int GRID_COLOR = Color.argb(75, 200, 200, 200);
  
  protected static final int TOUCH_MARGIN = 30;
  
  private double minX;
  private double maxX;
  private double minY;
  private double maxY;
  private int left;
  private int top;
  private int right;
  private int bottom;
  private double xPixelsPerUnit;
  private double yPixelsPerUnit;
  
  /**
   * Builds a new XY chart instance.
   * 
   * @param dataset the multiple series dataset
   * @param renderer the multiple series renderer
   */
  protected EXYChart(XYMultipleSeriesDataset dataset,
		  XYMultipleSeriesRenderer renderer) {
    mDataset = dataset;
    mRenderer = renderer;
  }

  /**
   * The graphical representation of the XY chart.
   * 
   * @param canvas the canvas to paint to
   * @param x the top left x value of the view to draw to
   * @param y the top left y value of the view to draw to
   * @param width the width of the view to draw to
   * @param height the height of the view to draw to
   */
  @Override
  public void draw(Canvas canvas, int x, int y, int width, int height) {
    Paint paint = new Paint();
    paint.setAntiAlias(mRenderer.isAntialiasing());
    int legendSize = 30;
    if (mRenderer.isShowLegend()) {
      legendSize = height / 5;
    }
    // (added) changed:
    int yGap = getYGap(paint) - 10;
    // changed:
    left = x + 40;
    top = y + 30;
    // changed:
    right = x + width - 10;
    // changed:
    bottom = y + height - legendSize - yGap;
    if (screenR == null) {
      screenR = new Rect();
    }
    screenR.set(left, top, right, bottom);
    drawBackground(mRenderer, canvas, x, y, width, height, paint);

    if (paint.getTypeface() == null
        || !paint.getTypeface().toString().equals(mRenderer.getTextTypefaceName())
        || paint.getTypeface().getStyle() != mRenderer.getTextTypefaceStyle()) {
      paint.setTypeface(Typeface.create(mRenderer.getTextTypefaceName(), mRenderer
          .getTextTypefaceStyle()));
    }
    Orientation or = mRenderer.getOrientation();
    if (or == Orientation.VERTICAL) {
      right -= legendSize;
      bottom += legendSize - 20;
    }
    int angle = or.getAngle();
    boolean rotate = angle == 90;
    mScale = (float) (height) / width;
    mTranslate = Math.abs(width - height) / 2;
    if (mScale < 1) {
      mTranslate *= -1;
    }
    mCenter = new PointF((x + width) / 2, (y + height) / 2);
    if (rotate) {
      transform(canvas, angle, false);
    }
    // changed
    minX = mRenderer.getXAxisMin();
    maxX = mRenderer.getXAxisMax();
    minY = mRenderer.getYAxisMin();
    maxY = mRenderer.getYAxisMax();
    boolean isMinXSet = mRenderer.isMinXSet();
    boolean isMaxXSet = mRenderer.isMaxXSet();
    boolean isMinYSet = mRenderer.isMinYSet();
    boolean isMaxYSet = mRenderer.isMaxYSet();
    xPixelsPerUnit = 0;
    yPixelsPerUnit = 0;
    int sLength = mDataset.getSeriesCount();
    String[] titles = new String[sLength];
    for (int i = 0; i < sLength; i++) {
      XYSeries series = mDataset.getSeriesAt(i);
      titles[i] = series.getTitle();
      if (series.getItemCount() == 0) {
        continue;
      }
      if (!isMinXSet) {
        double minimumX = series.getMinX();
        minX = Math.min(minX, minimumX);
        calcRange[0] = minX;
      }
      if (!isMaxXSet) {
        double maximumX = series.getMaxX();
        maxX = Math.max(maxX, maximumX);
        calcRange[1] = maxX;
      }
      if (!isMinYSet) {
        double minimumY = series.getMinY();
        minY = Math.min(minY, (float) minimumY);
        calcRange[2] = minY;
      }
      if (!isMaxYSet) {
        double maximumY = series.getMaxY();
        maxY = Math.max(maxY, (float) maximumY);
        calcRange[3] = maxY;
      }
    }
    double xDiff = .08 * (maxX - minX);
    maxX += xDiff;
    minX -= xDiff;
    double yDiff = .08 * (maxY - minY);
    maxY += yDiff;
    minY -= yDiff;
    if (maxX - minX != 0) {
      xPixelsPerUnit = (right - left) / (maxX - minX);
    }
    if (maxY - minY != 0) {
      yPixelsPerUnit = (float) ((bottom - top) / (maxY - minY));
    }

    boolean hasValues = false;
    for (int i = 0; i < sLength; i++) {
      XYSeries series = mDataset.getSeriesAt(i);
      if (series.getItemCount() == 0) {
        continue;
      }
      hasValues = true;
      SimpleSeriesRenderer seriesRenderer = mRenderer.getSeriesRendererAt(i);
      int originalValuesLength = series.getItemCount();
      float[] points = null;
      int valuesLength = originalValuesLength;
      int length = valuesLength * 2;
      points = new float[length];
      for (int j = 0; j < length; j += 2) {
        int index = j / 2;
        points[j] = (float) (left + xPixelsPerUnit * (series.getX(index) - minX));
        points[j + 1] = (float) (bottom - yPixelsPerUnit * (series.getY(index) - minY));
      }
      drawSeries(canvas, paint, points, seriesRenderer, Math.min(bottom,
          (float) (bottom + yPixelsPerUnit * minY)), i);
      if (isRenderPoints(seriesRenderer)) {
        ScatterChart pointsChart = new ScatterChart(mDataset, mRenderer);
        pointsChart.drawSeries(canvas, paint, points, seriesRenderer, 0, i);
      }
      paint.setTextSize(mRenderer.getChartValuesTextSize());
      if (or == Orientation.HORIZONTAL) {
        paint.setTextAlign(Align.CENTER);
      } else {
        paint.setTextAlign(Align.LEFT);
      }
      if (mRenderer.isDisplayChartValues()) {
        drawChartValuesText(canvas, series, paint, points, i);
      }
    }

    boolean showLabels = mRenderer.isShowLabels() && hasValues;
    boolean showGrid = mRenderer.isShowGrid();
    if (showLabels || showGrid) {
      List<Double> xLabels = MathHelper.getLabels(minX, maxX, mRenderer.getXLabels());
      List<Double> yLabels = MathHelper.getLabels(minY, maxY, mRenderer.getYLabels());
      if (showLabels) {
        paint.setColor(mRenderer.getLabelsColor());
        paint.setTextSize(mRenderer.getLabelsTextSize());
        paint.setTextAlign(Align.CENTER);
      }
      drawXLabels(xLabels, mRenderer.getXTextLabelLocations(), canvas, paint, left, top, bottom,
          xPixelsPerUnit, minX);
      int length = yLabels.size();
      for (int i = 0; i < length; i++) {
        double label = yLabels.get(i);
        float yLabel = (float) (bottom - yPixelsPerUnit * (label - minY));
        if (or == Orientation.HORIZONTAL) {
          if (showLabels) {
            paint.setColor(mRenderer.getLabelsColor());
            canvas.drawLine(left - 4, yLabel, left, yLabel, paint);
            // changed:
            drawText(canvas, getLabel(label), left - 12, yLabel + 2, paint, 0);
          }
          if (showGrid) {
            paint.setColor(GRID_COLOR);
            canvas.drawLine(left, yLabel, right, yLabel, paint);
          }
        } else if (or == Orientation.VERTICAL) {
          if (showLabels) {
            paint.setColor(mRenderer.getLabelsColor());
            canvas.drawLine(right + 4, yLabel, right, yLabel, paint);
            drawText(canvas, getLabel(label), right + 10, yLabel - 2, paint, 0);
          }
          if (showGrid) {
            paint.setColor(GRID_COLOR);
            canvas.drawLine(right, yLabel, left, yLabel, paint);
          }
        }
      }

      if (showLabels) {
        paint.setColor(mRenderer.getLabelsColor());
        paint.setTextSize(mRenderer.getAxisTitleTextSize());
        paint.setTextAlign(Align.CENTER);
        if (or == Orientation.HORIZONTAL) {
        	// changed:
          drawText(canvas, mRenderer.getXTitle(), x + 16 + width / 2, bottom + 24 + yGap, paint, 0);
          // changed:
          drawText(canvas, mRenderer.getYTitle(), x + 15, y + (height - yGap) / 2, paint, -90);
          paint.setTextSize(mRenderer.getChartTitleTextSize());
          drawText(canvas, mRenderer.getChartTitle(), x + width / 2, top - 10, paint, 0);
        } else if (or == Orientation.VERTICAL) {
        	// changed:
          drawText(canvas, mRenderer.getXTitle(), x + 16 + width / 2, y + height - 10 + yGap, paint, -90);
          // changed:
          drawText(canvas, mRenderer.getYTitle(), right + 25, y + (height - yGap) / 2, paint, 0);
          paint.setTextSize(mRenderer.getChartTitleTextSize());
          drawText(canvas, mRenderer.getChartTitle(), x + 14, top + height / 2, paint, 0);
        }
      }
    }

    if (or == Orientation.HORIZONTAL) {
      drawLegend(canvas, mRenderer, titles, left, right, y, width, height, legendSize, paint);
    } else if (or == Orientation.VERTICAL) {
      transform(canvas, angle, true);
      drawLegend(canvas, mRenderer, titles, left, right, y, width, height, legendSize, paint);
      transform(canvas, angle, false);
    }
    if (mRenderer.isShowAxes()) {
      paint.setColor(mRenderer.getAxesColor());
      canvas.drawLine(left, bottom, right, bottom, paint);
      if (or == Orientation.HORIZONTAL) {
        canvas.drawLine(left, top, left, bottom, paint);
      } else if (or == Orientation.VERTICAL) {
        canvas.drawLine(right, top, right, bottom, paint);
      }
    }
    if (rotate) {
      transform(canvas, angle, true);
    }
  }

  /**
   * The graphical representation of the series values as text.
   * 
   * @param canvas the canvas to paint to
   * @param series the series to be painted
   * @param paint the paint to be used for drawing
   * @param points the array of points to be used for drawing the series
   * @param seriesIndex the index of the series currently being drawn
   */
  protected void drawChartValuesText(Canvas canvas, XYSeries series, Paint paint, float[] points,
      int seriesIndex) {
    for (int k = 0; k < points.length; k += 2) {
      drawText(canvas, getLabel(series.getY(k / 2)), points[k], points[k + 1] - 3.5f, paint, 0);
    }
  }

  /**
   * The graphical representation of a text, to handle both HORIZONTAL and
   * VERTICAL orientations and extra rotation angles.
   * 
   * @param canvas the canvas to paint to
   * @param text the text to be rendered
   * @param x the X axis location of the text
   * @param y the Y axis location of the text
   * @param paint the paint to be used for drawing
   * @param extraAngle the array of points to be used for drawing the series
   */
  protected void drawText(Canvas canvas, String text, float x, float y, Paint paint, int extraAngle) {
    int angle = -mRenderer.getOrientation().getAngle() + extraAngle;
    if (angle != 0) {
      // canvas.scale(1 / mScale, mScale);
      canvas.rotate(angle, x, y);
    }
    canvas.drawText(text, x, y, paint);
    if (angle != 0) {
      canvas.rotate(-angle, x, y);
      // canvas.scale(mScale, 1 / mScale);
    }
  }

  /**
   * Transform the canvas such as it can handle both HORIZONTAL and VERTICAL
   * orientations.
   * 
   * @param canvas the canvas to paint to
   * @param angle the angle of rotation
   * @param inverse if the inverse transform needs to be applied
   */
  private void transform(Canvas canvas, float angle, boolean inverse) {
    if (inverse) {
      canvas.scale(1 / mScale, mScale);
      canvas.translate(mTranslate, -mTranslate);
      canvas.rotate(-angle, mCenter.x, mCenter.y);
    } else {
      canvas.rotate(angle, mCenter.x, mCenter.y);
      canvas.translate(-mTranslate, mTranslate);
      canvas.scale(mScale, 1 / mScale);
    }
  }

  /**
   * Makes sure the fraction digit is not displayed, if not needed.
   * 
   * @param label the input label value
   * @return the label without the useless fraction digit
   */
  protected String getLabel(double label) {
    String text = "";
    if (label == Math.round(label)) {
      text = Math.round(label) + "";
    } else {
      text = label + "";
    }
    return text;
  }

  /**
   * The graphical representation of the labels on the X axis.
   * 
   * @param xLabels the X labels values
   * @param xTextLabelLocations the X text label locations
   * @param canvas the canvas to paint to
   * @param paint the paint to be used for drawing
   * @param left the left value of the labels area
   * @param top the top value of the labels area
   * @param bottom the bottom value of the labels area
   * @param xPixelsPerUnit the amount of pixels per one unit in the chart labels
   * @param minX the minimum value on the X axis in the chart
   */
  protected void drawXLabels(List<Double> xLabels, Double[] xTextLabelLocations, Canvas canvas,
      Paint paint, int left, int top, int bottom, double xPixelsPerUnit, double minX) {
    int length = xLabels.size();
    boolean showLabels = mRenderer.isShowLabels();
    boolean showGrid = mRenderer.isShowGrid();
    for (int i = 0; i < length; i++) {
      double label = xLabels.get(i);
      float xLabel = (float) (left + xPixelsPerUnit * (label - minX));
      if (showLabels) {
        paint.setColor(mRenderer.getLabelsColor());
        canvas.drawLine(xLabel, bottom, xLabel, bottom + 4, paint);
        String labelText = mRenderer.getXTextLabel(label);
        if(labelText == null) {
        	labelText = getLabel(label);
        }
        paint.setTextAlign(Paint.Align.LEFT);
        drawText(canvas, labelText, xLabel - 3, bottom + 8, paint, 90);
        paint.setTextAlign(Paint.Align.CENTER);
      }
      if (showGrid) {
        paint.setColor(GRID_COLOR);
        canvas.drawLine(xLabel, bottom, xLabel, top, paint);
      }
    }
  }
  
  // TODO: docs
  public XYMultipleSeriesRenderer getRenderer() {
    return mRenderer;
  }
  
  public double[] getCalcRange() {
    return calcRange;
  }

  public PointF toRealPoint(float screenX, float screenY) {
    double realMinX = mRenderer.getXAxisMin();
    double realMaxX = mRenderer.getXAxisMax();
    double realMinY = mRenderer.getYAxisMin();
    double realMaxY = mRenderer.getYAxisMax();
    return new PointF((float) ((screenX - screenR.left) * (realMaxX - realMinX) / screenR.width() + realMinX), 
        (float) ((screenR.top + screenR.height() - screenY) * (realMaxY - realMinY) / screenR.height() + realMinY));
  }
  
  public PointF toScreenPoint(PointF realPoint) {
    double realMinX = mRenderer.getXAxisMin();
    double realMaxX = mRenderer.getXAxisMax();
    double realMinY = mRenderer.getYAxisMin();
    double realMaxY = mRenderer.getYAxisMax();
    return new PointF((float) ((realPoint.x - realMinX) * screenR.width() / (realMaxX - realMinX) + screenR.left),
        (float) ((realMaxY - realPoint.y) * screenR.height() / (realMaxY - realMinY) + screenR.top));
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
  public abstract void drawSeries(Canvas canvas, Paint paint, float[] points,
      SimpleSeriesRenderer seriesRenderer, float yAxisValue, int seriesIndex);

  /**
   * Returns if the chart should display the points as a certain shape.
   * 
   * @param renderer the series renderer
   */
  public boolean isRenderPoints(SimpleSeriesRenderer renderer) {
    return false;
  }
  
  private int getYGap(Paint paint) {
	  Double[] xLabelLocs = mRenderer.getXTextLabelLocations();
	  float max = paint.measureText(mRenderer.getXTextLabel(xLabelLocs[0]));
	  for(int i=0; i<xLabelLocs.length; i++) {
		  float next = paint.measureText(mRenderer.getXTextLabel(xLabelLocs[i]));
		  max = Math.max(max, next);
	  }
	  return Math.round(max);
  }
  
  protected float[] getScreenPt(double x, double y) {
	  float[] pt = new float[2];
      pt[0] = (float) (left + xPixelsPerUnit * (x - minX));
      pt[1] = (float) (bottom - yPixelsPerUnit * (y - minY));
	  return pt;
  }
  
  public double getClickedX(float x, float y) {
	  float[] pt = {x, y};
	  int seriesNum = -1;
	  int itemNum = -1;
	  double minDist = Integer.MAX_VALUE;
	  for(int i=0; i<mDataset.getSeriesCount(); i++) {
		  XYSeries series = mDataset.getSeriesAt(i);
		  for(int j=0; j<series.getItemCount(); j++) {
			  float[] next = getScreenPt(series.getX(j), series.getY(j));
			  if((Math.abs(next[0] - pt[0]) < TOUCH_MARGIN) &&
					  (Math.abs(next[1] - pt[1]) < TOUCH_MARGIN)) {
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
  
  protected double calcDist(float[] pt1, float[] pt2) {
	  return Math.pow(Math.pow(pt1[0] - pt2[0], 2) +
			  Math.pow(pt1[1] - pt2[1], 2), .5);
  }
}
