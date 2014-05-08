package org.opendatakit.tables.activities;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.robolectric.Robolectric.shadowOf;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.R;
import org.opendatakit.tables.fragments.WebFragment;
import org.opendatakit.tables.utils.Constants;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowLog;

import android.app.FragmentManager;
import android.view.Menu;
import android.view.MenuItem;


/**
 * 
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class WebViewActivityTest {
  
  WebViewActivityStub activity;
  
  @Before
  public void before() {
    ShadowLog.stream = System.out;
    this.activity = Robolectric.buildActivity(WebViewActivityStub.class)
        .create()
        .start()
        .resume()
        .visible()
        .get();
  }
  
  @After
  public void after() {
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
        fragmentManager.findFragmentByTag(Constants.FragmentTags.WEB_FRAGMENT);
    assertThat(webFragment).isNotNull();
  }
  
  
  
  

}
