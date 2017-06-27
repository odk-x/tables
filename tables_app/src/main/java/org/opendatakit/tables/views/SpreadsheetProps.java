package org.opendatakit.tables.views;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Niles on 6/20/17.
 * used in TableDisplayActivity to store stuff for SpreadsheetFragment
 */
public class SpreadsheetProps implements Parcelable {
  /**
   * parcelable cruft
   */
  public static final Parcelable.Creator<SpreadsheetProps> CREATOR = new Parcelable
      .Creator<SpreadsheetProps>() {
    public SpreadsheetProps createFromParcel(Parcel in) {
      return new SpreadsheetProps(in);
    }

    public SpreadsheetProps[] newArray(int size) {
      return new SpreadsheetProps[size];
    }
  };
  /**
   * Boolean to hold whether a data menu is open, so it can be re-opened after rotate
   */
  public boolean dataMenuOpen = false;
  /**
   * Boolean to hold whether a header menu is open, so it can be re-opened after rotate
   */
  public boolean headerMenuOpen = false;
  /**
   * Boolean to hold whether a delete row dialog is open, so it can be re-opened after rotate
   */
  public boolean deleteDialogOpen = false;
  /**
   * cellInfo stored so that the delete dialog and row actions menu know which cell was double
   * tapped or long tapped in order to open them.
   */
  public CellInfo lastDataCellMenued;
  /**
   * cellInfo stored so that the column actions menu know which cell was double tapped or long
   * tapped in order to open them.
   */
  public CellInfo lastHeaderCellMenued;
  /**
   * The properties of the sql query that the user can manipulate
   */
  private String sort;
  private String sortOrder;
  private String frozen;
  private String[] groupBy;
  /**
   * the activity to put the properties into in order to update the parent about changes to the
   * four sql properties
   */
  private Activity act;

  /**
   * empty constructor
   */
  public SpreadsheetProps() {

  }

  /**
   * Restores the properties from a parcel.
   *
   * @param in the parcel we serialized everything to
   */
  private SpreadsheetProps(Parcel in) {
    sort = readString(in);
    sortOrder = readString(in);
    frozen = readString(in);
    if (in.readByte() == 1) {
      int length = in.readInt();
      groupBy = new String[length];
      in.readStringArray(groupBy);
    }
    boolean[] bools = new boolean[3];
    in.readBooleanArray(bools);
    dataMenuOpen = bools[0];
    headerMenuOpen = bools[1];
    deleteDialogOpen = bools[2];
    lastDataCellMenued = readCellInfo(in);
    lastHeaderCellMenued = readCellInfo(in);
  }

  /**
   * writes properties to a parcel
   *
   * @param dest  the parcel we serialize to
   * @param flags unused
   */
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
    dest.writeBooleanArray(new boolean[] { dataMenuOpen, headerMenuOpen, deleteDialogOpen });
    writeCellInfo(dest, lastDataCellMenued);
    writeCellInfo(dest, lastHeaderCellMenued);
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

  /**
   * Puts props in the result so the calling intent will know about any changes made to the sql
   * properties. For example, if you open a collection view, freeze a column and reverse the sort
   * direction while there, then go back, your changes won't get undone because the calling
   * activity will
   * <p>
   * Originally I wanted to try using act.getCallingActivity(), seeing if it was an instance of
   * ISpreadsheetFragmentContainer and if it was, using getProps().setFrozen(), etc.. on it, but
   * getCallingActivity returns a ComponentName not an object
   */
  private void updateParent() {
    if (act == null)
      return;
    Intent i = new Intent();
    i.putExtra("props", this);
    act.setResult(Activity.RESULT_OK, i);
  }

  /**
   * Parcelable cruft
   *
   * @return 0
   */
  @Override
  public int describeContents() {
    return 0;
  }

  /**
   * Reads a string from a parcel if it was in there
   *
   * @param in the parcel to read from
   * @return the string that was originally put in the parcelable
   */
  private static String readString(Parcel in) {
    if (in.readByte() == 1) {
      return in.readString();
    }
    return null;
  }

  /**
   * Reads a cell info object from a parcel
   *
   * @param in the parcel to read from
   * @return the cell info object we read
   */
  private static CellInfo readCellInfo(Parcel in) {
    if (in.readByte() == 0) {
      return null;
    }
    return in.readParcelable(CellInfo.class.getClassLoader());
  }

  /**
   * Writes a string to the parcel if it's not null
   *
   * @param out the parcel to write to
   * @param str the string to write
   */
  private static void writeString(Parcel out, String str) {
    if (str == null) {
      out.writeByte((byte) 0);
    } else {
      out.writeByte((byte) 1);
      out.writeString(str);
    }
  }

  /**
   * Writes a cell info object to a parcel
   *
   * @param dest the parcel to write to
   * @param cell the cell to write to the parcel
   */
  private static void writeCellInfo(Parcel dest, Parcelable cell) {
    if (cell == null) {
      dest.writeByte((byte) 0);
    } else {
      dest.writeByte((byte) 1);
      dest.writeParcelable(cell, 0);
    }
  }

  /**
   * sets the activity so that when updateParent() is called, it can set the activity's result
   *
   * @param act the activity these properties belong to
   */
  public void setActivity(Activity act) {
    this.act = act;
  }
}

