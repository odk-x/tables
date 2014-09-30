package org.opendatakit.tables.activities;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.testutils.TestCaseUtils;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * Basic test for the {@link AbsTableActivity}. Note that it is NOT an
 * abstract class.
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class AbsTableActivityTest {
  
  @Before
  public void before() {
    SQLiteDatabase stubDb = SQLiteDatabase.create(null);
    DatabaseFactory factoryMock = mock(DatabaseFactory.class);
    doReturn(stubDb).when(factoryMock).getDatabase(any(Context.class), any(String.class));
    DatabaseFactory.set(factoryMock);
    ODKDatabaseUtils wrapperMock = mock(ODKDatabaseUtils.class);
    String tableId = AbsTableActivityStub.DEFAULT_TABLE_ID;
    List<String> tableIds = new ArrayList<String>();
    tableIds.add(tableId);
    doReturn(tableIds).when(wrapperMock).getAllTableIds(any(SQLiteDatabase.class));
    List<Column> columns = new ArrayList<Column>();
    doReturn(columns).when(wrapperMock).getUserDefinedColumns(any(SQLiteDatabase.class), eq(AbsTableActivityStub.DEFAULT_TABLE_ID));
    ODKDatabaseUtils.set(wrapperMock);
    
    TestCaseUtils.setExternalStorageMounted();
  }
  
  @After
  public void after() {
    TestCaseUtils.resetExternalStorageState();
    // Reset all these values in case they were changed.
    AbsTableActivityStub.APP_NAME = AbsTableActivityStub.DEFAULT_APP_NAME;
    AbsTableActivityStub.TABLE_ID = AbsTableActivityStub.DEFAULT_TABLE_ID;
  }
  
  @Test(expected=IllegalStateException.class)
  public void noTableIdThrowsIllegalStateException() {
    // We should get this if we create the activity without a table id.
    AbsTableActivityStub.TABLE_ID = null;
    this.buildActivity();
  }
  
  @Test
  public void tablePropertiesSetCorrectly() {
    this.buildActivity();
  }
  
  /**
   * Uses robolectic to create the activity, put it through the lifecycle,
   * and call visible. Then returns the activity.
   * @return
   */
  AbsTableActivityStub buildActivity() {
    AbsTableActivityStub result = 
        Robolectric.buildActivity(AbsTableActivityStub.class)
          .create()
          .start()
          .resume()
          .visible()
          .get();
    return result;
  }

}
