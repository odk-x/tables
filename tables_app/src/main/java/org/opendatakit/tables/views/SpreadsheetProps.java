package org.opendatakit.tables.views;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Niles on 6/20/17.
 * used in TableDisplayActivity to store stuff for SpreadsheetFragment
 */

public class SpreadsheetProps implements Parcelable {
  public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
    public SpreadsheetProps createFromParcel(Parcel in) {
      return new SpreadsheetProps(in);
    }

    public SpreadsheetProps[] newArray(int size) {
      return new SpreadsheetProps[size];
    }
  };
  private String sort;
  private String sortOrder;
  private String frozen;
  private String[] groupBy;
  private Activity act;

  public SpreadsheetProps() {

  }

  private SpreadsheetProps(Parcel in) {
    sort = readString(in);
    sortOrder = readString(in);
    frozen = readString(in);
    if (in.readByte() == 1) {
      int length = in.readInt();
      groupBy = new String[length];
      in.readStringArray(groupBy);
    }
  }

  public String getSort() {
    return sort;
  }

  public void setSort(String sort) {
    this.sort = sort;
    updateParent();
  }

  public String getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(String sortOrder) {
    this.sortOrder = sortOrder;
    updateParent();
  }

  public String getFrozen() {
    return frozen;
  }

  public void setFrozen(String frozen) {
    this.frozen = frozen;
    updateParent();
  }

  public String[] getGroupBy() {
    return groupBy;
  }

  public void setGroupBy(String[] groupBy) {
    this.groupBy = groupBy;
    updateParent();
  }

  private void updateParent() {
    if (act == null)
      return;
    Intent i = new Intent();
    Bundle extras = new Bundle();
    extras.putParcelable("props", this);
    i.putExtras(extras);
    act.setResult(Activity.RESULT_OK, i);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  private void writeString(Parcel out, String str) {
    if (str == null) {
      out.writeByte((byte) 0);
    } else {
      out.writeByte((byte) 1);
      out.writeString(str);
    }
  }

  private String readString(Parcel in) {
    if (in.readByte() == 1) {
      return in.readString();
    }
    return null;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    writeString(dest, sort);
    writeString(dest, sortOrder);
    writeString(dest, frozen);
    if (groupBy == null) {
      dest.writeByte((byte) 0);
    } else {
      dest.writeByte((byte) 1);
      dest.writeInt(groupBy.length);
      dest.writeStringArray(groupBy);
    }
  }

  public void setActivity(Activity act) {
    this.act = act;
  }
}

