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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.opendatakit.aggregate.odktables.rest.KeyValueStoreConstants;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesKeyValueStoreEntry;
import org.opendatakit.common.android.provider.ColumnDefinitionsColumns;
import org.opendatakit.tables.sync.SyncUtil;

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

  private static final ObjectMapper mapper;
  static {
    mapper = new ObjectMapper();
    mapper.setVisibilityChecker(mapper.getVisibilityChecker().withFieldVisibility(Visibility.ANY));
  }

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
   * if this is a composite type (geopoint), this is a JSON list of the element
   * keys of the direct descendants of this field name.
   */
  // private static final String DB_IS_UNIT_OF_RETENTION =
  // "is_unit_of_retention";
  /*
   * default: 1 (true) -- whether or not this is a column in the underlying
   * database table. If true, elementKey is the dbColumnName in which this
   * value is written. The value will be written as JSON if it is a composite
   * type. see larger comment below for example.
   */
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

  // /***********************************
  // * Default values for those columns that have defaults.
  // ***********************************/
  // public static final int DEFAULT_DB_IS_UNIT_OF_RETENTION = 1;

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
  /*
   * Text, null. Fot future use.
   */
  public static final String KEY_SMS_IN = KeyValueStoreConstants.COLUMN_SMS_IN;
  /*
   * Integer, not null. As boolean. Allow incoming SMS to modify the column.
   */
  public static final String KEY_SMS_OUT = KeyValueStoreConstants.COLUMN_SMS_OUT;
  /*
   * Integer, not null. As boolean. Allow outgoing SMS to access this column.
   */
  public static final String KEY_SMS_LABEL = KeyValueStoreConstants.COLUMN_SMS_LABEL;
  /*
   * Text null.
   */
  public static final String KEY_FOOTER_MODE = KeyValueStoreConstants.COLUMN_FOOTER_MODE;
  /*
   * What the footer should display.
   */

  /***********************************
   * Default values for those keys which require them. TODO When the keys in the
   * KVS are moved to the respective classes that use them, these should go
   * there most likely.
   ***********************************/
  public static final boolean DEFAULT_KEY_VISIBLE = true;
  public static final FooterMode DEFAULT_KEY_FOOTER_MODE = FooterMode.none;
  public static final boolean DEFAULT_KEY_SMS_IN = true;
  public static final boolean DEFAULT_KEY_SMS_OUT = true;
  public static final String DEFAULT_KEY_SMS_LABEL = null;
  public static final String DEFAULT_KEY_DISPLAY_FORMAT = null;
  public static final ArrayList<String> DEFAULT_KEY_DISPLAY_CHOICES_MAP = new ArrayList<String>();

  /***********************************
   * Keys for json.
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
  private static final String JSON_KEY_IS_UNIT_OF_RETENTION = "isUnitOfRetention";

  private static final String JSON_KEY_DISPLAY_VISIBLE = "displayVisible";
  private static final String JSON_KEY_DISPLAY_NAME = "displayName";
  private static final String JSON_KEY_DISPLAY_CHOICES_LIST = "displayChoicesList";
  private static final String JSON_KEY_DISPLAY_FORMAT = "displayFormat";

  private static final String JSON_KEY_SMS_IN = "smsIn";
  private static final String JSON_KEY_SMS_OUT = "smsOut";
  private static final String JSON_KEY_SMS_LABEL = "smsLabel";

  private static final String JSON_KEY_FOOTER_MODE = "footerMode";

  // the SQL where clause to use for selecting, updating,
  // or deleting the row for a given column
  // private static final String WHERE_SQL = DB_TABLE_ID + " = ? and "
  // + DB_ELEMENT_KEY + " = ?";

  // the columns to be selected when initializing ColumnProperties
  // private static final String[] INIT_COLUMNS = {
  // KeyValueStoreManager.TABLE_ID, DB_ELEMENT_KEY,
  // DB_ELEMENT_NAME, DB_ELEMENT_TYPE, DB_LIST_CHILD_ELEMENT_KEYS, DB_JOINS,
  // // DB_JOIN_TABLE_ID,
  // // DB_JOIN_ELEMENT_KEY,
  // DB_IS_UNIT_OF_RETENTION,

  // DB_DISPLAY_VISIBLE,
  // DB_DISPLAY_NAME,
  // DB_DISPLAY_CHOICES_MAP,
  // DB_DISPLAY_FORMAT,
  // DB_SMS_IN,
  // DB_SMS_OUT,
  // DB_SMS_LABEL,
  // DB_FOOTER_MODE
  // };

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
   * The fields that make up a ColumnProperties object.
   ***********************************/
  /*
   * The fields that belong only to the object, and are not related to the
   * actual column itself.
   */
  private final DbHelper dbh;
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
  private boolean isUnitOfRetention;
  /*
   * The fields that reside in the key value store.
   */
  private boolean displayVisible;
  private String jsonStringifyDisplayName;
  private String displayName;
  private ArrayList<String> displayChoicesList;
  private String displayFormat;
  private boolean smsIn;
  private boolean smsOut;
  private String smsLabel;
  private FooterMode footerMode;
  private ArrayList<JoinColumn> joins;

  /**
   *
   * @param dbh
   * @param tableId
   * @param elementKey
   * @param elementName
   * @param elementType
   * @param listChildElementKeys
   * @param isUnitOfRetention
   * @param displayVisible
   * @param jsonStringifyDisplayName -- wrapped via mapper.writeValueAsString()
   * @param displayChoicesList
   * @param displayFormat
   * @param smsIn
   * @param smsOut
   * @param smsLabel
   * @param footerMode
   * @param joins
   * @param backingStore
   */
  private ColumnProperties(DbHelper dbh, String tableId, String elementKey, String elementName,
      ColumnType elementType, List<String> listChildElementKeys,
      boolean isUnitOfRetention, boolean displayVisible, String jsonStringifyDisplayName,
      ArrayList<String> displayChoicesList, String displayFormat, boolean smsIn, boolean smsOut,
      String smsLabel, FooterMode footerMode, ArrayList<JoinColumn> joins, KeyValueStore.Type backingStore) {
    this.dbh = dbh;
    this.tableId = tableId;
    this.elementKey = elementKey;
    this.elementName = elementName;
    this.elementType = elementType;
    this.listChildElementKeys = listChildElementKeys;
    this.joins = joins;
    this.isUnitOfRetention = isUnitOfRetention;
    this.displayVisible = displayVisible;
    this.jsonStringifyDisplayName = jsonStringifyDisplayName;
    updateDisplayNameFromJsonStringifyDisplayName();
    this.displayChoicesList = displayChoicesList;
    this.displayFormat = displayFormat;
    this.smsIn = smsIn;
    this.smsOut = smsOut;
    this.smsLabel = smsLabel;
    this.footerMode = footerMode;
    this.backingStore = backingStore;
  }

  /**
   * Return the ColumnProperties for all the columns in a table, whether or not
   * they are written to the database table.
   *
   * @param dbh
   * @param tableId
   * @return a map of elementKey to ColumnProperties for all columns.
   */
  static Map<String, ColumnProperties> getColumnPropertiesForTable(DbHelper dbh, String tableId,
      KeyValueStore.Type typeOfStore) {
    SQLiteDatabase db = null;
    try {
      db = dbh.getReadableDatabase();
      List<String> elementKeys = ColumnDefinitions.getAllColumnNamesForTable(tableId, db);
      Map<String, ColumnProperties> elementKeyToColumnProperties = new HashMap<String, ColumnProperties>();
      for (int i = 0; i < elementKeys.size(); i++) {
        ColumnProperties cp = getColumnProperties(dbh, tableId, elementKeys.get(i), typeOfStore);
        elementKeyToColumnProperties.put(elementKeys.get(i), cp);
      }
      return elementKeyToColumnProperties;
    } finally {
      // // TODO: we need to resolve how we are going to prevent closing the
      // // db on callers. Removing this here, far far from ideal.
      // // if ( db != null ) {
      // // db.close();
      // // }
    }
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
  private static ColumnProperties getColumnProperties(DbHelper dbh, String tableId,
      String elementKey, KeyValueStore.Type typeOfStore) {

    SQLiteDatabase db = dbh.getReadableDatabase();

    // Get the KVS values
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStore intendedKVS = kvsm.getStoreForTable(tableId, typeOfStore);
    Map<String, String> kvsMap = intendedKVS.getKeyValues(ColumnProperties.KVS_PARTITION,
        elementKey, db);

    // Get the ColumnDefinition entries
    Map<String, String> columnDefinitionsMap = ColumnDefinitions.getColumnDefinitionFields(tableId,
        elementKey, db);

    return constructPropertiesFromMap(dbh, tableId, elementKey, columnDefinitionsMap, kvsMap,
        typeOfStore);
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
  private static ColumnProperties constructPropertiesFromMap(DbHelper dbh, String tableId,
      String elementKey, Map<String, String> columnDefinitions, Map<String, String> kvsProps,
      KeyValueStore.Type backingStore) {
    // First convert the non-string types to their appropriate types. This is
    // probably going to go away when the map becomes key->TypeValuePair.
    // KEY_SMS_IN
    String smsInStr = kvsProps.get(KEY_SMS_IN);
    boolean smsIn = SyncUtil.stringToBool(smsInStr);
    // KEY_SMS_OUT
    String smsOutStr = kvsProps.get(KEY_SMS_OUT);
    boolean smsOut = SyncUtil.stringToBool(smsOutStr);
    // KEY_DISPLAY_VISIBLE
    String displayVisibleStr = kvsProps.get(KEY_DISPLAY_VISIBLE);
    boolean displayVisible = SyncUtil.stringToBool(displayVisibleStr);
    // KEY_FOOTER_MODE
    String footerModeStr = kvsProps.get(KEY_FOOTER_MODE);
    // TODO don't forget that all of these value ofs for all these enums
    // should eventually be surrounded with try/catch to support versioning
    // when new values might come down from the server.
    FooterMode footerMode = (footerModeStr == null) ? DEFAULT_KEY_FOOTER_MODE : FooterMode
        .valueOf(footerModeStr);
    // KEY_JOINS

    // DB_IS_UNIT_OF_RETENTION
    String isUnitOfRetentionStr = columnDefinitions.get(ColumnDefinitionsColumns.IS_UNIT_OF_RETENTION);
    boolean isUnitOfRetention = SyncUtil.stringToBool(isUnitOfRetentionStr);
    // DB_COLUMN_TYPE
    String columnTypeStr = columnDefinitions.get(ColumnDefinitionsColumns.ELEMENT_TYPE);
    ColumnType columnType = ColumnType.valueOf(columnTypeStr);

    // Now we need to reclaim the list values from their db entries.
    String parseValue = null;
    ArrayList<String> displayChoicesList = null;
    ArrayList<String> listChildElementKeys = null;
    ArrayList<JoinColumn> joins = null;
    try {
      if (kvsProps.get(KEY_DISPLAY_CHOICES_LIST) != null) {
        String displayChoicesListValue = kvsProps.get(KEY_DISPLAY_CHOICES_LIST);
        parseValue = displayChoicesListValue;
        displayChoicesList = mapper.readValue(displayChoicesListValue, ArrayList.class);
      }

      if (columnDefinitions.get(ColumnDefinitionsColumns.LIST_CHILD_ELEMENT_KEYS) != null) {
        String listChildElementKeysValue = columnDefinitions
            .get(ColumnDefinitionsColumns.LIST_CHILD_ELEMENT_KEYS);
        parseValue = listChildElementKeysValue;
        listChildElementKeys = mapper.readValue(listChildElementKeysValue, ArrayList.class);
      }

      String joinsValue = kvsProps.get(KEY_JOINS);
      if ( joinsValue != null ) {
        parseValue = joinsValue;
        joins = JoinColumn.fromSerialization(joinsValue);
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
    return new ColumnProperties(dbh, tableId, elementKey,
        columnDefinitions.get(ColumnDefinitionsColumns.ELEMENT_NAME), columnType,
        listChildElementKeys, isUnitOfRetention, displayVisible, kvsProps.get(KEY_DISPLAY_NAME) /** JSON.stringify()'d */,
        displayChoicesList, kvsProps.get(KEY_DISPLAY_FORMAT), smsIn, smsOut,
        kvsProps.get(KEY_SMS_LABEL), footerMode, joins, backingStore);
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
   * @param tableId
   * @throws JsonGenerationException
   * @throws JsonMappingException
   * @throws IOException
   */
  void persistColumn(SQLiteDatabase db, String tableId) throws JsonGenerationException,
      JsonMappingException, IOException {
    // First prepare the entries for the key value store.
    List<OdkTablesKeyValueStoreEntry> values = new ArrayList<OdkTablesKeyValueStoreEntry>();
    values.add(createBooleanEntry(tableId, ColumnProperties.KVS_PARTITION, elementKey,
        KEY_DISPLAY_VISIBLE, displayVisible));
    values.add(createStringEntry(tableId, ColumnProperties.KVS_PARTITION, elementKey,
        KEY_DISPLAY_NAME, jsonStringifyDisplayName));
    values.add(createStringEntry(tableId, ColumnProperties.KVS_PARTITION, elementKey,
        KEY_DISPLAY_CHOICES_LIST, mapper.writeValueAsString(displayChoicesList)));
    values.add(createStringEntry(tableId, ColumnProperties.KVS_PARTITION, elementKey,
        KEY_DISPLAY_FORMAT, displayFormat));
    // TODO: both the SMS entries should become booleans?
    values.add(createBooleanEntry(tableId, ColumnProperties.KVS_PARTITION, elementKey, KEY_SMS_IN,
        smsIn));
    values.add(createBooleanEntry(tableId, ColumnProperties.KVS_PARTITION, elementKey, KEY_SMS_OUT,
        smsOut));
    values.add(createStringEntry(tableId, ColumnProperties.KVS_PARTITION, elementKey,
        KEY_SMS_LABEL, smsLabel));
    values.add(createStringEntry(tableId, ColumnProperties.KVS_PARTITION, elementKey,
        KEY_FOOTER_MODE, footerMode.name()));
    values.add(createStringEntry(tableId, ColumnProperties.KVS_PARTITION, elementKey,
        KEY_JOINS, JoinColumn.toSerialization(joins)));

    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStore kvs = kvsm.getStoreForTable(tableId, backingStore);
    kvs.addEntriesToStore(db, values);

    ColumnDefinitions.assertColumnDefinition(db, tableId, elementKey, elementName, elementType,
        mapper.writeValueAsString(listChildElementKeys), isUnitOfRetention);
  }

  public enum ColumnDefinitionChange {
    IDENTICAL, SAME_ELEMENT_TYPE, CHANGE_ELEMENT_TYPE, INCOMPATIBLE
  };

  /**
   * Determine if the changes between the current and supplied cp are
   * incompatible (cannot be reconciled), require altering the column type,
   * and/or require modifying the joins definition.
   *
   * @param cp
   * @return
   */
  ColumnDefinitionChange compareColumnDefinitions(ColumnProperties cp) {
    if (!this.getElementName().equals(cp.getElementName())) {
      return ColumnDefinitionChange.INCOMPATIBLE;
    }
    if (!this.getListChildElementKeys().equals(cp.getListChildElementKeys())) {
      return ColumnDefinitionChange.INCOMPATIBLE;
    }
    if (this.isUnitOfRetention() != cp.isUnitOfRetention()) {
      return ColumnDefinitionChange.INCOMPATIBLE;
    }
    if (this.getColumnType() != cp.getColumnType()) {
      return ColumnDefinitionChange.CHANGE_ELEMENT_TYPE;
    } else {
      // TODO: could save some reloading if we determine that
      // the two definitions are identical. For now, assume
      // they are not.
      return ColumnDefinitionChange.SAME_ELEMENT_TYPE;
    }
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
  static ColumnProperties createNotPersisted(DbHelper dbh, String tableId,
      String jsonStringifyDisplayName, String elementKey, String elementName, ColumnType columnType,
      List<String> listChildElementKeys, boolean isUnitOfRetention,
      boolean displayVisible, KeyValueStore.Type typeOfStore) {

    ColumnProperties cp = new ColumnProperties(dbh, tableId, elementKey, elementName, columnType,
        listChildElementKeys, isUnitOfRetention, displayVisible, jsonStringifyDisplayName,
        DEFAULT_KEY_DISPLAY_CHOICES_MAP, DEFAULT_KEY_DISPLAY_FORMAT, DEFAULT_KEY_SMS_IN,
        DEFAULT_KEY_SMS_OUT, DEFAULT_KEY_SMS_LABEL, DEFAULT_KEY_FOOTER_MODE, null, typeOfStore);

    return cp;
  }

  /**
   * Deletes the column represented by this ColumnProperties by deleting it from
   * the ColumnDefinitions table as well as the given key value store.
   * <p>
   * Also clears all the column color rules for the column. TODO: should maybe
   * delete the column from ALL the column key value stores to avoid conflict
   * with ColumnDefinitions?
   *
   * @param db
   */
  void deleteColumn(SQLiteDatabase db) {
    ColumnDefinitions.deleteColumnDefinition(tableId, elementKey, db);
    KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
    KeyValueStore kvs = kvsm.getStoreForTable(tableId, backingStore);
    kvs.clearEntries(ColumnProperties.KVS_PARTITION, elementKey, db);
    // this is to clear all the color rules. If we didn't do this, you could
    // have old color rules build up, and worse still, if you deleted this
    // column and then added a new column whose element key ended up being the
    // same, you would have rules suddenly applying to them.
    kvs.clearEntries(ColorRuleGroup.KVS_PARTITION_COLUMN, elementKey, db);
  }

  private static OdkTablesKeyValueStoreEntry createStringEntry(String tableId, String partition,
      String elementKey, String key, String value) {
    OdkTablesKeyValueStoreEntry entry = new OdkTablesKeyValueStoreEntry();
    entry.tableId = tableId;
    entry.partition = partition;
    entry.aspect = elementKey;
    entry.type = ColumnType.STRING.name();
    entry.value = value;
    entry.key = key;
    return entry;
  }

  private static OdkTablesKeyValueStoreEntry createIntEntry(String tableId, String partition,
      String elementKey, String key, int value) {
    OdkTablesKeyValueStoreEntry entry = new OdkTablesKeyValueStoreEntry();
    entry.tableId = tableId;
    entry.partition = partition;
    entry.aspect = elementKey;
    entry.type = ColumnType.INTEGER.name();
    entry.value = Integer.toString(value);
    entry.key = key;
    return entry;
  }

  private static OdkTablesKeyValueStoreEntry createBooleanEntry(String tableId, String partition,
      String elementKey, String key, boolean value) {
    OdkTablesKeyValueStoreEntry entry = new OdkTablesKeyValueStoreEntry();
    entry.tableId = tableId;
    entry.partition = partition;
    entry.aspect = elementKey;
    entry.type = ColumnType.BOOLEAN.name();
    entry.value = Boolean.toString(value);
    entry.key = key;
    return entry;
  }

  /**
   * DB_ELEMENT_KEY, DB_ELEMENT_NAME, DB_ELEMENT_TYPE,
   * DB_LIST_CHILD_ELEMENT_KEYS, DB_JOIN_TABLE_ID, DB_JOIN_ELEMENT_KEY,
   * DB_IS_UNIT_OF_RETENTION,
   *
   * DB_DISPLAY_VISIBLE, DB_DISPLAY_NAME, DB_DISPLAY_CHOICES_MAP,
   * DB_DISPLAY_FORMAT,
   *
   * DB_SMS_IN, DB_SMS_OUT, DB_SMS_LABEL,
   *
   * DB_FOOTER_MODE
   */

  public String getElementKey() {
    return elementKey;
  }

  public String getElementName() {
    return elementName;
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
  public void setColumnType(TableProperties tp, ColumnType columnType) {
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
      db.beginTransaction();
      setStringProperty(db, ColumnDefinitionsColumns.ELEMENT_TYPE, columnType.name());
      // TODO: we should run validation rules on the input, converting it to a
      // form that SQLite will properly convert into the new datatype.
      tp.reformTable(db);
      db.setTransactionSuccessful();
      db.endTransaction();
      this.elementType = columnType;
    } finally {
      tp.refreshColumns();
    }
  }

  public List<String> getListChildElementKeys() {
    return listChildElementKeys;
  }

  public void setListChildElementKeys(ArrayList<String> listChildElementKeys) {
    try {
      String strListChildElementKeys = null;
      if (listChildElementKeys != null && listChildElementKeys.size() > 0) {
        strListChildElementKeys = mapper.writeValueAsString(listChildElementKeys);
      }
      setStringProperty(ColumnDefinitionsColumns.LIST_CHILD_ELEMENT_KEYS, strListChildElementKeys);
      this.listChildElementKeys = listChildElementKeys;
    } catch (JsonGenerationException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("setListChildElementKeys failed: "
          + listChildElementKeys.toString(), e);
    } catch (JsonMappingException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("setListChildElementKeys failed: "
          + listChildElementKeys.toString(), e);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("setListChildElementKeys failed: "
          + listChildElementKeys.toString(), e);
    }
  }

  public boolean isUnitOfRetention() {
    return isUnitOfRetention;
  }

  public void setIsUnitOfRetention(boolean setting) {
    setBooleanProperty(ColumnDefinitionsColumns.IS_UNIT_OF_RETENTION, setting);
    this.isUnitOfRetention = setting;
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
  public void setDisplayVisible(boolean setting) {
    setBooleanProperty(KEY_DISPLAY_VISIBLE, setting);
    this.displayVisible = setting;
  }

  /**
   * @return the column's display name
   */
  public String getDisplayName() {
    return displayName;
  }

  private void updateDisplayNameFromJsonStringifyDisplayName() {
    try {
      this.displayName = null;
      if (jsonStringifyDisplayName != null && jsonStringifyDisplayName.length() > 0) {
        Object displayObject = mapper.readValue(jsonStringifyDisplayName, Object.class);
        if ( displayObject instanceof String ) {
          this.displayName = (String)  displayObject;
        } else if ( displayObject instanceof Map ) {
          // TODO: get current locale; deal with non-default locales
          @SuppressWarnings("rawtypes")
          Object v = ((Map) displayObject).get("default");
          if ( v != null && v instanceof String ) {
            this.displayName = (String) v;
          }
        }
      }
      if ( this.displayName == null || this.displayName.length() == 0 ) {
        throw new IllegalArgumentException("displayName is not valid: " + jsonStringifyDisplayName);
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
  public void setDisplayName(String displayName) {
    try {
      // error if displayName is not set...
      if ( displayName == null || displayName.length() == 0 ) {
        throw new IllegalArgumentException("displayName is not valid: " + displayName);
      }
      // parse the existing jsonStringifyDisplayName value...
      if (jsonStringifyDisplayName != null && jsonStringifyDisplayName.length() > 0) {
        Object displayObject = mapper.readValue(jsonStringifyDisplayName, Object.class);
        if ( displayObject instanceof String ) {
          // just overwrite it...
          String newJsonStringifyDisplayName = mapper.writeValueAsString(displayName);
          setStringProperty(KEY_DISPLAY_NAME, newJsonStringifyDisplayName);
          this.jsonStringifyDisplayName = newJsonStringifyDisplayName;
          this.displayName = displayName;
        } else if ( displayObject instanceof Map ) {
          // TODO: get current locale; deal with non-default locales
          ((Map) displayObject).put("default", displayName);
          String newJsonStringifyDisplayName = mapper.writeValueAsString(displayObject);
          setStringProperty(KEY_DISPLAY_NAME, newJsonStringifyDisplayName);
          this.jsonStringifyDisplayName = newJsonStringifyDisplayName;
          this.displayName = displayName;
        }
      } else {
        String newJsonStringifyDisplayName = mapper.writeValueAsString(displayName);
        setStringProperty(KEY_DISPLAY_NAME, newJsonStringifyDisplayName);
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
  public void setJoins(ArrayList<JoinColumn> joins) {
    try {
      String joinsStr = JoinColumn.toSerialization(joins);
      setStringProperty(KEY_JOINS, joinsStr);
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
    setBooleanProperty(KEY_SMS_IN, setting);
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
    setBooleanProperty(KEY_SMS_OUT, setting);
    this.smsOut = setting;
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
  public void setDisplayChoicesList(ArrayList<String> options) {
    try {
      String encoding = null;
      if (options != null && options.size() > 0) {
        encoding = mapper.writeValueAsString(options);
      }
      setStringProperty(KEY_DISPLAY_CHOICES_LIST, encoding);
      displayChoicesList = options;
    } catch (JsonGenerationException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("setDisplayChoicesList failed: " + options.toString(), e);
    } catch (JsonMappingException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("setDisplayChoicesList failed: " + options.toString(), e);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("setDisplayChoicesList failed: " + options.toString(), e);
    }
  }

  // Map<String, Object> toJson() {
  String toJson() throws JsonGenerationException, JsonMappingException, IOException {
    Map<String, Object> jo = new HashMap<String, Object>();
    jo.put(JSON_KEY_VERSION, 1);
    jo.put(JSON_KEY_TABLE_ID, tableId);
    jo.put(JSON_KEY_ELEMENT_KEY, elementKey);
    jo.put(JSON_KEY_ELEMENT_NAME, elementName);
    jo.put(JSON_KEY_JOINS, JoinColumn.toSerialization(joins));
    jo.put(JSON_KEY_ELEMENT_TYPE, elementType.name());
    jo.put(JSON_KEY_FOOTER_MODE, footerMode.name());
    jo.put(JSON_KEY_LIST_CHILD_ELEMENT_KEYS, listChildElementKeys);
    jo.put(JSON_KEY_IS_UNIT_OF_RETENTION, isUnitOfRetention);
    jo.put(JSON_KEY_DISPLAY_VISIBLE, displayVisible);
    jo.put(JSON_KEY_DISPLAY_NAME, jsonStringifyDisplayName);
    jo.put(JSON_KEY_DISPLAY_CHOICES_LIST, displayChoicesList);
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
      throw new IllegalArgumentException("toJson failed - tableId: " + tableId + " elementKey: "
          + elementKey, e);
    } catch (JsonMappingException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("toJson failed - tableId: " + tableId + " elementKey: "
          + elementKey, e);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("toJson failed - tableId: " + tableId + " elementKey: "
          + elementKey, e);
    }
    return toReturn;
  }

  /**
   * Construct a ColumnProperties object from JSON. NOTE: Nothing is written
   * to the database. The caller is responsible for writing the changes.
   *
   * @param dbh
   * @param json
   * @param typeOfStore
   * @return
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   */
  public static ColumnProperties constructColumnPropertiesFromJson(DbHelper dbh, String json,
      KeyValueStore.Type typeOfStore) throws JsonParseException, JsonMappingException, IOException {
    Map<String, Object> jo;
    try {
      jo = mapper.readValue(json, Map.class);
    } catch (JsonParseException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("constructColumnPropertiesFromJson failed: " + json, e);
    } catch (JsonMappingException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("constructColumnPropertiesFromJson failed: " + json, e);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("constructColumnPropertiesFromJson failed: " + json, e);
    }

    String joElType = (String) jo.get(JSON_KEY_ELEMENT_TYPE);
    ColumnType elementType = (joElType == null) ? ColumnType.NONE : ColumnType.valueOf(joElType);

    String joFootMode = (String) jo.get(JSON_KEY_FOOTER_MODE);
    FooterMode footerMode = (joFootMode == null) ? FooterMode.none : FooterMode.valueOf(joFootMode);

    ArrayList<JoinColumn> joins = JoinColumn.fromSerialization((String) jo.get(JSON_KEY_JOINS));
    Object joListChildren = jo.get(JSON_KEY_LIST_CHILD_ELEMENT_KEYS);
    ArrayList<String> listChildren = (joListChildren == null) ? new ArrayList<String>()
        : (ArrayList<String>) joListChildren;
    Object joListChoices = jo.get(JSON_KEY_DISPLAY_CHOICES_LIST);
    ArrayList<String> listChoices = (joListChoices == null) ? new ArrayList<String>()
        : (ArrayList<String>) joListChoices;

    ColumnProperties cp = new ColumnProperties(dbh, (String) jo.get(JSON_KEY_TABLE_ID),
        (String) jo.get(JSON_KEY_ELEMENT_KEY), (String) jo.get(JSON_KEY_ELEMENT_NAME), elementType,
        listChildren, (Boolean) jo.get(JSON_KEY_IS_UNIT_OF_RETENTION),
        (Boolean) jo.get(JSON_KEY_DISPLAY_VISIBLE), (String) jo.get(JSON_KEY_DISPLAY_NAME) /** JSON.stringify()'d */,
        listChoices, (String) jo.get(JSON_KEY_DISPLAY_FORMAT), (Boolean) jo.get(JSON_KEY_SMS_IN),
        (Boolean) jo.get(JSON_KEY_SMS_OUT), (String) jo.get(JSON_KEY_SMS_LABEL), footerMode, joins,
        typeOfStore);

    return cp;
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
      KeyValueStore kvs = kvsm.getStoreForTable(tableId, backingStore);
      kvs.insertOrUpdateKey(db, ColumnProperties.KVS_PARTITION, elementKey, property,
          ColumnType.INTEGER.name(), Integer.toString(value));
    }
    Log.d(TAG, "updated int property " + property + " to " + value + " for table " + tableId
        + ", column " + elementKey);
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

  private void setStringProperty(SQLiteDatabase db, String property, String value) {
    // is it a column definition property?
    if (ColumnDefinitions.columnNames.contains(property)) {
      ColumnDefinitions.setValue(tableId, elementKey, property, value, db);
    } else {
      // or a kvs property?
      KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
      KeyValueStore kvs = kvsm.getStoreForTable(tableId, backingStore);
      kvs.insertOrUpdateKey(db, ColumnProperties.KVS_PARTITION, elementKey, property,
          ColumnType.STRING.name(), value);
    }
    Log.d(TAG, "updated string property " + property + " to " + value + " for table " + tableId
        + ", column " + elementKey);
  }

  private void setBooleanProperty(String property, boolean value) {
    SQLiteDatabase db = dbh.getWritableDatabase();
    try {
      setBooleanProperty(db, property, value);
    } finally {
      // TODO: fix the when to close problem
      // db.close();
    }
  }

  private void setBooleanProperty(SQLiteDatabase db, String property, boolean value) {
    if (ColumnDefinitions.columnNames.contains(property)) {
      ColumnDefinitions.setValue(tableId, elementKey, property, value, db);
    } else {
      // or a kvs property?
      KeyValueStoreManager kvsm = KeyValueStoreManager.getKVSManager(dbh);
      KeyValueStore kvs = kvsm.getStoreForTable(tableId, backingStore);
      kvs.insertOrUpdateKey(db, ColumnProperties.KVS_PARTITION, elementKey, property,
          ColumnType.BOOLEAN.name(), Boolean.toString(value));
    }
    Log.d(TAG, "updated int property " + property + " to " + value + " for table " + tableId
        + ", column " + elementKey);
  }

}
