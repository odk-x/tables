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
package org.opendatakit.tables.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.views.ODKWebView;

/**
 * Created by jbeorse on 5/1/17.
 * The fragment with two webviews on one screen, one list and one detail
 */
public class DetailWithListDetailViewFragment extends AbsWebTableFragment {

  private static final String TAG = DetailWithListDetailViewFragment.class.getSimpleName();

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View newView = super.onCreateView(inflater, container, savedInstanceState);

    if (newView == null) {
      WebLogger.getLogger(getAppName()).e(TAG, "[onCreateView] parent view was null");
      return null;
    }

    ODKWebView webView = newView.findViewById(R.id.webkit);
    if (webView == null) {
      WebLogger.getLogger(getAppName()).e(TAG, "[onCreateView] web view was null");
      return newView;
    }
    webView.setContainerFragmentID(Constants.FragmentTags.DETAIL_WITH_LIST_DETAIL);
    return newView;
  }
}
