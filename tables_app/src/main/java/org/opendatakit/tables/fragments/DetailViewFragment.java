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
import android.os.RemoteException;
import android.webkit.WebView;
import android.widget.Toast;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.common.android.views.OdkData;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.WebViewUtil;
import org.opendatakit.tables.views.webkits.OdkCommon;
import org.opendatakit.tables.views.webkits.OdkTables;

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
   * The {@link UserTable} this view is displaying, consisting of a single row.
   */
  private UserTable mSingleRowTable;

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
    outState.putString(Constants.IntentKeys.ROW_ID, this.getRowId());
  }
  
  @Override
  public void databaseAvailable() {
    if ( Tables.getInstance().getDatabase() != null && getView() != null ) {
      try {
        AbsBaseActivity activity = (AbsBaseActivity) getActivity();
        WebView webView = (WebView) getView().findViewById(org.opendatakit.tables.R.id.webkit);
        this.mSingleRowTable = this.retrieveSingleRowTable();
        OdkTables odkTables = this.createControlObject();
        webView.addJavascriptInterface(odkTables.getJavascriptInterfaceWithWeakReference(),
            Constants.JavaScriptHandles.CONTROL);
        OdkCommon odkCommon = this.createCommonObject();
        webView.addJavascriptInterface(odkCommon.getJavascriptInterfaceWithWeakReference(),
            Constants.JavaScriptHandles.COMMON);
        OdkData odkData;
        odkData = this.getDataReference();
        webView.addJavascriptInterface(
                odkData.getJavascriptInterfaceWithWeakReference(),
                Constants.JavaScriptHandles.DATAIF);
        setWebKitVisibility();
        // Now save the references.
        this.mOdkTablesReference = odkTables;
        this.mOdkCommonReference = odkCommon;
        WebViewUtil.displayFileInWebView(activity, getAppName(), webView, getFileName());
      } catch (RemoteException e) {
        WebLogger.getLogger(getAppName()).printStackTrace(e);
        Toast.makeText(getActivity(), 
            getActivity().getString(R.string.abort_error_accessing_database), 
            Toast.LENGTH_LONG).show();
      }
    }
    // TODO: when control changes, we probably don't need to do all the changes above.
    super.databaseAvailable();
  }
  
  @Override
  public void databaseUnavailable() {
    setWebKitVisibility();
    super.databaseUnavailable();
  }

  /**
   * Get the {@link UserTable} consisting of one row being displayed by this
   * detail view.
   * 
   * @return
   */
  UserTable getSingleRowTable() {
    return this.mSingleRowTable;
  }

  /**
   * Retrieve the single row table to display in this view.
   * 
   * @return
   * @throws RemoteException 
   */
  UserTable retrieveSingleRowTable() throws RemoteException {
    if (this.mRowId == null) {
      WebLogger.getLogger(getAppName()).e(TAG,
          "asking to retrieve single row table for null row id");
    }
    String rowId = getRowId();
    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(getAppName());
      UserTable result = Tables.getInstance().getDatabase().getRowsWithId
          (getAppName(), db,
          getTableId(), getColumnDefinitions(), rowId);
      if (result.getNumberOfRows() > 1) {
        WebLogger.getLogger(getAppName()).e(TAG,
            "Single row table for row id " + rowId + " returned > 1 row");
      }
      return result;
    } finally {
      if (db != null) {
        Tables.getInstance().getDatabase().closeDatabase(getAppName(), db);
      }
    }
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
