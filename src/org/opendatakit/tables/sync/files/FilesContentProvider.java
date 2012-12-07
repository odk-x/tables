package org.opendatakit.tables.sync.files;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * Empty implementation of the content provider for using FileSyncAdapter. 
 * Apparently you need a content provider, but since we are interacting with 
 * files directly, we're just going to make it empty. Is this the best way to 
 * do it? One wonders...
 * @author sudar.sam@gmail.com
 *
 */
public class FilesContentProvider extends ContentProvider {
  
  // not including a URI. should there be one?
  public static final String MESSAGE = FilesContentProvider.class.getName() 
      + " is an empty implementation of ContentProvider not for real use.";

  @Override
  public int delete(Uri arg0, String arg1, String[] arg2) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public String getType(Uri arg0) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public Uri insert(Uri arg0, ContentValues arg1) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public boolean onCreate() {
    // Apparently this really gets called, so we can't throw an exception.
    return true;
  }

  @Override
  public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3, String arg4) {
    throw new UnsupportedOperationException(MESSAGE);
  }

  @Override
  public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
    throw new UnsupportedOperationException(MESSAGE);
  }

}
