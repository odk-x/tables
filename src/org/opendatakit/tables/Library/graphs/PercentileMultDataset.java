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

import org.achartengine.model.XYMultipleSeriesDataset;

public class PercentileMultDataset extends XYMultipleSeriesDataset {
	
	private static final long serialVersionUID = 1L;
	
	private List<PercentileSeries> mSeries = new ArrayList<PercentileSeries>();
	
	public void addSeries(PercentileSeries series) {
		mSeries.add(series);
	}
	
	public void removeSeries(int index) {
		mSeries.remove(index);
	}

	public void removeSeries(PercentileSeries series) {
		mSeries.remove(series);
	}

	public PercentileSeries getSeriesAt(int index) {
		return mSeries.get(index);
	}

	public int getSeriesCount() {
		return mSeries.size();
	}

	public PercentileSeries[] getSeries() {
		return mSeries.toArray(new PercentileSeries[0]);
	}
	
}
