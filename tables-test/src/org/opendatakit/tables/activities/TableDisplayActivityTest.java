package org.opendatakit.tables.activities;

import static org.fest.assertions.api.ANDROID.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TableDisplayActivityTest {
  
  TableDisplayActivityStub activity;
  
  @Before
  public void before() {
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

}
