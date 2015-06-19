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

import org.opendatakit.common.android.utilities.KeyValueStoreHelper;
import org.opendatakit.common.android.utilities.LocalKeyValueStoreConstants;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.database.service.KeyValueStoreEntry;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.GraphViewStruct;
import org.opendatakit.tables.views.components.GraphViewAdapter;

import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Displays the graphs currently saved in the activity.
 * 
 * @author sudar.sam@gmail.com
 *
 */
public class GraphManagerFragment extends AbsTableDisplayFragment {

  private static final String TAG = GraphManagerFragment.class.getSimpleName();

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View result = inflater.inflate(R.layout.graph_view_manager, container, false);
    return result;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
  }

  List<GraphViewStruct> retrieveGraphViews() throws RemoteException {
    // A graph is currently makred as default if its name is in this aspect
    // marked as the name.
    String tableId = getTableId();
    String currentDefaultGraphName = null;
    List<KeyValueStoreEntry> graphViewEntries = new ArrayList<KeyValueStoreEntry>();
    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(getAppName(), false);

      KeyValueStoreHelper kvshForGraphWritLarge = new KeyValueStoreHelper(Tables.getInstance(), getAppName(),
          db, tableId,
          LocalKeyValueStoreConstants.Graph.PARTITION);
      currentDefaultGraphName = kvshForGraphWritLarge
          .getString(LocalKeyValueStoreConstants.Graph.KEY_GRAPH_VIEW_NAME);

      graphViewEntries = Tables.getInstance().getDatabase().getDBTableMetadata(getAppName(), db, tableId,
          LocalKeyValueStoreConstants.Graph.PARTITION_VIEWS, null,
          LocalKeyValueStoreConstants.Graph.KEY_GRAPH_TYPE);
    } finally {
      if (db != null) {
        Tables.getInstance().getDatabase().closeDatabase(getAppName(), db);
      }
    }

    List<GraphViewStruct> result = new ArrayList<GraphViewStruct>();
    for (KeyValueStoreEntry e : graphViewEntries) {
      result.add(new GraphViewStruct(e.aspect, e.value, e.aspect.equals(currentDefaultGraphName)));
    }
    return result;
  }
  
  @Override
  public void databaseAvailable() {
    View v = this.getView();
    if ( v == null ) return;
    if ( Tables.getInstance().getDatabase() != null ) {
      // Set up the adapter.
      ListView listView = (ListView) v.findViewById(android.R.id.list);
      // Show or hide the empty as appropriate.
      TextView emptyView = (TextView) v.findViewById(android.R.id.empty);
      final GraphViewAdapter adapter;

      try {
        adapter = new GraphViewAdapter(getActivity(), getAppName(), 
            this.retrieveGraphViews());
      } catch (RemoteException e) {
        WebLogger.getLogger(getAppName()).printStackTrace(e);
        listView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        Toast.makeText(getActivity(), "Unable to access database", Toast.LENGTH_LONG).show();
        return;
      }
      listView.setAdapter(adapter);
      listView.setOnItemClickListener(new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
          TableDisplayActivity activity = (TableDisplayActivity) getActivity();
          // TODO: show the graph display fragment.
          WebLogger.getLogger(getAppName()).d(TAG, "[onItemClick] selected a graph view");
          String graphName = adapter.getGraphViews().get(position).graphName;
          activity.showGraphViewFragment(graphName);
        }

      });
      if (adapter.getCount() == 0) {
        listView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
      } else {
        listView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
      }
    } else {
      // Set up the adapter.
      ListView listView = (ListView) v.findViewById(android.R.id.list);
      // Show or hide the empty as appropriate.
      TextView emptyView = (TextView) v.findViewById(android.R.id.empty);
      listView.setVisibility(View.GONE);
      emptyView.setVisibility(View.VISIBLE);
    }
  }
  
  @Override
  public void databaseUnavailable() {
    
  }
  
  @Override
  public ViewFragmentType getFragmentType() {
    return ViewFragmentType.GRAPH_MANAGER;
  }

}
