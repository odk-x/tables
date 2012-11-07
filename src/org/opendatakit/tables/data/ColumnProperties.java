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
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * A class for accessing and managing column properties.
 * 
 * @author hkworden@gmail.com (Hilary Worden)
 */
public class ColumnProperties {

	private static final ObjectMapper mapper = new ObjectMapper();
    private static final String t = "ColumnProperties";
    
    // the name of the column properties table in the database
    private static final String DB_TABLENAME = "colProps";
    // names of columns in the column properties table
    private static final String DB_TABLE_ID = "tableId";
    // display attributes
    
    private static final String DB_ELEMENT_KEY = "elementKey";// (was DB_DB_COLUMN_NAME)
    /* (was dbColumnName) unique id for this element. 
		There should be only one such elementKey for a given tableId. This is the
		dbColumnName if it is a value persisted into the database */
    private static final String  DB_ELEMENT_NAME = "elementName";
    /* name for this element. Either the elementKey or the
		name of the element within its enclosing composite type (element name within a struct).
		This is therefore not unique within a table row, as there could be multiple entries
		with 'latitude' as  their element names. */
    private static final String DB_ELEMENT_TYPE = "elementType"; // (was DB_COL_TYPE)
    /* This is a string value. see larger comment below */
    private static final String  DB_LIST_CHILD_ELEMENT_KEYS = "listChildElementKeys";
    /* if this is a composite type (geopoint), this is a JSON list of the 
	 * element keys of the direct descendants of this field name.
	 */
    private static final String  DB_IS_PERSISTED = "isPersisted"; /* default: 1 (true) -- 
		whether or not this is persisted to the database. If true, elementId is the dbColumnName 
		in which this value is written. The value will be written as JSON if it is a composite 
		type. see larger comment below for example. */
    private static final String DB_JOIN_TABLE_ID = "joinTableId"; /* tableId of table to join against */
    private static final String DB_JOIN_ELEMENT_KEY = "joinElementKey"; // (was DB_JOIN_COLUMN_NAME)
    /* elementKey of the value to join (this table's element) against in other table */
	/* 
		Data types and how things are stored:
		
		We have these primitive elementTypes:
		  STRING
		  INTEGER
		  DECIMAL
		  DATE
		  DATETIME
		  TIME
		  BOOLEAN
		  MIMEURI
		and this composite type:
		  MULTIPLE_CHOICE
		for multiple choice options (arrays). 
		These could hold any data type, but initial implementation is only for STRING
		
		Anything else is user-specified strings that are used to identify struct-like datatype definitions.
		Initially, this would be 'geopoint'; Tables would add 'phonenumber', 'date range'
		
		e.g., multiple-choice list of symptoms:
		
		The data is stored under the 'patientSymptoms' column in the database as a JSON
		encoding of string values. i.e., '["ache","fever"]'
	
		ekey: patientSymptoms
		ename: patientSymptoms
		etype: MULTIPLE_CHOICE
		listChildEKeys: '[ patientSymptomsItem ]'
		isPersist: true
		
		ekey: patientSymptomsItem
		ename: null  // elements are not named within this list
		etype: STRING
		listChildEKeys: null
		isPersist: false
		
		-------------
		e.g., geopoint defining a northernmost point of something:
		
		The data is stored as 4 columns, 'northLatitude', 'northLongitude', 'northAltitude', 'northAccuracy'
		
		ekey: northernmostPoint
		ename: northernmostPoint
		etype: geopoint
		listChildEKeys: '[ "northLatitude", "northLongitude", "northAltitude", "northAccuracy"]'
		isPersist: false
		
		ekey: northLatitude
		ename: latitude
		etype: DECIMAL
		listChildEKeys: null
		isPersist: true
		
		ekey: northLongitude
		ename: longitude
		etype: DECIMAL
		listChildEKeys: null
		isPersist: true
		
		ekey: northAltitude
		ename: altitude
		etype: DECIMAL
		listChildEKeys: null
		isPersist: true
		
		ekey: northAccuracy
		ename: accuracy
		etype: DECIMAL
		listChildEKeys: null
		isPersist: true
		
		ODK Collect can do calculations and constraint tests like 'northermostPoint.altitude < 4.0'
	
		e.g., 'clientPhone' as a phonenumber type, which is just a restriction on a STRING value
		persists under 'clientPhone' column in database.
		
		ekey: clientPhone
		ename: clientPhone
		etype: phoneNumber
		listChildEKeys: [ "clientPhoneNumber" ] // single element
		isPersist: true
		
		ekey: clientPhoneNumber
		ename: null // null -- indicates restriction on etype
		etype: STRING
		listChildEKeys: null
		isPersist: false
		
		e.g., 'image' file capture in ODK Collect. Stored as a MIMEURI
	
		ekey: locationImage
		ename: locationImage
		etype: MIMEURI
		listChildEKeys: null
		isPersist: true
		
		MIMEURI stores a JSON object: 
		
			'{"path":"/mnt/sdcard/odk/tables/app/instances/2342.jpg","mimetype":"image/jpg"}'
	
		i.e., ODK Collect image/audio/video capture store everything as a MIMEURI with different mimetype values.
	*/
	
