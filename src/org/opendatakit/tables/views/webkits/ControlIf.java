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

	// @JavascriptInterface
	public boolean openTable(String tableName, String query) {
		return weakControl.get().openTable(tableName, query);
	}

	// @JavascriptInterface
	public boolean openTableWithSqlQuery(String tableName,
			String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().openTableWithSqlQuery(tableName,
				sqlWhereClause, sqlSelectionArgs);
	}

	// @JavascriptInterface
	public boolean openTableToListViewWithFile(String tableName,
			String searchText, String filename) {
		return weakControl.get().openTableToListViewWithFile(tableName,
				searchText, filename);
	}

	// @JavascriptInterface
	public boolean openTableToListViewWithFileAndSqlQuery(String tableName,
			String filename, String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().openTableToListViewWithFileAndSqlQuery(
				tableName, filename, sqlWhereClause, sqlSelectionArgs);
	}

	// @JavascriptInterface
	public boolean openTableToMapViewWithSqlQuery(String tableName,
			String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().openTableToMapViewWithSqlQuery(tableName,
				sqlWhereClause, sqlSelectionArgs);
	}

	// @JavascriptInterface
	public boolean openTableToMapView(String tableName, String searchText) {
		return weakControl.get().openTableToMapView(tableName, searchText);
	}

	// @JavascriptInterface
	public boolean openTableToSpreadsheetView(String tableName,
			String searchText) {
		return weakControl.get().openTableToSpreadsheetView(tableName,
				searchText);
	}

	// @JavascriptInterface
	public boolean openTableToSpreadsheetViewWithSqlQuery(String tableName,
			String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().openTableToSpreadsheetViewWithSqlQuery(
				tableName, sqlWhereClause, sqlSelectionArgs);
	}

	// @JavascriptInterface
	public String getDbNameForTable(String displayName) {
		return weakControl.get().getDbNameForTable(displayName);
	}

	// @JavascriptInterface
	public String getElementKeyForColumn(String tableDisplayName,
			String columnDisplayName) {
		return weakControl.get().getElementKeyForColumn(tableDisplayName,
				columnDisplayName);
	}

	// @JavascriptInterface
	public TableDataIf query(String tableName, String searchText) {
		TableData td = weakControl.get().query(tableName, searchText);
		if (td != null) {
			return td.getJavascriptInterfaceWithWeakReference();
		} else {
			return null;
		}
	}

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
	 * statements, above.
	 *
	 * @param tableName
	 *            -- display name for the table
	 */
	// @JavascriptInterface
	public void releaseQueryResources(String tableName) {
		weakControl.get().releaseQueryResources(tableName);
	}

	// @JavascriptInterface
	public JSONArray getTableDisplayNames() {
		return weakControl.get().getTableDisplayNames();
	}

	// @JavascriptInterface
	public void launchHTML(String filename) {
		weakControl.get().launchHTML(filename);
	}

	/**
	 * Only makes sense when we are on a list view.
	 *
	 * @param index
	 * @return
	 */
	// @JavascriptInterface
	public boolean openItem(int index) {
		return weakControl.get().openItem(index);
	}

	// @JavascriptInterface
	public boolean openDetailViewWithFile(int index, String filename) {
		return weakControl.get().openDetailViewWithFile(index, filename);
	}

	// @JavascriptInterface
	public void addRowWithCollect(String tableName) {
		weakControl.get().addRowWithCollect(tableName);
	}

	// @JavascriptInterface
	public void addRowWithCollectAndSpecificForm(String tableName,
			String formId, String formVersion, String formRootElement) {
		weakControl.get().addRowWithCollectAndSpecificForm(tableName, formId,
				formVersion, formRootElement);
	}

	// @JavascriptInterface
	public void addRowWithCollectAndPrepopulatedValues(String tableName,
			String jsonMap) {
		weakControl.get().addRowWithCollectAndPrepopulatedValues(tableName,
				jsonMap);
	}

	// @JavascriptInterface
	public void addRowWithCollectAndSpecificFormAndPrepopulatedValues(
			String tableName, String formId, String formVersion,
			String formRootElement, String jsonMap) {
		weakControl.get()
				.addRowWithCollectAndSpecificFormAndPrepopulatedValues(
						tableName, formId, formVersion, formRootElement,
						jsonMap);
	}

}