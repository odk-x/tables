package yoonsung.odk.spreadsheet.Activity;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.data.Preferences;
import yoonsung.odk.spreadsheet.sync.TablesContentProvider;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An activity for downloading from and uploading to an ODK Aggregate instance.
 * 
 * @author hkworden@gmail.com
 */
public class Aggregate extends Activity {

	private static final String ACCOUNT_TYPE_G = "com.google";

	private TextView uriLabel;
	private EditText uriField;
	private TextView usernameLabel;
	private EditText usernameField;
	private Button saveButton;
	private Spinner accountListSpinner;

	private AccountManager accountManager;
	private Preferences prefs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		accountManager = AccountManager.get(this);
		prefs = new Preferences(this);

		setTitle("ODK Tables > Sync");
		setContentView(R.layout.aggregate_activity);
		findViewComponents();
		setViewForGettingUserInfo();
		initClickHandlers();
		initializeData();
	}

	private void findViewComponents() {
		uriLabel = (TextView) findViewById(R.id.aggregate_activity_uri_label);
		uriField = (EditText) findViewById(R.id.aggregate_activity_uri_field);
		usernameLabel = (TextView) findViewById(R.id.aggregate_activity_username_label);
		usernameField = (EditText) findViewById(R.id.aggregate_activity_username_field);
		saveButton = (Button) findViewById(R.id.aggregate_activity_save_button);
		accountListSpinner = (Spinner) findViewById(R.id.aggregate_activity_account_list_spinner);
	}

	private void setViewForGettingUserInfo() {
		uriLabel.setVisibility(View.VISIBLE);
		uriField.setVisibility(View.VISIBLE);
		usernameLabel.setVisibility(View.VISIBLE);
		usernameField.setVisibility(View.VISIBLE);
		saveButton.setVisibility(View.VISIBLE);
		accountListSpinner.setVisibility(View.VISIBLE);
	}

	private void initClickHandlers() {
		saveButton.setOnClickListener(new SaveAggregateSettingsListener());

		Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE_G);
		String[] accountNames = new String[accounts.length];
		for (int i = 0; i < accountNames.length; i++)
			accountNames[i] = accounts[i].name;

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, accountNames);
		accountListSpinner.setAdapter(adapter);
	}

	private class SaveAggregateSettingsListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			String uri = uriField.getText().toString();
			String username = usernameField.getText().toString();
			prefs.setAggregateUri(uri);
			prefs.setAggregateUsername(username);

			String accountName = (String) accountListSpinner.getSelectedItem();
			Account[] accounts = accountManager.getAccountsByType("com.google");
			for (Account account : accounts) {
				if (account.name.equals(accountName)) {
					ContentResolver.setIsSyncable(account,
							TablesContentProvider.AUTHORITY, 1);
					ContentResolver.setSyncAutomatically(account,
							TablesContentProvider.AUTHORITY, true);
				} else {
					ContentResolver.setSyncAutomatically(account,
							TablesContentProvider.AUTHORITY, false);
					ContentResolver.setIsSyncable(account,
							TablesContentProvider.AUTHORITY, 0);
				}
			}
			Toast.makeText(Aggregate.this, "Saved", Toast.LENGTH_LONG).show();
			Aggregate.this.finish();
		}
	}

	private void initializeData() {
		String aggregateUri = prefs.getAggregateUri();
		String aggregateUsername = prefs.getAggregateUsername();

		if (aggregateUri == null)
			uriField.setText("http://");
		else
			uriField.setText(aggregateUri);

		if (aggregateUsername != null)
			usernameField.setText(aggregateUsername);

		setCurrentSyncAccount();
	}

	private void setCurrentSyncAccount() {
		Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE_G);
		for (int i = 0; i < accounts.length; i++) {
			Account account = accounts[i];
			int isSync = ContentResolver.getIsSyncable(account,
					TablesContentProvider.AUTHORITY);
			boolean syncAuto = ContentResolver.getSyncAutomatically(account,
					TablesContentProvider.AUTHORITY);
			if (isSync > 0 && syncAuto) {
				accountListSpinner.setSelection(i);
			}
		}

	}
}
