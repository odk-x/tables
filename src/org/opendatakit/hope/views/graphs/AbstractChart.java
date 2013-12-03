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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;


abstract class AbstractChart extends View {

    public enum LabelAxis {X, Y}
    public enum LabelOrientation { VERTICAL, HORIZONTAL }

    protected int[] DEFAULT_A_FACTORS = {1, 2, 5};
    protected int[] DEFAULT_B_FACTORS = {1};
    protected int[] DATE_TIME_B_FACTORS = {1, 30, 60, 3600};

    private static final int PADDING = 10;
    private static final int TICK_LENGTH = 6;
    private static final int TICK_SPACING = 4;

    private Paint paint;

    protected double minX;
    protected double maxX;
    protected double minY;
    protected double maxY;

    protected Label[] xLabels;
    protected Label[] yLabels;

    private int minScreenX;
    private double xDiffMultiplier;
    private int maxScreenY;
    private double yDiffMultiplier;

    protected AbstractChart(Context context) {
        super(context);
        setBackgroundColor(Color.WHITE);
        paint = new Paint();
    }

    protected double[] getRange(List<Double> values) {
        double[] range = new double[] {values.get(0), values.get(0)};
        for (int i = 1; i < values.size(); i++) {
            range[0] = Math.min(range[0], values.get(i));
            range[1] = Math.max(range[1], values.get(i));
        }
        return range;
    }

    protected Label[] getLabels(double min, double max, double tickSep,
            LabelAxis axis, LabelOrientation orientation) {
        List<Label> yLabelList = new ArrayList<Label>();
        double[] values = getLabelValues(min, max, tickSep);
        for (double value : values) {
            yLabelList.add(new Label(axis, orientation, value,
                    String.format("%.2f", value)));
        }
        return yLabelList.toArray(new Label[0]);
    }

    protected double[] getLabelValues(double min, double max, double tickSep) {
        if (tickSep == 0) {
            return new double[0];
        }
        List<Double> valueList = new ArrayList<Double>();
        min = Math.ceil(min / tickSep) * tickSep;
        for (double i = min; i < Math.min(0, max); i += tickSep) {
            valueList.add(i);
        }
        for (double i = Math.max(0, min); i <= max; i += tickSep) {
            valueList.add(i);
        }
        double[] arr = new double[valueList.size()];
        for (int i = 0; i < valueList.size(); i++) {
            arr[i] = valueList.get(i);
        }
        return arr;
    }

    protected void setScreenValues() {
        minScreenX = 0;
        for (Label label : yLabels) {
            Log.d("AC", label.label + ":" +  label.getLabelSize(paint)[0]);
            minScreenX = Math.max(minScreenX, label.getLabelSize(paint)[0]);
        }
        minScreenX += PADDING + TICK_LENGTH + TICK_SPACING;
        int xScreenRange = getWidth() - minScreenX - PADDING;
        xDiffMultiplier = xScreenRange / (maxX - minX);
        maxScreenY = PADDING;
        int maxXLabelHeight = 0;
        for (Label label : xLabels) {
            maxXLabelHeight = Math.max(maxXLabelHeight,
                    label.getLabelSize(paint)[1]);
        }
        int yScreenRange = getHeight() - maxScreenY - PADDING - TICK_LENGTH -
                TICK_SPACING - maxXLabelHeight;
        yDiffMultiplier = yScreenRange / (maxY - minY);
    }

    protected double getTickSeparation(double rangeStart, double rangeEnd,
            double goalCount, int[] aFactors, int[] bFactors) {
        double range = rangeEnd - rangeStart;
        if (range == 0) {
            return 0;
        }
        if ((rangeStart < 0) && (rangeEnd > 0)) {
            double partialRange = Math.max(rangeEnd, 0 - rangeStart);
            return getTickSeparation(0, partialRange,
                    goalCount * (partialRange / range), aFactors, bFactors);
        }
        double bestSeparation = range;
        double bestTickCount = 1;
        for (int af : aFactors) {
            for (int bf : bFactors) {
                double tickSep = af * bf;
                while (tickSep < range) {
                    double tickCount = range / tickSep;
                    if ((tickCount < goalCount) &&
                            (tickCount > bestTickCount)) {
                        bestSeparation = tickSep;
                        bestTickCount = tickCount;
                    }
                    tickSep *= 10;
                }
                double tickCount = range / (af * bf);
                while (tickCount <= goalCount) {
                    if (tickCount > bestTickCount) {
                        bestSeparation = range / tickCount;
                        bestTickCount = tickCount;
                    }
                    tickCount *= 10;
                }
            }
        }
        return bestSeparation;
    }

