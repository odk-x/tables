package org.opendatakit.tables.fragments;

import java.util.ArrayList;

import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.fragments.TableMapInnerFragment.TableMapInnerFragmentListener;
import org.opendatakit.tables.utils.Constants;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A TableMapFragment displays map information about a specific table.
 *
 * @author Chris Gelon (cgelon)
 * @author sudar.sam@gmail.com
 */
public class TableMapFragment extends AbsTableDisplayFragment implements
    TableMapInnerFragmentListener {

  /** The key for the Key-Value Store Partition for the TableMapFragment. */
  public static final String KVS_PARTITION = "TableMapFragment";
  /** The key to grab which column is being used for latitude. */
  public static final String KEY_MAP_LAT_COL = "keyMapLatCol";
  /** The key to grab which column is being used for longitude. */
  public static final String KEY_MAP_LONG_COL = "keyMapLongCol";
  /** The key to grab which file is being used for the list view. */
  public static final String KEY_FILENAME = "keyFilename";

  /** A tag to grab the list view fragment. */
  public static final String FRAGMENT_TAG_LIST = "fragmentTagList";
  /** A tag to grab the map view fragment. */
  public static final String FRAGMENT_TAG_MAP = "fragmentTagMap";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Only add the fragments if we haven't already initialized the state
    // before.
//    if (savedInstanceState == null) {
//      // Create the map fragment.
//      TableMapInnerFragment map = new TableMapInnerFragment();
//
//      // Create the list fragment.
//      TableMapListFragment list = new TableMapListFragment();
//
//      // Add both the list and the map at the same time.
//      getFragmentManager().beginTransaction()
//          .add(R.id.list, list, FRAGMENT_TAG_LIST)
//          .add(R.id.map, map, FRAGMENT_TAG_MAP)
//          .commit();
//    }
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    if (getActivity()
        .getResources()
        .getConfiguration()
        .orientation == Configuration.ORIENTATION_LANDSCAPE) {
      return inflater.inflate(
          R.layout.map_fragment_horizontal,
          container,
          false);
    } else {
      return inflater.inflate(R.layout.map_fragment, container, false);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    FragmentManager fragmentManager = this.getFragmentManager();
    // Attach the fragments if we need them.
    if (getMap() == null) {
      TableMapInnerFragment mapFragment = new TableMapInnerFragment();
      mapFragment.listener = this;
      fragmentManager.beginTransaction().add(
          R.id.list,
          mapFragment,
          Constants.FragmentTags.MAP_INNER_MAP).commit();
    } else {
      getMap().listener = this;
    }
    if (getList() == null) {
      TableMapListFragment listFragment = new TableMapListFragment();
      fragmentManager.beginTransaction().add(
          R.id.map,
          listFragment,
          Constants.FragmentTags.MAP_LIST).commit();
    }
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
        list.setMapListIndex(i);
      }
    }
  }

  @Override
  public void onSetInnerIndexes(ArrayList<Integer> indexes) {
    TableMapListFragment list = getList();
    if (list != null) {
      list.setMapListIndexes(indexes);
    }
  }

  public void init() {
    getMap().init();
  }

  /** The list view fragment. */
  private TableMapListFragment getList() {
    return (TableMapListFragment)
        getFragmentManager().findFragmentByTag(FRAGMENT_TAG_LIST);
  }

  /** The map view fragment. */
  private TableMapInnerFragment getMap() {
    return (TableMapInnerFragment)
        getFragmentManager().findFragmentByTag(FRAGMENT_TAG_MAP);
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

  @Override
  public ViewFragmentType getFragmentType() {
    return ViewFragmentType.MAP;
  }
}