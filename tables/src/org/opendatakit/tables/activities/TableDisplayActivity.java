package org.opendatakit.tables.activities;

import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.tables.fragments.SpreadsheetFragment;
import org.opendatakit.tables.fragments.TopLevelTableMenuFragment;
import org.opendatakit.tables.fragments.TopLevelTableMenuFragment.ITopLevelTableMenuActivity;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.SQLQueryStruct;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewTreeObserver;

/**
 * Displays information about a table. List, Map, and Detail views are all
 * displayed via this  activity.
 * @author sudar.sam@gmail.com
 *
 */
public class TableDisplayActivity extends AbsTableActivity
    implements ITopLevelTableMenuActivity {
  
  private static final String TAG = TableDisplayActivity.class.getSimpleName();
  
  /**
   * The fragment types this activity could be displaying.
   * @author sudar.sam@gmail.com
   *
   */
  public enum ViewFragmentType {
    SPREADSHEET,
    LIST,
    MAP,
    GRAPH,
    DETAIL;
  }
  
  /**
   * The {@link UserTable} that is being displayed in this activity.
   */
  private UserTable mUserTable;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.initializeBackingTable();
    this.initializeMenuFragment();
    this.initializeDisplayFragment();
  }
  
  /**
   * Initialize the correct display fragment based on the result of
   * {@link #retrieveTableIdFromIntent()}. Initializes Spreadsheet if none
   * is present in Intent.
   */
  protected void initializeDisplayFragment() {
    ViewFragmentType viewFragmentType = this.retrieveFragmentTypeToDisplay();
    switch (viewFragmentType) {
    case SPREADSHEET:
      this.showSpreadsheetFragment();
      break;
    case DETAIL:
      break;
    case GRAPH:
      break;
    case LIST:
      break;
    case MAP:
      break;
    default:
      Log.e(TAG, "ViewFragmentType not recognized: " + viewFragmentType);
      break;
    }
  }
  
  /**
   * @return the {@link ViewFragmentType} that was passed in the intent,
   * or null if none exists.
   */
  protected ViewFragmentType retrieveViewFragmentTypeFromIntent() {
    if (this.getIntent().getExtras() == null) {
      return null;
    }
    String viewFragmentTypeStr = this.getIntent().getExtras().getString(
        Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE);
    if (viewFragmentTypeStr == null) {
      return null;
    } else {
      ViewFragmentType result = ViewFragmentType.valueOf(viewFragmentTypeStr);
      return result;
    }
  }
  
  /**
   * Get the {@link ViewFragmentType} that should be displayed. Any type that
   * was passed in the Intent gets precedent. If none is present, the value
   * corresponding to {@link TableProperties#getDefaultViewType()} wins. If
   * none is present, returns {@link ViewFragmentType#SPREADSHEET}.
   * @return
   */
  protected ViewFragmentType retrieveFragmentTypeToDisplay() {
    ViewFragmentType result = retrieveViewFragmentTypeFromIntent();
    if (result == null) {
      TableViewType viewType = 
          this.getUserTable().getTableProperties().getDefaultViewType();
      result = this.getViewFragmentTypeFromViewType(viewType);
    }
    if (result == null) {
      result = ViewFragmentType.SPREADSHEET;
    }
    return result;
  }
  
  /**
   * Get the {@link ViewFragmentType} that corresponds to
   * {@link TableViewType}. If no match is found, returns null.
   * @param viewType
   * @return
   */
  protected ViewFragmentType getViewFragmentTypeFromViewType(
      TableViewType viewType) {
    switch (viewType) {
    case SPREADSHEET:
      return ViewFragmentType.SPREADSHEET;
    case MAP:
      return ViewFragmentType.MAP;
    case GRAPH:
      return ViewFragmentType.GRAPH;
    case LIST:
      return ViewFragmentType.LIST;
    default:
      Log.e(TAG, "viewType " + viewType + " not recognized.");
      return null;
    }
  }
  
  /**
   * Initialize {@link TableDisplayActivity#mUserTable}.
   */
  private void initializeBackingTable() {
    UserTable userTable = this.retrieveUserTable();
    this.mUserTable = userTable;
  }
  
  /**
   * Get the {@link UserTable} that is being held by this activity.
   * @return
   */
  public UserTable getUserTable() {
    return this.mUserTable;
  }
  
  /**
   * Refresh the data being displayed.
   */
  public void refreshDataTable() {
    this.initializeBackingTable();
  }
  
  /**
   * Get the {@link UserTable} from the database that should be displayed.
   * @return
   */
  UserTable retrieveUserTable() {
    TableProperties tableProperties = this.getTableProperties();
    SQLQueryStruct sqlQueryStruct = 
        this.retrieveSQLQueryStatStructFromIntent();
    DbTable dbTable = DbTable.getDbTable(tableProperties);
    UserTable result = dbTable.rawSqlQuery(
        sqlQueryStruct.whereClause,
        sqlQueryStruct.selectionArgs,
        sqlQueryStruct.groupBy,
        sqlQueryStruct.having,
        sqlQueryStruct.orderByElementKey,
        sqlQueryStruct.orderByDirection);
    return result;
  }
  
  /**
   * Retrieve the {@link SQLQueryStruct} specified in the {@link Intent} that
   * restricts the current table.
   * @return
   */
  SQLQueryStruct retrieveSQLQueryStatStructFromIntent() {
    SQLQueryStruct result = IntentUtil.getSQLQueryStructFromBundle(
        this.getIntent().getExtras());
    return result;
  }
  
  protected void initializeMenuFragment() {
    FragmentManager fragmentManager = this.getFragmentManager();
    TopLevelTableMenuFragment menuFragment = new TopLevelTableMenuFragment();
    fragmentManager.beginTransaction().add(
        menuFragment,
        Constants.FragmentTags.TABLE_MENU).commit();
  }
  
  /**
   * Show the spreadsheet fragment, creating a new one if it doesn't yet exist.
   */
  protected void showSpreadsheetFragment() {
    FragmentManager fragmentManager = this.getFragmentManager();
    // Try to retrieve one already there.
    SpreadsheetFragment spreadsheetFragment = (SpreadsheetFragment)
        fragmentManager.findFragmentByTag(Constants.FragmentTags.SPREADSHEET);
    if (spreadsheetFragment != null) {
      fragmentManager.beginTransaction().show(spreadsheetFragment).commit();
    }
    // Otherwise create a new one.
    spreadsheetFragment = new SpreadsheetFragment();
    fragmentManager.beginTransaction().add(
        spreadsheetFragment,
        Constants.FragmentTags.SPREADSHEET).commit();
  }

  /**
   * Return the {@link ViewFragmentType} that is currently being displayed.
   */
  @Override
  public ViewFragmentType getCurrentFragmentType() {
    // TODO: return the right thing.
    return ViewFragmentType.SPREADSHEET;
  }

}
