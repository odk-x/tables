package org.opendatakit.tables.fragments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.tables.views.components.TablePropertiesAdapter;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.content.Intent;
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
  
  /** All the TableProperties that should be visible to the user. */
  private List<TableProperties> mTableList;
  
  private TablePropertiesAdapter mTpAdapter;
  
  public TableManagerFragment() {
    // empty constructor required for fragments.
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "[onCreate]");
    this.mTableList = new ArrayList<TableProperties>();
    this.setHasOptionsMenu(true);
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Log.d(TAG, "[onOptionsItemSelected] selecting an item");
    return super.onOptionsItemSelected(item);
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
    List<TableProperties> newProperties = this.retrieveContentsToDisplay();
    Log.e(TAG, "got newProperties list of size: " + newProperties.size());
    this.setPropertiesList(newProperties);
    this.mTpAdapter = new TablePropertiesAdapter(this.getPropertiesList());
    this.setListAdapter(this.mTpAdapter);
    this.mTpAdapter.notifyDataSetChanged();
    this.registerForContextMenu(getListView());
  }
  
  @Override
  public void onResume() {
    super.onResume();
    ListView lv = this.getListView();
    int childcount = lv.getChildCount();
  }
  
  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    // There are two cases here: if it is the preferences icon, or if it is
    // the main view.
    if (v.getId() == R.id.row_item_icon) {
      Log.e(TAG, "was the icon");
    } else {
      Log.e(TAG, "was the main item");
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
  };
  
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterView.AdapterContextMenuInfo menuInfo = 
        (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
    final TableProperties tpOfSelectedItem = 
        this.getPropertiesList().get(menuInfo.position);
    switch (item.getItemId()) {
    case R.id.table_manager_delete_table:
      AlertDialog confirmDeleteAlert;
      // Prompt an alert box
      AlertDialog.Builder alert = new AlertDialog.Builder(this.getActivity());
      if (tpOfSelectedItem.isSharedTable()) {
        alert.setTitle(getString(R.string.confirm_remove_table)).setMessage(
            getString(
                R.string.are_you_sure_remove_table,
                tpOfSelectedItem.getDisplayName()));
      } else {
        alert.setTitle(getString(R.string.confirm_delete_table)).setMessage(
            getString(R.string.are_you_sure_delete_table,
                tpOfSelectedItem.getDisplayName()));
      }
      // OK Action => delete the table
      alert.setPositiveButton(getString(R.string.yes), 
          new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          // treat delete as a local removal -- not a server side deletion
          tpOfSelectedItem.deleteTable();
          // Now dleete the list.
          TableManagerFragment.this.setPropertiesList(
              retrieveContentsToDisplay());
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
      Intent intent = 
          ((AbsBaseActivity) getActivity()).createNewIntentWithAppName();
      // We also need to add the table id to the intent.
      intent.putExtra(
          Constants.IntentKeys.TABLE_ID,
          tpOfSelectedItem.getTableId());
      intent.setClass(
          getActivity(),
          TableLevelPreferencesActivity.class);
      getActivity().startActivity(intent);
      return true;
    }
    return false;
  };
  
  /**
   * Retrieve the contents that will be displayed in the list. This should be
   * used to populate the list.
   * @return
   */
  List<TableProperties> retrieveContentsToDisplay() {
    TableProperties[] tpArray = TableProperties.getTablePropertiesForAll(
        getActivity(),
        TableFileUtils.getDefaultAppName());
    List<TableProperties> tpList = Arrays.asList(tpArray);
    return tpList;
  }
  
  /**
   * Get the list currently displayed by the fragment.
   * @return
   */
  List<TableProperties> getPropertiesList() {
    return this.mTableList;
  }
  
  /**
   * Update the contents of the list with the this new list.
   * @param list
   */
  void setPropertiesList(List<TableProperties> list) {
    // We can't change the reference, which is held by the adapter.
    this.getPropertiesList().clear();
    for (TableProperties tp : list) {
      this.getPropertiesList().add(tp);
    }
  }
  
}
