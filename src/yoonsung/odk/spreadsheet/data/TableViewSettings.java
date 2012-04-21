package yoonsung.odk.spreadsheet.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.graphics.Color;


public class TableViewSettings {
    
    enum ViewType {
        OVERVIEW_VIEW,
        COLLECTION_VIEW
    }
    
    public class Type {
        public static final int SPREADSHEET = 0;
        public static final int LIST = 1;
        public static final int LINE_GRAPH = 2;
        public static final int BOX_STEM = 3;
        public static final int BAR_GRAPH = 4;
        public static final int MAP = 5;
        public static final int COUNT = 6;
        private Type() {}
    }
    
    public static final int[] MAP_COLOR_OPTIONS = {Color.BLACK, Color.BLUE};
    
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
    
    private TableViewSettings(TableProperties tp, ViewType type,
            String dbString) {
        this.tp = tp;
        this.type = type;
        mapColorRulers = new HashMap<String, ConditionalRuler>();
        mapSizeRulers = new HashMap<String, ConditionalRuler>();
        if (dbString == null) {
            viewType = Type.SPREADSHEET;
            return;
        }
        try {
            setFromJsonObject(new JSONObject(dbString));
        } catch(JSONException e) {
            throw new RuntimeException(e);
        }
    }
    
    void setFromJsonObject(JSONObject jo) throws JSONException {
        if (jo == null) {
            viewType = Type.SPREADSHEET;
            return;
        }
        viewType = jo.getInt(JSON_KEY_VIEW_TYPE);
        if (jo.has(JSON_KEY_TABLE_SETTINGS)) {
            setTableFromJsonObject(jo.getJSONObject(JSON_KEY_TABLE_SETTINGS));
        }
        if (jo.has(JSON_KEY_LIST_SETTINGS)) {
            setListFromJsonObject(jo.getJSONObject(JSON_KEY_LIST_SETTINGS));
        }
        if (jo.has(JSON_KEY_LINE_SETTINGS)) {
            setLineFromJsonObject(jo.getJSONObject(JSON_KEY_LINE_SETTINGS));
        }
        if (jo.has(JSON_KEY_BAR_SETTINGS)) {
            setBarFromJsonObject(jo.getJSONObject(JSON_KEY_BAR_SETTINGS));
        }
        if (jo.has(JSON_KEY_BOX_STEM_SETTINGS)) {
            setBoxStemFromJsonObject(jo.getJSONObject(
                    JSON_KEY_BOX_STEM_SETTINGS));
        }
        if (jo.has(JSON_KEY_MAP_SETTINGS)) {
            setMapFromJsonObject(jo.getJSONObject(JSON_KEY_MAP_SETTINGS));
        }
    }
    
    private void setTableFromJsonObject(JSONObject jo) throws JSONException {
        if (jo.has(JSON_KEY_TABLE_COL_WIDTHS)) {
            JSONArray ja = jo.getJSONArray(JSON_KEY_TABLE_COL_WIDTHS);
            tableColWidths = new int[ja.length()];
            for (int i = 0; i < ja.length(); i++) {
                tableColWidths[i] = ja.getInt(i);
            }
        }
        tableIndexedCol = jo.has(JSON_KEY_TABLE_INDEXED_COL) ?
                jo.getString(JSON_KEY_TABLE_INDEXED_COL) : null;
    }
    
    private void setListFromJsonObject(JSONObject jo) throws JSONException {
        listFormat = jo.has(JSON_KEY_LIST_FORMAT) ?
                jo.getString(JSON_KEY_LIST_FORMAT) : null;
    }
    
    private void setLineFromJsonObject(JSONObject jo) throws JSONException {
        lineXCol = jo.has(JSON_KEY_LINE_X_COL) ?
                jo.getString(JSON_KEY_LINE_X_COL) : null;
        lineYCol = jo.has(JSON_KEY_LINE_Y_COL) ?
                jo.getString(JSON_KEY_LINE_Y_COL) : null;
    }
    
    private void setBarFromJsonObject(JSONObject jo) throws JSONException {
        barXCol = jo.has(JSON_KEY_BAR_X_COL) ?
                jo.getString(JSON_KEY_BAR_X_COL) : null;
        barYCol = jo.has(JSON_KEY_BAR_Y_COL) ?
                jo.getString(JSON_KEY_BAR_Y_COL) : null;
    }
    
    private void setBoxStemFromJsonObject(JSONObject jo) throws JSONException {
        boxStemXCol = jo.has(JSON_KEY_BOX_STEM_X_COL) ?
                jo.getString(JSON_KEY_BOX_STEM_X_COL) : null;
        boxStemYCol = jo.has(JSON_KEY_BOX_STEM_Y_COL) ?
                jo.getString(JSON_KEY_BOX_STEM_Y_COL) : null;
    }
    