	/*							  
	
	NOTE: you can have a composite type stored in two ways: 
	   (1) store the leaf nodes of the composite type in the database. Describe the
	       entire type hierarchy down to those leaf nodes.
	   (2) store it as a json object at the top level. Describe the structure of this
	       json object and its leaf nodes (but none of these persist anything).
	
	   Each has its advantages -- 
	   (1) does independent value updates easily. 
	   (2) does atomic updates easily.
	*/
    private static final String DB_DISPLAY_VISIBLE = "displayVisible";// boolean (stored as Integer) 
    /* 1 as boolean (true) is this column visible in Tables
     * [may want tristate -1 = deleted, 0 = hidden, 1 = visible?] */
    private static final String DB_DISPLAY_NAME = "displayName";  /* perhaps as json i18n */
    private static final String DB_DISPLAY_CHOICES_MAP = "displayChoicesMap"; /* (was mcOptions) 
    // choices i18n structure (Java needs rework).
	// TODO: allocate large storage on Aggregate
	/* displayChoicesMap -- TODO: rework ( this is still an ArrayList<String> )
	 *
	 * This is a map used for select1 and select choices, either closed-universe (fixed set) or 
	 * open-universe (select1-or-other, select-or-other). Stores the full list of all values in the
	 * column. Example format (1st label shows localization, 2nd is simple single-language defn:
	 * 
	 * [ { "name": "1",
	 *   "label": { "fr" : "oui", "en" : "yes", "es" : "si" } },
	 *   { "name" : "0",
	 *    "label": "no" } ]
	 *    
	 * an open-universe list could just be a list of labels:
	 * 
	 * [ "yes", "oui", "si", "no" ]
	 * 
	 * i.e., there is no internationalization possible in open-universe lists, as we allow 
	 * free-form text entry. TODO: is this how we want this to work.
	 * 
	 * When a user chooses to enter their own data in the field, we add that entry to this list 
	 * for later display as an available choice (i.e., we update the choices list).
	 *
	 * TODO: how to define open vs. closed universe treatment?
	 * TODO: generalize for other data types? i.e., "name" as a date range?
	 */
    private static final String DB_DISPLAY_FORMAT = "displayFormat";  
    /* (FUTURE USE)
	format descriptor for this display column. e.g., 
	In the Javascript, we have 'handlebars helpers' for template generation. We could
	share a subset of this functionality in Tables for managing how to render a value.
	
	TODO: how does this interact with displayChoicesMap?
	
	The proposed eventual subset describes numeric formatting. It could also be used
	to render qrcode images, etc.  'this' and elementName
	both refer to this display value. E.g., sample usage syntax:
	           "{{toFixed this "2"}}",   // this.toFixed(2)
               "{{toExponential this "2"}}"  // this.toExponential(2)
               "{{toPrecision this "2"}}"  // this.toPrecision(2)
               "{{toString this "16"}}". // this.toString(16)
          otherwise, it does {{this}} substitutions for composite types. e.g., for geopoint:
               "({{toFixed this.latitude "2"}}, {{toFixed this.longitude "2"}) {{toFixed this.altitude "1"}}m error: {{toFixed this.accuracy "1"}}m"
          to produce '(48.50,32.20) 10.3m error: 6.0m'

	The only helper functions envisioned are "toFixed", "toExponential",
	"toPrecision", "toString" and "localize" and perhaps one for qrcode generation?
	
	TODO: how do you work with MULTIPLE_CHOICE e.g., for item separators (',' with final element ', and ')
	*/
    private static final String DB_SMS_IN = "smsIn"; /* (allow SMS incoming) default: 1 as boolean (true) */
    private static final String DB_SMS_OUT = "smsOut"; /* (allow SMS outgoing) default: 1 as boolean (true) */
	private static final String DB_SMS_LABEL = "smsLabel"; /* for SMS */

