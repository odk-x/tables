package org.opendatakit.tables.fragments;

import org.opendatakit.common.android.utilities.WebLogger;
import org.opendatakit.tables.activities.AbsBaseActivity;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.WebViewUtil;
import org.opendatakit.tables.views.webkits.Control;
import org.opendatakit.tables.views.webkits.TableData;

import android.app.Fragment;
import android.webkit.WebView;

/**
 * {@link Fragment} for displaying a List view.
 * @author sudar.sam@gmail.com
 *
 */
public class ListViewFragment extends AbsWebTableFragment {
  
  private static final String TAG = ListViewFragment.class.getSimpleName();
  
  @Override
  public void onDestroy() {
    super.onDestroy();
    WebLogger.getLogger(getAppName()).d(TAG, "[onDestroy]");
  }

  @Override
  public WebView buildView() {
    WebLogger.getLogger(getAppName()).d(TAG, "[buildView]");
    WebView result = WebViewUtil.getODKCompliantWebView((AbsBaseActivity) getActivity());
    Control control = this.createControlObject();
    result.addJavascriptInterface(
        control.getJavascriptInterfaceWithWeakReference(),
        Constants.JavaScriptHandles.CONTROL);
    TableData tableData = this.createDataObject();
    result.addJavascriptInterface(
        tableData.getJavascriptInterfaceWithWeakReference(),
        Constants.JavaScriptHandles.DATA);
    WebViewUtil.displayFileInWebView(
        getActivity(),
        getAppName(),
        result,
        getFileName());
    // Now save the references.
    this.mControlReference = control;
    this.mTableDataReference = tableData;
    return result;
  }

  @Override
  public ViewFragmentType getFragmentType() {
    return ViewFragmentType.LIST;
  }

  @Override
  protected TableData createDataObject() {
    TableData result = new TableData(getUserTable());
    return result;
  }

}
