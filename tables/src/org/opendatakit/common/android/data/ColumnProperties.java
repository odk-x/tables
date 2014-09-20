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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.opendatakit.aggregate.odktables.rest.KeyValueStoreConstants;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.common.android.data.KeyValueStoreHelper.AspectHelper;
import org.opendatakit.common.android.database.DataModelDatabaseHelper;
import org.opendatakit.common.android.provider.ColumnDefinitionsColumns;
import org.opendatakit.common.android.utilities.DataHelper;
import org.opendatakit.common.android.utilities.NameUtil;
import org.opendatakit.common.android.utilities.ODKDataUtils;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.common.android.utilities.ODKFileUtils;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * A class for accessing and managing column properties.
 * <p>
 * Column properties are located in several places. The more, although not
 * completely, immutable properties are located in a table that is defined in
 * {@link ColumnDefinition}. The mutable, mostly ODK Tables-specific columns
 * are located in {@link KeyValueStoreColumn}. ColumnProperties is this an
 * abstraction of both of these.
 * <p>
 * It is the column analogue of {@link TableProperties}.
 * <p>
 * In the future, it might make sense to have TableProperties and
 * ColumnProperties share a common parent class, as they essentially have the
 * same functionality. Same thing for TableDefinitions and ColumnDefinitions.
 *
 * @author hkworden@gmail.com (Hilary Worden)
 * @author sudar.sam@gmail.com
 */
public class ColumnProperties {

  private static final String TAG = "ColumnProperties";

  // the name of the column properties table in the database
  // private static final String DB_TABLENAME = "column_definitions";
  // names of columns in the column properties table
  // private static final String DB_TABLE_ID = "table_id";
  // display attributes

  // public static final String DB_ELEMENT_KEY =
  // "element_key";// (was DB_DB_COLUMN_NAME)
  /*
   * (was dbColumnName) unique id for this element. There should be only one
   * such elementKey for a given tableId. This is the dbColumnName if it is a
   * value written into the database
   */
  // private static final String DB_ELEMENT_NAME = "elementName";
  /*
   * name for this element. Either the elementKey or the name of the element
   * within its enclosing composite type (element name within a struct). This is
   * therefore not unique within a table row, as there could be multiple entries
   * with 'latitude' as their element names.
   */
  // private static final String DB_ELEMENT_TYPE =
  // "element_type"; // (was DB_COL_TYPE)
  /* This is a string value. see larger comment below */
  // private static final String DB_LIST_CHILD_ELEMENT_KEYS =
  // "list_child_element_keys";
  /*
   * These two columns have been replaced by the single column DB_JOINS
   */
  // private static final String DB_JOINS = "joins";
  // private static final String DB_JOIN_TABLE_ID = "joinTableId"; /* tableId of
  // table to join against */
  // private static final String DB_JOIN_ELEMENT_KEY = "joinElementKey"; // (was
  // DB_JOIN_COLUMN_NAME)
  //
  // javascript side:
  //
  //  _joins: { type: 'array', items: {
  //    type: 'object',
  //    listChildElementKeys: [],
  //    properties: {
  //        "table_id": { type: "string", isNotNullable: false,
  //                       isUnitOfRetention: false, elementKey: 'table_id', elementSet: 'columnMetadata' },
  //        "element_key": { type: "string", isNotNullable: false,
  //                       isUnitOfRetention: false, elementKey: 'elementKey', elementSet: 'columnMetadata' } } },
  //    isNotNullable: false, isUnitOfRetention: true, elementPath: 'joins', elementSet: 'columnMetadata' }
  //
  /*
   * elementKey of the value to join (this table's element) against in other
   * table
   */
  /*
   * Data types and how things are stored:
   *
   * We have these primitive elementTypes: STRING INTEGER DECIMAL DATE DATETIME
   * TIME BOOLEAN MIMEURI and this composite type: MULTIPLE_CHOICE for multiple
   * choice options (arrays). These could hold any data type, but initial
   * implementation is only for STRING
   *
   * Anything else is user-specified strings that are used to identify
   * struct-like datatype definitions. Initially, this would be 'geopoint';
   * Tables would add 'phonenumber', 'date range'
   *
   * e.g., multiple-choice list of symptoms:
   *
   * The data is stored under the 'patientSymptoms' column in the database as a
   * JSON encoding of string values. i.e., '["ache","fever"]'
   *
   * ekey: patientSymptoms ename: patientSymptoms etype: MULTIPLE_CHOICE
   * listChildEKeys: '[ patientSymptomsItem ]' isUnitOfRetention: true
   *
   * ekey: patientSymptomsItem ename: null // elements are not named within this
   * list etype: STRING listChildEKeys: null isUnitOfRetention: false
   *
   * ------------- e.g., geopoint defining a northernmost point of something:
   *
   * The data is stored as 4 columns, 'northLatitude', 'northLongitude',
   * 'northAltitude', 'northAccuracy'
   *
   * ekey: northernmostPoint ename: northernmostPoint etype: geopoint
   * listChildEKeys: '[ "northLatitude", "northLongitude", "northAltitude",
   * "northAccuracy"]' isUnitOfRetention: false
   *
   * ekey: northLatitude ename: latitude etype: DECIMAL listChildEKeys: null
   * isUnitOfRetention: true
   *
   * ekey: northLongitude ename: longitude etype: DECIMAL listChildEKeys: null
   * isUnitOfRetention: true
   *
   * ekey: northAltitude ename: altitude etype: DECIMAL listChildEKeys: null
   * isUnitOfRetention: true
   *
   * ekey: northAccuracy ename: accuracy etype: DECIMAL listChildEKeys: null
   * isUnitOfRetention: true
   *
   * ODK Collect can do calculations and constraint tests like
   * 'northermostPoint.altitude < 4.0'
   *
   * e.g., 'clientPhone' as a phonenumber type, which is just a restriction on a
   * STRING value that is stored under 'clientPhone' column in database (it is
   * its own unit of retention).
   *
   * ekey: clientPhone ename: clientPhone etype: phoneNumber listChildEKeys: [
   * "clientPhoneNumber" ] // single element isUnitOfRetention: true
   *
   * ekey: clientPhoneNumber ename: null // null -- indicates restriction on
   * etype etype: STRING listChildEKeys: null isUnitOfRetention: false
   *
   * e.g., 'image' file capture in ODK Collect. Stored as a MIMEURI
   *
   * ekey: locationImage ename: locationImage etype: MIMEURI listChildEKeys:
   * null isUnitOfRetention: true
   *
   * MIMEURI stores a JSON object:
   *
   * '{"uriFragment":"tables/tableId/instances/instanceId/2342.jpg","mimetype":"image/jpg"}'
   *
   * i.e., ODK Collect image/audio/video capture store everything as a MIMEURI
   * with different mimetype values.
   */

