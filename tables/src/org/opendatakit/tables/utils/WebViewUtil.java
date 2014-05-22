/*
 * Copyright (C) 2012 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.tables.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.type.TypeReference;
import org.opendatakit.common.android.data.DbTable;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.UserTable;
import org.opendatakit.common.android.data.UserTable.Row;
import org.opendatakit.common.android.provider.DataTableColumns;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.common.android.utilities.UrlUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewUtil {

  private static final String TAG = WebViewUtil.class.getSimpleName();

  /**
   * A {@link TypeReference} for a {@link HashMap} parameterized for String
   * keys and String values.
   */
  private static final TypeReference<HashMap<String, String>> MAP_REF =
      new TypeReference<HashMap<String, String>>() {};

  /**
   * The HTML to be displayed when loading a screen.
   */
  public static final String LOADING_HTML_MESSAGE =
      "<html><body><p>Loading, please wait...</p></body></html>";

  /**
   * Retrieve a map from a simple json map that has been stringified.
   *
   * @param jsonMap
   * @return null if the mapping fails, else the map
   */
  public static Map<String, String> getMapFromJson(String jsonMap) {
    Map<String, String> map = null;
    try {
      map = ODKFileUtils.mapper.readValue(jsonMap, MAP_REF);
    } catch (JsonParseException e) {
      e.printStackTrace();
    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return map;
  }

  /**
   * Get a {@link WebView} that is ready to be used for ODK settings. This
   * includes, e.g., having attached a logger and enabling javascript.
   * @return
   */
  @SuppressLint("SetJavaScriptEnabled")
  public static WebView getODKCompliantWebView(Context context) {
    WebView result = new WebView(context);
    final String webViewTag = "ODKCompliantWebView";
    result.getSettings().setJavaScriptEnabled(true);
    result.setWebViewClient(new WebViewClient() {

        @Override
        public void onReceivedError(
            WebView view,
            int errorCode,
            String description,
            String failingUrl) {
          super.onReceivedError(view, errorCode, description, failingUrl);
          Log.e(
              webViewTag,
              "[onReceivedError] errorCode: " +
              errorCode +
              "; description: "
              + description +
              "; failingUrl: " +
              failingUrl);
        }
    });
    result.setWebChromeClient(new WebChromeClient() {

      @Override
      public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        Log.i(
            webViewTag,
            "[onConsoleMessage] level: " +
            consoleMessage.messageLevel().name() +
            consoleMessage.message());
        return super.onConsoleMessage(consoleMessage);
      }

    });
    return result;
  }

  /**
   * Display the file in the WebView.
   * @param context
   * @param appName
   * @param webView the WebView in which the file should be displayed
   * @param fileName the relativePath to the file. If null, a no file specified
   * message is displayed.
   */
  public static void displayFileInWebView(
      Context context,
      String appName,
      WebView webView,
      String fileName) {
    if (fileName != null) {
      String webUrl = UrlUtils.getAsWebViewUri(
          context,
          appName,
          fileName);
      webView.loadUrl(webUrl);
    } else {
      // load the no file found html.
      webView.loadData(
          Constants.HTML.NO_FILE_NAME,
          Constants.MimeTypes.TEXT_HTML,
          null);
    }
  }

  /**
   * Retrieve a map of element key to value for each of the columns in the row
   * specified by rowId.
   * @param tableProperties
   * @param rowId
   * @return
   */
  public static Map<String, String> getMapOfElementKeyToValue(
      TableProperties tableProperties,
      String rowId) {
    String sqlQuery = DataTableColumns.ID + " = ? ";
    String[] selectionArgs = { rowId };
    DbTable dbTable = DbTable.getDbTable(tableProperties);
    UserTable userTable = dbTable.rawSqlQuery(
        sqlQuery,
        selectionArgs,
        null,
        null,
        null,
        null);
    if (userTable.getNumberOfRows() > 1) {
      Log.e(TAG, "query returned > 1 rows for tableId: " +
          tableProperties.getTableId() +
          " and " +
          "rowId: " +
          rowId);
    } else if (userTable.getNumberOfRows() == 0) {
      Log.e(TAG, "query returned no rows for tableId: " +
          tableProperties.getTableId() +
          " and rowId: " +
          rowId);
    }
    Map<String, String> elementKeyToValue = new HashMap<String, String>();
    Row requestedRow = userTable.getRowAtIndex(0);
    List<String> userDefinedElementKeys = tableProperties.getPersistedColumns();
    List<String> adminElementKeys = DbTable.getAdminColumns();
    List<String> allElementKeys = new ArrayList<String>();
    allElementKeys.addAll(userDefinedElementKeys);
    allElementKeys.addAll(adminElementKeys);
    for (String elementKey : allElementKeys) {
      elementKeyToValue.put(
          elementKey,
          requestedRow.getDataOrMetadataByElementKey(elementKey));
    }
    return elementKeyToValue;
  }

}
