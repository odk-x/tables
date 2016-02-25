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

import org.opendatakit.aggregate.odktables.rest.ElementDataType;
import org.opendatakit.common.android.utilities.KeyValueStoreUtils;
import org.opendatakit.database.service.KeyValueStoreEntry;
import org.opendatakit.database.service.OdkDbHandle;
import org.opendatakit.tables.application.Tables;
import org.opendatakit.tables.utils.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.utils.SurveyUtil.SurveyFormParameters;

import android.content.Context;
import android.os.RemoteException;

import java.util.List;

/**
 * Definition of the form data type.
 *
 * @author sudar.sam@gmail.com
 *
 */
public class FormType {
  private static final String TAG = "FormType";

  public static final String KVS_PARTITION = "FormType";
  public static final String KVS_ASPECT = "default";
  public static final String KEY_FORM_TYPE = "FormType.formType";

  /*
   * The two things that define a form in the system.
   */
  public enum Type {
    COLLECT, SURVEY
  };

  private Type type;
  private CollectFormParameters mCollectParams;
  private SurveyFormParameters mSurveyParams;

  public static FormType constructFormType(Context context, String appName, String tableId) throws RemoteException {
    String formType;
    OdkDbHandle db = null;
    try {
      db = Tables.getInstance().getDatabase().openDatabase(appName);
      List<KeyValueStoreEntry> kvsList =  Tables.getInstance().getDatabase()
              .getDBTableMetadata(appName, db, tableId, FormType.KVS_PARTITION, FormType.KVS_ASPECT, FormType.KEY_FORM_TYPE );
      if ( kvsList.size() != 1 ) {
        formType = null;
      } else {
        formType = KeyValueStoreUtils.getString(appName, kvsList.get(0));
      }
    } finally {
      if ( db != null ) {
        Tables.getInstance().getDatabase().closeDatabase(appName, db);
      }
    }
    // Not supporting this currently
//    if (formType == null) {
//      return new FormType(context, appName, tableId, CollectFormParameters.constructDefaultCollectFormParameters(
//          context, appName, tableId));
//    }
    try {
      Type t = Type.valueOf(formType);
      if (t == Type.COLLECT) {
        return new FormType(context, appName, tableId, CollectFormParameters.constructCollectFormParameters(
                context, appName, tableId));
      }
      return new FormType(context, appName, tableId, SurveyFormParameters.constructSurveyFormParameters(
          context, appName, tableId));
    } catch (Exception e) {
      return new FormType(context, appName, tableId, CollectFormParameters.constructCollectFormParameters(
          context, appName, tableId));
    }
  }

  public void persist(Context context, String appName, String tableId) throws RemoteException {
    OdkDbHandle db = null;
    try {
      KeyValueStoreEntry entry = KeyValueStoreUtils.buildEntry(tableId,
              FormType.KVS_PARTITION,
              FormType.KVS_ASPECT,
              FormType.KEY_FORM_TYPE,
              ElementDataType.string, type.name());

      db = Tables.getInstance().getDatabase().openDatabase(appName);
      // don't use a transaction, but ensure that if we are transitioning to
      // the collect type (or updating it), that we update its settings first.
      // ditto survey.
      if ( type == Type.COLLECT ) {
        this.mCollectParams.persist(appName, db, tableId);
      } else {
        this.mSurveyParams.persist(appName, db, tableId);
      }
      Tables.getInstance().getDatabase().replaceDBTableMetadata(appName, db, entry);
      // and once we have transitioned, then we alter the settings
      // of the form type we are no longer using.
      if ( type == Type.COLLECT ) {
        this.mSurveyParams.persist(appName, db, tableId);
      } else {
        this.mCollectParams.persist(appName, db, tableId);
      }
    } finally {
      if ( db != null ) {
        Tables.getInstance().getDatabase().closeDatabase(appName, db);
      }
    }
  }

  public FormType(Context context, String appName, String tableId, CollectFormParameters params) throws RemoteException {
    this.type = Type.COLLECT;
    this.mCollectParams = params;
    this.mSurveyParams = SurveyFormParameters.constructSurveyFormParameters(context, appName, tableId);
  }

  public FormType(Context context, String appName, String tableId, SurveyFormParameters params) throws RemoteException {
    this.type = Type.SURVEY;
    this.mSurveyParams = params;
    this.mCollectParams = CollectFormParameters.constructCollectFormParameters(
        context, appName, tableId);
  }

  public boolean isCollectForm() {
    return (type == Type.COLLECT);
  }

  public void setIsCollectForm(boolean isCollectForm) {
    if (isCollectForm) {
      type = Type.COLLECT;
    } else {
      type = Type.SURVEY;
    }
  }

  public String getFormId() {
    if (type == Type.COLLECT) {
      return this.mCollectParams.getFormId();
    } else {
      return this.mSurveyParams.getFormId();
    }
  }

  public void setFormId(String formId) {
    if (type == Type.COLLECT) {
      this.mCollectParams.setFormId(formId);
    } else {
      this.mSurveyParams.setFormId(formId);
    }
  }

  /**
   * Returns true if the form represents a default form.
   *
   * @return
   */
  public boolean isCustom() {
    if (type == Type.COLLECT) {
      return this.mCollectParams.isCustom();
    } else {
      return this.mSurveyParams.isUserDefined();
    }
  }

  public void setIsCustom(boolean isCustom) {
    if (type == Type.COLLECT) {
      this.mCollectParams.setIsCustom(isCustom);
    } else {
      this.mSurveyParams.setIsUserDefined(isCustom);
    }
  }

  public String getFormRootElement() {
    if (type == Type.COLLECT) {
      return this.mCollectParams.getRootElement();
    } else {
      throw new IllegalStateException("Unexpected attempt to retrieve FormRootElement");
    }
  }

  public void setFormRootElement(String formRootElement) {
    if (type == Type.COLLECT) {
      this.mCollectParams.setRootElement(formRootElement);
    } else {
      throw new IllegalStateException("Unexpected attempt to retrieve FormRootElement");
    }
  }

  public CollectFormParameters getCollectFormParameters() {
    if (type == Type.COLLECT) {
      return this.mCollectParams;
    } else {
      throw new IllegalStateException("Unexpected attempt to retrieve CollectFormParameters");
    }
  }

  public SurveyFormParameters getSurveyFormParameters() {
    if (type == Type.SURVEY) {
      return this.mSurveyParams;
    } else {
      throw new IllegalStateException("Unexpected attempt to retrieve SurveyFormParameters");
    }
  }

}
