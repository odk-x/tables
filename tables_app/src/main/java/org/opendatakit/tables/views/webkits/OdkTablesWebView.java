package org.opendatakit.tables.views.webkits;

import android.content.Context;
import android.util.AttributeSet;
import org.opendatakit.common.android.activities.IOdkDataActivity;
import org.opendatakit.common.android.views.ODKWebView;
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
    tables = new OdkTables(activity, activity.getTableId());
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
    String baseUrl = ((IOdkTablesActivity) getContext()).getUrlBaseLocation(
        hasPageFrameworkFinishedLoading() && getLoadPageUrl() != null );

    if ( baseUrl != null ) {
      resetLoadPageStatus(baseUrl);

      loadUrl(baseUrl);
    } else if ( hasPageFrameworkFinishedLoading() ) {
      log.w(t, "loadPage: framework was loaded -- but no URL -- don't load anything!");
    } else {
      log.w(t, "loadPage: framework did not load -- cannot load anything!");
    }

  }

  @Override public void clearPage() {
    log.i(t, "clearPage: current loadPageUrl: " + getLoadPageUrl());
    String baseUrl = ((IOdkTablesActivity) getContext()).getUrlBaseLocation(false);

    if ( baseUrl != null ) {
      resetLoadPageStatus(baseUrl);
      log.i(t, "clearPage: full reload: " + baseUrl);
      loadUrl(baseUrl);
    } else {
      log.w(t, "clearPage: framework did not load -- cannot load anything!");
    }

  }
}
