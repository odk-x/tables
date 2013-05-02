/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.views.graphs;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;


public class BoxStemChart extends AbstractChart {
    
    private static final double PADDING = 0.05;
    private static final double BAR_HALF_WIDTH = 0.15;
    
    private final DataPoint[] data;
    private final Paint paint;
    private final ClickListener listener;
    
    public BoxStemChart(Context context, DataPoint[] data,
            ClickListener listener) {
        super(context);
        this.data = data;
        paint = new Paint();
        this.listener = listener;
        setOnTouchListener(new BoxStemChartTouchListener());
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
        if (xTickSep == 0) {
            xLabels = new Label[1];
        } else {
            xLabels = new Label[data.length / xTickSep];
        }
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
            int leftSide = getScreenPoint(i - BAR_HALF_WIDTH,
                    data[i].getMidLow())[0];
            int rightSide = getScreenPoint(i + BAR_HALF_WIDTH,
                    data[i].getMidLow())[0];
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
    
    private class BoxStemChartTouchListener implements View.OnTouchListener {
        
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                return true;
            } else if (event.getAction() != MotionEvent.ACTION_UP) {
                return false;
            }
            int x = (new Float(event.getX())).intValue();
            int y = (new Float(event.getY())).intValue();
            double[] value = getDataPoint(x, y);
            int index = (new Double(value[0])).intValue();
            if ((index < 0) || (index >= data.length)) {
                return false;
            } else if ((value[1] < data[index].getLow()) ||
                    (value[1] > data[index].getHigh())) {
                return false;
            } else {
                listener.onClick(index);
                return true;
            }
        }
    }
    
    public interface ClickListener {
        
        public void onClick(int index);
    }
}
