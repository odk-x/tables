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

import java.util.ArrayList;

import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.tables.activities.TableDisplayActivity;

import android.app.Activity;
import android.app.Fragment;

/**
 * The base class for any {@link Fragment} that displays a table.
 * @author sudar.sam@gmail.com
 *
 */
public abstract class AbsTableDisplayFragment extends AbsBaseFragment {
  
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (!(activity instanceof TableDisplayActivity)) {
      throw new IllegalStateException("fragment must be attached to a " +
          TableDisplayActivity.class.getSimpleName());
    }
  }
  
  /**
   * Get the tableId of the active table.
   * @return
   */
  public String getTableId() {
    UserTable table = this.getUserTable();
    return table.getTableId();
  }
  
  /**
   * Get the description of the table.
   * 
   * @return
   */
  public ArrayList<ColumnDefinition> getColumnDefinitions() {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();
    return activity.getColumnDefinitions();
  }
  
  /**
   * Get the {@link UserTable} being held by the {@link TableDisplayActivity}.
   * @return
   */
  public UserTable getUserTable() {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();
    UserTable result = activity.getUserTable();
    return result;
  }
  
  /** Return the type of this fragment. */
  public abstract TableDisplayActivity.ViewFragmentType getFragmentType();

}
