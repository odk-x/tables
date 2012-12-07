package org.opendatakit.tables.data;

/**
 * Just a struct to hold the type and value of an entry from the key value 
 * store. It should be used in a Map<String, TypeValuePair>. An Entry of this
 * map would then form a (Key, Type, Value) tuple. It exists to give allow
 * correct parsing of the value without requiring prior knowledge about what
 * the type should be, and to allow easy throwing of exceptions if something
 * is not right.
 * @author sudar.sam@gmail.com
 *
 */
public class TypeValuePair {
  
  public String type;
  // The value is a String representation of the value. The type this value
  // should be interpreted as is given by the type field.
  public String value;
  
  public TypeValuePair(String type, String value) {
    this.type = type;
    this.value = value;
  }

}
