package org.opendatakit.tables.view.graphs;

import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;


public class LineChart extends PointPlot {
    
    private static final int LINE_COLOR = Color.BLUE;
    
    private final Paint linePaint;
    
    public LineChart(Context context, List<Double> xValues,
            List<Double> yValues) {
        super(context, xValues, yValues);
        linePaint = new Paint();
        linePaint.setColor(LINE_COLOR);
    }
    
    @Override
    protected void drawData(Canvas canvas) {
        for (int i = 1; i < xValues.size(); i++) {
            int[] ptA = getScreenPoint(xValues.get(i - 1), yValues.get(i - 1));
            int[] ptB = getScreenPoint(xValues.get(i), yValues.get(i));
            canvas.drawLine(ptA[0], ptA[1], ptB[0], ptB[1], linePaint);
        }
        super.drawData(canvas);
    }
}
