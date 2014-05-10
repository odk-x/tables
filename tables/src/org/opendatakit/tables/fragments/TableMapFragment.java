package org.opendatakit.tables.fragments;

import java.util.ArrayList;

import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.fragments.TableMapInnerFragment.TableMapInnerFragmentListener;
import org.opendatakit.tables.utils.ActivityUtil;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;

import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
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
  
  private static final String TAG = TableMapFragment.class.getSimpleName();

  /** The key for the Key-Value Store Partition for the TableMapFragment. */
//  public static final String KVS_PARTITION = "TableMapFragment";
//  /** The key to grab which column is being used for latitude. */
//  public static final String KEY_MAP_LAT_COL = "keyMapLatCol";
//  /** The key to grab which column is being used for longitude. */
//  public static final String KEY_MAP_LONG_COL = "keyMapLongCol";
//  /** The key to grab which file is being used for the list view. */
//  public static final String KEY_FILENAME = "keyFilename";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "[onCreate]");
  }
  
  /**
   * Create the inner map fragment that will be displayed.
   * @return
   */
  TableMapInnerFragment createInnerMapFragment() {
    TableMapInnerFragment result = new TableMapInnerFragment();
    return result;
  }
  
  /**
   * Create the list fragment that will be displayed.
   * @param listViewFileName the file name of the list view that will be
   * displayed
   * @return
   */
  MapListViewFragment createMapListViewFragment(String listViewFileName) {
    MapListViewFragment result = new MapListViewFragment();
    Bundle listArguments = new Bundle();
    IntentUtil.addFileNameToBundle(listArguments, listViewFileName);
    result.setArguments(listArguments);
    return result;
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
  public void onDestroyView() {
    FragmentManager fragmentManager = this.getFragmentManager();
    MapListViewFragment mapListViewFragment = getList();
    TableMapInnerFragment mapInnerFragment = getMap();
    FragmentTransaction transaction = fragmentManager.beginTransaction();
    if (mapListViewFragment != null) {
      transaction.remove(mapListViewFragment);
    }
    if (mapInnerFragment != null) {
      transaction.remove(mapListViewFragment);
    }
    transaction.commitAllowingStateLoss();
    super.onDestroyView();
  }

  @Override
  public void onResume() {
    super.onResume();
    FragmentManager fragmentManager = this.getFragmentManager();
    MapListViewFragment mapListViewFragment = getList();
    TableMapInnerFragment mapInnerFragment = getMap();
    // Attach the fragments if we need them.
    if (mapInnerFragment == null) {
      Log.d(
          TAG,
          "[onResume] existing inner map fragment not found, creating new");
      mapInnerFragment = this.createInnerMapFragment();
    } else {
      Log.d(
          TAG,
          "[onResume] existing inner map found");
    }
    mapInnerFragment.listener = this;
    if (mapListViewFragment == null) {
      Log.d(
          TAG,
          "[onResume] existing map list fragment not found, creating new");
      String fileName = IntentUtil.retrieveFileNameFromBundle(getArguments());
      mapListViewFragment = this.createMapListViewFragment(fileName);
    } else {
      Log.d(TAG, "[onResume] existing map list fragment found");
    }
    fragmentManager.beginTransaction()
      .replace(
          R.id.map_view_list,
          mapListViewFragment,
          Constants.FragmentTags.MAP_LIST)
      .replace(
          R.id.map_view_inner_map,
          mapInnerFragment,
          Constants.FragmentTags.MAP_INNER_MAP)
      .commit();
  }

  @Override
  public void onHideList() {
    Log.d(TAG, "[onHideList]");
    MapListViewFragment mapListViewFragment = this.getList();
    if (mapListViewFragment == null) {
      Log.e(TAG, "[onHideList] mapListViewFragment is null. Returning.");
      return;
    }
    FragmentManager fragmentManager = this.getFragmentManager();
    fragmentManager.beginTransaction().hide(mapListViewFragment).commit();
  }

  @Override
  public void onSetIndex(int i) {
    Log.d(TAG, "[onSetIndex]");
    if (!ActivityUtil.isTabletDevice(getActivity())) {
      MapListViewFragment list = getList();
      if (list != null) {
        list.setMapListIndex(i);
      }
    }
  }

  @Override
  public void onSetInnerIndexes(ArrayList<Integer> indexes) {
    Log.d(TAG, "[onSetInnerIndexes]");
    MapListViewFragment list = getList();
    if (list != null) {
      list.setMapListIndices(indexes);
    }
  }

  public void init() {
    getMap().clearAndInitializeMap();
  }

  /** The list view fragment. */
  MapListViewFragment getList() {
    return (MapListViewFragment) getFragmentManager().findFragmentByTag(
        Constants.FragmentTags.MAP_LIST);
  }

  /** The map view fragment. */
  TableMapInnerFragment getMap() {
    return (TableMapInnerFragment) getFragmentManager().findFragmentByTag(
        Constants.FragmentTags.MAP_INNER_MAP);
  }

  @Override
  public ViewFragmentType getFragmentType() {
    return ViewFragmentType.MAP;
  }
}