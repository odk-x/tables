package org.opendatakit.tables.fragments;

import org.opendatakit.tables.utils.WebViewUtil;

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
    if (savedInstanceState != null) {
      mSelectedItemIndex =
          savedInstanceState.getInt(INTENT_KEY_SELECTED_INDEX);
    } else {
      this.mSelectedItemIndex = INVALID_INDEX;
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
    Log.d(TAG, "[resetView]");
    if (this.getFileName() == null) {
      // don't need to do anything, as the view won't be getting updated.
      return;
    }
    WebView currentView = (WebView) this.getView();
    // Just reload the page.
    WebViewUtil.displayFileInWebView(
        this.getActivity(),
        this.getAppName(),
        currentView,
        this.getFileName());
  }

  /**
   *
   * @return true if the user has selected a row that should be displayed as
   * selected
   */
  protected boolean itemIsSelected() {
    return this.mSelectedItemIndex != INVALID_INDEX;
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "[onResume]");
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
   * Informs the list view that no item is selected. Resets the state after
   * a call to {@link #setIndexOfSelectedItem(int)}.
   */
  @Override
  public void setNoItemSelected() {
    this.mSelectedItemIndex = INVALID_INDEX;
    this.mTableDataReference.setNoItemSelected();
    this.resetView();
  }

}
