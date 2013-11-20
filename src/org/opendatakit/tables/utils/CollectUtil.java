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
package org.opendatakit.tables.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringEscapeUtils;
import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.opendatakit.common.android.provider.FileProvider;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.activities.Controller;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.ColumnType;
import org.opendatakit.tables.data.DataUtil;
import org.opendatakit.tables.data.DbHelper;
import org.opendatakit.tables.data.DbTable;
import org.opendatakit.tables.data.KeyValueHelper;
import org.opendatakit.tables.data.KeyValueStore;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.Query;
import org.opendatakit.tables.data.Query.Constraint;
import org.opendatakit.tables.data.TableProperties;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.actionbarsherlock.app.SherlockActivity;

/**
 * Utility methods for using ODK Collect.
 *
 * @author:sudar.sam@gmail.com --and somebody else, unknown
 */
public class CollectUtil {

  private static final String COLLECT_KEY_LAST_STATUS_CHANGE_DATE = "date";

  private static final String UTF_8 = "UTF-8";

  private static final String TAG = "CollectUtil";

  public static final String KVS_PARTITION = "CollectUtil";
  public static final String KVS_ASPECT = "default";

  /**
   * This is the name of the shared preference to which collect util will save
   * the things it must retain. At the moment this is only the row id that is
   * being edited. The vast majority of state should not be saved here.
   */
  private static final String SHARED_PREFERENCE_NAME = "CollectUtil_Preference";
  /**
   * This is the key name of the preference whose value will be the row id that
   * is currently being edited.
   */
  private static final String PREFERENCE_KEY_EDITED_ROW_ID = "editedRowId";

  /**
   * This is the table id of the tableId that will be receiving an add row. This
   * is necessary because javascript views can launch adds for tables other than
   * themselves, and this preference will store which table id the row should be
   * added to.
   */
  private static final String PREFERENCE_KEY_TABLE_ID_ADD = "tableIdAdd";

  /*
   * The names here should match those in the version of collect that is on the
   * phone. They came from InstanceProviderApi.
   */
  public static final String COLLECT_KEY_STATUS = "status";
  public static final String COLLECT_KEY_STATUS_INCOMPLETE = "incomplete";
  public static final String COLLECT_KEY_STATUS_COMPLETE = "complete";
  public static final String COLLECT_KEY_CAN_EDIT_WHEN_COMPLETE = "canEditWhenComplete";
  public static final String COLLECT_KEY_SUBMISSION_URI = "submissionUri";
  public static final String COLLECT_KEY_INSTANCE_FILE_PATH = "instanceFilePath";
  public static final String COLLECT_KEY_JR_FORM_ID = "jrFormId";
  public static final String COLLECT_KEY_JR_VERSION = "jrVersion";
  public static final String COLLECT_KEY_DISPLAY_NAME = "displayName";
  public static final String COLLECT_INSTANCE_ORDER_BY = BaseColumns._ID + " asc";
  public static final String COLLECT_INSTANCE_AUTHORITY = "org.odk.collect.android.provider.odk.instances";
  public static final Uri CONTENT_INSTANCE_URI = Uri.parse("content://"
      + COLLECT_INSTANCE_AUTHORITY + "/instances");

  public static final String COLLECT_FORM_AUTHORITY = "org.odk.collect.android.provider.odk.forms";
  public static final Uri CONTENT_FORM_URI = Uri.parse("content://" + COLLECT_FORM_AUTHORITY
      + "/forms");
  public static final String COLLECT_KEY_FORM_FILE_PATH = "formFilePath";

  private static final String COLLECT_FORMS_URI_STRING = "content://org.odk.collect.android.provider.odk.forms/forms";
  @SuppressWarnings("unused")
  private static final Uri ODKCOLLECT_FORMS_CONTENT_URI = Uri.parse(COLLECT_FORMS_URI_STRING);
  private static final String COLLECT_INSTANCES_URI_STRING = "content://org.odk.collect.android.provider.odk.instances/instances";
  private static final Uri COLLECT_INSTANCES_CONTENT_URI = Uri.parse(COLLECT_INSTANCES_URI_STRING);

  /********************
   * Keys present in the Key Value Store. These should represent data about the
   * form that is present if a form is defined for a particular table.
   ********************/
  public static final String KEY_FORM_VERSION = "CollectUtil.collectFormVersion";
  public static final String KEY_FORM_ID = "CollectUtil.formId";
  public static final String KEY_FORM_ROOT_ELEMENT = "CollectUtil.rootElement";

  /**
   * The default value of the root element for the form.
   */
  public static final String DEFAULT_ROOT_ELEMENT = "data";

  private static final String COLLECT_ADDROW_FORM_ID_PREFIX = "tablesId_";

  /**
   * Return the formId for the single file that will be written when there is no
   * custom form defined for a table.
   *
   * @param tp
   * @return
   */
  private static String getDefaultAddRowFormId(TableProperties tp) {
    return COLLECT_ADDROW_FORM_ID_PREFIX + tp.getTableId();
  }

  /**
   * This is the file name and path of the single file that will be written that
   * contains the key values of column name to data for a given row that was
   * created.
   *
   * @return
   */
  private static File getAddRowFormFile(TableProperties tp) {
    return new File(ODKFileUtils.getTablesFolder(TableFileUtils.ODK_TABLES_APP_NAME,
        tp.getTableId()), "addrowform.xml");
  }

  /**
   * This is the file name and path of the single file that will be written that
   * contains the key values of column name to data for a given row that will be
   * edited.
   *
   * @return
   */
  private static File getEditRowFormFile(TableProperties tp, String rowId) {
    return new File(ODKFileUtils.getInstanceFolder(TableFileUtils.ODK_TABLES_APP_NAME,
        tp.getTableId(), rowId), "editRowData.xml");
  }

