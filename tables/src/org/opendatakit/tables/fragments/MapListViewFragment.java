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

/**
 * The list view that is displayed in a map.
 * @author Chris Gelon
 * @author sudar.sam@gmail.com
 *
 */
public class MapListViewFragment extends ListViewFragment implements
    IMapListViewCallbacks {

  private static final String TAG = MapListViewFragment.class.getSimpleName();

  /**
   * The key saving the indices of the rows that should be displayed in this
   * fragment.
   */
  private static final String INTENT_KEY_VISIBLE_INDICES = "keyVisibleIndices";
  /**
   * Saves the index of the element that was selected.
   */
  private static final String INTENT_KEY_SELECTED_INDEX = "keySelectedIndex";

  /**
   * The indices of the rows that should be visible in the list view. Must be
   * an {@link ArrayList} so it can be placed in a {@link Bundle}.
   */
  protected ArrayList<Integer> mSubsetOfIndicesToDisplay;

  /**
   * The index of an item that has been selected by the user.
   */
  protected int mSelectedItemIndex;
  protected static final int INVALID_INDEX = -1;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null) {
      mSubsetOfIndicesToDisplay =
          savedInstanceState.getIntegerArrayList(INTENT_KEY_VISIBLE_INDICES);
      mSelectedItemIndex =
          savedInstanceState.getInt(INTENT_KEY_SELECTED_INDEX);
    } else {
      this.mSelectedItemIndex = INVALID_INDEX;
      this.mSubsetOfIndicesToDisplay = null;
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putIntegerArrayList(
        INTENT_KEY_VISIBLE_INDICES,
        this.mSubsetOfIndicesToDisplay);
    outState.putInt(INTENT_KEY_SELECTED_INDEX, this.mSelectedItemIndex);
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
    // and update the reference (any old reference has been released in JS)
    this.mControlReference = control;
    this.mTableDataReference = tableData;
    return result;
  }

  /**
   * Resets the webview (the list), and sets the visibility to visible.
   */
  void resetView() {
    Log.d(TAG, "[resetView]");
    if (this.getFileName() == null) {
      // don't need to do anything, as the view won't be getting updated.
      return;
    }
    WebView currentView = (WebView) this.getView();
    // Replace the data object.
    TableData tableData = this.createDataObject();
    currentView.addJavascriptInterface(
        tableData.getJavascriptInterfaceWithWeakReference(),
        Constants.JavaScriptHandles.DATA);
    WebViewUtil.displayFileInWebView(
        this.getActivity(),
        this.getAppName(),
        currentView,
        this.getFileName());
    // and update the reference (any old reference has been released in JS)
    this.mTableDataReference = tableData;
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
  protected boolean itemIsSelected() {
    return this.mSelectedItemIndex != INVALID_INDEX;
  }

  /**
   * Get the {@link UserTable} to display. Return a table with only
   * those described by the indices in that list.
   */
  @Override
  UserTable getUserTable() {
    List<Integer> indicesToDisplay = this.createListOfIndicesToDisplay();
    // Return the specific ordered list.
    UserTable result = new UserTable(super.getUserTable(), indicesToDisplay);
    return result;
  }

  /**
   * Create the list of indices to be displayed in the list view.
   * The selected item index will be first, and will be removed
   * from later in the list of indices. If there is no visible subset
   * of indices defined, all indices will be displayed.
   *
   * @return
   */
  List<Integer> createListOfIndicesToDisplay() {
    ArrayList<Integer> result = new ArrayList<Integer>();
    if (this.itemIsSelected()) {
      result.add(this.mSelectedItemIndex);
    }
    if ( this.mSubsetOfIndicesToDisplay == null ) {
      // visible indices are 0..n from base user table.
      for (int idx = 0; idx < super.getUserTable().getNumberOfRows(); idx++) {
        if ( idx != this.mSelectedItemIndex ) {
          result.add(idx);
        }
      }
    } else {
      // visible indices were told to us.
      for ( int i = 0 ; i < this.mSubsetOfIndicesToDisplay.size() ; ++i ) {
        Integer idx = this.mSubsetOfIndicesToDisplay.get(i);
        if ( idx != this.mSelectedItemIndex ) {
          result.add(idx);
        }
      }
    }
    return result;
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "[onResume]");
  }

  @Override
  public void setSubsetOfIndicesToDisplay(ArrayList<Integer> indices) {
    this.mSubsetOfIndicesToDisplay = indices;
    this.resetView();
  }

  @Override
  public ArrayList<Integer> getMapListIndices() {
    return this.mSubsetOfIndicesToDisplay;
  }

  /**
   * Sets the index of the list view, which will be the row of the data wanting
   * to be displayed.
   */
  @Override
  public void setIndexOfSelectedItem(final int index) {
    this.mSelectedItemIndex = index;
    this.resetView();
  }

  /**
   * Informs the list view that no item is selected. Resets the state after
   * a call to {@link #setIndexOfSelectedItem(int)}.
   */
  @Override
  public void setNoItemSelected() {
    this.mSelectedItemIndex = INVALID_INDEX;
    this.resetView();
  }

  @Override
  public void setDisplayAllItems() {
    this.mSubsetOfIndicesToDisplay = null;
    this.resetView();
  }

  @Override
  public boolean displayingSubsetOfTable() {
    return (this.mSubsetOfIndicesToDisplay != null);
  }

}
