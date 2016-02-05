package org.opendatakit.tables.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import org.json.JSONObject;
import org.opendatakit.IntentConsts;
import org.opendatakit.common.android.application.AppAwareApplication;
import org.opendatakit.common.android.application.CommonApplication;
import org.opendatakit.common.android.listener.DatabaseConnectionListener;
import org.opendatakit.common.android.logic.CommonToolProperties;
import org.opendatakit.common.android.logic.DynamicPropertiesCallback;
import org.opendatakit.common.android.logic.PropertiesSingleton;
import org.opendatakit.common.android.logic.PropertyManager;
import org.opendatakit.common.android.utilities.AndroidUtils;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.UrlUtils;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.common.android.views.ExecutorContext;
import org.opendatakit.common.android.views.ExecutorProcessor;
import org.opendatakit.common.android.views.ODKWebView;
import org.opendatakit.database.service.OdkDbInterface;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.views.webkits.TableDataExecutorProcessor;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * @author mitchellsundt@gmail.com
 *
 * Derived classes must implement:
 *   public String getTableId();
 *   public String getInstanceId();
 *   public ODKWebView getWebKitView();
 *   public void databaseAvailable();
 *   public void databaseUnavailable();
 *   public void initializationCompleted();
 */
public abstract class AbsBaseWebActivity extends AbsBaseActivity implements IOdkTablesActivity {
  private static final String t = "AbsBaseWebActivity";

  // tags for retained context
  private static final String DISPATCH_STRING_WAITING_FOR_DATA = "dispatchStringWaitingForData";
  private static final String ACTION_WAITING_FOR_DATA = "actionWaitingForData";

  private static final String SESSION_VARIABLES = "sessionVariables";

  private static final String QUEUED_ACTIONS = "queuedActions";
  private static final String RESPONSE_JSON = "responseJSON";

  private String dispatchStringWaitingForData = null;
  private String actionWaitingForData = null;

  private Bundle sessionVariables = new Bundle();

  private LinkedList<String> queuedActions = new LinkedList<String>();

  LinkedList<String> queueResponseJSON = new LinkedList<String>();

  /**
   * Member variables that do not need to be preserved across orientation
   * changes, etc.
   */

  private DatabaseConnectionListener mIOdkDataDatabaseListener;

  // no need to preserve
  private PropertyManager mPropertyManager;

  public abstract String getTableId();
  public abstract String getInstanceId();
  public abstract ODKWebView getWebKitView();

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    if (dispatchStringWaitingForData != null) {
      outState.putString(DISPATCH_STRING_WAITING_FOR_DATA, dispatchStringWaitingForData);
    }
    if (actionWaitingForData != null) {
      outState.putString(ACTION_WAITING_FOR_DATA, actionWaitingForData);
    }

    outState.putBundle(SESSION_VARIABLES, sessionVariables);

    if ( !queuedActions.isEmpty() ) {
      String[] actionOutcomesArray = new String[queuedActions.size()];
      queuedActions.toArray(actionOutcomesArray);
      outState.putStringArray(QUEUED_ACTIONS, actionOutcomesArray);
    }

    if ( !queueResponseJSON.isEmpty() ) {
      String[] qra = queueResponseJSON.toArray(new String[queueResponseJSON.size()]);
      outState.putStringArray(RESPONSE_JSON, qra);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mPropertyManager = new PropertyManager(this);

    if (savedInstanceState != null) {
      // if we are restoring, assume that initialization has already occurred.

      dispatchStringWaitingForData =
          savedInstanceState.containsKey(DISPATCH_STRING_WAITING_FOR_DATA) ?
              savedInstanceState.getString(DISPATCH_STRING_WAITING_FOR_DATA) : null;
      actionWaitingForData = savedInstanceState.containsKey(ACTION_WAITING_FOR_DATA) ? savedInstanceState
          .getString(ACTION_WAITING_FOR_DATA) : null;

      if (savedInstanceState.containsKey(SESSION_VARIABLES)) {
        sessionVariables = savedInstanceState.getBundle(SESSION_VARIABLES);
      }

      if (savedInstanceState.containsKey(QUEUED_ACTIONS)) {
        String[] actionOutcomesArray = savedInstanceState.getStringArray(QUEUED_ACTIONS);
        queuedActions.clear();
        queuedActions.addAll(Arrays.asList(actionOutcomesArray));
      }

      if (savedInstanceState.containsKey(RESPONSE_JSON)) {
        String[] pendingResponseJSON = savedInstanceState.getStringArray(RESPONSE_JSON);
        queueResponseJSON.clear();
        queueResponseJSON.addAll(Arrays.asList(pendingResponseJSON));
      }
    }
  }

