package org.opendatakit.tables.application;

import java.lang.reflect.Method;

import org.robolectric.TestLifecycleApplication;

public class TestTables extends Tables implements TestLifecycleApplication {

  @Override
  public void beforeTest(Method method) {
  }

  @Override
  public void prepareTest(Object test) {
  }

  @Override
  public void afterTest(Method method) {
  }

}
