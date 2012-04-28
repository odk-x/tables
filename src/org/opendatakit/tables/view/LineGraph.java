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
