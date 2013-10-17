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

import java.util.TreeSet;

import org.opendatakit.tables.data.TableProperties;

import android.util.Log;

/**
 * Methods for dealing with naming conventions.
 * @author sudar.sam@gmail.com
 *
 */
public class NameUtil {

  private static final String TAG = NameUtil.class.getSimpleName();

  /**
   * Because Android content provider internals do not quote the
   * column field names when constructing SQLite queries, we need
   * to either prevent the user from using SQLite keywords or code
   * up our own mapping code for Android. Rather than bloat our
   * code, we restrict the set of keywords the user can use.
   *
   * To this list, we add our metadata element names. This further
   * simplifies references to these fields, as we can just consider
   * them to be hidden during display and non-modifiable by the
   * UI, but accessible by the end user (though perhaps not mutable).
   *
   * Fortunately, the server code properly quotes all column and
   * table names, so we only have to track the SQLite reserved names
   * and not all MySQL or PostgreSQL reserved names.
   */
  private static final TreeSet<String> reservedNames;

  static {
    reservedNames = new TreeSet<String>();

    /**
     * ODK Metadata reserved names
     */
    reservedNames.add("SAVEPOINT_TIMESTAMP");
    reservedNames.add("SAVEPOINT_TYPE");
    reservedNames.add("FORM_ID");
    reservedNames.add("LOCALE");

    /**
     * SQLite keywords ( http://www.sqlite.org/lang_keywords.html )
     */
    reservedNames.add("ABORT");
    reservedNames.add("ACTION");
    reservedNames.add("ADD");
    reservedNames.add("AFTER");
    reservedNames.add("ALL");
    reservedNames.add("ALTER");
    reservedNames.add("ANALYZE");
    reservedNames.add("AND");
    reservedNames.add("AS");
    reservedNames.add("ASC");
    reservedNames.add("ATTACH");
    reservedNames.add("AUTOINCREMENT");
    reservedNames.add("BEFORE");
    reservedNames.add("BEGIN");
    reservedNames.add("BETWEEN");
    reservedNames.add("BY");
    reservedNames.add("CASCADE");
    reservedNames.add("CASE");
    reservedNames.add("CAST");
    reservedNames.add("CHECK");
    reservedNames.add("COLLATE");
    reservedNames.add("COLUMN");
    reservedNames.add("COMMIT");
    reservedNames.add("CONFLICT");
    reservedNames.add("CONSTRAINT");
    reservedNames.add("CREATE");
    reservedNames.add("CROSS");
    reservedNames.add("CURRENT_DATE");
    reservedNames.add("CURRENT_TIME");
    reservedNames.add("CURRENT_TIMESTAMP");
    reservedNames.add("DATABASE");
    reservedNames.add("DEFAULT");
    reservedNames.add("DEFERRABLE");
    reservedNames.add("DEFERRED");
    reservedNames.add("DELETE");
    reservedNames.add("DESC");
    reservedNames.add("DETACH");
    reservedNames.add("DISTINCT");
    reservedNames.add("DROP");
    reservedNames.add("EACH");
    reservedNames.add("ELSE");
    reservedNames.add("END");
    reservedNames.add("ESCAPE");
    reservedNames.add("EXCEPT");
    reservedNames.add("EXCLUSIVE");
    reservedNames.add("EXISTS");
    reservedNames.add("EXPLAIN");
    reservedNames.add("FAIL");
    reservedNames.add("FOR");
    reservedNames.add("FOREIGN");
    reservedNames.add("FROM");
    reservedNames.add("FULL");
    reservedNames.add("GLOB");
    reservedNames.add("GROUP");
    reservedNames.add("HAVING");
    reservedNames.add("IF");
    reservedNames.add("IGNORE");
    reservedNames.add("IMMEDIATE");
    reservedNames.add("IN");
    reservedNames.add("INDEX");
    reservedNames.add("INDEXED");
    reservedNames.add("INITIALLY");
    reservedNames.add("INNER");
    reservedNames.add("INSERT");
    reservedNames.add("INSTEAD");
    reservedNames.add("INTERSECT");
    reservedNames.add("INTO");
    reservedNames.add("IS");
    reservedNames.add("ISNULL");
    reservedNames.add("JOIN");
    reservedNames.add("KEY");
    reservedNames.add("LEFT");
    reservedNames.add("LIKE");
    reservedNames.add("LIMIT");
    reservedNames.add("MATCH");
    reservedNames.add("NATURAL");
    reservedNames.add("NO");
    reservedNames.add("NOT");
    reservedNames.add("NOTNULL");
    reservedNames.add("NULL");
    reservedNames.add("OF");
    reservedNames.add("OFFSET");
    reservedNames.add("ON");
    reservedNames.add("OR");
    reservedNames.add("ORDER");
    reservedNames.add("OUTER");
    reservedNames.add("PLAN");
    reservedNames.add("PRAGMA");
    reservedNames.add("PRIMARY");
    reservedNames.add("QUERY");
    reservedNames.add("RAISE");
    reservedNames.add("REFERENCES");
    reservedNames.add("REGEXP");
    reservedNames.add("REINDEX");
    reservedNames.add("RELEASE");
    reservedNames.add("RENAME");
    reservedNames.add("REPLACE");
    reservedNames.add("RESTRICT");
    reservedNames.add("RIGHT");
    reservedNames.add("ROLLBACK");
    reservedNames.add("ROW");
    reservedNames.add("SAVEPOINT");
    reservedNames.add("SELECT");
    reservedNames.add("SET");
    reservedNames.add("TABLE");
    reservedNames.add("TEMP");
    reservedNames.add("TEMPORARY");
    reservedNames.add("THEN");
    reservedNames.add("TO");
    reservedNames.add("TRANSACTION");
    reservedNames.add("TRIGGER");
    reservedNames.add("UNION");
    reservedNames.add("UNIQUE");
    reservedNames.add("UPDATE");
    reservedNames.add("USING");
    reservedNames.add("VACUUM");
    reservedNames.add("VALUES");
    reservedNames.add("VIEW");
    reservedNames.add("VIRTUAL");
    reservedNames.add("WHEN");
    reservedNames.add("WHERE");

    }

