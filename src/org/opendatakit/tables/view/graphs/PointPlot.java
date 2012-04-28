package org.opendatakit.tables.view.graphs;

import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;


public abstract class PointPlot extends AbstractChart {
    
    private static final double PADDING = 0.05;
    protected static final int POINT_RADIUS = 2;
    
    protected final List<Double> xValues;
    protected final List<Double> yValues;
    private final Paint paint;
    
    public PointPlot(Context context, List<Double> xValues,
            List<Double> yValues) {
        super(context);
        if (xValues.size() != yValues.size()) {
            throw new RuntimeException();
        }
        this.xValues = xValues;
        this.yValues = yValues;
        paint = new Paint();
        // initializing minimum and maximum x and y values
        double[] xRange = getRange(xValues);
        double xPadding = PADDING * (xRange[1] - xRange[0]);
        double[] yRange = getRange(yValues);
        double yPadding = PADDING * (yRange[1] - yRange[0]);
        minX = xRange[0] - xPadding;
        maxX = xRange[1] + xPadding;
        minY = yRange[0] - yPadding;
        maxY = yRange[1] + yPadding;
        // initializing label arrays
        double xTickSep = getTickSeparation(minX, maxX, 10, DEFAULT_A_FACTORS,
                DEFAULT_B_FACTORS);
        xLabels = getLabels(minX, maxX, xTickSep, LabelAxis.X,
                LabelOrientation.VERTICAL);
        double yTickSep = getTickSeparation(minY, maxY, 10, DEFAULT_A_FACTORS,
                DEFAULT_B_FACTORS);
        yLabels = getLabels(minY, maxY, yTickSep, LabelAxis.Y,
                LabelOrientation.HORIZONTAL);
    }
    
    @Override
    public void onDraw(Canvas canvas) {
        setScreenValues();
        drawXAxis(canvas);
        drawYAxis(canvas);
        drawXLabels(canvas, true);
        drawYLabels(canvas, true);
        drawData(canvas);
    }
    
    protected void drawData(Canvas canvas) {
        for (int i = 0; i < xValues.size(); i++) {
            int[] pt = getScreenPoint(xValues.get(i), yValues.get(i));
            canvas.drawCircle(pt[0], pt[1], POINT_RADIUS, paint);
        }
    }
}
