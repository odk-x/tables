package org.opendatakit.tables.fragments;

import java.util.ArrayList;

import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.WebViewUtil;
import org.opendatakit.tables.views.webkits.Control;
import org.opendatakit.tables.views.webkits.TableData;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

/**
 * The list view that is displayed in a map.
 * @author Chris Gelon
 * @author sudar.sam@gmail.com
 *
 */
public class MapListViewFragment extends ListViewFragment implements
    IMapListViewCallbacks{
  
  private static final String TAG = MapListViewFragment.class.getSimpleName();
  
  /**
   * The key saving the indices of the rows that should be displayed in this
   * fragment.
   */
  private static final String INTENT_KEY_VISIBLE_INDICES = "keyVisibleIndices";
  
  /** The indices of the rows that should be visible in the list view. */
  private ArrayList<Integer> mVisibleRowIndices;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null) {
      mVisibleRowIndices =
          savedInstanceState.getIntegerArrayList(INTENT_KEY_VISIBLE_INDICES);
    }
  }
  
  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putIntegerArrayList(
        INTENT_KEY_VISIBLE_INDICES,
        this.mVisibleRowIndices);
  }
  
  @Override
  public WebView buildView() {
    Log.d(TAG, "[buildView]");
    WebView result = WebViewUtil.getODKCompliantWebView(getActivity());
    Control control = this.createControlObject();
    result.addJavascriptInterface(
        control.getJavascriptInterfaceWithWeakReference(),
        Constants.JavaScriptHandles.CONTROL);
    TableData tableData = this.createDataObject();
    result.addJavascriptInterface(
        tableData.getJavascriptInterfaceWithWeakReference(),
        Constants.JavaScriptHandles.DATA);
    WebViewUtil.displayFileInWebView(
        this.getActivity(),
        this.getAppName(),
        result,
        this.getFileName());
    // Now save the references.
    this.mControlReference = control;
    this.mTableDataReference = tableData;
    return result;
  }
  
  @Override
  protected TableData createDataObject() {
    UserTable backingTable = this.getUserTable();
    TableData result = new TableData(backingTable);
    return result;
  }
  
  /**
   * Get the {@link UserTable} to display. If {@link #getMapListIndices()}
   * returns null, displays everything. Otherwise return a table with only
   * those described by the indices in that list.
   */
  @Override
  UserTable getUserTable() {
    UserTable result = null;
    if (this.getMapListIndices() == null) {
      result = super.getUserTable();
    } else {
      // Return only the subset.
      result = new UserTable(super.getUserTable(), getMapListIndices());
    }
    return result;
  }
  
  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "[onResume]");
  }

  @Override
  public void setMapListIndex(int index) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setMapListIndices(ArrayList<Integer> indices) {
    this.mVisibleRowIndices = indices;
  }

  @Override
  public ArrayList<Integer> getMapListIndices() {
    return this.mVisibleRowIndices;
  }
  
  // TODO uncomment and implement
//  /**
//   * Resets the webview (the list), and sets the visibility to visible.
//   */
//  private void resetView() {
//    if (mIndexes != null && mIndexes.size() > 0) {
//      TableProperties tp = ((TableActivity) getActivity()).getTableProperties();
//      UserTable table = ((TableActivity) getActivity()).getTable();
//      // Grab the key value store helper from the map fragment.
//      final KeyValueStoreHelper kvsHelper = tp
//          .getKeyValueStoreHelper(TableMapFragment.KVS_PARTITION);
//      // Find which file stores the html information for displaying
//      // the list.
//      String filename = kvsHelper.getString(TableMapFragment.KEY_FILENAME);
//      if (filename == null) {
//        Toast.makeText(getActivity(), getActivity().getString(R.string.list_view_file_not_set),
//            Toast.LENGTH_LONG).show();
//        return;
//      }
//      // Create the custom view and set it.
//      CustomTableView view = CustomTableView.get(getActivity(), tp.getAppName(), table,
//          filename, mIndexes, getFragmentManager()
//          .findFragmentByTag(Constants.FragmentTags.MAP_INNER_MAP));
//      view.display();
//
//      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//          LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
//      mContainer.removeAllViews();
//      mContainer.addView(view, params);
//      WebViewClient client = new WebViewClient() {
//        public void onPageFinished(WebView view, String url) {
//          mContainer.setVisibility(View.VISIBLE);
//        }
//      };
//
//      view.setOnFinishedLoaded(client);
//    }
//  }
//
//  /**
//   * Sets the index of the list view, which will be the row of the data wanting
//   * to be displayed.
//   */
//  public void setMapListIndex(final int index) {
//    if (mIndexes != null) {
//      mIndexes.clear();
//    }
//    mIndexes = new ArrayList<Integer>();
//    if (index >= 0) {
//      mIndexes.add(index);
//    }
//    resetView();
//  }
//
//  /**
//   * Sets the indexes of the list view, which will be the rows of data wanting
//   * to be displayed.
//   */
//  public void setMapListIndexes(ArrayList<Integer> indexes) {
//    if (mIndexes != null) {
//      mIndexes.clear();
//    }
//    mIndexes = indexes;
//    resetView();
//  }
//
//  /**
//   * Sets the visibility of this fragment (will set the container's visibility).
//   * Will only change the visibility if it isn't a tablet (this container is
//   * always visible when run by a tablet).
//   *
//   * @param visibility
//   *          The new visibility of the fragment (constants found in View).
//   */
//  public void setVisibility(int visibility) {
//    if (!ActivityUtil.isTabletDevice(getActivity())) {
//      mContainer.setVisibility(visibility);
//    }
//  }

}
