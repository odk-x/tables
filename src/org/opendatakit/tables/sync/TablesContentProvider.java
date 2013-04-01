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
package org.opendatakit.tables.sync;

import org.opendatakit.tables.util.TableFileUtils;

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

	public static final String AUTHORITY = "org.opendatakit.tables.android.provider.content";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TableFileUtils.ODK_TABLES_APP_NAME);

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
