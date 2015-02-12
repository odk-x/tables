/*
 * Copyright (C) 2014 University of Washington
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
package org.opendatakit.tables.views.webkits;

import java.lang.ref.WeakReference;

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
	public boolean isModifiable() {
		return weakGraphData.get().isModifiable();
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
	public String getBoxSource() {
		return weakGraphData.get().getBoxSource();
	}
	
	// @JavascriptInterface
	public String getBoxValues() {
		return weakGraphData.get().getBoxValues();
	}
	
	// @JavascriptInterface
	public String getBoxIterations() {
		return weakGraphData.get().getBoxIterations();
	}
	
	// @JavascriptInterface
		public String getBoxOperation() {
			return weakGraphData.get().getBoxOperation();
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