package org.opendatakit.tables.fragments;

import java.util.Set;

import org.opendatakit.common.android.data.PossibleTableViewTypes;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsTableActivity;
import org.opendatakit.tables.activities.TableDisplayActivity;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;

/**
 * Displays the common menu shared across all top level views onto a table.
 * I.e. the menu that is shared between Spreadsheet, Map, List, and Graph views
 * is initialized here.
 * @author sudar.sam@gmail.com
 *
 */
public class TopLevelTableMenuFragment extends Fragment {
  
  private static final String TAG = 
      TopLevelTableMenuFragment.class.getSimpleName();
  
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (!(activity instanceof TableDisplayActivity)) {
      throw new IllegalStateException("This fragment must be attached to " +
      		"a " + AbsTableActivity.class.getSimpleName());
    }
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "[onCreate]");
    // The whole point of this class is to display the menus.
    this.setHasOptionsMenu(true);
  }
  
  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(
        R.menu.top_level_table_menu,
        menu);
  }
  
  /**
   * Retrieve the {@link TableViewType}s that are valid for the table
   * associated with the {@link TableDisplayActivity}.
   * @return
   */
  PossibleTableViewTypes getValidViewTypes() {
    return this.getTableProperties().getPossibleViewTypes();
  }
  
  /**
   * Return the {@link TableProperties} associated with the Activity related
   * to this table.
   * @return
   */
  TableProperties getTableProperties() {
    TableDisplayActivity activity = (TableDisplayActivity) this.getActivity();
    return activity.getTableProperties();
  }
  
  /**
   * Disable or enable those menu items corresponding to view types that are
   * currently invalid or valid, respectively. The inflatedMenu must have
   * already been created from the resource.
   * @param validViewTypes
   * @param inflatedMenu
   */
  private void enableAndDisableViewTypes(
      Set<TableViewType> validViewTypes,
      Menu inflatedMenu) {
    // TODO: this.
  }
  

}
