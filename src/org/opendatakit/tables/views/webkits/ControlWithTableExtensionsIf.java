package org.opendatakit.tables.views.webkits;

import java.lang.ref.WeakReference;

import org.json.JSONArray;
import org.opendatakit.tables.views.webkits.CustomView.Control;
import org.opendatakit.tables.views.webkits.CustomView.TableData;

import android.webkit.JavascriptInterface;

class ControlWithTableExtensionsIf {
	private WeakReference<ExtendedTableControl> weakExtendedTableControl;
	private WeakReference<Control> weakControl;

	ControlWithTableExtensionsIf(ExtendedTableControl ec, Control ref) {
		this.weakExtendedTableControl = new WeakReference<ExtendedTableControl>(
				ec);
		this.weakControl = new WeakReference<Control>(ref);
	}

	@JavascriptInterface
	public boolean openTable(String tableName, String query) {
		return weakControl.get().openTable(tableName, query);
	}

	@JavascriptInterface
	public boolean openTableWithSqlQuery(String tableName,
			String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().openTableWithSqlQuery(tableName,
				sqlWhereClause, sqlSelectionArgs);
	}

	@JavascriptInterface
	public boolean openTableToListViewWithFile(String tableName,
			String searchText, String filename) {
		return weakControl.get().openTableToListViewWithFile(tableName,
				searchText, filename);
	}

	@JavascriptInterface
	public boolean openTableToListViewWithFileAndSqlQuery(String tableName,
			String filename, String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().openTableToListViewWithFileAndSqlQuery(
				tableName, filename, sqlWhereClause, sqlSelectionArgs);
	}

	@JavascriptInterface
	public boolean openTableToMapViewWithSqlQuery(String tableName,
			String sqlWhereClause, String[] sqlSelectionArgs) {
		return weakControl.get().openTableToMapViewWithSqlQuery(tableName,
				sqlWhereClause, sqlSelectionArgs);
	}

	@JavascriptInterface
	public boolean openTableToMapView(String tableName, String searchText) {
		return weakControl.get().openTableToMapView(tableName, searchText);
	}

	@JavascriptInterface
	public String getDbNameForTable(String displayName) {
		return weakControl.get().getDbNameForTable(displayName);
	}

	@JavascriptInterface
	public String getElementKeyForColumn(String tableDisplayName,
			String columnDisplayName) {
		return weakControl.get().getElementKeyForColumn(tableDisplayName,
				columnDisplayName);
	}

	@JavascriptInterface
	public TableDataIf query(String tableName, String searchText) {
		TableData td = weakControl.get().query(tableName, searchText);
		if (td != null) {
			return td.getJavascriptInterfaceWithWeakReference();
		} else {
			return null;
		}

	}

	@JavascriptInterface
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

	@JavascriptInterface
	public void releaseQueryResources(String tableName) {
		weakControl.get().releaseQueryResources(tableName);
	}

	@JavascriptInterface
	public JSONArray getTableDisplayNames() {
		return weakControl.get().getTableDisplayNames();
	}

	@JavascriptInterface
	public void launchHTML(String filename) {
		weakControl.get().launchHTML(filename);
	}

	/**
	 * Only makes sense when we are on a list view.
	 *
	 * @param index
	 * @return
	 */
	@JavascriptInterface
	public boolean openItem(int index) {
		return weakControl.get().openItem(index);
	}

	@JavascriptInterface
	public boolean openDetailViewWithFile(int index, String filename) {
		return weakControl.get().openDetailViewWithFile(index, filename);
	}

	@JavascriptInterface
	public boolean selectItem(int index) {
		return weakExtendedTableControl.get().selectItem(index);
	}

}