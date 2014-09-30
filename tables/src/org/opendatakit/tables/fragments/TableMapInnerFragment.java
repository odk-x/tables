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
import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.data.UserTable.Row;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TablePropertiesManager;
import org.opendatakit.tables.utils.GeoColumnUtil;
import org.opendatakit.tables.utils.LocalKeyValueStoreConstants;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
 * The InnerMapFragment has the capability of showing a map. It displays
 * markers for records in a database.
 *
 * @author Chris Gelon (cgelon)
 * @author sudar.sam@gmail.com
 */
public class TableMapInnerFragment extends MapFragment {

  private static final String TAG =
      TableMapInnerFragment.class.getSimpleName();

  private static final int INVALID_INDEX = -1;

  /** The default hue for markers if no color rules are applied. */
  private static final float DEFAULT_MARKER_HUE =
      BitmapDescriptorFactory.HUE_AZURE;
  /** The default hue for markers if no color rules are applied. */
  private static final float DEFAULT_SELECTED_MARKER_HUE =
      BitmapDescriptorFactory.HUE_GREEN;

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
   * The longitude of the center position where the camera is looking. Used
   * when saving the instance.
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

  /**
   * This value is only set after the activity was saved and then reinstated.
   * It is used to figure out which marker was selected before the activity was
   * previously destroyed. It will be set to -1 if no index was selected.
   */
  private int mCurrentIndex;

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    Log.d(TAG, "[onActivityCreated]");
  }

  int retrieveSavedIndexFromBundle(Bundle bundle) {
    if (bundle != null && bundle.containsKey(SAVE_KEY_INDEX)) {
      return bundle.getInt(SAVE_KEY_INDEX);
    } else {
      return INVALID_INDEX;
    }
  }


  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    Log.d(TAG, "[onViewCreated]");
    clearAndInitializeMap();
    if (savedInstanceState != null) {
      savedInstanceState.setClassLoader(LatLng.class.getClassLoader());
      getMap().moveCamera(
          CameraUpdateFactory.newLatLngZoom(
              new LatLng(
                  savedInstanceState.getDouble(SAVE_TARGET_LAT),
                  savedInstanceState.getDouble(SAVE_TARGET_LONG)),
              savedInstanceState.getFloat(SAVE_ZOOM)));
    }
    getMap().setOnMapLongClickListener(getOnMapLongClickListener());
    getMap().setOnMapClickListener(getOnMapClickListener());
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    Log.d(TAG, "[onAttach]");
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "[onCreate]");
    this.mCurrentIndex = this.retrieveSavedIndexFromBundle(savedInstanceState);
    Log.d(TAG, "[onCreate] retrieved index is: " + mCurrentIndex);
  }

  @Override
  public void onDetach() {
    super.onDetach();
    Log.d(TAG, "[onDetach]");
  }

  @Override
  public void onStart() {
    super.onStart();
    Log.d(TAG, "[onStart]");
  }

  @Override
  public void onStop() {
    super.onStop();
    Log.d(TAG, "[onStop]");
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Log.d(TAG, "[onSaveInstanceState]");
    int markerIndexToSave = INVALID_INDEX;
    if (mCurrentMarker != null) {
      markerIndexToSave = mMarkerIds.get(mCurrentMarker);
    }
    Log.d(
        TAG,
        "[onSaveInstanceState] saving markder index: " + markerIndexToSave);
    outState.putInt(
        SAVE_KEY_INDEX,
        markerIndexToSave);
    CameraPosition pos = getMap().getCameraPosition();
    outState.putFloat(SAVE_ZOOM, pos.zoom);
    outState.putDouble(SAVE_TARGET_LAT, pos.target.latitude);
    outState.putDouble(SAVE_TARGET_LONG, pos.target.longitude);
  }

  /** Re-initializes the map, including the markers. */
  public void clearAndInitializeMap() {
    Log.d(TAG, "[clearAndInitializeMap]");
    getMap().clear();
    resetColorProperties();
    setMarkers();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "[onDestroy]");
    // Clear up any memory references. When destroyed, there cannot be any
    // references to the markers, otherwise leaks will happen.
    mMarkerIds.clear();
    mVisibleMarkers.clear();
    mCurrentMarker = null;
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.d(TAG, "[onPause]");
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "[onResume]");
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    Log.d(TAG, "[onCreateView]");
    return super.onCreateView(inflater, container, savedInstanceState);
  }

  /**
   * Sets up the color properties used for color rules.
   */
  public void resetColorProperties() {
    findColorGroup();
  }

  /**
   * Finds the color group that will be needed when making color rules.
   */
  private void findColorGroup() {
    // Grab the color group
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();

    // Grab the key value store helper from the map fragment.
    final KeyValueStoreHelper kvsHelper = new KeyValueStoreHelper(
        activity, activity.getAppName(), activity.getTableId(),
        LocalKeyValueStoreConstants.Map.PARTITION);
    String colorType = kvsHelper.getString(
        TablePropertiesManager.KEY_COLOR_RULE_TYPE);
    if (colorType == null) {
      kvsHelper.setString(
          TablePropertiesManager.KEY_COLOR_RULE_TYPE,
          TablePropertiesManager.COLOR_TYPE_NONE);
      colorType = TablePropertiesManager.COLOR_TYPE_NONE;
    }

    // Create a guide depending on what type of color rule is selected.
    mColorGroup = null;
    if (colorType.equals(TablePropertiesManager.COLOR_TYPE_TABLE)) {
      mColorGroup = ColorRuleGroup.getTableColorRuleGroup(
          getActivity(), activity.getAppName(), activity.getTableId());
    }
    if (colorType.equals(TablePropertiesManager.COLOR_TYPE_STATUS)) {
      mColorGroup = ColorRuleGroup.getStatusColumnRuleGroup(
          getActivity(), activity.getAppName(), activity.getTableId());
    }
    if (colorType.equals(TablePropertiesManager.COLOR_TYPE_COLUMN)) {
      String colorColumnKey =
          kvsHelper.getString(TablePropertiesManager.KEY_COLOR_RULE_COLUMN);
      if (colorColumnKey != null) {
        mColorGroup =
            ColorRuleGroup.getColumnColorRuleGroup(
                getActivity(), activity.getAppName(), activity.getTableId(), colorColumnKey);
      }
    }
  }

  /**
   * Get the {@link UserTable} that this view is displaying.
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
  private void setMarkers() {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();

    if (mMarkerIds != null) {
      mMarkerIds.clear();
    }
    if (mVisibleMarkers != null) {
      mVisibleMarkers.clear();
    }

    mMarkerIds = new HashMap<Marker, Integer>();
    mVisibleMarkers = new HashSet<Marker>();

    String latitudeElementKey = getLatitudeElementKey();
    String longitudeElementKey = getLongitudeElementKey();
    if (latitudeElementKey == null || longitudeElementKey == null) {
      Toast.makeText(
          getActivity(),
          getActivity().getString(R.string.lat_long_not_set),
          Toast.LENGTH_LONG).show();
      return;
    }

    UserTable table = this.retrieveUserTable();

    ArrayList<ColumnDefinition> orderedDefns = activity.getColumnDefinitions();
    // Try to find the map columns in the store.
    ColumnDefinition latitudeColumn =
        ColumnDefinition.find(orderedDefns, latitudeElementKey);
    ColumnDefinition longitudeColumn =
        ColumnDefinition.find(orderedDefns, longitudeElementKey);

    // Find the locations from entries in the table.
    LatLng firstLocation = null;

    // Go through each row and create a marker at the specified location.
    for (int i = 0; i < table.getNumberOfRows(); i++) {
      Row row = table.getRowAtIndex(i);
      String latitudeString = row.getRawDataOrMetadataByElementKey(
          latitudeColumn.getElementKey());
      String longitudeString = row.getRawDataOrMetadataByElementKey(
          longitudeColumn.getElementKey());
      if (latitudeString == null ||
          longitudeString == null ||
          latitudeString.length() == 0 ||
          longitudeString.length() == 0) {
        continue;
      }

      // Create a LatLng from the latitude and longitude strings.
      LatLng location = parseLocationFromString(
          latitudeColumn,
          latitudeString,
          longitudeColumn,
          longitudeString);
      if (location == null) {
        continue;
      }
      if (firstLocation == null) {
        firstLocation = location;
      }

      Marker marker = getMap().addMarker(
          new MarkerOptions().position(location).draggable(false)
              .icon(BitmapDescriptorFactory.defaultMarker(getHueForRow(i))));
      mMarkerIds.put(marker, i);
      if (mCurrentIndex == i) {
        Log.d(TAG, "[setMarkers] selecting marker: " + i);
        selectMarker(marker);
      }
    }

    if (firstLocation != null) {
      getMap().moveCamera(
          CameraUpdateFactory.newLatLngZoom(firstLocation, 12f));
      getMap().setOnMarkerClickListener(getOnMarkerClickListener());
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
      ColorGuide guide = mColorGroup.getColorGuide(activity.getColumnDefinitions(), table.getRowAtIndex(index));
      // Based on if the guide matched or not, grab the hue.
      if (guide != null) {
        float[] hsv = new float[3];
        Color.colorToHSV(guide.getBackground(), hsv);
        return hsv[0];
      }
    }

    return DEFAULT_MARKER_HUE;
  }

  private String getLatitudeElementKey() {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();
    
    ArrayList<ColumnDefinition> orderedDefns = activity.getColumnDefinitions();
    final List<ColumnDefinition> geoPointCols = 
        GeoColumnUtil.get().getGeopointColumnDefinitions(orderedDefns);
    // Grab the key value store helper from the table activity.
    final KeyValueStoreHelper kvsHelper =
        new KeyValueStoreHelper(activity, activity.getAppName(), 
            activity.getTableId(), LocalKeyValueStoreConstants.Map.PARTITION);
    String latitudeElementKey =
        kvsHelper.getString(LocalKeyValueStoreConstants.Map.KEY_MAP_LAT_COL);
    if (latitudeElementKey == null) {
      // Go through each of the columns and check to see if there are
      // any columns labeled latitude or longitude.
      for (ColumnDefinition cd : orderedDefns) {
        if (GeoColumnUtil.get().isLatitudeColumnDefinition(geoPointCols, cd)) {
          latitudeElementKey = cd.getElementKey();
          kvsHelper.setString(
              LocalKeyValueStoreConstants.Map.KEY_MAP_LAT_COL,
              latitudeElementKey);
          break;
        }
      }
    }
    return latitudeElementKey;
  }

  private String getLongitudeElementKey() {
    TableDisplayActivity activity = (TableDisplayActivity) getActivity();
    ArrayList<ColumnDefinition> orderedDefns = activity.getColumnDefinitions();

    final List<ColumnDefinition> geoPointCols = 
        GeoColumnUtil.get().getGeopointColumnDefinitions(orderedDefns);
    // Grab the key value store helper from the table activity.
    final KeyValueStoreHelper kvsHelper =
        new KeyValueStoreHelper(activity, activity.getAppName(), 
            activity.getTableId(), LocalKeyValueStoreConstants.Map.PARTITION);
    String longitudeElementKey =
        kvsHelper.getString(LocalKeyValueStoreConstants.Map.KEY_MAP_LONG_COL);
    if (longitudeElementKey == null) {
      // Go through each of the columns and check to see if there are
      // any columns labled longitude
      for (ColumnDefinition cd : orderedDefns) {
        if (GeoColumnUtil.get().isLongitudeColumnDefinition(geoPointCols, cd)) {
          longitudeElementKey = cd.getElementKey();
          kvsHelper.setString(
              LocalKeyValueStoreConstants.Map.KEY_MAP_LONG_COL,
              longitudeElementKey);
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
      return new LatLng(
          Double.parseDouble(split[0]),
          Double.parseDouble(split[1]));
    } catch (Exception e) {
      Log.e(
          TAG,
          "The following location is not in the proper lat,lng form: " +
              location);
    }
    return null;
  }

  /**
   * Parses the latitude and longitude strings and creates a LatLng.
   */
  private LatLng parseLocationFromString(
      ColumnDefinition latitudeColumn,
      String latitude,
      ColumnDefinition longitudeColumn,
      String longitude) {
    try {
      return new LatLng(
          Double.parseDouble(latitude),
          Double.parseDouble(longitude));
    } catch (Exception e) {
       Log.e(
           TAG,
           "The following location did not parse correctly: " +
           latitude +
           "," +
           longitude);
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

        ArrayList<ColumnDefinition> orderedDefns = activity.getColumnDefinitions();
        for (ColumnDefinition cd : orderedDefns) {
          elementNameToValue.put(cd.getElementName(), "");
        }
        final KeyValueStoreHelper kvsHelper =
            new KeyValueStoreHelper(activity, activity.getAppName(), 
                activity.getTableId(), LocalKeyValueStoreConstants.Map.PARTITION);
        String latitudeElementKey =
            kvsHelper.getString(
                LocalKeyValueStoreConstants.Map.KEY_MAP_LAT_COL);
        String longitudeElementKey =
            kvsHelper.getString(
                LocalKeyValueStoreConstants.Map.KEY_MAP_LONG_COL);
        {
          ColumnDefinition latitudeColumn =
              ColumnDefinition.find(orderedDefns, latitudeElementKey);
          elementNameToValue.put(
                latitudeElementKey,
                Double.toString(location.latitude));
        }

        {
          ColumnDefinition longitudeColumn =
              ColumnDefinition.find(orderedDefns, longitudeElementKey);
          elementNameToValue.put(
                longitudeElementKey,
                Double.toString(location.longitude));
        }
        // To store the mapping in a bundle, we need to put it in string list.
        ArrayList<String> bundleStrings = new ArrayList<String>();
        for (String key : elementNameToValue.keySet()) {
          bundleStrings.add(key);
          bundleStrings.add(elementNameToValue.get(key));
        }
        // Bundle it all up for the fragment.
        Bundle b = new Bundle();
        b.putStringArrayList(
            LocationDialogFragment.ELEMENT_NAME_TO_VALUE_KEY,
            bundleStrings);
        b.putString(LocationDialogFragment.LOCATION_KEY, location.toString());
        LocationDialogFragment dialog = new LocationDialogFragment();
        dialog.setArguments(b);
        dialog.show(getFragmentManager(), "LocationDialogFragment");
      }
    };
  }

  /**
   * When a marker is clicked, set the index of the list fragment, and then
   * show it. If that index is already selected, then hide it.
   */
  private OnMarkerClickListener getOnMarkerClickListener() {
    return new OnMarkerClickListener() {
      @Override
      public boolean onMarkerClick(Marker arg0) {
        int index = (mCurrentMarker != null) ?
            mMarkerIds.get(mCurrentMarker) :
            INVALID_INDEX;
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
   * Selects a marker, updating the marker list, and changing the marker's
   * color to green. Makes the marker the currently selected marker.
   *
   * @param marker
   *          The marker to be selected.
   */
  private void selectMarker(Marker marker) {
    if (mCurrentMarker == marker)
      return;
    marker.setIcon(
        BitmapDescriptorFactory.defaultMarker(DEFAULT_SELECTED_MARKER_HUE));
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
    mCurrentMarker.setIcon(
        BitmapDescriptorFactory.defaultMarker(getHueForRow(index)));
    mCurrentMarker = null;
    listener.setNoItemSelected();
  }
}
