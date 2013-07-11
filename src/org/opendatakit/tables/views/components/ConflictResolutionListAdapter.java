package org.opendatakit.tables.views.components;

import java.util.List;

import org.opendatakit.tables.R;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * A basic adapter for displaying things with sections. Based on the code from
 * the Google IO 2012 app:
 * 
 * https://code.google.com/p/iosched/
 * @author sudar.sam@gmail.com
 *
 */
public class ConflictResolutionListAdapter extends BaseAdapter {
  
  private static final String TAG = 
      ConflictResolutionListAdapter.class.getSimpleName();
  
  private static final int INVALID_POSITION = -1;
  
  private LayoutInflater mLayoutInflater;
  private SparseArray<Section> mSections = new SparseArray<Section>();
  private SparseArray<ConflictColumn> mConflictColumns = 
      new SparseArray<ConflictColumn>();
  private SparseArray<ConcordantColumn> mConcordantColumns = 
      new SparseArray<ConcordantColumn>();
  /** 
   * The decisions the user has made on the resolution of the conflict. Indexes
   * into the sparse array will be the {@link ConflictColumn#position} of the
   * local column, which MUST be one less than the server conflict's position.
   */
  private SparseArray<Resolution> mResolutions = 
      new SparseArray<Resolution>();
  
  /**
   * The choice made by the user.
   */
  private enum Resolution {
    LOCAL, SERVER;
  }
  
  /** 
   * This is the padding on the left side of the text view for those items in
   * the adapter that aren't in a section.
   */
  private int mLeftPaddingOnTopLevel = -1;
  
  public static class Section {
    int firstPosition;
    CharSequence title;
    
    public Section(int firstPosition, CharSequence title) {
      this.firstPosition = firstPosition;
      this.title = title;
    }
    
    public CharSequence getTitle() {
      return title;
    }
  }
  
  /**
   * Represents a column that is in conflict--i.e. the contents differ between
   * the server and local versions. In this case two ConflictColumns would 
   * exist--one for the server and one for the local versions.
   * @author sudar.sam@gmail.com
   *
   */
  public static class ConflictColumn {
    int position;
    String value;
    boolean isLocal;
    
    public ConflictColumn(int position, String value, boolean isLocal) {
      this.position = position;
      this.value = value;
      this.isLocal = isLocal;
    }
    
    public String getValue() {
      return this.value;
    }
  }
  
  /**
   * Represents a column that is not in conflict--i.e. one that has the same 
   * value locally and on the server.
   * @author sudar.sam@gmail.com
   *
   */
  public static class ConcordantColumn {
    int position;
    String value;
    
    public ConcordantColumn(int position, String value) {
      this.position = position;
      this.value = value;
    }
  }
  
  public ConflictResolutionListAdapter(Context context, List<Section> sections, 
      List<ConcordantColumn> concordantColumns,
      List<ConflictColumn> conflictColumns) {
    mLayoutInflater = (LayoutInflater) context.getSystemService(
      Context.LAYOUT_INFLATER_SERVICE);
    // First let's set the columns and sections.
    for (Section section : sections) {
      mSections.append(section.firstPosition, section);
    }
    for (ConcordantColumn cc : concordantColumns) {
      mConcordantColumns.append(cc.position, cc);
    }
    for (ConflictColumn cc : conflictColumns) {
      mConflictColumns.append(cc.position, cc);
    }
    RadioGroup rg = new RadioGroup(context);
    rg.findV
  }
  
  public boolean isSectionHeaderPosition(int position) {
    return mSections.get(position) != null;
  }
  
  public boolean isConflictColumnPosition(int position) {
    return mConflictColumns.get(position) != null;
  }
  
  public boolean isConcordantColumnPosition(int position) {
    return mConcordantColumns.get(position) != null;
  }

  @Override
  public int getCount() {
    return (mSections.size() + mConflictColumns.size() 
        + mConcordantColumns.size());
  }
  
  @Override
  public Object getItem(int position) {
    // This position can be one of three types: a section, a concordant, or a
    // conflict column.
    if (isSectionHeaderPosition(position)) {
      return mSections.get(position);
    } else if (isConflictColumnPosition(position)) {
      return mConflictColumns.get(position);
    } else if (isConcordantColumnPosition(position)) {
      return mConcordantColumns.get(position);
    } else {
      Log.e(TAG, "[getItem] position " + position + " didn't match any of " +
      		"the types!");
      return null;
    }
  }

  @Override
  public long getItemId(int position) {
    return Integer.MAX_VALUE - position;
  }
  
