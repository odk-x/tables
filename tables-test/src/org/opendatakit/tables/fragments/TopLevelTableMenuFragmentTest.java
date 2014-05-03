package org.opendatakit.tables.fragments;

import static org.fest.assertions.api.ANDROID.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.activities.TableDisplayActivityStub;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ActivityController;

/**
 * 
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class TopLevelTableMenuFragmentTest {
  
  TopLevelTableMenuFragment fragment;
  
  @Before
  public void setup() {
    this.fragment = new TopLevelTableMenuFragment();
    ODKFragmentTestUtil.startFragmentForTableActivity(
        TableDisplayActivityStub.class,
        this.fragment,
        null);
    // It needs to have visible called so that the fragment is initialized.
    ActivityController.of(this.fragment.getActivity()).visible();
  }
  
  @After
  public void after() {
    TableDisplayActivityStub.resetState();
  }
  
  @Test
  public void assertNotNull() {
    assertThat(this.fragment).isNotNull();
  }
  

}
