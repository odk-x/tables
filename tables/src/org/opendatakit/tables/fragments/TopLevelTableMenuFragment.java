package org.opendatakit.tables.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;

/**
 * Displays the common menu shared across all top level views onto a table.
 * I.e. the menu that is shared between Spreadsheet, Map, List, and Graph views
 * is initialized here.
 * @author sudar.sam@gmail.com
 *
 */
public class TopLevelTableMenuFragment extends Fragment {
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // The whole point of this class is to display the menus.
    this.setHasOptionsMenu(true);
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    // TODO Auto-generated method stub
    super.onCreateContextMenu(menu, v, menuInfo);
  }

}
