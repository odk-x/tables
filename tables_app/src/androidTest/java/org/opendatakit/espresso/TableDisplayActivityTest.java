package org.opendatakit.espresso;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import androidx.test.rule.GrantPermissionRule;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.TableDisplayActivity;
import org.opendatakit.tables.fragments.TableManagerFragment;

@RunWith(AndroidJUnit4ClassRunner.class)
public class TableDisplayActivityTest {

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Rule
    public ActivityScenarioRule<TableDisplayActivity> activityRule =
            new ActivityScenarioRule<>(TableDisplayActivity.class);

    private ActivityScenario<TableDisplayActivity> scenario;

    @Before
    public void setUp() {
        scenario = activityRule.getScenario();
    }

    @After
    public void tearDown() {
        // Clean up after tests
        scenario.close();
    }

    @Test
    public void testOnCreate() {
        // Verify the UI elements and initial state
        onView(withId(R.id.recycler_view)).check(matches(isDisplayed()));
    }


    @Test
    public void testFragmentTransaction() {
        scenario.onActivity(activity -> {
            FragmentManager fragmentManager = activity.getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            // Start the transaction and add the fragment
            TableManagerFragment fragment = new TableManagerFragment();
            fragmentTransaction.add(R.id.activity_table_manager_list_fragment, fragment);
            fragmentTransaction.commit();

            // Verify the fragment is added
            fragmentManager.executePendingTransactions();
            assertTrue(fragment.isAdded());
        });
    }

    @Test
    public void testRecyclerViewItemClick() {
        // Simulate a click on an item in the RecyclerView
        onView(withId(R.id.recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // Verify that the appropriate activity is launched
        intended(hasComponent(TableDisplayActivity.class.getName()));
    }

    @Test
    public void testTableDetailsDisplay() {
        // Simulate selecting a table and verify the details are displayed
        onView(withId(R.id.recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // Verify that the details are displayed correctly
        onView(withId(R.id.activity_table_display_activity_split_content)).check(matches(isDisplayed()));
        onView(withId(R.id.activity_table_display_activity_split_content)).check(matches(withText("Expected Table Details")));
    }
}
