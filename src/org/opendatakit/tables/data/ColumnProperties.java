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
package org.opendatakit.tables.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.opendatakit.aggregate.odktables.entity.OdkTablesKeyValueStoreEntry;
import org.opendatakit.tables.sync.SyncUtil;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * A class for accessing and managing column properties.
 * <p>
 * Column properties are located in several places. The more, although not
 * completely, immutable properties are located in a table that is defined in
 * {@link ColumnDefinitions}. The mutable, mostly ODK Tables-specific columns
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

  private static final ObjectMapper mapper = new ObjectMapper();
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
   * value persisted into the database
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
   * if this is a composite type (geopoint), this is a JSON list of the element
   * keys of the direct descendants of this field name.
   */
  // private static final String DB_IS_PERSISTED =
  // "is_persisted";
  /*
   * default: 1 (true) -- whether or not this is persisted to the database. If
   * true, elementId is the dbColumnName in which this value is written. The
   * value will be written as JSON if it is a composite type. see larger comment
   * below for example.
   */
  /*
   * These two columns have been replaced by the single column DB_JOINS
   */
  // private static final String DB_JOINS = "joins";
  // private static final String DB_JOIN_TABLE_ID = "joinTableId"; /* tableId of
  // table to join against */
  // private static final String DB_JOIN_ELEMENT_KEY = "joinElementKey"; // (was
  // DB_JOIN_COLUMN_NAME)
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
   * listChildEKeys: '[ patientSymptomsItem ]' isPersist: true
   * 
   * ekey: patientSymptomsItem ename: null // elements are not named within this
   * list etype: STRING listChildEKeys: null isPersist: false
   * 
   * ------------- e.g., geopoint defining a northernmost point of something:
   * 
   * The data is stored as 4 columns, 'northLatitude', 'northLongitude',
   * 'northAltitude', 'northAccuracy'
   * 
   * ekey: northernmostPoint ename: northernmostPoint etype: geopoint
   * listChildEKeys: '[ "northLatitude", "northLongitude", "northAltitude",
   * "northAccuracy"]' isPersist: false
   * 
   * ekey: northLatitude ename: latitude etype: DECIMAL listChildEKeys: null
   * isPersist: true
   * 
   * ekey: northLongitude ename: longitude etype: DECIMAL listChildEKeys: null
   * isPersist: true
   * 
   * ekey: northAltitude ename: altitude etype: DECIMAL listChildEKeys: null
   * isPersist: true
   * 
   * ekey: northAccuracy ename: accuracy etype: DECIMAL listChildEKeys: null
   * isPersist: true
   * 
   * ODK Collect can do calculations and constraint tests like
   * 'northermostPoint.altitude < 4.0'
   * 
   * e.g., 'clientPhone' as a phonenumber type, which is just a restriction on a
   * STRING value persists under 'clientPhone' column in database.
   * 
   * ekey: clientPhone ename: clientPhone etype: phoneNumber listChildEKeys: [
   * "clientPhoneNumber" ] // single element isPersist: true
   * 
   * ekey: clientPhoneNumber ename: null // null -- indicates restriction on
   * etype etype: STRING listChildEKeys: null isPersist: false
   * 
   * e.g., 'image' file capture in ODK Collect. Stored as a MIMEURI
   * 
   * ekey: locationImage ename: locationImage etype: MIMEURI listChildEKeys:
   * null isPersist: true
   * 
   * MIMEURI stores a JSON object:
   * 
   * '{"path":"/mnt/sdcard/odk/tables/app/instances/2342.jpg","mimetype":"image/jpg"}'
   * 
   * i.e., ODK Collect image/audio/video capture store everything as a MIMEURI
   * with different mimetype values.
   */

  /*
   * 
   * NOTE: you can have a composite type stored in two ways: (1) store the leaf
   * nodes of the composite type in the database. Describe the entire type
   * hierarchy down to those leaf nodes. (2) store it as a json object at the
   * top level. Describe the structure of this json object and its leaf nodes
   * (but none of these persist anything).
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
  // private static final String DB_DISPLAY_CHOICES_MAP = "displayChoicesMap";
  // /* (was mcOptions)
  // choices i18n structure (Java needs rework).
  // TODO: allocate large storage on Aggregate
  /*
   * displayChoicesMap -- TODO: rework ( this is still an ArrayList<String> )
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
   * TODO: how does this interact with displayChoicesMap?
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

  // /***********************************
  // * Default values for those columns that have defaults.
  // ***********************************/
  // public static final int DEFAULT_DB_IS_PERSISTED = 1;

  /***********************************
   * The names of keys that are defaulted to exist in the column key value
   * store.
   ***********************************/
  public static final String KEY_DISPLAY_VISIBLE = "displayVisible";
    /*
     * Integer, non null. 1: visible
     *                    0: not visible
     *                   -1: deleted (even necessary?)
     */
  public static final String KEY_DISPLAY_NAME = "displayName";
    /*
     * Text, not null. Must be input when adding a column as they all must
     * have a display name.
     */
  public static final String KEY_DISPLAY_CHOICES_MAP = "displayChoicesMap";
    /*
     * Text, null. 
     */
  public static final String KEY_DISPLAY_FORMAT = "displayFormat";
    /*
     * Text, null. Fot future use.
     */
  public static final String KEY_SMS_IN = "smsIn";
    /*
     * Integer, not null. As boolean. Allow incoming SMS to modify the column.
     */
  public static final String KEY_SMS_OUT = "smsOut";
    /*
     * Integer, not null. As boolean. Allow outgoing SMS to access this column.
     */
  public static final String KEY_SMS_LABEL = "smsLabel";
    /*
     * Text null.
     */
  public static final String KEY_FOOTER_MODE = "footerMode";
    /*
     * What the footer should display.
     */
  
  
  /***********************************
   *  Default values for those keys which require them.
   *  TODO When the keys in the KVS are moved to the respective classes that 
   *  use them, these should go there most likely.
   ***********************************/
  public static final int DEFAULT_KEY_VISIBLE = 1;
  public static final FooterMode DEFAULT_KEY_FOOTER_MODE = FooterMode.none;
  public static final int DEFAULT_KEY_SMS_IN = 1;
  public static final int DEFAULT_KEY_SMS_OUT = 1;
  public static final String DEFAULT_KEY_SMS_LABEL = null;
  public static final String DEFAULT_KEY_DISPLAY_FORMAT = null;
  public static final String DEFAULT_KEY_DISPLAY_CHOICES_MAP = null;
  
  
  /*
   * These KEYS are distinct from columns b/c the keys are vertical keys in the
   * key value store, not horizontal columns in the column definitions table.
   */
  private static final String[] INIT_KEYS = { 
    KEY_DISPLAY_VISIBLE, 
    KEY_DISPLAY_NAME,
    KEY_DISPLAY_CHOICES_MAP, 
    KEY_DISPLAY_FORMAT, 
    KEY_SMS_IN, 
    KEY_SMS_OUT, 
    KEY_SMS_LABEL,
    KEY_FOOTER_MODE };
  
  
  /***********************************
   *  Keys for json.
   ***********************************/ 

  // private static final String DB_SMS_IN = "smsIn"; /* (allow SMS incoming)
  // default: 1 as boolean (true) */
  // private static final String DB_SMS_OUT = "smsOut"; /* (allow SMS outgoing)
  // default: 1 as boolean (true) */
  // private static final String DB_SMS_LABEL = "smsLabel"; /* for SMS */

  // private static final String DB_FOOTER_MODE = "footerMode"; /* 0=none,
  // 1=count, 2=minimum, 3=maximum, 4=mean, 5=sum */

  // keys for JSON
  private static final String JSON_KEY_VERSION = "jVersion";
  private static final String JSON_KEY_TABLE_ID = "tableId";

  public static final String JSON_KEY_ELEMENT_KEY = "elementKey";// (was
                                                                  // dbColumnName)
  public static final String JSON_KEY_ELEMENT_NAME = "elementName";
  private static final String JSON_KEY_ELEMENT_TYPE = "elementType"; // (was
                                                                     // colType)
  private static final String JSON_KEY_LIST_CHILD_ELEMENT_KEYS = "listChildElementKeys";
  private static final String JSON_KEY_JOINS = "joins";
  // private static final String JSON_KEY_JOIN_TABLE_ID = "joinTableId";
  // private static final String JSON_KEY_JOIN_ELEMENT_KEY = "joinElementKey";
  private static final String JSON_KEY_IS_PERSISTED = "isPersisted";

  private static final String JSON_KEY_DISPLAY_VISIBLE = "displayVisible";
  private static final String JSON_KEY_DISPLAY_NAME = "displayName";
  private static final String JSON_KEY_DISPLAY_CHOICES_MAP = "displayChoicesMap";
  private static final String JSON_KEY_DISPLAY_FORMAT = "displayFormat";

  private static final String JSON_KEY_SMS_IN = "smsIn";
  private static final String JSON_KEY_SMS_OUT = "smsOut";
  private static final String JSON_KEY_SMS_LABEL = "smsLabel";

  private static final String JSON_KEY_FOOTER_MODE = "footerMode";

  // the SQL where clause to use for selecting, updating,
  // or deleting the row for a given column
