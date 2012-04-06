package yoonsung.odk.spreadsheet.data;

import org.json.JSONException;
import org.json.JSONObject;


public class TableViewSettings {
    
    enum ViewType {
        OVERVIEW_VIEW,
        COLLECTION_VIEW
    }
    
    public class Type {
        public static final int TABLE = 0;
        public static final int LIST = 1;
        public static final int LINE_GRAPH = 2;
        public static final int BOX_STEM = 3;
        public static final int COUNT = 4;
        private Type() {}
    }
    
    private static final String JSON_KEY_VIEW_TYPE = "viewType";
    private static final String JSON_KEY_LIST_SETTINGS = "list";
    private static final String JSON_KEY_LIST_FORMAT = "listFormat";
    private static final String JSON_KEY_LINE_SETTINGS = "line";
    private static final String JSON_KEY_LINE_X_COL = "lineX";
    private static final String JSON_KEY_LINE_Y_COL = "lineY";
    private static final String JSON_KEY_BOX_STEM_SETTINGS = "boxStem";
    private static final String JSON_KEY_BOX_STEM_X_COL = "boxStemX";
    private static final String JSON_KEY_BOX_STEM_Y_COL = "boxStemY";
    
    private final TableProperties tp;
    private final ViewType type;
    private int viewType;
    private String listFormat;
    private String lineXCol;
    private String lineYCol;
    private String boxStemXCol;
    private String boxStemYCol;
    
    private TableViewSettings(TableProperties tp, ViewType type,
            String dbString) {
        this.tp = tp;
        this.type = type;
        if (dbString == null) {
            viewType = Type.TABLE;
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
            viewType = Type.TABLE;
            return;
        }
        viewType = jo.getInt(JSON_KEY_VIEW_TYPE);
        if (jo.has(JSON_KEY_LIST_SETTINGS)) {
            setListFromJsonObject(jo.getJSONObject(JSON_KEY_LIST_SETTINGS));
        }
        if (jo.has(JSON_KEY_LINE_SETTINGS)) {
            setLineFromJsonObject(jo.getJSONObject(JSON_KEY_LINE_SETTINGS));
        }
        if (jo.has(JSON_KEY_BOX_STEM_SETTINGS)) {
            setBoxStemFromJsonObject(jo.getJSONObject(
                    JSON_KEY_BOX_STEM_SETTINGS));
        }
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
    
    private void setBoxStemFromJsonObject(JSONObject jo) throws JSONException {
        boxStemXCol = jo.has(JSON_KEY_BOX_STEM_X_COL) ?
                jo.getString(JSON_KEY_BOX_STEM_X_COL) : null;
        boxStemYCol = jo.has(JSON_KEY_BOX_STEM_Y_COL) ?
                jo.getString(JSON_KEY_BOX_STEM_Y_COL) : null;
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
    
    public String getListFormat() {
        return listFormat;
    }
    
    public ColumnProperties getLineXCol() {
        return (lineXCol == null) ? null : tp.getColumnByDbName(lineXCol);
    }
    
    public ColumnProperties getLineYCol() {
        return (lineYCol == null) ? null : tp.getColumnByDbName(lineYCol);
    }
    
    public ColumnProperties getBoxStemXCol() {
        return (boxStemXCol == null) ? null :
            tp.getColumnByDbName(boxStemXCol);
    }
    
    public ColumnProperties getBoxStemYCol() {
        return (boxStemYCol == null) ? null :
            tp.getColumnByDbName(boxStemYCol);
    }
    
    JSONObject toJsonObject() {
        JSONObject jo = new JSONObject();
        try {
            jo.put(JSON_KEY_VIEW_TYPE, viewType);
            jo.put(JSON_KEY_LIST_SETTINGS, listSettingsToJsonObject());
            jo.put(JSON_KEY_LINE_SETTINGS, lineSettingsToJsonObject());
            jo.put(JSON_KEY_BOX_STEM_SETTINGS, boxStemSettingsToJsonObject());
            return jo;
        } catch(JSONException e) {
            throw new RuntimeException(e);
        }
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
    
    public void setViewType(int viewType) {
        this.viewType = viewType;
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
    
    public void setBoxStemXCol(ColumnProperties cp) {
        boxStemXCol = (cp == null) ? null : cp.getColumnDbName();
        set();
    }
    
    public void setBoxStemYCol(ColumnProperties cp) {
        boxStemYCol = (cp == null) ? null : cp.getColumnDbName();
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
}
