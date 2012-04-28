package org.opendatakit.tables.activities;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.opendatakit.tables.R;
import org.opendatakit.tables.Activity.ColumnManager;
import org.opendatakit.tables.Activity.TableManager;
import org.opendatakit.tables.Activity.TablePropertiesManager;
import org.opendatakit.tables.Activity.util.LanguageUtil;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.DataManager;
import org.opendatakit.tables.data.DataUtil;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.TableViewSettings;
import org.opendatakit.tables.data.UserTable;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;

/**
 * A controller for the elements common to the various table display
 * activities.
 * 
 * The general weirdness of how this package is structured (i.e., a Controller
 * class used by unrelated display activities, instead of just having those
 * display activities subclass a common parent) is because the Google Maps API
 * requires that activities that use MapViews extend the Android MapActivity
 * (meaning that the MapDisplayActivity couldn't extend the common display
 * activity unless the common display activity extended the Android MapActivity
 * class, which seemed undesirable since that would require that all of the
 * display activities be children of MapActivity for no good reason).
 */
public class Controller {
    
    public static final String INTENT_KEY_TABLE_ID = "tableId";
    public static final String INTENT_KEY_SEARCH = "search";
    public static final String INTENT_KEY_SEARCH_STACK = "searchStack";
    public static final String INTENT_KEY_IS_OVERVIEW = "isOverview";
    
    private static final int MENU_ITEM_ID_OPEN_TABLE_PROPERTIES = 0;
    private static final int MENU_ITEM_ID_OPEN_COLUMN_MANAGER = 1;
    private static final int MENU_ITEM_ID_CHANGE_TABLE_VIEW_TYPE = 2;
    private static final int MENU_ITEM_ID_OPEN_TABLE_MANAGER = 3;
    static final int FIRST_FREE_MENU_ITEM_ID = 4;
    
    private static final int RCODE_TABLE_PROPERTIES_MANAGER = 0;
    private static final int RCODE_COLUMN_MANAGER = 1;
    static final int FIRST_FREE_RCODE = 2;
    
    private static final String COLLECT_FORMS_URI_STRING =
        "content://org.odk.collect.android.provider.odk.forms/forms";
    private static final Uri ODKCOLLECT_FORMS_CONTENT_URI =
        Uri.parse(COLLECT_FORMS_URI_STRING);
    private static final String COLLECT_INSTANCES_URI_STRING =
        "content://org.odk.collect.android.provider.odk.instances/instances";
    private static final Uri COLLECT_INSTANCES_CONTENT_URI =
        Uri.parse(COLLECT_INSTANCES_URI_STRING);
    private static final String ODKCOLLECT_ADDROW_FILENAME =
        "/sdcard/odk/tables/addrowform.xml";
    private static final String ODKCOLLECT_ADDROW_ID = "tablesaddrowformid";
    
    private final DataUtil du;
    private final Activity activity;
    private final DisplayActivity da;
    private final DataManager dm;
    private TableProperties tp;
    private DbTable dbt;
    private TableViewSettings tvs;
    private final Stack<String> searchText;
    private final boolean isOverview;
    private final ViewGroup wrapper;
    private final EditText searchField;
    private final ViewGroup displayWrap;
    
