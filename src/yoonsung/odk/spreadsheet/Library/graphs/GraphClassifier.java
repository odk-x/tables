package yoonsung.odk.spreadsheet.Library.graphs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class GraphClassifier {
	
	public static final String MAIN_VIEW_PREFIX = "main:";
	public static final String HISTORY_IN_VIEW_PREFIX = "history:";
		
	public static final String LINE_GRAPH = "line";
	public static final String STEM_GRAPH = "stem";
	public static final String MAP = "map";
	
	private boolean isMain;

	private String graphType;
	private String colOne;
	private String colTwo;
	
	public GraphClassifier(Context context, boolean isMain) {
		this.isMain = isMain;
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context); 
		
		// Get preference on the graph type and columns
		String prefix = getPrefix();
	    this.graphType = settings.getString(prefix + "type", null);
	    this.colOne = settings.getString(prefix + "col1", null);
	    this.colTwo = settings.getString(prefix + "col2", null);
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
	
	private String getPrefix() {
		// Prefix for share preference key
		String prefix = "";
		if (isMain) {
			prefix = MAIN_VIEW_PREFIX;
		} else {
			prefix = HISTORY_IN_VIEW_PREFIX;
		}
		return prefix;
	}
	
}
