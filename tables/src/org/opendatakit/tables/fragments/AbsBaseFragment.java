/*
 * Copyright (C) 2014 University of Washington
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
package org.opendatakit.tables.fragments;

import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.webkitserver.WebkitServerConsts;
import org.opendatakit.webkitserver.service.OdkWebkitServerInterface;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

/**
 * Base class that all fragments should extend.
 * 
 * @author sudar.sam@gmail.com
 *
 */
public abstract class AbsBaseFragment extends Fragment implements ServiceConnection {

  private static final String LOGTAG = "AbsBaseFragment";

  private OdkWebkitServerInterface webkitfilesService = null;
  private boolean isDestroying = false;

  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (!(activity instanceof AbsBaseActivity)) {
      throw new IllegalStateException(AbsBaseFragment.class.getSimpleName()
          + " must be attached to an " + AbsBaseActivity.class.getSimpleName());
    }

    bindToService();
  }

  private void bindToService() {
    if (!this.isRemoving() && !isDestroying && webkitfilesService == null
        && this instanceof IWebFragment && getActivity() != null) {
      Log.i(LOGTAG, "Attempting bind to WebServer service");
      Intent bind_intent = new Intent();
      bind_intent.setClassName(WebkitServerConsts.WEBKITSERVER_SERVICE_PACKAGE, 
                               WebkitServerConsts.WEBKITSERVER_SERVICE_CLASS);
      getActivity().bindService(bind_intent, this, Context.BIND_AUTO_CREATE
          | ((Build.VERSION.SDK_INT >= 14) ? Context.BIND_ADJUST_WITH_ACTIVITY : 0));
    }
  }

  /**
   * Get the name of the app this fragment is operating under.
   * 
   * @return
   */
  protected String getAppName() {
    // we know this will succeed because of the check in onAttach
    AbsBaseActivity activity = (AbsBaseActivity) getActivity();
    return activity.getAppName();
  }

  @Override
  public void onHiddenChanged(boolean hidden) {
    super.onHiddenChanged(hidden);
    if (!hidden) {
      bindToService();
    }
  }

  @Override
  public void onDestroy() {
    Log.i(LOGTAG, "onDestroy - Releasing WebServer service");
    super.onDestroy();
    isDestroying = true;
    webkitfilesService = null;
    getActivity().unbindService(this);
  }

  @Override
  public void onServiceConnected(ComponentName className, IBinder service) {
    Log.i(LOGTAG, "Bound to WebServer service");
    webkitfilesService = OdkWebkitServerInterface.Stub.asInterface(service);
  }

  @Override
  public void onServiceDisconnected(ComponentName arg0) {
    if (isDestroying) {
      Log.i(LOGTAG, "Unbound from WebServer service (intentionally)");
    } else {
      Log.w(LOGTAG, "Unbound from WebServer service (unexpected)");
    }
    webkitfilesService = null;
    // the bindToService() method decides whether to connect or not...
    bindToService();
  }

}
