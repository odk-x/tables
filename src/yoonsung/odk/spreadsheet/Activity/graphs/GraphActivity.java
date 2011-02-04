package yoonsung.odk.spreadsheet.Activity.graphs;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class GraphActivity extends Activity {
	
	public static final int GRAPH_SETTING = 0;
	
	private String tableID;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Current Table
        this.tableID = getIntent().getStringExtra("tableID");
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, GRAPH_SETTING, 0, "Settings");
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        
    	switch(item.getItemId()) {
        	case GRAPH_SETTING:
        		Intent i = new Intent(this, GraphSetting.class);
        		i.putExtra("tableID", tableID);
        		startActivity(i);
        		return true;
    	}
    	
    	return super.onMenuItemSelected(featureId, item);
    }
}