  /*
   *
   * NOTE: you can have a composite type stored in two ways: (1) store the leaf
   * nodes of the composite type in the database. Describe the entire type
   * hierarchy down to those leaf nodes. (2) store it as a json object at the
   * top level (isUnitOfRetention == true) and describe the structure of this
   * json object and its leaf nodes (but none of these leaf nodes are retained
   * individually (isUnitOfRetention == false for these leaf nodes)).
   *
   * Each has its advantages -- (1) does independent value updates easily. (2)
   * does atomic updates easily.
   */
  // private static final String DB_DISPLAY_VISIBLE = "displayVisible";//
  // boolean (stored as Integer)
  /*
   * 1 as boolean (true) is this column visible in Tables [may want tristate -1
   * = deleted, 0 = hidden, 1 = visible?]
   */
  // private static final String DB_DISPLAY_NAME = "displayName"; /* perhaps as
  // json i18n */
  // private static final String DB_DISPLAY_CHOICES_LIST = "displayChoicesList";
  // /* (was mcOptions)
  // choices i18n structure (Java needs rework).
  // TODO: allocate large storage on Aggregate
  /*
   * displayChoicesList -- TODO: rework ( this is still an ArrayList<String> )
   *
   * This is a map used for select1 and select choices, either closed-universe
   * (fixed set) or open-universe (select1-or-other, select-or-other). Stores
   * the full list of all values in the column. Example format (1st label shows
   * localization, 2nd is simple single-language defn:
   *
   * [ { "name": "1", "label": { "fr" : "oui", "en" : "yes", "es" : "si" } }, {
   * "name" : "0", "label": "no" } ]
   *
   * an open-universe list could just be a list of labels:
   *
   * [ "yes", "oui", "si", "no" ]
   *
   * i.e., there is no internationalization possible in open-universe lists, as
   * we allow free-form text entry. TODO: is this how we want this to work.
   *
   * When a user chooses to enter their own data in the field, we add that entry
   * to this list for later display as an available choice (i.e., we update the
   * choices list).
   *
   * TODO: how to define open vs. closed universe treatment? TODO: generalize
   * for other data types? i.e., "name" as a date range?
   */
  // private static final String DB_DISPLAY_FORMAT = "displayFormat";
  /*
   * (FUTURE USE) format descriptor for this display column. e.g., In the
   * Javascript, we have 'handlebars helpers' for template generation. We could
   * share a subset of this functionality in Tables for managing how to render a
   * value.
   *
   * TODO: how does this interact with displayChoicesList?
   *
   * The proposed eventual subset describes numeric formatting. It could also be
   * used to render qrcode images, etc. 'this' and elementName both refer to
   * this display value. E.g., sample usage syntax: "{{toFixed this "2"}}", //
   * this.toFixed(2) "{{toExponential this "2"}}" // this.toExponential(2)
   * "{{toPrecision this "2"}}" // this.toPrecision(2) "{{toString this "16"}}".
   * // this.toString(16) otherwise, it does {{this}} substitutions for
   * composite types. e.g., for geopoint:
   * "({{toFixed this.latitude "2"}}, {{toFixed this.longitude "
   * 2"}) {{toFixed this.altitude "1"}}m error: {{toFixed this.accuracy "1"}}m"
   * to produce '(48.50,32.20) 10.3m error: 6.0m'
   *
   * The only helper functions envisioned are "toFixed", "toExponential",
   * "toPrecision", "toString" and "localize" and perhaps one for qrcode
   * generation?
   *
   * TODO: how do you work with MULTIPLE_CHOICE e.g., for item separators (','
   * with final element ', and ')
   */

