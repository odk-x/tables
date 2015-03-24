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
import org.opendatakit.common.android.application.CommonApplication;
import org.opendatakit.common.android.data.OrderedColumns;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.database.service.OdkDbInterface;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.testutils.TestCaseUtils;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import android.os.RemoteException;

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
    CommonApplication.setMocked();
    TestCaseUtils.setExternalStorageMounted();

    OdkDbInterface stubIf = mock(OdkDbInterface.class);

    try {
      OdkDbHandle hNoTransaction = new OdkDbHandle("noTrans");
      OdkDbHandle hTransaction = new OdkDbHandle("trans");
      doReturn(hTransaction).when(stubIf).openDatabase(any(String.class), eq(true));
      doReturn(hNoTransaction).when(stubIf).openDatabase(any(String.class), eq(false));
  
      String tableId = AbsTableActivityStub.DEFAULT_TABLE_ID;
      List<String> tableIds = new ArrayList<String>();
      tableIds.add(tableId);
      OrderedColumns orderedColumns = new OrderedColumns( AbsTableActivityStub.DEFAULT_APP_NAME, AbsTableActivityStub.DEFAULT_TABLE_ID, new ArrayList<Column>());

      doReturn(tableIds).when(stubIf).getAllTableIds( eq(AbsTableActivityStub.DEFAULT_APP_NAME), any(OdkDbHandle.class));
      doReturn(orderedColumns).when(stubIf).getUserDefinedColumns( eq(AbsTableActivityStub.DEFAULT_APP_NAME), any(OdkDbHandle.class), eq(AbsTableActivityStub.DEFAULT_TABLE_ID));
    } catch ( RemoteException e ) {
      // ignore?
    }
    
    Tables.setMockDatabase(stubIf);
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
