package yoonsung.odk.spreadsheet.activities;

import java.util.HashMap;
import java.util.Map;
import yoonsung.odk.spreadsheet.view.CustomDetailView;
import android.app.Activity;
import android.os.Bundle;


public class DetailDisplayActivity extends Activity
        implements DisplayActivity {
    
    public static final String INTENT_KEY_ROW_ID = "rowId";
    public static final String INTENT_KEY_ROW_KEYS = "rowKeys";
    public static final String INTENT_KEY_ROW_VALUES = "rowValues";
    
    private String rowId;
    private Controller c;
    private String[] keys;
    private String[] values;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rowId = getIntent().getStringExtra(INTENT_KEY_ROW_ID);
        c = new Controller(this, this, getIntent().getExtras());
        keys = getIntent().getStringArrayExtra(INTENT_KEY_ROW_KEYS);
        values = getIntent().getStringArrayExtra(INTENT_KEY_ROW_VALUES);
        init();
    }
    
    @Override
    public void init() {
        Map<String, String> data = new HashMap<String, String>();
        for (int i = 0; i < keys.length; i++) {
            data.put(keys[i], values[i]);
        }
        CustomDetailView cdv = new CustomDetailView(this,
                c.getTableProperties());
        cdv.display(rowId, data);
        c.setDisplayView(cdv);
        setContentView(c.getWrapperView());
    }
    
    @Override
    public void onSearch() {
        Controller.launchTableActivity(this, c.getTableProperties(),
                c.getSearchText(), c.getIsOverview());
    }
    
    @Override
    public void onAddRow() {
        // TODO Auto-generated method stub
        
    }
}