  /***********************************
   * The partition name of the column keys in the key value store.
   ***********************************/
  public static final String KVS_PARTITION = KeyValueStoreConstants.PARTITION_COLUMN;

  /***********************************
   * The names of keys that are defaulted to exist in the column key value
   * store.
   ***********************************/
  public static final String KEY_DISPLAY_VISIBLE = KeyValueStoreConstants.COLUMN_DISPLAY_VISIBLE;
  /*
   * Integer, non null. 1: visible 0: not visible -1: deleted (even necessary?)
   */
  public static final String KEY_DISPLAY_NAME = KeyValueStoreConstants.COLUMN_DISPLAY_NAME;
  /*
   * Text, not null. Must be input when adding a column as they all must have a
   * display name.
   */
  public static final String KEY_DISPLAY_CHOICES_LIST = KeyValueStoreConstants.COLUMN_DISPLAY_CHOICES_LIST;
  /*
   * Text, null.
   */
  public static final String KEY_DISPLAY_FORMAT = KeyValueStoreConstants.COLUMN_DISPLAY_FORMAT;
  /*
   * Text, null.
   */
  public static final String KEY_JOINS = KeyValueStoreConstants.COLUMN_JOINS;

  /***********************************
   * The fields that make up a ColumnProperties object.
   ***********************************/
  /*
   * The fields that belong only to the object, and are not related to the
   * actual column itself.
   */
  private final TableProperties tp;
  
  private final ColumnDefinition ci;
  /*
   * The fields that reside in ColumnDefinitions
   */
  private final String tableId;
  /*
   * The fields that reside in the key value store.
   */
  private boolean displayVisible;
  private String jsonStringifyDisplayName;
  private String displayName;
  private ArrayList<String> displayChoicesList;
  private String displayFormat;
  private ArrayList<JoinColumn> joins;

  /**
   *
   * @param tp
   * @param elementKey
   * @param elementName
   * @param elementType
   * @param listChildElementKeys
   * @param isUnitOfRetention
   * @param displayVisible
   * @param jsonStringifyDisplayName -- wrapped via mapper.writeValueAsString()
   * @param displayChoicesList
   * @param displayFormat
   * @param joins
   */
  private ColumnProperties(TableProperties tp, ColumnDefinition ci,
      boolean displayVisible, String jsonStringifyDisplayName,
      ArrayList<String> displayChoicesList, String displayFormat,
      ArrayList<JoinColumn> joins) {
    this.tp = tp;
    this.tableId = tp.getTableId();
    this.ci = ci;
    this.joins = (joins == null) ? new ArrayList<JoinColumn>() : joins;
    this.displayVisible = displayVisible;
    this.jsonStringifyDisplayName = jsonStringifyDisplayName;
    updateDisplayNameFromJsonStringifyDisplayName();
    this.displayChoicesList = (displayChoicesList == null) ? new ArrayList<String>() : displayChoicesList;
    this.displayFormat = displayFormat;
  }