  @Override
  public String getActiveUser() {
    PropertiesSingleton props = CommonToolProperties.get(this, getAppName());

    final DynamicPropertiesCallback cb = new DynamicPropertiesCallback(getAppName(),
        getTableId(), getInstanceId(),
        props.getProperty(CommonToolProperties.KEY_USERNAME),
        props.getProperty(CommonToolProperties.KEY_ACCOUNT));

    String name = mPropertyManager.getSingularProperty(PropertyManager.EMAIL, cb);
    if (name == null || name.length() == 0) {
      name = mPropertyManager.getSingularProperty(PropertyManager.USERNAME, cb);
      if (name != null && name.length() != 0) {
        name = "username:" + name;
      } else {
        name = null;
      }
    } else {
      name = "mailto:" + name;
    }
    return name;
  }

  @Override
  public String getProperty(String propertyId) {
    PropertiesSingleton props = CommonToolProperties.get(this, getAppName());

    final DynamicPropertiesCallback cb = new DynamicPropertiesCallback(getAppName(),
        getTableId(), getInstanceId(),
        props.getProperty(CommonToolProperties.KEY_USERNAME),
        props.getProperty(CommonToolProperties.KEY_ACCOUNT));

    String value = mPropertyManager.getSingularProperty(propertyId, cb);
    return value;
  }

  @Override
  public String getWebViewContentUri() {
    Uri u = UrlUtils.getWebViewContentUri(this);

    String uriString = u.toString();

    // Ensures that the string always ends with '/'
    if (uriString.charAt(uriString.length() - 1) != '/') {
      return uriString + "/";
    } else {
      return uriString;
    }
  }

  @Override
  public void setSessionVariable(String elementPath, String jsonValue) {
    sessionVariables.putString(elementPath, jsonValue);
  }

  @Override
  public String getSessionVariable(String elementPath) {
    return sessionVariables.getString(elementPath);
  }

