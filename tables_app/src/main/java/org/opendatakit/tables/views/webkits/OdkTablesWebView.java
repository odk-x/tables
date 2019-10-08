package org.opendatakit.tables.views.webkits;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import org.opendatakit.tables.activities.IOdkTablesActivity;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.views.ODKWebView;

/**
 * @author mitchellsundt@gmail.com
 */
public class OdkTablesWebView extends ODKWebView {
   // Used for logging
   private static final String TAG = OdkTablesWebView.class.getSimpleName();

   /**
    * IGNORE THE WARNINGS
    * This has to be a class property, or it will get garbage collected while the javascript is
    * still trying to use it
    */
   @SuppressWarnings("FieldCanBeLocal") private OdkTables tables;

   /**
    * Constructs a new WebView for use with tables
    *
    * @param context Used to construct an OdkTables object
    * @param attrs   unused
    */
  /* Some information about the warning we're about to ignore
   * "For applications built for API levels below 17, WebView#addJavascriptInterface presents a
   * security hazard as JavaScript on the target web page has the ability to use reflection to
   * access the injected object's public fields and thus manipulate the host application in
   * unintended ways."
   * https://labs.mwrinfosecurity.com/blog/2013/09/24/
   * webview-addjavascriptinterface-remote-code-execution/
   */
   @SuppressLint("AddJavascriptInterface") public OdkTablesWebView(Context context,
       AttributeSet attrs) {
      super(context, attrs);
      //setLayerType(View.LAYER_TYPE_SOFTWARE, null);
      // stomp on the odkTablesIf object...
      //noinspection ThisEscapedInObjectConstruction -- We're already in a stable state here
      tables = new OdkTables(context, this);
      addJavascriptInterface(tables.getJavascriptInterfaceWithWeakReference(),
          Constants.JavaScriptHandles.ODK_TABLES_IF);
   }

   @Override public boolean hasPageFramework() {
      return false;
   }

   /**
    * IMPORTANT: This function should only be called with the context of the database listeners
    * OR if called from elsewhere there should be an if statement before invoking that checks
    * if the database is currently available.
    */
   @Override public void reloadPage() {

      log.i(TAG, "reloadPage: current loadPageUrl: " + getLoadPageUrl());
      String baseUrl = ((IOdkTablesActivity) getOdkContext())
          .getUrlBaseLocation(false, getContainerFragmentID());

      if (baseUrl != null) {
         loadPageOnUiThread(baseUrl, getContainerFragmentID());
      } else {
         log.w(TAG, "reloadPage: framework did not load -- cannot load anything!");
      }

   }
}
