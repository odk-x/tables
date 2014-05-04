package org.opendatakit.tables.activities;

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
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.mAppName = retrieveAppNameFromIntent();
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
