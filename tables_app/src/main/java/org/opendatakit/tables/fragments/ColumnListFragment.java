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

import androidx.fragment.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import org.opendatakit.activities.IAppAwareActivity;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.data.OrderedColumns;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.properties.CommonToolProperties;
import org.opendatakit.properties.PropertiesSingleton;
import org.opendatakit.tables.activities.AbsTableActivity;
import org.opendatakit.tables.activities.TableLevelPreferencesActivity;
import org.opendatakit.tables.application.Tables;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the columns in a table.
 *
 * @author sudar.sam@gmail.com
 */
public class ColumnListFragment extends ListFragment {

  private static final String TAG = ColumnListFragment.class.getSimpleName();

  /**
   * The element keys of the columns.
   */
  private List<String> mElementKeys;

  /**
   * The display name of every column in the table.
   */
  private List<String> mDisplayNames;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (!(context instanceof AbsTableActivity)) {
      throw new IllegalStateException(
          "must be attached to " + AbsTableActivity.class.getSimpleName());
    }
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    IAppAwareActivity tableLevelPreferenceActivity = (IAppAwareActivity) getActivity();
    WebLogger.getLogger(tableLevelPreferenceActivity.getAppName()).d(TAG, "[onActivityCreated]");
    // All we need to do is get the columns to display.
    try {
      setElementKeysAndDisplayNames();
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(tableLevelPreferenceActivity.getAppName()).printStackTrace(e);
      return;
    }
    ListAdapter adapter = new ArrayAdapter<>(this.getActivity(),
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
   * Sets all the element keys for the columns in the table.
   *
   * @throws ServicesAvailabilityException if the database is down
   */
  private void setElementKeysAndDisplayNames() throws ServicesAvailabilityException {

    AbsTableActivity activity = retrieveTableActivity();
    String appName = activity.getAppName();
    OrderedColumns orderedDefns = activity.getColumnDefinitions();
    PropertiesSingleton props = CommonToolProperties.get(getActivity().getApplication(), appName);
    String userSelectedDefaultLocale = props.getUserSelectedDefaultLocale();
    UserDbInterface dbInterface = Tables.getInstance().getDatabase();
    TableUtil.TableColumns tc;
    DbHandle db = null;
    try {
      db = dbInterface.openDatabase(appName);
      tc = TableUtil.get().getTableColumns(userSelectedDefaultLocale, dbInterface, appName, db,
          activity.getTableId());

      ArrayList<String> colOrder;
      colOrder = TableUtil.get()
          .getColumnOrder(dbInterface, appName, db, activity.getTableId(), orderedDefns);
      this.mElementKeys = colOrder;
      List<String> displayNames = new ArrayList<>();
      for (String elementKey : mElementKeys) {
        String localizedDisplayName = tc.localizedDisplayNames.get(elementKey);
        displayNames.add(localizedDisplayName);
      }
      this.mDisplayNames = displayNames;
    } finally {
      if (db != null) {
        dbInterface.closeDatabase(appName, db);
      }
    }
  }

  /**
   * Retrieve the {@link AbsTableActivity} hosting this fragment.
   *
   * @return the parent activity casted to an AbsTableActivity
   */
  AbsTableActivity retrieveTableActivity() {
    return (AbsTableActivity) getActivity();
  }

}
