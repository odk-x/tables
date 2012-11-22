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
package org.opendatakit.tables.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;

import android.graphics.Color;

public class TableViewSettings {
  private static final ObjectMapper mapper = new ObjectMapper();

  enum ViewType {
    OVERVIEW_VIEW, COLLECTION_VIEW
  }

  public class Type {
    public static final int SPREADSHEET = 0;
    public static final int LIST = 1;
    public static final int LINE_GRAPH = 2;
    public static final int BOX_STEM = 3;
    public static final int BAR_GRAPH = 4;
    public static final int MAP = 5;
    public static final int COUNT = 6;

    private Type() {
    }
  }

  public static final int[] MAP_COLOR_OPTIONS = { Color.BLACK, Color.BLUE, Color.GREEN, Color.RED,
      Color.YELLOW };

  private static final String JSON_KEY_VIEW_TYPE = "viewType";
  private static final String JSON_KEY_TABLE_SETTINGS = "table";
  private static final String JSON_KEY_TABLE_COL_WIDTHS = "tableColWidths";
  private static final String JSON_KEY_TABLE_INDEXED_COL = "tableIndexedCol";
  private static final String JSON_KEY_LIST_SETTINGS = "list";
  private static final String JSON_KEY_LIST_FORMAT = "listFormat";
  private static final String JSON_KEY_LINE_SETTINGS = "line";
  private static final String JSON_KEY_LINE_X_COL = "lineX";
  private static final String JSON_KEY_LINE_Y_COL = "lineY";
  private static final String JSON_KEY_BAR_SETTINGS = "bar";
  private static final String JSON_KEY_BAR_X_COL = "barX";
  private static final String JSON_KEY_BAR_Y_COL = "barY";
  private static final String JSON_KEY_BOX_STEM_SETTINGS = "boxStem";
  private static final String JSON_KEY_BOX_STEM_X_COL = "boxStemX";
  private static final String JSON_KEY_BOX_STEM_Y_COL = "boxStemY";
  private static final String JSON_KEY_MAP_SETTINGS = "map";
  private static final String JSON_KEY_MAP_LOC_COL = "mapLocCol";
  private static final String JSON_KEY_MAP_LABEL_COL = "mapLabelCol";
  private static final String JSON_KEY_MAP_COLOR_RULERS = "mapColorRulers";
  private static final String JSON_KEY_MAP_SIZE_RULERS = "mapSizeRulers";
  private static final String JSON_KEY_CUSTOM_SETTINGS = "custom";
  private static final String JSON_KEY_CUSTOM_LIST_FILE = "customListFile";
  private static final String JSON_KEY_CUSTOM_DETAIL_FILE = "customDetailFile";
  private static final String JSON_KEY_CUSTOM_VIEWS_INFO = "customViewsInfo";

  private static final int DEFAULT_TABLE_COL_WIDTH = 125;

  private final TableProperties tp;
  private final ViewType type;
  private int viewType;
  private int[] tableColWidths;
  private String tableIndexedCol;
  private String listFormat;
  private String lineXCol;
  private String lineYCol;
  private String barXCol;
  private String barYCol;
  private String boxStemXCol;
  private String boxStemYCol;
  private String mapLocCol;
  private String mapLabelCol;
  private Map<String, ConditionalRuler> mapColorRulers;
  private Map<String, ConditionalRuler> mapSizeRulers;
  // I am adding custom detail name as well. Both of these should point to
  // filenames on the system that have the information to display the
  // appropriate views (list or detail). It is not clear to me what the
  // listFormat String up there is doing.
  private String customListFilename;
  private String customDetailFilename;
  private List<CustomView> customViews;

