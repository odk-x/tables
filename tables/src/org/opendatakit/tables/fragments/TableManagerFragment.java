package org.opendatakit.tables.fragments;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.common.android.utilities.TableUtil;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.DisplayPrefsActivity;
import org.opendatakit.tables.activities.ExportCSVActivity;
import org.opendatakit.tables.activities.ImportCSVActivity;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.utils.ActivityUtil;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.TableNameStruct;
import org.opendatakit.tables.views.components.TableNameStructAdapter;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class TableManagerFragment extends ListFragment {

  private static final String TAG = TableManagerFragment.class.getSimpleName();

  /** All the tableIds that should be visible to the user. */
  private List<TableNameStruct> mTableNameStructs;

  private TableNameStructAdapter mTpAdapter;

  public TableManagerFragment() {
    // empty constructor required for fragments.
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "[onCreate]");
    this.mTableNameStructs = new ArrayList<TableNameStruct>();
    this.setHasOptionsMenu(true);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Log.d(TAG, "[onOptionsItemSelected] selecting an item");
    AbsBaseActivity baseActivity = (AbsBaseActivity) getActivity();
    String appName = baseActivity.getAppName();
    Bundle bundle = new Bundle();
    IntentUtil.addAppNameToBundle(bundle, appName);
    switch (item.getItemId()) {
    case R.id.menu_table_manager_preferences:
      Intent preferenceIntent = new Intent(
          baseActivity,
          DisplayPrefsActivity.class);
      preferenceIntent.putExtras(bundle);
      this.startActivityForResult(
          preferenceIntent,
          Constants.RequestCodes.LAUNCH_DISPLAY_PREFS);
      return true;
    case R.id.menu_table_manager_import:
      Intent importIntent = new Intent(
          baseActivity,
          ImportCSVActivity.class);
      importIntent.putExtras(bundle);
      this.startActivityForResult(
          importIntent,
          Constants.RequestCodes.LAUNCH_IMPORT);
      return true;
    case R.id.menu_table_manager_export:
      Intent exportIntent = new Intent(
          baseActivity,
          ExportCSVActivity.class);
      exportIntent.putExtras(bundle);
      this.startActivityForResult(
          exportIntent,
          Constants.RequestCodes.LAUNCH_EXPORT);
      return true;
    case R.id.menu_table_manager_sync:
//      OdkSyncServiceProxy proxy = new OdkSyncServiceProxy(this.getActivity());
//      proxy.synchronizeFromServer(appName);
      Intent syncIntent = new Intent();
      syncIntent.setComponent(new ComponentName(
          "org.opendatakit.sync",
          "org.opendatakit.sync.activities.SyncActivity"));
      syncIntent.setAction(Intent.ACTION_DEFAULT);
      syncIntent.putExtras(bundle);
      this.startActivityForResult(
          syncIntent,
          Constants.RequestCodes.LAUNCH_SYNC);
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    Log.d(TAG, "[onCreateView]");
    View view = inflater.inflate(
        R.layout.fragment_table_list,
        container,
        false);
    return view;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    // call this here because we need a context.
    this.updateTableIdList();
    this.registerForContextMenu(getListView());
  }
  
  /**
   * Refresh the list of tables that is being displayed by
   * the fragment.
   */
  protected void updateTableIdList() {
    AbsBaseActivity baseActivity = (AbsBaseActivity) getActivity();
    SQLiteDatabase db = null;
    
    List<TableNameStruct> tableNameStructs = new ArrayList<TableNameStruct>();
    
    try {
      db = DatabaseFactory.get().getDatabase(
          baseActivity,
          baseActivity.getAppName());
      
      List<String> tableIds = ODKDatabaseUtils.get().getAllTableIds(db);
      
      for (String tableId : tableIds) {
        String localizedDisplayName = TableUtil.get().getLocalizedDisplayName(
            db,
            tableId);
        
        TableNameStruct tableNameStruct = new TableNameStruct(
            tableId,
            localizedDisplayName);
        
        tableNameStructs.add(tableNameStruct);        
      }
      
    } finally {
      if ( db != null ) {
        db.close();
      }
    }
    Log.e(TAG, "got tableId list of size: " + tableNameStructs.size());
    this.setList(tableNameStructs);
    this.mTpAdapter = new TableNameStructAdapter(
        baseActivity,
        baseActivity.getAppName(), 
        this.mTableNameStructs);
    this.setListAdapter(this.mTpAdapter);
    this.mTpAdapter.notifyDataSetChanged();
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    // There are two cases here: if it is the preferences icon, or if it is
    // the main view.
    if (v.getId() == R.id.row_item_icon) {
      Log.e(TAG, "was the icon");
    } else {
      AbsBaseActivity baseActivity = (AbsBaseActivity) getActivity();
      Intent intent = baseActivity.createNewIntentWithAppName();
      // Set the tableId.
      TableNameStruct nameStruct = (TableNameStruct)
          this.getListView().getItemAtPosition(position);
      String tableId = nameStruct.getTableId();
      intent.putExtra(
          Constants.IntentKeys.TABLE_ID,
          tableId);
      ComponentName componentName = new ComponentName(
          baseActivity,
          TableDisplayActivity.class);
      intent.setComponent(componentName);
      startActivityForResult(intent, Constants.RequestCodes.DISPLAY_VIEW);
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.table_manager, menu);
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public void onCreateContextMenu(
      ContextMenu menu,
      View v,
      ContextMenu.ContextMenuInfo menuInfo) {
    MenuInflater menuInflater = this.getActivity().getMenuInflater();
    menuInflater.inflate(R.menu.table_manager_context, menu);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterView.AdapterContextMenuInfo menuInfo =
        (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
    TableNameStruct selectedStruct = this.getList().get(menuInfo.position);
    final String tableIdOfSelectedItem = selectedStruct.getTableId();
    final AbsBaseActivity baseActivity = (AbsBaseActivity) getActivity();

    String localizedDisplayName;
    SQLiteDatabase db = null;
    try {
      db = DatabaseFactory.get().getDatabase(baseActivity,
          baseActivity.getAppName());
      localizedDisplayName = TableUtil.get().getLocalizedDisplayName(db, 
          tableIdOfSelectedItem);
    } finally {
      if ( db != null ) {
        db.close();
      }
    }

    switch (item.getItemId()) {
    case R.id.table_manager_delete_table:
      AlertDialog confirmDeleteAlert;
      // Prompt an alert box
      AlertDialog.Builder alert = new AlertDialog.Builder(this.getActivity());
      alert.setTitle(getString(R.string.confirm_remove_table)).setMessage(
          getString(
              R.string.are_you_sure_remove_table,
              localizedDisplayName));
      // OK Action => delete the table
      alert.setPositiveButton(getString(R.string.yes),
          new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          // treat delete as a local removal -- not a server side deletion
          SQLiteDatabase db = null;
          try {
            db = DatabaseFactory.get().getDatabase(baseActivity, baseActivity.getAppName());
            ODKDatabaseUtils.get().deleteDBTableAndAllData(db, baseActivity.getAppName(), tableIdOfSelectedItem);
          } finally {
            if ( db != null ) {
              db.close();
            }
          }
          // Now update the list.
          updateTableIdList();
        }
      });

      // Cancel Action
      alert.setNegativeButton(getString(R.string.cancel),
          new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          // Canceled.
        }
      });
      // show the dialog
      confirmDeleteAlert = alert.create();
      confirmDeleteAlert.show();
      return true;
    case R.id.table_manager_edit_table_properties:
      ActivityUtil.launchTableLevelPreferencesActivity(
          baseActivity,
          baseActivity.getAppName(),
          tableIdOfSelectedItem,
          TableLevelPreferencesActivity.FragmentType.TABLE_PREFERENCE);
      return true;
    }
    return false;
  }
  
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
    case Constants.RequestCodes.LAUNCH_SYNC:
      this.updateTableIdList();
      break;
    default: 
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  /**
   * Get the list currently displayed by the fragment.
   * @return
   */
  List<TableNameStruct> getList() {
    return this.mTableNameStructs;
  }

  /**
   * Update the contents of the list with the this new list.
   * @param list
   */
  void setList(List<TableNameStruct> list) {
    // We can't change the reference, which is held by the adapter.
    List<TableNameStruct> nameStructList = this.getList();
    nameStructList.clear();
    for (TableNameStruct nameStruct : list) {
      nameStructList.add(nameStruct);
    }
    if ( mTpAdapter != null ) {
      mTpAdapter.notifyDataSetChanged();
    }
  }

}
