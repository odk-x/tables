/*
 * Copyright (C) 2012 University of Washington
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
package org.opendatakit.common.android.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * A class for managing preferences.
 *
 * @author hkworden
 */
public class Preferences {

	private static final String FILE_NAME = "odktables_preferences";
	private static final String DEFAULT_TABLE_KEY = "defaultTable";
	private static final String PREFERRED_VIEW_TYPE_BASE_KEY = "viewType-";
	private static final String FONT_SIZE = "fontSize";
	private static final String SERVER_URI_KEY = "serverUri";
	private static final String ACCOUNT_KEY = "account";
	private static final String AUTH_KEY = "auth";
	private static final String TIME_LAST_CONFIG = "timeLastConfig";
	private static final String USE_HOME_SCREEN_KEY = "useHomeScreen";

	private static final int DEFAULT_FONT_SIZE = 16;

	private final String appName;

	private final SharedPreferences prefs;

	public Preferences(Context context, String appName) {
	   this.appName = appName;
		prefs = context.getSharedPreferences(FILE_NAME, 0);
	}

	public String getAppName() {
	  return appName;
	}

	/**
	 * Get true if the app has been configured to use a custom homescreen, else
	 * false.
	 * @return
	 */
	public boolean getUseHomeScreen() {
	  return prefs.getBoolean(USE_HOME_SCREEN_KEY, false);
	}

	/**
	 * Persist whether or not a user-defined home screen should be used.
	 * @param useHomeScreen
	 */
	public void setUseHomeScreen(boolean useHomeScreen) {
	  SharedPreferences.Editor editor = prefs.edit();
	  editor.putBoolean(USE_HOME_SCREEN_KEY, useHomeScreen);
	  editor.commit();
	}

	public String getDefaultTableId() {
		return prefs.getString(DEFAULT_TABLE_KEY, null);
	}

	public void setDefaultTableId(String tableId) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(DEFAULT_TABLE_KEY, tableId);
		editor.commit();
	}

	public TableViewType getPreferredViewType(String tableId) {
		return TableViewType.getViewTypeFromId(
		    prefs.getInt(PREFERRED_VIEW_TYPE_BASE_KEY + tableId,
				TableViewType.Spreadsheet.getId()));
	}

	public void setPreferredViewType(String tableId, TableViewType type) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(PREFERRED_VIEW_TYPE_BASE_KEY + tableId, type.getId());
		editor.commit();
	}

	public int getFontSize() {
	    return prefs.getInt(FONT_SIZE, DEFAULT_FONT_SIZE);
	}

	public void setFontSize(int fontSize) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(FONT_SIZE, fontSize);
        editor.commit();
	}

	/**
	 * Return the last time Tables was configured
	 */
	public long getTimeLastConfig() {
		return prefs.getLong(TIME_LAST_CONFIG, 0);
	}

	/**
	 * Updates lastTimeConfig
	 * @param long timeLastConfig
	 */
	public void setLastTimeConfig(long timeLastConfig) {
		SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(TIME_LAST_CONFIG, timeLastConfig);
        editor.commit();
	}

	public void setServerUri(String serverUri) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(SERVER_URI_KEY, serverUri);
		editor.commit();
	}

	public String getServerUri() {
		return prefs.getString(SERVER_URI_KEY, null);
	}

	public void setAccount(String accountName) {
	  SharedPreferences.Editor editor = prefs.edit();
	  editor.putString(ACCOUNT_KEY, accountName);
	  editor.commit();
	}

	public String getAccount() {
	  return prefs.getString(ACCOUNT_KEY, null);
	}

	public void setAuthToken(String authToken) {
	  SharedPreferences.Editor editor = prefs.edit();
	  editor.putString(AUTH_KEY, authToken);
	  editor.commit();
	}

	public String getAuthToken() {
	  return prefs.getString(AUTH_KEY, null);
	}

	public void clearTablePreferences(String tableId) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.remove(PREFERRED_VIEW_TYPE_BASE_KEY + tableId);
		editor.commit();
	}
}
