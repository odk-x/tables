package org.opendatakit.tables.views.components;

import java.util.List;

import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.tables.R;
import org.opendatakit.tables.utils.TableUtil;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
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

  private Context mContext;
  private String mAppName;
  private List<String> mTableIdList;

  public TablePropertiesAdapter(Context context, String appName, List<String> list) {
    this.mContext = context;
    this.mAppName = appName;
    this.mTableIdList = list;
  }

  @Override
  public View getView(
      int position,
      android.view.View convertView,
      android.view.ViewGroup parent) {
    Log.e(TAG, "getView called");
    final RelativeLayout view = convertView == null ?
        createView(parent) :
        (RelativeLayout) convertView;
    TextView textView = (TextView) view.findViewById(R.id.row_item_text);
    String tableId = this.getList().get(position);

    String localizedDisplayName;
    SQLiteDatabase db = null;
    try {
      db = DatabaseFactory.get().getDatabase(mContext, mAppName);
      localizedDisplayName = TableUtil.get().getLocalizedDisplayName(db, tableId);
    } finally {
      if ( db != null ) {
        db.close();
      }
    }
 
    textView.setText(localizedDisplayName);
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

  List<String> getList() {
    return this.mTableIdList;
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
