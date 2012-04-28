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

import org.achartengine.model.XYSeries;
import org.achartengine.util.MathHelper;

public class PercentileSeries extends XYSeries {
	
	private static final long serialVersionUID = 1L;
	
	private double mYMin;
	private double mYMax;
	private List<Double> mLow = new ArrayList<Double>();
	private List<Double> mMidlow = new ArrayList<Double>();
	private List<Double> mMidhigh = new ArrayList<Double>();
	private List<Double> mHigh = new ArrayList<Double>();
	
	protected PercentileSeries(String title) {
		super(title);
		mYMin = MathHelper.NULL_VALUE;
		mYMax = -MathHelper.NULL_VALUE;
	}
	
	public void add(double x, double y, double low, double midlow,
			double midhigh, double high) {
		super.add(x, y);
		mLow.add(low);
		mMidlow.add(midlow);
		mMidhigh.add(midhigh);
		mHigh.add(high);
		mYMin = Math.min(mYMin, low);
		mYMax = Math.max(mYMax, high);
	}
	
	public double getMinY() {
		return mYMin;
	}
	
	public double getMaxY() {
		return mYMax;
	}
	
	public double getLow(int index) {
		return mLow.get(index);
	}
	
	public double getMidlow(int index) {
		return mMidlow.get(index);
	}
	
	public double getMidhigh(int index) {
		return mMidhigh.get(index);
	}
	
	public double getHigh(int index) {
		return mHigh.get(index);
	}
	
}