  /**
   * Return the ColumnProperties for all the columns in a table, whether or not
   * they are written to the database table.
   *
   * @param db
   * @param tp
   * @return a map of elementKey to ColumnProperties for all columns.
   */
  static Map<String, ColumnProperties> getColumnPropertiesForTable(SQLiteDatabase db, TableProperties tp) {
    List<Column> columns = ODKDatabaseUtils.getUserDefinedColumns(db, tp.getTableId());
    ArrayList<ColumnDefinition> colDefns = ColumnDefinition.buildColumnDefinitions(columns);
    tp.setColumnDefinitions(colDefns);
    Map<String, ColumnProperties> elementKeyToColumnProperties = new HashMap<String, ColumnProperties>();
    for ( ColumnDefinition colDefn : colDefns ) {
      ColumnProperties cp = getColumnProperties(db, tp, colDefn);
      if ( cp == null ) continue;
      elementKeyToColumnProperties.put(colDefn.getElementKey(), cp);
    }
    return elementKeyToColumnProperties;
  }

  /**
   * Retrieve the ColumnProperties for the column specified by the given table
   * id and the given dbElementKey.
   *
   * @param dbh
   * @param tableId
   * @param dbElementKey
   * @param typeOfStore
   *          the type of the backing store from which to source the mutable
   *          column properties
   * @return
   */
  private static ColumnProperties getColumnProperties(SQLiteDatabase db, TableProperties tp, ColumnDefinition column) {
    // Get the KVS values
    List<KeyValueStoreEntry> entries = ODKDatabaseUtils.getDBTableMetadata(db, tp.getTableId(), 
        KeyValueStoreConstants.PARTITION_COLUMN, column.getElementKey(), null);

    return constructPropertiesFromMap(tp, column.getElementKey(), column, entries);
  }

  private static KeyValueStoreEntry findColumnProperty(List<KeyValueStoreEntry> entries, String key) {
    for ( KeyValueStoreEntry e : entries ) {
      if ( key.equals(e.key) ) {
        return e;
      }
    }
    return null;
  }
  
  /**
   * Construct a ColumnProperties from the given json serialization. NOTE: the
   * resulting ColumnProperties object has NOT been written to the database.
   * The caller is responsible for writing it and/or adding it to the
   * TableProperties of the tableId.
   *
   * @param dbh
   * @param tableId
   * @param elementKey
   * @param columnDefinitions
   * @param kvsProps
   * @param backingStore
   * @return
   */
  private static ColumnProperties constructPropertiesFromMap(TableProperties tp,
      String elementKey, ColumnDefinition column, List<KeyValueStoreEntry> entries) {
    // First convert the non-string types to their appropriate types. This is
    // probably going to go away when the map becomes key->TypeValuePair.
    // KEY_DISPLAY_VISIBLE
    String str;
    KeyValueStoreEntry entry;
    
    
    entry = findColumnProperty(entries, KeyValueStoreConstants.COLUMN_DISPLAY_VISIBLE);

    boolean displayVisible = false;
    if ( entry != null && entry.value != null && entry.value.length() != 0 ) {
      displayVisible = DataHelper.stringToBool(entry.value);
    } else {
      displayVisible = column.isUnitOfRetention();
    }
    
    entry = findColumnProperty(entries, KeyValueStoreConstants.COLUMN_DISPLAY_NAME);

    String displayName;
    if ( entry != null && entry.value != null && entry.value.length() != 0 ) {
      displayName = entry.value;
    } else {
      displayName = NameUtil.normalizeDisplayName(NameUtil.constructSimpleDisplayName(column.getElementKey()));
    }
    
    entry = findColumnProperty(entries, KeyValueStoreConstants.COLUMN_DISPLAY_FORMAT);

    String displayFormat;
    if ( entry != null && entry.value != null && entry.value.length() != 0 ) {
      displayFormat = entry.value;
    } else {
      displayFormat = null;
    }

    
    // Now we need to reclaim the list values from their db entries.
    String parseValue = null;
    ArrayList<String> displayChoicesList = null;
    ArrayList<JoinColumn> joins = null;
    try {
      entry = findColumnProperty(entries, KeyValueStoreConstants.COLUMN_DISPLAY_CHOICES_LIST);
      if ( entry != null && entry.value != null && entry.value.length() != 0) {
        parseValue = entry.value;
        displayChoicesList = ODKFileUtils.mapper.readValue(entry.value, ArrayList.class);
      }

      entry = findColumnProperty(entries, KeyValueStoreConstants.COLUMN_JOINS);
      if ( entry != null && entry.value != null && entry.value.length() != 0) {
        parseValue = entry.value;
        joins = JoinColumn.fromSerialization(entry.value);
      }
    } catch (JsonParseException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("invalid db value: " + parseValue, e);
    } catch (JsonMappingException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("invalid db value: " + parseValue, e);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("invalid db value: " + parseValue, e);
    }
    ElementType columnType = ElementType.parseElementType(column.getElementType(), 
        !column.getChildren().isEmpty());

    return new ColumnProperties(tp, column, displayVisible, displayName,
        displayChoicesList, displayFormat, joins);
  }
  
