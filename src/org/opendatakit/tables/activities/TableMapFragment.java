package org.opendatakit.tables.activities;

import java.util.HashMap;

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.view.custom.CustomTableView;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

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
			mIndex = -1;
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
			if (mIndex != -1) {
				// Grab the key value store helper from the map fragment.
		    	final KeyValueStoreHelper kvsHelper = mTableProperties.getKeyValueStoreHelper(TableMapFragment.KVS_PARTITION);
		    	// Find which file stores the html information for displaying the list.
				String filename = kvsHelper.getString(TableMapFragment.KEY_FILENAME);
				if (filename == null) {
					Toast.makeText(getActivity(), "List view file is not set!", Toast.LENGTH_LONG).show();
		    		return;
				}
				// Create the custom view and set it.
				CustomTableView view = CustomTableView.get(getActivity(), mTableProperties, mTable, filename, mIndex);
				view.display();
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
				mContainer.removeAllViews();
		        mContainer.addView(view, params);

		        WebViewClient client = new WebViewClient() {
	        	   public void onPageFinished(WebView view, String url) {
	        		   LinearLayout.LayoutParams containerParams =
	        				   new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, view.getMeasuredHeight());
	   		           if (containerParams.height > 0) {
	   		        	   //mContainer.setLayoutParams(containerParams);
	   		           }
	   		           mContainer.setVisibility(View.VISIBLE);
	        	    }
		        };
		        view.setOnFinishedLoaded(client);
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

		public boolean isListVisible() {
			return mContainer.getVisibility() == View.VISIBLE;
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
			//getMap().setOnMapLongClickListener(getOnMapLongClickListener());
			getMap().setOnMapClickListener(getOnMapClickListener());
		}

		/**
		 * Sets the location markers based off of the columns set in the table properties.
		 */
	    private void setMarkers() {
	    	if (mMarkerIds != null) {
	    		return;
	    	}
	    	mMarkerIds = new HashMap<Marker, Integer>();

	    	// Grab the key value store helper from the table activity.
	    	final KeyValueStoreHelper kvsHelper = mTableProperties.getKeyValueStoreHelper(KVS_PARTITION);
	    	String latitudeElementKey = kvsHelper.getString(KEY_MAP_LAT_COL);
	    	String longitudeElementKey = kvsHelper.getString(KEY_MAP_LONG_COL);
	    	if (latitudeElementKey == null || longitudeElementKey == null) {
	    		// Go through each of the columns and check to see if there are any columns labeled 
	            // latitude or longitude.
	            ColumnProperties[] cps = mTableProperties.getColumns();
	            if (latitudeElementKey == null) {
	            	for (int i = 0; i < cps.length; i++) {
		                if (cps[i].getDisplayName().equalsIgnoreCase("latitude")) {
		                	latitudeElementKey = cps[i].getElementKey();
		                	kvsHelper.setString(KEY_MAP_LAT_COL, latitudeElementKey);
		                	break;
		                }
		            }
	            }
	            if (longitudeElementKey == null) {
		            for (int i = 0; i < cps.length; i++) {
		                if (cps[i].getDisplayName().equalsIgnoreCase("longitude")) {
		                	longitudeElementKey = cps[i].getElementKey();
		                	kvsHelper.setString(KEY_MAP_LONG_COL, longitudeElementKey);
		                	break;
		                }
		            }
	            }
	    	}
	    	if (latitudeElementKey == null || longitudeElementKey == null) {
	    		Toast.makeText(getActivity(), "Latitude or longitude columns are not set!", Toast.LENGTH_LONG).show();
	    		return;
	    	}
	    	
	    	// Try to find the map columns in the store.
	    	ColumnProperties latitudeColumn = mTableProperties.getColumnByElementKey(latitudeElementKey);
	    	ColumnProperties longitudeColumn = mTableProperties.getColumnByElementKey(longitudeElementKey);

	    	// Find the locations from entries in the table.
			int latitudeColumnIndex = mTableProperties.getColumnIndex(latitudeColumn.getElementKey());
			int longitudeColumnIndex = mTableProperties.getColumnIndex(longitudeColumn.getElementKey());
			LatLng firstLocation = null;
			for (int i = 0; i < mTable.getHeight(); i++) {
				String latitudeString = mTable.getData(i, latitudeColumnIndex);
				String longitudeString = mTable.getData(i, longitudeColumnIndex);
				if (latitudeString == null || longitudeString == null || latitudeString.length() == 0 || longitudeString.length() == 0) continue;

				// Add the location as an overlay.
				LatLng location = parseLocationFromString(latitudeString + "," + longitudeString);
				if (location == null) continue;
				if (firstLocation == null) firstLocation = location;
				Marker marker = getMap().addMarker(new MarkerOptions()
						.position(location)
						.draggable(false));
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
					if (getListFragment().isListVisible()) {
						deselectCurrentMarker();
						getListFragment().mContainer.setVisibility(View.GONE);
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
					int currentIndex = getListFragment().getIndex();

					// Make the marker visible if it is either invisible or a new marker.
					// Make the marker invisible if clicking on the already selected marker.
					if (currentIndex != mMarkerIds.get(arg0) || !getListFragment().isListVisible()) {
						if (currentIndex != mMarkerIds.get(arg0)) {
							deselectCurrentMarker();
						}
						int newIndex = mMarkerIds.get(arg0);
						getListFragment().setIndex(newIndex);
						selectMarker(arg0);
					} else {
						deselectCurrentMarker();
						getListFragment().mContainer.setVisibility(View.GONE);
					}

					return true;
				}
			};
		}

	    /**
	     * Selects a marker, updating the marker list, and changing the marker's color to green.
	     * Makes the marker the currently selected marker.
	     * @param marker The marker to be selected.
	     */
	    private void selectMarker(Marker marker) {
	    	int index = mMarkerIds.get(marker);
	    	Marker newMarker = getMap().addMarker(new MarkerOptions()
				.position(marker.getPosition())
				.draggable(false)
				.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
	    	marker.remove();
	    	mMarkerIds.remove(marker);
	    	mMarkerIds.put(newMarker, index);
	    	mCurrentMarker = newMarker;
	    }

	    /**
	     * Deselects the currently selected marker, updating the marker list, and changing the marker
	     * back to a default color.
	     */
	    private void deselectCurrentMarker() {
	    	if (mCurrentMarker == null) return;

	    	int index = mMarkerIds.get(mCurrentMarker);
			Marker newMarker = getMap().addMarker(new MarkerOptions()
				.position(mCurrentMarker.getPosition())
				.draggable(false));
			mCurrentMarker.remove();
			mMarkerIds.remove(mCurrentMarker);
			mMarkerIds.put(newMarker, index);
			mCurrentMarker = null;
			getListFragment().setIndex(-1);
	    }

	    public ListFragment getListFragment() {
	    	return ((TableMapFragment)getParentFragment()).mList;
	    }
	}
}