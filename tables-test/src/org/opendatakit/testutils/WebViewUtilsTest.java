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
import org.opendatakit.common.android.data.ElementDataType;
import org.opendatakit.common.android.data.ElementType;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.tables.utils.WebViewUtil;
import org.opendatakit.tables.views.webkits.Control;
import org.opendatakit.tables.views.webkits.ControlStub;
import org.robolectric.RobolectricTestRunner;

import android.content.ContentValues;
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
    String elementKey = "anyElementKey";
    ColumnDefinition intCD = TestConstants.getColumnDefinitionMock(
        elementKey,
        columnType);
    
    ArrayList<ColumnDefinition> cdMock = new ArrayList<ColumnDefinition>();
    cdMock.add(intCD);

    ODKDatabaseUtils wrapperMock = mock(ODKDatabaseUtils.class);

    ArrayList<String> tableIds = new ArrayList<String>();
    tableIds.add(tableId);
    doReturn(tableIds).when(wrapperMock.getAllTableIds(any(SQLiteDatabase.class)));
    
    List<Column> columns = new ArrayList<Column>();
    columns.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
        ElementDataType.string.name(), "[]"));
    columns.add(new Column(TestConstants.ElementKeys.INT_COLUMN, TestConstants.ElementKeys.INT_COLUMN,
        ElementDataType.integer.name(), "[]"));
    columns.add(new Column(TestConstants.ElementKeys.NUMBER_COLUMN, TestConstants.ElementKeys.NUMBER_COLUMN,
        ElementDataType.number.name(), "[]"));
    ArrayList<ColumnDefinition> orderedDefns = ColumnDefinition.buildColumnDefinitions(columns);
    
    doReturn(columns).when(wrapperMock.getUserDefinedColumns(any(SQLiteDatabase.class), eq(tableId)));
    ODKDatabaseUtils.set(wrapperMock);
    
    Map<String,String> invalidMap = new HashMap<String,String>();
    invalidMap.put("bogus","4");
    
    ContentValues contentValues = WebViewUtil.getContentValuesFromMap(
        null, 
        "tables",
        "table1",
        cdMock,
        invalidMap);
    assertThat(contentValues).isNull();
  }

}
