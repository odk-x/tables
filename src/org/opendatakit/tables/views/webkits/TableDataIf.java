package org.opendatakit.tables.views.webkits;

import java.lang.ref.WeakReference;

import org.opendatakit.tables.views.webkits.CustomView.TableData;

public class TableDataIf {
	private WeakReference<TableData> weakTable;

	TableDataIf(TableData table) {
		this.weakTable = new WeakReference<TableData>(table);
	}

    public boolean inCollectionMode() {
      return weakTable.get().inCollectionMode();
    }

    public int getCount() {
      return weakTable.get().getCount();
    }

    public String getColumnData(String colName) {
      return weakTable.get().getColumnData(colName);
    }

    public String getColumns() {
      return weakTable.get().getColumns();
    }

    public String getForegroundColor(String colName, String value) {
      return weakTable.get().getForegroundColor(colName, value);
    }

    public int getCollectionSize(int rowNum) {
      return weakTable.get().getCollectionSize(rowNum);
    }

    public boolean isIndexed() {
      return weakTable.get().isIndexed();
    }

    public String getData(int rowNum, String colName) {
      return weakTable.get().getData(rowNum, colName);
    }

    public void editRowWithCollect(int rowNumber) {
    	weakTable.get().editRowWithCollect(rowNumber);
    }

    public void editRowWithCollectAndSpecificForm(int rowNumber,
        String formId, String formVersion, String formRootElement) {
    	weakTable.get().editRowWithCollectAndSpecificForm(rowNumber, formId,
          formVersion, formRootElement);
    }

    public void addRowWithCollect(String tableName) {
    	weakTable.get().addRowWithCollect(tableName);
    }

    public void addRowWithCollectAndSpecificForm(String tableName,
        String formId, String formVersion, String formRootElement) {
    	weakTable.get().addRowWithCollectAndSpecificForm(tableName, formId,
          formVersion, formRootElement);
    }

    public void addRowWithCollectAndPrepopulatedValues(String tableName,
        String jsonMap) {
    	weakTable.get().addRowWithCollectAndPrepopulatedValues(tableName,
          jsonMap);
    }

    public void addRowWithCollectAndSpecificFormAndPrepopulatedValues(
        String tableName, String formId, String formVersion,
        String formRootElement, String jsonMap) {
    	weakTable.get().addRowWithCollectAndSpecificFormAndPrepopulatedValues(
          tableName, formId, formVersion, formRootElement, jsonMap);
    }

  }