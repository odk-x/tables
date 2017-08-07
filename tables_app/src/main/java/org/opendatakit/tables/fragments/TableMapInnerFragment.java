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

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.*;
import org.opendatakit.activities.BaseActivity;
import org.opendatakit.activities.IAppAwareActivity;
import org.opendatakit.data.ColorGuide;
import org.opendatakit.data.ColorGuideGroup;
import org.opendatakit.data.ColorRuleGroup;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.LocalKeyValueStoreConstants;
import org.opendatakit.database.data.ColumnDefinition;
import org.opendatakit.database.data.OrderedColumns;
import org.opendatakit.database.data.Row;
import org.opendatakit.database.data.UserTable;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.utilities.ODKFileUtils;
import org.opendatakit.views.ViewDataQueryParams;

import java.util.HashMap;
import java.util.Map;

/**
 * The InnerMapFragment has the capability of showing a map. It displays markers
 * for records in a database.
 *
 * @author Chris Gelon (cgelon)
 * @author sudar.sam@gmail.com
 */
public class TableMapInnerFragment extends MapFragment implements OnMapReadyCallback {

  private static final String TAG = TableMapInnerFragment.class.getSimpleName();

  private static final int INVALID_INDEX = -1;

  /**
   * The default hue for markers if no color rules are applied.
   */
  private static final float DEFAULT_MARKER_HUE = BitmapDescriptorFactory.HUE_AZURE;
  /**
   * The default hue for markers if no color rules are applied.
   */
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
  /**
   * The zoom level of the camera. Used when saving the instance.
   */
  private static final String SAVE_ZOOM = "saveZoom";

  /**
   * Minimum distance from a marker to the edge of the screen when setting up the initial camera position
   */
  private static final int PADDING = 50;

  private static final float initCameraValue = -1;
  /**
   * The object that is listening in on events.
   */
  public TableMapInnerFragmentListener listener = null;
  private GoogleMap map = null;
  private double savedLatitude = initCameraValue;
  private double savedLongitude = initCameraValue;
  private float savedZoom = initCameraValue;
  /**
   * A mapping of all markers to index to determine which marker is selected.
   */
  private Map<Marker, Integer> mMarkerIds = null;
  /**
   * The currently selected marker.
   */
  private Marker mCurrentMarker = null;
  private ColorGuideGroup mColorGuideGroup = null;
  /**
   * the latitide elementKey to use for plotting
   */
  private String mLatitudeElementKey = null;
  /**
   * the longitude elementKey to use for plotting
   */
  private String mLongitudeElementKey = null;

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    AbsBaseActivity activity = (AbsBaseActivity) getActivity();
    WebLogger.getLogger(activity.getAppName()).d(TAG, "[onViewCreated]");