  /**
   * Invoked from within Javascript to launch an activity.
   *
   * @param dispatchString   Opaque string -- typically identifies prompt and user action
   *
   * @param action
   *          -- the intent to be launched
   * @param valueContentMap
   *          -- parameters to pass to the intent
   *          {
   *            uri: uriValue, // parse to a uri and set as the data of the
   *                           // intent
   *            extras: extrasMap, // added as extras to the intent
   *            package: packageStr, // the name of a package to launch
   *            type: typeStr, // will be set as the type
   *            data: dataUri // will be parsed to a uri and set as the data of
   *                          // the intent. For now this is equivalent to the
   *                          // uri field, although that name is less precise.
   *          }
   */
  @Override
  public String doAction(
      String dispatchString,
      String action,
      JSONObject valueContentMap) {

    // android.os.Debug.waitForDebugger();

    if (isWaitingForBinaryData()) {
      WebLogger.getLogger(getAppName()).w(t, "Already waiting for data -- ignoring");
      return "IGNORE";
    }

    Intent i;
    boolean isCurrentApp = false;
    String currentApp = "org.opendatakit." + ((AppAwareApplication) getApplication()).getToolName();

    boolean isOpendatakitApp = false;
    if (action.startsWith(currentApp)) {
      Class<?> clazz;
      try {
        clazz = Class.forName(action);
        i = new Intent(this, clazz);
        isCurrentApp = true;
      } catch (ClassNotFoundException e) {
        WebLogger.getLogger(getAppName()).printStackTrace(e);
        i = new Intent(action);
      }
    } else {
      i = new Intent(action);
    }

    if (action.startsWith("org.opendatakit.")) {
      isOpendatakitApp = true;
    }

    try {

      String uriKey = "uri";
      String extrasKey = "extras";
      String packageKey = "package";
      String typeKey = "type";
      String dataKey = "data";

      JSONObject valueMap = null;
      if (valueContentMap != null) {

        // do type first, as it says in the spec this call deletes any other
        // data (eg by setData()) on the intent.
        if (valueContentMap.has(typeKey)) {
          String type = valueContentMap.getString(typeKey);
          i.setType(type);
        }

        if (valueContentMap.has(uriKey) || valueContentMap.has(dataKey)) {
          // as it currently stands, the data property can be in either the uri
          // or data keys.
          String uriValueStr = null;
          if (valueContentMap.has(uriKey)) {
            uriValueStr = valueContentMap.getString(uriKey);
          }
          // go ahead and overwrite with data if it's present.
          if (valueContentMap.has(dataKey)) {
            uriValueStr = valueContentMap.getString(dataKey);
          }
          if (uriValueStr != null) {
            Uri uri = Uri.parse(uriValueStr);
            i.setData(uri);
          }
        }

        if (valueContentMap.has(extrasKey)) {
          valueMap = valueContentMap.getJSONObject(extrasKey);
        }

        if (valueContentMap.has(packageKey)) {
          String packageStr = valueContentMap.getString(packageKey);
          i.setPackage(packageStr);
        }

      }

      if (valueMap != null) {
        Bundle b;
        PropertiesSingleton props = CommonToolProperties.get(this, getAppName());

        final DynamicPropertiesCallback cb = new DynamicPropertiesCallback(getAppName(),
            getTableId(), getInstanceId(),
            props.getProperty(CommonToolProperties.KEY_USERNAME),
            props.getProperty(CommonToolProperties.KEY_ACCOUNT));

        b = AndroidUtils.convertToBundle(valueMap, new AndroidUtils.MacroStringExpander() {

          @Override
          public String expandString(String value) {
            if (value != null && value.startsWith("opendatakit-macro(") && value.endsWith(")")) {
              String term = value.substring("opendatakit-macro(".length(), value.length() - 1)
                  .trim();
              String v = mPropertyManager.getSingularProperty(term, cb);
              if (v != null) {
                return v;
              } else {
                WebLogger.getLogger(getAppName()).e(t, "Unable to process opendatakit-macro: " + value);
                throw new IllegalArgumentException(
                    "Unable to process opendatakit-macro expression: " + value);
              }
            } else {
              return value;
            }
          }
        });

        i.putExtras(b);
      }

      if (isOpendatakitApp) {
        // ensure that we supply our appName...
        if (!i.hasExtra(IntentConsts.INTENT_KEY_APP_NAME)) {
          i.putExtra(IntentConsts.INTENT_KEY_APP_NAME, getAppName());
          WebLogger.getLogger(getAppName()).w(t, "doAction into Survey or Tables does not supply an appName. Adding: "
              + getAppName());
        }
      }
    } catch (Exception ex) {
      WebLogger.getLogger(getAppName()).e(t, "JSONException: " + ex.toString());
      WebLogger.getLogger(getAppName()).printStackTrace(ex);
      return "JSONException: " + ex.toString();
    }

    dispatchStringWaitingForData = dispatchString;
    actionWaitingForData = action;

    try {
      startActivityForResult(i, Constants.RequestCodes.LAUNCH_DOACTION);
      return "OK";
    } catch (ActivityNotFoundException ex) {
      WebLogger.getLogger(getAppName()).e(t, "Unable to launch activity: " + ex.toString());
      WebLogger.getLogger(getAppName()).printStackTrace(ex);
      return "Application not found";
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    WebLogger.getLogger(getAppName()).i(t, "onActivityResult");
    ODKWebView view = getWebKitView();

    if (requestCode == Constants.RequestCodes.LAUNCH_DOACTION) {
      try {
        String jsonObject = null;
        Bundle b = (intent == null) ? null : intent.getExtras();
        JSONObject val = (b == null) ? null : AndroidUtils.convertFromBundle(getAppName(), b);
        JSONObject jsonValue = new JSONObject();
        jsonValue.put("status", resultCode);
        if ( val != null ) {
          jsonValue.put("result", val);
        }
        JSONObject result = new JSONObject();
        result.put("dispatchString", dispatchStringWaitingForData);
        result.put("action",  actionWaitingForData);
        result.put("jsonValue", jsonValue);

        String actionOutcome = result.toString();
        this.queueActionOutcome(actionOutcome);

        WebLogger.getLogger(getAppName()).i(t, "Constants.RequestCodes.LAUNCH_DOACTION: " + jsonObject);

        if ( view != null ) {
          view.signalQueuedActionAvailable();
        }
      } catch (Exception e) {
        try {
          JSONObject jsonValue = new JSONObject();
          jsonValue.put("status", 0);
          jsonValue.put("result", e.toString());
          JSONObject result = new JSONObject();
          result.put("dispatchString", dispatchStringWaitingForData);
          result.put("action",  actionWaitingForData);
          result.put("jsonValue", jsonValue);
          this.queueActionOutcome(result.toString());

          if ( view != null ) {
            view.signalQueuedActionAvailable();
          }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      } finally {
        dispatchStringWaitingForData = null;
        actionWaitingForData = null;
      }
    }
    super.onActivityResult(requestCode, resultCode, intent);
  }

  public boolean isWaitingForBinaryData() {
    return actionWaitingForData != null;
  }

  @Override
  public void queueActionOutcome(String outcome) {
    queuedActions.addLast(outcome);
  }

  @Override
  public void queueUrlChange(String hash) {
    try {
      String jsonEncoded = ODKFileUtils.mapper.writeValueAsString(hash);
      queuedActions.addLast(jsonEncoded);
    } catch ( Exception e ) {
      e.printStackTrace();
    }
  }

  @Override
  public String viewFirstQueuedAction() {
    String outcome =
        queuedActions.isEmpty() ? null : queuedActions.getFirst();
    return outcome;
  }

  @Override
  public void removeFirstQueuedAction() {
    if ( !queuedActions.isEmpty() ) {
      queuedActions.removeFirst();
    }
  }

  @Override
  public void signalResponseAvailable(String responseJSON) {
    if ( responseJSON == null ) {
      WebLogger.getLogger(getAppName()).e(t, "signalResponseAvailable -- got null responseJSON!");
    } else {
      WebLogger.getLogger(getAppName()).e(t, "signalResponseAvailable -- got "
          + responseJSON.length() + " long responseJSON!");
    }
    if ( responseJSON != null) {
      this.queueResponseJSON.push(responseJSON);
      final ODKWebView webView = getWebKitView();
      if (webView != null) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            webView.loadUrl("javascript:odkData.responseAvailable();");
          }
        });
      }
    }
  }

  @Override public String getResponseJSON() {
    if ( queueResponseJSON.isEmpty() ) {
      return null;
    }
    String responseJSON = queueResponseJSON.removeFirst();
    return responseJSON;
  }

  @Override public ExecutorProcessor newExecutorProcessor(ExecutorContext context) {
    return new TableDataExecutorProcessor(context);
  }

  @Override public void registerDatabaseConnectionBackgroundListener(
      DatabaseConnectionListener listener) {
    mIOdkDataDatabaseListener = listener;
  }

  @Override public OdkDbInterface getDatabase() {
    return ((CommonApplication) getApplication()).getDatabase();
  }

  @Override public Bundle getIntentExtras() {
    return this.getIntent().getExtras();
  }

  @Override
  public void databaseAvailable() {
    super.databaseAvailable();

    if ( mIOdkDataDatabaseListener != null ) {
      mIOdkDataDatabaseListener.databaseAvailable();
    }
  }

  @Override
  public void databaseUnavailable() {
    super.databaseUnavailable();

    if ( mIOdkDataDatabaseListener != null ) {
      mIOdkDataDatabaseListener.databaseUnavailable();
    }
  }
}
