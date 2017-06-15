package org.opendatakit.tables.views.webkits;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import org.opendatakit.logging.WebLogger;
import org.opendatakit.tables.activities.AbsBaseWebActivity;
import org.opendatakit.tables.activities.IOdkTablesActivity;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.views.ODKWebView;

/**
 * @author mitchellsundt@gmail.com
 */
public class OdkTablesWebView extends ODKWebView {
  // Used for logging
  private static final String TAG = OdkTablesWebView.class.getSimpleName();

  // IGNORE THE WARNINGS
  // This has to be a class property, or it will get garbage collected while the javascript is
  // still trying to use it
  private OdkTables tables;

  public OdkTablesWebView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    AbsBaseWebActivity activity = (AbsBaseWebActivity) context;
    // stomp on the odkTablesIf object...
    tables = new OdkTables(activity, this);
    addJavascriptInterface(tables.getJavascriptInterfaceWithWeakReference(),
        Constants.JavaScriptHandles.ODK_TABLES_IF);
  }

  @Override
  public boolean hasPageFramework() {
    return false;
  }

  /**
   * IMPORTANT: This function should only be called with the context of the database listeners
   * OR if called from elsewhere there should be an if statement before invoking that checks
   * if the database is currently available.
   */
  @Override
  public void loadPage() {

    /**
     * NOTE: Reload the web framework only if it has changed.
     */

    log.i(TAG, "loadPage: current loadPageUrl: " + getLoadPageUrl());
    String baseUrl = ((IOdkTablesActivity) getContext())
        .getUrlBaseLocation(hasPageFrameworkFinishedLoading() && getLoadPageUrl() != null,
            getContainerFragmentID());

    if (baseUrl != null) {
      loadPageOnUiThread(baseUrl, getContainerFragmentID(), false);
    } else {
      if (!hasPageFrameworkFinishedLoading()) {
        log.w(TAG, "Page framework hasn't finished loading, can't load!");
      } else if (!hasPageFramework()) {
        log.w(TAG, "No page framework and baseUrl is null - can't load!");
      }
    }

  }

  /**
   * IMPORTANT: This function should only be called with the context of the database listeners
   * OR if called from elsewhere there should be an if statement before invoking that checks
   * if the database is currently available.
   */
  @Override
  public void reloadPage() {

    log.i(TAG, "reloadPage: current loadPageUrl: " + getLoadPageUrl());
    String baseUrl = ((IOdkTablesActivity) getContext())
        .getUrlBaseLocation(false, getContainerFragmentID());

    if (baseUrl != null) {
      loadPageOnUiThread(baseUrl, getContainerFragmentID(), true);
    } else {
      log.w(TAG, "reloadPage: framework did not load -- cannot load anything!");
    }

  }
}
