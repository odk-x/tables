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

import org.json.JSONArray;
import org.opendatakit.tables.views.webkits.CustomView.Control;
import org.opendatakit.tables.views.webkits.CustomView.TableData;

public class ControlIf {

	private WeakReference<Control> weakControl;

	ControlIf(Control control) {
		weakControl = new WeakReference<Control>(control);
	}

	/**
	 * Open the table with the given name and searches with the given query.
	 * Opens to the current default view of the table.
	 * @param tableName the display name of the table
	 * @param query a query string, represented by a space-separated list of
	 * "displayName:cellContents". The easiest way to get the correct formatting
	 * of this string would be to drag and drop contents into the searchbox
	 * of the Spreadsheet view, letting Tables handle the formatting.
	 * @return true if the open succeeded
	 */
	// @JavascriptInterface
	public boolean openTable(String tableName, String query) {
		return weakControl.get().openTable(tableName, query);
	}

	/**
	 * Open the table with the given name using a sql query.
	 * <p>
	 * Performs a simple "SELECT * FROM" statement with the sqlWhereClause
	 * performing a query.
	 * <p>
	 * This is useful for doing more complicated joins than are possible with
	 * a simple query string, but is possible using SQL. Note that in this case,
	 * even though the SQL query is restricting the rows shown in the table, the
	 * query will not appear in the search box.
	 * @see #queryWithSql(String, String, String[])
	 * @param tableName the displayName of the table to open
	 * @param sqlWhereClause the where clause for the selection, beginning with
	 * "WHERE". Must include "?" instead of actual values, which are instead
	 * passed in the sqlSelectionArgs parameter. The references to tables must
	 * use the database table names, as can be retrieved with
	 * {@link #getDbNameForTable(String)}. The references to the columns
	 * must be the element key, not the display name of the column. The
	 * elementKey is fixed and unchanging for each column, and can be retrieved
	 * using {@link #getElementKeyForColumn(String, String)}.
	 * @param sqlSelectionArgs an array of selection arguments, one for each "?"
	 * in sqlWhereClause
	 * @return true if the open succeeded
	 */
	// @JavascriptInterface
	public boolean openTableWithSqlQuery(String tableName,
			String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().openTableWithSqlQuery(tableName,
				sqlWhereClause, sqlSelectionArgs);
	}

	/**
	 * Open the table specified by tableName as a list view using the specified
	 * filename as a list view. The filename is relative to the odk tables path.
	 * @param tableName the display name of the table to open
	 * @param searchText the search string with which to search the table.
	 * @param filename the filename of the list view with which to open the
	 * table. Must be relative to the odk tables path and must not start with
	 * a delimeter. I.e. "your/path/to/file.html" would be an acceptable
	 * parameter, where as "/your/path/to/file.html" would not.
	 * @return true if the open succeeded
	 */
	// @JavascriptInterface
	public boolean openTableToListViewWithFile(String tableName,
			String searchText, String filename) {
		return weakControl.get().openTableToListViewWithFile(tableName,
				searchText, filename);
	}

	/**
	 * Open the given table with the given list view, restricted by the given
	 * SQL query.
	 * @see #openTableToListViewWithFile(String, String, String)
	 * @see #openTableWithSqlQuery(String, String, String[])
	 * @see #queryWithSql(String, String, String[])
	 * @param tableName
	 * @param filename
	 * @param sqlWhereClause
	 * @param sqlSelectionArgs
	 * @return true if the open succeeded
	 */
	// @JavascriptInterface
	public boolean openTableToListViewWithFileAndSqlQuery(String tableName,
			String filename, String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().openTableToListViewWithFileAndSqlQuery(
				tableName, filename, sqlWhereClause, sqlSelectionArgs);
	}

	/**
	 * Open the given table to the map view, restricted with the given SQL
	 * query.
	 * @see #openTableWithSqlQuery(String, String, String[])
	 * @see #queryWithSql(String, String, String[])
	 * @param tableName display name of the table to open.
	 * @param sqlWhereClause
	 * @param sqlSelectionArgs
	 * @return true if the open succeeded
	 */
	// @JavascriptInterface
	public boolean openTableToMapViewWithSqlQuery(String tableName,
			String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().openTableToMapViewWithSqlQuery(tableName,
				sqlWhereClause, sqlSelectionArgs);
	}

