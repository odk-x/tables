/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.UUID;

import org.opendatakit.aggregate.odktables.rest.ApiConstants;
import org.opendatakit.common.android.provider.FormsColumns;
import org.opendatakit.common.android.provider.FormsProviderAPI;
import org.opendatakit.common.android.utilities.KeyValueHelper;
import org.opendatakit.common.android.utilities.KeyValueStoreHelper;
import org.opendatakit.common.android.utilities.ODKCursorUtils;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.application.Tables;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.RemoteException;

/**
 * The ODKSurvey analogue to {@link CollectUtil}. Various functions and
 * utilities necessary to use Survey to interact with ODKTables.
 *
 * @author sudar.sam@gmail.com
 *
 */
public class SurveyUtil {

  private static final String TAG = SurveyUtil.class.getSimpleName();

  public static final String KVS_PARTITION = "SurveyUtil";
  public static final String KVS_ASPECT = "default";
  public static final String KEY_FORM_ID = "SurveyUtil.formId";

  public static final String URL_ENCODED_SPACE = "%20";

  /**
   * A url-style query param that is used when constructing a survey intent.
   * It specifies the instance id that should be opened. An unrecognized
   * instanceId will add a new instance with that id.
   */
  private static final String URI_SURVEY_QUERY_PARAM_INSTANCE_ID = "instanceId";
  /**
   * A url-style query param that encodes to what position within a form Survey
   * should open. Can be ignored.
   */
  private static final String URI_SURVEY_QUERY_PARAM_SCREEN_PATH = "screenPath";

  /** This is prepended to each row/instance uuid. */
  private static final String INSTANCE_UUID_PREFIX = "uuid:";

  /** Survey's package name as declared in the manifest. */
  private static final String SURVEY_PACKAGE_NAME = "org.opendatakit.survey.android";
  /** The full path to Survey's main menu activity. */
  private static final String SURVEY_MAIN_MENU_ACTIVITY_COMPONENT_NAME = "org.opendatakit.survey.android.activities.SplashScreenActivity";

  /**
   * Returns Android's content scheme, with ://, ready for prepending to an
   * authority.
   */
  private static final String FORWARD_SLASH = "/";

  private static final String SURVEY_ADDROW_FORM_ID_PREFIX = "_generated_";

  /**
   * Return the formId for the single file that will be written when there is no
   * custom form defined for a table.
   *
   * @param tp
   * @return
   */
  private static String getDefaultAddRowFormId(String tableId) {
    return SURVEY_ADDROW_FORM_ID_PREFIX + tableId;
  }

  /**
   * Acquire an intent that will be set up to add a row using Survey. As with
   * {@link CollectUtil#getIntentForOdkCollectAddRow(Context, String, String, org.opendatakit.tables.utils.CollectUtil.CollectFormParameters, Map)}
   * , it
   * should eventually be able to prepopulate the row with the values in
   * elementKeyToValue. However! Much of this is still unimplemented.
   *
   * @param context
   * @param appName
   * @param tableId
   * @param elementNameToValue
   *          a mapping of elementName to value for the values
   *          that you wish to prepopulate in the add row. Note that these are
   *          different
   *          than the values used in Collect, which relies on elementKey rather
   *          than
   *          on elementName.
   * @return
   */
  public static Intent getIntentForOdkSurveyAddRow(Context context, 
                                                   String appName,
                                                   String tableId,
                                                   SurveyFormParameters surveyFormParameters,
                                                   Map<String, String> elementNameToValue) {

    // To launch to a specific form we need to construct an Intent meant for
    // the MainMenuActivity. This Intent takes a Uri as its setData element
    // that specifies the form and the instance. For more detail on what this
    // Uri must look like, see getUriForSurveyHelper.
    Intent intent = new Intent();
    intent.setComponent(new ComponentName(SURVEY_PACKAGE_NAME,
                                          SURVEY_MAIN_MENU_ACTIVITY_COMPONENT_NAME));
    intent.setAction(Intent.ACTION_EDIT);
    Uri addUri = getUriForSurveyAddRow(context, appName, tableId, surveyFormParameters, elementNameToValue,
        context.getContentResolver());
    intent.setData(addUri);
    return intent;
  }

