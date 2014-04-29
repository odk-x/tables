package org.opendatakit.tables.views.components;

import java.util.List;

import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.R;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * An adapter for displaying TableProperties.
 * @author sudar.sam@gmail.com
 *
 */
public class TablePropertiesAdapter extends BaseAdapter 
    implements ListAdapter {
  
  private static final String TAG = 
      TablePropertiesAdapter.class.getSimpleName();
  
  private List<TableProperties> mTableList;
  
  public TablePropertiesAdapter(List<TableProperties> list) {
    this.mTableList = list;
  }

  @Override
  public View getView(
      int position,
      android.view.View convertView,
      android.view.ViewGroup parent) {
    Log.e(TAG, "getView called");
    RelativeLayout view = convertView == null ? 
        createView(parent) : 
        (RelativeLayout) convertView;
    TextView textView = (TextView) view.findViewById(R.id.row_item_text);
    textView.setText(this.getList().get(position).getDisplayName());
    return view;
  }
  
  List<TableProperties> getList() {
    return this.mTableList;
  }
  
  private RelativeLayout createView(ViewGroup parent) {
    LayoutInflater layoutInflater = 
        (LayoutInflater) parent
            .getContext()
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    return (RelativeLayout) layoutInflater.inflate(
        R.layout.row_item_with_preference,
        parent,
        false);
  }

  @Override
  public int getCount() {
    Log.e(TAG, "getCount returns: " + this.getList().size());
    return this.getList().size();
  }

  @Override
  public Object getItem(int position) {
    return this.getList().get(position);
  }

  @Override
  public long getItemId(int position) {
    return 0;
  }

}
