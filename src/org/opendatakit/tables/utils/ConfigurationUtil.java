package org.opendatakit.tables.utils;

import java.io.File;

import org.opendatakit.common.android.data.Preferences;
import org.opendatakit.common.android.utils.TableFileUtils;

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

		if (TableFileUtils.tablesConfigurationFileExists(prefs.getAppName())) {
		  File configFile =
		      new File(TableFileUtils.getTablesConfigurationFile(prefs.getAppName()));
			long timeLastConfig = prefs.getTimeLastConfig();
			long timeLastMod = configFile.lastModified();
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