  /**
   * Acquire an Intent that can be launched to edit a given row using Survey.
   * The row that will be edited will be that pointed to by instanceId, which
   * must correspond to the rowId of that row. The form that will be used will
   * be that specified by surveyFormParameters.
   * <p>
   * TODO: does supporting both a form and an instance make sense with Survey?
   * Does the row perhaps presuppose some structure on the form? Should clarify
   * this.
   *
   * @param context
   * @param tp
   * @param appName
   * @param surveyFormParameters
   * @param instanceId
   * @return
   */
  public static Intent getIntentForOdkSurveyEditRow(Context context, 
                                                    String appName, String tableId,
                                                    SurveyFormParameters surveyFormParameters,
                                                    String instanceId) {
    // To launch a specific form for a particular row we need to construct up
    // an Intent for Survey's MainMenuActivity. This Intent takes a Uri via its
    // setData function that specifies the form and the instance. See
    // getUriForSurveyHelper for more detail.
    Intent intent = new Intent();
    intent.setComponent(new ComponentName(SURVEY_PACKAGE_NAME,
                                          SURVEY_MAIN_MENU_ACTIVITY_COMPONENT_NAME));
    intent.setAction(Intent.ACTION_EDIT);
    Uri editUri = getUriForSurveyEditRow(context, appName, tableId, surveyFormParameters, instanceId,
        context.getContentResolver());
    intent.setData(editUri);
    return intent;
  }

  /**
   * Get a Uri that can be added to a Survey Intent in order to add a row to
   * the specified table and app using the form pointed to by
   * surveyFormParameters.
   *
   * @param tp
   * @param appName
   * @param surveyFormParameters
   * @param elementNameToValue
   *          a map of prepopulated values to add to the form
   * @param resolver
   * @return
   */
  /*
   * The ContentResolver is being added here because it was needed in Collect.
   * It is conceivable to me that we might eventually need it to query Survey
   * in order to get the default form for a table or something of this nature.
   * For at least the very short term, it will remain.
   */
  private static Uri getUriForSurveyAddRow(Context context, String appName, String tableId,
                                           SurveyFormParameters surveyFormParameters,
                                           Map<String, String> elementNameToValue,
                                           ContentResolver resolver) {
    // We'll create a UUID, as that will tell survey we want a new one.
    String newUuid = INSTANCE_UUID_PREFIX + UUID.randomUUID().toString();
    Uri helpedUri = getUriForSurveyHelper(context, appName, tableId, surveyFormParameters, newUuid,
        elementNameToValue);
    return helpedUri;
  }

  /**
   * Get a Uri that can be added to a Survey Intent in order to edit the row
   * specified by instanceId using the form specified by surveyFormParameters.
   *
   * @param tp
   * @param appName
   * @param surveyFormParameters
   * @param instanceId
   * @param resolver
   * @return
   */
  /*
   * The ContentResolver is being added here because it was needed in Collect.
   * It is conceivable to me that we might eventually need it to query Survey
   * in order to get the default form for a table or something of this nature.
   * For at least the very short term, it will remain.
   */
  private static Uri getUriForSurveyEditRow(Context context, String appName, String tableId,
                                            SurveyFormParameters surveyFormParameters,
                                            String instanceId, ContentResolver resolver) {
    // The helper function does most of the heavy lifting here. Unlike the
    // add row call, all we do is hand off the info, as we already have our
    // instanceId, which is pointing to an existing row.
    // Note that while the code path might exist to add key-value pairs to the
    // end of the URI, since we're editing we're not going to allow this for
    // now. It's conceivable, perhaps, that we'll want to allow specification
    // of subforms or something, but for now we're not going to allow it.
    Uri helpedUri = getUriForSurveyHelper(context, appName, tableId, surveyFormParameters, instanceId, null);
    return helpedUri;
  }