  private static final String PATTERN_VALID_USER_DEFINED_DB_NAME =
      "^\\p{L}\\p{M}*(\\p{L}\\p{M}*|\\p{Nd}|_)*$";

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
    boolean matchHit = name.matches(PATTERN_VALID_USER_DEFINED_DB_NAME);
    // TODO: uppercase is bad...
    boolean reserveHit = reservedNames.contains(name.toUpperCase());
    Log.d(TAG, "[isValidUserDefinedDatabaseName] " + name + ": " +
        (!reserveHit && matchHit));
    return (!reserveHit && matchHit);
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
      result = "n" + result;
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
                    " a valid user-defined database name.");
        }
        return nextName;
      }
      suffix++;
    }
  }

  /**
   * Return a valid element key for the given table. Ensure to be unique.
   * The resultant key will return true if passed to
   * {@link isValidUserDefinedDatabaseName} and false if passed to
   * {@link elementKeyAlreadyExists}.
   * @param proposedDisplayName
   * @param tp
   * @return
   */
  public static String createUniqueElementKey(String proposedElementKey,
      TableProperties tp) {
    String baseName = createValidUserDefinedDatabaseName(proposedElementKey);
    if (!elementKeyAlreadyExists(baseName, tp)) {
      return baseName;
    }
    // else we need to make one.
    int suffix = 1;
    while (true) {
      String nextName = baseName + suffix;
      if (!elementKeyAlreadyExists(nextName, tp)) {
        if (!isValidUserDefinedDatabaseName(nextName)) {
            throw new IllegalStateException("[createUniqueDbTableName] " +
                 "invariant violated--about to return: " + nextName
                 + " and it is not" +
                      " a valid user-defined database name.");
        }
        return nextName;
      }
      suffix++;
    }
  }

  public static String createUniqueElementName(String proposedElementName,
      TableProperties tp) {
    String baseName = createValidUserDefinedDatabaseName(proposedElementName);
    if (!elementNameAlreadyExists(baseName, tp)) {
      return baseName;
    }
    // else we need to make one.
    int suffix = 1;
    while (true) {
      String nextName = baseName + suffix;
      if (!elementNameAlreadyExists(nextName, tp)) {
        if (!isValidUserDefinedDatabaseName(nextName)) {
          throw new IllegalStateException("[createUniqueDbTableName] " +
               "invariant violated--about to return: " + nextName
               + " and it is not" +
                    " a valid user-defined database name.");
        }
        return nextName;
      }
      suffix++;
    }
  }

  /**
   * Returns true if and only if the elementKey already exists in the table
   * specified by tp.
   * @param elementKey
   * @param tp
   * @return
   */
  public static boolean elementKeyAlreadyExists(String elementKey,
      TableProperties tp) {
    return (tp.getColumnByElementKey(elementKey) != null);
  }

  /**
   * Returns true if and only if the elementName already exists in the table
   * specified by tp.
   * @param elementName
   * @param tp
   * @return
   */
  public static boolean elementNameAlreadyExists(String elementName,
      TableProperties tp) {
    return (tp.getColumnByElementName(elementName) != null);
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