  /*
   * Get the where sql clause for the table id and the given element key.
   */
  private static final String WHERE_SQL_FOR_ELEMENT =
      ColumnDefinitionsColumns.TABLE_ID + " = ? AND " + ColumnDefinitionsColumns.ELEMENT_KEY + " = ?";


  /**
   * Add the definition of the column to the column_definitions table in
   * the SQLite database. All of the values in {@ColumnDefinitions.columnNames}
   * are added. Those with values not passed in are set to their default values
   * as defined in ColumnDefinitions.
   * <p>
   * Does NOT restructure the user table to add the column. Only adds a column
   * to column_definitions.
   * <p>
   * Returns a Map<String,String> of columnName->value. This is intended to
   * play nicely with {@link ColumnProperties}, which requires construction of
   * a ColumnProperties object with this information during table adding.
   * <p>
   * Does not close the passed in database.
   * TODO: check for redundant names
   * TODO: make this method also create the actual table for the rows.
   * TODO: make this return String->TypeValuePair, not the String represenation
   * of all the types.
   * @param db
   * @param tableId
   * @param elementKey
   * @param elementName
   * @param elementType type of the column. null values will be converted to
   * DEFAULT_DB_ELEMENT_TYPE
   * @param listChild
   * @return a map of column names to fields for the new table
   */
  public static void assertColumnDefinition(SQLiteDatabase db,
      String tableId, ColumnDefinition ci) {
    ContentValues values = new ContentValues();
    values.put(ColumnDefinitionsColumns.ELEMENT_NAME, ci.getElementName());
    values.put(ColumnDefinitionsColumns.ELEMENT_TYPE, ci.getElementType());
    values.put(ColumnDefinitionsColumns.LIST_CHILD_ELEMENT_KEYS, ci.getListChildElementKeys());

    Cursor c = null;
    try {
       c = db.query(DataModelDatabaseHelper.COLUMN_DEFINITIONS_TABLE_NAME,
                    null, WHERE_SQL_FOR_ELEMENT,
                    new String[] {tableId, ci.getElementKey()}, null, null, null);
       int count = c.getCount();
       c.close();
       if ( count == 1 ) {
         // update...
         db.update(DataModelDatabaseHelper.COLUMN_DEFINITIONS_TABLE_NAME,
             values, WHERE_SQL_FOR_ELEMENT,
             new String[] {tableId, ci.getElementKey()});
       } else {
         if ( count > 1 ) {
           // remove and re-insert...
           int delCount = db.delete(DataModelDatabaseHelper.COLUMN_DEFINITIONS_TABLE_NAME,
               WHERE_SQL_FOR_ELEMENT, new String[] {tableId, ci.getElementKey()});
           if (delCount != 1) {
             Log.e(TAG, "deleteColumn() deleted " + delCount + " rows");
           }
         }
         // insert...
         values.put(ColumnDefinitionsColumns.TABLE_ID, tableId);
         values.put(ColumnDefinitionsColumns.ELEMENT_KEY, ci.getElementKey());
         db.insert(DataModelDatabaseHelper.COLUMN_DEFINITIONS_TABLE_NAME, null, values);
       }
    } finally {
      if ( c != null && !c.isClosed()) {
         c.close();
      }
    }
  }

