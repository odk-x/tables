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

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreManager;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.utils.TableFileUtils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.actionbarsherlock.app.SherlockActivity;

/**
 * An activity for managing a table's property sets,
 * and the copying or merging of them across the
 * active, default and server sync sets.
 *
 * Stripped out of TablePropertiesManager
 *
 * @author mitchellsundt@gmail.com
 */
public class ManagePropertySetsManager extends SherlockActivity {

  private DbHelper dbh;
  private TableProperties tp;

  private AlertDialog revertDialog;
  private AlertDialog saveAsDefaultDialog;
  private AlertDialog defaultToServerDialog;
  private AlertDialog serverToDefaultDialog;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String appName = getIntent().getStringExtra(Controller.INTENT_KEY_APP_NAME);
    if ( appName == null ) {
      appName = TableFileUtils.getDefaultAppName();
    }
    String tableId = getIntent().getStringExtra(Controller.INTENT_KEY_TABLE_ID);
    if (tableId == null) {
      throw new RuntimeException("Table ID (" + tableId + ") is invalid.");
    }
    dbh = DbHelper.getDbHelper(this, appName);
    tp = TableProperties.getTablePropertiesForTable(dbh, tableId, KeyValueStore.Type.ACTIVE);

    setTitle(getString(R.string.manage_property_sets_title, tp.getDisplayName()));

    this.setContentView(R.layout.manage_property_sets);

    AlertDialog.Builder builder = new AlertDialog.Builder(ManagePropertySetsManager.this);
    builder.setMessage(getString(R.string.revert_warning));
    builder.setCancelable(true);
    builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
        KeyValueStore defaultKVS = kvsm.getStoreForTable(tp.getTableId(),
            KeyValueStore.Type.DEFAULT);
        if (!defaultKVS.entriesExist(db)) {
          AlertDialog.Builder noDefaultsDialog = new AlertDialog.Builder(
              ManagePropertySetsManager.this);
          noDefaultsDialog.setMessage(getString(R.string.no_default_no_changes));
          noDefaultsDialog.setNeutralButton(getString(R.string.ok), null);
          noDefaultsDialog.show();
        } else {
          kvsm.copyDefaultToActiveForTable(tp.getTableId());
        }
      }
    });
    builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        dialog.cancel();
      }
    });
    revertDialog = builder.create();

    builder = new AlertDialog.Builder(ManagePropertySetsManager.this);
    builder.setMessage(getString(R.string.are_you_sure_save_default_settings));
    builder.setCancelable(true);
    builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
        kvsm.setCurrentAsDefaultPropertiesForTable(tp.getTableId());
      }
    });
    builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        dialog.cancel();
      }
    });
    saveAsDefaultDialog = builder.create();

    builder = new AlertDialog.Builder(ManagePropertySetsManager.this);
    builder.setMessage(getString(R.string.are_you_sure_copy_default_settings_to_server_settings));
    builder.setCancelable(true);
    builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
        kvsm.copyDefaultToServerForTable(tp.getTableId());
      }
    });
    builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        dialog.cancel();
      }
    });
    defaultToServerDialog = builder.create();

    builder = new AlertDialog.Builder(ManagePropertySetsManager.this);
    builder
        .setMessage(getString(R.string.are_you_sure_merge_server_settings_into_default_settings));
    builder.setCancelable(true);
    builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
        kvsm.mergeServerToDefaultForTable(tp.getTableId());
      }
    });
    builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        dialog.cancel();
      }
    });
    serverToDefaultDialog = builder.create();


    View view;

    view = findViewById(R.id.restore_defaults_button);
    view.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        revertDialog.show();
      }

    });

    view = findViewById(R.id.save_to_defaults_button);
    view.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        saveAsDefaultDialog.show();
      }

    });

    view = findViewById(R.id.copy_defaults_to_server_button);
    view.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        defaultToServerDialog.show();
      }

    });

    view = findViewById(R.id.merge_server_into_defaults_button);
    view.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        serverToDefaultDialog.show();
      }

    });
  }

  @Override
  public void onBackPressed() {
    setResult(RESULT_OK);
    finish();
  }
}
