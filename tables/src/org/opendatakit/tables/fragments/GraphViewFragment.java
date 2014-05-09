package org.opendatakit.tables.fragments;

import java.io.File;

import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.activities.TableDisplayActivity.ViewFragmentType;
import org.opendatakit.tables.utils.Constants;
import org.opendatakit.tables.utils.TableFileUtils;
import org.opendatakit.tables.utils.WebViewUtil;
import org.opendatakit.tables.views.webkits.Control;
import org.opendatakit.tables.views.webkits.GraphData;
import org.opendatakit.tables.views.webkits.TableData;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

public class GraphViewFragment extends AbsWebTableFragment {
  
  private static final String TAG = GraphViewFragment.class.getSimpleName();
  
  private String mGraphName;
  
  /** A strong reference that must be kept or else nulls will be thrown. */
  protected GraphData mGraphDataStrongReference;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    String graphName =
        this.getArguments().getString(Constants.IntentKeys.GRAPH_NAME);
    if (graphName == null) {
      Log.e(TAG, "[onCreate] graphName was null!");
    }
    this.mGraphName = graphName;
  }
  
  /**
   * Return the name of the graph that is being displayed.
   * @return
   */
  String getGraphName() {
    return this.mGraphName;
  }
  
  @Override
  public WebView buildView() {
    WebView result = WebViewUtil.getODKCompliantWebView(getActivity());
    Control control = this.createControlObject();
    result.addJavascriptInterface(
        control.getJavascriptInterfaceWithWeakReference(),
        Constants.JavaScriptHandles.CONTROL);
    TableData tableData = this.createDataObject();
    result.addJavascriptInterface(
        tableData.getJavascriptInterfaceWithWeakReference(),
        Constants.JavaScriptHandles.CONTROL);
    String relativePathToGraphFile =
        TableFileUtils.getRelativePathToGraphFile(getAppName());
    String relativePath = ODKFileUtils.asUriFragment(
        getAppName(),
        new File(relativePathToGraphFile));
    GraphData graphData =
        new GraphData(getTableProperties(), this.getGraphName());
    result.addJavascriptInterface(
        graphData.getJavascriptInterfaceWithWeakReference(), 
        Constants.JavaScriptHandles.GRAPH);
    WebViewUtil.displayFileInWebView(
        getActivity(),
        getAppName(),
        result,
        relativePath);
    // Save the references
    this.mControlReference = control;
    this.mTableDataReference = tableData;
    this.mGraphDataStrongReference = graphData;
    return result;
  }

  @Override
  protected TableData createDataObject() {
    // Graph view displays everything.
    TableData result = new TableData(getUserTable());
    return result;
  }

  @Override
  public ViewFragmentType getFragmentType() {
    // TODO Auto-generated method stub
    return null;
  }

}
