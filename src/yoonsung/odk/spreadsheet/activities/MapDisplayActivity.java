package yoonsung.odk.spreadsheet.activities;

import java.util.ArrayList;
import java.util.List;
import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.data.ColumnProperties;
import yoonsung.odk.spreadsheet.data.DataManager;
import yoonsung.odk.spreadsheet.data.DataUtil;
import yoonsung.odk.spreadsheet.data.DbHelper;
import yoonsung.odk.spreadsheet.data.Query;
import yoonsung.odk.spreadsheet.data.TableViewSettings.ConditionalRuler;
import yoonsung.odk.spreadsheet.data.UserTable;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;


public class MapDisplayActivity extends MapActivity
        implements DisplayActivity {
    
    private static final int RCODE_ODKCOLLECT_ADD_ROW = 0;
    
    private static final String MAPS_API_KEY =
        "0xikiqqRicaG8hTFp_Lq5_SY7mCwcguCiKtLGlQ";
    
    private DataUtil du;
    private DataManager dm;
    private Controller c;
    private Query query;
    private UserTable table;
    private MapView mv;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        du = DataUtil.getDefaultDataUtil();
        c = new Controller(this, this, getIntent().getExtras());
        dm = new DataManager(DbHelper.getDbHelper(this));
        query = new Query(dm.getAllTableProperties(), c.getTableProperties());
        mv = new MapView(this, MAPS_API_KEY);
        mv.setClickable(true);
        mv.setBuiltInZoomControls(true);
        c.setDisplayView(mv);
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
        ColumnProperties locCol = c.getTableViewSettings().getMapLocationCol();
        int locColIndex = c.getTableProperties().getColumnIndex(
                locCol.getColumnDbName());
        ItemizedOverlayImpl<OverlayItem> itemizedOverlay =
            new ItemizedOverlayImpl<OverlayItem>();
        for (int i = 0; i < table.getHeight(); i++) {
            String locString = table.getData(i, locColIndex);
            if (locString == null) {
                continue;
            }
            int[] loc = du.parseLocationFromDb(locString);
            GeoPoint gp = new GeoPoint(loc[0], loc[1]);
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
    
    private class ItemizedOverlayImpl<Item extends OverlayItem>
            extends ItemizedOverlay<OverlayItem> {
        
        private final List<OverlayItem> mOverlays;
        
        public ItemizedOverlayImpl() {
            super(boundCenterBottom(getResources().getDrawable(
                    R.drawable.map_marker_small_black)));
            mOverlays = new ArrayList<OverlayItem>();
        }
        
        public void addPoint(GeoPoint gp, int drawableId) {
            OverlayItem item = new OverlayItem(gp, "", "");
            item.setMarker(boundCenterBottom(
                    getResources().getDrawable(drawableId)));
            addOverlay(item);
        }
        
        private void addOverlay(OverlayItem overlay) {
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
    }
}
