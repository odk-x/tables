package org.opendatakit.tables.views.webkits;

import java.lang.ref.WeakReference;

import org.json.JSONArray;
import org.opendatakit.tables.views.webkits.CustomView.Control;

class ControlWithTableExtensionsPreSdk14If {

	private WeakReference<ExtendedTableControl> weakExtendedTableControl;
	private WeakReference<Control> weakControl;

	ControlWithTableExtensionsPreSdk14If(ExtendedTableControl ec, Control ref) {
		this.weakExtendedTableControl = new WeakReference<ExtendedTableControl>(ec);
		this.weakControl = new WeakReference<Control>(ref);
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
		return weakControl.get().openTableToListViewWithFile(tableName, searchText, filename);
	}

	public boolean openTableToListViewWithFileAndSqlQuery(String tableName,
			String filename, String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().openTableToListViewWithFileAndSqlQuery(tableName, filename,
				sqlWhereClause, sqlSelectionArgs);
	}

	public boolean openTableToMapViewWithSqlQuery(String tableName,
			String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().openTableToMapViewWithSqlQuery(tableName, sqlWhereClause,
				sqlSelectionArgs);
	}

	public boolean openTableToMapView(String tableName, String searchText) {
		return weakControl.get().openTableToMapView(tableName, searchText);
	}

	public String getDbNameForTable(String displayName) {
		return weakControl.get().getDbNameForTable(displayName);
	}

	public String getElementKeyForColumn(String tableDisplayName,
			String columnDisplayName) {
		return weakControl.get().getElementKeyForColumn(tableDisplayName, columnDisplayName);
	}

	public String query(String tableName, String searchText) {
		return null;
	}

	public String queryWithSql(String tableName, String whereClause,
			String[] selectionArgs) {
		return null;
	}

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

	public boolean selectItem(int index) {
		return weakExtendedTableControl.get().selectItem(index);
	}

}