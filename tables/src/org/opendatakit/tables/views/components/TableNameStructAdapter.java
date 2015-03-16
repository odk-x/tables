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
package org.opendatakit.tables.views.components;

import java.util.List;

import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.utils.TableNameStruct;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * An adapter for displaying TableProperties.
 * 
 * @author sudar.sam@gmail.com
 *
 */
public class TableNameStructAdapter extends ArrayAdapter<TableNameStruct> {

  private static final String TAG = TableNameStructAdapter.class.getSimpleName();
  
  private String mAppName;

  public TableNameStructAdapter(AbsBaseActivity context, List<TableNameStruct> values) {
    super(context, R.layout.row_item_with_preference);
    this.mAppName = context.getAppName();
    this.clear();
    this.addAll(values);
  }

  @Override
  public View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
    if ( convertView == null ) {
      convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_item_with_preference, parent, false);
    }
    final RelativeLayout view = (RelativeLayout) convertView;
    TextView textView = (TextView) view.findViewById(R.id.row_item_text);

    TableNameStruct nameStruct = getItem(position);

    textView.setText(nameStruct.getLocalizedDisplayName());
    ImageView imageView = (ImageView) view.findViewById(R.id.row_item_icon);
    imageView.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        // We want to pop open the context menu. Therefore anything using this
        // must have registered the item for a click.
        view.performLongClick();
      }
    });
    return view;
  }

}
