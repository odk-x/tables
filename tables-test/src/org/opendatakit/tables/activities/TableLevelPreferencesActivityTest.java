package org.opendatakit.tables.activities;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.R;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import android.app.Fragment;


/**
 *
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class TableLevelPreferencesActivityTest {

  // TODO: Robolectric doesn't currently support PreferenceFragments. Wait for
  // now.

  TableLevelPreferencesActivityStub activity;

  @Before
  public void setup() {
    // Create a mock TableProperties that will give us known results.
    TableProperties propertiesMock = mock(TableProperties.class);
    TableLevelPreferencesActivityStub.TABLE_PROPERTIES = propertiesMock;
    this.activity =
        Robolectric.buildActivity(TableLevelPreferencesActivityStub.class)
          .create()
          .start()
          .resume()
          .visible()
          .get();
  }

  @After
  public void after() {
    TableLevelPreferencesActivityStub.resetState();
  }

  @Test
  public void tablePreferenceFragmentNotNull() {
    Fragment fragment = this.activity.getFragmentManager()
        .findFragmentById(R.id.fragment_table_preference);
    assertThat(fragment).isNotNull();
  }

}
