/*
 * Copyright (C) 2014 University of Washington
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
package org.opendatakit.tables.views;

import android.os.Parcel;
import android.os.Parcelable;

public class CellInfo implements Parcelable {
  // elementKey may be null if we ever need a touch
  // listener on status column. For now, everything works.

  public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
    public CellInfo createFromParcel(Parcel in) {
      return new CellInfo(in);
    }

    public CellInfo[] newArray(int size) {
      return new CellInfo[size];
    }
  };
  public final int rowId;
  // this is ONLY relevant to this TabularView
  final int colPos;
  public String elementKey;

  public CellInfo(String elementKey, int colPos, int rowId) {
    this.elementKey = elementKey;
    this.colPos = colPos;
    this.rowId = rowId;
  }

  public CellInfo(Parcel in) {
    if (in.readByte() == 1) {
      elementKey = in.readString();
    }
    rowId = in.readInt();
    colPos = in.readInt();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    if (elementKey == null) {
      dest.writeByte((byte) 0);
    } else {
      dest.writeByte((byte) 1);
      dest.writeString(elementKey);
    }
    dest.writeInt(rowId);
    dest.writeInt(colPos);
  }
}