//  private static final String WHERE_SQL = DB_TABLE_ID + " = ? and " 
//      + DB_ELEMENT_KEY + " = ?";

  // the columns to be selected when initializing ColumnProperties
//  private static final String[] INIT_COLUMNS = { KeyValueStoreManager.TABLE_ID, DB_ELEMENT_KEY,
//      DB_ELEMENT_NAME, DB_ELEMENT_TYPE, DB_LIST_CHILD_ELEMENT_KEYS, DB_JOINS,
//      // DB_JOIN_TABLE_ID,
//      // DB_JOIN_ELEMENT_KEY,
//      DB_IS_PERSISTED,

  // DB_DISPLAY_VISIBLE,
  // DB_DISPLAY_NAME,
  // DB_DISPLAY_CHOICES_MAP,
  // DB_DISPLAY_FORMAT,
  // DB_SMS_IN,
  // DB_SMS_OUT,
  // DB_SMS_LABEL,
  // DB_FOOTER_MODE
//  };

  // Has moved to FooterMode.java.
  // public class FooterMode {
  // public static final int NONE = 0;
  // public static final int COUNT = 1;
  // public static final int MINIMUM = 2;
  // public static final int MAXIMUM = 3;
  // public static final int MEAN = 4;
  // public static final int SUM = 5;
  // private FooterMode() {}
  // }

  /***********************************
   *  The fields that make up a ColumnProperties object.
   ***********************************/  
  /*
   * The fields that belong only to the object, and are not related to the 
   * actual column itself.
   */
  private final DbHelper dbh;
  private final String[] whereArgs;
  // The type of key value store from which these properties were drawn.
  private final KeyValueStore.Type backingStore;
  /*
   * The fields that reside in ColumnDefinitions
   */
  private final String tableId;
  private final String elementKey;
  private String elementName;
  private ColumnType elementType;
  private List<String> listChildElementKeys;
  private JoinColumn joins;
  // private String joinTableId;
  // private String joinElementKey;
  private boolean isPersisted;
  /*
   * The fields that reside in the key value store.
   */
  private int displayVisible;
  private String displayName;
  private ArrayList<String> displayChoicesMap;
  private String displayFormat;
  private boolean smsIn;
  private boolean smsOut;
  private String smsLabel;
  private FooterMode footerMode;

  private ColumnProperties(DbHelper dbh, 
      String tableId, 
      String elementKey,
      String elementName,
      ColumnType elementType,
      List<String> listChildElementKeys,
      JoinColumn joins,
      // String joinTableId,
      // String joinElementKey,
      boolean isPersisted, 
      int displayVisible, 
      String displayName,
      ArrayList<String> displayChoicesMap, 
      String displayFormat, 
      boolean smsIn,
      boolean smsOut,
      String smsLabel, 
      FooterMode footerMode,
      KeyValueStore.Type backingStore) {
    this.dbh = dbh;
    whereArgs = new String[] { String.valueOf(tableId), elementKey };
    this.tableId = tableId;
    this.elementKey = elementKey;
    this.elementName = elementName;
    this.elementType = elementType;
    this.listChildElementKeys = listChildElementKeys;
    this.joins = joins;
    // this.joinTableId = joinTableId;
    // this.joinElementKey = joinElementKey;
    this.isPersisted = isPersisted;
    this.displayVisible = displayVisible;
    this.displayName = displayName;
    this.displayChoicesMap = displayChoicesMap;
    this.displayFormat = displayFormat;
    this.smsIn = smsIn;
    this.smsOut = smsOut;
    this.smsLabel = smsLabel;
    this.footerMode = footerMode;
    if (backingStore == KeyValueStore.Type.ACTIVE ||
        backingStore == KeyValueStore.Type.DEFAULT ||
        backingStore == KeyValueStore.Type.SERVER) {
      Log.e(TAG, "ColumnProperties was given a non-column key value store");
      throw new IllegalArgumentException("non-column key value store passed" +
      		" to ColumnProperties constructor");
    }
    this.backingStore = backingStore;
  }
  
  public static String[] getInitKeys() {
    return INIT_KEYS;
  }

  /**
   * Retrieve the ColumnProperties for the column specified by the given table
   * id and the given dbElementKey.
   * @param dbh
   * @param tableId
   * @param dbElementKey
   * @param typeOfStore the type of the backing store from which to source the
   * mutable column properties
   * @return
   * @throws IllegalArgumentException if typeOfStore is not a column KVS
   */
  public static ColumnProperties getColumnProperties(DbHelper dbh, 
      String tableId, String dbElementKey, KeyValueStore.Type typeOfStore) {
    if (!isValidStore(typeOfStore)) {
      Log.e(TAG, "invalid KeyValueStore.Type passed to getColumnPropties: " +
      		typeOfStore);
      throw new IllegalArgumentException("invalid KeyValueStore.Type passed " +
      		"to getColumnPropties: " + typeOfStore);
    }
    Map<String, String> mapProps = getMapForColumn(dbh, tableId, dbElementKey, 
        typeOfStore);
    return constructPropertiesFromMap(dbh, mapProps, typeOfStore);

//    SQLiteDatabase db = null;
//    Cursor c = null;
//    ColumnProperties cp = null;
//    String parseValue = null;
//
//    try {
//      db = dbh.getReadableDatabase();
//      c = db.query(DB_TABLENAME, INIT_COLUMNS, WHERE_SQL, new String[] { tableId, dbElementKey },
//          null, null, null);
//
//      int dbcnIndex = c.getColumnIndexOrThrow(DB_ELEMENT_KEY);
//
//      int elementNameIndex = c.getColumnIndexOrThrow(DB_ELEMENT_NAME);
//      int elementTypeIndex = c.getColumnIndexOrThrow(DB_ELEMENT_TYPE);
//      int listChildElementKeysIndex = c.getColumnIndexOrThrow(DB_LIST_CHILD_ELEMENT_KEYS);
//      int joinIndex = c.getColumnIndexOrThrow(DB_JOINS);
//      // int joinTableIndex = c.getColumnIndexOrThrow(DB_JOIN_TABLE_ID);
//      // int joinElementIndex = c.getColumnIndexOrThrow(DB_JOIN_ELEMENT_KEY);
//      int isPersistedIndex = c.getColumnIndexOrThrow(DB_IS_PERSISTED);
//
//      int displayVisibleIndex = c.getColumnIndexOrThrow(DB_DISPLAY_VISIBLE);
//      int displayNameIndex = c.getColumnIndexOrThrow(DB_DISPLAY_NAME);
//      int displayChoicesMapIndex = c.getColumnIndexOrThrow(DB_DISPLAY_CHOICES_MAP);
//      int displayFormatIndex = c.getColumnIndexOrThrow(DB_DISPLAY_FORMAT);
//
//      int smsInIndex = c.getColumnIndexOrThrow(DB_SMS_IN);
//      int smsOutIndex = c.getColumnIndexOrThrow(DB_SMS_OUT);
//      int smsLabelIndex = c.getColumnIndexOrThrow(DB_SMS_LABEL);
//
//      int footerModeIndex = c.getColumnIndexOrThrow(DB_FOOTER_MODE);
//
//      c.moveToFirst();
//
//      @SuppressWarnings("unchecked")
//      ArrayList<String> displayChoicesMap = null;
//      if (!c.isNull(displayChoicesMapIndex)) {
//        String displayChoicesMapValue = c.getString(displayChoicesMapIndex);
//        parseValue = displayChoicesMapValue;
//        displayChoicesMap = mapper.readValue(displayChoicesMapValue, ArrayList.class);
//      }
//      ArrayList<String> listChildElementKeys = null;
//      if (!c.isNull(listChildElementKeysIndex)) {
//        String listChildElementKeysValue = c.getString(listChildElementKeysIndex);
//        parseValue = listChildElementKeysValue;
//        listChildElementKeys = mapper.readValue(listChildElementKeysValue, ArrayList.class);
//      }
//      cp = new ColumnProperties(dbh, tableId, c.getString(dbcnIndex),
//          c.getString(elementNameIndex), ColumnType.valueOf(c.getString(elementTypeIndex)),
//          listChildElementKeys, c.getString(joinIndex),
//          // c.getString(joinTableIndex),
//          // c.getString(joinElementIndex),
//          c.getInt(isPersistedIndex) == 1,
//
//          c.getInt(displayVisibleIndex) == 1, c.getString(displayNameIndex), displayChoicesMap,
//          c.getString(displayFormatIndex),
//
//          c.getInt(smsInIndex) == 1, c.getInt(smsOutIndex) == 1, c.getString(smsLabelIndex),
//
//          c.getInt(footerModeIndex));
//    } catch (JsonParseException e) {
//      e.printStackTrace();
//      throw new IllegalArgumentException("invalid db value: " + parseValue);
//    } catch (JsonMappingException e) {
//      e.printStackTrace();
//      throw new IllegalArgumentException("invalid db value: " + parseValue);
//    } catch (IOException e) {
//      e.printStackTrace();
//      throw new IllegalArgumentException("invalid db value: " + parseValue);
//    } finally {
//      try {
//        if (c != null && !c.isClosed()) {
//          c.close();
//        }
//      } finally {
//        // TODO: fix the when to close problem
//        // if ( db != null ) {
//        // db.close();
//        // }
//      }
//    }
//    return cp;
  }

  /*
   * Return the map of all the properties for the given column. Atm this is 
   * just key->value. The caller must know the intended value and parse it 
   * correctly.
   * 
   * This map should eventually become a key->TypeValuePair or something like
   * that. TODO: make it the above
   * 
   * This deserves its own method b/c to get the properties you are forced to 
   * go through both the key value store and the column properties table.
   */
  private static Map<String, String> getMapForColumn(DbHelper dbh, 
      String tableId, String elementKey, KeyValueStore.Type typeOfStore) {
    SQLiteDatabase db = null;
    try {
      db = dbh.getReadableDatabase();
      KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
      KeyValueStoreColumn intendedKVS = kvsm.getStoreForColumn(tableId, 
          elementKey, typeOfStore);
      Map<String, String> columnDefinitionsMap = 
          ColumnDefinitions.getFields(tableId, elementKey, db);
      Map<String, String> kvsMap = intendedKVS.getProperties(db);
      Map<String, String> mapProps = new HashMap<String, String>();
      mapProps.putAll(columnDefinitionsMap);
      mapProps.putAll(kvsMap);
      // TODO: fix the when to close problem
//    db.close();
//    db = null;
      return mapProps;
    } finally {
      // TODO: fix the when to close problem
//      if ( db != null ) {
//         db.close();
//      }
    }
  }
  
  private static ColumnProperties constructPropertiesFromMap(DbHelper dbh,
      Map<String, String> props, KeyValueStore.Type backingStore) {
    // First convert the non-string types to their appropriate types. This is
    // probably going to go away when the map becomes key->TypeValuePair.
    // KEY_SMS_IN
    String smsInStr = props.get(KEY_SMS_IN);
    int smsInInt = Integer.parseInt(smsInStr);
    boolean smsIn = SyncUtil.intToBool(smsInInt);
    // KEY_SMS_OUT
    String smsOutStr = props.get(KEY_SMS_OUT);
    int smsOutInt = Integer.parseInt(smsOutStr);
    boolean smsOut = SyncUtil.intToBool(smsOutInt);
    // KEY_DISPLAY_VISIBLE
    String displayVisibleStr = props.get(KEY_DISPLAY_VISIBLE);
    int displayVisible = Integer.parseInt(displayVisibleStr);
    // KEY_FOOTER_MODE
    String footerModeStr = props.get(KEY_FOOTER_MODE);
    // TODO don't forget that all of these value ofs for all these enums 
    // should eventually be surrounded with try/catch to support versioning
    // when new values might come down from the server.
    FooterMode footerMode = FooterMode.valueOf(footerModeStr);
    // DB_IS_PERSISTED
    String isPersistedStr = props.get(ColumnDefinitions.DB_IS_PERSISTED);
    int isPersistedInt = Integer.parseInt(isPersistedStr);
    boolean isPersisted = SyncUtil.intToBool(isPersistedInt);
    // DB_COLUMN_TYPE 
    String columnTypeStr = props.get(ColumnDefinitions.DB_ELEMENT_TYPE);
    ColumnType columnType = ColumnType.valueOf(columnTypeStr);
    
    // Now we need to reclaim the list values from their db entries.
    String parseValue = null;
    ArrayList<String> displayChoicesMap = null;
    ArrayList<String> listChildElementKeys = null;
    JoinColumn joins = null;
    try {
      if (props.get(KEY_DISPLAY_CHOICES_MAP) != null)  {
        String displayChoicesMapValue = props.get(KEY_DISPLAY_CHOICES_MAP);
        parseValue = displayChoicesMapValue;
        displayChoicesMap = mapper.readValue(displayChoicesMapValue, ArrayList.class);
      }

      if (props.get(ColumnDefinitions.DB_LIST_CHILD_ELEMENT_KEYS) != null) {
        String listChildElementKeysValue = 
            props.get(ColumnDefinitions.DB_LIST_CHILD_ELEMENT_KEYS);
        parseValue = listChildElementKeysValue;
        listChildElementKeys = mapper.readValue(listChildElementKeysValue, ArrayList.class);
      }
      if (props.get(ColumnDefinitions.DB_JOINS) != null) {
        String joinsValue = props.get(ColumnDefinitions.DB_JOINS);
        parseValue = joinsValue;
        joins = mapper.readValue(joinsValue, JoinColumn.class);
      }
    } catch (JsonParseException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("invalid db value: " + parseValue);
    } catch (JsonMappingException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("invalid db value: " + parseValue);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("invalid db value: " + parseValue);
    } finally {
      // TODO: fix the when to close problem
      // if ( db != null ) {
      // db.close();
      // }
    }
    return new ColumnProperties(dbh,
        props.get(ColumnDefinitions.DB_TABLE_ID),
        props.get(ColumnDefinitions.DB_ELEMENT_KEY),
        props.get(ColumnDefinitions.DB_ELEMENT_NAME),
        columnType,
        listChildElementKeys,
        joins,
        isPersisted,
        displayVisible,
        props.get(KEY_DISPLAY_NAME),
        displayChoicesMap,
        props.get(KEY_DISPLAY_FORMAT),
        smsIn,
        smsOut,
        props.get(KEY_SMS_LABEL),
        footerMode,
        backingStore);
    
    
  }
  
  
  /**
   * Return whether or not this is a typeOfStore appropriate for columns.
   * @param typeOfStore
   * @return
   */
  public static boolean isValidStore(KeyValueStore.Type typeOfStore) {
    return !(typeOfStore == KeyValueStore.Type.ACTIVE ||
      typeOfStore == KeyValueStore.Type.DEFAULT ||
      typeOfStore == KeyValueStore.Type.SERVER);
  }

  /**
   * Return the ColumnProperties for the PERSISTED columns belonging to this
   * table. 
   * TODO: this should probably be modified in the future to return both the
   * persisted and non persisted columns. At the moment ODK Tables only cares
   * about the persisted columns, and with this message returning only those
   * columns it removes the need to deal with non-persisted columns at this
   * juncture. 
   * @param dbh
   * @param tableId
   * @return
   */
  static ColumnProperties[] getColumnPropertiesForTable(DbHelper dbh, 
      String tableId) {
    SQLiteDatabase db = null;
    try {
      db = dbh.getReadableDatabase();
      List<String> elementKeys = 
          ColumnDefinitions.getPersistedElementKeysForTable(tableId, db);
      ColumnProperties[] cps = new ColumnProperties[elementKeys.size()];
      for (int i = 0; i < elementKeys.size(); i++) {
        ColumnProperties cp = getColumnProperties(dbh, tableId, 
            elementKeys.get(i), KeyValueStore.Type.COLUMN_ACTIVE);
        cps[i] = cp;
      }
      return cps;
    } finally {
//    // TODO: we need to resolve how we are going to prevent closing the
//    // db on callers. Removing this here, far far from ideal.
//    // if ( db != null ) {
//    // db.close();
//    // }
    }
    

//    SQLiteDatabase db = null;
//    Cursor c = null;
//    ColumnProperties[] cps = null;
//
//    try {
//      db = dbh.getReadableDatabase();
//      c = db.query(DB_TABLENAME, INIT_COLUMNS, DB_TABLE_ID + " = ?", new String[] { tableId },
//          null, null, null);
//      cps = new ColumnProperties[c.getCount()];
//
//      int dbcnIndex = c.getColumnIndexOrThrow(DB_ELEMENT_KEY);
//      int elementNameIndex = c.getColumnIndexOrThrow(DB_ELEMENT_NAME);
//      int elementTypeIndex = c.getColumnIndexOrThrow(DB_ELEMENT_TYPE);
//      int listChildElementKeysIndex = c.getColumnIndexOrThrow(DB_LIST_CHILD_ELEMENT_KEYS);
//      int joinsIndex = c.getColumnIndexOrThrow(DB_JOINS);
//      // int joinTableIndex = c.getColumnIndexOrThrow(DB_JOIN_TABLE_ID);
//      // int joinElementIndex = c.getColumnIndexOrThrow(DB_JOIN_ELEMENT_KEY);
//      int isPersistedIndex = c.getColumnIndexOrThrow(DB_IS_PERSISTED);
//
//      int displayVisibleIndex = c.getColumnIndexOrThrow(DB_DISPLAY_VISIBLE);
//      int displayNameIndex = c.getColumnIndexOrThrow(DB_DISPLAY_NAME);
//      int displayChoicesMapIndex = c.getColumnIndexOrThrow(DB_DISPLAY_CHOICES_MAP);
//      int displayFormatIndex = c.getColumnIndexOrThrow(DB_DISPLAY_FORMAT);
//
//      int smsInIndex = c.getColumnIndexOrThrow(DB_SMS_IN);
//      int smsOutIndex = c.getColumnIndexOrThrow(DB_SMS_OUT);
//      int smsLabelIndex = c.getColumnIndexOrThrow(DB_SMS_LABEL);
//
//      int footerModeIndex = c.getColumnIndexOrThrow(DB_FOOTER_MODE);
//
//      int i = 0;
//      c.moveToFirst();
//      while (i < cps.length) {
//        @SuppressWarnings("unchecked")
//        ArrayList<String> displayChoicesMap = null;
//        if (!c.isNull(displayChoicesMapIndex)) {
//          String displayChoicesMapValue = c.getString(displayChoicesMapIndex);
//          try {
//            displayChoicesMap = mapper.readValue(displayChoicesMapValue, ArrayList.class);
//          } catch (JsonParseException e) {
//            e.printStackTrace();
//            Log.e(TAG, "ignored expection");
//          } catch (JsonMappingException e) {
//            e.printStackTrace();
//            Log.e(TAG, "ignored expection");
//          } catch (IOException e) {
//            e.printStackTrace();
//            Log.e(TAG, "ignored expection");
//          }
//        }
//        ArrayList<String> listChildElementKeys = null;
//        if (!c.isNull(listChildElementKeysIndex)) {
//          String listChildElementKeysValue = c.getString(listChildElementKeysIndex);
//          try {
//            listChildElementKeys = mapper.readValue(listChildElementKeysValue, ArrayList.class);
//          } catch (JsonParseException e) {
//            e.printStackTrace();
//            Log.e(TAG, "ignored expection");
//          } catch (JsonMappingException e) {
//            e.printStackTrace();
//            Log.e(TAG, "ignored expection");
//          } catch (IOException e) {
//            e.printStackTrace();
//            Log.e(TAG, "ignored expection");
//          }
//        }
//        cps[i] = new ColumnProperties(dbh, tableId, c.getString(dbcnIndex),
//            c.getString(elementNameIndex), ColumnType.valueOf(c.getString(elementTypeIndex)),
//            listChildElementKeys, c.getString(joinsIndex),
//            // c.getString(joinTableIndex),
//            // c.getString(joinElementIndex),
//            c.getInt(isPersistedIndex) == 1,
//
//            c.getInt(displayVisibleIndex) == 1, c.getString(displayNameIndex), displayChoicesMap,
//            c.getString(displayFormatIndex),
//
//            c.getInt(smsInIndex) == 1, c.getInt(smsOutIndex) == 1, c.getString(smsLabelIndex),
//
//            c.getInt(footerModeIndex));
//        i++;
//        c.moveToNext();
//      }
//    } finally {
//      try {
//        if (c != null && !c.isClosed()) {
//          c.close();
//        }
//      } finally {
//        // TODO: we need to resolve how we are going to prevent closing the
//        // db on callers. Removing this here, far far from ideal.
//        // if ( db != null ) {
//        // db.close();
//        // }
//      }
//    }
//    return cps;
  }

  /**
   * Add a column to the datastore. elementKey and elementName should be 
   * made via createDbElementKey and createDbElementName to avoid conflicts.
   * A possible idea would be to pass them display name.
   * @param dbh
   * @param db
   * @param tableId
   * @param displayName 
   * @param elementKey
   * @param elementName
   * @param displayVisible
   * @return
   */
  static ColumnProperties addColumn(DbHelper dbh, SQLiteDatabase db, 
      String tableId,
      String displayName,
      String elementKey,
      String elementName,
      int displayVisible,
      KeyValueStore.Type typeOfStore) {
    // We're going to do this just by calling the corresponding methods on the
    // ColumnDefinitions and the key value store.
    
    if (!isValidStore(typeOfStore)) {
      Log.e(TAG, "store passed in to addColumn is not a valid column store" +
      		typeOfStore);
      throw new IllegalStateException("invalid column store passed to" +
      		"addColumn: " + typeOfStore);
    }
    
    // First prepare the entries for the key value store.
    List<KeyValueStoreColumnEntry> values = 
        new ArrayList<KeyValueStoreColumnEntry>();
    values.add(createIntEntry(tableId, elementKey, 
        KEY_DISPLAY_VISIBLE, displayVisible));
    values.add(createStringEntry(tableId, elementKey, KEY_DISPLAY_NAME, 
        displayName));
    values.add(createStringEntry(tableId, elementKey, KEY_DISPLAY_CHOICES_MAP,
        DEFAULT_KEY_DISPLAY_CHOICES_MAP));
    values.add(createStringEntry(tableId, elementKey, KEY_DISPLAY_FORMAT,
        DEFAULT_KEY_DISPLAY_FORMAT));
    // TODO: both the SMS entries should become booleans?
    values.add(createIntEntry(tableId, elementKey, KEY_SMS_IN, 
        DEFAULT_KEY_SMS_IN));
    values.add(createIntEntry(tableId, elementKey, KEY_SMS_OUT,
        DEFAULT_KEY_SMS_OUT));
    values.add(createStringEntry(tableId, elementKey, KEY_SMS_LABEL,
        DEFAULT_KEY_SMS_LABEL));
    values.add(createStringEntry(tableId, elementKey, KEY_FOOTER_MODE,
        DEFAULT_KEY_FOOTER_MODE.name()));
    Map<String, String> mapProps = new HashMap<String, String>();
    // TODO: might have to account for the null values being passed in here, 
    // maybe should be putting in empty strings instead?
    mapProps.put(KEY_DISPLAY_VISIBLE, String.valueOf(displayVisible));
    mapProps.put(KEY_DISPLAY_NAME, displayName);
    mapProps.put(KEY_DISPLAY_CHOICES_MAP, DEFAULT_KEY_DISPLAY_CHOICES_MAP);
    mapProps.put(KEY_DISPLAY_FORMAT, DEFAULT_KEY_DISPLAY_FORMAT);
    mapProps.put(KEY_SMS_IN, String.valueOf(DEFAULT_KEY_SMS_IN));
    mapProps.put(KEY_SMS_OUT, String.valueOf(DEFAULT_KEY_SMS_OUT));
    mapProps.put(KEY_SMS_LABEL, DEFAULT_KEY_SMS_LABEL);
    mapProps.put(KEY_FOOTER_MODE, DEFAULT_KEY_FOOTER_MODE.name());
    ColumnProperties cp = null;
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    try {
      db.beginTransaction();
      try {
        Map<String, String> columnDefProps = ColumnDefinitions.addColumn(db, 
            tableId, elementKey, elementName, 
            ColumnDefinitions.DEFAULT_DB_ELEMENT_TYPE, 
            DEFAULT_KEY_DISPLAY_CHOICES_MAP, 
            ColumnDefinitions.DEFAULT_DB_IS_PERSISTED,
            ColumnDefinitions.DEFAULT_DB_JOINS);
        KeyValueStoreColumn kvsc = kvsm.getStoreForColumn(tableId, elementKey, 
            typeOfStore);
        kvsc.addEntriesToColumnStore(db, values);
        mapProps.putAll(columnDefProps);
        cp = constructPropertiesFromMap(dbh, mapProps, typeOfStore);
        db.setTransactionSuccessful();
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        db.endTransaction();
      }
      return cp;
    } finally {
      // TODO: fix the when to close problem
//    db.close();     
    }
    
    
    
//    ContentValues values = new ContentValues();
//
//    values.put(DB_TABLE_ID, tableId);
//
//    values.put(DB_ELEMENT_KEY, elementKey);
//    values.put(DB_ELEMENT_NAME, elementKey);
//    values.put(DB_ELEMENT_TYPE, ColumnType.NONE.name());
//    values.putNull(DB_LIST_CHILD_ELEMENT_KEYS);
//    values.putNull(DB_JOINS);
//    // values.putNull(DB_JOIN_TABLE_ID);
//    // values.putNull(DB_JOIN_ELEMENT_KEY);
//    values.put(DB_IS_PERSISTED, 1);
//
//    values.put(DB_DISPLAY_VISIBLE, 1);
//    values.put(DB_DISPLAY_NAME, displayName);
//    values.putNull(DB_DISPLAY_CHOICES_MAP);
//    values.putNull(DB_DISPLAY_FORMAT);
//
//    values.put(DB_SMS_IN, 1);
//    values.put(DB_SMS_OUT, 1);
//    values.putNull(DB_SMS_LABEL);
//
//    values.put(DB_FOOTER_MODE, FooterMode.NONE);
//
//    db.insert(DB_TABLENAME, null, values);
//    return new ColumnProperties(dbh, tableId, elementKey, elementKey, ColumnType.NONE, null,
//    // null,
//    // null,
//        null, true,
//
//        true, displayName, null, null,
//
//        true, true, null,
//
//        FooterMode.NONE);
  }
  
