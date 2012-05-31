package org.opendatakit.tables.view.graphs;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;


public class BoxStemChart extends AbstractChart {
    
    private static final double PADDING = 0.05;
    
    private final DataPoint[] data;
    private final Paint paint;
    
    public BoxStemChart(Context context, DataPoint[] data) {
        super(context);
        this.data = data;
        paint = new Paint();
        // initializing minimum and maximum x and y values
        double loY = data[0].getLow();
        double hiY = data[0].getHigh();
        for (int i = 1; i < data.length; i++) {
            loY = Math.min(loY, data[i].getLow());
            hiY = Math.max(hiY, data[i].getHigh());
        }
        double yPadding = (hiY - loY) * PADDING;
        minX = -1;
        maxX = data.length;
        minY = loY - yPadding;
        maxY = hiY + yPadding;
        // initializing label arrays
        int xTickSep = (int) Math.ceil(getTickSeparation(0, data.length - 1,
                10, DEFAULT_A_FACTORS, DEFAULT_B_FACTORS));
        xLabels = new Label[data.length / xTickSep];
        for (int i = 0; i < xLabels.length; i++) {
            int value = i * xTickSep;
            xLabels[i] = new Label(LabelAxis.X, LabelOrientation.VERTICAL,
                    value, data[value].getX());
        }
        double yTickSep = getTickSeparation(minY, maxY, 10, DEFAULT_A_FACTORS,
                DEFAULT_B_FACTORS);
        yLabels = getLabels(minY, maxY, yTickSep, LabelAxis.Y,
                LabelOrientation.HORIZONTAL);
    }
    
    @Override
    public void onDraw(Canvas canvas) {
        setScreenValues();
        drawXAxis(canvas, minY);
        drawYAxis(canvas, -1);
        drawXLabels(canvas, true);
        drawYLabels(canvas, true);
        drawData(canvas);
    }
    
    protected void drawData(Canvas canvas) {
        for (int i = 0; i < data.length; i++) {
            int[] loPt = getScreenPoint(i, data[i].getLow());
            int[] mLoPt = getScreenPoint(i, data[i].getMidLow());
            int[] mHiPt = getScreenPoint(i, data[i].getMidHigh());
            int[] hiPt = getScreenPoint(i, data[i].getHigh());
            int leftSide = getScreenPoint(i - 0.15, data[i].getMidLow())[0];
            int rightSide = getScreenPoint(i + 0.15, data[i].getMidLow())[0];
            canvas.drawLine(loPt[0], loPt[1], mLoPt[0], mLoPt[1], paint);
            canvas.drawLine(mHiPt[0], mHiPt[1], hiPt[0], hiPt[1], paint);
            canvas.drawLine(leftSide, mLoPt[1], rightSide, mLoPt[1], paint);
            canvas.drawLine(leftSide, mHiPt[1], rightSide, mHiPt[1], paint);
            canvas.drawLine(leftSide, mLoPt[1], leftSide, mHiPt[1], paint);
            canvas.drawLine(rightSide, mLoPt[1], rightSide, mHiPt[1], paint);
            canvas.drawLine(leftSide, loPt[1], rightSide, loPt[1], paint);
            canvas.drawLine(leftSide, hiPt[1], rightSide, hiPt[1], paint);
        }
    }
    
    public static class DataPoint {
        
        private final String x;
        private final double low;
        private final double midLow;
        private final double midHigh;
        private final double high;
        
        public DataPoint(String x, double low, double midLow, double midHigh,
                double high) {
            this.x = x;
            this.low = low;
            this.midLow = midLow;
            this.midHigh = midHigh;
            this.high = high;
        }
        
        public String getX() {
            return x;
        }
        
        public double getLow() {
            return low;
        }
        
        public double getMidLow() {
            return midLow;
        }
        
        public double getMidHigh() {
            return midHigh;
        }
        
        public double getHigh() {
            return high;
        }
        
        @Override
        public String toString() {
            return low + "/" + midLow + "/" + midHigh + "/" + high;
        }
    }
}