    private static final String DB_FOOTER_MODE = "footerMode"; /* 0=none, 1=count, 2=minimum, 3=maximum, 4=mean, 5=sum */

    // keys for JSON
    private static final String JSON_KEY_VERSION = "jVersion";
    private static final String JSON_KEY_TABLE_ID = "tableId";
    
    private static final String JSON_KEY_ELEMENT_KEY = "elementKey";// (was dbColumnName)
    private static final String JSON_KEY_ELEMENT_NAME = "elementName";
    private static final String JSON_KEY_ELEMENT_TYPE = "elementType"; // (was colType)
    private static final String JSON_KEY_LIST_CHILD_ELEMENT_KEYS = "listChildElementKeys";
    private static final String JSON_KEY_JOIN_TABLE_ID = "joinTableId";
    private static final String JSON_KEY_JOIN_ELEMENT_KEY = "joinElementKey";
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
    private static final String WHERE_SQL = DB_TABLE_ID + " = ? and " +
            DB_ELEMENT_KEY + " = ?";

    // the columns to be selected when initializing ColumnProperties
    private static final String[] INIT_COLUMNS = {
        DB_ELEMENT_KEY,
        DB_ELEMENT_NAME,
        DB_ELEMENT_TYPE,
        DB_LIST_CHILD_ELEMENT_KEYS,
        DB_JOIN_TABLE_ID,
        DB_JOIN_ELEMENT_KEY,
        DB_IS_PERSISTED,
        
        DB_DISPLAY_VISIBLE,
        DB_DISPLAY_NAME,
        DB_DISPLAY_CHOICES_MAP,
        DB_DISPLAY_FORMAT,
        
        DB_SMS_IN,
        DB_SMS_OUT,
        DB_SMS_LABEL,
        
        DB_FOOTER_MODE
    };

    public class FooterMode {
        public static final int NONE = 0;
        public static final int COUNT = 1;
        public static final int MINIMUM = 2;
        public static final int MAXIMUM = 3;
        public static final int MEAN = 4;
        public static final int SUM = 5;
        private FooterMode() {}
    }
    
    private final DbHelper dbh;
    private final String[] whereArgs;
    
    private final String tableId;
    
    private final String elementKey;
    private String elementName;
    private ColumnType elementType;
    private List<String> listChildElementKeys;
    private String joinTableId;
    private String joinElementKey;
    private boolean isPersisted;
    
    private boolean displayVisible;
    private String displayName;
    private ArrayList<String> displayChoicesMap;
    private String displayFormat;
    
    private boolean smsIn;
    private boolean smsOut;
    private String smsLabel;

    private int footerMode;
    
    private ColumnProperties(DbHelper dbh, String tableId, 
    		String elementKey,
    		String elementName,
    		ColumnType elementType,
    		List<String> listChildElementKeys,
    		String joinTableId,
    		String joinElementKey,
    		boolean isPersisted,
    		boolean displayVisible,
    		String displayName,
    		ArrayList<String> displayChoicesMap,
    		String displayFormat,
    		boolean smsIn,
    		boolean smsOut,
    		String smsLabel,
    		int footerMode) {
        this.dbh = dbh;
        whereArgs = new String[] {String.valueOf(tableId), elementKey};
        this.tableId = tableId;
        
        this.elementKey = elementKey;
        this.elementName = elementName;
        this.elementType = elementType;
        this.listChildElementKeys = listChildElementKeys;
        this.joinTableId = joinTableId;
        this.joinElementKey = joinElementKey;
        this.isPersisted = isPersisted;

        this.displayVisible = displayVisible;
        this.displayName = displayName;
        this.displayChoicesMap = displayChoicesMap;
        this.displayFormat = displayFormat;

        this.smsIn = smsIn;
        this.smsOut = smsOut;
        this.smsLabel = smsLabel;

        this.footerMode = footerMode;
    }
    