  /**
   * Build a default form. This form will allow being swiped through, one field
   * at a time.
   *
   * @param file
   *          the file to write the form to
   * @param columns
   *          the columnProperties of the table.
   * @param title
   *          the title of the form
   * @param formId
   *          the id of the form
   * @return true if the file was successfully written
   */
  private static boolean buildBlankForm(File file, Collection<ColumnProperties> columns,
      String title, String formId) {
    OutputStreamWriter writer = null;
    try {
      FileOutputStream out = new FileOutputStream(file);
      writer = new OutputStreamWriter(out, UTF_8);
      writer.write("<h:html xmlns=\"http://www.w3.org/2002/xforms\" "
          + "xmlns:h=\"http://www.w3.org/1999/xhtml\" "
          + "xmlns:ev=\"http://www.w3.org/2001/xml-events\" "
          + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
          + "xmlns:jr=\"http://openrosa.org/javarosa\">");
      writer.write("<h:head>");
      writer.write("<h:title>");
      writer.write(StringEscapeUtils.escapeXml(title));
      writer.write("</h:title>");
      writer.write("<model>");
      writer.write("<instance>");
      writer.write("<");
      writer.write(DEFAULT_ROOT_ELEMENT);
      writer.write(" ");
      writer.write("id=\"");
      writer.write(StringEscapeUtils.escapeXml(formId));
      writer.write("\">");
      for (ColumnProperties cp : columns) {
        writer.write("<");
        writer.write(cp.getElementKey());
        writer.write("/>");
      }
      writer.write("<meta><instanceID/></meta>");
      writer.write("</");
      writer.write(DEFAULT_ROOT_ELEMENT);
      writer.write(">");
      writer.write("</instance>");
      for (ColumnProperties cp : columns) {
        writer.write("<bind nodeset=\"/");
        writer.write(DEFAULT_ROOT_ELEMENT);
        writer.write("/");
        writer.write(cp.getElementKey());
        writer.write("\" type=\"");
        writer.write(cp.getColumnType().collectType());
        writer.write("\"/>");

      }
      writer.write("<bind nodeset=\"/");
      writer.write(DEFAULT_ROOT_ELEMENT);
      writer.write("/meta/instanceID\" type=\"string\" required=\"true()\"/>");

      writer.write("<itext>");
      writer.write("<translation lang=\"eng\">");
      for (ColumnProperties cp : columns) {
        writer.write("<text id=\"/");
        writer.write(DEFAULT_ROOT_ELEMENT);
        writer.write("/");
        writer.write(cp.getElementKey());
        writer.write(":label\">");
        writer.write("<value>");
        writer.write(cp.getDisplayName());
        writer.write("</value>");
        writer.write("</text>");
      }
      writer.write("</translation>");
      writer.write("</itext>");
      writer.write("</model>");
      writer.write("</h:head>");
      writer.write("<h:body>");
      for (ColumnProperties cp : columns) {
        String type = cp.getColumnType().collectType();
        String action = "input";
        String additionalAttributes = "";
        if ( type.equals("binary") ) {
          action = "upload";
          additionalAttributes = " mediatype=\"" + cp.getColumnType().baseContentType() + "/*\"";
        }
        writer.write("<" + action + additionalAttributes + " ref=\"/" + DEFAULT_ROOT_ELEMENT + "/" + cp.getElementKey() + "\">");
        writer.write("<label ref=\"jr:itext('/" + DEFAULT_ROOT_ELEMENT + "/" + cp.getElementKey()
            + ":label')\"/>");
        writer.write("</" + action + ">");
      }
      writer.write("</h:body>");
      writer.write("</h:html>");
      writer.flush();
      writer.close();
      return true;
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return false;
    } finally {
      try {
        writer.close();
      } catch (IOException e) {
      }
    }
  }

  private static String getJrFormId(String filepath) {
    Document formDoc = parseForm(filepath);
    String namespace = formDoc.getRootElement().getNamespace();
    Element hhtmlEl = formDoc.getElement(namespace, "h:html");
    Element hheadEl = hhtmlEl.getElement(namespace, "h:head");
    Element modelEl = hheadEl.getElement(namespace, "model");
    Element instanceEl = modelEl.getElement(namespace, "instance");
    Element dataEl = instanceEl.getElement(1);
    return dataEl.getAttributeValue(namespace, "id");
  }

