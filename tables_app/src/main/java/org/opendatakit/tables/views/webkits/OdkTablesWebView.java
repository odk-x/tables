package org.opendatakit.tables.views.webkits;

import android.content.Context;
import android.util.AttributeSet;
import org.opendatakit.tables.activities.AbsBaseWebActivity;
import org.opendatakit.tables.activities.IOdkTablesActivity;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.views.ODKWebView;

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
     tables = new OdkTables(activity, this);
     addJavascriptInterface(tables.getJavascriptInterfaceWithWeakReference(),
         Constants.JavaScriptHandles.CONTROL);
  }

  @Override public boolean hasPageFramework() {
    return false;
  }

   /**
    *  IMPORTANT: This function should only be called with the context of the database listeners
    *  OR if called from elsewhere there should be an if statement before invoking that checks
    *  if the database is currently available.
    */
  @Override public void loadPage() {

    /**
     * NOTE: Reload the web framework only if it has changed.
     */

    log.i(t, "loadPage: current loadPageUrl: " + getLoadPageUrl());
    String baseUrl = ((IOdkTablesActivity) getContext()).getUrlBaseLocation(
        hasPageFrameworkFinishedLoading() && getLoadPageUrl() != null, getContainerFragmentID());

    if ( baseUrl != null ) {
      loadPageOnUiThread(baseUrl, getContainerFragmentID(), false);
    } else if ( hasPageFrameworkFinishedLoading() ) {
      log.w(t, "loadPage: framework was loaded -- but no URL -- don't load anything!");
    } else {
      log.w(t, "loadPage: framework did not load -- cannot load anything!");
    }

  }

   /**
    *  IMPORTANT: This function should only be called with the context of the database listeners
    *  OR if called from elsewhere there should be an if statement before invoking that checks
    *  if the database is currently available.
    */
   @Override public void reloadPage() {

    log.i(t, "reloadPage: current loadPageUrl: " + getLoadPageUrl());
    String baseUrl = ((IOdkTablesActivity) getContext()).getUrlBaseLocation(false,
            getContainerFragmentID());

    if ( baseUrl != null ) {
      loadPageOnUiThread(baseUrl, getContainerFragmentID(), true);
    } else {
      log.w(t, "reloadPage: framework did not load -- cannot load anything!");
    }

  }
}