  private TableViewSettings(TableProperties tp, ViewType type, String dbString) {
    this.tp = tp;
    this.type = type;
    mapColorRulers = new HashMap<String, ConditionalRuler>();
    mapSizeRulers = new HashMap<String, ConditionalRuler>();
    customViews = new ArrayList<CustomView>();
    if (dbString == null) {
      viewType = Type.SPREADSHEET;
      return;
    }
    try {
      Map<String, Object> dbObject = mapper.readValue(dbString, Map.class);
      setFromJsonObject(dbObject);
    } catch (JSONException e) {
      throw new RuntimeException(e);
    } catch (JsonParseException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("invalid db value: " + dbString);
    } catch (JsonMappingException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("invalid db value: " + dbString);
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("invalid db value: " + dbString);
    }
  }

  void setFromJsonObject(Map<String, Object> jo) throws JSONException {
    if (jo == null) {
      viewType = Type.SPREADSHEET;
      return;
    }
    if (!jo.containsKey(JSON_KEY_VIEW_TYPE)) {
      viewType = Type.SPREADSHEET;
      return;
    }
    viewType = (Integer) jo.get(JSON_KEY_VIEW_TYPE);
    if (jo.containsKey(JSON_KEY_TABLE_SETTINGS)) {
      setTableFromJsonObject((Map<String, Object>) jo.get(JSON_KEY_TABLE_SETTINGS));
    }
    if (jo.containsKey(JSON_KEY_LIST_SETTINGS)) {
      setListFromJsonObject((Map<String, Object>) jo.get(JSON_KEY_LIST_SETTINGS));
    }
    if (jo.containsKey(JSON_KEY_LINE_SETTINGS)) {
      setLineFromJsonObject((Map<String, Object>) jo.get(JSON_KEY_LINE_SETTINGS));
    }
    if (jo.containsKey(JSON_KEY_BAR_SETTINGS)) {
      setBarFromJsonObject((Map<String, Object>) jo.get(JSON_KEY_BAR_SETTINGS));
    }
    if (jo.containsKey(JSON_KEY_BOX_STEM_SETTINGS)) {
      setBoxStemFromJsonObject((Map<String, Object>) jo.get(JSON_KEY_BOX_STEM_SETTINGS));
    }
    if (jo.containsKey(JSON_KEY_MAP_SETTINGS)) {
      setMapFromJsonObject((Map<String, Object>) jo.get(JSON_KEY_MAP_SETTINGS));
    }
    if (jo.containsKey(JSON_KEY_CUSTOM_SETTINGS)) {
      setCustomFromJsonObject((Map<String, Object>) jo.get(JSON_KEY_CUSTOM_SETTINGS));
    }
  }

  private void setTableFromJsonObject(Map<String, Object> jo) throws JSONException {
    if (jo.containsKey(JSON_KEY_TABLE_COL_WIDTHS)) {
      ArrayList<Integer> ja = (ArrayList<Integer>) jo.get(JSON_KEY_TABLE_COL_WIDTHS);
      tableColWidths = new int[ja.size()];
      for (int i = 0; i < ja.size(); i++) {
        tableColWidths[i] = ja.get(i);
      }
    }
    tableIndexedCol = jo.containsKey(JSON_KEY_TABLE_INDEXED_COL) ? (String) jo
        .get(JSON_KEY_TABLE_INDEXED_COL) : null;
  }

  private void setListFromJsonObject(Map<String, Object> jo) throws JSONException {
    listFormat = jo.containsKey(JSON_KEY_LIST_FORMAT) ? (String) jo.get(JSON_KEY_LIST_FORMAT)
        : null;
  }

  private void setLineFromJsonObject(Map<String, Object> jo) throws JSONException {
    lineXCol = jo.containsKey(JSON_KEY_LINE_X_COL) ? (String) jo.get(JSON_KEY_LINE_X_COL) : null;
    lineYCol = jo.containsKey(JSON_KEY_LINE_Y_COL) ? (String) jo.get(JSON_KEY_LINE_Y_COL) : null;
  }

  private void setBarFromJsonObject(Map<String, Object> jo) throws JSONException {
    barXCol = jo.containsKey(JSON_KEY_BAR_X_COL) ? (String) jo.get(JSON_KEY_BAR_X_COL) : null;
    barYCol = jo.containsKey(JSON_KEY_BAR_Y_COL) ? (String) jo.get(JSON_KEY_BAR_Y_COL) : null;
  }

