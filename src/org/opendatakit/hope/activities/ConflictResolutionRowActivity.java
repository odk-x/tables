package org.opendatakit.hope.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.hope.R;
import org.opendatakit.hope.data.ConflictTable;
import org.opendatakit.hope.data.DbHelper;
import org.opendatakit.hope.data.DbTable;
import org.opendatakit.hope.data.KeyValueStore;
import org.opendatakit.hope.data.TableProperties;
import org.opendatakit.hope.data.UserTable;
import org.opendatakit.hope.views.components.ConflictResolutionListAdapter;
import org.opendatakit.hope.views.components.ConflictResolutionListAdapter.ConcordantColumn;
import org.opendatakit.hope.views.components.ConflictResolutionListAdapter.ConflictColumn;
import org.opendatakit.hope.views.components.ConflictResolutionListAdapter.Resolution;
import org.opendatakit.hope.views.components.ConflictResolutionListAdapter.Section;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListActivity;

/**
 * Activity for resolving the conflicts in a row. This is the native version, 
 * which presents a UI and does not support HTML or js rules.
 * @author sudar.sam@gmail.com
 *
 */
public class ConflictResolutionRowActivity extends SherlockListActivity
    implements ConflictResolutionListAdapter.UICallbacks {
  
  private static final String TAG = 
      ConflictResolutionRowActivity.class.getSimpleName();
  
  public static final String INTENT_KEY_ROW_NUM = "rowNumber";
  private static final int INVALID_ROW_NUMBER = -1;
  
  private static final String BUNDLE_KEY_SHOWING_LOCAL_DIALOG = 
      "showingLocalDialog";
  private static final String BUNDLE_KEY_SHOWING_SERVER_DIALOG = 
      "showingServerDialog";
  private static final String BUNDLE_KEY_SHOWING_RESOLVE_DIALOG = 
      "showingResolveDialog";
  private static final String BUNDLE_KEY_VALUE_KEYS = "valueValueKeys";
  private static final String BUNDLE_KEY_CHOSEN_VALUES = "chosenValues";
  private static final String BUNDLE_KEY_RESOLUTION_KEYS = "resolutionKeys";
  private static final String BUNDLE_KEY_RESOLUTION_VALUES = 
      "resolutionValues";
  
  private DbHelper mDbHelper;
  private ConflictTable mConflictTable;
  private ConflictResolutionListAdapter mAdapter;
  /** The row number of the row in conflict within the {@link ConflictTable}.*/
  private int mRowNumber;
  private String mRowId;
  private String mSyncTag;
  private UserTable mLocal;
  private UserTable mServer;
  private Button mButtonTakeLocal;
  private Button mButtonTakeServer;
  private Button mButtonResolveRow;
  private List<ConflictColumn> mConflictColumns;
  
  private boolean mIsShowingTakeLocalDialog;
  private boolean mIsShowingTakeServerDialog;
  private boolean mIsShowingResolveDialog;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(
        org.opendatakit.hope.R.layout.conflict_resolution_row_activity);
    this.mDbHelper = DbHelper.getDbHelper(this);
    this.mButtonTakeLocal = 
        (Button) findViewById(R.id.conflict_resolution_button_take_local);
    this.mButtonTakeLocal.setOnClickListener(new TakeLocalClickListener());
    this.mButtonTakeServer =
        (Button) findViewById(R.id.conflict_resolution_button_take_server);
    this.mButtonTakeServer.setOnClickListener(new TakeServerClickListener());
    this.mButtonResolveRow = 
        (Button) findViewById(R.id.conflict_resolution_button_resolve_row);
    this.mButtonResolveRow.setOnClickListener(new ResolveRowClickListener());
    String tableId = 
        getIntent().getStringExtra(Controller.INTENT_KEY_TABLE_ID);
    this.mRowNumber = getIntent().getIntExtra(INTENT_KEY_ROW_NUM, 
        INVALID_ROW_NUMBER);
    TableProperties tableProperties = 
        TableProperties.getTablePropertiesForTable(mDbHelper, tableId, 
            KeyValueStore.Type.ACTIVE);
    DbTable dbTable = DbTable.getDbTable(mDbHelper, tableProperties);
    this.mConflictTable = dbTable.getConflictTable();
    this.mLocal = mConflictTable.getLocalTable();
    this.mServer = mConflictTable.getServerTable();
    // We'll use these later on, so heat up the caches.
    this.mLocal.reloadCacheOfColumnProperties();
    this.mServer.reloadCacheOfColumnProperties();
    // And now we need to construct up the adapter.
    // There are several things to do be aware of. We need to get all the 
    // section headings, which will be the column names. We also need to get 
    // all the values which are in conflict, as well as those that are not.
    // We'll present them in user-defined order, as they may have set up the
    // useful information together.
    this.mRowId = this.mLocal.getRowId(mRowNumber);
    this.mSyncTag = this.mLocal.getMetadataByElementKey(mRowNumber, 
        DataTableColumns.SYNC_TAG);
    TableProperties tp = mConflictTable.getLocalTable().getTableProperties();
    List<String> columnOrder = tp.getColumnOrder();
    // This will be the number of rows down we are in the adapter. Each 
    // heading and each cell value gets its own row. Columns in conflict get 
    // two, as we'll need to display each one to the user.
    int adapterOffset = 0;
    List<Section> sections = new ArrayList<Section>();
    this.mConflictColumns = new ArrayList<ConflictColumn>();
    List<ConcordantColumn> noConflictColumns = 
        new ArrayList<ConcordantColumn>();
    for (int i = 0; i < columnOrder.size(); i++) {
      String elementKey = columnOrder.get(i);
      String columnDisplayName = 
          tp.getColumnByElementKey(elementKey).getDisplayName();
      Section newSection = new Section(adapterOffset, columnDisplayName);
      ++adapterOffset;
      sections.add(newSection);
      String localValue = mLocal.getDisplayTextOfData(this, mRowNumber, 
          mLocal.getColumnIndexOfElementKey(elementKey), true);
      String serverValue = mServer.getDisplayTextOfData(this, mRowNumber, 
          mLocal.getColumnIndexOfElementKey(elementKey), true);
      if (localValue.equals(serverValue)) {
        // TODO: this doesn't compare actual equality of blobs if their display
        // text is the same. 
        // We only want to display a single row, b/c there are no choices to 
        // be made by the user.
        ConcordantColumn concordance = new ConcordantColumn(adapterOffset,
            localValue);
        noConflictColumns.add(concordance);
        ++adapterOffset;
      } else {
        // We need to display both the server and local versions.
        ConflictColumn conflictColumn = new ConflictColumn(adapterOffset, 
            elementKey, localValue, serverValue);
        ++adapterOffset;
        mConflictColumns.add(conflictColumn);
      }
    }
    // Now that we have the appropriate lists, we need to construct the 
    // adapter that will display the information.
    this.mAdapter = new ConflictResolutionListAdapter(
        this.getSupportActionBar().getThemedContext(), this, sections, 
        noConflictColumns, mConflictColumns);
    this.setListAdapter(mAdapter);
    this.onDecisionMade(); // so the resolve button is disabled on startup.
  }

  /*
   * (non-Javadoc)
   * @see org.opendatakit.tables.views.components.ConflictResolutionListAdapter.UICallbacks#onDecisionMade(boolean)
   */
  @Override
  public void onDecisionMade() {
    if (isResolvable()) {
      this.mButtonResolveRow.setEnabled(true);
    } else {
      this.mButtonResolveRow.setEnabled(false);
    }
  }
  
  /**
   * True if all the conflict columns have entries that have been chosen by the
   * user in the adapter.
   * @return
   */
  private boolean isResolvable() {
    // We'll check if a decision has been made on every conflict column. If it
    // has we'll return true, otherwise we won't.
    Map<String, String> currentlyResolvedValues = mAdapter.getResolvedValues();
    for (ConflictColumn cc : this.mConflictColumns) {
      if (!currentlyResolvedValues.containsKey(cc.getElementKey())) {
        this.mButtonResolveRow.setEnabled(false);
        return false;
      }
    }
    return true;
  }
  
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(BUNDLE_KEY_SHOWING_LOCAL_DIALOG, 
        mIsShowingTakeLocalDialog);
    outState.putBoolean(BUNDLE_KEY_SHOWING_SERVER_DIALOG, 
        mIsShowingTakeServerDialog);
    outState.putBoolean(BUNDLE_KEY_SHOWING_RESOLVE_DIALOG, 
        mIsShowingResolveDialog);
    // We also need to get the chosen values and decisions and save them so 
    // that we don't lose information if they rotate the screen.
    Map<String, String> chosenValuesMap = mAdapter.getResolvedValues();
    Map<String, Resolution> userResolutions = mAdapter.getResolutions();
    if (chosenValuesMap.size() != userResolutions.size()) {
      Log.e(TAG, "[onSaveInstanceState] chosen values and user resolutions" +
      		" are not the same size. This should be impossible, so not " +
      		"saving state.");
      return;
    }
    String[] valueKeys = new String[chosenValuesMap.size()];
    String[] chosenValues = new String[chosenValuesMap.size()];
    String[] resolutionKeys = new String[userResolutions.size()];
    String[] resolutionValues = new String[userResolutions.size()];
    int i = 0;
    for (Map.Entry<String, String> valueEntry : chosenValuesMap.entrySet()) {
      valueKeys[i] = valueEntry.getKey();
      chosenValues[i] = valueEntry.getValue();
      ++i;;
    }
    i = 0;
    for (Map.Entry<String, Resolution> resolutionEntry : 
        userResolutions.entrySet()) {
      resolutionKeys[i] = resolutionEntry.getKey();
      resolutionValues[i] = resolutionEntry.getValue().name();
      ++i;
    }
    outState.putStringArray(BUNDLE_KEY_VALUE_KEYS, valueKeys);
    outState.putStringArray(BUNDLE_KEY_CHOSEN_VALUES, chosenValues);
    outState.putStringArray(BUNDLE_KEY_RESOLUTION_KEYS, resolutionKeys);
    outState.putStringArray(BUNDLE_KEY_RESOLUTION_VALUES, resolutionValues);
  }
  
  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    if (savedInstanceState.containsKey(BUNDLE_KEY_SHOWING_LOCAL_DIALOG)) {
      boolean wasShowingLocal = 
          savedInstanceState.getBoolean(BUNDLE_KEY_SHOWING_LOCAL_DIALOG);
      if (wasShowingLocal) this.mButtonTakeLocal.performClick();
    }
    if (savedInstanceState.containsKey(BUNDLE_KEY_SHOWING_SERVER_DIALOG)) {
      boolean wasShowingServer =
          savedInstanceState.getBoolean(BUNDLE_KEY_SHOWING_SERVER_DIALOG);
      if (wasShowingServer) this.mButtonTakeServer.performClick();
    }
    if (savedInstanceState.containsKey(BUNDLE_KEY_SHOWING_RESOLVE_DIALOG)) {
      boolean wasShowingResolve =
          savedInstanceState.getBoolean(BUNDLE_KEY_SHOWING_RESOLVE_DIALOG);
      if (wasShowingResolve) this.mButtonResolveRow.performClick();
    }
  }
  
  private class TakeLocalClickListener implements View.OnClickListener {

    @Override
    public void onClick(View v) {
      AlertDialog.Builder builder = new AlertDialog.Builder(
          ConflictResolutionRowActivity.this.getSupportActionBar()
          .getThemedContext());
      builder.setMessage(getString(R.string.take_local_warning));
      builder.setPositiveButton(getString(R.string.yes), 
          new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
              mIsShowingTakeLocalDialog = false;
              DbTable dbTable = 
                  DbTable.getDbTable(mDbHelper, mLocal.getTableProperties());
              Map<String, String> valuesToUse = new HashMap<String, String>();
              for (ConflictColumn cc : mConflictColumns) {
                valuesToUse.put(cc.getElementKey(), cc.getLocalValue());
              }
              dbTable.resolveConflict(mRowId, mSyncTag, valuesToUse);
              ConflictResolutionRowActivity.this.finish();
            }
          });
      builder.setCancelable(true);
      builder.setNegativeButton(getString(R.string.cancel), 
          new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
              dialog.cancel();
            }
          });
      builder.setOnCancelListener(new OnCancelListener() {
        
        @Override
        public void onCancel(DialogInterface dialog) {
          mIsShowingTakeLocalDialog = false;
        }
      });
      mIsShowingTakeLocalDialog = true;
      builder.create().show();
    }
    
  }
  
  private class TakeServerClickListener implements View.OnClickListener {
    
 
    @Override
    public void onClick(View v) {
      AlertDialog.Builder builder = new AlertDialog.Builder(
          ConflictResolutionRowActivity.this.getSupportActionBar()
          .getThemedContext());
      builder.setMessage(getString(R.string.take_server_warning));
      builder.setPositiveButton(getString(R.string.yes), 
          new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
              mIsShowingTakeServerDialog = false;
              DbTable dbTable = 
                  DbTable.getDbTable(mDbHelper, mLocal.getTableProperties());
              Map<String, String> valuesToUse = new HashMap<String, String>();
              for (ConflictColumn cc : mConflictColumns) {
                valuesToUse.put(cc.getElementKey(), cc.getServerValue());
              }
              dbTable.resolveConflict(mRowId, mSyncTag, valuesToUse);
              ConflictResolutionRowActivity.this.finish();
            }
          });
      builder.setCancelable(true);
      builder.setNegativeButton(getString(R.string.cancel), 
          new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
              dialog.cancel();
            }
          });
      builder.setOnCancelListener(new OnCancelListener() {
        
        @Override
        public void onCancel(DialogInterface dialog) {
          mIsShowingTakeServerDialog = false;
        }
      });
      mIsShowingTakeServerDialog = true;
      builder.create().show();
    }   
    
  }
  
  private class ResolveRowClickListener implements View.OnClickListener {
    
    private final String TAG = ResolveRowClickListener.class.getSimpleName();

    @Override
    public void onClick(View v) {
      AlertDialog.Builder builder = new AlertDialog.Builder(
          ConflictResolutionRowActivity.this.getSupportActionBar()
          .getThemedContext());
      builder.setMessage(getString(R.string.resolve_row_warning));
      builder.setPositiveButton(getString(R.string.yes), 
          new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
              mIsShowingResolveDialog = false;
              DbTable dbTable = 
                  DbTable.getDbTable(mDbHelper, mLocal.getTableProperties());
              if (!isResolvable()) {
                // We should never have gotten here! Triz-ouble.
                Log.e(TAG, "[onClick--positive button] the row is not " +
                		"resolvable! The button shouldn't have been enabled.");
                Toast.makeText(ConflictResolutionRowActivity.this
                    .getSupportActionBar().getThemedContext(), 
                    getString(R.string.resolve_cannot_complete_message), 
                    Toast.LENGTH_SHORT).show();
                return;
              }
              Map<String, String> valuesToUse = mAdapter.getResolvedValues();
              dbTable.resolveConflict(mRowId, mSyncTag, valuesToUse);
              ConflictResolutionRowActivity.this.finish();
            }
          });
      builder.setCancelable(true);
      builder.setNegativeButton(getString(R.string.cancel), 
          new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
              dialog.cancel();
            }
          });
      builder.setOnCancelListener(new OnCancelListener() {
        
        @Override
        public void onCancel(DialogInterface dialog) {
          mIsShowingResolveDialog = false;
        }
      });
      mIsShowingResolveDialog = true;
      builder.create().show();
      
    }
    
  }

}
