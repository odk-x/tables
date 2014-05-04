package org.opendatakit.tables.fragments;

import static org.mockito.Mockito.mock;

import org.opendatakit.common.android.data.PossibleTableViewTypes;
import org.opendatakit.tables.fragments.TopLevelTableMenuFragment;

/**
 * Stub.
 * @author sudar.sam@gmail.com
 *
 */
public class TopLevelTableMenuFragmentStub extends TopLevelTableMenuFragment {
  
  private static final PossibleTableViewTypes DEFAULT_POSSIBLE_VIEW_TYPES =
      new PossibleTableViewTypes(true, true, true, true);
  
  public static PossibleTableViewTypes POSSIBLE_VIEW_TYPES =
      DEFAULT_POSSIBLE_VIEW_TYPES;
  
  public static ITopLevelTableMenuActivity MENU_ACTIVITY_IMPL =
      mock(ITopLevelTableMenuActivity.class);
  
  @Override
  PossibleTableViewTypes getPossibleViewTypes() {
    System.out.println("calling the stub's getPossibleViewTypes");
    return POSSIBLE_VIEW_TYPES;
  }
  
  public static void resetState() {
    POSSIBLE_VIEW_TYPES = DEFAULT_POSSIBLE_VIEW_TYPES;
    MENU_ACTIVITY_IMPL = mock(ITopLevelTableMenuActivity.class);
  }
  
  @Override
  ITopLevelTableMenuActivity retrieveInterfaceImpl() {
    return MENU_ACTIVITY_IMPL;
  }
  
}