//  /**
//   * Add a column when you are given the column properties. This is likely to
//   * be used for when you add a column based on re-claimed, de-json'd 
//   * ColumnProperties objects you get from the server.
//   * TODO: check for redundancies.
//   * @param cp
//   * @param tableId
//   * @param typeOfStore
//   * @param dbh
//   * @param db
//   */
//  static void addColumn(ColumnProperties cp, String tableId,
//      KeyValueStore.Type typeOfStore, DbHelper dbh, SQLiteDatabase db) {
//    // Get the things shared by lots of stuff.
//    String elementKey = cp.getElementKey();
//    String listChildMapStr = null;
//    String choicesMapStr = null;
//    try {
//      choicesMapStr = 
//          mapper.writeValueAsString(cp.getDisplayChoicesMap());
//      listChildMapStr = 
//          mapper.writeValueAsString(cp.getListChildElementKeys());
//    } catch (JsonGenerationException e1) {
//      e1.printStackTrace();
//    } catch (JsonMappingException e1) {
//      e1.printStackTrace();
//    } catch (IOException e1) {
//      e1.printStackTrace();
//    }
//    // First we want to make the entries for the store.
//    List<KeyValueStoreColumnEntry> values = 
//        new ArrayList<KeyValueStoreColumnEntry>();
//    values.add(createIntEntry(tableId, elementKey, 
//        KEY_DISPLAY_VISIBLE, cp.getDisplayVisible()));
//    values.add(createStringEntry(tableId, elementKey, KEY_DISPLAY_NAME, 
//        cp.getDisplayName()));
//    values.add(createStringEntry(tableId, elementKey, KEY_DISPLAY_CHOICES_MAP,
//        choicesMapStr));
//    values.add(createStringEntry(tableId, elementKey, KEY_DISPLAY_FORMAT,
//        cp.getDisplayFormat()));
//    // TODO: both the SMS entries should become booleans?
//    values.add(createIntEntry(tableId, elementKey, KEY_SMS_IN, 
//        SyncUtil.boolToInt(cp.getSmsIn())));
//    values.add(createIntEntry(tableId, elementKey, KEY_SMS_OUT,
//        SyncUtil.boolToInt(cp.getSmsOut())));
//    values.add(createStringEntry(tableId, elementKey, KEY_SMS_LABEL,
//        cp.getSmsLabel()));
//    values.add(createStringEntry(tableId, elementKey, KEY_FOOTER_MODE,
//        cp.getFooterMode().name()));
//    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
//    try {
//      db.beginTransaction();
//      try {
//        Map<String, String> columnDefProps = ColumnDefinitions.addColumn(db, 
//            tableId, elementKey, cp.getElementName(), 
//            cp.getElementType(), 
//            listChildMapStr, 
//            SyncUtil.boolToInt(cp.isPersisted()),
//            cp.getJoins());
//        KeyValueStoreColumn kvsc = kvsm.getStoreForColumn(tableId, 
//            cp.getElementKey(), 
//            typeOfStore);
//        kvsc.addEntriesToColumnStore(db, values);
////        mapProps.putAll(columnDefProps);
////        cp = constructPropertiesFromMap(dbh, mapProps, typeOfStore);
//        db.setTransactionSuccessful();
//      } catch (Exception e) {
//        e.printStackTrace();
//      } finally {
//        db.endTransaction();
//      }
//    } finally {
//      // TODO: fix the when to close problem
////    db.close();     
//    }      
//  }

  /**
   * Deletes the column represented by this ColumnProperties by deleting it
   * from the ColumnDefinitions table as well as the given key value store.
   * TODO: should probably delete the column from ALL the column key value
   * stores.
   * @param db
   */
  void deleteColumn(SQLiteDatabase db) {
    ColumnDefinitions.deleteColumn(tableId, elementKey, db);
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStoreColumn kvsc = kvsm.getStoreForColumn(tableId, elementKey, 
        backingStore);
    
//    int count = db.delete(DB_TABLENAME, WHERE_SQL, new String[] { String.valueOf(tableId),
//        elementKey });
//    if (count != 1) {
//      Log.e(ColumnProperties.class.getName(), "deleteColumn() deleted " + count + " rows");
//    }
  }
  
  private static KeyValueStoreColumnEntry createStringEntry(String tableId,
      String elementKey, String key, String value) {
    KeyValueStoreColumnEntry entry = new KeyValueStoreColumnEntry();
    entry.tableId = tableId;
    entry.elementKey = elementKey;
    entry.type = ColumnType.TEXT.name();
    entry.value = value;
    entry.key = key;
    return entry;
  }
  
  private static KeyValueStoreColumnEntry createIntEntry(String tableId,
      String elementKey, String key, int value) {
    KeyValueStoreColumnEntry entry = new KeyValueStoreColumnEntry();
    entry.tableId = tableId;
    entry.elementKey = elementKey;
    entry.type = ColumnType.INTEGER.name();
    entry.value = String.valueOf(value);
    entry.key = key;
    return entry;   
  }
  
  public static String createDbElementKey(String tableId, String proposedKey,
      SQLiteDatabase db) {
    String baseName = "_" + proposedKey.replace("\\W", "_");
    if (!keyConflict(tableId, baseName, db)) {
      return baseName;
    }
    // otherwise we need to create a non-conflicting key.
    int suffix = 1;
    while (true) {
      String nextName = baseName + suffix;
      if (!keyConflict(tableId, nextName, db)) {
        return nextName;
      }
      suffix++;
    }
  }
  
  public static String createDbElementName(String tableId, String proposedName,
      SQLiteDatabase db) {
    String baseName = "_" + proposedName.replace("\\W", "_");
    if (!nameConflict(tableId, baseName, db)) {
      return baseName;
    }
    // otherwise we need to create a non-conflicting key.
    int suffix = 1;
    while (true) {
      String nextName = baseName + suffix;
      if (!nameConflict(tableId, nextName, db)) {
        return nextName;
      }
      suffix++;
    }   
  }
  
  private static boolean keyConflict(String tableId, String elementKey,
      SQLiteDatabase db) {
    List<String> existingKeys = 
        ColumnDefinitions.getAllElementKeysForTable(tableId, db);
    for (String existingKey : existingKeys) {
      if (existingKey.equals(elementKey)) {
        return true;
      }
    }
    return false;
  }
  
  private static boolean nameConflict(String tableId, String elementName,
      SQLiteDatabase db) {
    List<String> existingNames = 
        ColumnDefinitions.getAllElementNamesForTable(tableId, db);
    for (String existingName : existingNames) {
      if (existingName.equals(elementName)) {
        return true;
      }
    }
    return false;   
  }

  /**
   * DB_ELEMENT_KEY, DB_ELEMENT_NAME, DB_ELEMENT_TYPE,
   * DB_LIST_CHILD_ELEMENT_KEYS, DB_JOIN_TABLE_ID, DB_JOIN_ELEMENT_KEY,
   * DB_IS_PERSISTED,
   * 
   * DB_DISPLAY_VISIBLE, DB_DISPLAY_NAME, DB_DISPLAY_CHOICES_MAP,
   * DB_DISPLAY_FORMAT,
   * 
   * DB_SMS_IN, DB_SMS_OUT, DB_SMS_LABEL,
   * 
   * DB_FOOTER_MODE
   */

  /**
   * @return the column's name in the database
   */
  public String getColumnDbName() {
    return elementKey;
  }

  public String getElementKey() {
    return elementKey;
  }

  public String getElementName() {
    return elementName;
  }

