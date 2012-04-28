package org.opendatakit.tables.activities;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DataUtil;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.TableViewSettings.ConditionalRuler;
import org.opendatakit.tables.data.UserTable;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;


public class MapDisplayActivity extends MapActivity
        implements DisplayActivity {

    private static final int RCODE_ODKCOLLECT_ADD_ROW =
        Controller.FIRST_FREE_RCODE;
    
    private static final String MAPS_API_KEY =
        "0xikiqqRicaG8hTFp_Lq5_SY7mCwcguCiKtLGlQ";
    
    private static final int MINIMUM_CLICK_DISTANCE = 20;
    
    private DataUtil du;
    private DataManager dm;
    private Controller c;
    private Query query;
    private UserTable table;
    private int[][] locations;
    private RelativeLayout mapWrapper;
    private MapView mv;
    private ItemizedOverlayImpl<OverlayItem> itemizedOverlay;
    private int labelColIndex;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        du = DataUtil.getDefaultDataUtil();
        c = new Controller(this, this, getIntent().getExtras());
        dm = new DataManager(DbHelper.getDbHelper(this));
        query = new Query(dm.getAllTableProperties(), c.getTableProperties());
        labelColIndex = c.getTableProperties().getColumnIndex(
                c.getTableViewSettings().getMapLabelCol().getColumnDbName());
        mapWrapper = new RelativeLayout(this);
        mv = new MapView(this, MAPS_API_KEY);
        mv.setClickable(true);
        mv.setBuiltInZoomControls(true);
        mapWrapper.addView(mv);
        c.setDisplayView(mapWrapper);
        setContentView(c.getWrapperView());
        init();
    }
    
    @Override
    public void init() {
        query.clear();
        query.loadFromUserQuery(c.getSearchText());
        table = c.getIsOverview() ?
                c.getDbTable().getUserOverviewTable(query) :
                c.getDbTable().getUserTable(query);
        locations = new int[table.getHeight()][];
        ColumnProperties locCol = c.getTableViewSettings().getMapLocationCol();
        int locColIndex = c.getTableProperties().getColumnIndex(
                locCol.getColumnDbName());
        itemizedOverlay = new ItemizedOverlayImpl<OverlayItem>();
        for (int i = 0; i < table.getHeight(); i++) {
            String locString = table.getData(i, locColIndex);
            if (locString == null) {
                continue;
            }
            locations[i] = du.parseLocationFromDb(locString);
            GeoPoint gp = new GeoPoint(locations[i][0], locations[i][1]);
            itemizedOverlay.addPoint(gp, getDrawable(i));
        }
        mv.getOverlays().clear();
        mv.getOverlays().add(itemizedOverlay);
        mv.postInvalidate();
    }
    
    private int getDrawable(int rowNum) {
        ColumnProperties[] cps = c.getTableProperties().getColumns();
        for (int i = 0; i < cps.length; i++) {
            ConditionalRuler cr =
                c.getTableViewSettings().getMapColorRuler(cps[i]);
            int color = cr.getSetting(table.getData(rowNum, i), -1);
            if (color != -1) {
                switch (color) {
                case Color.BLACK:
                    return R.drawable.map_marker_small_black;
                case Color.BLUE:
                    return R.drawable.map_marker_small_blue;
                case Color.GREEN:
                    return R.drawable.map_marker_small_green;
                case Color.RED:
                    return R.drawable.map_marker_small_red;
                case Color.YELLOW:
                    return R.drawable.map_marker_small_yellow;
                }
            }
        }
        return R.drawable.map_marker_small_black;
    }
    
    @Override
    public void onBackPressed() {
        c.onBackPressed();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        if (c.handleActivityReturn(requestCode, resultCode, data)) {
            return;
        }
        switch (requestCode) {
        case RCODE_ODKCOLLECT_ADD_ROW:
            c.addRowFromOdkCollectForm(
                    Integer.valueOf(data.getData().getLastPathSegment()));
            init();
            break;
        default:
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        c.buildOptionsMenu(menu);
        return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        return c.handleMenuItemSelection(item.getItemId());
    }
    
    @Override
    public void onSearch() {
        c.recordSearch();
        init();
    }
    
    @Override
    public void onAddRow() {
        Intent intent = c.getIntentForOdkCollectAddRow();
        if (intent != null) {
            startActivityForResult(intent, RCODE_ODKCOLLECT_ADD_ROW);
        }
    }
    
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
    
    private void onItemClicked(int index) {
        GeoPoint gp = new GeoPoint(locations[index][0], locations[index][1]);
        itemizedOverlay.addInfoWindow(gp, table.getData(index, labelColIndex));
        mv.postInvalidate();
    }
    
    private class ItemizedOverlayImpl<Item extends OverlayItem>
            extends ItemizedOverlay<OverlayItem> {
        
        private final List<OverlayItem> mOverlays;
        private GeoPoint infoGeoPoint;
        private InfoWindow infoWindow;
        private OverlayItem infoWindowOverlay;
        
        public ItemizedOverlayImpl() {
            super(boundCenter(getResources().getDrawable(
                    R.drawable.map_marker_small_black)));
            mOverlays = new ArrayList<OverlayItem>();
        }
        
        public void addPoint(GeoPoint gp, int drawableId) {
            OverlayItem item = new OverlayItem(gp, "", "");
            item.setMarker(boundCenter(
                    getResources().getDrawable(drawableId)));
            addOverlay(item);
        }
        
        /**
         * This must be called after the addition of all point markers.
         */
        public void addInfoWindow(GeoPoint gp, String text) {
            removeInfoWindow();
            infoGeoPoint = gp;
            infoWindow = new InfoWindow(text);
            infoWindowOverlay = new OverlayItem(gp, "", "");
            infoWindowOverlay.setMarker(infoWindow);
            doPopulate();
        }
        
        public void removeInfoWindow() {
            if (infoWindowOverlay != null) {
                infoGeoPoint = null;
                infoWindow = null;
                infoWindowOverlay = null;
                doPopulate();
            }
        }
        
        private void addOverlay(OverlayItem overlay) {
            mOverlays.add(overlay);
            doPopulate();
        }
        
        @Override
        protected OverlayItem createItem(int i) {
            if (i == mOverlays.size()) {
                return infoWindowOverlay;
            } else {
                return mOverlays.get(i);
            }
        }
        
        @Override
        public int size() {
            return mOverlays.size() + (infoWindowOverlay == null ? 0 : 1);
        }
        
        private void doPopulate() {
            populate();
            setLastFocusedIndex(-1);
        }
        
        @Override
        public boolean onTouchEvent(MotionEvent event, MapView mapView) {
            if (event.getAction() != MotionEvent.ACTION_UP) {
                return false;
            }
            int x = (new Float(event.getX())).intValue();
            int y = (new Float(event.getY())).intValue();
            if (infoWindow != null) {
                int[] rp = getIwRelativePoint(x, y);
                if (isOnIw(rp)) {
                    if (isOnIwCloseButton(rp)) {
                        removeInfoWindow();
                    }
                    return true;
                }
            }
            double minDist = MINIMUM_CLICK_DISTANCE;
            int locIndex = -1;
            Point pt = new Point();
            Projection projection = mv.getProjection();
            for (int i = 0; i < locations.length; i++) {
                int[] loc = locations[i];
                if (loc == null) {
                    continue;
                }
                Point pixels = projection.toPixels(
                        new GeoPoint(loc[0], loc[1]), pt);
                double dist = Math.sqrt(Math.pow(pixels.x - x, 2) +
                        Math.pow(pixels.y - y, 2));
                if (dist < minDist) {
                    locIndex = i;
                }
            }
            if (locIndex != -1) {
                onItemClicked(locIndex);
                return true;
            }
            return false;
        }
        
        private boolean isOnIw(int[] rp) {
            return infoWindow.isOnWindow(rp[0], rp[1]);
        }
        
        private boolean isOnIwCloseButton(int[] rp) {
            return infoWindow.isOnCloseButton(rp[0], rp[1]);
        }
        
        private int[] getIwRelativePoint(int x, int y) {
            Point pt = new Point();
            Point pixels = mv.getProjection().toPixels(infoGeoPoint, pt);
            return new int[] {x - pixels.x, y - pixels.y};
        }
    }
    
    private class InfoWindow extends Drawable {
        
        private static final int CLOSE_BUTTON_SIZE = 15;
        private static final int CLOSE_BUTTON_PADDING = 5;
        
        private final String text;
        private final Paint textPaint;
        private final Paint bgPaint;
        private final Paint closeButtonPaint;
        private final Rect bounds;
        private final Rect textBounds;
        private final Rect closeButtonBounds;
        
        public InfoWindow(String text) {
            this.text = text;
            textPaint = new Paint();
            bgPaint = new Paint();
            bgPaint.setColor(Color.WHITE);
            closeButtonPaint = new Paint();
            closeButtonPaint.setStrokeWidth(3);
            textBounds = new Rect();
            textPaint.getTextBounds(text, 0, text.length(), textBounds);
            closeButtonBounds = new Rect(
                    textBounds.right + CLOSE_BUTTON_PADDING,
                    textBounds.top - CLOSE_BUTTON_SIZE - CLOSE_BUTTON_PADDING,
                    textBounds.right + CLOSE_BUTTON_SIZE +
                            CLOSE_BUTTON_PADDING,
                    textBounds.top - CLOSE_BUTTON_PADDING);
            bounds = new Rect(textBounds.left,
                    textBounds.top - CLOSE_BUTTON_SIZE -
                            (2 * CLOSE_BUTTON_PADDING),
                    textBounds.right + CLOSE_BUTTON_SIZE +
                            (2 * CLOSE_BUTTON_PADDING),
                    textBounds.bottom);
        }
        
        @Override
        public void draw(Canvas canvas) {
            canvas.drawRect(0, 0, textBounds.right + CLOSE_BUTTON_SIZE +
                    (2 * CLOSE_BUTTON_PADDING),
                    textBounds.top - CLOSE_BUTTON_SIZE -
                    (2 * CLOSE_BUTTON_PADDING), bgPaint);
            canvas.drawLine(closeButtonBounds.left, closeButtonBounds.top,
                    closeButtonBounds.right, closeButtonBounds.bottom,
                    closeButtonPaint);
            canvas.drawLine(closeButtonBounds.right, closeButtonBounds.top,
                    closeButtonBounds.left, closeButtonBounds.bottom,
                    closeButtonPaint);
            canvas.drawText(text, 0, textBounds.bottom, textPaint);
        }
        
        public boolean isOnWindow(int x, int y) {
            return ((bounds.left <= x) && (x <= bounds.right) &&
                    (bounds.top <= y) && (y <= bounds.bottom));
        }
        
        public boolean isOnCloseButton(int x, int y) {
            return ((closeButtonBounds.left <= x) &&
                    (x <= closeButtonBounds.right) &&
                    (closeButtonBounds.top <= y) &&
                    (y <= closeButtonBounds.bottom));
        }
        
        @Override
        public int getMinimumHeight() {
            return textBounds.bottom - textBounds.top;
        }
        
        @Override
        public int getMinimumWidth() {
            return textBounds.right - textBounds.left;
        }
        
        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
        
        @Override
        public void setAlpha(int alpha) {}
        
        @Override
        public void setColorFilter(ColorFilter cf) {}
    }
}
