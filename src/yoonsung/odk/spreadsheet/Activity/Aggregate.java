package yoonsung.odk.spreadsheet.Activity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.opendatakit.aggregate.odktables.client.entity.Modification;
import org.opendatakit.aggregate.odktables.client.entity.SynchronizedRow;
import org.opendatakit.aggregate.odktables.client.entity.TableEntry;
import org.opendatakit.tasks.TasksSync;
import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.Database.DBIO;
import yoonsung.odk.spreadsheet.Database.DataTable;
import yoonsung.odk.spreadsheet.Database.DataUtils;
import yoonsung.odk.spreadsheet.Database.TableList;
import android.app.Activity;
import android.content.ContentValues;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * An activity for downloading from and uploading to an ODK Aggregate instance.
 * 
 * @author hkworden@gmail.com
 */
public class Aggregate extends Activity {
    
    /* the states the screen can be in at different points in the process */
    private static enum ScreenState { REQUESTING_USER_INFO,
                                      REQUESTING_TABLE_SELECTION,
                                      REQUESTING_ACTION_SELECTION }
    
    private TextView uriLabel;
    private EditText uriField;
    private TextView usernameLabel;
    private EditText usernameField;
    private Button listTablesButton;
    private Spinner tableListSpinner;
    private Button downloadTableButton;
    private Button pullTableButton;
    private Button pushTableButton;
    
    private ScreenState screenState;
    
    private TasksSync ts;
    private DBIO db;
    private TableList tl;
    
    private List<TableEntry> tableEntryList;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("ODK Tables > Aggregate");
        screenState = ScreenState.REQUESTING_USER_INFO;
        setContentView(R.layout.aggregate_activity);
        findViewComponents();
        setViewForScreenState();
        initHandlers();
    }
    
    private void findViewComponents() {
        uriLabel = (TextView) findViewById(R.id.aggregate_activity_uri_label);
        uriField = (EditText) findViewById(R.id.aggregate_activity_uri_field);
        usernameLabel =
            (TextView) findViewById(R.id.aggregate_activity_username_label);
        usernameField =
            (EditText) findViewById(R.id.aggregate_activity_username_field);
        listTablesButton =
            (Button) findViewById(R.id.aggregate_activity_list_tables_button);
        tableListSpinner =
            (Spinner) findViewById(R.id.aggregate_activity_table_list_spinner);
        downloadTableButton = (Button) findViewById(
                R.id.aggregate_activity_download_table_button);
        pullTableButton =
            (Button) findViewById(R.id.aggregate_activity_pull_table_button);
        pushTableButton =
            (Button) findViewById(R.id.aggregate_activity_push_table_button);
    }
    
    private void initHandlers() {
        listTablesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleListTables();
            }
        });
        downloadTableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleDownload(1);
            }
        });
        pullTableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePull(1);
            }
        });
        pushTableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePush(1);
            }
        });
    }
    
    private void setViewForScreenState() {
        if (screenState == ScreenState.REQUESTING_USER_INFO) {
            uriLabel.setVisibility(View.VISIBLE);
            uriField.setVisibility(View.VISIBLE);
            usernameLabel.setVisibility(View.VISIBLE);
            usernameField.setVisibility(View.VISIBLE);
            listTablesButton.setVisibility(View.VISIBLE);
            tableListSpinner.setVisibility(View.INVISIBLE);
            downloadTableButton.setVisibility(View.INVISIBLE);
            pullTableButton.setVisibility(View.INVISIBLE);
            pushTableButton.setVisibility(View.INVISIBLE);
        }
        if ((screenState == ScreenState.REQUESTING_TABLE_SELECTION) ||
            (screenState == ScreenState.REQUESTING_ACTION_SELECTION)) {
            uriLabel.setVisibility(View.INVISIBLE);
            uriField.setVisibility(View.INVISIBLE);
            usernameLabel.setVisibility(View.INVISIBLE);
            usernameField.setVisibility(View.INVISIBLE);
            listTablesButton.setVisibility(View.INVISIBLE);
            tableListSpinner.setVisibility(View.VISIBLE);
        }
        if (screenState == ScreenState.REQUESTING_TABLE_SELECTION) {
            downloadTableButton.setVisibility(View.INVISIBLE);
            pullTableButton.setVisibility(View.INVISIBLE);
            pushTableButton.setVisibility(View.INVISIBLE);
        }
        if (screenState == ScreenState.REQUESTING_ACTION_SELECTION) {
            downloadTableButton.setVisibility(View.VISIBLE);
            pullTableButton.setVisibility(View.VISIBLE);
            pushTableButton.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Attempts to initialize the TasksSync object.
     * @return true if successful; false otherwise
     */
    private boolean initTasksSync() {
        URI uri;
        try {
            uri = new URI(uriField.getText().toString());
        } catch(URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        String username = usernameField.getText().toString();
        try {
            ts = new TasksSync(uri, username);
        } catch(Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    private void handleListTables() {
        if (!initTasksSync()) {
            Log.e("hkworden", "failed to initialize TasksSync");
            return;
        }
        try {
            tableEntryList = ts.listTables();
        } catch(Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        screenState = ScreenState.REQUESTING_TABLE_SELECTION;
        setViewForScreenState();
    }
    
    private void handleDownload(int index) {
        TableEntry entry = tableEntryList.get(index);
        
        // setting up the table on the client
        String tableName = entry.getTableName();
        if (tl.isTableExist(tableName)) {
            int appendage = 2;
            while (tl.isTableExist(tableName + appendage)) {
                appendage++;
            }
            tableName += appendage;
        }
        tl.registerNewTable(tableName);
        String tableId = Integer.toString(tl.getTableID(tableName));
        db.addNewTable(tableName);
        
        // getting the server data
        Modification mod;
        try {
            mod = ts.downloadTaskTable(entry.getAggregateTableIdentifier(),
                    tableId);
        } catch(Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        
        // adding the data to the client table
        DataTable dt = new DataTable(tableId);
        Date now = new Date();
        DataUtils du = DataUtils.getInstance();
        String timestamp = du.formatDateTimeForDB(now);
        List<SynchronizedRow> rows = mod.getRows();
        for (SynchronizedRow row : rows) {
            ContentValues values = new ContentValues();
            values.put(DataTable.DATA_SYNC_ID,
                    row.getAggregateRowIdentifier());
            values.put(DataTable.DATA_SYNC_TAG, row.getRevisionTag());
            Map<String, String> data = row.getColumnValuePairs();
            for (String key : data.keySet()) {
                values.put(key, data.get(key));
            }
            dt.addRow(values, null, timestamp);
        }
        
        // updating the modification number
        tl.updateSyncModNumber(tableId, mod.getModificationNumber());
    }
    
    private void handlePull(int index) {
        
    }
    
    private void handlePush(int index) {
        
    }
}
