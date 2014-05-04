package org.opendatakit.tables.activities;

import org.apache.commons.math.analysis.solvers.MullerSolver;
import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.tables.fragments.TopLevelTableMenuFragment;
import org.opendatakit.tables.fragments.TopLevelTableMenuFragment.ITopLevelTableMenuActivity;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.SQLQueryStruct;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;

/**
 * Displays information about a table. List, Map, and Detail views are all
 * displayed via this  activity.
 * @author sudar.sam@gmail.com
 *
 */
public class TableDisplayActivity extends AbsTableActivity
    implements ITopLevelTableMenuActivity {
  
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
    this.initializeFragments();
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
  
  protected void initializeFragments() {
    FragmentManager fragmentManager = this.getFragmentManager();
    TopLevelTableMenuFragment menuFragment = new TopLevelTableMenuFragment();
    fragmentManager.beginTransaction().add(
        menuFragment,
        Constants.FragmentTags.TABLE_MENU).commit();
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
