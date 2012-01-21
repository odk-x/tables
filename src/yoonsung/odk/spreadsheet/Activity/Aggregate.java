package yoonsung.odk.spreadsheet.Activity;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.opendatakit.aggregate.odktables.client.api.SynchronizeAPI;
import org.opendatakit.aggregate.odktables.client.entity.Column;
import org.opendatakit.aggregate.odktables.client.entity.Modification;
import org.opendatakit.aggregate.odktables.client.entity.SynchronizedRow;
import org.opendatakit.aggregate.odktables.client.entity.TableEntry;
import org.opendatakit.common.ermodel.simple.AttributeType;
import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.Activity.util.DialogsUtil;
import yoonsung.odk.spreadsheet.DataStructure.Table;
import yoonsung.odk.spreadsheet.Database.DBIO;
import yoonsung.odk.spreadsheet.Database.DataTable;
import yoonsung.odk.spreadsheet.Database.DataUtils;
import yoonsung.odk.spreadsheet.Database.TableList;
import yoonsung.odk.spreadsheet.Database.TableList.TableInfo;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
    
    public enum SyncType { LOCAL, REMOTE, SYNC }
    
    private TextView uriLabel;
    private EditText uriField;
    private TextView usernameLabel;
    private EditText usernameField;
    private Button listTablesButton;
    private Spinner tableListSpinner;
    private Button downloadTableButton;
    private Button pullTableButton;
    private Button pushTableButton;
    
    private SynchronizeAPI sync;
    private DBIO db;
    private TableList tl;
    
    private List<SyncTableInfo> tables;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("ODK Tables > Aggregate");
        db = new DBIO();
        tl = new TableList();
        setContentView(R.layout.aggregate_activity);
        findViewComponents();
        setViewForGettingUserInfo();
        initClickHandlers();
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
    
    private void initClickHandlers() {
        listTablesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleListTables();
            }
        });
        tableListSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                    int position, long id) {
                handleTableSelected(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }
        });
        downloadTableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleDownload();
            }
        });
        pullTableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePull();
            }
        });
        pushTableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePush();
            }
        });
    }
    
    private void setViewForGettingUserInfo() {
        uriLabel.setVisibility(View.VISIBLE);
        uriField.setVisibility(View.VISIBLE);
        usernameLabel.setVisibility(View.VISIBLE);
        usernameField.setVisibility(View.VISIBLE);
        listTablesButton.setVisibility(View.VISIBLE);
        tableListSpinner.setVisibility(View.GONE);
        downloadTableButton.setVisibility(View.GONE);
        pullTableButton.setVisibility(View.GONE);
        pushTableButton.setVisibility(View.GONE);
    }
    
    private void setViewForSelectingTable() {
        uriLabel.setVisibility(View.GONE);
        uriField.setVisibility(View.GONE);
        usernameLabel.setVisibility(View.GONE);
        usernameField.setVisibility(View.GONE);
        listTablesButton.setVisibility(View.GONE);
        tableListSpinner.setVisibility(View.VISIBLE);
        downloadTableButton.setVisibility(View.GONE);
        pullTableButton.setVisibility(View.GONE);
        pushTableButton.setVisibility(View.GONE);
    }
    
    private void handleListTables() {
        String uri = uriField.getText().toString();
        String username = usernameField.getText().toString();
        String[] loginInfo = { uri, username };
        (new ListTablesTask()).execute(loginInfo);
    }
    
    private void handleTableSelected(int index) {
        SyncTableInfo sti = tables.get(index);
        switch (sti.getSyncType()) {
        case LOCAL:
            downloadTableButton.setVisibility(View.GONE);
            pullTableButton.setVisibility(View.GONE);
            pushTableButton.setVisibility(View.VISIBLE);
            break;
        case REMOTE:
            downloadTableButton.setVisibility(View.VISIBLE);
            pullTableButton.setVisibility(View.GONE);
            pushTableButton.setVisibility(View.GONE);
            break;
        case SYNC:
            downloadTableButton.setVisibility(View.GONE);
            pullTableButton.setVisibility(View.VISIBLE);
            pushTableButton.setVisibility(View.VISIBLE);
            break;
        default:
            throw new RuntimeException("unexpected sync type: " +
                    sti.getSyncType());
        }
    }
    
    private void handleDownload() {
        int tableIndex = tableListSpinner.getSelectedItemPosition();
        (new DownloadTableTask()).execute(tableIndex);
    }
    
    private void handlePull() {
        int tableIndex = tableListSpinner.getSelectedItemPosition();
        (new PullTableTask()).execute(tableIndex);
    }
    
    private void handlePush() {
        int tableIndex = tableListSpinner.getSelectedItemPosition();
        (new PushTableTask()).execute(tableIndex);
    }
    
    /**
     * A task for getting the table list from the server.
     */
    private class ListTablesTask extends AsyncTask<String[], Integer,
            Boolean> {
        
        public static final int STATUS_CONNECTING = 0;
        public static final int STATUS_DOWNLOADING = 1;
        
        private ProgressDialog dialog;
        
        @Override
        public Boolean doInBackground(String[]... loginInfos) {
            publishProgress(STATUS_CONNECTING);
            
            if (!initTasksSync(loginInfos[0][0], loginInfos[0][1])) {
                return false;
            }
            
            publishProgress(STATUS_DOWNLOADING);
            try {
                tables = new ArrayList<SyncTableInfo>();
                List<TableEntry> remoteTableList = sync.listAllTables();
                List<TableInfo> localTableList = tl.getTableList();
                for (TableEntry remote : remoteTableList) {
                    if (remote.getTableID() == null) {
                        tables.add(new SyncTableInfo(null, remote));
                    } else {
                        TableInfo local = null;
                        for (TableInfo ti : localTableList) {
                            if (remote.getTableID().equals(ti.getTableID())) {
                                local = ti;
                                break;
                            }
                        }
                        tables.add(new SyncTableInfo(local, remote));
                        if (local != null) {
                            localTableList.remove(local);
                        }
                    }
                }
                for (TableInfo local : localTableList) {
                    tables.add(new SyncTableInfo(local, null));
                }
            } catch(Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            }
            return true;
        }
        
        /**
         * Attempts to initialize the TasksSync object.
         * @param uriString the URI (in String form) of the ODK Aggregate
         * instance
         * @param username the username
         * @return true if successful; false otherwise
         */
        private boolean initTasksSync(String uriString, String username) {
            URI uri;
            try {
                uri = new URI(uriString);
            } catch(URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            }
            try {
                sync = new SynchronizeAPI(uri, username);
            } catch(Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            }
            return true;
        }
        
        protected void onProgressUpdate(Integer... progress) {
            switch(progress[0]) {
            case STATUS_CONNECTING:
                dialog = new ProgressDialog(Aggregate.this);
                dialog.setMessage("connecting to server...");
                dialog.show();
                break;
            case STATUS_DOWNLOADING:
                dialog.setMessage("downloading table information...");
                break;
            }
        }
        
        protected void onPostExecute(Boolean result) {
            dialog.dismiss();
            if (result) {
                initTableSpinnerList();
                setViewForSelectingTable();
            } else {
                DialogsUtil.getInfoDialog(Aggregate.this, "Failure").show();
            }
        }
        
        private void initTableSpinnerList() {
            String[] tableNames = new String[tables.size()];
            for (int i = 0; i < tables.size(); i++) {
                SyncTableInfo sti = tables.get(i);
                tableNames[i] = sti.getDisplayInfo();
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    Aggregate.this, android.R.layout.simple_spinner_item,
                    tableNames);
            adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            tableListSpinner.setAdapter(adapter);
            tableListSpinner.setSelection(0);
        }
    }
    
    /**
     * A task for downloading a table from the server.
     */
    private class DownloadTableTask extends AsyncTask<Integer, Integer, Boolean> {
        
        public static final int STATUS_INITIALIZING_TABLE = 0;
        public static final int STATUS_DOWNLOADING_DATA = 1;
        public static final int STATUS_ADDING_DATA = 2;
        
        private ProgressDialog dialog;
        
        @Override
        protected Boolean doInBackground(Integer... tableIndices) {
            publishProgress(STATUS_INITIALIZING_TABLE);
            
            TableEntry entry = tables.get(tableIndices[0]).getRemoteInfo();

            // setting up the table on the client
            String tableName = entry.getTableName().replace(" ", "_");
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
            DataTable dt = new DataTable(tableId);
            for (Column col : entry.getColumns()) {
                dt.addNewColumn(col.getName());
            }
            
            // getting the server data
            publishProgress(STATUS_DOWNLOADING_DATA);
            Modification mod;
            try {
                mod = sync.cloneSynchronizedTable(
                        entry.getAggregateTableIdentifier(), tableId);
            } catch(Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            }
            
            // adding the data to the client table
            publishProgress(STATUS_ADDING_DATA);
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
            
            // updating the last sync time and modification number
            tl.updateLastSyncTime(tableId, new Date());
            tl.updateSyncModNumber(tableId, mod.getModificationNumber());
            
            return true;
        }
        
        protected void onProgressUpdate(Integer... progress) {
            switch(progress[0]) {
            case STATUS_INITIALIZING_TABLE:
                dialog = new ProgressDialog(Aggregate.this);
                dialog.setMessage("initializaing table...");
                dialog.show();
                break;
            case STATUS_DOWNLOADING_DATA:
                dialog.setMessage("downloading data...");
                break;
            case STATUS_ADDING_DATA:
                dialog.setMessage("adding data...");
                break;
            }
        }
        
        protected void onPostExecute(Boolean result) {
            dialog.dismiss();
            if (result) {
                DialogsUtil.getInfoDialog(Aggregate.this, "Success").show();
            } else {
                DialogsUtil.getInfoDialog(Aggregate.this, "Failure").show();
            }
        }
    }
    
    /**
     * A task for pulling table data from the server.
     */
    private class PullTableTask extends AsyncTask<Integer, Integer, Boolean> {
        
        public static final int STATUS_DOWNLOADING_DATA = 0;
        public static final int STATUS_UPDATING_DATA = 1;
        
        private ProgressDialog dialog;
        
        @Override
        protected Boolean doInBackground(Integer... tableIndices) {
            publishProgress(STATUS_DOWNLOADING_DATA);
            
            // getting the data table for the client table
            TableEntry entry = tables.get(tableIndices[0]).getRemoteInfo();
            String tableId = entry.getTableID();
            DataTable dt = new DataTable(tableId);
            
            // getting the server data
            Modification mod;
            try {
                mod = sync.synchronize(tableId, tl.getSyncModNumber(tableId));
            } catch(Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            }
            
            // updating data
            publishProgress(STATUS_UPDATING_DATA);
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
                if (row.getRowID() == null) {
                    dt.addRow(values, null, timestamp);
                } else {
                    dt.updateRow(values, Integer.valueOf(row.getRowID()));
                }
            }
            
            // updating the last sync time and modification number
            tl.updateLastSyncTime(tableId, new Date());
            tl.updateSyncModNumber(tableId, mod.getModificationNumber());
            
            return true;
        }
        
        protected void onProgressUpdate(Integer... progress) {
            switch(progress[0]) {
            case STATUS_DOWNLOADING_DATA:
                dialog = new ProgressDialog(Aggregate.this);
                dialog.setMessage("downloading data...");
                dialog.show();
                break;
            case STATUS_UPDATING_DATA:
                dialog.setMessage("updating data...");
                break;
            }
        }
        
        protected void onPostExecute(Boolean result) {
            dialog.dismiss();
            if (result) {
                DialogsUtil.getInfoDialog(Aggregate.this, "Success").show();
            } else {
                DialogsUtil.getInfoDialog(Aggregate.this, "Failure").show();
            }
        }
    }
    
    /**
     * A task for pushing table data to the server.
     */
    private class PushTableTask extends AsyncTask<Integer, Integer, Boolean> {
        
        public static final int STATUS_PREPARING_DATA = 0;
        public static final int STATUS_SENDING_DATA = 1;
        public static final int UPDATING_LOCAL_RECORDS = 2;
        
        private ProgressDialog dialog;
        
        @Override
        protected Boolean doInBackground(Integer... tableIndices) {
            publishProgress(STATUS_PREPARING_DATA);
            
            // getting the data table for the client table
            SyncTableInfo tableInfo = tables.get(tableIndices[0]);
            String tableId = tableInfo.getTableId();
            DataTable dt = new DataTable(tableId);
            
            // getting the client data
            DataUtils du = DataUtils.getInstance();
            Date lastSyncTime = tl.getLastSyncTime(tableId);
            List<SynchronizedRow> rows = new ArrayList<SynchronizedRow>();
            Table table = dt.getCompleteTable();
            List<String> data = table.getRawData();
            List<String> colNames = table.getHeader();
            List<Integer> rowIds = table.getRowID();
            int timestampCol = colNames.indexOf(DataTable.DATA_TIMESTAMP);
            int height = table.getHeight();
            int width = table.getWidth();
            for (int i = 0; i < height; i++) {
                String lastUpdateString = data.get((i * width) + timestampCol);
                Date lastUpdate;
                try {
                    lastUpdate = du.parseDateTimeFromDB(lastUpdateString);
                } catch(ParseException e) {
                    // TODO Auto-generated catch block
                    lastUpdate = new Date();
                }
                if ((lastSyncTime != null) &&
                        lastUpdate.before(lastSyncTime)) {
                    continue;
                }
                SynchronizedRow row = new SynchronizedRow();
                row.setRowID(rowIds.get(i).toString());
                for (int j = 0; j < width; j++) {
                    String colName = colNames.get(j);
                    if (!dt.isHiddenColumn(colName)) {
                        row.setValue(colName, data.get((i * width) + j));
                    }
                }
                rows.add(row);
            }
            
            // sending data
            publishProgress(STATUS_SENDING_DATA);
            if (tableInfo.getSyncType() == SyncType.LOCAL) {
                List<Column> columns = new ArrayList<Column>();
                for (String colName : colNames) {
                    columns.add(new Column(colName, AttributeType.STRING, true));
                }
                try {
                    sync.createSynchronizedTable(tableId,
                            tableInfo.getLocalInfo().getTableName(), columns);
                } catch(Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return false;
                }
            }
            Modification mod;
            try {
                mod = sync.insertSynchronizedRows(tableId,
                        tl.getSyncModNumber(tableId), rows);
            } catch(Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            }
            
            publishProgress(UPDATING_LOCAL_RECORDS);
            for (SynchronizedRow row : mod.getRows()) {
                ContentValues values = new ContentValues();
                Map<String, String> newData = row.getColumnValuePairs();
                for (String key : newData.keySet()) {
                    values.put(key, newData.get(key));
                }
                values.put(DataTable.DATA_SYNC_TAG, row.getRevisionTag());
                values.put(DataTable.DATA_SYNC_ID, row.getAggregateRowIdentifier());
                dt.updateRow(values, Integer.valueOf(row.getRowID()));
            }
            
            tl.updateLastSyncTime(tableId, new Date());
            tl.updateSyncModNumber(tableId, mod.getModificationNumber());
            
            return true;
        }
        
        protected void onProgressUpdate(Integer... progress) {
            switch(progress[0]) {
            case STATUS_PREPARING_DATA:
                dialog = new ProgressDialog(Aggregate.this);
                dialog.setMessage("preparing data...");
                dialog.show();
                break;
            case STATUS_SENDING_DATA:
                dialog.setMessage("sending data...");
                break;
            case UPDATING_LOCAL_RECORDS:
                dialog.setMessage("updating local records...");
                break;
            }
        }
        
        protected void onPostExecute(Boolean result) {
            dialog.dismiss();
            if (result) {
                DialogsUtil.getInfoDialog(Aggregate.this, "Success").show();
            } else {
                DialogsUtil.getInfoDialog(Aggregate.this, "Failure").show();
            }
        }
    }
    
    private class SyncTableInfo {
        
        private SyncType syncType;
        private TableInfo localInfo;
        private TableEntry remoteInfo;
        
        public SyncTableInfo(TableInfo localInfo, TableEntry remoteInfo) {
            if ((localInfo != null) && (remoteInfo != null)) {
                syncType = SyncType.SYNC;
            } else if (localInfo != null) {
                syncType = SyncType.LOCAL;
            } else if (remoteInfo != null) {
                syncType = SyncType.REMOTE;
            } else {
                throw new RuntimeException("attempted to construct " +
                        "SyncTableInfo with null local and remote data");
            }
            this.localInfo = localInfo;
            this.remoteInfo = remoteInfo;
        }
        
        public SyncType getSyncType() {
            return syncType;
        }
        
        public TableInfo getLocalInfo() {
            return localInfo;
        }
        
        public TableEntry getRemoteInfo() {
            return remoteInfo;
        }
        
        public String getTableId() {
            switch (syncType) {
            case LOCAL:
                return localInfo.getTableID();
            case REMOTE:
                return remoteInfo.getTableID();
            case SYNC:
                return localInfo.getTableID();
            default:
                throw new RuntimeException("unexpected sync type: " +
                        syncType);
            }
        }
        
        public String getDisplayInfo() {
            switch (syncType) {
            case LOCAL:
                return localInfo.getTableName() + " (local)";
            case REMOTE:
                return remoteInfo.getTableName() + " (remote)";
            case SYNC:
                return localInfo.getTableName() + " (sync)";
            default:
                throw new RuntimeException("unexpected sync type: " +
                        syncType);
            }
        }
    }
}
