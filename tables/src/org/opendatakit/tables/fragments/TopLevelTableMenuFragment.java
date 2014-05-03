package org.opendatakit.tables.fragments;

import org.opendatakit.tables.R;

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

}
