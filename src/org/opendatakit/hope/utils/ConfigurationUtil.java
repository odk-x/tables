package org.opendatakit.hope.utils;

import java.io.File;

import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.hope.data.Preferences;

/**
 * Various utilities for initial configuration of tables
 * @author June
 *
 */
public class ConfigurationUtil {

    /**
     * @return true if the config.properties file has been changed since the
     * configuration initialization
     */
	public static boolean isChanged(Preferences prefs) {

		File config = new File( ODKFileUtils.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME),
				TableFileUtils.ODK_TABLES_CONFIG_PROPERTIES_FILENAME);
		if (config.isFile()) {
			long timeLastConfig = prefs.getTimeLastConfig();
			long timeLastMod = config.lastModified();
			if (timeLastMod == timeLastConfig)
				return false;
			else
				return true;
		} else
			return false;
	}

	public static void updateTimeChanged(Preferences prefs, long timeLastConfig) {
		prefs.setLastTimeConfig(timeLastConfig);
	}
}
