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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class GraphClassifier {
	
	public static final String MAIN_VIEW  = ":main";
	public static final String HISTORY_IN_VIEW = ":history";
		
	public static final String LINE_GRAPH = "line";
	public static final String STEM_GRAPH = "stem";
	public static final String PIE_CHART = "pie";
	public static final String MAP = "map";
	public static final String CALENDAR = "calendar";
	
	private String tableID;
	private boolean isMain;
	
	private String graphType;
	private String colOne;
	private String colTwo;
	
	public GraphClassifier(Context context, String tableID, boolean isMain) {
		this.isMain = isMain;
		this.tableID = tableID;
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context); 
		
		// Get preference on the graph type and columns
		String prefix = getPrefix(tableID);
	    this.graphType = settings.getString(prefix + ":type", null);
	    this.colOne = settings.getString(prefix + ":col1", null);
	    this.colTwo = settings.getString(prefix + ":col2", null);
	}
	
	public String getGraphType() {
		return this.graphType;
	}
	
	public String getColOne() {
		return this.colOne;
	}
	
	public String getColTwo() {
		return this.colTwo;
	}
	
	private String getPrefix(String tableID) {
		// Prefix for share preference key
		String prefix = "";
		if (isMain) {
			prefix = "ODKTables" + ":" + tableID + MAIN_VIEW;
		} else {
			prefix = "ODKTables" + ":" + tableID + HISTORY_IN_VIEW;
		}
		return prefix;
	}
	
}
