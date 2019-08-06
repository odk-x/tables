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

import androidx.fragment.app.Fragment;
import android.content.Context;
import org.opendatakit.database.data.OrderedColumns;
import org.opendatakit.database.data.UserTable;
import org.opendatakit.tables.activities.AbsTableActivity;
import org.opendatakit.tables.activities.TableDisplayActivity;

/**
 * The base class for any {@link Fragment} that displays a table.
 *
 * @author sudar.sam@gmail.com
 */
public abstract class AbsTableDisplayFragment extends AbsTablesFragment {

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (!(context instanceof TableDisplayActivity)) {
      throw new IllegalStateException(
          "fragment must be attached to a " + TableDisplayActivity.class.getSimpleName());
    }
  }

  /**
   * Get the tableId of the active table.
   *
   * @return the table id
   */
  public String getTableId() {
    return ((AbsTableActivity) getActivity()).getTableId();
  }

  /**
   * Get the description of the table.
   *
   * @return the columns of the active table
   */
  public OrderedColumns getColumnDefinitions() {
    return ((AbsTableActivity) getActivity()).getColumnDefinitions();
  }

  /**
   * Get the {@link UserTable} being held by the {@link TableDisplayActivity}.
   *
   * @return the user table (data in the table) from the enclosing activity
   */
  public UserTable getUserTable() {
    return ((TableDisplayActivity) getActivity()).getUserTable();
  }

}
