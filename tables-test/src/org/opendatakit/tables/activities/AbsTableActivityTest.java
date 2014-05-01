package org.opendatakit.tables.activities;

import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.data.TableProperties;
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
  
  @After
  public void after() {
    // Reset all these values in case they were changed.
    TableActivityStub.APP_NAME = TableActivityStub.DEFAULT_APP_NAME;
    TableActivityStub.TABLE_PROPERTIES =
        TableActivityStub.DEFAULT_TABLE_PROPERTIES;
    TableActivityStub.TABLE_ID = TableActivityStub.DEFAULT_TABLE_ID;
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void noTableIdThrowsIllegalArgument() {
    // We should get this if we create the activity without a table id.
    TableActivityStub.TABLE_ID = null;
    TableActivityStub.TABLE_PROPERTIES = mock(TableProperties.class);
    this.buildActivity();
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void noTablePropertiesThrowsIllegalArgument() {
    TableActivityStub.TABLE_PROPERTIES = null;
    this.buildActivity();
  }
  
  @Test
  public void tablePropertiesSetCorrectly() {
    TableProperties mockTp = mock(TableProperties.class);
    TableActivityStub.TABLE_PROPERTIES = mockTp;
    TableActivityStub activity = this.buildActivity();
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
  TableActivityStub buildActivity() {
    TableActivityStub result = 
        Robolectric.buildActivity(TableActivityStub.class)
          .create()
          .start()
          .resume()
          .visible()
          .get();
    return result;
  }

}
