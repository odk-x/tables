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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.opendatakit.common.android.utilities.ODKFileUtils;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.KeyValueHelper;
import org.opendatakit.tables.data.KeyValueStoreHelper;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Utility methods for using ODK Collect.
 *
 * @author:sudar.sam@gmail.com --and somebody else, unknown
 */
public class CollectUtil {

  private static final String TAG = "CollectUtil";

  public static final String KVS_PARTITION = "CollectUtil";
  public static final String KVS_ASPECT = "default";

  /*
   * The names here should match those in the version of collect that is on
   * the phone. They came from InstanceProviderApi.
   */
  public static final String COLLECT_KEY_STATUS = "status";
  public static final String COLLECT_KEY_STATUS_INCOMPLETE = "incomplete";
  public static final String COLLECT_KEY_STATUS_COMPLETE = "complete";
  public static final String COLLECT_KEY_CAN_EDIT_WHEN_COMPLETE =
      "canEditWhenComplete";
  public static final String COLLECT_KEY_SUBMISSION_URI = "submissionUri";
  public static final String COLLECT_KEY_INSTANCE_FILE_PATH =
      "instanceFilePath";
  public static final String COLLECT_KEY_JR_FORM_ID = "jrFormId";
  public static final String COLLECT_KEY_JR_VERSION = "jrVersion";
  public static final String COLLECT_KEY_DISPLAY_NAME = "displayName";
  public static final String COLLECT_INSTANCE_AUTHORITY =
      "org.odk.collect.android.provider.odk.instances";
  public static final Uri CONTENT_INSTANCE_URI =
      Uri.parse("content://" + COLLECT_INSTANCE_AUTHORITY + "/instances");

  public static final String COLLECT_ADDROW_FORM_ID = "tablesaddrowformid";

  public static final String COLLECT_FORM_AUTHORITY =
      "org.odk.collect.android.provider.odk.forms";
  public static final Uri CONTENT_FORM_URI =
      Uri.parse("content://" + COLLECT_FORM_AUTHORITY + "/forms");
  public static final String COLLECT_KEY_FORM_FILE_PATH = "formFilePath";


  /********************
   * Keys present in the Key Value Store. These should represent data about
   * the form that is present if a form is defined for a particular table.
   ********************/
  public static final String KEY_FORM_VERSION =
      "CollectUtil.collectFormVersion";
  public static final String KEY_FORM_ID = "CollectUtil.formId";
  public static final String KEY_FORM_ROOT_ELEMENT = "CollectUtil.rootElement";


  /**
   * The default value of the root element for the form.
   */
  public static final String DEFAULT_ROOT_ELEMENT = "data";

