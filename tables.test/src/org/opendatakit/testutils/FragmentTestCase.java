/*
 * Copyright (C) 2014 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.testutils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
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
@RunWith(RobolectricTestRunner.class)
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
    activity = (Activity) controller.create().start().visible().get();
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
  
  @Test
  public void meaninglessTest() {
    assert(true == false);
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
