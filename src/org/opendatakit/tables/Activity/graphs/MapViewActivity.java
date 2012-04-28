package org.opendatakit.tables.Activity.graphs;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendatakit.tables.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class MapViewActivity extends MapActivity {
	
	public static final int GRAPH_SETTING = 0;
	
	MapView mapView; 
    MapController mc;
    GeoPoint p;
 
    class MapOverlay extends com.google.android.maps.ItemizedOverlay<OverlayItem> {
    	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
    	private Context mContext;
    	
    	public MapOverlay(Drawable defaultMarker, Context context) {
    		  super(boundCenterBottom(defaultMarker));
    		  mContext = context;
    	}
    	
    	public MapOverlay(Drawable defaultMarker) {
    		  super(boundCenterBottom(defaultMarker));
    	}
    	
    	public void addOverlay(OverlayItem overlay) {
    	    mOverlays.add(overlay);
    	    populate();
    	}
    	
    	@Override
    	protected OverlayItem createItem(int i) {
    	  return mOverlays.get(i);
    	}
    	
    	@Override
    	public int size() {
    	  return mOverlays.size();
    	}
    	
    	@Override
    	protected boolean onTap(int index) {
    	  OverlayItem item = mOverlays.get(index);
    	  AlertDialog dialog = new AlertDialog.Builder(mContext).create();
    	  dialog.setTitle(item.getTitle());
    	  dialog.setMessage(item.getSnippet());
    	  dialog.show();
    	  return true;
    	}
    }
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
    	
        // Map View
        mapView = (MapView) findViewById(R.id.mapView);
        
        // Zoom Controller
        mapView.setBuiltInZoomControls(true);
        
        List<Overlay> mapOverlays = null;
        MapOverlay itemizedoverlay = null;
        try {
        // Push-pin configuration 
    	mapOverlays = mapView.getOverlays();
    	Drawable drawable = this.getResources().getDrawable(R.drawable.pushpin);
    	itemizedoverlay = new MapOverlay(drawable, this);
        } catch (Exception e) {
        	Log.e("point1", e.toString());
        }
        /**
        GeoPoint p = getGeoPoint("Seattle");
        Log.d("MVA", "p:" + p.toString());
    	//First point
    	GeoPoint point = new GeoPoint(19240000,-99120000);
    	OverlayItem overlayitem = new OverlayItem(point, "Hola, Mundo!", "I'm in Mexico City!");
    	
    	// Second point 
    	GeoPoint point2 = new GeoPoint(35410000, 139460000);
    	OverlayItem overlayitem2 = new OverlayItem(point2, "Koniziwa", "I'm in Japan!");
        
    	// Add points on the map
    	itemizedoverlay.addOverlay(overlayitem);
    	itemizedoverlay.addOverlay(overlayitem2);
    	if (p != null) {
    		OverlayItem overlayitem3 = new OverlayItem(p, "Hello", "Smell the coffee");
    		itemizedoverlay.addOverlay(overlayitem3);
    	}
    	**/
    	ArrayList<String> locs = getIntent().getStringArrayListExtra("location");
    	for(String loc : locs) {
    	    Log.d("MVA", "MapViewActivity:loc:" + loc);
    	    GeoPoint locPoint = getGeoPoint(loc);
    	    Log.d("MVA", "MapViewActivity:locPoint:" + locPoint.toString());
    	    OverlayItem oItem = new OverlayItem(locPoint, "", "");
    	    itemizedoverlay.addOverlay(oItem);
    	}
    	
    	mapOverlays.add(itemizedoverlay);
    	
    	// Define the focus of the map
    	mc = mapView.getController();
    	//mc.animateTo(point);
    }
    
    public GeoPoint getGeoPoint(String location) {
    	/*Geocoder geoCoder = new Geocoder(this, Locale.getDefault());    
        GeoPoint point = null;
        try {
            List<Address> addresses = geoCoder.getFromLocationName(location, 1);
            if (addresses.size() > 0) {
                point = new GeoPoint(
                        (int) (addresses.get(0).getLatitude() * 1E6), 
                        (int) (addresses.get(0).getLongitude() * 1E6));
            }    
        } catch (IOException e) {}
        return point;*/
        // using solution from comment #21 at
        // https://code.google.com/p/android/issues/detail?id=8816
        JSONObject jo = getLocationInfo(location);
        return getGeoPoint(jo);
    }
    
    @Override
    protected boolean isRouteDisplayed() {
        // TODO Auto-generated method stub
        return false;
    }	
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, GRAPH_SETTING, 0, "Settings");
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        
    	switch(item.getItemId()) {
        	case GRAPH_SETTING:
        		Intent i = new Intent(this, GraphSetting.class);
        		startActivity(i);
        		return true;
    	}
    	
    	return super.onMenuItemSelected(featureId, item);
    }
    
    public JSONObject getLocationInfo(String address) {
        address = address.replaceAll(" ", "%20");
        HttpGet httpGet = new HttpGet("http://maps.google."
                + "com/maps/api/geocode/json?address=" + address
                + "&sensor=false");
        HttpClient client = new DefaultHttpClient();
        HttpResponse response;
        StringBuilder stringBuilder = new StringBuilder();

        try {
            response = client.execute(httpGet);
            HttpEntity entity = response.getEntity();
            InputStream stream = entity.getContent();
            int b;
            while ((b = stream.read()) != -1) {
                stringBuilder.append((char) b);
            }
        } catch (ClientProtocolException e) {
        } catch (IOException e) {
        }

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject = new JSONObject(stringBuilder.toString());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Log.d("MVA", "MapViewActivity:jsonObject:" + jsonObject.toString());
        return jsonObject;
    }
    
    public GeoPoint getGeoPoint(JSONObject jsonObject) {

        Double lon = new Double(0);
        Double lat = new Double(0);

        try {

            lon = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
                .getJSONObject("geometry").getJSONObject("location")
                .getDouble("lng");

            lat = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
                .getJSONObject("geometry").getJSONObject("location")
                .getDouble("lat");

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return new GeoPoint((int) (lat * 1E6), (int) (lon * 1E6));

    }
	
}
