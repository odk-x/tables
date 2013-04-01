package org.opendatakit.tables.util;

import java.io.File;

import org.opendatakit.tables.data.Preferences;

import android.os.Environment;

/**
 * Various utilities for initial configuration of tables
 * @author June
 *
 */
public class ConfigurationUtil {
	private static String root = Environment.getExternalStorageDirectory().getPath();
	private static String filepath = "/odk/tables/config.properties";
	
    /**
     * @return true if the config.properties file has been changed since the 
     * configuration initialization 
     */
	public static boolean isChanged(Preferences prefs) {
		
		File config = new File(root + filepath);
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
