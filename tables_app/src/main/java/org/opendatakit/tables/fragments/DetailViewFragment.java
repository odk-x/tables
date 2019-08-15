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
import android.view.View;
import org.opendatakit.activities.IOdkCommonActivity;
import org.opendatakit.database.data.BaseTable;
import org.opendatakit.database.queries.BindArgs;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.application.Tables;
/**
 * {@link Fragment} for displaying a detail view.
 *
 * @author sudar.sam@gmail.com
 */
public class DetailViewFragment extends AbsWebTableFragment {

  /**
   * Used for logging
   */
  @SuppressWarnings("unused")
  private static final String TAG = DetailViewFragment.class.getSimpleName();
  public void databaseAvailable() {
    super.databaseAvailable();
    View view = getView();
    if (view != null) {
      view.post(new Runnable() {
        @Override
        public void run() {
          checkAccess();
        }
      });
    }
  }
  private void checkAccess() {
    String tableId = ((IOdkCommonActivity) getActivity()).getTableId();
    String rowId = ((IOdkCommonActivity) getActivity()).getInstanceId();
    UserDbInterface dbInt = Tables.getInstance().getDatabase();
    boolean can_edit = true;
    try {
      DbHandle db = dbInt.openDatabase(getAppName());

      BaseTable result = dbInt
              // we know it's safe to dump the table id in there because we got it from the TDA
              .arbitrarySqlQuery(getAppName(), db,tableId, "SELECT * FROM "+tableId+" WHERE _id = ?",
                      new BindArgs(new String[] {rowId}), 1, 0);
      String access = result.getRowAtIndex(0).getRawStringByKey("_effective_access");
      if (access != null) {
        can_edit = access.contains("w");
      }
      dbInt.closeDatabase(getAppName(), db);
    } catch (Exception e) {
      WebLogger.getLogger(getAppName()).printStackTrace(e);
    }
    if (!can_edit) {
      View edit_button = getActivity().findViewById(R.id.menu_edit_row);
      if (edit_button != null) {
        edit_button.setVisibility(View.GONE);
      }
    }
  }
}
