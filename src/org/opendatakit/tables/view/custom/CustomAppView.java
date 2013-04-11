/*
 * Copyright (C) 2012 University of Washington
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
package org.opendatakit.tables.view.custom;

import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.util.TableFileUtils;

import android.content.Context;
import android.view.ViewGroup;

/**
 * The view that supports a custom home screen for an app. It will support html
 * and javascript and serve as an alternative first screen to the TableManager,
 * and will be customizable.
 * <p>
 * Built following the model of CustomTableView.
 * 
 * @author sudar.sam@gmail.com
 *
 */
public class CustomAppView extends CustomView {
  
  /**
   * The filename of the html that defines the custom homescreen of the app.
   */
  public static final String CUSTOM_FILE_NAME = "homescreen.html";
  
  private static final String DEFAULT_HTML =
      "<html><body>" +
      "<p>No filename has been specified.</p>" +
      "</body></html>";
  
  private Context mContext;
  private DbHelper mDbHelper;
  
  /**
   * Create the view. 
   * @param context
   */
  public CustomAppView(Context context) {
    super(context);
    this.mContext = context;
    this.mDbHelper = DbHelper.getDbHelper(context);
    this.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, 
        LayoutParams.FILL_PARENT));
  }
  
  public void display() {
    webView.addJavascriptInterface(new Control(mContext), "control");
    // We're going to assume this is only being called if homescreen.html has
    // been found, so we're just going to use that, not do any checking.
    String dir = ODKFileUtils.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME);
    load("file:///" + dir + "/" + CUSTOM_FILE_NAME);
    initView();
  }
  
}
