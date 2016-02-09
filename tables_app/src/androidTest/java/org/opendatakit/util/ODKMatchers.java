package org.opendatakit.util;

import android.support.test.espresso.matcher.BoundedMatcher;
import android.view.View;
import android.widget.ListView;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
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

  public static Matcher<TableNameStruct> withTable(final String tableId, final String displayName) {
    return new TypeSafeMatcher<TableNameStruct>() {
      private TableNameStruct tableName;

      @Override
      protected boolean matchesSafely(TableNameStruct item) {
        this.tableName = item;

        return item.getTableId().equals(tableId)
            && item.getLocalizedDisplayName().equals(displayName);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Table Id should be: " + tableName.getTableId());
        description.appendText("; Got: " + tableId);
        description.appendText("; Localized display name should be: "
            + tableName.getLocalizedDisplayName());
        description.appendText("; Got: " + displayName);
      }
    };
  }
}
