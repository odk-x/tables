package org.opendatakit.tables.views.components;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.shadowOf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.common.android.data.ElementDataType;
import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.common.android.utilities.TableUtil;
import org.opendatakit.tables.R;
import org.opendatakit.tables.utils.TableNameStruct;
import org.opendatakit.testutils.TestConstants;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowDrawable;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
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
    
    TableNameStruct structOne = new TableNameStruct(
        "first",
        "first_name");
    
    TableNameStruct structTwo = new TableNameStruct(
        "second",
        "second_name");
    
    this.mTableNameStructs = new ArrayList<TableNameStruct>();
    
    this.mTableNameStructs.add(structOne);
    this.mTableNameStructs.add(structTwo);
    
//    List<String> tableIds = new ArrayList<String>();
//    String tableId1 = "alpha";
//    String tableId2 = "beta";
//    tableIds.add(tableId1);
//    tableIds.add(tableId2);
//    doReturn(tableIds).when(wrapperMock).getAllTableIds(any(SQLiteDatabase.class));
//    
//    String elementKey = "anyElementKey";
//    
//    List<Column> columns1 = new ArrayList<Column>();
//    columns1.add(new Column(TestConstants.ElementKeys.STRING_COLUMN,TestConstants.ElementKeys.STRING_COLUMN,
//        ElementDataType.string.name(), "[]"));
//    columns1.add(new Column(TestConstants.ElementKeys.INT_COLUMN, TestConstants.ElementKeys.INT_COLUMN,
//        ElementDataType.integer.name(), "[]"));
//    columns1.add(new Column(TestConstants.ElementKeys.NUMBER_COLUMN, TestConstants.ElementKeys.NUMBER_COLUMN,
//        ElementDataType.number.name(), "[]"));
//
//    List<Column> columns2 = new ArrayList<Column>();
//    columns2.add(new Column(
//        TestConstants.ElementKeys.STRING_COLUMN,
//        TestConstants.ElementKeys.STRING_COLUMN,
//        ElementDataType.string.name(),
//        "[]"));
//    columns2.add(new Column(
//        TestConstants.ElementKeys.GEOPOINT_COLUMN,
//        TestConstants.ElementKeys.GEOPOINT_COLUMN,
//        ElementDataType.string.name(),
//        "[\"" +
//            TestConstants.ElementKeys.LATITUDE_COLUMN + "\",\"" +
//            TestConstants.ElementKeys.LONGITUDE_COLUMN + "\",\"" +
//            TestConstants.ElementKeys.ALTITUDE_COLUMN + "\",\"" + 
//            TestConstants.ElementKeys.ACCURACY_COLUMN + "\"]"));
//    columns2.add(new Column(
//        TestConstants.ElementKeys.LATITUDE_COLUMN,
//        "latitude",
//        ElementDataType.number.name(),
//        "[]"));
//    columns2.add(new Column(
//        TestConstants.ElementKeys.LONGITUDE_COLUMN,
//        "longitude",
//        ElementDataType.number.name(),
//        "[]"));
//    columns2.add(new Column(TestConstants.ElementKeys.ALTITUDE_COLUMN, "altitude",
//        ElementDataType.number.name(), "[]"));
//    columns2.add(new Column(TestConstants.ElementKeys.ACCURACY_COLUMN, "accuracy",
//        ElementDataType.number.name(), "[]"));
//    
//    doReturn(columns1).when(wrapperMock).getUserDefinedColumns(any(SQLiteDatabase.class), eq(tableId1));
//    doReturn(columns2).when(wrapperMock).getUserDefinedColumns(any(SQLiteDatabase.class), eq(tableId2));
//    ODKDatabaseUtils.set(wrapperMock);
//    
//    TableUtil util = mock(TableUtil.class);
//    when(util.getLocalizedDisplayName(
//        any(SQLiteDatabase.class),
//        eq(tableId1)))
//      .thenReturn(tableId1+"_name");
//    when(util.getLocalizedDisplayName(
//        any(SQLiteDatabase.class),
//        eq(tableId2)))
//      .thenReturn(tableId2+"_name");
//    TableUtil.set(util);

//    List<String> listOfMocks = new ArrayList<String>();
//    listOfMocks.add(tableId1);
//    listOfMocks.add(tableId2);
    this.mAdapter = new TableNameStructAdapter(
        null,
        "tables",
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
