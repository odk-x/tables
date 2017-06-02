package org.opendatakit.tables.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import org.json.JSONObject;
import org.opendatakit.application.CommonApplication;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.listener.DatabaseConnectionListener;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.properties.CommonToolProperties;
import org.opendatakit.properties.DynamicPropertiesCallback;
import org.opendatakit.properties.PropertyManager;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.views.webkits.TableDataExecutorProcessor;
import org.opendatakit.utilities.ODKFileUtils;
import org.opendatakit.views.ExecutorContext;
import org.opendatakit.views.ExecutorProcessor;
import org.opendatakit.views.ODKWebView;
import org.opendatakit.webkitserver.utilities.DoActionUtils;
import org.opendatakit.webkitserver.utilities.UrlUtils;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * @author mitchellsundt@gmail.com
 *         <p>
 *         Derived classes must implement:
 *         public String getTableId();
 *         public String getInstanceId();
 *         public ODKWebView getWebKitView();
 *         public void databaseAvailable();
 *         public void databaseUnavailable();
 *         public void initializationCompleted();
 */
public abstract class AbsBaseWebActivity extends AbsTableActivity implements IOdkTablesActivity {
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

  public abstract String getInstanceId();

  public abstract ODKWebView getWebKitView(String viewID);

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

    if (!queuedActions.isEmpty()) {
      String[] actionOutcomesArray = new String[queuedActions.size()];
      queuedActions.toArray(actionOutcomesArray);
      outState.putStringArray(QUEUED_ACTIONS, actionOutcomesArray);
    }

    if (!queueResponseJSON.isEmpty()) {
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

      dispatchStringWaitingForData = savedInstanceState
          .containsKey(DISPATCH_STRING_WAITING_FOR_DATA) ?
          savedInstanceState.getString(DISPATCH_STRING_WAITING_FOR_DATA) :
          null;
      actionWaitingForData = savedInstanceState.containsKey(ACTION_WAITING_FOR_DATA) ?
          savedInstanceState.getString(ACTION_WAITING_FOR_DATA) :
          null;

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
    try {
      return getDatabase().getActiveUser(mAppName);
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(mAppName).printStackTrace(e);
      return CommonToolProperties.ANONYMOUS_USER;
    }
  }

  @Override
  public String getProperty(String propertyId) {
    final DynamicPropertiesCallback cb = new DynamicPropertiesCallback(getAppName(), getTableId(),
        getInstanceId(), getActiveUser(), mProps.getUserSelectedDefaultLocale(),
        mProps.getProperty(CommonToolProperties.KEY_USERNAME),
        mProps.getProperty(CommonToolProperties.KEY_ACCOUNT));

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
   * <p>
   * See interface for argument spec.
   *
   * @return "OK" if successfully launched intent
   */
  @Override
  public String doAction(String dispatchStructAsJSONstring, String action,
      JSONObject valueContentMap) {

    // android.os.Debug.waitForDebugger();

    if (isWaitingForBinaryData()) {
      WebLogger.getLogger(getAppName()).w(t, "Already waiting for data -- ignoring");
      return "IGNORE";
    }

    Intent i = DoActionUtils
        .buildIntent(this, mPropertyManager, dispatchStructAsJSONstring, action, valueContentMap);

    if (i == null) {
      return "JSONException";
    }

    dispatchStringWaitingForData = dispatchStructAsJSONstring;
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
    ODKWebView view = getWebKitView(null);

    if (requestCode == Constants.RequestCodes.LAUNCH_DOACTION) {
      try {
        DoActionUtils
            .processActivityResult(this, view, resultCode, intent, dispatchStringWaitingForData,
                actionWaitingForData);
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
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public String viewFirstQueuedAction() {
    String outcome = queuedActions.isEmpty() ? null : queuedActions.getFirst();
    return outcome;
  }

  @Override
  public void removeFirstQueuedAction() {
    if (!queuedActions.isEmpty()) {
      queuedActions.removeFirst();
    }
  }

  @Override
  public void signalResponseAvailable(String responseJSON, String viewID) {
    if (responseJSON == null) {
      WebLogger.getLogger(getAppName()).e(t, "signalResponseAvailable -- got null responseJSON!");
    } else {
      WebLogger.getLogger(getAppName())
          .e(t, "signalResponseAvailable -- got " + responseJSON.length() + " long responseJSON!");
    }
    if (responseJSON != null) {
      this.queueResponseJSON.push(responseJSON);
      final ODKWebView webView = getWebKitView(viewID);
      if (webView != null) {
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            webView.signalResponseAvailable();
          }
        });
      }
    }
  }

  @Override
  public String getResponseJSON() {
    if (queueResponseJSON.isEmpty()) {
      return null;
    }
    String responseJSON = queueResponseJSON.removeFirst();
    return responseJSON;
  }

  @Override
  public ExecutorProcessor newExecutorProcessor(ExecutorContext context) {
    return new TableDataExecutorProcessor(context, this);
  }

  @Override
  public void registerDatabaseConnectionBackgroundListener(DatabaseConnectionListener listener) {
    mIOdkDataDatabaseListener = listener;
  }

  @Override
  public UserDbInterface getDatabase() {
    return ((CommonApplication) getApplication()).getDatabase();
  }

  @Override
  public Bundle getIntentExtras() {
    return this.getIntent().getExtras();
  }

  @Override
  public void databaseAvailable() {
    super.databaseAvailable();

    if (mIOdkDataDatabaseListener != null) {
      mIOdkDataDatabaseListener.databaseAvailable();
    }
  }

  @Override
  public void databaseUnavailable() {
    super.databaseUnavailable();

    if (mIOdkDataDatabaseListener != null) {
      mIOdkDataDatabaseListener.databaseUnavailable();
    }
  }
}
