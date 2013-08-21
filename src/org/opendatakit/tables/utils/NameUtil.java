/*
 * Copyright (C) 2013 University of Washington
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

import org.opendatakit.tables.data.TableProperties;

import android.util.Log;

/**
 * Methods for dealing with naming conventions.
 * @author sudar.sam@gmail.com
 *
 */
public class NameUtil {
  
  private static final String TAG = NameUtil.class.getSimpleName();
  
  private static final String PATTERN_VALID_USER_DEFINED_DB_NAME = 
      "^[\\p{Lu}\\p{Ll}0-9_]*|^__[\\p{Lu}\\p{Ll}0-9_]*";
  
  private static final String PATTERN_VALID_ADMIN_DB_NAME = 
      "^_[^_][\\p{Lu}\\p{Ll}0-9_]*";
  
  /*
   * For more detail about what this exactly means, see the doc of:
   * org.opendatakit.aggregate.parser.Naming.java.
   */
  private static final String PATTERN_NON_WORD_CHARS = 
      "[^\\p{Lu}\\p{Ll}0-9]";
  
  private static final String REPLACEMENT_CHAR = "_";
  
  /**
   * Replace the non-word characters.
   * @param name
   * @return
   */
  public static String replaceNonWordCharacters(String name) {
    Log.d(TAG, "[replaceNonWordCharacters] " + name + " has become: " +
        name.replaceAll(PATTERN_NON_WORD_CHARS, REPLACEMENT_CHAR));
    return name.replaceAll(PATTERN_NON_WORD_CHARS, REPLACEMENT_CHAR);
  }
  
  /**
   * Determines whether or not the given name is valid for a user-defined 
   * entity in the database. Valid names are determined to not begin with a 
   * single underscore, not to begin with a digit, and to contain only unicode
   * appropriate word characters.
   * @param name
   * @return true if valid else false
   */
  public static boolean isValidUserDefinedDatabaseName(String name) {
    Log.d(TAG, "[isValidUserDefinedDatabaseName] " + name + ": " + 
        name.matches(PATTERN_VALID_USER_DEFINED_DB_NAME));
    return name.matches(PATTERN_VALID_USER_DEFINED_DB_NAME);
  }
  
  /**
   * Returns true if the name is a valid admin column name for the database.
   * Admin columns must begin with a single underscore and be composed of only
   * unicode-aware word characters.
   * @param name
   * @return
   */
  public static boolean isValidAdminDefinedDatabaseName(String name) {
    Log.d(TAG, "[isValidAdminDefinedDatabaseName] " + name + ": " +
        name.matches(PATTERN_VALID_ADMIN_DB_NAME));
    return name.matches(PATTERN_VALID_ADMIN_DB_NAME);
  }
  
  /**
   * Creates a valid user-defined name. No non-word characters, not beginning
   * with a digit.
   * @param name
   * @return
   */
  public static String createValidUserDefinedDatabaseName(String name) {
    String result = replaceNonWordCharacters(name);
    if (result.matches("[0-9].*")) {
      result = "__" + result;
    }
    Log.d(TAG, "[createValidUerDefinedDatabaseName] " + name + ": " + result);
    return result;
  }
    
  /** 
   * Create a valid tableId. The resultant id will return true if passed to
   * {@link isValidUserDefinedDatabaseName} and will return false if passed to 
   * {@link tableIdAlreadyExists} with the same array of 
   * {@link TableProperties}. 
   * @return
   */
  public static String createUniqueTableId(String proposedTableId, 
      TableProperties[] allTableProperties) {
    // a table id is just a user-friendly version of the display name without
    // any word characters.
    String baseName = createValidUserDefinedDatabaseName(proposedTableId);
    if (!tableIdAlreadyExists(baseName, allTableProperties)) {
      return baseName;
    }
    // Otherwise we need to add some suffixes.
    int suffix = 1;
    while (true) {
      String nextName = baseName + suffix;
      if (!tableIdAlreadyExists(nextName, allTableProperties)) {
        if (!isValidUserDefinedDatabaseName(nextName)) {
          throw new IllegalStateException("[createUniqueTableId] invariant" +
          		" violated--about to return: " + nextName + " and it is not" +
          				" a valid user-defined database name.");
        }
        return nextName;
      }
      suffix++;
    }
  }
  
  /**
   * Creates a unique dbTableName. The resultant dbTableName will return true
   * if passed to {@link isValidUserDefinedDatabaseName} and will return false
   * if passed to {@link dbTableNameAlreadyExists} with the same array of 
   * {@link TableProperties}.
   * @param proposedDbTableName
   * @param allTableProperties
   * @return
   */
  public static String createUniqueDbTableName(String proposedDbTableName,
      TableProperties[] allTableProperties) {
    String baseName = createValidUserDefinedDatabaseName(proposedDbTableName);
    if (!dbTableNameAlreadyExists(baseName, allTableProperties)) {
      return baseName;
    }
    // else we need to construct one up.
    int suffix = 1;
    while (true) {
      String nextName = baseName + suffix;
      if (!dbTableNameAlreadyExists(nextName, allTableProperties)) {
        if (!isValidUserDefinedDatabaseName(nextName)) {
          throw new IllegalStateException("[createUniqueDbTableName] " +
          		"invariant violated--about to return: " + nextName 
          		+ " and it is not" +
                    " a valid user-defined database name.");        }
        return nextName;
      }
      suffix++;
    }
  }
  
  
  /**
   * Returns true if the proposedTableId exists in the arary of
   * allTableProperties. It does no checking to ensure that this is a valid
   * table id, nor does it ensure that the passed in array includes all the
   * {@link TableProperties} objects.
   * @param proposedTableId
   * @param allTableProperties
   * @return
   */
  public static boolean tableIdAlreadyExists(String proposedTableId,
      TableProperties[] allTableProperties) {
    for (TableProperties tp : allTableProperties) {
      if (tp.getTableId().equals(proposedTableId)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Returns true if a {@link TableProperties} in the allTableProperties array
   * includes a dbTableName equivalent to proposedDbTableName. Does not check
   * to ensure it is in fact a valid dbTableName or that the allTableProperties
   * array does indeed include all the {@link TableProperties}.
   * @param proposedDbTableName
   * @param allTableProperties
   * @return
   */
  public static boolean dbTableNameAlreadyExists(String proposedDbTableName,
      TableProperties[] allTableProperties) {
    for (TableProperties tp : allTableProperties) {
      if (tp.getDbTableName().equals(proposedDbTableName)) {
        return true;
      }
    }
    return false;
  }
  

}
