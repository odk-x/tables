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
package org.opendatakit.tables.view.graphs;

import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;


public class BarChart extends AbstractChart {
    
    private final List<Double> values;
    private final Paint paint;
    private final ClickListener listener;
    
    public BarChart(Context context, List<String> labels, List<Double> values,
            ClickListener listener) {
        super(context);
        if (labels.size() != values.size()) {
            throw new RuntimeException();
        }
        this.values = values;
        paint = new Paint();
        this.listener = listener;
        setOnTouchListener(new BarChartTouchListener());
        // initializing minimum and maximum x and y values
        minX = 0;
        maxX = labels.size();
        minY = 0;
        maxY = 0;
        for (int i = 0; i < values.size(); i++) {
            minY = Math.min(minY, values.get(i));
            maxY = Math.max(maxY, values.get(i));
        }
        // initializing label arrays
        xLabels = new Label[labels.size()];
        for (int i = 0; i < labels.size(); i++) {
            xLabels[i] = new Label(LabelAxis.X, LabelOrientation.VERTICAL,
                    i + 0.5, labels.get(i));
        }
        double tickSep = getTickSeparation(minY, maxY, 10, DEFAULT_A_FACTORS,
                DEFAULT_B_FACTORS);
        yLabels = getLabels(minY, maxY, tickSep, LabelAxis.Y,
                LabelOrientation.HORIZONTAL);
    }
    
    @Override
    public void onDraw(Canvas canvas) {
        setScreenValues();
        drawXAxis(canvas);
        drawYAxis(canvas);
        drawXLabels(canvas, false);
        drawYLabels(canvas, true);
        drawData(canvas);
    }
    
    private void drawData(Canvas canvas) {
        for (int i = 0; i < values.size(); i++) {
            int[] bl = getScreenPoint(i, Math.min(0, values.get(i)));
            int[] tr = getScreenPoint(i + 1, Math.max(0, values.get(i)));
            canvas.drawRect(bl[0] + 2, tr[1], tr[0] - 2, bl[1], paint);
        }
    }
    
    private class BarChartTouchListener implements View.OnTouchListener {
        
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int x = (new Float(event.getX())).intValue();
            int y = (new Float(event.getY())).intValue();
            double[] value = getDataPoint(x, y);
            int index = (new Double(value[0])).intValue();
            if ((value[1] >= Math.min(0, values.get(index))) &&
                    (value[1] <= Math.max(0, values.get(index)))) {
                listener.onClick(index);
                return true;
            } else {
                return false;
            }
        }
    }
    
    public interface ClickListener {
        
        public void onClick(int index);
    }
}
