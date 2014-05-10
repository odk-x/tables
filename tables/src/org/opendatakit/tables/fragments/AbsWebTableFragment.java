package org.opendatakit.tables.fragments;

import java.lang.ref.WeakReference;

import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.IntentUtil;
import org.opendatakit.tables.utils.WebViewUtil;
import org.opendatakit.tables.views.webkits.Control;
import org.opendatakit.tables.views.webkits.ControlIf;
import org.opendatakit.tables.views.webkits.TableData;
import org.opendatakit.tables.views.webkits.TableDataIf;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

/**
 * Base class for {@link Fragment}s that display information about a table
 * using a {@link CustomView}.
 * @author sudar.sam@gmail.com
 *
 */
public abstract class AbsWebTableFragment extends AbsTableDisplayFragment
    implements IWebFragment {

  private static final String TAG = AbsWebTableFragment.class.getSimpleName();
  /**
   * The {@link Control} object that was used to generate the
   * {@link ControlIf} that was passed to the {@link WebView}. This reference
   * must be saved to prevent garbage collection of the {@link WeakReference}
   * in {@link ControlIf}.
   */
  protected Control mControlReference;
  /**
   * The {@link TableData} object that was used to generate the
   * {@link TableDataIf} that was passed to the {@link WebView}. This reference
   * must be saved to prevent garbage collection of the {@link WeakReference}
   * in {@link TableDataIf}.
   */
  protected TableData mTableDataReference;

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
    Log.d(TAG, "[onCreate]");
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
    Log.d(TAG, "[onCreateView]");
    WebView webView = this.buildView();
    return webView;
  }


  /**
   * @see IWebFragment#createControlObject()
   */
  @Override
  public Control createControlObject() {
    Control result = new Control(getActivity(), getAppName());
    return result;
  }

  /**
   * Create a {@link TableData} object that can be added toe the webview.
   * @return
   */
  protected abstract TableData createDataObject();

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
  public WebView buildView() {
    WebView result = WebViewUtil.getODKCompliantWebView(getActivity());
    return result;
  }

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
