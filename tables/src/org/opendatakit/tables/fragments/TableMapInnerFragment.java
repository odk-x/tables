/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendatakit.common.android.data.ColorGuide;
import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.OrderedColumns;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.data.UserTable.Row;
import org.opendatakit.common.android.utilities.GeoColumnUtil;
import org.opendatakit.common.android.utilities.KeyValueStoreHelper;
import org.opendatakit.common.android.utilities.LocalKeyValueStoreConstants;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TablePropertiesManager;
import org.opendatakit.tables.application.Tables;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * The InnerMapFragment has the capability of showing a map. It displays markers
 * for records in a database.
 *
 * @author Chris Gelon (cgelon)
 * @author sudar.sam@gmail.com
 */
public class TableMapInnerFragment extends MapFragment {

  private static final String TAG = TableMapInnerFragment.class.getSimpleName();

  private static final int INVALID_INDEX = -1;

  /** The default hue for markers if no color rules are applied. */
  private static final float DEFAULT_MARKER_HUE = BitmapDescriptorFactory.HUE_AZURE;
  /** The default hue for markers if no color rules are applied. */
  private static final float DEFAULT_SELECTED_MARKER_HUE = BitmapDescriptorFactory.HUE_GREEN;

  /**
   * The index of the currently selected marker. Used when saving the instance.
   */
  private static final String SAVE_KEY_INDEX = "saveKeyIndex";
  /**
   * The latitude of the center position where the camera is looking. Used when
   * saving the instance.
   */
  private static final String SAVE_TARGET_LAT = "saveTargetLat";
  /**
   * The longitude of the center position where the camera is looking. Used when
   * saving the instance.
   */
  private static final String SAVE_TARGET_LONG = "saveTargetLong";
  /** The zoom level of the camera. Used when saving the instance. */
  private static final String SAVE_ZOOM = "saveZoom";

  /**
   * Interface for listening to different events that may be triggered by this
   * inner fragment.
   */
  public interface TableMapInnerFragmentListener {

    /**
     * Set the index of the marker that has been selected.
     */
    void onSetSelectedItemIndex(int i);

    /**
     * Sets that no item is selected.
     */
    void setNoItemSelected();

  }

  /** The object that is listening in on events. */
  public TableMapInnerFragmentListener listener;

  /**
   * A mapping of all markers to index to determine which marker is selected.
   */
  private Map<Marker, Integer> mMarkerIds;

  /** A set of all the visible markers. */
  private Set<Marker> mVisibleMarkers;

  /** The currently selected marker. */
  private Marker mCurrentMarker;

  /** Used for coloring markers. */
  private ColorRuleGroup mColorGroup;

  /** the latitide elementKey to use for plotting */
  private String mLatitudeElementKey;
  
  /** the longitude elementKey to use for plotting */
  private String mLongitudeElementKey;
  
  /**
   * This value is only set after the activity was saved and then reinstated. It
   * is used to figure out which marker was selected before the activity was
   * previously destroyed. It will be set to -1 if no index was selected.
   */
  private int mCurrentIndex;

