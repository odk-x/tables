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
package org.opendatakit.tables.types;

import android.app.Activity;
import org.opendatakit.aggregate.odktables.rest.ElementDataType;
import org.opendatakit.database.data.KeyValueStoreEntry;
import org.opendatakit.database.service.DbHandle;
import org.opendatakit.database.service.UserDbInterface;
import org.opendatakit.database.utilities.KeyValueStoreUtils;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.SurveyUtil.SurveyFormParameters;

/**
 * Definition of the form data type.
 *
 * @author sudar.sam@gmail.com
 */
public class FormType {
  /**
   * The key value store triplet's partition
   */
  public static final String KVS_PARTITION = "FormType";
  /**
   * The key value store triplet's aspect
   */
  public static final String KVS_ASPECT = "default";
  /**
   * The key value store triplet's key
   */
  public static final String KEY_FORM_TYPE = "FormType.formType";
  /**
   * Used for logging
   */
  @SuppressWarnings("unused")
  private static final String TAG = "FormType";
  private Type type;
  private SurveyFormParameters mSurveyParams;

  /**
   * Constructs a new FormType object
   *
   * @param params a SurveyFormParemeters object that contains the right form id, whether the
   *               form is user defined or not and the form's screen path
   */
  public FormType(SurveyFormParameters params) {
    this.type = Type.SURVEY;
    this.mSurveyParams = params;
  }

  /**
   * Constructs a new survey form parameters using the default form for the table, then returns
   * {@link #FormType(SurveyFormParameters)}
   * @param appName the app name
   * @param tableId the id of the table that the form will be for
   * @return a new FormType object configured with the default form for the passed table
   * @throws ServicesAvailabilityException if the database is down
   */
  public static FormType constructFormType(Activity act, String appName, String tableId)
      throws ServicesAvailabilityException {
    return new FormType(SurveyFormParameters.constructSurveyFormParameters(act, appName, tableId));
  }

  /**
   * Puts the form type in the database as the default form for the table
   * @param appName the app name
   * @param tableId the id of the table to set the default form on
   * @throws ServicesAvailabilityException if the database is down
   */
  public void persist(UserDbInterface dbInterface, String appName, DbHandle db, String tableId)
      throws ServicesAvailabilityException {
    KeyValueStoreEntry entry = KeyValueStoreUtils
        .buildEntry(tableId, FormType.KVS_PARTITION, FormType.KVS_ASPECT, FormType.KEY_FORM_TYPE,
            ElementDataType.string, type.name());

    // don't use a transaction, but ensure that if we are transitioning to
    // the survey type (or updating it), that we update its settings first.
    this.mSurveyParams.persist(dbInterface, appName, db, tableId);
    dbInterface.replaceTableMetadata(appName, db, entry);
    // and once we have transitioned, then we alter the settings
    // of the form type we are no longer using.
    this.mSurveyParams.persist(dbInterface, appName, db, tableId);
  }

  public String getFormId() {
    return this.mSurveyParams.getFormId();
  }

  public void setFormId(String formId) {
    this.mSurveyParams.setFormId(formId);
  }

  public SurveyFormParameters getSurveyFormParameters() {
    if (type == Type.SURVEY) {
      return this.mSurveyParams;
    } else {
      throw new IllegalStateException("Unexpected attempt to retrieve SurveyFormParameters");
    }
  }

  /**
   * Currently only a Survey form is valid.
   */
  @SuppressWarnings("JavaDoc")
  public enum Type {
    SURVEY
  }

}
