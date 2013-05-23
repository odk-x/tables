package org.opendatakit.tables.fragments;

import org.opendatakit.tables.R;
import org.opendatakit.tables.fragments.TableMapInnerFragment.TableMapInnerFragmentListener;

import android.annotation.SuppressLint;
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

  public static final String KVS_PARTITION = "TableMapFragment";
  public static final String KEY_MAP_LAT_COL = "keyMapLatCol";
  public static final String KEY_MAP_LONG_COL = "keyMapLongCol";
  public static final String KEY_FILENAME = "keyFilename";
  public static final String KEY_COLOR_RULE_TYPE = "keyColorRuleType";
  public static final String KEY_COLOR_RULE_COLUMN = "keyColorRuleColumn";

  public static final String COLOR_TYPE_NONE = "None";
  public static final String COLOR_TYPE_TABLE = "Table Color Rules";
  public static final String COLOR_TYPE_STATUS = "Status Column Color Rules";
  public static final String COLOR_TYPE_COLUMN = "Selectable Column Color Rules";
  
  private static final String FRAGMENT_TAG_LIST = "fragmentTagList";
  private static final String FRAGMENT_TAG_MAP = "fragmntTagMap";

  private TableMapListFragment mList;
  private TableMapInnerFragment mMap;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Only add the fragments if we haven't already initialized the state before.
    if (savedInstanceState == null) {
      // Create the map fragment.
      TableMapInnerFragment map = new TableMapInnerFragment();

      // Create the list fragment.
      TableMapListFragment list = new TableMapListFragment();

      // Add both the list and the map at the same time.
      getChildFragmentManager().beginTransaction().add(R.id.list, list, FRAGMENT_TAG_LIST).add(R.id.map, map, FRAGMENT_TAG_MAP)
          .commit();
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.map_fragment, container, false);
  }

  @Override
  public void onResume() {
    super.onResume();
    // Find the map and list fragments, and then set the map listener.
    mMap = (TableMapInnerFragment) getChildFragmentManager().findFragmentByTag(FRAGMENT_TAG_MAP);
    mList = (TableMapListFragment) getChildFragmentManager().findFragmentByTag(FRAGMENT_TAG_LIST);
    mMap.listener = this;
  }

  @Override
  public void onHideList() {
    mList.setVisibility(View.GONE);
  }

  @Override
  public void onSetIndex(int i) {
    mList.setIndex(i);
  }
  
  @Override
  public void init() {
    mMap.init();
  }

  @Override
  public void onSearch() {
    // TODO When searching, do something? Not really sure how the search would
    // work on the map yet...
  }

  /**
   * Checks if the device is a tablet or a phone
   * 
   * @param activityContext
   *          The Activity Context.
   * @return Returns true if the device is a Tablet
   */
  @SuppressLint("InlinedApi")
  public static boolean isTabletDevice(Context activityContext) {
    // Verifies if the Generalized Size of the device is XLARGE to be
    // considered a Tablet
    boolean xlarge = ((activityContext.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE);

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