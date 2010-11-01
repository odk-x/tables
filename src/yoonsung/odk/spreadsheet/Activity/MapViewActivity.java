package yoonsung.odk.spreadsheet.Activity;

import yoonsung.odk.spreadsheet.R;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MapView.LayoutParams;

public class MapViewActivity extends MapActivity {
	
	MapView mapView; 
    MapController mc;
    GeoPoint p;
 
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);
 
        //mapView = (MapView) findViewById(R.id.mapView);
        //mapView.setBuiltInZoomControls(true);
        
        
    }
 
    @Override
    protected boolean isRouteDisplayed() {
        // TODO Auto-generated method stub
        return false;
    }	
	
}
