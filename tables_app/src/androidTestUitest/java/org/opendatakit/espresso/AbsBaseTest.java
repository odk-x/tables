package org.opendatakit.espresso;

import android.app.Application;
import android.app.Activity;
import org.opendatakit.tables.application.Tables;
import android.content.Context;
import org.junit.Before;
import android.support.test.InstrumentationRegistry;
import java.lang.Thread; import java.lang.Throwable;
import android.util.Log;

public class AbsBaseTest {
  public static Tables c = null;
  private static String TAG = AbsBaseTest.class.getSimpleName();
  @Before public void _setUpC() {
    try {Thread.sleep(1000);} catch (Throwable ignored) {};
    Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    if (context instanceof Application) {
      c = (Tables) context;
    } else if (context instanceof Activity) {
      c = (Tables) ((Activity) context).getApplication();
    } else {
      if (context == null) {
        Log.e(TAG, "context is null!");
      } else {
        Log.i(TAG, context.toString());
      }
      if (c == null)
        throw new java.lang.IllegalStateException();
    }
  }

}