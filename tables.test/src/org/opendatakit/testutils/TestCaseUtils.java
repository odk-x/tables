package org.opendatakit.testutils;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.opendatakit.aggregate.odktables.rest.ElementDataType;
import org.opendatakit.aggregate.odktables.rest.SyncState;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.common.android.data.OrderedColumns;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.data.UserTable.Row;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.database.service.OdkDbInterface;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.SurveyUtil.SurveyFormParameters;
import org.opendatakit.tables.utils.TableUtil;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowEnvironment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Environment;
import android.os.RemoteException;

/**
 * Various methods that are useful for testing with roboelectric.
 * @author sudar.sam@gmail.com
 *
 */
public class TestCaseUtils {

  /**
   * Get an intent with the app name set to {@link TestConstants#TABLES_DEFAULT_APP_NAME}.
   * @return
   */
  public static Intent getIntentWithAppNameTables() {
    Intent result = new Intent();
    result.putExtra(
        Constants.IntentKeys.APP_NAME,
        TestConstants.TABLES_DEFAULT_APP_NAME);
    return result;
  }

  public static void setNoTableDataset() {
    OdkDbInterface stubIf = mock(OdkDbInterface.class);

    try {
      ArrayList<String> adminColumnsList = new ArrayList<String>();
      adminColumnsList.add(DataTableColumns.ID);
      adminColumnsList.add(DataTableColumns.ROW_ETAG);
      adminColumnsList.add(DataTableColumns.SYNC_STATE); // not exportable
      adminColumnsList.add(DataTableColumns.CONFLICT_TYPE); // not exportable
      adminColumnsList.add(DataTableColumns.FILTER_TYPE);
      adminColumnsList.add(DataTableColumns.FILTER_VALUE);
      adminColumnsList.add(DataTableColumns.FORM_ID);
      adminColumnsList.add(DataTableColumns.LOCALE);
      adminColumnsList.add(DataTableColumns.SAVEPOINT_TYPE);
      adminColumnsList.add(DataTableColumns.SAVEPOINT_TIMESTAMP);
      adminColumnsList.add(DataTableColumns.SAVEPOINT_CREATOR);
      Collections.sort(adminColumnsList);
      String[] adminColumns = new String[adminColumnsList.size()];
      adminColumnsList.toArray(adminColumns);

      OdkDbHandle hNoTransaction = new OdkDbHandle("noTrans");
      OdkDbHandle hTransaction = new OdkDbHandle("trans");
      doReturn(hTransaction).when(stubIf).openDatabase(any(String.class), eq(true));
      doReturn(hNoTransaction).when(stubIf).openDatabase(any(String.class), eq(false));
      
      List<String> tableIds = new ArrayList<String>();
      doReturn(tableIds).when(stubIf).getAllTableIds(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class));
      
      String[] empty = {};
      int userColumns;
      
      TableUtil util = mock(TableUtil.class);
      TableUtil.set(util);

    } catch (RemoteException e) {
      // ignore?
    }
    Tables.setMockDatabase(stubIf);
  }

  public static void setOneTableDataset() {

    OdkDbInterface stubIf = mock(OdkDbInterface.class);
    try {
      
      OdkDbHandle hNoTransaction = new OdkDbHandle("noTrans");
      OdkDbHandle hTransaction = new OdkDbHandle("trans");
      doReturn(hTransaction).when(stubIf).openDatabase(any(String.class), eq(true));
      doReturn(hNoTransaction).when(stubIf).openDatabase(any(String.class), eq(false));
      
      List<String> tableIds = new ArrayList<String>();
      String tableId1 = TestConstants.DEFAULT_EMPTY_TABLE_ID;
      tableIds.add(tableId1);
      doReturn(tableIds).when(stubIf).getAllTableIds(any(String.class), any(OdkDbHandle.class));
      
      List<Column> columns1 = new ArrayList<Column>();
      columns1.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
          ElementDataType.string.name(), "[]"));
      columns1.add(new Column(TestConstants.ElementKeys.INT_COLUMN, TestConstants.ElementKeys.INT_COLUMN,
          ElementDataType.integer.name(), "[]"));
      columns1.add(new Column(TestConstants.ElementKeys.NUMBER_COLUMN, TestConstants.ElementKeys.NUMBER_COLUMN,
          ElementDataType.number.name(), "[]"));
  
      OrderedColumns orderedColumns1 = new OrderedColumns(TestConstants.TABLES_DEFAULT_APP_NAME, tableId1, columns1);
  
      doReturn(orderedColumns1).when(stubIf).getUserDefinedColumns(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId1));
      
      TableUtil util = mock(TableUtil.class);
      when(util.getLocalizedDisplayName(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId1)))
        .thenReturn(tableId1+"_name");
      when(util.getDefaultViewType(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId1)))
        .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
      TableUtil.set(util);
    } catch (RemoteException e) {
      // ignore?
    }

    Tables.setMockDatabase(stubIf);
  }

  public static void setTwoTableDataset() {

    OdkDbInterface stubIf = mock(OdkDbInterface.class);

    try {
      ArrayList<String> adminColumnsList = new ArrayList<String>();
      adminColumnsList.add(DataTableColumns.ID);
      adminColumnsList.add(DataTableColumns.ROW_ETAG);
      adminColumnsList.add(DataTableColumns.SYNC_STATE); // not exportable
      adminColumnsList.add(DataTableColumns.CONFLICT_TYPE); // not exportable
      adminColumnsList.add(DataTableColumns.FILTER_TYPE);
      adminColumnsList.add(DataTableColumns.FILTER_VALUE);
      adminColumnsList.add(DataTableColumns.FORM_ID);
      adminColumnsList.add(DataTableColumns.LOCALE);
      adminColumnsList.add(DataTableColumns.SAVEPOINT_TYPE);
      adminColumnsList.add(DataTableColumns.SAVEPOINT_TIMESTAMP);
      adminColumnsList.add(DataTableColumns.SAVEPOINT_CREATOR);
      Collections.sort(adminColumnsList);
      String[] adminColumns = new String[adminColumnsList.size()];
      adminColumnsList.toArray(adminColumns);

      OdkDbHandle hNoTransaction = new OdkDbHandle("noTrans");
      OdkDbHandle hTransaction = new OdkDbHandle("trans");
      doReturn(hTransaction).when(stubIf).openDatabase(any(String.class), eq(true));
      doReturn(hNoTransaction).when(stubIf).openDatabase(any(String.class), eq(false));
      
      List<String> tableIds = new ArrayList<String>();
      String tableId1 = TestConstants.DEFAULT_EMPTY_TABLE_ID;
      String tableId2 = TestConstants.DEFAULT_EMPTY_GEOTABLE_ID;
      tableIds.add(tableId1);
      tableIds.add(tableId2);
      doReturn(tableIds).when(stubIf).getAllTableIds(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class));
      
      String[] empty = {};
      int userColumns;
      
      List<Column> columns1 = new ArrayList<Column>();
      columns1.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
          ElementDataType.string.name(), "[]"));
      columns1.add(new Column(TestConstants.ElementKeys.INT_COLUMN, TestConstants.ElementKeys.INT_COLUMN,
          ElementDataType.integer.name(), "[]"));
      columns1.add(new Column(TestConstants.ElementKeys.NUMBER_COLUMN, TestConstants.ElementKeys.NUMBER_COLUMN,
          ElementDataType.number.name(), "[]"));

      OrderedColumns orderedColumns1 = new OrderedColumns(TestConstants.TABLES_DEFAULT_APP_NAME, tableId1, columns1);

      userColumns = columns1.size();
      HashMap<String, Integer> elementKeyToIndex1 = new HashMap<String, Integer>();
      String[] elementKeyForIndex1 = new String[userColumns + adminColumns.length];
      elementKeyToIndex1.put(TestConstants.ElementKeys.STRING_COLUMN,0);
      elementKeyForIndex1[0] = TestConstants.ElementKeys.STRING_COLUMN;
      elementKeyToIndex1.put(TestConstants.ElementKeys.INT_COLUMN,1);
      elementKeyForIndex1[1] = TestConstants.ElementKeys.INT_COLUMN;
      elementKeyToIndex1.put(TestConstants.ElementKeys.NUMBER_COLUMN,2);
      elementKeyForIndex1[2] = TestConstants.ElementKeys.NUMBER_COLUMN;

      for (int j = 0; j < adminColumns.length; j++) {
        elementKeyToIndex1.put(adminColumns[j],userColumns+j);
        elementKeyForIndex1[userColumns+j] = adminColumns[j];
      }

      UserTable tbl1 = new UserTable(orderedColumns1,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex1, elementKeyForIndex1, 0);

      List<Column> columns2 = new ArrayList<Column>();
      columns2.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
          ElementDataType.string.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.GEOPOINT_COLUMN,TestConstants.ElementKeys.GEOPOINT_COLUMN,
          ElementDataType.object.name(), "[\"" +
              TestConstants.ElementKeys.LATITUDE_COLUMN + "\",\"" +
              TestConstants.ElementKeys.LONGITUDE_COLUMN + "\",\"" +
              TestConstants.ElementKeys.ALTITUDE_COLUMN + "\",\"" + 
              TestConstants.ElementKeys.ACCURACY_COLUMN + "\"]"));
      columns2.add(new Column(TestConstants.ElementKeys.LATITUDE_COLUMN, "latitude",
          ElementDataType.number.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.LONGITUDE_COLUMN, "longitude",
          ElementDataType.number.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.ALTITUDE_COLUMN, "altitude",
          ElementDataType.number.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.ACCURACY_COLUMN, "accuracy",
          ElementDataType.number.name(), "[]"));
  
      OrderedColumns orderedColumns2 = new OrderedColumns(TestConstants.TABLES_DEFAULT_APP_NAME, tableId2, columns2);

      userColumns = columns2.size()-1;
      HashMap<String, Integer> elementKeyToIndex2 = new HashMap<String, Integer>();
      String[] elementKeyForIndex2 = new String[userColumns + adminColumns.length];
      elementKeyToIndex2.put(TestConstants.ElementKeys.STRING_COLUMN,0);
      elementKeyForIndex2[0] = TestConstants.ElementKeys.STRING_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.LATITUDE_COLUMN,1);
      elementKeyForIndex2[1] = TestConstants.ElementKeys.LATITUDE_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.LONGITUDE_COLUMN,2);
      elementKeyForIndex2[2] = TestConstants.ElementKeys.LONGITUDE_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.ALTITUDE_COLUMN,3);
      elementKeyForIndex2[3] = TestConstants.ElementKeys.ALTITUDE_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.ACCURACY_COLUMN,4);
      elementKeyForIndex2[4] = TestConstants.ElementKeys.ACCURACY_COLUMN;

      for (int j = 0; j < adminColumns.length; j++) {
        elementKeyToIndex2.put(adminColumns[j],userColumns+j);
        elementKeyForIndex2[userColumns+j] = adminColumns[j];
      }

      UserTable tbl2 = new UserTable(orderedColumns2,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex2, elementKeyForIndex2, 0);
  

      doReturn(orderedColumns1).when(stubIf).getUserDefinedColumns(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId1));
      doReturn(orderedColumns2).when(stubIf).getUserDefinedColumns(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId2));
      
      TableUtil util = mock(TableUtil.class);
      when(util.getLocalizedDisplayName(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId1)))
        .thenReturn(tableId1+"_name");
      when(util.getLocalizedDisplayName(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId2)))
        .thenReturn(tableId2+"_name");
      when(util.getDefaultViewType(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId1)))
        .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
      when(util.getDefaultViewType(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId2)))
        .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
      TableUtil.set(util);

      doReturn(tbl1).when(stubIf).rawSqlQuery(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
            eq(tableId1), any(OrderedColumns.class), any(String.class), eq(empty), eq(empty), any(String.class), any(String.class), any(String.class));
      doReturn(tbl2).when(stubIf).rawSqlQuery(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
          eq(tableId2), any(OrderedColumns.class), any(String.class), eq(empty), eq(empty), any(String.class), any(String.class), any(String.class));
    } catch (RemoteException e) {
      // ignore?
    }
    Tables.setMockDatabase(stubIf);
  }


  public static void setThreeTableDataset(boolean mockData) {

    OdkDbInterface stubIf = mock(OdkDbInterface.class);

    try {
      ArrayList<String> adminColumnsList = new ArrayList<String>();
      adminColumnsList.add(DataTableColumns.ID);
      adminColumnsList.add(DataTableColumns.ROW_ETAG);
      adminColumnsList.add(DataTableColumns.SYNC_STATE); // not exportable
      adminColumnsList.add(DataTableColumns.CONFLICT_TYPE); // not exportable
      adminColumnsList.add(DataTableColumns.FILTER_TYPE);
      adminColumnsList.add(DataTableColumns.FILTER_VALUE);
      adminColumnsList.add(DataTableColumns.FORM_ID);
      adminColumnsList.add(DataTableColumns.LOCALE);
      adminColumnsList.add(DataTableColumns.SAVEPOINT_TYPE);
      adminColumnsList.add(DataTableColumns.SAVEPOINT_TIMESTAMP);
      adminColumnsList.add(DataTableColumns.SAVEPOINT_CREATOR);
      Collections.sort(adminColumnsList);
      String[] adminColumns = new String[adminColumnsList.size()];
      adminColumnsList.toArray(adminColumns);

      OdkDbHandle hNoTransaction = new OdkDbHandle("noTrans");
      OdkDbHandle hTransaction = new OdkDbHandle("trans");
      doReturn(hTransaction).when(stubIf).openDatabase(any(String.class), eq(true));
      doReturn(hNoTransaction).when(stubIf).openDatabase(any(String.class), eq(false));
      
      List<String> tableIds = new ArrayList<String>();
      String tableId1 = TestConstants.DEFAULT_EMPTY_TABLE_ID;
      String tableId2 = TestConstants.DEFAULT_EMPTY_GEOTABLE_ID;
      String tableId3 = TestConstants.DEFAULT_TABLE_ID;
      tableIds.add(tableId1);
      tableIds.add(tableId2);
      tableIds.add(tableId3);
      doReturn(tableIds).when(stubIf).getAllTableIds(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class));
      
      String[] empty = {};
      int userColumns;
      
      List<Column> columns1 = new ArrayList<Column>();
      columns1.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
          ElementDataType.string.name(), "[]"));
      columns1.add(new Column(TestConstants.ElementKeys.INT_COLUMN, TestConstants.ElementKeys.INT_COLUMN,
          ElementDataType.integer.name(), "[]"));
      columns1.add(new Column(TestConstants.ElementKeys.NUMBER_COLUMN, TestConstants.ElementKeys.NUMBER_COLUMN,
          ElementDataType.number.name(), "[]"));

      OrderedColumns orderedColumns1 = new OrderedColumns(TestConstants.TABLES_DEFAULT_APP_NAME, tableId1, columns1);

      userColumns = columns1.size();
      HashMap<String, Integer> elementKeyToIndex1 = new HashMap<String, Integer>();
      String[] elementKeyForIndex1 = new String[userColumns + adminColumns.length];
      elementKeyToIndex1.put(TestConstants.ElementKeys.STRING_COLUMN,0);
      elementKeyForIndex1[0] = TestConstants.ElementKeys.STRING_COLUMN;
      elementKeyToIndex1.put(TestConstants.ElementKeys.INT_COLUMN,1);
      elementKeyForIndex1[1] = TestConstants.ElementKeys.INT_COLUMN;
      elementKeyToIndex1.put(TestConstants.ElementKeys.NUMBER_COLUMN,2);
      elementKeyForIndex1[2] = TestConstants.ElementKeys.NUMBER_COLUMN;

      for (int j = 0; j < adminColumns.length; j++) {
        elementKeyToIndex1.put(adminColumns[j],userColumns+j);
        elementKeyForIndex1[userColumns+j] = adminColumns[j];
      }

      UserTable tbl1 = new UserTable(orderedColumns1,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex1, elementKeyForIndex1, 0);

      List<Column> columns2 = new ArrayList<Column>();
      columns2.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
          ElementDataType.string.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.GEOPOINT_COLUMN,TestConstants.ElementKeys.GEOPOINT_COLUMN,
          ElementDataType.object.name(), "[\"" +
              TestConstants.ElementKeys.LATITUDE_COLUMN + "\",\"" +
              TestConstants.ElementKeys.LONGITUDE_COLUMN + "\",\"" +
              TestConstants.ElementKeys.ALTITUDE_COLUMN + "\",\"" + 
              TestConstants.ElementKeys.ACCURACY_COLUMN + "\"]"));
      columns2.add(new Column(TestConstants.ElementKeys.LATITUDE_COLUMN, "latitude",
          ElementDataType.number.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.LONGITUDE_COLUMN, "longitude",
          ElementDataType.number.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.ALTITUDE_COLUMN, "altitude",
          ElementDataType.number.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.ACCURACY_COLUMN, "accuracy",
          ElementDataType.number.name(), "[]"));
  
      OrderedColumns orderedColumns2 = new OrderedColumns(TestConstants.TABLES_DEFAULT_APP_NAME, tableId2, columns2);

      userColumns = columns2.size()-1;
      HashMap<String, Integer> elementKeyToIndex2 = new HashMap<String, Integer>();
      String[] elementKeyForIndex2 = new String[userColumns + adminColumns.length];
      elementKeyToIndex2.put(TestConstants.ElementKeys.STRING_COLUMN,0);
      elementKeyForIndex2[0] = TestConstants.ElementKeys.STRING_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.LATITUDE_COLUMN,1);
      elementKeyForIndex2[1] = TestConstants.ElementKeys.LATITUDE_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.LONGITUDE_COLUMN,2);
      elementKeyForIndex2[2] = TestConstants.ElementKeys.LONGITUDE_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.ALTITUDE_COLUMN,3);
      elementKeyForIndex2[3] = TestConstants.ElementKeys.ALTITUDE_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.ACCURACY_COLUMN,4);
      elementKeyForIndex2[4] = TestConstants.ElementKeys.ACCURACY_COLUMN;

      for (int j = 0; j < adminColumns.length; j++) {
        elementKeyToIndex2.put(adminColumns[j],userColumns+j);
        elementKeyForIndex2[userColumns+j] = adminColumns[j];
      }

      UserTable tbl2 = new UserTable(orderedColumns2,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex2, elementKeyForIndex2, 0);
  
      List<Column> columns3 = new ArrayList<Column>();
      columns3.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
          ElementDataType.string.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.INT_COLUMN, TestConstants.ElementKeys.INT_COLUMN,
          ElementDataType.integer.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.NUMBER_COLUMN, TestConstants.ElementKeys.NUMBER_COLUMN,
          ElementDataType.number.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.GEOPOINT_COLUMN,TestConstants.ElementKeys.GEOPOINT_COLUMN,
          ElementDataType.object.name(), "[\"" +
              TestConstants.ElementKeys.LATITUDE_COLUMN + "\",\"" +
              TestConstants.ElementKeys.LONGITUDE_COLUMN + "\",\"" +
              TestConstants.ElementKeys.ALTITUDE_COLUMN + "\",\"" + 
              TestConstants.ElementKeys.ACCURACY_COLUMN + "\"]"));
      columns3.add(new Column(TestConstants.ElementKeys.LATITUDE_COLUMN, "latitude",
          ElementDataType.number.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.LONGITUDE_COLUMN, "longitude",
          ElementDataType.number.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.ALTITUDE_COLUMN, "altitude",
          ElementDataType.number.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.ACCURACY_COLUMN, "accuracy",
          ElementDataType.number.name(), "[]"));
      
      OrderedColumns orderedColumns3 = new OrderedColumns(TestConstants.TABLES_DEFAULT_APP_NAME, tableId3, columns3);

      userColumns = columns3.size()-1;
      HashMap<String, Integer> elementKeyToIndex3 = new HashMap<String, Integer>();
      String[] elementKeyForIndex3 = new String[userColumns + adminColumns.length];
      elementKeyToIndex3.put(TestConstants.ElementKeys.STRING_COLUMN,0);
      elementKeyForIndex3[0] = TestConstants.ElementKeys.STRING_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.INT_COLUMN,1);
      elementKeyForIndex3[1] = TestConstants.ElementKeys.INT_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.NUMBER_COLUMN,2);
      elementKeyForIndex3[2] = TestConstants.ElementKeys.NUMBER_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.LATITUDE_COLUMN,3);
      elementKeyForIndex3[3] = TestConstants.ElementKeys.LATITUDE_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.LONGITUDE_COLUMN,4);
      elementKeyForIndex3[4] = TestConstants.ElementKeys.LONGITUDE_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.ALTITUDE_COLUMN,5);
      elementKeyForIndex3[5] = TestConstants.ElementKeys.ALTITUDE_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.ACCURACY_COLUMN,6);
      elementKeyForIndex3[6] = TestConstants.ElementKeys.ACCURACY_COLUMN;

      for (int j = 0; j < adminColumns.length; j++) {
        elementKeyToIndex3.put(adminColumns[j],userColumns+j);
        elementKeyForIndex3[userColumns+j] = adminColumns[j];
      }

      UserTable tbl3 = new UserTable(orderedColumns3,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex3, elementKeyForIndex3, (mockData ? 3 : 0));
      
      UserTable singleRow1Tbl3 = new UserTable(orderedColumns3,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex3, elementKeyForIndex3, 1);
      UserTable singleRow2Tbl3 = new UserTable(orderedColumns3,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex3, elementKeyForIndex3, 1);
      UserTable singleRow3Tbl3 = new UserTable(orderedColumns3,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex3, elementKeyForIndex3, 1);
      
      if ( mockData ) {
        String rowId1for3 = TestConstants.ROWID_1;
        String[] rowData1for3 = new String[userColumns + adminColumns.length];
        rowData1for3[0] = "row1 - HUB Construction";
        rowData1for3[1] = "11";
        rowData1for3[2] = "99.11";
        rowData1for3[3] = "47.65434975";
        rowData1for3[4] = "-122.30498975";
        rowData1for3[5] = "21.299999237060547";
        rowData1for3[6] = "6.70820411";
        rowData1for3[userColumns+ 0] = rowId1for3; //DataTableColumns.ID
        rowData1for3[userColumns+ 1] = null; //DataTableColumns.ROW_ETAG
        rowData1for3[userColumns+ 2] = SyncState.new_row.name(); //DataTableColumns.SYNC_STATE
        rowData1for3[userColumns+ 3] = "0"; //DataTableColumns.CONFLICT_TYPE
        rowData1for3[userColumns+ 4] = null; //DataTableColumns.FILTER_TYPE
        rowData1for3[userColumns+ 5] = null; //DataTableColumns.FILTER_VALUE
        rowData1for3[userColumns+ 6] = "tablesId_geotagger"; //DataTableColumns.FORM_ID
        rowData1for3[userColumns+ 7] = "default"; //DataTableColumns.LOCALE
        rowData1for3[userColumns+ 8] = "COMPLETE"; //DataTableColumns.SAVEPOINT_TYPE
        rowData1for3[userColumns+ 9] = "2012-11-30T15:04:00.000000000"; //DataTableColumns.SAVEPOINT_TIMESTAMP
        rowData1for3[userColumns+10] = "anonymous1"; //DataTableColumns.SAVEPOINT_CREATOR
  
        Row row1 = new Row(tbl3, rowId1for3, rowData1for3 );
        Row singleRow1 = new Row(singleRow1Tbl3, rowId1for3, rowData1for3 );
        singleRow1Tbl3.addRow(singleRow1);
        doReturn(singleRow1Tbl3).when(stubIf).getDataInExistingDBTableWithId(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
            eq(TestConstants.DEFAULT_TABLE_ID), any(OrderedColumns.class), eq(rowId1for3));
  
        String rowId2for3 = TestConstants.ROWID_2;
        String[] rowData2for3 = new String[userColumns + adminColumns.length];
        rowData2for3[0] = "row2 - Kite Hill ... Gasworks";
        rowData2for3[1] = "22";
        rowData2for3[2] = "99.22";
        rowData2for3[3] = "47.64540379";
        rowData2for3[4] = "-122.33636588";
        rowData2for3[5] = "-7.199999809265137";
        rowData2for3[6] = "4.472136";
        rowData2for3[userColumns+ 0] = rowId2for3; //DataTableColumns.ID
        rowData2for3[userColumns+ 1] = "uuid:old222"; //DataTableColumns.ROW_ETAG
        rowData2for3[userColumns+ 2] = SyncState.changed.name(); //DataTableColumns.SYNC_STATE
        rowData2for3[userColumns+ 3] = "0"; //DataTableColumns.CONFLICT_TYPE
        rowData2for3[userColumns+ 4] = null; //DataTableColumns.FILTER_TYPE
        rowData2for3[userColumns+ 5] = null; //DataTableColumns.FILTER_VALUE
        rowData2for3[userColumns+ 6] = "tablesId_geotagger"; //DataTableColumns.FORM_ID
        rowData2for3[userColumns+ 7] = "default"; //DataTableColumns.LOCALE
        rowData2for3[userColumns+ 8] = "COMPLETE"; //DataTableColumns.SAVEPOINT_TYPE
        rowData2for3[userColumns+ 9] = "2014-05-05T22:25:14.688000000"; //DataTableColumns.SAVEPOINT_TIMESTAMP
        rowData2for3[userColumns+10] = "anonymous2"; //DataTableColumns.SAVEPOINT_CREATOR
  
        Row row2 = new Row(tbl3, rowId2for3, rowData2for3 );
        Row singleRow2 = new Row(singleRow2Tbl3, rowId2for3, rowData2for3 );
        singleRow2Tbl3.addRow(singleRow2);
        doReturn(singleRow2Tbl3).when(stubIf).getDataInExistingDBTableWithId(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
            eq(TestConstants.DEFAULT_TABLE_ID), any(OrderedColumns.class), eq(rowId2for3));
  
        String rowId3for3 = TestConstants.ROWID_3;
        String[] rowData3for3 = new String[userColumns + adminColumns.length];
        rowData3for3[0] = "row3 - The Ave";
        rowData3for3[1] = "33";
        rowData3for3[2] = "99.33";
        rowData3for3[3] = "47.65824883";
        rowData3for3[4] = "-122.31314593";
        rowData3for3[5] = "31.0";
        rowData3for3[6] = "5.0";
        rowData3for3[userColumns+ 0] = rowId3for3; //DataTableColumns.ID
        rowData3for3[userColumns+ 1] = null; //DataTableColumns.ROW_ETAG
        rowData3for3[userColumns+ 2] = SyncState.new_row.name(); //DataTableColumns.SYNC_STATE
        rowData3for3[userColumns+ 3] = "0"; //DataTableColumns.CONFLICT_TYPE
        rowData3for3[userColumns+ 4] = null; //DataTableColumns.FILTER_TYPE
        rowData3for3[userColumns+ 5] = null; //DataTableColumns.FILTER_VALUE
        rowData3for3[userColumns+ 6] = "tablesId_geotagger"; //DataTableColumns.FORM_ID
        rowData3for3[userColumns+ 7] = "default"; //DataTableColumns.LOCALE
        rowData3for3[userColumns+ 8] = "COMPLETE"; //DataTableColumns.SAVEPOINT_TYPE
        rowData3for3[userColumns+ 9] = "2012-11-30T15:06:00.000000000"; //DataTableColumns.SAVEPOINT_TIMESTAMP
        rowData3for3[userColumns+10] = "anonymous3"; //DataTableColumns.SAVEPOINT_CREATOR
  
        Row row3 = new Row(tbl3, rowId3for3, rowData3for3 );
        Row singleRow3 = new Row(singleRow3Tbl3, rowId3for3, rowData3for3 );
        singleRow3Tbl3.addRow(singleRow3);
        doReturn(singleRow3Tbl3).when(stubIf).getDataInExistingDBTableWithId(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
              eq(TestConstants.DEFAULT_TABLE_ID), any(OrderedColumns.class), eq(rowId3for3));
        
        tbl3.addRow(row1);
        tbl3.addRow(row2);
        tbl3.addRow(row3);
      }

      doReturn(orderedColumns1).when(stubIf).getUserDefinedColumns(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId1));
      doReturn(orderedColumns2).when(stubIf).getUserDefinedColumns(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId2));
      doReturn(orderedColumns3).when(stubIf).getUserDefinedColumns(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId3));
      
      TableUtil util = mock(TableUtil.class);
      when(util.getLocalizedDisplayName(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId1)))
        .thenReturn(tableId1+"_name");
      when(util.getLocalizedDisplayName(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId2)))
        .thenReturn(tableId2+"_name");
      when(util.getLocalizedDisplayName(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId3)))
        .thenReturn(tableId3+"_name");
      when(util.getDefaultViewType(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId1)))
        .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
      when(util.getDefaultViewType(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId2)))
        .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
      when(util.getDefaultViewType(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId3)))
        .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
      TableUtil.set(util);

      doReturn(tbl1).when(stubIf).rawSqlQuery(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
            eq(tableId1), any(OrderedColumns.class), any(String.class), eq(empty), eq(empty), any(String.class), any(String.class), any(String.class));
      doReturn(tbl2).when(stubIf).rawSqlQuery(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
          eq(tableId2), any(OrderedColumns.class), any(String.class), eq(empty), eq(empty), any(String.class), any(String.class), any(String.class));
      doReturn(tbl3).when(stubIf).rawSqlQuery(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
          eq(TestConstants.DEFAULT_TABLE_ID), any(OrderedColumns.class), any(String.class), eq(empty), eq(empty), any(String.class), any(String.class), any(String.class));
    } catch (RemoteException e) {
      // ignore?
    }
    Tables.setMockDatabase(stubIf);
  }

  public static void setThreeTableDatasetReviseRow1() {

    boolean mockData = true;
    OdkDbInterface stubIf = mock(OdkDbInterface.class);

    try {
      ArrayList<String> adminColumnsList = new ArrayList<String>();
      adminColumnsList.add(DataTableColumns.ID);
      adminColumnsList.add(DataTableColumns.ROW_ETAG);
      adminColumnsList.add(DataTableColumns.SYNC_STATE); // not exportable
      adminColumnsList.add(DataTableColumns.CONFLICT_TYPE); // not exportable
      adminColumnsList.add(DataTableColumns.FILTER_TYPE);
      adminColumnsList.add(DataTableColumns.FILTER_VALUE);
      adminColumnsList.add(DataTableColumns.FORM_ID);
      adminColumnsList.add(DataTableColumns.LOCALE);
      adminColumnsList.add(DataTableColumns.SAVEPOINT_TYPE);
      adminColumnsList.add(DataTableColumns.SAVEPOINT_TIMESTAMP);
      adminColumnsList.add(DataTableColumns.SAVEPOINT_CREATOR);
      Collections.sort(adminColumnsList);
      String[] adminColumns = new String[adminColumnsList.size()];
      adminColumnsList.toArray(adminColumns);

      OdkDbHandle hNoTransaction = new OdkDbHandle("noTrans");
      OdkDbHandle hTransaction = new OdkDbHandle("trans");
      doReturn(hTransaction).when(stubIf).openDatabase(any(String.class), eq(true));
      doReturn(hNoTransaction).when(stubIf).openDatabase(any(String.class), eq(false));
      
      List<String> tableIds = new ArrayList<String>();
      String tableId1 = TestConstants.DEFAULT_EMPTY_TABLE_ID;
      String tableId2 = TestConstants.DEFAULT_EMPTY_GEOTABLE_ID;
      String tableId3 = TestConstants.DEFAULT_TABLE_ID;
      tableIds.add(tableId1);
      tableIds.add(tableId2);
      tableIds.add(tableId3);
      doReturn(tableIds).when(stubIf).getAllTableIds(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class));
      
      String[] empty = {};
      int userColumns;
      
      List<Column> columns1 = new ArrayList<Column>();
      columns1.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
          ElementDataType.string.name(), "[]"));
      columns1.add(new Column(TestConstants.ElementKeys.INT_COLUMN, TestConstants.ElementKeys.INT_COLUMN,
          ElementDataType.integer.name(), "[]"));
      columns1.add(new Column(TestConstants.ElementKeys.NUMBER_COLUMN, TestConstants.ElementKeys.NUMBER_COLUMN,
          ElementDataType.number.name(), "[]"));

      OrderedColumns orderedColumns1 = new OrderedColumns(TestConstants.TABLES_DEFAULT_APP_NAME, tableId1, columns1);

      userColumns = columns1.size();
      HashMap<String, Integer> elementKeyToIndex1 = new HashMap<String, Integer>();
      String[] elementKeyForIndex1 = new String[userColumns + adminColumns.length];
      elementKeyToIndex1.put(TestConstants.ElementKeys.STRING_COLUMN,0);
      elementKeyForIndex1[0] = TestConstants.ElementKeys.STRING_COLUMN;
      elementKeyToIndex1.put(TestConstants.ElementKeys.INT_COLUMN,1);
      elementKeyForIndex1[1] = TestConstants.ElementKeys.INT_COLUMN;
      elementKeyToIndex1.put(TestConstants.ElementKeys.NUMBER_COLUMN,2);
      elementKeyForIndex1[2] = TestConstants.ElementKeys.NUMBER_COLUMN;

      for (int j = 0; j < adminColumns.length; j++) {
        elementKeyToIndex1.put(adminColumns[j],userColumns+j);
        elementKeyForIndex1[userColumns+j] = adminColumns[j];
      }

      UserTable tbl1 = new UserTable(orderedColumns1,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex1, elementKeyForIndex1, 0);

      List<Column> columns2 = new ArrayList<Column>();
      columns2.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
          ElementDataType.string.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.GEOPOINT_COLUMN,TestConstants.ElementKeys.GEOPOINT_COLUMN,
          ElementDataType.object.name(), "[\"" +
              TestConstants.ElementKeys.LATITUDE_COLUMN + "\",\"" +
              TestConstants.ElementKeys.LONGITUDE_COLUMN + "\",\"" +
              TestConstants.ElementKeys.ALTITUDE_COLUMN + "\",\"" + 
              TestConstants.ElementKeys.ACCURACY_COLUMN + "\"]"));
      columns2.add(new Column(TestConstants.ElementKeys.LATITUDE_COLUMN, "latitude",
          ElementDataType.number.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.LONGITUDE_COLUMN, "longitude",
          ElementDataType.number.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.ALTITUDE_COLUMN, "altitude",
          ElementDataType.number.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.ACCURACY_COLUMN, "accuracy",
          ElementDataType.number.name(), "[]"));
  
      OrderedColumns orderedColumns2 = new OrderedColumns(TestConstants.TABLES_DEFAULT_APP_NAME, tableId2, columns2);

      userColumns = columns2.size()-1;
      HashMap<String, Integer> elementKeyToIndex2 = new HashMap<String, Integer>();
      String[] elementKeyForIndex2 = new String[userColumns + adminColumns.length];
      elementKeyToIndex2.put(TestConstants.ElementKeys.STRING_COLUMN,0);
      elementKeyForIndex2[0] = TestConstants.ElementKeys.STRING_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.LATITUDE_COLUMN,1);
      elementKeyForIndex2[1] = TestConstants.ElementKeys.LATITUDE_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.LONGITUDE_COLUMN,2);
      elementKeyForIndex2[2] = TestConstants.ElementKeys.LONGITUDE_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.ALTITUDE_COLUMN,3);
      elementKeyForIndex2[3] = TestConstants.ElementKeys.ALTITUDE_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.ACCURACY_COLUMN,4);
      elementKeyForIndex2[4] = TestConstants.ElementKeys.ACCURACY_COLUMN;

      for (int j = 0; j < adminColumns.length; j++) {
        elementKeyToIndex2.put(adminColumns[j],userColumns+j);
        elementKeyForIndex2[userColumns+j] = adminColumns[j];
      }

      UserTable tbl2 = new UserTable(orderedColumns2,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex2, elementKeyForIndex2, 0);
  
      List<Column> columns3 = new ArrayList<Column>();
      columns3.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
          ElementDataType.string.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.INT_COLUMN, TestConstants.ElementKeys.INT_COLUMN,
          ElementDataType.integer.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.NUMBER_COLUMN, TestConstants.ElementKeys.NUMBER_COLUMN,
          ElementDataType.number.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.GEOPOINT_COLUMN,TestConstants.ElementKeys.GEOPOINT_COLUMN,
          ElementDataType.object.name(), "[\"" +
              TestConstants.ElementKeys.LATITUDE_COLUMN + "\",\"" +
              TestConstants.ElementKeys.LONGITUDE_COLUMN + "\",\"" +
              TestConstants.ElementKeys.ALTITUDE_COLUMN + "\",\"" + 
              TestConstants.ElementKeys.ACCURACY_COLUMN + "\"]"));
      columns3.add(new Column(TestConstants.ElementKeys.LATITUDE_COLUMN, "latitude",
          ElementDataType.number.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.LONGITUDE_COLUMN, "longitude",
          ElementDataType.number.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.ALTITUDE_COLUMN, "altitude",
          ElementDataType.number.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.ACCURACY_COLUMN, "accuracy",
          ElementDataType.number.name(), "[]"));
      
      OrderedColumns orderedColumns3 = new OrderedColumns(TestConstants.TABLES_DEFAULT_APP_NAME, tableId3, columns3);

      userColumns = columns3.size()-1;
      HashMap<String, Integer> elementKeyToIndex3 = new HashMap<String, Integer>();
      String[] elementKeyForIndex3 = new String[userColumns + adminColumns.length];
      elementKeyToIndex3.put(TestConstants.ElementKeys.STRING_COLUMN,0);
      elementKeyForIndex3[0] = TestConstants.ElementKeys.STRING_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.INT_COLUMN,1);
      elementKeyForIndex3[1] = TestConstants.ElementKeys.INT_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.NUMBER_COLUMN,2);
      elementKeyForIndex3[2] = TestConstants.ElementKeys.NUMBER_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.LATITUDE_COLUMN,3);
      elementKeyForIndex3[3] = TestConstants.ElementKeys.LATITUDE_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.LONGITUDE_COLUMN,4);
      elementKeyForIndex3[4] = TestConstants.ElementKeys.LONGITUDE_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.ALTITUDE_COLUMN,5);
      elementKeyForIndex3[5] = TestConstants.ElementKeys.ALTITUDE_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.ACCURACY_COLUMN,6);
      elementKeyForIndex3[6] = TestConstants.ElementKeys.ACCURACY_COLUMN;

      for (int j = 0; j < adminColumns.length; j++) {
        elementKeyToIndex3.put(adminColumns[j],userColumns+j);
        elementKeyForIndex3[userColumns+j] = adminColumns[j];
      }

      UserTable tbl3 = new UserTable(orderedColumns3,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex3, elementKeyForIndex3, (mockData ? 3 : 0));
      
      UserTable singleRow1Tbl3 = new UserTable(orderedColumns3,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex3, elementKeyForIndex3, 1);
      UserTable singleRow2Tbl3 = new UserTable(orderedColumns3,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex3, elementKeyForIndex3, 1);
      UserTable singleRow3Tbl3 = new UserTable(orderedColumns3,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex3, elementKeyForIndex3, 1);
      
      if ( mockData ) {
        String rowId1for3 = TestConstants.ROWID_1;
        String[] rowData1for3 = new String[userColumns + adminColumns.length];
        rowData1for3[0] = "row1 - HUB Construction edited";
        rowData1for3[1] = "311";
        rowData1for3[2] = "33.11";
        rowData1for3[3] = "47.65434975";
        rowData1for3[4] = "-122.30498975";
        rowData1for3[5] = "21.299999237060547";
        rowData1for3[6] = "6.70820411";
        rowData1for3[userColumns+ 0] = rowId1for3; //DataTableColumns.ID
        rowData1for3[userColumns+ 1] = null; //DataTableColumns.ROW_ETAG
        rowData1for3[userColumns+ 2] = SyncState.new_row.name(); //DataTableColumns.SYNC_STATE
        rowData1for3[userColumns+ 3] = "0"; //DataTableColumns.CONFLICT_TYPE
        rowData1for3[userColumns+ 4] = null; //DataTableColumns.FILTER_TYPE
        rowData1for3[userColumns+ 5] = null; //DataTableColumns.FILTER_VALUE
        rowData1for3[userColumns+ 6] = "tablesId_geotagger"; //DataTableColumns.FORM_ID
        rowData1for3[userColumns+ 7] = "default"; //DataTableColumns.LOCALE
        rowData1for3[userColumns+ 8] = "COMPLETE"; //DataTableColumns.SAVEPOINT_TYPE
        rowData1for3[userColumns+ 9] = "2014-11-30T15:04:00.000000000"; //DataTableColumns.SAVEPOINT_TIMESTAMP
        rowData1for3[userColumns+10] = "anonymous1"; //DataTableColumns.SAVEPOINT_CREATOR
  
        Row row1 = new Row(tbl3, rowId1for3, rowData1for3 );
        Row singleRow1 = new Row(singleRow1Tbl3, rowId1for3, rowData1for3 );
        singleRow1Tbl3.addRow(singleRow1);
        doReturn(singleRow1Tbl3).when(stubIf).getDataInExistingDBTableWithId(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
            eq(TestConstants.DEFAULT_TABLE_ID), any(OrderedColumns.class), eq(rowId1for3));
  
        String rowId2for3 = TestConstants.ROWID_2;
        String[] rowData2for3 = new String[userColumns + adminColumns.length];
        rowData2for3[0] = "row2 - Kite Hill ... Gasworks";
        rowData2for3[1] = "22";
        rowData2for3[2] = "99.22";
        rowData2for3[3] = "47.64540379";
        rowData2for3[4] = "-122.33636588";
        rowData2for3[5] = "-7.199999809265137";
        rowData2for3[6] = "4.472136";
        rowData2for3[userColumns+ 0] = rowId2for3; //DataTableColumns.ID
        rowData2for3[userColumns+ 1] = "uuid:old222"; //DataTableColumns.ROW_ETAG
        rowData2for3[userColumns+ 2] = SyncState.changed.name(); //DataTableColumns.SYNC_STATE
        rowData2for3[userColumns+ 3] = "0"; //DataTableColumns.CONFLICT_TYPE
        rowData2for3[userColumns+ 4] = null; //DataTableColumns.FILTER_TYPE
        rowData2for3[userColumns+ 5] = null; //DataTableColumns.FILTER_VALUE
        rowData2for3[userColumns+ 6] = "tablesId_geotagger"; //DataTableColumns.FORM_ID
        rowData2for3[userColumns+ 7] = "default"; //DataTableColumns.LOCALE
        rowData2for3[userColumns+ 8] = "COMPLETE"; //DataTableColumns.SAVEPOINT_TYPE
        rowData2for3[userColumns+ 9] = "2014-05-05T22:25:14.688000000"; //DataTableColumns.SAVEPOINT_TIMESTAMP
        rowData2for3[userColumns+10] = "anonymous2"; //DataTableColumns.SAVEPOINT_CREATOR
  
        Row row2 = new Row(tbl3, rowId2for3, rowData2for3 );
        Row singleRow2 = new Row(singleRow2Tbl3, rowId2for3, rowData2for3 );
        singleRow2Tbl3.addRow(singleRow2);
        doReturn(singleRow2Tbl3).when(stubIf).getDataInExistingDBTableWithId(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
            eq(TestConstants.DEFAULT_TABLE_ID), any(OrderedColumns.class), eq(rowId2for3));
  
        String rowId3for3 = TestConstants.ROWID_3;
        String[] rowData3for3 = new String[userColumns + adminColumns.length];
        rowData3for3[0] = "row3 - The Ave";
        rowData3for3[1] = "33";
        rowData3for3[2] = "99.33";
        rowData3for3[3] = "47.65824883";
        rowData3for3[4] = "-122.31314593";
        rowData3for3[5] = "31.0";
        rowData3for3[6] = "5.0";
        rowData3for3[userColumns+ 0] = rowId3for3; //DataTableColumns.ID
        rowData3for3[userColumns+ 1] = null; //DataTableColumns.ROW_ETAG
        rowData3for3[userColumns+ 2] = SyncState.new_row.name(); //DataTableColumns.SYNC_STATE
        rowData3for3[userColumns+ 3] = "0"; //DataTableColumns.CONFLICT_TYPE
        rowData3for3[userColumns+ 4] = null; //DataTableColumns.FILTER_TYPE
        rowData3for3[userColumns+ 5] = null; //DataTableColumns.FILTER_VALUE
        rowData3for3[userColumns+ 6] = "tablesId_geotagger"; //DataTableColumns.FORM_ID
        rowData3for3[userColumns+ 7] = "default"; //DataTableColumns.LOCALE
        rowData3for3[userColumns+ 8] = "COMPLETE"; //DataTableColumns.SAVEPOINT_TYPE
        rowData3for3[userColumns+ 9] = "2012-11-30T15:06:00.000000000"; //DataTableColumns.SAVEPOINT_TIMESTAMP
        rowData3for3[userColumns+10] = "anonymous3"; //DataTableColumns.SAVEPOINT_CREATOR
  
        Row row3 = new Row(tbl3, rowId3for3, rowData3for3 );
        Row singleRow3 = new Row(singleRow3Tbl3, rowId3for3, rowData3for3 );
        singleRow3Tbl3.addRow(singleRow3);
        doReturn(singleRow3Tbl3).when(stubIf).getDataInExistingDBTableWithId(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
              eq(TestConstants.DEFAULT_TABLE_ID), any(OrderedColumns.class), eq(rowId3for3));
        
        tbl3.addRow(row1);
        tbl3.addRow(row2);
        tbl3.addRow(row3);
      }

      doReturn(orderedColumns1).when(stubIf).getUserDefinedColumns(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId1));
      doReturn(orderedColumns2).when(stubIf).getUserDefinedColumns(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId2));
      doReturn(orderedColumns3).when(stubIf).getUserDefinedColumns(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId3));
      
      TableUtil util = mock(TableUtil.class);
      when(util.getLocalizedDisplayName(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId1)))
        .thenReturn(tableId1+"_name");
      when(util.getLocalizedDisplayName(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId2)))
        .thenReturn(tableId2+"_name");
      when(util.getLocalizedDisplayName(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId3)))
        .thenReturn(tableId3+"_name");
      when(util.getDefaultViewType(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId1)))
        .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
      when(util.getDefaultViewType(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId2)))
        .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
      when(util.getDefaultViewType(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId3)))
        .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
      TableUtil.set(util);

      doReturn(tbl1).when(stubIf).rawSqlQuery(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
            eq(tableId1), any(OrderedColumns.class), any(String.class), eq(empty), eq(empty), any(String.class), any(String.class), any(String.class));
      doReturn(tbl2).when(stubIf).rawSqlQuery(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
          eq(tableId2), any(OrderedColumns.class), any(String.class), eq(empty), eq(empty), any(String.class), any(String.class), any(String.class));
      doReturn(tbl3).when(stubIf).rawSqlQuery(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
          eq(TestConstants.DEFAULT_TABLE_ID), any(OrderedColumns.class), any(String.class), eq(empty), eq(empty), any(String.class), any(String.class), any(String.class));
    } catch (RemoteException e) {
      // ignore?
    }
    Tables.setMockDatabase(stubIf);
  }
  

  public static void setThreeTableDatasetReviseRow2() {

    boolean mockData = true;
    OdkDbInterface stubIf = mock(OdkDbInterface.class);

    try {
      ArrayList<String> adminColumnsList = new ArrayList<String>();
      adminColumnsList.add(DataTableColumns.ID);
      adminColumnsList.add(DataTableColumns.ROW_ETAG);
      adminColumnsList.add(DataTableColumns.SYNC_STATE); // not exportable
      adminColumnsList.add(DataTableColumns.CONFLICT_TYPE); // not exportable
      adminColumnsList.add(DataTableColumns.FILTER_TYPE);
      adminColumnsList.add(DataTableColumns.FILTER_VALUE);
      adminColumnsList.add(DataTableColumns.FORM_ID);
      adminColumnsList.add(DataTableColumns.LOCALE);
      adminColumnsList.add(DataTableColumns.SAVEPOINT_TYPE);
      adminColumnsList.add(DataTableColumns.SAVEPOINT_TIMESTAMP);
      adminColumnsList.add(DataTableColumns.SAVEPOINT_CREATOR);
      Collections.sort(adminColumnsList);
      String[] adminColumns = new String[adminColumnsList.size()];
      adminColumnsList.toArray(adminColumns);

      OdkDbHandle hNoTransaction = new OdkDbHandle("noTrans");
      OdkDbHandle hTransaction = new OdkDbHandle("trans");
      doReturn(hTransaction).when(stubIf).openDatabase(any(String.class), eq(true));
      doReturn(hNoTransaction).when(stubIf).openDatabase(any(String.class), eq(false));
      
      List<String> tableIds = new ArrayList<String>();
      String tableId1 = TestConstants.DEFAULT_EMPTY_TABLE_ID;
      String tableId2 = TestConstants.DEFAULT_EMPTY_GEOTABLE_ID;
      String tableId3 = TestConstants.DEFAULT_TABLE_ID;
      tableIds.add(tableId1);
      tableIds.add(tableId2);
      tableIds.add(tableId3);
      doReturn(tableIds).when(stubIf).getAllTableIds(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class));
      
      String[] empty = {};
      int userColumns;
      
      List<Column> columns1 = new ArrayList<Column>();
      columns1.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
          ElementDataType.string.name(), "[]"));
      columns1.add(new Column(TestConstants.ElementKeys.INT_COLUMN, TestConstants.ElementKeys.INT_COLUMN,
          ElementDataType.integer.name(), "[]"));
      columns1.add(new Column(TestConstants.ElementKeys.NUMBER_COLUMN, TestConstants.ElementKeys.NUMBER_COLUMN,
          ElementDataType.number.name(), "[]"));

      OrderedColumns orderedColumns1 = new OrderedColumns(TestConstants.TABLES_DEFAULT_APP_NAME, tableId1, columns1);

      userColumns = columns1.size();
      HashMap<String, Integer> elementKeyToIndex1 = new HashMap<String, Integer>();
      String[] elementKeyForIndex1 = new String[userColumns + adminColumns.length];
      elementKeyToIndex1.put(TestConstants.ElementKeys.STRING_COLUMN,0);
      elementKeyForIndex1[0] = TestConstants.ElementKeys.STRING_COLUMN;
      elementKeyToIndex1.put(TestConstants.ElementKeys.INT_COLUMN,1);
      elementKeyForIndex1[1] = TestConstants.ElementKeys.INT_COLUMN;
      elementKeyToIndex1.put(TestConstants.ElementKeys.NUMBER_COLUMN,2);
      elementKeyForIndex1[2] = TestConstants.ElementKeys.NUMBER_COLUMN;

      for (int j = 0; j < adminColumns.length; j++) {
        elementKeyToIndex1.put(adminColumns[j],userColumns+j);
        elementKeyForIndex1[userColumns+j] = adminColumns[j];
      }

      UserTable tbl1 = new UserTable(orderedColumns1,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex1, elementKeyForIndex1, 0);

      List<Column> columns2 = new ArrayList<Column>();
      columns2.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
          ElementDataType.string.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.GEOPOINT_COLUMN,TestConstants.ElementKeys.GEOPOINT_COLUMN,
          ElementDataType.object.name(), "[\"" +
              TestConstants.ElementKeys.LATITUDE_COLUMN + "\",\"" +
              TestConstants.ElementKeys.LONGITUDE_COLUMN + "\",\"" +
              TestConstants.ElementKeys.ALTITUDE_COLUMN + "\",\"" + 
              TestConstants.ElementKeys.ACCURACY_COLUMN + "\"]"));
      columns2.add(new Column(TestConstants.ElementKeys.LATITUDE_COLUMN, "latitude",
          ElementDataType.number.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.LONGITUDE_COLUMN, "longitude",
          ElementDataType.number.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.ALTITUDE_COLUMN, "altitude",
          ElementDataType.number.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.ACCURACY_COLUMN, "accuracy",
          ElementDataType.number.name(), "[]"));
  
      OrderedColumns orderedColumns2 = new OrderedColumns(TestConstants.TABLES_DEFAULT_APP_NAME, tableId2, columns2);

      userColumns = columns2.size()-1;
      HashMap<String, Integer> elementKeyToIndex2 = new HashMap<String, Integer>();
      String[] elementKeyForIndex2 = new String[userColumns + adminColumns.length];
      elementKeyToIndex2.put(TestConstants.ElementKeys.STRING_COLUMN,0);
      elementKeyForIndex2[0] = TestConstants.ElementKeys.STRING_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.LATITUDE_COLUMN,1);
      elementKeyForIndex2[1] = TestConstants.ElementKeys.LATITUDE_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.LONGITUDE_COLUMN,2);
      elementKeyForIndex2[2] = TestConstants.ElementKeys.LONGITUDE_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.ALTITUDE_COLUMN,3);
      elementKeyForIndex2[3] = TestConstants.ElementKeys.ALTITUDE_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.ACCURACY_COLUMN,4);
      elementKeyForIndex2[4] = TestConstants.ElementKeys.ACCURACY_COLUMN;

      for (int j = 0; j < adminColumns.length; j++) {
        elementKeyToIndex2.put(adminColumns[j],userColumns+j);
        elementKeyForIndex2[userColumns+j] = adminColumns[j];
      }

      UserTable tbl2 = new UserTable(orderedColumns2,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex2, elementKeyForIndex2, 0);
  
      List<Column> columns3 = new ArrayList<Column>();
      columns3.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
          ElementDataType.string.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.INT_COLUMN, TestConstants.ElementKeys.INT_COLUMN,
          ElementDataType.integer.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.NUMBER_COLUMN, TestConstants.ElementKeys.NUMBER_COLUMN,
          ElementDataType.number.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.GEOPOINT_COLUMN,TestConstants.ElementKeys.GEOPOINT_COLUMN,
          ElementDataType.object.name(), "[\"" +
              TestConstants.ElementKeys.LATITUDE_COLUMN + "\",\"" +
              TestConstants.ElementKeys.LONGITUDE_COLUMN + "\",\"" +
              TestConstants.ElementKeys.ALTITUDE_COLUMN + "\",\"" + 
              TestConstants.ElementKeys.ACCURACY_COLUMN + "\"]"));
      columns3.add(new Column(TestConstants.ElementKeys.LATITUDE_COLUMN, "latitude",
          ElementDataType.number.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.LONGITUDE_COLUMN, "longitude",
          ElementDataType.number.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.ALTITUDE_COLUMN, "altitude",
          ElementDataType.number.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.ACCURACY_COLUMN, "accuracy",
          ElementDataType.number.name(), "[]"));
      
      OrderedColumns orderedColumns3 = new OrderedColumns(TestConstants.TABLES_DEFAULT_APP_NAME, tableId3, columns3);

      userColumns = columns3.size()-1;
      HashMap<String, Integer> elementKeyToIndex3 = new HashMap<String, Integer>();
      String[] elementKeyForIndex3 = new String[userColumns + adminColumns.length];
      elementKeyToIndex3.put(TestConstants.ElementKeys.STRING_COLUMN,0);
      elementKeyForIndex3[0] = TestConstants.ElementKeys.STRING_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.INT_COLUMN,1);
      elementKeyForIndex3[1] = TestConstants.ElementKeys.INT_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.NUMBER_COLUMN,2);
      elementKeyForIndex3[2] = TestConstants.ElementKeys.NUMBER_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.LATITUDE_COLUMN,3);
      elementKeyForIndex3[3] = TestConstants.ElementKeys.LATITUDE_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.LONGITUDE_COLUMN,4);
      elementKeyForIndex3[4] = TestConstants.ElementKeys.LONGITUDE_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.ALTITUDE_COLUMN,5);
      elementKeyForIndex3[5] = TestConstants.ElementKeys.ALTITUDE_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.ACCURACY_COLUMN,6);
      elementKeyForIndex3[6] = TestConstants.ElementKeys.ACCURACY_COLUMN;

      for (int j = 0; j < adminColumns.length; j++) {
        elementKeyToIndex3.put(adminColumns[j],userColumns+j);
        elementKeyForIndex3[userColumns+j] = adminColumns[j];
      }

      UserTable tbl3 = new UserTable(orderedColumns3,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex3, elementKeyForIndex3, (mockData ? 3 : 0));
      
      UserTable singleRow1Tbl3 = new UserTable(orderedColumns3,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex3, elementKeyForIndex3, 1);
      UserTable singleRow2Tbl3 = new UserTable(orderedColumns3,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex3, elementKeyForIndex3, 1);
      UserTable singleRow3Tbl3 = new UserTable(orderedColumns3,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex3, elementKeyForIndex3, 1);
      
      if ( mockData ) {
        String rowId1for3 = TestConstants.ROWID_1;
        String[] rowData1for3 = new String[userColumns + adminColumns.length];
        rowData1for3[0] = "row1 - HUB Construction";
        rowData1for3[1] = "11";
        rowData1for3[2] = "99.11";
        rowData1for3[3] = "47.65434975";
        rowData1for3[4] = "-122.30498975";
        rowData1for3[5] = "21.299999237060547";
        rowData1for3[6] = "6.70820411";
        rowData1for3[userColumns+ 0] = rowId1for3; //DataTableColumns.ID
        rowData1for3[userColumns+ 1] = null; //DataTableColumns.ROW_ETAG
        rowData1for3[userColumns+ 2] = SyncState.new_row.name(); //DataTableColumns.SYNC_STATE
        rowData1for3[userColumns+ 3] = "0"; //DataTableColumns.CONFLICT_TYPE
        rowData1for3[userColumns+ 4] = null; //DataTableColumns.FILTER_TYPE
        rowData1for3[userColumns+ 5] = null; //DataTableColumns.FILTER_VALUE
        rowData1for3[userColumns+ 6] = "tablesId_geotagger"; //DataTableColumns.FORM_ID
        rowData1for3[userColumns+ 7] = "default"; //DataTableColumns.LOCALE
        rowData1for3[userColumns+ 8] = "COMPLETE"; //DataTableColumns.SAVEPOINT_TYPE
        rowData1for3[userColumns+ 9] = "2012-11-30T15:04:00.000000000"; //DataTableColumns.SAVEPOINT_TIMESTAMP
        rowData1for3[userColumns+10] = "anonymous1"; //DataTableColumns.SAVEPOINT_CREATOR
  
        Row row1 = new Row(tbl3, rowId1for3, rowData1for3 );
        Row singleRow1 = new Row(singleRow1Tbl3, rowId1for3, rowData1for3 );
        singleRow1Tbl3.addRow(singleRow1);
        doReturn(singleRow1Tbl3).when(stubIf).getDataInExistingDBTableWithId(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
            eq(TestConstants.DEFAULT_TABLE_ID), any(OrderedColumns.class), eq(rowId1for3));
  
        String rowId2for3 = TestConstants.ROWID_2;
        String[] rowData2for3 = new String[userColumns + adminColumns.length];
        rowData2for3[0] = "row2 - Kite Hill ... Gasworks - REVISED";
        rowData2for3[1] = "422";
        rowData2for3[2] = "44.22";
        rowData2for3[3] = "47.64540379";
        rowData2for3[4] = "-122.33636588";
        rowData2for3[5] = "-7.199999809265137";
        rowData2for3[6] = "4.472136";
        rowData2for3[userColumns+ 0] = rowId2for3; //DataTableColumns.ID
        rowData2for3[userColumns+ 1] = "uuid:old222"; //DataTableColumns.ROW_ETAG
        rowData2for3[userColumns+ 2] = SyncState.changed.name(); //DataTableColumns.SYNC_STATE
        rowData2for3[userColumns+ 3] = "0"; //DataTableColumns.CONFLICT_TYPE
        rowData2for3[userColumns+ 4] = null; //DataTableColumns.FILTER_TYPE
        rowData2for3[userColumns+ 5] = null; //DataTableColumns.FILTER_VALUE
        rowData2for3[userColumns+ 6] = "tablesId_geotagger"; //DataTableColumns.FORM_ID
        rowData2for3[userColumns+ 7] = "default"; //DataTableColumns.LOCALE
        rowData2for3[userColumns+ 8] = "COMPLETE"; //DataTableColumns.SAVEPOINT_TYPE
        rowData2for3[userColumns+ 9] = "2014-11-05T22:25:14.688000000"; //DataTableColumns.SAVEPOINT_TIMESTAMP
        rowData2for3[userColumns+10] = "anonymous2"; //DataTableColumns.SAVEPOINT_CREATOR
  
        Row row2 = new Row(tbl3, rowId2for3, rowData2for3 );
        Row singleRow2 = new Row(singleRow2Tbl3, rowId2for3, rowData2for3 );
        singleRow2Tbl3.addRow(singleRow2);
        doReturn(singleRow2Tbl3).when(stubIf).getDataInExistingDBTableWithId(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
            eq(TestConstants.DEFAULT_TABLE_ID), any(OrderedColumns.class), eq(rowId2for3));
  
        String rowId3for3 = TestConstants.ROWID_3;
        String[] rowData3for3 = new String[userColumns + adminColumns.length];
        rowData3for3[0] = "row3 - The Ave";
        rowData3for3[1] = "33";
        rowData3for3[2] = "99.33";
        rowData3for3[3] = "47.65824883";
        rowData3for3[4] = "-122.31314593";
        rowData3for3[5] = "31.0";
        rowData3for3[6] = "5.0";
        rowData3for3[userColumns+ 0] = rowId3for3; //DataTableColumns.ID
        rowData3for3[userColumns+ 1] = null; //DataTableColumns.ROW_ETAG
        rowData3for3[userColumns+ 2] = SyncState.new_row.name(); //DataTableColumns.SYNC_STATE
        rowData3for3[userColumns+ 3] = "0"; //DataTableColumns.CONFLICT_TYPE
        rowData3for3[userColumns+ 4] = null; //DataTableColumns.FILTER_TYPE
        rowData3for3[userColumns+ 5] = null; //DataTableColumns.FILTER_VALUE
        rowData3for3[userColumns+ 6] = "tablesId_geotagger"; //DataTableColumns.FORM_ID
        rowData3for3[userColumns+ 7] = "default"; //DataTableColumns.LOCALE
        rowData3for3[userColumns+ 8] = "COMPLETE"; //DataTableColumns.SAVEPOINT_TYPE
        rowData3for3[userColumns+ 9] = "2012-11-30T15:06:00.000000000"; //DataTableColumns.SAVEPOINT_TIMESTAMP
        rowData3for3[userColumns+10] = "anonymous3"; //DataTableColumns.SAVEPOINT_CREATOR
  
        Row row3 = new Row(tbl3, rowId3for3, rowData3for3 );
        Row singleRow3 = new Row(singleRow3Tbl3, rowId3for3, rowData3for3 );
        singleRow3Tbl3.addRow(singleRow3);
        doReturn(singleRow3Tbl3).when(stubIf).getDataInExistingDBTableWithId(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
              eq(TestConstants.DEFAULT_TABLE_ID), any(OrderedColumns.class), eq(rowId3for3));
        
        tbl3.addRow(row1);
        tbl3.addRow(row2);
        tbl3.addRow(row3);
      }

      doReturn(orderedColumns1).when(stubIf).getUserDefinedColumns(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId1));
      doReturn(orderedColumns2).when(stubIf).getUserDefinedColumns(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId2));
      doReturn(orderedColumns3).when(stubIf).getUserDefinedColumns(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId3));
      
      TableUtil util = mock(TableUtil.class);
      when(util.getLocalizedDisplayName(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId1)))
        .thenReturn(tableId1+"_name");
      when(util.getLocalizedDisplayName(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId2)))
        .thenReturn(tableId2+"_name");
      when(util.getLocalizedDisplayName(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId3)))
        .thenReturn(tableId3+"_name");
      when(util.getDefaultViewType(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId1)))
        .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
      when(util.getDefaultViewType(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId2)))
        .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
      when(util.getDefaultViewType(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId3)))
        .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
      TableUtil.set(util);

      doReturn(tbl1).when(stubIf).rawSqlQuery(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
            eq(tableId1), any(OrderedColumns.class), any(String.class), eq(empty), eq(empty), any(String.class), any(String.class), any(String.class));
      doReturn(tbl2).when(stubIf).rawSqlQuery(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
          eq(tableId2), any(OrderedColumns.class), any(String.class), eq(empty), eq(empty), any(String.class), any(String.class), any(String.class));
      doReturn(tbl3).when(stubIf).rawSqlQuery(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
          eq(TestConstants.DEFAULT_TABLE_ID), any(OrderedColumns.class), any(String.class), eq(empty), eq(empty), any(String.class), any(String.class), any(String.class));
    } catch (RemoteException e) {
      // ignore?
    }
    Tables.setMockDatabase(stubIf);
  }

  public static void setThreeTableDatasetAddRow4() {

    boolean mockData = true;
    OdkDbInterface stubIf = mock(OdkDbInterface.class);

    try {
      ArrayList<String> adminColumnsList = new ArrayList<String>();
      adminColumnsList.add(DataTableColumns.ID);
      adminColumnsList.add(DataTableColumns.ROW_ETAG);
      adminColumnsList.add(DataTableColumns.SYNC_STATE); // not exportable
      adminColumnsList.add(DataTableColumns.CONFLICT_TYPE); // not exportable
      adminColumnsList.add(DataTableColumns.FILTER_TYPE);
      adminColumnsList.add(DataTableColumns.FILTER_VALUE);
      adminColumnsList.add(DataTableColumns.FORM_ID);
      adminColumnsList.add(DataTableColumns.LOCALE);
      adminColumnsList.add(DataTableColumns.SAVEPOINT_TYPE);
      adminColumnsList.add(DataTableColumns.SAVEPOINT_TIMESTAMP);
      adminColumnsList.add(DataTableColumns.SAVEPOINT_CREATOR);
      Collections.sort(adminColumnsList);
      String[] adminColumns = new String[adminColumnsList.size()];
      adminColumnsList.toArray(adminColumns);

      OdkDbHandle hNoTransaction = new OdkDbHandle("noTrans");
      OdkDbHandle hTransaction = new OdkDbHandle("trans");
      doReturn(hTransaction).when(stubIf).openDatabase(any(String.class), eq(true));
      doReturn(hNoTransaction).when(stubIf).openDatabase(any(String.class), eq(false));
      
      List<String> tableIds = new ArrayList<String>();
      String tableId1 = TestConstants.DEFAULT_EMPTY_TABLE_ID;
      String tableId2 = TestConstants.DEFAULT_EMPTY_GEOTABLE_ID;
      String tableId3 = TestConstants.DEFAULT_TABLE_ID;
      tableIds.add(tableId1);
      tableIds.add(tableId2);
      tableIds.add(tableId3);
      doReturn(tableIds).when(stubIf).getAllTableIds(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class));
      
      String[] empty = {};
      int userColumns;
      
      List<Column> columns1 = new ArrayList<Column>();
      columns1.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
          ElementDataType.string.name(), "[]"));
      columns1.add(new Column(TestConstants.ElementKeys.INT_COLUMN, TestConstants.ElementKeys.INT_COLUMN,
          ElementDataType.integer.name(), "[]"));
      columns1.add(new Column(TestConstants.ElementKeys.NUMBER_COLUMN, TestConstants.ElementKeys.NUMBER_COLUMN,
          ElementDataType.number.name(), "[]"));

      OrderedColumns orderedColumns1 = new OrderedColumns(TestConstants.TABLES_DEFAULT_APP_NAME, tableId1, columns1);

      userColumns = columns1.size();
      HashMap<String, Integer> elementKeyToIndex1 = new HashMap<String, Integer>();
      String[] elementKeyForIndex1 = new String[userColumns + adminColumns.length];
      elementKeyToIndex1.put(TestConstants.ElementKeys.STRING_COLUMN,0);
      elementKeyForIndex1[0] = TestConstants.ElementKeys.STRING_COLUMN;
      elementKeyToIndex1.put(TestConstants.ElementKeys.INT_COLUMN,1);
      elementKeyForIndex1[1] = TestConstants.ElementKeys.INT_COLUMN;
      elementKeyToIndex1.put(TestConstants.ElementKeys.NUMBER_COLUMN,2);
      elementKeyForIndex1[2] = TestConstants.ElementKeys.NUMBER_COLUMN;

      for (int j = 0; j < adminColumns.length; j++) {
        elementKeyToIndex1.put(adminColumns[j],userColumns+j);
        elementKeyForIndex1[userColumns+j] = adminColumns[j];
      }

      UserTable tbl1 = new UserTable(orderedColumns1,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex1, elementKeyForIndex1, 0);

      List<Column> columns2 = new ArrayList<Column>();
      columns2.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
          ElementDataType.string.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.GEOPOINT_COLUMN,TestConstants.ElementKeys.GEOPOINT_COLUMN,
          ElementDataType.object.name(), "[\"" +
              TestConstants.ElementKeys.LATITUDE_COLUMN + "\",\"" +
              TestConstants.ElementKeys.LONGITUDE_COLUMN + "\",\"" +
              TestConstants.ElementKeys.ALTITUDE_COLUMN + "\",\"" + 
              TestConstants.ElementKeys.ACCURACY_COLUMN + "\"]"));
      columns2.add(new Column(TestConstants.ElementKeys.LATITUDE_COLUMN, "latitude",
          ElementDataType.number.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.LONGITUDE_COLUMN, "longitude",
          ElementDataType.number.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.ALTITUDE_COLUMN, "altitude",
          ElementDataType.number.name(), "[]"));
      columns2.add(new Column(TestConstants.ElementKeys.ACCURACY_COLUMN, "accuracy",
          ElementDataType.number.name(), "[]"));
  
      OrderedColumns orderedColumns2 = new OrderedColumns(TestConstants.TABLES_DEFAULT_APP_NAME, tableId2, columns2);

      userColumns = columns2.size()-1;
      HashMap<String, Integer> elementKeyToIndex2 = new HashMap<String, Integer>();
      String[] elementKeyForIndex2 = new String[userColumns + adminColumns.length];
      elementKeyToIndex2.put(TestConstants.ElementKeys.STRING_COLUMN,0);
      elementKeyForIndex2[0] = TestConstants.ElementKeys.STRING_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.LATITUDE_COLUMN,1);
      elementKeyForIndex2[1] = TestConstants.ElementKeys.LATITUDE_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.LONGITUDE_COLUMN,2);
      elementKeyForIndex2[2] = TestConstants.ElementKeys.LONGITUDE_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.ALTITUDE_COLUMN,3);
      elementKeyForIndex2[3] = TestConstants.ElementKeys.ALTITUDE_COLUMN;
      elementKeyToIndex2.put(TestConstants.ElementKeys.ACCURACY_COLUMN,4);
      elementKeyForIndex2[4] = TestConstants.ElementKeys.ACCURACY_COLUMN;

      for (int j = 0; j < adminColumns.length; j++) {
        elementKeyToIndex2.put(adminColumns[j],userColumns+j);
        elementKeyForIndex2[userColumns+j] = adminColumns[j];
      }

      UserTable tbl2 = new UserTable(orderedColumns2,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex2, elementKeyForIndex2, 0);
  
      List<Column> columns3 = new ArrayList<Column>();
      columns3.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
          ElementDataType.string.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.INT_COLUMN, TestConstants.ElementKeys.INT_COLUMN,
          ElementDataType.integer.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.NUMBER_COLUMN, TestConstants.ElementKeys.NUMBER_COLUMN,
          ElementDataType.number.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.GEOPOINT_COLUMN,TestConstants.ElementKeys.GEOPOINT_COLUMN,
          ElementDataType.object.name(), "[\"" +
              TestConstants.ElementKeys.LATITUDE_COLUMN + "\",\"" +
              TestConstants.ElementKeys.LONGITUDE_COLUMN + "\",\"" +
              TestConstants.ElementKeys.ALTITUDE_COLUMN + "\",\"" + 
              TestConstants.ElementKeys.ACCURACY_COLUMN + "\"]"));
      columns3.add(new Column(TestConstants.ElementKeys.LATITUDE_COLUMN, "latitude",
          ElementDataType.number.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.LONGITUDE_COLUMN, "longitude",
          ElementDataType.number.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.ALTITUDE_COLUMN, "altitude",
          ElementDataType.number.name(), "[]"));
      columns3.add(new Column(TestConstants.ElementKeys.ACCURACY_COLUMN, "accuracy",
          ElementDataType.number.name(), "[]"));
      
      OrderedColumns orderedColumns3 = new OrderedColumns(TestConstants.TABLES_DEFAULT_APP_NAME, tableId3, columns3);

      userColumns = columns3.size()-1;
      HashMap<String, Integer> elementKeyToIndex3 = new HashMap<String, Integer>();
      String[] elementKeyForIndex3 = new String[userColumns + adminColumns.length];
      elementKeyToIndex3.put(TestConstants.ElementKeys.STRING_COLUMN,0);
      elementKeyForIndex3[0] = TestConstants.ElementKeys.STRING_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.INT_COLUMN,1);
      elementKeyForIndex3[1] = TestConstants.ElementKeys.INT_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.NUMBER_COLUMN,2);
      elementKeyForIndex3[2] = TestConstants.ElementKeys.NUMBER_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.LATITUDE_COLUMN,3);
      elementKeyForIndex3[3] = TestConstants.ElementKeys.LATITUDE_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.LONGITUDE_COLUMN,4);
      elementKeyForIndex3[4] = TestConstants.ElementKeys.LONGITUDE_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.ALTITUDE_COLUMN,5);
      elementKeyForIndex3[5] = TestConstants.ElementKeys.ALTITUDE_COLUMN;
      elementKeyToIndex3.put(TestConstants.ElementKeys.ACCURACY_COLUMN,6);
      elementKeyForIndex3[6] = TestConstants.ElementKeys.ACCURACY_COLUMN;

      for (int j = 0; j < adminColumns.length; j++) {
        elementKeyToIndex3.put(adminColumns[j],userColumns+j);
        elementKeyForIndex3[userColumns+j] = adminColumns[j];
      }

      UserTable tbl3 = new UserTable(orderedColumns3,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex3, elementKeyForIndex3, (mockData ? 3 : 0));
      
      UserTable singleRow1Tbl3 = new UserTable(orderedColumns3,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex3, elementKeyForIndex3, 1);
      UserTable singleRow2Tbl3 = new UserTable(orderedColumns3,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex3, elementKeyForIndex3, 1);
      UserTable singleRow3Tbl3 = new UserTable(orderedColumns3,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex3, elementKeyForIndex3, 1);
      UserTable singleRow4Tbl3 = new UserTable(orderedColumns3,
          null, empty, empty, null, null, null, adminColumns, 
          elementKeyToIndex3, elementKeyForIndex3, 1);
      
      if ( mockData ) {
        String rowId1for3 = TestConstants.ROWID_1;
        String[] rowData1for3 = new String[userColumns + adminColumns.length];
        rowData1for3[0] = "row1 - HUB Construction";
        rowData1for3[1] = "11";
        rowData1for3[2] = "99.11";
        rowData1for3[3] = "47.65434975";
        rowData1for3[4] = "-122.30498975";
        rowData1for3[5] = "21.299999237060547";
        rowData1for3[6] = "6.70820411";
        rowData1for3[userColumns+ 0] = rowId1for3; //DataTableColumns.ID
        rowData1for3[userColumns+ 1] = null; //DataTableColumns.ROW_ETAG
        rowData1for3[userColumns+ 2] = SyncState.new_row.name(); //DataTableColumns.SYNC_STATE
        rowData1for3[userColumns+ 3] = "0"; //DataTableColumns.CONFLICT_TYPE
        rowData1for3[userColumns+ 4] = null; //DataTableColumns.FILTER_TYPE
        rowData1for3[userColumns+ 5] = null; //DataTableColumns.FILTER_VALUE
        rowData1for3[userColumns+ 6] = "tablesId_geotagger"; //DataTableColumns.FORM_ID
        rowData1for3[userColumns+ 7] = "default"; //DataTableColumns.LOCALE
        rowData1for3[userColumns+ 8] = "COMPLETE"; //DataTableColumns.SAVEPOINT_TYPE
        rowData1for3[userColumns+ 9] = "2012-11-30T15:04:00.000000000"; //DataTableColumns.SAVEPOINT_TIMESTAMP
        rowData1for3[userColumns+10] = "anonymous1"; //DataTableColumns.SAVEPOINT_CREATOR
  
        Row row1 = new Row(tbl3, rowId1for3, rowData1for3 );
        Row singleRow1 = new Row(singleRow1Tbl3, rowId1for3, rowData1for3 );
        singleRow1Tbl3.addRow(singleRow1);
        doReturn(singleRow1Tbl3).when(stubIf).getDataInExistingDBTableWithId(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
            eq(TestConstants.DEFAULT_TABLE_ID), any(OrderedColumns.class), eq(rowId1for3));
  
        String rowId2for3 = TestConstants.ROWID_2;
        String[] rowData2for3 = new String[userColumns + adminColumns.length];
        rowData2for3[0] = "row2 - Kite Hill ... Gasworks";
        rowData2for3[1] = "22";
        rowData2for3[2] = "99.22";
        rowData2for3[3] = "47.64540379";
        rowData2for3[4] = "-122.33636588";
        rowData2for3[5] = "-7.199999809265137";
        rowData2for3[6] = "4.472136";
        rowData2for3[userColumns+ 0] = rowId2for3; //DataTableColumns.ID
        rowData2for3[userColumns+ 1] = "uuid:old222"; //DataTableColumns.ROW_ETAG
        rowData2for3[userColumns+ 2] = SyncState.changed.name(); //DataTableColumns.SYNC_STATE
        rowData2for3[userColumns+ 3] = "0"; //DataTableColumns.CONFLICT_TYPE
        rowData2for3[userColumns+ 4] = null; //DataTableColumns.FILTER_TYPE
        rowData2for3[userColumns+ 5] = null; //DataTableColumns.FILTER_VALUE
        rowData2for3[userColumns+ 6] = "tablesId_geotagger"; //DataTableColumns.FORM_ID
        rowData2for3[userColumns+ 7] = "default"; //DataTableColumns.LOCALE
        rowData2for3[userColumns+ 8] = "COMPLETE"; //DataTableColumns.SAVEPOINT_TYPE
        rowData2for3[userColumns+ 9] = "2014-05-05T22:25:14.688000000"; //DataTableColumns.SAVEPOINT_TIMESTAMP
        rowData2for3[userColumns+10] = "anonymous2"; //DataTableColumns.SAVEPOINT_CREATOR
  
        Row row2 = new Row(tbl3, rowId2for3, rowData2for3 );
        Row singleRow2 = new Row(singleRow2Tbl3, rowId2for3, rowData2for3 );
        singleRow2Tbl3.addRow(singleRow2);
        doReturn(singleRow2Tbl3).when(stubIf).getDataInExistingDBTableWithId(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
            eq(TestConstants.DEFAULT_TABLE_ID), any(OrderedColumns.class), eq(rowId2for3));
  
        String rowId3for3 = TestConstants.ROWID_3;
        String[] rowData3for3 = new String[userColumns + adminColumns.length];
        rowData3for3[0] = "row3 - The Ave";
        rowData3for3[1] = "33";
        rowData3for3[2] = "99.33";
        rowData3for3[3] = "47.65824883";
        rowData3for3[4] = "-122.31314593";
        rowData3for3[5] = "31.0";
        rowData3for3[6] = "5.0";
        rowData3for3[userColumns+ 0] = rowId3for3; //DataTableColumns.ID
        rowData3for3[userColumns+ 1] = null; //DataTableColumns.ROW_ETAG
        rowData3for3[userColumns+ 2] = SyncState.new_row.name(); //DataTableColumns.SYNC_STATE
        rowData3for3[userColumns+ 3] = "0"; //DataTableColumns.CONFLICT_TYPE
        rowData3for3[userColumns+ 4] = null; //DataTableColumns.FILTER_TYPE
        rowData3for3[userColumns+ 5] = null; //DataTableColumns.FILTER_VALUE
        rowData3for3[userColumns+ 6] = "tablesId_geotagger"; //DataTableColumns.FORM_ID
        rowData3for3[userColumns+ 7] = "default"; //DataTableColumns.LOCALE
        rowData3for3[userColumns+ 8] = "COMPLETE"; //DataTableColumns.SAVEPOINT_TYPE
        rowData3for3[userColumns+ 9] = "2012-11-30T15:06:00.000000000"; //DataTableColumns.SAVEPOINT_TIMESTAMP
        rowData3for3[userColumns+10] = "anonymous3"; //DataTableColumns.SAVEPOINT_CREATOR
  
        Row row3 = new Row(tbl3, rowId3for3, rowData3for3 );
        Row singleRow3 = new Row(singleRow3Tbl3, rowId3for3, rowData3for3 );
        singleRow3Tbl3.addRow(singleRow3);
        doReturn(singleRow3Tbl3).when(stubIf).getDataInExistingDBTableWithId(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
              eq(TestConstants.DEFAULT_TABLE_ID), any(OrderedColumns.class), eq(rowId3for3));
        
        String rowId4for3 = TestConstants.ROWID_4;
        String[] rowData4for3 = new String[userColumns + adminColumns.length];
        rowData4for3[0] = "row4 - Phinney Ridge";
        rowData4for3[1] = "44";
        rowData4for3[2] = "99.44";
        rowData4for3[3] = "47.66801164";
        rowData4for3[4] = "-122.35537487";
        rowData4for3[5] = "80.0";
        rowData4for3[6] = "5.0";
        rowData4for3[userColumns+ 0] = rowId4for3; //DataTableColumns.ID
        rowData4for3[userColumns+ 1] = null; //DataTableColumns.ROW_ETAG
        rowData4for3[userColumns+ 2] = SyncState.new_row.name(); //DataTableColumns.SYNC_STATE
        rowData4for3[userColumns+ 3] = "0"; //DataTableColumns.CONFLICT_TYPE
        rowData4for3[userColumns+ 4] = null; //DataTableColumns.FILTER_TYPE
        rowData4for3[userColumns+ 5] = null; //DataTableColumns.FILTER_VALUE
        rowData4for3[userColumns+ 6] = "tablesId_geotagger"; //DataTableColumns.FORM_ID
        rowData4for3[userColumns+ 7] = "default"; //DataTableColumns.LOCALE
        rowData4for3[userColumns+ 8] = "COMPLETE"; //DataTableColumns.SAVEPOINT_TYPE
        rowData4for3[userColumns+ 9] = "2015-03-05T22:25:14.688000000"; //DataTableColumns.SAVEPOINT_TIMESTAMP
        rowData4for3[userColumns+10] = "anonymous2"; //DataTableColumns.SAVEPOINT_CREATOR
  
        Row row4 = new Row(tbl3, rowId4for3, rowData4for3 );
        Row singleRow4 = new Row(singleRow4Tbl3, rowId4for3, rowData4for3 );
        singleRow4Tbl3.addRow(singleRow4);
        doReturn(singleRow4Tbl3).when(stubIf).getDataInExistingDBTableWithId(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
            eq(TestConstants.DEFAULT_TABLE_ID), any(OrderedColumns.class), eq(rowId4for3));
        
        tbl3.addRow(row1);
        tbl3.addRow(row2);
        tbl3.addRow(row3);
        tbl3.addRow(row4);
      }

      doReturn(orderedColumns1).when(stubIf).getUserDefinedColumns(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId1));
      doReturn(orderedColumns2).when(stubIf).getUserDefinedColumns(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId2));
      doReturn(orderedColumns3).when(stubIf).getUserDefinedColumns(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId3));
      
      TableUtil util = mock(TableUtil.class);
      when(util.getLocalizedDisplayName(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId1)))
        .thenReturn(tableId1+"_name");
      when(util.getLocalizedDisplayName(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId2)))
        .thenReturn(tableId2+"_name");
      when(util.getLocalizedDisplayName(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId3)))
        .thenReturn(tableId3+"_name");
      when(util.getDefaultViewType(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId1)))
        .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
      when(util.getDefaultViewType(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId2)))
        .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
      when(util.getDefaultViewType(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId3)))
        .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
      TableUtil.set(util);

      doReturn(tbl1).when(stubIf).rawSqlQuery(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
            eq(tableId1), any(OrderedColumns.class), any(String.class), eq(empty), eq(empty), any(String.class), any(String.class), any(String.class));
      doReturn(tbl2).when(stubIf).rawSqlQuery(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
          eq(tableId2), any(OrderedColumns.class), any(String.class), eq(empty), eq(empty), any(String.class), any(String.class), any(String.class));
      doReturn(tbl3).when(stubIf).rawSqlQuery(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), 
          eq(TestConstants.DEFAULT_TABLE_ID), any(OrderedColumns.class), any(String.class), eq(empty), eq(empty), any(String.class), any(String.class), any(String.class));
    } catch (RemoteException e) {
      // ignore?
    }
    Tables.setMockDatabase(stubIf);
  }
  
  /**
   * Make the external storage as mounted. This is required by classes that
   * call through to {@link ODKFileUtils#verifyExternalStorageAvailability()}.
   */
  public static void setExternalStorageMounted() {

    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
  }
  
  public static void assertFile(File fullPath, String content) {
    fullPath.getParentFile().mkdirs();
    try {
      FileOutputStream fs = new FileOutputStream(fullPath, false);
      fs.write(content.getBytes("UTF-8"));
      fs.flush();
      fs.close();
    } catch (FileNotFoundException e) {
      throw new IllegalStateException("unable to create/truncate test file");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException("unable to encode UTF-8 file");
    } catch (IOException e) {
      throw new IllegalStateException("unable to write test file");
    }
  }
  
  /**
   * Restore the external storage to the original state. Pairs with
   * {@link #setExternalStorageMounted()} to undo the call.
   */
  public static void resetExternalStorageState() {
    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_REMOVED);
  }

  public static void startFragmentForMainActivity(Fragment fragment) {
    MainActivity mainActivity = Robolectric.buildActivity(MainActivity.class)
        .create().start().resume().attach().get();
    FragmentManager fm = mainActivity.getFragmentManager();
    fm.beginTransaction().replace(
        R.id.activity_main_activity,
        fragment,
        "TEST_TAG").commit();
  }

  /**
   * Retrieve a {@link CollectFormParameters} object for use in testing.
   * @param isCustom
   * @param FORM_ID
   * @param FORM_VERSION
   * @param formXMLRootElement
   * @param rowDisplayName
   * @return
   */
  public static CollectFormParameters getCollectFormParameters() {
    return new CollectFormParameters(
        TestConstants.FORM_IS_USER_DEFINED,
        TestConstants.FORM_ID,
        TestConstants.FORM_VERSION,
        TestConstants.ROOT_ELEMENT,
        TestConstants.ROW_NAME);
  }

  public static SurveyFormParameters getSurveyFormParameters() {
    return new SurveyFormParameters(
        TestConstants.FORM_IS_USER_DEFINED,
        TestConstants.FORM_ID,
        TestConstants.SCREEN_PATH);
  }

}
