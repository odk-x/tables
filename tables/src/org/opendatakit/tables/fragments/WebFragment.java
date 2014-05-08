package org.opendatakit.tables.fragments;

import java.lang.ref.WeakReference;

import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.CustomViewUtil;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.views.webkits.Control;
import org.opendatakit.tables.views.webkits.ControlIf;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

/**
 * Displays an HTML file that is not associated with a particular table.
 * Consequently it does not add a data JavaScript interface to its 
 * {@link WebView}. To display data about a table, see 
 * {@link AbsWebTableFragment} and its subclasses.
 * @author sudar.sam@gmail.com
 *
 */
public class WebFragment extends Fragment implements IWebFragment {
  
  private static final String TAG = WebFragment.class.getSimpleName();
  
  /** The name of the file this fragment is displaying. */
  protected String mFileName;
  
  /** The {@link Control} object that was jused to generate the
   * {@link ControlIf} that was passed to the {@link WebView}. This reference
   * must be saved to prevent garbage collection of the {@link WeakReference}
   * in {@link ControlIf}.
   */
  protected Control mControlReference;

  @Override
  public String retrieveFileNameFromBundle(Bundle bundle) {
    String fileName = IntentUtil.retrieveFileNameFromBundle(bundle);
    return fileName;
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "[onCreate]");
    // Get the file name. Saved state gets precedence. Then arguments.
    String retrievedFileName = retrieveFileNameFromBundle(savedInstanceState);
    if (retrievedFileName == null) {
      retrievedFileName = this.retrieveFileNameFromBundle(this.getArguments());
    }
    this.mFileName = retrievedFileName;
  }
  
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    Log.d(TAG, "[onCreateView] activity is: " + this.getActivity());
    WebView webView = this.buildView();
    return webView;
  }

  @Override
  public void putFileNameInBundle(Bundle bundle) {
    if (this.getFileName() != null) {
      bundle.putString(Constants.IntentKeys.FILE_NAME, this.getFileName());
    }
  }

  @Override
  public WebView buildView() {
    Log.d(TAG, "[buildView] activity is: " + this.getActivity());
    WebView result = CustomViewUtil.getODKCompliantWebView(getActivity());
    Control control = this.createControlObject();
    this.mControlReference = control;
    result.addJavascriptInterface(
        control.getJavascriptInterfaceWithWeakReference(),
        Constants.JavaScriptHandles.CONTROL);
    return result;
  }

  @Override
  public String getFileName() {
    return this.mFileName;
  }

  /**
   * @see IWebFragment#createControlObject()
   */
  @Override
  public Control createControlObject() {
    AbsBaseActivity activity = (AbsBaseActivity) getActivity();
    Control result = new Control(activity);
    return result;
  }
  
}
