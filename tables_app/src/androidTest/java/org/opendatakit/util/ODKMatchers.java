package org.opendatakit.util;

import android.support.test.espresso.matcher.BoundedMatcher;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ListView;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.opendatakit.common.android.data.ColorRule;
import org.opendatakit.tables.utils.TableNameStruct;

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
}