    public static ColumnProperties getColumnProperties(DbHelper dbh,
            String tableId, String dbElementKey) {
    	
    	SQLiteDatabase db = null;
    	Cursor c = null;
    	ColumnProperties cp = null;
        String parseValue = null;

        try {
	        db = dbh.getReadableDatabase();
	        c = db.query(DB_TABLENAME, INIT_COLUMNS, WHERE_SQL,
	                new String[] {tableId, dbElementKey}, null, null, null);
	        
	        int dbcnIndex = c.getColumnIndexOrThrow(DB_ELEMENT_KEY);
	        
	        int elementNameIndex = c.getColumnIndexOrThrow(DB_ELEMENT_NAME);
	        int elementTypeIndex = c.getColumnIndexOrThrow(DB_ELEMENT_TYPE);
	        int listChildElementKeysIndex = c.getColumnIndexOrThrow(DB_LIST_CHILD_ELEMENT_KEYS);
	        int joinTableIndex = c.getColumnIndexOrThrow(DB_JOIN_TABLE_ID);
	        int joinElementIndex = c.getColumnIndexOrThrow(DB_JOIN_ELEMENT_KEY);
	        int isPersistedIndex = c.getColumnIndexOrThrow(DB_IS_PERSISTED);
	
	        int displayVisibleIndex = c.getColumnIndexOrThrow(DB_DISPLAY_VISIBLE);
	        int displayNameIndex = c.getColumnIndexOrThrow(DB_DISPLAY_NAME);
	        int displayChoicesMapIndex = c.getColumnIndexOrThrow(DB_DISPLAY_CHOICES_MAP);
	        int displayFormatIndex = c.getColumnIndexOrThrow(DB_DISPLAY_FORMAT);
	
	        int smsInIndex = c.getColumnIndexOrThrow(DB_SMS_IN);
	        int smsOutIndex = c.getColumnIndexOrThrow(DB_SMS_OUT);
	        int smsLabelIndex = c.getColumnIndexOrThrow(DB_SMS_LABEL);
	
	        int footerModeIndex = c.getColumnIndexOrThrow(DB_FOOTER_MODE);
	        
	        c.moveToFirst();

	        @SuppressWarnings("unchecked")
	        ArrayList<String> displayChoicesMap = null;
	        if ( !c.isNull(displayChoicesMapIndex) ) {
	        	String displayChoicesMapValue = c.getString(displayChoicesMapIndex);
	        	parseValue = displayChoicesMapValue;
	        	displayChoicesMap = mapper.readValue(displayChoicesMapValue, ArrayList.class);
	        }
	        ArrayList<String> listChildElementKeys = null;
	        if ( !c.isNull(listChildElementKeysIndex) ) {
	        	String listChildElementKeysValue = c.getString(listChildElementKeysIndex);
	        	parseValue = listChildElementKeysValue;
	        	listChildElementKeys = mapper.readValue(listChildElementKeysValue, ArrayList.class);
	        }
	        cp = new ColumnProperties(dbh, tableId,
	        		c.getString(dbcnIndex), 
	        		c.getString(elementNameIndex),
	        		ColumnType.valueOf(c.getString(elementTypeIndex)),
	        		listChildElementKeys,
	        		c.getString(joinTableIndex),
	        		c.getString(joinElementIndex),
	        		c.getInt(isPersistedIndex) == 1,
	
	        		c.getInt(displayVisibleIndex) == 1,
	        		c.getString(displayNameIndex),
	        		displayChoicesMap,
	        		c.getString(displayFormatIndex),
	
	                c.getInt(smsInIndex) == 1,
	                c.getInt(smsOutIndex) == 1,
	        		c.getString(smsLabelIndex),
	
	                c.getInt(footerModeIndex));
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
    		try {
    			if ( c != null && !c.isClosed() ) {
    				c.close();
    			}
    		} finally {
    			if ( db != null ) {
    				db.close();
    			}
    		}
    	}
        return cp;
    }
    
