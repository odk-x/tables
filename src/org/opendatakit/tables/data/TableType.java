package org.opendatakit.tables.data;

/**
 * The type of a table. Data is user data. Security is access control stuff.
 * Shortcut is sms shortcuts.
 * <p>
 * The lower case names of constants are for javascript reasons and the ability
 * to simply call TableType.valueOf("data") when you read an object from the 
 * database.
 * @author sudar.sam@gmail.com
 *
 */
public enum TableType {
  data,
  security,
  shortcut;

}
