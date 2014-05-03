package org.opendatakit.tables.activities;

import static org.fest.assertions.api.ANDROID.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.fragments.TopLevelTableMenuFragment;
import org.opendatakit.tables.utils.Constants;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import android.app.FragmentManager;

@RunWith(RobolectricTestRunner.class)
public class TableDisplayActivityTest {
  
  TableDisplayActivityStub activity;
  
  @Before
  public void before() {
    ShadowLog.stream = System.out;
    this.activity = Robolectric.buildActivity(TableDisplayActivityStub.class)
        .create()
        .start()
        .resume()
        .visible()
        .get();
  }
  
  @After
  public void after() {
    TableDisplayActivityStub.resetState();
  }
  
  @Test
  public void activityIsCreatedSuccessfully() {
    assertThat(this.activity).isNotNull();
  }
  
  @Test
  public void menuFragmentIsNotNull() {
    FragmentManager fragmentManager = this.activity.getFragmentManager();
    TopLevelTableMenuFragment menuFragment = (TopLevelTableMenuFragment)
        fragmentManager.findFragmentByTag(Constants.FragmentTags.TABLE_MENU);
    assertThat(menuFragment).isNotNull();
  }

}
