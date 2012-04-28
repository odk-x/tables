package org.opendatakit.tables.view.graphs;

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
        if (tickSep == 0) {
            return new Label[0];
        }
        for (double i = min; i < 0; i += tickSep) {
            yLabelList.add(new Label(axis, orientation, i,
                    String.format("%.2f", i)));
        }
        for (double i = 0; i <= max; i += tickSep) {
            yLabelList.add(new Label(axis, orientation, i,
                    String.format("%.2f", i)));
        }
        return yLabelList.toArray(new Label[0]);
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
                    if (tickCount > bestTickCount) {
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
    
    protected void drawXAxis(Canvas canvas) {
        int[] start = getScreenPoint(minX, 0);
        int[] end = getScreenPoint(maxX, 0);
        canvas.drawLine(start[0], start[1], end[0], end[1], paint);
    }
    
    protected void drawYAxis(Canvas canvas) {
        int[] start = getScreenPoint(0, minY);
        int[] end = getScreenPoint(0, maxY);
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
                (new Double(xDiffMultiplier * (x - minX))).intValue();
        int screenY = maxScreenY +
                (new Double(yDiffMultiplier * (maxY - y))).intValue();
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
            if (orientation == LabelOrientation.VERTICAL) {
                canvas.rotate(-90);
                paint.setTextAlign(Paint.Align.RIGHT);
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
