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
package org.opendatakit.hope.views.webkits;

import java.lang.ref.WeakReference;

import org.opendatakit.hope.views.webkits.CustomView.TableData;

/**
 * This class is handed to the javascript as "data" when displaying a table in
 * a List View. It is a way to get at the data in a table, allowing for the 
 * display of a table to be customized.
 * <p>
 * Standard practice is to choose several columns which are enough to provide
 * a summary of each row, and then iterate over all the rows of the table using
 * {@link #getCount()} and {@link #getData(int, String)}, rendering each row as
 * an item. A click handler can then be added to call 
 * {@link ControlIf#openItem(int)} to launch a Detail View for a clicked row.
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

	// @JavascriptInterface
	public boolean inCollectionMode() {
		return weakTable.get().inCollectionMode();
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
	 * Returns a stringified JSONArray of all the values in the given column.
	 * @param colName the display name of the column
	 * @return JSONArray of all the data in the column
	 */
	// @JavascriptInterface
	public String getColumnData(String colName) {
		return weakTable.get().getColumnData(colName);
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
	 * @param colName the display name of the column
	 * @param value the string value of the datum
	 * @return String representation of the text color
	 */
	// @JavascriptInterface
	public String getForegroundColor(String colName, String value) {
		return weakTable.get().getForegroundColor(colName, value);
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
	public boolean isIndexed() {
		return weakTable.get().isIndexed();
	}

	/**
	 * Retrieve the datum at the given row in the given column name. The rows
	 * are zero-indexed, meaning the first row is 0. For  example, if you were 
	 * displaying a list view and had a column titled "Age", you would retrieve 
	 * the "Age" value for the second row by calling getData(1, "Age").
	 * @param rowNum the row number
	 * @param colName the display name of the column to which you want the 
	 * data.
	 * @return the String representation of the datum at the given row in the
	 * given column
	 */
	// @JavascriptInterface
	public String getData(int rowNum, String colName) {
		return weakTable.get().getData(rowNum, colName);
	}

	/**
	 * Edit the row using Collect and the default form.
	 * @param rowNumber
	 */
	// @JavascriptInterface
	public void editRowWithCollect(int rowNumber) {
		weakTable.get().editRowWithCollect(rowNumber);
	}

	/**
	 * Edit the row using Collect and a specific form.
	 * @param rowNumber
	 * @param formId 
	 * @param formVersion can be null
	 * @param formRootElement
	 */
	// @JavascriptInterface
	public void editRowWithCollectAndSpecificForm(int rowNumber, String formId,
			String formVersion, String formRootElement) {
		weakTable.get().editRowWithCollectAndSpecificForm(rowNumber, formId,
				formVersion, formRootElement);
	}

	/**
	 * @see ControlIf#addRowWithCollect(String)
	 * @param tableName
	 */
	// @JavascriptInterface
	public void addRowWithCollect(String tableName) {
		weakTable.get().addRowWithCollect(tableName);
	}

	/**
	 * @see ControlIf#addRowWithCollectAndSpecificForm(String, String, String, String)
	 * @param tableName
	 * @param formId
	 * @param formVersion
	 * @param formRootElement
	 */
	// @JavascriptInterface
	public void addRowWithCollectAndSpecificForm(String tableName,
			String formId, String formVersion, String formRootElement) {
		weakTable.get().addRowWithCollectAndSpecificForm(tableName, formId,
				formVersion, formRootElement);
	}

	/**
	 * @see ControlIf#addRowWithCollectAndPrepopulatedValues(String, String)
	 * @param tableName
	 * @param jsonMap
	 */
	// @JavascriptInterface
	public void addRowWithCollectAndPrepopulatedValues(String tableName,
			String jsonMap) {
		weakTable.get().addRowWithCollectAndPrepopulatedValues(tableName,
				jsonMap);
	}

	/**
	 * @see ControlIf#addRowWithCollectAndSpecificFormAndPrepopulatedValues(String, String, String, String, String)
	 * @param tableName
	 * @param formId
	 * @param formVersion
	 * @param formRootElement
	 * @param jsonMap
	 */
	// @JavascriptInterface
	public void addRowWithCollectAndSpecificFormAndPrepopulatedValues(
			String tableName, String formId, String formVersion,
			String formRootElement, String jsonMap) {
		weakTable.get().addRowWithCollectAndSpecificFormAndPrepopulatedValues(
				tableName, formId, formVersion, formRootElement, jsonMap);
	}

}