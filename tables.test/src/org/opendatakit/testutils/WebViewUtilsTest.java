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
import org.opendatakit.aggregate.odktables.rest.ElementType;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.common.android.application.CommonApplication;
import org.opendatakit.common.android.data.OrderedColumns;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.database.service.OdkDbInterface;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.WebViewUtil;
import org.opendatakit.tables.views.webkits.Control;
import org.robolectric.RobolectricTestRunner;

import android.content.ContentValues;
import android.os.RemoteException;

/**
 *
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class WebViewUtilsTest {

  @Test
  public void getContentValuesInvalidIntFails() throws RemoteException {
    this.assertInvalidHelper(ElementType.parseElementType("integer", false), "invalid");
  }

  @Test
  public void getContentValuesInvalidNumberFails() throws RemoteException {
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
   * @throws RemoteException 
   */
  private void assertInvalidHelper(
      ElementType columnType,
      String invalidValue) throws RemoteException {
    
    CommonApplication.setMocked();
    TestCaseUtils.setExternalStorageMounted();

    String tableId = "table1";
    String elementKey = "anyKey";
    List<Column> columns = new ArrayList<Column>();
    columns.add(new Column(elementKey,elementKey,columnType.toString(), "[]"));
    OrderedColumns orderedDefns = new OrderedColumns(TestConstants.TABLES_DEFAULT_APP_NAME, tableId, columns);
    
    OdkDbInterface stubIf = mock(OdkDbInterface.class);

    try {
      OdkDbHandle hNoTransaction = new OdkDbHandle("noTrans");
      OdkDbHandle hTransaction = new OdkDbHandle("trans");
      doReturn(hTransaction).when(stubIf).openDatabase(any(String.class), eq(true));
      doReturn(hNoTransaction).when(stubIf).openDatabase(any(String.class), eq(false));
      
      ArrayList<String> tableIds = new ArrayList<String>();
      tableIds.add(tableId);
      doReturn(tableIds).when(stubIf).getAllTableIds(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class));
      
      
      doReturn(orderedDefns).when(stubIf).getUserDefinedColumns(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId));

    } catch (RemoteException e) {
      // ignore?
    }
    
    Tables.setMockDatabase(stubIf);
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
