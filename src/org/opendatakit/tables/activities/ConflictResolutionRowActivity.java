package org.opendatakit.tables.activities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opendatakit.tables.R;
import org.opendatakit.tables.data.ConflictTable;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;
import org.opendatakit.tables.views.components.ConflictResolutionListAdapter;
import org.opendatakit.tables.views.components.ConflictResolutionListAdapter.ConcordantColumn;
import org.opendatakit.tables.views.components.ConflictResolutionListAdapter.ConflictColumn;
import org.opendatakit.tables.views.components.ConflictResolutionListAdapter.Section;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockListActivity;

/**
 * Activity for resolving the conflicts in a row. This is the native version, 
 * which presents a UI and does not support HTML or js rules.
 * @author sudar.sam@gmail.com
 *
 */
public class ConflictResolutionRowActivity extends SherlockListActivity
    implements ConflictResolutionListAdapter.UICallbacks {
  
  public static final String INTENT_KEY_ROW_NUM = "rowNumber";
  private static final int INVALID_ROW_NUMBER = -1;
  
  private ConflictTable mConflictTable;
  private ConflictResolutionListAdapter mAdapter;
  /** The row number of the row in conflict within the {@link ConflictTable}.*/
  private int mRowNumber;
  private UserTable mLocal;
  private UserTable mServer;
  private Button mButtonTakeLocal;
  private Button mButtonTakeServer;
  private Button mButtonResolveRow;
  private List<ConflictColumn> mConflictColumns;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(
        org.opendatakit.tables.R.layout.conflict_resolution_row_activity);
    this.mButtonTakeLocal = 
        (Button) findViewById(R.id.conflict_resolution_button_take_local);
    this.mButtonTakeServer =
        (Button) findViewById(R.id.conflict_resolution_button_take_server);
    this.mButtonResolveRow = 
        (Button) findViewById(R.id.conflict_resolution_button_resolve_row);
    String tableId = 
        getIntent().getStringExtra(Controller.INTENT_KEY_TABLE_ID);
    this.mRowNumber = getIntent().getIntExtra(INTENT_KEY_ROW_NUM, 
        INVALID_ROW_NUMBER);
    DbHelper dbHelper = DbHelper.getDbHelper(this);
    TableProperties tableProperties = 
        TableProperties.getTablePropertiesForTable(dbHelper, tableId, 
            KeyValueStore.Type.ACTIVE);
    DbTable dbTable = DbTable.getDbTable(dbHelper, tableProperties);
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
    // We'll check if a decision has been made on every conflict column. If it
    // has we'll enable the resolution button, otherwise we won't.
    Map<String, String> currentlyResolvedValues = mAdapter.getResolvedValues();
    for (ConflictColumn cc : this.mConflictColumns) {
      if (!currentlyResolvedValues.containsKey(cc.getElementKey())) {
        this.mButtonResolveRow.setEnabled(false);
        return;
      }
    }
    // If we made it here then we know the user has chosen a value for every
    // column that was in conflict.
    this.mButtonResolveRow.setEnabled(true);
  }
  
  private class TakeLocalClickListener implements View.OnClickListener {

    @Override
    public void onClick(View v) {
      // TODO Auto-generated method stub
      
    }
    
  }
  
  private class TakeServerClickListener implements View.OnClickListener {
 
    @Override
    public void onClick(View v) {
      // TODO Auto-generated method stub
      
    }   
    
  }
  
  private class ResolveRowClickListener implements View.OnClickListener {

    @Override
    public void onClick(View v) {
      // TODO Auto-generated method stub
      
    }
    
  }

}
