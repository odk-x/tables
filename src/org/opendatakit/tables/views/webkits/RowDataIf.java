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
package org.opendatakit.tables.views.webkits;

import java.lang.ref.WeakReference;

import org.opendatakit.tables.views.webkits.CustomView.RowData;

/**
 * This class is handed to the javascript as "data" when displaying a Detail
 * View. It provides a way to get at the data in a row.
 * <p>
 * Standard practice is to define a Detail View html file which uses the "data"
 * javascript interface to get data using {@link #get(String)} and display it
 * to a user.
 * @author Mitch Sundt
 * @author sudar.sam@gmail.com
 *
 */
public class RowDataIf {
	private WeakReference<RowData> weakRowData;

	RowDataIf(RowData row) {
		this.weakRowData = new WeakReference<RowData>(row);
	}

	/**
	 * Takes the element path for the column and returns the value in that 
    * column.
	 * @param elementPath
	 * @return the data value. null if the column could not be found, or if the
	 * value in the database was null.
	 */
	// @JavascriptInterface
	public String get(String elementPath) {
		return weakRowData.get().get(elementPath);
	}
	
	/**
	 * Return the table id of the table containing this row.
	 * @return the table id
	 */
	public String getTableId() {
	  return weakRowData.get().getTableId();
	}

}