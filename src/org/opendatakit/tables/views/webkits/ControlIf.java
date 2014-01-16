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

/**
 * This object is handed to all the javascript views as "control".
 * @author mitchellsundt@gmail.com
 * @author sudar.sam@gmail.com
 *
 */
public class ControlIf {

	private WeakReference<Control> weakControl;

	ControlIf(Control control) {
		weakControl = new WeakReference<Control>(control);
	}

	/**
	 * Open the table with the given name.
	 * Opens to the current default view of the table.
	 * @param tableId the table id of the table
	 * @return true if the open succeeded
	 */
	// @JavascriptInterface
	public boolean openTable(String tableId) {
		return weakControl.get().helperOpenTable(tableId, null, null);
	}
	
	/**
	 * Alias for {@link #openTableWithSqlQuery(String, String, String[])}
	 * @see #openTableWithSqlQuery(String, String, String[])
	 * @param tableId
	 * @param sqlWhereClause
	 * @param sqlSelectionArgs
	 * @return true if the open succeeded
	 */
	public boolean openTable(String tableId, String sqlWhereClause,
	    String[] sqlSelectionArgs) {
	  return openTableWithSqlQuery(tableId, sqlWhereClause, sqlSelectionArgs);
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
	 * @param tableId the table id of the table to open
	 * @param sqlWhereClause the where clause for the selection, beginning with
	 * "WHERE". Must include "?" instead of actual values, which are instead
	 * passed in the sqlSelectionArgs parameter. The references to tables must
	 * use the table ids. The references to the columns
	 * must be the element paths. The
	 * elementKey is fixed and unchanging for each column, and can be retrieved
	 * using {@link #getElementKey(String, String)}.
	 * @param sqlSelectionArgs an array of selection arguments, one for each "?"
	 * in sqlWhereClause
	 * @return true if the open succeeded
	 */
	// @JavascriptInterface
	public boolean openTableWithSqlQuery(String tableId,
			String sqlWhereClause, String[] sqlSelectionArgs) {
	  // TODO: convert to element keys
		return weakControl.get().helperOpenTable(tableId,
				sqlWhereClause, sqlSelectionArgs);
	}

	/**
	 * Open the table specified by tableName as a list view using the specified
	 * filename as a list view. The filename is relative to the odk tables path.
	 * @param tableId the table id of the table to open
	 * @param relativePath the name of the file specifying the list view,
    * relative to the app folder.
	 * @return true if the open succeeded
	 */
	// @JavascriptInterface
	public boolean openTableToListViewWithFile(String tableId,
	     String relativePath) {
		return weakControl.get().helperOpenTableWithFile(tableId,
				relativePath, null, null);
	}
	
	/**
	 * Alias for {@link #openTableToListViewWithFile(String, String)}.
	 * @see #openTableToListViewWithFile(String, String)
	 * @param tableId
	 * @param relativePath
	 * @return true if the open succeeded
	 */
	public boolean openTableToListView(String tableId, String relativePath) {
	  return openTableToListViewWithFile(tableId, relativePath);
	}
	
	/**
	 * Alias for {@link #openTableToListViewWithFileAndSqlQuery(String, String, String, String[])}
	 * @see #openTableToListViewWithFileAndSqlQuery(String, String, String, String[])
	 * @param tableId
	 * @param relativePath
	 * @param sqlWhereClause
	 * @param sqlSelectionArgs
	 * @return true if the open succeeded
	 */
	public boolean openTableToListView(String tableId, String relativePath,
	    String sqlWhereClause, String[] sqlSelectionArgs) {
	  return openTableToListViewWithFileAndSqlQuery(tableId, relativePath, 
	      sqlWhereClause, sqlSelectionArgs);
	}

	/**
	 * Open the given table with the given list view, restricted by the given
	 * SQL query.
	 * @see #openTableToListViewWithFile(String, String)
	 * @see #openTableWithSqlQuery(String, String, String[])
	 * @see #queryWithSql(String, String, String[])
	 * @param tableId
	 * @param relativePath the name of the file specifying the list view,
    * relative to the app folder.
	 * @param sqlWhereClause
	 * @param sqlSelectionArgs
	 * @return true if the open succeeded
	 */
	// @JavascriptInterface
	public boolean openTableToListViewWithFileAndSqlQuery(String tableId,
			String relativePath, String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().helperOpenTableWithFile(
				tableId, relativePath, sqlWhereClause, sqlSelectionArgs);
	}

	/**
	 * Open the given table to the map view, restricted with the given SQL
	 * query.
	 * @see #openTableWithSqlQuery(String, String, String[])
	 * @see #queryWithSql(String, String, String[])
	 * @param tableId table id of the table to open.
	 * @param sqlWhereClause
	 * @param sqlSelectionArgs
	 * @return true if the open succeeded
	 */
	// @JavascriptInterface
	public boolean openTableToMapViewWithSqlQuery(String tableId,
			String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().helperOpenTableToMapView(tableId,
				sqlWhereClause, sqlSelectionArgs);
	}

	/**
	 * Open the given table to the map view, restricted with the given query
	 * string. Uses the settings that have last been saved to map view.
	 * @see #openTable(String)
	 * @param tableId
	 * @return true if the open succeeded
	 */
	// @JavascriptInterface
	public boolean openTableToMapView(String tableId) {
		return weakControl.get().helperOpenTableToMapView(tableId, null, null);
	}
	
	/**
	 * Alias for {@link #openTableToMapViewWithSqlQuery(String, String, String[])}
	 * @see #openTableToMapViewWithSqlQuery(String, String, String[])
	 * @param tableId
	 * @param sqlWhereClause
	 * @param sqlSelectionArgs
	 * @return true if the open succeeded
	 */
	public boolean openTableToMapView(String tableId, String sqlWhereClause,
	    String[] sqlSelectionArgs) {
	  return openTableToMapViewWithSqlQuery(tableId, sqlWhereClause, 
	      sqlSelectionArgs);
	}

	/**
	 * Open the table to spreadsheet view, restricting by the given query.
	 * @see #openTable(String)
	 * @param tableId
	 * @return true if the open succeeded
	 */
	// @JavascriptInterface
	public boolean openTableToSpreadsheetView(String tableId) {
		return weakControl.get()
		    .helperOpenTableToSpreadsheetView(tableId, null, null);
	}
	
	/**
	 * Alias for {@link #openTableToSpreadsheetViewWithSqlQuery(String, String, String[])}
	 * @see #openTableToSpreadsheetViewWithSqlQuery(String, String, String[])
	 * @param tableId
	 * @param sqlWhereClause
	 * @param sqlSelectionArgs
	 * @return true if the open succeeded
	 */
	public boolean openTableToSpreadsheetView(String tableId, 
	    String sqlWhereClause, String[] sqlSelectionArgs) {
	  return openTableToSpreadsheetViewWithSqlQuery(tableId, sqlWhereClause,
	      sqlSelectionArgs);
	}

	/**
	 * Open the table to spreadsheet view, restricting by the given SQL query.
	 * @see #openTable(String)
	 * @see #openTableWithSqlQuery(String, String, String[])
	 * @see #queryWithSql(String, String, String[])
	 * @param tableId
	 * @param sqlWhereClause
	 * @param sqlSelectionArgs
	 * @return true if the open succeeded
	 */
	// @JavascriptInterface
	public boolean openTableToSpreadsheetViewWithSqlQuery(String tableId,
			String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().helperOpenTableToSpreadsheetView(
				tableId, sqlWhereClause, sqlSelectionArgs);
	}

	/**
	 * Return a {@link TableDataIf} object for the given table id, queried
	 * using SQL.
	 * <p>
	 * For example, if you wanted all the rows where the column foo equaled
	 * bar, the where clause would be "WHERE foo = ? ", and the selection args
	 * would be ["bar"].
	 * <p>
	 * This can be used to do powerful cross-table queries.
	 * @param tableId display name of the table
	 * @param whereClause an SQL where clause as specified in
	 * {@link #openTableWithSqlQuery(String, String, String[])}.
	 * @param selectionArgs selection arguments for the whereClause as
	 * specified in {@link #openTableWithSqlQuery(String, String, String[])}.
	 * @return a new TableDataIf with the results of the query. Should be
	 * released with {@link #releaseQueryResources(String)} when it is no longer
	 * needed.
	 */
	// @JavascriptInterface
	public TableDataIf queryWithSql(String tableId, String whereClause,
			String[] selectionArgs) {
		TableData td = weakControl.get().queryWithSql(tableId, whereClause,
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
	 * @param tableId display name for the table
	 */
	// @JavascriptInterface
	public void releaseQueryResources(String tableId) {
		weakControl.get().releaseQueryResources(tableId);
	}

	/**
	 * Get the table ids of all the tables in the database.
	 * @return a stringified json array of the table ids
	 */
	// @JavascriptInterface
	public String getAllTableIds() {
		return weakControl.get().getAllTableIds();
	}

	/**
	 * Launch an arbitrary HTML file specified by filename.
	 * @see ControlIf#openTableToListViewWithFile(String, String)
	 * @param relativePath file name relative to the ODK Tables folder.
	 * @return true if the file was launched, false if something went wrong
	 */
	// @JavascriptInterface
	public boolean launchHTML(String relativePath) {
		return weakControl.get().launchHTML(relativePath);
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
    * Open the item specified by the index to the detail view specified by
    * the given filename. The filename is relative to the odk tables
    * directory.
	 * <p>
	 * Only makes sense when you are in a list view.
	 * @param index
	 * @param relativePath the name of the file specifying the detail view,
    * relative to the app folder.
	 * @return true if the open succeeded
	 */
	// @JavascriptInterface
	public boolean openDetailViewWithFile(int index, String relativePath) {
		return weakControl.get().openDetailViewWithFile(index, relativePath);
	}

	/**
	 * Add a row using Collect and the default form. 
	 * @param tableId the display name of the table to receive the add.
    * @return true if the activity was launched, false if something went wrong
    * @deprecated
	 */
	// @JavascriptInterface
	public boolean addRowWithCollect(String tableId) {
		return weakControl.get().helperAddRowWithCollect(tableId, null, null, 
		    null, null);
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
    * @param tableId the display name of the table to receive the add
    * @param formId
    * @param formVersion may be null
    * @param formRootElement
    * @return true if the activity was launched, false if something went wrong
    * @deprecated
    */
   // @JavascriptInterface
   public boolean addRowWithCollectAndSpecificForm(String tableId,
         String formId, String formVersion, String formRootElement) {
      return weakControl.get().helperAddRowWithCollect(tableId, formId,
            formVersion, formRootElement, null);
   }

   /**
    * Add a row using Collect and the default form. Similar to
    * {@link #addRowWithCollect(String)}, except that rather than using the
    * query string to prepopulate values, a json map is used.
    * @param tableId
    * @param jsonMap a JSON map of element key to value, as retrieved by
    * {@link #getElementKey(String, String)}. The map can then be
    * converted to a String using JSON.stringify() and passed to this method. A
    * null value will not prepopulate any values.
    * @return true if the activity was launched, false if something went wrong
    * @deprecated
    */
   // @JavascriptInterface
   public boolean addRowWithCollectAndPrepopulatedValues(String tableId,
         String jsonMap) {
      return weakControl.get().helperAddRowWithCollect(tableId, null, null, 
          null, jsonMap);
   }
   
   /**
    * An alias for {@link #addRowWithCollectAndSpecificFormAndPrepopulatedValues(String, String, String, String, String)}.
    * Because the above method has the null-safe json map parameter, this can
    * be used as a shorter version of the other Collect methods.
    * @see #addRowWithCollectAndSpecificFormAndPrepopulatedValues(String, String, String, String, String)
    * @param tableId
    * @param formId
    * @param formVersion
    * @param formRootElement
    * @param jsonMap
    * @return true if the activity was launched, false if something went wrong
    * @deprecated
    */
   public boolean addRowWithCollect(String tableId,
       String formId, String formVersion, String formRootElement, 
       String jsonMap) {
     return addRowWithCollectAndSpecificFormAndPrepopulatedValues(tableId, 
         formId, formVersion, formRootElement, jsonMap);
   }
   
   /**
    * Add a row using Collect, a specific form, and a map of prepopulated
    * values.
    * @see #addRowWithCollectAndSpecificForm(String, String, String, String)
    * @see #addRowWithCollectAndPrepopulatedValues(String, String)
    * @param tableId
    * @param formId
    * @param formVersion
    * @param formRootElement
    * @param jsonMap
    * @return true if the activity was launched, false if something went wrong
    * @deprecated
    */
   // @JavascriptInterface
   public boolean addRowWithCollectAndSpecificFormAndPrepopulatedValues(
         String tableId, String formId, String formVersion,
         String formRootElement, String jsonMap) {
      return weakControl.get()
            .helperAddRowWithCollect(tableId, formId, formVersion, 
                formRootElement, jsonMap);
   }
   
   /**
    * Edit the given row using Collect.
    * @param tableId
    * @param rowId
    * @return true if the activity was launched, false if something went wrong
    * @deprecated
    */
   public boolean editRowWithCollect(String tableId, String rowId) {
     return this.editRowWithCollect(tableId, rowId, null, null, null);
   }
   
   /**
    * Edit the given row using Collect and a specific form.
    * @param tableId
    * @param rowId
    * @param formId
    * @param formVersion
    * @param formRootElement
    * @return true if the activity was launched, false if something went wrong
    * @deprecated
    */
   public boolean editRowWithCollectAndSpecificForm(String tableId, 
       String rowId, String formId, String formVersion, 
       String formRootElement) {
     return weakControl.get().helperEditRowWithCollect(tableId, rowId, formId,
         formVersion, formRootElement);
   }
   
   /**
    * Alias for {@link #editRowWithCollectAndSpecificForm(String, String, 
    * String, String, String)}.
    * @see #editRowWithCollect(String, String, String, String, String)
    * @param tableId
    * @param rowId
    * @param formId
    * @param formVersion
    * @param formRootElement
    * @return true if the activity was launched, false if something went wrong
    * @deprecated
    */
   public boolean editRowWithCollect(String tableId, String rowId,
       String formId, String formVersion, String formRootElement) {
     return this.editRowWithCollectAndSpecificForm(tableId, rowId, formId, 
         formVersion, formRootElement);
   }
   
   /**
    * Edit the given row using Survey and the default form.
    * @param tableId
    * @param rowId
    * @return true if the activity was launched, false if something went wrong
    */
   public boolean editRowWithSurvey(String tableId, String rowId) {
     return editRowWithSurveyAndSpecificForm(tableId, rowId, null, null);
   }
   
   /**
    * Edit the given row using Survey and a specific form.
    * @param tableId
    * @param rowId
    * @param formId
    * @param screenPath
    * @return true if the activity was launched, false if something went wrong
    */
   public boolean editRowWithSurveyAndSpecificForm(String tableId, 
       String rowId, String formId, String screenPath) {
     return weakControl.get().helperEditRowWithSurvey(tableId, rowId, formId, 
         screenPath);
   }
   
   /**
    * Alias for {@link #editRowWithSurveyAndSpecificForm(String, String, 
    * String, String)}.
    * @see #editRowWithSurveyAndSpecificForm(String, String, String, 
    * String)
    * @param tableId
    * @param rowId
    * @param formId
    * @param screenPath
    * @return true if the activity was launched, false if something went wrong
    */
   public boolean editRowWithSurvey(String tableId, String rowId,
       String formId, String screenPath) {
     return editRowWithSurveyAndSpecificForm(tableId, rowId, formId,
         screenPath);
   }
   
   /**
    * Add a row with Survey and the default form.
    * @param tableId the table to receive the add
    * @return true if Survey was launched, else false
    */
   public boolean addRowWithSurvey(String tableId) {
     return this.addRowWithSurvey(tableId, null, null, null);
   }
   
   /**
    * Alias for {@link #addRowWithSurveyAndSpecificFormAndPrepopulatedValues(String, String, String, String)}.
    * Because the json map parameter is only added if it is non-null, you can
    * use this as a shorter method for your interactions with Survey, much like
    * {@link #addRowWithCollect(String, String, String, String, String)} for
    * Collect.
    * @param tableId
    * @param formId
    * @param screenPath
    * @param jsonMap
    * @return true if the activity was launched, false if something went wrong
    */
   public boolean addRowWithSurvey(String tableId, String formId, 
       String screenPath, String jsonMap) {
     return this.addRowWithSurveyAndSpecificFormAndPrepopulatedValues(tableId,
         formId, screenPath, jsonMap);
   }
	
	/**
	 * Add a row using Survey. Not yet sure if we'll actually need to take a 
	 * table name here or not...
	 * @param tableId
	 */
	// @JavascriptInterface
	public boolean addRowWithSurveyAndSpecificForm(String tableId, String formId,
	    String screenPath) {
	  return weakControl.get().helperAddRowWithSurvey(tableId, formId,
	      screenPath, null);
	}
	
	/**
	 * Add a row using Survey and the default form, but prepopulating some
	 * values.
	 * @param tableId the id of the table to receive the add
	 * @param jsonMap a JSON map of element key to value, as retrieved by
    * {@link #getElementKey(String, String)}. The map can then be
    * converted to a String using JSON.stringify() and passed to this method. A
    * null value will not prepopulate any values. 
	 * @return true if the activity was launched, false if something went wrong
	 */
	public boolean addRowWithSurveyAndPrepopulatedValues(String tableId,
	    String jsonMap) {
	  return weakControl.get().helperAddRowWithSurvey(tableId, null, null, 
	      jsonMap);
	}
	
	/**
	 * Add a row using survey and prepopulating the form with the given values.
	 * The jsonmap should contain a mapping of elementName to value.
	 * @param tableId
	 * @param formId
	 * @param screenPath
	 * @param jsonMap
	 * @return true if the activity was launched, false if something went wrong
	 */
	public boolean addRowWithSurveyAndSpecificFormAndPrepopulatedValues(
	    String tableId, String formId, String screenPath, String jsonMap) {
	  return weakControl.get().helperAddRowWithSurvey(
	      tableId, formId, screenPath, jsonMap);
	  
	}
	
	/**
	 * Return the element key for the column with the given element path.
	 * @param tableId
	 * @param elementPath
	 * @return the element key for the column
	 */
	public String getElementKey(String tableId, String elementPath) {
	  return weakControl.get().getElementKey(tableId, elementPath);
	}
	
	/**
	 * Get the display name for the given column.
	 * @param tableId
	 * @param elementPath
	 * @return the display name for the given column
	 */
	public String getColumnDisplayName(String tableId, String elementPath) {
	  return weakControl.get().getColumnDisplayName(tableId, elementPath);
	}
	
	/**
	 * Retrieve the display name for the given table.
	 * <p>
	 * If the display name has been localized, it returns the json
	 * representation of the display name.
	 * @param tableId
	 * @return the display name for the table, in stringified json form if the
	 * name has been internationalized
	 */
	public String getTableDisplayName(String tableId) {
	  return weakControl.get().getTableDisplayName(tableId);
	}
	
	/**
	 * Determine if the column exist in the given table.
	 * @param tableId
	 * @param elementPath
	 * @return true if the column exists, else false
	 */
	public boolean columnExists(String tableId, String elementPath) {
	  return weakControl.get().columnExists(tableId, elementPath);
	}
	
	/**
	 * Take the path of a file relative to the app folder and return a url by
	 * which it can be accessed.
	 * @param relativePath
	 * @return an absolute URI to the file
	 */
	public String getFileAsUrl(String relativePath) {
	  return weakControl.get().getFileAsUrl(relativePath);
	}
	
	/**
	 * Return the platform info as a stringified json object. This is an object
	 * containing the keys: container, version, appName, baseUri, logLevel.
	 * @return a stringified json object with the above keys
	 */
	public String getPlatformInfo() {
	  return weakControl.get().getPlatformInfo();
	}

}