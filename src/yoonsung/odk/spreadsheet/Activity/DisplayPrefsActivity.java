package yoonsung.odk.spreadsheet.Activity;

import yoonsung.odk.spreadsheet.DataStructure.Table;
import yoonsung.odk.spreadsheet.Database.Data;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * An activity for setting display preferences.
 * 
 * TODO: have options for each table
 */
public class DisplayPrefsActivity extends Activity {
	
	/** the shared preferences manager */
	private SharedPreferences.Editor editor;
	
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences settings =
        	PreferenceManager.getDefaultSharedPreferences(this);
		editor = settings.edit();
    	// preparing the content view
    	Table table = (new Data()).getTable();
    	TableLayout v = new TableLayout(this);
    	for(int i=0; i<table.getWidth(); i++) {
    		TableRow row = new TableRow(this);
    		String colName = table.getColName(i);
    		TextView label = new TextView(this);
    		label.setText(colName);
    		row.addView(label);
    		EditText et = new EditText(this);
    		et.setText(settings.getString("tablewidths-table-" + colName,
    				"125"));
    		et.addTextChangedListener(new ETListener(colName));
    		row.addView(et);
    		v.addView(row);
    	}
        setContentView(v);
    }
    
    /**
     * A listener for changes to the width fields.
     */
    private class ETListener implements TextWatcher {
    	private String colName;
    	ETListener(String colName) {
    		this.colName = colName;
    	}
		@Override
		public void afterTextChanged(Editable s) {
			try {
				editor.putInt("tablewidths-table-" + colName,
						new Integer(s.toString()));
				editor.commit();
			} catch(NumberFormatException e) {
				// TODO: something here
			}
		}
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {}
		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {}
    }
	
}
