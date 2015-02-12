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
import org.opendatakit.tables.utils.GraphViewStruct;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the data structure that actually bears the list to be displayed to
 * the user, and handles the View creation of each element in the normal
 * Android Adapter way.
 * <p>
 * The general idea is that this class gives the icon necessary for viewing
 * the adapter and adding settings.
 *
 * @author sudar.sam@gmail.com
 *
 */
public class GraphViewAdapter extends BaseAdapter implements ListAdapter {
  
  private static final String TAG = GraphViewAdapter.class.getSimpleName();
  
  private List<GraphViewStruct> mGraphViews;
  private Context mContext;
  private final String mAppName;
  
  public GraphViewAdapter(Context context, String appName, List<GraphViewStruct> graphViews) {
    this.mGraphViews = graphViews;
    this.mAppName = appName;
  }
  
  List<GraphViewStruct> getGraphViews() {
    return this.mGraphViews;
  }

  /**
   * The method responsible for getting the view that will represent an
   * element in the list. Since we're just displaying Strings, we must make
   * sure that the settings button generates the correct options for selecting
   * a new list view.
   */
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    // Per the Android programming contract, it's possible that we've
    // been handed an old view we need to convert to a new one. (This
    // is what the convertView param is.) So, try that.
    View row = convertView;
    // It's possible that we weren't handed one and have to construct
    // it up from scratch.
    if (row == null) {
      row = this.createView(parent);
    }
    final int currentPosition = position;
    final GraphViewStruct graphView =
        this.getGraphViews().get(currentPosition);
    String graphViewName = graphView.graphName;
    // Set the label of this row.
    TextView label = (TextView) row.findViewById(R.id.row_label);
    label.setText(graphViewName);
    TextView extraString = (TextView) row.findViewById(R.id.row_ext);
//    AspectHelper aspectHelper = kvsh.getAspectHelper(listViewName);
//    String filename = aspectHelper.getString(GraphDisplayActivity.GRAPH_TYPE);
    extraString.setText(graphView.graphType);
    // The radio button showing whether or not this is the default list view.
    final RadioButton radioButton = (RadioButton)
        row.findViewById(R.id.radio_button);
    if (graphView.isDefault) {
      radioButton.setChecked(true);
    } else {
      radioButton.setChecked(false);
    }
    radioButton.setVisibility(View.VISIBLE);
    // Set the click listener to set as default.
    radioButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        if (graphView.isDefault) {
          // Already set to default, do nothing.
        } else {
          setToDefault(graphView);
          radioButton.setChecked(true);
          Toast.makeText(
             mContext,
             mContext.getString(
                 R.string.set_as_default_graph_view,
                 graphView.graphName),
             Toast.LENGTH_SHORT).show();
        }
      }

    });

    // And now prepare the listener for the settings icon.
    final ImageView editView = (ImageView) row
        .findViewById(R.id.row_options);
    final View holderView = row;
    editView.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        // Open the context menu of the view, because that's where we're
        // doing the logistics.
        holderView.showContextMenu();
      }
    });
    // And now we're set, so just kick it on back.
    return row;
  }
  
  private View createView(ViewGroup parent){
    LayoutInflater layoutInflater = (LayoutInflater)
        parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    return layoutInflater.inflate(
        R.layout.row_for_edit_view_entry,
        parent,
        false);
  }
  
  /**
   * Set the graph view to be the default graph view. UNIMPLEMENTED.
   * @param graphView
   */
  private void setToDefault(GraphViewStruct graphView) {
    // TODO:
    WebLogger.getLogger(mAppName).e(TAG, "[setToDefault] unimplemented!");
  }

  @Override
  public int getCount() {
    return this.getGraphViews().size();
  }

  @Override
  public Object getItem(int position) {
    return this.getGraphViews().get(position);
  }

  @Override
  public long getItemId(int position) {
    return 0;
  }
  
}

