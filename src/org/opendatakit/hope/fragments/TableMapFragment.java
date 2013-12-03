package org.opendatakit.hope.fragments;

import java.util.ArrayList;

import org.opendatakit.hope.R;
import org.opendatakit.hope.fragments.TableMapInnerFragment.TableMapInnerFragmentListener;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A TableMapFragment displays map information about a specific table.
 *
 * @author Chris Gelon (cgelon)
 */
public class TableMapFragment extends Fragment implements ITableFragment,
    TableMapInnerFragmentListener {

  /** The key for the Key-Value Store Partition for the TableMapFragment. */
  public static final String KVS_PARTITION = "TableMapFragment";
  /** The key to grab which column is being used for latitude. */
  public static final String KEY_MAP_LAT_COL = "keyMapLatCol";
  /** The key to grab which column is being used for longitude. */
  public static final String KEY_MAP_LONG_COL = "keyMapLongCol";
  /** The key to grab which file is being used for the list view. */
  public static final String KEY_FILENAME = "keyFilename";
  /** The key for the type of color rule to use on the map. */
  public static final String KEY_COLOR_RULE_TYPE = "keyColorRuleType";
  /**
   * The key for, if the color rule is based off of a column, the column to use.
   */
  public static final String KEY_COLOR_RULE_COLUMN = "keyColorRuleColumn";

  /** The constant if we want no color rules. */
  public static final String COLOR_TYPE_NONE = "None";
  /** The constant if we want the color rules based off of the table. */
  public static final String COLOR_TYPE_TABLE = "Table Color Rules";
  /** The constant if we want the color rules based off of the status column. */
  public static final String COLOR_TYPE_STATUS = "Status Column Color Rules";
  /** The constant if we want the color rules based off of a column. */
  public static final String COLOR_TYPE_COLUMN = "Selectable Column Color Rules";

  /** A tag to grab the list view fragment. */
  public static final String FRAGMENT_TAG_LIST = "fragmentTagList";
  /** A tag to grab the map view fragment. */
  public static final String FRAGMENT_TAG_MAP = "fragmentTagMap";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Only add the fragments if we haven't already initialized the state
    // before.
    if (savedInstanceState == null) {
      // Create the map fragment.
      TableMapInnerFragment map = new TableMapInnerFragment();

      // Create the list fragment.
      TableMapListFragment list = new TableMapListFragment();

      // Add both the list and the map at the same time.
      getChildFragmentManager().beginTransaction().add(R.id.list, list, FRAGMENT_TAG_LIST)
          .add(R.id.map, map, FRAGMENT_TAG_MAP).commit();
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
      return inflater.inflate(R.layout.map_fragment_horizontal, container, false);
    } else {
      return inflater.inflate(R.layout.map_fragment, container, false);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    getMap().listener = this;
  }

  @Override
  public void onHideList() {
    getList().setVisibility(View.GONE);
  }

  @Override
  public void onSetIndex(int i) {
    if (!isTabletDevice(getActivity())) {
      TableMapListFragment list = getList();
      if (list != null) {
        list.setIndex(i);
      }
    }
  }

  @Override
  public void onSetIndexes(ArrayList<Integer> indexes) {
    TableMapListFragment list = getList();
    if (list != null) {
      list.setIndexes(indexes);
    }
  }

  @Override
  public void init() {
    getMap().init();
  }

  @Override
  public void onSearch() {
    // TODO When searching, do something? Not really sure how the search would
    // work on the map yet...
  }

  /** The list view fragment. */
  private TableMapListFragment getList() {
    return (TableMapListFragment) getChildFragmentManager().findFragmentByTag(FRAGMENT_TAG_LIST);
  }

  /** The map view fragment. */
  private TableMapInnerFragment getMap() {
    return (TableMapInnerFragment) getChildFragmentManager().findFragmentByTag(FRAGMENT_TAG_MAP);
  }

  /**
   * Checks if the device is a tablet or a phone
   *
   * @param activityContext
   *          The Activity Context.
   * @return Returns true if the device is a Tablet
   */
  public static boolean isTabletDevice(Context activityContext) {
    // Verifies if the Generalized Size of the device is XLARGE to be
    // considered a Tablet
    boolean xlarge = ((activityContext.getResources().getConfiguration().screenLayout &
          Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE);

    // If XLarge, checks if the Generalized Density is at least MDPI
    // (160dpi)
    if (xlarge) {
      DisplayMetrics metrics = new DisplayMetrics();
      Activity activity = (Activity) activityContext;
      activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

      // MDPI=160, DEFAULT=160, DENSITY_HIGH=240, DENSITY_MEDIUM=160,
      // DENSITY_TV=213, DENSITY_XHIGH=320
      if (metrics.densityDpi == DisplayMetrics.DENSITY_DEFAULT
          || metrics.densityDpi == DisplayMetrics.DENSITY_HIGH
          || metrics.densityDpi == DisplayMetrics.DENSITY_MEDIUM
          || metrics.densityDpi == DisplayMetrics.DENSITY_TV
          || metrics.densityDpi == DisplayMetrics.DENSITY_XHIGH) {

        // Yes, this is a tablet!
        return true;
      }
    }

    // No, this is not a tablet!
    return false;
  }
}