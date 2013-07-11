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
   * the server and local versions. 
   * @author sudar.sam@gmail.com
   *
   */
  public static class ConflictColumn {
    int position;
    String localValue;
    String serverValue;
    
    public ConflictColumn(int position, String localValue, 
        String serverValue) {
      this.position = position;
      this.localValue = localValue;
      this.serverValue = serverValue;
    }
    
    public String getLocalValue() {
      return this.localValue;
    }
    
    public String getServerValue() {
      return this.serverValue;
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
  
  private void setResolution(int position, Resolution decision) {
    mResolutions.append(position, decision);
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
      ConflictColumn conflictColumn = this.mConflictColumns.get(position);
      LinearLayout view = (LinearLayout) convertView;
      if (view == null) {
        int layoutId; // the layout to use
        layoutId = org.opendatakit.tables.R.layout.list_item_conflict_row;
        view = (LinearLayout) mLayoutInflater.inflate(layoutId, parent, false);
      }
      // the text view displaying the local value
      TextView localTextView =
          (TextView) view.findViewById(R.id.list_item_local_text);
      localTextView.setText(conflictColumn.localValue);
      TextView serverTextView =
          (TextView) view.findViewById(R.id.list_item_server_text);
      serverTextView.setText(conflictColumn.serverValue);
      // The decision the user has made. May be null if it hasn't been set.
      Resolution userDecision = mResolutions.get(position);
      RadioButton localButton = 
          (RadioButton) view.findViewById(R.id.list_item_local_radio_button);
      RadioButton serverButton = 
          (RadioButton) view.findViewById(R.id.list_item_server_radio_button);
      if (userDecision != null) {
        if (userDecision == Resolution.LOCAL) {
          localButton.setChecked(true);
          serverButton.setChecked(false);
        } else {
          // they've decided on the server version of the row.
          localButton.setChecked(false);
          serverButton.setChecked(true);
        }
      } else {
        localButton.setChecked(false);
        serverButton.setChecked(false);
      }
      // Alright. Now we need to set the click listeners. It's going to be a 
      // little bit tricky. We want the list item as well as the radio buttons
      // to register as a choice, and to update the other radiobutton as 
      // appropriate. In order to do this, we're going to add the entire view
      // object, including itself, as the view's tag. That way we can get at 
      // them to update appropriately.
      LinearLayout localRow = (LinearLayout) 
          view.findViewById(R.id.list_item_conflict_resolution_local_row);
      LinearLayout serverRow = (LinearLayout)
          view.findViewById(R.id.list_item_conflict_resolution_server_row);
      localRow.setTag(view);
      serverRow.setTag(view);
      localButton.setTag(view);
      serverButton.setTag(view);
      // We also need to add the position to each of the views, so that when 
      // it's clicked we'll be able to figure out to which row it was
      // referring. We'll use the parent id for the key.
      localRow.setTag(R.id.list_view_conflict_row, position);
      serverRow.setTag(R.id.list_view_conflict_row, position);
      localButton.setTag(R.id.list_view_conflict_row, position);
      serverButton.setTag(R.id.list_view_conflict_row, position);
      localRow.setOnClickListener(new ResolutionOnClickListener());
      serverRow.setOnClickListener(new ResolutionOnClickListener());
      localButton.setOnClickListener(new ResolutionOnClickListener());
      serverButton.setOnClickListener(new ResolutionOnClickListener());
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
  
  /**
   * The class that handles registering a user's choice and updating the view
   * appropriately. The view that adds this as a click listener must have 
   * included the whole parent viewgroup as its tag.
   * @author sudar.sam@gmail.com
   *
   */
  private class ResolutionOnClickListener implements View.OnClickListener {

    @Override
    public void onClick(View v) {
      // First get the parent view of the whole conflict row, via which we'll
      // be able to get at the appropriate radio buttons.
      View conflictRowView = (View) v.getTag();
      int position = (Integer) v.getTag(R.id.list_view_conflict_row);
      RadioButton localButton = (RadioButton) conflictRowView.findViewById(
          R.id.list_item_local_radio_button);
      RadioButton serverButton = (RadioButton) conflictRowView.findViewById(
          R.id.list_item_server_radio_button);
      // Now we need to figure out if this is a server or a local click, which
      // we'll know by which view the click came in on.
      int viewId = v.getId();
      if (viewId == R.id.list_item_conflict_resolution_local_row || 
          viewId == R.id.list_item_local_radio_button) {
        // Then we have clicked on a local row.
        localButton.setChecked(true);
        serverButton.setChecked(false);
        setResolution(position, Resolution.LOCAL);
      } else if (viewId == R.id.list_item_conflict_resolution_server_row || 
          viewId == R.id.list_item_server_radio_button) {
        // Then we've clicked on a server row.
        localButton.setChecked(false);
        serverButton.setChecked(true);
        setResolution(position, Resolution.SERVER);
      } else {
        Log.e(TAG, "[onClick] wasn't a recognized id, not saving choice!");
      }
    }
    
  }

}