    Controller(Activity activity, final DisplayActivity da,
            Bundle intentBundle) {
        du = DataUtil.getDefaultDataUtil();
        this.activity = activity;
        this.da = da;
        // getting intent information
        String tableId = intentBundle.getString(INTENT_KEY_TABLE_ID);
        if (tableId == null) {
            throw new RuntimeException();
        }
        searchText = new Stack<String>();
        if (intentBundle.containsKey(INTENT_KEY_SEARCH_STACK)) {
            String[] searchValues = intentBundle.getStringArray(
                    INTENT_KEY_SEARCH_STACK);
            for (String searchValue : searchValues) {
                searchText.add(searchValue);
            }
        } else {
            String initialSearchText = intentBundle.getString(
                    INTENT_KEY_SEARCH);
            searchText.add((initialSearchText == null) ? "" :
                initialSearchText);
        }
        isOverview = intentBundle.getBoolean(INTENT_KEY_IS_OVERVIEW, false);
        // initializing data objects
        dm = new DataManager(DbHelper.getDbHelper(activity));
        tp = dm.getTableProperties(tableId);
        dbt = dm.getDbTable(tableId);
        tvs = isOverview ? tp.getOverviewViewSettings() :
                tp.getCollectionViewSettings();
        // initializing view objects
        LinearLayout controlWrap = new LinearLayout(activity);
        searchField = new EditText(activity);
        searchField.setText(searchText.peek());
        ImageButton searchButton = new ImageButton(activity);
        searchButton.setImageResource(R.drawable.search_icon);
        searchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                da.onSearch();
            }
        });
        ImageButton addRowButton = new ImageButton(activity);
        addRowButton.setImageResource(R.drawable.addrow_icon);
        addRowButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                da.onAddRow();
            }
        });
        LinearLayout.LayoutParams searchFieldParams =
                new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        searchFieldParams.weight = 1;
        controlWrap.addView(searchField, searchFieldParams);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.weight = 0;
        controlWrap.addView(searchButton, buttonParams);
        controlWrap.addView(addRowButton, buttonParams);
        displayWrap = new LinearLayout(activity);
        LinearLayout wrapper = new LinearLayout(activity);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams controlParams =
                new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        wrapper.addView(controlWrap, controlParams);
        LinearLayout.LayoutParams displayParams =
                new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.FILL_PARENT);
        wrapper.addView(displayWrap, displayParams);
        this.wrapper = wrapper;
    }
    
    TableProperties getTableProperties() {
        return tp;
    }
    
    DbTable getDbTable() {
        return dbt;
    }
    
    TableViewSettings getTableViewSettings() {
        return tvs;
    }
    
    boolean getIsOverview() {
        return isOverview;
    }
    
    String getSearchText() {
        return searchText.peek();
    }
    
    View getWrapperView() {
        return wrapper;
    }
    
    void setDisplayView(View dv) {
        displayWrap.removeAllViews();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.FILL_PARENT);
        displayWrap.addView(dv, params);
    }
    
    void releaseView(View v) {
        displayWrap.removeView(v);
    }
    
    void recordSearch() {
        searchText.add(searchField.getText().toString());
    }
    
    void onBackPressed() {
        if (searchText.size() == 1) {
            activity.finish();
        } else {
            searchText.pop();
            searchField.setText(searchText.peek());
            da.init();
        }
    }
    
    void buildOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_ITEM_ID_OPEN_TABLE_PROPERTIES, Menu.NONE,
                "Table Properties");
        menu.add(Menu.NONE, MENU_ITEM_ID_OPEN_COLUMN_MANAGER, Menu.NONE,
                "Column Manager");
        menu.add(Menu.NONE, MENU_ITEM_ID_CHANGE_TABLE_VIEW_TYPE, Menu.NONE,
                "View Type");
        menu.add(Menu.NONE, MENU_ITEM_ID_OPEN_TABLE_MANAGER, Menu.NONE,
                "Table Manager");
    }
    
    boolean handleMenuItemSelection(int itemId) {
        switch (itemId) {
        case MENU_ITEM_ID_OPEN_TABLE_PROPERTIES:
            {
            Intent intent = new Intent(activity, TablePropertiesManager.class);
            intent.putExtra(TablePropertiesManager.INTENT_KEY_TABLE_ID,
                    tp.getTableId());
            activity.startActivityForResult(intent,
                    RCODE_TABLE_PROPERTIES_MANAGER);
            }
            return true;
        case MENU_ITEM_ID_OPEN_COLUMN_MANAGER:
            {
            Intent intent = new Intent(activity, ColumnManager.class);
            intent.putExtra(ColumnManager.INTENT_KEY_TABLE_ID,
                    tp.getTableId());
            activity.startActivityForResult(intent, RCODE_COLUMN_MANAGER);
            }
            return true;
        case MENU_ITEM_ID_CHANGE_TABLE_VIEW_TYPE:
            (new ViewTypeSelectorDialog()).show();
            return true;
        case MENU_ITEM_ID_OPEN_TABLE_MANAGER:
            activity.startActivity(new Intent(activity, TableManager.class));
            return true;
        default:
            return false;
        }
    }
    
    boolean handleActivityReturn(int requestCode, int returnCode,
            Intent data) {
        switch (requestCode) {
        case RCODE_TABLE_PROPERTIES_MANAGER:
            handleTablePropertiesManagerReturn();
            return true;
        case RCODE_COLUMN_MANAGER:
            handleColumnManagerReturn();
            return true;
        default:
            return false;
        }
    }
    
    private void handleTablePropertiesManagerReturn() {
        int oldViewType = tvs.getViewType();
        tp = dm.getTableProperties(tp.getTableId());
        dbt = dm.getDbTable(tp.getTableId());
        tvs = isOverview ? tp.getOverviewViewSettings() :
                tp.getCollectionViewSettings();
        if (oldViewType == tvs.getViewType()) {
            da.init();
        } else {
            launchTableActivity(activity, tp, searchText, isOverview);
            activity.finish();
        }
    }
    
    private void handleColumnManagerReturn() {
        tp = dm.getTableProperties(tp.getTableId());
        dbt = dm.getDbTable(tp.getTableId());
        tvs = isOverview ? tp.getOverviewViewSettings() :
                tp.getCollectionViewSettings();
        da.init();
    }
    
    void deleteRow(String rowId) {
        dbt.markDeleted(rowId);
    }
    
    Intent getIntentForOdkCollectAddRow() {
        try {
            FileWriter writer = new FileWriter(ODKCOLLECT_ADDROW_FILENAME);
            writer.write("<h:html xmlns=\"http://www.w3.org/2002/xforms\" " +
                    "xmlns:h=\"http://www.w3.org/1999/xhtml\" " +
                    "xmlns:ev=\"http://www.w3.org/2001/xml-events\" " +
                    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:jr=\"http://openrosa.org/javarosa\">");
            writer.write("<h:head>");
            writer.write("<h:title>Add row: " + tp.getDisplayName() +
                    "</h:title>");
            writer.write("<model>");
            writer.write("<instance>");
            writer.write("<data id=\"" + ODKCOLLECT_ADDROW_ID + "\">");
            for (ColumnProperties cp : tp.getColumns()) {
                writer.write("<" + cp.getColumnDbName() + "/>");
            }
            writer.write("</data>");
            writer.write("</instance>");
            writer.write("<itext>");
            writer.write("<translation lang=\"eng\">");
            for (ColumnProperties cp : tp.getColumns()) {
                writer.write("<text id=\"/data/" + cp.getColumnDbName() +
                        ":label\">");
                writer.write("<value>" + cp.getDisplayName() + "</value>");
                writer.write("</text>");
            }
            writer.write("</translation>");
            writer.write("</itext>");
            writer.write("</model>");
            writer.write("</h:head>");
            writer.write("<h:body>");
            for (ColumnProperties cp : tp.getColumns()) {
                writer.write("<input ref=\"/data/" + cp.getColumnDbName() +
                        "\">");
                writer.write("<label ref=\"jr:itext('/data/" +
                        cp.getColumnDbName() + ":label')\"/>");
                writer.write("</input>");
            }
            writer.write("</h:body>");
            writer.write("</h:html>");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        ContentValues insertValues = new ContentValues();
        insertValues.put("formFilePath", ODKCOLLECT_ADDROW_FILENAME);
        insertValues.put("displayName", "Add row: " + tp.getDisplayName());
        insertValues.put("jrFormId", ODKCOLLECT_ADDROW_ID);
        Uri insertResult = activity.getContentResolver().insert(
                ODKCOLLECT_FORMS_CONTENT_URI, insertValues);
        int formId = Integer.valueOf(insertResult.getLastPathSegment());
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("org.odk.collect.android",
                "org.odk.collect.android.activities.FormEntryActivity"));
        intent.setAction(Intent.ACTION_EDIT);
        intent.setData(Uri.parse(COLLECT_FORMS_URI_STRING + "/" + formId));
        return intent;
    }
    
    boolean addRowFromOdkCollectForm(int instanceId) {
        Map<String, String> formValues = getOdkCollectFormValues(instanceId);
        if (formValues == null) {
            return false;
        }
        Map<String, String> values = new HashMap<String, String>();
        for (String key : formValues.keySet()) {
            ColumnProperties cp = tp.getColumnByDbName(key);
            if (cp == null) {
                continue;
            }
            String value = du.validifyValue(cp, formValues.get(key));
            if (value != null) {
                values.put(key, value);
            }
        }
        dbt.addRow(values);
        return true;
    }
    
    private Map<String, String> getOdkCollectFormValues(int instanceId) {
        String[] projection = { "instanceFilePath" };
        String selection = "_id = ?";
        String[] selectionArgs = { (instanceId + "") };
        Cursor c = activity.managedQuery(COLLECT_INSTANCES_CONTENT_URI,
                projection, selection, selectionArgs, null);
        if (c.getCount() != 1) {
            return null;
        }
        c.moveToFirst();
        String instancepath = c.getString(c.getColumnIndexOrThrow(
                "instanceFilePath"));
        Document xmlDoc = new Document();
        KXmlParser xmlParser = new KXmlParser();
        try {
            xmlParser.setInput(new FileReader(instancepath));
            xmlDoc.parse(xmlParser);
        } catch(IOException e) {
            e.printStackTrace();
            return null;
        } catch(XmlPullParserException e) {
            e.printStackTrace();
            return null;
        }
        Element rootEl = xmlDoc.getRootElement();
        Node rootNode = rootEl.getRoot();
        Element dataEl = rootNode.getElement(0);
        Map<String, String> values = new HashMap<String, String>();
        for (int i = 0; i < dataEl.getChildCount(); i++) {
            Element child = dataEl.getElement(i);
            String key = child.getName();
            String value = child.getText(0);
            values.put(key, value);
        }
        return values;
    }
    
    void openCellEditDialog(String rowId, String value, int colIndex) {
        (new CellEditDialog(rowId, value, colIndex)).show();
    }
    
    public static void launchTableActivity(Context context, TableProperties tp,
            boolean isOverview) {
        Controller.launchTableActivity(context, tp, null, null, isOverview);
    }
    
    public static void launchTableActivity(Context context, TableProperties tp,
            String searchText, boolean isOverview) {
        Controller.launchTableActivity(context, tp, searchText, null,
                isOverview);
    }
    
    private static void launchTableActivity(Context context,
            TableProperties tp, Stack<String> searchStack,
            boolean isOverview) {
        Controller.launchTableActivity(context, tp, null, searchStack,
                isOverview);
    }
    
    private static void launchTableActivity(Context context,
            TableProperties tp, String searchText, Stack<String> searchStack,
            boolean isOverview) {
        TableViewSettings tvs = isOverview ? tp.getOverviewViewSettings() :
                tp.getCollectionViewSettings();
        Intent intent;
        switch (tvs.getViewType()) {
        case TableViewSettings.Type.LIST:
            intent = new Intent(context, ListDisplayActivity.class);
            break;
        case TableViewSettings.Type.LINE_GRAPH:
            intent = new Intent(context, LineGraphDisplayActivity.class);
            break;
        case TableViewSettings.Type.BOX_STEM:
            intent = new Intent(context, BoxStemGraphDisplayActivity.class);
            break;
        case TableViewSettings.Type.BAR_GRAPH:
            intent = new Intent(context, BarGraphDisplayActivity.class);
            break;
        case TableViewSettings.Type.MAP:
            intent = new Intent(context, MapDisplayActivity.class);
            break;
        default:
            intent = new Intent(context, SpreadsheetDisplayActivity.class);
        }
        intent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
        if (searchStack != null) {
            String[] stackValues = new String[searchStack.size()];
            for (int i = 0; i < searchStack.size(); i++) {
                stackValues[i] = searchStack.get(i);
            }
            intent.putExtra(INTENT_KEY_SEARCH_STACK, stackValues);
        } else if (searchText != null) {
            intent.putExtra(INTENT_KEY_SEARCH, searchText);
        }
        intent.putExtra(INTENT_KEY_IS_OVERVIEW, isOverview);
        context.startActivity(intent);
    }
    
    public static void launchDetailActivity(Context context,
            TableProperties tp, UserTable table, int rowNum) {
        String[] keys = new String[table.getWidth()];
        String[] values = new String[table.getWidth()];
        for (int i = 0; i < table.getWidth(); i++) {
            keys[i] = tp.getColumns()[i].getColumnDbName();
            values[i] = table.getData(rowNum, i);
        }
        Intent intent = new Intent(context, DetailDisplayActivity.class);
        intent.putExtra(INTENT_KEY_TABLE_ID, tp.getTableId());
        intent.putExtra(DetailDisplayActivity.INTENT_KEY_ROW_ID,
                table.getRowId(rowNum));
        intent.putExtra(DetailDisplayActivity.INTENT_KEY_ROW_KEYS, keys);
        intent.putExtra(DetailDisplayActivity.INTENT_KEY_ROW_VALUES, values);
        context.startActivity(intent);
    }
    
    private class ViewTypeSelectorDialog extends AlertDialog {
        
        public ViewTypeSelectorDialog() {
            super(activity);
            buildView(activity);
        }
        
        private void buildView(Context context) {
            LinearLayout wrapper = new LinearLayout(context);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            // adding the view type spinner
            int selectionIndex = 0;
            final int[] viewTypeIds = tvs.getPossibleViewTypes();
            String[] viewTypeStringIds = new String[viewTypeIds.length];
            String[] viewTypeNames = new String[viewTypeIds.length];
            for (int i = 0; i < viewTypeIds.length; i++) {
                if (tvs.getViewType() == viewTypeIds[i]) {
                    selectionIndex = i;
                }
                viewTypeStringIds[i] = String.valueOf(viewTypeIds[i]);
                viewTypeNames[i] = LanguageUtil.getViewTypeLabel(
                        viewTypeIds[i]);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                    android.R.layout.simple_spinner_item, viewTypeNames);
            adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            final Spinner spinner = new Spinner(context);
            spinner.setAdapter(adapter);
            spinner.setSelection(selectionIndex);
            wrapper.addView(spinner);
            // adding the set and cancel buttons
            Button setButton = new Button(context);
            setButton.setText(activity.getResources().getString(R.string.set));
            setButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tvs.setViewType(
                            viewTypeIds[spinner.getSelectedItemPosition()]);
                    Controller.launchTableActivity(activity, tp, searchText,
                            isOverview);
                    activity.finish();
                }
            });
            Button cancelButton = new Button(context);
            cancelButton.setText(activity.getResources().getString(
                    R.string.cancel));
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
            LinearLayout buttonWrapper = new LinearLayout(context);
            buttonWrapper.addView(setButton);
            buttonWrapper.addView(cancelButton);
            wrapper.addView(buttonWrapper);
            // setting the dialog view
            setView(wrapper);
        }
    }
    
    private class CellEditDialog extends AlertDialog {
        
        private final String rowId;
        private final int colIndex;
        private final CellValueView.CellEditView cev;
        
        public CellEditDialog(String rowId, String value, int colIndex) {
            super(activity);
            this.rowId = rowId;
            this.colIndex = colIndex;
            cev = CellValueView.getCellEditView(activity,
                    tp.getColumns()[colIndex], value);
            buildView(activity);
        }
        
        private void buildView(Context context) {
            Button setButton = new Button(context);
            setButton.setText(activity.getResources().getString(R.string.set));
            setButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String value = du.validifyValue(tp.getColumns()[colIndex],
                            cev.getValue());
                    if (value == null) {
                        // TODO: alert the user
                        return;
                    }
                    Map<String, String> values = new HashMap<String, String>();
                    values.put(tp.getColumns()[colIndex].getColumnDbName(),
                            value);
                    dbt.updateRow(rowId, values);
                    da.init();
                    dismiss();
                }
            });
            Button cancelButton = new Button(context);
            cancelButton.setText(activity.getResources().getString(
                    R.string.cancel));
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
            LinearLayout buttonWrapper = new LinearLayout(context);
            buttonWrapper.addView(setButton);
            buttonWrapper.addView(cancelButton);
            LinearLayout wrapper = new LinearLayout(context);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.addView(cev);
            wrapper.addView(buttonWrapper);
            setView(wrapper);
        }
    }
}
