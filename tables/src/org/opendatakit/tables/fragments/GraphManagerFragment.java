package org.opendatakit.tables.fragments;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.common.android.data.KeyValueStoreEntry;
import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.common.android.utilities.LocalKeyValueStoreConstants;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.utils.GraphViewStruct;
import org.opendatakit.tables.views.components.GraphViewAdapter;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Displays the graphs currently saved in the activity.
 * @author sudar.sam@gmail.com
 *
 */
public class GraphManagerFragment extends AbsTableDisplayFragment {
  
  private static final String TAG = GraphManagerFragment.class.getSimpleName();
  
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    View result = inflater.inflate(
        R.layout.graph_view_manager,
        container,
        false);
    return result;
  }
  
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    // Set up the adapter.
    ListView listView =
        (ListView) this.getView().findViewById(android.R.id.list);
    final GraphViewAdapter adapter =
        new GraphViewAdapter(getActivity(), this.retrieveGraphViews());
    listView.setAdapter(adapter);
    listView.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(
          AdapterView<?> parent,
          View view,
          int position,
          long id) {
        TableDisplayActivity activity = (TableDisplayActivity) getActivity();
        // TODO: show the graph display fragment.
        Log.d(TAG, "[onItemClick] selected a graph view");
        String graphName = retrieveGraphViews().get(position).graphName;
        activity.showGraphViewFragment(graphName);
      }
    
    });
    // Show or hide the empty as appropriate.
    TextView emptyView =
        (TextView) this.getView().findViewById(android.R.id.empty);
    if (adapter.getCount() == 0) {
      listView.setVisibility(View.GONE);
      emptyView.setVisibility(View.VISIBLE);
    } else {
      listView.setVisibility(View.VISIBLE);
      emptyView.setVisibility(View.GONE);
    }
  }
  
  List<GraphViewStruct> retrieveGraphViews() {
    // A graph is currently makred as default if its name is in this aspect
    // marked as the name.
    String tableId = getTableId();
    String currentDefaultGraphName = null;
    List<KeyValueStoreEntry> graphViewEntries = new ArrayList<KeyValueStoreEntry>();
    SQLiteDatabase db = null;
    try {
      db = DatabaseFactory.get().getDatabase(getActivity(), getAppName());
      
      KeyValueStoreHelper kvshForGraphWritLarge =
          new KeyValueStoreHelper(db, tableId, LocalKeyValueStoreConstants.Graph.PARTITION);
      currentDefaultGraphName = kvshForGraphWritLarge.getString(
          LocalKeyValueStoreConstants.Graph.KEY_GRAPH_VIEW_NAME);

      graphViewEntries = ODKDatabaseUtils.get().getDBTableMetadata(db, tableId, 
          LocalKeyValueStoreConstants.Graph.PARTITION_VIEWS, null, LocalKeyValueStoreConstants.Graph.KEY_GRAPH_TYPE);
    } finally {
      if ( db != null ) {
        db.close();
      }
    }
    
    List<GraphViewStruct> result = new ArrayList<GraphViewStruct>();
    for ( KeyValueStoreEntry e : graphViewEntries ) {
      result.add(new GraphViewStruct(
          e.aspect,
          e.value,
          e.aspect.equals(currentDefaultGraphName)));
    }
    return result;
  }

  @Override
  public ViewFragmentType getFragmentType() {
    return ViewFragmentType.GRAPH_MANAGER;
  }

}