  private void setBoxStemFromJsonObject(Map<String, Object> jo) throws JSONException {
    boxStemXCol = jo.containsKey(JSON_KEY_BOX_STEM_X_COL) ? (String) jo
        .get(JSON_KEY_BOX_STEM_X_COL) : null;
    boxStemYCol = jo.containsKey(JSON_KEY_BOX_STEM_Y_COL) ? (String) jo
        .get(JSON_KEY_BOX_STEM_Y_COL) : null;
  }

  private void setMapFromJsonObject(Map<String, Object> jo) throws JSONException {
    mapLocCol = jo.containsKey(JSON_KEY_MAP_LOC_COL) ? (String) jo.get(JSON_KEY_MAP_LOC_COL) : null;
    mapLabelCol = jo.containsKey(JSON_KEY_MAP_LABEL_COL) ? (String) jo.get(JSON_KEY_MAP_LABEL_COL)
        : null;
    if (jo.containsKey(JSON_KEY_MAP_COLOR_RULERS)) {
      Map<String, Object> colorRulerJo = (Map<String, Object>) jo.get(JSON_KEY_MAP_COLOR_RULERS);
      for (ColumnProperties cp : tp.getColumns()) {
        String cdn = cp.getColumnDbName();
        if (colorRulerJo.containsKey(cdn)) {
          mapColorRulers
              .put(cdn, new ConditionalRuler((Map<String, Object>) colorRulerJo.get(cdn)));
        }
      }
    }
    if (jo.containsKey(JSON_KEY_MAP_SIZE_RULERS)) {
      Map<String, Object> sizeRulerJo = (Map<String, Object>) jo.get(JSON_KEY_MAP_SIZE_RULERS);
      for (ColumnProperties cp : tp.getColumns()) {
        String cdn = cp.getColumnDbName();
        if (sizeRulerJo.containsKey(cdn)) {
          mapSizeRulers.put(cdn, new ConditionalRuler((Map<String, Object>) sizeRulerJo.get(cdn)));
        }
      }
    }
  }

  private void setCustomFromJsonObject(Map<String, Object> jo) throws JSONException {
    if (jo.containsKey(JSON_KEY_CUSTOM_LIST_FILE)) {
      customListFilename = (String) jo.get(JSON_KEY_CUSTOM_LIST_FILE);
    }
    if (jo.containsKey(JSON_KEY_CUSTOM_DETAIL_FILE)) {
      customDetailFilename = (String) jo.get(JSON_KEY_CUSTOM_DETAIL_FILE);
    }
    if (jo.containsKey(JSON_KEY_CUSTOM_VIEWS_INFO)) {
      ArrayList<Map<String, Object>> viewsJa = (ArrayList<Map<String, Object>>) jo
          .get(JSON_KEY_CUSTOM_VIEWS_INFO);
      for (int i = 0; i < viewsJa.size(); i++) {
        customViews.add(new CustomView(viewsJa.get(i)));
      }
    }
  }

  public static TableViewSettings newOverviewTVS(TableProperties tp, String dbString) {
    return new TableViewSettings(tp, ViewType.OVERVIEW_VIEW, dbString);
  }

  public static TableViewSettings newCollectionTVS(TableProperties tp, String dbString) {
    return new TableViewSettings(tp, ViewType.COLLECTION_VIEW, dbString);
  }

  public int getViewType() {
    return viewType;
  }

  public int[] getTableColWidths() {
    if (tableColWidths == null) {
      int[] colWidths = new int[tp.getColumns().length];
      for (int i = 0; i < colWidths.length; i++) {
        colWidths[i] = DEFAULT_TABLE_COL_WIDTH;
      }
      return colWidths;
    } else if (tableColWidths.length < tp.getColumnOrder().size()) {
      int colCount = tp.getColumnOrder().size();
      int[] colWidths = new int[colCount];
      for (int i = 0; i < tableColWidths.length; i++) {
        colWidths[i] = tableColWidths[i];
      }
      for (int i = tableColWidths.length; i < colCount; i++) {
        colWidths[i] = DEFAULT_TABLE_COL_WIDTH;
      }
      return colWidths;
    } else if (tableColWidths.length > tp.getColumnOrder().size()) {
      int colCount = tp.getColumnOrder().size();
      int[] colWidths = new int[colCount];
      for (int i = 0; i < colCount; i++) {
        colWidths[i] = tableColWidths[i];
      }
      return colWidths;
    } else {
      return tableColWidths;
    }
  }

