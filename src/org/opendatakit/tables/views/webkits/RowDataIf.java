package org.opendatakit.tables.views.webkits;

import java.lang.ref.WeakReference;

import org.opendatakit.tables.views.webkits.CustomView.RowData;

public class RowDataIf {
	private WeakReference<RowData> weakRowData;

	RowDataIf(RowData row) {
		this.weakRowData = new WeakReference<RowData>(row);
	}

    /**
     * Takes the user label for the column and returns the value in that column.
     * Any null values are replaced by the empty string.
     * <p>
     * Returns null if the column matching the passed in user label could not be
     * found.
     *
     * @param key
     * @return
     */
    public String get(String key) {
    	return weakRowData.get().get(key);
    }

	/**
     * Edit the row with collect. Uses the form specified in the table
     * properties or else the ODKTables-generated default form.
     */
    public void editRowWithCollect() {
    	weakRowData.get().editRowWithCollect();
    }

    /**
     * Edit the row with collect.
     * <p>
     * Similar to {@link #editRowWithCollect()}, except that it allows you to
     * edit the row with a specific form.
     *
     * @param tableName
     * @param formId
     * @param formVersion
     * @param formRootElement
     */
    public void editRowWithCollectAndSpecificForm(String formId, String formVersion,
        String formRootElement) {
    	weakRowData.get().editRowWithCollectAndSpecificForm(formId, formVersion, formRootElement);
    }

    /**
     * Add a row using collect and the default form.
     *
     * @param tableName
     */
    public void addRowWithCollect(String tableName) {
    	weakRowData.get().addRowWithCollect(tableName);
    }

    public void addRowWithCollectAndPrepopulatedValues(String tableName,
        String jsonMap) {
    	weakRowData.get().addRowWithCollectAndPrepopulatedValues(tableName, jsonMap);
    }

    /**
     * Add a row using Collect. This is the hook into the javascript. The
     * activity holding this view must have implemented the onActivityReturn
     * method appropriately to handle the result.
     * <p>
     * It allows you to specify a form other than that which may be the default
     * for the table. It differs in {@link #addRow(String)} in that it lets you
     * add the row using an arbitrary form.
     */
    public void addRowWithCollectAndSpecificForm(String tableName, String formId,
        String formVersion, String formRootElement) {
    	weakRowData.get().addRowWithCollectAndSpecificForm(tableName, formId, formVersion, formRootElement);
    }

    public void addRowWithCollectAndSpecificFormAndPrepopulatedValues(
        String tableName, String formId, String formVersion,
        String formRootElement, String jsonMap) {
    	weakRowData.get().addRowWithCollectAndSpecificFormAndPrepopulatedValues(tableName,
    			formId, formVersion, formRootElement, jsonMap);
    }

  }