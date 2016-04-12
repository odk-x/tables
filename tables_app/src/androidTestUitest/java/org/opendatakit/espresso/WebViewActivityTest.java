package org.opendatakit.espresso;

import android.content.ComponentName;
import android.content.Intent;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.web.model.Atom;
import android.support.test.espresso.web.model.ElementReference;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import android.webkit.WebView;
import org.junit.Before;
import org.junit.Rule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.activities.MainActivity;
import org.opendatakit.util.DisableAnimationsRule;

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
   @ClassRule public static DisableAnimationsRule disableAnimationsRule = new DisableAnimationsRule();

   /**
    * A JUnit {@link Rule @Rule} to launch your activity under test. This is a replacement
    * for {@link ActivityInstrumentationTestCase2}.
    * <p>
    * Rules are interceptors which are executed for each test method and will run before
    * any of your setup code in the {@link Before @Before} method.
    * <p>
    * {@link ActivityTestRule} will create and launch of the activity for you and also expose
    * the activity under test. To get a reference to the activity you can use
    * the {@link ActivityTestRule#getActivity()} method.
    */
   @Rule public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<MainActivity>(
       MainActivity.class, false, false) {
      @Override protected void afterActivityLaunched() {
         // Technically we do not need to do this - WebViewActivity has javascript turned on.
         // Other WebViews in your app may have javascript turned off, however since the only way
         // to automate WebViews is through javascript, it must be enabled.
         onWebView().forceJavascriptEnabled();
      }
   };

   @Test public void infiniteTestToReplicateSigabrt() {
      // Lazily launch the Activity with a custom start Intent per test
      mActivityRule.launchActivity(withWebFormIntent());

      // Run through the Tables app an infinite number of times to get a
      // crash
      int numOfTimesToRun = 1;
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
               onWebView().withElement(findElement(Locator.ID, "72c8186a-8141-4b06-a764-a9029c021b20"))
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

         elementFound =  findElement(Locator.ID, "FIELD_16");
         found = false;
         while (!found) {
            found = true;
            try {
               Thread.sleep(numOfMsToSleep);
               // Find the response element by ID
               onWebView().withElement(findElement(Locator.ID, "FIELD_16"))
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

   /**
    * @return start {@link Intent} for the simple web form URL.
    */
   private static Intent withWebFormIntent() {

      Intent intent = new Intent();
      intent.setComponent(new ComponentName("org.opendatakit.tables.android",
          "org.opendatakit.tables.android.activities.Launcher"));
      return intent;
   }

}
