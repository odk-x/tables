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

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import org.opendatakit.activities.IOdkDataActivity;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.views.webkits.OdkTablesWebView;

/**
 * The list view that is displayed in a map.
 *
 * @author Chris Gelon
 * @author sudar.sam@gmail.com
 */
public class MapListViewFragment extends ListViewFragment implements IMapListViewCallbacks {

  /**
   * Represents an index that can't possibly be in the list
   */
  public static final int INVALID_INDEX = -1;
  private static final String TAG = MapListViewFragment.class.getSimpleName();
  /**
   * Saves the index of the element that was selected.
   */
  private static final String INTENT_KEY_SELECTED_INDEX = "keySelectedIndex";
  /**
   * The index of an item that has been selected by the user.
   * We must default to invalid index because the initial load of the list view may take place before onCreate is called
   * I have no idea why
   */
  protected int mSelectedItemIndex = INVALID_INDEX;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // AppName may not be available...
    if (savedInstanceState != null) {
      this.mSelectedItemIndex = savedInstanceState.containsKey(INTENT_KEY_SELECTED_INDEX) ?
          savedInstanceState.getInt(INTENT_KEY_SELECTED_INDEX) :
          INVALID_INDEX;
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(INTENT_KEY_SELECTED_INDEX, mSelectedItemIndex);
  }

  /**
   * Resets the webview (the list), and sets the visibility to visible.
   */
  void resetView() {

    // do not initiate reload until we have the database set up...
    Activity activity = getActivity();
    if (activity instanceof IOdkDataActivity) {
      if (((IOdkDataActivity) activity).getDatabase() == null) {
        return;
      }
    } else {
      Log.e(TAG,
          "Problem: MapListView not being rendered from activity that is an " + "IOdkDataActivity");
      return;
    }

    WebLogger.getLogger(getAppName()).d(TAG, "[resetView]");

    if (getView() == null)
      return; // Can't do anything

    OdkTablesWebView currentView = getWebKit();
    // reload the page.
    // the webkit doesn't like to reload, convince it
    currentView.setForceLoadDuringReload();
    currentView.reloadPage();
  }

  @Override
  public void onResume() {
    super.onResume();
    OdkTablesWebView view = getWebKit();
    if ( view != null ) {
      view.onResume();
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    OdkTablesWebView view = getWebKit();
    if ( view != null ) {
      view.onPause();
    }
  }

  /**
   * Informs the list view that no item is selected. Resets the state after a
   * call to {@link #setIndexOfSelectedItem(int)}.
   */
  @Override
  public void setNoItemSelected() {
    this.mSelectedItemIndex = INVALID_INDEX;
    // TODO: Make map index work with async API
    //this.mTableDataReference.setNoItemSelected();
    this.resetView();
  }

  public int getIndexOfSelectedItem() {
    return this.mSelectedItemIndex;
  }

  /**
   * Sets the index of the list view, which will be the row of the data wanting
   * to be displayed.
   */
  @Override
  public void setIndexOfSelectedItem(final int index) {
    this.mSelectedItemIndex = index;
    // TODO: Make map index work with async API
    //this.mTableDataReference.setSelectedMapIndex(index);
    this.resetView();
  }

}