    static ColumnProperties[] getColumnPropertiesForTable(DbHelper dbh,
            String tableId) {
    	SQLiteDatabase db = null;
    	Cursor c = null;
    	ColumnProperties[] cps = null;
    	
    	try {
	        db = dbh.getReadableDatabase();
	        c = db.query(DB_TABLENAME, INIT_COLUMNS, DB_TABLE_ID + " = ?",
	                new String[] {tableId}, null, null, null);
	        cps = new ColumnProperties[c.getCount()];
	
	        int dbcnIndex = c.getColumnIndexOrThrow(DB_ELEMENT_KEY);
	        int elementNameIndex = c.getColumnIndexOrThrow(DB_ELEMENT_NAME);
	        int elementTypeIndex = c.getColumnIndexOrThrow(DB_ELEMENT_TYPE);
	        int listChildElementKeysIndex = c.getColumnIndexOrThrow(DB_LIST_CHILD_ELEMENT_KEYS);
	        int joinTableIndex = c.getColumnIndexOrThrow(DB_JOIN_TABLE_ID);
	        int joinElementIndex = c.getColumnIndexOrThrow(DB_JOIN_ELEMENT_KEY);
	        int isPersistedIndex = c.getColumnIndexOrThrow(DB_IS_PERSISTED);
	
	        int displayVisibleIndex = c.getColumnIndexOrThrow(DB_DISPLAY_VISIBLE);
	        int displayNameIndex = c.getColumnIndexOrThrow(DB_DISPLAY_NAME);
	        int displayChoicesMapIndex = c.getColumnIndexOrThrow(DB_DISPLAY_CHOICES_MAP);
	        int displayFormatIndex = c.getColumnIndexOrThrow(DB_DISPLAY_FORMAT);
	
	        int smsInIndex = c.getColumnIndexOrThrow(DB_SMS_IN);
	        int smsOutIndex = c.getColumnIndexOrThrow(DB_SMS_OUT);
	        int smsLabelIndex = c.getColumnIndexOrThrow(DB_SMS_LABEL);
	
	        int footerModeIndex = c.getColumnIndexOrThrow(DB_FOOTER_MODE);
	        
	        int i = 0;
	        c.moveToFirst();
	        while (i < cps.length) {
		        @SuppressWarnings("unchecked")
		        ArrayList<String> displayChoicesMap = null;
		        if ( !c.isNull(displayChoicesMapIndex) ) {
		        	String displayChoicesMapValue = c.getString(displayChoicesMapIndex);
		        	try {
						displayChoicesMap = mapper.readValue(displayChoicesMapValue, ArrayList.class);
					} catch (JsonParseException e) {
						e.printStackTrace();
						Log.e(t, "ignored expection");
					} catch (JsonMappingException e) {
						e.printStackTrace();
						Log.e(t, "ignored expection");
					} catch (IOException e) {
						e.printStackTrace();
						Log.e(t, "ignored expection");
					}
		        }
		        ArrayList<String> listChildElementKeys = null;
		        if ( !c.isNull(listChildElementKeysIndex) ) {
		        	String listChildElementKeysValue = c.getString(listChildElementKeysIndex);
		        	try {
						listChildElementKeys = mapper.readValue(listChildElementKeysValue, ArrayList.class);
					} catch (JsonParseException e) {
						e.printStackTrace();
						Log.e(t, "ignored expection");
					} catch (JsonMappingException e) {
						e.printStackTrace();
						Log.e(t, "ignored expection");
					} catch (IOException e) {
						e.printStackTrace();
						Log.e(t, "ignored expection");
					}
		        }
		        cps[i] = new ColumnProperties(dbh, tableId,
		        		c.getString(dbcnIndex), 
		        		c.getString(elementNameIndex),
		        		ColumnType.valueOf(c.getString(elementTypeIndex)),
		        		listChildElementKeys,
		        		c.getString(joinTableIndex),
		        		c.getString(joinElementIndex),
		        		c.getInt(isPersistedIndex) == 1,
		
		        		c.getInt(displayVisibleIndex) == 1,
		        		c.getString(displayNameIndex),
		        		displayChoicesMap,
		        		c.getString(displayFormatIndex),
		
		                c.getInt(smsInIndex) == 1,
		                c.getInt(smsOutIndex) == 1,
		        		c.getString(smsLabelIndex),
		
		                c.getInt(footerModeIndex));
	            i++;
	            c.moveToNext();
	        }
    	} finally {
    		try {
    			if ( c != null && !c.isClosed() ) {
    				c.close();
    			}
    		} finally {
    			if ( db != null ) {
    				db.close();
    			}
    		}
    	}
        return cps;
    }
    
