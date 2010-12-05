package yoonsung.odk.spreadsheet.Activity.defaultopts;

import yoonsung.odk.spreadsheet.R;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class DefaultsActivity extends TabActivity {
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.defaultoptions_view);
		TabHost th = getTabHost();
		TabHost.TabSpec spec;
		Intent intent;
		// the addition defaults
		intent = new Intent().setClass(this, AdditionDefaults.class);
		spec = th.newTabSpec("adds");
		spec.setIndicator("Addition");
		spec.setContent(intent);
		th.addTab(spec);
		// the query defaults
		intent = new Intent().setClass(this, QueryDefaults.class);
		spec = th.newTabSpec("queries");
		spec.setIndicator("Querying");
		spec.setContent(intent);
		th.addTab(spec);
	}
	
}