  private static Document parseForm(String filepath) {
    File xmlFile = new File(filepath);
    InputStream is;
    try {
      is = new FileInputStream(xmlFile);
    } catch (FileNotFoundException e) {
      throw new IllegalStateException(e);
    }
    // Now get the reader.
    InputStreamReader isr = null;
    try {
      isr = new InputStreamReader(is, Charset.forName(FileUtils.UTF8));
    } catch (UnsupportedCharsetException e) {
      Log.w(TAG, "UTF-8 wasn't supported--trying with default charset");
      isr = new InputStreamReader(is);
    }

    Document formDoc = new Document();
    KXmlParser formParser = new KXmlParser();
    try {
      formParser.setInput(isr);
      formDoc.parse(formParser);
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (XmlPullParserException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      try {
        isr.close();
      } catch (IOException e) {
      }
    }
    return formDoc;
  }

  /**
   * Write the row for a table out to a file to later be inserted into an
   * existing Collect form. This existing form must match the fields specified
   * in params.
   * <p>
   * The file generated is at the location and name specified in
   * {@link DATA_FILE_PATH_AND_NAME}.
   *
   * @param table
   *          the user table
   * @param tp
   *          the TableProperties for the table represented by the table param
   * @param params
   *          the form parameters
   * @return true if the write succeeded
   */
  /*
   * The mechanics of this are modeled on the getIntentForOdkCollectEditRow
   * method in Controller that handles the case for editing every column in a
   * screen by screen fashion, generating the entire form on the fly.
   */
  private static boolean writeRowDataToBeEdited(Context context,
      // UserTable table, int rowNum,
      Map<String, String> values, TableProperties tp, CollectFormParameters params, String rowId) {
    /*
     * This is currently implemented thinking that all you need to have is:
     *
     * <?xml version='1.0' ?><data id="tablesaddrowformid">
     *
     * followed by a series of:
     *
     * <columnName1>firstFieldData</columnName1> ...
     * <lastColumn>lastField</lastField>
     *
     * We will just go ahead and write all the fields/columns, knowing that the
     * form will simply ignore those for which it does not have matching entry
     * fields.
     */
    OutputStreamWriter writer = null;
    try {
      FileOutputStream out = new FileOutputStream(getEditRowFormFile(tp, rowId));
      writer = new OutputStreamWriter(out, UTF_8);
      writer.write("<?xml version='1.0' ?><");
      writer.write(params.getRootElement());
      writer.write(" id=\"");
      writer.write(StringEscapeUtils.escapeXml(params.getFormId()));
      writer.write("\">");
      for (ColumnProperties cp : tp.getColumns().values()) {
        String value = (values == null) ? null : values.get(cp.getElementKey());
        writer.write("<");
        writer.write(cp.getElementKey());
        // TODO: share processing with UserTable.getDisplayTextOfData()
        ColumnType type = cp.getColumnType();
        if ( value != null &&
            (type == ColumnType.IMAGEURI ||
             type == ColumnType.AUDIOURI ||
             type == ColumnType.VIDEOURI ||
             type == ColumnType.MIMEURI )) {
          if ( value.trim().length() != 0 ) {
            @SuppressWarnings("unchecked")
            Map<String,String> ref = ODKFileUtils.mapper.readValue(value, Map.class);
            if ( ref != null ) {
              String uriFragment = ref.get("uriFragment");
              String uri = FileProvider.getAsUri(context, TableFileUtils.ODK_TABLES_APP_NAME, uriFragment);
              File f = FileProvider.getAsFile(context, uri);
              value = f.getName();
            } else {
              value = null;
            }
          } else {
            value = null;
          }
        }
        if (value == null) {
          writer.write("/>");
        } else {
          writer.write(">");
          if ( type == ColumnType.IMAGEURI ||
               type == ColumnType.AUDIOURI ||
               type == ColumnType.VIDEOURI ||
               type == ColumnType.MIMEURI ) {
            writer.write(StringEscapeUtils.escapeXml(value));
          } else if ( type == ColumnType.GEOPOINT ) {
            // If value is an empty string we don't want to call split, as
            // we'll end up with a one length array of the empty string.
            String[] parts;
            if (value.equals("")) {
              parts = new String[0];
            } else {
              parts = value.split(",");
            }
            String sep = "";
            StringBuilder b = new StringBuilder();
            for ( String p : parts ) {
              b.append(sep);
              b.append(p.trim());
              sep = " ";
            }
            // and change it to have all for parts -- lat long alt acc
            for (int count = parts.length; count < 4; ++count ) {
              b.append(sep);
              b.append("-999999");
              sep = " ";
            }
            writer.write(StringEscapeUtils.escapeXml(b.toString()));
          } else if ( type == ColumnType.DATE ) {
            // TODO: get this in the correct format...
            writer.write(StringEscapeUtils.escapeXml(value));
          } else if ( type == ColumnType.DATETIME ) {
            writer.write(StringEscapeUtils.escapeXml(value));
          } else if ( type == ColumnType.TIME ) {
            writer.write(StringEscapeUtils.escapeXml(value));
          } else {
            writer.write(StringEscapeUtils.escapeXml(value));
          }
          writer.write("</");
          writer.write(cp.getElementKey());
          writer.write(">");
        }
      }
      writer.write("<meta>");
      writer.write("<instanceID>");
      writer.write(StringEscapeUtils.escapeXml(rowId));
      writer.write("</instanceID>");
      writer.write("</meta>");
      writer.write("</");
      writer.write(params.getRootElement());
      writer.write(">");
      writer.flush();
      writer.close();
      return true;
    } catch (IOException e) {
      Log.e(TAG, "IOException while writing data file");
      e.printStackTrace();
      return false;
    } finally {
      try {
        writer.close();
      } catch (IOException e) {
      }
    }
  }

  private static boolean isExistingCollectInstanceForRowData(TableProperties tp,
      String rowId, ContentResolver resolver) {

    Cursor c = null;
    try {
        String instanceFilePath = getEditRowFormFile(tp, rowId).getAbsolutePath();
        c = resolver.query(CONTENT_INSTANCE_URI, null, COLLECT_KEY_INSTANCE_FILE_PATH + "=?",
          new String[] { instanceFilePath }, COLLECT_INSTANCE_ORDER_BY);
        if ( c.getCount() == 0 ) {
          c.close();
          return false;
        }
        c.close();
        return true;
    } catch (Exception e) {
      Log.w(TAG, "caught an exception while deleting an instance, ignoring and proceeding");
      return true; // since we don't really know what is going on...
    } finally {
    	if ( c != null && !c.isClosed()) {
    		c.close();
    	}
    }
  }


  /**
   * Insert the values existing in the file specified by
   * {@link DATA_FILE_PATH_AND_NAME} into the form specified by params.
   * <p>
   * If the display name is not defined in the {@code params} parameter then the
   * string resource is used.
   * <p>
   * The inserted row is marked as INCOMPLETE.
   * <p>
   * PRECONDITION: in order to be populated with data, the data file containing
   * the row's data must have been written, most likely by calling
   * writeRowDataToBeEdited().
   * <p>
   * PRECONDITION: previous instances should already have been deleted by now,
   * or the passed in file names should be uniqued by adding timestamps, or
   * something.
   *
   * @param params
   *          the identifying parameters for the form. Should be the same object
   *          used to write the instance file.
   * @param rowNum
   *          the row number of the row being edited
   * @param resolver
   *          the ContentResolver of the activity making the request.
   * @return
   */
  /*
   * This is based on the code at: http://code.google.com/p/opendatakit/source/
   * browse/src/org/odk/collect/android/tasks/SaveToDiskTask.java?repo=collect
   * in the method updateInstanceDatabase().
   */
  private static Uri getUriForCollectInstanceForRowData(TableProperties tp, CollectFormParameters params,
        String rowId, boolean shouldUpdate, ContentResolver resolver) {

    String instanceFilePath = getEditRowFormFile(tp, rowId).getAbsolutePath();

    ContentValues values = new ContentValues();
    // First we need to fill the values with various little things.
    values.put(COLLECT_KEY_STATUS, COLLECT_KEY_STATUS_INCOMPLETE);
    values.put(COLLECT_KEY_CAN_EDIT_WHEN_COMPLETE, Boolean.toString(true));
    values.put(COLLECT_KEY_INSTANCE_FILE_PATH, instanceFilePath);
    values.put(COLLECT_KEY_JR_FORM_ID, params.getFormId());
    // only add the version if it exists (ie not null)
    if (params.getFormVersion() != null) {
      values.put(COLLECT_KEY_JR_VERSION, params.getFormVersion());
    }

    Uri uriOfForm;
    if ( shouldUpdate ) {
      int count = resolver.update(CONTENT_INSTANCE_URI, values, COLLECT_KEY_INSTANCE_FILE_PATH + "=?",
          new String[] { instanceFilePath });
      if ( count == 0) {
        uriOfForm = resolver.insert(CONTENT_INSTANCE_URI, values);
      } else {
        Cursor c = null;
        try {
        	c = resolver.query(CONTENT_INSTANCE_URI, null, COLLECT_KEY_INSTANCE_FILE_PATH + "=?",
	            new String[] { instanceFilePath }, COLLECT_INSTANCE_ORDER_BY);

	        if ( c.moveToFirst() ) {
	          // we got a result, meaning that the form exists in collect.
	          // so we just need to set the URI.
	          int collectInstanceKey; // this is the primary key of the form in
	          // Collect's
	          // database.
	          collectInstanceKey = c.getInt(c.getColumnIndexOrThrow(BaseColumns._ID));
	          uriOfForm = (Uri.parse(CONTENT_INSTANCE_URI + "/" + collectInstanceKey));
	          c.close();
	        } else {
	          c.close();
	          throw new IllegalStateException("it was updated we should have found the record!");
	        }
        } finally {
        	if ( c != null && !c.isClosed() ) {
        		c.close();
        	}
        }
      }
    } else {
      // now we want to get the uri for the insertion.
        uriOfForm = resolver.insert(CONTENT_INSTANCE_URI, values);
    }
    return uriOfForm;
  }

  /**
   * Delete the form specified by the id given in the parameters. Does not check
   * form version.
   *
   * @param resolver
   *          ContentResolver of the calling activity
   * @param formId
   *          the id of the form to be deleted
   * @return the result of the the delete call
   */
  private static int deleteForm(ContentResolver resolver, String formId) {
    try {
      return resolver.delete(CONTENT_FORM_URI, COLLECT_KEY_JR_FORM_ID + "=?",
          new String[] { formId });
    } catch (Exception e) {
      Log.d(TAG, "caught an exception while deleting a form, returning 0 and proceeding");
      return 0;
    }
  }

  /**
   * Insert a form into collect. Returns the URI of the inserted form. Note that
   * form version is not passed in with the content values and is likely
   * therefore not considered. (Not sure exactly how collect checks this.)
   * <p>
   * Precondition: the form should not exist in Collect before this call is
   * made. In other words, the a query made on the form should return no
   * results.
   *
   * @param resolver
   *          the ContentResolver of the calling activity
   * @param formFilePath
   *          the filePath to the form
   * @param displayName
   *          the displayName of the form
   * @param formId
   *          the id of the form
   * @return the result of the insert call, likely the URI of the resulting
   *         form. If the form was not first deleted there could be a problem
   */
  private static Uri insertFormIntoCollect(ContentResolver resolver, String formFilePath,
      String displayName, String formId) {
    ContentValues insertValues = new ContentValues();
    insertValues.put(COLLECT_KEY_FORM_FILE_PATH, formFilePath);
    insertValues.put(COLLECT_KEY_DISPLAY_NAME, displayName);
    insertValues.put(COLLECT_KEY_JR_FORM_ID, formId);
    return resolver.insert(CONTENT_FORM_URI, insertValues);
  }

  /**
   * Return the URI of the form for adding a row to a table. If the formId is
   * custom defined it must exist to Collect (most likely by putting the form in
   * Collect's form folder and starting Collect once). If the form does not
   * exist, it inserts the static addRowForm information into Collect.
   * <p>
   * Display name only matters if it is a programmatically generated form.
   * <p>
   * Precondition: If formId refers to a custom form, it must have already been
   * scanned in and known to exist to Collect. If the formId is not custom, but
   * refers to a form built on the fly, it should be the id of
   * {@link COLLECT_ADDROW_FORM_ID}, and the form should already have been
   * written.
   *
   * @param resolver
   *          ContentResolver of the calling activity
   * @param formId
   *          id of the form whose uri will be returned
   * @param formDisplayName
   *          display name of the table. Only pertinent if the form has been
   *          programmatically generated.
   * @return the uri of the form.
   */
  private static Uri getUriOfForm(ContentResolver resolver, String formId) {
    Uri resultUri = null;
    Cursor c = null;
    try {
      c = resolver.query(CollectUtil.CONTENT_FORM_URI, null, CollectUtil.COLLECT_KEY_JR_FORM_ID
          + "=?", new String[] { formId }, null);
      if (!c.moveToFirst()) {
        Log.e(TAG, "query of Collect for form returned no results");
      } else {
        // we got a result, meaning that the form exists in collect.
        // so we just need to set the URI.
        int collectFormKey; // this is the primary key of the form in
        // Collect's
        // database.
        collectFormKey = c.getInt(c.getColumnIndexOrThrow(BaseColumns._ID));
        resultUri = (Uri.parse(CollectUtil.CONTENT_FORM_URI + "/" + collectFormKey));
      }
    } finally {
      if (c != null && !c.isClosed()) {
        c.close();
      }
    }
    return resultUri;
  }

  /**
   * This is a convenience method that should be called when generating non-user
   * defined forms for adding or editing rows. It calls, in this order,
   * {@link deleteForm}, {@link buildBlankForm}, and
   * {@link insertFormIntoCollect}.
   *
   * @param resolver
   *          content resolver of the calling activity
   * @param params
   * @param tp
   * @return true if every method returned successfully
   */
  private static boolean deleteWriteAndInsertFormIntoCollect(ContentResolver resolver,
      CollectFormParameters params, TableProperties tp) {
    if (params.isCustom()) {
      Log.e(TAG, "passed custom form to be deleted, rewritten, and "
          + "inserted into Collect. Not performing task.");
      return false;
    }
    CollectUtil.deleteForm(resolver, params.getFormId());
    // First we want to write the file.
    boolean writeSuccessful = CollectUtil.buildBlankForm(getAddRowFormFile(tp),
                  tp.getColumnsInOrder(), tp.getDisplayName(), params.getFormId());
    if (!writeSuccessful) {
      Log.e(TAG, "problem writing file for add row");
      return false;
    }
    // Now we want to insert the file.
    Uri insertedFormUri = CollectUtil.insertFormIntoCollect(resolver, getAddRowFormFile(tp)
        .getAbsolutePath(), tp.getDisplayName(), params.getFormId());
    if (insertedFormUri == null) {
      Log.e(TAG, "problem inserting form into collect, return uri was null");
      return false;
    }
    return true;
  }

  /**
   * Identical to
   * {@link #getIntentForOdkCollectEditRow(Context, TableProperties, Map, CollectFormParameters)}
   * , except this method constructs the map of elementKey to value for you.
   */
  /*
   * This is a move away from the general "odk add row" usage that is going on
   * when no row is defined. As I understand it, the new case will work as
   * follows.
   *
   * There exits an "tableEditRow" form for a particular table. This form, as I
   * understand it, must exist both in the tables directory, as well as in
   * Collect so that Collect can launch it with an Intent.
   *
   * You then also construct a "values" sort of file, that is the data from the
   * database that will pre-populate the fields. Mitch referred to something
   * like this as the "instance" file.
   *
   * Once you have both of these files, the form and the data, you insert the
   * data into the form. When you launch the form, it is then pre-populated with
   * data from the database.
   *
   * In order to make this work, the form must exist both within the places
   * Collect knows to look, as well as in the Tables folder. You also must know
   * the:
   *
   * collectFormVersion collectFormId collectXFormRootElement (default to
   * "data")
   *
   * These will most likely exist as keys in the key value store. They must
   * match the form.
   *
   * Other things needed will be:
   *
   * instanceFilePath // I think the filepath with all the values displayName //
   * just text, eg a row ID formId // the same thing as collectFormId?
   * formVersion status // either INCOMPLETE or COMPLETE
   *
   * Examples for how this is done in Collect can be found in the Collect code
   * in org.odk.collect.android.tasks.SaveToDiskTask.java, in the
   * updateInstanceDatabase() method.
   *
   * The functionality to construct the elementKeyToValue array from the rowNum
   * and table has been elevated. Now only the
   * CollectUtil.getIntentForOdkCollectEditRow(...) method is exposed.
   *
   * public static Intent getIntentForOdkCollectEditRow(Context context,
   * TableProperties tp, UserTable table, int rowNum, CollectFormParameters
   * params) { }
   */

  public static Intent getIntentForOdkCollectEditRow(Context context, TableProperties tp,
      Map<String, String> elementKeyToValue, String formId, String formVersion,
      String formRootElement, String rowId) {

    CollectFormParameters formParameters = CollectFormParameters.constructCollectFormParameters(tp);

    if (formId != null && !formId.equals("")) {
      formParameters.setFormId(formId);
    }
    if (formVersion != null && !formVersion.equals("")) {
      formParameters.setFormVersion(formVersion);
    }
    if (formRootElement != null && !formRootElement.equals("")) {
      formParameters.setRootElement(formRootElement);
    }
    Intent editRowIntent = CollectUtil.getIntentForOdkCollectEditRow(context, tp,
        elementKeyToValue, formParameters, rowId);

    return editRowIntent;
  }

  /**
   * Return an intent that can be used to edit a row.
   * <p>
   * The idea here is that we might want to edit a row of the table using a
   * pre-set Collect form. This form would be user-defined and would be a more
   * user-friendly thing that would display only the pertinent information for a
   * particular user.
   *
   * @param context
   * @param tp
   * @param elementKeyToValue
   * @param params
   * @return
   */
  private static Intent getIntentForOdkCollectEditRow(Context context, TableProperties tp,
      Map<String, String> elementKeyToValue, CollectFormParameters params, String rowId) {
    // Check if there is a custom form. If there is not, we want to delete
    // the old form and write the new form.
    if (!params.isCustom()) {
      boolean formIsReady = CollectUtil.deleteWriteAndInsertFormIntoCollect(
          context.getContentResolver(), params, tp);
      if (!formIsReady) {
        Log.e(TAG, "could not delete, write, or insert a generated form");
        return null;
      }
    }
    boolean shouldUpdate = CollectUtil.isExistingCollectInstanceForRowData( tp, rowId, context.getContentResolver());

    boolean writeDataSuccessful = CollectUtil.writeRowDataToBeEdited(context, elementKeyToValue, tp, params,
        rowId);
    if (!writeDataSuccessful) {
      Log.e(TAG, "could not write instance file successfully!");
    }
    Uri insertUri = CollectUtil.getUriForCollectInstanceForRowData(tp, params, rowId, shouldUpdate,
        context.getContentResolver());

    // Copied the below from getIntentForOdkCollectEditRow().
    Intent intent = new Intent();
    intent.setComponent(new ComponentName("org.odk.collect.android",
        "org.odk.collect.android.activities.FormEntryActivity"));
    intent.setAction(Intent.ACTION_EDIT);
    intent.setData(insertUri);
    //intent.putExtra("start", true); // jump right into form
    return intent;
  }

  /**
   * Launch collect with the given intent. This method should be used rather
   * than launching the activity yourself because the rowId needs to be retained
   * in order to update the database.
   *
   * @param activityToAwaitReturn
   * @param collectEditIntent
   * @param rowId
   */
  public static void launchCollectToEditRow(Activity activityToAwaitReturn,
      Intent collectEditIntent, String rowId) {
    // We want to be able to launch an edit row action from a variety of
    // different activities, such as the spreadsheet and the webviews. In
    // order to update the database, we must know what the row id of the row
    // was which we are editing. There appears to be no way to pass this
    // information to collect and have it return it to us, so we're going to
    // store it in a shared preference.
    //
    // Note that we aren't storing this in the key value store because it is
    // a very temporary bit of state that would be meaningless if the call
    // and return to/from collect was interrupted.
    SharedPreferences preferences = activityToAwaitReturn.getSharedPreferences(
        SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
    preferences.edit().putString(PREFERENCE_KEY_EDITED_ROW_ID, rowId).commit();
    activityToAwaitReturn.startActivityForResult(collectEditIntent,
        Controller.RCODE_ODKCOLLECT_EDIT_ROW);
  }

  /**
   * Launch Collect with the given Intent. This method should be used rather
   * than launching the Intent yourself if the row is going to be added into a
   * table other than that which you are currently displaying. This method
   * handles storing the table id of that table so that it can be reclaimed when
   * the activity returns.
   * <p>
   * Launches with the return code
   * {@link Controller#RCODE_ODK_COLLECT_ADD_ROW_SPECIFIED_TABLE}.
   *
   * @param activityToAwaitReturn
   * @param collectAddIntent
   * @param tp
   *          the TableProperties of the table that will be receiving the add
   *          row from Collect
   */
  public static void launchCollectToAddRow(Activity activityToAwaitReturn, Intent collectAddIntent,
      TableProperties tp) {
    // We want to save the id of the table that is going to receive the row
    // that returns from Collect. We'll store it in a SharedPreference so
    // that we can get at it.
    SharedPreferences preferences = activityToAwaitReturn.getSharedPreferences(
        SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
    preferences.edit().putString(PREFERENCE_KEY_TABLE_ID_ADD, tp.getTableId()).commit();
    activityToAwaitReturn.startActivityForResult(collectAddIntent,
        Controller.RCODE_ODK_COLLECT_ADD_ROW_SPECIFIED_TABLE);
  }
  /**
   * This gets a map of values for insertion into a row after returning from a
   * Collect form. It handles validating the values and removes nulls from the
   * map.
   *
   * @return
   */
  public static Map<String, String> getMapForInsertion(Context context, TableProperties tp,
      FormValues formValues) {
    DataUtil du = DataUtil.getDefaultDataUtil();
    Map<String, String> values = new HashMap<String, String>();
    for (ColumnProperties cp : tp.getColumns().values()) {
      // we want to use element key here
      String elementKey = cp.getElementKey();
      String value = formValues.formValues.get(elementKey);
      value = du.validifyValue(cp, formValues.formValues.get(elementKey));
      // reset b/c validifyValue can return null.
      ColumnType type = cp.getColumnType();
      if (type == ColumnType.AUDIOURI) {
        value = du.serializeAsMimeUri(context, tp, formValues.instanceID, type.baseContentType(), value);
      } else if (type == ColumnType.IMAGEURI) {
        value = du.serializeAsMimeUri(context, tp, formValues.instanceID, type.baseContentType(), value);
      } else if (type == ColumnType.MIMEURI) {
        value = du.serializeAsMimeUri(context, tp, formValues.instanceID, type.baseContentType(), value);
      } else if (type == ColumnType.VIDEOURI) {
        value = du.serializeAsMimeUri(context, tp, formValues.instanceID, type.baseContentType(), value);
      }
      if (value != null) {
        values.put(elementKey, value);
      }
    }
    return values;
  }

  /**
   * Returns true if the instance has been marked as complete/finalized. If the
   * instance cannot be found or is not marked as complete, returns false.
   *
   * @param context
   * @param instanceId
   * @return
   */
  private static boolean instanceIsFinalized(Context context, int instanceId) {
    String[] projection = { COLLECT_KEY_STATUS };
    String selection = "_id = ?";
    String[] selectionArgs = { instanceId + "" };
    Cursor c = null;
    try {
	    c = context.getContentResolver().query(COLLECT_INSTANCES_CONTENT_URI,
	    		projection, selection, selectionArgs, COLLECT_INSTANCE_ORDER_BY);

	    if (c.getCount() == 0) {
	      return false;
	    }
	    c.moveToFirst();
	    String status = c.getString(c.getColumnIndexOrThrow(COLLECT_KEY_STATUS));
	    // potential status values are incomplete, complete, submitted, submission_failed
	    // all but the incomplete status indicate a marked-as-complete record.
	    if (status != null && !status.equals(COLLECT_KEY_STATUS_INCOMPLETE)) {
	      return true;
	    } else {
	      return false;
	    }
    } finally {
    	if ( c != null && !c.isClosed()) {
    		c.close();
    	}
    }
  }

  private static class FormValues {
    Map<String,String> formValues = new HashMap<String,String>();
    Long timestamp; // should be endTime in form?
    String instanceID;
    String formId;
    String locale;
    // TODO: clarify whether this is the userId or the uriAccessControl
    // at this point. It might need to be the userId...
    String accessControl;

    FormValues() {};
  };

  /**
   * Return the Collect form values from the given instance id.
   *
   * @param context
   * @param instanceId
   * @return
   */
  public static FormValues getOdkCollectFormValuesFromInstanceId(Context context,
      int instanceId) {
    String[] projection = { COLLECT_KEY_LAST_STATUS_CHANGE_DATE, "displayName", "instanceFilePath" };
    String selection = "_id = ?";
    String[] selectionArgs = { (instanceId + "") };
    Cursor c = null;
    try {
    	c = context.getContentResolver().query(COLLECT_INSTANCES_CONTENT_URI, projection,
    			selection, selectionArgs, null);
	    if (c.getCount() != 1) {
	      return null;
	    }
	    c.moveToFirst();
	    FormValues fv = new FormValues();
	    fv.timestamp = c.getLong(c.getColumnIndexOrThrow(COLLECT_KEY_LAST_STATUS_CHANGE_DATE));
	    String instancepath = c.getString(c.getColumnIndexOrThrow("instanceFilePath"));
	    File instanceFile = new File(instancepath);
	    parseXML(fv, instanceFile);
	    return fv;
    } finally {
    	if ( c != null && !c.isClosed() ) {
    		c.close();
    	}
    }
  }

  /**
   * Retrieves the tableId that was stored during the call to
   * {@link CollectUtil#launchCollectToAddRow(Activity, Intent, TableProperties)}
   * . Removes the tableId so that future calls to the same method will return
   * null.
   *
   * @param context
   * @return the stored tableId, or null if no tableId was found.
   */
  public static String retrieveAndRemoveTableIdForAddRow(Context context) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE_NAME,
        Context.MODE_PRIVATE);
    String tableId = sharedPreferences.getString(PREFERENCE_KEY_TABLE_ID_ADD, null);
    sharedPreferences.edit().remove(PREFERENCE_KEY_TABLE_ID_ADD).commit();
    return tableId;
  }

  private static boolean updateRowFromOdkCollectInstance(Context context, TableProperties tp,
      int instanceId) {
    // First we need to check to make sure the row id is in the shared
    // preferences. If it's not, something has gone wrong.
    // TODO: This should be migrated to use metadata/instanceID in the
    // instance xpath.
    SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE_NAME,
        Context.MODE_PRIVATE);
    String rowId = sharedPreferences.getString(PREFERENCE_KEY_EDITED_ROW_ID, null);
    if (rowId == null) {
      // Then it wasn't retained and something went wrong.
      Log.e(TAG, "rowId retrieved from shared preferences was null.");
      return false;
    }
    FormValues formValues = CollectUtil.getOdkCollectFormValuesFromInstanceId(context,
        instanceId);
    if (formValues == null) {
      return false;
    }
    Map<String, String> values = CollectUtil.getMapForInsertion(context, tp, formValues);
    DbHelper dbh = DbHelper.getDbHelper(context);
    DbTable dbTable = DbTable.getDbTable(dbh, tp);
    dbTable.updateRow(rowId, values, formValues.accessControl, formValues.timestamp, formValues.formId, formValues.locale);
    // If we made it here and there were no errors, then clear the row id
    // from the shared preferences. This is just a bit of housekeeping that
    // will mean there's no you could accidentally wind up overwriting the
    // wrong row.
    sharedPreferences.edit().remove(PREFERENCE_KEY_EDITED_ROW_ID).commit();
    return true;
  }

