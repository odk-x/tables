package org.opendatakit.tables.views.webkits;

import java.lang.ref.WeakReference;

import org.opendatakit.tables.views.webkits.CustomView.TableData;

import android.webkit.JavascriptInterface;

public class TableDataIf {
	private WeakReference<TableData> weakTable;

	TableDataIf(TableData table) {
		this.weakTable = new WeakReference<TableData>(table);
	}

	@JavascriptInterface
	public boolean inCollectionMode() {
		return weakTable.get().inCollectionMode();
	}

	@JavascriptInterface
	public int getCount() {
		return weakTable.get().getCount();
	}

	@JavascriptInterface
	public String getColumnData(String colName) {
		return weakTable.get().getColumnData(colName);
	}

	@JavascriptInterface
	public String getColumns() {
		return weakTable.get().getColumns();
	}

	@JavascriptInterface
	public String getForegroundColor(String colName, String value) {
		return weakTable.get().getForegroundColor(colName, value);
	}

	@JavascriptInterface
	public int getCollectionSize(int rowNum) {
		return weakTable.get().getCollectionSize(rowNum);
	}

	@JavascriptInterface
	public boolean isIndexed() {
		return weakTable.get().isIndexed();
	}

	@JavascriptInterface
	public String getData(int rowNum, String colName) {
		return weakTable.get().getData(rowNum, colName);
	}

	@JavascriptInterface
	public void editRowWithCollect(int rowNumber) {
		weakTable.get().editRowWithCollect(rowNumber);
	}

	@JavascriptInterface
	public void editRowWithCollectAndSpecificForm(int rowNumber, String formId,
			String formVersion, String formRootElement) {
		weakTable.get().editRowWithCollectAndSpecificForm(rowNumber, formId,
				formVersion, formRootElement);
	}

	@JavascriptInterface
	public void addRowWithCollect(String tableName) {
		weakTable.get().addRowWithCollect(tableName);
	}

	@JavascriptInterface
	public void addRowWithCollectAndSpecificForm(String tableName,
			String formId, String formVersion, String formRootElement) {
		weakTable.get().addRowWithCollectAndSpecificForm(tableName, formId,
				formVersion, formRootElement);
	}

	@JavascriptInterface
	public void addRowWithCollectAndPrepopulatedValues(String tableName,
			String jsonMap) {
		weakTable.get().addRowWithCollectAndPrepopulatedValues(tableName,
				jsonMap);
	}

	@JavascriptInterface
	public void addRowWithCollectAndSpecificFormAndPrepopulatedValues(
			String tableName, String formId, String formVersion,
			String formRootElement, String jsonMap) {
		weakTable.get().addRowWithCollectAndSpecificFormAndPrepopulatedValues(
				tableName, formId, formVersion, formRootElement, jsonMap);
	}

}