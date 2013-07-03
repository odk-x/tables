/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
import org.opendatakit.tables.sync.exceptions.InvalidAuthTokenException;
import org.opendatakit.tables.sync.files.FileSyncAdapter;

import com.actionbarsherlock.app.SherlockActivity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SyncResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * An activity for downloading from and uploading to an ODK Aggregate instance.
 * 
 * @author hkworden@gmail.com
 * @author the.dylan.price@gmail.com
 */
public class Aggregate extends SherlockActivity {
  
  public static final String TAG = "Aggregate--Activity";

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

    setTitle("");
    setContentView(R.layout.aggregate_activity);
    findViewComponents();
    initializeData();
    updateButtonsEnabled();
  }
  
  @Override
  protected void onStart() {
    super.onStart();
    updateButtonsEnabled();
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    updateButtonsEnabled();
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

  private void updateButtonsEnabled() {
    String accountName = prefs.getAccount();
    String serverUri = prefs.getServerUri();
    boolean haveSettings = (accountName != null) && (serverUri != null);
    boolean authorizeAccount = prefs.getAuthToken() == null;

    boolean restOfButtons = haveSettings && !authorizeAccount;

    findViewById(R.id.aggregate_activity_save_settings_button).setEnabled(true);
    findViewById(R.id.aggregate_activity_authorize_account_button).setEnabled(authorizeAccount);
    findViewById(R.id.aggregate_activity_choose_tables_button).setEnabled(restOfButtons);
    findViewById(R.id.aggregate_activity_get_table_button).setEnabled(restOfButtons);
    findViewById(R.id.aggregate_activity_sync_now_button).setEnabled(restOfButtons);
  }

  private void saveSettings() {

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

  private AlertDialog.Builder buildOkMessage(String title, String message) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setCancelable(false);
    builder.setPositiveButton("OK", null);
    builder.setTitle(title);
    builder.setMessage(message);
    return builder;
  }

  /**
   * Hooked up to save settings button in aggregate_activity.xml
   */
  public void onClickSaveSettings(View v) {
    // show warning message
    AlertDialog.Builder msg = buildOkMessage("Are you sure?",
        "If you change your settings, tables you have synched now "
            + "may no longer be able to be synched.");

    msg.setPositiveButton("Save", new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        saveSettings();
        // SS Oct 15: clear the auth token here.
        // TODO if you change a user you can switch to their privileges without
        // this.
        invalidateAuthToken(prefs.getAuthToken(), Aggregate.this);
        updateButtonsEnabled();
      }
    });

    msg.setNegativeButton("Cancel", null);
    msg.show();
  }

  /**
   * Hooked up to authorizeAccountButton's onClick in aggregate_activity.xml
   */
  public void onClickAuthorizeAccount(View v) {
    Intent i = new Intent(this, AccountInfoActivity.class);
    Account account = new Account(prefs.getAccount(), ACCOUNT_TYPE_G);
    i.putExtra(AccountInfoActivity.INTENT_EXTRAS_ACCOUNT, account);
    startActivity(i);
    updateButtonsEnabled();
  }

  /**
   * Hooked to chooseTablesButton's onClick in aggregate_activity.xml
   */
  public void onClickChooseTables(View v) {
    Intent i = new Intent(this, AggregateChooseTablesActivity.class);
    startActivity(i);
    updateButtonsEnabled();
  }

  /**
   * Hooked up to downloadTableButton's onClick in aggregate_activity.xml
   */
  public void onClickDownloadTableFromServer(View v) {
    Intent i = new Intent(this, AggregateDownloadTableActivity.class);
    startActivity(i);
    updateButtonsEnabled();
  }

  /**
   * Hooked to syncNowButton's onClick in aggregate_activity.xml
   */
  public void onClickSyncNow(View v) {
    Log.d(TAG, "in onClickSyncNow");
    String accountName = prefs.getAccount();

    if (accountName == null) {
      Toast.makeText(this, "Please choose an account", Toast.LENGTH_SHORT).show();
    } else {
      SyncNowTask syncTask = new SyncNowTask();
      syncTask.execute();
    }
    updateButtonsEnabled();
  }
  
  /**
   * Hooked to syncFilesNowButton's onClick in aggregate_activity.xml
   * @param accountName
   */
  public void onClickSyncFilesNow(View v) {
    Log.d(TAG, "in onClickSyncFilesNow");
    String accountName = prefs.getAccount();
    //Intent i = new Intent()
    if (accountName == null) {
      Toast.makeText(this, "Please choose an account", Toast.LENGTH_SHORT).show();
    } else {
      Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE_G);
      for (Account account : accounts) {
        if (account.name.equals(accountName)) {
          Bundle extras = new Bundle();
          ContentResolver.setIsSyncable(account, "org.opendatakit.tables.tablefilesauthority", 1);
          ContentResolver.setSyncAutomatically(account, "org.opendatakit.tables.tablefilesauthority", true);
          ContentResolver.requestSync(account,
              "org.opendatakit.tables.tablefilesauthority", extras);         
        }
      }

    }
  }

  public static void requestSync(String accountName) {
    if (accountName != null) {
      Account account = new Account(accountName, ACCOUNT_TYPE_G);
      ContentResolver.requestSync(account, TablesContentProvider.AUTHORITY, new Bundle());
    }
  }

  public static void invalidateAuthToken(String authToken, Context context) {
    AccountManager.get(context).invalidateAuthToken(ACCOUNT_TYPE_G, authToken);
    Preferences prefs = new Preferences(context);
    prefs.setAuthToken(null);
  }

  private class SyncNowTask extends AsyncTask<Void, Void, Void> {
    private ProgressDialog pd;
    private boolean success;
    private String message;

    @Override
    protected void onPreExecute() {
      pd = ProgressDialog.show(Aggregate.this, "Please Wait", "Synchronizing...");
      success = false;
      message = null;
    }

    @Override
    protected Void doInBackground(Void... params) {
      try {
        DbHelper dbh = DbHelper.getDbHelper(Aggregate.this);
        Synchronizer synchronizer = new AggregateSynchronizer(prefs.getServerUri(),
            prefs.getAuthToken());
        SyncProcessor processor = new SyncProcessor(synchronizer, new DataManager(dbh),
            new SyncResult());
        processor.synchronize();
        success = true;
      } catch (InvalidAuthTokenException e) {
        invalidateAuthToken(prefs.getAuthToken(), Aggregate.this);
        success = false;
        message = "Authorization expired. Please re-authorize account.";
      } catch (Exception e) {
        success = false;
        message = e.getMessage();
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      pd.dismiss();
      if (!success && message != null) {
        buildOkMessage("Sync Error", message).show();
      }
      updateButtonsEnabled();
    }

  }
  
  /*
   * Hopefully the task for syncing files. Modeled on SyncNowTask.
   */
  private class SyncFilesNowTask extends AsyncTask<Void, Void, Void> {
    private ProgressDialog pd;
    private boolean success;
    private String message;
    
    @Override
    protected void onPreExecute() {
      pd = ProgressDialog.show(Aggregate.this, "Please Wait", 
          "Synchonizing files...");
      success = false;
      message = null;
    }
    
    @Override
    protected Void doInBackground(Void... params) {
      try {
        // first see if the server is null. For a while this was a bug in the
        // above code.
        if (prefs.getServerUri() == null) {
          message = "Please save settings first.";
          return null;
        } 
        FileSyncAdapter syncAdapter = new FileSyncAdapter(
            getApplicationContext(), true);
        return null;
      }  finally{
        
      }
    }
  }
}
