package yoonsung.odk.spreadsheet.Activity.util;

import yoonsung.odk.spreadsheet.data.TableProperties;
import yoonsung.odk.spreadsheet.data.TableViewSettings;


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
        switch (viewType) {
        case TableViewSettings.Type.SPREADSHEET:
            return "Spreadsheet";
        case TableViewSettings.Type.LIST:
            return "List";
        case TableViewSettings.Type.LINE_GRAPH:
            return "Line Graph";
        case TableViewSettings.Type.BOX_STEM:
            return "Box-Stem Graph";
        case TableViewSettings.Type.BAR_GRAPH:
            return "Bar Graph";
        default:
            throw new RuntimeException();
        }
    }
}