	/**
	 * Open the given table to the map view, restricted with the given query
	 * string. Uses the settings that have last been saved to map view.
	 * @see #openTable(String, String)
	 * @param tableName
	 * @param searchText
	 * @return true if the open succeeded
	 */
	// @JavascriptInterface
	public boolean openTableToMapView(String tableName, String searchText) {
		return weakControl.get().openTableToMapView(tableName, searchText);
	}

	/**
	 * Open the table to spreadsheet view, restricting by the given query.
	 * @see #openTable(String, String)
	 * @param tableName
	 * @param searchText
	 * @return true if the open succeeded
	 */
	// @JavascriptInterface
	public boolean openTableToSpreadsheetView(String tableName,
			String searchText) {
		return weakControl.get().openTableToSpreadsheetView(tableName,
				searchText);
	}

	/**
	 * Open the table to spreadsheet view, restricting by the given SQL query.
	 * @see #openTable(String, String)
	 * @see #openTableWithSqlQuery(String, String, String[])
	 * @see #queryWithSql(String, String, String[])
	 * @param tableName
	 * @param sqlWhereClause
	 * @param sqlSelectionArgs
	 * @return true if the open succeeded
	 */
	// @JavascriptInterface
	public boolean openTableToSpreadsheetViewWithSqlQuery(String tableName,
			String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().openTableToSpreadsheetViewWithSqlQuery(
				tableName, sqlWhereClause, sqlSelectionArgs);
	}

	/**
	 * Get the database name of the table with the given display name. This is
	 * necessary for sophisticated queries like those allowed with
	 * {@link #openTableWithSqlQuery(String, String, String[])}.
	 * @param displayName display name of the table
	 * @return the database name of the table
	 */
	// @JavascriptInterface
	public String getDbNameForTable(String displayName) {
		return weakControl.get().getDbNameForTable(displayName);
	}

	/**
	 * Get the database name of the column, also known as the element key. This
	 * is necessary for sophisticated queries like those allowed with
	 * {@link #openTableWithSqlQuery(String, String, String[])}.
	 * @param tableDisplayName display name of the table holding the column
	 * @param columnDisplayName display name of the column
	 * @return the element key (i.e. the database name) of the column with the
	 * given display name in the given table.
	 */
	// @JavascriptInterface
	public String getElementKeyForColumn(String tableDisplayName,
			String columnDisplayName) {
		return weakControl.get().getElementKeyForColumn(tableDisplayName,
				columnDisplayName);
	}

	/**
	 * Return a {@link TableDataIf} object for the given table name, queried
	 * with the given searchText.
	 * @param tableName display name of the table
	 * @param searchText a search string as defined in
	 * {@link #openTable(String, String)}
	 * @return a new TableDataIf with the results of the query. Should be
    * released with {@link #releaseQueryResources(String)} when it is no longer
    * needed.
	 */
	// @JavascriptInterface
	public TableDataIf query(String tableName, String searchText) {
		TableData td = weakControl.get().query(tableName, searchText);
		if (td != null) {
			return td.getJavascriptInterfaceWithWeakReference();
		} else {
			return null;
		}
	}

	/**
	 * Return a {@link TableDataIf} object for the given table name, queried
	 * using SQL.
	 * <p>
	 * For example, if you wanted all the rows where the column foo equaled
	 * bar, the where clause would be "WHERE foo = ? ", and the selection args
	 * would be ["bar"].
	 * <p>
	 * This can be used to do powerful cross-table queries.
	 * @param tableName display name of the table
	 * @param whereClause an SQL where clause as specified in
	 * {@link #openTableWithSqlQuery(String, String, String[])}.
	 * @param selectionArgs selection arguments for the whereClause as
	 * specified in {@link #openTableWithSqlQuery(String, String, String[])}.
	 * @return a new TableDataIf with the results of the query. Should be
	 * released with {@link #releaseQueryResources(String)} when it is no longer
	 * needed.
	 */
	// @JavascriptInterface
	public TableDataIf queryWithSql(String tableName, String whereClause,
			String[] selectionArgs) {
		TableData td = weakControl.get().queryWithSql(tableName, whereClause,
				selectionArgs);
		if (td != null) {
			return td.getJavascriptInterfaceWithWeakReference();
		} else {
			return null;
		}
	}

