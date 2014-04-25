package org.opendatakit.tables.fragments;

import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.logic.PropertyManager;
import org.opendatakit.tables.R;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.tables.views.components.TablePropertiesAdapter;

import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

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
  }
  
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Log.d(TAG, "[onActivityCreated]");
  }
  
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    Log.d(TAG, "[onCreateView]");
    this.setHasOptionsMenu(true);
    return super.onCreateView(inflater, container, savedInstanceState);
  }
  
  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    Log.d(TAG, "[onViewCreated]");
    Log.d(TAG, "in onViewCreated get activity is: " + getActivity());
    this.mTableList = TableProperties.getTablePropertiesForAll(
        getActivity(),
        TableFileUtils.getDefaultAppName());
    TablePropertiesAdapter adapter = new TablePropertiesAdapter(
        getActivity(),
        R.layout.row_item_with_preference,
        this.getTablePropertiesList());
    this.setListAdapter(adapter);
  }
  
  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    Log.e(TAG, "XXXXX created menu XXXXX");
    inflater.inflate(R.menu.table_manager, menu);
    super.onCreateOptionsMenu(menu, inflater);
  }
  
  /**
   * Get the {@link TableProperties} displayed by the object.
   * @return
   */
  TableProperties[] getTablePropertiesList() {
    return this.mTableList;
  }

}