  /**
   * NOTE: ONLY CALL THIS FROM TableProperties.addColumn() !!!!!!!
   *
   * Add a column to the datastore. elementKey and elementName should be made
   * via createDbElementKey and createDbElementName to avoid conflicts. A
   * possible idea would be to pass them display name.
   *
   *
   * @param db
   * @throws JsonGenerationException
   * @throws JsonMappingException
   * @throws IOException
   */
  void persistColumn(SQLiteDatabase db) throws JsonGenerationException, JsonMappingException, IOException {
    // First prepare the entries for the key value store.
    List<KeyValueStoreEntry> values = new ArrayList<KeyValueStoreEntry>();
    values.add(createBooleanEntry(tableId, ColumnProperties.KVS_PARTITION, ci.getElementKey(),
        KEY_DISPLAY_VISIBLE, displayVisible));
    values.add(createStringEntry(tableId, ColumnProperties.KVS_PARTITION, ci.getElementKey(),
        KEY_DISPLAY_NAME, jsonStringifyDisplayName));
    values.add(createStringEntry(tableId, ColumnProperties.KVS_PARTITION, ci.getElementKey(),
        KEY_DISPLAY_CHOICES_LIST, ODKFileUtils.mapper.writeValueAsString(displayChoicesList)));
    values.add(createStringEntry(tableId, ColumnProperties.KVS_PARTITION, ci.getElementKey(),
        KEY_DISPLAY_FORMAT, displayFormat));
    values.add(createStringEntry(tableId, ColumnProperties.KVS_PARTITION, ci.getElementKey(),
        KEY_JOINS, JoinColumn.toSerialization(joins)));

    // this updates or inserts these values into the data store
    ODKDatabaseUtils.replaceDBTableMetadata(db, tp.getTableId(), values, false);

    assertColumnDefinition(db, tableId, ci);
  }

  /**
   * NOTE: ONLY CALL THIS FROM TableProperties.addColumn() !!!!!!!
   *
   * Create a ColumnProperties object with the given values (assumed to be good).
   * Caller is responsible for writing this to the database.
   *
   * @param dbh
   * @param tableId
   * @param jsonStringifyDisplayName -- wrapped via mapper.writeValueAsString()
   * @param elementKey
   * @param elementName
   * @param columnType
   * @param listChildElementKeys
   * @param isUnitOfRetention
   * @param displayVisible
   * @param typeOfStore
   * @return
   */
  static ColumnProperties createNotPersisted(TableProperties tp,
      String jsonStringifyDisplayName, ColumnDefinition ci, boolean displayVisible) {

    ColumnProperties cp = new ColumnProperties(tp, ci, displayVisible, jsonStringifyDisplayName,
        null, null, null);

    return cp;
  }

  private static KeyValueStoreEntry createStringEntry(String tableId, String partition,
      String elementKey, String key, String value) {
    KeyValueStoreEntry entry = new KeyValueStoreEntry();
    entry.tableId = tableId;
    entry.partition = partition;
    entry.aspect = elementKey;
    entry.type = ElementDataType.string.name();
    entry.value = value;
    entry.key = key;
    return entry;
  }

  private static KeyValueStoreEntry createIntEntry(String tableId, String partition,
      String elementKey, String key, int value) {
    KeyValueStoreEntry entry = new KeyValueStoreEntry();
    entry.tableId = tableId;
    entry.partition = partition;
    entry.aspect = elementKey;
    entry.type = ElementDataType.integer.name();
    entry.value = Integer.toString(value);
    entry.key = key;
    return entry;
  }

  private static KeyValueStoreEntry createBooleanEntry(String tableId, String partition,
      String elementKey, String key, boolean value) {
    KeyValueStoreEntry entry = new KeyValueStoreEntry();
    entry.tableId = tableId;
    entry.partition = partition;
    entry.aspect = elementKey;
    entry.type = ElementDataType.bool.name();
    entry.value = Boolean.toString(value);
    entry.key = key;
    return entry;
  }

  public String getElementKey() {
    return ci.getElementKey();
  }

  public String getElementName() {
    return ci.getElementName();
  }

  public boolean isUnitOfRetention() {
    return ci.isUnitOfRetention();
  }
  
  public ColumnProperties getContainingElement() {
    return tp.getColumnByElementKey(ci.getParent().getElementKey());
  }
  
  public List<ColumnDefinition> getChildren() {
    return ci.getChildren();
  }

  public String getListChildElementKeys() {
    return ci.getListChildElementKeys();
  }
  /**
   * @return the column's type
   */
  public ElementType getColumnType() {
    return ElementType.parseElementType(ci.getElementType(), !ci.getChildren().isEmpty());
  }

  public static Class<?> getDataType(ElementDataType dataType) {
    
    if ( dataType == ElementDataType.array ) {
      return ArrayList.class;
    }
    if (dataType == ElementDataType.object) {
      return HashMap.class;
    }
    
    if ( dataType == ElementDataType.integer ) {
      return Integer.class;
    }

    if ( dataType == ElementDataType.number ) {
      return Double.class;
    }

    if ( dataType == ElementDataType.bool ) {
      return Boolean.class;
    }

    return String.class;
  }

  /**
   * @return whether or not this column is visible within Tables
   */
  public boolean getDisplayVisible() {
    return displayVisible;
  }

