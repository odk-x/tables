package org.opendatakit.tables.fragments;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.tables.activities.AbsTableActivity;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.utils.ColumnUtil;
import org.opendatakit.tables.utils.TableUtil;

import android.app.Activity;
import android.app.ListFragment;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Displays the columns in a table.
 * 
 * @author sudar.sam@gmail.com
 *
 */
public class ColumnListFragment extends ListFragment {

  private static final String TAG = ColumnListFragment.class.getSimpleName();

  /** The element keys of the columns. */
  private List<String> mElementKeys;

  /** The display name of every column in the table. */
  private List<String> mDisplayNames;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (!(activity instanceof AbsTableActivity)) {
      throw new IllegalStateException("must be attached to "
          + AbsTableActivity.class.getSimpleName());
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
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(),
        android.R.layout.simple_list_item_1, this.mDisplayNames);
    this.setListAdapter(adapter);
  }

  public void onListItemClick(ListView l, View v, int position, long id) {
    TableLevelPreferencesActivity tableLevePreferenceActivity = (TableLevelPreferencesActivity) this
        .getActivity();
    String elementKey = this.mElementKeys.get(position);
    tableLevePreferenceActivity.showColumnPreferenceFragment(elementKey);
  }

  /**
   * Retrieve all the element keys for the columns in the table.
   * 
   * @return
   */
  ArrayList<String> retrieveAllElementKeys() {
    AbsTableActivity activity = retrieveTableActivity();

    ArrayList<String> colOrder;
    SQLiteDatabase db = null;
    try {
      db = DatabaseFactory.get().getDatabase(activity, activity.getAppName());
      colOrder = TableUtil.get().getColumnOrder(db, activity.getTableId());
    } finally {
      if ( db != null ) {
        db.close();
      }
    }
    if (colOrder.isEmpty()) {
      ArrayList<ColumnDefinition> orderedDefns = activity.getColumnDefinitions();
      for ( ColumnDefinition cd : orderedDefns ) {
        if ( cd.isUnitOfRetention() ) {
          colOrder.add(cd.getElementKey());
        }
      }
    }
    return colOrder;
  }

  /**
   * Get all the display names of the columns.
   * 
   * @return
   */
  List<String> retrieveAllDisplayNames() {
    AbsTableActivity activity = retrieveTableActivity();
    List<String> result = new ArrayList<String>();
    List<String> elementKeys = this.retrieveAllElementKeys();
    SQLiteDatabase db = null;
    try {
      db = DatabaseFactory.get().getDatabase(activity, activity.getAppName());
      for (String elementKey : elementKeys) {
        String localizedDisplayName = ColumnUtil.get().getLocalizedDisplayName(db,
            activity.getTableId(), elementKey);
        result.add(localizedDisplayName);
      }
    } finally {
      if (db != null) {
        db.close();
      }
    }
    return result;
  }

  /**
   * Retrieve the {@link AbsTableActivity} hosting this fragment.
   * 
   * @return
   */
  AbsTableActivity retrieveTableActivity() {
    AbsTableActivity activity = (AbsTableActivity) this.getActivity();
    return activity;
  }

}
