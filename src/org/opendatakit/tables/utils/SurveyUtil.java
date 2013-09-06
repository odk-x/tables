package org.opendatakit.tables.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.opendatakit.tables.data.TableProperties;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * The ODKSurvey analogue to {@link CollectUtil}. Various functions and 
 * utilities necessary to use Survey to interact with ODKTables.
 * @author sudar.sam@gmail.com
 *
 */
public class SurveyUtil {
  
  private static final String TAG = SurveyUtil.class.getSimpleName();
  
  /** This is prepended to each row/instance uuid. */
  private static final String INSTANCE_UUID_PREFIX = "uuid:";
  
  /** Survey's package name as declared in the manifest. */
  private static final String SURVEY_PACKAGE_NAME = 
      "org.opendatakit.survey.android";
  /** The full path to Survey's main menu activity. */
  private static final String SURVEY_MAIN_MENU_ACTIVITY_COMPONENT_NAME = 
      "org.opendatakit.survey.android.activities.MainMenuActivity";
  
  private static final SimpleDateFormat SIMPLE_DATE_FORMAT = 
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
  
  /**
   * Acquire an intent that will be set up to add a row using Survey. As with
   * {@link CollectUtil#getIntentForOdkCollectAddRow(Context, TableProperties, 
   * org.opendatakit.tables.utils.CollectUtil.CollectFormParameters, Map)}, it
   * should eventually be able to prepopulate the row with the values in 
   * elementKeyToValue. However! Much of this is still unimplemented.
   * @param context
   * @param tp
   * @param elementKeyToValue
   * @return
   */
  public static Intent getIntentForOdkSurveyAddRow(Context context, 
      TableProperties tp, Map<String, String> elementKeyToValue) {
    // Maybe do some checking here to see if it's a custom form or something.
    // This will vary a bit from the Collect way of doing things, methinks.
    // TODO: above.
    // We'll manufacture a row id for this new record.
    String rowId = INSTANCE_UUID_PREFIX + UUID.randomUUID().toString();
    String instanceName = SIMPLE_DATE_FORMAT.format(new Date());
    
    Log.e(TAG, "not completely implemented!"); // fix javadoc when removed
    
    // For now we're just going to see if I'm creating the intent correctly, so
    // we won't put any interesting information in it.
    Intent intent = new Intent();
    intent.setComponent(new ComponentName(SURVEY_PACKAGE_NAME, 
        SURVEY_MAIN_MENU_ACTIVITY_COMPONENT_NAME));
    intent.setAction(Intent.ACTION_EDIT);
    return intent;
  }
  
  /**
   * Eventually will launch the Intent with the correct return code. For now,
   * however, just starts it directly without waiting for return.
   * @param activityToAwaitReturn
   * @param surveyAddIntent
   * @param tp
   */
  public static void launchSurveyToAddRow(Activity activityToAwaitReturn,
      Intent surveyAddIntent, TableProperties tp) {
    activityToAwaitReturn.startActivity(surveyAddIntent);
  }

}