    protected void drawXAxis(Canvas canvas, double y) {
        int[] start = getScreenPoint(minX, y);
        int[] end = getScreenPoint(maxX, y);
        canvas.drawLine(start[0], start[1], end[0], end[1], paint);
    }

    protected void drawYAxis(Canvas canvas, double x) {
        int[] start = getScreenPoint(x, minY);
        int[] end = getScreenPoint(x, maxY);
        canvas.drawLine(start[0], start[1], end[0], end[1], paint);
    }

    protected void drawXLabels(Canvas canvas, boolean drawTicks) {
        for (Label label : xLabels) {
            label.draw(canvas, paint, drawTicks);
        }
    }

    protected void drawYLabels(Canvas canvas, boolean drawTicks) {
        for (Label label : yLabels) {
            label.draw(canvas, paint, drawTicks);
        }
    }

    protected int[] getScreenPoint(double x, double y) {
        int screenX = minScreenX +
                (Double.valueOf(xDiffMultiplier * (x - minX))).intValue();
        int screenY = maxScreenY +
                (Double.valueOf(yDiffMultiplier * (maxY - y))).intValue();
        return new int[] {screenX, screenY};
    }

    protected double[] getDataPoint(int x, int y) {
        double dataX = ((x - minScreenX) / xDiffMultiplier) + minX;
        double dataY = maxY - ((y - maxScreenY) / yDiffMultiplier);
        return new double[] {dataX, dataY};
    }

    protected class Label {

        private LabelAxis axis;
        private LabelOrientation orientation;
        private double value;
        private String label;

        public Label(LabelAxis axis, LabelOrientation orientation,
                double value, String label) {
            this.axis = axis;
            this.orientation = orientation;
            this.value = value;
            this.label = label;
        }

        void draw(Canvas canvas, Paint paint, boolean drawTick) {
            int[] pt;
            int[] tickStart;
            int[] tickEnd;
            if (axis == LabelAxis.X) {
                pt = getScreenPoint(value, minY);
                pt[1] += (TICK_LENGTH + TICK_SPACING);
                tickStart = new int[] {pt[0], pt[1] - TICK_SPACING};
                tickEnd = new int[] {pt[0],
                        pt[1] - TICK_SPACING - TICK_LENGTH};
                pt[0] += paint.getTextSize() / 3;
            } else {
                pt = new int[] {PADDING + getLabelSize(paint)[0],
                        getScreenPoint(0.0, value)[1]};
                tickStart = new int[] {pt[0] + TICK_SPACING, pt[1]};
                tickEnd = new int[] {pt[0] + TICK_SPACING + TICK_LENGTH,
                        pt[1]};
                pt[1] += paint.getTextSize() / 3;
            }
            paint.setTextAlign(Paint.Align.RIGHT);
            if (orientation == LabelOrientation.VERTICAL) {
                canvas.rotate(-90);
                canvas.drawText(label, -1 *pt[1], pt[0], paint);
                canvas.rotate(90);
            } else {
                canvas.drawText(label, pt[0], pt[1], paint);
            }
            if (drawTick) {
                canvas.drawLine(tickStart[0], tickStart[1], tickEnd[0],
                        tickEnd[1], paint);
            }
        }

        int[] getLabelSize(Paint paint) {
            Rect r = new Rect();
            paint.getTextBounds(label, 0, label.length(), r);
            if (orientation == LabelOrientation.HORIZONTAL) {
                return new int[] {(r.right - r.left), (r.bottom - r.top)};
            } else {
                return new int[] {(r.bottom - r.top), (r.right - r.left)};
            }
        }
    }
}