  /**
   * Returns false if the returnCode is not ok or if the instance pointed to by
   * the intent was not marked as finalized.
   * <p>
   * Otherwise returns the result of
   * {@link #updateRowFromOdkCollectInstance(Context, TableProperties, int)}.
   *
   * @param context
   * @param tp
   * @param returnCode
   * @param data
   * @return
   */
  public static boolean handleOdkCollectEditReturn(Context context, TableProperties tp,
      int returnCode, Intent data) {
    if (returnCode != SherlockActivity.RESULT_OK) {
      Log.i(TAG, "return code wasn't sherlock_ok, not inserting " + "edited data.");
      return false;
    }
    int instanceId = Integer.valueOf(data.getData().getLastPathSegment());
    if (!instanceIsFinalized(context, instanceId)) {
      Log.i(TAG, "instance wasn't marked as finalized--not updating");
      return false;
    }
    return updateRowFromOdkCollectInstance(context, tp, instanceId);
  }

  /**
   * Returns false if the returnCode is not ok or if the instance pointed to by
   * the intent was not marked as finalized.
   * <p>
   * Otherwise returns the result of
   * {@link #addRowFromOdkCollectInstance(Context, TableProperties, int)}.
   *
   * @param context
   * @param tp
   * @param returnCode
   * @param data
   * @return
   */
  public static boolean handleOdkCollectAddReturn(Context context, TableProperties tp,
      int returnCode, Intent data) {
    if (returnCode != SherlockActivity.RESULT_OK) {
      Log.i(TAG, "return code wasn't sherlock_ok--not adding row");
      return false;
    }
    int instanceId = Integer.valueOf(data.getData().getLastPathSegment());
    if (!instanceIsFinalized(context, instanceId)) {
      Log.i(TAG, "instance wasn't finalized--not adding");
      return false;
    }
    return addRowFromOdkCollectInstance(context, tp, instanceId);
  }

