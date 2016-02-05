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

import android.app.Fragment;
import android.os.Bundle;
import org.opendatakit.IntentConsts;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.utils.IntentUtil;

/**
 * {@link Fragment} for displaying a detail view.
 * 
 * @author sudar.sam@gmail.com
 *
 */
public class DetailViewFragment extends AbsWebTableFragment {

  private static final String TAG = DetailViewFragment.class.getSimpleName();
  
  /**
   * The row id of the row that is being displayed in this table.
   */
  private String mRowId;

  /**
   * Retrieve the row id from the bundle.
   * 
   * @param bundle
   *          the row id, or null if not present.
   * @return
   */
  String retrieveRowIdFromBundle(Bundle bundle) {
    String rowId = IntentUtil.retrieveRowIdFromBundle(bundle);
    return rowId;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String retrievedRowId = this.retrieveRowIdFromBundle(this.getArguments());
    this.mRowId = retrievedRowId;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(IntentConsts.INTENT_KEY_INSTANCE_ID, this.getRowId());
  }

  @Override
  public ViewFragmentType getFragmentType() {
    return ViewFragmentType.DETAIL;
  }

  /**
   * Get the id of the row being displayed.
   * 
   * @return
   */
  public String getRowId() {
    return this.mRowId;
  }

}
