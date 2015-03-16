package org.opendatakit.tables.activities;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.robolectric.Robolectric.shadowOf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.common.android.application.CommonApplication;
import org.opendatakit.common.android.data.OrderedColumns;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.database.service.OdkDbInterface;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.MainActivity.ScreenType;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.fragments.WebFragment;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.opendatakit.testutils.TestCaseUtils;
import org.opendatakit.testutils.TestConstants;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowLog;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;


/**
 * 
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class WebViewActivityTest {
  
  MainActivity activity;
  
  @Before
  public void before() {
    CommonApplication.setMocked();
    TestCaseUtils.setExternalStorageMounted();

    TestCaseUtils.setThreeTableDataset(true);

    ShadowLog.stream = System.out;
    
    File initialFile = ODKFileUtils.asAppFile(TestConstants.TABLES_DEFAULT_APP_NAME, TestConstants.DEFAULT_FILE_NAME);
    TestCaseUtils.assertFile(initialFile, "<html><head></head><body><p>Blank</p></body></html>");
    
    this.activity = ODKFragmentTestUtil.startMainActivity(TestConstants.DEFAULT_FILE_NAME);
  }
  
  @After
  public void after() {
    TestCaseUtils.resetExternalStorageState();
    WebViewActivityStub.resetState();
  }
  
  @Test
  public void activityIsCreatedSuccessfully() {
    assertThat(this.activity).isNotNull();
  }
  
  @Test
  public void menuHasTableManagerItem() {
    ShadowActivity shadow = shadowOf(this.activity);
    Menu optionsMenu = shadow.getOptionsMenu();
    MenuItem tableManagerItem =
        optionsMenu.findItem(R.id.menu_web_view_activity_table_manager);
    assertThat(tableManagerItem)
        .isNotNull()
        .isVisible();
  }
  
  @Test
  public void webFragmentIsNotNull() {
    FragmentManager fragmentManager = this.activity.getFragmentManager();
    WebFragment webFragment = (WebFragment)
        fragmentManager.findFragmentByTag(ScreenType.WEBVIEW_SCREEN.name());
    assertThat(webFragment).isNotNull();
  }
  
  
  /**
   * This is somewhat of an integration test, ensuring that the file name is
   * passed to the fragment correctly. It can be removed if it ends up coupling
   * things together too tightly. It is a nice and simple gut check, however.
   */
  @Test
  public void fileNamePassedFromActivityIntentToFragment() {
    FragmentManager fragmentManager = this.activity.getFragmentManager();
    WebFragment webFragment = (WebFragment)
        fragmentManager.findFragmentByTag(ScreenType.WEBVIEW_SCREEN.name());
    String fragmentFileName = webFragment.getFileName();
    String actualFilename;
    if ( File.separatorChar != '/' ) {
      // Windows nonsense...
      actualFilename = TestConstants.DEFAULT_FILE_NAME.replaceAll("/", File.separator + File.separator);
    } else {
      actualFilename = TestConstants.DEFAULT_FILE_NAME;
    }
    org.fest.assertions.api.Assertions.assertThat(fragmentFileName)
        .isNotNull()
        .isEqualTo(actualFilename);
  }
  

}
