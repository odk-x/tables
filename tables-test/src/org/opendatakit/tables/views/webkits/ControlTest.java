package org.opendatakit.tables.views.webkits;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.robolectric.Robolectric.shadowOf;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.data.ColumnProperties;
import org.opendatakit.common.android.data.ColumnType;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.activities.AbsBaseActivityStub;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.activities.WebViewActivity;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.ODKDatabaseUtilsWrapper;
import org.opendatakit.tables.utils.SQLQueryStruct;
import org.opendatakit.tables.utils.WebViewUtil;
import org.opendatakit.testutils.TestConstants;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowActivity.IntentForResult;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;

/**
 *
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class ControlTest {

  public static final String VALID_STRING_VALUE = "a string value";
  public static final String VALID_INT_VALUE = "1";
  public static final String VALID_NUMBER_VALUE = "12.3512";

  Control control;
  Activity activity;
  /** The wrapper that should be used by the control stub. */
  ODKDatabaseUtilsWrapper wrapperMock;

  @Before
  public void before() {
    AbsBaseActivityStub activityStub = Robolectric.buildActivity(
        AbsBaseActivityStub.class)
          .create()
          .start()
          .resume()
          .visible()
          .get();
    // First set up the database utils wrapper mock.
    this.wrapperMock = mock(ODKDatabaseUtilsWrapper.class);
    ControlStub.DATABASE = TestConstants.getDatabaseMock();
    ControlStub.DB_UTILS_WRAPPER = wrapperMock;
    Control control = new ControlStub(
        activityStub,
        TestConstants.TABLES_DEFAULT_APP_NAME);
    this.activity = activityStub;
    this.control = control;
  }

  @After
  public void after() {
    AbsBaseActivityStub.resetState();
    ControlStub.resetState();
    // null them out just to make sure.
    this.activity = null;
    this.control = null;
  }

  protected void setupControlWithTablePropertiesMock() {
    TableProperties mock = mock(TableProperties.class);
    ControlStub.TABLE_PROPERTIES_FOR_ID = mock;
  }

  @Test
  public void addRowReturnsFalseIfTableDoesNotExist() {
    this.helperAddOrUpdateFailsIfTableDoesNotExist(false);
  }

  @Test
  public void addRowReturnsFalseIfColumnDoesNotExist() {
    this.helperAddOrUpdateFailsIfColumnDoesNotExist(false);
  }

  @Test
  public void updateRowReturnsFalseIfTableDoesNotExist() {
    this.helperAddOrUpdateFailsIfTableDoesNotExist(true);
  }

  @Test
  public void updateRowReturnsFalseIfColumnDoesNotExist() {
    this.helperAddOrUpdateFailsIfColumnDoesNotExist(true);
  }

  protected void helperAddOrUpdateFailsIfTableDoesNotExist(boolean isUpdate) {
    // we don't want the tp to be present
    ControlStub.TABLE_PROPERTIES_FOR_ID = null;
    boolean result;
    if (isUpdate) {
      result = this.control.updateRow("anyId", "anyString", "anyRowId");
    } else {
      result = this.control.addRow("anyId", "anyString");
    }
    assertThat(result).isFalse();
  }

  /**
   * Asserts that the add/update row fails if the column does not exist.
   * @param isUpdate false if you are testing
   * {@link Control#addRow(String, String)}. If true, calls
   * {@link Control#updateRow(String, String, String)}.
   */
  protected void helperAddOrUpdateFailsIfColumnDoesNotExist(boolean isUpdate) {
    TableProperties tpMock = TestConstants.getTablePropertiesMock();
    ControlStub.TABLE_PROPERTIES_FOR_ID = tpMock;
    String nonExistentColumn = "thisColumnDoesNotExist";
    doReturn(null).when(tpMock).getColumnByElementKey(nonExistentColumn);
    ControlStub.TABLE_PROPERTIES_FOR_ID = tpMock;
    String stringifiedMap = WebViewUtil.stringify(getValidMap());
    boolean result;
    if (isUpdate) {
      result = this.control.updateRow("anyId", stringifiedMap, "anyRowId");
    } else {
      result = this.control.addRow("anyId", stringifiedMap);
    }
    assertThat(result).isFalse();
  }

  @Test
  public void addRowWithNullContentValuesFails() {
    this.helperAddOrUpdateWithNullContentValuesFails(false);
  }

  @Test
  public void updateRowWithNullContentValuesFails() {
    this.helperAddOrUpdateWithNullContentValuesFails(true);
  }

  protected void helperAddOrUpdateWithNullContentValuesFails(
      boolean isUpdate) {
    setupControlWithTablePropertiesMock();
    // we need to return a null ContentValues when we ask for it.
    ControlStub.CONTENT_VALUES = null;
    String stringifiedJSON = WebViewUtil.stringify(getValidMap());
    boolean result;
    if (isUpdate) {
      result = this.control.updateRow("anyTableId", stringifiedJSON, "anyRowId");
    } else {
      result = this.control.addRow("anyTableId", stringifiedJSON);
    }
    assertThat(result).isFalse();
  }

  @Test
  public void addRowWithValidValuesCallsDBUtilsWrapper() {
    this.helperAddOrUpdateRowWithValuesCallsDBUtilsWrapper(false);
  }

  @Test
  public void updateRowWithValidValuesCallsDBUtilsWrapper() {
    this.helperAddOrUpdateRowWithValuesCallsDBUtilsWrapper(true);
  }

  protected void helperAddOrUpdateRowWithValuesCallsDBUtilsWrapper(
      boolean isUpdate) {
    setupControlWithTablePropertiesMock();
    TableProperties tpMock = ControlStub.TABLE_PROPERTIES_FOR_ID;
    ColumnProperties stringColumn = TestConstants.getColumnPropertiesMock(
        TestConstants.ElementKeys.STRING_COLUMN,
        ColumnType.STRING);
    ColumnProperties intColumn = TestConstants.getColumnPropertiesMock(
        TestConstants.ElementKeys.INT_COLUMN,
        ColumnType.INTEGER);
    ColumnProperties numberColumn = TestConstants.getColumnPropertiesMock(
        TestConstants.ElementKeys.NUMBER_COLUMN,
        ColumnType.NUMBER);
    doReturn(stringColumn).when(tpMock).getColumnByElementKey(
        TestConstants.ElementKeys.STRING_COLUMN);
    doReturn(intColumn).when(tpMock).getColumnByElementKey(
        TestConstants.ElementKeys.INT_COLUMN);
    doReturn(numberColumn).when(tpMock).getColumnByElementKey(
        TestConstants.ElementKeys.NUMBER_COLUMN);
    // Now we'll do the call and make sure that we called through to the mock
    // object successfully.
    String tableId = "anyTableId";
    Map<String, String> validMap = getValidMap();
    String validMapString = WebViewUtil.stringify(validMap);
    ContentValues contentValues = getContentValuesForValidMap();
    ControlStub.CONTENT_VALUES = contentValues;
    if (isUpdate) {
      String uuid = "aRowId";
      this.control.updateRow(tableId, validMapString, uuid);
      verify(wrapperMock, times(1)).writeDataIntoExistingDBTableWithId(
          eq(ControlStub.DATABASE),
          eq(tableId),
          eq(contentValues),
          eq(uuid));
    } else {
      control.addRow(tableId, validMapString);
      verify(wrapperMock, times(1)).writeDataIntoExistingDBTable(
          eq(ControlStub.DATABASE),
          eq(tableId),
          eq(contentValues));
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
        WebViewActivity.class);
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
