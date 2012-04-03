package yoonsung.odk.spreadsheet.sync;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * Empty implementation of content provider for using the SyncAdapter.
 * 
 * @author the.dylan.price@gmail.com
 * 
 */
public class TablesContentProvider extends ContentProvider {

	public static final String AUTHORITY = "yoonsung.odk.spreadsheet.sync.tablescontentprovider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/tables");
	public static final String MESSAGE = TablesContentProvider.class.getName()
			+ " is an empty implementation of ContentProvider. It is not meant for actual use.";

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public String getType(Uri uri) {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public boolean onCreate() {
		// this actually gets called so we can't throw exception
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		throw new UnsupportedOperationException(MESSAGE);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		throw new UnsupportedOperationException(MESSAGE);
	}

}
