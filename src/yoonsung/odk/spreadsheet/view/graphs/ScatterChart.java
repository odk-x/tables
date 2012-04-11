package yoonsung.odk.spreadsheet.view.graphs;

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
        super(context, xValues, yValues);
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
