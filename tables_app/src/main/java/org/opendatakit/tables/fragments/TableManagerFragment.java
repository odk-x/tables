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

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.ListFragment;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.opendatakit.activities.IAppAwareActivity;
import org.opendatakit.consts.IntentConsts;
import org.opendatakit.consts.RequestCodeConsts;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.listener.DatabaseConnectionListener;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.properties.CommonToolProperties;
import org.opendatakit.properties.PropertiesSingleton;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.ActivityUtil;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.TableNameStruct;
import org.opendatakit.tables.views.components.TableNameStructAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A fragment that displays a list of tables with a table actions button to the right of each of
 * them. The default fragment created in MainMenuActivity if there is no splash screen set
 */
public class TableManagerFragment extends ListFragment implements DatabaseConnectionListener, MainActivity.UXNotifyListener {

  private static final String TAG = TableManagerFragment.class.getSimpleName();

  private static final int ID = R.layout.fragment_table_list;

  private TableNameStructAdapter mTpAdapter = null;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    WebLogger.getLogger(((IAppAwareActivity) getActivity()).getAppName()).d(TAG, "[onCreateView]");
    return inflater.inflate(ID, container, false);
  }

  @Override
  public void onResume() {
    super.onResume();
    updateTableIdList();
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    this.setHasOptionsMenu(true);
    this.registerForContextMenu(this.getListView());
  }

  /**
   * Refresh the list of tables that is being displayed by the fragment.
   */
  protected void updateTableIdList() {
    AbsBaseActivity baseActivity = (AbsBaseActivity) getActivity();
    if (baseActivity == null) {
      return;
    }

    String appName = baseActivity.getAppName();
    PropertiesSingleton props = CommonToolProperties.get(getActivity().getApplication(), appName);
    String userSelectedDefaultLocale = props.getUserSelectedDefaultLocale();

    UserDbInterface dbInterface = Tables.getInstance().getDatabase();
    DbHandle db = null;

    List<TableNameStruct> tableNameStructs = new ArrayList<>();
    final Constants.TABLE_SORT_ORDER fragSortOrder = getArguments() == null ? null : Constants.TABLE_SORT_ORDER.valueOf(this.getArguments().getString(CommonToolProperties.KEY_PREF_TABLES_SORT_BY_ORDER) );

    if (Tables.getInstance().getDatabase() != null) {

      try {
        db = dbInterface.openDatabase(appName);

        List<String> tableIds = dbInterface.getAllTableIds(appName, db);

        for (String tableId : tableIds) {
          String localizedDisplayName = TableUtil.get()
              .getLocalizedDisplayName(userSelectedDefaultLocale, dbInterface, appName, db,
                  tableId);

          TableNameStruct tableNameStruct = new TableNameStruct(tableId, localizedDisplayName);

          tableNameStructs.add(tableNameStruct);
        }
        WebLogger.getLogger(baseActivity.getAppName())
            .e(TAG, "got tableId list of size: " + tableNameStructs.size());
      } catch (ServicesAvailabilityException e) {
        WebLogger.getLogger(baseActivity.getAppName()).e(TAG, "error while fetching tableId list");
        WebLogger.getLogger(baseActivity.getAppName()).printStackTrace(e);
      } finally {
        if (db != null) {
          try {
            dbInterface.closeDatabase(appName, db);
          } catch (ServicesAvailabilityException e) {
            WebLogger.getLogger(baseActivity.getAppName()).e(TAG, "error while closing database");
            WebLogger.getLogger(baseActivity.getAppName()).printStackTrace(e);
          }
        }
      }
    }
    if(fragSortOrder != null) {
      Collections.sort(tableNameStructs, new Comparator<TableNameStruct>() {
        @Override
        public int compare(TableNameStruct o1, TableNameStruct o2)
        {
          if( fragSortOrder == Constants.TABLE_SORT_ORDER.SORT_DESC )
            return o2.getLocalizedDisplayName().compareTo(o1.getLocalizedDisplayName());
          else
            return o1.getLocalizedDisplayName().compareTo(o2.getLocalizedDisplayName());
        }
      });
    }

    if (mTpAdapter == null) {
      this.mTpAdapter = new TableNameStructAdapter(baseActivity, tableNameStructs);
      this.setListAdapter(this.mTpAdapter);
    } else {
      this.mTpAdapter.clear();
      this.mTpAdapter.addAll(tableNameStructs);
    }
    // and set visibility of the no data vs. list
    if (this.getView() != null) {
      TextView none = this.getView().findViewById(android.R.id.empty);
      View listing = this.getView().findViewById(android.R.id.list);
      if (tableNameStructs.isEmpty()) {
        if (Tables.getInstance().getDatabase() == null) {
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
      intent.putExtra(IntentConsts.INTENT_KEY_TABLE_ID, tableId);
      ComponentName componentName = new ComponentName(baseActivity, TableDisplayActivity.class);
      intent.setComponent(componentName);
      startActivityForResult(intent, RequestCodeConsts.RequestCodes.DISPLAY_VIEW);
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    MenuInflater menuInflater = this.getActivity().getMenuInflater();
    menuInflater.inflate(R.menu.table_manager_context, menu);
    menu.setHeaderTitle(R.string.table_actions);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
    TableNameStruct selectedStruct = this.mTpAdapter.getItem(menuInfo.position);
    if (selectedStruct == null) {
      return super.onContextItemSelected(item);
    }
    final String tableIdOfSelectedItem = selectedStruct.getTableId();
    final AbsBaseActivity baseActivity = (AbsBaseActivity) getActivity();
    final String appName = baseActivity.getAppName();
    String localizedDisplayName = selectedStruct.getLocalizedDisplayName();

    int itemId = item.getItemId();

    if (itemId == R.id.table_manager_delete_table) {
      AlertDialog confirmDeleteAlert;
      MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(this.getActivity());
      alert.setTitle(getString(R.string.confirm_remove_table))
              .setMessage(getString(R.string.are_you_sure_remove_table, localizedDisplayName));
      alert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          DbHandle db = null;
          try {
            try {
              db = Tables.getInstance().getDatabase().openDatabase(appName);
              Tables.getInstance().getDatabase().deleteTableAndAllData(appName, db, tableIdOfSelectedItem);
            } finally {
              if (db != null) {
                Tables.getInstance().getDatabase().closeDatabase(appName, db);
              }
            }
            updateTableIdList();
          } catch (ServicesAvailabilityException e) {
            WebLogger.getLogger(((IAppAwareActivity) getActivity()).getAppName()).printStackTrace(e);
            Toast.makeText(getActivity(), "Unable to access database", Toast.LENGTH_LONG).show();
          }
        }
      });

      alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          // Canceled.
        }
      });

      confirmDeleteAlert = alert.create();
      confirmDeleteAlert.show();
      return true;
    } else if (itemId == R.id.table_manager_edit_table_properties) {
      ActivityUtil.launchTableLevelPreferencesActivity(baseActivity, baseActivity.getAppName(),
              tableIdOfSelectedItem, TableLevelPreferencesActivity.FragmentType.TABLE_PREFERENCE);
      return true;
    } else {
      return super.onContextItemSelected(item);
    }
  }


  @Override
  public void databaseAvailable() {
    this.updateTableIdList();
  }

  @Override
  public void databaseUnavailable() {
    this.updateTableIdList();
  }
  
  @Override
  public void notifyUIChanges() {
    updateTableIdList();
  }
}
