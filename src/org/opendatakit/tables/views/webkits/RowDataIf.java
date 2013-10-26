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
	 * Takes the display name for the column and returns the value in that 
	 * column. Any null values are replaced by the empty string.
	 * <p>
	 * Returns null if the column matching the passed in user label could not be
	 * found.
	 *
	 * @param key the display name of the column
	 * @return the datum at the given column name
	 */
	// @JavascriptInterface
	public String get(String key) {
		return weakRowData.get().get(key);
	}

	/**
	 * Edit the row with collect. Uses the default form.
	 */
	// @JavascriptInterface
	public void editRowWithCollect() {
		weakRowData.get().editRowWithCollect();
	}

	/**
	 * Edit the row with collect.
	 * <p>
	 * Similar to {@link #editRowWithCollect()}, except that it allows you to
	 * edit the row with a specific form.
	 *
	 * @param formId
	 * @param formVersion
	 * @param formRootElement
	 */
	// @JavascriptInterface
	public void editRowWithCollectAndSpecificForm(String formId,
			String formVersion, String formRootElement) {
		weakRowData.get().editRowWithCollectAndSpecificForm(formId,
				formVersion, formRootElement);
	}
	
	/**
	 * Edit the row with Survey and the specified form.
	 * @param formId
	 * @param screenPath
	 * @param formPath
	 * @param refId
	 */
	// @JavascriptInterface
	public void editRowWithSurveyAndSpecificForm(String formId, 
	    String screenPath) {
	  weakRowData.get().editRowWithSurveyAndSpecificForm(formId, screenPath);
	}

	/**
	 * @see ControlIf#addRowWithCollect(String)
	 * @param tableName
	 */
	// @JavascriptInterface
	public void addRowWithCollect(String tableName) {
		weakRowData.get().addRowWithCollect(tableName);
	}

	/**
	 * @see ControlIf#addRowWithCollectAndPrepopulatedValues(String, String)
	 * @param tableName
	 * @param jsonMap
	 */
	// @JavascriptInterface
	public void addRowWithCollectAndPrepopulatedValues(String tableName,
			String jsonMap) {
		weakRowData.get().addRowWithCollectAndPrepopulatedValues(tableName,
				jsonMap);
	}

	/**
	 * @see ControlIf#addRowWithCollectAndSpecificForm(String, String, String, String)
	 */
	// @JavascriptInterface
	public void addRowWithCollectAndSpecificForm(String tableName,
			String formId, String formVersion, String formRootElement) {
		weakRowData.get().addRowWithCollectAndSpecificForm(tableName, formId,
				formVersion, formRootElement);
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
		weakRowData.get()
				.addRowWithCollectAndSpecificFormAndPrepopulatedValues(
						tableName, formId, formVersion, formRootElement,
						jsonMap);
	}

}