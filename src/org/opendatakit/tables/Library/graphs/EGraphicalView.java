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