  private static boolean addRowFromOdkCollectInstance(Context context, TableProperties tp,
      int instanceId) {
    FormValues formValues = CollectUtil.getOdkCollectFormValuesFromInstanceId(context,
        instanceId);
    if (formValues == null) {
      return false;
    }
    Map<String, String> values = getMapForInsertion(context, tp, formValues);
    DbTable dbTable = DbTable.getDbTable(DbHelper.getDbHelper(context), tp);
    dbTable.addRow(values, formValues.instanceID, formValues.timestamp, formValues.accessControl,
            formValues.formId, formValues.locale);
    return true;
  }

  public static Intent getIntentForOdkCollectAddRowByQuery(Context context, TableProperties tp,
      CollectFormParameters params, String queryString) {
    Intent intentAddRow;
    if (queryString == null || queryString.length() == 0) {
      intentAddRow = CollectUtil.getIntentForOdkCollectAddRow(context, tp, params, null);
    } else {
      Map<String, String> elementKeyToValue =
          CollectUtil.getMapFromQuery(context, tp, queryString);
      intentAddRow = CollectUtil.getIntentForOdkCollectAddRow(context, tp, params,
          elementKeyToValue);
    }
    return intentAddRow;
  }

  /**
   * Return an intent that can be launched to add a row.
   *
   * @param context
   * @param tp
   * @param params
   * @param elementKeyToValue
   *          values with which you want to prepopulate the add row form.
   * @return
   */
  public static Intent getIntentForOdkCollectAddRow(Context context, TableProperties tp,
      CollectFormParameters params, Map<String, String> elementKeyToValue) {
    /*
     * So, there are several things to check here. The first thing we want to do
     * is see if a custom form has been defined for this table. If there is not,
     * then we will need to write a custom one. When we do this, we will then
     * have to call delete on Collect to remove the old form, which may have
     * used the same id. This will not fail if a form has not been already been
     * written--delete will simply return 0.
     */
    // Check if there is a custom form. If there is not, we want to delete
    // the old form and write the new form.
    if (!params.isCustom()) {
      boolean formIsReady = CollectUtil.deleteWriteAndInsertFormIntoCollect(
          context.getContentResolver(), params, tp);
      if (!formIsReady) {
        Log.e(TAG, "could not delete, write, or insert a generated form");
        return null;
      }
    }
    // manufacture a rowId for this record...
    String rowId = "uuid:" + UUID.randomUUID().toString();

    boolean shouldUpdate = CollectUtil.isExistingCollectInstanceForRowData( tp, rowId, context.getContentResolver());

    // emit the empty or partially-populated instance
    // we've received some values to prepopulate the add row with.
    boolean writeDataSuccessful = CollectUtil.writeRowDataToBeEdited(context, elementKeyToValue, tp, params,
        rowId);
    if (!writeDataSuccessful) {
      Log.e(TAG, "could not write instance file successfully!");
    }
    // Here we'll just act as if we're inserting 0, which
    // really doesn't matter?
    Uri formToLaunch = CollectUtil.getUriForCollectInstanceForRowData(tp, params, rowId, shouldUpdate,
        context.getContentResolver());

    // And now finally create the intent.
    Intent intent = new Intent();
    intent.setComponent(new ComponentName("org.odk.collect.android",
        "org.odk.collect.android.activities.FormEntryActivity"));
    intent.setAction(Intent.ACTION_EDIT);
    intent.setData(formToLaunch);
    intent.putExtra("start", true); // jump right into form
    return intent;
  }

