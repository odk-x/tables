/*
 * Copyright (C) 2015 University of Washington
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

package org.opendatakit.tables.views.webkits;

import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import org.json.JSONObject;
import org.opendatakit.common.android.logic.CommonToolProperties;
import org.opendatakit.common.android.logic.DynamicPropertiesCallback;
import org.opendatakit.common.android.logic.PropertiesSingleton;
import org.opendatakit.common.android.logic.PropertyManager;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.UrlUtils;
import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.activities.AbsBaseActivity;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by clarice on 11/3/15.
 */
public class Common {

   protected AbsBaseActivity mActivity;
   //private final WebLogger log;

   // no need to preserve
   private PropertyManager mPropertyManager;

   public CommonIf getJavascriptInterfaceWithWeakReference() {
      return new CommonIf(this);
   }

   /**
    * This construct requires an activity rather than a context because we want
    * to be able to launch intents for result rather than merely launch them on
    * their own.
    *
    * @param activity
    *          the activity that will be holding the view
    * @throws RemoteException
    */
   public Common(AbsBaseActivity activity) throws RemoteException {
      this.mActivity = activity;

      //log = WebLogger.getLogger(this.mActivity.getAppName());

      // Is this the best place for the
      // property manager creation to reside?
      mPropertyManager = new PropertyManager(this.mActivity);
   }

   /**
    * @see {@link CommonIf#getPlatformInfo()}
    * @return
    */
   public String getPlatformInfo() {
      String appName = mActivity.getAppName();
      // This is based on:
      // org.opendatakit.survey.android.views.ODKShimJavascriptCallback
      Map<String, String> platformInfo = new HashMap<String, String>();
      platformInfo.put(PlatformInfoKeys.VERSION, Build.VERSION.RELEASE);
      platformInfo.put(PlatformInfoKeys.CONTAINER, "Android");
      platformInfo.put(PlatformInfoKeys.APP_NAME, appName);
      platformInfo.put(PlatformInfoKeys.BASE_URI, getBaseContentUri());
      platformInfo.put(PlatformInfoKeys.LOG_LEVEL, "D");
      JSONObject jsonObject = new JSONObject(platformInfo);
      String result = jsonObject.toString();
      return result;
   }

   /**
    * @see {@link CommonIf#getFileAsUrl(String)}
    * @param relativePath
    * @return
    */
   public String getFileAsUrl(String relativePath) {
      String baseUri = getBaseContentUri();
      String result = baseUri + relativePath;
      return result;
   }

   /**
    * @see {@link CommonIf#getRowFileAsUrl(String, String, String)}
    * @param tableId
    * @param rowId
    * @param rowPathUri
    * @return
    */
   public String getRowFileAsUrl(String tableId, String rowId, String rowPathUri) {
      String appName = mActivity.getAppName();
      String baseUri = getBaseContentUri();
      File rowpathFile = ODKFileUtils.getRowpathFile(appName, tableId, rowId, rowPathUri);
      String uriFragment = ODKFileUtils.asUriFragment(appName, rowpathFile);
      return baseUri + uriFragment;
   }

   /**
    * @see {@link CommonIf#log()}
    * @return
    */
   public void log(String level, String loggingString) {
      char l = (level == null) ? 'I' : level.charAt(0);
      switch (l) {
      case 'A':
         WebLogger.getLogger(this.mActivity.getAppName()).a("shim", loggingString);
         break;
      case 'D':
         WebLogger.getLogger(this.mActivity.getAppName()).d("shim", loggingString);
         break;
      case 'E':
         WebLogger.getLogger(this.mActivity.getAppName()).e("shim", loggingString);
         break;
      case 'I':
         WebLogger.getLogger(this.mActivity.getAppName()).i("shim", loggingString);
         break;
      case 'S':
         WebLogger.getLogger(this.mActivity.getAppName()).s("shim", loggingString);
         break;
      case 'V':
         WebLogger.getLogger(this.mActivity.getAppName()).v("shim", loggingString);
         break;
      case 'W':
         WebLogger.getLogger(this.mActivity.getAppName()).w("shim", loggingString);
         break;
      default:
         WebLogger.getLogger(this.mActivity.getAppName()).i("shim", loggingString);
         break;
      }
   }

   /**
    * @see {@link CommonIf#getProperty()}
    * @return
    */
   public String getProperty(String propertyId) {
      log("I", "getProperty(" + propertyId + ")");

      //return mActivity.getProperty(propertyId);

      // Current Survey implementation - differences are setting tableId and instanceId to null
      // for the cb

      //FormIdStruct form = getCurrentForm();
      PropertiesSingleton props = CommonToolProperties.get(this.mActivity.getApplicationContext(), this.mActivity
          .getAppName());
      final DynamicPropertiesCallback cb = new DynamicPropertiesCallback(this.mActivity.getAppName(),
          null, null,
          props.getProperty(CommonToolProperties.KEY_USERNAME),
          props.getProperty(CommonToolProperties.KEY_ACCOUNT));

      String value = mPropertyManager.getSingularProperty(propertyId, cb);
      return value;
   }

   /**
    * @see {@link CommonIf#getBaseUrl()}
    * @return
    */
   public String getBaseUrl() {
      return ODKFileUtils.getRelativeSystemPath();
   }

   /**
    * Return the base uri for the Tables app name with a trailing separator.
    *
    * @return
    */
   private String getBaseContentUri() {
      String appName = mActivity.getAppName();
      Uri contentUri = UrlUtils.getWebViewContentUri(this.mActivity);
      contentUri = Uri.withAppendedPath(contentUri, Uri.encode(appName));
      return contentUri.toString() + "/";
   }


   /**
    * The keys for the platformInfo json object.
    *
    * @author sudar.sam@gmail.com
    *
    */
   private static class PlatformInfoKeys {
      public static final String CONTAINER = "container";
      public static final String VERSION = "version";
      public static final String APP_NAME = "appName";
      public static final String BASE_URI = "baseUri";
      public static final String LOG_LEVEL = "logLevel";
   }
}
