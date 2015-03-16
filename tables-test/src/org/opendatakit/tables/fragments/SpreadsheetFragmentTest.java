package org.opendatakit.tables.fragments;

import static org.fest.assertions.api.ANDROID.assertThat;
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
import org.opendatakit.common.android.application.CommonApplication;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.database.service.OdkDbInterface;
import org.opendatakit.tables.activities.TableDisplayActivityStub;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.TableUtil;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.opendatakit.testutils.TestCaseUtils;
import org.opendatakit.testutils.TestConstants;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.RemoteException;
import android.view.View;
import android.widget.TextView;

@RunWith(RobolectricTestRunner.class)
public class SpreadsheetFragmentTest {

  SpreadsheetFragment fragment;
  Activity activity;

  private void doGlobalSetup(String tableId) {
    TestCaseUtils.setExternalStorageMounted();
    activity = ODKFragmentTestUtil.startFragmentForTableDisplayActivity(
        ViewFragmentType.SPREADSHEET, tableId);
    FragmentManager mgr = activity.getFragmentManager();
    fragment = (SpreadsheetFragment) mgr.findFragmentByTag(ViewFragmentType.SPREADSHEET.name());
  }

  @Before
  public void before() {
    ShadowLog.stream = System.out;
    // We don't want to build the menu here, as it doesn't
    // give us any information in this test class.
    TableDisplayActivityStub.BUILD_MENU_FRAGMENT = false;
  }

  private void setupWithNoData() {
    CommonApplication.setMocked();
    TestCaseUtils.setExternalStorageMounted();
    
    TestCaseUtils.setOneTableDataset();

    doGlobalSetup(TestConstants.DEFAULT_EMPTY_TABLE_ID);
  }

  private void setupWithData() {
    CommonApplication.setMocked();
    TestCaseUtils.setExternalStorageMounted();
    
    TestCaseUtils.setThreeTableDataset(true);

    doGlobalSetup(TestConstants.DEFAULT_TABLE_ID);
  }

  @After
  public void after() {
    TestCaseUtils.resetExternalStorageState();
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