  /**
   * Parse the given xml file and return a map of element to value.
   * <p>
   * Based on Collect's {@code parseXML} in {@code FileUtils}.
   *
   * @param xmlFile
   * @return
   */
  private static void parseXML(FormValues fv, File xmlFile) {

    InputStream is;
    try {
      is = new FileInputStream(xmlFile);
    } catch (FileNotFoundException e) {
      throw new IllegalStateException(e);
    }
    // Now get the reader.
    InputStreamReader isr;
    try {
      isr = new InputStreamReader(is, Charset.forName(FileUtils.UTF8));
    } catch (UnsupportedCharsetException e) {
      Log.w(TAG, "UTF-8 wasn't supported--trying with default charset");
      isr = new InputStreamReader(is);
    }
    if (isr != null) {
      Document document;
      try {
        document = new Document();
        KXmlParser parser = new KXmlParser();
        try {
          parser.setInput(isr);
          document.parse(parser);
        } catch (XmlPullParserException e) {
          Log.e(TAG, "problem with xmlpullparse");
          e.printStackTrace();
        } catch (IOException e) {
          Log.e(TAG, "io exception when parsing");
          e.printStackTrace();
        }
      } finally {
        try {
          isr.close();
        } catch (IOException e) {
          Log.e(TAG, "couldn't close reader");
          e.printStackTrace();
        }
      }
      Element rootEl = document.getRootElement();
      fv.locale = Locale.getDefault().getLanguage();
      fv.formId = rootEl.getAttributeValue(null, "id");
      Node rootNode = rootEl.getRoot();
      Element dataEl = rootNode.getElement(0);
      for (int i = 0; i < dataEl.getChildCount(); i++) {
        Element child = dataEl.getElement(i);
        String key = child.getName();
        if ( key.equals("meta") ) {
          for ( int j = 0 ; j < child.getChildCount(); j++) {
            Element e = child.getElement(j);
            String name = e.getName();
            if ( name.equals("instanceID") ) {
              fv.instanceID = ODKFileUtils.getXMLText(e, false);
            }
          }
        } else {
          String value = ODKFileUtils.getXMLText(child, false);
          fv.formValues.put(key, value);
        }
      }
    }
  }

