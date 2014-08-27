package org.opendatakit.tables.activities;

import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.data.TableProperties;
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
    AbsTableActivityStub.TABLE_PROPERTIES =
        AbsTableActivityStub.DEFAULT_TABLE_PROPERTIES;
    AbsTableActivityStub.TABLE_ID = AbsTableActivityStub.DEFAULT_TABLE_ID;
  }
  
  @Test(expected=IllegalStateException.class)
  public void noTableIdThrowsIllegalStateException() {
    // We should get this if we create the activity without a table id.
    AbsTableActivityStub.TABLE_ID = null;
    AbsTableActivityStub.TABLE_PROPERTIES = mock(TableProperties.class);
    this.buildActivity();
  }
  
  @Test(expected=IllegalStateException.class)
  public void noTablePropertiesThrowsIllegalStateException() {
    AbsTableActivityStub.TABLE_PROPERTIES = null;
    this.buildActivity();
  }
  
  @Test
  public void tablePropertiesSetCorrectly() {
    TableProperties mockTp = mock(TableProperties.class);
    AbsTableActivityStub.TABLE_PROPERTIES = mockTp;
    AbsTableActivityStub activity = this.buildActivity();
    TableProperties retrievedTp = activity.getTableProperties();
    org.fest.assertions.api.Assertions.assertThat(retrievedTp)
        .isNotNull()
        .isSameAs(mockTp);
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
