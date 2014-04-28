package org.opendatakit.tables.views.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  private UICallbacks mCallbacks;
  private LayoutInflater mLayoutInflater;
  private SparseArray<Section> mSections = new SparseArray<Section>();
  private SparseArray<ConflictColumn> mConflictColumns =
      new SparseArray<ConflictColumn>();
  private SparseArray<ConcordantColumn> mConcordantColumns =
      new SparseArray<ConcordantColumn>();
  /** Whether or not conflictColumns are enabled should be disabled. */
  private boolean mConflictColumnsAreEnabled;
  /**
   * The decisions the user has made on the resolution of the conflict. Maps
   * element key to resolution.
   */
  private Map<String, Resolution> mResolutions =
      new HashMap<String, Resolution>();
  /**
   * A map of element key to the user's chosen value for the column.
   */
  private Map<String, String> mResolvedValues;

  /**
   * The choice made by the user.
   */
  public enum Resolution {
    LOCAL, SERVER;
  }

  public interface UICallbacks {
    /**
     * Called when the user has made a decision about which row to use.
     */
    public void onDecisionMade();
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
    final int position;
    final String localValue;
    final String serverValue;
    final String elementKey;

    public ConflictColumn(int position, String elementKey, String localValue,
        String serverValue) {
      this.position = position;
      this.elementKey = elementKey;
      this.localValue = localValue;
      this.serverValue = serverValue;
    }

    public String getLocalValue() {
      return this.localValue;
    }

    public String getServerValue() {
      return this.serverValue;
    }

    public String getElementKey() {
      return this.elementKey;
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

  public ConflictResolutionListAdapter(Context context, UICallbacks callbacks,
      List<Section> sections, List<ConcordantColumn> concordantColumns,
      List<ConflictColumn> conflictColumns) {
    this.mLayoutInflater = (LayoutInflater) context.getSystemService(
      Context.LAYOUT_INFLATER_SERVICE);
    this.mResolvedValues = new HashMap<String, String>();
    this.mCallbacks = callbacks;
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
    this.mConflictColumnsAreEnabled = true;
  }

  public boolean isSectionHeaderPosition(int position) {
    return mSections.get(position) != null;
  }

  /**
   * Set the user choices backing this object. Intended to allow restoring of
   * state on things like screen rotation. Cannot rely on the framework to do
   * this for us because are programmatically creating views, and thus reusing
   * ids.
   * @param chosenValues
   * @param chosenResolutions
   */
  public void setRestoredState(Map<String, String> chosenValues,
      Map<String, Resolution> chosenResolutions) {
    this.mResolvedValues = chosenValues;
    this.mResolutions = chosenResolutions;
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
      if (mConflictColumnsAreEnabled) {
        return true;
      } else {
        return false;
      }
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
    // this might be false because this says in the spec something about
    // dividers returning true or false? Kind of a strange thing, but if
    // you're wondering why, consider looking at that.
    return false;
  }

  /**
   * Return a map of element key to the values chosen by the user. If a value
   * isn't present, it hasn't been selected by the user. If it the column
   * wasn't in conflict, it cannot be selected by the user. When a decision
   * has been made for every conflict column, the row is resolvable.
   * @return
   */
  public Map<String, String> getResolvedValues() {
    return this.mResolvedValues;
  }

  /**
   * Sets whether or not the views returned by this adapter are clickable.
   * @param enabled
   */
  public void setConflictColumnsEnabled(boolean enabled) {
    this.mConflictColumnsAreEnabled = enabled;
  }

  /**
   * Return a map of element key to the {@link Resolution} indicating whether
   * or not a user has made a decision.
   * @return
   */
  public Map<String, Resolution> getResolutions() {
    return this.mResolutions;
  }

  /**
   * Update the adapter's internal data structures to reflect the user's
   * choices.
   * @param position
   * @param decision
   */
  private void setResolution(ConflictColumn conflictColumn,
      Resolution decision) {
    this.mResolutions.put(conflictColumn.elementKey, decision);
    String chosenValue;
    // we didn't return, so we know it's safe.
    if (decision == Resolution.LOCAL) {
      chosenValue = conflictColumn.localValue;
    } else if (decision == Resolution.SERVER) {
      chosenValue = conflictColumn.serverValue;
    } else {
      Log.e(TAG, "[setResolution] decision didn't match a known resolution" +
      		" type: " + decision.name() + "! not setting anything");
      return;
    }
    this.mResolvedValues.put(conflictColumn.elementKey, chosenValue);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    if (isSectionHeaderPosition(position)) {
      TextView view = (TextView) convertView;
      if (view == null) {
        view = (TextView) mLayoutInflater.inflate(R.layout.list_item_section_heading, parent,
          false);
      }
      view.setText(mSections.get(position).title);
      return view;
    } else if (isConflictColumnPosition(position)) {
      ConflictColumn conflictColumn = this.mConflictColumns.get(position);
      LinearLayout view = (LinearLayout) convertView;
      if (view == null) {
        int layoutId; // the layout to use
        layoutId = R.layout.list_item_conflict_row;
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
      Resolution userDecision = mResolutions.get(conflictColumn.elementKey);
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
      // little bit tricky. We want the list item as well to update the other
      // radiobutton as
      // appropriate. In order to do this, we're going to add the entire view
      // object, including itself, as the view's tag. That way we can get at
      // them to update appropriately.
      LinearLayout localRow = (LinearLayout)
          view.findViewById(R.id.list_item_conflict_resolution_local_row);
      LinearLayout serverRow = (LinearLayout)
          view.findViewById(R.id.list_item_conflict_resolution_server_row);
      localRow.setTag(view);
      serverRow.setTag(view);
      // We also need to add the position to each of the views, so that when
      // it's clicked we'll be able to figure out to which row it was
      // referring. We'll use the parent id for the key.
      localRow.setTag(R.id.list_view_conflict_row, position);
      serverRow.setTag(R.id.list_view_conflict_row, position);
      localRow.setOnClickListener(new ResolutionOnClickListener());
      serverRow.setOnClickListener(new ResolutionOnClickListener());
      localRow.setEnabled(mConflictColumnsAreEnabled);
      serverRow.setEnabled(mConflictColumnsAreEnabled);
      // We'll want the radio buttons to trigger their whole associated list
      // item to keep the UI the same. Otherwise you could press the radio
      // button and NOT have the whole row highlighted, which I find annoying.
      serverButton.setClickable(false);
      localButton.setClickable(false);
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
      ConflictColumn conflictColumn = mConflictColumns.get(position);
      RadioButton localButton = (RadioButton) conflictRowView.findViewById(
          R.id.list_item_local_radio_button);
      RadioButton serverButton = (RadioButton) conflictRowView.findViewById(
          R.id.list_item_server_radio_button);
      // Now we need to figure out if this is a server or a local click, which
      // we'll know by which view the click came in on.
      int viewId = v.getId();
      if (viewId == R.id.list_item_conflict_resolution_local_row) {
        // Then we have clicked on a local row.
        localButton.setChecked(true);
        serverButton.setChecked(false);
        setResolution(conflictColumn, Resolution.LOCAL);
        mCallbacks.onDecisionMade();
      } else if (viewId == R.id.list_item_conflict_resolution_server_row) {
        // Then we've clicked on a server row.
        localButton.setChecked(false);
        serverButton.setChecked(true);
        setResolution(conflictColumn, Resolution.SERVER);
        mCallbacks.onDecisionMade();
      } else {
        Log.e(TAG, "[onClick] wasn't a recognized id, not saving choice!");
      }
    }

  }

}
