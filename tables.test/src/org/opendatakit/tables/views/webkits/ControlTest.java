package org.opendatakit.tables.views.webkits;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.Robolectric.shadowOf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.opendatakit.aggregate.odktables.rest.ElementDataType;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.common.android.application.CommonApplication;
import org.opendatakit.common.android.data.OrderedColumns;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.database.service.OdkDbInterface;
import org.opendatakit.database.service.TableHealthInfo;
import org.opendatakit.database.service.TableHealthStatus;
import org.opendatakit.tables.activities.AbsBaseActivityStub;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.SQLQueryStruct;
import org.opendatakit.tables.utils.WebViewUtil;
import org.opendatakit.testutils.TestCaseUtils;
import org.opendatakit.testutils.TestConstants;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowActivity.IntentForResult;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.os.RemoteException;

/**
 *
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class ControlTest {

  public static final String PRESENT_TABLE_ID = "anyTableId";
  public static final String MISSING_TABLE_ID = "anyId";
  public static final String VALID_STRING_VALUE = "a string value";
  public static final String VALID_INT_VALUE = "1";
  public static final String VALID_NUMBER_VALUE = "12.3512";

  Control control;
  Activity activity;
  
  @Before
  public void before() throws RemoteException {
    CommonApplication.setMocked();
    TestCaseUtils.setExternalStorageMounted();

    OdkDbInterface stubIf = mock(OdkDbInterface.class);
    
    try {
      OdkDbHandle hNoTransaction = new OdkDbHandle("noTrans");
      OdkDbHandle hTransaction = new OdkDbHandle("trans");
      doReturn(hTransaction).when(stubIf).openDatabase(any(String.class), eq(true));
      doReturn(hNoTransaction).when(stubIf).openDatabase(any(String.class), eq(false));

      String tableId = PRESENT_TABLE_ID;
      List<String> tableIds = new ArrayList<String>();
      tableIds.add(tableId);
      doReturn(tableIds).when(stubIf).getAllTableIds(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class));

      List<Column> columns = new ArrayList<Column>();
      columns.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
          ElementDataType.string.name(), "[]"));
      columns.add(new Column(TestConstants.ElementKeys.INT_COLUMN, TestConstants.ElementKeys.INT_COLUMN,
          ElementDataType.integer.name(), "[]"));
      columns.add(new Column(TestConstants.ElementKeys.NUMBER_COLUMN, TestConstants.ElementKeys.NUMBER_COLUMN,
          ElementDataType.number.name(), "[]"));
      OrderedColumns orderedColumns = new OrderedColumns(TestConstants.TABLES_DEFAULT_APP_NAME, tableId, columns);
      doReturn(orderedColumns).when(stubIf).getUserDefinedColumns(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(tableId));
      List<TableHealthInfo> statuses = new ArrayList<TableHealthInfo>();
      TableHealthInfo thi = new TableHealthInfo(tableId, TableHealthStatus.TABLE_HEALTH_IS_CLEAN);
      statuses.add(thi);
      
      doReturn(statuses).when(stubIf).getTableHealthStatuses(eq(TestConstants.TABLES_DEFAULT_APP_NAME), any(OdkDbHandle.class));

    } catch (RemoteException e) {
      // ignore?
    }

    Tables.setMockDatabase(stubIf);
    TestCaseUtils.setExternalStorageMounted();
    AbsBaseActivityStub activityStub = Robolectric.buildActivity(
        AbsBaseActivityStub.class)
          .create()
          .start()
          .resume()
          .visible()
          .get();
    Control control = new ControlStub(
        activityStub, null, null);
    this.activity = activityStub;
    this.control = control;
  }

  @After
  public void after() {
    AbsBaseActivityStub.resetState();
    ControlStub.resetState();
    TestCaseUtils.resetExternalStorageState();
    // null them out just to make sure.
    this.activity = null;
    this.control = null;
  }

  @Test
  public void addRowReturnsFalseIfTableDoesNotExist() throws RemoteException {
    this.helperAddOrUpdateFailsIfTableDoesNotExist(false);
  }

  @Test
  public void addRowReturnsFalseIfColumnDoesNotExist() throws RemoteException {
    this.helperAddOrUpdateFailsIfColumnDoesNotExist(false);
  }

  @Test
  public void updateRowReturnsFalseIfTableDoesNotExist() throws RemoteException {
    this.helperAddOrUpdateFailsIfTableDoesNotExist(true);
  }

  @Test
  public void updateRowReturnsFalseIfColumnDoesNotExist() throws RemoteException {
    this.helperAddOrUpdateFailsIfColumnDoesNotExist(true);
  }

  protected void helperAddOrUpdateFailsIfTableDoesNotExist(boolean isUpdate) throws RemoteException {
    // we don't want the table to be present
    if (isUpdate) {
      boolean result =
          this.control.updateRow(MISSING_TABLE_ID, "anyString", "anyRowId");
      assertThat(result).isFalse();
    } else {
      this.setupControlStubToReturnGeneratedRowId("some id");
      String rowId = this.control.addRow(MISSING_TABLE_ID, "anyString");
      assertThat(rowId).isNull();
    }
  }
  
  protected void setupControlStubToReturnGeneratedRowId(String rowId) {
    ControlStub.GENERATED_ROW_ID = rowId;
  }

  /**
   * Asserts that the add/update row fails if the column does not exist.
   * @param isUpdate false if you are testing
   * {@link Control#addRow(String, String)}. If true, calls
   * {@link Control#updateRow(String, String, String)}.
   */
  protected void helperAddOrUpdateFailsIfColumnDoesNotExist(boolean isUpdate) throws RemoteException {
    // use this mock object...
    String stringifiedMap = WebViewUtil.stringify(getInvalidMap());
    if (isUpdate) {
      boolean result =
          this.control.updateRow(PRESENT_TABLE_ID, stringifiedMap, "anyRowId");
      assertThat(result).isFalse();
    } else {
      this.setupControlStubToReturnGeneratedRowId("some id");
      String rowId = this.control.addRow(PRESENT_TABLE_ID, stringifiedMap);
      assertThat(rowId).isNull();
    }
  }

  @Test
  public void addRowWithNullContentValuesFails() throws RemoteException {
    this.helperAddOrUpdateWithNullContentValuesFails(false);
  }

  @Test
  public void updateRowWithNullContentValuesFails() throws RemoteException {
    this.helperAddOrUpdateWithNullContentValuesFails(true);
  }

  protected void helperAddOrUpdateWithNullContentValuesFails(
      boolean isUpdate) throws RemoteException {

    // Now we'll do the call and make sure that we called through to the mock
    // object successfully.
    ContentValues contentValues = getContentValuesForValidMap();
    ControlStub.CONTENT_VALUES = contentValues;
    // we need to return a null ContentValues when we ask for it.
    ControlStub.CONTENT_VALUES = null;
    String stringifiedJSON = WebViewUtil.stringify(getValidMap());
    if (isUpdate) {
      boolean result =
          this.control.updateRow(PRESENT_TABLE_ID, stringifiedJSON, "anyRowId");
      assertThat(result).isFalse();
    } else {
      this.setupControlStubToReturnGeneratedRowId("some row id");
      String newRowId = this.control.addRow(PRESENT_TABLE_ID, stringifiedJSON);
      assertThat(newRowId).isNull();
    }
  }

  @Test
  public void addRowWithValidValuesCallsDBUtilsWrapper() throws RemoteException {
    this.helperAddOrUpdateRowWithValuesCallsDBUtilsWrapper(false);
  }

  @Test
  public void updateRowWithValidValuesCallsDBUtilsWrapper() throws RemoteException {
    this.helperAddOrUpdateRowWithValuesCallsDBUtilsWrapper(true);
  }

  protected void helperAddOrUpdateRowWithValuesCallsDBUtilsWrapper(
      boolean isUpdate) throws RemoteException {
    // Now we'll do the call and make sure that we called through to the mock
    // object successfully.
    Map<String, String> validMap = getValidMap();
    String validMapString = WebViewUtil.stringify(validMap);
    ContentValues contentValues = getContentValuesForValidMap();
    ControlStub.CONTENT_VALUES = contentValues;
    String rowId = "aRowId";
    if (isUpdate) {
      this.control.updateRow(PRESENT_TABLE_ID, validMapString, rowId);
      verify(Tables.getInstance().getDatabase(), times(1)).updateDataInExistingDBTableWithId(
          any(String.class),
          any(OdkDbHandle.class),
          eq(PRESENT_TABLE_ID),
          Matchers.any(OrderedColumns.class),
          eq(contentValues),
          eq(rowId));
    } else {
      ControlStub.GENERATED_ROW_ID = rowId;
      String returnedRowId = control.addRow(PRESENT_TABLE_ID, validMapString);
      verify(Tables.getInstance().getDatabase(), times(1)).insertDataIntoExistingDBTableWithId(
          any(String.class),
          any(OdkDbHandle.class),
          eq(PRESENT_TABLE_ID),
          Matchers.any(OrderedColumns.class),
          eq(contentValues),
          eq(rowId));
      assertThat(returnedRowId).isEqualTo(rowId);
    }
  }

  @Test
  public void helperLaunchDefaultViewLaunchesCorrectIntent() {
    this.control.helperLaunchDefaultView(
        TestConstants.DEFAULT_TABLE_ID,
        TestConstants.DEFAULT_SQL_WHERE_CALUSE,
        TestConstants.DEFAULT_SQL_SELECTION_ARGS,
        TestConstants.DEFAULT_SQL_GROUP_BY,
        TestConstants.DEFAULT_SQL_HAVING,
        TestConstants.DEFAULT_SQL_ORDER_BY_ELEMENT_KEY,
        TestConstants.DEFAULT_SQL_ORDER_BY_DIRECTION);
    IntentForResult intent = this.getNextStartedIntent();
    this.assertComponentIsForTableDisplayActivity(
        intent.intent.getComponent());
    this.assertDefaultTableIdIsPresent(intent);
    this.assertFileNameIsNotPresent(intent);
    this.assertViewFragmentTypeIsPresent(intent, null);
    this.assertDefaultSQLArgsArePresent(intent);
    this.assertRowIdIsNotPresent(intent);
  }

  @Test
  public void helperOpenTableToSpreadsheetViewLaunchesCorrectIntent() {
    this.control.helperOpenTableToSpreadsheetView(
        TestConstants.DEFAULT_TABLE_ID,
        TestConstants.DEFAULT_SQL_WHERE_CALUSE,
        TestConstants.DEFAULT_SQL_SELECTION_ARGS,
        TestConstants.DEFAULT_SQL_GROUP_BY,
        TestConstants.DEFAULT_SQL_HAVING,
        TestConstants.DEFAULT_SQL_ORDER_BY_ELEMENT_KEY,
        TestConstants.DEFAULT_SQL_ORDER_BY_DIRECTION);
    IntentForResult intent = this.getNextStartedIntent();
    this.assertComponentIsForTableDisplayActivity(
        intent.intent.getComponent());
    this.assertDefaultTableIdIsPresent(intent);
    this.assertViewFragmentTypeIsPresent(intent, ViewFragmentType.SPREADSHEET);
    this.assertRowIdIsNotPresent(intent);
    this.assertFileNameIsNotPresent(intent);
    this.assertDefaultSQLArgsArePresent(intent);
  }

  @Test
  public void helperOpenTableToMapViewLaunchesCorrectIntent() {
    this.control.helperOpenTableToMapView(
        TestConstants.DEFAULT_TABLE_ID,
        TestConstants.DEFAULT_FILE_NAME,
        TestConstants.DEFAULT_SQL_WHERE_CALUSE,
        TestConstants.DEFAULT_SQL_SELECTION_ARGS,
        TestConstants.DEFAULT_SQL_GROUP_BY,
        TestConstants.DEFAULT_SQL_HAVING,
        TestConstants.DEFAULT_SQL_ORDER_BY_ELEMENT_KEY,
        TestConstants.DEFAULT_SQL_ORDER_BY_DIRECTION);
    IntentForResult intent = this.getNextStartedIntent();
    this.assertComponentIsForTableDisplayActivity(
        intent.intent.getComponent());
    this.assertDefaultTableIdIsPresent(intent);
    this.assertRowIdIsNotPresent(intent);
    this.assertViewFragmentTypeIsPresent(intent, ViewFragmentType.MAP);
    this.assertDefaultFileNameIsPresent(intent);
    this.assertDefaultSQLArgsArePresent(intent);
  }

  @Test
  public void helperOpenTableWithFileLaunchesCorrectIntent() {
    this.control.helperOpenTableWithFile(
        TestConstants.DEFAULT_TABLE_ID,
        TestConstants.DEFAULT_FILE_NAME,
        TestConstants.DEFAULT_SQL_WHERE_CALUSE,
        TestConstants.DEFAULT_SQL_SELECTION_ARGS,
        TestConstants.DEFAULT_SQL_GROUP_BY,
        TestConstants.DEFAULT_SQL_HAVING,
        TestConstants.DEFAULT_SQL_ORDER_BY_ELEMENT_KEY,
        TestConstants.DEFAULT_SQL_ORDER_BY_DIRECTION);
    IntentForResult intent = this.getNextStartedIntent();
    this.assertComponentIsForTableDisplayActivity(
        intent.intent.getComponent());
    this.assertDefaultTableIdIsPresent(intent);
    this.assertDefaultFileNameIsPresent(intent);
    this.assertRowIdIsNotPresent(intent);
    this.assertViewFragmentTypeIsPresent(intent, ViewFragmentType.LIST);
    this.assertDefaultSQLArgsArePresent(intent);
  }

  @Test
  public void openDetailViewWithFileLaunchesCorrectIntent() {
    this.control.openDetailViewWithFile(
        TestConstants.DEFAULT_TABLE_ID,
        TestConstants.DEFAULT_ROW_ID,
        TestConstants.DEFAULT_FILE_NAME);
    IntentForResult intent = this.getNextStartedIntent();
    this.assertComponentIsForTableDisplayActivity(
        intent.intent.getComponent());
    assertThat(intent.intent.getExtras()).isNotNull();
    this.assertDefaultTableIdIsPresent(intent);
    this.assertDefaultRowIdIsPresent(intent);
    this.assertDefaultFileNameIsPresent(intent);
  }

  @Test
  public void launchHTMLLaunchesCorrectIntent() {
    this.control.launchHTML(TestConstants.DEFAULT_FILE_NAME);
    IntentForResult intent = this.getNextStartedIntent();
    this.assertComponentIsForWebViewActivity(intent.intent.getComponent());
    assertThat(intent.intent.getExtras()).isNotNull();
    this.assertDefaultFileNameIsPresent(intent);
    this.assertTableIdIsNotPresent(intent);
    this.assertRowIdIsNotPresent(intent);
  }

  /**
   * Get a map with valid values of the map returned by
   * {@link TestConstants#getMapOfElementKeyToValue(String, String)}.
   * @return
   */
  protected Map<String, String> getValidMap() {
    Map<String, String> map = TestConstants.getMapOfElementKeyToValue(
        VALID_STRING_VALUE,
        VALID_INT_VALUE,
        VALID_NUMBER_VALUE);
    return map;
  }

  protected Map<String, String> getInvalidMap() {
    Map<String, String> map = TestConstants.getMapOfElementKeyToValueIncludingMissing(
        VALID_STRING_VALUE,
        VALID_INT_VALUE,
        VALID_NUMBER_VALUE);
    return map;
  }

  protected ContentValues getContentValuesForValidMap() {
    ContentValues result = new ContentValues();
    result.put(TestConstants.ElementKeys.STRING_COLUMN, VALID_STRING_VALUE);
    result.put(
        TestConstants.ElementKeys.INT_COLUMN,
        Integer.parseInt(VALID_INT_VALUE));
    result.put(
        TestConstants.ElementKeys.NUMBER_COLUMN,
        Double.parseDouble(VALID_NUMBER_VALUE));
    return result;
  }

  private IntentForResult getNextStartedIntent() {
    ShadowActivity shadowActivity = shadowOf(this.activity);
    return shadowActivity.peekNextStartedActivityForResult();
  }

  private void assertComponentIsForTableDisplayActivity(
      ComponentName component) {
    ComponentName target = new ComponentName(
        this.activity,
        TableDisplayActivity.class);
    org.fest.assertions.api.Assertions.assertThat(component).isEqualTo(target);
  }

  private void assertComponentIsForWebViewActivity(ComponentName component) {
    ComponentName target = new ComponentName(
        this.activity,
        MainActivity.class);
    org.fest.assertions.api.Assertions.assertThat(component).isEqualTo(target);
  }

  private void assertDefaultTableIdIsPresent(IntentForResult intent) {
    String tableId = IntentUtil.retrieveTableIdFromBundle(
        intent.intent.getExtras());
    org.fest.assertions.api.Assertions.assertThat(tableId)
        .isNotNull()
        .isEqualTo(TestConstants.DEFAULT_TABLE_ID);
  }

  private void assertDefaultRowIdIsPresent(IntentForResult intent) {
    String rowId = IntentUtil.retrieveRowIdFromBundle(
        intent.intent.getExtras());
    org.fest.assertions.api.Assertions.assertThat(rowId)
        .isNotNull()
        .isEqualTo(TestConstants.DEFAULT_ROW_ID);
  }

  private void assertRowIdIsNotPresent(IntentForResult intent) {
    String rowId = IntentUtil.retrieveRowIdFromBundle(
        intent.intent.getExtras());
    org.fest.assertions.api.Assertions.assertThat(rowId).isNull();
  }

  private void assertDefaultFileNameIsPresent(IntentForResult intent) {
    String fileName = IntentUtil.retrieveFileNameFromBundle(
        intent.intent.getExtras());
    org.fest.assertions.api.Assertions.assertThat(fileName)
        .isNotNull()
        .isEqualTo(TestConstants.DEFAULT_FILE_NAME);
  }

  private void assertFileNameIsNotPresent(IntentForResult intent) {
    String fileName = IntentUtil.retrieveFileNameFromBundle(
        intent.intent.getExtras());
    org.fest.assertions.api.Assertions.assertThat(fileName).isNull();
  }

  private void assertTableIdIsNotPresent(IntentForResult intent){
    String tableId = IntentUtil.retrieveTableIdFromBundle(
        intent.intent.getExtras());
    org.fest.assertions.api.Assertions.assertThat(tableId).isNull();
  }

  private void assertViewFragmentTypeIsPresent(
      IntentForResult intent,
      ViewFragmentType viewFragmentType) {
    String typeString = intent.intent.getExtras().getString(
        Constants.IntentKeys.TABLE_DISPLAY_VIEW_TYPE);
    if (viewFragmentType == null) {
      org.fest.assertions.api.Assertions.assertThat(typeString).isNull();
    } else {
      org.fest.assertions.api.Assertions.assertThat(typeString)
        .isNotNull()
        .isEqualTo(viewFragmentType.name());
    }
  }

  private void assertDefaultSQLArgsArePresent(IntentForResult intent) {
    SQLQueryStruct queryStruct = IntentUtil.getSQLQueryStructFromBundle(
        intent.intent.getExtras());
    org.fest.assertions.api.Assertions.assertThat(queryStruct).isNotNull();
    org.fest.assertions.api.Assertions.assertThat(queryStruct.whereClause)
        .isNotNull()
        .isEqualTo(TestConstants.DEFAULT_SQL_WHERE_CALUSE);
    org.fest.assertions.api.Assertions.assertThat(queryStruct.selectionArgs)
        .isNotNull()
        .isEqualTo(TestConstants.DEFAULT_SQL_SELECTION_ARGS);
    org.fest.assertions.api.Assertions.assertThat(queryStruct.groupBy)
        .isNotNull()
        .isEqualTo(TestConstants.DEFAULT_SQL_GROUP_BY);
    org.fest.assertions.api.Assertions.assertThat(queryStruct.having)
        .isNotNull()
        .isEqualTo(TestConstants.DEFAULT_SQL_HAVING);
    org.fest.assertions.api.Assertions.assertThat(
        queryStruct.orderByElementKey)
        .isNotNull()
        .isEqualTo(TestConstants.DEFAULT_SQL_ORDER_BY_ELEMENT_KEY);
    org.fest.assertions.api.Assertions.assertThat(queryStruct.orderByDirection)
        .isNotNull()
        .isEqualTo(TestConstants.DEFAULT_SQL_ORDER_BY_DIRECTION);
  }

}
