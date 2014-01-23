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

import org.opendatakit.tables.views.webkits.CustomView.TableData;

/**
 * This class is handed to the javascript as "data" when displaying a table in
 * a List View. It is a way to get at the data in a table, allowing for the 
 * display of a table to be customized.
 * <p>
 * Standard practice is to choose several columns which are enough to provide
 * a summary of each row, and then iterate over all the rows of the table using
 * {@link #getCount()} and {@link #getData(int, String)}, rendering each row as
 * an item. A click handler can then be added to call 
 * {@link ControlIf#openDetailView(String, String, String)} to launch a Detail View for a clicked row.
 * <p>
 * This class then serves as a summary and an access point to the Detail View.
 * @author Mitch Sundt
 * @author sudar.sam@gmail.com
 *
 */
public class TableDataIf {
	private WeakReference<TableData> weakTable;

	TableDataIf(TableData table) {
		this.weakTable = new WeakReference<TableData>(table);
	}

	/**
	 * Returns the number of rows in the table being viewed as restricted by
	 * the current query, be it SQL or a query string.
	 * @return the number of rows in the table
	 */
	// @JavascriptInterface
	public int getCount() {
		return weakTable.get().getCount();
	}

	/**
	 * Returns a stringified JSONArray of all the values in the given column, 
	 * or null if the column cannot be found.
	 * @param elementPath the element path of the column
	 * @return JSONArray of all the data in the column
	 */
	// @JavascriptInterface
	public String getColumnData(String elementPath) {
		return weakTable.get().getColumnData(elementPath);
	}

	/**
	 * Return a stringified JSON object mapping elementKey its column type.
	 * @return Stringified JSON map of element key to column type
	 */
	// @JavascriptInterface
	public String getColumns() {
		return weakTable.get().getColumns();
	}

	/**
	 * Get the text color of the cell in the given column for the given value.
	 * Uses the color rules of the column. The default value is -16777216.
	 * @param elementPath the element path of the column
	 * @param value the string value of the datum
	 * @return String representation of the text color
	 */
	// @JavascriptInterface
	public String getForegroundColor(String elementPath, String value) {
		return weakTable.get().getForegroundColor(elementPath, value);
	}

	/**
	 * Return the number of rows in the collection at the given row index. Only
	 * meaningful if {@link #inCollectionMode()} returns true.
	 * @param rowNum
	 * @return number of rows in the collection
	 */
	// @JavascriptInterface
	public int getCollectionSize(int rowNum) {
		return weakTable.get().getCollectionSize(rowNum);
	}

	/**
	 * Returns true if the table is indexed.
	 * @return true if index else false
	 */
	// @JavascriptInterface
	public boolean isGroupedBy() {
		return weakTable.get().isGroupedBy();
	}

	/**
	 * Retrieve the datum at the given row in the given column name. The rows
	 * are zero-indexed, meaning the first row is 0. For  example, if you were 
	 * displaying a list view and had a column titled "Age", you would retrieve 
	 * the "Age" value for the second row by calling getData(1, "Age").
	 * <p>
	 * The null value is returned if the column could not be found, or if the
	 * value in the database is null.
	 * @param rowNum the row number
	 * @param elementPath the element path of the column
	 * @return the String representation of the datum at the given row in the
	 * given column, or null if the value in the database is null or the column
	 * does not exist
	 */
	// @JavascriptInterface
	public String getData(int rowNum, String elementPath) {
		return weakTable.get().getData(rowNum, elementPath);
	}
	
	/**
	 * Retrieve the datum in the given column from the first row. This is a
	 * convenience method when operating in a detail view and is equivalent to
	 * calling {@link #getData(int, String)} with a rowNum of 0.
	 * @param elementPath
	 * @return the String representation of the datum in the given column at the
	 * first row of the table
	 */
	public String get(String elementPath) {
	  return this.getData(0, elementPath);
	}
	
}