    getMapAsync(this);
  }

  @Override
  public void onMapReady(final GoogleMap map) {
    if (map != null) {
      this.map = map;

      clearAndInitializeMap();

      this.map.setMyLocationEnabled(true);
      this.map.setOnMapLongClickListener(getOnMapLongClickListener());
      this.map.setOnMapClickListener(getOnMapClickListener());
    }
  }

  /**
   * Re-initializes the map, including the markers.
   **/
  public void clearAndInitializeMap() {
    AbsBaseActivity activity = (AbsBaseActivity) getActivity();
    WebLogger.getLogger(activity.getAppName()).d(TAG, "[clearAndInitializeMap]");
    if (map != null) {
      map.clear();
    }
    try {
      resetColorProperties();
      setMarkers();
    } catch (ServicesAvailabilityException e) {
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
    if (map != null) {
      mMarkerIds.clear();
      mCurrentMarker = null;
      map = null;
    }
  }

  /**
   * Sets up the color properties used for color rules.
   *
   * @throws ServicesAvailabilityException if the database is down
   */
  public void resetColorProperties() throws ServicesAvailabilityException {
    findColorGroupAndDbConfiguration();
  }

  /**
   * Finds the color group that will be needed when making color rules.
   *
   * @throws ServicesAvailabilityException if the database is down
   */
  private void findColorGroupAndDbConfiguration() throws ServicesAvailabilityException {
    // Grab the color group
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();

    UserDbInterface dbInterface = Tables.getInstance().getDatabase();
    DbHandle db = null;
    try {
      db = dbInterface.openDatabase(activity.getAppName());

      // get the elementKey for the latitude and longitude columns
      mLatitudeElementKey = getLatitudeElementKey(db);
      mLongitudeElementKey = getLongitudeElementKey(db);

      String[] adminColumns = dbInterface.getAdminColumns();

      TableUtil.MapViewColorRuleInfo colorRuleInfo = TableUtil.get()
          .getMapListViewColorRuleInfo(dbInterface, activity.getAppName(), db,
              activity.getTableId());

      // Create a guide depending on what type of color rule is selected.
      /*
    Used for coloring markers.
   */
      ColorRuleGroup mColorGroup = null;
      if (colorRuleInfo.colorType != null && colorRuleInfo.colorType.equals(LocalKeyValueStoreConstants.Map.COLOR_TYPE_TABLE)) {
        mColorGroup = ColorRuleGroup
            .getTableColorRuleGroup(dbInterface, activity.getAppName(), db, activity.getTableId(),
                adminColumns);
      }
      if (colorRuleInfo.colorType != null && colorRuleInfo.colorType.equals(LocalKeyValueStoreConstants.Map.COLOR_TYPE_STATUS)) {
        mColorGroup = ColorRuleGroup
            .getStatusColumnRuleGroup(dbInterface, activity.getAppName(), db, activity.getTableId(),
                adminColumns);
      }

      if (mColorGroup != null) {
        UserTable userTableForColor = activity.getUserTable();
        mColorGuideGroup = new ColorGuideGroup(mColorGroup, userTableForColor);
      }
    } finally {
      if (db != null) {
        dbInterface.closeDatabase(activity.getAppName(), db);
      }
    }
  }

  /**
   * Sets the location markers based off of the columns set in the table
   * properties.
   */
  private void setMarkers() {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();

    if (mMarkerIds != null) {
      mMarkerIds.clear();
    }

    mMarkerIds = new HashMap<>();

    if (mLatitudeElementKey == null || mLongitudeElementKey == null) {
      Toast.makeText(getActivity(), getActivity().getString(R.string.lat_long_not_set),
          Toast.LENGTH_LONG).show();
      return;
    }

    OrderedColumns orderedDefns = activity.getColumnDefinitions();
    ViewDataQueryParams params = activity.getViewQueryParams(Constants.FragmentTags.MAP_INNER_MAP);
      UserTable table;
    try {
        table = Tables.getInstance().getDatabase().simpleQuery(activity.getAppName(), Tables.getInstance().getDatabase().openDatabase(activity.getAppName()), params.tableId, orderedDefns, params.whereClause, params.selectionArgs, params.groupBy, params.having, new String[] { params.orderByElemKey }, new String[] { params.orderByDir }, 10000, 0);
    } catch (Exception e) { return; }


    if (table != null && orderedDefns != null) {
      // Try to find the map columns in the store.
      ColumnDefinition latitudeColumn = orderedDefns.find(mLatitudeElementKey);
      ColumnDefinition longitudeColumn = orderedDefns.find(mLongitudeElementKey);

      // Find the locations from entries in the table.
      LatLngBounds.Builder builder = new LatLngBounds.Builder();
      boolean includedAtLeastOne = false;

      // Go through each row and create a marker at the specified location.
      for (int i = 0; i < table.getNumberOfRows(); i++) {
        Row row = table.getRowAtIndex(i);
        String latitudeString = row.getDataByKey(latitudeColumn.getElementKey());
        String longitudeString = row.getDataByKey(longitudeColumn.getElementKey());
        if (latitudeString == null || longitudeString == null || latitudeString.isEmpty()
            || longitudeString.isEmpty()) {
          continue;
        }

        // Create a LatLng from the latitude and longitude strings.
        LatLng location = parseLocationFromString(latitudeString, longitudeString);
        if (location == null) {
          continue;
        }
        includedAtLeastOne = true;
        builder.include(location);

        if (map != null) {
          Marker marker = map.addMarker(new MarkerOptions().position(location).draggable(false)
              .icon(BitmapDescriptorFactory.defaultMarker(getHueForRow(i))));
          mMarkerIds.put(marker, i);
        }
      }

    if (includedAtLeastOne) {
      map.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), PADDING));
    }
    map.setOnMarkerClickListener(getOnMarkerClickListener());
    }
  }

  /**
   * Retrieves the hue of the specified row depending on the current color
   * rules.
   *
   * @param index The index of the row to search for.
   * @return The hue depending on the color rules for this row, or the default
   * marker color if no rules apply to the row.
   */
  private float getHueForRow(int index) {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();

    UserTable table = activity.getUserTable();
    // Create a guide depending on the color group.
    if (table != null && mColorGuideGroup != null) {
      ColorGuide guide = mColorGuideGroup.getColorGuideForRowIndex(index);
      // Based on if the guide matched or not, grab the hue.
      if (guide != null) {
        float[] hsv = new float[3];
        Color.colorToHSV(guide.getBackground(), hsv);
        return hsv[0];
      }
    }

    return DEFAULT_MARKER_HUE;
  }

  private String getLatitudeElementKey(DbHandle dbHandle) throws ServicesAvailabilityException {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();

    OrderedColumns orderedDefns = activity.getColumnDefinitions();
    return TableUtil.get()
        .getMapListViewLatitudeElementKey(Tables.getInstance().getDatabase(),
            activity.getAppName(), dbHandle, activity.getTableId(), orderedDefns);
  }

  private String getLongitudeElementKey(DbHandle dbHandle) throws ServicesAvailabilityException {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();
    OrderedColumns orderedDefns = activity.getColumnDefinitions();
    return TableUtil.get()
        .getMapListViewLongitudeElementKey(Tables.getInstance().getDatabase(),
            activity.getAppName(), dbHandle, activity.getTableId(), orderedDefns);
  }

  /**
   * Parses the latitude and longitude strings and creates a LatLng.
   */
  private LatLng parseLocationFromString(String latitude, String longitude) {
    try {
      return new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
    } catch (Exception e) {
      String appName = ((IAppAwareActivity) getActivity()).getAppName();
      WebLogger.getLogger(appName)
          .e(TAG, "The following location did not parse correctly: " + latitude + "," + longitude);
      WebLogger.getLogger(appName).printStackTrace(e);
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
  // Check that this is not working on 208
  private OnMapLongClickListener getOnMapLongClickListener() {
    return new OnMapLongClickListener() {
      @Override
      public void onMapLongClick(LatLng location) {
        TableDisplayActivity activity = (TableDisplayActivity) getActivity();

        // Create a mapping from the lat and long columns to the
        // values in the location.
        Map<String, Object> elementKeyToValue = new HashMap<>();

        elementKeyToValue.put(mLatitudeElementKey, location.latitude);
        elementKeyToValue.put(mLongitudeElementKey, location.longitude);

        // To store the mapping in a bundle, JSON stringify it.
        String jsonStringifyElementKeyToValueMap;
        try {
          jsonStringifyElementKeyToValueMap = ODKFileUtils.mapper
              .writeValueAsString(elementKeyToValue);
        } catch (JsonProcessingException e) {
          WebLogger.getLogger(activity.getAppName()).printStackTrace(e);
          throw new IllegalStateException("should never happen");
        }
        // Bundle it all up for the fragment.
        Bundle b = new Bundle();
        b.putString(LocationDialogFragment.ELEMENT_KEY_TO_VALUE_MAP_KEY,
            jsonStringifyElementKeyToValueMap);
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
      public boolean onMarkerClick(Marker clickedMarker) {
        int index = clickedMarker == null ? INVALID_INDEX : mMarkerIds.get(clickedMarker);
        WebLogger.getLogger(null).e(TAG, Integer.toString(index));
        // Make the marker visible if it is either invisible or a
        // new marker.
        // Make the marker invisible if clicking on the already
        // selected marker.
        if (mCurrentMarker == null || index != mMarkerIds.get(mCurrentMarker)) {
          deselectCurrentMarker();
          selectMarker(clickedMarker);
          listener.onSetSelectedItemIndex(index);
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
   * @param marker The marker to be selected.
   */
  private void selectMarker(Marker marker) {
    if (mCurrentMarker != null && mCurrentMarker.equals(marker))
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

  /**
   * Interface for listening to different events that may be triggered by this
   * inner fragment.
   */
  public interface TableMapInnerFragmentListener {

    /**
     * Set the index of the marker that has been selected.
     *
     * @param i the index of the marker
     */
    void onSetSelectedItemIndex(int i);

    /**
     * Sets that no item is selected.
     */
    void setNoItemSelected();

  }
}
