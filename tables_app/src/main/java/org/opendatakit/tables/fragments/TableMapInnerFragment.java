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

import android.Manifest;
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
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.opendatakit.activities.IAppAwareActivity;
import org.opendatakit.data.ColorGuide;
import org.opendatakit.data.ColorGuideGroup;
import org.opendatakit.data.ColorRuleGroup;
import org.opendatakit.data.utilities.TableUtil;
import org.opendatakit.database.LocalKeyValueStoreConstants;
import org.opendatakit.database.data.*;
import org.opendatakit.database.queries.ArbitraryQuery;
import org.opendatakit.database.queries.ResumableQuery;
import org.opendatakit.database.queries.SimpleQuery;
import org.opendatakit.database.queries.SingleRowQuery;
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
import org.opendatakit.utilities.RuntimePermissionUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * The InnerMapFragment has the capability of showing a map. It displays markers
 * for records in a database.
 *
 * @author Chris Gelon (cgelon)
 * @author sudar.sam@gmail.com
 */
public class TableMapInnerFragment extends SupportMapFragment implements OnMapReadyCallback {
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
  /**
   * This value is only set after the activity was saved and then reinstated. It
   * is used to figure out which marker was selected before the activity was
   * previously destroyed. It will be set to -1 if no index was selected.
   */
  private int mCurrentIndex = 0;

  /**
   * Gets an index from the passed bundle if it exists
   *
   * @param bundle the bundle to pull the index from
   * @return the index that was in the bundle or {@link #INVALID_INDEX}
   */
  static int retrieveSavedIndexFromBundle(Bundle bundle) {
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
    this.mCurrentIndex = TableMapInnerFragment.retrieveSavedIndexFromBundle(savedInstanceState);
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
    WebLogger.getLogger(activity.getAppName())
        .d(TAG, "[onSaveInstanceState] saving markder index: " + markerIndexToSave);
    outState.putInt(SAVE_KEY_INDEX, markerIndexToSave);

    if (map != null) {
      CameraPosition pos = map.getCameraPosition();
      outState.putFloat(SAVE_ZOOM, pos.zoom);
      outState.putDouble(SAVE_TARGET_LAT, pos.target.latitude);
      outState.putDouble(SAVE_TARGET_LONG, pos.target.longitude);
    }
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    AbsBaseActivity activity = (AbsBaseActivity) getActivity();
    WebLogger.getLogger(activity.getAppName()).d(TAG, "[onViewCreated]");

    getMapAsync(this);

    if (savedInstanceState != null) {
      savedInstanceState.setClassLoader(LatLng.class.getClassLoader());
      this.savedLatitude = savedInstanceState.getDouble(SAVE_TARGET_LAT);
      this.savedLongitude = savedInstanceState.getDouble(SAVE_TARGET_LONG);
      this.savedZoom = savedInstanceState.getFloat(SAVE_ZOOM);
    }
  }