  /**
   * Helper function for getting an Intent to add or edit data using Survey.
   *
   * @param tp
   *          the table that will be receiving the add
   *          TODO: are we going to handle adding data to multiple tables? Does
   *          Survey
   *          know how to do this to the point that we don't even need this
   *          parameter?
   * @param surveyFormParameters
   *          the parameters detailing the form to use.
   * @param appName
   *          the app name
   * @param instanceId
   *          the instance of the id that will be edited or added. A
   *          newly-generated id will result in an add row--an existent ID will
   *          result
   *          in an edit row for the specified id.
   * @return
   */
  private static Uri getUriForSurveyHelper(Context context, String appName,
                                           String tableId,
                                           SurveyFormParameters surveyFormParameters,
                                           String instanceId,
                                           Map<String, String> elementNameToValue) {
    // We're operating for the moment under the assumption that Survey expects
    // a uri like the following:
    // content://org.opendatakit.survey.android.provider.FormProvider/
    // appname/formId/#.
    // # is a url frame and it accepts query parameters as in a URL. Pertinent
    // ones include those specified in SurveyFormParameters. Of particular note
    // are the following:
    // instanceId (to edit a particular instance)
    // screenPath (to open to a particular screen--not yet useful, but perhaps
    // later).
    if (appName == null) {
      // maybe we actually allow this and default to Tables?
      throw new IllegalArgumentException("app name cannot be null");
    }
    if (surveyFormParameters.getFormId() == null) {
      // again, maybe we allow this and choose a default? likely not.
      throw new IllegalArgumentException("cannot have a null formId");
    }
    Uri uri = Uri.withAppendedPath(
        Uri.withAppendedPath(
          Uri.withAppendedPath(FormsProviderAPI.CONTENT_URI, appName),
            tableId), surveyFormParameters.getFormId());

    String uriStr = uri.toString() + FORWARD_SLASH + "#";
    // Now we need to add our query parameters.
    try {
      uriStr += URI_SURVEY_QUERY_PARAM_INSTANCE_ID + "="
          + URLEncoder.encode(instanceId, ApiConstants.UTF8_ENCODE);
      String screenPath = surveyFormParameters.getScreenPath();
      if (screenPath != null && screenPath.length() != 0) {
        uriStr += "&" + URI_SURVEY_QUERY_PARAM_SCREEN_PATH + "="
            + URLEncoder.encode(screenPath, ApiConstants.UTF8_ENCODE);
      }
      if (elementNameToValue != null && !elementNameToValue.isEmpty()) {
        // We'll add all the entries to the URI as key-value pairs.
        // Use a StringBuilder in case we have a lot of these.
        // We'll assume we already have parameters added to the frame. This is
        // reasonable because this URI call thus far insists on an instanceId,
        // so
        // we know there will be at least that parameter.
        StringBuilder stringBuilder = new StringBuilder(uriStr);
        for (Map.Entry<String, String> entry : elementNameToValue.entrySet()) {
          // First add the ampersand
          stringBuilder.append("&");
          stringBuilder.append(URLEncoder.encode(entry.getKey(), ApiConstants.UTF8_ENCODE));
          stringBuilder.append("=");
          // We've got to replace the plus with %20, which is what js's
          // decodeURIComponent expects.
          String escapedValue = URLEncoder.encode(
              entry.getValue(),
              ApiConstants.UTF8_ENCODE)
              .replace("+", URL_ENCODED_SPACE);
          stringBuilder.append(escapedValue);
        }
        uriStr = stringBuilder.toString();
      }
    } catch (UnsupportedEncodingException e) {
      WebLogger.getLogger(appName).printStackTrace(e);
      throw new IllegalArgumentException("error escaping URI parameters");
    }
    WebLogger.getLogger(appName).d(TAG, "Survey uriStr: " + uriStr);
    return Uri.parse(uriStr);
  }

  /**
   * TODO: Eventually will launch the Intent with the correct return code.
   * For now,
   * however, just starts it directly without waiting for return.
   *
   * @param activityToAwaitReturn
   * @param surveyAddIntent
   * @param tp
   */
  public static void launchSurveyToAddRow(AbsBaseActivity activityToAwaitReturn, 
      String tableId, Intent surveyAddIntent) {
    activityToAwaitReturn.setActionTableId(tableId);
    activityToAwaitReturn.startActivityForResult(
        surveyAddIntent,
        Constants.RequestCodes.ADD_ROW_SURVEY);
  }

  /**
   * Add a row with Survey. Convenience method for calling
   * {@link #getIntentForOdkSurveyAddRow(Context, String, String,
   *  SurveyFormParameters, Map)} followed by
   *  {@link #launchSurveyToAddRow(Activity, String, Intent)}.
   * 
   * @param activity activity to await activity return
   * @param appName
   * @param tableId
   * @param surveyFormParameters
   * @param prepopulatedValues values you want to prepopulate the form with.
   * Should be element key to value.
   */
  public static void addRowWithSurvey(
      AbsBaseActivity activity,
      String appName,
      String tableId,
      SurveyFormParameters surveyFormParameters,
      Map<String, String> prepopulatedValues) {
    Intent addRowIntent = SurveyUtil.getIntentForOdkSurveyAddRow(
        activity,
        appName, 
        tableId,
        surveyFormParameters,
        prepopulatedValues);
    SurveyUtil.launchSurveyToAddRow(activity, tableId, addRowIntent);
  }

