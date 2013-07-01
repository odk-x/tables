package org.opendatakit.tables.views.webkits;

import java.lang.ref.WeakReference;

import org.opendatakit.tables.views.webkits.CustomGraphView.GraphData;

public class GraphDataIf {
	  private WeakReference<GraphData> weakGraphData;

	  GraphDataIf( GraphData graphData ) {
		  this.weakGraphData = new WeakReference<GraphData>(graphData);
	  }

    public boolean isModified() {
    	return weakGraphData.get().isModified();
    }

    public boolean hasGraph(String graph) {
    	return weakGraphData.get().hasGraph(graph);
    }

    public String getGraphType() {
    	return weakGraphData.get().getGraphType();
    }

    public String getGraphXAxis() {
      return weakGraphData.get().getGraphXAxis();
    }

    public String getGraphYAxis() {
        return weakGraphData.get().getGraphYAxis();
    }

    public String getGraphRAxis() {
        return weakGraphData.get().getGraphRAxis();
    }

    public String getGraphOp() {
        return weakGraphData.get().getGraphOp();
    }

    public void saveSelection(String aspect, String value) {
    	weakGraphData.get().saveSelection(aspect, value);
    }


    public void deleteDefaultGraph() {
    	weakGraphData.get().deleteDefaultGraph();
    }
  }