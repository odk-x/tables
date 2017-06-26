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
package org.opendatakit.tables.views.components;

import android.content.Context;
import android.widget.ArrayAdapter;
import org.opendatakit.data.TableViewType;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.data.PossibleTableViewTypes;

/**
 * Adapter that displays {@see TableViewType} options.
 * <p>
 * Used in DefaultViewTypePreference
 *
 * @author sudar.sam@gmail.com
 */
public class TableViewTypeAdapter extends ArrayAdapter<CharSequence> {

  /**
   * Used for logging
   */
  private static final String TAG = TableViewTypeAdapter.class.getSimpleName();
  /**
   * The app name
   */
  private final String mAppName;
  private PossibleTableViewTypes mPossibleViewTypes;
  private CharSequence[] mViewTypeValues;

  /**
   * Sets up local variables for a TableViewTypeAdapter
   *
   * @param context     unused
   * @param appName     the app name
   * @param resource    unused
   * @param entries     unused
   * @param entryValues A list of all view types, spreadsheet, list and map
   * @param viewTypes   An object with methods that can tell us which view types (map, spreadsheet,
   *                    list) the user should be able to select
   */
  public TableViewTypeAdapter(Context context, String appName, int resource, CharSequence[] entries,
      CharSequence[] entryValues, PossibleTableViewTypes viewTypes) {
    super(context, resource, entries);
    mAppName = appName;
    mViewTypeValues = entryValues;
    mPossibleViewTypes = viewTypes;
  }

  @Override
  public boolean areAllItemsEnabled() {
    // so we get asked about individual availability.
    return false;
  }

  @Override
  public boolean isEnabled(int position) {
    if (this.mPossibleViewTypes == null) {
      return false;
    }

    String currentItem = mViewTypeValues[position].toString();
    if (currentItem.equals(TableViewType.SPREADSHEET.name())) {
      return mPossibleViewTypes.spreadsheetViewIsPossible();
    } else if (currentItem.equals(TableViewType.LIST.name())) {
      return mPossibleViewTypes.listViewIsPossible();
    } else if (currentItem.equals(TableViewType.MAP.name())) {
      return mPossibleViewTypes.mapViewIsPossible();
    } else {
      // Enable it.
      WebLogger.getLogger(mAppName).e(TAG, "unrecognized entryValue: " + currentItem);
      return true;
    }
  }

}
