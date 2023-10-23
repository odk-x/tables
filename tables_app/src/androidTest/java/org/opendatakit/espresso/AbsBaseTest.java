package org.opendatakit.espresso;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.opendatakit.tables.application.Tables;


public class AbsBaseTest {
  public static Tables c = null;
  private static String TAG = AbsBaseTest.class.getSimpleName();
  @Before public void _setUpC()  {
    c = Tables.getInstance();
    _setUpC(ApplicationProvider.getApplicationContext());
    if (c == null) {
      _setUpC(InstrumentationRegistry.getInstrumentation().getTargetContext());
    }
    if (c == null)
      throw new java.lang.IllegalStateException();
  }

  public void _setUpC(Object context) {
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
    }
  }

}