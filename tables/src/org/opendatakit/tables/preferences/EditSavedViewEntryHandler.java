package org.opendatakit.tables.preferences;

public interface EditSavedViewEntryHandler {

	void tryToSaveNewName(String value);
	String getCurrentViewName();
	
}
