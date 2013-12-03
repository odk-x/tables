package org.opendatakit.hope.preferences;

public interface EditSavedViewEntryHandler {

	void tryToSaveNewName(String value);
	String getCurrentViewName();
	
}
