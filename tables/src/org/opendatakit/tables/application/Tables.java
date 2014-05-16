/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.tables.application;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.R;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.util.Log;
import fi.iki.elonen.SimpleWebServer;

public class Tables extends Application {

  public static final String t = "Tables";

  private Set<String> appNameHasBeenInitialized = new HashSet<String>();
  private SimpleWebServer server = null;
  private volatile Thread webServer = null;

  private static Tables singleton = null;

  public static Tables getInstance() {
    return singleton;
  }

  private synchronized void startServer() {
    if (server == null || !server.isAlive()) {
      stopServer();
      SimpleWebServer testing = new SimpleWebServer();
      try {
        testing.start();
        server = testing;
      } catch (IOException e) {
        Log.e("Tables.Thread.WebServer", "Exception: " + e.toString());
      }
    }
  }

  private synchronized void stopServer() {
    if (server != null) {
      try {
        server.stop();
      } catch (Exception e) {
        // ignore...
      }
      server = null;
    }
  }

  public boolean shouldRunInitializationTask(String appName) {
    return !appNameHasBeenInitialized.contains(appName);
  }

  public void clearRunInitializationTask(String appName) {
    appNameHasBeenInitialized.add(appName);
  }

  public String getVersionCodeString() {
    try {
      PackageInfo pinfo;
      pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
      int versionNumber = pinfo.versionCode;
      return Integer.toString(versionNumber);
    } catch (NameNotFoundException e) {
      e.printStackTrace();
      return "";
    }
  }

  public String getVersionedAppName() {
    String versionDetail = "";
    try {
      PackageInfo pinfo;
      pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
      int versionNumber = pinfo.versionCode;
      String versionName = pinfo.versionName;
      versionDetail = " " + versionName + "(" + versionNumber + ")";
    } catch (NameNotFoundException e) {
      e.printStackTrace();
    }
    return getString(R.string.app_name) + versionDetail;
  }

  /**
   * Creates required directories on the SDCard (or other external storage)
   *
   * @return true if there are tables present
   * @throws RuntimeException
   *           if there is no SDCard or the directory exists as a non directory
   */
  public static void createODKDirs(String appName) throws RuntimeException {

    ODKFileUtils.verifyExternalStorageAvailability();

    ODKFileUtils.assertDirectoryStructure(appName);
  }

  @Override
  public void onCreate() {
    singleton = this;

    super.onCreate();

    webServer = new Thread(null, new Runnable() {
      @Override
      public void run() {
        Thread mySelf = Thread.currentThread();
        int retryCount = 0;
        for (;webServer == mySelf;) {
          startServer();
          try {
            retryCount++;
            Thread.sleep(1000);
            if ( retryCount % 60 == 0 ) {
              Log.d(t,"Tables.Thread.WebServer -- waking to confirm webserver is working");
            }
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        stopServer();
      }}, "WebServer");
    webServer.start();
  }


  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    Log.i(t, "onConfigurationChanged");
  }

  @Override
  public void onTerminate() {
    Thread tmpThread = webServer;
    webServer = null;
    tmpThread.interrupt();
    try {
      // give it time to drain...
      Thread.sleep(200);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    super.onTerminate();
    Log.i(t, "onTerminate");
  }

}
