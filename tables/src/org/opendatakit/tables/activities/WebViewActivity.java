package org.opendatakit.tables.activities;

import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.R;
import org.opendatakit.tables.fragments.WebFragment;
import org.opendatakit.tables.utils.CollectUtil;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;

/**
 * Displays a {@link WebView} that is not associated with a particular table.
 * 
 * @author sudar.sam@gmail.com
 *
 */
public class WebViewActivity extends AbsBaseActivity {

  private static final String TAG = WebViewActivity.class.getSimpleName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    WebLogger.getLogger(getAppName()).d(TAG, "[onCreate]");
    this.setContentView(R.layout.activity_web_view_activity);
    WebFragment webFragment = (WebFragment) this.getFragmentManager().findFragmentByTag(
        Constants.FragmentTags.WEB_FRAGMENT);
    String fileName = this.retrieveFileName(savedInstanceState);
    if (webFragment == null) {
      WebLogger.getLogger(getAppName()).d(TAG, "[onCreate] webFragment null, creating new");
      webFragment = new WebFragment();
      Bundle arguments = new Bundle();
      IntentUtil.addFileNameToBundle(arguments, fileName);
      webFragment.setArguments(arguments);
      this.getFragmentManager()
          .beginTransaction()
          .add(R.id.activity_web_view_activity_frame_layout, webFragment,
              Constants.FragmentTags.WEB_FRAGMENT).commit();
    }
  };

  /**
   * Retrieve the file name from either the saved instance state or the
   * {@link Intent} that was used to create the activity.
   * 
   * @param savedInstanceState
   * @return
   */
  protected String retrieveFileName(Bundle savedInstanceState) {
    String result = IntentUtil.retrieveFileNameFromSavedStateOrArguments(savedInstanceState, this
        .getIntent().getExtras());
    return result;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater menuInflater = this.getMenuInflater();
    menuInflater.inflate(R.menu.web_view_activity, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.menu_web_view_activity_table_manager:
      Intent tableManagerIntent = new Intent(this, MainActivity.class);
      tableManagerIntent.putExtra(Constants.IntentKeys.APP_NAME, this.getAppName());
      startActivityForResult(tableManagerIntent, Constants.RequestCodes.LAUNCH_TABLE_MANAGER);
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    String tableId = this.getActionTableId();
    if (tableId != null) {

      switch (requestCode) {
      case Constants.RequestCodes.LAUNCH_CHECKPOINT_RESOLVER:
      case Constants.RequestCodes.LAUNCH_CONFLICT_RESOLVER:
        // don't let the user cancel out of these...
        break;
      // For now, we will just refresh the table if something could have
      // changed.
      case Constants.RequestCodes.ADD_ROW_COLLECT:
        if (resultCode == Activity.RESULT_OK) {
          WebLogger.getLogger(getAppName()).d(TAG,
              "[onActivityResult] result ok, refreshing backing table");
          CollectUtil.handleOdkCollectAddReturn(getBaseContext(), getAppName(), tableId,
              resultCode, data);
        } else {
          WebLogger.getLogger(getAppName()).d(TAG,
              "[onActivityResult] result canceled, not refreshing backing " + "table");
        }
        break;
      case Constants.RequestCodes.EDIT_ROW_COLLECT:
        if (resultCode == Activity.RESULT_OK) {
          WebLogger.getLogger(getAppName()).d(TAG,
              "[onActivityResult] result ok, refreshing backing table");
          CollectUtil.handleOdkCollectEditReturn(getBaseContext(), getAppName(), tableId,
              resultCode, data);
        } else {
          WebLogger.getLogger(getAppName()).d(TAG,
              "[onActivityResult] result canceled, not refreshing backing " + "table");
        }
        break;
      case Constants.RequestCodes.ADD_ROW_SURVEY:
      case Constants.RequestCodes.EDIT_ROW_SURVEY:
        if (resultCode == Activity.RESULT_OK) {
          WebLogger.getLogger(getAppName()).d(TAG,
              "[onActivityResult] result ok, refreshing backing table");
        } else {
          WebLogger.getLogger(getAppName()).d(TAG,
              "[onActivityResult] result canceled, refreshing backing table");
        }
        break;
      }
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

}
