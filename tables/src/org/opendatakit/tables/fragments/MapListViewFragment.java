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

}
