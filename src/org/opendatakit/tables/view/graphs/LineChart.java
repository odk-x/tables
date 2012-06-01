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

import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;


public class LineChart extends PointPlot {
    
    private static final int LINE_COLOR = Color.BLUE;
    
    private final Paint linePaint;
    
    private LineChart(Context context, List<Double> xNumValues,
            List<DateTime> xDateValues, List<Double> yValues,
            DataType dataType) {
        super(context, xNumValues, yValues, dataType);
        linePaint = new Paint();
        linePaint.setColor(LINE_COLOR);
    }
    
    public static LineChart createNumberLineChart(Context context,
            List<Double> xValues, List<Double> yValues) {
        return new LineChart(context, xValues, null, yValues, DataType.NUMBER);
    }
    
    public static LineChart createDateLineChart(Context context,
            List<DateTime> xValues, List<Double> yValues) {
        List<Double> xNumValues = new ArrayList<Double>(xValues.size());
        for (DateTime dt : xValues) {
            xNumValues.add(dt.getMillis() / 1000.0);
        }
        return new LineChart(context, xNumValues, xValues, yValues,
                DataType.DATE);
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