  public int getTableIndexedColIndex() {
    if (tableIndexedCol == null) {
      return -1;
    }
    for (int i = 0; i < tp.getColumnOrder().size(); i++) {
      if (tableIndexedCol.equals(tp.getColumnOrder().get(i))) {
        return i;
      }
    }
    return -1;
  }

  public String getListFormat() {
    return listFormat;
  }

  public ColumnProperties getLineXCol() {
    return (lineXCol == null) ? null : tp.getColumnByDbName(lineXCol);
  }

  public ColumnProperties getLineYCol() {
    return (lineYCol == null) ? null : tp.getColumnByDbName(lineYCol);
  }

  public ColumnProperties getBarXCol() {
    return (barXCol == null) ? null : tp.getColumnByDbName(barXCol);
  }

  public String getBarYCol() {
    return barYCol;
  }

  public ColumnProperties getBoxStemXCol() {
    return (boxStemXCol == null) ? null : tp.getColumnByDbName(boxStemXCol);
  }

  public ColumnProperties getBoxStemYCol() {
    return (boxStemYCol == null) ? null : tp.getColumnByDbName(boxStemYCol);
  }

  public ColumnProperties getMapLocationCol() {
    return (mapLocCol == null) ? null : tp.getColumnByDbName(mapLocCol);
  }

  public ColumnProperties getMapLabelCol() {
    return (mapLabelCol == null) ? null : tp.getColumnByDbName(mapLabelCol);
  }

  public ConditionalRuler getMapColorRuler(ColumnProperties cp) {
    if (mapColorRulers.containsKey(cp.getColumnDbName())) {
      return mapColorRulers.get(cp.getColumnDbName());
    } else {
      ConditionalRuler cr = new ConditionalRuler();
      mapColorRulers.put(cp.getColumnDbName(), cr);
      return cr;
    }
  }

  public ConditionalRuler getMapSizeRuler(ColumnProperties cp) {
    if (mapSizeRulers.containsKey(cp.getColumnDbName())) {
      return mapSizeRulers.get(cp.getColumnDbName());
    } else {
      ConditionalRuler cr = new ConditionalRuler();
      mapSizeRulers.put(cp.getColumnDbName(), cr);
      return cr;
    }
  }

  public String getCustomListFilename() {
    return customListFilename;
  }
  
  public String getCustomDetailFileName() {
    return customDetailFilename;
  }

  public int getCustomViewCount() {
    return customViews.size();
  }

  public CustomView getCustomView(int index) {
    return customViews.get(index);
  }

  Map<String, Object> toJsonObject() {
    Map<String, Object> jo = new HashMap<String, Object>();
    jo.put(JSON_KEY_VIEW_TYPE, viewType);
    jo.put(JSON_KEY_TABLE_SETTINGS, tableSettingsToJsonObject());
    jo.put(JSON_KEY_LIST_SETTINGS, listSettingsToJsonObject());
    jo.put(JSON_KEY_LINE_SETTINGS, lineSettingsToJsonObject());
    jo.put(JSON_KEY_BAR_SETTINGS, barSettingsToJsonObject());
    jo.put(JSON_KEY_BOX_STEM_SETTINGS, boxStemSettingsToJsonObject());
    jo.put(JSON_KEY_MAP_SETTINGS, mapSettingsToJsonObject());
    jo.put(JSON_KEY_CUSTOM_SETTINGS, customSettingsToJsonObject());
    return jo;
  }

