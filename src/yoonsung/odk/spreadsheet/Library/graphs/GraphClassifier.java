package yoonsung.odk.spreadsheet.Library.graphs;

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