  /**
   * Construct a map based on the query string. The idea is that you can pass in
   * the current query and get back a map of values that will represent the
   * values that should prepopulate to the add row form.
   * <p>
   * If the user has searched for facility_code: 12345, for example, then if
   * they choose to add a row, the facility_code should perhaps be pre-
   * populated with 12345. This method provides the map of elementKey to value
   * for the given query.
   *
   * @param query
   * @return
   */
  private static Map<String, String> getMapFromQuery(Context context, TableProperties tp, String queryString) {
    Map<String, String> elementKeyToValue = new HashMap<String, String>();
    // First add all empty strings. We will overwrite the ones that are
    // queried
    // for in the search box. We need this so that if an add is canceled, we
    // can check for equality and know not to add it. If we didn't do this,
    // but we've prepopulated an add with a query, when we return and don't
    // do
    // a check, we'll add a blank row b/c there are values in the key value
    // pairs, even though they were our prepopulated values.
    for (ColumnProperties cp : tp.getColumns().values()) {
      elementKeyToValue.put(cp.getElementKey(), "");
    }
    Query currentQuery = new Query(DbHelper.getDbHelper(context), KeyValueStore.Type.ACTIVE, tp);
    currentQuery.loadFromUserQuery(queryString);
    for (int i = 0; i < currentQuery.getConstraintCount(); i++) {
      Constraint constraint = currentQuery.getConstraint(i);
      // NB: This is predicated on their only ever being a single
      // search value. I'm not sure how additional values could be
      // added.
      elementKeyToValue.put(constraint.getColumnDbName(), constraint.getValue(0));
    }
    return elementKeyToValue;
  }

