/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.fragments;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.OrderedColumns;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.tables.activities.AbsTableActivity;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.TableUtil;
import org.opendatakit.tables.utils.TableUtil.TableColumns;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.os.RemoteException;
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
    TableLevelPreferencesActivity tableLevelPreferenceActivity = (TableLevelPreferencesActivity) this
        .getActivity();
    WebLogger.getLogger(tableLevelPreferenceActivity.getAppName()).d(TAG, "[onActivityCreated]");
    // All we need to do is get the columns to display.
    try {
      setElementKeysAndDisplayNames();
    } catch (RemoteException e) {
      WebLogger.getLogger(tableLevelPreferenceActivity.getAppName()).printStackTrace(e);
      return;
    }
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(),
        android.R.layout.simple_list_item_1, this.mDisplayNames);
    this.setListAdapter(adapter);
  }

  public void onListItemClick(ListView l, View v, int position, long id) {
    TableLevelPreferencesActivity tableLevelPreferenceActivity = (TableLevelPreferencesActivity) this
        .getActivity();
    String elementKey = this.mElementKeys.get(position);
    tableLevelPreferenceActivity.showColumnPreferenceFragment(elementKey);
  }

  /**
   * Retrieve all the element keys for the columns in the table.
   * 
   * @return
   * @throws RemoteException 
   */
  private void setElementKeysAndDisplayNames() throws RemoteException {
    
    AbsTableActivity activity = retrieveTableActivity();
    String appName = activity.getAppName();
    OrderedColumns orderedDefns = activity.getColumnDefinitions();
    TableColumns tc = null;
    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(appName, false);
      tc = TableUtil.get().getTableColumns(appName, db, activity.getTableId());

      ArrayList<String> colOrder;
      colOrder = TableUtil.get().getColumnOrder(appName, db, activity.getTableId());
      if (colOrder.isEmpty()) {
        for ( ColumnDefinition cd : orderedDefns.getColumnDefinitions() ) {
          if ( cd.isUnitOfRetention() ) {
            colOrder.add(cd.getElementKey());
          }
        }
      }
      this.mElementKeys = colOrder;
      List<String> displayNames = new ArrayList<String>();
      for (String elementKey : mElementKeys) {
        String localizedDisplayName = tc.localizedDisplayNames.get(elementKey);
        displayNames.add(localizedDisplayName);
      }
      this.mDisplayNames = displayNames;
    } finally {
      if ( db != null ) {
        Tables.getInstance().getDatabase().closeDatabase(appName, db);
      }
    }
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
