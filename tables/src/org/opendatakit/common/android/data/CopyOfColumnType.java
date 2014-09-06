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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Act like an enum, in that == comparisons work for comparing two typenames.
 * But allow the enum to grow, so ColumnType.valueOf() will extend the list of
 * ColumnTypes.
 *
 * It is OK to add values to this enumeration. The name() of the enumeration is
 * stored in the database, so the order of the names should not be important
 * here.
 */
public class CopyOfColumnType {
	private static Map<String, CopyOfColumnType> nameMap = new HashMap<String, CopyOfColumnType>();

	/*
	 * NB: All of these files must have an entry in the map in
	 * AggregateSynchronizer or else sync won't work correctly.
	 */

	public static CopyOfColumnType NONE;
	public static CopyOfColumnType STRING;
	public static CopyOfColumnType INTEGER;
	public static CopyOfColumnType NUMBER;
	public static CopyOfColumnType DATE;
	public static CopyOfColumnType DATETIME;
	public static CopyOfColumnType TIME;

	// TODO: need way to internationalize label
	// TODO: should pull these from a database table of app properties (e.g., geopoint)

	// TODO: confirm this propagates into Aggregate OK?
	public static CopyOfColumnType BOOLEAN; // not in Tables,

	// TODO: need to track image/audio/video mime type,
	// TODO: need file entry in Aggregate (JSON in Tables)
	public static CopyOfColumnType MIMEURI; // not in Collect
   public static CopyOfColumnType IMAGEURI; // in Collect
   public static CopyOfColumnType AUDIOURI; // in Collect
   public static CopyOfColumnType VIDEOURI; // in Collect

	// TODO: replace MC_OPTIONS usage with this and child element
	public static CopyOfColumnType MULTIPLE_CHOICES; // NEW

	// TODO: goes away; becomes plain old composite type?
	// TODO: was 'Location' -- was that lat-long, or any cartesian coordinate?
	public static CopyOfColumnType GEOPOINT;

	 // TODO: not in collect; becomes composite element
	public static CopyOfColumnType DATE_RANGE; // not in Collect, Aggregate

	 // TODO: not in Collect; becomes text specialization element
	public static CopyOfColumnType PHONE_NUMBER; // not in Collect, Aggregate

	// TODO: This is a property of the TABLE not any one COLUMN. Move to TableProperties!!!
	 // TODO: not in Collect; becomes MIMEURI specialization element
	public static CopyOfColumnType COLLECT_FORM; // not in Collect, Aggregate

	 // TODO: goes away -- replaced by MULTIPLE_CHOICES and child type description
	public static CopyOfColumnType MC_OPTIONS; // select1/select

	// TODO: goes away -- Used in col properties preferences/settings display to enable the showing
	// of the joinTableId, joinElementKey properties.  Those should be independently configurable...
	// TODO: this goes away -- it confounds the data type with the ability to use that data type
	// to link across to another table. The linking ability is NOT a data type, but a column property
	// that can modify the way the data is presented (e.g., with click actions to link to that other table.
	public static CopyOfColumnType TABLE_JOIN;

	static {
		nameMap.put("none", NONE = new CopyOfColumnType("none", "None", "string"));
		nameMap.put("string", STRING = new CopyOfColumnType("string", "Text", "string"));
		nameMap.put("integer", INTEGER = new CopyOfColumnType("integer", "Integer", "int"));
		nameMap.put("number", NUMBER = new CopyOfColumnType("number", "Number", "decimal"));
		nameMap.put("date", DATE = new CopyOfColumnType("date", "Date", "date"));
		nameMap.put("dateTime", DATETIME
				= new CopyOfColumnType("dateTime", "Date and Time", "dateTime"));
		nameMap.put("time", TIME = new CopyOfColumnType("time", "Time", "time"));

		nameMap.put("boolean", BOOLEAN = new CopyOfColumnType("boolean", "Boolean", "string"));
		nameMap.put("mimeUri", MIMEURI = new CopyOfColumnType("mimeUri", "File", "binary", "file"));
      nameMap.put("imageUri", IMAGEURI = new CopyOfColumnType("imageUri", "Image", "binary", "image"));
      nameMap.put("audioUri", AUDIOURI = new CopyOfColumnType("audioUri", "Audio", "binary", "audio"));
      nameMap.put("videoUri", VIDEOURI = new CopyOfColumnType("videoUri", "Video", "binary", "video"));
		nameMap.put("multipleChoices", MULTIPLE_CHOICES
				= new CopyOfColumnType("multipleChoices", "Multiple Choices (list)", "string"));

		nameMap.put("geopoint", GEOPOINT = new CopyOfColumnType("geopoint", "Location", "geopoint"));

		nameMap.put("dateRange", DATE_RANGE = new CopyOfColumnType("dateRange",
				"Date Range", "string"));
		nameMap.put("phoneNumber", PHONE_NUMBER = new CopyOfColumnType("phoneNumber",
				"Phone Number", "string"));
		// TODO: move to TableProperties
		nameMap.put("collectForm", COLLECT_FORM = new CopyOfColumnType("collectForm",
				"Collect Form", "string"));
		// TODO: GO AWAY!!! this is replaced by MULTIPLE_CHOICES and element item type
		nameMap.put("mcOptions", MC_OPTIONS = new CopyOfColumnType("mcOptions",
				"Multiple Choices", "string"));
		// TODO: GO AWAY!!! this info is captured in Column Properties...
		nameMap.put("tableJoin", TABLE_JOIN = new CopyOfColumnType("tableJoin",
				"Join", "string"));
	}

	private final String typename;
	private final String label;
	private final String collectType;
	private final String baseContentType;

	/*
	 * I think I need this for serialization...
	 */
	private CopyOfColumnType() {
	  // just for serialization
	  typename = "";
	  label = "";
	  collectType = "string";
	  baseContentType = null;
	}

   private CopyOfColumnType(String typename, String label, String collectType, String mediaType) {
      this.typename = typename;
      this.label = label;
      this.collectType = collectType;
      this.baseContentType = mediaType;
   }

	private CopyOfColumnType(String typename, String label, String collectType) {
		this(typename, label, collectType, null);
	}

	public final String name() {
		return typename;
	}

	public final String label() {
		return label;
	}

	public final String collectType() {
	  return collectType;
	}

	public final String baseContentType() {
	  return baseContentType;
	}

	public final String toString() {
		return typename;
	}

	public static final CopyOfColumnType valueOf(String name) {
		CopyOfColumnType t = nameMap.get(name);
		if (t != null)
			return t;
		t = new CopyOfColumnType(name, name, "string");
		nameMap.put(name, t);
		return t;
	}

	public static final ArrayList<CopyOfColumnType> getAllColumnTypes() {
		ArrayList<CopyOfColumnType> sortedList = new ArrayList<CopyOfColumnType>(
				nameMap.values());
		Collections.sort(sortedList, new Comparator<CopyOfColumnType>() {

			@Override
			public int compare(CopyOfColumnType lhs, CopyOfColumnType rhs) {
				return lhs.label().compareTo(rhs.label());
			}
		});

		return sortedList;
	}

	public static final String[] getAllColumnTypeLabels() {
		String[] vlist = new String[nameMap.size()];
		int i = 0;
		for (CopyOfColumnType t : getAllColumnTypes()) {
			vlist[i++] = t.label();
		}
		return vlist;
	}
}