  /**
   * Sets whether or not this column is visible within Tables
   *
   * @param setting
   *          the new display visibility setting
   */
  public void setDisplayVisible(SQLiteDatabase db, boolean setting) {
    setBooleanProperty(db, KEY_DISPLAY_VISIBLE, setting);
    this.displayVisible = setting;
  }

  /**
   * @return the column's display name
   */
  public String getDisplayName() {
    return displayName;
  }

  public String getLocalizedDisplayName() {
    String localized = ODKDataUtils.getLocalizedDisplayName(jsonStringifyDisplayName);
    if ( localized == null ) {
      return getElementKey();
    }
    return localized;
  }

  private void updateDisplayNameFromJsonStringifyDisplayName() {
    try {
      this.displayName = null;
      if (jsonStringifyDisplayName != null && jsonStringifyDisplayName.length() > 0) {
        Object displayObject = ODKFileUtils.mapper.readValue(jsonStringifyDisplayName, Object.class);
        if ( displayObject instanceof String ) {
          this.displayName = (String)  displayObject;
          if ( this.displayName == null || this.displayName.length() == 0 ) {
          	// just use the elementKey and fudge it back into the serialization
          	this.displayName = NameUtil.constructSimpleDisplayName(ci.getElementKey());
          	this.jsonStringifyDisplayName = ODKFileUtils.mapper.writeValueAsString(this.displayName);
          }
        } else if ( displayObject instanceof Map ) {
          // TODO: get current locale; deal with non-default locales
          @SuppressWarnings("rawtypes")
          String lang = Locale.getDefault().getLanguage();
          if ( lang != null ) {
            Object v = ((Map) displayObject).get(lang);
            if ( v != null && v instanceof String ) {
              this.displayName = (String) v;
            }
          }
          if ( this.displayName == null ) {
            @SuppressWarnings("rawtypes")
            Object v = ((Map) displayObject).get(ODKDatabaseUtils.DEFAULT_LOCALE);
            if ( v != null && v instanceof String ) {
              this.displayName = (String) v;
            }
          }
          if ( this.displayName == null || this.displayName.length() == 0 ) {
            this.displayName = NameUtil.constructSimpleDisplayName(ci.getElementKey());
            ((Map) displayObject).put(ODKDatabaseUtils.DEFAULT_LOCALE, this.displayName);
            this.jsonStringifyDisplayName = ODKFileUtils.mapper.writeValueAsString(displayObject);
          }
        }
      }
      
      if ( this.displayName == null || this.displayName.length() == 0 ) {
        // no idea what this is -- overwrite it with a simple string.
        this.displayName = NameUtil.constructSimpleDisplayName(ci.getElementKey());
        this.jsonStringifyDisplayName = ODKFileUtils.mapper.writeValueAsString(this.displayName);
      }
    } catch (JsonParseException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("displayName is not JSON.stringify() content: " + jsonStringifyDisplayName);
    } catch (JsonMappingException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("displayName is not JSON.stringify() content: " + jsonStringifyDisplayName);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("displayName is not JSON.stringify() content: " + jsonStringifyDisplayName);
    }
  }

  /**
   * Sets the column's display name.
   *
   * NOTE: The caller is responsible for confirming that the name will not be in
   * conflict with any other column display names in use within the table. Use
   * TableProperties.createDisplayName(proposedName) to do this.
   *
   * NOTE: this updates the jsonStringifyDisplayName, which is what is writtin
   * in the KVS.
   *
   * @param displayName
   *          the new display name
   * @return the
   */
  public void setDisplayName(SQLiteDatabase db, String displayName) {
    try {
      // error if displayName is not set...
      if ( displayName == null || displayName.length() == 0 ) {
        throw new IllegalArgumentException("displayName is not valid: " + displayName);
      }
      // parse the existing jsonStringifyDisplayName value...
      if (jsonStringifyDisplayName != null && jsonStringifyDisplayName.length() > 0) {
        Object displayObject = ODKFileUtils.mapper.readValue(jsonStringifyDisplayName, Object.class);
        if ( displayObject instanceof String ) {
          // just overwrite it...
          String newJsonStringifyDisplayName = ODKFileUtils.mapper.writeValueAsString(displayName);
          setObjectProperty(db, KEY_DISPLAY_NAME, newJsonStringifyDisplayName);
          this.jsonStringifyDisplayName = newJsonStringifyDisplayName;
          this.displayName = displayName;
        } else if ( displayObject instanceof Map ) {
          // TODO: get current locale; deal with non-default locales
          ((Map) displayObject).put("default", displayName);
          String newJsonStringifyDisplayName = ODKFileUtils.mapper.writeValueAsString(displayObject);
          setObjectProperty(db, KEY_DISPLAY_NAME, newJsonStringifyDisplayName);
          this.jsonStringifyDisplayName = newJsonStringifyDisplayName;
          this.displayName = displayName;
        }
      } else {
        String newJsonStringifyDisplayName = ODKFileUtils.mapper.writeValueAsString(displayName);
        setObjectProperty(db, KEY_DISPLAY_NAME, newJsonStringifyDisplayName);
        this.jsonStringifyDisplayName = newJsonStringifyDisplayName;
        this.displayName = displayName;
      }
    } catch (JsonParseException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("displayName is not JSON.stringify() content: " + jsonStringifyDisplayName);
    } catch (JsonMappingException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("displayName is not JSON.stringify() content: " + jsonStringifyDisplayName);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("displayName is not JSON.stringify() content: " + jsonStringifyDisplayName);
    }
  }

