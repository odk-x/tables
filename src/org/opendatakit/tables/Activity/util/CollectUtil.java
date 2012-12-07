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
package org.opendatakit.tables.Activity.util;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.kxml2.io.KXmlParser;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.opendatakit.tables.data.ColumnProperties;
import org.opendatakit.tables.data.TableProperties;
import org.opendatakit.tables.data.UserTable;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

/**
 * Utility methods for using ODK Collect.
 */
public class CollectUtil {
  
  private static final String TAG = "CollectUtil";
  
  /*
   * The names here should match those in the version of collect that is on
   * the phone. They came from InstanceProviderApi.
   */
  public static final String STATUS = "status";
  public static final String STATUS_INCOMPLETE = "incomplete";
  public static final String STATUS_COMPLETE = "complete";
  public static final String CAN_EDIT_WHEN_COMPLETE = "canEditWhenComplete";
  public static final String SUBMISSION_URI = "submissionUri";
  public static final String INSTANCE_FILE_PATH = "instanceFilePath";
  public static final String JR_FORM_ID = "jrFormId";
  public static final String JR_VERSION = "jrVersion";
  public static final String DISPLAY_NAME = "displayName";
  public static final String COLLECT_AUTHORITY = 
      "org.odk.collect.android.provider.odk.instances";
  public static final Uri CONTENT_URI = 
      Uri.parse("content://" + COLLECT_AUTHORITY + "/instances");
  
  /********************
   * Keys present in the Key Value Store. These should represent data about
   * the form that is present if a form is defined for a particular table.
   ********************/
  public static final String KEY_FORM_VERSION = 
      "CollectUtil.collectFormVersion";
  public static final String KEY_FORM_ID = "CollectUtil.formId";
  public static final String KEY_FORM_ROOT_ELEMENT = "CollectUtil.rootElement";

  
  /**
   * This is the file name and path of the single file that will be written
   * that contains the key values of column name to data for a given row
   * that will be edited.
   */
  public static final String DATA_FILE_PATH_AND_NAME = 
      "/sdcard/odk/tables/editRowData.xml";
  
  /**
   * The default value of the root element for the form. 
   */
  public static final String DEFAULT_ROOT_ELEMENT = "data";
    
    public static void buildForm(String filename, String[] keys, String title,
            String id) {
        try {
            FileWriter writer = new FileWriter(filename);
            writer.write("<h:html xmlns=\"http://www.w3.org/2002/xforms\" " +
                    "xmlns:h=\"http://www.w3.org/1999/xhtml\" " +
                    "xmlns:ev=\"http://www.w3.org/2001/xml-events\" " +
                    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:jr=\"http://openrosa.org/javarosa\">");
            writer.write("<h:head>");
            writer.write("<h:title>" + title + "</h:title>");
            writer.write("<model>");
            writer.write("<instance>");
            writer.write("<data id=\"" + id + "\">");
            for (String key : keys) {
                writer.write("<" + key + "/>");
            }
            writer.write("</data>");
            writer.write("</instance>");
            writer.write("<itext>");
            writer.write("<translation lang=\"eng\">");
            for (String key : keys) {
                writer.write("<text id=\"/data/" + key + ":label\">");
                writer.write("<value>" + key + "</value>");
                writer.write("</text>");
            }
            writer.write("</translation>");
            writer.write("</itext>");
            writer.write("</model>");
            writer.write("</h:head>");
            writer.write("<h:body>");
            for (String key : keys) {
                writer.write("<input ref=\"/data/" + key + "\">");
                writer.write("<label ref=\"jr:itext('/data/" + key +
                        ":label')\"/>");
                writer.write("</input>");
            }
            writer.write("</h:body>");
            writer.write("</h:html>");
            writer.close();
        } catch(IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
    public static boolean writeRowDataToBeEdited(UserTable table, int rowNum,
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
        FileWriter writer = new FileWriter(DATA_FILE_PATH_AND_NAME);
        writer.write("<?xml version='1.0' ?><data id=\"");
        writer.write(params.getFormId());
        writer.write("\">");
        for (ColumnProperties cp : tp.getColumns()) {
          String value = table.getData(rowNum,
              tp.getColumnIndex(cp.getColumnDbName()));
          if (value == null) {
            writer.write("<" + cp.getColumnDbName() + "/>");
          } else {
            writer
                .write("<" + cp.getColumnDbName() + ">" + value + "</" + 
                    cp.getColumnDbName() + ">");

          }
        }
        writer.write("</data>");
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
      values.put(DISPLAY_NAME, displayName);
      values.put(STATUS, STATUS_COMPLETE);
      values.put(CAN_EDIT_WHEN_COMPLETE, Boolean.toString(true));
      values.put(INSTANCE_FILE_PATH, DATA_FILE_PATH_AND_NAME);
      values.put(JR_FORM_ID, params.getFormId());
      // only add the version if it exists (ie not null)
      if (params.getFormVersion() != null) {
        values.put(JR_VERSION, params.getFormVersion());
      }
      // now we want to get the uri for the insertion.
      Uri uriOfForm = resolver.insert(CONTENT_URI, values); 
      return uriOfForm;
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
      
      /*
       * Just putting this here in case it needs to be serialized at some point
       * and someone forgets about this requirement.
       */
      private CollectFormParameters() {
      }
      
      public CollectFormParameters(String formId) {
        this.mFormId = formId;
        this.mFormVersion = null;
        this.mFormVersion = null;
        this.mRowDisplayName = null;
      }
      
      public CollectFormParameters(String formId, String formVersion, 
          String formXMLRootElement) {
        this.mFormId = formId;
        this.mFormVersion = formVersion;
        this.mFormXMLRootElement = formXMLRootElement;
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
