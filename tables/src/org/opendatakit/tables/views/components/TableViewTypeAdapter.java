package org.opendatakit.tables.views.components;

import org.opendatakit.common.android.data.PossibleTableViewTypes;
import org.opendatakit.common.android.data.TableViewType;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;

/**
 * Adapter that displays {@link TableViewType} options.
 * @author sudar.sam@gmail.com
 *
 */
public class TableViewTypeAdapter extends ArrayAdapter<CharSequence> {
  
  private static final String TAG = TableViewTypeAdapter.class.getSimpleName();
  
  private PossibleTableViewTypes mPossibleViewTypes;
  private CharSequence[] mViewTypeValues;
  private Context mContext;

  public TableViewTypeAdapter(
      Context context,
      int resource,
      CharSequence[] entries,
      CharSequence[] entryValues,
      PossibleTableViewTypes viewTypes) {
    super(context, resource, entries);
    this.mContext = context;
    this.mViewTypeValues = entryValues;
    this.mPossibleViewTypes = viewTypes;
  }
  
  @Override
  public boolean areAllItemsEnabled() {
    // so we get asked about individual availability.
    return false;
  }
  
  
  
  
  
  @Override
  public boolean isEnabled(int position) {
    String currentItem = this.mViewTypeValues[position].toString();
    if (currentItem.equals(TableViewType.SPREADSHEET.name())) {
      if (this.mPossibleViewTypes.spreadsheetViewIsPossible()) {
        return true;
      } else {
        return false;
      }
    } else if (currentItem.equals(TableViewType.LIST.name())) {
      if (this.mPossibleViewTypes.listViewIsPossible()) {
        return true;
      } else {
        return false;
      }
    } else if (currentItem.equals(TableViewType.MAP.name())) {
      if (this.mPossibleViewTypes.mapViewIsPossible()) {
        return true;
      } else {
        return false;
      }
    } else if (currentItem.equals(TableViewType.GRAPH.name())) {
      if (this.mPossibleViewTypes.graphViewIsPossible()) {
        return true;
      } else {
        return false;
      }
    } else {
      // Enable it.
      Log.e(TAG, "unrecognized entryValue: " + currentItem);
      return true;
    }
  }

}
