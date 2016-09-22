package org.opendatakit.tables.views.webkits;

import android.content.Context;
import android.os.Looper;
import android.util.AttributeSet;
import org.opendatakit.activities.IOdkDataActivity;
import org.opendatakit.views.ODKWebView;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.AbsBaseWebActivity;
import org.opendatakit.tables.activities.IOdkTablesActivity;
import org.opendatakit.tables.utils.Constants;

/**
 * @author mitchellsundt@gmail.com
 */
public class OdkTablesWebView extends ODKWebView {
  private static final String t = "OdkTablesWebView";

  private OdkTables tables;

  public OdkTablesWebView(Context context, AttributeSet attrs) {
    super(context, attrs);

    AbsBaseWebActivity activity = (AbsBaseWebActivity) context;

    // stomp on the odkTables object...
    tables = new OdkTables(activity, this, activity.getTableId());
    addJavascriptInterface(tables.getJavascriptInterfaceWithWeakReference(),
        Constants.JavaScriptHandles.CONTROL);
  }

  @Override public boolean hasPageFramework() {
    return false;
  }

  @Override public void loadPage() {
    /**
     * NOTE: Reload the web framework only if it has changed.
     */

    if ( ((IOdkDataActivity) getContext()).getDatabase() == null ) {
      // do not initiate reload until we have the database set up...
      return;
    }

    log.i(t, "loadPage: current loadPageUrl: " + getLoadPageUrl());
    final String baseUrl = ((IOdkTablesActivity) getContext()).getUrlBaseLocation(
        hasPageFrameworkFinishedLoading() && getLoadPageUrl() != null );

    if ( baseUrl != null ) {
      resetLoadPageStatus(baseUrl);

      // Ensure that this is run on the UI thread
      if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
        post(new Runnable() {
          public void run() {
            loadUrl(baseUrl);
          }
        });
      } else {
        loadUrl(baseUrl);
      }

    } else if ( hasPageFrameworkFinishedLoading() ) {
      log.w(t, "loadPage: framework was loaded -- but no URL -- don't load anything!");
    } else {
      log.w(t, "loadPage: framework did not load -- cannot load anything!");
    }

  }

  @Override public void reloadPage() {
    log.i(t, "reloadPage: current loadPageUrl: " + getLoadPageUrl());
    final String baseUrl = ((IOdkTablesActivity) getContext()).getUrlBaseLocation(false);

    if ( baseUrl != null ) {
      if ( hasPageFrameworkFinishedLoading() || !baseUrl.equals(getLoadPageUrl()) ) {
        resetLoadPageStatus(baseUrl);
        log.i(t, "reloadPage: full reload: " + baseUrl);

        // Ensure that this is run on the UI thread
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
          post(new Runnable() {
            public void run() {
              loadUrl(baseUrl);
            }
          });
        } else {
          loadUrl(baseUrl);
        }
      } else {
        log.w(t, "reloadPage: framework in process of loading -- ignoring request!");
      }
    } else {
      log.w(t, "reloadPage: framework did not load -- cannot load anything!");
    }

  }
}
