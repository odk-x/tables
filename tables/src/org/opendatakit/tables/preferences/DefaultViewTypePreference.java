package org.opendatakit.tables.preferences;

import java.util.Arrays;

import org.opendatakit.common.android.data.PossibleTableViewTypes;
import org.opendatakit.common.android.data.TableProperties;
import org.opendatakit.common.android.data.TableViewType;
import org.opendatakit.tables.R;
import org.opendatakit.tables.utils.PreferenceUtil;
import org.opendatakit.tables.views.components.TableViewTypeAdapter;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;

public class DefaultViewTypePreference extends ListPreference {

  /** The view types allowed for the table this preference will display. */
  private TableProperties mTableProperties;
  private PossibleTableViewTypes mPossibleViewTypes;
  private Context mContext;
  private CharSequence[] mEntryValues;
  
  public DefaultViewTypePreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.mContext = context;
  }
  
  public void setFields(TableProperties tableProperties) {
    this.mTableProperties = tableProperties;
    this.mPossibleViewTypes = tableProperties.getPossibleViewTypes();
    // Let's set the currently selected one.
    this.mEntryValues = this.mContext.getResources().getTextArray(
        R.array.table_view_types_values);
    TableViewType defaultViewType = tableProperties.getDefaultViewType();
    if (defaultViewType == null) {
      // default to spreadsheet.
      this.setValueIndex(0);
    } else {
      int index = Arrays.asList(this.mEntryValues)
          .indexOf(defaultViewType.name());
      this.setValueIndex(index);
    }
  }
  
  @Override
  protected void onPrepareDialogBuilder(Builder builder) {
    // We want to enable/disable the correct list.
    ListAdapter adapter = new TableViewTypeAdapter(
        this.mContext,
        android.R.layout.select_dialog_singlechoice,
        this.getEntries(),
        this.getEntryValues(),
        this.mPossibleViewTypes);
    builder.setAdapter(adapter, this);
    super.onPrepareDialogBuilder(builder);
  }
  
  

}
