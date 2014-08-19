package org.opendatakit.tables.activities;

import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.TableFileUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * The base Activity for all ODK Tables activities. Performs basic
 * functionality like retrieving the app name from an intent that all classes
 * should be doing.
 * @author sudar.sam@gmail.com
 *
 */
public abstract class AbsBaseActivity extends Activity {
  
  protected String mAppName;
  protected String mActionTableId = null;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.mAppName = retrieveAppNameFromIntent();
    if ( savedInstanceState != null && savedInstanceState.containsKey(Constants.IntentKeys.ACTION_TABLE_ID) ) {
      mActionTableId = savedInstanceState.getString(Constants.IntentKeys.ACTION_TABLE_ID);
      if ( mActionTableId != null && mActionTableId.length() == 0 ) {
        mActionTableId = null;
      }
    }
  }
  
  
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    
    if ( mActionTableId != null && mActionTableId.length() != 0 ) {
      outState.putString(Constants.IntentKeys.ACTION_TABLE_ID, mActionTableId);
    }
  }

  public String getActionTableId() {
    return mActionTableId;
  }
  
  public void setActionTableId(String tableId) {
    mActionTableId = tableId;
  }
  
  public boolean maybeLaunchConflictResolver(TableProperties restoreTableProperties) {
    if ( restoreTableProperties.hasConflicts() ) {
      Intent i = new Intent(this,
          ConflictResolutionListActivity.class);
      i.putExtra(Constants.IntentKeys.APP_NAME,
          getAppName());
      i.putExtra(
          Constants.IntentKeys.TABLE_ID,
          restoreTableProperties.getTableId());
      this.startActivityForResult(i, Constants.RequestCodes.LAUNCH_CONFLICT_RESOLVER);
      return true;
    }
    return false;
  }
  
  public boolean maybeLaunchCheckpointResolver(TableProperties restoreTableProperties) {
    if ( restoreTableProperties.hasCheckpoints() ) {
      Intent i = new Intent(this,
          CheckpointResolutionListActivity.class);
      i.putExtra(Constants.IntentKeys.APP_NAME,
          getAppName());
      i.putExtra(
          Constants.IntentKeys.TABLE_ID,
          restoreTableProperties.getTableId());
      this.startActivityForResult(i, Constants.RequestCodes.LAUNCH_CHECKPOINT_RESOLVER);
      return true;
    }
    return false;
  }
  
  /**
   * Gets the app name from the Intent. If it is not present it returns a
   * default app name.
   * @return
   */
  String retrieveAppNameFromIntent() {
    String result = 
        this.getIntent().getStringExtra(Constants.IntentKeys.APP_NAME);
    if (result == null) {
      result = TableFileUtils.getDefaultAppName();
    }
    return result;
  }
  
  /**
   * Get the app name that has been set for this activity.
   * @return
   */
  public String getAppName() {
    return this.mAppName;
  }
  
  /**
   * All Intents in the app expect an app name. This method returns an Intent
   * that can be expected to play nice with other activities. The class is not
   * set.
   * @return
   */
  public Intent createNewIntentWithAppName() {
    Intent intent = new Intent();
    intent.putExtra(
        Constants.IntentKeys.APP_NAME,
        getAppName());
    return intent;
  }

}