  private Map<String, Object> tableSettingsToJsonObject() {
    Map<String, Object> jo = new HashMap<String, Object>();
    if (tableColWidths != null) {
      ArrayList<Integer> arr = new ArrayList<Integer>();
      for (int width : tableColWidths) {
        arr.add(width);
      }
      jo.put(JSON_KEY_TABLE_COL_WIDTHS, arr);
    }
    if (tableIndexedCol != null) {
      jo.put(JSON_KEY_TABLE_INDEXED_COL, tableIndexedCol);
    }
    return jo;
  }

  private Map<String, Object> listSettingsToJsonObject() {
    Map<String, Object> jo = new HashMap<String, Object>();
    if (listFormat != null) {
      jo.put(JSON_KEY_LIST_FORMAT, listFormat);
    }
    return jo;
  }

  private Map<String, Object> lineSettingsToJsonObject() {
    Map<String, Object> jo = new HashMap<String, Object>();
    if (lineXCol != null) {
      jo.put(JSON_KEY_LINE_X_COL, lineXCol);
    }
    if (lineYCol != null) {
      jo.put(JSON_KEY_LINE_Y_COL, lineYCol);
    }
    return jo;
  }

  private Map<String, Object> barSettingsToJsonObject() {
    Map<String, Object> jo = new HashMap<String, Object>();
    if (barXCol != null) {
      jo.put(JSON_KEY_BAR_X_COL, barXCol);
    }
    if (barYCol != null) {
      jo.put(JSON_KEY_BAR_Y_COL, barYCol);
    }
    return jo;
  }

  private Map<String, Object> boxStemSettingsToJsonObject() {
    Map<String, Object> jo = new HashMap<String, Object>();
    if (boxStemXCol != null) {
      jo.put(JSON_KEY_BOX_STEM_X_COL, boxStemXCol);
    }
    if (boxStemYCol != null) {
      jo.put(JSON_KEY_BOX_STEM_Y_COL, boxStemYCol);
    }
    return jo;
  }

  private Map<String, Object> mapSettingsToJsonObject() {
    Map<String, Object> jo = new HashMap<String, Object>();
    if (mapLocCol != null) {
      jo.put(JSON_KEY_MAP_LOC_COL, mapLocCol);
    }
    if (mapLabelCol != null) {
      jo.put(JSON_KEY_MAP_LABEL_COL, mapLabelCol);
    }
    Map<String, Object> colorRulerJo = new HashMap<String, Object>();
    for (String key : mapColorRulers.keySet()) {
      ConditionalRuler cr = mapColorRulers.get(key);
      if (cr.getRuleCount() > 0) {
        colorRulerJo.put(key, cr.toJsonObject());
      }
    }
    jo.put(JSON_KEY_MAP_COLOR_RULERS, colorRulerJo);
    Map<String, Object> sizeRulerJo = new HashMap<String, Object>();
    for (String key : mapSizeRulers.keySet()) {
      ConditionalRuler cr = mapSizeRulers.get(key);
      if (cr.getRuleCount() > 0) {
        sizeRulerJo.put(key, cr.toJsonObject());
      }
    }
    jo.put(JSON_KEY_MAP_SIZE_RULERS, sizeRulerJo);
    return jo;
  }

  private Map<String, Object> customSettingsToJsonObject() {
    Map<String, Object> jo = new HashMap<String, Object>();
    if (customListFilename != null) {
      jo.put(JSON_KEY_CUSTOM_LIST_FILE, customListFilename);
    }
    if (customDetailFilename != null) {
      jo.put(JSON_KEY_CUSTOM_DETAIL_FILE, customDetailFilename);
    }
    if (!customViews.isEmpty()) {
      JSONArray ja = new JSONArray();
      for (CustomView cv : customViews) {
        ja.put(cv.toJsonObject());
      }
      jo.put(JSON_KEY_CUSTOM_VIEWS_INFO, ja);
    }
    return jo;
  }

  public void setViewType(int viewType) {
    this.viewType = viewType;
    set();
  }

  public void setTableColWidths(int[] widths) {
    tableColWidths = widths;
    set();
  }

