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
package org.opendatakit.tables.Activity;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;

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
    public void prepRegularCellOccm(ContextMenu menu, int cellId) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void prepHeaderCellOccm(ContextMenu menu, int cellId) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void prepIndexedColCellOccm(ContextMenu menu, int cellId) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void prepFooterCellOccm(ContextMenu menu, int cellId) {
        // TODO Auto-generated method stub
        
    }
    
}
