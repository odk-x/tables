package org.opendatakit.testutils;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.data.ElementType;
import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.tables.utils.WebViewUtil;
import org.opendatakit.tables.views.webkits.Control;
import org.robolectric.RobolectricTestRunner;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 *
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class WebViewUtilsTest {

  @Test
  public void getContentValuesInvalidIntFails() {
    this.assertInvalidHelper(ElementType.parseElementType("integer", false), "invalid");
  }

  @Test
  public void getContentValuesInvalidNumberFails() {
    this.assertInvalidHelper(ElementType.parseElementType("number", false), "invalid");
  }

  /**
   * Perform an assertion for an invalid value for the given column type,
   * ensuring that the insertion fails.
   * @param elementKey
   * @param columnType
   * @param invalidValue
   * @param rowId if null, calls {@link Control#addRow(String, String)}.
   * Otherwise it calls {@link Control#updateRow(String, String, String)}.
   */
  private void assertInvalidHelper(
      ElementType columnType,
      String invalidValue) {
    
    String tableId = "table1";
    String elementKey = "anyKey";
    
    SQLiteDatabase stubDb = SQLiteDatabase.create(null);
    DatabaseFactory factoryMock = mock(DatabaseFactory.class);
    doReturn(stubDb).when(factoryMock).getDatabase(any(Context.class), any(String.class));
    DatabaseFactory.set(factoryMock);

    ODKDatabaseUtils wrapperMock = mock(ODKDatabaseUtils.class);

    ArrayList<String> tableIds = new ArrayList<String>();
    tableIds.add(tableId);
    doReturn(tableIds).when(wrapperMock).getAllTableIds(any(SQLiteDatabase.class));
    
    List<Column> columns = new ArrayList<Column>();
    columns.add(new Column(elementKey,elementKey,columnType.toString(), "[]"));
    ArrayList<ColumnDefinition> orderedDefns = ColumnDefinition.buildColumnDefinitions(tableId, columns);
    
    doReturn(columns).when(wrapperMock).getUserDefinedColumns(any(SQLiteDatabase.class), eq(tableId));
    ODKDatabaseUtils.set(wrapperMock);
    
    Map<String,String> invalidMap = new HashMap<String,String>();
    invalidMap.put(elementKey,"bogus");
    
    ContentValues contentValues = WebViewUtil.getContentValuesFromMap(
        null, 
        "tables",
        tableId,
        orderedDefns,
        invalidMap);
    assertThat(contentValues).isNull();
  }

}
