package yoonsung.odk.spreadsheet.view;

import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.view.View;
import yoonsung.odk.spreadsheet.DataStructure.Table;
import yoonsung.odk.spreadsheet.Library.graphs.GXYPoint;
import yoonsung.odk.spreadsheet.Library.graphs.GraphFactory;


public class LineGraph {
    
    public static View build(Activity a, Table table, int xIndex, int yIndex,
            String title) {
        List<GXYPoint> data = new ArrayList<GXYPoint>();
        for (int i = 0; i < table.getHeight(); i++) {
            double x = Double.parseDouble(table.getCellValue(
                    (table.getWidth() * i) + xIndex));
            double y = Double.parseDouble(table.getCellValue(
                    (table.getWidth() * i) + yIndex));
            data.add(new GXYPoint(x, y));
        }
        return (new GraphFactory(a)).getXYLineGraph(data, title,
                table.getColName(xIndex), table.getColName(yIndex));
    }
}
