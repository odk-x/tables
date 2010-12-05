package yoonsung.odk.spreadsheet.Activity.defaultopts;

import java.util.ArrayList;
import java.util.List;
import yoonsung.odk.spreadsheet.Database.ColumnProperty;
import yoonsung.odk.spreadsheet.Database.DefaultsManager;
import yoonsung.odk.spreadsheet.Database.TableProperty;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class AdditionDefaults extends Activity {
	
	private ColumnProperty cp;
	private DefaultsManager dm;
	private TableProperty tp;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		cp = new ColumnProperty();
		dm = new DefaultsManager();
		tp = new TableProperty();
		ScrollView sv = new ScrollView(this);
		sv.addView(getView());
		setContentView(sv);
	}
	
	private View getView() {
		TableLayout table = new TableLayout(this);
		addAvailOpt(table);
		return table;
	}
	
	/**
	 * Adds the availability option to the table if there are any daterange
	 * columns.
	 * @param table the table view to add to
	 */
	private void addAvailOpt(TableLayout table) {
		TableRow.LayoutParams lp = new TableRow.LayoutParams(
				TableRow.LayoutParams.FILL_PARENT,
				TableRow.LayoutParams.WRAP_CONTENT, 1);
		lp.span = 2;
		// forming the spinner
		List<String> drCols = new ArrayList<String>();
		List<String> cols = tp.getColOrderArrayList();
		for(String colname : cols) {
			if(("Date Range").equals(cp.getType(colname))) {
				drCols.add(colname);
			}
		}
		if(drCols.isEmpty()) {return;}
		drCols.add(0, "None");
		Spinner drSpin = new Spinner(this);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item,
				drCols.toArray(new String[0]));
		adapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);
		drSpin.setAdapter(adapter);
		int pos = drCols.indexOf(dm.getAddAvailCol());
		if(pos < 0) {pos = 0;}
		drSpin.setSelection(pos);
		drSpin.setOnItemSelectedListener(new DRAListener(dm));
		// forming and adding the label
		TextView drLabel = new TextView(this);
		drLabel.setText("Restrict by availability in:");
		TableRow labelRow = new TableRow(this);
		drLabel.setLayoutParams(lp);
		labelRow.addView(drLabel);
		table.addView(labelRow);
		// adding the table row
		drSpin.setLayoutParams(lp);
		TableRow spinRow = new TableRow(this);
		spinRow.addView(drSpin);
		table.addView(spinRow);
	}
	
	private class DRAListener implements AdapterView.OnItemSelectedListener {
		DefaultsManager dm;
		protected DRAListener(DefaultsManager dm) {
			this.dm = dm;
		}
		@Override
		public void onItemSelected(AdapterView<?> parent, View view,
				int position, long id) {
			if(position == 0) {
				update("");
			} else {
				update(parent.getItemAtPosition(position).toString());
			}
		}
		@Override
		public void onNothingSelected(AdapterView<?> parent) {
			update("");
		}
		private void update(String val) {
			dm.setQueryAvailCol(val);
		}
	}
	
}