    private void setMapFromJsonObject(JSONObject jo) throws JSONException {
        mapLocCol = jo.has(JSON_KEY_MAP_LOC_COL) ?
                jo.getString(JSON_KEY_MAP_LOC_COL) : null;
        mapLabelCol = jo.has(JSON_KEY_MAP_LABEL_COL) ?
                jo.getString(JSON_KEY_MAP_LABEL_COL) : null;
        if (jo.has(JSON_KEY_MAP_COLOR_RULERS)) {
            JSONObject colorRulerJo =
                jo.getJSONObject(JSON_KEY_MAP_COLOR_RULERS);
            for (ColumnProperties cp : tp.getColumns()) {
                String cdn = cp.getColumnDbName();
                if (colorRulerJo.has(cdn)) {
                    mapColorRulers.put(cdn, new ConditionalRuler(
                            colorRulerJo.getJSONObject(cdn)));
                }
            }
        }
        if (jo.has(JSON_KEY_MAP_SIZE_RULERS)) {
            JSONObject sizeRulerJo = jo.getJSONObject(
                    JSON_KEY_MAP_SIZE_RULERS);
            for (ColumnProperties cp : tp.getColumns()) {
                String cdn = cp.getColumnDbName();
                if (sizeRulerJo.has(cdn)) {
                    mapSizeRulers.put(cdn, new ConditionalRuler(
                            sizeRulerJo.getJSONObject(cdn)));
                }
            }
        }
    }
    
    public static TableViewSettings newOverviewTVS(TableProperties tp,
            String dbString) {
        return new TableViewSettings(tp, ViewType.OVERVIEW_VIEW, dbString);
    }
    
    public static TableViewSettings newCollectionTVS(TableProperties tp,
            String dbString) {
        return new TableViewSettings(tp, ViewType.COLLECTION_VIEW, dbString);
    }
    
    public int getViewType() {
        return viewType;
    }
    
    public int[] getTableColWidths() {
        if (tableColWidths != null) {
            return tableColWidths;
        }
        int[] colWidths = new int[tp.getColumns().length];
        for (int i = 0; i < colWidths.length; i++) {
            colWidths[i] = DEFAULT_TABLE_COL_WIDTH;
        }
        return colWidths;
    }
    
