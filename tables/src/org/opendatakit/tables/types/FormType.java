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

import org.opendatakit.common.android.data.KeyValueHelper;
import org.opendatakit.common.android.data.KeyValueStoreHelper;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.tables.utils.CollectUtil.CollectFormParameters;
import org.opendatakit.tables.utils.SurveyUtil.SurveyFormParameters;

/**
 * Definition of the form data type.
 *
 * @author sudar.sam@gmail.com
 *
 */
public class FormType {

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

  public static FormType constructFormType(TableProperties tp) {
    KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(FormType.KVS_PARTITION);
    KeyValueHelper aspectHelper = kvsh.getAspectHelper(FormType.KVS_ASPECT);
    String formType = aspectHelper.getString(FormType.KEY_FORM_TYPE);
    if (formType == null) {
      return new FormType(CollectFormParameters.constructDefaultCollectFormParameters(tp), tp);
    }
    try {
      Type t = Type.valueOf(formType);
      if (t == Type.COLLECT) {
        return new FormType(CollectFormParameters.constructCollectFormParameters(tp), tp);
      }
      return new FormType(SurveyFormParameters.constructSurveyFormParameters(tp), tp);
    } catch (Exception e) {
      return new FormType(CollectFormParameters.constructCollectFormParameters(tp), tp);
    }
  }

  public void persist(TableProperties tp) {
    KeyValueStoreHelper kvsh = tp.getKeyValueStoreHelper(FormType.KVS_PARTITION);
    KeyValueHelper aspectHelper = kvsh.getAspectHelper(FormType.KVS_ASPECT);
    aspectHelper.setString(KEY_FORM_TYPE, type.name());

    this.mCollectParams.persist(tp);
    this.mSurveyParams.persist(tp);
  }

  public FormType(CollectFormParameters params, TableProperties tp) {
    this.type = Type.COLLECT;
    this.mCollectParams = params;
    this.mSurveyParams = SurveyFormParameters.constructSurveyFormParameters(tp);
  }

  public FormType(SurveyFormParameters params, TableProperties tp) {
    this.type = Type.SURVEY;
    this.mSurveyParams = params;
    this.mCollectParams = CollectFormParameters.constructCollectFormParameters(tp);
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
