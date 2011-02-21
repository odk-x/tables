package yoonsung.odk.spreadsheet.Activity.graphs;

import java.util.ArrayList;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.Database.TableProperty;
import yoonsung.odk.spreadsheet.Library.graphs.GraphClassifier;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.TabSpec;

public class GraphSetting extends Activity {

	public static final String[] TYPES = {"Line Graph", "Box-Stem Graph", "Pie Chart", "Map", "Calendar"};
	public static final String TAB_1 = "main";
	public static final String TAB_2 = "history";
	
	private TabHost myTabHost;
	//private String currentTab;
	
	private String tableID;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
        setContentView(R.layout.graph_setting);

        // Intent
        this.tableID = getIntent().getStringExtra("tableID");
        
        //currentTab = "main";
        
        myTabHost = (TabHost)this.findViewById(R.id.graph_setting_tabhost);
        myTabHost.setup();        
        
        /*
        myTabHost.setOnTabChangedListener(new OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabId) {
				reset();
			}
		});
        */
        
        TabSpec ts1 = createTab(this, "Prime/Main", TAB_1);
        TabSpec ts2 = createTab(this, "Secondary/History-in", TAB_2);
        
        myTabHost.addTab(ts1);
        myTabHost.addTab(ts2);
        
        myTabHost.setCurrentTab(0);
	}
	 	
	private TabSpec createTab(Context context, String tabName, String tag) {
		TabSpec ts = myTabHost.newTabSpec(tag);
        ts.setIndicator(tabName);                               
        ts.setContent(new TabHost.TabContentFactory(){
        	public View createTabContent(String tag) {                        
        		return createControlView("Graph Type:");
        	}              
        });    
        return ts;
	}
	
	private LinearLayout createControlView(String title) {
		final LinearLayout wrapper = new LinearLayout(this);
		wrapper.setOrientation(LinearLayout.VERTICAL);
		
		LinearLayout typeLL = new LinearLayout(this);
		
		final Context finContext = this;
		
		// Title for the spinner
		TextView typeTV = new TextView(this);
		typeTV.setText(title);
		
		// Spinner
		Spinner typeSpinner;
		if (myTabHost.getCurrentTabTag().equals(TAB_1)) {
			typeSpinner = createSpinner("ODKTables" + ":" + tableID + GraphClassifier.MAIN_VIEW + ":type", TYPES);
			Log.e("type key", "ODKTables" + ":" + tableID + GraphClassifier.MAIN_VIEW + ":type");
		} else {
			typeSpinner = createSpinner("ODKTables" + ":" + tableID + GraphClassifier.HISTORY_IN_VIEW + ":type", TYPES);
			Log.e("type key", "ODKTables" + ":" + tableID + GraphClassifier.HISTORY_IN_VIEW + ":type");
		}
		
		// Item chosen from the spinner
		typeSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
			 @Override
			 public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
				 LinearLayout axisLL = new LinearLayout(finContext);
				 axisLL.setOrientation(LinearLayout.VERTICAL);
				 
				 // Main or History-in & prefix
				 String prefix = "";
				 if (myTabHost.getCurrentTabTag().equals(TAB_1)) {
					 prefix = "ODKTables" + ":" + tableID + GraphClassifier.MAIN_VIEW;
				 } else {
					 prefix = "ODKTables" + ":" + tableID + GraphClassifier.HISTORY_IN_VIEW;
				 }
				  
				 // Type of graph
				 String selectedItem = arg0.getSelectedItem().toString();
				 
				 //  Get available columns & conversion
				 TableProperty tp = new TableProperty(tableID);
				 ArrayList<String> colOrder = tp.getColOrderArrayList();
				 String[] cols = new String[colOrder.size()];
				 for (int i = 0; i < colOrder.size(); i++) {
					 cols[i] = colOrder.get(i);
				 }
				 
				 // Share preference editor
				 SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(finContext);
			     SharedPreferences.Editor editor = settings.edit();
			     				 
				
				 if (selectedItem.equals("Line Graph")) {
					 //change graph type
					 editor.putString(prefix+":type", GraphClassifier.LINE_GRAPH);
					 editor.commit();
					 
					 // Set spinners for x and y
					 Spinner x = createSpinner(prefix+":col1", cols);
					 setOnItemSelectedListenerForSpinner(prefix+":col1", x);
					 axisLL.addView(x);
					 Spinner y = createSpinner(prefix+":col2", cols);
					 setOnItemSelectedListenerForSpinner(prefix+":col2", y);
					 axisLL.addView(y);
				 } else if (selectedItem.equals("Box-Stem Graph")) {
					//change graph type
					 editor.putString(prefix+":type", GraphClassifier.STEM_GRAPH);
					 editor.commit();
					 
					 // Set spinners for x and y
					 Spinner x = createSpinner(prefix+":col1", cols);
					 setOnItemSelectedListenerForSpinner(prefix+":col1", x);
					 axisLL.addView(x);
					 Spinner y = createSpinner(prefix+":col2", cols);
					 setOnItemSelectedListenerForSpinner(prefix+":col2", y);
					 axisLL.addView(y);
				 } else if(selectedItem.equals("Pie Chart")) {
					 editor.putString(prefix+":type", GraphClassifier.PIE_CHART);
					 editor.commit();
					 Spinner x = createSpinner(prefix+":col1", cols);
					 setOnItemSelectedListenerForSpinner(prefix+":col1", x);
					 axisLL.addView(x);
				 } else if (selectedItem.equals("Map")) {
					 //change graph type
					 editor.putString(prefix+":type", GraphClassifier.MAP);
					 editor.commit();
					 
					 // Set spinner for location
					 Spinner axis = createSpinner(prefix+"col1", cols);
					 setOnItemSelectedListenerForSpinner(prefix+"col1", axis);
					 axisLL.addView(axis);
				 } else if (selectedItem.equals("Calendar")) {
					//change graph type
					 editor.putString(prefix+":type", GraphClassifier.CALENDAR);
					 editor.commit();
					 
					 // Set spinners for x and y
					 Spinner x = createSpinner(prefix+":col1", cols);
					 setOnItemSelectedListenerForSpinner(prefix+"col1", x);
					 axisLL.addView(x);
					 Spinner y = createSpinner(prefix+":col2", cols);
					 setOnItemSelectedListenerForSpinner(prefix+":col2", y);
					 axisLL.addView(y);
				 }
				 
				 
				 // Fields spinners should not be duplicated
				 if (wrapper.getChildCount() > 1) {
					 wrapper.removeViewAt(1);
				 }
				 wrapper.addView(axisLL, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			 }

			 @Override
			 public void onNothingSelected(AdapterView<?> arg0) {
			 }
		});
		
		typeLL.addView(typeTV);
		typeLL.addView(typeSpinner);
		
		wrapper.addView(typeLL, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		
		return wrapper;
	}
	
	private Spinner createSpinner(String key, String[] spinnerChoices) {
		Spinner spinner = new Spinner(this);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
		            android.R.layout.simple_spinner_item, spinnerChoices);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		
		// Default select
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this); 
		Log.e("Create Spinner Key", key);
		String value = settings.getString(key, null);
		
		int position;
		if (value == null) {
			position = 0;
		} else {
			String item = value;
			if (value.equals(GraphClassifier.LINE_GRAPH)) {
				item = "Line Graph";
			} else if (value.equals(GraphClassifier.STEM_GRAPH)) {
				item = "Box-Stem Graph";
			} else if(value.equals(GraphClassifier.PIE_CHART)) {
				item = "Pie Chart";
			} else if (value.equals(GraphClassifier.MAP) ){
				item = "Map";
			} else if (value.equals(GraphClassifier.CALENDAR) ){
				item = "Calendar";
			}
			Log.e("Item", item);
			position = adapter.getPosition(item);
		}
		spinner.setSelection(position);
		Log.e("Create Spinner position", "" + position);
		
		return spinner;
	}
	
	private void setOnItemSelectedListenerForSpinner(String key, Spinner sp) {
		final String finKey = key;
		final Context finContext = this;
		
		sp.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
			 @Override
			 public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
				// Selected item
				String selectedItem = arg0.getSelectedItem().toString();
				
				// Share preference editor
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(finContext);
		        SharedPreferences.Editor editor = settings.edit();
		        editor.putString(finKey, selectedItem);

		        // Commit the edits!
		        editor.commit();
		        
			 }
			 
			 @Override
			 public void onNothingSelected(AdapterView<?> arg0) {
			 }
		 });
	}
	
}
