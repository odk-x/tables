package org.opendatakit.testutils;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import java.util.Set;

import org.opendatakit.common.android.data.PossibleTableViewTypes;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.tables.utils.SQLQueryStruct;
import org.opendatakit.tables.views.webkits.CustomView;

/**
 * Constants for use in tests.
 * @author sudar.sam@gmail.com
 *
 */
public class TestConstants {

  public static boolean FORM_IS_USER_DEFINED = false;
  public static String FORM_ID = "testFormId";
  public static String FORM_VERSION = "testFormVersion";
  public static String ROOT_ELEMENT = "testRootElement";
  public static String ROW_NAME = "testRowName";
  public static String SCREEN_PATH = "?testKey=testValue";

  public static String DEFAULT_SQL_WHERE_CALUSE = "dudeWhereIsMyCar";
  public static String[] DEFAULT_SQL_SELECTION_ARGS =
      new String[] { "one", "two" };
  public static String[] DEFAULT_SQL_GROUP_BY =
      new String[] { "group one", "group two" };
  public static String DEFAULT_SQL_HAVING = "anyOldHaving";
  public static String DEFAULT_SQL_ORDER_BY_ELEMENT_KEY =
      "elementKeyByWhichToOrder";
  public static String DEFAULT_SQL_ORDER_BY_DIRECTION =
      "directionByWhichToOrder";

  public static final String DEFAULT_FRAGMENT_TAG = "testFragmentTag";

  public static final int DEFAULT_FRAGMENT_ID = 12345;

  public static final String DEFAULT_FILE_NAME = "test/File/Name";

  public static final String DEFAULT_ROW_ID = "testRowId";

  /**
   * The default app name for tables. Using this rather than the
   * getDefaultAppName method because that dumps the stack trace.
   */
  public static final String TABLES_DEFAULT_APP_NAME = "tables";

  public static final String DEFAULT_TABLE_ID = "testTableId";

  /**
   * Return an unimplemented mock of {@link TableProperties}.
   */
  public static final TableProperties TABLE_PROPERTIES_MOCK =
      mock(TableProperties.class);

  public static TableProperties getTablePropertiesMock() {
    TableProperties tpMock = mock(TableProperties.class);
    doReturn(getAllValidPossibleTableViewTypes())
        .when(tpMock).getPossibleViewTypes();
    doReturn(TableViewType.SPREADSHEET)
        .when(tpMock).getDefaultViewType();
    return tpMock;
  }

  public static SQLQueryStruct getSQLQueryStructMock() {
    SQLQueryStruct mock = mock(SQLQueryStruct.class);
    return mock;
  }

  public static UserTable getUserTableMock() {
    UserTable mock = mock(UserTable.class);
    return mock;
  }

  public static PossibleTableViewTypes getAllValidPossibleTableViewTypes() {
    PossibleTableViewTypes allValid = mock(PossibleTableViewTypes.class);
    doReturn(true).when(allValid).spreadsheetViewIsPossible();
    doReturn(true).when(allValid).listViewIsPossible();
    doReturn(true).when(allValid).mapViewIsPossible();
    doReturn(true).when(allValid).graphViewIsPossible();
    Set<TableViewType> allViewTypes = new HashSet<TableViewType>();
    allViewTypes.add(TableViewType.SPREADSHEET);
    allViewTypes.add(TableViewType.LIST);
    allViewTypes.add(TableViewType.MAP);
    allViewTypes.add(TableViewType.GRAPH);
    doReturn(allViewTypes).when(allValid).getAllPossibleViewTypes();
    return allValid;
  }

  public static CustomView getCustomViewMock() {
    return mock(CustomView.class);
  }
}
