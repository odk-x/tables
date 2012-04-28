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
package org.opendatakit.tables.Library.graphs;

import java.util.ArrayList;
import java.util.List;

import org.achartengine.GraphicalView;

import android.content.Context;
import android.view.MotionEvent;

public class EGraphicalView extends GraphicalView {
	
	private EXYChart chart;
	private List<GraphClickListener> listeners;
	
	public EGraphicalView(Context context, EXYChart chart) {
		super(context, chart);
		this.chart = chart;
		listeners = new ArrayList<GraphClickListener>();
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_UP) {
			for(GraphClickListener l : listeners) {
				l.graphClicked(chart.getClickedX(event.getX(), event.getY()));
			}
		}
		return true;
	}
	
	public void addListener(GraphClickListener listener) {
		listeners.add(listener);
	}
	
}
