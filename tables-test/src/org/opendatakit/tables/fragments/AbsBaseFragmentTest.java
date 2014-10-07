package org.opendatakit.tables.fragments;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.tables.activities.AbsBaseActivityStub;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.opendatakit.testutils.TestCaseUtils;
import org.robolectric.RobolectricTestRunner;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * 
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class AbsBaseFragmentTest {
  
  AbsBaseFragmentStub fragment;
  Activity activity;
  
  @Before
  public void setup() {
    SQLiteDatabase stubDb = SQLiteDatabase.create(null);
    DatabaseFactory factoryMock = mock(DatabaseFactory.class);
    doReturn(stubDb).when(factoryMock).getDatabase(any(Context.class), any(String.class));
    DatabaseFactory.set(factoryMock);
    ODKDatabaseUtils wrapperMock = mock(ODKDatabaseUtils.class);
    List<String> tableIds = new ArrayList<String>();
    doReturn(tableIds).when(wrapperMock).getAllTableIds(any(SQLiteDatabase.class));
    ODKDatabaseUtils.set(wrapperMock);

    TestCaseUtils.setExternalStorageMounted();
    AbsBaseFragmentStub stub = new AbsBaseFragmentStub();
    ODKFragmentTestUtil.startFragmentForActivity(
        AbsBaseActivityStub.class,
        stub,
        null);
    this.fragment = stub;
    this.activity = this.fragment.getActivity();
  }
  
  @After
  public void after() {
    TestCaseUtils.resetExternalStorageState();
    AbsBaseActivityStub.resetState();
  }
  
  @Test
  public void fragmentInitializesNonNull() {
    assertThat(this.fragment).isNotNull();
  }
  
  @Test(expected=IllegalStateException.class)
  public void activityOtherThanAbsBaseActivityThrowsIllegalState() {
    AbsBaseFragmentStub stub = new AbsBaseFragmentStub();
    ODKFragmentTestUtil.startFragmentForActivity(
        Activity.class,
        stub,
        null);
  }
  
  @Test
  public void fragmentGetsAppNameFromParentActivity() {
    String targetAppName = "myFancyAppName";
    AbsBaseActivityStub.APP_NAME = targetAppName;
    AbsBaseFragmentStub stub = new AbsBaseFragmentStub();
    ODKFragmentTestUtil.startFragmentForActivity(
        AbsBaseActivityStub.class,
        stub,
        null);
    String retrievedAppName = stub.getAppName();
    org.fest.assertions.api.Assertions.assertThat(retrievedAppName)
        .isNotNull()
        .isEqualTo(targetAppName);
  }

}
