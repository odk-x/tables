package org.opendatakit.tables.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import org.json.JSONObject;
import org.opendatakit.consts.RequestCodeConsts;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.listener.DatabaseConnectionListener;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.properties.CommonToolProperties;
import org.opendatakit.properties.DynamicPropertiesCallback;
import org.opendatakit.properties.PropertyManager;
import org.opendatakit.tables.application.Tables;
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
  // used for logging
  private static final String TAG = AbsBaseWebActivity.class.getSimpleName();

  // tags for retained context
  private static final String DISPATCH_STRING_WAITING_FOR_DATA = "dispatchStringWaitingForData";
  private static final String ACTION_WAITING_FOR_DATA = "actionWaitingForData";

  private static final String SESSION_VARIABLES = "sessionVariables";

  private static final String QUEUED_ACTIONS = "queuedActions";
  private static final String RESPONSE_JSON_MAIN = "responseJSON_main";
  private static final String RESPONSE_JSON_SUBLIST = "responseJSON_sublist";

  /**
   * With the advent of the split screen detail-with-sublist view, we need to
   * guard access to the data and result queues and session variable data structures.
   *
   * The queued data response lists need to be separated into response streams for each webkit
   * that is active. We currently can have either one or two (detail-with-sublist) active.
   *
   * The dispatchString, action, and queuedActions (action results) are guarded only
   * to ensure that they are updated concurrently and consistently. Results are expected
   * to be read from the primary webkit (e.g., the detail webkit).
   *
   * The session variables bundle is a complex data structure and is guarded to ensure the two
   * webkits don't attempt to manipulate it at the same time (it is unclear if the webkit threads
   * will be the same thread or different).  Session variables are shared across the webkits.
   */
  private final Object guardCachedContent = new Object();
  private LinkedList<String> guardedQueueResponseJSON_main = new LinkedList<>();
  private LinkedList<String> guardedQueueResponseJSON_sublist = new LinkedList<>();
  private String guardedDispatchStringWaitingForData = null;
  private String guardedActionWaitingForData = null;
  private LinkedList<String> guardedQueuedActions = new LinkedList<>();
  private Bundle guardedSessionVariables = new Bundle();
  /**
   * Member variables that do not need to be preserved across orientation
   * changes, etc.
   */

  private DatabaseConnectionListener mIOdkDataDatabaseListener;

  // no need to preserve
  private PropertyManager mPropertyManager;

  public abstract String getInstanceId();

  /**
   * Gets the active webkit view
   *
   * @param fragmentID The id for the webkit in the view heirarchy, if there are multiple
   * @return the webkit if it was found, or else null
   */
  public abstract ODKWebView getWebKitView(String fragmentID);

  /**
   * We need to save whether we were waiting for data (a json string), our session variables, our
   * queued actions and the queue response.
   *
   * @param outState the state to be saved
   */
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    synchronized (guardCachedContent) {
      if (guardedDispatchStringWaitingForData != null) {
        outState.putString(DISPATCH_STRING_WAITING_FOR_DATA, guardedDispatchStringWaitingForData);
      }
      if (guardedActionWaitingForData != null) {
        outState.putString(ACTION_WAITING_FOR_DATA, guardedActionWaitingForData);
      }

      outState.putBundle(SESSION_VARIABLES, guardedSessionVariables);

      if (!guardedQueuedActions.isEmpty()) {
        String[] actionOutcomesArray = new String[guardedQueuedActions.size()];
        guardedQueuedActions.toArray(actionOutcomesArray);
        outState.putStringArray(QUEUED_ACTIONS, actionOutcomesArray);
      }

      if (!guardedQueueResponseJSON_main.isEmpty()) {
        String[] qra = guardedQueueResponseJSON_main.toArray(new String[guardedQueueResponseJSON_main.size()]);
        outState.putStringArray(RESPONSE_JSON_MAIN, qra);
      }

      if (!guardedQueueResponseJSON_sublist.isEmpty()) {
        String[] qra = guardedQueueResponseJSON_sublist.toArray(new String[guardedQueueResponseJSON_sublist.size()]);
        outState.putStringArray(RESPONSE_JSON_SUBLIST, qra);
      }
    }
  }

  /**
   * Pulls out the things we saved earlier, including whether we were waiting for data, our
   * session variables, our queued actions and the queued response
   *
   * @param savedInstanceState the state we saved in onSaveInstanceState
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mPropertyManager = new PropertyManager(this);

    synchronized(guardCachedContent) {
      if (savedInstanceState != null) {
        // if we are restoring, assume that initialization has already occurred.

        guardedDispatchStringWaitingForData = savedInstanceState.containsKey
            (DISPATCH_STRING_WAITING_FOR_DATA) ?
            savedInstanceState.getString(DISPATCH_STRING_WAITING_FOR_DATA) :
            null;
        guardedActionWaitingForData = savedInstanceState.containsKey(ACTION_WAITING_FOR_DATA) ?
            savedInstanceState.getString(ACTION_WAITING_FOR_DATA) :
            null;

        if (savedInstanceState.containsKey(SESSION_VARIABLES)) {
          guardedSessionVariables = savedInstanceState.getBundle(SESSION_VARIABLES);
        }

        if (savedInstanceState.containsKey(QUEUED_ACTIONS)) {
          String[] actionOutcomesArray = savedInstanceState.getStringArray(QUEUED_ACTIONS);
          guardedQueuedActions.clear();
          if (actionOutcomesArray != null) {
            guardedQueuedActions.addAll(Arrays.asList(actionOutcomesArray));
          }
        }

        if (savedInstanceState.containsKey(RESPONSE_JSON_MAIN)) {
          String[] pendingResponseJSON = savedInstanceState.getStringArray(RESPONSE_JSON_MAIN);
          guardedQueueResponseJSON_main.clear();
          if (pendingResponseJSON != null) {
            guardedQueueResponseJSON_main.addAll(Arrays.asList(pendingResponseJSON));
          }
        }

        if (savedInstanceState.containsKey(RESPONSE_JSON_SUBLIST)) {
          String[] pendingResponseJSON = savedInstanceState.getStringArray(RESPONSE_JSON_SUBLIST);
          guardedQueueResponseJSON_sublist.clear();
          if (pendingResponseJSON != null) {
            guardedQueueResponseJSON_sublist.addAll(Arrays.asList(pendingResponseJSON));
          }
        }
      }
    }
  }

  /**
   * Tries to pull the active user from the database
   *
   * @return the active user according to the database, or anonymous if the database is down
   */
  @Override
  public String getActiveUser() {
    try {
      return getDatabase().getActiveUser(mAppName);
    } catch (ServicesAvailabilityException e) {
      WebLogger.getLogger(mAppName).printStackTrace(e);
      return CommonToolProperties.ANONYMOUS_USER;
    }
  }

  /**
   * Tries to retrieve a property from the PropertyManager, if possible
   *
   * @param propertyId the property to get
   * @return the value of the property
   */
  @Override
  public String getProperty(String propertyId) {
    final DynamicPropertiesCallback cb = new DynamicPropertiesCallback(getAppName(), getTableId(),
        getInstanceId(), getActiveUser(), mProps.getUserSelectedDefaultLocale());

    return mPropertyManager.getSingularProperty(propertyId, cb);
  }

  /**
   * Gets the URI of the current webview
   *
   * @return the URI of the current web view
   */
  @Override
  public String getWebViewContentUri() {
    Uri u = UrlUtils.getWebViewContentUri();

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
    synchronized (guardCachedContent) {
      guardedSessionVariables.putString(elementPath, jsonValue);
    }
  }

  @Override
  public String getSessionVariable(String elementPath) {
    synchronized (guardCachedContent) {
      return guardedSessionVariables.getString(elementPath);
    }
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
      WebLogger.getLogger(getAppName()).w(TAG, "Already waiting for data -- ignoring");
      return "IGNORE";
    }

    Intent i = DoActionUtils
        .buildIntent(this, mPropertyManager, dispatchStructAsJSONstring, action, valueContentMap);

    if (i == null) {
      return "JSONException";
    }

    synchronized (guardCachedContent) {
      guardedDispatchStringWaitingForData = dispatchStructAsJSONstring;
      guardedActionWaitingForData = action;
    }

    try {
      startActivityForResult(i, RequestCodeConsts.RequestCodes.LAUNCH_DOACTION);
      return "OK";
    } catch (ActivityNotFoundException ex) {
      WebLogger.getLogger(getAppName()).e(TAG, "Unable to launch activity: " + ex);
      WebLogger.getLogger(getAppName()).printStackTrace(ex);
      return "Application not found";
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    WebLogger.getLogger(getAppName()).i(TAG, "onActivityResult");
    ODKWebView view = getWebKitView(null);

    if (requestCode == RequestCodeConsts.RequestCodes.LAUNCH_DOACTION) {
      String dispatchString;
      String action;
      synchronized (guardCachedContent) {
        // save persisted values into a local variable
        dispatchString = guardedDispatchStringWaitingForData;
        action = guardedActionWaitingForData;

        // clear the persisted values
        guardedDispatchStringWaitingForData = null;
        guardedActionWaitingForData = null;
      }
      // DoActionUtils may invoke queueActionOutcome (gaining the lock
      // and adding the response to the queued actions) and, if it does,
      // it will then also signal the view that there are responses available.
      DoActionUtils
          .processActivityResult(this, view, resultCode, intent,
              dispatchString,
              action);
    }
    super.onActivityResult(requestCode, resultCode, intent);
  }

  public boolean isWaitingForBinaryData() {
    synchronized (guardCachedContent) {
      return guardedActionWaitingForData != null;
    }
  }

  @Override
  public void queueActionOutcome(String outcome) {
    synchronized (guardCachedContent) {
      guardedQueuedActions.addLast(outcome);
    }
  }

  @Override
  public void queueUrlChange(String hash) {
    try {
      String jsonEncoded = ODKFileUtils.mapper.writeValueAsString(hash);
      synchronized (guardCachedContent) {
        guardedQueuedActions.addLast(jsonEncoded);
      }
    } catch (Exception e) {
      WebLogger.getLogger(getAppName()).printStackTrace(e);
    }
  }

  @Override
  public String viewFirstQueuedAction() {
    synchronized (guardCachedContent) {
      return guardedQueuedActions.isEmpty() ? null : guardedQueuedActions.getFirst();
    }
  }

  @Override
  public void removeFirstQueuedAction() {
    synchronized (guardCachedContent) {
      if (!guardedQueuedActions.isEmpty()) {
        guardedQueuedActions.removeFirst();
      }
    }
  }

  @Override
  public void signalResponseAvailable(String responseJSON, String fragmentID) {
    if (responseJSON == null) {
      WebLogger.getLogger(getAppName()).e(TAG, "signalResponseAvailable -- got null responseJSON!");
    } else {
      WebLogger.getLogger(getAppName()).e(TAG,
          "signalResponseAvailable -- got " + responseJSON.length() + " long responseJSON!");
    }

    if (responseJSON != null) {
      synchronized (guardCachedContent) {
        if (fragmentID != null && Constants.FragmentTags.DETAIL_WITH_LIST_LIST.equals(fragmentID)) {
          this.guardedQueueResponseJSON_sublist.push(responseJSON);
        } else {
          this.guardedQueueResponseJSON_main.push(responseJSON);
        }
      }
      final ODKWebView webView = getWebKitView(fragmentID);
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
  public String getResponseJSON(String fragmentID) {
    synchronized (guardCachedContent) {
      if (fragmentID != null && Constants.FragmentTags.DETAIL_WITH_LIST_LIST.equals(fragmentID)) {
        if (guardedQueueResponseJSON_sublist.isEmpty()) {
          return null;
        }
        return guardedQueueResponseJSON_sublist.removeFirst();
      } else {
        if (guardedQueueResponseJSON_main.isEmpty()) {
          return null;
        }
        return guardedQueueResponseJSON_main.removeFirst();
      }
    }
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
    return Tables.getInstance().getDatabase();
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
