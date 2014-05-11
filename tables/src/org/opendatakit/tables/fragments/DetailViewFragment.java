package org.opendatakit.tables.fragments;

import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.WebViewUtil;
import org.opendatakit.tables.views.webkits.Control;
import org.opendatakit.tables.views.webkits.TableData;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

/**
 * {@link Fragment} for displaying a detail view.
 * @author sudar.sam@gmail.com
 *
 */
public class DetailViewFragment extends AbsWebTableFragment {
  
  private static final String TAG = DetailViewFragment.class.getSimpleName();
  
  /**
   * The row id of the row that is being displayed in this table.
   */
  private String mRowId;
  /** 
   * The {@link UserTable} this view is displaying, consisting of a single row.
   */
  private UserTable mSingleRowTable;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
     super.onCreate(savedInstanceState);
     String retrievedRowId = this.retrieveRowIdFromBundle(this.getArguments());
     this.mRowId = retrievedRowId;
     this.setHasOptionsMenu(true);
  }
  
  @Override
  public WebView buildView() {
    // First we need to construct the single row table.
    this.initializeTable();
    WebView result = WebViewUtil.getODKCompliantWebView(getActivity());
    Control control = this.createControlObject();
    result.addJavascriptInterface(
        control.getJavascriptInterfaceWithWeakReference(),
        Constants.JavaScriptHandles.CONTROL);
    TableData tableData = this.createDataObject();
    result.addJavascriptInterface(
        tableData.getJavascriptInterfaceWithWeakReference(),
        Constants.JavaScriptHandles.DATA);
    WebViewUtil.displayFileInWebView(
        getActivity(),
        getAppName(),
        result,
        getFileName());
    // Now save the references.
    this.mControlReference = control;
    this.mTableDataReference = tableData;
    return result;
  }
  
  private void initializeTable() {
    UserTable retrievedTable = this.retrieveSingleRowTable();
    this.mSingleRowTable = retrievedTable;
  }
  
  /**
   * Get the {@link UserTable} consisting of one row being displayed by this
   * detail view.
   * @return
   */
  UserTable getSingleRowTable() {
    return this.mSingleRowTable;
  }
  
  /**
   * Retrieve the single row table to display in this view.
   * @return
   */
  UserTable retrieveSingleRowTable() {
    if (this.mRowId == null) {
      Log.e(TAG, "asking to retrieve single row table for null row id");
    }
    String rowId = getRowId();
    DbTable dbTable = DbTable.getDbTable(getTableProperties());
    UserTable result = dbTable.getTableForSingleRow(rowId);
    if (result.getNumberOfRows() > 1 ) {
      Log.e(TAG, "Single row table for row id " + rowId + " returned > 1 row");
    }
    return result;
  }

  @Override
  public ViewFragmentType getFragmentType() {
    return ViewFragmentType.DETAIL;
  }
  
  /**
   * Get the id of the row being displayed.
   * @return
   */
  public String getRowId() {
    return this.mRowId;
  }
  
  /**
   * Retrieve the row id from the bundle.
   * @param bundle the row id, or null if not present.
   * @return
   */
  String retrieveRowIdFromBundle(Bundle bundle) {
    String rowId = IntentUtil.retrieveRowIdFromBundle(bundle);
    return rowId;
  }
  
  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString(Constants.IntentKeys.ROW_ID, this.getRowId());
  }

  @Override
  protected TableData createDataObject() {
    UserTable singleRowTable = this.retrieveSingleRowTable();
    TableData result = new TableData(singleRowTable);
    return result;
  }

}
