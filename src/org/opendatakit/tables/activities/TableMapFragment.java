package org.opendatakit.tables.activities;

import java.util.HashMap;

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.view.custom.CustomTableView;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * A TableMapFragment displays map information about a specific table.
 * 
 * @author Chris Gelon (cgelon)
 */
public class TableMapFragment extends Fragment {
	
	public static final String KVS_PARTITION = "TableMapFragment";
	public static final String KEY_MAP_LABEL_COL = "keyMapLabelCol";
    public static final String KEY_MAP_LOC_COL = "keyMapLocCol";
    public static final String KEY_MAP_LAT_COL = "keyMapLatCol";
    public static final String KEY_MAP_LONG_COL = "keyMapLongCol";
    public static final String KEY_FILENAME = "keyFilename";
    
    public static final String KEY_TABLE = "keyTable";
    public static final String KEY_TABLE_PROPERTIES = "keyTableProperties";
	
	/** Table that represents all of the data in the query. */
	private UserTable mTable;
	/** The properties that pertain to the table. */
	private TableProperties mTableProperties;
	
	private ListFragment mList;
	private InnerTableMapFragment mMap;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Create the map fragment.
        mMap = new InnerTableMapFragment();
        mMap.setTable(mTable, mTableProperties);
        
        // Create the list fragment.
        mList = new ListFragment();
        mList.setTable(mTable, mTableProperties);
		
