package org.opendatakit.tables.fragments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.R;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.tables.views.components.TablePropertiesAdapter;

import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

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
  }
  
  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.table_manager, menu);
    super.onCreateOptionsMenu(menu, inflater);
  }
  
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
