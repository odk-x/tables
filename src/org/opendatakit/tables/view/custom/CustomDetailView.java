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

import java.util.Map;

import org.opendatakit.tables.Activity.util.CustomViewUtil;
import org.opendatakit.tables.data.TableProperties;

import android.content.Context;

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
    
    private static final String DEFAULT_HTML =
        "<html><body>" +
        "<p>No detail view has been specified.</p>" +
        "</body></html>";
    
    private Context context;
    private TableProperties tp;
    private RowData jsData;
    
    public CustomDetailView(Context context, TableProperties tp) {
        super(context);
        this.context = context;
        this.tp = tp;
        jsData = new RowData(tp);
    }
    
    public void display(String rowId, Map<String, String> data) {
        jsData.set(data);
        webView.addJavascriptInterface(new Control(context), "control");
        webView.addJavascriptInterface(jsData, "data");
        String filename = tp.getStringEntry(CustomDetailView.KVS_PARTITION, 
            CustomDetailView.KVS_ASPECT_DEFAULT,
            CustomDetailView.KEY_FILENAME);
        if (filename != null) {
            load("file:///" + filename);
        } else {
            loadData(DEFAULT_HTML, "text/html", null);
        }
        initView();
    }
}
