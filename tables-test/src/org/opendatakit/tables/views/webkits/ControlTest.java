package org.opendatakit.tables.views.webkits;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.robolectric.Robolectric.shadowOf;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.activities.AbsBaseActivityStub;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.activities.WebViewActivity;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.SQLQueryStruct;
import org.opendatakit.tables.views.webkits.Control;
import org.opendatakit.testutils.TestConstants;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowActivity.IntentForResult;

import android.app.Activity;
import android.content.ComponentName;

/**
 * 
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class ControlTest {
  
  Control control;
  Activity activity;
  
  @Before
  public void before() {
    AbsBaseActivityStub activityStub = Robolectric.buildActivity(
        AbsBaseActivityStub.class)
          .create()
          .start()
          .resume()
          .visible()
          .get();
    Control control = new Control(activityStub);
    this.activity = activityStub;
    this.control = control;
  }
  
  @After
  public void after() {
    AbsBaseActivityStub.resetState();
    // null them out just to make sure.
    this.activity = null;
    this.control = null;
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
