package yoonsung.odk.spreadsheet.data;

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
	private static final String SERVER_URI_KEY = "serverUri";
	private static final String ACCOUNT_KEY = "account";
	private static final String AUTH_KEY = "auth";

	public class ViewType {
		public static final int TABLE = 0;
		public static final int LIST = 1;
		public static final int LINE_GRAPH = 2;
		public static final int MAP = 3;
		public static final int COUNT = 4; // the number of types
	}

	private final SharedPreferences prefs;

	public Preferences(Context context) {
		prefs = context.getSharedPreferences(FILE_NAME, 0);
	}

	public String getDefaultTableId() {
		return prefs.getString(DEFAULT_TABLE_KEY, null);
	}

	public void setDefaultTableId(String tableId) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(DEFAULT_TABLE_KEY, tableId);
		editor.commit();
	}

	public int getPreferredViewType(String tableId) {
		return prefs.getInt(PREFERRED_VIEW_TYPE_BASE_KEY + tableId,
				ViewType.TABLE);
	}

	public void setPreferredViewType(String tableId, int type) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(PREFERRED_VIEW_TYPE_BASE_KEY + tableId, type);
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