    public int getTableIndexedColIndex() {
        if (tableIndexedCol == null) {
            return -1;
        }
        for (int i = 0; i < tp.getColumnOrder().length; i++) {
            if (tableIndexedCol.equals(tp.getColumnOrder()[i])) {
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
        return (boxStemXCol == null) ? null :
            tp.getColumnByDbName(boxStemXCol);
    }
    
    public ColumnProperties getBoxStemYCol() {
        return (boxStemYCol == null) ? null :
            tp.getColumnByDbName(boxStemYCol);
    }
    
    public ColumnProperties getMapLocationCol() {
        return (mapLocCol == null) ? null : tp.getColumnByDbName(mapLocCol);
    }
    
    public ColumnProperties getMapLabelCol() {
        return (mapLabelCol == null) ? null :
            tp.getColumnByDbName(mapLabelCol);
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
    
    JSONObject toJsonObject() {
        JSONObject jo = new JSONObject();
        try {
            jo.put(JSON_KEY_VIEW_TYPE, viewType);
            jo.put(JSON_KEY_TABLE_SETTINGS, tableSettingsToJsonObject());
            jo.put(JSON_KEY_LIST_SETTINGS, listSettingsToJsonObject());
            jo.put(JSON_KEY_LINE_SETTINGS, lineSettingsToJsonObject());
            jo.put(JSON_KEY_BAR_SETTINGS, barSettingsToJsonObject());
            jo.put(JSON_KEY_BOX_STEM_SETTINGS, boxStemSettingsToJsonObject());
            jo.put(JSON_KEY_MAP_SETTINGS, mapSettingsToJsonObject());
            return jo;
        } catch(JSONException e) {
            throw new RuntimeException(e);
        }
    }
    
    private JSONObject tableSettingsToJsonObject() throws JSONException {
        JSONObject jo = new JSONObject();
        if (tableColWidths != null) {
            JSONArray arr = new JSONArray();
            for (int width : tableColWidths) {
                arr.put(width);
            }
            jo.put(JSON_KEY_TABLE_COL_WIDTHS, arr);
        }
        if (tableIndexedCol != null) {
            jo.put(JSON_KEY_TABLE_INDEXED_COL, tableIndexedCol);
        }
        return jo;
    }
    
    private JSONObject listSettingsToJsonObject() throws JSONException {
        JSONObject jo = new JSONObject();
        if (listFormat != null) {
            jo.put(JSON_KEY_LIST_FORMAT, listFormat);
        }
        return jo;
    }
    
    private JSONObject lineSettingsToJsonObject() throws JSONException {
        JSONObject jo = new JSONObject();
        if (lineXCol != null) {
            jo.put(JSON_KEY_LINE_X_COL, lineXCol);
        }
        if (lineYCol != null) {
            jo.put(JSON_KEY_LINE_Y_COL, lineYCol);
        }
        return jo;
    }
    
    private JSONObject barSettingsToJsonObject() throws JSONException {
        JSONObject jo = new JSONObject();
        if (barXCol != null) {
            jo.put(JSON_KEY_BAR_X_COL, barXCol);
        }
        if (barYCol != null) {
            jo.put(JSON_KEY_BAR_Y_COL, barYCol);
        }
        return jo;
    }
    
    private JSONObject boxStemSettingsToJsonObject() throws JSONException {
        JSONObject jo = new JSONObject();
        if (boxStemXCol != null) {
            jo.put(JSON_KEY_BOX_STEM_X_COL, boxStemXCol);
        }
        if (boxStemYCol != null) {
            jo.put(JSON_KEY_BOX_STEM_Y_COL, boxStemYCol);
        }
        return jo;
    }
    
    private JSONObject mapSettingsToJsonObject() throws JSONException {
        JSONObject jo = new JSONObject();
        if (mapLocCol != null) {
            jo.put(JSON_KEY_MAP_LOC_COL, mapLocCol);
        }
        if (mapLabelCol != null) {
            jo.put(JSON_KEY_MAP_LABEL_COL, mapLabelCol);
        }
        JSONObject colorRulerJo = new JSONObject();
        for (String key : mapColorRulers.keySet()) {
            ConditionalRuler cr = mapColorRulers.get(key);
            if (cr.getRuleCount() > 0) {
                colorRulerJo.put(key, cr.toJsonObject());
            }
        }
        jo.put(JSON_KEY_MAP_COLOR_RULERS, colorRulerJo);
        JSONObject sizeRulerJo = new JSONObject();
        for (String key : mapSizeRulers.keySet()) {
            ConditionalRuler cr = mapSizeRulers.get(key);
            if (cr.getRuleCount() > 0) {
                sizeRulerJo.put(key, cr.toJsonObject());
            }
        }
        jo.put(JSON_KEY_MAP_SIZE_RULERS, sizeRulerJo);
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
    
    private void set() {
        String dbString = toJsonObject().toString();
        if (type == ViewType.OVERVIEW_VIEW) {
            tp.setOverviewViewSettings(dbString);
        } else {
            tp.setCollectionViewSettings(dbString);
        }
    }
    
    public int[] getPossibleViewTypes() {
        int numericColCount = 0;
        int locationColCount = 0;
        for (ColumnProperties cp : tp.getColumns()) {
            if (cp.getColumnType() == ColumnProperties.ColumnType.NUMBER) {
                numericColCount++;
            } else if (cp.getColumnType() ==
                ColumnProperties.ColumnType.LOCATION) {
                locationColCount++;
            }
        }
        List<Integer> list = new ArrayList<Integer>();
        list.add(Type.SPREADSHEET);
        list.add(Type.LIST);
        if (numericColCount >= 2) {
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
            private Comparator() {}
        }
        
        private final List<Integer> comparators;
        private final List<String> values;
        private final List<Integer> settings;
        
        ConditionalRuler() {
            comparators = new ArrayList<Integer>();
            values = new ArrayList<String>();
            settings = new ArrayList<Integer>();
        }
        
        ConditionalRuler(JSONObject jo) {
            comparators = new ArrayList<Integer>();
            values = new ArrayList<String>();
            settings = new ArrayList<Integer>();
            JSONArray comparatorsArr;
            try {
                comparatorsArr = jo.getJSONArray(JSON_KEY_COMPARATORS);
                JSONArray valuesArr = jo.getJSONArray(JSON_KEY_VALUES);
                JSONArray colorsArr = jo.getJSONArray(JSON_KEY_SETTINGS);
                for (int i = 0; i < comparatorsArr.length(); i++) {
                    comparators.add(comparatorsArr.getInt(i));
                    values.add(valuesArr.getString(i));
                    settings.add(colorsArr.getInt(i));
                }
            } catch(JSONException e) {
                e.printStackTrace();
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
        
        JSONObject toJsonObject() {
            JSONObject jo = new JSONObject();
            try {
                jo.put(JSON_KEY_COMPARATORS, new JSONArray(comparators));
                jo.put(JSON_KEY_VALUES, new JSONArray(values));
                jo.put(JSON_KEY_SETTINGS, new JSONArray(settings));
            } catch(JSONException e) {
                e.printStackTrace();
            }
            return jo;
        }
    }
}
