package yoonsung.odk.spreadsheet.Activity;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class ShortcutTableActivity extends TableActivity {
    
    // options menu IDs
    private static final int OPEN_TABLE_MANAGER = 0;
    private static final int IMPORTEXPORT = 1;
    
    /**
     * Initializes the contents of the standard options menu.
     * @param menu the options menu to place items in
     * @return true if the menu is to be displayed; false otherwise
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int none = Menu.NONE;
        menu.add(none, OPEN_TABLE_MANAGER, none, "Table Manager");
        menu.add(none, IMPORTEXPORT, none, "Import/Export");
        return true;
    }
    
    /**
     * Called when an item in the options menu is selected.
     * @param featureId the panel that the menu is in
     * @param item the menu item that was selected.
     * @return true to finish processing of selection; false to perform normal
     * menu handling
     */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case OPEN_TABLE_MANAGER:
            openTableManager();
            return true;
        case IMPORTEXPORT:
            openImportExportScreen();
            return true;
        default:
            return super.onMenuItemSelected(featureId, item);
        }
    }
    
    @Override
    public void prepRegularCellOccmListener(TextView cell) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void prepHeaderCellOccmListener(TextView cell) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void prepIndexedColCellOccmListener(TextView cell) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void prepFooterCellOccmListener(TextView cell) {
        // TODO Auto-generated method stub
        
    }
    
}
