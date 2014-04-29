package org.opendatakit.testutils;

import org.junit.After;
import org.junit.Before;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.util.ActivityController;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;

/**
 * Provides basic functionality for tests of fragment. Handles putting
 * Fragments into an empty base activity. Tests for fragments should extend
 * this class.
 * <p>
 * Base on:
 * http://blog.nikhaldimann.com/2013/10/10/robolectric-2-2-some-pages-from-the-missing-manual/
 * @author sudar.sam@gmail.com
 *
 */
public class FragmentTestCase<T extends Fragment> {
  
  private static final String FRAGMENT_TAG = "fragment";
  
  private ActivityController controller;
  private Activity activity;
  private T fragment;
  
  @Before
  public void before() {
    ShadowLog.stream = System.out;
  }
  
  /**
   * Adds the fragment to a new blank activity, initializing its view.
   * @param fragment
   */
  public void startFragment(T fragment) {
    this.fragment = fragment;
    controller = Robolectric.buildActivity(Activity.class);
    activity = controller.create().start().visible().get();
    FragmentManager fragmentManager = activity.getFragmentManager();
    fragmentManager.beginTransaction()
        .add(this.fragment, FRAGMENT_TAG).commit();
  }
  
  /**
   * Get the activity that contains the fragment.
   * @return
   */
  public Activity getParentActivity() {
    return this.activity;
  }
  
  @After
  public void destroyFragment() {
    if (this.fragment != null) {
      FragmentManager fragmentManager = this.activity.getFragmentManager();
      fragmentManager.beginTransaction().remove(this.fragment).commit();
      this.fragment = null;
      this.activity = null;
    }
  }
  
  public void pauseAndResumeFragment() {
    this.controller.pause().resume();
  }
  
  @SuppressWarnings("unchecked")
  public T recreateFragment() {
    this.activity.recreate();
    this.fragment = 
        (T) this.activity.getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
    return this.fragment;
  }

}
