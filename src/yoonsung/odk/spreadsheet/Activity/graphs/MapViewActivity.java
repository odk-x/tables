package yoonsung.odk.spreadsheet.Activity.graphs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import yoonsung.odk.spreadsheet.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
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
        
        // Push-pin configuration 
    	List<Overlay> mapOverlays = mapView.getOverlays();
    	Drawable drawable = this.getResources().getDrawable(R.drawable.pushpin);
    	MapOverlay itemizedoverlay = new MapOverlay(drawable, this);
    	
      
        GeoPoint p = getGeoPoint("Seattle");
  
    	// First point
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
    	mapOverlays.add(itemizedoverlay);
    	
    	// Define the focus of the map
    	mc = mapView.getController();
    	mc.animateTo(point);
    }
    
    public GeoPoint getGeoPoint(String location) {
    	Geocoder geoCoder = new Geocoder(this, Locale.getDefault());    
        GeoPoint point = null;
        try {
            List<Address> addresses = geoCoder.getFromLocationName(location, 1);
            if (addresses.size() > 0) {
                point = new GeoPoint(
                        (int) (addresses.get(0).getLatitude() * 1E6), 
                        (int) (addresses.get(0).getLongitude() * 1E6));
            }    
        } catch (IOException e) {}
        return point;
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
	
}
