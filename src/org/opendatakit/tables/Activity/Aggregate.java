package org.opendatakit.tables.Activity;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.Preferences;
import org.opendatakit.tables.sync.SyncProcessor;
import org.opendatakit.tables.sync.Synchronizer;
import org.opendatakit.tables.sync.TablesContentProvider;
import org.opendatakit.tables.sync.aggregate.AggregateSynchronizer;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * An activity for downloading from and uploading to an ODK Aggregate instance.
 * 
 * @author hkworden@gmail.com
 */
public class Aggregate extends Activity {

  private static final String ACCOUNT_TYPE_G = "com.google";
  private static final String URI_FIELD_EMPTY = "http://";

  private EditText uriField;
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
    initializeData();
  }

  @Override
  protected void onPause() {
    super.onPause();
    saveSettings();
  }

  public void saveSettings() {
    // save fields in preferences
    String uri = uriField.getText().toString();
    if (uri.equals(URI_FIELD_EMPTY))
      uri = null;
    String accountName = (String) accountListSpinner.getSelectedItem();

    prefs.setServerUri(uri);
    prefs.setAccount(accountName);

    // set account sync properties
    Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE_G);
    for (Account account : accounts) {
      if (account.name.equals(accountName)) {
        ContentResolver.setIsSyncable(account, TablesContentProvider.AUTHORITY, 1);
        ContentResolver.setSyncAutomatically(account, TablesContentProvider.AUTHORITY, true);
      } else {
        ContentResolver.setSyncAutomatically(account, TablesContentProvider.AUTHORITY, false);
        ContentResolver.setIsSyncable(account, TablesContentProvider.AUTHORITY, 0);
      }
    }
  }

  private void findViewComponents() {
    uriField = (EditText) findViewById(R.id.aggregate_activity_uri_field);
    accountListSpinner = (Spinner) findViewById(R.id.aggregate_activity_account_list_spinner);
  }

  private void initializeData() {
    // Add accounts to spinner
    Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE_G);
    List<String> accountNames = new ArrayList<String>(accounts.length);
    for (int i = 0; i < accounts.length; i++)
      accountNames.add(accounts[i].name);

    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
        android.R.layout.select_dialog_item, accountNames);
    accountListSpinner.setAdapter(adapter);

    // Set saved server url
    String serverUri = prefs.getServerUri();

    if (serverUri == null)
      uriField.setText(URI_FIELD_EMPTY);
    else
      uriField.setText(serverUri);

    // Set chosen account
    String accountName = prefs.getAccount();
    if (accountName != null) {
      int index = accountNames.indexOf(accountName);
      accountListSpinner.setSelection(index);
    }
  }

  /**
   * Hooked up to save settings button in aggregate_activity.xml
   */
  public void saveSettings(View v) {
    saveSettings();
    Intent i = new Intent(this, AccountInfoActivity.class);
    Account account = new Account(prefs.getAccount(), ACCOUNT_TYPE_G);
    i.putExtra(AccountInfoActivity.INTENT_EXTRAS_ACCOUNT, account);
    startActivity(i);
  }

  /**
   * Hooked to chooseTablesButton's onClick in aggregate_activity.xml
   */
  public void chooseTables(View v) {
    Intent i = new Intent(this, AggregateChooseTablesActivity.class);
    startActivity(i);
  }

  /**
   * Hooked up to downloadTableButton's onClick in aggregate_activity.xml
   */
  public void downloadTableFromServer(View v) {
    Intent i = new Intent(this, AggregateDownloadTableActivity.class);
    startActivity(i);
  }

  /**
   * Hooked to syncNowButton's onClick in aggregate_activity.xml
   */
  public void syncNow(View v) {
    String accountName = prefs.getAccount();

    if (accountName == null) {
      Toast.makeText(this, "Please choose an account", Toast.LENGTH_SHORT).show();
    } else {
      // requestSync(accountName);
      // Toast.makeText(this, "Sync started.", Toast.LENGTH_SHORT);
      SyncNowTask syncTask = new SyncNowTask();
      syncTask.execute();
    }
  }

  public static void requestSync(String accountName) {
    if (accountName != null) {
      Account account = new Account(accountName, ACCOUNT_TYPE_G);
      ContentResolver.requestSync(account, TablesContentProvider.AUTHORITY, new Bundle());
    }
  }

  private class SyncNowTask extends AsyncTask<Void, Void, Void> {
    private ProgressDialog pd;

    @Override
    protected void onPreExecute() {
      pd = ProgressDialog.show(Aggregate.this, "Please Wait", "Synchronizing...");
    }

    @Override
    protected Void doInBackground(Void... params) {
      DbHelper dbh = DbHelper.getDbHelper(Aggregate.this);
      Synchronizer synchronizer = new AggregateSynchronizer(prefs.getServerUri(),
          prefs.getAuthToken());
      SyncProcessor processor = new SyncProcessor(synchronizer, new DataManager(dbh),
          new SyncResult());
      processor.synchronize();
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      pd.dismiss();
    }

  }
}