  int retrieveSavedIndexFromBundle(Bundle bundle) {
    if (bundle != null && bundle.containsKey(SAVE_KEY_INDEX)) {
      return bundle.getInt(SAVE_KEY_INDEX);
    } else {
      return INVALID_INDEX;
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // AppName may not yet be available...
    this.mCurrentIndex = this.retrieveSavedIndexFromBundle(savedInstanceState);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    AbsBaseActivity activity = (AbsBaseActivity) getActivity();
    WebLogger.getLogger(activity.getAppName()).d(TAG, "[onSaveInstanceState]");
    int markerIndexToSave = INVALID_INDEX;
    if (mCurrentMarker != null) {
      markerIndexToSave = mMarkerIds.get(mCurrentMarker);
    }
    WebLogger.getLogger(activity.getAppName()).d(TAG,
        "[onSaveInstanceState] saving markder index: " + markerIndexToSave);
    outState.putInt(SAVE_KEY_INDEX, markerIndexToSave);
    CameraPosition pos = getMap().getCameraPosition();
    outState.putFloat(SAVE_ZOOM, pos.zoom);
    outState.putDouble(SAVE_TARGET_LAT, pos.target.latitude);
    outState.putDouble(SAVE_TARGET_LONG, pos.target.longitude);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    AbsBaseActivity activity = (AbsBaseActivity) getActivity();
    WebLogger.getLogger(activity.getAppName()).d(TAG, "[onActivityCreated]");
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    AbsBaseActivity activity = (AbsBaseActivity) getActivity();
    WebLogger.getLogger(activity.getAppName()).d(TAG, "[onViewCreated]");
    clearAndInitializeMap();
    if ( !Tables.getInstance().isMocked() ) {
      if (savedInstanceState != null) {
        savedInstanceState.setClassLoader(LatLng.class.getClassLoader());
        getMap().moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                new LatLng(savedInstanceState.getDouble(SAVE_TARGET_LAT), savedInstanceState
                    .getDouble(SAVE_TARGET_LONG)), savedInstanceState.getFloat(SAVE_ZOOM)));
      }
      getMap().setMyLocationEnabled(true);
      getMap().setOnMapLongClickListener(getOnMapLongClickListener());
      getMap().setOnMapClickListener(getOnMapClickListener());
    }
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    AbsBaseActivity aactivity = (AbsBaseActivity) activity;
    WebLogger.getLogger(aactivity.getAppName()).d(TAG, "[onAttach]");
  }

  @Override
  public void onDetach() {
    super.onDetach();
    AbsBaseActivity activity = (AbsBaseActivity) getActivity();
    WebLogger.getLogger(activity.getAppName()).d(TAG, "[onDetach]");
  }

  @Override
  public void onStart() {
    super.onStart();
    AbsBaseActivity activity = (AbsBaseActivity) getActivity();
    WebLogger.getLogger(activity.getAppName()).d(TAG, "[onStart]");
  }

  @Override
  public void onStop() {
    super.onStop();
    AbsBaseActivity activity = (AbsBaseActivity) getActivity();
    WebLogger.getLogger(activity.getAppName()).d(TAG, "[onStop]");
  }

  /** Re-initializes the map, including the markers. 
   * @throws RemoteException */
  public void clearAndInitializeMap() {
    AbsBaseActivity activity = (AbsBaseActivity) getActivity();
    WebLogger.getLogger(activity.getAppName()).d(TAG, "[clearAndInitializeMap]");
    if( getMap() != null ) {
      getMap().clear();
    }
    try {
      resetColorProperties();
      setMarkers();
    } catch (RemoteException e) {
      WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
      WebLogger.getLogger(activity.getAppName()).e(TAG, "Unable to access database");
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    AbsBaseActivity activity = (AbsBaseActivity) getActivity();
    WebLogger.getLogger(activity.getAppName()).d(TAG, "[onDestroy]");
    // Clear up any memory references. When destroyed, there cannot be any
    // references to the markers, otherwise leaks will happen.
    mMarkerIds.clear();
    mVisibleMarkers.clear();
    mCurrentMarker = null;
  }

  @Override
  public void onPause() {
    super.onPause();
    AbsBaseActivity activity = (AbsBaseActivity) getActivity();
    WebLogger.getLogger(activity.getAppName()).d(TAG, "[onPause]");
  }

  @Override
  public void onResume() {
    super.onResume();
    AbsBaseActivity activity = (AbsBaseActivity) getActivity();
    WebLogger.getLogger(activity.getAppName()).d(TAG, "[onResume]");
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return super.onCreateView(inflater, container, savedInstanceState);
  }

  /**
   * Sets up the color properties used for color rules.
   * @throws RemoteException 
   */
  public void resetColorProperties() throws RemoteException {
    findColorGroupAndDbConfiguration();
  }

  /**
   * Finds the color group that will be needed when making color rules.
   * @throws RemoteException 
   */
  private void findColorGroupAndDbConfiguration() throws RemoteException {
    // Grab the color group
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();

    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(activity.getAppName(), false);
      
      // get the elementKey for the latitude and longitude columns
      mLatitudeElementKey = getLatitudeElementKey(db);
      mLongitudeElementKey = getLongitudeElementKey(db);

      String[] adminColumns = Tables.getInstance().getDatabase().getAdminColumns();
      // Grab the key value store helper from the map fragment.
      final KeyValueStoreHelper kvsHelper = new KeyValueStoreHelper(
          Tables.getInstance(), activity.getAppName(), db,
          activity.getTableId(), LocalKeyValueStoreConstants.Map.PARTITION);
      String colorType = kvsHelper.getString(TablePropertiesManager.KEY_COLOR_RULE_TYPE);
      if (colorType == null) {
        kvsHelper.setString(TablePropertiesManager.KEY_COLOR_RULE_TYPE,
            TablePropertiesManager.COLOR_TYPE_NONE);
        colorType = TablePropertiesManager.COLOR_TYPE_NONE;
      }
  
      // Create a guide depending on what type of color rule is selected.
      mColorGroup = null;
      if (colorType.equals(TablePropertiesManager.COLOR_TYPE_TABLE)) {
        mColorGroup = ColorRuleGroup.getTableColorRuleGroup(Tables.getInstance(), activity.getAppName(),
            db, activity.getTableId(), adminColumns);
      }
      if (colorType.equals(TablePropertiesManager.COLOR_TYPE_STATUS)) {
        mColorGroup = ColorRuleGroup.getStatusColumnRuleGroup(Tables.getInstance(), activity.getAppName(),
            db, activity.getTableId(), adminColumns);
      }
      if (colorType.equals(TablePropertiesManager.COLOR_TYPE_COLUMN)) {
        String colorColumnKey = kvsHelper.getString(TablePropertiesManager.KEY_COLOR_RULE_COLUMN);
        if (colorColumnKey != null) {
          mColorGroup = ColorRuleGroup.getColumnColorRuleGroup(Tables.getInstance(), activity.getAppName(),
              db, activity.getTableId(), colorColumnKey, adminColumns);
        }
      }
    } finally {
      if ( db != null ) {
        Tables.getInstance().getDatabase().closeDatabase(activity.getAppName(), db);
      }
    }
  }

  /**
   * Get the {@link UserTable} that this view is displaying.
   * 
   * @return
   */
  UserTable retrieveUserTable() {
    TableDisplayActivity activity = (TableDisplayActivity) this.getActivity();
    UserTable result = activity.getUserTable();
    return result;
  }

  /**
   * Sets the location markers based off of the columns set in the table
   * properties.
   */
  private void setMarkers() throws RemoteException {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();

    boolean isMocked = Tables.getInstance().isMocked();

    if (mMarkerIds != null) {
      mMarkerIds.clear();
    }
    if (mVisibleMarkers != null) {
      mVisibleMarkers.clear();
    }

    mMarkerIds = new HashMap<Marker, Integer>();
    mVisibleMarkers = new HashSet<Marker>();

    if (mLatitudeElementKey == null || mLongitudeElementKey == null) {
      Toast.makeText(getActivity(), getActivity().getString(R.string.lat_long_not_set),
          Toast.LENGTH_LONG).show();
      return;
    }

    UserTable table = this.retrieveUserTable();

    OrderedColumns orderedDefns = activity.getColumnDefinitions();
    // Try to find the map columns in the store.
    ColumnDefinition latitudeColumn = orderedDefns.find(mLatitudeElementKey);
    ColumnDefinition longitudeColumn = orderedDefns.find(mLongitudeElementKey);

    // Find the locations from entries in the table.
    LatLng firstLocation = null;

    // Go through each row and create a marker at the specified location.
    for (int i = 0; i < table.getNumberOfRows(); i++) {
      Row row = table.getRowAtIndex(i);
      String latitudeString = row.getRawDataOrMetadataByElementKey(latitudeColumn.getElementKey());
      String longitudeString = row
          .getRawDataOrMetadataByElementKey(longitudeColumn.getElementKey());
      if (latitudeString == null || longitudeString == null || latitudeString.length() == 0
          || longitudeString.length() == 0) {
        continue;
      }

      // Create a LatLng from the latitude and longitude strings.
      LatLng location = parseLocationFromString(latitudeColumn, latitudeString, longitudeColumn,
          longitudeString);
      if (location == null) {
        continue;
      }
      if (firstLocation == null) {
        firstLocation = location;
      }

      if ( !isMocked ) {
        Marker marker = getMap().addMarker(
            new MarkerOptions().position(location).draggable(false)
                .icon(BitmapDescriptorFactory.defaultMarker(getHueForRow(i))));
        mMarkerIds.put(marker, i);
        if (mCurrentIndex == i) {
          WebLogger.getLogger(activity.getAppName()).d(TAG, "[setMarkers] selecting marker: " + i);
          selectMarker(marker);
        }
      }
    }

    if (firstLocation != null) {
      if ( !isMocked ) {
        getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 12f));
        getMap().setOnMarkerClickListener(getOnMarkerClickListener());
      }
    }
  }

  /**
   * Retrieves the hue of the specified row depending on the current color
   * rules.
   *
   * @param index
   *          The index of the row to search for.
   * @return The hue depending on the color rules for this row, or the default
   *         marker color if no rules apply to the row.
   */
  private float getHueForRow(int index) {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();

    UserTable table = this.retrieveUserTable();
    // Create a guide depending on the color group.
    if (mColorGroup != null) {
      ColorGuide guide = mColorGroup.getColorGuide(activity.getColumnDefinitions(),
          table.getRowAtIndex(index));
      // Based on if the guide matched or not, grab the hue.
      if (guide != null) {
        float[] hsv = new float[3];
        Color.colorToHSV(guide.getBackground(), hsv);
        return hsv[0];
      }
    }

    return DEFAULT_MARKER_HUE;
  }

  private String getLatitudeElementKey(OdkDbHandle dbHandle) throws RemoteException {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();

    OrderedColumns orderedDefns = activity.getColumnDefinitions();
    final List<ColumnDefinition> geoPointCols = orderedDefns.getGeopointColumnDefinitions();
    // Grab the key value store helper from the table activity.
    final KeyValueStoreHelper kvsHelper = new KeyValueStoreHelper(
        Tables.getInstance(), activity.getAppName(), dbHandle,
        activity.getTableId(), LocalKeyValueStoreConstants.Map.PARTITION);
    String latitudeElementKey = kvsHelper
        .getString(LocalKeyValueStoreConstants.Map.KEY_MAP_LAT_COL);
    if (latitudeElementKey == null) {
      // Go through each of the columns and check to see if there are
      // any columns labeled latitude or longitude.
      for (ColumnDefinition cd : orderedDefns.getColumnDefinitions()) {
        if (GeoColumnUtil.get().isLatitudeColumnDefinition(geoPointCols, cd)) {
          latitudeElementKey = cd.getElementKey();
          kvsHelper.setString(LocalKeyValueStoreConstants.Map.KEY_MAP_LAT_COL, latitudeElementKey);
          break;
        }
      }
    }
    return latitudeElementKey;
  }

  private String getLongitudeElementKey(OdkDbHandle dbHandle) throws RemoteException {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();
    OrderedColumns orderedDefns = activity.getColumnDefinitions();

    final List<ColumnDefinition> geoPointCols = orderedDefns.getGeopointColumnDefinitions();
    // Grab the key value store helper from the table activity.
    final KeyValueStoreHelper kvsHelper = new KeyValueStoreHelper(
        Tables.getInstance(), activity.getAppName(), dbHandle, 
        activity.getTableId(), LocalKeyValueStoreConstants.Map.PARTITION);
    String longitudeElementKey = kvsHelper
        .getString(LocalKeyValueStoreConstants.Map.KEY_MAP_LONG_COL);
    if (longitudeElementKey == null) {
      // Go through each of the columns and check to see if there are
      // any columns labled longitude
      for (ColumnDefinition cd : orderedDefns.getColumnDefinitions()) {
        if (GeoColumnUtil.get().isLongitudeColumnDefinition(geoPointCols, cd)) {
          longitudeElementKey = cd.getElementKey();
          kvsHelper
              .setString(LocalKeyValueStoreConstants.Map.KEY_MAP_LONG_COL, longitudeElementKey);
          break;
        }
      }
    }
    return longitudeElementKey;
  }

  /**
   * Parses the location string and creates a LatLng. The format of the string
   * should be: lat,lng
   */
  @SuppressWarnings("unused")
  private LatLng parseLocationFromString(String location) {
    String[] split = location.split(",");
    try {
      return new LatLng(Double.parseDouble(split[0]), Double.parseDouble(split[1]));
    } catch (Exception e) {
      AbsBaseActivity activity = (AbsBaseActivity) getActivity();
      WebLogger.getLogger(activity.getAppName()).e(TAG,
          "The following location is not in the proper lat,lng form: " + location);
    }
    return null;
  }

  /**
   * Parses the latitude and longitude strings and creates a LatLng.
   */
  private LatLng parseLocationFromString(ColumnDefinition latitudeColumn, String latitude,
      ColumnDefinition longitudeColumn, String longitude) {
    try {
      return new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
    } catch (Exception e) {
      AbsBaseActivity activity = (AbsBaseActivity) getActivity();
      WebLogger.getLogger(activity.getAppName()).e(TAG,
          "The following location did not parse correctly: " + latitude + "," + longitude);
    }
    return null;
  }

  /**
   * If a marker is selected, deselect it.
   */
  private OnMapClickListener getOnMapClickListener() {
    return new OnMapClickListener() {
      @Override
      public void onMapClick(LatLng point) {
        deselectCurrentMarker();
      }
    };
  }

  /**
   * On a long click, add a row to the data table at the position clicked.
   */
  private OnMapLongClickListener getOnMapLongClickListener() {
    return new OnMapLongClickListener() {
      @Override
      public void onMapLongClick(LatLng location) {
        TableDisplayActivity activity = (TableDisplayActivity) getActivity();

        // Create a mapping from the lat and long columns to the
        // values in the location.
        Map<String, String> elementNameToValue = new HashMap<String, String>();

        OrderedColumns orderedDefns = activity.getColumnDefinitions();
        for (ColumnDefinition cd : orderedDefns.getColumnDefinitions()) {
          elementNameToValue.put(cd.getElementName(), "");
        }
        {
          ColumnDefinition latitudeColumn = orderedDefns.find(mLatitudeElementKey);
          elementNameToValue.put(mLatitudeElementKey, Double.toString(location.latitude));
        }

        {
          ColumnDefinition longitudeColumn = orderedDefns.find(mLongitudeElementKey);
          elementNameToValue.put(mLongitudeElementKey, Double.toString(location.longitude));
        }
        // To store the mapping in a bundle, we need to put it in string list.
        ArrayList<String> bundleStrings = new ArrayList<String>();
        for (String key : elementNameToValue.keySet()) {
          bundleStrings.add(key);
          bundleStrings.add(elementNameToValue.get(key));
        }
        // Bundle it all up for the fragment.
        Bundle b = new Bundle();
        b.putStringArrayList(LocationDialogFragment.ELEMENT_NAME_TO_VALUE_KEY, bundleStrings);
        b.putString(LocationDialogFragment.LOCATION_KEY, location.toString());
        LocationDialogFragment dialog = new LocationDialogFragment();
        dialog.setArguments(b);
        dialog.show(getFragmentManager(), "LocationDialogFragment");
      }
    };
  }

  /**
   * When a marker is clicked, set the index of the list fragment, and then show
   * it. If that index is already selected, then hide it.
   */
  private OnMarkerClickListener getOnMarkerClickListener() {
    return new OnMarkerClickListener() {
      @Override
      public boolean onMarkerClick(Marker arg0) {
        int index = (mCurrentMarker != null) ? mMarkerIds.get(mCurrentMarker) : INVALID_INDEX;
        // Make the marker visible if it is either invisible or a
        // new marker.
        // Make the marker invisible if clicking on the already
        // selected marker.
        if (index != mMarkerIds.get(arg0)) {
          deselectCurrentMarker();
          int newIndex = mMarkerIds.get(arg0);
          selectMarker(arg0);
          listener.onSetSelectedItemIndex(newIndex);
        } else {
          deselectCurrentMarker();
        }
        return true;
      }
    };
  }

  /**
   * Selects a marker, updating the marker list, and changing the marker's color
   * to green. Makes the marker the currently selected marker.
   *
   * @param marker
   *          The marker to be selected.
   */
  private void selectMarker(Marker marker) {
    if (mCurrentMarker == marker)
      return;
    marker.setIcon(BitmapDescriptorFactory.defaultMarker(DEFAULT_SELECTED_MARKER_HUE));
    mCurrentMarker = marker;
  }

  /**
   * Deselects the currently selected marker, updating the marker list, and
   * changing the marker back to a default color.
   */
  private void deselectCurrentMarker() {
    if (mCurrentMarker == null) {
      return;
    }
    int index = mMarkerIds.get(mCurrentMarker);
    mCurrentMarker.setIcon(BitmapDescriptorFactory.defaultMarker(getHueForRow(index)));
    mCurrentMarker = null;
    listener.setNoItemSelected();
  }
}