  /**
   * Launch survey to edit a row. Convenience method for calling
   * {@link #getIntentForOdkSurveyEditRow(Context, String, String,
   *  SurveyFormParameters, String)} followed by
   * {@link #launchSurveyToEditRow(Activity, Intent, String, String)}.
   * 
   * @param activity activity to await the return of the launch
   * @param appName
   * @param tableId
   * @param instanceId id of the row to edit
   * @param surveyFormParameters
   */
  public static void editRowWithSurvey(
      AbsBaseActivity activity,
      String appName,
      String tableId,
      String instanceId,
      SurveyFormParameters surveyFormParameters) {
    Intent editRowIntent = SurveyUtil.getIntentForOdkSurveyEditRow(
        activity,
        appName, 
        tableId,
        surveyFormParameters,
        instanceId);
    SurveyUtil.launchSurveyToEditRow(
        activity,
        tableId,
        editRowIntent,
        instanceId);
  }

  /**
   * TODO: eventually launch with the correct return code. For now, just starts
   * the activity without waiting for the return.
   *
   * @param activityToAwaitReturn
   * @param surveyEditIntent
   * @param tp
   */
  public static void launchSurveyToEditRow(AbsBaseActivity activityToAwaitReturn, 
                                           String tableId,
                                           Intent surveyEditIntent,
                                           String rowId) {
    activityToAwaitReturn.setActionTableId(tableId);
    activityToAwaitReturn.startActivityForResult(
        surveyEditIntent,
        Constants.RequestCodes.EDIT_ROW_SURVEY);
  }

  /**
   * Houses parameters for a Survey form.
   *
   * @author sudar.sam@gmail.com
   *
   */
  public static class SurveyFormParameters {

    /**
     * The app-wide unique id for the form. Must obey standard Survey rules for
     * form ids. This includes beginning with a letter and followed by only
     * letters, "_", or 0-9.
     */
    private String mFormId;
    /**
     * The screen path specifying where to start within a form. Can be null,
     * and will imply to start at the beginning of the form.
     */
    private String mScreenPath;
    /**
     * Flag indicating whether this form has been user-defined. False indicates
     * that it is NOT custom, and has been generated by Tables based on the
     * table definition. True indicates that the form IS custom and has been
     * created by a user.
     */
    private boolean mIsUserDefined;

    @SuppressWarnings("unused")
    private SurveyFormParameters() {
      // including this in case it needs to be serialized to json.
    }

    public SurveyFormParameters(boolean isUserDefined, String formId, String screenPath) {
      this.mIsUserDefined = isUserDefined;
      this.mFormId = formId;
      this.mScreenPath = screenPath;
    }

    public String getFormId() {
      return this.mFormId;
    }

    public void setFormId(String formId) {
      this.mIsUserDefined = true;
      this.mFormId = formId;
    }

    public String getScreenPath() {
      return this.mScreenPath;
    }

    public void setScreenPath(String screenPath) {
      this.mIsUserDefined = true;
      this.mScreenPath = screenPath;
    }

    public boolean isUserDefined() {
      return this.mIsUserDefined;
    }

    public void setIsUserDefined(boolean isUserDefined) {
      this.mIsUserDefined = isUserDefined;
    }

    /**
     * Construct a SurveyFormParameters object from the given tableId.
     * The object is determined to have custom parameters if a formId can be
     * retrieved from the tableId. Otherwise the default addrow
     * parameters are set.
     * <p>
     * The display name of the row will be the display name of the table.
     *
     * @param tp
     * @return
     * @throws RemoteException 
     */
    public static SurveyFormParameters constructSurveyFormParameters(
        Context context, String appName, String tableId) throws RemoteException {
      String formId;
      OdkDbHandle db = null;
      try {
        db = Tables.getInstance().getDatabase().openDatabase(appName, false);
        KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName,
            db, tableId, SurveyUtil.KVS_PARTITION);
        KeyValueHelper aspectHelper = kvsh.getAspectHelper(SurveyUtil.KVS_ASPECT);
        formId = aspectHelper.getString(SurveyUtil.KEY_FORM_ID);
      } finally {
        if ( db != null ) {
          Tables.getInstance().getDatabase().closeDatabase(appName, db);
        }
      }
      if (formId == null) {
        return new SurveyFormParameters(false, getDefaultAddRowFormId(tableId), null);
      }
      // Else we know it is custom.
      return new SurveyFormParameters(true, formId, null);
    }

    public void persist(String appName, OdkDbHandle db, String tableId) throws RemoteException {
      KeyValueStoreHelper kvsh = new KeyValueStoreHelper(Tables.getInstance(), appName, db, 
          tableId, SurveyUtil.KVS_PARTITION);
      KeyValueHelper aspectHelper = kvsh.getAspectHelper(SurveyUtil.KVS_ASPECT);
      if (this.isUserDefined()) {
        aspectHelper.setString(SurveyUtil.KEY_FORM_ID, this.mFormId);
      } else {
        aspectHelper.removeKey(SurveyUtil.KEY_FORM_ID);
      }
    }
  }

}