//  public void setElementName(String elementName) {
//    setStringProperty(ColumnDefinitions.DB_ELEMENT_NAME, elementName);
//    this.elementName = elementName;
//  }

  // TODO: remove this
  public ColumnType getElementType() {
    return elementType;
  }

  // TODO: remove this
  public void setElementType(ColumnType elementType) {
    setStringProperty(ColumnDefinitions.DB_ELEMENT_TYPE, elementType.name());
    this.elementType = elementType;
  }

  /**
   * @return the column's type
   */
  public ColumnType getColumnType() {
    return elementType;
  }

  /**
   * Sets the column's type.
   * 
   * @param columnType
   *          the new type
   */
  public void setColumnType(ColumnType columnType) {
    TableProperties tp = TableProperties.getTablePropertiesForTable(dbh, tableId,
        KeyValueStore.Type.ACTIVE);
    ArrayList<String> colOrder = tp.getColumnOrder();
    tp.getColumns(); // ensuring columns are initialized
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
      db.beginTransaction();
      setStringProperty(db, ColumnDefinitions.DB_ELEMENT_TYPE, 
          columnType.name());
      tp.reformTable(db, colOrder);
      db.setTransactionSuccessful();
      db.endTransaction();
    } finally {
      // TODO: fix the when to close problem
      // if ( db != null ) {
      // db.close();
      // }
    }
    this.elementType = columnType;
  }

  public List<String> getListChildElementKeys() {
    return listChildElementKeys;
  }

  public void setListChildElementKeys(ArrayList<String> listChildElementKeys) {
    this.listChildElementKeys = listChildElementKeys;
  }

  public boolean isPersisted() {
    return isPersisted;
  }

  public void setIsPersisted(boolean setting) {
    setIntProperty(ColumnDefinitions.DB_IS_PERSISTED, setting ? 1 : 0);
    this.isPersisted = setting;
  }

  /**
   * @return whether or not this column is visible within Tables
   */
  public int getDisplayVisible() {
    return displayVisible;
  }

  /**
   * Sets whether or not this column is visible within Tables
   * 
   * @param setting
   *          the new display visibility setting
   */
  public void setDisplayVisible(int setting) {
    setIntProperty(KEY_DISPLAY_VISIBLE, setting);
    this.displayVisible = setting;
  }

  /**
   * @return the column's display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Sets the column's display name.
   * 
   * @param displayName
   *          the new display name
   */
  public void setDisplayName(String displayName) {
    setStringProperty(KEY_DISPLAY_NAME, displayName);
    this.displayName = displayName;
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
  public void setDisplayFormat(String format) {
    setStringProperty(KEY_DISPLAY_FORMAT, format);
    this.displayFormat = format;
  }

  /**
   * @return the column's footer mode
   */
  public FooterMode getFooterMode() {
    return footerMode;
  }

  /**
   * Sets the column's footer mode.
   * 
   * @param footerMode
   *          the new footer mode
   */
  public void setFooterMode(FooterMode footerMode) {
    setStringProperty(KEY_FOOTER_MODE, footerMode.name());
    this.footerMode = footerMode;
  }

  /**
   * @return the column's abbreviation (or null for no abbreviation)
   */
  public String getSmsLabel() {
    return smsLabel;
  }

  /**
   * Sets the column's abbreviation.
   * 
   * @param abbreviation
   *          the new abbreviation (or null for no abbreviation)
   */
  public void setSmsLabel(String abbreviation) {
    setStringProperty(KEY_SMS_LABEL, abbreviation);
    this.smsLabel = abbreviation;
  }

  /**
   * @return the SMS-in setting
   */
  public boolean getSmsIn() {
    return smsIn;
  }

  /**
   * Sets the SMS-in setting.
   * 
   * @param setting
   *          the new SMS-in setting
   */
  public void setSmsIn(boolean setting) {
    setIntProperty(KEY_SMS_IN, setting ? 1 : 0);
    this.smsIn = setting;
  }

  /**
   * @return the SMS-out setting
   */
  public boolean getSmsOut() {
    return smsOut;
  }

  /**
   * Sets the SMS-out setting.
   * 
   * @param setting
   *          the new SMS-out setting
   */
  public void setSmsOut(boolean setting) {
    setIntProperty(KEY_SMS_OUT, setting ? 1 : 0);
    this.smsOut = setting;
  }

  /**
   * @return an array of the multiple-choice options
   */
  public ArrayList<String> getDisplayChoicesMap() {
    return displayChoicesMap;
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
  public void setDisplayChoicesMap(ArrayList<String> options) {
    String encoding;
    try {
      encoding = mapper.writeValueAsString(options);
      setStringProperty(KEY_DISPLAY_CHOICES_MAP, encoding);
      displayChoicesMap = options;
    } catch (JsonGenerationException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("failed JSON toString conversion: " + options.toString());
    } catch (JsonMappingException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("failed JSON toString conversion: " + options.toString());
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("failed JSON toString conversion: " + options.toString());
    }
  }

  /**
   * @return the join table ID
   */
  // public String getJoinTableId() {
  // return joinTableId;
  // }

  public JoinColumn getJoins() {
    return joins;
  }

  /**
   * Converts the JoinColumn to the json representation of the object 
   * using a mapper and adds it to the database.
   * <p>
   * If there is a mapping exception of writing the JoinColumn to a String, it
   * does nothing, leaving the database untouched.
   * @param joins
   */
  public void setJoins(JoinColumn joins) {
    mapper.setVisibilityChecker(mapper.getVisibilityChecker()
        .withFieldVisibility(Visibility.ANY));
    String joinsStr = null;
    try {
      joinsStr = mapper.writeValueAsString(joins);
    } catch (JsonGenerationException e) {
      Log.e(TAG, "JsonGenerationException writing joins in ColumnProperties");
      e.printStackTrace();
    } catch (JsonMappingException e) {
      Log.e(TAG, "JsonMappingException writing joins in ColumnProperties");
      e.printStackTrace();
    } catch (IOException e) {
      Log.e(TAG, "IOException writing joins in ColumnProperties");
      e.printStackTrace();
    } 
    if (joinsStr != null) {
      setStringProperty(ColumnDefinitions.DB_JOINS, joinsStr);
      this.joins = joins;
    }
  }

  /**
   * Sets the join table ID.
   * 
   * @param tableId
   *          the join table Id
   */
  // public void setJoinTableId(String tableId) {
  // setStringProperty(DB_JOIN_TABLE_ID, tableId);
  // joinTableId = tableId;
  // }
  //
  // public String getJoinElementKey() {
  // return joinElementKey;
  // }
  //
  // public void setJoinElementKey(String joinElementKey) {
  // setStringProperty(DB_JOIN_ELEMENT_KEY, tableId);
  // this.joinElementKey = joinElementKey;
  // }

  // TODO: rename to getJoinElementKey()
  /**
   * @return the join table column name
   */
  // public String getJoinColumnName() {
  // return joinElementKey;
  // }

  /**
   * Sets the join column name.
   * 
   * @param columnName
   *          the join column name
   */
  // public void setJoinColumnName(String columnName) {
  // setStringProperty(DB_JOIN_ELEMENT_KEY, columnName);
  // joinElementKey = columnName;
  // }

//  Map<String, Object> toJson() {
  String toJson() {
    mapper.setVisibilityChecker(mapper.getVisibilityChecker()
        .withFieldVisibility(Visibility.ANY));
    Map<String, Object> jo = new HashMap<String, Object>();
    jo.put(JSON_KEY_VERSION, 1);
    jo.put(JSON_KEY_TABLE_ID, tableId);

    jo.put(JSON_KEY_ELEMENT_KEY, elementKey);
    jo.put(JSON_KEY_ELEMENT_NAME, elementName);
    //jo.put(JSON_KEY_ELEMENT_TYPE, elementType);
    String elType = null;
    String footMode = null;
    try {
      elType = mapper.writeValueAsString(elementType);
      footMode = mapper.writeValueAsString(footerMode);
    } catch (JsonGenerationException e) {
      e.printStackTrace();
   } catch (JsonMappingException e) {
      e.printStackTrace();
   } catch (IOException e) {
      e.printStackTrace();
   }
    jo.put(JSON_KEY_ELEMENT_TYPE, elType);
    jo.put(JSON_KEY_FOOTER_MODE, footMode);
//    jo.put(JSON_KEY_FOOTER_MODE, footerMode);
    jo.put(JSON_KEY_LIST_CHILD_ELEMENT_KEYS, listChildElementKeys);
    jo.put(JSON_KEY_JOINS, joins);
    // jo.put(JSON_KEY_JOIN_TABLE_ID, joinTableId);
    // jo.put(JSON_KEY_JOIN_ELEMENT_KEY, joinElementKey);
    jo.put(JSON_KEY_IS_PERSISTED, isPersisted);

    jo.put(JSON_KEY_DISPLAY_VISIBLE, displayVisible);
    jo.put(JSON_KEY_DISPLAY_NAME, displayName);
    jo.put(JSON_KEY_DISPLAY_CHOICES_MAP, displayChoicesMap);
    jo.put(JSON_KEY_DISPLAY_FORMAT, displayFormat);

    jo.put(JSON_KEY_SMS_IN, smsIn);
    jo.put(JSON_KEY_SMS_OUT, smsOut);
    jo.put(JSON_KEY_SMS_LABEL, smsLabel);
    
    String toReturn = null;
    try {
      // I think this removes exceptions from not having getters/setters...
      toReturn = mapper.writeValueAsString(jo);
    } catch (JsonGenerationException e) {
      e.printStackTrace();
   } catch (JsonMappingException e) {
      e.printStackTrace();
   } catch (IOException e) {
      e.printStackTrace();
   }
    Log.d(TAG, "json: " + toReturn);
//    return jo;
    return toReturn;
  }
  
  /**
   * This should be called when you first are creating a ColumnProperties when
   * you have a json. Eg when you are downloading a table from the server.
   * This gives you the object, but persists nothing. You must then add it to
   * the datastore.
   * @param json
   * @return
   */
  // TODO it might be best to give options to set the appropriate things to be
  // unique. Couldn't do it here b/c can't look at schema, just trying to
  // get it to work.
  public static ColumnProperties constructColumnPropertiesFromJson(
      DbHelper dbh,
      String json) {
    ColumnProperties cp = new ColumnProperties(dbh,
        "",
        "",
        "",
        ColumnType.NONE,
        new ArrayList<String>(),
        null,
        true,
        1,
        "",
        new ArrayList<String>(),
        "",
        true,
        true,
        "",
        FooterMode.none,
        KeyValueStore.Type.COLUMN_ACTIVE);
    cp.setFromJson(json);
    return cp;
    
  }
  
  


//  void setFromJsonObject(Map<String, Object> jo) {
  void setFromJson(String json) {
    mapper.setVisibilityChecker(mapper.getVisibilityChecker()
        .withFieldVisibility(Visibility.ANY));
    Map<String, Object> jo;
    try {
      jo = mapper.readValue(json, Map.class);
    } catch (JsonParseException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("invalid json: " + json);
   } catch (JsonMappingException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("invalid json: " + json);
   } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("invalid json: " + json);
   }

    jo.put(JSON_KEY_VERSION, 1);
    jo.put(JSON_KEY_TABLE_ID, tableId);

    jo.put(JSON_KEY_ELEMENT_KEY, elementKey);

//    setElementName((String) jo.get(JSON_KEY_ELEMENT_NAME));
//    setElementType(ColumnType.valueOf(
//        (String) jo.get(JSON_KEY_ELEMENT_TYPE)));
    String joElType = (String) jo.get(JSON_KEY_ELEMENT_TYPE);
    String joFootMode = (String) jo.get(JSON_KEY_FOOTER_MODE);
    String joJoins = (String) jo.get(JSON_KEY_JOINS);
    ColumnType colType = null;
    FooterMode footMode = null;
    JoinColumn joins = null;
    try {
      colType = mapper.readValue(joElType, ColumnType.class);
      footMode = mapper.readValue(joFootMode, FooterMode.class);
      joins = mapper.readValue(joJoins, JoinColumn.class);
    } catch (JsonParseException e) {
      e.printStackTrace();
    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } 
    // do a check just in case there was an error
    if (colType == null) {
      colType = ColumnType.NONE;
    }
    if (footMode == null) {
      footMode = FooterMode.none;
    }
    setElementType(colType);
    setFooterMode(footMode);
    setJoins(joins);
    setListChildElementKeys(
        (ArrayList<String>) jo.get(JSON_KEY_LIST_CHILD_ELEMENT_KEYS));
    // setJoinTableId((String) jo.get(JSON_KEY_JOIN_TABLE_ID));
    // setJoinColumnName((String) jo.get(JSON_KEY_JOIN_ELEMENT_KEY));
    setIsPersisted((Boolean) jo.get(JSON_KEY_IS_PERSISTED));

    setDisplayVisible((Integer) jo.get(JSON_KEY_DISPLAY_VISIBLE));
    setDisplayName((String) jo.get(JSON_KEY_DISPLAY_NAME));
    setDisplayChoicesMap(
        (ArrayList<String>) jo.get(JSON_KEY_DISPLAY_CHOICES_MAP));
    setDisplayFormat((String) jo.get(JSON_KEY_DISPLAY_FORMAT));

    setSmsIn((Boolean) jo.get(JSON_KEY_SMS_IN));
    setSmsOut((Boolean) jo.get(JSON_KEY_SMS_OUT));
    setSmsLabel((String) jo.get(JSON_KEY_SMS_LABEL));

//    jo.put(JSON_KEY_FOOTER_MODE, footerMode);
//    setFooterMode(FooterMode.valueOf((String) jo.get(JSON_KEY_FOOTER_MODE)));
  }

  private void setIntProperty(String property, int value) {
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
      setIntProperty(db, property, value);
    } finally {
      // TODO: fix the when to close problem
      // db.close();
    }
  }

  private void setIntProperty(SQLiteDatabase db, String property, int value) {
    if (ColumnDefinitions.columnNames.contains(property)) {
      ColumnDefinitions.setValue(tableId, elementKey, property, value, db);
    } else {
      // or a kvs property?
      KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
      KeyValueStoreColumn kvsc = kvsm.getStoreForColumn(tableId, elementKey, 
          backingStore);
      kvsc.insertOrUpdateKey(db, ColumnType.INTEGER.name(), property, 
          Integer.toString(value));
    }
    Log.d(TAG, "updated int property " + property + " to " + value +
        " for table " + tableId + ", column " + elementKey);
//    ContentValues values = new ContentValues();
//    values.put(property, value);
//    int count = db.update(DB_TABLENAME, values, WHERE_SQL, whereArgs);
//    if (count != 1) {
//      Log.e(ColumnProperties.class.getName(), "setting " + property + " updated " + count + " rows");
//    }
  }

  private void setStringProperty(String property, String value) {
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
      setStringProperty(db, property, value);
    } finally {
      // TODO: fix the when to close problem
      // db.close();
    }
  }

  private void setStringProperty(SQLiteDatabase db, String property, 
      String value) {
    // is it a column definition property?
    if (ColumnDefinitions.columnNames.contains(property)) {
      ColumnDefinitions.setValue(tableId, elementKey, property, value, db);
    } else {
      // or a kvs property?
      KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
      KeyValueStoreColumn kvsc = kvsm.getStoreForColumn(tableId, elementKey, 
          backingStore);
      kvsc.insertOrUpdateKey(db, ColumnType.TEXT.name(), property, value);
    }
    Log.d(TAG, "updated string property " + property + " to " + value +
        " for table " + tableId + ", column " + elementKey);
//    ContentValues values = new ContentValues();
//    values.put(property, value);
//    int count = db.update(DB_TABLENAME, values, WHERE_SQL, whereArgs);
//    if (count != 1) {
//      Log.e(ColumnProperties.class.getName(), "setting " + property + " updated " + count + " rows");
//    }
  }

  // static String getTableCreateSql() {
  // return "CREATE TABLE " + DB_TABLENAME + "(" +
  // DB_TABLE_ID + " TEXT NOT NULL" +
  // ", " + DB_ELEMENT_KEY + " TEXT NOT NULL" +
  // ", " + DB_ELEMENT_NAME + " TEXT NOT NULL" +
  // ", " + DB_ELEMENT_TYPE + " TEXT NOT NULL" +
  // ", " + DB_LIST_CHILD_ELEMENT_KEYS + " TEXT NULL" +
  // ", " + DB_JOINS + " TEXT NULL" +
  // // ", " + DB_JOIN_TABLE_ID + " TEXT NULL" +
  // // ", " + DB_JOIN_ELEMENT_KEY + " TEXT NULL" +
  // ", " + DB_IS_PERSISTED + " INTEGER NOT NULL" +
  //
  // ", " + DB_DISPLAY_VISIBLE + " INTEGER NOT NULL" +
  // ", " + DB_DISPLAY_NAME + " TEXT NOT NULL" +
  // ", " + DB_DISPLAY_CHOICES_MAP + " TEXT NULL" +
  // ", " + DB_DISPLAY_FORMAT + " TEXT NULL" +
  //
  // ", " + DB_SMS_IN + " INTEGER NOT NULL" +
  // ", " + DB_SMS_OUT + " INTEGER NOT NULL" +
  // ", " + DB_SMS_LABEL + " TEXT NULL" +
  //
  // ", " + DB_FOOTER_MODE + " TEXT NOT NULL" +
  // ")";
  // }
}