  @Override
  public int getItemViewType(int position) {
    if (isSectionHeaderPosition(position)) {
      return 0;
    } else if (isConflictColumnPosition(position)) {
      return 1;
    } else if (isConcordantColumnPosition(position)) {
      return 2;
    } else {
      Log.e(TAG, "[getItem] position " + position + " didn't match any of " +
            "the types!");
      return -1;
    }
  }
  
  @Override
  public boolean isEnabled(int position) {
    if (isSectionHeaderPosition(position)) {
      return false;
    } else if (isConflictColumnPosition(position)) {
      return true;
    } else if (isConcordantColumnPosition(position)) {
      return false;
    } else {
      Log.e(TAG, "[getItem] position " + position + " didn't match any of " +
            "the types!");
      return false;
    }
  }
  
  @Override
  public int getViewTypeCount() {
    return 3; // heading, conflict, concordant.
  }
  
  @Override
  public boolean areAllItemsEnabled() {
    return false;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    if (isSectionHeaderPosition(position)) {
      TextView view = (TextView) convertView;
      if (view == null) {
        view = (TextView) mLayoutInflater.inflate(
            org.opendatakit.tables.R.layout.list_item_section_heading, parent,
          false);
      }
      view.setText(mSections.get(position).title);
      return view;
    } else if (isConflictColumnPosition(position)) {
      LinearLayout view = (LinearLayout) convertView;
      if (view == null) {
        int layoutId; // the layout to use
        layoutId = org.opendatakit.tables.R.layout
            .list_item_conflict_resolution_local_row;
        view = (LinearLayout) mLayoutInflater.inflate(layoutId, parent, false);
      }
      TextView textView =
          (TextView) view.findViewById(R.id.list_item_local_text);
      textView.setText(mConflictColumns.get(position).value);
      // Here we get the position in the mResolutions array. Since we're 
      // indexing into that array at the location of the local index, if it's
      // a server column we're going to go to 1 less than that index. Note that
      // we're assuming the conflict and concordant rows were passed in 
      // appropriately.
      int positionInMResolutions = getPositionInMResolutions(position);
      // The decision the user has made. May be null if it hasn't been set.
      Resolution userDecision = mResolutions.get(positionInMResolutions);
      RadioButton rb = (RadioButton) view.findViewById(
          R.id.list_item_local_radio_button);
      if (mConflictColumns.get(position).isLocal) {
        if (userDecision == Resolution.LOCAL) {
          rb.setChecked(true);
        } else {
          // We'll set it false if it's null (not yet set) OR if it's server.
          rb.setChecked(false);
        }
      } else {
        // It's a server row.
        if (userDecision == Resolution.SERVER) {
          rb.setChecked(true);
        } else {
          // we again set to false if it's null (no decision) or if it's local.
          rb.setChecked(false);
        }
      }
      // So we can know the position and thus the resolution in the on click
      // listener.
      view.setTag(R.id.list_item_local_radio_button, position);
      view.setOnClickListener(new ResolutionRadioButtonOnClickListener());
      return view;

    } else if (isConcordantColumnPosition(position)) {
      TextView view = (TextView) convertView;
      if (view == null) {
        view = (TextView) mLayoutInflater.inflate(
            android.R.layout.simple_list_item_1, parent, false);
      }
      view.setText(mConcordantColumns.get(position).value);
      return view;
    } else {
      Log.e(TAG, "[getView] ran into trouble, position didn't match any of " +
      		"the types!");
      return null;
    }
  }
  
  private int getPositionInMResolutions(int positionInAdapter) {
    if (!isConflictColumnPosition(positionInAdapter)) {
      return INVALID_POSITION;
    }
    return mConflictColumns.get(positionInAdapter).isLocal
        ? positionInAdapter 
        : positionInAdapter - 1;
  }
  
  private class ResolutionRadioButtonOnClickListener 
      implements View.OnClickListener {

    @Override
    public void onClick(View v) {
      // get the position.
      int position = (Integer) v.getTag(R.id.list_item_local_radio_button);
      RadioButton rb = 
          (RadioButton) v.findViewById(R.id.list_item_local_radio_button);
      Log.e(TAG, "position : " + position);
      int positionInMResolutions = getPositionInMResolutions(position);
      if (mConflictColumns.get(position).isLocal) {
        Log.e(TAG, "setting local");
        mResolutions.append(positionInMResolutions, Resolution.LOCAL);
        rb.setChecked(true);
      } else {
        Log.e(TAG, "setting server");
        // it's a server row, b/c we would have thrown an error in 
        //getPositionInMResolutions
        mResolutions.append(positionInMResolutions, Resolution.SERVER);
        rb.setChecked(true);
      }
    }
    
  }

}
