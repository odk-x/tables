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

import android.content.Context;
import org.opendatakit.aggregate.odktables.rest.ElementDataType;
import org.opendatakit.database.data.KeyValueStoreEntry;
import org.opendatakit.database.service.DbHandle;
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
  public static final String KVS_PARTITION = "FormType";
  public static final String KVS_ASPECT = "default";
  public static final String KEY_FORM_TYPE = "FormType.formType";
  private static final String TAG = "FormType";
  private Type type;
  private SurveyFormParameters mSurveyParams;

  public FormType(Context context, String appName, String tableId, SurveyFormParameters params) {
    this.type = Type.SURVEY;
    this.mSurveyParams = params;
  }

  public static FormType constructFormType(Context context, String appName, String tableId)
      throws ServicesAvailabilityException {
    return new FormType(context, appName, tableId,
        SurveyFormParameters.constructSurveyFormParameters(appName, tableId));
  }

  public void persist(Context context, String appName, String tableId)
      throws ServicesAvailabilityException {
    DbHandle db = null;
    try {
      KeyValueStoreEntry entry = KeyValueStoreUtils
          .buildEntry(tableId, FormType.KVS_PARTITION, FormType.KVS_ASPECT, FormType.KEY_FORM_TYPE,
              ElementDataType.string, type.name());

      db = Tables.getInstance().getDatabase().openDatabase(appName);
      // don't use a transaction, but ensure that if we are transitioning to
      // the survey type (or updating it), that we update its settings first.
      this.mSurveyParams.persist(appName, db, tableId);
      Tables.getInstance().getDatabase().replaceTableMetadata(appName, db, entry);
      // and once we have transitioned, then we alter the settings
      // of the form type we are no longer using.
      this.mSurveyParams.persist(appName, db, tableId);
    } finally {
      if (db != null) {
        Tables.getInstance().getDatabase().closeDatabase(appName, db);
      }
    }
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

  /*
   * Currently only a Survey form is valid.
   */
  public enum Type {
    SURVEY
  }

}