    static ColumnProperties addColumn(DbHelper dbh, SQLiteDatabase db,
            String tableId, String elementKey, String displayName) {
        ContentValues values = new ContentValues();
        
        values.put(DB_TABLE_ID, tableId);
        
        values.put(DB_ELEMENT_KEY, elementKey);
        values.put(DB_ELEMENT_NAME, elementKey);
        values.put(DB_ELEMENT_TYPE, ColumnType.NONE.name());
        values.putNull(DB_LIST_CHILD_ELEMENT_KEYS);
        values.putNull(DB_JOIN_TABLE_ID);
        values.putNull(DB_JOIN_ELEMENT_KEY);
        values.put(DB_IS_PERSISTED, 1);
        
        values.put(DB_DISPLAY_VISIBLE, 1);
        values.put(DB_DISPLAY_NAME, displayName);
        values.putNull(DB_DISPLAY_CHOICES_MAP);
        values.putNull(DB_DISPLAY_FORMAT);

        values.put(DB_SMS_IN, 1);
        values.put(DB_SMS_OUT, 1);
        values.putNull(DB_SMS_LABEL);

        values.put(DB_FOOTER_MODE, FooterMode.NONE);
        
        db.insert(DB_TABLENAME, null, values);
        return new ColumnProperties(dbh, tableId, 
        		elementKey,
        		elementKey,
        		ColumnType.NONE,
        		null,
        		null,
        		null,
        		true,
        		
        		true,
                displayName,
                null,
                null,
                
                true, 
                true, 
                null,
                
                FooterMode.NONE);
    }
    
    void deleteColumn(SQLiteDatabase db) {
        int count = db.delete(DB_TABLENAME, WHERE_SQL,
                new String[] {String.valueOf(tableId), elementKey});
        if (count != 1) {
            Log.e(ColumnProperties.class.getName(),
                    "deleteColumn() deleted " + count + " rows");
        }
    }
    
