package org.opendatakit.tables.fragments;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.tables.activities.TableDisplayActivityStub;
import org.opendatakit.tables.utils.TableUtil;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.opendatakit.testutils.TestCaseUtils;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.view.View;
import android.widget.TextView;

@RunWith(RobolectricTestRunner.class)
public class SpreadsheetFragmentTest {

  SpreadsheetFragmentStub fragment;
  Activity activity;

  @Before
  public void before() {
    TestCaseUtils.setThreeTableDataset();
    ShadowLog.stream = System.out;
    TestCaseUtils.setExternalStorageMounted();
    // We don't want to build the menu here, as it doesn't
    // give us any information in this test class.
    TableDisplayActivityStub.BUILD_MENU_FRAGMENT = false;
  }
  private void doGlobalSetup() {
    this.fragment = new SpreadsheetFragmentStub();
    ODKFragmentTestUtil.startFragmentForActivity(
        TableDisplayActivityStub.class,
        this.fragment,
        null);
    this.activity = this.fragment.getActivity();
  }

  private void setupWithNoData() {
    // Make the UserTable show width 0.
    TableUtil tableUtilMock = mock(TableUtil.class);
    
    doReturn(new ArrayList<String>()).when(tableUtilMock).getColumnOrder(any(SQLiteDatabase.class),
        any(String.class));
    UserTable userTableMock = mock(UserTable.class);
    doReturn("emptyTable").when(userTableMock).getTableId();
    doReturn("tables").when(userTableMock).getAppName();
    doReturn(0).when(userTableMock).getWidth();
    TableDisplayActivityStub.USER_TABLE = userTableMock;
    doGlobalSetup();
  }

  private void setupWithData() {
    // For the hell of it we'll make 10 columns.
    int numColumns = 10;
    UserTable userTableMock = mock(UserTable.class);
    doReturn(numColumns).when(userTableMock).getWidth();
    TableDisplayActivityStub.USER_TABLE = userTableMock;
    doGlobalSetup();
  }

  @After
  public void after() {
    TestCaseUtils.resetExternalStorageState();
    TableDisplayActivityStub.resetState();
  }

  @Test
  public void viewWithWidthZeroIsTextView() {
    this.setupWithNoData();
    View fragmentView = this.fragment.getView();
    assertThat(fragmentView)
        .isNotNull()
        .isInstanceOf(TextView.class);
  }

  // this mofo is super complicated atm.
//  @Test
//  public void viewWithDataIsSpreadsheetView() {
//    this.setupWithData();
//    View fragmentView = this.fragment.getView();
//    assertThat(fragmentView)
//        .isNotNull()
//        .isInstanceOf(SpreadsheetView.class);
//  }

}
