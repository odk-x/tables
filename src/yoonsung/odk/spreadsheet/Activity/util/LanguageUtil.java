package yoonsung.odk.spreadsheet.Activity.util;

import android.graphics.Color;
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
        case TableViewSettings.Type.MAP:
            return "Map";
        default:
            throw new RuntimeException();
        }
    }
    
    public static String getTvsConditionalComparator(int comparatorType) {
        switch (comparatorType) {
        case TableViewSettings.ConditionalRuler.Comparator.EQUALS:
            return "=";
        case TableViewSettings.ConditionalRuler.Comparator.LESS_THAN:
            return "<";
        case TableViewSettings.ConditionalRuler.Comparator.LESS_THAN_EQUALS:
            return "<=";
        case TableViewSettings.ConditionalRuler.Comparator.GREATER_THAN:
            return ">";
        case TableViewSettings.ConditionalRuler.Comparator.GREATER_THAN_EQUALS:
            return ">=";
        default:
            throw new RuntimeException();
        }
    }
    
    public static String getMapColorLabel(int color) {
        switch (color) {
        case Color.BLACK:
            return "Black";
        case Color.BLUE:
            return "Blue";
        case Color.GREEN:
            return "Green";
        case Color.RED:
            return "Red";
        case Color.YELLOW:
            return "Yellow";
        default:
            throw new RuntimeException();
        }
    }
}
