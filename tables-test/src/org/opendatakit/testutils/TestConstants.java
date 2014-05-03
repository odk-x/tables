package org.opendatakit.testutils;

import static org.mockito.Mockito.mock;

import org.opendatakit.common.android.data.TableProperties;

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
  
  public static final String DEFAULT_FRAGMENT_TAG = "testFragmentTag";
  
  public static final int DEFAULT_FRAGMENT_ID = 12345;
  
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
}
