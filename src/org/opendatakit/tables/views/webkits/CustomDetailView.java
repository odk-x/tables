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
package org.opendatakit.tables.views.webkits;

import java.io.File;
import java.util.HashMap;

import org.opendatakit.common.android.provider.FileProvider;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;

import android.app.Activity;

/**
 * A view for displaying a customizable detail view of a row of data.
 *
 * @author hkworden
 * @author sudar.sam@gmail.com
 */
public class CustomDetailView extends CustomView {

  private static final String TAG = "CustomDetailView";

  /**************************
   * Strings necessary for the key value store.
   **************************/
  public static final String KVS_PARTITION = "CustomDetailView";

  /**
   * This is the default aspect for the list view. This should be all that is
   * used until we allow multiple list views for a single file.
   */
  public static final String KVS_ASPECT_DEFAULT = "default";

  public static final String KEY_FILENAME = "filename";

  private static final String DEFAULT_HTML = "<html><body>"
      + "<p>No detail view has been specified.</p>" + "</body></html>";

  private Activity mActivity;
  private RowData jsData;
  private KeyValueStoreHelper detailKVSH;
  /** The filename of the html we are displaying. */
  private String mFilename;

  /**
   * Construct the detail view.
   *
   * @param activity
   * @param tp
   * @param filename
   *          the filename to display as the detail view. If null, tries to
   *          receive the value from the key value store.
   */
  public CustomDetailView(Activity activity, TableProperties tp,
      String filename, CustomViewCallbacks callbacks) {
    super(activity, callbacks);
    this.mActivity = activity;
    this.detailKVSH = tp.getKeyValueStoreHelper(CustomDetailView.KVS_PARTITION);
    if (filename == null) {
      String recoveredFilename = this.detailKVSH.getString(CustomDetailView.KEY_FILENAME);
      if (recoveredFilename == null) {
        // Then no default has been set.
        this.mFilename = null;
      } else {
        this.mFilename = recoveredFilename;
      }
    } else {
      // The caller has specified a filename.
      this.mFilename = filename;
    }
    jsData = new RowData(tp);
  }

  public void display(String rowId, UserTable userTable) {
    int rowNum = userTable.getRowNumFromId(rowId);
    HashMap<String, String> tmpData = new HashMap<String, String>();
    for (int i = 0; i < userTable.getWidth(); i++) {
      tmpData.put(userTable.getElementKey(i), userTable.getData(rowNum, i));
    }

    jsData.set(rowId, userTable.getInstanceName(rowNum),
        tmpData);
    Control c = new Control(mActivity);
    webView.addJavascriptInterface(c.getJavascriptInterface(), "control");
    webView.addJavascriptInterface(jsData, "data");
    if (this.mFilename != null) {
      load(FileProvider.getAsUrl(mActivity, new File(mFilename)));
    } else {
      loadData(DEFAULT_HTML, "text/html", null);
    }
    initView();
  }
}
