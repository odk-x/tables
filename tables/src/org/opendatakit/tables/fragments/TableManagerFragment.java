/*
 * Copyright (C) 2014 University of Washington
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
package org.opendatakit.tables.fragments;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.common.android.listener.DatabaseConnectionListener;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.ActivityUtil;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.TableNameStruct;
import org.opendatakit.tables.utils.TableUtil;
import org.opendatakit.tables.views.components.TableNameStructAdapter;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class TableManagerFragment extends ListFragment implements DatabaseConnectionListener {

  private static final String TAG = TableManagerFragment.class.getSimpleName();

  private static final int ID = R.layout.fragment_table_list;
  
  private TableNameStructAdapter mTpAdapter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    WebLogger.getLogger(((AbsBaseActivity) getActivity()).getAppName()).d(TAG, "[onCreateView]");
    View view = inflater.inflate(ID, container, false);
    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    updateTableIdList();
  }

  /**
   * Refresh the list of tables that is being displayed by the fragment.
   * @throws RemoteException 
   */
  protected void updateTableIdList() {
    AbsBaseActivity baseActivity = (AbsBaseActivity) getActivity();
    if ( baseActivity == null ) {
      return;
    }
    
    String appName = baseActivity.getAppName();
    OdkDbHandle db = null;

    List<TableNameStruct> tableNameStructs = new ArrayList<TableNameStruct>();

    if ( Tables.getInstance().getDatabase() != null ) {
      
      try {
        db = Tables.getInstance().getDatabase().openDatabase(appName, false);
  
        List<String> tableIds = Tables.getInstance().getDatabase().getAllTableIds(appName, db);
  
        for (String tableId : tableIds) {
          String localizedDisplayName = TableUtil.get().getLocalizedDisplayName(appName, db, tableId);
  
          TableNameStruct tableNameStruct = new TableNameStruct(tableId, localizedDisplayName);
  
          tableNameStructs.add(tableNameStruct);
        }
        WebLogger.getLogger(baseActivity.getAppName()).e(TAG,
            "got tableId list of size: " + tableNameStructs.size());
      } catch ( RemoteException e ) {
        WebLogger.getLogger(baseActivity.getAppName()).e(TAG,
            "error while fetching tableId list: " + e.toString());
      } finally {
        if (db != null) {
          try {
            Tables.getInstance().getDatabase().closeDatabase(appName, db);
          } catch (RemoteException e) {
            WebLogger.getLogger(baseActivity.getAppName()).e(TAG,
                "error while closing database: " + e.toString());
          }
        }
      }
    }

    if ( mTpAdapter == null ) {
      this.mTpAdapter = new TableNameStructAdapter(baseActivity, tableNameStructs);
      this.setListAdapter(this.mTpAdapter);
    } else {
      this.mTpAdapter.clear();
      this.mTpAdapter.addAll(tableNameStructs);
    }
    // and set visibility of the no data vs. list
    if ( this.getView() != null ) {
      TextView none = (TextView) this.getView().findViewById(android.R.id.empty);
      View listing = this.getView().findViewById(android.R.id.list);
      if ( tableNameStructs.isEmpty() ) {
        if ( Tables.getInstance().getDatabase() == null ) {
          none.setText(R.string.database_unavailable);
        } else {
          none.setText(R.string.no_table_data);
        }
        none.setVisibility(View.VISIBLE);
        listing.setVisibility(View.GONE);
      } else {
        listing.setVisibility(View.VISIBLE);
        none.setVisibility(View.GONE);
      }
    }
    this.mTpAdapter.notifyDataSetChanged();
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    // There are two cases here: if it is the preferences icon, or if it is
    // the main view.
    AbsBaseActivity baseActivity = (AbsBaseActivity) getActivity();
    if (v.getId() == R.id.row_item_icon) {
      WebLogger.getLogger(baseActivity.getAppName()).e(TAG, "was the icon");
    } else {
      Intent intent = baseActivity.createNewIntentWithAppName();
      // Set the tableId.
      TableNameStruct nameStruct = (TableNameStruct) this.getListView().getItemAtPosition(position);
      String tableId = nameStruct.getTableId();
      intent.putExtra(Constants.IntentKeys.TABLE_ID, tableId);
      ComponentName componentName = new ComponentName(baseActivity, TableDisplayActivity.class);
      intent.setComponent(componentName);
      startActivityForResult(intent, Constants.RequestCodes.DISPLAY_VIEW);
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    MenuInflater menuInflater = this.getActivity().getMenuInflater();
    menuInflater.inflate(R.menu.table_manager_context, menu);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
        .getMenuInfo();
    TableNameStruct selectedStruct = this.mTpAdapter.getItem(menuInfo.position);
    final String tableIdOfSelectedItem = selectedStruct.getTableId();
    final AbsBaseActivity baseActivity = (AbsBaseActivity) getActivity();
    final String appName = baseActivity.getAppName();
    
    String localizedDisplayName = selectedStruct.getLocalizedDisplayName();

    switch (item.getItemId()) {
    case R.id.table_manager_delete_table:
      AlertDialog confirmDeleteAlert;
      // Prompt an alert box
      AlertDialog.Builder alert = new AlertDialog.Builder(this.getActivity());
      alert.setTitle(getString(R.string.confirm_remove_table)).setMessage(
          getString(R.string.are_you_sure_remove_table, localizedDisplayName));
      // OK Action => delete the table
      alert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          // treat delete as a local removal -- not a server side deletion
          boolean successful = false;
          OdkDbHandle db = null;
          try {
            try {
              db = Tables.getInstance().getDatabase().openDatabase(appName, true);
              Tables.getInstance().getDatabase().deleteDBTableAndAllData(appName, db, tableIdOfSelectedItem);
              successful = true;
            } finally {
              if (db != null) {
                Tables.getInstance().getDatabase().closeTransactionAndDatabase(appName, db, successful);
              }
            }
            // Now update the list.
            updateTableIdList();
          } catch (RemoteException e) {
            WebLogger.getLogger(((AbsBaseActivity) getActivity()).getAppName()).printStackTrace(e);
            Toast.makeText(getActivity(), "Unable to access database", Toast.LENGTH_LONG).show();
          }
        }
      });

      // Cancel Action
      alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          // Canceled.
        }
      });
      // show the dialog
      confirmDeleteAlert = alert.create();
      confirmDeleteAlert.show();
      return true;
    case R.id.table_manager_edit_table_properties:
      ActivityUtil.launchTableLevelPreferencesActivity(baseActivity, baseActivity.getAppName(),
          tableIdOfSelectedItem, TableLevelPreferencesActivity.FragmentType.TABLE_PREFERENCE);
      return true;
    }
    return false;
  }

  @Override
  public void databaseAvailable() {
    this.updateTableIdList();
  }

  @Override
  public void databaseUnavailable() {
    this.updateTableIdList();
  }

}