	/**
	 * Releases the results returned from the query() and queryWithSql()
	 * statements, above. The object will be retained until this method is
	 * called, so good practice is to release the query when its data is no
	 * longer needed to free up resources.
	 * @param tableName display name for the table
	 */
	// @JavascriptInterface
	public void releaseQueryResources(String tableName) {
		weakControl.get().releaseQueryResources(tableName);
	}

	/**
	 * Get the display names of all the tables in the database.
	 * @return an JSONArray containing the display names
	 */
	// @JavascriptInterface
	public JSONArray getTableDisplayNames() {
		return weakControl.get().getTableDisplayNames();
	}

	/**
	 * Launch an arbitrary HTML file specified by filename.
	 * @see #openTableToListViewWithFile(String, String, String)
	 * @param filename file name relative to the ODK Tables folder.
	 */
	// @JavascriptInterface
	public void launchHTML(String filename) {
		weakControl.get().launchHTML(filename);
	}

	/**
	 * Opens the detail view for the item at the given index.
	 * <p>
	 * Only makes sense when we are on a list view.
	 * @param index
	 * @return true if the open succeeded
	 */
	// @JavascriptInterface
	public boolean openItem(int index) {
		return weakControl.get().openItem(index);
	}

	/**
	 * Open the detail view for the item at the given index with the given
	 * filename.
	 * <p>
	 * Only makes sense when we are in a list view.
	 * @param index
	 * @param filename
	 * @return true if the open suceeded
	 */
	// @JavascriptInterface
	public boolean openDetailViewWithFile(int index, String filename) {
		return weakControl.get().openDetailViewWithFile(index, filename);
	}

	/**
	 * Add a row using Collect and the default form. Uses the query string
	 * currently restricting the data of the table to prepopulate values in
	 * Collect. I.e. if you have searched for id:12, the id field of the Collect
	 * form (if it exists) will be prepopulated with the value 12.
	 * @param tableName the display name of the table to receive the add.
	 */
	// @JavascriptInterface
	public void addRowWithCollect(String tableName) {
		weakControl.get().addRowWithCollect(tableName);
	}

	/**
	 * Add a row using Collect and the form specified by the given parameters.
	 * Allows you to specify an arbitrary form depending on the work flow.
	 * <p>
	 * The form must have been added to Collect and visible in the "Fill Blank
	 * Forms" screen.
	 * <p>
	 * Values are prepopulated as described in
	 * {@link #addRowWithCollect(String)}.
	 * @param tableName the display name of the table to receive the add
	 * @param formId
	 * @param formVersion may be null
	 * @param formRootElement
	 */
	// @JavascriptInterface
	public void addRowWithCollectAndSpecificForm(String tableName,
			String formId, String formVersion, String formRootElement) {
		weakControl.get().addRowWithCollectAndSpecificForm(tableName, formId,
				formVersion, formRootElement);
	}

	/**
	 * Add a row using Collect and the default form. Similar to
	 * {@link #addRowWithCollect(String)}, except that rather than using the
	 * query string to prepopulate values, a json map is used.
	 * @param tableName
	 * @param jsonMap a JSON map of element key to value, as retrieved by
	 * {@link #getElementKeyForColumn(String, String)}. The map can then be
	 * converted to a String using JSON.stringify() and passed to this method.
	 */
	// @JavascriptInterface
	public void addRowWithCollectAndPrepopulatedValues(String tableName,
			String jsonMap) {
		weakControl.get().addRowWithCollectAndPrepopulatedValues(tableName,
				jsonMap);
	}

	/**
	 * Add a row using Collect, a specific form, and a map of prepopulated
	 * values.
	 * @see #addRowWithCollectAndSpecificForm(String, String, String, String)
	 * @see #addRowWithCollectAndPrepopulatedValues(String, String)
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
		weakControl.get()
				.addRowWithCollectAndSpecificFormAndPrepopulatedValues(
						tableName, formId, formVersion, formRootElement,
						jsonMap);
	}
	
	// @JavascriptInterface
	public String getSearchText() {
	  return weakControl.get().getSearchText();
	}

}