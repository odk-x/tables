package org.opendatakit.tables.fragments;
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
  
  @Override
  PossibleTableViewTypes getPossibleViewTypes() {
    System.out.println("calling the stub's getPossibleViewTypes");
    return POSSIBLE_VIEW_TYPES;
  }
  
  public static void resetState() {
    POSSIBLE_VIEW_TYPES = DEFAULT_POSSIBLE_VIEW_TYPES;
  }
  
}
