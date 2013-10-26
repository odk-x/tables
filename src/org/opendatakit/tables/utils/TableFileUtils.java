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
package org.opendatakit.tables.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.opendatakit.common.android.utilities.ODKFileUtils;

import android.util.Log;

/**
 * This is a general place for utils regarding odktables files. These are files
 * that are associated with various tables, such as html files for different
 * views, etc.
 * @author sudar.sam@gmail.com
 *
 */
public class TableFileUtils {
  
  private static final String TAG = TableFileUtils.class.getSimpleName();

  /** The default app name for ODK Tables */
  public static final String ODK_TABLES_APP_NAME = "tables";
  
  /** Filename for the top-level configuration file */
  public static final String ODK_TABLES_CONFIG_PROPERTIES_FILENAME = "config.properties";

  /** Filename for a csv file used for joining files (?) */
  public static final String ODK_TABLES_JOINING_CSV_FILENAME = "temp.csv";

  /** Timeout (in ms) we specify for each http request */
  public static final int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;

  /** URI from base to get the manifest for a server. */
  public static final String MANIFEST_ADDR_URI = "/tableKeyValueManifest";

  /** The url parameter name of the tableId. */
  public static final String TABLE_ID_PARAM = "tableId";
  
  /** The response type expected from the server for a json object. */
  public static final String RESP_TYPE_JSON = "application/json; charset=utf-8";
    
  /** Name of the metadata directory under the app id. */
  public static final String DIR_METADATA = 
      ODKFileUtils.getAppFolder("tables") + "/metadata";
//      ODKFileUtils.getNameOfMetadataFolder();
  /** Name of the logging directory under the app id. */
  public static final String DIR_LOGGING = 
      ODKFileUtils.getAppFolder("tables") + "/logging";
//      ODKFileUtils.getNameOfLoggingFolder();
  /** Name of the framework directory under the app id. */
  public static final String DIR_FRAMEWORK = 
      ODKFileUtils.getAppFolder("tables") + "/framework";
//      ODKFileUtils.getNameOfFrameworkFolder();
  public static final String DIR_INSTANCES = 
      ODKFileUtils.getAppFolder("tables") + "/instances";
//      ODKFileUtils.getNameOfInstancesFolder();
  
  /** 
   * Name of the directory under the app id where the table-specific stuff 
   * is stored. 
   */
  public static final String DIR_TABLES = "tables";
  
  /**
   * Returns the directories that live under the app id and that should never
   * be synched.
   * @return
   */
  public static Set<String> getUnsynchedDirectories() {
    Set<String> dirs = new HashSet<String>();
    dirs.add(DIR_METADATA);
    dirs.add(DIR_LOGGING);
    return dirs;
  }
  
  /**
   * Get all the files under the given folder, excluding those directories that
   * are the concatenation of folder and a member of excluding. If the member 
   * of excluding is a directory, none of its children will be synched either.
   * <p>
   * If the folder doesn't exist it returns an empty list. 
   * <p>
   * If the file exists but is not a directory, logs an error and returns an
   * empty list.
   * @param folder
   * @param excluding can be null--nothing will be excluded. Should be relative
   * to the given folder.
   * @param relativeTo the path to which the returned paths will be relative.
   * A null value makes them relative to the folder parameter. If it is non 
   * null, folder must start with relativeTo, or else the files in
   * folder could not possibly be relative to relativeTo. In this case will 
   * throw an IllegalArgumentException.
   * @return the relative paths of the files under the folder--i.e. the paths
   * after the folder parameter, not including the first separator
   * @throws IllegalArgumentException if relativeTo is not a substring of 
   * folder. 
   */
  public static List<String> getAllFilesUnderFolder(String folder, 
      Set<String> excluding, String relativeTo) {
    if (excluding == null) {
      excluding = new HashSet<String>();
    }
    if (relativeTo == null) {
      relativeTo = folder;
    } else if (!folder.startsWith(relativeTo)) {
      throw new IllegalArgumentException("relativeTo: " + relativeTo + ", " +
      		" must be a substring of folder: " + folder + ".");
    }
    File baseFolder = new File(folder);
    LinkedList<File> unexploredDirs = new LinkedList<File>();
    if (!baseFolder.exists()) {
      return new ArrayList<String>();
    } else if (!baseFolder.isDirectory()) {
      Log.e(TAG, "[getAllFilesUnderFolder] folder is not a directory: " 
          + folder);
      return new ArrayList<String>();
    }
    unexploredDirs.add(baseFolder);
    List<File> nondirFiles = new ArrayList<File>();
    // we'll use this length for checking exclusion.
    int appFolderLength = folder.length();
    while (!unexploredDirs.isEmpty()) {
      File exploring = unexploredDirs.pop();
      File[] files = exploring.listFiles();
      for (File f : files) {
        // First we'll check to see if we should exclude it.
        // To do this we'll get the relative path.
        // +1 for the separator.
        String relativePath = f.getPath().substring(appFolderLength + 1);
        if (excluding.contains(relativePath)) {
          // Then we do nothing.
          continue;
        } else if (f.isDirectory()) {
          // we'll need to explore it
          unexploredDirs.add(f); 
        } else {
          // we'll add it to our list of files.
          nondirFiles.add(f);
        }
      }
    }
    List<String> relativePaths = new ArrayList<String>();
    // we want the relative path, so drop the necessary bets.
    int relativeFolderLength = relativeTo.length();
    for (File f : nondirFiles) {
      // +1 to exclude the separator.
      relativePaths.add(f.getPath().substring(relativeFolderLength + 1));
    }
    return relativePaths;
  }

}
