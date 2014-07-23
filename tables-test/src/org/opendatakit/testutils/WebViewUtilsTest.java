package org.opendatakit.testutils;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.android.data.ColumnProperties;
import org.opendatakit.common.android.data.ColumnType;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.utils.WebViewUtil;
import org.opendatakit.tables.views.webkits.Control;
import org.robolectric.RobolectricTestRunner;

import android.content.ContentValues;

/**
 *
 * @author sudar.sam@gmail.com
 *
 */
@RunWith(RobolectricTestRunner.class)
public class WebViewUtilsTest {

  @Test
  public void getContentValuesInvalidIntFails() {
    this.assertInvalidHelper(ColumnType.INTEGER, "invalid");
  }

  @Test
  public void getContentValuesInvalidNumberFails() {
    this.assertInvalidHelper(ColumnType.NUMBER, "invalid");
  }

  /**
   * Perform an assertion for an invalid value for the given column type,
   * ensuring that the insertion fails.
   * @param elementKey
   * @param columnType
   * @param invalidValue
   * @param rowId if null, calls {@link Control#addRow(String, String)}.
   * Otherwise it calls {@link Control#updateRow(String, String, String)}.
   */
  private void assertInvalidHelper(
      ColumnType columnType,
      String invalidValue) {
    String elementKey = "anyElementKey";
    TableProperties tpMock = mock(TableProperties.class);
    ColumnProperties intColumn = TestConstants.getColumnPropertiesMock(
        elementKey,
        columnType);
    doReturn(intColumn).when(tpMock).getColumnByElementKey(
        elementKey);
    Map<String, String> invalidMap = new HashMap<String, String>();
    invalidMap.put(elementKey, invalidValue);
    ContentValues contentValues = WebViewUtil.getContentValuesFromMap(
        tpMock,
        invalidMap);
    assertThat(contentValues).isNull();
  }

}