    /**
     *         DB_ELEMENT_KEY,
        DB_ELEMENT_NAME,
        DB_ELEMENT_TYPE,
        DB_LIST_CHILD_ELEMENT_KEYS,
        DB_JOIN_TABLE_ID,
        DB_JOIN_ELEMENT_KEY,
        DB_IS_PERSISTED,
        
        DB_DISPLAY_VISIBLE,
        DB_DISPLAY_NAME,
        DB_DISPLAY_CHOICES_MAP,
        DB_DISPLAY_FORMAT,
        
        DB_SMS_IN,
        DB_SMS_OUT,
        DB_SMS_LABEL,
        
        DB_FOOTER_MODE

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
	
	public void setElementName(String elementName) {
		setStringProperty(DB_ELEMENT_NAME, elementName);
		this.elementName = elementName;
	}

	// TODO: remove this
    public ColumnType getElementType() {
		return elementType;
	}

    // TODO: remove this
	public void setElementType(ColumnType elementType) {
		setStringProperty(DB_ELEMENT_TYPE, elementType.name());
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
     * @param columnType the new type
     */
    public void setColumnType(ColumnType columnType) {
        TableProperties tp = TableProperties.getTablePropertiesForTable(dbh,
                tableId, KeyValueStore.Type.ACTIVE);
        ArrayList<String> colOrder = tp.getColumnOrder();
        tp.getColumns(); // ensuring columns are initialized
        SQLiteDatabase db = dbh.getWritableDatabase();
        try {
	        db.beginTransaction();
	        setStringProperty(db, DB_ELEMENT_TYPE, columnType.name());
	        tp.reformTable(db, colOrder);
	        db.setTransactionSuccessful();
	        db.endTransaction();
        } finally {
        	if ( db != null ) {
        		db.close();
        	}
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
        setIntProperty(DB_IS_PERSISTED, setting ? 1 : 0);
        this.isPersisted = setting;
	}

    
    /**
     * @return whether or not this column is visible within Tables
     */
    public boolean getDisplayVisible() {
        return displayVisible;
    }
    
    /**
     * Sets whether or not this column is visible within Tables
     * @param setting the new display visibility setting
     */
    public void setDisplayVisible(boolean setting) {
        setIntProperty(DB_DISPLAY_VISIBLE, setting ? 1 : 0);
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
     * @param displayName the new display name
     */
    public void setDisplayName(String displayName) {
        setStringProperty(DB_DISPLAY_NAME, displayName);
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
     * @param abbreviation the new abbreviation (or null for no abbreviation)
     */
    public void setDisplayFormat(String format) {
        setStringProperty(DB_DISPLAY_FORMAT, format);
        this.displayFormat = format;
    }
    

     
    /**
     * @return the column's footer mode
     */
    public int getFooterMode() {
        return footerMode;
    }
    
    /**
     * Sets the column's footer mode.
     * @param footerMode the new footer mode
     */
    public void setFooterMode(int footerMode) {
        setIntProperty(DB_FOOTER_MODE, footerMode);
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
     * @param abbreviation the new abbreviation (or null for no abbreviation)
     */
    public void setSmsLabel(String abbreviation) {
        setStringProperty(DB_SMS_LABEL, abbreviation);
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
     * @param setting the new SMS-in setting
     */
    public void setSmsIn(boolean setting) {
        setIntProperty(DB_SMS_IN, setting ? 1 : 0);
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
     * @param setting the new SMS-out setting
     */
    public void setSmsOut(boolean setting) {
        setIntProperty(DB_SMS_OUT, setting ? 1 : 0);
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
     * @param options the array of options
     * @throws IOException 
     * @throws JsonMappingException 
     * @throws JsonGenerationException 
     */
    public void setDisplayChoicesMap(ArrayList<String> options) {
    	String encoding;
		try {
			encoding = mapper.writeValueAsString(options);
	        setStringProperty(DB_DISPLAY_CHOICES_MAP, encoding);
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
    public String getJoinTableId() {
        return joinTableId;
    }
    
    /**
     * Sets the join table ID.
     * @param tableId the join table Id
     */
    public void setJoinTableId(String tableId) {
        setStringProperty(DB_JOIN_TABLE_ID, tableId);
        joinTableId = tableId;
    }
	
	public String getJoinElementKey() {
		return joinElementKey;
	}

	public void setJoinElementKey(String joinElementKey) {
        setStringProperty(DB_JOIN_ELEMENT_KEY, tableId);
		this.joinElementKey = joinElementKey;
	}

	// TODO: rename to getJoinElementKey()
    /**
     * @return the join table column name
     */
    public String getJoinColumnName() {
        return joinElementKey;
    }
    
    /**
     * Sets the join column name.
     * @param columnName the join column name
     */
    public void setJoinColumnName(String columnName) {
        setStringProperty(DB_JOIN_ELEMENT_KEY, columnName);
        joinElementKey = columnName;
    }
    
    Map<String,Object> toJsonObject() {

    	Map<String,Object> jo = new HashMap<String,Object>();
    	jo.put(JSON_KEY_VERSION, 1);
    	jo.put(JSON_KEY_TABLE_ID, tableId);
        
    	jo.put(JSON_KEY_ELEMENT_KEY, elementKey);
        jo.put(JSON_KEY_ELEMENT_NAME, elementName);
        jo.put(JSON_KEY_ELEMENT_TYPE, elementType);
        jo.put(JSON_KEY_LIST_CHILD_ELEMENT_KEYS, listChildElementKeys);
        jo.put(JSON_KEY_JOIN_TABLE_ID, joinTableId);
        jo.put(JSON_KEY_JOIN_ELEMENT_KEY, joinElementKey);
        jo.put(JSON_KEY_IS_PERSISTED, isPersisted);

        jo.put(JSON_KEY_DISPLAY_VISIBLE, displayVisible);
        jo.put(JSON_KEY_DISPLAY_NAME, displayName);
    	jo.put(JSON_KEY_DISPLAY_CHOICES_MAP, displayChoicesMap);
        jo.put(JSON_KEY_DISPLAY_FORMAT, displayFormat);

        jo.put(JSON_KEY_SMS_IN, smsIn);
        jo.put(JSON_KEY_SMS_OUT, smsOut);
        jo.put(JSON_KEY_SMS_LABEL, smsLabel);

        jo.put(JSON_KEY_FOOTER_MODE, footerMode);

        return jo;
    }
    
    void setFromJsonObject(Map<String,Object> jo) {
    	
    	jo.put(JSON_KEY_VERSION, 1);
    	jo.put(JSON_KEY_TABLE_ID, tableId);
        
    	jo.put(JSON_KEY_ELEMENT_KEY, elementKey);
    	
    	setElementName((String) jo.get(JSON_KEY_ELEMENT_NAME));
    	setElementType(ColumnType.valueOf((String) jo.get(JSON_KEY_ELEMENT_TYPE)));
    	setListChildElementKeys((ArrayList<String>) jo.get(JSON_KEY_LIST_CHILD_ELEMENT_KEYS));
        setJoinTableId((String) jo.get(JSON_KEY_JOIN_TABLE_ID));
        setJoinColumnName((String) jo.get(JSON_KEY_JOIN_ELEMENT_KEY));
    	setIsPersisted((Boolean) jo.get(JSON_KEY_IS_PERSISTED));

        setDisplayVisible((Boolean) jo.get(JSON_KEY_DISPLAY_VISIBLE));
        setDisplayName((String) jo.get(JSON_KEY_DISPLAY_NAME));
        setDisplayChoicesMap((ArrayList<String>) jo.get(JSON_KEY_DISPLAY_CHOICES_MAP));
        setDisplayFormat((String) jo.get(JSON_KEY_DISPLAY_FORMAT));

        setSmsIn((Boolean) jo.get(JSON_KEY_SMS_IN));
        setSmsOut((Boolean) jo.get(JSON_KEY_SMS_OUT));
        setSmsLabel((String) jo.get(JSON_KEY_SMS_LABEL));

        jo.put(JSON_KEY_FOOTER_MODE, footerMode);
        setFooterMode((Integer) jo.get(JSON_KEY_FOOTER_MODE));
    }
    
    private void setIntProperty(String property, int value) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        try {
        	setIntProperty(db, property, value);
        } finally {
            db.close();
        }
    }
    
    private void setIntProperty(SQLiteDatabase db, String property,
            int value) {
        ContentValues values = new ContentValues();
        values.put(property, value);
        int count = db.update(DB_TABLENAME, values, WHERE_SQL, whereArgs);
        if (count != 1) {
            Log.e(ColumnProperties.class.getName(),
                    "setting " + property + " updated " + count + " rows");
        }
    }
    
    private void setStringProperty(String property, String value) {
        SQLiteDatabase db = dbh.getWritableDatabase();
        try {
        	setStringProperty(db, property,  value);
        } finally {
        	db.close();
        }
    }
    
    private void setStringProperty(SQLiteDatabase db, String property, String value) {
        ContentValues values = new ContentValues();
        values.put(property, value);
        int count = db.update(DB_TABLENAME, values, WHERE_SQL, whereArgs);
        if (count != 1) {
            Log.e(ColumnProperties.class.getName(),
                    "setting " + property + " updated " + count + " rows");
        }
    }
   
    static String getTableCreateSql() {
        return "CREATE TABLE " + DB_TABLENAME + "(" +
                       DB_TABLE_ID + " TEXT NOT NULL" +
        		
                ", " + DB_ELEMENT_KEY + " TEXT NOT NULL" +
                ", " + DB_ELEMENT_NAME + " TEXT NOT NULL" +
                ", " + DB_ELEMENT_TYPE + " TEXT NOT NULL" +
                ", " + DB_LIST_CHILD_ELEMENT_KEYS + " TEXT NULL" +
                ", " + DB_JOIN_TABLE_ID + " TEXT NULL" +
                ", " + DB_JOIN_ELEMENT_KEY + " TEXT NULL" +
                ", " + DB_IS_PERSISTED + " INTEGER NOT NULL" +
                       
                ", " + DB_DISPLAY_VISIBLE + " INTEGER NOT NULL" +
                ", " + DB_DISPLAY_NAME + " TEXT NOT NULL" +
                ", " + DB_DISPLAY_CHOICES_MAP + " TEXT NULL" +
                ", " + DB_DISPLAY_FORMAT + " TEXT NULL" +
                
                ", " + DB_SMS_IN + " INTEGER NOT NULL" +
                ", " + DB_SMS_OUT + " INTEGER NOT NULL" +
                ", " + DB_SMS_LABEL + " TEXT NULL" +

                ", " + DB_FOOTER_MODE + " TEXT NOT NULL" +
                ")";
    }
}
