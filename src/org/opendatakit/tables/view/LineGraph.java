package org.opendatakit.tables.view;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.tables.DataStructure.Table;
import org.opendatakit.tables.Library.graphs.GXYPoint;
import org.opendatakit.tables.Library.graphs.GraphFactory;

import android.app.Activity;
import android.view.View;


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
