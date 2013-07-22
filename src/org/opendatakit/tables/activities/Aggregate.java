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
package org.opendatakit.tables.activities;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.opendatakit.aggregate.odktables.rest.interceptor.AggregateRequestInterceptor;
import org.opendatakit.tables.R;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.Preferences;
import org.opendatakit.tables.sync.SyncProcessor;
import org.opendatakit.tables.sync.SyncUtil;
import org.opendatakit.tables.sync.SynchronizationResult;
import org.opendatakit.tables.sync.Synchronizer;
import org.opendatakit.tables.sync.TableResult;
import org.opendatakit.tables.sync.TablesContentProvider;
import org.opendatakit.tables.sync.aggregate.AggregateSynchronizer;
import org.opendatakit.tables.sync.exceptions.InvalidAuthTokenException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

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

import com.actionbarsherlock.app.SherlockActivity;

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
    builder.setPositiveButton(getString(R.string.ok), null);
    builder.setTitle(title);
    builder.setMessage(message);
    return builder;
  }

  private AlertDialog.Builder buildResultMessage(String title,
      SynchronizationResult result) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setCancelable(false);
    builder.setPositiveButton(getString(R.string.ok), null);
    builder.setTitle(title);
    // Now we'll make the message. This should include the contents of the
    // result parameter.
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < result.getTableResults().size(); i++) {
      TableResult tableResult = result.getTableResults().get(i);
      stringBuilder.append(SyncUtil.getMessageForTableResult(this,
          tableResult));
//      stringBuilder.append(tableResult.getTableDisplayName() + ": " +
//        SyncUtil.getLocalizedNameForTableResultStatus(this,
//            tableResult.getStatus()));
//      if (tableResult.getStatus() == Status.EXCEPTION) {
//        stringBuilder.append(" with message: " + tableResult.getMessage());
//      }
      if (i < result.getTableResults().size() - 1) {
        // only append if we have a
        stringBuilder.append("\n");
      }
    }
    builder.setMessage(stringBuilder.toString());
    return builder;
  }

  /**
   * Hooked up to save settings button in aggregate_activity.xml
   */
  public void onClickSaveSettings(View v) {
    // show warning message
    AlertDialog.Builder msg = buildOkMessage(getString(R.string.confirm_change_settings),
        getString(R.string.change_settings_warning));

    msg.setPositiveButton(getString(R.string.save), new OnClickListener() {
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

    msg.setNegativeButton(getString(R.string.cancel), null);
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
      Toast.makeText(this, getString(R.string.choose_account), Toast.LENGTH_SHORT).show();
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
      Toast.makeText(this, getString(R.string.choose_account), Toast.LENGTH_SHORT).show();
    } else {
      SyncFilesNowTask syncFilesTask = new SyncFilesNowTask();
      syncFilesTask.execute();
//      Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE_G);
//      for (Account account : accounts) {
//        if (account.name.equals(accountName)) {
//          Bundle extras = new Bundle();
//          ContentResolver.setIsSyncable(account, "org.opendatakit.tables.tablefilesauthority", 1);
//          ContentResolver.setSyncAutomatically(account, "org.opendatakit.tables.tablefilesauthority", true);
//          ContentResolver.requestSync(account,
//              "org.opendatakit.tables.tablefilesauthority", extras);
//        }
//      }

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

  private class SyncNowTask extends
      AsyncTask<Void, Void, SynchronizationResult> {
    private ProgressDialog pd;
    private boolean success;
    private String message;

    @Override
    protected void onPreExecute() {
      pd = ProgressDialog.show(Aggregate.this, getString(R.string.please_wait),
          getString(R.string.synchronizing));
      success = false;
      message = null;
    }

    @Override
    protected SynchronizationResult doInBackground(Void... params) {
      SynchronizationResult result = null;
      try {
        DbHelper dbh = DbHelper.getDbHelper(Aggregate.this);
        Synchronizer synchronizer = new AggregateSynchronizer(prefs.getServerUri(),
            prefs.getAuthToken());
        SyncProcessor processor = new SyncProcessor(dbh,
            synchronizer, new SyncResult());
        result = processor.synchronize();
        success = true;
      } catch (InvalidAuthTokenException e) {
        invalidateAuthToken(prefs.getAuthToken(), Aggregate.this);
        success = false;
        message = getString(R.string.auth_expired);
      } catch (Exception e) {
        success = false;
        message = e.getMessage();
      }
      return result;
    }

    @Override
    protected void onPostExecute(SynchronizationResult result) {
      pd.dismiss();
      if (!success && message != null) {
        buildOkMessage(getString(R.string.sync_error), message).show();
      } else {
        buildResultMessage(getString(R.string.sync_result), result).show();
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
      pd = ProgressDialog.show(Aggregate.this, getString(R.string.please_wait),
          getString(R.string.synchronizing_files));
      success = false;
      message = null;
    }

    @Override
    protected Void doInBackground(Void... params) {
      try {
        // first see if the server is null. For a while this was a bug in the
        // above code.
        if (prefs.getServerUri() == null) {
          message = getString(R.string.save_settings_first);
          return null;
        }
        String aggregateUri = prefs.getServerUri(); // uri of our server.
        URI uri = URI.create(aggregateUri).normalize();
        uri = uri.resolve("/odktables/files/").normalize();
        URI fileServletUri = uri;
        List<ClientHttpRequestInterceptor> interceptors =
            new ArrayList<ClientHttpRequestInterceptor>();
        String accessToken = prefs.getAuthToken();
        
        interceptors.add(new AggregateRequestInterceptor(accessToken));
//        ClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
//        ClientHttpRequest request = 
//            factory.createRequest(fileServletUri, HttpMethod.POST);
        String filePath = "tables/helloServer.txt"; // just a hardcoded dummy
        File file = new File("/sdcard/odk/" + filePath);
        URI filePostUri = fileServletUri.resolve(filePath).normalize();
        // from http://agilesc.barryku.com/?p=243
//        MultiValueMap<String, Object> parts = 
//            new LinkedMultiValueMap<String, Object>();
//        parts.add(filePath, new FileSystemResource(file));
        RestTemplate rt = SyncUtil.getRestTemplateForFiles();
        String response = rt.postForObject(filePostUri, new FileSystemResource(file), String.class);
        int i = 3; // just to trigger the debugger.
        
        
        
        
        
        
       
//        Serializer serializer = SimpleXMLSerializerForAggregate.getSerializer();
//        List<HttpMessageConverter<?>> converters =
//            new ArrayList<HttpMessageConverter<?>>();
//        converters.add(new JsonObjectHttpMessageConverter());
//        converters.add(new SimpleXmlHttpMessageConverter(serializer));
//        rt.setMessageConverters(converters);
//        MultiValueMap<String, Object> map = 
//            new LinkedMultiValueMap<String, Object>();
//        map.add("app_id", "tables");
//        map.add("table_id", "testTableId");
//        String filePath = "tables/helloServer.txt"; // just a hardcoded dummy
//        File file = new File("/sdcard/odk/" + filePath);
//        map.add(filePath, file);
//        URI totalUrl = fileServletUri.resolve(filePath).normalize();
//        HttpHeaders headers = new HttpHeaders();
//        List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
//        acceptableMediaTypes.add(new MediaType("text", "xml"));
//        headers.setAccept(acceptableMediaTypes);
//        headers.setContentType(MediaType.TEXT_XML);
//        HttpEntity<MultiValueMap<String, Object>> request = 
//            new HttpEntity<MultiValueMap<String, Object>>(map, headers);
//        String response = rt.postForObject(totalUrl, request, 
//            String.class);
//        Log.e(TAG, "response: " + response);
// This is an old an no-good way of doing the manifest. Commenting out as I try
// to get the upload from phone server stuff working.
//        FileSyncAdapter syncAdapter = new FileSyncAdapter(
//            getApplicationContext(), true);
        return null;
      }  finally{

      }
    }
  }
}
