package yoonsung.odk.spreadsheet.Activity.util;

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
        case TableProperties.ViewType.TABLE:
            return "Table";
        case TableProperties.ViewType.LIST:
            return "List";
        case TableProperties.ViewType.LINE_GRAPH:
            return "Line Graph";
        default:
            throw new RuntimeException("Invalid view type (" + viewType +
                    ").");
        }
    }
}
