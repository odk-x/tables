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

import org.opendatakit.common.android.data.PossibleTableViewTypes;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.common.android.utilities.WebLogger;

import android.content.Context;
import android.widget.ArrayAdapter;

/**
 * Adapter that displays {@link TableViewType} options.
 * @author sudar.sam@gmail.com
 *
 */
public class TableViewTypeAdapter extends ArrayAdapter<CharSequence> {
  
  private static final String TAG = TableViewTypeAdapter.class.getSimpleName();
  
  private PossibleTableViewTypes mPossibleViewTypes;
  private CharSequence[] mViewTypeValues;
  private Context mContext;
  private final String mAppName;

  public TableViewTypeAdapter(
      Context context,
      String appName,
      int resource,
      CharSequence[] entries,
      CharSequence[] entryValues,
      PossibleTableViewTypes viewTypes) {
    super(context, resource, entries);
    this.mContext = context;
    this.mAppName = appName;
    this.mViewTypeValues = entryValues;
    this.mPossibleViewTypes = viewTypes;
  }
  
  @Override
  public boolean areAllItemsEnabled() {
    // so we get asked about individual availability.
    return false;
  }
  
  
  
  
  
  @Override
  public boolean isEnabled(int position) {
    String currentItem = this.mViewTypeValues[position].toString();
    if (currentItem.equals(TableViewType.SPREADSHEET.name())) {
      if (this.mPossibleViewTypes.spreadsheetViewIsPossible()) {
        return true;
      } else {
        return false;
      }
    } else if (currentItem.equals(TableViewType.LIST.name())) {
      if (this.mPossibleViewTypes.listViewIsPossible()) {
        return true;
      } else {
        return false;
      }
    } else if (currentItem.equals(TableViewType.MAP.name())) {
      if (this.mPossibleViewTypes.mapViewIsPossible()) {
        return true;
      } else {
        return false;
      }
    } else if (currentItem.equals(TableViewType.GRAPH.name())) {
      if (this.mPossibleViewTypes.graphViewIsPossible()) {
        return true;
      } else {
        return false;
      }
    } else {
      // Enable it.
      WebLogger.getLogger(mAppName).e(TAG, "unrecognized entryValue: " + currentItem);
      return true;
    }
  }

}
