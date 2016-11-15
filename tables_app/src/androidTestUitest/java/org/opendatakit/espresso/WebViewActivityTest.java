package org.opendatakit.espresso;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.web.model.Atom;
import android.support.test.espresso.web.model.ElementReference;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.test.suitebuilder.annotation.LargeTest;
import android.webkit.WebView;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.util.DisableAnimationsRule;
import org.opendatakit.util.UAUtils;

import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;

/**
 * Basic sample that shows the usage of Espresso web showcasing API.
 * <p/>
 * The sample has a simple layout which contains a single {@link WebView}. The HTML page displays
 * a form with an input tag and buttons to submit the form.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class WebViewActivityTest {
   private Boolean initSuccess = null;
   private UiDevice mDevice;

   @ClassRule
   public static DisableAnimationsRule disableAnimationsRule = new DisableAnimationsRule();

   @Rule
   public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<MainActivity>(
       MainActivity.class, false, true) {
      @Override
      protected void beforeActivityLaunched() {
         super.beforeActivityLaunched();

         if (initSuccess == null) {
            mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            initSuccess = UAUtils.turnOnCustomHome(mDevice);
         }
      }

      @Override
      protected void afterActivityLaunched() {
         super.afterActivityLaunched();

         onWebView().forceJavascriptEnabled();
      }
   };

   @Before
   public void setup() {
      UAUtils.assertInitSucess(initSuccess);
   }

   @Test public void infiniteTestToReplicateSigabrt() {
      if (true) return;

      // Run through the Tables app an infinite number of times to get a
      // crash
      int numOfTimesToRun = 400;
      int numOfMsToSleep = 0;
      for (int i = 0; i < numOfTimesToRun; i++)
      {
         boolean found = false;
         Atom<ElementReference> elementFound =  findElement(Locator.ID, "launch-button");
         while (!found) {
            found = true;
            try {
               Thread.sleep(numOfMsToSleep);
               onWebView()
                   // Find the input element by ID
                   .withElement(findElement(Locator.ID, "launch-button"))
                       // Launch into teahouses
                   .perform(webClick());
            } catch (RuntimeException e) {
               //e.printStackTrace();
               System.out.println("Failed to find the launch button");
               found = false;
            } catch (InterruptedException ie) {
               System.out.println("Error with thread sleep");
            }
         }

         found = false;
         while (!found) {
            found = true;
            try {
               Thread.sleep(numOfMsToSleep);
               // Find View Tea Houses button
               onWebView().withElement(findElement(Locator.ID, "view-houses"))
                   // Click the button
                   .perform(webClick());
            } catch (RuntimeException e) {
               //e.printStackTrace();
               System.out.println("Failed to find the View Tea Houses button");
               found = false;
            } catch (InterruptedException ie) {
               System.out.println("Error with thread sleep");
            }
         }

         // Find the li
         found = false;
         while (!found) {
            found = true;
            try {
               Thread.sleep(numOfMsToSleep);
               onWebView().withElement(findElement(Locator.ID, "t3"))
                   // Simulate a click via javascript
                   .perform(webClick());
            } catch (RuntimeException e) {
               //e.printStackTrace();
               System.out.println("Failed to find li");
               found = false;
            } catch (InterruptedException ie) {
               System.out.println("Error with thread sleep");
            }
         }

         elementFound =  findElement(Locator.ID, "tea_button");
         found = false;
         while (!found) {
            found = true;
            try {
               Thread.sleep(numOfMsToSleep);
               // Find the response element by ID
               onWebView().withElement(findElement(Locator.ID, "tea_button"))
                   // Could also be id FIELD_16
                   // Verify that the response page contains the entered text
                   .perform(webClick());
            } catch (RuntimeException e) {
               //e.printStackTrace();
               System.out.println("Failed to find the Teas button on detail view");
               found = false;
            } catch (InterruptedException ie) {
               System.out.println("Error with thread sleep");
            }
         }


         Espresso.pressBack();
         Espresso.pressBack();
         Espresso.pressBack();
         Espresso.pressBack();

         System.out.println("Number of iterations = " + i);
      }
   }
}
