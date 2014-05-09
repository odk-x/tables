package org.opendatakit.tables.activities;

import org.opendatakit.tables.R;
import org.opendatakit.tables.fragments.WebFragment;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;

/**
 * Displays a {@link WebView} that is not associated with a particular table.
 * @author sudar.sam@gmail.com
 *
 */
public class WebViewActivity extends AbsBaseActivity {
  
  private static final String TAG = WebViewActivity.class.getSimpleName();
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "[onCreate]");
    this.setContentView(R.layout.activity_web_view_activity);
    WebFragment webFragment = (WebFragment)
        this.getFragmentManager().findFragmentByTag(
            Constants.FragmentTags.WEB_FRAGMENT);
    String fileName = this.retrieveFileName(savedInstanceState);
    if (webFragment == null) {
      Log.d(TAG, "[onCreate] webFragment null, creating new");
      webFragment = new WebFragment();
      Bundle arguments = new Bundle();
      IntentUtil.addFileNameToBundle(arguments, fileName);
      webFragment.setArguments(arguments);
      this.getFragmentManager().beginTransaction().add(
          R.id.activity_web_view_activity_frame_layout,
          webFragment,
          Constants.FragmentTags.WEB_FRAGMENT).commit();
    }
  };
  
  /**
   * Retrieve the file name from either the saved instance state or the
   * {@link Intent} that was used to create the activity.
   * @param savedInstanceState
   * @return
   */
  protected String retrieveFileName(Bundle savedInstanceState) {
    String result = IntentUtil.retrieveFileNameFromSavedStateOrArguments(
        savedInstanceState,
        this.getIntent().getExtras());
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
      tableManagerIntent.putExtra(
          Constants.IntentKeys.APP_NAME,
          this.getAppName());
      startActivityForResult(
          tableManagerIntent,
          Constants.RequestCodes.LAUNCH_TABLE_MANAGER);
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

}
