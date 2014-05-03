package org.opendatakit.tables.activities;

import org.opendatakit.tables.fragments.TopLevelTableMenuFragment;
import org.opendatakit.tables.utils.Constants;

import android.app.FragmentManager;
import android.os.Bundle;

/**
 * Displays information about a table. List, Map, and Detail views are all
 * displayed via this  activity.
 * @author sudar.sam@gmail.com
 *
 */
public class TableDisplayActivity extends AbsTableActivity {
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.initializeFragments();
  }
  
  protected void initializeFragments() {
    FragmentManager fragmentManager = this.getFragmentManager();
    TopLevelTableMenuFragment menuFragment = new TopLevelTableMenuFragment();
    fragmentManager.beginTransaction().add(
        menuFragment,
        Constants.FragmentTags.TABLE_MENU).commit();
  }

}