		// Add both the list and the map at the same time.
		getChildFragmentManager().beginTransaction()
				.add(R.id.list, mList).add(R.id.map, mMap).commit();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.map_fragment, container, false);
	}
	
	/**
	 * Sets the table and table properties to this fragment.
	 */
	public void setTable(UserTable table, TableProperties properties) {
		mTable = table;
		mTableProperties = properties;
	}
	
	/**
	 * A ListFragment displays data in a table in a list format. The format is 
	 * specified by a html file in the TablePropertiesManager Activity.
	 * This ListFragment is special: it only displays one row, which is the data 
	 * from the selected map marker.
	 * 
	 * @author Chris Gelon (cgelon)
	 */
	public static class ListFragment extends SherlockFragment {
		/** The key for the arguments bundle that holds which row is currently selected. */
		public static final String KEY_ROW_INDEX = "keyRowIndex";
		
		/** Table that represents all of the data in the query. */
		private UserTable mTable;
		/** The properties that pertain to the table. */
		private TableProperties mTableProperties;
		
		/** The container that holds this fragment's views. */
		private ViewGroup mContainer;
		
		/** The index in the UserTable of the currently selected row. */
		private int mIndex;
		
		/**
		 * Sets the table and table properties to this fragment.
		 */
		public void setTable(UserTable table, TableProperties properties) {
			mTable = table;
			mTableProperties = properties;
		}
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);	
			mIndex = 0;
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			mContainer = container;
			mContainer.setVisibility(View.GONE);
			return null;
		}
		
		@Override
		public void onResume() {
	        super.onResume();
	        resetView();
	    }
		
		private void resetView() {
			// Grab the key value store helper from the map fragment.
	    	//final KeyValueStoreHelper kvsHelper = mTableProperties.getKeyValueStoreHelper(TableMapFragment.KVS_PARTITION);
	    	// Find which file stores the html information for displaying the list.
			String filename = Environment.getExternalStorageDirectory().getPath() + "/odk/tables/facilities_list_chunked.html"; //kvsHelper.getString(TableMapFragment.KEY_FILENAME);
			// Create the custom view and set it.
	        CustomTableView view = CustomTableView.get(getActivity(), mTableProperties, mTable, filename, mIndex);
			view.display();
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
			        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
			mContainer.removeAllViews();
	        mContainer.addView(view, params);
	        if (view.getWebHeight() > 0) {
	        	LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, view.getWebHeight());
		        //mContainer.setLayoutParams(containerParams);
	        }
		}
		
		/** Sets the index of the list view, which will be the row of the data wanting to be displayed. */
		public void setIndex(int index) {
			mIndex = index;
			resetView();
		}
		
		/**
		 * @return The current index of the item being displayed.
		 */
		public int getIndex() {
			return mIndex;
		}
	}
	
	/**
	 * The InnerMapFragment has the capability of showing a map.
	 * It displays markers based off of location column set in the TableProperitiesManager Activity.
	 * 
	 * @author Chris Gelon (cgelon)
	 */
	public static class InnerTableMapFragment extends SherlockMapFragment {
		private static String TAG = "InnerMapFragment";
		
		/** Table that represents all of the data in the query. */
		private UserTable mTable;
		/** The properties that pertain to the table. */
		private TableProperties mTableProperties;
		
		/** A mapping of all markers to index to determine which marker is selected. */
		private HashMap<Marker, Integer> mMarkerIds;
		
		/** The currently selected marker. */
		private Marker mCurrentMarker;
		
		/**
		 * Sets the table and table properties to this fragment.
		 */
		public void setTable(UserTable table, TableProperties properties) {
			mTable = table;
			mTableProperties = properties;
		}
		
		@Override
		public void onStart() {
			super.onStart();
			setMarkers();
			getMap().setOnMapLongClickListener(getOnMapLongClickListener());
			getMap().setOnMapClickListener(getOnMapClickListener());
		}

		/**
		 * Sets the location markers based off of the columns set in the table properties.
		 */
	    private void setMarkers() {
	    	mMarkerIds = new HashMap<Marker, Integer>();
	    	
	    	// Grab the key value store helper from the table activity.
	    	final KeyValueStoreHelper kvsHelper = mTableProperties.getKeyValueStoreHelper(KVS_PARTITION);
	    	// Try to find the map columns in the store.
	    	ColumnProperties labelColumn = mTableProperties.getColumnByElementKey(kvsHelper.getString(KEY_MAP_LABEL_COL));
	    	ColumnProperties locationColumn = mTableProperties.getColumnByElementKey(kvsHelper.getString(KEY_MAP_LOC_COL));
	    	ColumnProperties latitudeColumn = mTableProperties.getColumnByElementKey(kvsHelper.getString(KEY_MAP_LAT_COL));
	    	ColumnProperties longitudeColumn = mTableProperties.getColumnByElementKey(kvsHelper.getString(KEY_MAP_LONG_COL));
	    	
	    	// Find the locations from entries in the table.
			int labelColumnIndex = mTableProperties.getColumnIndex(labelColumn.getElementKey());
			int locationColumnIndex = mTableProperties.getColumnIndex(locationColumn.getElementKey());
			int latitudeColumnIndex = mTableProperties.getColumnIndex(latitudeColumn.getElementKey());
			int longitudeColumnIndex = mTableProperties.getColumnIndex(longitudeColumn.getElementKey());
			LatLng firstLocation = null;
			for (int i = 0; i < mTable.getHeight(); i++) {
				String labelString = mTable.getData(i, labelColumnIndex);
				String locationString = mTable.getData(i, locationColumnIndex);
				String latitudeString = mTable.getData(i, latitudeColumnIndex);
				String longitudeString = mTable.getData(i, longitudeColumnIndex);
				if (latitudeString == null || longitudeString == null || latitudeString.length() == 0 || longitudeString.length() == 0) continue;
				//if (locationString == null || locationString.length() == 0) continue;
				
				// Add the location as an overlay.
				LatLng location = parseLocationFromString(latitudeString + "," + longitudeString);
				if (location == null) continue;
				if (firstLocation == null) firstLocation = location;
				Marker marker = getMap().addMarker(new MarkerOptions()
						.position(location)
						.draggable(false)
						.title(labelString));
				mMarkerIds.put(marker, i);
	        }
			if (firstLocation != null) {
				getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 12f));
				getMap().setOnMarkerClickListener(getOnMarkerClickListener());
			}
	    }
	    
	    /**
	     * Parses the location string and creates a LatLng.
	     * The format of the string should be:
	     * 	lat,lng
	     */
	    private LatLng parseLocationFromString(String location) {
	    	String[] split = location.split(",");
	    	try {
	    		return new LatLng(Double.parseDouble(split[0]), Double.parseDouble(split[1]));
	    	} catch (Exception e) {
	    		Log.e(TAG, "The following location is not in the proper lat,lng form: " + location);
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
					ListFragment list = getListFragment();
					if (list.mContainer.getVisibility() == View.VISIBLE) {
						list.mContainer.setVisibility(View.GONE);
						int index = mMarkerIds.get(mCurrentMarker);
						Marker newMarker = getMap().addMarker(new MarkerOptions()
							.position(mCurrentMarker.getPosition())
							.draggable(false));
						mCurrentMarker.remove();
						mMarkerIds.remove(mCurrentMarker);
						mMarkerIds.put(newMarker, index);
						mCurrentMarker = null;
					}
				}
			};
		}
	    
	    private OnMapLongClickListener getOnMapLongClickListener() {
			return new OnMapLongClickListener() {
				@Override
				public void onMapLongClick(LatLng location) {
					getMap().addMarker(new MarkerOptions()
							.position(location)
							.draggable(false)
							.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
				}
			};
		}
	    
	    /**
	     * When a marker is clicked, set the index of the list fragment, and then show it.
	     * If that index is already selected, then hide it.
	     */
	    private OnMarkerClickListener getOnMarkerClickListener() {
			return new OnMarkerClickListener()
			{
				@Override
				public boolean onMarkerClick(Marker arg0) {
					ListFragment list = getListFragment();
					int currentIndex = list.getIndex();
					
					// Make the marker visible if it is either invisible or a new marker.
					// Make the marker invisible if clicking on the already selected marker.
					if (currentIndex != mMarkerIds.get(arg0) || list.mContainer.getVisibility() == View.GONE) {
						if (currentIndex != mMarkerIds.get(arg0) && mCurrentMarker != null) {
							int index = mMarkerIds.get(mCurrentMarker);
							Marker newMarker = getMap().addMarker(new MarkerOptions()
								.position(mCurrentMarker.getPosition())
								.draggable(false));
							mCurrentMarker.remove();
							mMarkerIds.remove(mCurrentMarker);
							mMarkerIds.put(newMarker, index);
						}
						int newIndex = mMarkerIds.get(arg0);
						list.setIndex(newIndex);
						list.mContainer.setVisibility(View.VISIBLE);
						Marker newMarker = getMap().addMarker(new MarkerOptions()
							.position(arg0.getPosition())
							.draggable(false)
							.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
						arg0.remove();
						mMarkerIds.remove(arg0);
						mMarkerIds.put(newMarker, newIndex);
						mCurrentMarker = newMarker;
					} else {
						list.mContainer.setVisibility(View.GONE);
						int index = mMarkerIds.get(arg0);
						Marker newMarker = getMap().addMarker(new MarkerOptions()
							.position(arg0.getPosition())
							.draggable(false));
						arg0.remove();
						mMarkerIds.remove(arg0);
						mMarkerIds.put(newMarker, index);
						mCurrentMarker = null;
					}
					
					
					return true;
				}
			};
		}
	    
	    public ListFragment getListFragment() {
	    	return ((TableMapFragment)getParentFragment()).mList;
	    }
	}
}