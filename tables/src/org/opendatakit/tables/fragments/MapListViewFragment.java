package org.opendatakit.tables.fragments;

import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.views.webkits.TableData;

import android.os.Bundle;
import android.webkit.WebView;

/**
 * The list view that is displayed in a map.
 * 
 * @author Chris Gelon
 * @author sudar.sam@gmail.com
 *
 */
public class MapListViewFragment extends ListViewFragment implements IMapListViewCallbacks {

  private static final String TAG = MapListViewFragment.class.getSimpleName();

  /**
   * Saves the index of the element that was selected.
   */
  private static final String INTENT_KEY_SELECTED_INDEX = "keySelectedIndex";

  /**
   * The index of an item that has been selected by the user.
   */
  protected int mSelectedItemIndex;
  protected static final int INVALID_INDEX = -1;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.mSelectedItemIndex = this.retrieveSelectedItemIndexFromBundle(savedInstanceState);
    WebLogger.getLogger(getAppName()).d(TAG,
        "[onCreate] retrieved selected index: " + this.mSelectedItemIndex);
  }

  @Override
  protected TableData createDataObject() {
    // We need to account for the fact that we had previously selected an item.
    TableData result = super.createDataObject();
    if (mSelectedItemIndex != INVALID_INDEX) {
      result.setSelectedMapIndex(this.mSelectedItemIndex);
    } else {
      result.setNoItemSelected();
    }
    return result;
  }

  int retrieveSelectedItemIndexFromBundle(Bundle bundle) {
    if (bundle != null && bundle.containsKey(INTENT_KEY_SELECTED_INDEX)) {
      return bundle.getInt(INTENT_KEY_SELECTED_INDEX);
    } else {
      return INVALID_INDEX;
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(INTENT_KEY_SELECTED_INDEX, this.mSelectedItemIndex);
  }

  /**
   * Resets the webview (the list), and sets the visibility to visible.
   */
  void resetView() {
    WebLogger.getLogger(getAppName()).d(TAG, "[resetView]");
    if (this.getFileName() == null) {
      // don't need to do anything, as the view won't be getting updated.
      return;
    }
    WebView currentView = (WebView) this.getView();
    // Just reload the page.
    currentView.reload();
  }

  /**
   *
   * @return true if the user has selected a row that should be displayed as
   *         selected
   */
  protected boolean itemIsSelected() {
    return this.mSelectedItemIndex != INVALID_INDEX;
  }

  @Override
  public void onResume() {
    super.onResume();
    WebLogger.getLogger(getAppName()).d(TAG, "[onResume]");
  }

  /**
   * Sets the index of the list view, which will be the row of the data wanting
   * to be displayed.
   */
  @Override
  public void setIndexOfSelectedItem(final int index) {
    this.mSelectedItemIndex = index;
    this.mTableDataReference.setSelectedMapIndex(index);
    this.resetView();
  }

  /**
   * Informs the list view that no item is selected. Resets the state after a
   * call to {@link #setIndexOfSelectedItem(int)}.
   */
  @Override
  public void setNoItemSelected() {
    this.mSelectedItemIndex = INVALID_INDEX;
    this.mTableDataReference.setNoItemSelected();
    this.resetView();
  }

}
