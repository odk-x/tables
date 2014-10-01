package org.opendatakit.tables.views.components;

import java.util.List;

import org.opendatakit.aggregate.odktables.rest.SyncState;
import org.opendatakit.common.android.data.ColorRule;
import org.opendatakit.common.android.data.ColorRuleGroup;
import org.opendatakit.common.android.database.DatabaseFactory;
import org.opendatakit.common.android.utilities.ColumnUtil;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.tables.R;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

public class ColorRuleAdapter extends ArrayAdapter<ColorRule> {
  
  private static final String TAG = ColorRuleAdapter.class.getSimpleName();
  
  private final Context mContext;
  private final String mAppName;
  private final String mTableId;
  private List<ColorRule> mColorRules;
  private int mResourceId;
  private ColorRuleGroup.Type mType;
  
  public ColorRuleAdapter(
      Context context,
      String appName,
      String tableId,
      int resource,
      List<ColorRule> colorRules,
      ColorRuleGroup.Type colorRuleType) {
    super(context, resource, colorRules);
    this.mContext = context;
    this.mAppName = appName;
    this.mTableId = tableId;
    this.mResourceId = resource;
    this.mColorRules = colorRules;
    this.mType = colorRuleType;
  }
  
  private View createView(ViewGroup parent) {
    LayoutInflater layoutInflater = (LayoutInflater)
        parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    return layoutInflater.inflate(
        this.mResourceId,
        parent,
        false);
  }
  
  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View row = convertView;
    if (row == null) {
      row = this.createView(parent);
    }
    final int currentPosition = position;
    // We'll need to display the display name if this is an editable field.
    // (ie if a status column or table rule)
    String description = "";
    boolean isMetadataRule = false;
    if (mType == ColorRuleGroup.Type.STATUS_COLUMN ||
        mType == ColorRuleGroup.Type.TABLE) {
      ColorRule colorRule = mColorRules.get(currentPosition);
      String elementKey = colorRule.getColumnElementKey();
      if (ODKDatabaseUtils.get().getAdminColumns().contains(elementKey)) {
        isMetadataRule = true;
        // We know it must be a String rep of an int.
        SyncState targetState = SyncState.valueOf(colorRule.getVal());
        // For now we need to handle the special cases of the sync state.
        if (targetState == SyncState.new_row) {
          description = this.mContext.getString(
              R.string.sync_state_equals_new_row_message);
        } else if (targetState == SyncState.changed) {
          description = this.mContext.getString(
              R.string.sync_state_equals_changed_message);
        } else if (targetState == SyncState.synced) {
          description = this.mContext.getString(
              R.string.sync_state_equals_synced_message);
        } else if (targetState == SyncState.synced_pending_files) {
          description = this.mContext.getString(
              R.string.sync_state_equals_synced_pending_files_message);
        } else if (targetState == SyncState.deleted) {
          description = this.mContext.getString(
              R.string.sync_state_equals_deleted_message);
        } else if (targetState == SyncState.in_conflict) {
          description = this.mContext.getString(
              R.string.sync_state_equals_in_conflict_message);
        } else {
          Log.e(TAG, "unrecognized sync state: " + targetState);
          description = "unknown";
        }
      } else {

        String localizedDisplayName;
        SQLiteDatabase db = null;
        try {
          db = DatabaseFactory.get().getDatabase(mContext, mAppName);
          localizedDisplayName = ColumnUtil.get().getLocalizedDisplayName(db, mTableId, elementKey);
        } finally {
          if ( db != null ) {
            db.close();
          }
        }

        description = localizedDisplayName;
      }
    }
    if (!isMetadataRule) {
      description += " " +
          mColorRules.get(currentPosition).getOperator().getSymbol() + " " +
          mColorRules.get(currentPosition).getVal();
    }
    TextView label =
        (TextView) row.findViewById(R.id.row_label);
    label.setText(description);
    final int backgroundColor =
        mColorRules.get(currentPosition).getBackground();
    final int textColor =
        mColorRules.get(currentPosition).getForeground();
    // Will demo the color rule.
    TextView exampleView =
        (TextView) row.findViewById(R.id.row_ext);
    exampleView.setText(this.mContext.getString(R.string.status_column));
    exampleView.setTextColor(textColor);
    exampleView.setBackgroundColor(backgroundColor);
    exampleView.setVisibility(View.VISIBLE);
    // The radio button is meaningless here, so get it off the screen.
    final RadioButton radioButton = (RadioButton)
        row.findViewById(R.id.radio_button);
    radioButton.setVisibility(View.GONE);
    // And now the settings icon.
    final ImageView editView = (ImageView)
        row.findViewById(R.id.row_options);
    final View holderView = row;
    editView.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        holderView.showContextMenu();
      }
    });
    return row;
  }
}
