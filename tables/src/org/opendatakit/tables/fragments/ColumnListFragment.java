package org.opendatakit.tables.fragments;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.activities.AbsTableActivity;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.utils.ColumnUtil;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Displays the columns in a table.
 * @author sudar.sam@gmail.com
 *
 */
public class ColumnListFragment extends ListFragment {
  
  private static final String TAG =
      ColumnListFragment.class.getSimpleName();
  
  /** The element keys of the columns. */
  private List<String> mElementKeys;
  
  /** The display name of every column in the table. */
  private List<String> mDisplayNames;
  
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (!(activity instanceof AbsTableActivity)) {
      throw new IllegalStateException(
          "must be attached to " + AbsTableActivity.class.getSimpleName());
    }
  }
  
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Log.d(TAG, "[onActivityCreated]");
    // All we need to do is get the columns to display.
    List<String> elementKeys = this.retrieveAllElementKeys();
    List<String> displayNames = this.retrieveAllDisplayNames();
    this.mElementKeys = elementKeys;
    this.mDisplayNames = displayNames;
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
        this.getActivity(),
        android.R.layout.simple_list_item_1,
        this.mDisplayNames);
    this.setListAdapter(adapter);
  }
  
  public void onListItemClick(ListView l, View v, int position, long id) {
    TableLevelPreferencesActivity tableLevePreferenceActivity =
        (TableLevelPreferencesActivity) this.getActivity();
    String elementKey = this.mElementKeys.get(position);
    tableLevePreferenceActivity.showColumnPreferenceFragment(elementKey);
  }
  
  /**
   * Retrieve all the element keys for the columns in the table.
   * @return
   */
  List<String> retrieveAllElementKeys() {
    List<String> result = this.retrieveTableProperties().getColumnOrder();
    return result;
  }
  
  /**
   * Get all the display names of the columns.
   * @return
   */
  List<String> retrieveAllDisplayNames() {
    List<String> result = new ArrayList<String>();
    List<String> elementKeys = this.retrieveAllElementKeys();
    TableProperties tableProperties = this.retrieveTableProperties();
    for (String elementKey : elementKeys) {
      String displayName = ColumnUtil.getLocalizedDisplayName(tableProperties, elementKey);
      result.add(displayName);
    }
    return result;
  }
  
  /**
   * Get the {@link TableProperties} associated with this activity.
   * @return
   */
  TableProperties retrieveTableProperties() { 
    return this.retrieveTableActivity().getTableProperties();
  }
  
  /**
   * Retrieve the {@link AbsTableActivity} hosting this fragment.
   * @return
   */
  AbsTableActivity retrieveTableActivity() {
    AbsTableActivity activity = (AbsTableActivity) this.getActivity();
    return activity;
  }

}
