package org.opendatakit.tables.views.components;

import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * An adapter for displaying TableProperties.
 * @author sudar.sam@gmail.com
 *
 */
public class TablePropertiesAdapter extends ArrayAdapter<TableProperties> {
  
  private int mLayoutId;
  private TableProperties[] mArray;
  
  public TablePropertiesAdapter(
      Context context,
      int resource,
      TableProperties[] objects) {
    super(context, resource, objects);
    this.mLayoutId = resource;
    this.mArray = objects;
  }

  public android.view.View getView(
      int position,
      android.view.View convertView,
      android.view.ViewGroup parent) {
    RelativeLayout view = convertView == null ? 
        createView(parent) : 
        (RelativeLayout) convertView;
    TextView textView = (TextView) view.findViewById(R.id.row_item_text);
    textView.setText(this.mArray[position].getDisplayName());
    return view;
  }
  
  private RelativeLayout createView(ViewGroup parent) {
    LayoutInflater layoutInflater = 
        (LayoutInflater) parent
            .getContext()
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    return (RelativeLayout) layoutInflater.inflate(mLayoutId, parent, false);
  }

}
