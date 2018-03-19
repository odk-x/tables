package org.opendatakit.espresso;

import android.app.Activity;
import android.app.Application;
import android.support.test.InstrumentationRegistry;
import android.util.Log;
import org.junit.Before;
import org.opendatakit.tables.application.Tables;

//import android.content.Context;

public class AbsBaseTest {
  public static Tables c = null;
  private static String TAG = AbsBaseTest.class.getSimpleName();
  @Before public void _setUpC() throws Exception {
    c = Tables.getInstance();
    if(true) return;
    //try {Thread.sleep(1000);} catch (Throwable ignored) {};
    _setUpC(InstrumentationRegistry.getInstrumentation().getTargetContext());
    if (c == null) {
      _setUpC(InstrumentationRegistry.getTargetContext());
    }
    if (c == null) {
      _setUpC(InstrumentationRegistry.getInstrumentation().getContext());
    }
    if (c == null) {
      _setUpC(InstrumentationRegistry.getContext());
    }
    if (c == null)
      throw new java.lang.IllegalStateException();
  }
  public void _setUpC(Object context) throws Exception {
    if (context instanceof Application) {
      c = (Tables) context;
    } else if (context instanceof Activity) {
      c = (Tables) ((Activity) context).getApplication();
    //} else if (context instanceof Context) {
      //c = ((Context) context).getApplicationContext();
    } else {
      if (context == null) {
        Log.e(TAG, "context is null!");
      } else {
        Log.i(TAG, context.toString());
      }
    }
  }

}