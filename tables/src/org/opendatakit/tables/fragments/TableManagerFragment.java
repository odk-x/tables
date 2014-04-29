package org.opendatakit.tables.fragments;

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
import android.widget.LinearLayout;

public class TableManagerFragment extends ListFragment {
  
  private static final String TAG = TableManagerFragment.class.getSimpleName();
  
  /** All the TableProperties that should be visible to the user. */
  private TableProperties[] mTableList;
  
  public TableManagerFragment() {
    // empty constructor required for fragments.
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "[onCreate]");
    this.setHasOptionsMenu(true);
    this.setMenuVisibility(true);
    this.setListAdapter(new TablePropertiesAdapter(
        getActivity(),
        R.layout.row_item_with_preference,
        new TableProperties[0]));
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
  public void onViewCreated(View view, Bundle savedInstanceState) {
    Log.d(TAG, "[onViewCreated]");
    this.mTableList = retrieveContentsToDisplay();
    TablePropertiesAdapter adapter = new TablePropertiesAdapter(
        getActivity(),
        R.layout.row_item_with_preference,
        this.getList());
    this.setListAdapter(adapter);
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
  TableProperties[] retrieveContentsToDisplay() {
    return TableProperties.getTablePropertiesForAll(
        getActivity(),
        TableFileUtils.getDefaultAppName());
  }
  
  /**
   * Get the list currently displayed by the fragment.
   * @return
   */
  TableProperties[] getList() {
    return this.mTableList;
  }

}
