package org.opendatakit.tables.views.webkits;

import java.lang.ref.WeakReference;

import org.json.JSONArray;
import org.opendatakit.tables.views.webkits.CustomView.Control;

public class ControlPreSdk14If {

	private WeakReference<Control> weakControl;

	ControlPreSdk14If(Control control) {
		weakControl = new WeakReference<Control>(control);
	}

	public boolean openTable(String tableName, String query) {
		return weakControl.get().openTable(tableName, query);
	}

	public boolean openTableWithSqlQuery(String tableName,
			String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().openTableWithSqlQuery(tableName, sqlWhereClause,
				sqlSelectionArgs);
	}

	public boolean openTableToListViewWithFile(String tableName,
			String searchText, String filename) {
		return weakControl.get().openTableToListViewWithFile(tableName, searchText,
				filename);
	}

	public boolean openTableToListViewWithFileAndSqlQuery(String tableName,
			String filename, String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().openTableToListViewWithFileAndSqlQuery(tableName,
				filename, sqlWhereClause, sqlSelectionArgs);
	}

	public boolean openTableToMapViewWithSqlQuery(String tableName,
			String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().openTableToMapViewWithSqlQuery(tableName,
				sqlWhereClause, sqlSelectionArgs);
	}

	public boolean openTableToMapView(String tableName, String searchText) {
		return weakControl.get().openTableToMapView(tableName, searchText);
	}

	public boolean openTableToSpreadsheetView(String tableName,
			String searchText) {
		return weakControl.get().openTableToSpreadsheetView(tableName, searchText);
	}

	public boolean openTableToSpreadsheetViewWithSqlQuery(String tableName,
			String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().openTableToSpreadsheetViewWithSqlQuery(tableName,
				sqlWhereClause, sqlSelectionArgs);
	}

	public String getDbNameForTable(String displayName) {
		return weakControl.get().getDbNameForTable(displayName);
	}

	public String getElementKeyForColumn(String tableDisplayName,
			String columnDisplayName) {
		return weakControl.get().getElementKeyForColumn(tableDisplayName,
				columnDisplayName);
	}

	public String query(String tableName, String searchText) {
		return null;
	}

	public String queryWithSql(String tableName, String whereClause,
			String[] selectionArgs) {
		return null;
	}

	/**
	 * Releases the results returned from the query() and queryWithSql()
	 * statements, above.
	 *
	 * @param tableName
	 *            -- display name for the table
	 */
	public void releaseQueryResources(String tableName) {
		weakControl.get().releaseQueryResources(tableName);
	}

	public JSONArray getTableDisplayNames() {
		return weakControl.get().getTableDisplayNames();
	}

	public void launchHTML(String filename) {
		weakControl.get().launchHTML(filename);
	}

	/**
	 * Only makes sense when we are on a list view.
	 *
	 * @param index
	 * @return
	 */
	public boolean openItem(int index) {
		return weakControl.get().openItem(index);
	}

	public boolean openDetailViewWithFile(int index, String filename) {
		return weakControl.get().openDetailViewWithFile(index, filename);
	}

	public void addRowWithCollect(String tableName) {
		weakControl.get().addRowWithCollect(tableName);
	}

	public void addRowWithCollectAndSpecificForm(String tableName,
			String formId, String formVersion, String formRootElement) {
		weakControl.get().addRowWithCollectAndSpecificForm(tableName, formId,
				formVersion, formRootElement);
	}

	public void addRowWithCollectAndPrepopulatedValues(String tableName,
			String jsonMap) {
		weakControl.get().addRowWithCollectAndPrepopulatedValues(tableName, jsonMap);
	}

	public void addRowWithCollectAndSpecificFormAndPrepopulatedValues(
			String tableName, String formId, String formVersion,
			String formRootElement, String jsonMap) {
		weakControl.get().addRowWithCollectAndSpecificFormAndPrepopulatedValues(
				tableName, formId, formVersion, formRootElement, jsonMap);
	}

}