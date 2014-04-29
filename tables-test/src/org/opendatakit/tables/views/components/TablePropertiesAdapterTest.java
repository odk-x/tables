package org.opendatakit.tables.views.components;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.shadowOf;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.R;
import org.opendatakit.tables.views.components.TablePropertiesAdapter;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowDrawable;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

@RunWith(RobolectricTestRunner.class)
public class TablePropertiesAdapterTest {
  
  private TablePropertiesAdapter mAdapter;
  
  @Before
  public void setup() {
    TableProperties tp1 = mock(TableProperties.class);
    TableProperties tp2 = mock(TableProperties.class);
    when(tp1.getDisplayName()).thenReturn("alpha");
    when(tp2.getDisplayName()).thenReturn("beta");
    this.mAdapter = new TablePropertiesAdapter(
        Robolectric.application,
        R.layout.row_item_with_preference,
        new TableProperties[] {
            tp1,
            tp2
        });
  }
  
  @Test
  public void testGetCount() {
    assertThat(mAdapter).hasCount(2);
  }
  
  @Test
  public void getView_firstItemCorrectText() {
    View view = this.getView(0, null);
    assertThat(view)
      .isNotNull()
      .isVisible();
    TextView textView = (TextView) view.findViewById(R.id.row_item_text);
    assertEquals("alpha", textView.getText().toString());
  }
  
  @Test
  public void getView_secondItemCorrectText() {
    View view = this.getView(1, null);
    assertThat(view)
      .isNotNull()
      .isVisible();
    TextView textView = (TextView) view.findViewById(R.id.row_item_text);
    assertEquals("beta", textView.getText().toString());
  }
  
  @Test
  public void getView_shouldRecycleViews() {
    LayoutInflater layoutInflater = (LayoutInflater) Robolectric
        .application
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    RelativeLayout existingView = (RelativeLayout)
        layoutInflater.inflate(R.layout.row_item_with_preference,
        null); 
    View view = this.getView(0, existingView);
    assertThat(view).isSameAs(existingView);
  }
  
  @Test
  public void getView_correctDrawableResource() {
    View view = this.getView(0, null);
    ImageView imageView = (ImageView) view.findViewById(R.id.row_item_icon);
    ShadowDrawable shadow = shadowOf(imageView.getDrawable());
    org.fest.assertions.api.Assertions.assertThat(
            shadow.getCreatedFromResId())
        .isEqualTo(R.drawable.ic_menu_preferences);
  }
  
  @Test
  public void getView_iconIsPresentAndVisible() {
    View view = this.getView(0, null);
    ImageView icon = (ImageView) view.findViewById(R.id.row_item_icon);
    assertThat(icon)
      .isNotNull()
      .isVisible();
  }
  
  /**
   * Retrieve a view from the {@link #mAdapter} from the given position with
   * the given existingView being recycled. The root view group passed in is
   * a {@link LinearLayout}.
   * @param position
   * @param existingView
   * @return
   */
  private View getView(int position, View existingView) {
    View result = this.mAdapter.getView(
        position,
        existingView,
        new LinearLayout(Robolectric.application));
    return result;
  }

}
