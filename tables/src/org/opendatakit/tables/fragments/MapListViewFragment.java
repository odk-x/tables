package org.opendatakit.tables.fragments;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.WebViewUtil;
import org.opendatakit.tables.views.webkits.Control;
import org.opendatakit.tables.views.webkits.TableData;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

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
  
  /**
   * The index of an item that has been selected by the user.
   */
  private int mSelectedItemIndex;
  
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
  
  /**
   * Resets the webview (the list), and sets the visibility to visible.
   */
  private void resetView() {
    if (this.getFileName() == null) {
      // don't need to do anything, as the view won't be getting updated.
      return;
    }
    WebView currentView = (WebView) this.getView();
    // Replace the data object.
    TableData tableData = this.createDataObject();
    this.mTableDataReference = tableData;
    currentView.addJavascriptInterface(
        tableData.getJavascriptInterfaceWithWeakReference(),
        Constants.JavaScriptHandles.DATA);
    WebViewUtil.displayFileInWebView(
        this.getActivity(),
        this.getAppName(),
        currentView,
        this.getFileName());
  }
  
  @Override
  protected TableData createDataObject() {
    UserTable backingTable = this.getUserTable();
    TableData result = new TableData(backingTable);
    return result;
  }
  
  /**
   * 
   * @return true if the user has selected a row that should be displayed as
   * selected
   */
  private boolean itemIsSelected() {
    return this.mSelectedItemIndex >= 0;
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
      result = new UserTable(
          super.getUserTable(),
          this.createListOfIndicesToDisplay());
    }
    return result;
  }
  
  /**
   * Create the list of indices to be displayed in the list view to the user.
   * The selected item index will be first, and will have only a single entry.
   * @return
   */
  List<Integer> createListOfIndicesToDisplay() {
    if (this.itemIsSelected()) {
      List<Integer> result = new ArrayList<Integer>();
      result.add(this.mSelectedItemIndex);
      for (int i = 0; i < this.mVisibleRowIndices.size(); i++) {
        int currentIndex = this.mVisibleRowIndices.get(i);
        if (currentIndex != this.mSelectedItemIndex) {
          result.add(currentIndex);
        } else {
          // already added to the list--don't add again.
          continue;
        }
      }
      return result;
    } else {
      return this.mVisibleRowIndices;
    }
    
  }
  
  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "[onResume]");
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


  /**
   * Sets the index of the list view, which will be the row of the data wanting
   * to be displayed.
   */
  public void setMapListIndex(final int index) {
    this.mSelectedItemIndex = index;
    this.resetView();
  }

  /**
   * Sets the indexes of the list view, which will be the rows of data wanting
   * to be displayed.
   */
  public void setMapListIndexes(ArrayList<Integer> indexes) {
    if (this.mVisibleRowIndices != null) {
      this.mVisibleRowIndices.clear();
    }
    this.mVisibleRowIndices.addAll(indexes);
    this.resetView();
  }

}
