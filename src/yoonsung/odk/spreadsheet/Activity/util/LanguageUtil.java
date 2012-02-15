package yoonsung.odk.spreadsheet.Activity.util;

import yoonsung.odk.spreadsheet.data.Preferences;
import yoonsung.odk.spreadsheet.data.TableProperties;


public class LanguageUtil {
    
    public static String getTableTypeLabel(int tableType) {
        switch (tableType) {
        case TableProperties.TableType.DATA:
            return "Data";
        case TableProperties.TableType.SECURITY:
            return "Access Control";
        case TableProperties.TableType.SHORTCUT:
            return "Shortcut";
        default:
            throw new RuntimeException("Invalid table type (" + tableType +
                    ").");
        }
    }
    
    public static String getViewTypeLabel(int viewType) {
        switch(viewType) {
        case Preferences.ViewType.TABLE:
            return "Table";
        case Preferences.ViewType.LIST:
            return "List";
        case Preferences.ViewType.LINE_GRAPH:
            return "Line Graph";
        case Preferences.ViewType.MAP:
            return "Map";
        default:
            throw new RuntimeException("Invalid view type (" + viewType +
                    ").");
        }
    }
}
