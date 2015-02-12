package org.opendatakit.testutils;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.aggregate.odktables.rest.ElementDataType;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.TableUtil;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsTableActivityStub;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.tables.utils.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.SurveyUtil.SurveyFormParameters;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowEnvironment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

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

  public static void setTwoTableDataset() {
    
    SQLiteDatabase stubDb = SQLiteDatabase.create(null);
    DatabaseFactory factoryMock = mock(DatabaseFactory.class);
    doReturn(stubDb).when(factoryMock).getDatabase(any(Context.class), any(String.class));
    DatabaseFactory.set(factoryMock);
    
    ODKDatabaseUtils wrapperMock = mock(ODKDatabaseUtils.class);
    List<String> tableIds = new ArrayList<String>();
    String tableId1 = "alpha";
    String tableId2 = "beta";
    tableIds.add(tableId1);
    tableIds.add(tableId2);
    doReturn(tableIds).when(wrapperMock).getAllTableIds(any(SQLiteDatabase.class));
    
    List<Column> columns1 = new ArrayList<Column>();
    columns1.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
        ElementDataType.string.name(), "[]"));
    columns1.add(new Column(TestConstants.ElementKeys.INT_COLUMN, TestConstants.ElementKeys.INT_COLUMN,
        ElementDataType.integer.name(), "[]"));
    columns1.add(new Column(TestConstants.ElementKeys.NUMBER_COLUMN, TestConstants.ElementKeys.NUMBER_COLUMN,
        ElementDataType.number.name(), "[]"));

    List<Column> columns2 = new ArrayList<Column>();
    columns2.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
        ElementDataType.string.name(), "[]"));
    columns2.add(new Column(TestConstants.ElementKeys.GEOPOINT_COLUMN,TestConstants.ElementKeys.GEOPOINT_COLUMN,
        ElementDataType.string.name(), "[\"" +
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
    
    doReturn(columns1).when(wrapperMock).getUserDefinedColumns(any(SQLiteDatabase.class), eq(tableId1));
    doReturn(columns2).when(wrapperMock).getUserDefinedColumns(any(SQLiteDatabase.class), eq(tableId2));
    ODKDatabaseUtils.set(wrapperMock);
    
    TableUtil util = mock(TableUtil.class);
    when(util.getLocalizedDisplayName(any(SQLiteDatabase.class), eq(tableId1)))
      .thenReturn(tableId1+"_name");
    when(util.getLocalizedDisplayName(any(SQLiteDatabase.class), eq(tableId2)))
      .thenReturn(tableId2+"_name");
    when(util.getDefaultViewType(any(SQLiteDatabase.class), eq(tableId1)))
      .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
    when(util.getDefaultViewType(any(SQLiteDatabase.class), eq(tableId2)))
      .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
    TableUtil.set(util);

  }
  

  public static void setThreeTableDataset() {
    
    SQLiteDatabase stubDb = SQLiteDatabase.create(null);
    DatabaseFactory factoryMock = mock(DatabaseFactory.class);
    doReturn(stubDb).when(factoryMock).getDatabase(any(Context.class), any(String.class));
    DatabaseFactory.set(factoryMock);
    
    ODKDatabaseUtils wrapperMock = mock(ODKDatabaseUtils.class);
    List<String> tableIds = new ArrayList<String>();
    String tableId1 = "alpha";
    String tableId2 = "beta";
    String tableId3 = AbsTableActivityStub.DEFAULT_TABLE_ID;
    tableIds.add(tableId1);
    tableIds.add(tableId2);
    tableIds.add(tableId3);
    doReturn(tableIds).when(wrapperMock).getAllTableIds(any(SQLiteDatabase.class));
    
    List<Column> columns1 = new ArrayList<Column>();
    columns1.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
        ElementDataType.string.name(), "[]"));
    columns1.add(new Column(TestConstants.ElementKeys.INT_COLUMN, TestConstants.ElementKeys.INT_COLUMN,
        ElementDataType.integer.name(), "[]"));
    columns1.add(new Column(TestConstants.ElementKeys.NUMBER_COLUMN, TestConstants.ElementKeys.NUMBER_COLUMN,
        ElementDataType.number.name(), "[]"));

    List<Column> columns2 = new ArrayList<Column>();
    columns2.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
        ElementDataType.string.name(), "[]"));
    columns2.add(new Column(TestConstants.ElementKeys.GEOPOINT_COLUMN,TestConstants.ElementKeys.GEOPOINT_COLUMN,
        ElementDataType.string.name(), "[\"" +
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


    List<Column> columns3 = new ArrayList<Column>();
    columns3.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
        ElementDataType.string.name(), "[]"));
    columns3.add(new Column(TestConstants.ElementKeys.INT_COLUMN, TestConstants.ElementKeys.INT_COLUMN,
        ElementDataType.integer.name(), "[]"));
    columns3.add(new Column(TestConstants.ElementKeys.NUMBER_COLUMN, TestConstants.ElementKeys.NUMBER_COLUMN,
        ElementDataType.number.name(), "[]"));
    columns3.add(new Column(TestConstants.ElementKeys.GEOPOINT_COLUMN,TestConstants.ElementKeys.GEOPOINT_COLUMN,
        ElementDataType.string.name(), "[\"" +
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

    doReturn(columns1).when(wrapperMock).getUserDefinedColumns(any(SQLiteDatabase.class), eq(tableId1));
    doReturn(columns2).when(wrapperMock).getUserDefinedColumns(any(SQLiteDatabase.class), eq(tableId2));
    doReturn(columns3).when(wrapperMock).getUserDefinedColumns(any(SQLiteDatabase.class), eq(tableId3));
    ODKDatabaseUtils.set(wrapperMock);
    
    TableUtil util = mock(TableUtil.class);
    when(util.getLocalizedDisplayName(any(SQLiteDatabase.class), eq(tableId1)))
      .thenReturn(tableId1+"_name");
    when(util.getLocalizedDisplayName(any(SQLiteDatabase.class), eq(tableId2)))
      .thenReturn(tableId2+"_name");
    when(util.getLocalizedDisplayName(any(SQLiteDatabase.class), eq(tableId3)))
      .thenReturn(tableId3+"_name");
    when(util.getDefaultViewType(any(SQLiteDatabase.class), eq(tableId1)))
      .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
    when(util.getDefaultViewType(any(SQLiteDatabase.class), eq(tableId2)))
      .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
    when(util.getDefaultViewType(any(SQLiteDatabase.class), eq(tableId3)))
      .thenReturn(TableUtil.DEFAULT_KEY_CURRENT_VIEW_TYPE);
    TableUtil.set(util);

  }
  
  /**
   * Make the external storage as mounted. This is required by classes that
   * call through to {@link ODKFileUtils#verifyExternalStorageAvailability()}.
   */
  public static void setExternalStorageMounted() {

    ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
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
        R.id.main_activity_frame_layout,
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
