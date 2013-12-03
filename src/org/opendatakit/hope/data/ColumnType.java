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
package org.opendatakit.hope.data;

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
public class ColumnType {
	private static Map<String, ColumnType> nameMap = new HashMap<String, ColumnType>();

	public static ColumnType NONE;
	public static ColumnType TEXT;
	public static ColumnType INTEGER;
	public static ColumnType NUMBER;
	public static ColumnType DATE;
	public static ColumnType DATETIME;
	public static ColumnType TIME;

	// TODO: need way to internationalize label
	// TODO: should pull these from a database table of app properties (e.g., geopoint)

	// TODO: confirm this propagates into Aggregate OK?
	public static ColumnType BOOLEAN; // not in Tables,

	// TODO: need to track image/audio/video mime type,
	// TODO: need file entry in Aggregate (JSON in Tables)
	public static ColumnType MIMEURI; // not in Collect
   public static ColumnType IMAGEURI; // in Collect
   public static ColumnType AUDIOURI; // in Collect
   public static ColumnType VIDEOURI; // in Collect

	// TODO: replace MC_OPTIONS usage with this and child element
	public static ColumnType MULTIPLE_CHOICES; // NEW

	// TODO: goes away; becomes plain old composite type?
	// TODO: was 'Location' -- was that lat-long, or any cartesian coordinate?
	public static ColumnType GEOPOINT;

	 // TODO: not in collect; becomes composite element
	public static ColumnType DATE_RANGE; // not in Collect, Aggregate

	 // TODO: not in Collect; becomes text specialization element
	public static ColumnType PHONE_NUMBER; // not in Collect, Aggregate

	// TODO: This is a property of the TABLE not any one COLUMN. Move to TableProperties!!!
	 // TODO: not in Collect; becomes MIMEURI specialization element
	public static ColumnType COLLECT_FORM; // not in Collect, Aggregate

	 // TODO: goes away -- replaced by MULTIPLE_CHOICES and child type description
	public static ColumnType MC_OPTIONS; // select1/select

	// TODO: goes away -- Used in col properties preferences/settings display to enable the showing
	// of the joinTableId, joinElementKey properties.  Those should be independently configurable...
	// TODO: this goes away -- it confounds the data type with the ability to use that data type
	// to link across to another table. The linking ability is NOT a data type, but a column property
	// that can modify the way the data is presented (e.g., with click actions to link to that other table.
	public static ColumnType TABLE_JOIN;

	static {
		nameMap.put("none", NONE = new ColumnType("none", "None", "string"));
		nameMap.put("text", TEXT = new ColumnType("text", "Text", "string"));
		nameMap.put("integer", INTEGER = new ColumnType("integer", "Integer", "int"));
		nameMap.put("number", NUMBER = new ColumnType("number", "Number", "decimal"));
		nameMap.put("date", DATE = new ColumnType("date", "Date", "date"));
		nameMap.put("datetime", DATETIME
				= new ColumnType("datetime", "Date and Time", "dateTime"));
		nameMap.put("time", TIME = new ColumnType("time", "Time", "time"));

		nameMap.put("boolean", BOOLEAN = new ColumnType("boolean", "Boolean", "string"));
		nameMap.put("mimeUri", MIMEURI = new ColumnType("mimeUri", "File", "binary", "file"));
      nameMap.put("imageUri", IMAGEURI = new ColumnType("imageUri", "Image", "binary", "image"));
      nameMap.put("audioUri", AUDIOURI = new ColumnType("audioUri", "Audio", "binary", "audio"));
      nameMap.put("videoUri", VIDEOURI = new ColumnType("videoUri", "Video", "binary", "video"));
		nameMap.put("multipleChoices", MULTIPLE_CHOICES
				= new ColumnType("multipleChoices", "Multiple Choices (list)", "string"));

		nameMap.put("geopoint", GEOPOINT = new ColumnType("geopoint", "Location", "geopoint"));

		nameMap.put("dateRange", DATE_RANGE = new ColumnType("dateRange",
				"Date Range", "string"));
		nameMap.put("phoneNumber", PHONE_NUMBER = new ColumnType("phoneNumber",
				"Phone Number", "string"));
		// TODO: move to TableProperties
		nameMap.put("collectForm", COLLECT_FORM = new ColumnType("collectForm",
				"Collect Form", "string"));
		// TODO: GO AWAY!!! this is replaced by MULTIPLE_CHOICES and element item type
		nameMap.put("mcOptions", MC_OPTIONS = new ColumnType("mcOptions",
				"Multiple Choices", "string"));
		// TODO: GO AWAY!!! this info is captured in Column Properties...
		nameMap.put("tableJoin", TABLE_JOIN = new ColumnType("tableJoin",
				"Join", "string"));
	}

	private final String typename;
	private final String label;
	private final String collectType;
	private final String baseContentType;

	/*
	 * I think I need this for serialization...
	 */
	private ColumnType() {
	  // just for serialization
	  typename = "";
	  label = "";
	  collectType = "string";
	  baseContentType = null;
	}

   private ColumnType(String typename, String label, String collectType, String mediaType) {
      this.typename = typename;
      this.label = label;
      this.collectType = collectType;
      this.baseContentType = mediaType;
   }

	private ColumnType(String typename, String label, String collectType) {
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

	public static final ColumnType valueOf(String name) {
		ColumnType t = nameMap.get(name);
		if (t != null)
			return t;
		t = new ColumnType(name, name, "string");
		nameMap.put(name, t);
		return t;
	}

	public static final ArrayList<ColumnType> getAllColumnTypes() {
		ArrayList<ColumnType> sortedList = new ArrayList<ColumnType>(
				nameMap.values());
		Collections.sort(sortedList, new Comparator<ColumnType>() {

			@Override
			public int compare(ColumnType lhs, ColumnType rhs) {
				return lhs.label().compareTo(rhs.label());
			}
		});

		return sortedList;
	}

	public static final String[] getAllColumnTypeLabels() {
		String[] vlist = new String[nameMap.size()];
		int i = 0;
		for (ColumnType t : getAllColumnTypes()) {
			vlist[i++] = t.label();
		}
		return vlist;
	}
}