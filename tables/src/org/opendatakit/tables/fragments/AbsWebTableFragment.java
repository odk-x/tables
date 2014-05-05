package org.opendatakit.tables.fragments;

import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.views.webkits.CustomView;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Base class for {@link Fragment}s that display information about a table
 * using a {@link CustomView}.
 * @author sudar.sam@gmail.com
 *
 */
public abstract class AbsWebTableFragment extends AbsTableDisplayFragment 
    implements IWebFragment {
  
  /** The file name this fragment is displaying. */
  protected String mFileName;
  
  /**
   * Retrieve the file name that should be displayed.
   * @return the file name, or null if one has not been set.
   */
  @Override
  public String retrieveFileNameFromBundle(Bundle bundle) {
    String fileName = IntentUtil.retrieveFileNameFromBundle(bundle);
    return fileName;
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Get the file name if it was there.
    String retrievedFileName = retrieveFileNameFromBundle(savedInstanceState);
    if (retrievedFileName == null) {
      // then try to get it from its arguments.
      retrievedFileName = this.retrieveFileNameFromBundle(this.getArguments());
    }
    this.mFileName = retrievedFileName;
  }
  
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    return this.buildView();
  }
  
  @Override
  public void putFileNameInBundle(Bundle bundle) {
    if (this.getFileName() != null) {
      bundle.putString(Constants.IntentKeys.FILE_NAME, this.getFileName());
    }
  }
  
  /**
   * Build the {@link CustomView} that will be displayed by the fragment.
   * @return
   */
  @Override
  public abstract CustomView buildView();
  
  /**
   * Get the file name this fragment is displaying.
   */
  @Override
  public String getFileName() {
    return this.mFileName;
  }
  
  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    this.putFileNameInBundle(outState);
  }
  
}
