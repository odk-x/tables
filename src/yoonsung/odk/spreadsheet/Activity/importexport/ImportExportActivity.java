package yoonsung.odk.spreadsheet.Activity.importexport;

import yoonsung.odk.spreadsheet.R;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

/**
 * An activity for importing and exporting tables.
 */
public class ImportExportActivity extends TabActivity {
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.importexport_view);
		TabHost th = getTabHost();
		TabHost.TabSpec spec;
		Intent intent;
		// the import CSV option
		intent = new Intent().setClass(this, ImportCSVActivity.class);
		spec = th.newTabSpec("importcsv");
		spec.setIndicator("Import CSV");
		spec.setContent(intent);
		th.addTab(spec);
		// the export CSV option
		intent = new Intent().setClass(this, ExportCSVActivity.class);
		spec = th.newTabSpec("exportcsv");
		spec.setIndicator("Export CSV");
		spec.setContent(intent);
		th.addTab(spec);
	}
	
}
