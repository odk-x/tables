/*
 * Copyright (C) 2017 University of Washington
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

package org.opendatakit.tables.views.webkits;

import android.os.Bundle;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.views.ODKWebView;

import java.lang.ref.WeakReference;

/**
 * Created by jbeorse on 5/9/17.
 */

public class OdkTables {

   private static final String TAG = OdkTables.class.getSimpleName();

   private WeakReference<ODKWebView> mWebView;
   protected AbsBaseActivity mActivity;

   /**
    *
    * @param activity
    *          the activity that will be holding the view
    */
   public OdkTables(AbsBaseActivity activity, ODKWebView webView) {
      this.mActivity = activity;
      this.mWebView = new WeakReference<ODKWebView>(webView);
   }

   public boolean isInactive() {
      return (mWebView.get() == null) || mWebView.get().isInactive();
   }

   public OdkTablesIf getJavascriptInterfaceWithWeakReference() {
      return new OdkTablesIf(this);
   }

   /**
    * Set the list view contents for a detail with list view
    *
    * @param tableId
    * @param relativePath
    *          the path relative to the app folder
    * @param sqlWhereClause
    * @param sqlSelectionArgs
    * @return
    */
   public boolean helperSetSubListView(String tableId, String relativePath,
       String sqlWhereClause, String[] sqlSelectionArgs,
       String[] sqlGroupBy, String sqlHaving,
       String sqlOrderByElementKey, String sqlOrderByDirection) {
      return this.helperUpdateView(tableId, sqlWhereClause, sqlSelectionArgs, sqlGroupBy, sqlHaving,
          sqlOrderByElementKey, sqlOrderByDirection, ViewFragmentType.SUB_LIST, relativePath);
   }

   /**
    * Send a bundle to update a view without opening a new activity.
    *
    * @param tableId
    * @param sqlWhereClause
    * @param sqlSelectionArgs
    * @param sqlGroupBy
    * @param sqlHaving
    * @param sqlOrderByElementKey
    * @param sqlOrderByDirection
    * @param viewType
    * @param relativePath
    * @return
    * @throws IllegalArgumentException
    *           if viewType is not a sub view
    */
   boolean helperUpdateView(String tableId, String sqlWhereClause, String[] sqlSelectionArgs,
       String[] sqlGroupBy, String sqlHaving, String sqlOrderByElementKey,
       String sqlOrderByDirection, ViewFragmentType viewType, String relativePath) {
      if (viewType != ViewFragmentType.SUB_LIST) {
         throw new IllegalArgumentException("Cannot use this method to update a view that doesn't " +
             "support updates. Currently only DetailWithListView's Sub List supports this action");
      }

      Bundle bundle = new Bundle();
      IntentUtil.addSQLKeysToBundle(bundle, sqlWhereClause, sqlSelectionArgs, sqlGroupBy, sqlHaving,
          sqlOrderByElementKey, sqlOrderByDirection);
      IntentUtil.addTableIdToBundle(bundle, tableId);
      IntentUtil.addFragmentViewTypeToBundle(bundle, viewType);
      IntentUtil.addFileNameToBundle(bundle, relativePath);

      switch(viewType) {
      case SUB_LIST:
         TableDisplayActivity activity = (TableDisplayActivity) mActivity;
         activity.updateFragment(Constants.FragmentTags.DETAIL_WITH_LIST_LIST, bundle);
         break;
      default: // This is unreachable
         break;
      }
      return true;
   }

}
