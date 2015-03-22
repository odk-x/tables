package org.opendatakit.tables.views.components;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.junit.Assert.assertEquals;
import static org.robolectric.Robolectric.shadowOf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.application.CommonApplication;
import org.opendatakit.tables.R;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.AbsBaseActivityStub;
import org.opendatakit.tables.utils.TableNameStruct;
import org.opendatakit.testutils.ODKFragmentTestUtil;
import org.opendatakit.testutils.TestCaseUtils;
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
public class TableNameStructAdapterTest {

  private TableNameStructAdapter mAdapter;
  private List<TableNameStruct> mTableNameStructs;

  @Before
  public void setup() {
    
//    SQLiteDatabase stubDb = SQLiteDatabase.create(null);
//    DatabaseFactory factoryMock = mock(DatabaseFactory.class);
//    doReturn(stubDb).when(factoryMock).getDatabase(any(Context.class), any(String.class));
//    DatabaseFactory.set(factoryMock);
    
//    ODKDatabaseUtils wrapperMock = mock(ODKDatabaseUtils.class);
    CommonApplication.setMocked();
    TestCaseUtils.setExternalStorageMounted();

    TableNameStruct structOne = new TableNameStruct(
        "first",
        "first_name");
    
    TableNameStruct structTwo = new TableNameStruct(
        "second",
        "second_name");
    
    this.mTableNameStructs = new ArrayList<TableNameStruct>();
    
    this.mTableNameStructs.add(structOne);
    this.mTableNameStructs.add(structTwo);

    AbsBaseActivity activity = ODKFragmentTestUtil.startActivityAttachFragment(
        AbsBaseActivityStub.class,
        null, null, null);
    
    this.mAdapter = new TableNameStructAdapter(activity,
        this.mTableNameStructs);
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
    assertEquals("first_name", textView.getText().toString());
  }

  @Test
  public void getView_secondItemCorrectText() {
    View view = this.getView(1, null);
    assertThat(view)
      .isNotNull()
      .isVisible();
    TextView textView = (TextView) view.findViewById(R.id.row_item_text);
    assertEquals("second_name", textView.getText().toString());
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
  public void getView_iconClickPerformsLongClickOnParent() {
    View view = this.getView(1, null);
    ImageView icon = (ImageView) view.findViewById(R.id.row_item_icon);
    // We're going to test that this works just by adding a longClick listener.
    // Because we have to use final, we can't do a simple ++. Going to modify
    // an object.
    final String key = "key";
    final Map<String, Integer> buttonClicks = new HashMap<String, Integer>();
    buttonClicks.put(key, 0);
    view.setOnLongClickListener(new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View arg0) {
        int oldValue = buttonClicks.get(key);
        int newValue = oldValue + 1;
        buttonClicks.put(key, newValue);
        return true;
      }
    });
    // Before the click, it should be 0.
    assertEquals((Integer) 0, buttonClicks.get(key));
    // Do the click.
    icon.performClick();
    assertEquals((Integer) 1, buttonClicks.get(key));
  }

  @Test
  public void getView_correctDrawableResource() {
    View view = this.getView(0, null);
    ImageView imageView = (ImageView) view.findViewById(R.id.row_item_icon);
    ShadowDrawable shadow = shadowOf(imageView.getDrawable());
    org.fest.assertions.api.Assertions.assertThat(
            shadow.getCreatedFromResId())
        .isEqualTo(R.drawable.ic_action_settings);
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
