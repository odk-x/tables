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

import org.opendatakit.common.android.application.CommonApplication;
import org.opendatakit.tables.R;

public class Tables extends CommonApplication {

  public static final String t = "Tables";

  private static Tables singleton = null;

  public static Tables getInstance() {
    return singleton;
  }

  @Override
  public void onCreate() {
    singleton = this;

    super.onCreate();
  }

  @Override
  public int getApkDisplayNameResourceId() {
    return R.string.app_name;
  }

  @Override
  public int getConfigZipResourceId() {
    return R.raw.configzip;
  }

  @Override
  public int getSystemZipResourceId() {
    return R.raw.systemzip;
  }

  @Override
  public int getWebKitResourceId() {
    return -1;
  }

  public String getVersionedAppName() {
    String versionDetail = this.getVersionDetail();
    return getString(R.string.app_name) + versionDetail;
  }
}