  /**
   * This is the file name and path of the single file that will be written
   * that contains the key values of column name to data for a given row
   * that was created.
   *
   * @return
   */
  private static File getAddRowFormFile() {
    return new File(ODKFileUtils.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME), "addrowform.xml");
  }

  /**
   * This is the file name and path of the single file that will be written
   * that contains the key values of column name to data for a given row
   * that will be edited.
   *
   * @return
   */
  private static File getEditRowFormFile() {
    return new File(ODKFileUtils.getAppFolder(TableFileUtils.ODK_TABLES_APP_NAME), "editRowData.xml");
  }

  /**
   * Build a default form. This form will allow being swiped through,
   * one field at a time.
   * @param file the file to write the form to
   * @param columns the columnProperties of the table.
   * @param title the title of the form
   * @param formId the id of the form
   * @return true if the file was successfully written
   */
  public static boolean buildBlankForm(File file,
      ColumnProperties[] columns, String title, String formId) {
    try {
      FileWriter writer = new FileWriter(file);
      writer.write("<h:html xmlns=\"http://www.w3.org/2002/xforms\" "
          + "xmlns:h=\"http://www.w3.org/1999/xhtml\" "
          + "xmlns:ev=\"http://www.w3.org/2001/xml-events\" "
          + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
          + "xmlns:jr=\"http://openrosa.org/javarosa\">");
      writer.write("<h:head>");
      writer.write("<h:title>" + title + "</h:title>");
      writer.write("<model>");
      writer.write("<instance>");
      writer.write("<" + DEFAULT_ROOT_ELEMENT + " ");
      writer.write("id=\"" + formId + "\">");
      for (ColumnProperties cp : columns) {
        writer.write("<" + cp.getElementName() + "/>");
      }
      writer.write("</" + DEFAULT_ROOT_ELEMENT + ">");
      writer.write("</instance>");
      writer.write("<itext>");
      writer.write("<translation lang=\"eng\">");
      for (ColumnProperties cp : columns) {
        writer.write("<text id=\"/" + DEFAULT_ROOT_ELEMENT + "/" +
          cp.getElementName() + ":label\">");
        writer.write("<value>" + cp.getDisplayName() + "</value>");
        writer.write("</text>");
      }
      writer.write("</translation>");
      writer.write("</itext>");
      writer.write("</model>");
      writer.write("</h:head>");
      writer.write("<h:body>");
      for (ColumnProperties cp : columns) {
        writer.write("<input ref=\"/" + DEFAULT_ROOT_ELEMENT + "/" +
          cp.getElementName() + "\">");
        writer.write("<label ref=\"jr:itext('/" + DEFAULT_ROOT_ELEMENT + "/" +
          cp.getElementName() + ":label')\"/>");
        writer.write("</input>");
      }
      writer.write("</h:body>");
      writer.write("</h:html>");
      writer.close();
      return true;
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return false;
    }
  }

    public static String getJrFormId(String filepath) {
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
        Document formDoc = new Document();
        KXmlParser formParser = new KXmlParser();
        try {
            formParser.setInput(new FileReader(filepath));
            formDoc.parse(formParser);
        } catch(FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch(XmlPullParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch(IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return formDoc;
    }

    /**
     * Write the row for a table out to a file to later be inserted into an
     * existing Collect form. This existing form must match the fields
     * specified in params.
     * <p>
     * The file generated is at the location and name specified in
     * {@link DATA_FILE_PATH_AND_NAME}.
     * @param table the user table
     * @param tp the TableProperties for the table represented by the table
     *   param
     * @param params the form parameters
     * @return true if the write succeeded
     */
    /*
     * The mechanics of this are modeled on the getIntentForOdkCollectEditRow
     * method in Controller that handles the case for editing every column
     * in a screen by screen fashion, generating the entire form on the fly.
     */
    public static boolean writeRowDataToBeEdited(//UserTable table, int rowNum,
        Map<String, String> values,
        TableProperties tp, CollectFormParameters params) {
      /*
       * This is currently implemented thinking that all you need to have
       * is:
       *
       * <?xml version='1.0' ?><data id="tablesaddrowformid">
       *
       * followed by a series of:
       *
       * <columnName1>firstFieldData</columnName1>
       * ...
       * <lastColumn>lastField</lastField>
       *
       * We will just go ahead and write all the fields/columns, knowing that
       * the form will simply ignore those for which it does not have matching
       * entry fields.
       *
       */
      try {
        FileWriter writer = new FileWriter(getEditRowFormFile());
        writer.write("<?xml version='1.0' ?><");
        writer.write(params.getRootElement());
        writer.write(" id=\"");
        writer.write(params.getFormId());
        writer.write("\">");
        for (ColumnProperties cp : tp.getColumns()) {
          String value = values.get(cp.getElementName());
          if (value == null) {
            writer.write("<" + cp.getElementName() + "/>");
          } else {
            writer
                .write("<" + cp.getElementName() + ">" + value + "</" +
                    cp.getElementName() + ">");

          }
        }
//      for (Map.Entry<String, String> entry : values.entrySet()) {
//      String value = entry.getValue();
//      if (value == null) {
//        writer.write("<" + entry.getKey() + "/>");
//      } else {
//        writer
//            .write("<" + entry.getKey() + ">" + value + "</" +
//                entry.getKey() + ">");
//
//      }
//    }
        writer.write("</");
        writer.write(params.getRootElement());
        writer.write(">");
        writer.close();
        return true;
      } catch (IOException e) {
        Log.e(TAG, "IOException while writing data file");
        e.printStackTrace();
        return false;
      }
    }

    /**
     * Insert the values existing in the file specified by
     * {@link DATA_FILE_PATH_AND_NAME} into the form specified by params.
     * <p>
     * PRECONDITION: in order to be populated with data, the data file
     * containing the row's data must have been written, most likely by calling
     * writeRowDataToBeEdited().
     * <p>
     * PRECONDITION: previous instances should already have been deleted by
     * now, or the passed in file names should be uniqued by adding timestamps,
     * or something.
     * @param params the identifying parameters for the form. Should be the
     *   same object used to write the instance file.
     * @param rowNum the row number of the row being edited
     * @param resolver the ContentResolver of the activity making the request.
     * @return
     */
    /*
     * This is based on the code at:
     * http://code.google.com/p/opendatakit/source/browse/src/org/odk/collect/android/tasks/SaveToDiskTask.java?repo=collect
     * in the method updateInstanceDatabase().
     */
    public static Uri getUriForInsertedData(CollectFormParameters params,
        int rowNum, ContentResolver resolver) {
      ContentValues values = new ContentValues();
      // First we need to fill the values with various little things.
      String displayName;
      if (params.getRowDisplayName() == null) {
        displayName = String.valueOf(rowNum);
      } else {
        displayName = params.getRowDisplayName();
      }
      values.put(COLLECT_KEY_DISPLAY_NAME, displayName);
      values.put(COLLECT_KEY_STATUS, COLLECT_KEY_STATUS_COMPLETE);
      values.put(COLLECT_KEY_CAN_EDIT_WHEN_COMPLETE, Boolean.toString(true));
      values.put(COLLECT_KEY_INSTANCE_FILE_PATH, getEditRowFormFile().getAbsolutePath());
      values.put(COLLECT_KEY_JR_FORM_ID, params.getFormId());
      // only add the version if it exists (ie not null)
      if (params.getFormVersion() != null) {
        values.put(COLLECT_KEY_JR_VERSION, params.getFormVersion());
      }
      // now we want to get the uri for the insertion.
      Uri uriOfForm = resolver.insert(CONTENT_INSTANCE_URI, values);
      return uriOfForm;
    }

    /**
     * Delete the form specified by the id given in the parameters. Does not
     * check form version.
     * @param resolver ContentResolver of the calling activity
     * @param formId the id of the form to be deleted
     * @return the result of the the delete call
     */
    public static int deleteForm(ContentResolver resolver, String formId) {
      try {
        return resolver.delete(CONTENT_FORM_URI, COLLECT_KEY_JR_FORM_ID + "=?",
            new String[] {formId});
      } catch (Exception e) {
        Log.d(TAG, "caught an exception while deleting a form, returning" +
        		" 0 and proceeding");
        return 0;
      }
    }

    /**
     * Insert a form into collect. Returns the URI of the inserted form. Note
     * that form version is not passed in with the content values and is likely
     * therefore not considered. (Not sure exactly how collect checks this.)
     * <p>
     * Precondition: the form should not exist in Collect before this call is
     * made. In other words, the a query made on the form should return no
     * results.
     * @param resolver the ContentResolver of the calling activity
     * @param formFilePath the filePath to the form
     * @param displayName the displayName of the form
     * @param formId the id of the form
     * @return the result of the insert call, likely the URI of the resulting
     *   form. If the form was not first deleted there could be a problem
     */
    public static Uri insertFormIntoCollect(ContentResolver resolver,
        String formFilePath, String displayName, String formId) {
      ContentValues insertValues = new ContentValues();
      insertValues.put(COLLECT_KEY_FORM_FILE_PATH, formFilePath);
      insertValues.put(COLLECT_KEY_DISPLAY_NAME, displayName);
      insertValues.put(COLLECT_KEY_JR_FORM_ID, formId);
      return resolver.insert(CONTENT_FORM_URI, insertValues);
    }

    /**
     * Return the URI of the form for adding a row to a table. If the formId
     * is custom defined it must exist to Collect (most likely by putting the
     * form in Collect's form folder and starting Collect once). If the form
     * does not exist, it inserts the static addRowForm information into
     * Collect.
     * <p>
     * Display name only matters if it is a programmatically generated form.
     * <p>
     * Precondition: If formId refers to a custom form, it must have already
     * been scanned in and known to exist to Collect. If the formId is not
     * custom, but refers to a form built on the fly, it should be the id
     * of {@link COLLECT_ADDROW_FORM_ID}, and the form should already have
     * been written.
     * @param resolver ContentResolver of the calling activity
     * @param formId id of the form whose uri will be returned
     * @param formDisplayName display name of the table. Only pertinent if the
     *   form has been programmatically generated.
     * @return the uri of the form.
     */
    public static Uri getUriOfForm(ContentResolver resolver,
        String formId) {
      Uri resultUri = null;
      Cursor c = null;
      try {
        c = resolver.query(
            CollectUtil.CONTENT_FORM_URI, null,
            CollectUtil.COLLECT_KEY_JR_FORM_ID +"=?",
            new String[] {formId}, null);
        if (!c.moveToFirst()) {
          Log.e(TAG, "query of Collect for form returned no results");
        } else {
          // we got a result, meaning that the form exists in collect.
          // so we just need to set the URI.
          int collectFormKey; // this is the primary key of the form in Collect's
          // database.
          collectFormKey = c.getInt(c.getColumnIndexOrThrow(BaseColumns._ID));
          resultUri = (Uri.parse(CollectUtil.CONTENT_FORM_URI + "/" +
            collectFormKey));
        }
      } finally {
        if (c != null && !c.isClosed()) {
          c.close();
        }
      }
      return resultUri;
    }

    /**
     * This is a convenience method that should be called when generating
     * non-user defined forms for adding or editing rows. It calls, in this
     * order, {@link deleteForm}, {@link buildBlankForm}, and
     * {@link insertFormIntoCollect}.
     * @param resolver content resolver of the calling activity
     * @param params
     * @param tp
     * @return true if every method returned successfully
     */
    public static boolean deleteWriteAndInsertFormIntoCollect(
        ContentResolver resolver, CollectFormParameters params,
        TableProperties tp) {
      if (params.isCustom()) {
        Log.e(TAG, "passed custom form to be deleted, rewritten, and " +
        		"inserted into Collect. Not performing task.");
        return false;
      }
      CollectUtil.deleteForm(resolver,
          params.getFormId());
      // First we want to write the file.
      boolean writeSuccessful =
          CollectUtil.buildBlankForm(getAddRowFormFile(),
              tp.getColumns(), tp.getDisplayName(), params.getFormId());
      if (!writeSuccessful) {
        Log.e(TAG, "problem writing file for add row");
        return false;
      }
      // Now we want to insert the file.
      Uri insertedFormUri = CollectUtil.insertFormIntoCollect(
          resolver, getAddRowFormFile().getAbsolutePath(),
          tp.getDisplayName(), params.getFormId());
      if (insertedFormUri == null) {
        Log.e(TAG, "problem inserting form into collect, return uri was null");
        return false;
      }
      return true;
    }
    
    /**
     * Return an intent that can be used to edit a row.
     * <p>
     * The idea here is that we might want to edit a row of the table using a
     * pre-set Collect form. This form would be user-defined and would be a 
     * more user-friendly thing that would display only the pertinent 
     * information for a particular user.
     * @param table
     * @param rowNum
     * @return
     */
    /*
     * This is a move away from the general "odk add row" usage that is going on
     * when no row is defined. As I understand it, the new case will work as
     * follows.
     *
     * There exits an "tableEditRow" form for a particular table. This form, 
     * as I understand it, must exist both in the tables directory, as well as 
     * in Collect so that Collect can launch it with an Intent.
     *
     * You then also construct a "values" sort of file, that is the data from 
     * the database that will pre-populate the fields. Mitch referred to 
     * something like this as the "instance" file.
     *
     * Once you have both of these files, the form and the data, you insert the
     * data into the form. When you launch the form, it is then pre-populated
     * with data from the database.
     *
     * In order to make this work, the form must exist both within the places
     * Collect knows to look, as well as in the Tables folder. You also must 
     * know the:
     *
     * collectFormVersion
     * collectFormId
     * collectXFormRootElement (default to "data")
     *
     * These will most likely exist as keys in the key value store. They must
     * match the form.
     *
     * Other things needed will be:
     *
     * instanceFilePath  // I think the filepath with all the values
     * displayName       // just text, eg a row ID
     * formId            // the same thing as collectFormId?
     * formVersion
     * status            // either INCOMPLETE or COMPLETE
     *
     * Examples for how this is done in Collect can be found in the Collect 
     * code in org.odk.collect.android.tasks.SaveToDiskTask.java, in the
     * updateInstanceDatabase() method.
     */
    public static Intent getIntentForOdkCollectEditRow(Context context, 
        TableProperties tp, UserTable table, int rowNum, 
        CollectFormParameters params) {
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
      Map<String, String> elementNameToValue = new HashMap<String, String>();
      for (ColumnProperties cp : tp.getColumns()) {
        String value = table.getData(rowNum,
            tp.getColumnIndex(cp.getElementName()));
        elementNameToValue.put(cp.getElementName(), value);
      }
      boolean writeDataSuccessful =
          CollectUtil.writeRowDataToBeEdited(elementNameToValue, tp, params);
      if (!writeDataSuccessful) {
        Log.e(TAG, "could not write instance file successfully!");
      }
      Uri insertUri =
          CollectUtil.getUriForInsertedData(params, rowNum,
              context.getContentResolver());
      // Copied the below from getIntentForOdkCollectEditRow().
      Intent intent = new Intent();
      intent.setComponent(new ComponentName("org.odk.collect.android",
              "org.odk.collect.android.activities.FormEntryActivity"));
      intent.setAction(Intent.ACTION_EDIT);
      intent.setData(insertUri);
      return intent;
    }

    
    /**
     * Return an intent that can be launched to add a row.
     * @param context
     * @param tp
     * @param params
     * @param elementNameToValue
     * @return
     */
    public static Intent getIntentForOdkCollectAddRow(Context context,
        TableProperties tp, CollectFormParameters params,
        Map<String, String> elementNameToValue) {
      /*
       * So, there are several things to check here. The first thing we want to
       * do is see if a custom form has been defined for this table. If there is
       * not, then we will need to write a custom one. When we do this, we will
       * then have to call delete on Collect to remove the old form, which will
       * have used the same id. This will not fail if a form has not been already
       * been written--delete will simply return 0.
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
      Uri formToLaunch;
      if (elementNameToValue == null) {
        formToLaunch = CollectUtil.getUriOfForm(context.getContentResolver(),
            params.getFormId());
        if (formToLaunch == null) {
          Log.e(TAG, "URI of the form to pass to Collect and launch was null");
          return null;
        }
      } else {
        // we've received some values to prepopulate the add row with.
        boolean writeDataSuccessful =
            CollectUtil.writeRowDataToBeEdited(elementNameToValue, tp, params);
        if (!writeDataSuccessful) {
          Log.e(TAG, "could not write instance file successfully!");
        }
        // Here we'll just act as if we're inserting 0, which
        // really doesn't matter?
        formToLaunch =
            CollectUtil.getUriForInsertedData(params, 0,
                context.getContentResolver());
      }
      // And now finally create the intent.
      Intent intent = new Intent();
      intent.setComponent(new ComponentName("org.odk.collect.android",
          "org.odk.collect.android.activities.FormEntryActivity"));
      intent.setAction(Intent.ACTION_EDIT);
      intent.setData(formToLaunch);
      return intent;
    }

    /**
     * This is holds the most basic information needed to define
     * a form to be opened by Collect. Essentially it wraps the formVersion,
     * formId, and formXMLRootElement.
     * <p>
     * Its accessor methods return the default values or the set values as
     * appropriate, so that calling the getters will be safe and there will be
     * no need for checking returned values for null or whatever else.
     * <p>
     * At least at the moment, this is conceptualized to exist alongside the
     * current interaction with Collect, which writes out a complete form
     * that includes every column in the database on a single swipe through
     * screen. This instead is supposed to fill a pre-defined form that has
     * been set by the user.
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

      private CollectFormParameters(String formId) {
        this.mFormId = formId;
        this.mFormVersion = null;
        this.mFormVersion = null;
        this.mRowDisplayName = null;
        this.mIsCustom = false;
      }

      /**
       * Create an object housing parameters for a Collect form. Very
       * important is the isCustom parameter, which should be true is a custom
       * form has been defined, and false otherwise. This will have
       * implications for which forms are used and deleted and created, and is
       * very important to get right.
       * @param isCustom
       * @param formId
       * @param formVersion
       * @param formXMLRootElement
       */
      private CollectFormParameters(boolean isCustom, String formId,
          String formVersion, String formXMLRootElement) {
        this.mIsCustom = isCustom;
        this.mFormId = formId;
        this.mFormVersion = formVersion;
        this.mFormXMLRootElement = formXMLRootElement;
      }

      /**
       * Construct a CollectFormProperties object from the given
       * TableProperties.
       * The object is determined to have custom parameters if a formId can be
       * retrieved from the TableProperties object. Otherwise the default
       * addrow
       * parameters are set. If no formVersion is defined, it is left as null,
       *  as
       * later on a check is used that if none is defined (ie is null), do not
       * insert it to a map. If no root element is defined, the default root
       * element is added.
       * @param tp
       * @return
       */
      public static CollectFormParameters constructCollectFormParameters(
          TableProperties tp) {
        KeyValueStoreHelper kvsh =
            tp.getKeyValueStoreHelper(CollectUtil.KVS_PARTITION);
        KeyValueHelper aspectHelper =
            kvsh.getAspectHelper(CollectUtil.KVS_ASPECT);
        String formId = aspectHelper.getString(CollectUtil.KEY_FORM_ID);
        if (formId == null) {
          return new CollectFormParameters(false,
              COLLECT_ADDROW_FORM_ID, null, DEFAULT_ROOT_ELEMENT);
        }
        // Else we know it is custom.
        String formVersion =
            aspectHelper.getString(CollectUtil.KEY_FORM_VERSION);
        String rootElement =
            aspectHelper.getString(CollectUtil.KEY_FORM_ROOT_ELEMENT);
        if (rootElement == null) {
          rootElement = DEFAULT_ROOT_ELEMENT;
        }
        return new CollectFormParameters(true, formId, formVersion,
            rootElement);
      }

      public void setFormId(String formId) {
        this.mFormId = formId;
      }

      public void setFormVersion(String formVersion) {
        this.mFormVersion = formVersion;
      }

      public void setRootElement(String rootElement) {
        this.mFormXMLRootElement = rootElement;
      }

      public void setRowDisplayName(String name) {
        this.mRowDisplayName = name;
      }

      public void setIsCustom(boolean isCustom) {
        this.mIsCustom = isCustom;
      }

      public boolean isCustom() {
        return this.mIsCustom;
      }

      /**
       * Return the root element of the form to be used for writing. If none
       * has been set, returns the {@link DEFAULT_ROOT_ELEMENT}.
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
       * Return the form version. This does not do any null checking. A null
       * value means that no form version has been specified and it should
       * just be omitted.
       * @return
       */
      public String getFormVersion() {
        return this.mFormVersion;
      }

      /**
       * Return the ID of the form.
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
