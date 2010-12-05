package yoonsung.odk.spreadsheet.Activity.defaultopts;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class AdditionDefaults extends Activity {
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TextView tv = new TextView(this);
		tv.setText("addition defaults");
		setContentView(tv);
	}
	
}