  public void setTableIndexedCol(String indexedCol) {
    tableIndexedCol = indexedCol;
    set();
  }

  public void setListFormat(String format) {
    listFormat = format;
    set();
  }

  public void setLineXCol(ColumnProperties cp) {
    lineXCol = (cp == null) ? null : cp.getColumnDbName();
    set();
  }

  public void setLineYCol(ColumnProperties cp) {
    lineYCol = (cp == null) ? null : cp.getColumnDbName();
    set();
  }

  public void setBarXCol(ColumnProperties cp) {
    barXCol = (cp == null) ? null : cp.getColumnDbName();
    set();
  }

  public void setBarYCol(String barYCol) {
    this.barYCol = barYCol;
    set();
  }

  public void setBoxStemXCol(ColumnProperties cp) {
    boxStemXCol = (cp == null) ? null : cp.getColumnDbName();
    set();
  }

  public void setBoxStemYCol(ColumnProperties cp) {
    boxStemYCol = (cp == null) ? null : cp.getColumnDbName();
    set();
  }

  public void setMapLocationCol(ColumnProperties cp) {
    mapLocCol = (cp == null) ? null : cp.getColumnDbName();
    set();
  }

  public void setMapLabelCol(ColumnProperties cp) {
    mapLabelCol = (cp == null) ? null : cp.getColumnDbName();
    set();
  }

  public void setCustomListFilename(String filename) {
    customListFilename = filename;
    set();
  }
  
  public void setCustomDetailFilename(String filename) {
    customDetailFilename = filename;
    set();
  }

  public void addCustomView(CustomView cv) {
    customViews.add(cv);
    set();
  }

  public void removeCustomView(int index) {
    customViews.remove(index);
    set();
  }

  private void set() {
    String dbString;
    try {
      dbString = mapper.writeValueAsString(toJsonObject());
    } catch (JsonGenerationException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("invalid conversion: " + toJsonObject().toString());
    } catch (JsonMappingException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("invalid conversion: " + toJsonObject().toString());
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("invalid conversion: " + toJsonObject().toString());
    }
    if (type == ViewType.OVERVIEW_VIEW) {
      tp.setOverviewViewSettings(dbString);
    } else {
      tp.setCollectionViewSettings(dbString);
    }
  }

  public int[] getPossibleViewTypes() {
    int numericColCount = 0;
    int locationColCount = 0;
    int dateColCount = 0;
    for (ColumnProperties cp : tp.getColumns()) {
      if (cp.getColumnType() == ColumnType.NUMBER || cp.getColumnType() == ColumnType.INTEGER) {
        numericColCount++;
      } else if (cp.getColumnType() == ColumnType.GEOPOINT) {
        locationColCount++;
      } else if (cp.getColumnType() == ColumnType.DATE || cp.getColumnType() == ColumnType.DATETIME
          || cp.getColumnType() == ColumnType.TIME) {
        dateColCount++;
      }
    }
    List<Integer> list = new ArrayList<Integer>();
    list.add(Type.SPREADSHEET);
    list.add(Type.LIST);
    if ((numericColCount >= 2) || ((numericColCount >= 1) && (dateColCount >= 1))) {
      list.add(Type.LINE_GRAPH);
    }
    if (numericColCount >= 1) {
      list.add(Type.BOX_STEM);
    }
    list.add(Type.BAR_GRAPH);
    if (locationColCount >= 1) {
      list.add(Type.MAP);
    }
    int[] arr = new int[list.size()];
    for (int i = 0; i < list.size(); i++) {
      arr[i] = list.get(i);
    }
    return arr;
  }

  public class ConditionalRuler {

    private static final String JSON_KEY_COMPARATORS = "comparators";
    private static final String JSON_KEY_VALUES = "values";
    private static final String JSON_KEY_SETTINGS = "settings";

    public class Comparator {
      public static final int EQUALS = 0;
      public static final int LESS_THAN = 1;
      public static final int LESS_THAN_EQUALS = 2;
      public static final int GREATER_THAN = 3;
      public static final int GREATER_THAN_EQUALS = 4;
      public static final int COUNT = 5;

