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

import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.utils.TableNameStruct;

import java.util.List;

/**
 * An adapter for displaying TableProperties.
 *
 * @author sudar.sam@gmail.com
 */
public class TableNameStructAdapter extends ArrayAdapter<TableNameStruct> {

  /**
   * Used for logging
   */
  @SuppressWarnings("unused")
  private static final String TAG = TableNameStructAdapter.class.getSimpleName();

  /**
   * Constructs a new TableNameStructAdapter with a set of values to be added
   *
   * @param context unused
   * @param values  All added to the array adapter
   */
  public TableNameStructAdapter(AbsBaseActivity context, List<TableNameStruct> values) {
    super(context, R.layout.row_item_with_preference);
    this.clear();
    this.addAll(values);
  }

  @NonNull
  @Override
  public View getView(int position, android.view.View convertView,
      @NonNull android.view.ViewGroup parent) {
    if (convertView == null) {
      convertView = LayoutInflater.from(getContext())
          .inflate(R.layout.row_item_with_preference, parent, false);
    }
    final RelativeLayout view = (RelativeLayout) convertView;
    TextView textView = view.findViewById(R.id.row_item_text);

    TableNameStruct nameStruct = getItem(position);
    if (nameStruct == null) { // should never happen
      TextView failure = new TextView(parent.getContext());
      failure.setText(R.string.error);
      return failure;
    }

    textView.setText(nameStruct.getLocalizedDisplayName());
    ImageView imageView = view.findViewById(R.id.row_item_icon);
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