  /**
   * This is holds the most basic information needed to define a form to be
   * opened by Collect. Essentially it wraps the formVersion, formId, and
   * formXMLRootElement.
   * <p>
   * Its accessor methods return the default values or the set values as
   * appropriate, so that calling the getters will be safe and there will be no
   * need for checking returned values for null or whatever else.
   * <p>
   * At least at the moment, this is conceptualized to exist alongside the
   * current interaction with Collect, which writes out a complete form that
   * includes every column in the database on a single swipe through screen.
   * This instead is supposed to fill a pre-defined form that has been set by
   * the user.
   *
   * @author sudar.sam@gmail.com
   *
   */
  public static class CollectFormParameters {

    private String mFormId;
    private String mFormVersion;
    private String mFormXMLRootElement;
    private String mRowDisplayName;
    private boolean mIsCustom;

    /*
     * Just putting this here in case it needs to be serialized at some point
     * and someone forgets about this requirement.
     */
    private CollectFormParameters() {
    }

    /**
     * Create an object housing parameters for a Collect form. Very important is
     * the isCustom parameter, which should be true is a custom form has been
     * defined, and false otherwise. This will have implications for which forms
     * are used and deleted and created, and is very important to get right.
     *
     * @param isCustom
     * @param formId
     * @param formVersion
     * @param formXMLRootElement
     */
    private CollectFormParameters(boolean isCustom, String formId, String formVersion,
        String formXMLRootElement, String rowDisplayName) {
      this.mIsCustom = isCustom;
      this.mFormId = formId;
      this.mFormVersion = formVersion;
      this.mFormXMLRootElement = formXMLRootElement;
      this.mRowDisplayName = rowDisplayName;
    }

    /**
     * Construct a CollectFormProperties object from the given TableProperties.
     * The object is determined to have custom parameters if a formId can be
     * retrieved from the TableProperties object. Otherwise the default addrow
     * parameters are set. If no formVersion is defined, it is left as null, as
     * later on a check is used that if none is defined (ie is null), do not
     * insert it to a map. If no root element is defined, the default root
     * element is added.
     * <p>
     * The display name of the row will be the display name of the table.
     *
     * @param tp
     * @return
     */
    public static CollectFormParameters constructCollectFormParameters(TableProperties tp) {
      KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(CollectUtil.KVS_PARTITION);
      KeyValueHelper aspectHelper = kvsh.getAspectHelper(CollectUtil.KVS_ASPECT);
      String formId = aspectHelper.getString(CollectUtil.KEY_FORM_ID);
      if (formId == null) {
        return new CollectFormParameters(false, getDefaultAddRowFormId(tp), null,
            DEFAULT_ROOT_ELEMENT, tp.getDisplayName());
      }
      // Else we know it is custom.
      String formVersion = aspectHelper.getString(CollectUtil.KEY_FORM_VERSION);
      String rootElement = aspectHelper.getString(CollectUtil.KEY_FORM_ROOT_ELEMENT);
      if (rootElement == null) {
        rootElement = DEFAULT_ROOT_ELEMENT;
      }
      return new CollectFormParameters(true, formId, formVersion, rootElement, tp.getDisplayName());
    }

    /**
     * Sets the form id and marks the form as custom.
     *
     * @param formId
     */
    public void setFormId(String formId) {
      this.mFormId = formId;
      this.mIsCustom = true;
    }

    /**
     * Sets the form version and marks the form as custom.
     *
     * @param formVersion
     */
    public void setFormVersion(String formVersion) {
      this.mFormVersion = formVersion;
      this.mIsCustom = true;
    }

    /**
     * Sets the root element and marks the form as custom.
     *
     * @param rootElement
     */
    public void setRootElement(String rootElement) {
      this.mFormXMLRootElement = rootElement;
      this.mIsCustom = true;
    }

    /**
     * Sets the row display name and marks the form as custom.
     *
     * @param name
     */
    public void setRowDisplayName(String name) {
      this.mRowDisplayName = name;
      this.mIsCustom = true;
    }

    public void setIsCustom(boolean isCustom) {
      this.mIsCustom = isCustom;
    }

    public boolean isCustom() {
      return this.mIsCustom;
    }

    /**
     * Return the root element of the form to be used for writing. If none has
     * been set, returns the {@link DEFAULT_ROOT_ELEMENT}.
     *
     * @return
     */
    public String getRootElement() {
      if (this.mFormXMLRootElement == null) {
        return DEFAULT_ROOT_ELEMENT;
      } else {
        return this.mFormXMLRootElement;
      }
    }

    /**
     * Return the form version. This does not do any null checking. A null value
     * means that no form version has been specified and it should just be
     * omitted.
     *
     * @return
     */
    public String getFormVersion() {
      return this.mFormVersion;
    }

    /**
     * Return the ID of the form.
     *
     * @return
     */
    public String getFormId() {
      return this.mFormId;
    }

    public String getRowDisplayName() {
      return this.mRowDisplayName;
    }
  }
}