      private Comparator() {
      }
    }

    private final List<Integer> comparators;
    private final List<String> values;
    private final List<Integer> settings;

    ConditionalRuler() {
      comparators = new ArrayList<Integer>();
      values = new ArrayList<String>();
      settings = new ArrayList<Integer>();
    }

    ConditionalRuler(Map<String, Object> jo) {
      comparators = new ArrayList<Integer>();
      values = new ArrayList<String>();
      settings = new ArrayList<Integer>();
      ArrayList<Integer> comparatorsArr = (ArrayList<Integer>) jo.get(JSON_KEY_COMPARATORS);
      ArrayList<String> valuesArr = (ArrayList<String>) jo.get(JSON_KEY_VALUES);
      ArrayList<Integer> colorsArr = (ArrayList<Integer>) jo.get(JSON_KEY_SETTINGS);
      for (int i = 0; i < comparatorsArr.size(); i++) {
        comparators.add(comparatorsArr.get(i));
        values.add((String) valuesArr.get(i));
        settings.add((Integer) colorsArr.get(i));
      }
    }

    public int getSetting(String value, int defaultSetting) {
      for (int i = 0; i < comparators.size(); i++) {
        if (checkMatch(i, value)) {
          return settings.get(i);
        }
      }
      return defaultSetting;
    }

    private boolean checkMatch(int index, String value) {
      switch (comparators.get(index)) {
      case Comparator.EQUALS:
        return value.equals(values.get(index));
      case Comparator.LESS_THAN:
        return (value.compareTo(values.get(index)) < 0);
      case Comparator.LESS_THAN_EQUALS:
        return (value.compareTo(values.get(index)) <= 0);
      case Comparator.GREATER_THAN:
        return (value.compareTo(values.get(index)) > 0);
      case Comparator.GREATER_THAN_EQUALS:
        return (value.compareTo(values.get(index)) >= 0);
      default:
        throw new RuntimeException();
      }
    }

    public void addRule(int comparator, String value, int setting) {
      comparators.add(comparator);
      values.add(value);
      settings.add(setting);
      set();
    }

    public int getRuleCount() {
      return comparators.size();
    }

    public int getRuleComparator(int index) {
      return comparators.get(index);
    }

    public void setRuleComparator(int index, int comparator) {
      comparators.set(index, comparator);
      set();
    }

    public String getRuleValue(int index) {
      return values.get(index);
    }

    public void setRuleValue(int index, String value) {
      values.set(index, value);
      set();
    }

    public int getRuleSetting(int index) {
      return settings.get(index);
    }

    public void setRuleSetting(int index, int setting) {
      settings.set(index, setting);
      set();
    }

    public void deleteRule(int index) {
      comparators.remove(index);
      values.remove(index);
      settings.remove(index);
      set();
    }

    Map<String, Object> toJsonObject() {
      Map<String, Object> jo = new HashMap<String, Object>();
      jo.put(JSON_KEY_COMPARATORS, comparators);
      jo.put(JSON_KEY_VALUES, values);
      jo.put(JSON_KEY_SETTINGS, settings);
      return jo;
    }
  }

  public class CustomView {

    private static final String JSON_KEY_NAME = "name";
    private static final String JSON_KEY_FILENAME = "filename";

    private String name;
    private String filename;

    CustomView() {
      name = "";
      filename = null;
    }

    private CustomView(Map<String, Object> jo) {
      name = (String) jo.get(JSON_KEY_NAME);
      filename = (String) jo.get(JSON_KEY_FILENAME);
    }

    public String getName() {
      return name;
    }

    public String getFilename() {
      return filename;
    }

    public void setName(String name) {
      this.name = name;
      set();
    }

    public void setFilename(String filename) {
      this.filename = filename;
      set();
    }

    private Map<String, Object> toJsonObject() {
      Map<String, Object> jo = new HashMap<String, Object>();
      jo.put(JSON_KEY_NAME, name);
      jo.put(JSON_KEY_FILENAME, filename);
      return jo;
    }
  }
}