  @Override
  public void onMapReady(final GoogleMap map) {
    if (map != null) {
      this.map = map;

      clearAndInitializeMap();
      // TODO: These are floats being compared, so we should probably not be testing straight equality
      if (savedLatitude != initCameraValue && savedLongitude != initCameraValue
              && savedZoom != initCameraValue) {
        this.map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(new LatLng(savedLatitude, savedLongitude), savedZoom));
      }

      this.map.setOnMapLongClickListener(getOnMapLongClickListener());
      this.map.setOnMapClickListener(getOnMapClickListener());

      String[] permissions = new String[] {
              Manifest.permission.ACCESS_FINE_LOCATION,
              Manifest.permission.ACCESS_COARSE_LOCATION
      };

      if (RuntimePermissionUtils.checkSelfAnyPermission(getActivity(), permissions)) {
        this.map.setMyLocationEnabled(true);
      } else {
        if (!RuntimePermissionUtils.shouldShowAnyPermissionRationale(getActivity(), permissions))
        // this is when location permission is permanently denied
        Toast.makeText(getActivity(), R.string.location_permission_perm_denied, Toast.LENGTH_LONG).show();
      }
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
    ResumableQuery resumableQuery = activity.getViewQuery(Constants.FragmentTags.MAP_INNER_MAP);

    UserTable table;
    try {
      if (resumableQuery instanceof ArbitraryQuery) {
        ArbitraryQuery query = (ArbitraryQuery) resumableQuery;
        table = Tables.getInstance().getDatabase().arbitrarySqlQuery(activity.getAppName(),
            Tables.getInstance().getDatabase().openDatabase(activity.getAppName()),
            query.getTableId(), orderedDefns, query.getSqlCommand(), query.getSqlBindArgs(), -1, 0);
      } else if (resumableQuery instanceof SimpleQuery || resumableQuery instanceof SingleRowQuery) {
        SimpleQuery query = (SimpleQuery) resumableQuery;
        table = Tables.getInstance().getDatabase().simpleQuery(activity.getAppName(),
            Tables.getInstance().getDatabase().openDatabase(activity.getAppName()),
            query.getTableId(), orderedDefns, query.getWhereClause(), query.getSqlBindArgs(),
            query.getGroupByArgs(), query.getHavingClause(), query.getOrderByColNames(),
            query.getOrderByDirections(), -1, 0);
      } else {
        String appName = ((IAppAwareActivity) getActivity()).getAppName();
        WebLogger.getLogger(appName).e(TAG, "invalid query type");
        return;
      }
    } catch (ServicesAvailabilityException sae) {
      String appName = ((IAppAwareActivity) getActivity()).getAppName();
      WebLogger.getLogger(appName).e(TAG, "simpleQuery failed");
      WebLogger.getLogger(appName).printStackTrace(sae);
      return;
    }

    if (table != null && orderedDefns != null) {
      // Try to find the map columns in the store.
      ColumnDefinition latitudeColumn = orderedDefns.find(mLatitudeElementKey);
      ColumnDefinition longitudeColumn = orderedDefns.find(mLongitudeElementKey);

      // Find the locations from entries in the table.
      LatLngBounds.Builder builder = new LatLngBounds.Builder();
      int markers = 0;
      LatLng onlyLocation = null;

      // Go through each row and create a marker at the specified location.
      for (int i = 0; i < table.getNumberOfRows(); i++) {
        TypedRow row = table.getRowAtIndex(i);
        String latitudeString = row.getStringValueByKey(latitudeColumn.getElementKey());
        String longitudeString = row.getStringValueByKey(longitudeColumn.getElementKey());
        if (latitudeString == null || longitudeString == null || latitudeString.isEmpty()
                || longitudeString.isEmpty()) {
          continue;
        }

        // Create a LatLng from the latitude and longitude strings.
        LatLng location = parseLocationFromString(latitudeString, longitudeString);
        if (location == null) {
          continue;
        }
        markers++;
        builder.include(location);
        onlyLocation = location;

        if (map != null) {
          Marker marker = map.addMarker(new MarkerOptions().position(location).draggable(false)
                  .icon(BitmapDescriptorFactory.defaultMarker(getHueForRow(i))));
          mMarkerIds.put(marker, i);
          if (mCurrentIndex == i) {
            WebLogger.getLogger(activity.getAppName())
                    .d(TAG, "[setMarkers] selecting marker: " + i);
            selectMarker(marker);
          }
        }
      }

      if (markers > 1) {
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), PADDING));
      } else if (markers == 1) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(onlyLocation, 12f));
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
   * Parses the location string and creates a LatLng. The format of the string
   * should be: lat,lng
   */
  @SuppressWarnings("unused")
  private LatLng parseLocationFromString(String location) {
    String[] split = location.split(",");
    try {
      return new LatLng(Double.parseDouble(split[0]), Double.parseDouble(split[1]));
    } catch (Exception e) {
      String appName = ((IAppAwareActivity) getActivity()).getAppName();
      WebLogger.getLogger(appName)
          .e(TAG, "The following location is not in the proper lat,lng form: " + location);
      WebLogger.getLogger(appName).printStackTrace(e);
    }
    return null;
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
        dialog.show(getParentFragmentManager(), "LocationDialogFragment");
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
        int index = mCurrentMarker == null ? INVALID_INDEX : mMarkerIds.get(mCurrentMarker);
        // Make the marker visible if it is either invisible or a
        // new marker.
        // Make the marker invisible if clicking on the already
        // selected marker.
        if (index != mMarkerIds.get(clickedMarker)) {
          deselectCurrentMarker();
          int newIndex = mMarkerIds.get(clickedMarker);
          selectMarker(clickedMarker);
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
