package org.opendatakit.tables.views.webkits;

import java.lang.ref.WeakReference;

import org.opendatakit.tables.views.webkits.CustomGraphView.GraphData;

public class GraphDataIf {
	private WeakReference<GraphData> weakGraphData;

	GraphDataIf(GraphData graphData) {
		this.weakGraphData = new WeakReference<GraphData>(graphData);
	}

	// @JavascriptInterface
	public boolean isModified() {
		return weakGraphData.get().isModified();
	}

	// @JavascriptInterface
	public boolean hasGraph(String graph) {
		return weakGraphData.get().hasGraph(graph);
	}

	// @JavascriptInterface
	public String getGraphType() {
		return weakGraphData.get().getGraphType();
	}

	// @JavascriptInterface
	public String getGraphXAxis() {
		return weakGraphData.get().getGraphXAxis();
	}

	// @JavascriptInterface
	public String getGraphYAxis() {
		return weakGraphData.get().getGraphYAxis();
	}

	// @JavascriptInterface
	public String getGraphRAxis() {
		return weakGraphData.get().getGraphRAxis();
	}

	// @JavascriptInterface
	public String getGraphOp() {
		return weakGraphData.get().getGraphOp();
	}

	// @JavascriptInterface
	public void saveSelection(String aspect, String value) {
		weakGraphData.get().saveSelection(aspect, value);
	}

	// @JavascriptInterface
	public void deleteDefaultGraph() {
		weakGraphData.get().deleteDefaultGraph();
	}
}