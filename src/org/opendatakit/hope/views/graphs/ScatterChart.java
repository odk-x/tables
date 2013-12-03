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
package org.opendatakit.hope.views.graphs;

import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;


public class ScatterChart extends PointPlot {
    
    private static final int OUTER_CIRCLE_RADIUS = POINT_RADIUS + 2;
    private static final int OUTER_CIRCLE_COLOR = Color.BLUE;
    
    private final Paint outerCirclePaint;
    
    public ScatterChart(Context context, List<Double> xValues,
            List<Double> yValues) {
        super(context, xValues, yValues, DataType.NUMBER);
        outerCirclePaint = new Paint();
        outerCirclePaint.setColor(OUTER_CIRCLE_COLOR);
    }
    
    @Override
    protected void drawData(Canvas canvas) {
        super.drawData(canvas);
        for (int i = 0; i < xValues.size(); i++) {
            int[] pt = getScreenPoint(xValues.get(i), yValues.get(i));
            canvas.drawCircle(pt[0], pt[1], OUTER_CIRCLE_RADIUS,
                    outerCirclePaint);
        }
    }
}
