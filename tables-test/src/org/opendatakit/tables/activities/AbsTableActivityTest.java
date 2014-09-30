package org.opendatakit.tables.activities;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.testutils.TestCaseUtils;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

/**
 * Basic test for the {@link AbsTableActivity}. Note that it is NOT an
 * abstract class.
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class AbsTableActivityTest {
  
  @Before
  public void before() {
    TestCaseUtils.setExternalStorageMounted();
  }
  
  @After
  public void after() {
    TestCaseUtils.resetExternalStorageState();
    // Reset all these values in case they were changed.
    AbsTableActivityStub.APP_NAME = AbsTableActivityStub.DEFAULT_APP_NAME;
    AbsTableActivityStub.TABLE_ID = AbsTableActivityStub.DEFAULT_TABLE_ID;
  }
  
  @Test(expected=IllegalStateException.class)
  public void noTableIdThrowsIllegalStateException() {
    // We should get this if we create the activity without a table id.
    AbsTableActivityStub.TABLE_ID = null;
    this.buildActivity();
  }
  
  @Test(expected=IllegalStateException.class)
  public void noTablePropertiesThrowsIllegalStateException() {
    this.buildActivity();
  }
  
  @Test
  public void tablePropertiesSetCorrectly() {
    AbsTableActivityStub activity = this.buildActivity();
  }
  
  /**
   * Uses robolectic to create the activity, put it through the lifecycle,
   * and call visible. Then returns the activity.
   * @return
   */
  AbsTableActivityStub buildActivity() {
    AbsTableActivityStub result = 
        Robolectric.buildActivity(AbsTableActivityStub.class)
          .create()
          .start()
          .resume()
          .visible()
          .get();
    return result;
  }

}
