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
import org.opendatakit.tables.utils.IntentUtil;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowLog;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
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
  
  
  /**
   * This is somewhat of an integration test, ensuring that the file name is
   * passed to the fragment correctly. It can be removed if it ends up coupling
   * things together too tightly. It is a nice and simple gut check, however.
   */
  @Test
  public void fileNamePassedFromActivityIntentToFragment() {
    String targetFileName = "path/to/fancy/file";
    Intent intent = new Intent(
        Robolectric.application.getApplicationContext(),
        WebViewActivityStub.class);
    Bundle bundle = new Bundle();
    IntentUtil.addFileNameToBundle(bundle, targetFileName);
    IntentUtil.addAppNameToBundle(bundle, WebViewActivityStub.APP_NAME);
    intent.putExtras(bundle);
    WebViewActivityStub customBuiltActivity =
        Robolectric.buildActivity(WebViewActivityStub.class)
        .withIntent(intent)
        .create()
        .start()
        .resume()
        .visible()
        .get();
    FragmentManager fragmentManager = customBuiltActivity.getFragmentManager();
    WebFragment webFragment = (WebFragment) fragmentManager.findFragmentByTag(
        Constants.FragmentTags.WEB_FRAGMENT);
    String fragmentFileName = webFragment.getFileName();
    org.fest.assertions.api.Assertions.assertThat(fragmentFileName)
        .isNotNull()
        .isEqualTo(targetFileName);
  }
  

}
