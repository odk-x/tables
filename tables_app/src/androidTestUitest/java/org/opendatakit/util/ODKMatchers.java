package org.opendatakit.util;

import android.content.Intent;
import android.view.View;
import android.webkit.WebView;
import android.widget.ListView;

import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.espresso.matcher.ViewMatchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.opendatakit.data.ColorRule;
import org.opendatakit.tables.utils.TableNameStruct;

import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasData;
import static androidx.test.espresso.intent.matcher.IntentMatchers.toPackage;
import static androidx.test.espresso.intent.matcher.UriMatchers.hasHost;
import static androidx.test.espresso.intent.matcher.UriMatchers.hasPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.opendatakit.util.TestConstants.APP_NAME;
import static org.opendatakit.util.TestConstants.SURVEY_PKG_NAME;

public class ODKMatchers {
  public static Matcher<View> withSize(final int size) {
    return new BoundedMatcher<View, ListView>(ListView.class) {
      private int listSize;

      @Override
      public void describeTo(Description description) {
        description.appendText("List size should be: " + size + "; Got: " + this.listSize);
      }

      @Override
      protected boolean matchesSafely(ListView item) {
        this.listSize = item.getCount();

        return this.listSize == size;
      }
    };
  }

  public static Matcher<View> withUrl(final Matcher<String> url) {
    return new BoundedMatcher<View, WebView>(WebView.class) {
      @Override
      public void describeTo(Description description) {
        description.appendText("with URL: ");
        url.describeTo(description);
      }

      @Override
      protected boolean matchesSafely(WebView item) {
        return url.matches(item.getUrl());
      }
    };
  }

  public static Matcher<TableNameStruct> withTable(final String tableId) {
    return new TypeSafeMatcher<TableNameStruct>() {
      private TableNameStruct tableName;

      @Override
      protected boolean matchesSafely(TableNameStruct item) {
        this.tableName = item;

        return item.getTableId().equals(tableId);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Table Id should be: " + tableName.getTableId());
        description.appendText("; Got: " + tableId);
      }
    };
  }

  public static Matcher<ColorRule> withColorRule(final ColorRule rule) {
    return new TypeSafeMatcher<ColorRule>() {
      @Override
      protected boolean matchesSafely(ColorRule rule2) {
        return rule2.equalsWithoutId(rule);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Expected: " + rule.toString());
      }
    };
  }

  public static Matcher<Intent> hasTable(final String tableId, final String formId,
      final String instanceId) {
    String scheme = "content://";
    String host = "org.opendatakit.provider.forms";
    String path = "/" + APP_NAME + "/" + tableId + "/" + formId + "/";

    Matcher<Intent> partial = allOf(hasAction("android.intent.action.EDIT"),
        toPackage(SURVEY_PKG_NAME));

    if (instanceId == null) {
      return allOf(partial, hasData(allOf(hasHost(host), hasPath(path))));
    } else {
      return allOf(partial, hasData(scheme + host + path + "#instanceId=" + instanceId));
    }
  }

  public static Matcher<View> withPref(@StringRes int titleId, String summary) {
    Espresso
        .onView(ViewMatchers.isAssignableFrom(RecyclerView.class))
        .perform(RecyclerViewActions.scrollTo(allOf(
            ViewMatchers.hasDescendant(ViewMatchers.withText(summary)),
            ViewMatchers.hasDescendant(ViewMatchers.withText(titleId))
        )));

    return allOf(
        ViewMatchers.withParent(ViewMatchers.withChild(ViewMatchers.withText(titleId))),
        ViewMatchers.withId(android.R.id.summary),
        ViewMatchers.withText(summary)
    );
  }
}