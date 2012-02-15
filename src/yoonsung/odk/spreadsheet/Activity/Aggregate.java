package yoonsung.odk.spreadsheet.Activity;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.data.Preferences;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * An activity for downloading from and uploading to an ODK Aggregate instance.
 * 
 * @author hkworden@gmail.com
 */
public class Aggregate extends Activity {

	private TextView uriLabel;
	private EditText uriField;
	private TextView usernameLabel;
	private EditText usernameField;
	private Button saveButton;

	private Preferences prefs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prefs = new Preferences(this);
		setTitle("ODK Tables > Sync");
		setContentView(R.layout.aggregate_activity);
		findViewComponents();
		setViewForGettingUserInfo();
		initClickHandlers();
	}

	private void findViewComponents() {
		uriLabel = (TextView) findViewById(R.id.aggregate_activity_uri_label);
		uriField = (EditText) findViewById(R.id.aggregate_activity_uri_field);
		usernameLabel = (TextView) findViewById(R.id.aggregate_activity_username_label);
		usernameField = (EditText) findViewById(R.id.aggregate_activity_username_field);
		saveButton = (Button) findViewById(R.id.aggregate_activity_save_button);
	}

	private void setViewForGettingUserInfo() {
		uriLabel.setVisibility(View.VISIBLE);
		uriField.setVisibility(View.VISIBLE);
		usernameLabel.setVisibility(View.VISIBLE);
		usernameField.setVisibility(View.VISIBLE);
	}

	private void initClickHandlers() {
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				handleSave();
			}
		});
	}

	private void handleSave() {
		String uri = uriField.getText().toString();
		String username = usernameField.getText().toString();
		prefs.setAggregateUri(uri);
		prefs.setAggregateUsername(username);
	}
}