  /**
   * @return the column's display format string or null if pass-through
   */
  public String getDisplayFormat() {
    return displayFormat;
  }

  /**
   * Sets the column's display format string.
   *
   * @param abbreviation
   *          the new abbreviation (or null for no abbreviation)
   */
  public void setDisplayFormat(SQLiteDatabase db, String format) {
    setStringProperty(db, KEY_DISPLAY_FORMAT, format);
    this.displayFormat = format;
  }

  /**
   * @return the join definition
   */
  public ArrayList<JoinColumn> getJoins() {
    return joins;
  }

  /**
   * Converts the JoinColumn to the json representation of the object using a
   * mapper and adds it to the database.
   * <p>
   * If there is a mapping exception of writing the JoinColumn to a String, it
   * does nothing, leaving the database untouched.
   *
   * @param joins
   */
  public void setJoins(SQLiteDatabase db, ArrayList<JoinColumn> joins) {
    try {
      String joinsStr = JoinColumn.toSerialization(joins);
      setObjectProperty(db, KEY_JOINS, joinsStr);
      this.joins = joins;
    } catch (JsonGenerationException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("setJoins failed: " + joins.toString(), e);
    } catch (JsonMappingException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("setJoins failed: " + joins.toString(), e);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("setJoins failed: " + joins.toString(), e);
    }
  }

  /**
   * @return an array of the multiple-choice options
   */
  public ArrayList<String> getDisplayChoicesList() {
    return displayChoicesList;
  }

  /**
   * Sets the multiple-choice options.
   *
   * @param options
   *          the array of options
   * @throws IOException
   * @throws JsonMappingException
   * @throws JsonGenerationException
   */
  public void setDisplayChoicesList(SQLiteDatabase db, ArrayList<String> options) {
    setArrayProperty(db, KEY_DISPLAY_CHOICES_LIST, options);
    displayChoicesList = options;
  }

  private void setIntProperty(SQLiteDatabase db, String property, int value) {
    // or a kvs property?
    KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(ColumnProperties.KVS_PARTITION);
    AspectHelper aspectHelper = kvsh.getAspectHelper(ci.getElementKey());
    aspectHelper.setInteger(property, value);
  }

  private void setObjectProperty(SQLiteDatabase db, String property, String value) {
    // or a kvs property?
    KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(ColumnProperties.KVS_PARTITION);
    AspectHelper aspectHelper = kvsh.getAspectHelper(ci.getElementKey());
    aspectHelper.setObject(property, value);
  }

  private <T> void setArrayProperty(SQLiteDatabase db, String property, ArrayList<T> values) {
    // or a kvs property?
    KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(ColumnProperties.KVS_PARTITION);
    AspectHelper aspectHelper = kvsh.getAspectHelper(ci.getElementKey());
    aspectHelper.setArray(property, values);
  }

  private void setStringProperty(SQLiteDatabase db, String property, String value) {
    // or a kvs property?
    KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(ColumnProperties.KVS_PARTITION);
    AspectHelper aspectHelper = kvsh.getAspectHelper(ci.getElementKey());
    aspectHelper.setString(property, value);
  }

  private void setBooleanProperty(SQLiteDatabase db, String property, boolean value) {
    // or a kvs property?
    KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(ColumnProperties.KVS_PARTITION);
    AspectHelper aspectHelper = kvsh.getAspectHelper(ci.getElementKey());
    aspectHelper.setBoolean(property, value);
  }

}
